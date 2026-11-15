package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
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
import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.EqSpecification
import com.example.domain.model.MediaType
import com.example.domain.model.Playlist
import com.example.domain.model.RepeatMode
import com.example.domain.model.Track
import com.example.domain.model.VisualizerMode
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
import com.example.ui.theme.VeloraChampagneGold

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
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PhiTokens.CornerRadius21))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            FazoraCarbonElevated,
                            FazoraCarbon,
                            FazoraCarbonElevated
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            FazoraGold.copy(alpha = 0.65f),
                            FazoraCarbonBorder,
                            FazoraGold.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(PhiTokens.CornerRadius21)
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PhiTokens.Space13, vertical = 10.dp)
            ) {
                // Studio Disc Emblem with concentric gold ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    FazoraGold.copy(alpha = 0.28f),
                                    FazoraObsidian
                                )
                            )
                        )
                        .border(1.5.dp, if (engineState.isPlaying) FazoraGoldBright else FazoraCarbonBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = when {
                            track?.mediaType == MediaType.VIDEO -> Icons.Default.Movie
                            engineState.isPlaying -> Icons.Default.Equalizer
                            else -> Icons.Default.GraphicEq
                        },
                        contentDescription = stringResource(R.string.cd_open_player),
                        tint = FazoraGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(PhiTokens.Space13))

                Column(
                    modifier = Modifier.weight(PhiTokens.PRIMARY_WEIGHT)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (track != null && track.sampleRateHz >= 48_000) {
                            Surface(
                                color = FazoraGold.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HI-RES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FazoraGoldBright,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = track?.title ?: "Select a track to begin playback",
                            style = MaterialTheme.typography.titleMedium,
                            color = FazoraIvory,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (track != null) {
                            "${track.artist} • ${Track.formatDurationMs(engineState.positionMs)} / ${Track.formatDurationMs(engineState.durationMs)}"
                        } else {
                            stringResource(R.string.tagline)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = VeloraChampagneGold,
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

                // Sculpted Gold Play/Pause Pill Button
                Surface(
                    color = FazoraGold,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onTogglePlayPause() }
                        .testTag("mini_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (engineState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = FazoraObsidian,
                            modifier = Modifier.size(22.dp)
                        )
                    }
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

            LinearProgressIndicator(
                progress = { progress },
                color = FazoraGoldBright,
                trackColor = FazoraObsidian.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
            )
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
    val isHiRes = track.sampleRateHz >= 48_000 || track.bitrateKbps >= 1000

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
            .background(
                if (isActive) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            FazoraGold.copy(alpha = 0.20f),
                            FazoraCarbonElevated,
                            FazoraCarbon
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            FazoraCarbon,
                            FazoraCarbonElevated.copy(alpha = 0.65f)
                        )
                    )
                }
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) FazoraGold else FazoraCarbonBorder,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13)
            )
            .clickable { onPlay() }
            .testTag("track_item_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PhiTokens.Space13, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) {
                            Brush.linearGradient(listOf(FazoraGoldBright, FazoraGold))
                        } else {
                            Brush.linearGradient(listOf(FazoraCarbonElevated, FazoraObsidian))
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) FazoraGoldBright else FazoraCarbonBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = when {
                        track.mediaType == MediaType.VIDEO -> Icons.Default.Movie
                        isActive -> Icons.Default.Equalizer
                        else -> Icons.Default.MusicNote
                    },
                    contentDescription = track.title,
                    tint = if (isActive) FazoraObsidian else FazoraGold
                )
            }

            Spacer(modifier = Modifier.width(PhiTokens.Space13))

            Column(
                modifier = Modifier.weight(PhiTokens.PRIMARY_WEIGHT)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isActive) FazoraGoldBright else FazoraIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isHiRes) {
                        Surface(
                            color = FazoraGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(
                                0.5.dp,
                                FazoraGold.copy(alpha = 0.6f),
                                RoundedCornerShape(4.dp)
                            )
                        ) {
                            Text(
                                text = "HI-RES",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraChampagneGold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FazoraSteel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.durationFormatted} • ${track.sampleRateHz / 1000.0} kHz • ${track.bitrateKbps} kbps",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) FazoraGold else VeloraChampagneGold.copy(alpha = 0.85f)
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
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
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
    visualizerMode: VisualizerMode = VisualizerMode.DUAL_STUDIO,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
                .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            FazoraCarbonElevated.copy(alpha = 0.55f),
                            FazoraObsidian,
                            FazoraObsidian
                        )
                    )
                )
                .border(1.dp, FazoraGold.copy(alpha = 0.35f), RoundedCornerShape(PhiTokens.CornerRadius13))
                .padding(PhiTokens.Space8)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .testTag("eq_spectrum_canvas")
            ) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas

                val quarterY = h * 0.25f
                val midY = h * 0.5f
                val threeQuarterY = h * 0.75f

                // Horizontal reference dB grid lines (+6dB / -15dBFS, 0dB EQ center / -30dBFS, -6dB / -45dBFS)
                drawLine(
                    color = FazoraCarbonBorder.copy(alpha = 0.45f),
                    start = Offset(0f, quarterY),
                    end = Offset(w, quarterY),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = FazoraGold.copy(alpha = 0.32f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = FazoraCarbonBorder.copy(alpha = 0.45f),
                    start = Offset(0f, threeQuarterY),
                    end = Offset(w, threeQuarterY),
                    strokeWidth = 1.dp.toPx()
                )

                val bandCount = EqSpecification.BAND_COUNT
                val slotWidth = w / bandCount
                val barWidth = slotWidth * 0.58f

                // Vertical subtle frequency guides
                for (i in 0 until bandCount) {
                    val centerX = i * slotWidth + slotWidth * 0.5f
                    drawLine(
                        color = FazoraCarbonBorder.copy(alpha = 0.22f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, h),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val drawSpectrumBars =
                    visualizerMode == VisualizerMode.SPECTRUM_BARS ||
                        visualizerMode == VisualizerMode.DUAL_STUDIO

                val drawWaveform =
                    visualizerMode == VisualizerMode.WAVEFORM_OSCILLOSCOPE ||
                        visualizerMode == VisualizerMode.DUAL_STUDIO

                if (drawSpectrumBars) {
                    for (i in 0 until bandCount) {
                        val db = spectrumBandsDb.getOrElse(i) { -60f }.coerceIn(-60f, 0f)
                        val normalizedHeight = ((db + 60f) / 60f).coerceIn(0.05f, 1.0f)
                        val barHeight = h * normalizedHeight
                        val left = i * slotWidth + (slotWidth - barWidth) * 0.5f
                        val top = h - barHeight

                        val barAlpha = if (visualizerMode == VisualizerMode.DUAL_STUDIO) {
                            0.82f
                        } else {
                            0.96f
                        }

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    FazoraGoldBright.copy(alpha = barAlpha),
                                    FazoraGold.copy(alpha = barAlpha),
                                    FazoraGoldMuted.copy(alpha = barAlpha * 0.85f)
                                ),
                                startY = top,
                                endY = h
                            ),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
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
                                FazoraGoldBright.copy(alpha = 0.24f),
                                FazoraGold.copy(alpha = 0.05f),
                                FazoraGoldBright.copy(alpha = 0.24f)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = wavePath,
                        color = if (visualizerMode == VisualizerMode.WAVEFORM_OSCILLOSCOPE) {
                            FazoraGoldBright
                        } else {
                            FazoraIvory.copy(alpha = 0.92f)
                        },
                        style = Stroke(width = 2.2.dp.toPx())
                    )
                }

                // 10-Band Biquad EQ Continuous Transfer Function Curve Overlay |H(e^{jw})|
                if (eqEnabled && eqGainsDb.size >= bandCount) {
                    val cascadeResponseDb = BiquadEqDsp.evaluateCascadeMagnitudeResponseDb(
                        sampleRateHz = 48_000,
                        gainsDb = eqGainsDb,
                        pointCount = 64
                    )
                    val eqPath = Path()
                    val ptCount = cascadeResponseDb.size
                    for (ptIdx in 0 until ptCount) {
                        val responseDb = cascadeResponseDb[ptIdx].coerceIn(-15f, 15f)
                        val x = (ptIdx.toFloat() / (ptCount - 1).coerceAtLeast(1)) * (w - slotWidth) + slotWidth * 0.5f
                        val y = (midY - (responseDb / 12f) * (h * 0.42f)).coerceIn(2f, h - 2f)
                        if (ptIdx == 0) {
                            eqPath.moveTo(x, y)
                        } else {
                            eqPath.lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = eqPath,
                        color = FazoraGoldBright,
                        style = Stroke(width = 2.4.dp.toPx())
                    )
                    for (i in 0 until bandCount) {
                        val gain = eqGainsDb[i].coerceIn(-12f, 12f)
                        val x = i * slotWidth + slotWidth * 0.5f
                        val y = (midY - (gain / 12f) * (h * 0.42f)).coerceIn(2f, h - 2f)
                        drawCircle(
                            color = FazoraObsidian,
                            radius = 4.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = FazoraGoldBright,
                            radius = 3.2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

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
                        color = VeloraChampagneGold,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
