package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VeloraDatabase
import com.example.data.local.TrackEntity
import com.example.data.playback.Media3PlayerEngine
import com.example.data.repository.VeloraLibraryRepository
import com.example.data.scanner.BundledMediaSynthesizer
import com.example.data.scanner.MediaLibraryScanner
import com.example.data.settings.VeloraSettingsRepository
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
    private lateinit var database: VeloraDatabase
    private lateinit var repository: VeloraLibraryRepository

    @Before
    fun setUp() {
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
    fun `MainActivity launches and renders VeloraRootApp without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
        controller.pause().stop().destroy()
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
        val settingsRepo = VeloraSettingsRepository(
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
        val backup = VeloraSettingsRepository.backupCorruptFileIfPresent(corruptFile)
        assertNotNull(backup)
        assertTrue(backup!!.exists())

        val recoveredRepo = VeloraSettingsRepository(
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
        val settingsRepo = VeloraSettingsRepository(
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

    @Test
    fun `assembleVeloraUiState characterization test - sorting, filtering, search, and smart playlists`() {
        val fixedTracks = listOf(
            Track(
                id = "t_alpha",
                mediaStoreId = 101L,
                title = "Alpha Nocturne",
                artist = "Zed Composer",
                album = "Midnight Suite",
                durationMs = 240_000L,
                uri = "/storage/emulated/0/Music/alpha.flac",
                folderPath = "/storage/emulated/0/Music",
                mediaType = MediaType.AUDIO,
                addedTimestampMs = 1_000L,
                sampleRateHz = 96_000,
                bitrateKbps = 1411,
                isFavorite = true
            ),
            Track(
                id = "t_beta",
                mediaStoreId = 102L,
                title = "Beta Horizon",
                artist = "Aria Ensemble",
                album = "Dawn",
                durationMs = 180_000L,
                uri = "/storage/emulated/0/Music/beta.mp3",
                folderPath = "/storage/emulated/0/Music",
                mediaType = MediaType.AUDIO,
                addedTimestampMs = 5_000L,
                sampleRateHz = 44_100,
                bitrateKbps = 320,
                isFavorite = false
            ),
            Track(
                id = "t_video",
                mediaStoreId = 103L,
                title = "Cinema Reel",
                artist = "Director Cut",
                album = "Visuals",
                durationMs = 600_000L,
                uri = "/storage/emulated/0/Movies/reel.mp4",
                folderPath = "/storage/emulated/0/Movies",
                mediaType = MediaType.VIDEO,
                addedTimestampMs = 3_000L,
                sampleRateHz = 48_000,
                bitrateKbps = 4800,
                isFavorite = false
            )
        )

        val settings = com.example.domain.model.AppSettings(onlyValidMusicInSearch = true)
        val orchestratorState = com.example.domain.engine.MediaOrchestratorState(
            queue = com.example.domain.model.PlaybackQueue(
                tracks = listOf(fixedTracks[0], fixedTracks[1]),
                currentIndex = 0
            )
        )

        // 1. Default TITLE_ASC & ALL filter
        val stateTitleAsc = com.example.ui.viewmodel.assembleVeloraUiState(
            arrayOf(
                fixedTracks,
                emptyList<com.example.domain.model.Playlist>(),
                com.example.domain.model.EqPreset.BUILT_IN_PRESETS,
                settings,
                com.example.domain.model.ScanProgress(),
                orchestratorState,
                "",
                emptyList<Track>(),
                null,
                null,
                false,
                emptyList<com.example.domain.model.PlaybackHistoryEntry>(),
                com.example.domain.model.TrackSortOrder.TITLE_ASC,
                com.example.domain.model.TrackFilterMode.ALL,
                emptyList<com.example.domain.model.RadioStation>(),
                com.example.domain.model.StudioDspState(),
                null,
                com.example.domain.model.AudioRouteInfo(),
                emptyList<com.example.domain.model.SmartRuleCriterion>(),
                com.example.domain.model.VideoCinemaSettings()
            )
        )
        assertEquals(listOf("t_alpha", "t_beta"), stateTitleAsc.audioTracks.map { it.id })
        assertEquals(listOf("t_video"), stateTitleAsc.videoTracks.map { it.id })
        assertTrue(stateTitleAsc.smartPlaylists.isNotEmpty())
        assertTrue(stateTitleAsc.omniSearchMatches.isEmpty())

        // 2. ARTIST_ASC sort + FAVORITES_ONLY filter + search query
        val stateFavFiltered = com.example.ui.viewmodel.assembleVeloraUiState(
            arrayOf(
                fixedTracks,
                emptyList<com.example.domain.model.Playlist>(),
                com.example.domain.model.EqPreset.BUILT_IN_PRESETS,
                settings,
                com.example.domain.model.ScanProgress(),
                orchestratorState,
                "Nocturne",
                listOf(fixedTracks[0]),
                null,
                null,
                false,
                emptyList<com.example.domain.model.PlaybackHistoryEntry>(),
                com.example.domain.model.TrackSortOrder.ARTIST_ASC,
                com.example.domain.model.TrackFilterMode.FAVORITES_ONLY,
                emptyList<com.example.domain.model.RadioStation>(),
                com.example.domain.model.StudioDspState(),
                null,
                com.example.domain.model.AudioRouteInfo(),
                emptyList<com.example.domain.model.SmartRuleCriterion>(),
                com.example.domain.model.VideoCinemaSettings()
            )
        )
        assertEquals(listOf("t_alpha"), stateFavFiltered.searchResults.map { it.id })
        assertTrue(stateFavFiltered.omniSearchMatches.any { it.track.id == "t_alpha" })
    }

    @Test
    fun `Playlist position increments monotonically after middle track removal and Room migrations are registered`() = runBlocking {
        assertEquals(3, VeloraDatabase.ALL_MIGRATIONS.size)

        val tracks = repository.performScan(emptySet(), includeMediaStore = false)
        assertTrue(tracks.size >= 4)
        val playlistId = repository.createPlaylist("Monotonic Queue Test", listOf(tracks[0].id, tracks[1].id, tracks[2].id))
        repository.removeTrackFromPlaylist(playlistId, tracks[1].id)

        // Adding a fourth track after removing the middle element must not collide with the last track's position
        val fourthTrack = tracks[3]
        repository.addTrackToPlaylist(playlistId, fourthTrack.id)

        val updatedPlaylist = repository.playlists.first().first { it.id == playlistId }
        assertEquals(listOf(tracks[0].id, tracks[2].id, fourthTrack.id), updatedPlaylist.tracks.map { it.id })
    }

    @Test
    fun `Media3PlayerEngine rejects unsafe network URLs and MediaOrchestrator prepends out-of-pool tracks`() = runBlocking {
        val engine = Media3PlayerEngine(context)
        val unsafeTrack = Track(
            id = "ssrf_probe",
            mediaStoreId = -1L,
            title = "Loopback Stream",
            artist = "Probe",
            album = "Net",
            durationMs = 60_000L,
            uri = "https://127.0.0.1/stream.m3u8",
            folderPath = "Network",
            mediaType = MediaType.AUDIO
        )
        engine.loadAndPlay(unsafeTrack, 0L)
        assertEquals(PlaybackStatus.ERROR, engine.playbackState.value.status)

        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val settingsRepo = VeloraSettingsRepository(
            context = context,
            scope = testScope,
            fileName = "orchestrator_pool_${System.currentTimeMillis()}"
        )
        val orchestrator = com.example.domain.engine.MediaOrchestrator(
            repository = repository,
            settingsRepository = settingsRepo,
            playerEngine = engine,
            dispatcher = Dispatchers.Unconfined
        )
        val tracks = repository.performScan(emptySet(), includeMediaStore = false)
        val externalTrack = Track(
            id = "external_single",
            mediaStoreId = 777L,
            title = "External Track",
            artist = "Guest",
            album = "Single",
            durationMs = 120_000L,
            uri = "/storage/emulated/0/Music/external.mp3",
            folderPath = "/storage/emulated/0/Music",
            mediaType = MediaType.AUDIO
        )
        orchestrator.play(externalTrack, listOf(tracks[0], tracks[1]), 0L)
        assertEquals(externalTrack.id, orchestrator.orchestratorState.value.queue.currentTrack?.id)
        assertTrue(orchestrator.orchestratorState.value.queue.tracks.any { it.id == externalTrack.id })

        orchestrator.close()
        engine.release()
    }

    @Test
    fun `VeloraPlaybackService lifecycle creates and tears down cleanly under Robolectric`() {
        val controller = org.robolectric.Robolectric.buildService(
            com.example.data.playback.VeloraPlaybackService::class.java
        )
        val service = controller.create().get()
        assertNotNull(service)
        assertEquals("velora_media_session", com.example.data.playback.VeloraPlaybackService.SESSION_ID)
        controller.destroy()
    }
}
