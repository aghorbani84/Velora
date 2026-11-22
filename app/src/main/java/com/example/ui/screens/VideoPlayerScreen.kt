package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.domain.engine.PlaybackStatus
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.ExperienceTier
import com.example.domain.model.FullscreenPolicy
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.SubtitleColorTheme
import com.example.domain.model.SubtitleFontScale
import com.example.domain.model.Track
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VideoControlGroup
import com.example.ui.components.TrackRowItem
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraVideoSurface
import com.example.ui.components.bufferedPositionMs
import com.example.ui.components.veloraCinemaGestures
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import com.example.ui.viewmodel.PlaybackPositionState
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Video player UX (ported from PR #2 onto the 3-tier system).
//
// ONE shared surface + controls overlay for embedded and fullscreen:
// transport (-10s / play-pause / +10s), the VideoSeekBar with buffered
// progress, and fullscreen — identical in every tier. PiP, lock, speed,
// aspect, subtitles, audio track and bookmarks live behind ONE "more"
// bottom sheet that exists only in the PRO tier (FullscreenPolicy.isInMoreSheet);
// SIMPLE and STANDARD hide those controls. The library actions below the
// player (pick / gallery / auto-scan) are PRO-only as well.
// ---------------------------------------------------------------------------

private val VideoOnSurface = Color.White
private val VideoOnSurfaceMuted = Color.White.copy(alpha = 0.72f)
private const val SCRIM_ALPHA = 0.62f
private const val CARD_SCRIM_ALPHA = 0.85f
private const val SEEK_STEP_MS = 10_000L
private const val SEEK_NOTICE_MS = 900L
private const val MIN_AUTO_HIDE_MS = 3500L

// ---------------------------------------------------------------------------
// Seek bar: a real M3 Slider (battle-tested drag handling, 48dp touch
// height) with its own visuals drawn transparent, plus our own track:
// inactive bar, buffered fill, played fill, and a 28dp thumb. The Slider
// sits on top and takes all input. Geometry follows seekThumbCenterX()
// (VideoSeekGeometry.kt). LTR only: media progress is tape direction.
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
    val density = LocalDensity.current
    val accent = MaterialTheme.colorScheme.primary
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
                    .background(VideoOnSurface.copy(alpha = 0.24f))
            )
            // Buffered fill (what is already downloaded).
            if (bufferedPx > 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = trackStartDp)
                        .width(bufferedWidthDp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(VideoOnSurface.copy(alpha = 0.48f))
                )
            }
            // Played fill.
            if (playedPx > 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = trackStartDp)
                        .width(playedWidthDp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent)
                )
            }
            // 28dp thumb (the Slider's own thumb is transparent, see below).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffsetDp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .border(2.dp, Color.Black, CircleShape)
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
                color = VideoOnSurface
            )
            Text(
                text = Track.formatDurationMs(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = VideoOnSurfaceMuted
            )
        }
    }
}

// ---------------------------------------------------------------------------
// One-time gesture hint: shown the first time the player opens, until the
// user taps "Got it" (persisted via settings.videoGestureHintSeen).
// ---------------------------------------------------------------------------

@Composable
private fun VideoGestureHintBanner(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = Color.Black.copy(alpha = CARD_SCRIM_ALPHA),
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier.border(1.dp, accent, RoundedCornerShape(PhiTokens.CornerRadius13))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PhiTokens.Space13)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "👆👆 دو ضربه: عقب / جلو ۱۰ ثانیه" else "👆👆 Double-tap: back / forward 10s",
                    style = MaterialTheme.typography.labelLarge,
                    color = VideoOnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPersian) {
                        "↕️ کشیدن عمودی در تمام‌صفحه — چپ: روشنایی، راست: صدا"
                    } else {
                        "↕️ Swipe vertically in fullscreen — left: brightness, right: volume"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = VideoOnSurfaceMuted
                )
            }
            Spacer(modifier = Modifier.width(PhiTokens.Space8))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("video_gesture_hint_dismiss")
            ) {
                Text(text = if (isPersian) "فهمیدم" else "Got it", color = accent)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Centred state card for the terminal/empty states: playback error
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
    modifier: Modifier = Modifier,
    // While the controls are locked the terminal-state action must not be
    // reachable; the card renders its button disabled.
    actionEnabled: Boolean = true
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = Color.Black.copy(alpha = CARD_SCRIM_ALPHA),
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = modifier.border(1.dp, accent, RoundedCornerShape(PhiTokens.CornerRadius21))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PhiTokens.Space21)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = VideoOnSurface)
            if (message != null) {
                Spacer(modifier = Modifier.height(PhiTokens.Space8))
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = VideoOnSurfaceMuted)
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space13))
            Button(
                onClick = onAction,
                enabled = actionEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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

private fun pickedVideoTrack(idPrefix: String, fileName: String, artist: String, album: String, uri: String, folder: String): Track {
    val now = System.currentTimeMillis()
    return Track(
        id = "${idPrefix}_$now",
        mediaStoreId = now,
        title = fileName,
        artist = artist,
        album = album,
        durationMs = 0L,
        uri = uri,
        folderPath = folder,
        mediaType = MediaType.VIDEO
    )
}

// NOTE: kotlin.OptIn is required here - only the Kotlin annotation is honored by the
// compiler for ExperimentalMaterial3Api; the androidx annotation is Lint-only.
@OptIn(ExperimentalMaterial3Api::class)
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
    onSelectSubtitleTrack: (String?) -> Unit = {},
    onToggleFavorite: (Track) -> Unit = {},
    onEnterPip: () -> Unit = {},
    onAutoScanMedia: () -> Unit = {},
    onSetVolume: (Float) -> Unit = {},
    onSetVideoBrightness: (Float) -> Unit = {},
    onSetSubtitleFontScale: (SubtitleFontScale) -> Unit = {},
    onSetSubtitleColorTheme: (SubtitleColorTheme) -> Unit = {},
    onAddVideoBookmark: () -> Unit = {},
    onVideoGestureHintSeen: () -> Unit = {},
    isSimpleMode: Boolean = uiState.settings.isSimpleMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = uiState.engineState
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val tier = if (isSimpleMode && uiState.settings.experienceTier == ExperienceTier.PRO) {
        ExperienceTier.SIMPLE
    } else {
        uiState.settings.experienceTier
    }
    val hasMoreSheet = VideoControlGroup.entries.any { FullscreenPolicy.isInMoreSheet(tier, it) }
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary

    val isCurrentTrackVideo = engine.currentTrack?.mediaType == MediaType.VIDEO
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
            delay(SEEK_NOTICE_MS)
            seekNotice = null
        }
    }

    LaunchedEffect(controlsVisible, positionState.isPlaying, interactionNonce, uiState.settings.autoHideVideoControlsMs) {
        if (controlsVisible && positionState.isPlaying && !controlsLocked) {
            delay(uiState.settings.autoHideVideoControlsMs.coerceAtLeast(MIN_AUTO_HIDE_MS))
            controlsVisible = false
        }
    }

    val scanPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val scanPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { onAutoScanMedia() }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Local Video"
            onPlayVideoTrack(
                pickedVideoTrack(
                    idPrefix = "picked_video",
                    fileName = fileName,
                    artist = if (isPersian) "حافظه محلی" else "Device Storage",
                    album = if (isPersian) "ویدیوهای دستگاه" else "Local Videos",
                    uri = uri.toString(),
                    folder = "/storage/emulated/0/Movies"
                )
            )
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Gallery Video"
            onPlayVideoTrack(
                pickedVideoTrack(
                    idPrefix = "gallery_video",
                    fileName = fileName,
                    artist = if (isPersian) "گالری" else "Gallery",
                    album = if (isPersian) "ویدیوهای دستگاه" else "Device Videos",
                    uri = uri.toString(),
                    folder = "/storage/emulated/0/DCIM"
                )
            )
        }
    }

    val durationMs = positionState.durationMs.coerceAtLeast(activeVideo?.durationMs ?: SEEK_STEP_MS)
    val currentPosMs = if (isDraggingSeek) (dragSeekFraction * durationMs).toLong() else positionState.positionMs
    val seekFraction = if (durationMs > 0L) {
        (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    // Buffered progress through the platform adapter; refreshes whenever the
    // screen recomposes on position ticks, which is plenty for a buffered bar.
    val bufferedFraction = if (durationMs > 0L) {
        (playerEngine.bufferedPositionMs().toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
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

    val activeSubtitleText = remember(uiState.activeLyrics, currentPosMs, uiState.lyricsOffsetMs) {
        val idx = VeloraLyricsAndSubtitleEngine.findActiveLineIndex(
            uiState.activeLyrics,
            currentPosMs,
            uiState.lyricsOffsetMs
        )
        uiState.activeLyrics.getOrNull(idx)?.text.orEmpty()
    }

    val showGestureHint = !uiState.settings.videoGestureHintSeen && !hintDismissedLocally && activeVideo != null
    val isEmptyVideoLibrary = uiState.videoTracks.isEmpty() && activeVideo == null
    val isPlaybackError = engine.status == PlaybackStatus.ERROR
    val isPlaybackEnded = engine.status == PlaybackStatus.ENDED
    val showTransportAndSeek = !controlsLocked && !isPlaybackError && !isEmptyVideoLibrary

    // True immersive fullscreen: hide system bars + lock landscape for
    // landscape video while fullscreen is open; restore both on exit.
    val activity = context as? Activity
    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            val window = activity?.window
            val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
    // ONE shared controls overlay: title row, gesture hint, transport, seek.
    // Identical in embedded and fullscreen and in every tier, except the
    // "more" button, which only exists when the tier has a more sheet (PRO).
    // While locked, the top row keeps only the unlock button (+ fullscreen
    // toggle) and the transport/seek rows hide.
    // -----------------------------------------------------------------------

    @Composable
    fun PlayerControlsOverlay(isFullscreenSurface: Boolean) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
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
                            color = VideoOnSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "$resolutionLabel • ${engine.videoAspectMode.label} • ${engine.playbackSpeed}x",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
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
                                    .testTag("video_unlock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = if (isPersian) "باز کردن قفل" else "Unlock controls",
                                    tint = accent
                                )
                            }
                        } else if (hasMoreSheet) {
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
                                    tint = accent
                                )
                            }
                        }
                        // 1-tap CC toggle, fullscreen + PRO only (subtitles are a
                        // more-sheet control): no 3 taps for CC while watching.
                        if (isFullscreenSurface && !controlsLocked &&
                            FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.SUBTITLES)
                        ) {
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
                                    tint = if (engine.subtitlesEnabled) accent else VideoOnSurfaceMuted
                                )
                            }
                        }
                        if (FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.FULLSCREEN)) {
                            IconButton(
                                onClick = {
                                    interactionNonce++
                                    isFullscreen = !isFullscreenSurface
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag(
                                        if (isFullscreenSurface) "video_exit_fullscreen_button" else "video_fullscreen_button"
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isFullscreenSurface) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = stringResource(R.string.cd_video_fullscreen),
                                    tint = accent
                                )
                            }
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

                    // Transport stays available on ENDED so the user can jump
                    // back 10s without hitting Replay first.
                    if (showTransportAndSeek && FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.TRANSPORT)) {
                        // Tape direction, not reading direction: LTR in Persian too.
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        interactionNonce++
                                        onSeekTo((positionState.positionMs - SEEK_STEP_MS).coerceAtLeast(0L))
                                        seekNotice = "-10s"
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("video_seek_back_10s")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "-10s",
                                        tint = VideoOnSurface,
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
                                        // Embedded keeps a smaller, less saturated disc so it
                                        // does not swallow a ~200dp-tall 16:9 viewport.
                                        containerColor = if (isFullscreenSurface) accent else accent.copy(alpha = 0.8f),
                                        contentColor = onAccent
                                    ),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .size(if (isFullscreenSurface) 72.dp else 56.dp)
                                        .testTag("video_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (positionState.isPlaying && isCurrentTrackVideo) {
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
                                        onSeekTo(PlaybackQueue.clampSeekPosition(positionState.positionMs + SEEK_STEP_MS, durationMs))
                                        seekNotice = "+10s"
                                    },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .testTag("video_seek_fwd_10s")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "+10s",
                                        tint = VideoOnSurface,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Seek stays available on ENDED so the user can scrub back; it
                // hides on error, while locked, and when there is no video.
                if (showTransportAndSeek && FullscreenPolicy.isVideoControlVisible(tier, VideoControlGroup.SEEK)) {
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
                            // Gesture-nav clearance in fullscreen.
                            modifier = if (isFullscreenSurface) Modifier.navigationBarsPadding() else Modifier
                        )
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // ONE shared surface: embedded and fullscreen render the same Box —
    // player, dim, subtitles, tap-to-play cover, HUD, spinner, state cards
    // and the shared controls overlay.
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
                .clip(RoundedCornerShape(if (isFullscreenSurface) 0.dp else PhiTokens.CornerRadius13))
                .background(Color.Black)
                .veloraCinemaGestures(
                    controlsLocked = controlsLocked,
                    positionMs = positionState.positionMs,
                    durationMs = durationMs,
                    currentBrightness = uiState.videoCinemaSettings.brightnessLevel,
                    currentVolume = engine.volume,
                    onToggleControls = { controlsVisible = !controlsVisible },
                    onInteraction = { interactionNonce++ },
                    onSeekTo = onSeekTo,
                    onTogglePlayPause = onTogglePlayPause,
                    onSetBrightness = onSetVideoBrightness,
                    onSetVolume = onSetVolume,
                    onHudNotice = { seekNotice = it },
                    verticalSwipesEnabled = isFullscreenSurface
                )
                .testTag(if (isFullscreenSurface) "fullscreen_video_dialog" else "video_gesture_container")
        ) {
            VeloraVideoSurface(
                playerEngine = playerEngine,
                videoAspectMode = engine.videoAspectMode,
                surfaceTestTag = if (isFullscreenSurface) "fullscreen_video_surface" else "inline_video_surface",
                modifier = Modifier.fillMaxSize()
            )

            val dimAlpha = (1f - uiState.videoCinemaSettings.brightnessLevel).coerceIn(0f, 0.75f)
            if (dimAlpha > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                )
            }

            if (engine.subtitlesEnabled && activeSubtitleText.isNotBlank()) {
                VideoSubtitleOverlay(
                    text = activeSubtitleText,
                    cinemaSettings = uiState.videoCinemaSettings,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(if (isFullscreenSurface) PhiTokens.Space34 else PhiTokens.Space13)
                )
            }

            val showPlayCover = !isCurrentTrackVideo && activeVideo != null &&
                !isPlaybackError && !isPlaybackEnded && !controlsLocked
            if (showPlayCover && activeVideo != null) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = CARD_SCRIM_ALPHA))
                        .padding(PhiTokens.Space13)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .size(54.dp)
                            .clickable { onPlayVideoTrack(activeVideo) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = activeVideo.title, style = MaterialTheme.typography.titleMedium, color = VideoOnSurface)
                    Text(
                        text = if (isPersian) "برای پخش لمس کنید" else "Tap to Play Video",
                        style = MaterialTheme.typography.labelSmall,
                        color = VideoOnSurfaceMuted
                    )
                }
            }

            val notice = seekNotice
            if (notice != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .border(1.dp, accent, RoundedCornerShape(PhiTokens.CornerRadius21))
                        .testTag("video_gesture_hud_label")
                ) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.titleLarge,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            if (engine.status == PlaybackStatus.BUFFERING) {
                CircularProgressIndicator(color = accent, modifier = Modifier.align(Alignment.Center))
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
                    message = engine.errorMessage ?: if (isPersian) {
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
                    actionEnabled = !controlsLocked,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (isPlaybackEnded) {
                VideoStateOverlayCard(
                    icon = Icons.Default.Replay,
                    title = if (isPersian) "پایان ویدیو" else "Video Finished",
                    message = if (isPersian) "از ابتدا دوباره تماشا کنید." else "Watch again from the start.",
                    actionLabel = if (isPersian) "پخش دوباره" else "Replay",
                    actionTestTag = "video_replay_button",
                    onAction = {
                        interactionNonce++
                        onSeekTo(0L)
                        onTogglePlayPause()
                    },
                    actionEnabled = !controlsLocked,
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
                    actionEnabled = !controlsLocked,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // The "more" sheet content (PRO only) — ONE implementation, two hosts: a
    // ModalBottomSheet over the screen (embedded) and a bottom panel inside
    // the fullscreen Dialog. Every item is gated by FullscreenPolicy.isInMoreSheet.
    // -----------------------------------------------------------------------

    @Composable
    fun MoreSheetContent() {
        Column(
            verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(PhiTokens.Space21)
                .testTag("video_more_sheet")
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPersian) "تنظیمات پخش" else "Playback Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
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
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.SPEED)) {
                Text(
                    text = if (isPersian) "سرعت پخش ویدیو" else "Playback Speed",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(speedOptions) { speed ->
                        VeloraSecondaryButton(
                            text = "${speed}x",
                            icon = Icons.Default.Speed,
                            selected = kotlin.math.abs(engine.playbackSpeed - speed) < 0.01f,
                            onClick = {
                                interactionNonce++
                                onSetPlaybackSpeed(speed)
                            },
                            tag = "video_speed_chip_$speed"
                        )
                    }
                }
            }

            if (FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.ASPECT)) {
                Text(
                    text = if (isPersian) "نسبت ابعاد تصویر" else "Aspect Ratio & Framing",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(VideoAspectMode.entries) { mode ->
                        VeloraSecondaryButton(
                            text = mode.label,
                            icon = Icons.Default.AspectRatio,
                            selected = engine.videoAspectMode == mode,
                            onClick = {
                                interactionNonce++
                                onSetAspectMode(mode)
                            },
                            tag = "video_aspect_chip_${mode.name}"
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.SUBTITLES)) {
                    VeloraSecondaryButton(
                        text = if (isPersian) {
                            if (engine.subtitlesEnabled) "زیرنویس: روشن" else "زیرنویس: خاموش"
                        } else {
                            if (engine.subtitlesEnabled) "Subtitles: ON" else "Subtitles: OFF"
                        },
                        icon = Icons.Default.Subtitles,
                        selected = engine.subtitlesEnabled,
                        onClick = {
                            showMoreSheet = false
                            showSubtitleDialog = true
                        },
                        tag = "video_subtitle_button",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.AUDIO_TRACK)) {
                    VeloraSecondaryButton(
                        text = if (isPersian) {
                            "ترک‌های صوتی (${engine.audioTracks.size.coerceAtLeast(1)})"
                        } else {
                            "Audio Tracks (${engine.audioTracks.size.coerceAtLeast(1)})"
                        },
                        icon = Icons.Default.Audiotrack,
                        onClick = {
                            showMoreSheet = false
                            showAudioDialog = true
                        },
                        tag = "video_audio_track_button",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.BOOKMARK)) {
                VeloraSecondaryButton(
                    text = if (isPersian) "نشانک صحنه" else "Bookmark Scene",
                    icon = Icons.Default.BookmarkAdd,
                    onClick = {
                        interactionNonce++
                        onAddVideoBookmark()
                    },
                    tag = "video_bookmark_button",
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.timeMachineBookmarks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(uiState.timeMachineBookmarks, key = { "vbm_${it.id}" }) { bm ->
                            VeloraSecondaryButton(
                                text = "🔖 ${bm.formattedTimestamp} • ${bm.trackTitle}",
                                onClick = { onSeekTo(bm.timestampMs) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.PIP)) {
                    VeloraSecondaryButton(
                        text = "PiP",
                        icon = Icons.Default.PictureInPictureAlt,
                        onClick = {
                            showMoreSheet = false
                            isFullscreen = false
                            onEnterPip()
                        },
                        tag = "video_pip_button",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.LOCK)) {
                    VeloraSecondaryButton(
                        text = if (isPersian) "قفل کنترل‌ها" else "Lock Controls",
                        icon = Icons.Default.Lock,
                        onClick = {
                            controlsLocked = true
                            showMoreSheet = false
                            interactionNonce++
                        },
                        tag = "video_lock_button",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Persistent rediscovery for the surface gestures: the one-time
            // banner alone is not enough, so the sheet links to the same text.
            VeloraSecondaryButton(
                text = if (isPersian) "راهنمای ژست‌ها" else "Gestures help",
                icon = Icons.Default.Info,
                onClick = { showGesturesHelpDialog = true },
                tag = "video_gestures_help_button",
                modifier = Modifier.fillMaxWidth()
            )
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

                // The sheet inside the fullscreen Dialog: a bottom panel (the
                // screen-level ModalBottomSheet would land in a separate window
                // under this Dialog). Same content, same state.
                AnimatedVisibility(
                    visible = showMoreSheet && hasMoreSheet,
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = PhiTokens.CornerRadius21,
                                topEnd = PhiTokens.CornerRadius21
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.75f)
                                .heightIn(max = 480.dp)
                        ) {
                            MoreSheetContent()
                        }
                    }
                }
            }
        }
    }

    if (showSubtitleDialog && FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.SUBTITLES)) {
        VideoSubtitlesDialog(
            uiState = uiState,
            isPersian = isPersian,
            onSetSubtitlesEnabled = onSetSubtitlesEnabled,
            onSetSubtitleFontScale = onSetSubtitleFontScale,
            onSetSubtitleColorTheme = onSetSubtitleColorTheme,
            onDismiss = { showSubtitleDialog = false },
            subtitleTracks = engine.subtitleTracks,
            onSelectSubtitleTrack = onSelectSubtitleTrack
        )
    }

    if (showAudioDialog && FullscreenPolicy.isInMoreSheet(tier, VideoControlGroup.AUDIO_TRACK)) {
        VideoAudioTrackDialog(
            audioTracks = engine.audioTracks,
            isPersian = isPersian,
            onSelectAudioTrack = onSelectAudioTrack,
            onDismiss = { showAudioDialog = false }
        )
    }

    // Gestures help, reachable from the "more" sheet at any time.
    if (showGesturesHelpDialog) {
        AlertDialog(
            onDismissRequest = { showGesturesHelpDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            title = { Text(if (isPersian) "راهنمای ژست‌ها" else "Gestures help") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    Text(
                        if (isPersian) {
                            "👆👆 دو ضربه — چپ: ۱۰ ثانیه عقب، راست: ۱۰ ثانیه جلو"
                        } else {
                            "👆👆 Double-tap — left: back 10s, right: forward 10s"
                        }
                    )
                    Text(
                        if (isPersian) {
                            "↕️ کشیدن عمودی در تمام‌صفحه — چپ: روشنایی، راست: صدا"
                        } else {
                            "↕️ Swipe vertically in fullscreen — left: brightness, right: volume"
                        }
                    )
                    Text(
                        text = if (isPersian) "یک ضربه: نمایش / پنهان کردن کنترل‌ها" else "Single tap: show / hide controls",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showGesturesHelpDialog = false }) {
                    Text(if (isPersian) "فهمیدم" else "Got it")
                }
            }
        )
    }

    // The "more" sheet over the embedded player. In fullscreen the same
    // content renders inside the Dialog (see above).
    if (showMoreSheet && hasMoreSheet && !isFullscreen) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            sheetState = moreSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            MoreSheetContent()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("video_player_screen")
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(PhiTokens.CornerRadius21))
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                    PlayerSurface(isFullscreenSurface = false)

                    // Below the player only the library actions remain (PRO only).
                    // Speed, aspect, subtitles, audio tracks and bookmarks moved
                    // into the "more" sheet.
                    if (FullscreenPolicy.isPickVideoVisible(tier)) {
                        Spacer(modifier = Modifier.height(PhiTokens.Space13))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            VeloraPrimaryButton(
                                text = if (isPersian) "انتخاب ویدیو" else "Pick Video",
                                icon = Icons.Default.FolderOpen,
                                onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                                tag = "pick_video_button",
                                modifier = Modifier.weight(1f)
                            )
                            VeloraSecondaryButton(
                                text = if (isPersian) "گالری فیلم" else "From Gallery",
                                icon = Icons.Default.FolderOpen,
                                onClick = { galleryPickerLauncher.launch("video/*") },
                                tag = "gallery_video_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        VeloraSecondaryButton(
                            text = if (isPersian) "اسکن ویدیوهای دستگاه" else "Scan Device Videos",
                            icon = Icons.Default.Refresh,
                            onClick = { scanPermissionLauncher.launch(scanPermissions) },
                            tag = "pick_device_video_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                    onAction = { scanPermissionLauncher.launch(scanPermissions) }
                )
            }
        } else {
            item {
                Text(
                    text = if (isPersian) {
                        "ویدیوهای موجود (${uiState.videoTracks.size})"
                    } else {
                        "Video Streams (${uiState.videoTracks.size})"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
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
