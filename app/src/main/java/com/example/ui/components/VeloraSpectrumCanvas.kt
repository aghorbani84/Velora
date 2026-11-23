package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.VisualizerMode
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraAlphaTokens
import kotlin.math.PI
import kotlin.math.sin

/**
 * 10-Band Goertzel Real-Time Spectrum & Live Biquad Transfer Curve Visualizer.
 * Uses only theme color tokens and PhiTokens sizing.
 */
@Composable
fun SpectrumAnalyzerCanvas(
    spectrumBands: List<Float>,
    eqBandGainsDb: List<Float>,
    isPlaying: Boolean,
    visualizerMode: VisualizerMode = VisualizerMode.SPECTRUM_BARS,
    spectrumHeight: Dp = PhiTokens.SpectrumHeight,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline

    val transition = rememberInfiniteTransition(label = "spectrum_phase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val transferCurveDb = remember(eqBandGainsDb) {
        BiquadEqDsp.evaluateCascadeMagnitudeResponseDb(
            sampleRateHz = 48_000,
            gainsDb = eqBandGainsDb,
            pointCount = 32
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(spectrumHeight)
            .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
            .background(surfaceColor)
            .border(
                PhiTokens.BorderHairline,
                outlineColor,
                RoundedCornerShape(PhiTokens.CornerRadius13)
            )
            .testTag("spectrum_analyzer_canvas")
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        drawLine(
            color = outlineColor,
            start = Offset(0f, height * 0.5f),
            end = Offset(width, height * 0.5f),
            strokeWidth = 1f
        )

        val bands = if (spectrumBands.isNotEmpty()) spectrumBands else List(10) { 0.12f }
        when (visualizerMode) {
            VisualizerMode.WAVEFORM_OSCILLOSCOPE -> {
                val wavePath = Path()
                val steps = 48
                for (i in 0..steps) {
                    val x = (i.toFloat() / steps.toFloat()) * width
                    val bandIdx = ((i.toFloat() / steps) * (bands.size - 1)).toInt().coerceIn(0, bands.lastIndex)
                    val amp = if (isPlaying) bands[bandIdx].coerceIn(0.08f, 1f) else 0.08f
                    val y = height * 0.5f + sin(phase + i * 0.35f) * (height * 0.38f * amp)
                    if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                }
                drawPath(
                    path = wavePath,
                    color = primaryColor,
                    style = Stroke(width = 3f)
                )
            }
            VisualizerMode.DUAL_STUDIO, VisualizerMode.SPECTRUM_BARS -> {
                val count = bands.size.coerceAtLeast(1)
                val slotWidth = width / count.toFloat()
                val barWidth = slotWidth * PhiTokens.PRIMARY_WEIGHT
                bands.forEachIndexed { idx, rawLevel ->
                    val animatedFloor = if (isPlaying) {
                        (0.14f + 0.06f * sin(phase + idx * 0.6f)).coerceAtLeast(0.08f)
                    } else {
                        0.06f
                    }
                    val level = rawLevel.coerceAtLeast(animatedFloor).coerceIn(0.05f, 1f)
                    val barHeight = height * 0.82f * level
                    val left = idx * slotWidth + (slotWidth - barWidth) * 0.5f
                    val top = height - barHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                tertiaryColor,
                                primaryColor,
                                secondaryColor.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA)
                            ),
                            startY = top,
                            endY = height
                        ),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }

        if (transferCurveDb.size >= 2) {
            val eqPath = Path()
            transferCurveDb.forEachIndexed { i, db ->
                val x = (i.toFloat() / (transferCurveDb.size - 1).toFloat()) * width
                val normalized = ((db + 15f) / 30f).coerceIn(0f, 1f)
                val y = height * (1f - normalized)
                if (i == 0) eqPath.moveTo(x, y) else eqPath.lineTo(x, y)
            }
            drawPath(
                path = eqPath,
                color = secondaryColor,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
