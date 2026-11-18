package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.example.domain.model.AppSettings
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.Track
import com.example.ui.screens.ArtistScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.PlayerAndEqScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.viewmodel.VeloraUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P0-6 — `SimpleModeCompositionTest` (DESIGN-SYSTEM.md §5.1, CLEAN-CODE-PLAN.md).
 *
 * The proof is STRUCTURAL, not visual: in SIMPLE, no node carrying a pro
 * test tag may exist in the composition tree at all — hiding a control with
 * `alpha(0)` or `visibility = Gone` fails this test, because the composable
 * must not be emitted. `if (tier != SIMPLE)` gating at the call site is the
 * only accepted mechanism (VeloraTransportControls already does this).
 *
 * What this test renders: each of the 8 destinations is rendered DIRECTLY
 * (not through NavGraph/ViewModel) with a fabricated [VeloraUiState] and
 * no-op callbacks, so the test exercises the real screens' tier gating
 * without a device, a database or an engine. The video screen is rendered
 * with `playerEngine = null` — [com.example.ui.components.VeloraVideoSurface]
 * returns early on a null engine, so no PlayerView is ever created under
 * Robolectric; the tier assertions do not depend on the surface.
 *
 * Pro-tag list (DESIGN-SYSTEM.md §5.1): `transport_shuffle_button`,
 * `transport_repeat_button`, `eq_*`, `speed_*`, `subtitle_*`,
 * `audio_track_*`, `ab_loop_*`, `pip_button`, `video_auto_hide_slider`,
 * plus `studio_dsp_rack_card` (the studio DSP rack, a PRO-only surface not
 * listed in §5.1). HONEST LIMIT: most of the `*_` tags do not exist in the
 * code yet — the rebuilt screens of Phase 2 must ADD them, and this test
 * will then start guarding them. Today those assertions pass vacuously;
 * the tags that DO exist (`transport_shuffle_button`,
 * `transport_repeat_button`, `video_auto_hide_slider`,
 * `studio_dsp_rack_card`, `subtitle_*` inside the video dialogs) are the
 * ones actually guarded, and the STANDARD control test below proves the
 * assertions can detect the tags when they are present.
 *
 * `tier_option_PRO` is deliberately NOT in the pro list: it is the door out
 * of SIMPLE, not a pro control (§5.1).
 *
 * The allow-list (SIMPLE must still show the essentials) is asserted only
 * on the PLAYER destination: the other destinations have no transport
 * controls by design, and the video screen's inline controls carry no
 * `transport_*` tags.
 *
 * UNTESTED locally: Gradle does not run on this machine. CI on the PR is the
 * verification path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SimpleModeCompositionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val demoTracks = listOf(
        Track(
            id = "sm_audio_1", mediaStoreId = 201L, title = "Simple Song",
            artist = "Simple Artist", album = "Simple Album",
            durationMs = 200_000L, uri = "/storage/emulated/0/Music/simple.mp3",
            folderPath = "/storage/emulated/0/Music", mediaType = MediaType.AUDIO
        ),
        Track(
            id = "sm_video_1", mediaStoreId = 202L, title = "Simple Clip",
            artist = "Simple Director", album = "Simple Visuals",
            durationMs = 120_000L, uri = "/storage/emulated/0/Movies/simple.mp4",
            folderPath = "/storage/emulated/0/Movies", mediaType = MediaType.VIDEO
        )
    )

    private fun simpleState() = VeloraUiState(
        allTracks = demoTracks,
        audioTracks = demoTracks.filter { it.mediaType == MediaType.AUDIO },
        videoTracks = demoTracks.filter { it.mediaType == MediaType.VIDEO },
        settings = AppSettings() // defaults: isSimpleMode=true, tier SIMPLE
    )

    private fun standardState() = VeloraUiState(
        allTracks = demoTracks,
        audioTracks = demoTracks.filter { it.mediaType == MediaType.AUDIO },
        videoTracks = demoTracks.filter { it.mediaType == MediaType.VIDEO },
        settings = AppSettings(
            isSimpleMode = false,
            experienceTier = ExperienceTier.STANDARD
        )
    )

    /** Exact pro tags (§5.1 + studio_dsp_rack_card). */
    private val proTags = listOf(
        "transport_shuffle_button",
        "transport_repeat_button",
        "video_auto_hide_slider",
        "studio_dsp_rack_card",
        "pip_button"
    )

    /** Pro tag prefixes (§5.1): any node whose tag starts with one of these. */
    private val proTagPrefixes = listOf("eq_", "speed_", "subtitle_", "audio_track_", "ab_loop_")

    /** Pro content descriptions (§5.1). */
    private val proContentDescriptions = listOf("Toggle Shuffle", "Cycle Repeat Mode")

    private fun assertNoProNodes() {
        for (tag in proTags) {
            composeRule.onAllNodesWithTag(tag).assertCountEquals(0)
        }
        for (prefix in proTagPrefixes) {
            composeRule.onAllNodesWithTagPrefix(prefix).assertCountEquals(0)
        }
        for (description in proContentDescriptions) {
            composeRule.onAllNodesWithContentDescription(description).assertCountEquals(0)
        }
    }

    @Test
    fun `SIMPLE library has no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    uiState = state,
                    onScanLibrary = {}, onAddFolderUri = {}, onRemoveFolder = {},
                    onCreatePlaylist = {}, onDeletePlaylist = {},
                    onPlayPlaylist = { _, _ -> }, onPlayTrack = {},
                    onAddTrackToQueue = {}, onAddTrackToPlaylist = { _, _ -> }
                )
            }
        }
        assertNoProNodes()
    }

    @Test
    fun `SIMPLE music has no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                MusicScreen(
                    uiState = state,
                    onPlayAll = {}, onShuffleAll = {}, onPlayTrack = {},
                    onAddTrackToQueue = {}, onAddTrackToPlaylist = { _, _ -> }
                )
            }
        }
        assertNoProNodes()
    }

    @Test
    fun `SIMPLE artists has no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                ArtistScreen(
                    uiState = state,
                    onSelectArtist = {}, onPlayArtist = { _, _ -> },
                    onPlayTrack = {}, onAddTrackToQueue = {},
                    onAddTrackToPlaylist = { _, _ -> }
                )
            }
        }
        assertNoProNodes()
    }

    @Test
    fun `SIMPLE radio has no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                RadioScreen(
                    uiState = state,
                    onTuneStation = {}, onAddStationToQueue = {},
                    onAddCustomStation = { _, _, _ -> }
                )
            }
        }
        assertNoProNodes()
    }

    @Test
    fun `SIMPLE video has no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                VideoPlayerScreen(
                    uiState = state,
                    playerEngine = null,
                    onPlayVideoTrack = {}, onTogglePlayPause = {}, onSeekTo = {},
                    onSetPlaybackSpeed = {}, onSetAspectMode = {},
                    onSetSubtitlesEnabled = {}, onSelectAudioTrack = {},
                    onAddTrackToQueue = {}, onAddTrackToPlaylist = { _, _ -> }
                )
            }
        }
        composeRule.onNodeWithTag("video_player_screen").assertIsDisplayed()
        assertNoProNodes()
    }

    @Test
    fun `SIMPLE search has no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                SearchScreen(
                    uiState = state,
                    onQueryChange = {}, onPlayTrack = {},
                    onAddTrackToQueue = {}, onAddTrackToPlaylist = { _, _ -> }
                )
            }
        }
        assertNoProNodes()
    }

    @Test
    fun `SIMPLE player shows only the essentials and no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                PlayerAndEqScreen(
                    uiState = state,
                    onTogglePlayPause = {}, onStop = {}, onPrevious = {},
                    onNext = {}, onSeekTo = {}, onToggleShuffle = {},
                    onCycleRepeat = {}, onVolumeChange = {}, onToggleMute = {},
                    onSetEqEnabled = {}, onSetEqBandGain = { _, _ -> },
                    onApplyPreset = {}, onSaveCustomPreset = {},
                    onDeleteCustomPreset = {}, onSelectQueueTrack = {},
                    onRemoveFromQueue = {}, onClearError = {}
                )
            }
        }
        composeRule.onNodeWithTag("player_and_eq_screen").assertIsDisplayed()
        // Allow-list (§5.1 step 3): the essentials must be present.
        composeRule.onNodeWithTag("transport_play_pause_button").assertIsDisplayed()
        composeRule.onNodeWithTag("transport_prev_button").assertIsDisplayed()
        composeRule.onNodeWithTag("transport_next_button").assertIsDisplayed()
        composeRule.onNodeWithTag("player_seek_slider").assertIsDisplayed()
        assertNoProNodes()
    }

    @Test
    fun `SIMPLE settings has no pro nodes`() {
        val state = simpleState()
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    uiState = state,
                    onSetAutoHideMs = {}, onSignInWithGoogle = {}, onSignOut = {}
                )
            }
        }
        assertNoProNodes()
    }

    /**
     * Control test: the same player screen at STANDARD MUST expose the pro
     * controls. If this fails, the assertions above are vacuous and prove
     * nothing; if it passes, the SIMPLE assertions are meaningful.
     */
    @Test
    fun `STANDARD player exposes shuffle and repeat - control test`() {
        val state = standardState()
        composeRule.setContent {
            MaterialTheme {
                PlayerAndEqScreen(
                    uiState = state,
                    onTogglePlayPause = {}, onStop = {}, onPrevious = {},
                    onNext = {}, onSeekTo = {}, onToggleShuffle = {},
                    onCycleRepeat = {}, onVolumeChange = {}, onToggleMute = {},
                    onSetEqEnabled = {}, onSetEqBandGain = { _, _ -> },
                    onApplyPreset = {}, onSaveCustomPreset = {},
                    onDeleteCustomPreset = {}, onSelectQueueTrack = {},
                    onRemoveFromQueue = {}, onClearError = {}
                )
            }
        }
        composeRule.onNodeWithTag("transport_shuffle_button").assertIsDisplayed()
        composeRule.onNodeWithTag("transport_repeat_button").assertIsDisplayed()
        composeRule.onNodeWithTag("transport_play_pause_button").assertIsDisplayed()
    }
}

/**
 * Prefix variant of [androidx.compose.ui.test.onAllNodesWithTag]: matches any
 * node whose test tag starts with [prefix]. Needed because the §5.1 pro list
 * contains whole families (`eq_*`, `speed_*`, ...) rather than exact tags.
 */
private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagPrefix(
    prefix: String
): androidx.compose.ui.test.SemanticsNodeInteractionCollection {
    val matcher = androidx.compose.ui.test.SemanticsMatcher(
        "testTag starts with \"$prefix\""
    ) { node ->
        node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)
            ?.startsWith(prefix) == true
    }
    return onAllNodes(matcher)
}
