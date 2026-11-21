package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.ExperienceTier
import com.example.domain.model.FullscreenPolicy
import com.example.domain.model.SubtitleColorTheme
import com.example.domain.model.SubtitleFontScale
import com.example.domain.model.Track
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VideoControlGroup
import com.example.ui.components.TrackRowItem
import com.example.ui.components.VeloraAnimatedReveal
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraErrorBanner
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.components.VeloraSeekBar
import com.example.ui.components.VeloraVideoSurface
import com.example.ui.components.formatBadge
import com.example.ui.components.formattedDuration
import com.example.ui.components.veloraCinemaGestures
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import com.example.ui.viewmodel.PlaybackPositionState

@Composable
fun VideoPlayerScreen(
    uiState: FazoraUiState,
    playerEngine: PlayerEngine?,
    positionState: PlaybackPositionState = PlaybackPositionState(
        positionMs = uiState.engineState.positionMs,
        durationMs = uiState.engineState.durationMs,
        isPlaying = uiState.engineState.isPlaying
    ),
    onPlayVideoTrack: (Track) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAspectMode: (VideoAspectMode) -> Unit,
    onSetSubtitlesEnabled: (Boolean) -> Unit,
    onSelectAudioTrack: (String) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit = {},
    onEnterPip: () -> Unit = {},
    onAutoScanMedia: () -> Unit = {},
    onSetVolume: (Float) -> Unit = {},
    onSetVideoBrightness: (Float) -> Unit = {},
    onSetSubtitleFontScale: (SubtitleFontScale) -> Unit = {},
    onSetSubtitleColorTheme: (SubtitleColorTheme) -> Unit = {},
    onAddVideoBookmark: () -> Unit = {},
    isSimpleMode: Boolean = uiState.settings.isSimpleMode,
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val tier = if (isSimpleMode && uiState.settings.experienceTier == ExperienceTier.PRO) {
        ExperienceTier.SIMPLE
    } else {
        uiState.settings.experienceTier
    }
    val engine = uiState.engineState
    val context = LocalContext.current

    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var gestureHudLabel by remember { mutableStateOf<String?>(null) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { onAutoScanMedia() }

    val activeSubtitleText = remember(uiState.activeLyrics, positionState.positionMs, uiState.lyricsOffsetMs) {
        val idx = VeloraLyricsAndSubtitleEngine.findActiveLineIndex(
            uiState.activeLyrics,
            positionState.positionMs,
            uiState.lyricsOffsetMs
        )
        uiState.activeLyrics.getOrNull(idx)?.text.orEmpty()
    }

    if (showSubtitleDialog && FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.SUBTITLES)) {
        VideoSubtitlesDialog(
            uiState = uiState,
            isPersian = isPersian,
            onSetSubtitlesEnabled = onSetSubtitlesEnabled,
            onSetSubtitleFontScale = onSetSubtitleFontScale,
            onSetSubtitleColorTheme = onSetSubtitleColorTheme,
            onDismiss = { showSubtitleDialog = false }
        )
    }

    if (showAudioTrackDialog && FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.AUDIO_TRACK)) {
        VideoAudioTrackDialog(
            audioTracks = engine.audioTracks,
            isPersian = isPersian,
            onSelectAudioTrack = onSelectAudioTrack,
            onDismiss = { showAudioTrackDialog = false }
        )
    }

    if (isFullscreen) {
        FullscreenVideoDialog(
            context = context,
            playerEngine = playerEngine,
            uiState = uiState,
            positionState = positionState,
            controlsLocked = controlsLocked,
            gestureHudLabel = gestureHudLabel,
            activeSubtitleText = activeSubtitleText,
            onToggleControls = { controlsVisible = !controlsVisible },
            onToggleLock = { controlsLocked = !controlsLocked },
            onExitFullscreen = { isFullscreen = false },
            onSeekTo = onSeekTo,
            onTogglePlayPause = onTogglePlayPause,
            onSetBrightness = onSetVideoBrightness,
            onSetVolume = onSetVolume,
            onHudNotice = { gestureHudLabel = it }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("video_player_screen")
    ) {
        engine.errorMessage?.let { msg ->
            item {
                VeloraErrorBanner(
                    message = msg,
                    actionLabel = if (isPersian) "تلاش مجدد" else "Retry",
                    onAction = onTogglePlayPause
                )
            }
        }

        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "موتور سینمایی ویدیو" else "VELORA CINEMA ENGINE",
                title = engine.currentTrack?.title ?: if (isPersian) "آماده پخش ویدیو" else "Cinema Viewport",
                subtitle = engine.currentTrack?.let {
                    "${it.formatBadge} • ${it.formattedDuration}"
                } ?: if (isPersian) "یک ویدیو از فهرست پایین انتخاب کنید" else "Select a video stream below"
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PhiTokens.VideoSurfaceHeight)
                        .clip(RoundedCornerShape(PhiTokens.CornerRadius21))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .veloraCinemaGestures(
                            controlsLocked = controlsLocked,
                            positionMs = positionState.positionMs,
                            durationMs = positionState.durationMs,
                            currentBrightness = uiState.videoCinemaSettings.brightnessLevel,
                            currentVolume = engine.volume,
                            onToggleControls = { controlsVisible = !controlsVisible },
                            onInteraction = {},
                            onSeekTo = onSeekTo,
                            onTogglePlayPause = onTogglePlayPause,
                            onSetBrightness = onSetVideoBrightness,
                            onSetVolume = onSetVolume,
                            onHudNotice = { gestureHudLabel = it }
                        )
                        .testTag("video_gesture_container")
                ) {
                    VeloraVideoSurface(
                        playerEngine = playerEngine,
                        videoAspectMode = engine.videoAspectMode,
                        surfaceTestTag = "inline_video_surface",
                        modifier = Modifier.fillMaxSize()
                    )

                    if (engine.subtitlesEnabled && activeSubtitleText.isNotBlank()) {
                        VideoSubtitleOverlay(
                            text = activeSubtitleText,
                            cinemaSettings = uiState.videoCinemaSettings,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(PhiTokens.Space13)
                        )
                    }

                    IconButton(
                        onClick = { controlsLocked = !controlsLocked },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .minimumInteractiveComponentSize()
                            .testTag(if (controlsLocked) "video_unlock_controls_button" else "video_lock_controls_button")
                    ) {
                        Icon(
                            imageVector = if (controlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (controlsLocked) "Unlock Controls" else "Lock Controls",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                VeloraAnimatedReveal(
                    visible = gestureHudLabel != null,
                    label = "video_hud_reveal"
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(PhiTokens.Space4))
                        Text(
                            text = gestureHudLabel.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("video_gesture_hud_label")
                        )
                    }
                }

                if (!controlsLocked && controlsVisible) {
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    VeloraSeekBar(
                        positionMs = positionState.positionMs,
                        durationMs = positionState.durationMs,
                        onSeekTo = onSeekTo,
                        sliderTestTag = "video_seek_slider"
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    VideoControlToolbar(
                        uiState = uiState,
                        tier = tier,
                        isPersian = isPersian,
                        isPlaying = positionState.isPlaying,
                        onTogglePlayPause = onTogglePlayPause,
                        onEnterFullscreen = { isFullscreen = true },
                        onEnterPip = onEnterPip,
                        onPickVideo = { permissionLauncher.launch(permissionsToRequest) },
                        onSetPlaybackSpeed = onSetPlaybackSpeed,
                        onSetAspectMode = onSetAspectMode,
                        onOpenSubtitles = { showSubtitleDialog = true },
                        onOpenAudioTracks = { showAudioTrackDialog = true },
                        onAddVideoBookmark = onAddVideoBookmark
                    )
                }
            }
        }

        if (uiState.videoTracks.isEmpty()) {
            item {
                VeloraEmptyState(
                    icon = Icons.Default.Movie,
                    title = if (isPersian) "ویدیویی یافت نشد" else "No Video Streams Indexed",
                    subtitle = if (isPersian) {
                        "برای اسکن ویدیوهای حافظه دستگاه دکمه اسکن را لمس کنید."
                    } else {
                        "Scan your device storage to discover MP4, MKV, and WebM videos."
                    },
                    actionLabel = if (isPersian) "اسکن ویدیوهای دستگاه" else "Scan Device Videos",
                    actionTag = "empty_video_scan_button",
                    onAction = { permissionLauncher.launch(permissionsToRequest) }
                )
            }
        } else {
            items(uiState.videoTracks, key = { it.id }) { video ->
                TrackRowItem(
                    track = video,
                    isActive = engine.currentTrack?.id == video.id,
                    playlists = uiState.playlists,
                    onPlay = { onPlayVideoTrack(video) },
                    onAddToQueue = { onAddTrackToQueue(video) },
                    onAddToPlaylist = { pid -> onAddTrackToPlaylist(pid, video.id) },
                    onToggleFavorite = { onToggleFavorite(video) }
                )
            }
        }
    }
}
