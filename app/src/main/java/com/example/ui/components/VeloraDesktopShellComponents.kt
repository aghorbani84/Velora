package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.ui.navigation.VeloraDestination
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraAlphaTokens
import com.example.ui.theme.VeloraElevation
import com.example.ui.theme.mixedScriptText
import com.example.ui.theme.veloraColors
import com.example.ui.viewmodel.PlaybackPositionState

@Composable
fun VeloraDesktopSidebar(
    destinations: List<VeloraDestination>,
    currentDestination: VeloraDestination,
    playlists: List<Playlist>,
    tier: ExperienceTier,
    isPersian: Boolean,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onNavigate: (VeloraDestination) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = MaterialTheme.veloraColors
    val width = if (collapsed) PhiTokens.SidebarCollapsedWidth else PhiTokens.SidebarExpandedWidth

    Surface(
        color = semantic.bgSecondary,
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .animateContentSize()
            .testTag("desktop_sidebar")
    ) {
        Row(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = PhiTokens.Space8, vertical = PhiTokens.Space13)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!collapsed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = semantic.accent,
                                modifier = Modifier.size(PhiTokens.IconSize24)
                            )
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(
                                text = "VELORA",
                                style = MaterialTheme.typography.headlineMedium,
                                color = semantic.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("desktop_sidebar_collapse_toggle")
                    ) {
                        Icon(
                            imageVector = if (collapsed) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                            contentDescription = if (collapsed) "Expand Sidebar" else "Collapse Sidebar",
                            tint = semantic.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PhiTokens.Space8))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(PhiTokens.Space4),
                    modifier = Modifier.weight(1f)
                ) {
                    items(destinations, key = { it.route }) { dest ->
                        val selected = dest == currentDestination
                        val label = if (isPersian) dest.labelFa else dest.label
                        SidebarNavItem(
                            label = label,
                            icon = dest.icon,
                            selected = selected,
                            collapsed = collapsed,
                            onClick = { onNavigate(dest) },
                            tag = "nav_${dest.route}"
                        )
                    }

                    if (!collapsed && tier != ExperienceTier.SIMPLE && playlists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(PhiTokens.Space13))
                            HorizontalDivider(color = semantic.divider)
                            Spacer(modifier = Modifier.height(PhiTokens.Space13))
                            Text(
                                text = if (isPersian) "پلی‌لیست‌ها" else "PLAYLISTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.textMuted,
                                modifier = Modifier.padding(horizontal = PhiTokens.Space8)
                            )
                            Spacer(modifier = Modifier.height(PhiTokens.Space4))
                        }
                        items(playlists, key = { "sidebar_pl_${it.id}" }) { playlist ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                                    .clickable { onPlayPlaylist(playlist) }
                                    .padding(horizontal = PhiTokens.Space8, vertical = PhiTokens.Space8)
                                    .testTag("sidebar_playlist_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = semantic.accentGold,
                                    modifier = Modifier.size(PhiTokens.IconSize16)
                                )
                                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                                Text(
                                    text = mixedScriptText(playlist.name),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .width(PhiTokens.BorderHairline)
                    .fillMaxHeight()
                    .background(semantic.divider)
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    collapsed: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val semantic = MaterialTheme.veloraColors
    Surface(
        color = if (selected) semantic.accentContainer else semantic.bgSecondary,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .then(
                if (selected) {
                    Modifier.border(
                        PhiTokens.BorderHairline,
                        semantic.accent.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA),
                        RoundedCornerShape(PhiTokens.CornerRadius13)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
            modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) semantic.accent else semantic.textSecondary,
                modifier = Modifier.size(PhiTokens.IconSize20)
            )
            if (!collapsed) {
                Spacer(modifier = Modifier.width(PhiTokens.Space13))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) semantic.accentBright else semantic.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DesktopPlayerBar(
    engineState: EnginePlaybackState,
    positionState: PlaybackPositionState,
    queueMode: QueueMode,
    repeatMode: RepeatMode,
    tier: ExperienceTier,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = engineState.currentTrack
    val semantic = MaterialTheme.veloraColors
    val safeDuration = positionState.durationMs.coerceAtLeast(1L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val effectiveMs = dragFraction?.let { (it * safeDuration).toLong() }
        ?: positionState.positionMs.coerceIn(0L, safeDuration)
    val fraction = (effectiveMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Surface(
        color = semantic.surfaceElevated,
        shadowElevation = VeloraElevation.Level3,
        modifier = modifier
            .fillMaxWidth()
            .border(
                PhiTokens.BorderHairline,
                semantic.accent.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA)
            )
            .testTag("desktop_player_bar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PhiTokens.Space21, vertical = PhiTokens.Space8)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(0.28f)
                    .clickable(onClick = onOpenNowPlaying)
                    .testTag("desktop_player_track_info")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(PhiTokens.AvatarSize48)
                        .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                        .background(semantic.accentContainer)
                ) {
                    Icon(
                        imageVector = if (track?.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = semantic.accent
                    )
                }
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mixedScriptText(track?.title ?: "VELORA Ready"),
                        style = MaterialTheme.typography.titleMedium,
                        color = semantic.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mixedScriptText(track?.artist ?: "Select media to start"),
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(0.44f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space4)
                ) {
                    if (tier != ExperienceTier.SIMPLE) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Toggle Shuffle",
                                tint = if (queueMode == QueueMode.SHUFFLE) semantic.accent else semantic.textMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = semantic.textPrimary
                        )
                    }
                    Surface(
                        color = semantic.accent,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(PhiTokens.AvatarSize48)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("desktop_player_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (positionState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (positionState.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("desktop_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = semantic.textPrimary
                        )
                    }
                    if (tier != ExperienceTier.SIMPLE) {
                        IconButton(
                            onClick = onCycleRepeat,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (repeatMode != RepeatMode.OFF) semantic.accent else semantic.textMuted
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Track.formatDurationMs(effectiveMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.textSecondary
                    )
                    Slider(
                        value = fraction,
                        onValueChange = { dragFraction = it },
                        onValueChangeFinished = {
                            val target = ((dragFraction ?: fraction) * safeDuration).toLong()
                            dragFraction = null
                            onSeekTo(PlaybackQueue.clampSeekPosition(target, safeDuration))
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = semantic.accent,
                            activeTrackColor = semantic.accent,
                            inactiveTrackColor = semantic.divider
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("desktop_seek_slider")
                    )
                    Text(
                        text = Track.formatDurationMs(positionState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.textMuted
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(0.28f)
            ) {
                IconButton(
                    onClick = onOpenNowPlaying,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("desktop_open_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Open Now Playing & EQ",
                        tint = semantic.accent
                    )
                }
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (engineState.isMuted) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = if (engineState.isMuted) "Unmute" else "Mute",
                        tint = semantic.textSecondary
                    )
                }
                Slider(
                    value = if (engineState.isMuted) 0f else engineState.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = semantic.accent,
                        activeTrackColor = semantic.accent,
                        inactiveTrackColor = semantic.divider
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("desktop_volume_slider")
                )
            }
        }
    }
}
