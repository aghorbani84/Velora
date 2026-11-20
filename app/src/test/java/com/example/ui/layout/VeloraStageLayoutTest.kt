package com.example.ui.layout

import androidx.compose.ui.unit.dp
import com.example.ui.theme.PhiTokens
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Arithmetic coverage for rule R-S3 ([VeloraStageLayout]).
 *
 * Pure Dp maths, so no Robolectric and no composition: these assertions pin
 * the numbers behind the layout rule, while the display-level proof belongs
 * with the `NowPlayingStageCard` wiring on issue #5. The window sizes below
 * are the ones CI actually failed on - 320x470dp is the Robolectric default
 * that produced the two `assertIsDisplayed()` failures in run #77 of PR #4,
 * and 411x891dp is the phone window P0-6 now pins.
 */
class VeloraStageLayoutTest {

    private val floor = VeloraStageLayout.CONTROLS_AREA_MIN_HEIGHT

    @Test
    fun `short 320x470 window keeps the full 317dp controls floor`() {
        val stage = VeloraStageLayout.stageBlockHeightCap(320.dp, 470.dp)
        val controls = VeloraStageLayout.controlsAreaHeight(320.dp, 470.dp)

        // 470 - 317 - 144 leaves 9dp, so the remainder rule must bind and the
        // artwork row must collapse instead of the controls.
        assertTrue("controls floor $floor not met, got $controls", controls >= floor)
        assertTrue("stage $stage + controls $controls overflow 470.dp", stage + controls <= 470.dp)
    }

    @Test
    fun `short window shrinks the stage below the width fraction`() {
        val stage = VeloraStageLayout.stageBlockHeightCap(320.dp, 470.dp)
        val widthOnlyCap =
            (320.dp * PhiTokens.ARTWORK_STAGE_MAX_FRACTION) + PhiTokens.SpectrumHeight

        assertTrue(
            "stage $stage was not shrunk below the width-only cap $widthOnlyCap",
            stage < widthOnlyCap
        )
    }

    @Test
    fun `tall phone window is still governed by the golden ratio fraction`() {
        val stage = VeloraStageLayout.stageBlockHeightCap(411.dp, 891.dp)
        val fractionCap =
            (411.dp * PhiTokens.ARTWORK_STAGE_MAX_FRACTION) + PhiTokens.SpectrumHeight
        val controls = VeloraStageLayout.controlsAreaHeight(411.dp, 891.dp)

        assertTrue("stage $stage must not exceed the 61.8% fraction $fractionCap", stage <= fractionCap)
        assertTrue("controls floor $floor not met, got $controls", controls >= floor)
    }

    @Test
    fun `window shorter than the floor never lets the stage overflow it`() {
        val stage = VeloraStageLayout.stageBlockHeightCap(1024.dp, 300.dp)
        val controls = VeloraStageLayout.controlsAreaHeight(1024.dp, 300.dp)

        assertTrue("stage $stage exceeds the 300.dp window", stage <= 300.dp)
        assertTrue("stage $stage + controls $controls overflow the window", stage + controls <= 300.dp)
    }
}
