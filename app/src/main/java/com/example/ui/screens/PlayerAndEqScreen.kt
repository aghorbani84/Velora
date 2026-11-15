package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AudioRouteType
import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
import com.example.domain.model.GestureActionType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.QueueMode
import com.example.domain.model.RepeatMode
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.TimeMachineBookmark
import com.example.domain.model.Track
import com.example.domain.model.VeloraGestureEngine
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VisualizerMode
import com.example.ui.components.SpectrumAnalyzerCanvas
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraCrimsonError
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldBright
import com.example.ui.theme.FazoraGoldDeep
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraChampagneGold
import com.example.ui.viewmodel.FazoraUiState
import java.util.Locale
import kotlinx.coroutines.delay

private enum class ConsoleModuleTab(val labelEn: String, val labelFa: String) {
    ALL("All Console", "تمام کنسول"),
    EQ_DSP("10-Band EQ & DSP", "اکولایزر و DSP"),
    LYRICS("Synced Lyrics", "متن آهنگ"),
    QUEUE("Master Queue", "صف پخش")
}

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
    onEditTrackMetadata: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onStampLyricLine: (Int) -> Unit = {},
    onAddNewLyricLineAtPos: (String) -> Unit = {},
    onExportLrc: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    val engine = uiState.engineState
    val queue = uiState.queue
    val activeTrack = engine.currentTrack ?: queue.currentTrack
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val isSimpleMode = uiState.settings.isSimpleMode

    var activeModuleTab by remember { mutableStateOf(ConsoleModuleTab.ALL) }
    var isDraggingTimeline by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showExactSeekDialog by remember { mutableStateOf(false) }
    var exactSeekInitial by remember { mutableStateOf("") }
    var showTagEditorDialog by remember { mutableStateOf(false) }

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

    val showEqSection = activeModuleTab == ConsoleModuleTab.ALL || activeModuleTab == ConsoleModuleTab.EQ_DSP
    val showLyricsSection = activeModuleTab == ConsoleModuleTab.ALL || activeModuleTab == ConsoleModuleTab.LYRICS
    val showQueueSection = activeModuleTab == ConsoleModuleTab.ALL || activeModuleTab == ConsoleModuleTab.QUEUE

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
                PlayerErrorBanner(
                    errorMessage = engine.errorMessage,
                    onClearError = onClearError
                )
            }
        }

        item {
            PlayerMasterTransportDeck(
                uiState = uiState,
                activeTrack = activeTrack,
                isPersian = isPersian,
                durationMs = durationMs,
                effectivePositionMs = effectivePositionMs,
                sliderFraction = sliderFraction,
                onDragFractionChange = { fraction ->
                    isDraggingTimeline = true
                    dragFraction = fraction.coerceIn(0f, 1f)
                },
                onDragFinished = {
                    val targetMs = PlaybackQueue.clampSeekPosition(
                        (dragFraction * durationMs).toLong(),
                        durationMs
                    )
                    isDraggingTimeline = false
                    onSeekTo(targetMs)
                },
                onOpenExactSeek = {
                    exactSeekInitial = effectivePositionMs.toString()
                    showExactSeekDialog = true
                },
                onOpenTagEditor = { showTagEditorDialog = true },
                onToggleFavorite = onToggleFavorite,
                onAddBookmark = onAddBookmark,
                onSetVisualizerMode = onSetVisualizerMode,
                onSeekTo = onSeekTo,
                onTogglePlayPause = onTogglePlayPause,
                onStop = onStop,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onVolumeChange = onVolumeChange,
                onToggleMute = onToggleMute
            )
        }

        // Studio Workspace Module Selector Bar
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ConsoleModuleTab.entries) { tab ->
                    val selected = activeModuleTab == tab
                    Surface(
                        color = if (selected) FazoraGold else FazoraCarbon,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = if (selected) FazoraGoldBright else FazoraCarbonBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { activeModuleTab = tab }
                    ) {
                        Text(
                            text = if (isPersian) tab.labelFa else tab.labelEn,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) FazoraObsidian else FazoraIvory,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (isSimpleMode && showEqSection) {
            item {
                SimpleModeOpenStudioCard(
                    isPersian = isPersian,
                    onOpenStudio = { onToggleSimpleMode(false) }
                )
            }
        } else if (showEqSection) {
            item {
                TenBandBiquadEqRackCard(
                    uiState = uiState,
                    onSetEqEnabled = onSetEqEnabled,
                    onSetEqBandGain = onSetEqBandGain,
                    onApplyPreset = onApplyPreset,
                    onOpenSavePreset = { showSavePresetDialog = true },
                    onDeleteCustomPreset = onDeleteCustomPreset
                )
            }

            item {
                StudioDspRackPanel(
                    dspState = uiState.studioDspState,
                    isPersian = isPersian,
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

        if (showLyricsSection && uiState.activeLyrics.isNotEmpty()) {
            item {
                SynchronizedLyricsAndBookmarksCard(
                    lyrics = uiState.activeLyrics,
                    positionMs = effectivePositionMs,
                    lyricsOffsetMs = uiState.lyricsOffsetMs,
                    bookmarks = uiState.timeMachineBookmarks,
                    isPersian = isPersian,
                    onAdjustOffset = onAdjustLyricsOffset,
                    onSeekTo = onSeekTo,
                    onAddBookmark = onAddBookmark,
                    onStampLyricLine = onStampLyricLine,
                    onAddNewLyricLineAtPos = onAddNewLyricLineAtPos,
                    onExportLrc = onExportLrc
                )
            }
        }

        if (showQueueSection) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Playback Queue (${queue.tracks.size} tracks • ${queue.mode.name} • Repeat ${queue.repeatMode.name})",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = FazoraIvory
                    )
                }
            }

            items(queue.orderedQueueTracks, key = { "q_${it.id}" }) { track ->
                val isCurrent = queue.currentTrack?.id == track.id
                Surface(
                    color = if (isCurrent) FazoraGoldMuted else FazoraCarbon,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isCurrent) FazoraGold else FazoraCarbonBorder,
                            RoundedCornerShape(14.dp)
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
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) FazoraGoldBright else FazoraIvory,
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
    }

    if (showSavePresetDialog) {
        SaveEqPresetDialog(
            onDismiss = { showSavePresetDialog = false },
            onSaveCustomPreset = onSaveCustomPreset
        )
    }

    if (showExactSeekDialog) {
        ExactSeekDialog(
            initialValue = exactSeekInitial,
            effectivePositionMs = effectivePositionMs,
            durationMs = durationMs,
            isPersian = isPersian,
            onDismiss = { showExactSeekDialog = false },
            onSeekTo = onSeekTo
        )
    }

    if (showTagEditorDialog && activeTrack != null) {
        MetadataTagEditorDialog(
            track = activeTrack,
            isPersian = isPersian,
            onDismiss = { showTagEditorDialog = false },
            onEditTrackMetadata = onEditTrackMetadata
        )
    }
}

@Composable
private fun PlayerErrorBanner(
    errorMessage: String,
    onClearError: () -> Unit
) {
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
                text = errorMessage,
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

@Composable
private fun PlayerMasterTransportDeck(
    uiState: FazoraUiState,
    activeTrack: Track?,
    isPersian: Boolean,
    durationMs: Long,
    effectivePositionMs: Long,
    sliderFraction: Float,
    onDragFractionChange: (Float) -> Unit,
    onDragFinished: () -> Unit,
    onOpenExactSeek: () -> Unit,
    onOpenTagEditor: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddBookmark: () -> Unit,
    onSetVisualizerMode: (VisualizerMode) -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit
) {
    val engine = uiState.engineState
    val queue = uiState.queue
    val autoHeadroomDb = remember(engine.eqBandGainsDb) {
        BiquadEqDsp.computeOptimalAutoPreampDb(engine.eqBandGainsDb)
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        FazoraGold.copy(alpha = 0.6f),
                        FazoraCarbonBorder,
                        FazoraGoldDeep.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(PhiTokens.CornerRadius21)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FazoraCarbonElevated, FazoraCarbon)
                    )
                )
                .padding(PhiTokens.Space21)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MASTER ACOUSTIC DECK • ${engine.status.name}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = FazoraGoldBright
                )
                Surface(
                    color = FazoraObsidian.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (engine.engineAvailable) "64-BIT TPDF DSP" else "DSP READY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = VeloraChampagneGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
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
                        fontWeight = FontWeight.ExtraBold,
                        color = FazoraIvory,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeTrack?.let { "${it.artist} • ${it.album}" }
                            ?: "Select a track from Library or Music",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    val route = uiState.audioRoute
                    val routeIcon = when (route.type) {
                        AudioRouteType.BLUETOOTH -> "📶"
                        AudioRouteType.WIRED_HEADPHONES -> "🎧"
                        AudioRouteType.USB_DAC -> "🎛️"
                        AudioRouteType.SPEAKER -> "🔊"
                        AudioRouteType.UNKNOWN -> "🎵"
                    }
                    Surface(
                        color = FazoraObsidian.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(
                            1.dp,
                            FazoraCarbonBorder,
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$routeIcon ${route.deviceName} • ${uiState.settings.adaptiveAudioMode.labelEn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FazoraGoldBright
                            )
                        }
                    }
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
                                tint = if (activeTrack.isFavorite) FazoraGoldBright else FazoraSteel
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
                        TextButton(
                            onClick = onOpenTagEditor,
                            modifier = Modifier.testTag("player_edit_tags_button")
                        ) {
                            Text(
                                text = if (isPersian) "تگ" else "Tags",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = FazoraGold
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
                    text = "REAL-TIME GOERTZEL & BIQUAD TRANSFER CURVE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = FazoraGold
                )
                Text(
                    text = String.format(
                        Locale.US,
                        "RMS %.1f dBFS • PEAK %.1f dBFS • Headroom %.1fdB",
                        engine.rmsLevelDb,
                        engine.peakLevelDb,
                        autoHeadroomDb
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = VeloraChampagneGold,
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
                        color = if (selected) FazoraGold else FazoraObsidian.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (selected) FazoraGoldBright else FazoraCarbonBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSetVisualizerMode(mode) }
                            .testTag("visualizer_mode_${mode.name}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
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

            var playerGestureHud by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(playerGestureHud) {
                if (playerGestureHud != null) {
                    delay(900L)
                    playerGestureHud = null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(engine.positionMs, durationMs) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val res = VeloraGestureEngine.resolveDoubleTapGesture(
                                    tapX = offset.x,
                                    surfaceWidth = size.width.toFloat(),
                                    currentPositionMs = engine.positionMs,
                                    durationMs = durationMs
                                )
                                when (res.action) {
                                    GestureActionType.SEEK_BACKWARD,
                                    GestureActionType.SEEK_FORWARD -> {
                                        onSeekTo(res.targetPositionMs)
                                        playerGestureHud = res.hudLabel
                                    }
                                    else -> {
                                        onTogglePlayPause()
                                        playerGestureHud = res.hudLabel
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(engine.volume) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val swipe = VeloraGestureEngine.resolveVerticalSwipeGesture(
                                    startX = size.width.toFloat(),
                                    surfaceWidth = size.width.toFloat(),
                                    surfaceHeight = size.height.toFloat(),
                                    dragDeltaY = dragAmount,
                                    currentBrightness = 1f,
                                    currentVolume = engine.volume
                                )
                                onVolumeChange(swipe.newValue)
                                playerGestureHud = swipe.hudLabel
                            }
                        )
                    }
                    .testTag("player_gesture_surface")
            ) {
                SpectrumAnalyzerCanvas(
                    spectrumBandsDb = engine.spectrumBandsDb,
                    eqGainsDb = engine.eqBandGainsDb,
                    eqEnabled = engine.eqEnabled,
                    spectrumPeaksDb = engine.spectrumPeaksDb,
                    waveformSamples = engine.waveformSamples,
                    visualizerMode = engine.visualizerMode
                )

                if (playerGestureHud != null) {
                    Surface(
                        color = FazoraObsidian.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius21))
                    ) {
                        Text(
                            text = playerGestureHud.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FazoraGoldBright,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            Slider(
                value = sliderFraction,
                onValueChange = onDragFractionChange,
                onValueChangeFinished = onDragFinished,
                colors = SliderDefaults.colors(
                    thumbColor = FazoraGoldBright,
                    activeTrackColor = FazoraGold,
                    inactiveTrackColor = FazoraObsidian
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seek_slider")
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${Track.formatDurationMs(effectivePositionMs)} (${effectivePositionMs}ms)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = FazoraGoldBright,
                    modifier = Modifier
                        .clickable { onOpenExactSeek() }
                        .testTag("player_position_text")
                )
                Text(
                    text = Track.formatDurationMs(durationMs),
                    style = MaterialTheme.typography.labelLarge,
                    color = FazoraSteel,
                    modifier = Modifier.testTag("player_duration_text")
                )
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            PlayerTransportButtonsRow(
                isPlaying = engine.isPlaying,
                isShuffle = queue.mode == QueueMode.SHUFFLE,
                repeatMode = queue.repeatMode,
                onToggleShuffle = onToggleShuffle,
                onPrevious = onPrevious,
                onTogglePlayPause = onTogglePlayPause,
                onStop = onStop,
                onNext = onNext,
                onCycleRepeat = onCycleRepeat
            )

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
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = stringResource(R.string.cd_mute_toggle),
                        tint = if (engine.isMuted) FazoraCrimsonError else FazoraGold
                    )
                }
                Slider(
                    value = engine.volume,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(
                        thumbColor = FazoraGoldBright,
                        activeTrackColor = FazoraGold,
                        inactiveTrackColor = FazoraObsidian
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_volume_slider")
                )
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Text(
                    text = "${(engine.volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = FazoraIvory
                )
            }
        }
    }
}

@Composable
private fun PlayerTransportButtonsRow(
    isPlaying: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onToggleShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_shuffle_button")
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.cd_shuffle_mode),
                tint = if (isShuffle) FazoraGoldBright else FazoraSteel
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

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(FazoraGoldBright, FazoraGold, FazoraGoldDeep)
                    )
                )
                .clickable { onTogglePlayPause() }
                .testTag("player_play_pause_button")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_pause),
                tint = FazoraObsidian,
                modifier = Modifier.size(32.dp)
            )
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
                    imageVector = if (repeatMode == RepeatMode.ONE) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    },
                    contentDescription = stringResource(R.string.cd_repeat_mode),
                    tint = if (repeatMode != RepeatMode.OFF) FazoraGoldBright else FazoraSteel
                )
                Text(
                    text = when (repeatMode) {
                        RepeatMode.OFF -> "OFF"
                        RepeatMode.ALL -> "ALL"
                        RepeatMode.ONE -> "1"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (repeatMode != RepeatMode.OFF) FazoraGoldBright else FazoraSteel,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("player_repeat_mode_label")
                )
            }
        }
    }
}

@Composable
private fun SimpleModeOpenStudioCard(
    isPersian: Boolean,
    onOpenStudio: () -> Unit
) {
    Surface(
        color = FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
            .clickable { onOpenStudio() }
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
                    fontWeight = FontWeight.Bold,
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
                onClick = onOpenStudio,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier.testTag("open_studio_pro_button")
            ) {
                Text(if (isPersian) "باز کردن" else "Open Studio", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TenBandBiquadEqRackCard(
    uiState: FazoraUiState,
    onSetEqEnabled: (Boolean) -> Unit,
    onSetEqBandGain: (Int, Float) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onOpenSavePreset: () -> Unit,
    onDeleteCustomPreset: (String) -> Unit
) {
    val engine = uiState.engineState
    val autoHeadroomDb = remember(engine.eqBandGainsDb) {
        BiquadEqDsp.computeOptimalAutoPreampDb(engine.eqBandGainsDb)
    }

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
                        fontWeight = FontWeight.Bold,
                        color = FazoraGoldBright
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "Cascaded DF-II Transposed IIR (Q = 1.414) • Auto-Headroom: %+.1f dB",
                            autoHeadroomDb
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraChampagneGold
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
                    fontWeight = FontWeight.Bold,
                    color = FazoraIvory
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    OutlinedButton(
                        onClick = { onApplyPreset(EqPreset.FLAT) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("eq_reset_flat_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = FazoraSteel)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flat", color = FazoraIvory)
                    }
                    Button(
                        onClick = onOpenSavePreset,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FazoraGold,
                            contentColor = FazoraObsidian
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("eq_save_preset_button")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Preset", fontWeight = FontWeight.Bold)
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isSelected) FazoraGoldBright else FazoraCarbonBorder,
                                RoundedCornerShape(12.dp)
                            )
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

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
                        fontWeight = FontWeight.Bold,
                        color = FazoraGold,
                        modifier = Modifier.width(56.dp)
                    )
                    Slider(
                        value = gain,
                        onValueChange = { newGain -> onSetEqBandGain(index, newGain) },
                        valueRange = EqSpecification.MIN_GAIN_DB..EqSpecification.MAX_GAIN_DB,
                        enabled = engine.eqEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = FazoraGoldBright,
                            activeTrackColor = FazoraGold,
                            inactiveTrackColor = FazoraObsidian
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_band_slider_$index")
                    )
                    Text(
                        text = String.format(Locale.US, "%+.1fdB", gain),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (gain != 0f) FazoraGoldBright else FazoraIvory,
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

@Composable
private fun SaveEqPresetDialog(
    onDismiss: () -> Unit,
    onSaveCustomPreset: (String) -> Unit
) {
    var customPresetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier.testTag("confirm_save_eq_preset_button")
            ) {
                Text("Save Preset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FazoraSteel)
            }
        }
    )
}

@Composable
private fun ExactSeekDialog(
    initialValue: String,
    effectivePositionMs: Long,
    durationMs: Long,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    var exactSeekInput by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FazoraCarbonElevated,
        title = {
            Text(
                text = if (isPersian) "پرش دقیق به زمان (میلی‌ثانیه یا دقیقه:ثانیه)" else "Precision Exact Seek (ms or mm:ss)",
                color = FazoraIvory
            )
        },
        text = {
            OutlinedTextField(
                value = exactSeekInput,
                onValueChange = { exactSeekInput = it },
                label = { Text("e.g. 42500 or 01:25") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exact_seek_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = exactSeekInput.trim()
                    val parsedMs = if (trimmed.contains(":")) {
                        val parts = trimmed.split(":")
                        val mins = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                        val secs = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                        (mins * 60_000L) + (secs * 1000L)
                    } else {
                        trimmed.toLongOrNull() ?: effectivePositionMs
                    }
                    onSeekTo(PlaybackQueue.clampSeekPosition(parsedMs, durationMs))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier.testTag("confirm_exact_seek_button")
            ) {
                Text(if (isPersian) "پرش" else "Seek")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel", color = FazoraSteel)
            }
        }
    )
}

@Composable
private fun MetadataTagEditorDialog(
    track: Track,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onEditTrackMetadata: (String, String, String, String) -> Unit
) {
    var editTitleInput by remember(track.id) { mutableStateOf(track.title) }
    var editArtistInput by remember(track.id) { mutableStateOf(track.artist) }
    var editAlbumInput by remember(track.id) { mutableStateOf(track.album) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FazoraCarbonElevated,
        title = {
            Text(
                text = if (isPersian) "ویرایش متادیتای آهنگ" else "Edit Track Metadata Tags",
                color = FazoraIvory
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                OutlinedTextField(
                    value = editTitleInput,
                    onValueChange = { editTitleInput = it },
                    label = { Text(if (isPersian) "عنوان آهنگ" else "Track Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_edit_title_input")
                )
                OutlinedTextField(
                    value = editArtistInput,
                    onValueChange = { editArtistInput = it },
                    label = { Text(if (isPersian) "نام هنرمند" else "Artist") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_edit_artist_input")
                )
                OutlinedTextField(
                    value = editAlbumInput,
                    onValueChange = { editAlbumInput = it },
                    label = { Text(if (isPersian) "آلبوم" else "Album") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_edit_album_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onEditTrackMetadata(
                        track.id,
                        editTitleInput.ifBlank { track.title },
                        editArtistInput.ifBlank { track.artist },
                        editAlbumInput.ifBlank { track.album }
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier.testTag("confirm_save_tags_button")
            ) {
                Text(if (isPersian) "ذخیره تگ" else "Save Tags")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel", color = FazoraSteel)
            }
        }
    )
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
    onAddBookmark: () -> Unit,
    onStampLyricLine: (Int) -> Unit = {},
    onAddNewLyricLineAtPos: (String) -> Unit = {},
    onExportLrc: () -> String = { "" }
) {
    val activeIdx = VeloraLyricsAndSubtitleEngine.findActiveLineIndex(lyrics, positionMs, lyricsOffsetMs)
    var showLrcStudioDialog by remember { mutableStateOf(false) }

    if (showLrcStudioDialog) {
        InteractiveLrcTimestampStudioDialog(
            lyrics = lyrics,
            currentPositionMs = positionMs,
            isPersian = isPersian,
            onDismiss = { showLrcStudioDialog = false },
            onStampLine = onStampLyricLine,
            onAddNewLineAtPos = onAddNewLyricLineAtPos,
            onExportLrc = onExportLrc
        )
    }

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
                        fontWeight = FontWeight.Bold,
                        color = FazoraGold
                    )
                    Text(
                        text = "Time-Synchronized Multi-Script Lyrics (Offset: ${lyricsOffsetMs}ms)",
                        style = MaterialTheme.typography.bodySmall,
                        color = FazoraSteel
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { showLrcStudioDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FazoraGold,
                            contentColor = FazoraObsidian
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_lrc_timestamp_studio_button")
                    ) {
                        Text(if (isPersian) "⏱️ ساخت LRC" else "⏱️ LRC Studio", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAdjustOffset(-250L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("lyrics_offset_minus_button")
                    ) {
                        Text("-250ms", color = FazoraIvory)
                    }
                    OutlinedButton(
                        onClick = { onAdjustOffset(250L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .border(
                            width = if (isCurrentLine) 1.dp else 0.dp,
                            color = if (isCurrentLine) FazoraGold else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
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
                            fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
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
                    fontWeight = FontWeight.Bold,
                    color = FazoraIvory
                )
                OutlinedButton(
                    onClick = onAddBookmark,
                    shape = RoundedCornerShape(10.dp),
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
                            shape = RoundedCornerShape(10.dp),
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
