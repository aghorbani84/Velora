package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.domain.model.MediaType
import com.example.ui.components.VeloraLargeTitleHeader
import com.example.ui.components.VeloraRetainedContentSwap
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.veloraColors
import com.example.ui.viewmodel.VeloraUiState
import com.example.ui.viewmodel.VeloraViewModel
import com.example.ui.viewmodel.addCustomRadioStation
import com.example.ui.viewmodel.addCustomSmartRule
import com.example.ui.viewmodel.addRadioStationToQueue
import com.example.ui.viewmodel.exportPlaylistAsM3u8
import com.example.ui.viewmodel.importM3u8Playlist
import com.example.ui.viewmodel.playSmartPlaylist
import com.example.ui.viewmodel.tuneRadioStation

@Composable
fun LibraryHubScreen(
    uiState: VeloraUiState,
    viewModel: VeloraViewModel,
    selectedSubTab: LibrarySubTab,
    onSelectSubTab: (LibrarySubTab) -> Unit,
    onOpenVideoDestination: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val semantic = MaterialTheme.veloraColors
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(semantic.bg)
            .testTag("library_hub_screen")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
            modifier = Modifier
                .fillMaxWidth()
                .background(semantic.bgSecondary)
                .padding(horizontal = PhiTokens.Space21, vertical = PhiTokens.Space13)
        ) {
            VeloraLargeTitleHeader(
                eyebrow = if (isPersian) "مرکز کتابخانه یکپارچه" else "YOUR COLLECTION",
                title = if (isPersian) "کتابخانه" else "Library",
                subtitle = if (isPersian) {
                    "پلی‌لیست‌ها، آهنگ‌ها، هنرمندان، رادیو زنده و پوشه‌ها در یک مکان"
                } else {
                    "Playlists, music tracks, artists, live radio, and folders in one unified hub"
                }
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                contentPadding = PaddingValues(vertical = PhiTokens.Space2),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_segmented_tabs")
            ) {
                items(LibrarySubTab.entries) { subTab ->
                    VeloraSecondaryButton(
                        text = if (isPersian) subTab.labelFa else subTab.labelEn,
                        icon = subTab.icon,
                        selected = selectedSubTab == subTab,
                        onClick = { onSelectSubTab(subTab) },
                        tag = subTab.testTag
                    )
                }
            }
        }

        VeloraRetainedContentSwap(
            targetState = selectedSubTab,
            label = "library_subtab_swap",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { activeTab ->
            when (activeTab) {
                LibrarySubTab.PLAYLISTS -> {
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
                                onOpenVideoDestination()
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
                        onAddCustomSmartRule = { name, br, sr, plays, favs, kw ->
                            viewModel.addCustomSmartRule(name, br, sr, plays, favs, kw)
                        },
                        onExportM3u8 = { pid -> viewModel.exportPlaylistAsM3u8(pid, context.filesDir) },
                        onImportM3u8 = { name, content -> viewModel.importM3u8Playlist(name, content) }
                    )
                }
                LibrarySubTab.MUSIC -> {
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
                LibrarySubTab.ARTISTS -> {
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
                LibrarySubTab.RADIO -> {
                    RadioScreen(
                        uiState = uiState,
                        onTuneStation = { viewModel.tuneRadioStation(it) },
                        onAddStationToQueue = { viewModel.addRadioStationToQueue(it) },
                        onAddCustomStation = { name, url, genre ->
                            viewModel.addCustomRadioStation(name, url, genre)
                        }
                    )
                }
                LibrarySubTab.FOLDERS -> {
                    FoldersScreen(
                        uiState = uiState,
                        onPlayTrack = { track, folderPool ->
                            viewModel.playTrack(track, folderPool)
                            if (track.mediaType == MediaType.VIDEO) {
                                onOpenVideoDestination()
                            }
                        },
                        onPlayFolder = { folderTracks, shuffle ->
                            viewModel.mediaOrchestrator.playTrackPool(folderTracks, shuffle)
                        },
                        onAddTrackToQueue = { viewModel.addTrackToQueue(it) },
                        onAddTrackToPlaylist = { pid, tid -> viewModel.addTrackToPlaylist(pid, tid) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onScanLibrary = { viewModel.scanLibrary() }
                    )
                }
            }
        }
    }
}
