package com.example.data.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import com.example.domain.model.BiquadCoefficients
import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.EqSpecification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

data class AudioVisualizerSnapshot(
    val spectrumBandsDb: List<Float> = List(EqSpecification.BAND_COUNT) { -60f },
    val spectrumPeaksDb: List<Float> = List(EqSpecification.BAND_COUNT) { -60f },
    val waveformSamples: List<Float> = List(EqSpecification.WAVEFORM_POINT_COUNT) { 0f },
    val rmsLevelDb: Float = -60f,
    val peakLevelDb: Float = -60f
)

@OptIn(UnstableApi::class)
class TenBandBiquadAudioProcessor : AudioProcessor {

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var inputEnded: Boolean = false

    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var internalBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    @Volatile
    private var eqEnabled: Boolean = true

    @Volatile
    private var preampLinearGain: Double = 1.0

    @Volatile
    private var bassBoostExtraDb: Float = 0f

    @Volatile
    private var stereoWidthFactor: Double = 0.0

    @Volatile
    private var currentGainsDb: List<Float> = List(EqSpecification.BAND_COUNT) { 0f }

    @Volatile
    private var coefficients: List<BiquadCoefficients> =
        BiquadEqDsp.calculateTenBandCoefficients(44100, currentGainsDb)

    private var z1State: Array<DoubleArray> = Array(EqSpecification.BAND_COUNT) { DoubleArray(2) }
    private var z2State: Array<DoubleArray> = Array(EqSpecification.BAND_COUNT) { DoubleArray(2) }

    private val smoothedSpectrum = FloatArray(EqSpecification.BAND_COUNT) { -60f }
    private val peakHoldSpectrum = FloatArray(EqSpecification.BAND_COUNT) { -60f }
    private val reusableMonoAnalysisBuffer = FloatArray(512)
    private var reusableFrameSamples = DoubleArray(2)
    private var ditherState: Long = 0x5DEECE66DL

    private val _spectrumBandsDb = MutableStateFlow(List(EqSpecification.BAND_COUNT) { -60f })
    val spectrumBandsDb: StateFlow<List<Float>> = _spectrumBandsDb.asStateFlow()

    private val _visualizerSnapshot = MutableStateFlow(AudioVisualizerSnapshot())
    val visualizerSnapshot: StateFlow<AudioVisualizerSnapshot> = _visualizerSnapshot.asStateFlow()

    @Volatile
    private var isLowBatteryMode: Boolean = false
    private var telemetryThrottleCounter: Int = 0

    fun setLowBatteryMode(enabled: Boolean) {
        isLowBatteryMode = enabled
    }

    fun setEnabled(enabled: Boolean) {
        eqEnabled = enabled
    }

    fun setPreampGainDb(preampDb: Float) {
        val clamped = preampDb.coerceIn(-12f, 12f)
        preampLinearGain = Math.pow(10.0, clamped / 20.0)
    }

    fun setBassBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        bassBoostExtraDb = (clamped / 100f) * 6.0f
        recomputeCoefficients()
    }

    fun setStereoWidthPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        stereoWidthFactor = (clamped / 100.0) * 0.85
    }

    fun setBandGains(gainsDb: List<Float>) {
        val normalized = EqSpecification.normalizeGains(gainsDb)
        currentGainsDb = normalized
        recomputeCoefficients()
    }

    private fun recomputeCoefficients() {
        val sr = if (inputAudioFormat.sampleRate > 0) inputAudioFormat.sampleRate else 44100
        val effectiveGains = currentGainsDb.mapIndexed { index, gain ->
            val extra = when (index) {
                0 -> bassBoostExtraDb
                1 -> bassBoostExtraDb * 0.65f
                else -> 0f
            }
            EqSpecification.clampGainDb(gain + extra)
        }
        coefficients = BiquadEqDsp.calculateTenBandCoefficients(sr, effectiveGains)
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        z1State = Array(EqSpecification.BAND_COUNT) { DoubleArray(channels) }
        z2State = Array(EqSpecification.BAND_COUNT) { DoubleArray(channels) }
        if (reusableFrameSamples.size < channels) {
            reusableFrameSamples = DoubleArray(channels)
        }
        recomputeCoefficients()
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return outputAudioFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (internalBuffer.capacity() < remaining) {
            internalBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            internalBuffer.clear()
        }

        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val activeCoeffs = coefficients
        val applyEq = eqEnabled
        val preamp = preampLinearGain
        val width = stereoWidthFactor

        val frameCount = remaining / (2 * channels)
        val analysisWindowSize = frameCount.coerceAtMost(reusableMonoAnalysisBuffer.size)
        val monoAnalysisBuffer = if (analysisWindowSize == reusableMonoAnalysisBuffer.size) {
            reusableMonoAnalysisBuffer
        } else {
            FloatArray(analysisWindowSize)
        }
        if (reusableFrameSamples.size < channels) {
            reusableFrameSamples = DoubleArray(channels)
        }
        val frameSamples = reusableFrameSamples
        var broadbandEnergySum = 0.0
        var maxPeakAbs = 0.0

        for (frameIdx in 0 until frameCount) {
            var frameMonoSum = 0.0
            for (ch in 0 until channels) {
                val rawShort = inputBuffer.short
                frameSamples[ch] = processChannelSample(
                    rawSample = (rawShort / 32768.0) * preamp,
                    channel = ch,
                    applyEq = applyEq,
                    activeCoeffs = activeCoeffs
                )
            }
            if (applyEq && channels == 2 && width > 0.01) {
                val mid = 0.5 * (frameSamples[0] + frameSamples[1])
                val side = 0.5 * (frameSamples[0] - frameSamples[1]) * (1.0 + width)
                frameSamples[0] = BiquadEqDsp.softClipLimiter(mid + side)
                frameSamples[1] = BiquadEqDsp.softClipLimiter(mid - side)
            }
            for (ch in 0 until channels) {
                val processedSample = frameSamples[ch]
                broadbandEnergySum += processedSample * processedSample
                val absSample = abs(processedSample)
                if (absSample > maxPeakAbs) {
                    maxPeakAbs = absSample
                }
                frameMonoSum += processedSample
                val dithered = processedSample * 32767.0 + nextTpdfDitherLsb()
                val outShort = dithered.toInt().coerceIn(-32768, 32767).toShort()
                internalBuffer.putShort(outShort)
            }
            if (frameIdx < analysisWindowSize) {
                monoAnalysisBuffer[frameIdx] = (frameMonoSum / channels).toFloat()
            }
        }

        if (frameCount > 0) {
            emitTelemetrySnapshot(
                monoAnalysisBuffer = monoAnalysisBuffer,
                frameCount = frameCount,
                channels = channels,
                broadbandEnergySum = broadbandEnergySum,
                maxPeakAbs = maxPeakAbs
            )
        }

        internalBuffer.flip()
        outputBuffer = internalBuffer
    }

    /**
     * Generates zero-mean Triangular Probability Density Function (TPDF) dither in [-0.5, +0.5] LSB
     * using a lock-free xorshift64* generator to eliminate 16-bit quantization distortion.
     */
    private fun nextTpdfDitherLsb(): Double {
        var x = ditherState
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        val u1 = ((x and 0xFFFFFFL).toDouble() / 16777216.0) - 0.5
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        ditherState = x
        val u2 = ((x and 0xFFFFFFL).toDouble() / 16777216.0) - 0.5
        return (u1 + u2) * 0.5
    }

    private fun processChannelSample(
        rawSample: Double,
        channel: Int,
        applyEq: Boolean,
        activeCoeffs: List<BiquadCoefficients>
    ): Double {
        var sample = rawSample
        if (applyEq) {
            for (bandIdx in 0 until EqSpecification.BAND_COUNT) {
                val c = activeCoeffs[bandIdx]
                val y = c.b0 * sample + z1State[bandIdx][channel]
                z1State[bandIdx][channel] = c.b1 * sample - c.a1 * y + z2State[bandIdx][channel]
                z2State[bandIdx][channel] = c.b2 * sample - c.a2 * y
                sample = y
            }
        }
        return BiquadEqDsp.softClipLimiter(sample)
    }

    private fun emitTelemetrySnapshot(
        monoAnalysisBuffer: FloatArray,
        frameCount: Int,
        channels: Int,
        broadbandEnergySum: Double,
        maxPeakAbs: Double
    ) {
        if (isLowBatteryMode) {
            telemetryThrottleCounter++
            if (telemetryThrottleCounter % 8 != 0) {
                return
            }
        }
        val sampleRate = if (inputAudioFormat.sampleRate > 0) inputAudioFormat.sampleRate else 44100
        val totalSamples = (frameCount * channels).coerceAtLeast(1)
        val rms = sqrt(broadbandEnergySum / totalSamples)
        val rmsDb = if (rms <= 1e-6) -60f else (20.0 * log10(rms)).toFloat().coerceIn(-60f, 0f)
        val peakDb = if (maxPeakAbs <= 1e-6) -60f else (20.0 * log10(maxPeakAbs)).toFloat().coerceIn(-60f, 0f)

        val goertzelBandsDb = BiquadEqDsp.computeTenBandSpectrumDb(
            samples = monoAnalysisBuffer,
            sampleRateHz = sampleRate
        )

        val updatedBands = List(EqSpecification.BAND_COUNT) { idx ->
            val target = goertzelBandsDb[idx]
            val prev = smoothedSpectrum[idx]
            // Fast attack (0.65), smooth release (0.32)
            val next = if (target > prev) {
                prev + (target - prev) * 0.65f
            } else {
                prev + (target - prev) * 0.32f
            }.coerceIn(-60f, 0f)
            smoothedSpectrum[idx] = next

            // Ballistic peak hold with gentle decay
            val currentPeak = max(next, peakHoldSpectrum[idx] - 0.55f).coerceIn(-60f, 0f)
            peakHoldSpectrum[idx] = currentPeak
            next
        }

        val updatedPeaks = List(EqSpecification.BAND_COUNT) { idx -> peakHoldSpectrum[idx] }
        val waveformPoints = BiquadEqDsp.downsampleWaveform(
            samples = monoAnalysisBuffer,
            pointCount = EqSpecification.WAVEFORM_POINT_COUNT
        )

        _spectrumBandsDb.value = updatedBands
        _visualizerSnapshot.value = AudioVisualizerSnapshot(
            spectrumBandsDb = updatedBands,
            spectrumPeaksDb = updatedPeaks,
            waveformSamples = waveformPoints,
            rmsLevelDb = rmsDb,
            peakLevelDb = peakDb
        )
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        for (b in 0 until EqSpecification.BAND_COUNT) {
            z1State[b].fill(0.0)
            z2State[b].fill(0.0)
            smoothedSpectrum[b] = -60f
            peakHoldSpectrum[b] = -60f
        }
    }

    override fun reset() {
        flush()
        internalBuffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}
