package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.PlaybackStatus
import com.example.domain.model.AppSettings
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.ThemePaletteMode
import com.example.domain.model.Track
import com.example.ui.components.MiniPlayerBar
import com.example.ui.navigation.VeloraDestination
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.LibrarySubTab
import com.example.ui.screens.PlayerAndEqScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.groupTracksByFolder
import com.example.ui.theme.VeloraTheme
import com.example.ui.viewmodel.PlaybackPositionState
import com.example.ui.viewmodel.VeloraUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class Step1AuditVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleTrack(
        id: String,
        title: String,
        artist: String = "Velora Artist",
        folderPath: String = "/storage/emulated/0/Music/Jazz",
        mediaType: MediaType = MediaType.AUDIO
    ) = Track(
        id = id,
        mediaStoreId = id.hashCode().toLong(),
        title = title,
        artist = artist,
        album = "Studio Master",
        durationMs = 180_000L,
        uri = "$folderPath/$id.flac",
        folderPath = folderPath,
        mediaType = mediaType
    )

    @Test
    fun `SYSTEM_DYNAMIC is retired, VELORA_SAFFRON is default, and FOLDERS is 5th LibrarySubTab`() {
        assertFalse(ThemePaletteMode.entries.any { it.name == "SYSTEM_DYNAMIC" })
        assertEquals(ThemePaletteMode.VELORA_SAFFRON, AppSettings().themePaletteMode)
        assertEquals(5, LibrarySubTab.entries.size)
        assertEquals(LibrarySubTab.FOLDERS, LibrarySubTab.entries.last())
        assertEquals(4, VeloraDestination.visibleForTier(ExperienceTier.SIMPLE).size)
        assertEquals(5, VeloraDestination.visibleForTier(ExperienceTier.STANDARD).size)
        assertEquals(5, VeloraDestination.visibleForTier(ExperienceTier.PRO).size)
    }

    @Test
    fun `PlaybackQueue moveTrack reorders tracks while tracking currentIndex`() {
        val t1 = sampleTrack("t1", "Alpha")
        val t2 = sampleTrack("t2", "Beta")
        val t3 = sampleTrack("t3", "Gamma")
        val queue = PlaybackQueue().setQueue(listOf(t1, t2, t3), startIndex = 1)

        val movedUp = queue.moveTrack(fromIndex = 1, toIndex = 0)
        assertEquals(listOf("t2", "t1", "t3"), movedUp.tracks.map { it.id })
        assertEquals(0, movedUp.currentIndex)
        assertEquals("t2", movedUp.currentTrack?.id)

        val movedDown = movedUp.moveTrack(fromIndex = 0, toIndex = 2)
        assertEquals(listOf("t1", "t3", "t2"), movedDown.tracks.map { it.id })
        assertEquals(2, movedDown.currentIndex)
    }

    @Test
    fun `MiniPlayerBar exposes Previous button wired to onSkipPrevious`() {
        val track = sampleTrack("mp_1", "Obsidian Echo")
        var previousClicked = false

        composeRule.setContent {
            VeloraTheme {
                MiniPlayerBar(
                    engineState = EnginePlaybackState(
                        currentTrack = track,
                        status = PlaybackStatus.PLAYING
                    ),
                    positionState = PlaybackPositionState(
                        positionMs = 12_000L,
                        durationMs = 180_000L,
                        isPlaying = true
                    ),
                    onTogglePlayPause = {},
                    onSkipPrevious = { previousClicked = true },
                    onSkipNext = {},
                    onOpenPlayer = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mini_player_prev").assertIsDisplayed().performClick()
        assertTrue(previousClicked)
    }

    @Test
    fun `FoldersScreen groups tracks by directory and plays selected folder`() {
        val jazzTrack = sampleTrack("f1", "Midnight Sax", folderPath = "/storage/emulated/0/Music/Jazz")
        val rockTrack = sampleTrack("f2", "Obsidian Riff", folderPath = "/storage/emulated/0/Music/Rock")
        val groups = groupTracksByFolder(listOf(jazzTrack, rockTrack), isPersian = false)
        assertEquals(listOf("Jazz", "Rock"), groups.map { it.displayName })

        var playedFolderSize = 0
        composeRule.setContent {
            VeloraTheme {
                FoldersScreen(
                    uiState = VeloraUiState(
                        allTracks = listOf(jazzTrack, rockTrack),
                        settings = AppSettings(experienceTier = ExperienceTier.STANDARD)
                    ),
                    onPlayTrack = { _, _ -> },
                    onPlayFolder = { tracks, _ -> playedFolderSize = tracks.size },
                    onAddTrackToQueue = {},
                    onAddTrackToPlaylist = { _, _ -> },
                    onToggleFavorite = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("folders_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("folder_play_button").performClick()
        assertEquals(1, playedFolderSize)
    }

    @Test
    @Config(qualifiers = "w412dp-h1200dp")
    fun `PRO VideoPlayerScreen exposes 0_75x speed chip`() {
        val t1 = sampleTrack("v1", "Cinema Demo", mediaType = MediaType.VIDEO)
        var selectedVideoSpeed = 0f

        composeRule.setContent {
            VeloraTheme {
                VideoPlayerScreen(
                    uiState = VeloraUiState(
                        allTracks = listOf(t1),
                        videoTracks = listOf(t1),
                        engineState = EnginePlaybackState(currentTrack = t1),
                        settings = AppSettings(
                            isSimpleMode = false,
                            experienceTier = ExperienceTier.PRO
                        )
                    ),
                    playerEngine = null,
                    onPlayVideoTrack = {},
                    onTogglePlayPause = {},
                    onSeekTo = {},
                    onSetPlaybackSpeed = { selectedVideoSpeed = it },
                    onSetAspectMode = {},
                    onSetSubtitlesEnabled = {},
                    onSelectAudioTrack = {},
                    onAddTrackToQueue = {},
                    onAddTrackToPlaylist = { _, _ -> },
                    onToggleFavorite = {},
                    onEnterPip = {},
                    onAutoScanMedia = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_speed_chip_0.75")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(0.75f, selectedVideoSpeed, 0.001f)
    }

    @Test
    @Config(qualifiers = "w412dp-h1400dp")
    fun `PlayerAndEqScreen PRO exposes dsp_speed_0_75 and queue move buttons`() {
        val t1 = sampleTrack("q1", "First Track")
        val t2 = sampleTrack("q2", "Second Track")
        var chosenAudioSpeed = 0f
        var movedPair: Pair<Int, Int>? = null

        composeRule.setContent {
            VeloraTheme {
                PlayerAndEqScreen(
                    uiState = VeloraUiState(
                        allTracks = listOf(t1, t2),
                        queue = PlaybackQueue().setQueue(listOf(t1, t2), startIndex = 0),
                        engineState = EnginePlaybackState(currentTrack = t1),
                        settings = AppSettings(
                            isSimpleMode = false,
                            experienceTier = ExperienceTier.PRO
                        )
                    ),
                    onTogglePlayPause = {},
                    onStop = {},
                    onPrevious = {},
                    onNext = {},
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onVolumeChange = {},
                    onToggleMute = {},
                    onSetEqEnabled = {},
                    onSetEqBandGain = { _, _ -> },
                    onApplyPreset = {},
                    onSaveCustomPreset = {},
                    onDeleteCustomPreset = {},
                    onSelectQueueTrack = {},
                    onRemoveFromQueue = {},
                    onClearError = {},
                    onMoveQueueItem = { from, to -> movedPair = from to to },
                    onSetPlaybackSpeed = { chosenAudioSpeed = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("player_and_eq_screen")
            .performScrollToNode(hasTestTag("studio_dsp_rack_card"))
        composeRule.onNodeWithTag("dsp_speed_0.75").performScrollTo().performClick()
        assertEquals(0.75f, chosenAudioSpeed, 0.001f)

        composeRule.onNodeWithTag("player_and_eq_screen")
            .performScrollToNode(hasTestTag("queue_list_section"))
        composeRule.onNodeWithTag("move_queue_down_0").performScrollTo().performClick()
        assertEquals(0 to 1, movedPair)
    }

    @Test
    fun `SettingsScreen shows backup_json_input before export and SearchScreen categorizes results`() {
        val audio = sampleTrack("s1", "Velora Anthem", artist = "Velora Band", mediaType = MediaType.AUDIO)
        val video = sampleTrack("s2", "Velora Live Clip", artist = "Velora Band", mediaType = MediaType.VIDEO)
        val playlist = Playlist(id = 42L, name = "Velora Mix", createdAtMs = 1L, tracks = listOf(audio))

        composeRule.setContent {
            VeloraTheme {
                SettingsScreen(
                    uiState = VeloraUiState(settings = AppSettings(experienceTier = ExperienceTier.STANDARD)),
                    onSetAutoHideMs = {},
                    onSignInWithGoogle = {},
                    onSignOut = {},
                    onSetLanguageCode = {},
                    onTogglePlugin = {},
                    onExportBackupJson = { "{}" },
                    onRestoreBackupJson = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("firebase_sync_card"))
        composeRule.onNodeWithTag("backup_json_input").assertIsDisplayed()
    }

    @Test
    fun `SearchScreen renders categorized sections for artists, playlists, audio, and video`() {
        val audio = sampleTrack("s1", "Velora Anthem", artist = "Velora Band", mediaType = MediaType.AUDIO)
        val video = sampleTrack("s2", "Velora Live Clip", artist = "Velora Band", mediaType = MediaType.VIDEO)
        val playlist = Playlist(id = 42L, name = "Velora Mix", createdAtMs = 1L, tracks = listOf(audio))

        composeRule.setContent {
            VeloraTheme {
                SearchScreen(
                    uiState = VeloraUiState(
                        searchQuery = "Velora",
                        searchResults = listOf(audio, video),
                        artistsMap = mapOf("Velora Band" to listOf(audio)),
                        playlists = listOf(playlist),
                        settings = AppSettings(experienceTier = ExperienceTier.STANDARD)
                    ),
                    onQueryChange = {},
                    onPlayTrack = {},
                    onAddTrackToQueue = {},
                    onAddTrackToPlaylist = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("search_screen")
            .performScrollToNode(hasTestTag("search_section_artists"))
        composeRule.onNodeWithTag("search_section_artists").assertIsDisplayed()
        composeRule.onNodeWithTag("search_screen")
            .performScrollToNode(hasTestTag("search_section_playlists"))
        composeRule.onNodeWithTag("search_section_playlists").assertIsDisplayed()
        composeRule.onNodeWithTag("search_screen")
            .performScrollToNode(hasTestTag("search_section_audio"))
        composeRule.onNodeWithTag("search_section_audio").assertIsDisplayed()
        composeRule.onNodeWithTag("search_screen")
            .performScrollToNode(hasTestTag("search_section_video"))
        composeRule.onNodeWithTag("search_section_video").assertIsDisplayed()
    }
}
