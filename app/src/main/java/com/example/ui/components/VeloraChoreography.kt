package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.theme.VeloraMotionTokens

/**
 * Stageshow-translated Jetpack Compose choreography primitives for VELORA.
 *
 * Principles enforced:
 * - Retained frame: outer chrome and controls hold steady while only variable body content swaps.
 * - Causal two-curve motion: Travel (0.45, 0, 0.1, 1) and Arrival (0.25, 0.8, 0.25, 1), never bounce.
 * - Never slice ink: vertical expansion sequences cleanly with opacity fades.
 */
@Composable
fun <T> VeloraRetainedContentSwap(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "velora_retained_swap",
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            retainedSwapTransform()
        },
        label = label,
        modifier = modifier
    ) { state ->
        content(state)
    }
}

private fun retainedSwapTransform(): ContentTransform {
    val enter = fadeIn(
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.ContentSwapMs,
            easing = VeloraMotionTokens.ArrivalEasing
        )
    ) + slideInVertically(
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.ContentSwapMs,
            easing = VeloraMotionTokens.ArrivalEasing
        ),
        initialOffsetY = { fullHeight -> (fullHeight / 28).coerceIn(8, 24) }
    )
    val exit = fadeOut(
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.FeedbackMs,
            easing = VeloraMotionTokens.TravelEasing
        )
    )
    return enter togetherWith exit
}

/**
 * Smooth vertical reveal (Stageshow Collapse translation) for banners, HUD notices, and markers.
 */
@Composable
fun VeloraAnimatedReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String = "velora_reveal",
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(
                durationMillis = VeloraMotionTokens.ContentSwapMs,
                easing = VeloraMotionTokens.ArrivalEasing
            ),
            expandFrom = Alignment.Top
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = VeloraMotionTokens.FeedbackMs,
                easing = VeloraMotionTokens.ArrivalEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = VeloraMotionTokens.FeedbackMs,
                easing = VeloraMotionTokens.TravelEasing
            )
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = VeloraMotionTokens.ContentSwapMs,
                easing = VeloraMotionTokens.TravelEasing
            ),
            shrinkTowards = Alignment.Top
        ),
        label = label,
        modifier = modifier,
        content = content
    )
}

/**
 * Smooth in-place rolling text transition (Stageshow Roll translation) for changing labels and track titles.
 */
@Composable
fun VeloraRollingLabel(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val enter = fadeIn(
                animationSpec = tween(
                    durationMillis = VeloraMotionTokens.FeedbackMs,
                    easing = VeloraMotionTokens.ArrivalEasing
                )
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = VeloraMotionTokens.FeedbackMs,
                    easing = VeloraMotionTokens.TravelEasing
                ),
                initialOffsetY = { height -> (height / 3).coerceAtLeast(4) }
            )
            val exit = fadeOut(
                animationSpec = tween(
                    durationMillis = VeloraMotionTokens.FeedbackMs,
                    easing = VeloraMotionTokens.TravelEasing
                )
            ) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = VeloraMotionTokens.FeedbackMs,
                    easing = VeloraMotionTokens.TravelEasing
                ),
                targetOffsetY = { height -> -(height / 3).coerceAtLeast(4) }
            )
            enter togetherWith exit
        },
        label = "velora_rolling_label",
        modifier = modifier
    ) { targetText ->
        Text(
            text = targetText,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}
