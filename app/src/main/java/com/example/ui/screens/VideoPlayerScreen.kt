package com.example.ui.screens

import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.domain.engine.PlaybackStatus
import com.example.domain.model.GestureActionType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.SubtitleColorTheme
import com.example.domain.model.SubtitleFontScale
import com.example.domain.model.Track
import com.example.domain.model.VeloraGestureEngine
import com.example.domain.model.VideoAspectMode
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    uiState: FazoraUiState,
    exoPlayer: ExoPlayer?,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = uiState.engineState
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val isCurrentTrackVideo = engine.currentTrack?.mediaType == com.example.domain.model.MediaType.VIDEO
    val activeVideo = if (isCurrentTrackVideo) engine.currentTrack else uiState.videoTracks.firstOrNull()

    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var seekNotice by remember { mutableStateOf<String?>(null) }
    var controlsLocked by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionNonce by remember { mutableIntStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(seekNotice) {
        if (seekNotice != null) {
            delay(900L)
            seekNotice = null
        }
    }

    LaunchedEffect(controlsVisible, engine.isPlaying, interactionNonce, uiState.settings.autoHideVideoControlsMs) {
        if (controlsVisible && engine.isPlaying && !controlsLocked) {
            delay(uiState.settings.autoHideVideoControlsMs.coerceAtLeast(3500L))
            controlsVisible = false
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val fileName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() } ?: "Local Video"
            val pickedTrack = Track(
                id = "picked_video_${System.currentTimeMillis()}",
                mediaStoreId = System.currentTimeMillis(),
                title = fileName,
                artist = if (isPersian) "حافظه محلی" else "Device Storage",
                album = if (isPersian) "ویدیوهای دستگاه" else "Local Videos",
                durationMs = 0L,
                uri = uri.toString(),
                folderPath = "/storage/emulated/0/Movies",
                mediaType = com.example.domain.model.MediaType.VIDEO
            )
            onPlayVideoTrack(pickedTrack)
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() } ?: "Gallery Video"
            val pickedTrack = Track(
                id = "gallery_video_${System.currentTimeMillis()}",
                mediaStoreId = System.currentTimeMillis(),
                title = fileName,
                artist = if (isPersian) "گالری" else "Gallery",
                album = if (isPersian) "ویدیوهای دستگاه" else "Device Videos",
                durationMs = 0L,
                uri = uri.toString(),
                folderPath = "/storage/emulated/0/DCIM",
                mediaType = com.example.domain.model.MediaType.VIDEO
            )
            onPlayVideoTrack(pickedTrack)
        }
    }

    val durationMs = engine.durationMs.coerceAtLeast(activeVideo?.durationMs ?: 10_000L)
    val currentPosMs = if (isDraggingSeek) {
        (dragSeekFraction * durationMs).toLong()
    } else {
        engine.positionMs
    }
    val seekFraction = if (durationMs > 0L) {
        (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val nativeRatio = if (engine.videoWidth > 0 && engine.videoHeight > 0) {
        engine.videoWidth.toFloat() / engine.videoHeight.toFloat()
    } else {
        16f / 9f
    }

    val aspectRatioFloat = when (engine.videoAspectMode) {
        VideoAspectMode.FIT -> nativeRatio.coerceIn(0.5f, 2.4f)
        VideoAspectMode.FILL -> 16f / 9f
        VideoAspectMode.GOLDEN_PHI -> PhiTokens.PHI
        VideoAspectMode.SIXTEEN_NINE -> 16f / 9f
    }

    val resolutionLabel = when {
        engine.videoHeight >= 2160 -> "4K UHD"
        engine.videoHeight >= 1080 -> "1080p FHD"
        engine.videoHeight >= 720 -> "720p HD"
        engine.videoHeight > 0 -> "${engine.videoHeight}p"
        else -> "HD Video"
    }

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler { isFullscreen = false }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            val playerView = LayoutInflater.from(ctx).inflate(
                                R.layout.velora_video_player_view,
                                null,
                                false
                            ) as PlayerView
                            playerView.apply {
                                setEnableComposeSurfaceSyncWorkaround(true)
                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                useController = false
                                keepScreenOn = true
                                player = exoPlayer
                                onResume()
                            }
                        },
                        update = { view ->
                            if (view.player !== exoPlayer) {
                                view.player = exoPlayer
                            }
                            view.onResume()
                            view.resizeMode = when (engine.videoAspectMode) {
                                VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                VideoAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                VideoAspectMode.GOLDEN_PHI -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                VideoAspectMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        onRelease = { view -> view.player = null },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("fullscreen_media3_player_view")
                    )
                }

                val fullscreenDimAlpha = (1f - uiState.videoCinemaSettings.brightnessLevel).coerceIn(0f, 0.75f)
                if (fullscreenDimAlpha > 0.02f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = fullscreenDimAlpha))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(controlsLocked, engine.positionMs, durationMs) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (!controlsLocked) {
                                        interactionNonce++
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
                                                seekNotice = res.hudLabel
                                            }
                                            else -> {
                                                onTogglePlayPause()
                                                seekNotice = res.hudLabel
                                            }
                                        }
                                    }
                                },
                                onTap = {
                                    controlsVisible = !controlsVisible
                                    interactionNonce++
                                }
                            )
                        }
                        .pointerInput(controlsLocked, uiState.videoCinemaSettings.brightnessLevel, engine.volume) {
                            var dragStartX = 0f
                            detectVerticalDragGestures(
                                onDragStart = { offset -> dragStartX = offset.x },
                                onVerticalDrag = { change, dragAmount ->
                                    if (!controlsLocked) {
                                        change.consume()
                                        val swipe = VeloraGestureEngine.resolveVerticalSwipeGesture(
                                            startX = dragStartX,
                                            surfaceWidth = size.width.toFloat(),
                                            surfaceHeight = size.height.toFloat(),
                                            dragDeltaY = dragAmount,
                                            currentBrightness = uiState.videoCinemaSettings.brightnessLevel,
                                            currentVolume = engine.volume
                                        )
                                        if (swipe.action == GestureActionType.ADJUST_BRIGHTNESS) {
                                            onSetVideoBrightness(swipe.newValue)
                                        } else {
                                            onSetVolume(swipe.newValue)
                                        }
                                        seekNotice = swipe.hudLabel
                                    }
                                }
                            )
                        }
                )

                if (seekNotice != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius21))
                    ) {
                        Text(
                            text = seekNotice.orEmpty(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = FazoraGold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }

                if (engine.status == PlaybackStatus.BUFFERING) {
                    CircularProgressIndicator(
                        color = FazoraGold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(PhiTokens.Space21)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeVideo?.title ?: "Velora Cinema",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FazoraIvory,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$resolutionLabel • ${engine.videoAspectMode.label}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FazoraGold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { controlsLocked = !controlsLocked },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = if (controlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Lock controls",
                                            tint = if (controlsLocked) FazoraGold else FazoraSteel
                                        )
                                    }
                                    IconButton(
                                        onClick = { isFullscreen = false },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FullscreenExit,
                                            contentDescription = "Exit Fullscreen",
                                            tint = FazoraIvory
                                        )
                                    }
                                }
                            }

                            if (!controlsLocked) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    IconButton(
                                        onClick = {
                                            interactionNonce++
                                            onSeekTo((engine.positionMs - 10_000L).coerceAtLeast(0L))
                                            seekNotice = "-10s"
                                        },
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "-10s",
                                            tint = FazoraIvory,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            interactionNonce++
                                            if (!isCurrentTrackVideo && activeVideo != null) {
                                                onPlayVideoTrack(activeVideo)
                                            } else {
                                                onTogglePlayPause()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FazoraGold,
                                            contentColor = FazoraObsidian
                                        ),
                                        modifier = Modifier.size(68.dp),
                                        shape = CircleShape
                                    ) {
                                        val icon = if (engine.isPlaying && isCurrentTrackVideo) {
                                            Icons.Default.Pause
                                        } else {
                                            Icons.Default.PlayArrow
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            interactionNonce++
                                            onSeekTo(engine.positionMs + 10_000L)
                                            seekNotice = "+10s"
                                        },
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "+10s",
                                            tint = FazoraIvory,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Slider(
                                        value = seekFraction,
                                        onValueChange = { frac ->
                                            isDraggingSeek = true
                                            dragSeekFraction = frac
                                        },
                                        onValueChangeFinished = {
                                            isDraggingSeek = false
                                            onSeekTo((dragSeekFraction * durationMs).toLong())
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = FazoraGold,
                                            activeTrackColor = FazoraGold,
                                            inactiveTrackColor = FazoraSteel.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = Track.formatDurationMs(currentPosMs),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FazoraIvory
                                        )
                                        Text(
                                            text = Track.formatDurationMs(durationMs),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = FazoraSteel
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            containerColor = FazoraCarbonElevated,
            title = {
                Text(
                    text = if (isPersian) "تنظیمات زیرنویس ویدیو" else "Video Subtitles",
                    color = FazoraIvory
                )
            },
            text = {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetSubtitlesEnabled(!engine.subtitlesEnabled) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isPersian) "نمایش زیرنویس" else "Display Subtitles",
                            color = FazoraIvory
                        )
                        RadioButton(
                            selected = engine.subtitlesEnabled,
                            onClick = { onSetSubtitlesEnabled(!engine.subtitlesEnabled) },
                            colors = RadioButtonDefaults.colors(selectedColor = FazoraGold)
                        )
                    }

                    if (engine.subtitleTracks.isEmpty()) {
                        Text(
                            text = if (isPersian) {
                                "این ویدیو فاقد زیرنویس توکار است (زیرنویس هماهنگ‌شده LRC/SRT فعال است)."
                            } else {
                                "No embedded tracks; synchronized LRC/SRT overlay is active."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = FazoraSteel,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        engine.subtitleTracks.forEach { sub ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSetSubtitlesEnabled(true)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = sub.isSelected && engine.subtitlesEnabled,
                                    onClick = { onSetSubtitlesEnabled(true) },
                                    colors = RadioButtonDefaults.colors(selectedColor = FazoraGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${sub.label} (${sub.language})",
                                    color = FazoraIvory,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    VideoSubtitleStylePickerSection(
                        cinemaSettings = uiState.videoCinemaSettings,
                        isPersian = isPersian,
                        onSelectFontScale = onSetSubtitleFontScale,
                        onSelectColorTheme = onSetSubtitleColorTheme
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSubtitleDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FazoraGold,
                        contentColor = FazoraObsidian
                    )
                ) {
                    Text(if (isPersian) "تایید" else "OK")
                }
            }
        )
    }

    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            containerColor = FazoraCarbonElevated,
            title = {
                Text(
                    text = if (isPersian) "ترک صوتی و زبان ویدیو" else "Audio Stream / Language",
                    color = FazoraIvory
                )
            },
            text = {
                Column {
                    if (engine.audioTracks.isEmpty()) {
                        Text(
                            text = if (isPersian) "یک ترک صوتی استریو فعال است." else "Standard audio track is active.",
                            color = FazoraSteel
                        )
                    } else {
                        engine.audioTracks.forEach { aud ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAudioTrack(aud.id) }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = aud.isSelected,
                                    onClick = { onSelectAudioTrack(aud.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = FazoraGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${aud.label} (${aud.language})",
                                    color = FazoraIvory,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAudioDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FazoraGold,
                        contentColor = FazoraObsidian
                    )
                ) {
                    Text(if (isPersian) "بستن" else "Close")
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("video_player_screen")
    ) {
        item {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatioFloat)
                            .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
                            .background(Color.Black)
                            .pointerInput(engine.positionMs, durationMs) {
                                detectTapGestures(
                                    onDoubleTap = { offset ->
                                        interactionNonce++
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
                                                seekNotice = res.hudLabel
                                            }
                                            else -> {
                                                onTogglePlayPause()
                                                seekNotice = res.hudLabel
                                            }
                                        }
                                    },
                                    onTap = {
                                        controlsVisible = !controlsVisible
                                        interactionNonce++
                                    }
                                )
                            }
                            .pointerInput(uiState.videoCinemaSettings.brightnessLevel, engine.volume) {
                                var dragStartX = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { offset -> dragStartX = offset.x },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        val swipe = VeloraGestureEngine.resolveVerticalSwipeGesture(
                                            startX = dragStartX,
                                            surfaceWidth = size.width.toFloat(),
                                            surfaceHeight = size.height.toFloat(),
                                            dragDeltaY = dragAmount,
                                            currentBrightness = uiState.videoCinemaSettings.brightnessLevel,
                                            currentVolume = engine.volume
                                        )
                                        if (swipe.action == GestureActionType.ADJUST_BRIGHTNESS) {
                                            onSetVideoBrightness(swipe.newValue)
                                        } else {
                                            onSetVolume(swipe.newValue)
                                        }
                                        seekNotice = swipe.hudLabel
                                    }
                                )
                            }
                            .testTag("video_surface_container")
                    ) {
                        if (exoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    val playerView = LayoutInflater.from(ctx).inflate(
                                        R.layout.velora_video_player_view,
                                        null,
                                        false
                                    ) as PlayerView
                                    playerView.apply {
                                        setEnableComposeSurfaceSyncWorkaround(true)
                                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        useController = false
                                        keepScreenOn = true
                                        player = exoPlayer
                                        onResume()
                                    }
                                },
                                update = { view ->
                                    if (view.player !== exoPlayer) {
                                        view.player = exoPlayer
                                    }
                                    view.onResume()
                                    view.resizeMode = when (engine.videoAspectMode) {
                                        VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        VideoAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        VideoAspectMode.GOLDEN_PHI -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        VideoAspectMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                onRelease = { view -> view.player = null },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("media3_player_view")
                            )
                        }

                        val dimAlpha = (1f - uiState.videoCinemaSettings.brightnessLevel).coerceIn(0f, 0.75f)
                        if (dimAlpha > 0.02f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = dimAlpha))
                            )
                        }

                        if (engine.subtitlesEnabled) {
                            VideoSubtitleOverlay(
                                lyrics = uiState.activeLyrics,
                                positionMs = currentPosMs,
                                offsetMs = uiState.lyricsOffsetMs,
                                cinemaSettings = uiState.videoCinemaSettings,
                                isPersian = isPersian,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        if (!isCurrentTrackVideo && activeVideo != null) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.85f))
                                    .padding(PhiTokens.Space13)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = FazoraGold,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clickable { onPlayVideoTrack(activeVideo) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeVideo.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FazoraIvory
                                )
                                Text(
                                    text = if (isPersian) "برای پخش لمس کنید" else "Tap to Play Video",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FazoraSteel
                                )
                            }
                        }

                        if (seekNotice != null) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius21))
                            ) {
                                Text(
                                    text = seekNotice.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = FazoraGold,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }

                        if (engine.status == PlaybackStatus.BUFFERING) {
                            CircularProgressIndicator(
                                color = FazoraGold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        this@Column.AnimatedVisibility(
                            visible = controlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(FazoraObsidian.copy(alpha = 0.62f))
                                    .padding(PhiTokens.Space13)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeVideo?.title ?: "Velora Cinema",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = FazoraIvory,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "$resolutionLabel • ${engine.videoAspectMode.label} • ${engine.playbackSpeed}x",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = FazoraGold
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedButton(
                                                onClick = { onEnterPip() },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("video_pip_button")
                                            ) {
                                                Text("PiP", color = FazoraGold)
                                            }
                                            IconButton(
                                                onClick = {
                                                    isFullscreen = true
                                                    interactionNonce++
                                                },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("video_fullscreen_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fullscreen,
                                                    contentDescription = stringResource(R.string.cd_video_fullscreen),
                                                    tint = FazoraGold
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                interactionNonce++
                                                onSeekTo((engine.positionMs - 10_000L).coerceAtLeast(0L))
                                                seekNotice = "-10s"
                                            },
                                            modifier = Modifier.testTag("video_seek_back_10s")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Replay10,
                                                contentDescription = "-10s",
                                                tint = FazoraIvory
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("-10s", color = FazoraIvory)
                                        }

                                        Button(
                                            onClick = {
                                                interactionNonce++
                                                if (!isCurrentTrackVideo && activeVideo != null) {
                                                    onPlayVideoTrack(activeVideo)
                                                } else {
                                                    onTogglePlayPause()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = FazoraGold,
                                                contentColor = FazoraObsidian
                                            ),
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("video_play_pause_button")
                                        ) {
                                            val playPauseIcon = if (engine.isPlaying && isCurrentTrackVideo) {
                                                Icons.Default.Pause
                                            } else {
                                                Icons.Default.PlayArrow
                                            }
                                            val playPauseLabel = if (engine.isPlaying && isCurrentTrackVideo) {
                                                if (isPersian) "توقف ویدیو" else "Pause Video"
                                            } else {
                                                if (isPersian) "پخش ویدیو" else "Play Video"
                                            }
                                            Icon(
                                                imageVector = playPauseIcon,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                                            Text(playPauseLabel)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                interactionNonce++
                                                onSeekTo(engine.positionMs + 10_000L)
                                                seekNotice = "+10s"
                                            },
                                            modifier = Modifier.testTag("video_seek_fwd_10s")
                                        ) {
                                            Text("+10s", color = FazoraIvory)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Forward10,
                                                contentDescription = "+10s",
                                                tint = FazoraIvory
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Slider(
                                            value = seekFraction,
                                            onValueChange = { fraction ->
                                                interactionNonce++
                                                isDraggingSeek = true
                                                dragSeekFraction = fraction.coerceIn(0f, 1f)
                                            },
                                            onValueChangeFinished = {
                                                val target = PlaybackQueue.clampSeekPosition(
                                                    (dragSeekFraction * durationMs).toLong(),
                                                    durationMs
                                                )
                                                isDraggingSeek = false
                                                onSeekTo(target)
                                            },
                                            colors = SliderDefaults.colors(
                                                thumbColor = FazoraGold,
                                                activeTrackColor = FazoraGold,
                                                inactiveTrackColor = FazoraCarbonElevated
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("video_seek_slider")
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = Track.formatDurationMs(currentPosMs),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = FazoraIvory
                                            )
                                            Text(
                                                text = Track.formatDurationMs(durationMs),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = FazoraSteel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Text(
                        text = if (isPersian) "سرعت پخش ویدیو" else "Playback Speed",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(speedOptions) { speed ->
                            val isSelected = kotlin.math.abs(engine.playbackSpeed - speed) < 0.01f
                            OutlinedButton(
                                onClick = {
                                    interactionNonce++
                                    onSetPlaybackSpeed(speed)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                    contentColor = if (isSelected) FazoraObsidian else FazoraIvory
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_speed_${speed}")
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${speed}x")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Text(
                        text = if (isPersian) "نسبت ابعاد تصویر" else "Aspect Ratio & Framing",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(VideoAspectMode.entries) { mode ->
                            val isSelected = engine.videoAspectMode == mode
                            OutlinedButton(
                                onClick = {
                                    interactionNonce++
                                    onSetAspectMode(mode)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                    contentColor = if (isSelected) FazoraObsidian else FazoraIvory
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_aspect_${mode.name}")
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mode.label)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showSubtitleDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("video_subtitles_toggle")
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(
                                text = if (isPersian) {
                                    if (engine.subtitlesEnabled) "زیرنویس: روشن" else "زیرنویس: خاموش"
                                } else {
                                    if (engine.subtitlesEnabled) "Subtitles: ON" else "Subtitles: OFF"
                                },
                                color = FazoraIvory
                            )
                        }

                        OutlinedButton(
                            onClick = { showAudioDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("video_audio_track_button")
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(
                                text = if (isPersian) {
                                    "ترک‌های صوتی (${engine.audioTracks.size.coerceAtLeast(1)})"
                                } else {
                                    "Audio Tracks (${engine.audioTracks.size.coerceAtLeast(1)})"
                                },
                                color = FazoraIvory
                            )
                        }

                        OutlinedButton(
                            onClick = onAddVideoBookmark,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("video_add_bookmark_button")
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPersian) "بوکمارک صحنه" else "Bookmark Scene", color = FazoraIvory)
                        }
                    }

                    if (uiState.timeMachineBookmarks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            items(uiState.timeMachineBookmarks, key = { "vbm_${it.id}" }) { bm ->
                                OutlinedButton(
                                    onClick = { onSeekTo(bm.timestampMs) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("🔖 ${bm.formattedTimestamp} • ${bm.trackTitle}", color = FazoraGold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FazoraGold,
                                contentColor = FazoraObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("pick_video_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(if (isPersian) "انتخاب ویدیو" else "Pick Video")
                        }

                        OutlinedButton(
                            onClick = { galleryPickerLauncher.launch("video/*") },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("gallery_video_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(if (isPersian) "گالری فیلم" else "From Gallery", color = FazoraIvory)
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space8))

                    OutlinedButton(
                        onClick = onAutoScanMedia,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("autoscan_video_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = FazoraGold)
                        Spacer(modifier = Modifier.width(PhiTokens.Space8))
                        Text(
                            text = if (isPersian) {
                                "🔄 جست‌وجو و اسکن خودکار ویدیوهای حافظه"
                            } else {
                                "🔄 Auto-Scan Device Videos"
                            },
                            color = FazoraIvory
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isPersian) {
                    "ویدیوهای موجود (${uiState.videoTracks.size})"
                } else {
                    "Video Streams (${uiState.videoTracks.size})"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = FazoraIvory
            )
        }

        items(uiState.videoTracks, key = { it.id }) { track ->
            TrackRowItem(
                track = track,
                isActive = engine.currentTrack?.id == track.id,
                playlists = uiState.playlists,
                onPlay = { onPlayVideoTrack(track) },
                onAddToQueue = { onAddTrackToQueue(track) },
                onAddToPlaylist = { playlistId -> onAddTrackToPlaylist(playlistId, track.id) },
                onToggleFavorite = { onToggleFavorite(track) }
            )
        }
    }
}
