package com.example.ui.screens

import android.content.Intent
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
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
import com.example.domain.model.FullscreenPolicy
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

// ---------------------------------------------------------------------------
// Shared surface gestures (double-tap zones + vertical-drag brightness /
// volume). One implementation for the embedded surface and fullscreen —
// previously the two copies had drifted apart (embedded ignored the lock,
// fullscreen honoured it; the unified rule is: the lock gates gestures,
// a plain tap always toggles the controls).
// ---------------------------------------------------------------------------

private fun Modifier.videoSurfaceGestures(
    controlsLocked: Boolean,
    positionMs: Long,
    durationMs: Long,
    brightnessLevel: Float,
    volume: Float,
    // Fullscreen only: the embedded player sits in a scrollable list and
    // vertical swipes fight list scrolling, so they stay off there.
    verticalSwipesEnabled: Boolean = true,
    onBumpInteraction: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleControls: () -> Unit,
    onSetVideoBrightness: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onNotice: (String) -> Unit
): Modifier = this
    .pointerInput(controlsLocked, positionMs, durationMs) {
        detectTapGestures(
            onDoubleTap = { offset ->
                if (!controlsLocked) {
                    onBumpInteraction()
                    val res = VeloraGestureEngine.resolveDoubleTapGesture(
                        tapX = offset.x,
                        surfaceWidth = size.width.toFloat(),
                        currentPositionMs = positionMs,
                        durationMs = durationMs
                    )
                    when (res.action) {
                        GestureActionType.SEEK_BACKWARD,
                        GestureActionType.SEEK_FORWARD -> {
                            onSeekTo(res.targetPositionMs)
                            onNotice(res.hudLabel)
                        }
                        else -> {
                            onTogglePlayPause()
                            onNotice(res.hudLabel)
                        }
                    }
                }
            },
            onTap = { onToggleControls() }
        )
    }
    .pointerInput(controlsLocked, brightnessLevel, volume, verticalSwipesEnabled) {
        var dragStartX = 0f
        detectVerticalDragGestures(
            onDragStart = { offset -> dragStartX = offset.x },
            onVerticalDrag = { change, dragAmount ->
                if (!controlsLocked && verticalSwipesEnabled) {
                    change.consume()
                    val swipe = VeloraGestureEngine.resolveVerticalSwipeGesture(
                        startX = dragStartX,
                        surfaceWidth = size.width.toFloat(),
                        surfaceHeight = size.height.toFloat(),
                        dragDeltaY = dragAmount,
                        currentBrightness = brightnessLevel,
                        currentVolume = volume
                    )
                    if (swipe.action == GestureActionType.ADJUST_BRIGHTNESS) {
                        onSetVideoBrightness(swipe.newValue)
                    } else {
                        onSetVolume(swipe.newValue)
                    }
                    onNotice(swipe.hudLabel)
                }
            }
        )
    }

// ---------------------------------------------------------------------------
// The Media3 surface itself — one implementation for embedded + fullscreen.
// ---------------------------------------------------------------------------

@OptIn(UnstableApi::class)
@Composable
private fun Media3PlayerSurface(
    exoPlayer: ExoPlayer?,
    aspectMode: VideoAspectMode,
    testTag: String,
    modifier: Modifier = Modifier
) {
    if (exoPlayer == null) return
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
            view.resizeMode = when (aspectMode) {
                VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoAspectMode.GOLDEN_PHI -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                VideoAspectMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        onRelease = { view -> view.player = null },
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag)
    )
}

// ---------------------------------------------------------------------------
// Seek bar: a real M3 Slider (battle-tested drag handling, 48dp touch
// height) with its own visuals drawn transparent, plus our own track:
// inactive bar, buffered fill, played fill, and a 28dp thumb. The Slider
// sits on top and takes all input; the visuals underneath never intercept
// touches. Geometry follows seekThumbCenterX() — see the note above.
// ---------------------------------------------------------------------------

@Composable
private fun VideoSeekBar(
    fraction: Float,
    bufferedFraction: Float,
    currentPosMs: Long,
    durationMs: Long,
    isDragging: Boolean,
    dragFraction: Float,
    onDragging: (Float) -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // LTR only: media progress is tape direction, never reading direction
    // (M3 bidirectionality rule) — the bar is wrapped in an LTR layout
    // direction at the call site, so no mirroring happens here.
    val density = LocalDensity.current
    val displayedFraction = if (isDragging) dragFraction else fraction
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val thumbRadiusPx = with(density) { 10.dp.toPx() }
            val travelPx = (widthPx - 2f * thumbRadiusPx).coerceAtLeast(0f)
            val trackStartDp = with(density) { thumbRadiusPx.toDp() }
            val trackWidthDp = with(density) { travelPx.toDp() }
            val playedPx = displayedFraction.coerceIn(0f, 1f) * travelPx
            val bufferedPx = bufferedFraction.coerceIn(0f, 1f) * travelPx
            val playedWidthDp = with(density) { playedPx.toDp() }
            val bufferedWidthDp = with(density) { bufferedPx.toDp() }
            val playedXDp = with(density) { thumbRadiusPx.toDp() }
            val bufferedXDp = with(density) { thumbRadiusPx.toDp() }
            val thumbCxPx = seekThumbCenterX(displayedFraction, widthPx, thumbRadiusPx)
            val thumbOffsetDp = with(density) { (thumbCxPx - 14.dp.toPx()).toDp() }

            // Inactive track.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = trackStartDp)
                    .width(trackWidthDp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(FazoraCarbonElevated)
            )
            // Buffered fill (what is already downloaded).
            if (bufferedPx > 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = bufferedXDp)
                        .width(bufferedWidthDp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(FazoraSteel)
                )
            }
            // Played fill.
            if (playedPx > 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = playedXDp)
                        .width(playedWidthDp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(FazoraGold)
                )
            }
            // 28dp thumb (the Slider's own thumb is transparent, see below).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffsetDp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(FazoraGold)
                    .border(2.dp, FazoraObsidian, CircleShape)
            )
            // The real Slider on top: invisible visuals, real drag handling.
            Slider(
                value = displayedFraction,
                onValueChange = { f -> onDragging(f.coerceIn(0f, 1f)) },
                onValueChangeFinished = {
                    val target = PlaybackQueue.clampSeekPosition(
                        (dragFraction * durationMs).toLong(),
                        durationMs
                    )
                    onSeekTo(target)
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_seek_slider")
            )
        }
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

// ---------------------------------------------------------------------------
// One-time gesture hint: shown the first time the player opens, until the
// user taps "Got it" (persisted via settings) — double-tap ±10s, vertical
// swipe for brightness (left half) / volume (right half).
// ---------------------------------------------------------------------------

@Composable
private fun VideoGestureHintBanner(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier
            .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius13))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PhiTokens.Space13)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "👆👆 دو ضربه: عقب / جلو ۱۰ ثانیه" else "👆👆 Double-tap: back / forward 10s",
                    style = MaterialTheme.typography.labelLarge,
                    color = FazoraIvory
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPersian) "↕️ کشیدن عمودی در تمام‌صفحه — چپ: روشنایی، راست: صدا" else "↕️ Swipe vertically in fullscreen — left: brightness, right: volume",
                    style = MaterialTheme.typography.labelMedium,
                    color = FazoraSteel
                )
            }
            Spacer(modifier = Modifier.width(PhiTokens.Space8))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("video_gesture_hint_dismiss")
            ) {
                Text(
                    text = if (isPersian) "فهمیدم" else "Got it",
                    color = FazoraGold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Centred state card for the three terminal/empty states: playback error
// (one-tap retry), ended (replay), and the no-video library (pick a video).
// ---------------------------------------------------------------------------

@Composable
private fun VideoStateOverlayCard(
    icon: ImageVector,
    title: String,
    message: String?,
    actionLabel: String,
    actionTestTag: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = modifier
            .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius21))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PhiTokens.Space21)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FazoraGold,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = FazoraIvory
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(PhiTokens.Space8))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FazoraSteel
                )
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space13))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(actionTestTag)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
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
    onSelectSubtitleTrack: (String?) -> Unit = {},
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
    onVideoGestureHintSeen: () -> Unit = {},
    isSimpleMode: Boolean = true,
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
    var showMoreSheet by remember { mutableStateOf(false) }
    var showGesturesHelpDialog by remember { mutableStateOf(false) }
    var hintDismissedLocally by remember { mutableStateOf(false) }
    val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
    // Buffered progress, read off the player the screen already holds. It
    // refreshes whenever the screen recomposes (position ticks), which is
    // plenty for a buffered bar — no engine change needed.
    val bufferedFraction = if (durationMs > 0L) {
        ((exoPlayer?.bufferedPosition ?: 0L).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
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

    val showGestureHint = !uiState.settings.videoGestureHintSeen &&
        !hintDismissedLocally &&
        activeVideo != null
    val isEmptyVideoLibrary = uiState.videoTracks.isEmpty() && activeVideo == null
    val isPlaybackError = engine.status == PlaybackStatus.ERROR
    val isPlaybackEnded = engine.status == PlaybackStatus.ENDED

    // Req 3 — true immersive fullscreen: hide system bars + lock landscape
    // for landscape video while fullscreen is open; restore both on exit.
    // The landscape decision is the tested pure rule in FullscreenPolicy.
    val activity = context as? Activity
    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            val window = activity?.window
            val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val previousOrientation = activity?.requestedOrientation
                ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (FullscreenPolicy.shouldLockLandscape(nativeRatio)) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
            onDispose {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
                activity?.requestedOrientation = previousOrientation
            }
        } else {
            onDispose { }
        }
    }

    // -----------------------------------------------------------------------
    // ONE shared surface: embedded and fullscreen render the same Box —
    // player, dim, subtitles, tap-to-play cover, HUD, spinner, state cards,
    // and the shared controls overlay. Previously the two copies had
    // drifted apart (subtitles only embedded, lock only fullscreen, the
    // fullscreen seek never clamped, different play buttons).
    // -----------------------------------------------------------------------

    @Composable
    fun PlayerSurface(isFullscreenSurface: Boolean) {
        Box(
            modifier = Modifier
                .then(
                    if (isFullscreenSurface) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatioFloat)
                    }
                )
                .clip(
                    RoundedCornerShape(
                        if (isFullscreenSurface) 0.dp else PhiTokens.CornerRadius13
                    )
                )
                .background(Color.Black)
                .videoSurfaceGestures(
                    controlsLocked = controlsLocked,
                    positionMs = engine.positionMs,
                    durationMs = durationMs,
                    brightnessLevel = uiState.videoCinemaSettings.brightnessLevel,
                    volume = engine.volume,
                    verticalSwipesEnabled = isFullscreenSurface,
                    onBumpInteraction = { interactionNonce++ },
                    onSeekTo = onSeekTo,
                    onTogglePlayPause = onTogglePlayPause,
                    onToggleControls = {
                        controlsVisible = !controlsVisible
                        interactionNonce++
                    },
                    onSetVideoBrightness = onSetVideoBrightness,
                    onSetVolume = onSetVolume,
                    onNotice = { seekNotice = it }
                )
                .testTag(
                    if (isFullscreenSurface) "fullscreen_video_surface" else "video_surface_container"
                )
        ) {
            Media3PlayerSurface(
                exoPlayer = exoPlayer,
                aspectMode = engine.videoAspectMode,
                testTag = if (isFullscreenSurface) "fullscreen_media3_player_view" else "media3_player_view"
            )

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

            if (!isCurrentTrackVideo && activeVideo != null &&
                !isPlaybackError && !isPlaybackEnded
            ) {
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

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                PlayerControlsOverlay(isFullscreenSurface)
            }

            // Terminal states render above the overlay scrim so they stay
            // readable even after the controls auto-hide.
            if (isPlaybackError) {
                VideoStateOverlayCard(
                    icon = Icons.Default.Error,
                    title = if (isPersian) "خطا در پخش ویدیو" else "Video Playback Failed",
                    message = engine.errorMessage
                        ?: if (isPersian) {
                            "پخش این ویدیو ممکن نشد. دوباره تلاش کنید."
                        } else {
                            "This video could not be played. Please try again."
                        },
                    actionLabel = if (activeVideo != null) {
                        if (isPersian) "تلاش دوباره" else "Retry"
                    } else {
                        if (isPersian) "انتخاب ویدیو" else "Pick Video"
                    },
                    actionTestTag = "video_retry_button",
                    onAction = {
                        interactionNonce++
                        if (activeVideo != null) {
                            onPlayVideoTrack(activeVideo)
                        } else {
                            videoPickerLauncher.launch(arrayOf("video/*"))
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (isPlaybackEnded) {
                VideoStateOverlayCard(
                    icon = Icons.Default.Replay,
                    title = if (isPersian) "پایان ویدیو" else "Video Finished",
                    message = if (isPersian) {
                        "از ابتدا دوباره تماشا کنید."
                    } else {
                        "Watch again from the start."
                    },
                    actionLabel = if (isPersian) "پخش دوباره" else "Replay",
                    actionTestTag = "video_replay_button",
                    onAction = {
                        interactionNonce++
                        onSeekTo(0L)
                        onTogglePlayPause()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (isEmptyVideoLibrary) {
                VideoStateOverlayCard(
                    icon = Icons.Default.Movie,
                    title = if (isPersian) "هنوز ویدیویی نیست" else "No Videos Yet",
                    message = if (isPersian) {
                        "یک ویدیو از حافظه انتخاب کنید تا پخش شود."
                    } else {
                        "Pick a video from your device to play it."
                    },
                    actionLabel = if (isPersian) "انتخاب ویدیو" else "Pick Video",
                    actionTestTag = "video_empty_pick_button",
                    onAction = {
                        interactionNonce++
                        videoPickerLauncher.launch(arrayOf("video/*"))
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // ONE shared controls overlay: title row, gesture hint, transport,
    // seek. Identical in embedded and fullscreen. The top row holds title
    // + "more" + fullscreen in both modes (the "more" sheet is how Simple
    // Mode reaches PiP, subtitles and the rest); in fullscreen it also
    // holds a 1-tap CC subtitles toggle. While locked, the top row keeps
    // only the unlock button (+ exit fullscreen) and the transport/seek
    // rows hide — same as before.
    // -----------------------------------------------------------------------

    @Composable
    fun PlayerControlsOverlay(isFullscreenSurface: Boolean) {
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
                        if (controlsLocked) {
                            IconButton(
                                onClick = {
                                    controlsLocked = false
                                    interactionNonce++
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = if (isPersian) "باز کردن قفل" else "Unlock controls",
                                    tint = FazoraGold
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    showMoreSheet = true
                                    interactionNonce++
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = if (isPersian) "تنظیمات بیشتر" else "More settings",
                                    tint = FazoraGold
                                )
                            }
                        }
                        // 1-tap subtitles toggle, fullscreen only: in
                        // embedded the sheet holds the full subtitle
                        // settings, but bilingual users should not need
                        // 3 taps for CC while watching fullscreen.
                        if (isFullscreenSurface) {
                            IconButton(
                                onClick = {
                                    interactionNonce++
                                    onSetSubtitlesEnabled(!engine.subtitlesEnabled)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("video_cc_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = if (isPersian) "زیرنویس" else "Subtitles",
                                    tint = if (engine.subtitlesEnabled) FazoraGold else FazoraSteel
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                interactionNonce++
                                isFullscreen = !isFullscreenSurface
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(
                                    if (isFullscreenSurface) {
                                        "video_exit_fullscreen_button"
                                    } else {
                                        "video_fullscreen_button"
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = if (isFullscreenSurface) {
                                    Icons.Default.FullscreenExit
                                } else {
                                    Icons.Default.Fullscreen
                                },
                                contentDescription = stringResource(R.string.cd_video_fullscreen),
                                tint = FazoraGold
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showGestureHint && !controlsLocked) {
                        VideoGestureHintBanner(
                            isPersian = isPersian,
                            onDismiss = {
                                hintDismissedLocally = true
                                onVideoGestureHintSeen()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = PhiTokens.Space13)
                        )
                    }

                    // Transport stays available on ENDED so the user can
                    // jump back 10s without hitting Replay first.
                    if (!controlsLocked && !isPlaybackError &&
                        !isEmptyVideoLibrary
                    ) {
                        // Tape direction, not reading direction: the
                        // transport stays LTR in Persian (left = back).
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("video_seek_back_10s")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "-10s",
                                        tint = FazoraIvory,
                                        modifier = Modifier.size(40.dp)
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
                                        // Embedded keeps a smaller, less
                                        // saturated disc: a 72dp solid-gold
                                        // circle swallows a ~200dp-tall 16:9
                                        // viewport. Fullscreen keeps the 72dp
                                        // solid disc.
                                        containerColor = if (isFullscreenSurface) {
                                            FazoraGold
                                        } else {
                                            FazoraGold.copy(alpha = 0.8f)
                                        },
                                        contentColor = FazoraObsidian
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(if (isFullscreenSurface) 72.dp else 56.dp)
                                        .testTag("video_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (engine.isPlaying && isCurrentTrackVideo) {
                                            Icons.Default.Pause
                                        } else {
                                            Icons.Default.PlayArrow
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(if (isFullscreenSurface) 36.dp else 28.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        interactionNonce++
                                        onSeekTo(engine.positionMs + 10_000L)
                                        seekNotice = "+10s"
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("video_seek_fwd_10s")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "+10s",
                                        tint = FazoraIvory,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Seek stays available on ENDED so the user can scrub back;
                // it hides on error, while locked, and when there is no video.
                if (!controlsLocked && !isPlaybackError && !isEmptyVideoLibrary) {
                    // Tape direction, not reading direction: the
                    // seek bar and its timecodes stay LTR in Persian.
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        VideoSeekBar(
                            fraction = seekFraction,
                            bufferedFraction = bufferedFraction,
                            currentPosMs = currentPosMs,
                            durationMs = durationMs,
                            isDragging = isDraggingSeek,
                            dragFraction = dragSeekFraction,
                            onDragging = { f ->
                                interactionNonce++
                                isDraggingSeek = true
                                dragSeekFraction = f
                            },
                            onSeekTo = { target ->
                                isDraggingSeek = false
                                onSeekTo(target)
                            },
                            // Gesture-nav clearance: in fullscreen the
                            // seek bar sits at the very bottom of the
                            // screen, so pad it above the nav bar inset.
                            modifier = if (isFullscreenSurface) {
                                Modifier.navigationBarsPadding()
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // The "more" sheet content — ONE implementation, two hosts: a
    // ModalBottomSheet over the screen (embedded) and a bottom panel inside
    // the fullscreen Dialog. Speed, aspect, subtitles, audio tracks,
    // bookmarks, PiP, lock — everything that used to sprawl below the
    // player in Studio mode. Nothing is removed, only organized.
    // -----------------------------------------------------------------------

    @Composable
    fun MoreSheetContent() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(PhiTokens.Space21)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPersian) "تنظیمات پخش" else "Playback Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = FazoraIvory
                )
                IconButton(
                    onClick = { showMoreSheet = false },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("video_more_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isPersian) "بستن" else "Close",
                        tint = FazoraIvory
                    )
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
                    onClick = {
                        showMoreSheet = false
                        showSubtitleDialog = true
                    },
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
                    onClick = {
                        showMoreSheet = false
                        showAudioDialog = true
                    },
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
                    onClick = {
                        interactionNonce++
                        onAddVideoBookmark()
                    },
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
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
                OutlinedButton(
                    onClick = {
                        showMoreSheet = false
                        isFullscreen = false
                        onEnterPip()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("video_pip_button")
                ) {
                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = null, tint = FazoraGold)
                    Spacer(modifier = Modifier.width(PhiTokens.Space8))
                    Text("PiP", color = FazoraIvory)
                }

                OutlinedButton(
                    onClick = {
                        controlsLocked = true
                        showMoreSheet = false
                        interactionNonce++
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("video_lock_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = FazoraGold)
                    Spacer(modifier = Modifier.width(PhiTokens.Space8))
                    Text(if (isPersian) "قفل کنترل‌ها" else "Lock Controls", color = FazoraIvory)
                }
            }

            // Persistent rediscovery for the surface gestures: the
            // one-time banner alone is not enough, so the sheet links to
            // the same help text on demand.
            Spacer(modifier = Modifier.height(PhiTokens.Space13))
            OutlinedButton(
                onClick = { showGesturesHelpDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("video_gestures_help_button")
            ) {
                Icon(Icons.Default.Help, contentDescription = null, tint = FazoraGold)
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Text(
                    text = if (isPersian) "راهنمای ژست‌ها" else "Gestures help",
                    color = FazoraIvory
                )
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler { isFullscreen = false }
            BackHandler(enabled = showMoreSheet) { showMoreSheet = false }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                PlayerSurface(isFullscreenSurface = true)

                // The sheet inside the fullscreen Dialog: a bottom panel
                // (the screen-level ModalBottomSheet would land in a
                // separate window under this Dialog). Same content, same
                // state — it survives the embedded <-> fullscreen switch.
                AnimatedVisibility(
                    visible = showMoreSheet,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { showMoreSheet = false }
                        )
                        Surface(
                            color = FazoraCarbonElevated,
                            shape = RoundedCornerShape(
                                topStart = PhiTokens.CornerRadius21,
                                topEnd = PhiTokens.CornerRadius21
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.75f)
                                // Cap the panel so it never grows past a
                                // usable sheet on tall screens; on short
                                // landscape screens the 0.75f fraction
                                // already keeps it small and the sheet
                                // content scrolls internally (see
                                // MoreSheetContent).
                                .heightIn(max = 480.dp)
                        ) {
                            MoreSheetContent()
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
                                        // FIX: tapping a track used to only
                                        // enable subtitles — it never
                                        // selected the track.
                                        onSelectSubtitleTrack(sub.id)
                                        onSetSubtitlesEnabled(true)
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                RadioButton(
                                    selected = sub.isSelected && engine.subtitlesEnabled,
                                    onClick = {
                                        onSelectSubtitleTrack(sub.id)
                                        onSetSubtitlesEnabled(true)
                                    },
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

    // Gestures help, reachable from the "more" sheet at any time — the
    // same copy as the one-time banner (literal left/right, never
    // start/end), plus the fullscreen-only note for vertical swipes.
    if (showGesturesHelpDialog) {
        AlertDialog(
            onDismissRequest = { showGesturesHelpDialog = false },
            containerColor = FazoraCarbonElevated,
            title = {
                Text(
                    text = if (isPersian) "راهنمای ژست‌ها" else "Gestures help",
                    color = FazoraIvory
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isPersian) {
                            "👆👆 دو ضربه — چپ: ۱۰ ثانیه عقب، راست: ۱۰ ثانیه جلو"
                        } else {
                            "👆👆 Double-tap — left: back 10s, right: forward 10s"
                        },
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) {
                            "↕️ کشیدن عمودی در تمام‌صفحه — چپ: روشنایی، راست: صدا"
                        } else {
                            "↕️ Swipe vertically in fullscreen — left: brightness, right: volume"
                        },
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) {
                            "یک ضربه: نمایش / پنهان کردن کنترل‌ها"
                        } else {
                            "Single tap: show / hide controls"
                        },
                        color = FazoraSteel
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGesturesHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FazoraGold,
                        contentColor = FazoraObsidian
                    )
                ) {
                    Text(if (isPersian) "فهمیدم" else "Got it")
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
                                    .padding(vertical = 8.dp)
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

    // The "more" sheet over the embedded player. In fullscreen the same
    // content renders inside the Dialog (see above) so it is never hidden
    // behind a separate window.
    if (showMoreSheet && !isFullscreen) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = moreSheetState,
            containerColor = FazoraCarbonElevated,
            contentColor = FazoraIvory
        ) {
            MoreSheetContent()
        }
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
                    PlayerSurface(isFullscreenSurface = false)

                    // Below the player only the library actions remain
                    // (Studio mode, as before). Speed, aspect, subtitles,
                    // audio tracks and bookmarks moved into the "more" sheet.
                    if (!isSimpleMode) {
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
