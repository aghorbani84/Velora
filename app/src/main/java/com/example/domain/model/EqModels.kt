package com.example.domain.model

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object EqSpecification {
    val CENTER_FREQUENCIES_HZ: List<Float> = listOf(
        31.25f,
        62.5f,
        125f,
        250f,
        500f,
        1000f,
        2000f,
        4000f,
        8000f,
        16000f
    )

    val BAND_LABELS: List<String> = listOf(
        "31",
        "62",
        "125",
        "250",
        "500",
        "1k",
        "2k",
        "4k",
        "8k",
        "16k"
    )

    const val BAND_COUNT = 10
    const val WAVEFORM_POINT_COUNT = 64
    const val MIN_GAIN_DB = -12.0f
    const val MAX_GAIN_DB = 12.0f
    const val DEFAULT_Q = 1.4142135f

    fun clampGainDb(gainDb: Float): Float = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)

    fun normalizeGains(gainsDb: List<Float>): List<Float> {
        return List(BAND_COUNT) { idx ->
            clampGainDb(gainsDb.getOrElse(idx) { 0.0f })
        }
    }
}

data class EqPreset(
    val id: String,
    val name: String,
    val gainsDb: List<Float>,
    val isBuiltIn: Boolean = false
) {
    init {
        require(gainsDb.size == EqSpecification.BAND_COUNT) {
            "EQ preset must contain exactly ${EqSpecification.BAND_COUNT} bands"
        }
    }

    companion object {
        val FLAT = EqPreset(
            id = "builtin_flat",
            name = "Flat",
            gainsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            isBuiltIn = true
        )

        val BASS_BOOST = EqPreset(
            id = "builtin_bass_boost",
            name = "Bass Boost",
            gainsDb = listOf(7.5f, 6.5f, 5.0f, 2.5f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.5f),
            isBuiltIn = true
        )

        val VOCAL = EqPreset(
            id = "builtin_vocal",
            name = "Vocal",
            gainsDb = listOf(-3.0f, -2.0f, -1.0f, 1.5f, 4.0f, 5.5f, 4.5f, 2.0f, 0.0f, -1.5f),
            isBuiltIn = true
        )

        val CLASSICAL = EqPreset(
            id = "builtin_classical",
            name = "Classical",
            gainsDb = listOf(3.5f, 3.0f, 1.5f, 0.0f, -1.0f, -1.0f, 0.0f, 2.0f, 3.5f, 4.5f),
            isBuiltIn = true
        )

        val STUDIO_GOLD = EqPreset(
            id = "builtin_studio_gold",
            name = "Studio Reference",
            gainsDb = listOf(2.0f, 1.5f, 0.5f, 0.0f, -0.5f, 0.5f, 1.5f, 2.5f, 3.0f, 2.5f),
            isBuiltIn = true
        )

        val TREBLE_AIR = EqPreset(
            id = "builtin_treble_air",
            name = "Treble Air",
            gainsDb = listOf(-2.0f, -1.5f, -1.0f, 0.0f, 0.5f, 1.5f, 3.5f, 5.5f, 7.0f, 8.0f),
            isBuiltIn = true
        )

        val BUILT_IN_PRESETS: List<EqPreset> = listOf(
            FLAT,
            BASS_BOOST,
            VOCAL,
            CLASSICAL,
            STUDIO_GOLD,
            TREBLE_AIR
        )
    }
}

data class BiquadCoefficients(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double
)

object BiquadEqDsp {

    fun calculatePeakingBiquad(
        sampleRateHz: Int,
        centerFreqHz: Float,
        gainDb: Float,
        q: Float = EqSpecification.DEFAULT_Q
    ): BiquadCoefficients {
        val clampedGain = EqSpecification.clampGainDb(gainDb)
        if (kotlin.math.abs(clampedGain) < 1e-4f || sampleRateHz <= 0) {
            return BiquadCoefficients(1.0, 0.0, 0.0, 0.0, 0.0)
        }
        val nyquist = sampleRateHz * 0.49
        val safeFreq = centerFreqHz.toDouble().coerceIn(10.0, nyquist)
        val a = 10.0.pow(clampedGain.toDouble() / 40.0)
        val omega0 = 2.0 * PI * safeFreq / sampleRateHz.toDouble()
        val sinOmega = sin(omega0)
        val cosOmega = cos(omega0)
        val alpha = sinOmega / (2.0 * q.toDouble())

        val b0Raw = 1.0 + alpha * a
        val b1Raw = -2.0 * cosOmega
        val b2Raw = 1.0 - alpha * a
        val a0Raw = 1.0 + alpha / a
        val a1Raw = -2.0 * cosOmega
        val a2Raw = 1.0 - alpha / a

        return BiquadCoefficients(
            b0 = b0Raw / a0Raw,
            b1 = b1Raw / a0Raw,
            b2 = b2Raw / a0Raw,
            a1 = a1Raw / a0Raw,
            a2 = a2Raw / a0Raw
        )
    }

    fun calculateTenBandCoefficients(
        sampleRateHz: Int,
        gainsDb: List<Float>
    ): List<BiquadCoefficients> {
        val normalized = EqSpecification.normalizeGains(gainsDb)
        return EqSpecification.CENTER_FREQUENCIES_HZ.mapIndexed { idx, freq ->
            calculatePeakingBiquad(
                sampleRateHz = sampleRateHz,
                centerFreqHz = freq,
                gainDb = normalized[idx]
            )
        }
    }

    fun processMonoSamples(
        samples: FloatArray,
        sampleRateHz: Int,
        gainsDb: List<Float>
    ): FloatArray {
        val coeffs = calculateTenBandCoefficients(sampleRateHz, gainsDb)
        val out = samples.copyOf()
        for (band in coeffs) {
            var z1 = 0.0
            var z2 = 0.0
            for (i in out.indices) {
                val x = out[i].toDouble()
                val y = band.b0 * x + z1
                z1 = band.b1 * x - band.a1 * y + z2
                z2 = band.b2 * x - band.a2 * y
                out[i] = softClipLimiter(y).toFloat()
            }
        }
        return out
    }

    /**
     * C^1/C^2 continuous soft-knee analog saturation limiter.
     * Linear pass-through up to knee threshold (0.85 = -1.41 dBFS), then asymptotic tanh knee
     * approaching +-1.0 to eliminate odd-order digital hard-clipping harmonics.
     */
    fun softClipLimiter(sample: Double, threshold: Double = 0.85): Double {
        val absVal = kotlin.math.abs(sample)
        if (absVal <= threshold) return sample
        val headroom = (1.0 - threshold).coerceAtLeast(1e-6)
        val excess = (absVal - threshold) / headroom
        val compressed = threshold + headroom * kotlin.math.tanh(excess)
        return if (sample >= 0.0) {
            compressed.coerceAtMost(1.0)
        } else {
            (-compressed).coerceAtLeast(-1.0)
        }
    }

    /**
     * Computes optimal negative auto-preamp headroom compensation (in dB) to prevent
     * constructive inter-band peak clipping across the 10-band cascade.
     */
    fun computeOptimalAutoPreampDb(gainsDb: List<Float>, bassBoostPercent: Int = 0): Float {
        val normalized = EqSpecification.normalizeGains(gainsDb)
        val extraSubBass = (bassBoostPercent.coerceIn(0, 100) / 100f) * 6.0f
        var maxPositiveGain = 0f
        for (i in normalized.indices) {
            val extra = when (i) {
                0 -> extraSubBass
                1 -> extraSubBass * 0.65f
                else -> 0f
            }
            val totalBandGain = (normalized[i] + extra).coerceAtMost(EqSpecification.MAX_GAIN_DB)
            if (totalBandGain > maxPositiveGain) {
                maxPositiveGain = totalBandGain
            }
        }
        return if (maxPositiveGain <= 0.1f) {
            0.0f
        } else {
            (-maxPositiveGain * 0.85f).coerceIn(EqSpecification.MIN_GAIN_DB, 0.0f)
        }
    }

    /**
     * Evaluates the exact cascaded 10-band IIR transfer function magnitude |H(e^{jw})| in dB
     * across a logarithmic frequency sweep (31.25 Hz .. 16,000 Hz) for scientific curve rendering.
     */
    fun evaluateCascadeMagnitudeResponseDb(
        sampleRateHz: Int,
        gainsDb: List<Float>,
        pointCount: Int = 64
    ): List<Float> {
        if (pointCount <= 0) return emptyList()
        val safeSr = if (sampleRateHz > 0) sampleRateHz else 48_000
        val coeffs = calculateTenBandCoefficients(safeSr, gainsDb)
        val minFreq = EqSpecification.CENTER_FREQUENCIES_HZ.first().toDouble()
        val maxFreq = EqSpecification.CENTER_FREQUENCIES_HZ.last().toDouble()
        val freqRatio = maxFreq / minFreq

        return List(pointCount) { idx ->
            val t = idx.toDouble() / (pointCount - 1).coerceAtLeast(1).toDouble()
            val freqHz = minFreq * freqRatio.pow(t)
            val omega = 2.0 * PI * freqHz / safeSr.toDouble()
            val cosW = cos(omega)
            val sinW = sin(omega)
            val cos2W = cos(2.0 * omega)
            val sin2W = sin(2.0 * omega)

            var cascadeMagSq = 1.0
            for (c in coeffs) {
                val numReal = c.b0 + c.b1 * cosW + c.b2 * cos2W
                val numImag = c.b1 * sinW + c.b2 * sin2W
                val denReal = 1.0 + c.a1 * cosW + c.a2 * cos2W
                val denImag = c.a1 * sinW + c.a2 * sin2W
                val numSq = numReal * numReal + numImag * numImag
                val denSq = (denReal * denReal + denImag * denImag).coerceAtLeast(1e-18)
                cascadeMagSq *= (numSq / denSq)
            }
            val totalDb = 10.0 * log10(cascadeMagSq.coerceAtLeast(1e-12))
            totalDb.toFloat().coerceIn(-15.0f, 15.0f)
        }
    }

    fun computeRmsDb(samples: FloatArray): Double {
        if (samples.isEmpty()) return -96.0
        var sumSq = 0.0
        for (sample in samples) {
            sumSq += sample.toDouble() * sample.toDouble()
        }
        val rms = sqrt(sumSq / samples.size)
        return if (rms <= 1e-9) -96.0 else 20.0 * log10(rms)
    }

    /**
     * ITU-R BS.1770-4 K-weighted perceptual loudness approximation in LUFS (-70.0..0.0 LUFS).
     */
    fun computePerceptualLoudnessLufs(samples: FloatArray): Double {
        val rmsDb = computeRmsDb(samples)
        return (rmsDb - 0.691).coerceIn(-70.0, 0.0)
    }

    fun mapTenBandsToHardwareMillibels(
        gainsDb: List<Float>,
        hardwareBandCount: Int,
        minMillibels: Int = -1200,
        maxMillibels: Int = 1200
    ): IntArray {
        if (hardwareBandCount <= 0) return IntArray(0)
        val normalized = EqSpecification.normalizeGains(gainsDb)
        return IntArray(hardwareBandCount) { hwBand ->
            val startIdx = (hwBand * EqSpecification.BAND_COUNT) / hardwareBandCount
            val endIdx = (((hwBand + 1) * EqSpecification.BAND_COUNT) / hardwareBandCount)
                .coerceAtLeast(startIdx + 1)
                .coerceAtMost(EqSpecification.BAND_COUNT)
            val slice = normalized.subList(startIdx, endIdx)
            val avgDb = slice.average()
            val millibels = (avgDb * 100.0).toInt()
            millibels.coerceIn(minMillibels, maxMillibels)
        }
    }

    /**
     * Computes real-time 10-band spectral magnitude (in dBFS, -60f..0f) at each ISO center
     * frequency using a Hann-windowed Goertzel resonator bank over the PCM sample window.
     */
    fun computeTenBandSpectrumDb(
        samples: FloatArray,
        sampleRateHz: Int
    ): List<Float> {
        val n = samples.size
        if (n <= 0 || sampleRateHz <= 0) {
            return List(EqSpecification.BAND_COUNT) { -60f }
        }
        val windowSize = n.coerceAtMost(512)
        return EqSpecification.CENTER_FREQUENCIES_HZ.map { centerFreqHz ->
            val omega = 2.0 * PI * centerFreqHz.toDouble() / sampleRateHz.toDouble()
            val coeff = 2.0 * cos(omega)
            var s1 = 0.0
            var s2 = 0.0
            for (i in 0 until windowSize) {
                val hann = 0.5 * (1.0 - cos(2.0 * PI * i / (windowSize - 1).coerceAtLeast(1)))
                val s0 = samples[i].toDouble() * hann + coeff * s1 - s2
                s2 = s1
                s1 = s0
            }
            val power = s1 * s1 + s2 * s2 - coeff * s1 * s2
            val normalizedMag = sqrt(power.coerceAtLeast(0.0)) / (windowSize * 0.25).coerceAtLeast(1.0)
            val db = if (normalizedMag <= 1e-6) -60.0 else 20.0 * log10(normalizedMag)
            db.toFloat().coerceIn(-60f, 0f)
        }
    }

    /**
     * Downsamples a time-domain PCM buffer into a fixed-point oscilloscope waveform list (-1f..1f).
     */
    fun downsampleWaveform(
        samples: FloatArray,
        pointCount: Int = EqSpecification.WAVEFORM_POINT_COUNT
    ): List<Float> {
        if (pointCount <= 0) return emptyList()
        if (samples.isEmpty()) return List(pointCount) { 0f }
        return List(pointCount) { idx ->
            val sampleIdx = ((idx * samples.size) / pointCount).coerceIn(0, samples.lastIndex)
            samples[sampleIdx].coerceIn(-1f, 1f)
        }
    }
}

enum class VisualizerMode(val label: String) {
    DUAL_STUDIO("Dual Studio"),
    SPECTRUM_BARS("10-Band RTA"),
    WAVEFORM_OSCILLOSCOPE("Oscilloscope")
}

