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
    private var currentGainsDb: List<Float> = List(EqSpecification.BAND_COUNT) { 0f }

    @Volatile
    private var coefficients: List<BiquadCoefficients> =
        BiquadEqDsp.calculateTenBandCoefficients(44100, currentGainsDb)

    private var z1State: Array<DoubleArray> = Array(EqSpecification.BAND_COUNT) { DoubleArray(2) }
    private var z2State: Array<DoubleArray> = Array(EqSpecification.BAND_COUNT) { DoubleArray(2) }

    private val smoothedSpectrum = FloatArray(EqSpecification.BAND_COUNT) { -60f }
    private val peakHoldSpectrum = FloatArray(EqSpecification.BAND_COUNT) { -60f }

    private val _spectrumBandsDb = MutableStateFlow(List(EqSpecification.BAND_COUNT) { -60f })
    val spectrumBandsDb: StateFlow<List<Float>> = _spectrumBandsDb.asStateFlow()

    private val _visualizerSnapshot = MutableStateFlow(AudioVisualizerSnapshot())
    val visualizerSnapshot: StateFlow<AudioVisualizerSnapshot> = _visualizerSnapshot.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        eqEnabled = enabled
    }

    fun setBandGains(gainsDb: List<Float>) {
        val normalized = EqSpecification.normalizeGains(gainsDb)
        currentGainsDb = normalized
        val sr = if (inputAudioFormat.sampleRate > 0) inputAudioFormat.sampleRate else 44100
        coefficients = BiquadEqDsp.calculateTenBandCoefficients(sr, normalized)
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
        coefficients = BiquadEqDsp.calculateTenBandCoefficients(
            sampleRateHz = inputAudioFormat.sampleRate,
            gainsDb = currentGainsDb
        )
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

        val frameCount = remaining / (2 * channels)
        val analysisWindowSize = frameCount.coerceAtMost(512)
        val monoAnalysisBuffer = FloatArray(analysisWindowSize)
        var broadbandEnergySum = 0.0
        var maxPeakAbs = 0.0

        for (frameIdx in 0 until frameCount) {
            var frameMonoSum = 0.0
            for (ch in 0 until channels) {
                val rawShort = inputBuffer.short
                val processedSample = processChannelSample(
                    rawSample = rawShort / 32768.0,
                    channel = ch,
                    applyEq = applyEq,
                    activeCoeffs = activeCoeffs
                )
                broadbandEnergySum += processedSample * processedSample
                val absSample = abs(processedSample)
                if (absSample > maxPeakAbs) {
                    maxPeakAbs = absSample
                }
                frameMonoSum += processedSample
                val outShort = (processedSample * 32767.0).toInt().toShort()
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
        return sample.coerceIn(-1.0, 1.0)
    }

    private fun emitTelemetrySnapshot(
        monoAnalysisBuffer: FloatArray,
        frameCount: Int,
        channels: Int,
        broadbandEnergySum: Double,
        maxPeakAbs: Double
    ) {
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
