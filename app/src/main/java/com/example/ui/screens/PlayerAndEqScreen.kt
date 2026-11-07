package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.TimeMachineBookmark
import com.example.domain.model.Track
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VisualizerMode
import com.example.ui.components.SpectrumAnalyzerCanvas
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraCrimsonError
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldBright
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraChampagneGold
import com.example.ui.viewmodel.FazoraUiState
import java.util.Locale

@Composable
fun PlayerAndEqScreen(
    uiState: FazoraUiState,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onSetEqBandGain: (Int, Float) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onSelectQueueTrack: (Track) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onClearError: () -> Unit,
    onSetVisualizerMode: (VisualizerMode) -> Unit = {},
    onToggleFavorite: (Track) -> Unit = {},
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onAddBookmark: () -> Unit = {},
    onSetPreampDb: (Float) -> Unit = {},
    onSetBassBoost: (Int) -> Unit = {},
    onSetStereoWidth: (Int) -> Unit = {},
    onSetCrossfadeSec: (Int) -> Unit = {},
    onSetLoopPointA: () -> Unit = {},
    onSetLoopPointB: () -> Unit = {},
    onClearLoop: () -> Unit = {},
    onSetSleepTimerMinutes: (Int) -> Unit = {},
    onImportCustomLrcOrSrt: (String) -> Unit = {},
    onToggleSimpleMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val engine = uiState.engineState
    val queue = uiState.queue
    val activeTrack = engine.currentTrack ?: queue.currentTrack
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val isSimpleMode = uiState.settings.isSimpleMode

    var isDraggingTimeline by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var customPresetName by remember { mutableStateOf("") }

    val durationMs = engine.durationMs.coerceAtLeast(activeTrack?.durationMs ?: 0L)
    val effectivePositionMs = if (isDraggingTimeline) {
        (dragFraction * durationMs).toLong()
    } else {
        engine.positionMs
    }
    val sliderFraction = if (durationMs > 0L) {
        (effectivePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("player_eq_screen")
    ) {
        if (engine.errorMessage != null) {
            item {
                Surface(
                    color = FazoraCarbonElevated,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FazoraCrimsonError, RoundedCornerShape(PhiTokens.CornerRadius13))
                        .testTag("player_error_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(PhiTokens.Space13)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Playback error",
                            tint = FazoraCrimsonError
                        )
                        Spacer(modifier = Modifier.width(PhiTokens.Space13))
                        Text(
                            text = engine.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FazoraIvory,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearError) {
                            Text("Dismiss", color = FazoraGold)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PhiTokens.Space21)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "MASTER TRANSPORT • ${engine.status.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = FazoraGold
                        )
                        Text(
                            text = if (engine.engineAvailable) "Media3 + 10-Band Biquad DSP" else "DSP Ready",
                            style = MaterialTheme.typography.labelMedium,
                            color = FazoraSteel
                        )
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeTrack?.title ?: "No Track Loaded",
                                style = MaterialTheme.typography.headlineLarge,
                                color = FazoraIvory,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(PhiTokens.Space8))
                            Text(
                                text = activeTrack?.let { "${it.artist} • ${it.album}" }
                                    ?: "Select a track from Library or Music",
                                style = MaterialTheme.typography.bodyLarge,
                                color = FazoraSteel
                            )
                        }
                        if (activeTrack != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleFavorite(activeTrack) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("player_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (activeTrack.isFavorite) {
                                            Icons.Default.Favorite
                                        } else {
                                            Icons.Default.FavoriteBorder
                                        },
                                        contentDescription = stringResource(R.string.cd_toggle_favorite),
                                        tint = if (activeTrack.isFavorite) FazoraGold else FazoraSteel
                                    )
                                }
                                IconButton(
                                    onClick = onAddBookmark,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("player_add_bookmark_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkAdd,
                                        contentDescription = "Add Time-Machine Bookmark",
                                        tint = VeloraChampagneGold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "REAL-TIME DSP VISUALIZER",
                            style = MaterialTheme.typography.labelMedium,
                            color = FazoraGold
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "RMS %.1f dBFS • PEAK %.1f dBFS",
                                engine.rmsLevelDb,
                                engine.peakLevelDb
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = FazoraSteel,
                            modifier = Modifier.testTag("visualizer_telemetry_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space8))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        VisualizerMode.entries.forEach { mode ->
                            val selected = engine.visualizerMode == mode
                            Surface(
                                color = if (selected) FazoraGold else FazoraCarbonElevated,
                                shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                                    .clickable { onSetVisualizerMode(mode) }
                                    .testTag("visualizer_mode_${mode.name}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(
                                        horizontal = PhiTokens.Space8,
                                        vertical = PhiTokens.Space8
                                    )
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) FazoraObsidian else FazoraIvory,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space8))

                    SpectrumAnalyzerCanvas(
                        spectrumBandsDb = engine.spectrumBandsDb,
                        eqGainsDb = engine.eqBandGainsDb,
                        eqEnabled = engine.eqEnabled,
                        spectrumPeaksDb = engine.spectrumPeaksDb,
                        waveformSamples = engine.waveformSamples,
                        visualizerMode = engine.visualizerMode
                    )

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Slider(
                        value = sliderFraction,
                        onValueChange = { fraction ->
                            isDraggingTimeline = true
                            dragFraction = fraction.coerceIn(0f, 1f)
                        },
                        onValueChangeFinished = {
                            val targetMs = PlaybackQueue.clampSeekPosition(
                                (dragFraction * durationMs).toLong(),
                                durationMs
                            )
                            isDraggingTimeline = false
                            onSeekTo(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = FazoraGold,
                            activeTrackColor = FazoraGold,
                            inactiveTrackColor = FazoraCarbonElevated
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_slider")
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = Track.formatDurationMs(effectivePositionMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = FazoraIvory,
                            modifier = Modifier.testTag("player_position_text")
                        )
                        Text(
                            text = Track.formatDurationMs(durationMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = FazoraSteel,
                            modifier = Modifier.testTag("player_duration_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isShuffle = queue.mode == QueueMode.SHUFFLE
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = stringResource(R.string.cd_shuffle_mode),
                                tint = if (isShuffle) FazoraGold else FazoraSteel
                            )
                        }

                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.cd_previous_track),
                                tint = FazoraIvory,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Surface(
                            color = FazoraGold,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(PhiTokens.Space55)
                                .clip(CircleShape)
                                .clickable { onTogglePlayPause() }
                                .testTag("player_play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (engine.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_play_pause),
                                    tint = FazoraObsidian,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop playback",
                                tint = FazoraIvory
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.cd_next_track),
                                tint = FazoraIvory,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = onCycleRepeat,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_repeat_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (queue.repeatMode == RepeatMode.ONE) {
                                        Icons.Default.RepeatOne
                                    } else {
                                        Icons.Default.Repeat
                                    },
                                    contentDescription = stringResource(R.string.cd_repeat_mode),
                                    tint = if (queue.repeatMode != RepeatMode.OFF) FazoraGold else FazoraSteel
                                )
                                Text(
                                    text = when (queue.repeatMode) {
                                        RepeatMode.OFF -> "OFF"
                                        RepeatMode.ALL -> "ALL"
                                        RepeatMode.ONE -> "1"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (queue.repeatMode != RepeatMode.OFF) FazoraGoldBright else FazoraSteel,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("player_repeat_mode_label")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_mute_button")
                        ) {
                            Icon(
                                imageVector = if (engine.isMuted || engine.volume == 0f) {
                                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeOff
                                } else {
                                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = stringResource(R.string.cd_mute_toggle),
                                tint = if (engine.isMuted) FazoraCrimsonError else FazoraGold
                            )
                        }
                        Slider(
                            value = engine.volume,
                            onValueChange = onVolumeChange,
                            colors = SliderDefaults.colors(
                                thumbColor = FazoraGold,
                                activeTrackColor = FazoraGold,
                                inactiveTrackColor = FazoraCarbonElevated
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("player_volume_slider")
                        )
                        Spacer(modifier = Modifier.width(PhiTokens.Space8))
                        Text(
                            text = "${(engine.volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = FazoraIvory
                        )
                    }
                }
            }
        }

        if (isSimpleMode) {
            item {
                Surface(
                    color = FazoraCarbon,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
                        .clickable { onToggleSimpleMode(false) }
                        .testTag("simple_mode_open_studio_card")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(PhiTokens.Space21)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) {
                                    "🎛️ اکولایزر ۱۰ بانده و استودیو DSP"
                                } else {
                                    "🎛️ 10-Band Biquad EQ & Studio DSP"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = FazoraGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPersian) {
                                    "تنظیم فرکانس‌ها، بوست ساب‌بیس، پهنای استریو 3D و تایمر خواب"
                                } else {
                                    "Adjust frequency bands, sub-bass boost, 3D width & sleep timer"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = FazoraSteel
                            )
                        }
                        Button(
                            onClick = { onToggleSimpleMode(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FazoraGold,
                                contentColor = FazoraObsidian
                            ),
                            modifier = Modifier.testTag("open_studio_pro_button")
                        ) {
                            Text(if (isPersian) "باز کردن" else "Open Studio")
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    color = FazoraCarbon,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PhiTokens.Space21)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "10-BAND BIQUAD DSP EQUALIZER",
                                style = MaterialTheme.typography.labelLarge,
                                color = FazoraGold
                            )
                            Text(
                                text = "Cascaded Direct Form II Transposed IIR Peaking Filters (Q = 1.414)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FazoraSteel
                            )
                        }
                        Switch(
                            checked = engine.eqEnabled,
                            onCheckedChange = onSetEqEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FazoraObsidian,
                                checkedTrackColor = FazoraGold,
                                uncheckedThumbColor = FazoraSteel,
                                uncheckedTrackColor = FazoraCarbonElevated
                            ),
                            modifier = Modifier.testTag("eq_enable_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Acoustic Presets",
                            style = MaterialTheme.typography.titleMedium,
                            color = FazoraIvory
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            OutlinedButton(
                                onClick = { onApplyPreset(EqPreset.FLAT) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("eq_reset_flat_button")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = FazoraSteel)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Flat", color = FazoraIvory)
                            }
                            Button(
                                onClick = { showSavePresetDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FazoraGold,
                                    contentColor = FazoraObsidian
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("eq_save_preset_button")
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Preset")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space8))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.eqPresets, key = { it.id }) { preset ->
                            val isSelected = uiState.activeEqPresetId == preset.id
                            Surface(
                                color = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
                                    .clickable { onApplyPreset(preset) }
                                    .testTag("eq_preset_chip_${preset.name.lowercase().replace(' ', '_')}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) FazoraObsidian else FazoraIvory
                                    )
                                    if (!preset.isBuiltIn) {
                                        Spacer(modifier = Modifier.width(PhiTokens.Space8))
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete preset ${preset.name}",
                                            tint = if (isSelected) FazoraObsidian else FazoraSteel,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { onDeleteCustomPreset(preset.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space21))

                    EqSpecification.CENTER_FREQUENCIES_HZ.forEachIndexed { index, _ ->
                        val label = EqSpecification.BAND_LABELS[index]
                        val gain = engine.eqBandGainsDb.getOrElse(index) { 0f }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${label}Hz",
                                style = MaterialTheme.typography.labelLarge,
                                color = FazoraGold,
                                modifier = Modifier.width(55.dp)
                            )
                            Slider(
                                value = gain,
                                onValueChange = { newGain -> onSetEqBandGain(index, newGain) },
                                valueRange = EqSpecification.MIN_GAIN_DB..EqSpecification.MAX_GAIN_DB,
                                enabled = engine.eqEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = FazoraGold,
                                    activeTrackColor = FazoraGold,
                                    inactiveTrackColor = FazoraCarbonElevated
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_slider_$index")
                            )
                            Text(
                                text = String.format(Locale.US, "%+.1fdB", gain),
                                style = MaterialTheme.typography.labelLarge,
                                color = FazoraIvory,
                                modifier = Modifier
                                    .width(68.dp)
                                    .padding(start = PhiTokens.Space8)
                                    .testTag("eq_band_value_$index")
                            )
                        }
                    }
                }
            }
        }

        item {
            StudioDspRackPanel(
                dspState = uiState.studioDspState,
                isPersian = uiState.settings.appLanguageCode == "fa",
                onSetPreampDb = onSetPreampDb,
                onSetBassBoost = onSetBassBoost,
                onSetStereoWidth = onSetStereoWidth,
                onSetCrossfadeSec = onSetCrossfadeSec,
                onSetLoopPointA = onSetLoopPointA,
                onSetLoopPointB = onSetLoopPointB,
                onClearLoop = onClearLoop,
                onSetSleepTimerMinutes = onSetSleepTimerMinutes,
                onImportCustomLrcOrSrt = onImportCustomLrcOrSrt
            )
        }
    }

        if (uiState.activeLyrics.isNotEmpty()) {
            item {
                SynchronizedLyricsAndBookmarksCard(
                    lyrics = uiState.activeLyrics,
                    positionMs = effectivePositionMs,
                    lyricsOffsetMs = uiState.lyricsOffsetMs,
                    bookmarks = uiState.timeMachineBookmarks,
                    isPersian = uiState.settings.appLanguageCode == "fa",
                    onAdjustOffset = onAdjustLyricsOffset,
                    onSeekTo = onSeekTo,
                    onAddBookmark = onAddBookmark
                )
            }
        }

        item {
            Text(
                text = "Playback Queue (${queue.tracks.size} tracks • ${queue.mode.name} • Repeat ${queue.repeatMode.name})",
                style = MaterialTheme.typography.headlineMedium,
                color = FazoraIvory
            )
        }

        items(queue.orderedQueueTracks, key = { "q_${it.id}" }) { track ->
            val isCurrent = queue.currentTrack?.id == track.id
            Surface(
                color = if (isCurrent) FazoraGoldMuted else FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isCurrent) FazoraGold else FazoraCarbonBorder,
                        RoundedCornerShape(PhiTokens.CornerRadius13)
                    )
                    .clickable { onSelectQueueTrack(track) }
                    .testTag("queue_item_${track.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = FazoraIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${track.durationFormatted}",
                            style = MaterialTheme.typography.labelMedium,
                            color = FazoraSteel
                        )
                    }
                    IconButton(
                        onClick = { onRemoveFromQueue(track.id) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("queue_remove_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove ${track.title} from queue",
                            tint = FazoraSteel
                        )
                    }
                }
            }
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            containerColor = FazoraCarbonElevated,
            title = { Text("Save Custom 10-Band EQ Preset", color = FazoraIvory) },
            text = {
                OutlinedTextField(
                    value = customPresetName,
                    onValueChange = { customPresetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_preset_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customPresetName.isNotBlank()) {
                            onSaveCustomPreset(customPresetName.trim())
                            customPresetName = ""
                            showSavePresetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian),
                    modifier = Modifier.testTag("confirm_save_eq_preset_button")
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel", color = FazoraSteel)
                }
            }
        )
    }
}

@Composable
private fun SynchronizedLyricsAndBookmarksCard(
    lyrics: List<SyncedLyricLine>,
    positionMs: Long,
    lyricsOffsetMs: Long,
    bookmarks: List<TimeMachineBookmark>,
    isPersian: Boolean,
    onAdjustOffset: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onAddBookmark: () -> Unit
) {
    val activeIdx = VeloraLyricsAndSubtitleEngine.findActiveLineIndex(lyrics, positionMs, lyricsOffsetMs)

    Surface(
        color = FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
            .testTag("synchronized_lyrics_card")
    ) {
        Column(modifier = Modifier.padding(PhiTokens.Space21)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUTOMATIC LYRICS & TIME MACHINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Text(
                        text = "Time-Synchronized Multi-Script Lyrics (Offset: ${lyricsOffsetMs}ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = FazoraSteel
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { onAdjustOffset(-250L) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lyrics_offset_minus_button")
                    ) {
                        Text("-250ms", color = FazoraIvory)
                    }
                    OutlinedButton(
                        onClick = { onAdjustOffset(250L) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lyrics_offset_plus_button")
                    ) {
                        Text("+250ms", color = FazoraIvory)
                    }
                }
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            lyrics.forEachIndexed { idx, line ->
                val isCurrentLine = idx == activeIdx
                Surface(
                    color = if (isCurrentLine) FazoraGoldMuted else FazoraCarbonElevated,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onSeekTo(line.timestampMs) }
                        .testTag("lyric_line_$idx")
                ) {
                    Column(modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)) {
                        Text(
                            text = if (isPersian && line.translationFa.isNotBlank()) line.translationFa else line.text,
                            style = if (isCurrentLine) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = if (isCurrentLine) FazoraGoldBright else FazoraIvory
                        )
                        if (!isPersian && line.translationFa.isNotBlank()) {
                            Text(
                                text = line.translationFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraChampagneGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Media Time Machine Bookmarks (${bookmarks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = FazoraIvory
                )
                OutlinedButton(
                    onClick = onAddBookmark,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_time_machine_bookmark_button")
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = FazoraGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bookmark Pos", color = FazoraIvory)
                }
            }

            if (bookmarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(PhiTokens.Space8))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(bookmarks, key = { it.id }) { bm ->
                        OutlinedButton(
                            onClick = { onSeekTo(bm.timestampMs) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("bookmark_chip_${bm.id}")
                        ) {
                            Text("${bm.formattedTimestamp} • ${bm.trackTitle}", color = VeloraChampagneGold)
                        }
                    }
                }
            }
        }
    }
}
