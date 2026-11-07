package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.playback.VeloraPlaybackService
import com.example.data.playback.Media3PlayerEngine
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.ArtistScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.PlayerAndEqScreen
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
import com.example.ui.viewmodel.addTimeMachineBookmark
import com.example.ui.viewmodel.adjustLyricsOffset
import com.example.ui.viewmodel.executeCopilotCommand
import com.example.ui.viewmodel.playNetworkStream
import com.example.ui.viewmodel.playSmartPlaylist
import com.example.ui.viewmodel.toggleDeclarativePlugin
import org.koin.androidx.compose.koinViewModel

enum class VeloraDestination(
    val route: String,
    val label: String,
    val labelFa: String,
    val icon: ImageVector
) {
    LIBRARY("library", "Library", "کتابخانه", Icons.Default.LibraryMusic),
    MUSIC("music", "Music", "موسیقی", Icons.Default.MusicNote),
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

        // Start foreground-capable MediaSessionService so background playback persists
        runCatching {
            startService(Intent(this, VeloraPlaybackService::class.java))
        }

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
    var currentDestination by rememberSaveable { mutableStateOf(VeloraDestination.LIBRARY) }
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    // BackHandler returns to Library from any secondary destination
    BackHandler(enabled = currentDestination != VeloraDestination.LIBRARY) {
        currentDestination = VeloraDestination.LIBRARY
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
                    VeloraDestination.entries.forEach { dest ->
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
                                onOpenFullPlayer = { currentDestination = VeloraDestination.PLAYER }
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
                                VeloraDestination.entries.forEach { dest ->
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
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
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
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
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
                            onAddBookmark = { viewModel.addTimeMachineBookmark() }
                        )

                        VeloraDestination.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onSetAutoHideMs = { viewModel.updateAutoHideControlsMs(it) },
                            onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                            onSignOut = { viewModel.signOut() },
                            onSetLanguageCode = { viewModel.setAppLanguageCode(it) },
                            onTogglePlugin = { viewModel.toggleDeclarativePlugin(it) }
                        )
                    }
                }
            }
        }
    }
    }
}
