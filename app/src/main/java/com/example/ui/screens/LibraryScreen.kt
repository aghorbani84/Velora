package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.domain.model.ExperienceTier
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.Playlist
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.Track
import com.example.domain.model.TrackFilterMode
import com.example.domain.model.TrackSortOrder
import com.example.ui.components.TrackRowItem
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraLoadingSkeleton
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState

@Composable
fun LibraryScreen(
    uiState: FazoraUiState,
    onScanLibrary: () -> Unit,
    onAddFolderUri: (String) -> Unit,
    onCreateCustomFolder: (String) -> Unit = {},
    onRemoveFolder: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (Playlist, Boolean) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit = {},
    onSelectSortOrder: (TrackSortOrder) -> Unit = {},
    onSelectFilterMode: (TrackFilterMode) -> Unit = {},
    onResumeHistory: (PlaybackHistoryEntry) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onPlaySmartPlaylist: (SmartPlaylist, Boolean) -> Unit = { _, _ -> },
    onAddCustomSmartRule: (String, Int, Int, Int, Boolean, String) -> Unit = { _, _, _, _, _, _ -> },
    onExportM3u8: (Long) -> String = { "" },
    onImportM3u8: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val tier = uiState.settings.experienceTier
    var folderInput by remember { mutableStateOf("") }
    var playlistNameInput by remember { mutableStateOf("") }
    var showSmartRuleDialog by remember { mutableStateOf(false) }
    var showM3u8Dialog by remember { mutableStateOf(false) }

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
    ) { onScanLibrary() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.toString()?.let(onAddFolderUri) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.toString()?.let(onAddFolderUri) }

    if (showSmartRuleDialog && tier == ExperienceTier.PRO) {
        SmartRuleBuilderDialog(
            isPersian = isPersian,
            onDismiss = { showSmartRuleDialog = false },
            onSaveRule = { name, br, sr, plays, favs, kw ->
                onAddCustomSmartRule(name, br, sr, plays, favs, kw)
                showSmartRuleDialog = false
            }
        )
    }

    if (showM3u8Dialog && tier != ExperienceTier.SIMPLE) {
        M3u8ManagerDialog(
            playlists = uiState.playlists,
            isPersian = isPersian,
            onDismiss = { showM3u8Dialog = false },
            onExportPlaylist = onExportM3u8,
            onImportPlaylist = { name, content ->
                onImportM3u8(name, content)
                showM3u8Dialog = false
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("library_screen")
    ) {
        item {
            LibraryDiscoveryCard(
                uiState = uiState,
                isPersian = isPersian,
                tier = tier,
                folderInput = folderInput,
                onFolderInputChange = { folderInput = it },
                onAutoScan = { permissionLauncher.launch(permissionsToRequest) },
                onManualScan = onScanLibrary,
                onPickFile = { filePickerLauncher.launch(arrayOf("audio/*", "video/*")) },
                onPickFolder = { folderPickerLauncher.launch(null) },
                onAddFolder = {
                    if (folderInput.isNotBlank()) {
                        onAddFolderUri(folderInput.trim())
                        folderInput = ""
                    }
                },
                onCreateCustomFolder = {
                    if (folderInput.isNotBlank()) {
                        onCreateCustomFolder(folderInput.trim())
                        folderInput = ""
                    }
                },
                onRemoveFolder = onRemoveFolder
            )
        }

        if (uiState.scanProgress.isScanning) {
            item {
                VeloraLoadingSkeleton(
                    rowCount = 3,
                    label = if (isPersian) "در حال اسکن کتابخانه رسانه..." else "Scanning media library..."
                )
            }
        }

        if (tier != ExperienceTier.SIMPLE) {
            item {
                PlaylistsSectionCard(
                    playlists = uiState.playlists,
                    isPersian = isPersian,
                    playlistNameInput = playlistNameInput,
                    onPlaylistNameChange = { playlistNameInput = it },
                    onCreatePlaylist = {
                        if (playlistNameInput.isNotBlank()) {
                            onCreatePlaylist(playlistNameInput.trim())
                            playlistNameInput = ""
                        }
                    },
                    onPlayPlaylist = onPlayPlaylist,
                    onDeletePlaylist = onDeletePlaylist,
                    onOpenM3u8 = { showM3u8Dialog = true }
                )
            }

            item {
                SmartPlaylistsAndAnalyticsSection(
                    uiState = uiState,
                    isPersian = isPersian,
                    tier = tier,
                    onPlaySmartPlaylist = onPlaySmartPlaylist,
                    onOpenSmartRuleBuilder = { showSmartRuleDialog = true }
                )
            }
        }

        item {
            FilterAndSortSection(
                totalTracks = uiState.allTracks.size,
                filterMode = uiState.filterMode,
                sortOrder = uiState.sortOrder,
                isPersian = isPersian,
                onSelectFilterMode = onSelectFilterMode,
                onSelectSortOrder = onSelectSortOrder
            )
        }

        if (uiState.allTracks.isEmpty()) {
            item {
                VeloraEmptyState(
                    icon = Icons.Default.LibraryMusic,
                    title = if (isPersian) "کتابخانه رسانه خالی است" else "Your Media Library Is Empty",
                    subtitle = if (isPersian) {
                        "برای یافتن خودکار آهنگ‌ها و ویدیوها، دکمه اسکن دستگاه را بزنید."
                    } else {
                        "Scan your device storage or import a media file to start playback."
                    },
                    actionLabel = if (isPersian) "اسکن حافظه دستگاه" else "Scan Device Media",
                    actionTag = "empty_state_scan_button",
                    onAction = { permissionLauncher.launch(permissionsToRequest) }
                )
            }
        } else {
            items(uiState.allTracks, key = { it.id }) { track ->
                TrackRowItem(
                    track = track,
                    isActive = uiState.engineState.currentTrack?.id == track.id,
                    playlists = uiState.playlists,
                    onPlay = { onPlayTrack(track) },
                    onAddToQueue = { onAddTrackToQueue(track) },
                    onAddToPlaylist = { pid -> onAddTrackToPlaylist(pid, track.id) },
                    onToggleFavorite = { onToggleFavorite(track) }
                )
            }
        }

        if (tier != ExperienceTier.SIMPLE) {
            item {
                PlaybackHistorySection(
                    history = uiState.playbackHistory,
                    isPersian = isPersian,
                    onResumeHistory = onResumeHistory,
                    onClearHistory = onClearHistory
                )
            }
        }
    }
}

@Composable
private fun LibraryDiscoveryCard(
    uiState: FazoraUiState,
    isPersian: Boolean,
    tier: ExperienceTier,
    folderInput: String,
    onFolderInputChange: (String) -> Unit,
    onAutoScan: () -> Unit,
    onManualScan: () -> Unit,
    onPickFile: () -> Unit,
    onPickFolder: () -> Unit,
    onAddFolder: () -> Unit,
    onCreateCustomFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    VeloraSectionCard(
        eyebrow = if (isPersian) "کتابخانه رسانه ولورا" else "VELORA MEDIA ENGINE",
        title = if (isPersian) "مدیریت و اسکن فایل‌ها (${uiState.allTracks.size})" else "Media Library (${uiState.allTracks.size})",
        subtitle = if (isPersian) {
            "${uiState.audioTracks.size} آهنگ • ${uiState.videoTracks.size} ویدیو • ${uiState.favoriteTracks.size} محبوب"
        } else {
            "${uiState.audioTracks.size} Audio • ${uiState.videoTracks.size} Video • ${uiState.favoriteTracks.size} Favorites"
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
            modifier = Modifier.fillMaxWidth()
        ) {
            VeloraPrimaryButton(
                text = if (isPersian) "اسکن خودکار حافظه" else "Auto-Scan Storage",
                icon = Icons.Default.Refresh,
                onClick = onAutoScan,
                tag = "library_auto_scan_button",
                modifier = Modifier.weight(PhiTokens.PRIMARY_WEIGHT)
            )
            VeloraSecondaryButton(
                text = if (isPersian) "بروزرسانی" else "Rescan",
                icon = Icons.Default.Refresh,
                onClick = onManualScan,
                tag = "scan_library_button",
                modifier = Modifier.weight(PhiTokens.SECONDARY_WEIGHT)
            )
        }
        Spacer(modifier = Modifier.height(PhiTokens.Space8))
        Row(
            horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
            modifier = Modifier.fillMaxWidth()
        ) {
            VeloraSecondaryButton(
                text = if (isPersian) "انتخاب فایل" else "Import File",
                icon = Icons.Default.AudioFile,
                onClick = onPickFile,
                tag = "import_device_file_button",
                modifier = Modifier.weight(1f)
            )
            VeloraSecondaryButton(
                text = if (isPersian) "انتخاب پوشه" else "Import Folder",
                icon = Icons.Default.FolderOpen,
                onClick = onPickFolder,
                tag = "import_device_folder_button",
                modifier = Modifier.weight(1f)
            )
        }

        if (tier == ExperienceTier.PRO) {
            Spacer(modifier = Modifier.height(PhiTokens.Space13))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = onFolderInputChange,
                    label = { Text(if (isPersian) "مسیر پوشه مجاز" else "Allowed Folder Path") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("folder_path_input")
                )
                VeloraPrimaryButton(
                    text = if (isPersian) "افزودن" else "Add",
                    icon = Icons.Default.Add,
                    onClick = onAddFolder,
                    tag = "add_folder_button"
                )
                IconButton(
                    onClick = onCreateCustomFolder,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("create_custom_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Create Sandbox Folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (uiState.settings.allowedFolderUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(PhiTokens.Space8))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(uiState.settings.allowedFolderUris.toList()) { folder ->
                        VeloraSecondaryButton(
                            text = folder.substringAfterLast('/'),
                            icon = Icons.Default.DeleteOutline,
                            onClick = { onRemoveFolder(folder) },
                            tag = "remove_folder_${folder.hashCode()}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsSectionCard(
    playlists: List<Playlist>,
    isPersian: Boolean,
    playlistNameInput: String,
    onPlaylistNameChange: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlayPlaylist: (Playlist, Boolean) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenM3u8: () -> Unit
) {
    VeloraSectionCard(
        eyebrow = if (isPersian) "پلی‌لیست‌های کاربر" else "PLAYLISTS & M3U8",
        title = if (isPersian) "پلی‌لیست‌ها (${playlists.size})" else "Playlists (${playlists.size})",
        headerAction = {
            VeloraSecondaryButton(
                text = "M3U8",
                onClick = onOpenM3u8,
                tag = "open_m3u8_dialog_button"
            )
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = playlistNameInput,
                onValueChange = onPlaylistNameChange,
                label = { Text(if (isPersian) "نام پلی‌لیست جدید" else "New Playlist Name") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("new_playlist_name_input")
            )
            VeloraPrimaryButton(
                text = if (isPersian) "ساخت" else "Create",
                icon = Icons.Default.Add,
                onClick = onCreatePlaylist,
                tag = "create_playlist_button"
            )
        }
        if (playlists.isNotEmpty()) {
            Spacer(modifier = Modifier.height(PhiTokens.Space13))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                items(playlists, key = { it.id }) { playlist ->
                    VeloraSectionCard(
                        title = playlist.name,
                        subtitle = "${playlist.trackCount} tracks",
                        modifier = Modifier
                            .width(PhiTokens.CardWidthCompact)
                            .testTag("playlist_card_${playlist.id}")
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            VeloraPrimaryButton(
                                text = if (isPersian) "پخش" else "Play",
                                icon = Icons.Default.PlayArrow,
                                onClick = { onPlayPlaylist(playlist, false) },
                                tag = "play_playlist_${playlist.id}",
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onPlayPlaylist(playlist, true) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("shuffle_playlist_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle Playlist",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onDeletePlaylist(playlist.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_playlist_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Playlist",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
