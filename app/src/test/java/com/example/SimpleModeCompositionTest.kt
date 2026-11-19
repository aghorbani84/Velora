package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.getOrNull
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
 * P0-6 — `SimpleModeCompositionTest` (DESIGN-SYSTEM.md §5.1 Revision 1,
 * CLEAN-CODE-PLAN.md).
 *
 * The proof is STRUCTURAL, not visual: in SIMPLE, no node carrying a pro
 * test tag may exist in the composition tree at all — hiding a control with
 * `alpha(0)` or `visibility = Gone` fails this test, because the composable
 * must not be emitted. `if (tier != SIMPLE)` gating at the call site is the
 * only accepted mechanism (VeloraTransportControls already does this).
 *
 * What this test renders: each destination is rendered DIRECTLY (not through
 * NavGraph/ViewModel) with a fabricated [VeloraUiState] and no-op callbacks,
 * so the test exercises the real screens' tier gating without a device, a
 * database or an engine. The video screen is rendered with
 * `playerEngine = null` — [com.example.ui.components.VeloraVideoSurface]
 * returns early on a null engine, so no PlayerView is ever created under
 * Robolectric; the tier assertions do not depend on the surface.
 *
 * Contract implemented here (Revision 1 §5.1, per-destination absence
 * lists). Every tag asserted below was verified against main 19e70bc: either
 * it exists in the code behind a real tier gate, or it does not exist yet
 * (in which case the assertion passes vacuously today and starts guarding
 * the moment Phase 2 adds the tag). Where Revision 1's contract name differs
 * from the tag the code actually uses, the REAL tag is asserted and the
 * rename is recorded as Phase 2 work — asserting a tag that does not exist
 * would prove nothing:
 * - `player_stop_button` (contract) -> `transport_stop_button` (code)
 * - `exact_seek_button` -> `open_exact_seek_button`
 * - `tag_editor_button` -> `open_tag_editor_button`
 * - `favorite_button` (player) -> `player_favorite_button`
 * - `shuffle_all_button` (music) -> `music_shuffle_all_button`
 * - `bookmark_button` (video) -> `video_bookmark_button`
 * - `time_machine_*` -> `add_time_machine_bookmark_button`
 * - `sleep_timer_*` / `crossfade_*` -> `dsp_sleep_timer_*` / `dsp_crossfade_*`
 *
 * NOT covered here, honestly: the shell navigation assertion (Revision 1
 * §5.1 step 2 — the shell shows exactly music/video/player/settings in
 * SIMPLE and library/artists/radio/search are absent from navigation).
 * [com.example.ui.screens.VeloraShellScreen] already implements that
 * filtering (VeloraShellScreen.kt:131-143), but asserting it needs the shell
 * with a real ViewModel under `createAndroidComposeRule`, and no such test
 * exists in this project yet — the design doc itself flags that pattern
 * UNTESTED under Robolectric and leaves it to Phase 2 to prove (or move the
 * test to `androidTest`). The four hidden destinations are still rendered
 * directly below as defence in depth: if one were ever shown in SIMPLE, it
 * would have to be pro-clean.
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

    /** Pro tags / prefixes shared by every destination (§5.1). */
    private val commonProTags = listOf(
        "transport_shuffle_button",
        "transport_repeat_button",
        "pip_button",
        "pro_settings_door"
    )
    private val commonProPrefixes = listOf(
        "eq_", "speed_", "subtitle_", "audio_track_", "ab_loop_"
    )

    /** Player-only absence list (§5.1 Revision 1, real tag names — see KDoc). */
    private val playerProTags = listOf(
        "video_auto_hide_slider",
        "studio_dsp_rack_card",
        "player_favorite_button",
        "transport_stop_button",
        "open_exact_seek_button",
        "open_tag_editor_button",
        "synchronized_lyrics_card",
        "add_time_machine_bookmark_button"
    )
    private val playerProPrefixes = listOf(
        "queue_", "remove_queue_item_", "lyric_line_",
        "lrc_studio_", "visualizer_mode_",
        "dsp_sleep_timer_", "dsp_crossfade_"
    )

    /** Music-only absence list (§5.1 Revision 1, real tag names). */
    private val musicProTags = listOf(
        "music_shuffle_all_button",
        "quick_filter_field",
        "hires_filter_toggle"
    )

    /** Video-only absence list (§5.1 Revision 1, real tag names). */
    private val videoProTags = listOf("video_bookmark_button")

    /** Settings-only absence list (§5.1 Revision 1). */
    private val settingsProTags = listOf("video_auto_hide_slider")
    private val settingsProPrefixes = listOf("plugin_toggle_")

    /** Pro content descriptions (§5.1; only the ones that exist in code). */
    private val proContentDescriptions = listOf(
        "Toggle Shuffle", "Cycle Repeat Mode",
        // Same tier gate as the buttons they describe (stage-card header /
        // stop row); "Equalizer" has no contentDescription in the code.
        "Toggle Favorite", "Stop Playback", "Edit Metadata Tags"
    )

    private fun assertNoNodesWithTags(tags: List<String>, prefixes: List<String>) {
        for (tag in tags) {
            composeRule.onAllNodesWithTag(tag).assertCountEquals(0)
        }
        for (prefix in prefixes) {
            composeRule.onAllNodesWithTagPrefix(prefix).assertCountEquals(0)
        }
    }

    private fun assertNoProNodes() {
        assertNoNodesWithTags(commonProTags, commonProPrefixes)
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
        assertNoNodesWithTags(musicProTags, emptyList())
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
        assertNoNodesWithTags(videoProTags, emptyList())
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
        assertNoNodesWithTags(playerProTags, playerProPrefixes)
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
        assertNoNodesWithTags(settingsProTags, settingsProPrefixes)
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
