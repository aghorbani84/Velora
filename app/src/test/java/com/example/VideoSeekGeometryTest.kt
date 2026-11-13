package com.example

import com.example.ui.screens.seekFractionForX
import com.example.ui.screens.seekThumbCenterX
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure seek-geometry tests for the video player UX pass. No device needed.
 * The overlay thumb and the buffered/played fills in VideoSeekBar share
 * this math with the M3 Slider, whose thumb centre travels between one
 * thumb radius (10dp) and (width - thumbRadius).
 *
 * LTR only, by design: media progress is tape direction, never reading
 * direction — the seek bar is wrapped in an LTR layout direction at the
 * call site, so there is no RTL case to test here.
 */
class VideoSeekGeometryTest {

    private val width = 1000f
    private val radius = 10f

    @Test
    fun `fraction maps linearly across the inset track`() {
        // The track starts one thumb radius in: x = radius is 0%.
        assertEquals(0f, seekFractionForX(10f, width, radius), 0.001f)
        assertEquals(0.5f, seekFractionForX(500f, width, radius), 0.001f)
        // x = width - radius is 100%.
        assertEquals(1f, seekFractionForX(990f, width, radius), 0.001f)
    }

    @Test
    fun `fraction clamps outside the bar`() {
        assertEquals(0f, seekFractionForX(-50f, width, radius), 0.001f)
        assertEquals(0f, seekFractionForX(0f, width, radius), 0.001f)
        assertEquals(1f, seekFractionForX(1000f, width, radius), 0.001f)
        assertEquals(1f, seekFractionForX(1200f, width, radius), 0.001f)
    }

    @Test
    fun `fraction and thumb centre round-trip exactly at 0 and 100 percent`() {
        assertEquals(0f, seekFractionForX(seekThumbCenterX(0f, width, radius), width, radius), 0.001f)
        assertEquals(1f, seekFractionForX(seekThumbCenterX(1f, width, radius), width, radius), 0.001f)
    }

    @Test
    fun `fraction and thumb centre round-trip at mid positions`() {
        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
            assertEquals(
                fraction,
                seekFractionForX(seekThumbCenterX(fraction, width, radius), width, radius),
                0.001f
            )
        }
    }

    @Test
    fun `fraction guards NaN and degenerate widths`() {
        assertEquals(0f, seekFractionForX(Float.NaN, width, radius), 0.001f)
        assertEquals(0f, seekFractionForX(500f, Float.NaN, radius), 0.001f)
        assertEquals(0f, seekFractionForX(500f, width, Float.NaN), 0.001f)
        assertEquals(0f, seekFractionForX(5f, 0f, radius), 0.001f)
        assertEquals(0f, seekFractionForX(5f, -100f, radius), 0.001f)
        // Bar narrower than two thumb radii: no travel at all.
        assertEquals(0f, seekFractionForX(7f, 15f, radius), 0.001f)
        assertEquals(0f, seekFractionForX(10f, 20f, radius), 0.001f)
    }

    @Test
    fun `thumb centre stays inside the track`() {
        assertEquals(10f, seekThumbCenterX(0f, width, radius), 0.001f)
        assertEquals(500f, seekThumbCenterX(0.5f, width, radius), 0.001f)
        assertEquals(990f, seekThumbCenterX(1f, width, radius), 0.001f)
    }

    @Test
    fun `thumb fraction clamps`() {
        assertEquals(10f, seekThumbCenterX(-0.5f, width, radius), 0.001f)
        assertEquals(990f, seekThumbCenterX(1.5f, width, radius), 0.001f)
    }

    @Test
    fun `thumb guards NaN and zero width`() {
        assertEquals(0f, seekThumbCenterX(Float.NaN, width, radius), 0.001f)
        assertEquals(0f, seekThumbCenterX(0.5f, Float.NaN, radius), 0.001f)
        assertEquals(0f, seekThumbCenterX(0.5f, width, Float.NaN), 0.001f)
        assertEquals(0f, seekThumbCenterX(0.5f, 0f, radius), 0.001f)
        assertEquals(0f, seekThumbCenterX(0.5f, -100f, radius), 0.001f)
    }
}
