package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.model.EqSpecification
import com.example.domain.model.MediaType
import com.example.domain.model.Playlist
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldBright
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens

@Composable
fun MiniPlayerBar(
    engineState: EnginePlaybackState,
    repeatMode: RepeatMode,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onPrevious: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = engineState.currentTrack
    val progress = if (engineState.durationMs > 0L) {
        (engineState.positionMs.toFloat() / engineState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        color = FazoraCarbonElevated,
        tonalElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(topStart = PhiTokens.CornerRadius13, topEnd = PhiTokens.CornerRadius13))
            .clip(RoundedCornerShape(topStart = PhiTokens.CornerRadius13, topEnd = PhiTokens.CornerRadius13))
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        if (totalDrag < -75f) {
                            onNext()
                        } else if (totalDrag > 75f) {
                            onPrevious()
                        }
                    }
                )
            }
            .clickable { onOpenFullPlayer() }
            .testTag("mini_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                color = FazoraGold,
                trackColor = FazoraObsidian,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                        .background(FazoraGoldMuted)
                ) {
                    Icon(
                        imageVector = if (track?.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.GraphicEq,
                        contentDescription = stringResource(R.string.cd_open_player),
                        tint = FazoraGold
                    )
                }

                Spacer(modifier = Modifier.width(PhiTokens.Space13))

                Column(
                    modifier = Modifier.weight(PhiTokens.PRIMARY_WEIGHT)
                ) {
                    Text(
                        text = track?.title ?: "Select a track to begin playback",
                        style = MaterialTheme.typography.titleMedium,
                        color = FazoraIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (track != null) {
                            "${track.artist} • ${Track.formatDurationMs(engineState.positionMs)} / ${Track.formatDurationMs(engineState.durationMs)}"
                        } else {
                            stringResource(R.string.tagline)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = FazoraSteel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_repeat_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = stringResource(R.string.cd_repeat_mode),
                            tint = if (repeatMode != RepeatMode.OFF) FazoraGold else FazoraSteel
                        )
                        if (repeatMode == RepeatMode.ONE) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.labelMedium,
                                color = FazoraGoldBright,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(start = 14.dp, bottom = 12.dp)
                                    .testTag("mini_repeat_one_badge")
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (engineState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        tint = FazoraGold
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_track),
                        tint = FazoraIvory
                    )
                }
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
    onToggleFavorite: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPlaylistMenu by remember { mutableStateOf(false) }

    Surface(
        color = if (isActive) FazoraGoldMuted else FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isActive) FazoraGold else FazoraCarbonBorder,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13)
            )
            .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
            .clickable { onPlay() }
            .testTag("track_item_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isActive) FazoraGold else FazoraCarbonElevated)
            ) {
                Icon(
                    imageVector = if (track.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.MusicNote,
                    contentDescription = track.title,
                    tint = if (isActive) FazoraObsidian else FazoraGold
                )
            }

            Spacer(modifier = Modifier.width(PhiTokens.Space13))

            Column(
                modifier = Modifier.weight(PhiTokens.PRIMARY_WEIGHT)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = FazoraIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FazoraSteel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.durationFormatted} • ${track.sampleRateHz / 1000.0}kHz • ${track.bitrateKbps}kbps",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) FazoraGoldBright else FazoraSteel
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("favorite_button_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_toggle_favorite),
                    tint = if (track.isFavorite) FazoraGold else FazoraSteel
                )
            }

            IconButton(
                onClick = onAddToQueue,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_queue_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.AddToQueue,
                    contentDescription = "Add ${track.title} to queue",
                    tint = FazoraIvory
                )
            }

            Box {
                IconButton(
                    onClick = { showPlaylistMenu = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_playlist_${track.id}")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add ${track.title} to playlist",
                        tint = FazoraGold
                    )
                }

                DropdownMenu(
                    expanded = showPlaylistMenu,
                    onDismissRequest = { showPlaylistMenu = false },
                    modifier = Modifier.background(FazoraCarbonElevated)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Play Next (پخش بعدی)", color = FazoraGold, style = MaterialTheme.typography.bodyMedium)
                        },
                        onClick = {
                            onPlayNext()
                            showPlaylistMenu = false
                        }
                    )
                    if (playlists.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Create a playlist in Library first",
                                    color = FazoraSteel,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = { showPlaylistMenu = false }
                        )
                    } else {
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Add to ${playlist.name}",
                                        color = FazoraIvory,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    onAddToPlaylist(playlist.id)
                                    showPlaylistMenu = false
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
fun SpectrumAnalyzerCanvas(
    spectrumBandsDb: List<Float>,
    eqGainsDb: List<Float>,
    eqEnabled: Boolean,
    spectrumPeaksDb: List<Float> = List(EqSpecification.BAND_COUNT) { -60f },
    waveformSamples: List<Float> = List(EqSpecification.WAVEFORM_POINT_COUNT) { 0f },
    visualizerMode: com.example.domain.model.VisualizerMode = com.example.domain.model.VisualizerMode.DUAL_STUDIO,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
                .background(FazoraObsidian)
                .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
                .padding(PhiTokens.Space8)
                .testTag("eq_spectrum_canvas")
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val quarterY = h * 0.25f
            val midY = h * 0.5f
            val threeQuarterY = h * 0.75f

            // Horizontal reference dB grid lines (-15dB, -30dB / 0dB EQ center, -45dB)
            drawLine(
                color = FazoraCarbonBorder.copy(alpha = 0.45f),
                start = Offset(0f, quarterY),
                end = Offset(w, quarterY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = FazoraCarbonBorder,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = FazoraCarbonBorder.copy(alpha = 0.45f),
                start = Offset(0f, threeQuarterY),
                end = Offset(w, threeQuarterY),
                strokeWidth = 1.dp.toPx()
            )

            val bandCount = EqSpecification.BAND_COUNT
            val slotWidth = w / bandCount
            val barWidth = slotWidth * 0.56f

            val drawSpectrumBars =
                visualizerMode == com.example.domain.model.VisualizerMode.SPECTRUM_BARS ||
                    visualizerMode == com.example.domain.model.VisualizerMode.DUAL_STUDIO

            val drawWaveform =
                visualizerMode == com.example.domain.model.VisualizerMode.WAVEFORM_OSCILLOSCOPE ||
                    visualizerMode == com.example.domain.model.VisualizerMode.DUAL_STUDIO

            if (drawSpectrumBars) {
                for (i in 0 until bandCount) {
                    val db = spectrumBandsDb.getOrElse(i) { -60f }.coerceIn(-60f, 0f)
                    val normalizedHeight = ((db + 60f) / 60f).coerceIn(0.05f, 1.0f)
                    val barHeight = h * normalizedHeight
                    val left = i * slotWidth + (slotWidth - barWidth) * 0.5f
                    val top = h - barHeight

                    val barAlpha = if (visualizerMode == com.example.domain.model.VisualizerMode.DUAL_STUDIO) {
                        0.78f
                    } else {
                        0.95f
                    }

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                FazoraGoldBright.copy(alpha = barAlpha),
                                FazoraGold.copy(alpha = barAlpha),
                                FazoraGoldMuted.copy(alpha = barAlpha)
                            ),
                            startY = top,
                            endY = h
                        ),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Ballistic Peak-Hold floating indicator cap
                    val peakDb = spectrumPeaksDb.getOrElse(i) { db }.coerceIn(-60f, 0f)
                    val normalizedPeak = ((peakDb + 60f) / 60f).coerceIn(0.05f, 1.0f)
                    val peakY = (h - h * normalizedPeak).coerceIn(2.dp.toPx(), h - 4.dp.toPx())
                    drawRoundRect(
                        color = FazoraIvory,
                        topLeft = Offset(left, peakY - 2.dp.toPx()),
                        size = Size(barWidth, 3.dp.toPx()),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
            }

            if (drawWaveform && waveformSamples.size >= 2) {
                val wavePath = Path()
                val fillPath = Path()
                val count = waveformSamples.size
                val stepX = w / (count - 1).coerceAtLeast(1)

                for (i in 0 until count) {
                    val sample = waveformSamples[i].coerceIn(-1f, 1f)
                    val x = i * stepX
                    val y = (midY - sample * (h * 0.44f)).coerceIn(2f, h - 2f)
                    if (i == 0) {
                        wavePath.moveTo(x, y)
                        fillPath.moveTo(x, midY)
                        fillPath.lineTo(x, y)
                    } else {
                        wavePath.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }
                fillPath.lineTo(w, midY)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            FazoraGoldBright.copy(alpha = 0.22f),
                            FazoraGold.copy(alpha = 0.05f),
                            FazoraGoldBright.copy(alpha = 0.22f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                drawPath(
                    path = wavePath,
                    color = if (visualizerMode == com.example.domain.model.VisualizerMode.WAVEFORM_OSCILLOSCOPE) {
                        FazoraGoldBright
                    } else {
                        FazoraIvory.copy(alpha = 0.92f)
                    },
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }

            // 10-Band Biquad EQ Transfer Curve Overlay
            if (eqEnabled && eqGainsDb.size >= bandCount) {
                val eqPath = Path()
                for (i in 0 until bandCount) {
                    val gain = eqGainsDb[i].coerceIn(-12f, 12f)
                    val x = i * slotWidth + slotWidth * 0.5f
                    val y = midY - (gain / 12f) * (h * 0.42f)
                    if (i == 0) {
                        eqPath.moveTo(x, y)
                    } else {
                        eqPath.lineTo(x, y)
                    }
                    drawCircle(
                        color = FazoraGoldBright,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
                drawPath(
                    path = eqPath,
                    color = FazoraGoldBright,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ISO 10-Band Frequency Axis Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PhiTokens.Space8)
        ) {
            EqSpecification.BAND_LABELS.forEach { freqLabel ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = FazoraSteel,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

