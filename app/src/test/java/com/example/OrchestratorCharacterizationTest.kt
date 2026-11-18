package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VeloraDatabase
import com.example.data.playback.Media3PlayerEngine
import com.example.data.repository.VeloraLibraryRepository
import com.example.data.scanner.MediaLibraryScanner
import com.example.data.settings.VeloraSettingsRepository
import com.example.domain.engine.MediaOrchestrator
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-5 — Characterization tests for orchestrator queue behaviour
 * (CLEAN-CODE-PLAN.md; MediaOrchestrator.kt, PlaybackQueue.kt).
 *
 * Before this file the orchestrator had ZERO direct tests; its queue
 * behaviour was only exercised incidentally by ExampleRobolectricTest
 * (play/pause/skip/seek/history + the out-of-pool prepend case). These tests
 * lock the CURRENT behaviour — including the out-of-pool cases in P4-C2 —
 * so the P1 state-slicing and P2 port refactors can prove they did not
 * change what the queue does.
 *
 * Harness mirrors ExampleRobolectricTest exactly: in-memory Room, the real
 * scanner (bundled demo tracks are real files, so the real Media3 engine
 * loads them the same way production does), a per-test DataStore file, and
 * `Dispatchers.Unconfined` so orchestrator coroutines run on the test thread
 * until their first suspension.
 *
 * DataStore persistence assertions use `withTimeout(5_000) { first { ... } }`
 * instead of a single `getSettingsOnce()`: the write is launched in the
 * orchestrator scope and lands asynchronously, so a bare read could observe
 * the pre-write state on a slow runner. The timeout fails fast with a clear
 * message instead of hanging the CI step.
 *
 * UNTESTED locally: Gradle does not run on this machine. CI on the PR is the
 * verification path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OrchestratorCharacterizationTest {

    private lateinit var context: Context
    private lateinit var database: VeloraDatabase
    private lateinit var repository: VeloraLibraryRepository
    private lateinit var settingsRepo: VeloraSettingsRepository
    private lateinit var engine: Media3PlayerEngine
    private lateinit var orchestrator: MediaOrchestrator
    private lateinit var pool: List<Track>

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, VeloraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scanner = MediaLibraryScanner(context)
        repository = VeloraLibraryRepository(
            context = context,
            trackDao = database.trackDao(),
            playlistDao = database.playlistDao(),
            eqPresetDao = database.eqPresetDao(),
            scanner = scanner,
            playbackHistoryDao = database.playbackHistoryDao()
        )
        pool = repository.performScan(emptySet(), includeMediaStore = false)
        assertTrue("expected at least 4 bundled tracks, found ${pool.size}", pool.size >= 4)
        pool = pool.take(4)

        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        settingsRepo = VeloraSettingsRepository(
            context = context,
            scope = testScope,
            fileName = "orchestrator_char_${System.nanoTime()}"
        )
        engine = Media3PlayerEngine(context)
        orchestrator = MediaOrchestrator(
            repository = repository,
            settingsRepository = settingsRepo,
            playerEngine = engine,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        orchestrator.close()
        engine.release()
        database.close()
    }

    private fun queue() = orchestrator.orchestratorState.value.queue

    @Test
    fun `playTrackPool NORMAL starts at the first track and skip advances in order`() {
        orchestrator.playTrackPool(pool)
        assertEquals(pool.map { it.id }, queue().tracks.map { it.id })
        assertEquals(0, queue().currentIndex)
        assertEquals(pool[0].id, orchestrator.playbackState.value.currentTrack?.id)

        orchestrator.skipToNext()
        assertEquals(pool[1].id, orchestrator.playbackState.value.currentTrack?.id)
        orchestrator.skipToNext()
        assertEquals(pool[2].id, orchestrator.playbackState.value.currentTrack?.id)
        orchestrator.skipToNext()
        assertEquals(pool[3].id, orchestrator.playbackState.value.currentTrack?.id)

        // End of queue with repeat OFF stops playback and keeps the index.
        orchestrator.skipToNext()
        assertFalse(orchestrator.playbackState.value.isPlaying)
        assertEquals(3, queue().currentIndex)
    }

    @Test
    fun `playTrackPool SHUFFLE keeps the first track and yields a full permutation`() {
        orchestrator.playTrackPool(pool, shuffle = true)
        val q = queue()
        assertEquals(QueueMode.SHUFFLE, q.mode)
        assertEquals(pool.size, q.shuffledOrder.size)
        assertEquals(setOf(0, 1, 2, 3), q.shuffledOrder.toSet())
        // The shuffled order is anchored at the starting track.
        assertEquals(0, q.shuffledOrder.first())
        assertEquals(pool[0].id, q.currentTrack?.id)

        orchestrator.skipToNext()
        assertEquals(pool[q.shuffledOrder[1]].id, queue().currentTrack?.id)
    }

    @Test
    fun `play with an in-pool track jumps to its position without changing the queue`() {
        orchestrator.playTrackPool(pool)
        orchestrator.play(pool[2], pool)
        assertEquals(pool.map { it.id }, queue().tracks.map { it.id })
        assertEquals(2, queue().currentIndex)
        assertEquals(pool[2].id, orchestrator.playbackState.value.currentTrack?.id)
    }

    @Test
    fun `play with an out-of-pool track prepends it to the queue`() {
        val outsider = pool[3]
        val rest = pool.take(3)
        orchestrator.playTrackPool(rest)
        orchestrator.play(outsider, rest)
        assertEquals(
            listOf(outsider.id) + rest.map { it.id },
            queue().tracks.map { it.id }
        )
        assertEquals(0, queue().currentIndex)
    }

    @Test
    fun `addTrackToQueue appends and playNextInQueue inserts after the current track`() {
        orchestrator.playTrackPool(pool.take(3))
        val extra = pool[3]
        orchestrator.addTrackToQueue(extra)
        assertEquals(pool.map { it.id }, queue().tracks.map { it.id })
        assertEquals(0, queue().currentIndex)
        assertEquals("Added '${extra.title}' to queue", orchestrator.orchestratorState.value.lastBannerMessage)

        // A fresh outsider track goes right after the current one.
        val nextUp = pool[3].copy(id = "next_up", title = "Next Up")
        orchestrator.playNextInQueue(nextUp)
        assertEquals(nextUp.id, queue().tracks[1].id)
        assertEquals(0, queue().currentIndex)
    }

    @Test
    fun `addTrackToQueue ignores a duplicate id`() {
        orchestrator.playTrackPool(pool.take(3))
        orchestrator.addTrackToQueue(pool[1])
        assertEquals(pool.take(3).map { it.id }, queue().tracks.map { it.id })
    }

    @Test
    fun `removeTrackFromQueue keeps the current track when a later track is removed`() {
        orchestrator.playTrackPool(pool)
        orchestrator.removeTrackFromQueue(pool[1].id)
        assertEquals(
            listOf(pool[0].id, pool[2].id, pool[3].id),
            queue().tracks.map { it.id }
        )
        assertEquals(0, queue().currentIndex)
        assertEquals(pool[0].id, queue().currentTrack?.id)
    }

    @Test
    fun `removeTrackFromQueue of the current track advances to the next track`() {
        orchestrator.playTrackPool(pool)
        orchestrator.removeTrackFromQueue(pool[0].id)
        assertEquals(
            listOf(pool[1].id, pool[2].id, pool[3].id),
            queue().tracks.map { it.id }
        )
        assertEquals(0, queue().currentIndex)
        assertEquals(pool[1].id, queue().currentTrack?.id)
    }

    @Test
    fun `selectQueueTrack switches to an in-queue track and refuses out-of-pool tracks`() {
        orchestrator.playTrackPool(pool)
        orchestrator.selectQueueTrack(pool[2])
        assertEquals(2, queue().currentIndex)

        val outsider = pool[2].copy(id = "not_in_queue", title = "Not In Queue")
        orchestrator.selectQueueTrack(outsider)
        assertEquals(2, queue().currentIndex)
        assertEquals(
            "Track 'Not In Queue' is not in the active queue",
            orchestrator.orchestratorState.value.lastBannerMessage
        )
    }

    @Test
    fun `skipToPrevious at the start restarts the current track`() {
        orchestrator.playTrackPool(pool)
        orchestrator.skipToPrevious()
        assertEquals(0, queue().currentIndex)
        assertEquals(pool[0].id, queue().currentTrack?.id)
    }

    @Test
    fun `skipToPrevious from the second track moves back to the first`() {
        orchestrator.playTrackPool(pool)
        orchestrator.skipToNext()
        assertEquals(pool[1].id, queue().currentTrack?.id)
        orchestrator.skipToPrevious()
        assertEquals(pool[0].id, queue().currentTrack?.id)
        assertEquals(0, queue().currentIndex)
    }

    @Test
    fun `toggleShuffle toggles the mode and persists it to settings`() = runBlocking {
        orchestrator.playTrackPool(pool)
        orchestrator.toggleShuffle()
        assertEquals(QueueMode.SHUFFLE, queue().mode)
        withTimeout(5_000) {
            settingsRepo.settingsFlow.first { it.queueMode == QueueMode.SHUFFLE }
        }
        orchestrator.toggleShuffle()
        assertEquals(QueueMode.NORMAL, queue().mode)
        withTimeout(5_000) {
            settingsRepo.settingsFlow.first { it.queueMode == QueueMode.NORMAL }
        }
    }

    @Test
    fun `cycleRepeatMode cycles OFF ALL ONE OFF and persists each step`() = runBlocking {
        orchestrator.playTrackPool(pool)
        orchestrator.cycleRepeatMode()
        assertEquals(RepeatMode.ALL, queue().repeatMode)
        withTimeout(5_000) {
            settingsRepo.settingsFlow.first { it.repeatMode == RepeatMode.ALL }
        }
        orchestrator.cycleRepeatMode()
        assertEquals(RepeatMode.ONE, queue().repeatMode)
        orchestrator.cycleRepeatMode()
        assertEquals(RepeatMode.OFF, queue().repeatMode)
        withTimeout(5_000) {
            settingsRepo.settingsFlow.first { it.repeatMode == RepeatMode.OFF }
        }
    }

    @Test
    fun `togglePlayPause with no current track starts the fallback pool`() {
        orchestrator.togglePlayPause(pool)
        assertEquals(pool[0].id, orchestrator.playbackState.value.currentTrack?.id)
        assertEquals(0, queue().currentIndex)
    }

    @Test
    fun `stop halts playback and play records the track in history`() = runBlocking {
        orchestrator.play(pool[0], pool)
        orchestrator.stop()
        assertFalse(orchestrator.playbackState.value.isPlaying)

        // recordTrackPlayed is launched in the orchestrator scope, so wait for
        // the Room Flow to emit the entry instead of racing a single read.
        withTimeout(5_000) {
            repository.playbackHistory.first { history ->
                history.any { it.track.id == pool[0].id && it.playCount >= 1 }
            }
        }
    }
}
