package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.model.EqPreset
import com.example.domain.model.ExperienceTier
import com.example.domain.model.FullscreenPolicy
import com.example.domain.model.MusicControlGroup
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.TimeMachineBookmark
import com.example.domain.model.Track
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VisualizerMode
import com.example.ui.components.SpectrumAnalyzerCanvas
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraErrorBanner
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.components.VeloraSeekBar
import com.example.ui.components.VeloraTransportControls
import com.example.ui.components.formatBadge
import com.example.ui.components.veloraCinemaGestures
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import com.example.ui.viewmodel.PlaybackPositionState

@Composable
fun PlayerAndEqScreen(
    uiState: FazoraUiState,
    positionState: PlaybackPositionState = PlaybackPositionState(
        positionMs = uiState.engineState.positionMs,
        durationMs = uiState.engineState.durationMs,
        isPlaying = uiState.engineState.isPlaying
    ),
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
    onToggleSimpleMode: (Boolean) -> Unit = {},
    onSetExperienceTier: (ExperienceTier) -> Unit = { tier ->
        onToggleSimpleMode(tier == ExperienceTier.SIMPLE)
    },
    onEditTrackMetadata: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onStampLyricLine: (Int) -> Unit = {},
    onAddNewLyricLineAtPos: (String) -> Unit = {},
    onExportLrc: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val tier = uiState.settings.experienceTier
    val engine = uiState.engineState
    val track = engine.currentTrack
    var showExactSeekDialog by remember { mutableStateOf(false) }
    var showTagEditorDialog by remember { mutableStateOf(false) }
    var gestureHudNotice by remember { mutableStateOf<String?>(null) }

    if (showExactSeekDialog) {
        ExactSeekDialog(
            initialValue = (positionState.positionMs / 1000L).toString(),
            effectivePositionMs = positionState.positionMs,
            durationMs = positionState.durationMs,
            isPersian = isPersian,
            onDismiss = { showExactSeekDialog = false },
            onSeekTo = onSeekTo
        )
    }

    if (showTagEditorDialog && track != null) {
        MetadataTagEditorDialog(
            track = track,
            isPersian = isPersian,
            onDismiss = { showTagEditorDialog = false },
            onEditTrackMetadata = onEditTrackMetadata
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("player_and_eq_screen")
    ) {
        engine.errorMessage?.let { msg ->
            item {
                VeloraErrorBanner(
                    message = msg,
                    actionLabel = if (isPersian) "بستن" else "Dismiss",
                    onAction = onClearError
                )
            }
        }

        item {
            NowPlayingStageCard(
                uiState = uiState,
                positionState = positionState,
                tier = tier,
                isPersian = isPersian,
                gestureHudNotice = gestureHudNotice,
                onHudNotice = { gestureHudNotice = it },
                onTogglePlayPause = onTogglePlayPause,
                onStop = onStop,
                onPrevious = onPrevious,
                onNext = onNext,
                onSeekTo = onSeekTo,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onVolumeChange = onVolumeChange,
                onToggleMute = onToggleMute,
                onSetVisualizerMode = onSetVisualizerMode,
                onToggleFavorite = onToggleFavorite,
                onOpenExactSeek = { showExactSeekDialog = true },
                onOpenTagEditor = { showTagEditorDialog = true }
            )
        }

        if (FullscreenPolicy.isMusicControlVisible(tier, MusicControlGroup.TEN_BAND_EQ)) {
            item {
                EqualizerAndStudioDspSection(
                    uiState = uiState,
                    isPersian = isPersian,
                    onSetEqEnabled = onSetEqEnabled,
                    onSetEqBandGain = onSetEqBandGain,
                    onApplyPreset = onApplyPreset,
                    onSaveCustomPreset = onSaveCustomPreset,
                    onDeleteCustomPreset = onDeleteCustomPreset,
                    onSetPreampDb = onSetPreampDb,
                    onSetBassBoost = onSetBassBoost,
                    onSetStereoWidth = onSetStereoWidth,
                    onSetCrossfadeSec = onSetCrossfadeSec,
                    onSetLoopPointA = onSetLoopPointA,
                    onSetLoopPointB = onSetLoopPointB,
                    onClearLoop = onClearLoop,
                    onSetSleepTimerMinutes = onSetSleepTimerMinutes
                )
            }
        } else {
            item {
                VeloraSectionCard(
                    eyebrow = if (isPersian) "سطح کاربری فعلی: ${tier.labelFa}" else "ACTIVE TIER: ${tier.labelEn}",
                    title = if (isPersian) "استودیو صوتی و اکولایزر ۱۰ باندی" else "10-Band EQ & Studio DSP Rack",
                    subtitle = if (isPersian) {
                        "برای دسترسی به اکولایزر ۱۰ باندی Biquad، تقویت ساب‌بیس و حلقه A-B، سطح کاربری حرفه‌ای (PRO) را فعال کنید."
                    } else {
                        "Switch to Studio Pro tier to unlock the 10-band Biquad equalizer, sub-bass boost, and A-B loop."
                    }
                ) {
                    VeloraPrimaryButton(
                        text = if (isPersian) "ارتقا به حالت حرفه‌ای (PRO)" else "Switch to Studio Pro",
                        icon = Icons.Default.Tune,
                        onClick = { onSetExperienceTier(ExperienceTier.PRO) },
                        tag = "open_studio_pro_button"
                    )
                }
            }
        }

        if (FullscreenPolicy.isMusicControlVisible(tier, MusicControlGroup.LYRICS_VIEWER)) {
            item {
                SynchronizedLyricsAndBookmarksSection(
                    lyrics = uiState.activeLyrics,
                    positionMs = positionState.positionMs,
                    lyricsOffsetMs = uiState.lyricsOffsetMs,
                    bookmarks = uiState.timeMachineBookmarks,
                    tier = tier,
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

        if (FullscreenPolicy.isMusicControlVisible(tier, MusicControlGroup.QUEUE_AND_MODES)) {
            item {
                QueueSectionCard(
                    uiState = uiState,
                    isPersian = isPersian,
                    onSelectQueueTrack = onSelectQueueTrack,
                    onRemoveFromQueue = onRemoveFromQueue
                )
            }
        }
    }
}

@Composable
private fun NowPlayingStageCard(
    uiState: FazoraUiState,
    positionState: PlaybackPositionState,
    tier: ExperienceTier,
    isPersian: Boolean,
    gestureHudNotice: String?,
    onHudNotice: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSetVisualizerMode: (VisualizerMode) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenExactSeek: () -> Unit,
    onOpenTagEditor: () -> Unit
) {
    val engine = uiState.engineState
    val track = engine.currentTrack

    VeloraSectionCard(
        eyebrow = if (isPersian) "پخش‌کننده استودیویی ولورا" else "VELORA NOW PLAYING",
        title = track?.title ?: if (isPersian) "هیچ رسانه‌ای انتخاب نشده" else "No Active Track",
        subtitle = track?.let {
            "${it.artist} • ${it.album} • ${it.formatBadge} ${it.sampleRateHz / 1000.0}kHz"
        } ?: if (isPersian) "یک آهنگ از کتابخانه انتخاب کنید" else "Select a track from your library",
        headerAction = if (track != null && tier != ExperienceTier.SIMPLE) {
            {
                Row {
                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onOpenTagEditor,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_tag_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Metadata Tags",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            null
        }
    ) {
        val normalizedSpectrum = remember(engine.spectrumBandsDb) {
            engine.spectrumBandsDb.map { ((it + 60f) / 60f).coerceIn(0.05f, 1f) }
        }
        SpectrumAnalyzerCanvas(
            spectrumBands = normalizedSpectrum,
            eqBandGainsDb = engine.eqBandGainsDb,
            isPlaying = positionState.isPlaying,
            visualizerMode = engine.visualizerMode,
            modifier = Modifier.veloraCinemaGestures(
                controlsLocked = false,
                positionMs = positionState.positionMs,
                durationMs = positionState.durationMs,
                currentBrightness = uiState.videoCinemaSettings.brightnessLevel,
                currentVolume = engine.volume,
                onToggleControls = {},
                onInteraction = {},
                onSeekTo = onSeekTo,
                onTogglePlayPause = onTogglePlayPause,
                onSetBrightness = {},
                onSetVolume = onVolumeChange,
                onHudNotice = onHudNotice
            )
        )

        if (gestureHudNotice != null) {
            Spacer(modifier = Modifier.height(PhiTokens.Space4))
            Text(
                text = gestureHudNotice,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("player_gesture_hud_label")
            )
        }

        if (tier == ExperienceTier.PRO) {
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(VisualizerMode.entries) { mode ->
                    VeloraSecondaryButton(
                        text = mode.label,
                        icon = Icons.Default.GraphicEq,
                        selected = engine.visualizerMode == mode,
                        onClick = { onSetVisualizerMode(mode) },
                        tag = "visualizer_mode_${mode.name}"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(PhiTokens.Space13))

        VeloraSeekBar(
            positionMs = positionState.positionMs,
            durationMs = positionState.durationMs,
            onSeekTo = onSeekTo
        )

        Spacer(modifier = Modifier.height(PhiTokens.Space8))

        VeloraTransportControls(
            isPlaying = positionState.isPlaying,
            experienceTier = tier,
            queueMode = uiState.queue.mode,
            repeatMode = uiState.queue.repeatMode,
            onTogglePlayPause = onTogglePlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onToggleShuffle = onToggleShuffle,
            onCycleRepeat = onCycleRepeat
        )

        Spacer(modifier = Modifier.height(PhiTokens.Space13))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("transport_mute_button")
            ) {
                Icon(
                    imageVector = if (engine.isMuted) {
                        Icons.AutoMirrored.Filled.VolumeOff
                    } else {
                        Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = if (engine.isMuted) "Unmute" else "Mute",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = if (engine.isMuted) 0f else engine.volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_volume_slider")
            )
            if (tier != ExperienceTier.SIMPLE) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("transport_stop_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Playback",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VeloraSecondaryButton(
                    text = if (isPersian) "پرش دقیق" else "Seek ms",
                    icon = Icons.Default.Timer,
                    onClick = onOpenExactSeek,
                    tag = "open_exact_seek_button"
                )
            }
        }
    }
}
