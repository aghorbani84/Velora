package com.example.ui.navigation

import android.app.Activity
import android.app.PictureInPictureParams
import android.util.Rational
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.domain.model.MediaType
import com.example.ui.screens.ArtistScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.PlayerAndEqScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.viewmodel.VeloraUiState
import com.example.ui.viewmodel.VeloraViewModel
import com.example.ui.viewmodel.addCustomRadioStation
import com.example.ui.viewmodel.addCustomSmartRule
import com.example.ui.viewmodel.addNewSyncedLyricLineAtCurrentPosition
import com.example.ui.viewmodel.addRadioStationToQueue
import com.example.ui.viewmodel.addTimeMachineBookmark
import com.example.ui.viewmodel.adjustLyricsOffset
import com.example.ui.viewmodel.clearAbLoop
import com.example.ui.viewmodel.executeCopilotCommand
import com.example.ui.viewmodel.exportCurrentLyricsAsLrc
import com.example.ui.viewmodel.exportOfflineBackupJson
import com.example.ui.viewmodel.exportPlaylistAsM3u8
import com.example.ui.viewmodel.importCustomLyricsOrSrt
import com.example.ui.viewmodel.importM3u8Playlist
import com.example.ui.viewmodel.playNetworkStream
import com.example.ui.viewmodel.playNextInQueue
import com.example.ui.viewmodel.playSmartPlaylist
import com.example.ui.viewmodel.restoreOfflineBackupJson
import com.example.ui.viewmodel.setAbLoopPointA
import com.example.ui.viewmodel.setAbLoopPointB
import com.example.ui.viewmodel.setAdaptiveAudioMode
import com.example.ui.viewmodel.setAutoScanOnStartup
import com.example.ui.viewmodel.setBackgroundVideoAudio
import com.example.ui.viewmodel.setBassBoostPercent
import com.example.ui.viewmodel.setCrossfadeSeconds
import com.example.ui.viewmodel.setLowBatteryMode
import com.example.ui.viewmodel.setOnlyValidMusicInSearch
import com.example.ui.viewmodel.setPreampGainDb
import com.example.ui.viewmodel.setSleepTimerMinutes
import com.example.ui.viewmodel.setStereoWidthPercent
import com.example.ui.viewmodel.setSubtitleColorTheme
import com.example.ui.viewmodel.setSubtitleFontScale
import com.example.ui.viewmodel.setThemePaletteMode
import com.example.ui.viewmodel.setVideoBrightnessLevel
import com.example.ui.viewmodel.stampLyricLineAtCurrentPosition
import com.example.ui.viewmodel.toggleDeclarativePlugin
import com.example.ui.viewmodel.tuneRadioStation
import com.example.ui.viewmodel.updateTrackMetadata

object NavGraph {
    const val DEFAULT_START_ROUTE = "music"
}

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
    SETTINGS("settings", "Settings", "تنظیمات", Icons.Default.Settings);

    companion object {
        fun fromRoute(route: String?): VeloraDestination {
            return entries.firstOrNull { it.route == route } ?: MUSIC
        }
    }
}

fun NavHostController.navigateSingleTopTo(destination: VeloraDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun VeloraNavGraph(
    navController: NavHostController,
    viewModel: VeloraViewModel,
    uiState: VeloraUiState,
    startDestination: VeloraDestination = VeloraDestination.MUSIC,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val media3ExoPlayer = viewModel.exoPlayer

    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        composable(VeloraDestination.LIBRARY.route) {
            LibraryScreen(
                uiState = uiState,
                onScanLibrary = { viewModel.scanLibrary() },
                onAddFolderUri = { viewModel.addFolderUri(it) },
                onCreateCustomFolder = { viewModel.createAndScanCustomFolder(it) },
                onRemoveFolder = { viewModel.removeFolder(it) },
                onCreatePlaylist = { viewModel.createPlaylist(it) },
                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                onPlayPlaylist = { playlist, shuffle -> viewModel.playPlaylist(playlist, shuffle) },
                onPlayTrack = { track ->
                    viewModel.playTrack(track, uiState.allTracks)
                    if (track.mediaType == MediaType.VIDEO) {
                        navController.navigateSingleTopTo(VeloraDestination.VIDEO)
                    }
                },
                onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onSelectSortOrder = { viewModel.setSortOrder(it) },
                onSelectFilterMode = { viewModel.setFilterMode(it) },
                onResumeHistory = { viewModel.resumeFromHistory(it) },
                onClearHistory = { viewModel.clearPlaybackHistory() },
                onPlaySmartPlaylist = { playlist, shuffle -> viewModel.playSmartPlaylist(playlist, shuffle) },
                onPlayNetworkStream = { url, title -> viewModel.playNetworkStream(url, title) },
                onAddCustomSmartRule = { name, br, sr, plays, favs, kw ->
                    viewModel.addCustomSmartRule(name, br, sr, plays, favs, kw)
                },
                onExportM3u8 = { pid -> viewModel.exportPlaylistAsM3u8(pid, context.filesDir) },
                onImportM3u8 = { name, content -> viewModel.importM3u8Playlist(name, content) },
                onPlayNext = { viewModel.playNextInQueue(it) }
            )
        }

        composable(VeloraDestination.MUSIC.route) {
            MusicScreen(
                uiState = uiState,
                onPlayAll = { viewModel.playAllMusic(shuffle = false) },
                onShuffleAll = { viewModel.playAllMusic(shuffle = true) },
                onPlayTrack = { viewModel.playTrack(it, uiState.audioTracks) },
                onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAutoScanMedia = { viewModel.autoScanDeviceMedia() }
            )
        }

        composable(VeloraDestination.RADIO.route) {
            RadioScreen(
                uiState = uiState,
                onTuneStation = { viewModel.tuneRadioStation(it) },
                onAddStationToQueue = { viewModel.addRadioStationToQueue(it) },
                onAddCustomStation = { name, url, genre ->
                    viewModel.addCustomRadioStation(name, url, genre)
                }
            )
        }

        composable(VeloraDestination.ARTISTS.route) {
            ArtistScreen(
                uiState = uiState,
                onSelectArtist = { viewModel.selectArtist(it) },
                onPlayArtist = { artist, shuffle -> viewModel.playArtistTracks(artist, shuffle) },
                onPlayTrack = { track ->
                    val artistPool = uiState.artistsMap[track.artist].orEmpty()
                    viewModel.playTrack(track, artistPool)
                },
                onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
            )
        }

        composable(VeloraDestination.VIDEO.route) {
            VideoPlayerScreen(
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
                onAutoScanMedia = { viewModel.autoScanDeviceMedia() },
                onSetVolume = { viewModel.setVolume(it) },
                onSetVideoBrightness = { viewModel.setVideoBrightnessLevel(it) },
                onSetSubtitleFontScale = { viewModel.setSubtitleFontScale(it) },
                onSetSubtitleColorTheme = { viewModel.setSubtitleColorTheme(it) },
                onAddVideoBookmark = { viewModel.addTimeMachineBookmark("Cinema Scene") }
            )
        }

        composable(VeloraDestination.SEARCH.route) {
            SearchScreen(
                uiState = uiState,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onPlayTrack = { viewModel.playTrack(it, uiState.searchResults) },
                onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onExecuteCopilotCommand = { cmd -> viewModel.executeCopilotCommand(cmd) },
                onToggleOnlyValidMusic = { viewModel.setOnlyValidMusicInSearch(it) }
            )
        }

        composable(VeloraDestination.PLAYER.route) {
            PlayerAndEqScreen(
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
                onToggleSimpleMode = { viewModel.setSimpleMode(it) },
                onEditTrackMetadata = { id, title, artist, album ->
                    viewModel.updateTrackMetadata(id, title, artist, album)
                },
                onStampLyricLine = { viewModel.stampLyricLineAtCurrentPosition(it) },
                onAddNewLyricLineAtPos = { viewModel.addNewSyncedLyricLineAtCurrentPosition(it) },
                onExportLrc = { viewModel.exportCurrentLyricsAsLrc(context.filesDir) }
            )
        }

        composable(VeloraDestination.SETTINGS.route) {
            SettingsScreen(
                uiState = uiState,
                onSetAutoHideMs = { viewModel.updateAutoHideControlsMs(it) },
                onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
                onSignOut = { viewModel.signOut() },
                onSetLanguageCode = { viewModel.setAppLanguageCode(it) },
                onTogglePlugin = { viewModel.toggleDeclarativePlugin(it) },
                onExportBackupJson = { viewModel.exportOfflineBackupJson(context.filesDir) },
                onRestoreBackupJson = { viewModel.restoreOfflineBackupJson(it, context.filesDir) },
                onToggleSimpleMode = { viewModel.setSimpleMode(it) },
                onToggleAutoScanOnStartup = { viewModel.setAutoScanOnStartup(it) },
                onAutoScanMedia = { viewModel.autoScanDeviceMedia() },
                onToggleOnlyValidMusic = { viewModel.setOnlyValidMusicInSearch(it) },
                onToggleBackgroundVideoAudio = { viewModel.setBackgroundVideoAudio(it) },
                onToggleLowBatteryMode = { viewModel.setLowBatteryMode(it) },
                onSetAdaptiveAudioMode = { viewModel.setAdaptiveAudioMode(it) },
                onSetThemePaletteMode = { viewModel.setThemePaletteMode(it) }
            )
        }
    }
}
