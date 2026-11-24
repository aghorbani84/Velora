package com.example.ui.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class VeloraStageLayoutTest {

    @Test
    fun `470dp viewport preserves the 317dp controls and queue floor`() {
        assertEquals(153.dp, VeloraStageLayout.stageBlockHeightCap(320.dp, 470.dp))
        assertEquals(317.dp, VeloraStageLayout.controlsAreaHeight(320.dp, 470.dp))
    }

    @Test
    fun `spectrum shrinks when needed to preserve the controls floor`() {
        assertEquals(3.dp, VeloraStageLayout.stageBlockHeightCap(320.dp, 320.dp))
        assertEquals(317.dp, VeloraStageLayout.controlsAreaHeight(320.dp, 320.dp))
    }

    @Test
    fun `viewport shorter than the controls floor leaves no stage height`() {
        assertEquals(0.dp, VeloraStageLayout.stageBlockHeightCap(320.dp, 280.dp))
        assertEquals(280.dp, VeloraStageLayout.controlsAreaHeight(320.dp, 280.dp))
    }

    @Test
    fun `compact spectrum height can be supplied without weakening the floor`() {
        assertEquals(
            72.dp,
            VeloraStageLayout.stageBlockHeightCap(
                maxWidth = 320.dp,
                availableHeight = 389.dp,
                preferredSpectrumHeight = 72.dp
            )
        )
        assertEquals(
            317.dp,
            VeloraStageLayout.controlsAreaHeight(
                maxWidth = 320.dp,
                availableHeight = 389.dp,
                preferredSpectrumHeight = 72.dp
            )
        )
    }
}
