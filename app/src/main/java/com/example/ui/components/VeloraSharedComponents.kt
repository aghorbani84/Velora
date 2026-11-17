package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.QueueMode
import com.example.domain.model.Track
import com.example.ui.theme.PhiTokens
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
    val taggedModifier = if (tag != null) modifier.testTag(tag) else modifier
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
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
fun VeloraSectionCard(
    eyebrow: String? = null,
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = PhiTokens.BorderHairline,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21)
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
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(PhiTokens.Space4))
                            Text(
                                text = subtitle,
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
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "velora_skeleton")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "skeleton_alpha"
    )
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
        repeat(rowCount) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhiTokens.Space55)
                    .alpha(pulseAlpha)
                    .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
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
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
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
    sliderTestTag: String = "player_seek_slider"
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val effectiveMs = dragFraction?.let { (it * safeDuration).toLong() } ?: positionMs.coerceIn(0L, safeDuration)
    val fraction = (effectiveMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

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
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = Track.formatDurationMs(effectiveMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("elapsed_time_label")
            )
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
                    modifier = Modifier.size(PhiTokens.IconSize34)
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

    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = PhiTokens.BorderHairline,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13)
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.album}",
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
                            imageVector = Icons.Default.PlaylistAdd,
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
    val progress = if (positionState.durationMs > 0L) {
        (positionState.positionMs.toFloat() / positionState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space4)
            .border(
                PhiTokens.BorderHairline,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                RoundedCornerShape(PhiTokens.CornerRadius13)
            )
            .clickable(onClick = onOpenPlayer)
            .testTag("mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} • ${Track.formatDurationMs(positionState.positionMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        imageVector = if (engineState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (engineState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary
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
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
