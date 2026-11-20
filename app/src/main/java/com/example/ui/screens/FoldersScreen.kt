package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.domain.model.ExperienceTier
import com.example.domain.model.Track
import com.example.ui.components.TrackRowItem
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.VeloraUiState

internal fun groupTracksByFolder(tracks: List<Track>, isPersian: Boolean): List<FolderGroup> {
    if (tracks.isEmpty()) return emptyList()
    return tracks
        .groupBy { track ->
            track.folderPath.trim().trimEnd('/').ifBlank { "root" }
        }
        .entries
        .map { (normalizedPath, folderTracks) ->
            val segment = normalizedPath
                .substringAfterLast('/')
                .substringAfterLast(':')
                .ifBlank { if (isPersian) "پوشه اصلی" else "Root Folder" }
            FolderGroup(
                path = normalizedPath,
                displayName = segment,
                tracks = folderTracks
            )
        }
        .sortedBy { it.displayName.lowercase() }
}

@Composable
fun FoldersScreen(
    uiState: VeloraUiState,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayFolder: (List<Track>, Boolean) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onScanLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val tier = uiState.settings.experienceTier
    val folderGroups = remember(uiState.allTracks, isPersian) {
        groupTracksByFolder(uiState.allTracks, isPersian)
    }
    var selectedPath by rememberSaveable { mutableStateOf<String?>(null) }
    val activeFolder = folderGroups.firstOrNull { it.path == selectedPath } ?: folderGroups.firstOrNull()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("folders_screen")
    ) {
        if (folderGroups.isEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(PhiTokens.Space21),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VeloraEmptyState(
                        icon = Icons.Default.FolderOpen,
                        title = if (isPersian) "هنوز پوشه‌ای فهرست نشده است" else "No Media Folders Indexed",
                        subtitle = if (isPersian) {
                            "برای مرور مستقیم پوشه‌های حافظه دستگاه، کتابخانه را اسکن کنید."
                        } else {
                            "Scan your device storage to browse audio and video collections by folder."
                        },
                        actionLabel = if (isPersian) "اسکن حافظه" else "Scan Library",
                        actionTag = "folders_empty_scan_button",
                        onAction = onScanLibrary
                    )
                }
            }
            return@BoxWithConstraints
        }

        val isExpanded = maxWidth >= PhiTokens.WideScreenBreakpoint
        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(PhiTokens.Space13),
                    verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                    modifier = Modifier
                        .weight(PhiTokens.SECONDARY_WEIGHT)
                        .fillMaxHeight()
                ) {
                    items(folderGroups, key = { it.path }) { folder ->
                        VeloraSecondaryButton(
                            text = "${folder.displayName} (${folder.tracks.size})",
                            icon = Icons.Default.Folder,
                            selected = folder.path == activeFolder?.path,
                            onClick = { selectedPath = folder.path },
                            tag = "folder_chip_${folder.path.hashCode()}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                FolderTrackDetailColumn(
                    folder = activeFolder,
                    uiState = uiState,
                    tier = tier,
                    isPersian = isPersian,
                    onPlayTrack = onPlayTrack,
                    onPlayFolder = onPlayFolder,
                    onAddTrackToQueue = onAddTrackToQueue,
                    onAddTrackToPlaylist = onAddTrackToPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier
                        .weight(PhiTokens.PRIMARY_WEIGHT)
                        .fillMaxHeight()
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(PhiTokens.Space21),
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VeloraSectionCard(
                        eyebrow = if (isPersian) "مرور بر اساس پوشه" else "FOLDER BROWSER",
                        title = if (isPersian) {
                            "پوشه‌ها (${folderGroups.size})"
                        } else {
                            "Folders (${folderGroups.size})"
                        },
                        subtitle = activeFolder?.path
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            items(folderGroups, key = { it.path }) { folder ->
                                VeloraSecondaryButton(
                                    text = "${folder.displayName} (${folder.tracks.size})",
                                    icon = Icons.Default.Folder,
                                    selected = folder.path == activeFolder?.path,
                                    onClick = { selectedPath = folder.path },
                                    tag = "folder_chip_${folder.path.hashCode()}"
                                )
                            }
                        }
                        if (activeFolder != null) {
                            Spacer(modifier = Modifier.height(PhiTokens.Space13))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                VeloraPrimaryButton(
                                    text = if (isPersian) "پخش پوشه" else "Play Folder",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { onPlayFolder(activeFolder.tracks, false) },
                                    tag = "folder_play_button",
                                    modifier = Modifier.weight(1f)
                                )
                                if (tier != ExperienceTier.SIMPLE) {
                                    VeloraSecondaryButton(
                                        text = if (isPersian) "تصادفی" else "Shuffle",
                                        icon = Icons.Default.Shuffle,
                                        onClick = { onPlayFolder(activeFolder.tracks, true) },
                                        tag = "folder_shuffle_button",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                if (activeFolder != null) {
                    items(activeFolder.tracks, key = { it.id }) { track ->
                        TrackRowItem(
                            track = track,
                            isActive = uiState.engineState.currentTrack?.id == track.id,
                            playlists = uiState.playlists,
                            onPlay = { onPlayTrack(track, activeFolder.tracks) },
                            onAddToQueue = { onAddTrackToQueue(track) },
                            onAddToPlaylist = { pid -> onAddTrackToPlaylist(pid, track.id) },
                            onToggleFavorite = { onToggleFavorite(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTrackDetailColumn(
    folder: FolderGroup?,
    uiState: VeloraUiState,
    tier: ExperienceTier,
    isPersian: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayFolder: (List<Track>, Boolean) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = modifier
    ) {
        if (folder != null) {
            item {
                VeloraSectionCard(
                    eyebrow = if (isPersian) "پوشه انتخاب‌شده" else "SELECTED FOLDER",
                    title = "${folder.displayName} (${folder.tracks.size})",
                    subtitle = folder.path
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        VeloraPrimaryButton(
                            text = if (isPersian) "پخش پوشه" else "Play Folder",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onPlayFolder(folder.tracks, false) },
                            tag = "folder_play_button"
                        )
                        if (tier != ExperienceTier.SIMPLE) {
                            VeloraSecondaryButton(
                                text = if (isPersian) "تصادفی" else "Shuffle",
                                icon = Icons.Default.Shuffle,
                                onClick = { onPlayFolder(folder.tracks, true) },
                                tag = "folder_shuffle_button"
                            )
                        }
                    }
                }
            }
            items(folder.tracks, key = { it.id }) { track ->
                TrackRowItem(
                    track = track,
                    isActive = uiState.engineState.currentTrack?.id == track.id,
                    playlists = uiState.playlists,
                    onPlay = { onPlayTrack(track, folder.tracks) },
                    onAddToQueue = { onAddTrackToQueue(track) },
                    onAddToPlaylist = { pid -> onAddTrackToPlaylist(pid, track.id) },
                    onToggleFavorite = { onToggleFavorite(track) }
                )
            }
        }
    }
}
