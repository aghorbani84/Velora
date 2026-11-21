package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.QueueMode
import com.example.domain.model.Track
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraAlphaTokens
import com.example.ui.theme.VeloraElevation
import com.example.ui.theme.VeloraMotionTokens
import com.example.ui.theme.mixedScriptText
import com.example.ui.theme.veloraColors
import com.example.ui.viewmodel.PlaybackPositionState
import java.util.Locale

val Track.formatBadge: String
    get() = uri.substringBefore('?').substringAfterLast('.', "PCM").uppercase(Locale.ROOT)

val Track.formattedDuration: String
    get() = durationFormatted

@Composable
fun VeloraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tag: String? = null
) {
    val taggedModifier = if (tag != null) modifier.testTag(tag) else modifier
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8),
        modifier = taggedModifier.minimumInteractiveComponentSize()
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(PhiTokens.IconSize20))
            Spacer(modifier = Modifier.width(PhiTokens.Space8))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VeloraSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    tag: String? = null
) {
    val semantic = MaterialTheme.veloraColors
    val taggedModifier = if (tag != null) modifier.testTag(tag) else modifier
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.FeedbackMs,
            easing = VeloraMotionTokens.TouchResponseEasing
        ),
        label = "secondary_btn_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.FeedbackMs,
            easing = VeloraMotionTokens.TouchResponseEasing
        ),
        label = "secondary_btn_fg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA)
            semantic.isDark -> semantic.surfaceCard
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.FeedbackMs,
            easing = VeloraMotionTokens.TouchResponseEasing
        ),
        label = "secondary_btn_border"
    )
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        border = BorderStroke(PhiTokens.BorderHairline, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8),
        modifier = taggedModifier.minimumInteractiveComponentSize()
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(PhiTokens.IconSize20)
            )
            Spacer(modifier = Modifier.width(PhiTokens.Space8))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun VeloraLargeTitleHeader(
    title: String,
    eyebrow: String? = null,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PhiTokens.Space4)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!eyebrow.isNullOrBlank()) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.veloraColors.accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(PhiTokens.Space2))
            }
            Text(
                text = mixedScriptText(title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.veloraColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(PhiTokens.Space4))
                Text(
                    text = mixedScriptText(subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.veloraColors.textMuted
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(PhiTokens.Space13))
            action()
        }
    }
}

@Composable
fun VeloraSectionCard(
    eyebrow: String? = null,
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val semantic = MaterialTheme.veloraColors
    val cardShape = RoundedCornerShape(PhiTokens.CornerRadius21)
    val needsEdgeBorder = !semantic.isDark || semantic.accent == MaterialTheme.colorScheme.onBackground
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (needsEdgeBorder) {
                    Modifier.border(
                        width = PhiTokens.BorderHairline,
                        color = MaterialTheme.colorScheme.outline,
                        shape = cardShape
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PhiTokens.Space21)
        ) {
            if (eyebrow != null || title != null || headerAction != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (eyebrow != null) {
                            Text(
                                text = eyebrow,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (title != null) {
                            Text(
                                text = mixedScriptText(title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(PhiTokens.Space4))
                            Text(
                                text = mixedScriptText(subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (headerAction != null) {
                        Spacer(modifier = Modifier.width(PhiTokens.Space8))
                        headerAction()
                    }
                }
                Spacer(modifier = Modifier.height(PhiTokens.Space13))
            }
            content()
        }
    }
}

@Composable
fun VeloraEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    actionTag: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = modifier
            .fillMaxWidth()
            .border(
                PhiTokens.BorderHairline,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(PhiTokens.CornerRadius21)
            )
            .testTag("velora_empty_state")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(PhiTokens.Space34)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(PhiTokens.Space55)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PhiTokens.IconSize34)
                )
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space13))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(PhiTokens.Space21))
                VeloraPrimaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    tag = actionTag
                )
            }
        }
    }
}

@Composable
fun VeloraLoadingSkeleton(
    rowCount: Int = 4,
    label: String = "Indexing media...",
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "velora_skeleton")
    val pulseAlpha by transition.animateFloat(
        initialValue = if (reducedMotion) 0.60f else 0.38f,
        targetValue = if (reducedMotion) 0.60f else 0.84f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = VeloraMotionTokens.SkeletonPulseMs,
                easing = VeloraMotionTokens.ArrivalEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val titleFractions = remember { listOf(0.62f, 0.48f, 0.72f, 0.54f) }
    val subtitleFractions = remember { listOf(0.42f, 0.34f, 0.50f, 0.38f) }
    val semantic = MaterialTheme.veloraColors

    Column(
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
        modifier = modifier
            .fillMaxWidth()
            .testTag("velora_loading_skeleton")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        repeat(rowCount) { index ->
            val titleWidth = titleFractions[index % titleFractions.size]
            val subWidth = subtitleFractions[index % subtitleFractions.size]
            Surface(
                color = semantic.bgSecondary,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(pulseAlpha)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                    modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                ) {
                    Box(
                        modifier = Modifier
                            .size(PhiTokens.AvatarSize48)
                            .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                            .background(semantic.surfaceCard)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(titleWidth)
                                .height(PhiTokens.Space13)
                                .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                                .background(semantic.surfaceCard)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(subWidth)
                                .height(PhiTokens.Space8)
                                .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                                .background(semantic.surfaceWell)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(PhiTokens.Space34)
                            .height(PhiTokens.Space13)
                            .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                            .background(semantic.surfaceCard)
                    )
                }
            }
        }
    }
}

@Composable
fun VeloraErrorBanner(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = VeloraAlphaTokens.ERROR_SURFACE_ALPHA),
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier
            .fillMaxWidth()
            .border(
                PhiTokens.BorderHairline,
                MaterialTheme.colorScheme.error,
                RoundedCornerShape(PhiTokens.CornerRadius13)
            )
            .testTag("velora_error_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(PhiTokens.Space13)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(PhiTokens.Space8))
            VeloraSecondaryButton(
                text = actionLabel,
                onClick = onAction,
                tag = "error_banner_action_button"
            )
        }
    }
}

@Composable
fun VeloraSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    sliderTestTag: String = "player_seek_slider",
    loopStartMs: Long? = null,
    loopEndMs: Long? = null
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val effectiveMs = dragFraction?.let { (it * safeDuration).toLong() } ?: positionMs.coerceIn(0L, safeDuration)
    val fraction = (effectiveMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val hasLoopMarkers = durationMs > 0L && (loopStartMs != null || loopEndMs != null)
    val loopAccent = MaterialTheme.colorScheme.primary
    val loopTrackBg = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                val target = ((dragFraction ?: fraction) * safeDuration).toLong()
                dragFraction = null
                onSeekTo(PlaybackQueue.clampSeekPosition(target, safeDuration))
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(sliderTestTag)
        )
        if (hasLoopMarkers) {
            val startFrac = loopStartMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
            val endFrac = loopEndMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhiTokens.Space8)
                    .testTag("seekbar_ab_markers")
            ) {
                val centerY = size.height / 2f
                drawLine(
                    color = loopTrackBg,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = size.height * 0.25f,
                    cap = StrokeCap.Round
                )
                if (startFrac != null && endFrac != null && endFrac > startFrac) {
                    val startX = startFrac * size.width
                    val endX = endFrac * size.width
                    drawRect(
                        color = loopAccent.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA),
                        topLeft = Offset(startX, 0f),
                        size = Size((endX - startX).coerceAtLeast(2f), size.height)
                    )
                }
                startFrac?.let { sf ->
                    val x = sf * size.width
                    drawLine(
                        color = loopAccent,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = size.height * 0.35f,
                        cap = StrokeCap.Round
                    )
                }
                endFrac?.let { ef ->
                    val x = ef * size.width
                    drawLine(
                        color = loopAccent,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = size.height * 0.35f,
                        cap = StrokeCap.Round
                    )
                }
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space4))
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = Track.formatDurationMs(effectiveMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("elapsed_time_label")
            )
            if (hasLoopMarkers) {
                val aLabel = loopStartMs?.let { Track.formatDurationMs(it) } ?: "--:--"
                val bLabel = loopEndMs?.let { Track.formatDurationMs(it) } ?: "--:--"
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                    modifier = Modifier
                        .semantics(mergeDescendants = true) {}
                        .testTag("seekbar_ab_loop_badge")
                ) {
                    Text(
                        text = mixedScriptText("A $aLabel → B $bLabel"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            horizontal = PhiTokens.Space8,
                            vertical = PhiTokens.Space2
                        )
                    )
                }
            }
            Text(
                text = Track.formatDurationMs(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("total_duration_label")
            )
        }
    }
}

@Composable
fun VeloraTransportControls(
    isPlaying: Boolean,
    experienceTier: ExperienceTier,
    queueMode: QueueMode,
    repeatMode: com.example.domain.model.RepeatMode,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val showExtendedModes = experienceTier != ExperienceTier.SIMPLE
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
            .fillMaxWidth()
            .testTag("velora_transport_controls")
    ) {
        if (showExtendedModes) {
            val shuffleOn = queueMode == QueueMode.SHUFFLE
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("transport_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Toggle Shuffle",
                    tint = if (shuffleOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("transport_prev_button")
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous Track",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(PhiTokens.IconSize34)
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            modifier = Modifier
                .size(PhiTokens.Space55)
                .clip(CircleShape)
                .clickable(onClick = onTogglePlayPause)
                .semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
                .testTag("transport_play_pause_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(PhiTokens.IconSize34)
                        .padding(start = if (isPlaying) PhiTokens.Space2 - PhiTokens.Space2 else PhiTokens.Space2)
                )
            }
        }

        IconButton(
            onClick = onNext,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("transport_next_button")
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next Track",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(PhiTokens.IconSize34)
            )
        }

        if (showExtendedModes) {
            val repeatActive = repeatMode != com.example.domain.model.RepeatMode.OFF
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("transport_repeat_button")
            ) {
                Icon(
                    imageVector = if (repeatMode == com.example.domain.model.RepeatMode.ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    },
                    contentDescription = "Cycle Repeat Mode",
                    tint = if (repeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TrackRowItem(
    track: Track,
    isActive: Boolean,
    playlists: List<Playlist>,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val semantic = MaterialTheme.veloraColors
    val rowBg by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.FeedbackMs,
            easing = VeloraMotionTokens.TouchResponseEasing
        ),
        label = "track_row_bg"
    )
    val titleColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.FeedbackMs,
            easing = VeloraMotionTokens.TouchResponseEasing
        ),
        label = "track_row_title"
    )
    val rowShape = RoundedCornerShape(PhiTokens.CornerRadius13)
    val showBorder = isActive || !semantic.isDark

    Surface(
        color = rowBg,
        shape = rowShape,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = PhiTokens.BorderHairline,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA)
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = rowShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onPlay)
            .testTag("track_row_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(PhiTokens.AvatarSize48)
                    .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                    .background(
                        if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Icon(
                    imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(PhiTokens.Space13))

            Column(
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space2),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = mixedScriptText(track.title),
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mixedScriptText("${track.artist} • ${track.album}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.formatBadge} • ${track.sampleRateHz / 1000.0}kHz • ${track.formattedDuration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (onToggleFavorite != null) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("favorite_track_${track.id}")
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (track.isFavorite) "Remove Favorite" else "Add Favorite",
                        tint = if (track.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            IconButton(
                onClick = onAddToQueue,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("queue_track_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.AddToQueue,
                    contentDescription = "Add to Queue",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (playlists.isNotEmpty()) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("playlist_menu_track_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                onClick = {
                                    menuExpanded = false
                                    onAddToPlaylist(playlist.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    engineState: EnginePlaybackState,
    positionState: PlaybackPositionState = PlaybackPositionState(
        positionMs = engineState.positionMs,
        durationMs = engineState.durationMs,
        isPlaying = engineState.isPlaying
    ),
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = engineState.currentTrack ?: return
    val semantic = MaterialTheme.veloraColors
    val progress = if (positionState.durationMs > 0L) {
        (positionState.positionMs.toFloat() / positionState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        color = semantic.surfaceRaised,
        shadowElevation = VeloraElevation.Level3,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space4)
            .border(
                PhiTokens.BorderHairline,
                semantic.accent.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA),
                RoundedCornerShape(PhiTokens.CornerRadius21)
            )
            .clickable(onClick = onOpenPlayer)
            .testTag("mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                color = semantic.accent,
                trackColor = semantic.bgSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(PhiTokens.AvatarSize48)
                        .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(semantic.accentContainer, semantic.surfaceCard)
                            )
                        )
                ) {
                    Icon(
                        imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = semantic.accent
                    )
                }
                Spacer(modifier = Modifier.width(PhiTokens.Space13))
                Column(
                    verticalArrangement = Arrangement.spacedBy(PhiTokens.Space2),
                    modifier = Modifier.weight(1f)
                ) {
                    VeloraRollingLabel(
                        text = mixedScriptText(track.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = semantic.textPrimary
                    )
                    Text(
                        text = mixedScriptText(
                            "${track.artist} • ${Track.formatDurationMs(positionState.positionMs)}"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = semantic.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (positionState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (positionState.isPlaying) "Pause" else "Play",
                        tint = semantic.accent
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Next",
                        tint = semantic.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun VeloraHorizontalMediaShelf(
    title: String,
    subtitle: String? = null,
    tracks: List<Track>,
    activeTrackId: String?,
    onPlayTrack: (Track) -> Unit,
    actionLabel: String? = null,
    actionTag: String? = null,
    onAction: (() -> Unit)? = null,
    shelfTag: String? = null,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.veloraColors
    Column(
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = (if (shelfTag != null) modifier.testTag(shelfTag) else modifier).fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mixedScriptText(title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = semantic.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = mixedScriptText(subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.textMuted
                    )
                }
            }
            if (actionLabel != null && onAction != null) {
                VeloraSecondaryButton(
                    text = actionLabel,
                    onClick = onAction,
                    tag = actionTag
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
            items(tracks, key = { it.id }) { track ->
                VeloraMediaShelfCard(
                    track = track,
                    isActive = track.id == activeTrackId,
                    onClick = { onPlayTrack(track) }
                )
            }
        }
    }
}

@Composable
fun VeloraMediaShelfCard(
    track: Track,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.veloraColors
    val cardColor by animateColorAsState(
        targetValue = if (isActive) semantic.accentContainer else semantic.surfaceCard,
        animationSpec = tween(
            durationMillis = VeloraMotionTokens.FeedbackMs,
            easing = VeloraMotionTokens.TouchResponseEasing
        ),
        label = "shelf_card_bg"
    )
    val cardShape = RoundedCornerShape(PhiTokens.CornerRadius13)
    Surface(
        color = cardColor,
        shape = cardShape,
        modifier = modifier
            .width(PhiTokens.ShelfCardWidth)
            .then(
                if (isActive || !semantic.isDark) {
                    Modifier.border(
                        width = PhiTokens.BorderHairline,
                        color = if (isActive) {
                            semantic.accent.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA)
                        } else {
                            semantic.divider
                        },
                        shape = cardShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag("shelf_card_${track.id}")
    ) {
        Column(
            modifier = Modifier.padding(PhiTokens.Space13)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(PhiTokens.ShelfCardArtSize)
                    .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                    .background(
                        Brush.linearGradient(
                            colors = if (isActive) {
                                listOf(semantic.accent, semantic.accentDeep)
                            } else {
                                listOf(semantic.surfaceElevated, semantic.bgSecondary)
                            }
                        )
                    )
            ) {
                Icon(
                    imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else semantic.accent,
                    modifier = Modifier.size(PhiTokens.IconSize34)
                )
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            Text(
                text = mixedScriptText(track.title),
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) semantic.accentBright else semantic.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mixedScriptText(track.artist),
                style = MaterialTheme.typography.bodySmall,
                color = semantic.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(PhiTokens.Space4))
            Text(
                text = "${track.formatBadge} • ${track.formattedDuration}",
                style = MaterialTheme.typography.labelSmall,
                color = semantic.textMuted,
                maxLines = 1
            )
        }
    }
}

