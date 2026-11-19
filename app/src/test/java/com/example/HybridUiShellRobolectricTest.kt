package com.example

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.PlaybackStatus
import com.example.domain.model.AppSettings
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.Playlist
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.ui.components.DesktopPlayerBar
import com.example.ui.components.VeloraDesktopSidebar
import com.example.ui.navigation.VeloraDestination
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibrarySubTab
import com.example.ui.theme.VeloraTheme
import com.example.ui.viewmodel.PlaybackPositionState
import com.example.ui.viewmodel.VeloraUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class HybridUiShellRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `HomeScreen renders Made for You and Recently Played shelves when tracks exist`() {
        val sampleTrack = Track(
            id = "shelf_t1",
            mediaStoreId = 1L,
            title = "Golden Horizon",
            artist = "Velora Ensemble",
            album = "Obsidian Masters",
            durationMs = 210_000L,
            uri = "/storage/emulated/0/Music/golden.flac",
            folderPath = "/storage/emulated/0/Music",
            mediaType = MediaType.AUDIO,
            isFavorite = true
        )
        val uiState = VeloraUiState(
            allTracks = listOf(sampleTrack),
            audioTracks = listOf(sampleTrack),
            favoriteTracks = listOf(sampleTrack),
            settings = AppSettings(experienceTier = ExperienceTier.STANDARD)
        )
        var clickedSubTab: LibrarySubTab? = null

        composeRule.setContent {
            VeloraTheme {
                HomeScreen(
                    uiState = uiState,
                    onPlayTrack = { _, _ -> },
                    onPlayAll = {},
                    onPlaySmartPlaylist = { _, _ -> },
                    onResumeHistory = {},
                    onTuneRadio = {},
                    onOpenLibrarySubTab = { clickedSubTab = it },
                    onOpenNowPlaying = {},
                    onAutoScanMedia = {}
                )
            }
        }

        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("home_jump_artists_button").performClick()
        assertEquals(LibrarySubTab.ARTISTS, clickedSubTab)
        composeRule.onNodeWithTag("home_shelf_made_for_you").assertIsDisplayed()
        composeRule.onNodeWithTag("home_screen")
            .performScrollToNode(hasTestTag("home_shelf_recently_played"))
        composeRule.onNodeWithTag("home_shelf_recently_played").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp")
    fun `DesktopSidebar and DesktopPlayerBar render destinations, playlists, and transport controls`() {
        val sampleTrack = Track(
            id = "desk_t1",
            mediaStoreId = 2L,
            title = "Desktop Nocturne",
            artist = "Velora Studio",
            album = "Saffron Sessions",
            durationMs = 180_000L,
            uri = "/storage/emulated/0/Music/nocturne.flac",
            folderPath = "/storage/emulated/0/Music",
            mediaType = MediaType.AUDIO
        )
        val playlist = Playlist(
            id = 7L,
            name = "Late Night Coding",
            tracks = listOf(sampleTrack),
            createdAtMs = 1_000L
        )
        var collapsedToggled = false
        var playPauseToggled = false

        composeRule.setContent {
            VeloraTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        VeloraDesktopSidebar(
                            destinations = VeloraDestination.visibleForTier(ExperienceTier.STANDARD),
                            currentDestination = VeloraDestination.HOME,
                            playlists = listOf(playlist),
                            tier = ExperienceTier.STANDARD,
                            isPersian = false,
                            collapsed = false,
                            onToggleCollapse = { collapsedToggled = true },
                            onNavigate = {},
                            onPlayPlaylist = {}
                        )
                    }
                    DesktopPlayerBar(
                        engineState = EnginePlaybackState(
                            currentTrack = sampleTrack,
                            status = PlaybackStatus.PAUSED
                        ),
                        positionState = PlaybackPositionState(positionMs = 10_000L, durationMs = 180_000L, isPlaying = false),
                        queueMode = QueueMode.NORMAL,
                        repeatMode = RepeatMode.OFF,
                        tier = ExperienceTier.STANDARD,
                        onTogglePlayPause = { playPauseToggled = true },
                        onPrevious = {},
                        onNext = {},
                        onSeekTo = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                        onVolumeChange = {},
                        onToggleMute = {},
                        onOpenNowPlaying = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("desktop_sidebar").assertIsDisplayed()
        composeRule.onNodeWithTag("sidebar_playlist_7").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_player_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_sidebar_collapse_toggle").performClick()
        composeRule.onNodeWithTag("desktop_player_play_pause").performClick()
        assertTrue(collapsedToggled)
        assertTrue(playPauseToggled)
    }
}
