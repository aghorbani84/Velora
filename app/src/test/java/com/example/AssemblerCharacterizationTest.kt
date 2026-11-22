package com.example

import com.example.domain.engine.MediaOrchestratorState
import com.example.domain.model.AppSettings
import com.example.domain.model.AudioRouteInfo
import com.example.domain.model.EqPreset
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.Playlist
import com.example.domain.model.RadioStation
import com.example.domain.model.ScanProgress
import com.example.domain.model.SmartRuleCriterion
import com.example.domain.model.StudioDspState
import com.example.domain.model.Track
import com.example.domain.model.TrackFilterMode
import com.example.domain.model.TrackSortOrder
import com.example.domain.model.VeloraPhase2Engine
import com.example.domain.model.VideoCinemaSettings
import com.example.domain.model.PlaybackQueue
import com.example.ui.viewmodel.assembleVeloraUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-4 — Characterization tests for `assembleVeloraUiState`
 * (CLEAN-CODE-PLAN.md; assembler at
 * ui/viewmodel/VeloraUiStateAssembler.kt:315-463).
 *
 * These tests lock the CURRENT projection behaviour for a fixed track list,
 * so the P1 state-slicing refactor (which replaces this function) can prove
 * it did not change what the UI sees. They extend — not replace — the
 * characterization test in ExampleRobolectricTest (sorting, filtering,
 * search, smart playlists for three tracks).
 *
 * The memo cache inside the assembler is a file-level global shared by every
 * test in the JVM: each test below builds FRESH list instances so no call can
 * accidentally return another test's cached projection.
 *
 * UNTESTED locally: Gradle does not run on this machine. CI on the PR is the
 * verification path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AssemblerCharacterizationTest {

    private fun fixedTracks() = listOf(
        Track(
            id = "t_alpha", mediaStoreId = 101L, title = "Alpha Nocturne",
            artist = "Zed Composer", album = "Midnight Suite",
            durationMs = 240_000L, uri = "/storage/emulated/0/Music/alpha.flac",
            folderPath = "/storage/emulated/0/Music", mediaType = MediaType.AUDIO,
            addedTimestampMs = 1_000L, sampleRateHz = 96_000, bitrateKbps = 1411,
            isFavorite = true
        ),
        Track(
            id = "t_beta", mediaStoreId = 102L, title = "Beta Horizon",
            artist = "Aria Ensemble", album = "Dawn",
            durationMs = 180_000L, uri = "/storage/emulated/0/Music/beta.mp3",
            folderPath = "/storage/emulated/0/Music", mediaType = MediaType.AUDIO,
            addedTimestampMs = 5_000L, sampleRateHz = 44_100, bitrateKbps = 320,
            isFavorite = false
        ),
        Track(
            id = "t_gamma", mediaStoreId = 103L, title = "Gamma Rays",
            artist = "Aria Ensemble", album = "Dawn",
            durationMs = 200_000L, uri = "/storage/emulated/0/Music/gamma.flac",
            folderPath = "/storage/emulated/0/Music", mediaType = MediaType.AUDIO,
            addedTimestampMs = 9_000L, sampleRateHz = 48_000, bitrateKbps = 320,
            isFavorite = false
        ),
        // 20-second clip: NOT a likely-valid song (duration < 35s).
        Track(
            id = "t_clip", mediaStoreId = 104L, title = "Clip Short",
            artist = "Zed Composer", album = "Sketches",
            durationMs = 20_000L, uri = "/storage/emulated/0/Music/clip.mp3",
            folderPath = "/storage/emulated/0/Music", mediaType = MediaType.AUDIO,
            addedTimestampMs = 7_000L, sampleRateHz = 44_100, bitrateKbps = 320,
            isFavorite = false
        ),
        Track(
            id = "t_video", mediaStoreId = 105L, title = "Cinema Reel",
            artist = "Director Cut", album = "Visuals",
            durationMs = 600_000L, uri = "/storage/emulated/0/Movies/reel.mp4",
            folderPath = "/storage/emulated/0/Movies", mediaType = MediaType.VIDEO,
            addedTimestampMs = 3_000L, sampleRateHz = 48_000, bitrateKbps = 4800,
            isFavorite = false
        )
    )

    private fun fixedHistory(tracks: List<Track>) = listOf(
        PlaybackHistoryEntry(
            track = tracks[0], lastPlayedAtMs = 1_700_000_000_000L,
            lastPositionMs = 60_000L, playCount = 5, completedCount = 3
        ),
        PlaybackHistoryEntry(
            track = tracks[1], lastPlayedAtMs = 1_700_000_100_000L,
            lastPositionMs = 30_000L, playCount = 2, completedCount = 0
        )
    )

    private fun assemble(
        tracks: List<Track>,
        settings: AppSettings = AppSettings(onlyValidMusicInSearch = true),
        query: String = "",
        dbResults: List<Track> = emptyList(),
        history: List<PlaybackHistoryEntry> = emptyList(),
        sortOrder: TrackSortOrder = TrackSortOrder.TITLE_ASC,
        filterMode: TrackFilterMode = TrackFilterMode.ALL,
        smartRules: List<SmartRuleCriterion> = emptyList()
    ) = assembleVeloraUiState(
        arrayOf(
            tracks,
            emptyList<Playlist>(),
            EqPreset.BUILT_IN_PRESETS,
            settings,
            ScanProgress(),
            MediaOrchestratorState(
                queue = PlaybackQueue(tracks = tracks.take(2), currentIndex = 0)
            ),
            query,
            dbResults,
            null,
            null,
            false,
            history,
            sortOrder,
            filterMode,
            emptyList<RadioStation>(),
            StudioDspState(),
            null,
            AudioRouteInfo(),
            smartRules,
            VideoCinemaSettings()
        )
    )

    @Test
    fun `DURATION_DESC orders longest first across audio and video`() {
        val state = assemble(fixedTracks(), sortOrder = TrackSortOrder.DURATION_DESC)
        assertEquals(
            listOf("t_video", "t_alpha", "t_gamma", "t_beta", "t_clip"),
            state.allTracks.map { it.id }
        )
    }

    @Test
    fun `RECENTLY_ADDED orders by added timestamp descending`() {
        val state = assemble(fixedTracks(), sortOrder = TrackSortOrder.RECENTLY_ADDED)
        assertEquals(
            listOf("t_gamma", "t_clip", "t_beta", "t_video", "t_alpha"),
            state.allTracks.map { it.id }
        )
    }

    @Test
    fun `HI_RES_ONLY keeps 48kHz and above`() {
        val state = assemble(fixedTracks(), filterMode = TrackFilterMode.HI_RES_ONLY)
        assertEquals(listOf("t_alpha", "t_video", "t_gamma"), state.allTracks.map { it.id })
        assertEquals(listOf("t_alpha", "t_gamma"), state.audioTracks.map { it.id })
        assertEquals(listOf("t_video"), state.videoTracks.map { it.id })
    }

    @Test
    fun `FAVORITES_ONLY keeps only favorites`() {
        val state = assemble(fixedTracks(), filterMode = TrackFilterMode.FAVORITES_ONLY)
        assertEquals(listOf("t_alpha"), state.allTracks.map { it.id })
        assertEquals(listOf("t_alpha"), state.favoriteTracks.map { it.id })
    }

    @Test
    fun `onlyValidMusic excludes the short clip from audio but not from allTracks`() {
        val withFilter = assemble(fixedTracks())
        assertFalse(withFilter.audioTracks.any { it.id == "t_clip" })
        assertTrue(withFilter.allTracks.any { it.id == "t_clip" })

        val withoutFilter = assemble(
            fixedTracks(),
            settings = AppSettings(onlyValidMusicInSearch = false)
        )
        assertEquals(
            listOf("t_alpha", "t_beta", "t_clip", "t_gamma"),
            withoutFilter.audioTracks.map { it.id }
        )
    }

    @Test
    fun `video tracks never leak into audioTracks and vice versa`() {
        val state = assemble(fixedTracks())
        assertTrue(state.audioTracks.none { it.mediaType == MediaType.VIDEO })
        assertEquals(listOf("t_video"), state.videoTracks.map { it.id })
    }

    @Test
    fun `artistsMap groups audio only and defaults selectedArtist to the first key`() {
        val state = assemble(fixedTracks())
        assertEquals(listOf("Aria Ensemble", "Zed Composer"), state.artistsMap.keys.toList())
        assertEquals(listOf("t_beta", "t_gamma"), state.artistsMap["Aria Ensemble"]?.map { it.id })
        assertEquals("Aria Ensemble", state.selectedArtist)
    }

    @Test
    fun `blank query returns the database results unchanged`() {
        val tracks = fixedTracks()
        val state = assemble(tracks, dbResults = listOf(tracks[1]))
        assertEquals(listOf("t_beta"), state.searchResults.map { it.id })
        assertTrue(state.omniSearchMatches.isEmpty())
    }

    @Test
    fun `title and artist queries rank omni matches and union the db results`() {
        val tracks = fixedTracks()
        val byTitle = assemble(tracks, query = "nocturne", dbResults = listOf(tracks[1]))
        assertTrue(byTitle.omniSearchMatches.any { it.track.id == "t_alpha" })
        assertTrue(byTitle.searchResults.any { it.id == "t_alpha" })
        assertTrue(byTitle.searchResults.any { it.id == "t_beta" })

        val byArtist = assemble(tracks, query = "aria")
        assertEquals(
            setOf("t_beta", "t_gamma"),
            byArtist.omniSearchMatches.map { it.track.id }.toSet()
        )
    }

    @Test
    fun `listening stats aggregate plays completions and top artists from history`() {
        val tracks = fixedTracks()
        val state = assemble(tracks, history = fixedHistory(tracks))
        val stats = state.listeningStats
        assertEquals(7, stats.totalPlaysCount)
        assertEquals(3, stats.totalCompletedCount)
        // (3 * 100) / 7 = 42 (integer division).
        assertEquals(42, stats.completionRatePercent)
        // alpha: 5 * 240000 / 2 + 60000 = 660000; beta: 2 * 180000 / 2 + 30000 = 210000.
        // Total 870000 ms -> 14 minutes.
        assertEquals(14L, stats.totalListeningMinutes)
        assertEquals("t_alpha", stats.topTracks.first().first.id)
        assertEquals(5, stats.topTracks.first().second)
        assertEquals("Zed Composer", stats.topArtists.first().first)
        assertEquals(5, stats.topArtists.first().second)
        assertEquals(5, stats.hiResTracksCount)
        assertEquals(1, stats.favoriteTracksCount)
    }

    @Test
    fun `smart rule evaluation matches bitrate sample-rate plays favorites and keyword`() {
        val tracks = fixedTracks()
        val rule = SmartRuleCriterion(
            id = "r1", name = "Hi-Res Favs",
            minSampleRateHz = 48_000, favoritesOnly = true
        )
        val matched = VeloraPhase2Engine.evaluateSmartRule(rule, tracks, fixedHistory(tracks))
        assertEquals(listOf("t_alpha"), matched.tracks.map { it.id })
        assertTrue(matched.explainReasonEn.contains("Hi-Res Favs"))

        val byArtist = SmartRuleCriterion(id = "r2", name = "Aria", artistKeyword = "aria")
        assertEquals(
            listOf("t_beta", "t_gamma"),
            VeloraPhase2Engine.evaluateSmartRule(byArtist, tracks, emptyList()).tracks.map { it.id }
        )

        val byPlays = SmartRuleCriterion(id = "r3", name = "Played", minPlayCount = 3)
        assertEquals(
            listOf("t_alpha"),
            VeloraPhase2Engine.evaluateSmartRule(byPlays, tracks, fixedHistory(tracks)).tracks.map { it.id }
        )
    }

    @Test
    fun `custom smart rules are prepended to the generated smart playlists`() {
        val tracks = fixedTracks()
        val rule = SmartRuleCriterion(
            id = "r1", name = "Hi-Res Favs",
            minSampleRateHz = 48_000, favoritesOnly = true
        )
        val state = assemble(tracks, history = fixedHistory(tracks), smartRules = listOf(rule))
        assertEquals(listOf("t_alpha"), state.smartPlaylists.first().tracks.map { it.id })
        // The generated favorites playlist still contains exactly the favorite.
        assertTrue(
            state.smartPlaylists.any { it.tracks.map { t -> t.id } == listOf("t_alpha") }
        )
    }

    @Test
    fun `same inputs twice produce equal projections`() {
        val tracks = fixedTracks()
        val first = assemble(tracks)
        val second = assemble(tracks)
        assertEquals(first.audioTracks.map { it.id }, second.audioTracks.map { it.id })
        assertEquals(first.allTracks.map { it.id }, second.allTracks.map { it.id })
        assertEquals(first.listeningStats, second.listeningStats)
    }
}
