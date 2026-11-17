package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.AudioTrackInfo
import com.example.domain.model.ExperienceTier
import com.example.domain.model.FullscreenPolicy
import com.example.domain.model.SubtitleColorTheme
import com.example.domain.model.SubtitleFontScale
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VideoCinemaSettings
import com.example.domain.model.VideoControlGroup
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSeekBar
import com.example.ui.components.VeloraVideoSurface
import com.example.ui.components.veloraCinemaGestures
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import com.example.ui.viewmodel.PlaybackPositionState

@Composable
internal fun VideoControlToolbar(
    uiState: FazoraUiState,
    tier: ExperienceTier,
    isPersian: Boolean,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onEnterPip: () -> Unit,
    onPickVideo: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAspectMode: (VideoAspectMode) -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onAddVideoBookmark: () -> Unit
) {
    val engine = uiState.engineState
    Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.TRANSPORT)) {
                VeloraPrimaryButton(
                    text = if (isPlaying) {
                        if (isPersian) "توقف" else "Pause"
                    } else {
                        if (isPersian) "پخش" else "Play"
                    },
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = onTogglePlayPause,
                    tag = "video_play_pause_button"
                )
            }
            if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.FULLSCREEN)) {
                VeloraSecondaryButton(
                    text = if (isPersian) "تمام‌صفحه" else "Fullscreen",
                    icon = Icons.Default.Fullscreen,
                    onClick = onEnterFullscreen,
                    tag = "video_fullscreen_button"
                )
            }
            if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.PIP)) {
                VeloraSecondaryButton(
                    text = "PiP",
                    icon = Icons.Default.PictureInPictureAlt,
                    onClick = onEnterPip,
                    tag = "video_pip_button"
                )
            }
            if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.PICK_VIDEO)) {
                VeloraSecondaryButton(
                    text = if (isPersian) "اسکن" else "Scan",
                    icon = Icons.Default.Refresh,
                    onClick = onPickVideo,
                    tag = "pick_device_video_button"
                )
            }
        }

        if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.SPEED)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)) { speed ->
                    VeloraSecondaryButton(
                        text = "${speed}x",
                        icon = Icons.Default.Speed,
                        selected = engine.playbackSpeed == speed,
                        onClick = { onSetPlaybackSpeed(speed) },
                        tag = "video_speed_chip_$speed"
                    )
                }
            }
        }

        if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.ASPECT)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(VideoAspectMode.entries) { mode ->
                    VeloraSecondaryButton(
                        text = mode.label,
                        icon = Icons.Default.AspectRatio,
                        selected = engine.videoAspectMode == mode,
                        onClick = { onSetAspectMode(mode) },
                        tag = "video_aspect_chip_${mode.name}"
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
            if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.SUBTITLES)) {
                VeloraSecondaryButton(
                    text = if (isPersian) "زیرنویس" else "Subtitles",
                    icon = Icons.Default.Subtitles,
                    selected = engine.subtitlesEnabled,
                    onClick = onOpenSubtitles,
                    tag = "video_subtitle_button"
                )
            }
            if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.AUDIO_TRACK)) {
                VeloraSecondaryButton(
                    text = if (isPersian) "ترک صوتی" else "Audio Track",
                    icon = Icons.Default.Audiotrack,
                    onClick = onOpenAudioTracks,
                    tag = "video_audio_track_button"
                )
            }
            if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.BOOKMARK)) {
                VeloraSecondaryButton(
                    text = if (isPersian) "نشانک صحنه" else "Bookmark",
                    icon = Icons.Default.BookmarkAdd,
                    onClick = onAddVideoBookmark,
                    tag = "video_bookmark_button"
                )
            }
        }
    }
}

@Composable
internal fun FullscreenVideoDialog(
    context: Context,
    playerEngine: PlayerEngine?,
    uiState: FazoraUiState,
    positionState: PlaybackPositionState,
    controlsLocked: Boolean,
    gestureHudLabel: String?,
    activeSubtitleText: String,
    onToggleControls: () -> Unit,
    onToggleLock: () -> Unit,
    onExitFullscreen: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSetBrightness: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onHudNotice: (String) -> Unit
) {
    val engine = uiState.engineState
    val nativeRatio = when (engine.videoAspectMode) {
        VideoAspectMode.GOLDEN_PHI -> PhiTokens.PHI
        VideoAspectMode.SIXTEEN_NINE, VideoAspectMode.FIT, VideoAspectMode.FILL -> 16f / 9f
    }

    DisposableEffect(nativeRatio) {
        val activity = context as? Activity
        val prevOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity?.window
        val insetsController = if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else {
            null
        }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        if (FullscreenPolicy.shouldLockLandscape(nativeRatio)) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = prevOrientation
        }
    }

    Dialog(
        onDismissRequest = onExitFullscreen,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .veloraCinemaGestures(
                    controlsLocked = controlsLocked,
                    positionMs = positionState.positionMs,
                    durationMs = positionState.durationMs,
                    currentBrightness = uiState.videoCinemaSettings.brightnessLevel,
                    currentVolume = engine.volume,
                    onToggleControls = onToggleControls,
                    onInteraction = {},
                    onSeekTo = onSeekTo,
                    onTogglePlayPause = onTogglePlayPause,
                    onSetBrightness = onSetBrightness,
                    onSetVolume = onSetVolume,
                    onHudNotice = onHudNotice
                )
                .testTag("fullscreen_video_dialog")
        ) {
            VeloraVideoSurface(
                playerEngine = playerEngine,
                videoAspectMode = engine.videoAspectMode,
                surfaceTestTag = "fullscreen_video_surface",
                modifier = Modifier.fillMaxSize()
            )

            if (engine.subtitlesEnabled && activeSubtitleText.isNotBlank()) {
                VideoSubtitleOverlay(
                    text = activeSubtitleText,
                    cinemaSettings = uiState.videoCinemaSettings,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PhiTokens.Space34)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(PhiTokens.Space21)
            ) {
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (controlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Toggle Lock",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (!controlsLocked) {
                    IconButton(
                        onClick = onExitFullscreen,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("exit_fullscreen_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!controlsLocked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(PhiTokens.Space21)
                ) {
                    if (gestureHudLabel != null) {
                        Text(
                            text = gestureHudLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    VeloraSeekBar(
                        positionMs = positionState.positionMs,
                        durationMs = positionState.durationMs,
                        onSeekTo = onSeekTo,
                        sliderTestTag = "fullscreen_video_seek_slider"
                    )
                }
            }
        }
    }
}

@Composable
fun VideoSubtitleOverlay(
    text: String,
    cinemaSettings: VideoCinemaSettings,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(PhiTokens.CornerRadius8),
        modifier = modifier.testTag("video_subtitle_overlay")
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = when (cinemaSettings.subtitleColorTheme) {
                SubtitleColorTheme.SAFFRON_ON_DARK -> MaterialTheme.colorScheme.primary
                SubtitleColorTheme.YELLOW_HIGH_CONTRAST -> MaterialTheme.colorScheme.secondary
                SubtitleColorTheme.CYAN_STUDIO -> MaterialTheme.colorScheme.tertiary
                SubtitleColorTheme.WHITE_ON_DARK -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space4)
        )
    }
}

@Composable
internal fun VideoSubtitlesDialog(
    uiState: FazoraUiState,
    isPersian: Boolean,
    onSetSubtitlesEnabled: (Boolean) -> Unit,
    onSetSubtitleFontScale: (SubtitleFontScale) -> Unit,
    onSetSubtitleColorTheme: (SubtitleColorTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (isPersian) "تنظیمات زیرنویس" else "Subtitles & Caption Styling") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.testTag("subtitle_style_picker_section")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isPersian) "نمایش زیرنویس" else "Enable Subtitles")
                    Switch(
                        checked = uiState.engineState.subtitlesEnabled,
                        onCheckedChange = onSetSubtitlesEnabled,
                        modifier = Modifier.testTag("video_subtitle_enable_switch")
                    )
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                    modifier = Modifier.testTag("subtitle_font_scale_slider")
                ) {
                    items(SubtitleFontScale.entries) { scale ->
                        VeloraSecondaryButton(
                            text = if (isPersian) scale.labelFa else scale.labelEn,
                            selected = uiState.videoCinemaSettings.subtitleFontScale == scale,
                            onClick = { onSetSubtitleFontScale(scale) },
                            tag = "subtitle_scale_${scale.name}"
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(SubtitleColorTheme.entries) { theme ->
                        VeloraSecondaryButton(
                            text = theme.labelEn,
                            selected = uiState.videoCinemaSettings.subtitleColorTheme == theme,
                            onClick = { onSetSubtitleColorTheme(theme) },
                            tag = "subtitle_color_${theme.name}"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Done") }
        }
    )
}

@Composable
internal fun VideoAudioTrackDialog(
    audioTracks: List<AudioTrackInfo>,
    isPersian: Boolean,
    onSelectAudioTrack: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (isPersian) "انتخاب ترک صوتی" else "Select Audio Track") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                if (audioTracks.isEmpty()) {
                    Text(
                        text = if (isPersian) "ترک صوتی پیش‌فرض فعال است." else "Default embedded audio stream active.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    audioTracks.forEach { track ->
                        VeloraSecondaryButton(
                            text = "${track.label} (${track.language})",
                            selected = track.isSelected,
                            onClick = {
                                onSelectAudioTrack(track.id)
                                onDismiss()
                            },
                            tag = "audio_track_option_${track.id}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )
}
