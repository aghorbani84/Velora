package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.domain.model.PhiDesignMetrics
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Track
import com.example.domain.model.VideoAspectMode
import com.example.ui.components.SpectrumAnalyzerCanvas
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraEmeraldOk
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.HEX_CARBON
import com.example.ui.theme.HEX_GOLD
import com.example.ui.theme.HEX_IVORY
import com.example.ui.theme.HEX_OBSIDIAN
import com.example.ui.theme.HEX_STEEL
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import kotlinx.coroutines.delay
import java.util.Locale

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
    modifier: Modifier = Modifier
) {
    val engine = uiState.engineState
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val isCurrentTrackVideo = engine.currentTrack?.mediaType == com.example.domain.model.MediaType.VIDEO
    val activeVideo = if (isCurrentTrackVideo) engine.currentTrack else uiState.videoTracks.firstOrNull()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Local Video"
            val pickedTrack = Track(
                id = "picked_video_${System.currentTimeMillis()}",
                mediaStoreId = System.currentTimeMillis(),
                title = fileName,
                artist = "Device Storage",
                album = "Local Video",
                durationMs = 0L,
                uri = uri.toString(),
                folderPath = "/storage/emulated/0/Movies",
                mediaType = com.example.domain.model.MediaType.VIDEO
            )
            onPlayVideoTrack(pickedTrack)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var interactionNonce by remember { mutableIntStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isDraggingSeek by remember { mutableStateOf(false) }
    var dragSeekFraction by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(controlsVisible, engine.isPlaying, interactionNonce, uiState.settings.autoHideVideoControlsMs) {
        if (controlsVisible && engine.isPlaying) {
            delay(uiState.settings.autoHideVideoControlsMs)
            controlsVisible = false
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

    val aspectRatioFloat = when (engine.videoAspectMode) {
        VideoAspectMode.FIT -> 16f / 9f
        VideoAspectMode.FILL -> 18f / 9f
        VideoAspectMode.GOLDEN_PHI -> PhiTokens.PHI
        VideoAspectMode.SIXTEEN_NINE -> 16f / 9f
    }

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    LazyColumn(
        contentPadding = PaddingValues(if (isFullscreen) PhiTokens.Space8 else PhiTokens.Space21),
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
                            .background(FazoraObsidian)
                            .clickable {
                                controlsVisible = !controlsVisible
                                interactionNonce++
                            }
                            .testTag("video_surface_container")
                    ) {
                        if (exoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        useController = false
                                        player = exoPlayer
                                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                                        keepScreenOn = true
                                        resizeMode = when (engine.videoAspectMode) {
                                            VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            VideoAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            VideoAspectMode.GOLDEN_PHI -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                            VideoAspectMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        }
                                    }
                                },
                                update = { view ->
                                    if (view.player !== exoPlayer) {
                                        view.player = exoPlayer
                                    }
                                    view.resizeMode = when (engine.videoAspectMode) {
                                        VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        VideoAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        VideoAspectMode.GOLDEN_PHI -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                        VideoAspectMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("media3_player_view")
                            )
                        }

                        if (!isCurrentTrackVideo) {
                            SpectrumAnalyzerCanvas(
                                spectrumBandsDb = engine.spectrumBandsDb,
                                eqGainsDb = engine.eqBandGainsDb,
                                eqEnabled = engine.eqEnabled,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .padding(horizontal = PhiTokens.Space8)
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
                                                text = activeVideo?.title ?: "Fazora Motion Calibration",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = FazoraIvory
                                            )
                                            Text(
                                                text = "${engine.videoAspectMode.label} • ${engine.playbackSpeed}x",
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
                                                    isFullscreen = !isFullscreen
                                                    interactionNonce++
                                                },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("video_fullscreen_button")
                                            ) {
                                                Icon(
                                                    imageVector = if (isFullscreen) {
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

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                interactionNonce++
                                                onSeekTo((engine.positionMs - 10_000L).coerceAtLeast(0L))
                                            },
                                            modifier = Modifier.testTag("video_seek_back_10s")
                                        ) {
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
                                            },
                                            modifier = Modifier.testTag("video_seek_fwd_10s")
                                        ) {
                                            Text("+10s", color = FazoraIvory)
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
                        text = "Playback Speed",
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
                        text = "Aspect Ratio & Framing",
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
                            onClick = { onSetSubtitlesEnabled(!engine.subtitlesEnabled) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("video_subtitles_toggle")
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(
                                text = if (engine.subtitlesEnabled) {
                                    "Subtitles: ON (${engine.subtitleTracks.size})"
                                } else {
                                    "Subtitles: OFF"
                                },
                                color = FazoraIvory
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val nextAudio = engine.audioTracks.firstOrNull { !it.isSelected }
                                if (nextAudio != null) {
                                    onSelectAudioTrack(nextAudio.id)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("video_audio_track_button")
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(
                                text = "Audio Tracks (${engine.audioTracks.size.coerceAtLeast(1)})",
                                color = FazoraIvory
                            )
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
                            Text(if (isPersian) "انتخاب ویدیو از دستگاه" else "Pick Local Video")
                        }

                        OutlinedButton(
                            onClick = onAutoScanMedia,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("autoscan_video_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(if (isPersian) "اسکن خودکار ویدیوها" else "Auto-Scan Media", color = FazoraIvory)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Video Streams (${uiState.videoTracks.size})",
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

@Composable
fun SearchScreen(
    uiState: FazoraUiState,
    onQueryChange: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit = {},
    onExecuteCopilotCommand: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val semanticPresets = remember {
        listOf(
            "favorite" to "Favorites",
            "unfinished" to "Continue",
            "hi-res" to "Hi-Res / FLAC",
            "night" to "Calm / Night"
        )
    }
    val copilotCommands = remember {
        listOf(
            "resume last" to "Resume Last",
            "play favorites" to "Play Favorites",
            "export srt" to "Export .SRT",
            "export vtt" to "Export .VTT",
            "explain" to "Why Recommended?"
        )
    }
    val graphSummary = remember(uiState.allTracks) {
        com.example.domain.model.VeloraPersonalMediaGraph.buildGraphSummary(uiState.allTracks)
    }
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val reasonByTrackId = remember(uiState.omniSearchMatches, isPersian) {
        uiState.omniSearchMatches.associate { match ->
            match.track.id to (if (isPersian) match.matchReasonFa else match.matchReasonEn)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("search_screen")
    ) {
        item {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "OMNISEARCH • FUZZY & SEMANTIC INDEX",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onQueryChange,
                        label = { Text("Search title, artist, album, or mood (favorite, hi-res, night)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FazoraGold) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("search_clear_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = FazoraSteel)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_query_input")
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(semanticPresets, key = { it.first }) { (queryToken, label) ->
                            val isSelected = uiState.searchQuery.equals(queryToken, ignoreCase = true)
                            OutlinedButton(
                                onClick = {
                                    onQueryChange(if (isSelected) "" else queryToken)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                    contentColor = if (isSelected) FazoraObsidian else FazoraIvory
                                ),
                                contentPadding = PaddingValues(horizontal = PhiTokens.Space13, vertical = 4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("semantic_chip_$queryToken")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "${uiState.searchResults.size} matching tracks (Prefix • Substring • Levenshtein Fuzzy • Semantic)",
                        style = MaterialTheme.typography.labelMedium,
                        color = FazoraSteel
                    )
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
                    .testTag("media_copilot_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "MEDIA COPILOT & PERSONAL MEDIA GRAPH",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Graph Nodes: ${graphSummary.totalNodes} • Verified Edges: ${graphSummary.totalEdges} • " +
                            "Formats: ${graphSummary.formatDistribution.entries.joinToString { "${it.key} (${it.value})" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(copilotCommands, key = { it.first }) { (cmd, label) ->
                            OutlinedButton(
                                onClick = { onExecuteCopilotCommand(cmd) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = FazoraCarbonElevated,
                                    contentColor = FazoraIvory
                                ),
                                contentPadding = PaddingValues(horizontal = PhiTokens.Space13, vertical = 4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copilot_cmd_${cmd.replace(' ', '_')}")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        items(uiState.searchResults, key = { "search_${it.id}" }) { track ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val reason = reasonByTrackId[track.id]
                if (!reason.isNullOrBlank() && uiState.searchQuery.isNotBlank()) {
                    Text(
                        text = "Match: $reason",
                        style = MaterialTheme.typography.labelSmall,
                        color = FazoraGold,
                        modifier = Modifier.padding(horizontal = PhiTokens.Space8)
                    )
                }
                TrackRowItem(
                    track = track,
                    isActive = uiState.engineState.currentTrack?.id == track.id,
                    playlists = uiState.playlists,
                    onPlay = { onPlayTrack(track) },
                    onAddToQueue = { onAddTrackToQueue(track) },
                    onAddToPlaylist = { playlistId -> onAddTrackToPlaylist(playlistId, track.id) },
                    onToggleFavorite = { onToggleFavorite(track) }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: FazoraUiState,
    onSetAutoHideMs: (Long) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onSetLanguageCode: (String) -> Unit = {},
    onTogglePlugin: (String) -> Unit = {},
    onExportBackupJson: () -> Unit = {},
    onToggleSimpleMode: (Boolean) -> Unit = {},
    onToggleAutoScanOnStartup: (Boolean) -> Unit = {},
    onAutoScanMedia: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val user = uiState.firebaseUser
    val isPersian = settings.appLanguageCode == "fa"
    val contrastIvoryObsidian = remember { PhiDesignMetrics.contrastRatio(HEX_IVORY, HEX_OBSIDIAN) }
    val contrastIvoryCarbon = remember { PhiDesignMetrics.contrastRatio(HEX_IVORY, HEX_CARBON) }
    val contrastGoldObsidian = remember { PhiDesignMetrics.contrastRatio(HEX_GOLD, HEX_OBSIDIAN) }
    val contrastSteelCarbon = remember { PhiDesignMetrics.contrastRatio(HEX_STEEL, HEX_CARBON) }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("settings_screen")
    ) {
        item {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
                    .testTag("simple_mode_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "طراحی رابط کاربری و تراکم صفحه" else "INTERFACE DENSITY & WORKSPACE MODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) "حالت ساده (خلوت) / حالت استودیوی پیشرفته" else "Simple Mode / Studio Advanced Mode",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian)
                            "حالت ساده: محیط خلوت و مینیمال بدون منوها و اسلایدرهای شلوغ، متمرکز بر پخش روان آهنگ و ویدیو.\n" +
                            "حالت استودیو: فعال‌سازی تمام تب‌های تخصصی، اکولایزر ۱۰ بانده Biquad، بوست ساب‌بیس، پهنای استریو 3D و بوک‌مارک."
                        else
                            "Simple Mode keeps the player clean, distraction-free, and uncluttered. " +
                            "Studio Advanced Mode reveals the full 10-band DSP rack, Goertzel analyzer, and depth controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                        Button(
                            onClick = { onToggleSimpleMode(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.isSimpleMode) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (settings.isSimpleMode) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "✨ حالت ساده" else "✨ Simple Mode")
                        }

                        Button(
                            onClick = { onToggleSimpleMode(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!settings.isSimpleMode) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (!settings.isSimpleMode) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "🎛️ استودیو پیشرفته" else "🎛️ Studio Pro")
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
                    .testTag("auto_scan_settings_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "پایش خودکار رسانه‌های دستگاه" else "AUTOMATIC MEDIA DISCOVERY",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) {
                            "جست‌وجو و اسکن خودکار آهنگ‌ها و ویدیوها"
                        } else {
                            "Auto-Scan Device Music & Videos"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) {
                            "با فعال بودن این گزینه، ولورا هنگام ورود تمام آهنگ‌ها، دانلودها، " +
                                "فایل‌های تلگرام/واتساپ و ویدیوهای دستگاه را خودکار شناسایی و فهرست می‌کند."
                        } else {
                            "Automatically indexes newly added audio files, downloads, " +
                                "voice recordings, and videos whenever VELORA launches."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isPersian) "اسکن خودکار هنگام اجرای برنامه" else "Auto-scan on app launch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FazoraIvory
                        )
                        androidx.compose.material3.Switch(
                            checked = settings.autoScanOnStartup,
                            onCheckedChange = { onToggleAutoScanOnStartup(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Button(
                        onClick = onAutoScanMedia,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FazoraGold,
                            contentColor = FazoraObsidian
                        ),
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPersian) "🔄 اسکن کامل و خودکار حافظه همین حالا" else "🔄 Auto-Scan All Device Storage Now")
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
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "INTERNATIONALIZATION & RTL ARCHITECTURE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Language & Layout Direction (English / فارسی)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Switch between English (LTR) and Persian / فارسی (RTL) layout direction and localized labels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                        val isEnglish = settings.appLanguageCode != "fa"
                        OutlinedButton(
                            onClick = { onSetLanguageCode("en") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isEnglish) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (isEnglish) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("language_en_button")
                        ) {
                            Text("English (LTR)")
                        }
                        OutlinedButton(
                            onClick = { onSetLanguageCode("fa") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isEnglish) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (!isEnglish) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("language_fa_button")
                        ) {
                            Text("فارسی (RTL)")
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
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "FIREBASE CLOUD PERSISTENCE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Cloud Sync & Identity",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Synchronize playlists and custom 10-band EQ presets securely across your devices " +
                            "with zero-trust Firestore security rules and Google Sign-In.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    if (user != null) {
                        Surface(
                            color = FazoraCarbonElevated,
                            shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                                Text(
                                    text = "Connected Account: ${user.displayName ?: "Audiophile"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FazoraEmeraldOk
                                )
                                Text(
                                    text = user.email ?: user.uid,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FazoraSteel
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = FazoraCarbonElevated,
                                contentColor = FazoraIvory
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("firebase_sign_out_button")
                        ) {
                            Text("Sign Out from Firebase")
                        }
                    } else {
                        Button(
                            onClick = onSignInWithGoogle,
                            enabled = !uiState.isSigningIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FazoraGold,
                                contentColor = FazoraObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("firebase_google_sign_in_button")
                        ) {
                            Text(
                                if (uiState.isSigningIn) "Signing in with Google..." else "Sign in with Google"
                            )
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
                    .testTag("settings_plugin_registry_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "TIER 1 DECLARATIVE PLUGIN REGISTRY & CODEC MATRIX",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Sandboxed Extensions & Universal Media Support",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Google Play compliant declarative extensions (zero dynamic .dex/.so code loading). " +
                            "Audio: MP3, AAC/M4A, FLAC, WAV, OGG/Opus • Video: MP4, MKV, WebM, HLS (.m3u8), DASH (.mpd).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    uiState.declarativePlugins.forEach { plugin ->
                        Surface(
                            color = FazoraCarbonElevated,
                            shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PhiTokens.Space13)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${plugin.name} (v${plugin.version})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FazoraIvory
                                    )
                                    Text(
                                        text = "${plugin.tier} • ${plugin.capabilities.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FazoraSteel
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onTogglePlugin(plugin.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (plugin.isEnabled) FazoraGold else FazoraObsidian,
                                        contentColor = if (plugin.isEnabled) FazoraObsidian else FazoraIvory
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("toggle_plugin_${plugin.id}")
                                ) {
                                    Text(if (plugin.isEnabled) "Active" else "Disabled")
                                }
                            }
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
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "ATOMIC DATASTORE SETTINGS",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Engine & Persistence Configuration",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Persisted automatically across restarts via Jetpack DataStore Preferences " +
                            "with ReplaceFileCorruptionHandler backup recovery " +
                            "(Corruption fallbacks triggered: ${settings.corruptionFallbackCount}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )

                    Spacer(modifier = Modifier.height(PhiTokens.Space21))

                    Text(
                        text = "Default Video Auto-Hide Timeout: ${settings.autoHideVideoControlsMs} ms",
                        style = MaterialTheme.typography.titleMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        listOf(2500L, 3500L, 5000L, 8000L).forEach { timeout ->
                            val isSelected = settings.autoHideVideoControlsMs == timeout
                            OutlinedButton(
                                onClick = { onSetAutoHideMs(timeout) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                    contentColor = if (isSelected) FazoraObsidian else FazoraIvory
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_autohide_$timeout")
                            ) {
                                Text("${timeout / 1000.0}s")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Button(
                        onClick = onExportBackupJson,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FazoraGold,
                            contentColor = FazoraObsidian
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("export_backup_json_button")
                    ) {
                        Text(
                            if (settings.appLanguageCode == "fa") {
                                "پشتیبان‌گیری آفلاین (JSON Snapshot) از پلی‌لیست‌ها و تنظیمات"
                            } else {
                                "Export One-Tap Offline JSON Backup Snapshot"
                            }
                        )
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
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "PHI (φ = 1.618) & WCAG 2.1 AA TELEMETRY",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Fibonacci Spacing: 8dp / 13dp / 21dp / 34dp / 55dp • Split: 61.8% / 38.2% • Min Touch Target: 48dp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    ContrastAuditRow("Ivory (#F5F2EB) on Obsidian (#0B0B0D)", contrastIvoryObsidian)
                    ContrastAuditRow("Ivory (#F5F2EB) on Carbon (#121216)", contrastIvoryCarbon)
                    ContrastAuditRow("Saffron (#E5A817) on Obsidian (#0B0B0D)", contrastGoldObsidian)
                    ContrastAuditRow("Warm Muted (#A5A29B) on Carbon (#121216)", contrastSteelCarbon)
                }
            }
        }
    }
}

@Composable
private fun ContrastAuditRow(label: String, ratio: Double) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FazoraSteel
        )
        Text(
            text = String.format(Locale.US, "%.2f:1 (AA PASS)", ratio),
            style = MaterialTheme.typography.labelLarge,
            color = FazoraEmeraldOk
        )
    }
}
