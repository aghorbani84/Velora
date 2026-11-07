package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FazoraDatabase
import com.example.data.local.TrackEntity
import com.example.data.playback.Media3PlayerEngine
import com.example.data.repository.FazoraLibraryRepository
import com.example.data.scanner.BundledMediaSynthesizer
import com.example.data.scanner.MediaLibraryScanner
import com.example.data.settings.FazoraSettingsRepository
import com.example.domain.engine.PlaybackStatus
import com.example.domain.model.MediaType
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.orchestrator.addTimeMachineBookmark
import com.example.orchestrator.adjustLyricsOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: FazoraDatabase
    private lateinit var repository: FazoraLibraryRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, FazoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scanner = MediaLibraryScanner(context)
        repository = FazoraLibraryRepository(
            context = context,
            trackDao = database.trackDao(),
            playlistDao = database.playlistDao(),
            eqPresetDao = database.eqPresetDao(),
            scanner = scanner,
            playbackHistoryDao = database.playbackHistoryDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context matches VELORA`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("VELORA", appName)
    }

    @Test
    fun `Room repository indexes bundled WAV tracks, deduplicates by mediaStoreId, preserves favorites, and searches`() = runBlocking {
        val tracks = repository.performScan(emptySet(), includeMediaStore = false)
        assertTrue("Expected at least 5 studio master tracks, found ${tracks.size}", tracks.size >= 5)

        // Toggle favorite on first track and verify it persists across rescans
        val favTrackId = tracks.first().id
        assertTrue(repository.toggleFavorite(favTrackId))
        assertEquals(1, repository.favoriteTracks.first().size)

        // Re-running scan must deduplicate by mediaStoreId & URI and preserve favorite status
        val rescanned = repository.performScan(emptySet(), includeMediaStore = false)
        val storedCount = database.trackDao().getTrackCount()
        assertEquals(rescanned.size, storedCount)
        assertTrue(rescanned.first { it.id == favTrackId }.isFavorite)

        // Search query over title/artist/album
        val obsidianMatches = repository.searchTracks("Obsidian").first()
        assertEquals(1, obsidianMatches.size)
        assertTrue(obsidianMatches.first().title.contains("Obsidian"))
    }

    @Test
    fun `playlists and custom 10-band EQ presets persist in Room`() = runBlocking {
        val tracks = repository.performScan(emptySet(), includeMediaStore = false)
        val firstTrack = tracks.first()

        // Create playlist and add track
        val playlistId = repository.createPlaylist("Late Night Masters", listOf(firstTrack.id))
        assertTrue(playlistId > 0L)
        val playlists = repository.playlists.first()
        assertEquals(1, playlists.size)
        assertEquals("Late Night Masters", playlists.first().name)
        assertEquals(1, playlists.first().tracks.size)
        assertEquals(firstTrack.id, playlists.first().tracks.first().id)

        // Save custom 10-band EQ preset (verifies save_preset saves an EqPreset with its name, not an empty Playlist)
        val customGains = listOf(4.5f, 3.0f, 1.5f, 0f, -1.5f, 0f, 2.0f, 3.5f, 5.0f, 6.0f)
        val savedPreset = repository.saveCustomEqPreset("Warm Analog Tape", customGains)
        assertEquals("Warm Analog Tape", savedPreset.name)
        assertEquals(customGains, savedPreset.gainsDb)
        assertFalse(savedPreset.isBuiltIn)

        val allPresets = repository.allEqPresets.first()
        assertTrue(allPresets.any { it.name == "Warm Analog Tape" && it.gainsDb == customGains })
    }

    @Test
    fun `Room repository caches audio metadata and persists playback history for offline access`() = runBlocking {
        val customOfflineTrack = Track(
            id = "offline_cached_1",
            mediaStoreId = 777_001L,
            title = "Offline Master Reference",
            artist = "Fazora Acoustic Lab",
            album = "Offline Studio Vault",
            durationMs = 240_000L,
            uri = "/storage/emulated/0/Music/offline_master_ref.wav",
            folderPath = "/storage/emulated/0/Music",
            mediaType = MediaType.AUDIO,
            sampleRateHz = 96_000,
            bitrateKbps = 4608
        )

        // Record playback twice and update resume position + completion
        repository.recordTrackPlayed(customOfflineTrack, startPositionMs = 12_500L, playedAtMs = 1_000L)
        repository.recordTrackPlayed(customOfflineTrack, startPositionMs = 45_000L, playedAtMs = 2_000L)
        repository.updateTrackHistoryPosition(customOfflineTrack.id, positionMs = 98_400L, completed = true)

        val history = repository.playbackHistory.first()
        assertEquals(1, history.size)
        val entry = history.first()
        assertEquals("Offline Master Reference", entry.track.title)
        assertEquals(96_000, entry.track.sampleRateHz)
        assertEquals(4608, entry.track.bitrateKbps)
        assertEquals(2, entry.playCount)
        assertEquals(1, entry.completedCount)
        assertEquals(98_400L, entry.lastPositionMs)

        // Clear history while keeping cached track metadata in Room
        repository.clearPlaybackHistory()
        assertTrue(repository.playbackHistory.first().isEmpty())
        assertNotNull(database.trackDao().getTrackById(customOfflineTrack.id))
    }

    @Test
    fun `DataStore settings persist atomically and corrupt file creates backup and falls back to defaults`() = runBlocking {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val settingsRepo = FazoraSettingsRepository(
            context = context,
            scope = testScope,
            fileName = "test_fazora_settings_${System.currentTimeMillis()}"
        )

        settingsRepo.updateQueueModes(QueueMode.SHUFFLE, RepeatMode.ONE)
        settingsRepo.updatePlaybackSpeed(1.5f)
        val loaded = settingsRepo.getSettingsOnce()
        assertEquals(QueueMode.SHUFFLE, loaded.queueMode)
        assertEquals(RepeatMode.ONE, loaded.repeatMode)
        assertEquals(1.5f, loaded.defaultPlaybackSpeed, 0.01f)

        // Verify corrupt file backup helper creates .corrupt.bak file and never swallows silently
        val corruptName = "corrupt_fazora_${System.currentTimeMillis()}"
        val corruptFile = File(context.filesDir, "datastore/$corruptName.preferences_pb").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(0xFF.toByte(), 0x00, 0x7F, 0x42, 0x13))
        }
        val backup = FazoraSettingsRepository.backupCorruptFileIfPresent(corruptFile)
        assertNotNull(backup)
        assertTrue(backup!!.exists())

        val recoveredRepo = FazoraSettingsRepository(
            context = context,
            scope = testScope,
            fileName = corruptName
        )
        val recoveredSettings = recoveredRepo.getSettingsOnce()
        assertEquals(1, recoveredSettings.corruptionFallbackCount)
        assertEquals(QueueMode.NORMAL, recoveredSettings.queueMode)
    }

    @Test
    fun `Media3PlayerEngine surfaces ERROR state when loading a missing file instead of staying playing`() {
        val engine = Media3PlayerEngine(context)
        val missingTrack = Track(
            id = "missing_1",
            mediaStoreId = 999_999L,
            title = "Nonexistent File",
            artist = "Ghost",
            album = "None",
            durationMs = 60_000L,
            uri = "/nonexistent/path/missing_audio.wav",
            folderPath = "/storage/emulated/0/Music",
            mediaType = MediaType.AUDIO
        )

        engine.loadAndPlay(missingTrack, 0L)
        val state = engine.playbackState.value
        assertEquals(PlaybackStatus.ERROR, state.status)
        assertNotNull(state.errorMessage)
        assertFalse(state.isPlaying)
        engine.release()
    }

    @Test
    fun `performance benchmark - indexing 100 tracks into Room completes within 500ms budget`() = runBlocking {
        val batch100 = (1..100).map { i ->
            TrackEntity(
                id = "bench_track_$i",
                mediaStoreId = 50_000L + i,
                title = "Benchmark Composition $i",
                artist = "Fazora Ensemble ${i % 10}",
                album = "Benchmark Suite ${i % 5}",
                durationMs = 180_000L,
                uri = "/storage/emulated/0/Music/bench_$i.wav",
                folderPath = "/storage/emulated/0/Music",
                mediaType = MediaType.AUDIO.name,
                artworkUri = null,
                addedTimestampMs = System.currentTimeMillis(),
                sampleRateHz = 44100,
                bitrateKbps = 1411
            )
        }

        val startMs = System.currentTimeMillis()
        database.trackDao().upsertTracks(batch100)
        val queried = database.trackDao().getAllTracksOnce()
        val elapsedMs = System.currentTimeMillis() - startMs

        assertEquals(100, queried.size)
        assertTrue("Expected 100-track Room index + query < 500ms budget, measured ${elapsedMs}ms", elapsedMs < 500L)
    }

    @Test
    fun `IMediaOrchestrator coordinates play, pause, skipToNext, stop, seekTo, and observable PlaybackState flow`() = runBlocking {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val settingsRepo = FazoraSettingsRepository(
            context = context,
            scope = testScope,
            fileName = "orchestrator_settings_${System.currentTimeMillis()}"
        )
        val engine = Media3PlayerEngine(context)
        val orchestrator: com.example.orchestrator.IMediaOrchestrator = com.example.domain.engine.MediaOrchestrator(
            repository = repository,
            settingsRepository = settingsRepo,
            playerEngine = engine,
            dispatcher = Dispatchers.Unconfined
        )

        orchestrator.bootstrapAndRestoreSession()
        val tracks = repository.getAllTracksOnce()
        assertTrue(tracks.size >= 2)

        val firstTrack = tracks[0]
        val secondTrack = tracks[1]

        // 1. play(mediaItem) and verify observable PlaybackState flow
        orchestrator.play(firstTrack, tracks, startPositionMs = 0L)
        val playbackStateAfterPlay = orchestrator.playbackState.value
        assertEquals(firstTrack.id, playbackStateAfterPlay.currentTrack?.id)
        assertNotNull(playbackStateAfterPlay.activeFormat)

        // 2. seekTo(position)
        orchestrator.seekTo(15_000L)

        // 3. pause()
        orchestrator.pause()
        assertFalse(orchestrator.playbackState.value.isPlaying)

        // 4. skipToNext()
        orchestrator.skipToNext()
        assertEquals(secondTrack.id, orchestrator.playbackState.value.currentTrack?.id)

        // 5. stop()
        orchestrator.stop()
        assertFalse(orchestrator.playbackState.value.isPlaying)

        // Verify Room playback history was recorded by IMediaOrchestrator
        val history = repository.playbackHistory.first()
        assertTrue(history.any { it.track.id == firstTrack.id && it.playCount >= 1 })

        // Verify DataStore queue session was persisted by IMediaOrchestrator
        val savedSettings = settingsRepo.getSettingsOnce()
        assertEquals(secondTrack.id, savedSettings.lastActiveTrackId)
        assertTrue(savedSettings.lastQueueTrackIds.contains(firstTrack.id))

        // Verify Time Machine bookmark and lyrics offset update structured StateFlow
        orchestrator.adjustLyricsOffset(500L)
        orchestrator.addTimeMachineBookmark("Golden Ratio Peak")
        val updatedState = orchestrator.orchestratorState.value
        assertEquals(500L, updatedState.lyricsOffsetMs)
        assertEquals(1, updatedState.timeMachineBookmarks.size)
        assertEquals("Golden Ratio Peak", updatedState.timeMachineBookmarks.first().label)

        orchestrator.close()
        engine.release()
    }
}
