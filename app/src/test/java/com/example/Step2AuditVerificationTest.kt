package com.example

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.PlaybackStatus
import com.example.domain.engine.formatAudioChannelLabel
import com.example.domain.model.AppSettings
import com.example.domain.model.ExperienceTier
import com.example.domain.model.GestureActionType
import com.example.domain.model.MediaType
import com.example.domain.model.StudioDspState
import com.example.domain.model.Track
import com.example.domain.model.VeloraGestureEngine
import com.example.ui.components.VeloraSeekBar
import com.example.ui.components.veloraCinemaGestures
import com.example.ui.screens.PlayerAndEqScreen
import com.example.ui.screens.VeloraTopHeaderBar
import com.example.ui.theme.VeloraTheme
import com.example.ui.viewmodel.PlaybackPositionState
import com.example.ui.viewmodel.VeloraUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class Step2AuditVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleFlacTrack() = Track(
        id = "step2_flac",
        mediaStoreId = 901L,
        title = "Saffron Nocturne",
        artist = "Velora Ensemble",
        album = "Obsidian Master",
        durationMs = 240_000L,
        uri = "/storage/emulated/0/Music/saffron_nocturne.flac",
        folderPath = "/storage/emulated/0/Music",
        mediaType = MediaType.AUDIO,
        sampleRateHz = 96_000,
        bitrateKbps = 2116
    )

    @Test
    fun `VeloraGestureEngine horizontal scrub resolves forward, backward, and clamped positions`() {
        val forward = VeloraGestureEngine.resolveHorizontalScrubGesture(
            dragDeltaX = 250f,
            surfaceWidth = 1000f,
            currentPositionMs = 60_000L,
            durationMs = 240_000L,
            fullWidthScrubWindowMs = 80_000L
        )
        assertEquals(GestureActionType.SEEK_FORWARD, forward.action)
        assertEquals(80_000L, forward.targetPositionMs)
        assertEquals(20, forward.deltaSeconds)
        assertTrue(forward.hudLabel.contains("01:20"))
        assertTrue(forward.hudLabel.contains("+20s"))

        val backwardClamped = VeloraGestureEngine.resolveHorizontalScrubGesture(
            dragDeltaX = -900f,
            surfaceWidth = 1000f,
            currentPositionMs = 15_000L,
            durationMs = 240_000L,
            fullWidthScrubWindowMs = 80_000L
        )
        assertEquals(GestureActionType.SEEK_BACKWARD, backwardClamped.action)
        assertEquals(0L, backwardClamped.targetPositionMs)
        assertEquals(-15, backwardClamped.deltaSeconds)
        assertTrue(backwardClamped.hudLabel.contains("00:00"))
    }

    @Test
    fun `EnginePlaybackState audioSpecSummary reports real bit depth and channel layout without fabrication`() {
        val flac = sampleFlacTrack()
        val withDecoderInfo = EnginePlaybackState(
            currentTrack = flac,
            audioBitDepth = 24,
            audioChannelCount = 2
        )
        val fullSummary = withDecoderInfo.audioSpecSummary(flac, "FLAC")
        assertTrue(fullSummary.contains("FLAC"))
        assertTrue(fullSummary.contains("96000Hz"))
        assertTrue(fullSummary.contains("2116 kbps"))
        assertTrue(fullSummary.contains("24-bit"))
        assertTrue(fullSummary.contains("Stereo"))

        val losslessUnknownDepth = EnginePlaybackState(currentTrack = flac)
        val losslessSummary = losslessUnknownDepth.audioSpecSummary(flac, "FLAC")
        assertTrue(losslessSummary.contains("Lossless"))
        assertTrue(!losslessSummary.contains("-bit"))

        assertEquals("Mono", formatAudioChannelLabel(1))
        assertEquals("Stereo", formatAudioChannelLabel(2))
        assertEquals("5.1", formatAudioChannelLabel(6))
        assertEquals("7.1", formatAudioChannelLabel(8))
    }

    @Test
    fun `VeloraSeekBar renders A-B loop visual markers and badge only when bounds are set`() {
        composeRule.setContent {
            VeloraTheme {
                VeloraSeekBar(
                    positionMs = 45_000L,
                    durationMs = 180_000L,
                    onSeekTo = {},
                    loopStartMs = 30_000L,
                    loopEndMs = 75_000L
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("seekbar_ab_markers").assertIsDisplayed()
        composeRule.onNodeWithTag("seekbar_ab_loop_badge")
            .assertIsDisplayed()
            .assertTextContains("00:30", substring = true)
            .assertTextContains("01:15", substring = true)
    }

    @Test
    fun `VeloraSeekBar hides A-B loop markers when no loop bounds are set`() {
        composeRule.setContent {
            VeloraTheme {
                VeloraSeekBar(
                    positionMs = 45_000L,
                    durationMs = 180_000L,
                    onSeekTo = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("seekbar_ab_markers").assertDoesNotExist()
        composeRule.onNodeWithTag("seekbar_ab_loop_badge").assertDoesNotExist()
    }

    @Test
    fun `VeloraTopHeaderBar exposes direct SIMPLE, STANDARD, and PRO tier selectors alongside toggle`() {
        val selectedTiers = mutableListOf<ExperienceTier>()

        composeRule.setContent {
            VeloraTheme {
                VeloraTopHeaderBar(
                    trackCount = 42,
                    tier = ExperienceTier.STANDARD,
                    isPersian = false,
                    bannerMessage = null,
                    onDismissBanner = {},
                    onCycleLanguage = {},
                    onSelectTier = { selectedTiers.add(it) }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("shell_mode_toggle_button").assertIsDisplayed()
        composeRule.onNodeWithTag("tier_selector_SIMPLE").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tier_selector_STANDARD").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("tier_selector_PRO").assertIsDisplayed().performClick()

        assertEquals(
            listOf(ExperienceTier.SIMPLE, ExperienceTier.STANDARD, ExperienceTier.PRO),
            selectedTiers
        )
    }

    @Test
    @Config(qualifiers = "w412dp-h1400dp")
    fun `NowPlayingScreen renders bit depth and channel badge and forwards A-B loop markers to seek bar`() {
        val flac = sampleFlacTrack()
        val uiState = VeloraUiState(
            settings = AppSettings(isSimpleMode = false, experienceTier = ExperienceTier.PRO),
            engineState = EnginePlaybackState(
                currentTrack = flac,
                status = PlaybackStatus.PLAYING,
                audioBitDepth = 24,
                audioChannelCount = 2
            ),
            studioDspState = StudioDspState(
                loopStartMs = 20_000L,
                loopEndMs = 95_000L
            )
        )

        composeRule.setContent {
            VeloraTheme {
                PlayerAndEqScreen(
                    uiState = uiState,
                    positionState = PlaybackPositionState(
                        positionMs = 40_000L,
                        durationMs = 240_000L,
                        isPlaying = true
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
                    onClearError = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("now_playing_audio_spec_badge")
            .assertIsDisplayed()
            .assertTextContains("24-bit", substring = true)
            .assertTextContains("Stereo", substring = true)
        composeRule.onNodeWithTag("seekbar_ab_markers").assertIsDisplayed()
        composeRule.onNodeWithTag("seekbar_ab_loop_badge")
            .assertIsDisplayed()
            .assertTextContains("00:20", substring = true)
            .assertTextContains("01:35", substring = true)
    }

    @Test
    fun `veloraCinemaGestures horizontal drag scrubs timeline`() {
        var committedSeekMs: Long? = null
        var lastHud: String? = null

        composeRule.setContent {
            VeloraTheme {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .testTag("cinema_gesture_surface")
                        .veloraCinemaGestures(
                            controlsLocked = false,
                            positionMs = 30_000L,
                            durationMs = 180_000L,
                            currentBrightness = 0.7f,
                            currentVolume = 0.8f,
                            onToggleControls = {},
                            onInteraction = {},
                            onSeekTo = { committedSeekMs = it },
                            onTogglePlayPause = {},
                            onSetBrightness = {},
                            onSetVolume = {},
                            onHudNotice = { lastHud = it }
                        )
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cinema_gesture_surface").performTouchInput {
            swipeRight(startX = width * 0.2f, endX = width * 0.8f)
        }
        composeRule.waitForIdle()

        assertNotNull(committedSeekMs)
        assertTrue((committedSeekMs ?: 0L) > 30_000L)
        assertNotNull(lastHud)
        assertTrue(lastHud!!.contains("⏩"))
    }

    @Test
    fun `veloraCinemaGestures horizontal drag is ignored when controls are locked`() {
        var committedSeekMs: Long? = null
        var lastHud: String? = null

        composeRule.setContent {
            VeloraTheme {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .testTag("cinema_gesture_surface_locked")
                        .veloraCinemaGestures(
                            controlsLocked = true,
                            positionMs = 30_000L,
                            durationMs = 180_000L,
                            currentBrightness = 0.7f,
                            currentVolume = 0.8f,
                            onToggleControls = {},
                            onInteraction = {},
                            onSeekTo = { committedSeekMs = it },
                            onTogglePlayPause = {},
                            onSetBrightness = {},
                            onSetVolume = {},
                            onHudNotice = { lastHud = it }
                        )
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("cinema_gesture_surface_locked").performTouchInput {
            swipeRight(startX = width * 0.2f, endX = width * 0.8f)
        }
        composeRule.waitForIdle()
        assertNull(committedSeekMs)
        assertNull(lastHud)
    }
}
