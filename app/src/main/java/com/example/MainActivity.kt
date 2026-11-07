package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.Activity
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.playback.Media3PlayerEngine
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.ArtistScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.PlayerAndEqScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.VeloraTheme
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.VeloraViewModel
import com.example.ui.viewmodel.addCustomRadioStation
import com.example.ui.viewmodel.addRadioStationToQueue
import com.example.ui.viewmodel.addTimeMachineBookmark
import com.example.ui.viewmodel.adjustLyricsOffset
import com.example.ui.viewmodel.clearAbLoop
import com.example.ui.viewmodel.executeCopilotCommand
import com.example.ui.viewmodel.exportOfflineBackupJson
import com.example.ui.viewmodel.importCustomLyricsOrSrt
import com.example.ui.viewmodel.playNetworkStream
import com.example.ui.viewmodel.playSmartPlaylist
import com.example.ui.viewmodel.setAbLoopPointA
import com.example.ui.viewmodel.setAbLoopPointB
import com.example.ui.viewmodel.setBassBoostPercent
import com.example.ui.viewmodel.setCrossfadeSeconds
import com.example.ui.viewmodel.setPreampGainDb
import com.example.ui.viewmodel.setSleepTimerMinutes
import com.example.ui.viewmodel.setStereoWidthPercent
import com.example.ui.viewmodel.toggleDeclarativePlugin
import com.example.ui.viewmodel.tuneRadioStation
import org.koin.androidx.compose.koinViewModel

enum class VeloraDestination(
    val route: String,
    val label: String,
    val labelFa: String,
    val icon: ImageVector
) {
    LIBRARY("library", "Library", "کتابخانه", Icons.Default.LibraryMusic),
    MUSIC("music", "Music", "موسیقی", Icons.Default.MusicNote),
    RADIO("radio", "Radio", "رادیو", Icons.Default.Radio),
    ARTISTS("artists", "Artists", "هنرمندان", Icons.Default.Album),
    VIDEO("video", "Video", "ویدیو", Icons.Default.Movie),
    SEARCH("search", "Search", "جستجو", Icons.Default.Search),
    PLAYER("player", "Player & EQ", "پخش و اکولایزر", Icons.Default.GraphicEq),
    SETTINGS("settings", "Settings", "تنظیمات", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VeloraTheme {
                VeloraRootApp()
            }
        }
    }
}

@Composable
fun VeloraRootApp(
    viewModel: VeloraViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val isSimpleMode = uiState.settings.isSimpleMode
    val homeDestination = if (isSimpleMode) VeloraDestination.MUSIC else VeloraDestination.LIBRARY
    var currentDestination by rememberSaveable { mutableStateOf(VeloraDestination.MUSIC) }
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    val visibleDestinations = if (isSimpleMode) {
        listOf(
            VeloraDestination.MUSIC,
            VeloraDestination.VIDEO,
            VeloraDestination.RADIO,
            VeloraDestination.SETTINGS
        )
    } else {
        VeloraDestination.entries
    }

    LaunchedEffect(isSimpleMode) {
        if (isSimpleMode && currentDestination !in visibleDestinations && currentDestination != VeloraDestination.PLAYER) {
            currentDestination = VeloraDestination.MUSIC
        }
    }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.autoScanDeviceMedia()
    }

    LaunchedEffect(uiState.settings.autoScanOnStartup) {
        if (uiState.settings.autoScanOnStartup) {
            val allGranted = permissionsToRequest.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                viewModel.autoScanDeviceMedia()
            }
        }
    }

    // BackHandler returns to home destination from any secondary destination
    BackHandler(enabled = currentDestination != homeDestination) {
        currentDestination = homeDestination
    }

    val media3ExoPlayer = (viewModel.playerEngine as? Media3PlayerEngine)?.exoPlayer

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(FazoraObsidian)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
        val useNavigationRail = maxWidth >= 680.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = FazoraCarbon,
                    contentColor = FazoraIvory,
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("fazora_nav_rail")
                ) {
                    visibleDestinations.forEach { dest ->
                        val selected = currentDestination == dest
                        val navLabel = if (isPersian) dest.labelFa else dest.label
                        NavigationRailItem(
                            selected = selected,
                            onClick = { currentDestination = dest },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = navLabel
                                )
                            },
                            label = {
                                Text(
                                    text = navLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = FazoraObsidian,
                                selectedTextColor = FazoraGold,
                                indicatorColor = FazoraGold,
                                unselectedIconColor = FazoraSteel,
                                unselectedTextColor = FazoraSteel
                            ),
                            modifier = Modifier.testTag("nav_item_${dest.route}")
                        )
                    }
                }
            }

            Scaffold(
                containerColor = FazoraObsidian,
                topBar = {
                    Surface(
                        color = FazoraCarbon,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VELORA",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = FazoraGold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = FazoraCarbonElevated,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isSimpleMode) "SIMPLE" else "STUDIO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSimpleMode) FazoraIvory else FazoraGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { permissionLauncher.launch(permissionsToRequest) },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("top_bar_auto_scan_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Auto-scan media",
                                        tint = FazoraGold
                                    )
                                }

                                Button(
                                    onClick = { viewModel.setSimpleMode(!isSimpleMode) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSimpleMode) FazoraCarbonElevated else FazoraGold,
                                        contentColor = if (isSimpleMode) FazoraGold else FazoraObsidian
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("top_bar_mode_switcher_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSimpleMode) Icons.Default.Tune else Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSimpleMode) {
                                            if (isPersian) "حالت پیشرفته" else "Studio Pro"
                                        } else {
                                            if (isPersian) "حالت ساده" else "Simple Mode"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        if (uiState.statusBannerMessage != null) {
                            Surface(
                                color = FazoraGoldMuted,
                                shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PhiTokens.Space13, vertical = 4.dp)
                                    .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius8))
                                    .testTag("status_banner")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = uiState.statusBannerMessage.orEmpty(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = FazoraIvory,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.dismissBanner() }) {
                                        Text("OK", color = FazoraGold)
                                    }
                                }
                            }
                        }

                        if (currentDestination != VeloraDestination.PLAYER) {
                            MiniPlayerBar(
                                engineState = uiState.engineState,
                                repeatMode = uiState.queue.repeatMode,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onNext = { viewModel.skipToNext() },
                                onCycleRepeat = { viewModel.cycleRepeatMode() },
                                onOpenFullPlayer = { currentDestination = VeloraDestination.PLAYER },
                                onPrevious = { viewModel.skipToPrevious() }
                            )
                        }

                        if (!useNavigationRail) {
                            NavigationBar(
                                containerColor = FazoraCarbon,
                                contentColor = FazoraIvory,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fazora_bottom_nav")
                            ) {
                                visibleDestinations.forEach { dest ->
                                    val selected = currentDestination == dest
                                    val navLabel = if (isPersian) dest.labelFa else dest.label
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentDestination = dest },
                                        icon = {
                                            Icon(
                                                imageVector = dest.icon,
                                                contentDescription = navLabel
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = navLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = FazoraObsidian,
                                            selectedTextColor = FazoraGold,
                                            indicatorColor = FazoraGold,
                                            unselectedIconColor = FazoraSteel,
                                            unselectedTextColor = FazoraSteel
                                        ),
                                        modifier = Modifier.testTag("nav_item_${dest.route}")
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentDestination) {
                        VeloraDestination.LIBRARY -> LibraryScreen(
                            uiState = uiState,
                            onScanLibrary = { viewModel.scanLibrary() },
                            onAddFolderUri = { viewModel.addFolderUri(it) },
                            onCreateCustomFolder = { viewModel.createAndScanCustomFolder(it) },
                            onRemoveFolder = { viewModel.removeFolder(it) },
                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onPlayPlaylist = { playlist, shuffle ->
                                viewModel.playPlaylist(playlist, shuffle)
                            },
                            onPlayTrack = { track ->
                                viewModel.playTrack(track, uiState.allTracks)
                                if (track.mediaType == com.example.domain.model.MediaType.VIDEO) {
                                    currentDestination = VeloraDestination.VIDEO
                                }
                            },
                            onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                            onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSelectSortOrder = { viewModel.setSortOrder(it) },
                            onSelectFilterMode = { viewModel.setFilterMode(it) },
                            onResumeHistory = { viewModel.resumeFromHistory(it) },
                            onClearHistory = { viewModel.clearPlaybackHistory() },
                            onPlaySmartPlaylist = { playlist, shuffle ->
                                viewModel.playSmartPlaylist(playlist, shuffle)
                            },
                            onPlayNetworkStream = { url, title ->
                                viewModel.playNetworkStream(url, title)
                            }
                        )

                        VeloraDestination.MUSIC -> MusicScreen(
                            uiState = uiState,
                            onPlayAll = { viewModel.playAllMusic(shuffle = false) },
                            onShuffleAll = { viewModel.playAllMusic(shuffle = true) },
                            onPlayTrack = { viewModel.playTrack(it, uiState.audioTracks) },
                            onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                            onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAutoScanMedia = { viewModel.autoScanDeviceMedia() }
                        )

                        VeloraDestination.RADIO -> RadioScreen(
                            uiState = uiState,
                            onTuneStation = { viewModel.tuneRadioStation(it) },
                            onAddStationToQueue = { viewModel.addRadioStationToQueue(it) },
                            onAddCustomStation = { name, url, genre ->
                                viewModel.addCustomRadioStation(name, url, genre)
                            }
                        )

                        VeloraDestination.ARTISTS -> ArtistScreen(
                            uiState = uiState,
                            onSelectArtist = { viewModel.selectArtist(it) },
                            onPlayArtist = { artist, shuffle ->
                                viewModel.playArtistTracks(artist, shuffle)
                            },
                            onPlayTrack = { track ->
                                val artistPool = uiState.artistsMap[track.artist].orEmpty()
                                viewModel.playTrack(track, artistPool)
                            },
                            onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                            onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )

                        VeloraDestination.VIDEO -> VideoPlayerScreen(
                            uiState = uiState,
                            exoPlayer = media3ExoPlayer,
                            onPlayVideoTrack = { viewModel.playTrack(it, uiState.videoTracks) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSeekTo = { viewModel.seekTo(it) },
                            onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) },
                            onSetAspectMode = { viewModel.setVideoAspectMode(it) },
                            onSetSubtitlesEnabled = { viewModel.setSubtitlesEnabled(it) },
                            onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
                            onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                            onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onEnterPip = {
                                runCatching {
                                    val activity = context as? Activity
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(16, 9))
                                        .build()
                                    activity?.enterPictureInPictureMode(params)
                                }
                            },
                            onAutoScanMedia = { viewModel.autoScanDeviceMedia() }
                        )

                        VeloraDestination.SEARCH -> SearchScreen(
                            uiState = uiState,
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onPlayTrack = { viewModel.playTrack(it, uiState.searchResults) },
                            onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                            onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onExecuteCopilotCommand = { cmd -> viewModel.executeCopilotCommand(cmd) }
                        )

                        VeloraDestination.PLAYER -> PlayerAndEqScreen(
                            uiState = uiState,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onStop = { viewModel.stopPlayback() },
                            onPrevious = { viewModel.skipToPrevious() },
                            onNext = { viewModel.skipToNext() },
                            onSeekTo = { viewModel.seekTo(it) },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onCycleRepeat = { viewModel.cycleRepeatMode() },
                            onVolumeChange = { viewModel.setVolume(it) },
                            onToggleMute = { viewModel.toggleMute() },
                            onSetEqEnabled = { viewModel.setEqEnabled(it) },
                            onSetEqBandGain = { idx, gain -> viewModel.setEqBandGain(idx, gain) },
                            onApplyPreset = { viewModel.applyEqPreset(it) },
                            onSaveCustomPreset = { viewModel.saveCustomEqPreset(it) },
                            onDeleteCustomPreset = { viewModel.deleteCustomEqPreset(it) },
                            onSelectQueueTrack = { viewModel.selectQueueTrack(it) },
                            onRemoveFromQueue = { viewModel.removeTrackFromQueue(it) },
                            onClearError = { viewModel.clearEngineError() },
                            onSetVisualizerMode = { viewModel.setVisualizerMode(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onAdjustLyricsOffset = { viewModel.adjustLyricsOffset(it) },
                            onAddBookmark = { viewModel.addTimeMachineBookmark() },
                            onSetPreampDb = { viewModel.setPreampGainDb(it) },
                            onSetBassBoost = { viewModel.setBassBoostPercent(it) },
                            onSetStereoWidth = { viewModel.setStereoWidthPercent(it) },
                            onSetCrossfadeSec = { viewModel.setCrossfadeSeconds(it) },
                            onSetLoopPointA = { viewModel.setAbLoopPointA() },
                            onSetLoopPointB = { viewModel.setAbLoopPointB() },
                            onClearLoop = { viewModel.clearAbLoop() },
                            onSetSleepTimerMinutes = { viewModel.setSleepTimerMinutes(it) },
                            onImportCustomLrcOrSrt = { viewModel.importCustomLyricsOrSrt(it) },
                            onToggleSimpleMode = { viewModel.setSimpleMode(it) }
                        )

                        VeloraDestination.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onSetAutoHideMs = { viewModel.updateAutoHideControlsMs(it) },
                            onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                            onSignOut = { viewModel.signOut() },
                            onSetLanguageCode = { viewModel.setAppLanguageCode(it) },
                            onTogglePlugin = { viewModel.toggleDeclarativePlugin(it) },
                            onExportBackupJson = { viewModel.exportOfflineBackupJson(context.filesDir) },
                            onToggleSimpleMode = { viewModel.setSimpleMode(it) },
                            onToggleAutoScanOnStartup = { viewModel.setAutoScanOnStartup(it) },
                            onAutoScanMedia = { viewModel.autoScanDeviceMedia() }
                        )
                    }
                }
            }
        }
    }
    }
}
