package com.example

import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.EqSpecification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * WP-5.2 Property-based verification suite for the 10-band RBJ peaking biquad DSP engine.
 */
class DspBiquadMathTest {

    @Test
    fun `ISO 10-band frequencies and Q factor satisfy sqrt(2) precision invariant`() {
        assertEquals(10, EqSpecification.BAND_COUNT)
        val expectedQ = sqrt(2.0).toFloat()
        assertEquals(expectedQ, EqSpecification.DEFAULT_Q, 1e-5f)
        assertEquals(31.25f, EqSpecification.CENTER_FREQUENCIES_HZ.first(), 1e-4f)
        assertEquals(16_000.0f, EqSpecification.CENTER_FREQUENCIES_HZ.last(), 1e-4f)
    }

    @Test
    fun `zero dB gain yields exact unity pass-through coefficients across all 10 ISO bands`() {
        val sampleRates = listOf(44_100, 48_000, 96_000)
        for (sr in sampleRates) {
            for (freq in EqSpecification.CENTER_FREQUENCIES_HZ) {
                val coeffs = BiquadEqDsp.calculatePeakingBiquad(
                    sampleRateHz = sr,
                    centerFreqHz = freq,
                    gainDb = 0.0f
                )
                assertEquals(1.0, coeffs.b0, 1e-9)
                assertEquals(0.0, coeffs.b1, 1e-9)
                assertEquals(0.0, coeffs.b2, 1e-9)
                assertEquals(0.0, coeffs.a1, 1e-9)
                assertEquals(0.0, coeffs.a2, 1e-9)
            }
        }
    }

    @Test
    fun `biquad coefficients across all bands, sample rates, and gains are finite and BIBO stable`() {
        val sampleRates = listOf(44_100, 48_000, 88_200, 96_000, 192_000)
        val gainsDb = listOf(-12.0f, -9.5f, -6.0f, -3.0f, -0.5f, 0.0f, 0.5f, 3.0f, 6.0f, 9.5f, 12.0f)

        for (sr in sampleRates) {
            for (freq in EqSpecification.CENTER_FREQUENCIES_HZ) {
                for (gain in gainsDb) {
                    val c = BiquadEqDsp.calculatePeakingBiquad(
                        sampleRateHz = sr,
                        centerFreqHz = freq,
                        gainDb = gain
                    )
                    // 1. Finite check (zero NaN / Infinity)
                    assertFalse("b0 NaN/Inf at f=$freq sr=$sr g=$gain", c.b0.isNaN() || c.b0.isInfinite())
                    assertFalse("b1 NaN/Inf at f=$freq sr=$sr g=$gain", c.b1.isNaN() || c.b1.isInfinite())
                    assertFalse("b2 NaN/Inf at f=$freq sr=$sr g=$gain", c.b2.isNaN() || c.b2.isInfinite())
                    assertFalse("a1 NaN/Inf at f=$freq sr=$sr g=$gain", c.a1.isNaN() || c.a1.isInfinite())
                    assertFalse("a2 NaN/Inf at f=$freq sr=$sr g=$gain", c.a2.isNaN() || c.a2.isInfinite())

                    // 2. Jury / Schur-Cohn BIBO stability conditions for 2nd-order IIR denominator:
                    //    1 + a1*z^-1 + a2*z^-2 has roots strictly inside unit circle iff:
                    //    |a2| < 1  AND  |a1| < 1 + a2
                    val absA2 = abs(c.a2)
                    val absA1 = abs(c.a1)
                    assertTrue(
                        "Unstable pole |a2| >= 1 (${c.a2}) at f=$freq sr=$sr g=$gain",
                        absA2 < 1.0
                    )
                    assertTrue(
                        "Unstable pole |a1| >= 1 + a2 (a1=${c.a1}, a2=${c.a2}) at f=$freq sr=$sr g=$gain",
                        absA1 < 1.0 + c.a2 + 1e-9
                    )
                }
            }
        }
    }

    @Test
    fun `softClipLimiter is strictly linear below knee threshold and asymptotically bounded at unity`() {
        assertEquals(0.0, BiquadEqDsp.softClipLimiter(0.0), 1e-12)
        assertEquals(0.5, BiquadEqDsp.softClipLimiter(0.5), 1e-12)
        assertEquals(-0.85, BiquadEqDsp.softClipLimiter(-0.85), 1e-12)

        val softHigh = BiquadEqDsp.softClipLimiter(2.5)
        val softExtreme = BiquadEqDsp.softClipLimiter(10.0)
        assertTrue("Soft limiter must smoothly compress above knee (0.85 < $softHigh <= 1.0)", softHigh > 0.85 && softHigh <= 1.0)
        assertTrue("Soft limiter must be monotonic ($softExtreme >= $softHigh)", softExtreme >= softHigh)
        assertEquals(-softHigh, BiquadEqDsp.softClipLimiter(-2.5), 1e-12)
    }

    @Test
    fun `cascade magnitude response curve is 0dB for flat preset and reflects band boost and auto-preamp`() {
        val flatCurve = BiquadEqDsp.evaluateCascadeMagnitudeResponseDb(
            sampleRateHz = 48_000,
            gainsDb = com.example.domain.model.EqPreset.FLAT.gainsDb,
            pointCount = 64
        )
        assertEquals(64, flatCurve.size)
        for (db in flatCurve) {
            assertEquals(0.0f, db, 1e-4f)
        }

        val bassCurve = BiquadEqDsp.evaluateCascadeMagnitudeResponseDb(
            sampleRateHz = 48_000,
            gainsDb = com.example.domain.model.EqPreset.BASS_BOOST.gainsDb,
            pointCount = 64
        )
        assertTrue("Sub-bass response must be boosted > +5 dB", bassCurve.first() > 5.0f)

        val autoPreamp = BiquadEqDsp.computeOptimalAutoPreampDb(
            gainsDb = com.example.domain.model.EqPreset.BASS_BOOST.gainsDb,
            bassBoostPercent = 50
        )
        assertTrue("Auto-preamp must apply negative headroom compensation ($autoPreamp dB)", autoPreamp < -5.0f)
    }

    @Test
    fun `Oklab perceptual lightness and APCA contrast validate Velora Saffron on Obsidian`() {
        val lObsidian = com.example.domain.model.PhiDesignMetrics.perceptualOklabLightness(0x0B0B0D)
        val lSaffron = com.example.domain.model.PhiDesignMetrics.perceptualOklabLightness(0xE5A817)
        val lIvory = com.example.domain.model.PhiDesignMetrics.perceptualOklabLightness(0xF5F2EB)

        assertTrue("Obsidian Oklab L must be dark (< 0.20)", lObsidian < 0.20)
        assertTrue("Saffron Oklab L must be bright (> 0.70)", lSaffron > 0.70)
        assertTrue("Ivory Oklab L must exceed Saffron L", lIvory > lSaffron)

        val apcaSaffron = abs(com.example.domain.model.PhiDesignMetrics.apcaPerceptualContrastLc(0xE5A817, 0x0B0B0D))
        val apcaIvory = abs(com.example.domain.model.PhiDesignMetrics.apcaPerceptualContrastLc(0xF5F2EB, 0x0B0B0D))
        assertTrue("Saffron on Obsidian must exceed APCA Lc 60 ($apcaSaffron)", apcaSaffron >= 60.0)
        assertTrue("Ivory on Obsidian must exceed APCA Lc 90 ($apcaIvory)", apcaIvory >= 90.0)
    }
}
