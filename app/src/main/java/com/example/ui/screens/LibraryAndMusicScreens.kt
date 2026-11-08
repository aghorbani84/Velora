package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.Playlist
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.Track
import com.example.domain.model.TrackFilterMode
import com.example.domain.model.TrackSortOrder
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraChampagneGold
import com.example.ui.viewmodel.FazoraUiState

@Composable
fun LibraryScreen(
    uiState: FazoraUiState,
    onScanLibrary: () -> Unit,
    onAddFolderUri: (String) -> Unit,
    onCreateCustomFolder: (String) -> Unit,
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
    onPlaySmartPlaylist: (SmartPlaylist, Boolean) -> Unit = { _, _ -> },
    onPlayNetworkStream: (String, String) -> Unit = { _, _ -> },
    onResumeHistory: (PlaybackHistoryEntry) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onAddCustomSmartRule: (String, Int, Int, Int, Boolean, String) -> Unit = { _, _, _, _, _, _ -> },
    onExportM3u8: (Long) -> String = { "" },
    onImportM3u8: (String, String) -> Unit = { _, _ -> },
    onPlayNext: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    var showNetworkStreamDialog by remember { mutableStateOf(false) }
    var streamUrlInput by remember { mutableStateOf("") }
    var streamTitleInput by remember { mutableStateOf("") }

    var showSmartRuleDialog by remember { mutableStateOf(false) }
    var showM3uManagerDialog by remember { mutableStateOf(false) }

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
        onScanLibrary()
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onAddFolderUri(uri.toString())
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("library_screen")
    ) {
        item {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PhiTokens.Space21)
                ) {
                    Text(
                        text = stringResource(R.string.library_header_badge),
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.headlineLarge,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "${uiState.allTracks.size} indexed tracks " +
                            "(${uiState.audioTracks.size} Audio • ${uiState.videoTracks.size} Video • " +
                            "${uiState.favoriteTracks.size} Favorites) • " +
                            "Scan time: ${uiState.scanProgress.lastScanDurationMs}ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )

                    if (uiState.scanProgress.isScanning) {
                        Spacer(modifier = Modifier.height(PhiTokens.Space13))
                        LinearProgressIndicator(
                            color = FazoraGold,
                            trackColor = FazoraCarbonElevated,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Scanning: ${uiState.scanProgress.currentSourceLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = FazoraGold
                        )
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { permissionLauncher.launch(permissionsToRequest) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FazoraGold,
                                contentColor = FazoraObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("scan_library_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text("Scan Media")
                        }

                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pick_saf_folder_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text("SAF Folder", color = FazoraIvory)
                        }

                        OutlinedButton(
                            onClick = { showCreateFolderDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("create_folder_button")
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text("+ Folder", color = FazoraIvory)
                        }

                        OutlinedButton(
                            onClick = { showNetworkStreamDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_stream_dialog_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VeloraChampagneGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stream", color = FazoraIvory)
                        }
                    }

                    if (uiState.settings.allowedFolderUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(PhiTokens.Space13))
                        Text(
                            text = "Allowlisted Media Folders (${uiState.settings.allowedFolderUris.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = FazoraGold
                        )
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        uiState.settings.allowedFolderUris.forEach { folder ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = folder,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FazoraIvory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onRemoveFolder(folder) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("remove_folder_${folder.hashCode()}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove folder $folder",
                                        tint = FazoraSteel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            ListeningStatsRecapCard(
                stats = uiState.listeningStats,
                isPersian = uiState.settings.appLanguageCode == "fa",
                onOpenRuleBuilder = { showSmartRuleDialog = true },
                onOpenM3uManager = { showM3uManagerDialog = true }
            )
        }

        if (uiState.smartPlaylists.isNotEmpty()) {
            item {
                SmartPlaylistsSection(
                    smartPlaylists = uiState.smartPlaylists,
                    isPersian = uiState.settings.appLanguageCode == "fa",
                    onPlaySmartPlaylist = onPlaySmartPlaylist
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Playlists (${uiState.playlists.size})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    OutlinedButton(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("create_playlist_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = FazoraGold)
                        Spacer(modifier = Modifier.width(PhiTokens.Space8))
                        Text("New Playlist", color = FazoraIvory)
                    }
                }

                Spacer(modifier = Modifier.height(PhiTokens.Space13))

                if (uiState.playlists.isEmpty()) {
                    Surface(
                        color = FazoraCarbon,
                        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
                    ) {
                        Text(
                            text = "No playlists yet. Tap 'New Playlist' to create a persistent Room playlist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FazoraSteel,
                            modifier = Modifier.padding(PhiTokens.Space13)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)
                    ) {
                        items(uiState.playlists, key = { it.id }) { playlist ->
                            Surface(
                                color = FazoraCarbon,
                                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                                modifier = Modifier
                                    .width(240.dp)
                                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
                                    .testTag("playlist_card_${playlist.id}")
                            ) {
                                Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = FazoraIvory,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onDeletePlaylist(playlist.id) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("delete_playlist_${playlist.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete ${playlist.name}",
                                                tint = FazoraSteel
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${playlist.trackCount} tracks • ${Track.formatDurationMs(playlist.totalDurationMs)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FazoraSteel
                                    )
                                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                                        Button(
                                            onClick = { onPlayPlaylist(playlist, false) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = FazoraGold,
                                                contentColor = FazoraObsidian
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .testTag("play_playlist_${playlist.id}")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Text("Play")
                                        }
                                        OutlinedButton(
                                            onClick = { onPlayPlaylist(playlist, true) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("shuffle_playlist_${playlist.id}")
                                        ) {
                                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle playlist", tint = FazoraGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            PlaybackHistorySection(
                history = uiState.playbackHistory,
                onResumeHistory = onResumeHistory,
                onClearHistory = onClearHistory
            )
        }

        item {
            LibrarySortAndFilterBar(
                sortOrder = uiState.sortOrder,
                filterMode = uiState.filterMode,
                isPersian = uiState.settings.appLanguageCode == "fa",
                onSelectSortOrder = onSelectSortOrder,
                onSelectFilterMode = onSelectFilterMode
            )
        }

        item {
            Text(
                text = "All Indexed Media (${uiState.allTracks.size})",
                style = MaterialTheme.typography.headlineMedium,
                color = FazoraIvory
            )
        }

        items(uiState.allTracks, key = { it.id }) { track ->
            TrackRowItem(
                track = track,
                isActive = uiState.engineState.currentTrack?.id == track.id,
                playlists = uiState.playlists,
                onPlay = { onPlayTrack(track) },
                onAddToQueue = { onAddTrackToQueue(track) },
                onPlayNext = { onPlayNext(track) },
                onAddToPlaylist = { playlistId -> onAddTrackToPlaylist(playlistId, track.id) },
                onToggleFavorite = { onToggleFavorite(track) }
            )
        }
    }

    if (showSmartRuleDialog) {
        SmartRuleBuilderDialog(
            isPersian = uiState.settings.appLanguageCode == "fa",
            onDismiss = { showSmartRuleDialog = false },
            onCreateRule = onAddCustomSmartRule
        )
    }

    if (showM3uManagerDialog) {
        M3u8ManagerDialog(
            playlists = uiState.playlists,
            isPersian = uiState.settings.appLanguageCode == "fa",
            onDismiss = { showM3uManagerDialog = false },
            onExportM3u8 = onExportM3u8,
            onImportM3u8 = onImportM3u8
        )
    }

    if (showNetworkStreamDialog) {
        AlertDialog(
            onDismissRequest = { showNetworkStreamDialog = false },
            containerColor = FazoraCarbonElevated,
            title = { Text("Authorized Network Stream (HLS / DASH / HTTP)", color = FazoraIvory) },
            text = {
                Column {
                    Text(
                        "Enter a direct HLS (.m3u8), MPEG-DASH (.mpd), or Progressive HTTP/HTTPS audio/video URL.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    OutlinedTextField(
                        value = streamTitleInput,
                        onValueChange = { streamTitleInput = it },
                        label = { Text("Stream Title (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_title_input")
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    OutlinedTextField(
                        value = streamUrlInput,
                        onValueChange = { streamUrlInput = it },
                        label = { Text("https://.../stream.m3u8 or .mp3") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stream_url_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (streamUrlInput.isNotBlank()) {
                            onPlayNetworkStream(streamUrlInput.trim(), streamTitleInput.trim())
                            streamUrlInput = ""
                            streamTitleInput = ""
                            showNetworkStreamDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian),
                    modifier = Modifier.testTag("confirm_play_stream_button")
                ) {
                    Text("Launch Stream")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNetworkStreamDialog = false }) {
                    Text("Cancel", color = FazoraSteel)
                }
            }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = FazoraCarbonElevated,
            title = { Text("Add Acoustic Media Folder", color = FazoraIvory) },
            text = {
                Column {
                    Text(
                        "Enter a studio folder name or safe local path. Unsafe roots (/home, .ssh, /tmp, C:/) are strictly rejected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name or Safe Path") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newFolderName.trim()
                        if (trimmed.startsWith("/")) {
                            onAddFolderUri(trimmed)
                        } else {
                            onCreateCustomFolder(trimmed.ifEmpty { "Acoustic Vault" })
                        }
                        newFolderName = ""
                        showCreateFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian),
                    modifier = Modifier.testTag("confirm_create_folder_button")
                ) {
                    Text("Add & Scan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel", color = FazoraSteel)
                }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = FazoraCarbonElevated,
            title = { Text("Create Playlist", color = FazoraIvory) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian),
                    modifier = Modifier.testTag("confirm_create_playlist_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = FazoraSteel)
                }
            }
        )
    }
}

@Composable
private fun SmartPlaylistsSection(
    smartPlaylists: List<SmartPlaylist>,
    isPersian: Boolean,
    onPlaySmartPlaylist: (SmartPlaylist, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_playlists_section")
    ) {
        Text(
            text = if (isPersian) "ساخته‌شده برای شما (هوش محلی VELORA)" else "MADE FOR YOU • LOCAL INTELLIGENCE",
            style = MaterialTheme.typography.labelLarge,
            color = FazoraGold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isPersian) "پلی‌لیست‌های هوشمند و پویا" else "Smart Personalization Mixes (${smartPlaylists.size})",
            style = MaterialTheme.typography.headlineMedium,
            color = FazoraIvory
        )
        Spacer(modifier = Modifier.height(PhiTokens.Space13))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
            items(smartPlaylists, key = { it.category.id }) { smart ->
                Surface(
                    color = FazoraCarbon,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                    modifier = Modifier
                        .width(265.dp)
                        .border(1.dp, VeloraChampagneGold.copy(alpha = 0.45f), RoundedCornerShape(PhiTokens.CornerRadius13))
                        .testTag("smart_playlist_card_${smart.category.id}")
                ) {
                    Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                        Text(
                            text = if (isPersian) smart.category.titleFa else smart.category.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            color = FazoraGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPersian) smart.category.subtitleFa else smart.category.subtitleEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = FazoraIvory,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${smart.tracks.size} tracks • ${if (isPersian) smart.explainReasonFa else smart.explainReasonEn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VeloraChampagneGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            Button(
                                onClick = { onPlaySmartPlaylist(smart, false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FazoraGold,
                                    contentColor = FazoraObsidian
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("play_smart_playlist_${smart.category.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Mix")
                            }
                            OutlinedButton(
                                onClick = { onPlaySmartPlaylist(smart, true) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("shuffle_smart_playlist_${smart.category.id}")
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle smart mix", tint = FazoraGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySortAndFilterBar(
    sortOrder: TrackSortOrder,
    filterMode: TrackFilterMode,
    isPersian: Boolean,
    onSelectSortOrder: (TrackSortOrder) -> Unit,
    onSelectFilterMode: (TrackFilterMode) -> Unit
) {
    Surface(
        color = FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
            .testTag("library_sort_filter_bar")
    ) {
        Column(modifier = Modifier.padding(PhiTokens.Space13)) {
            Text(
                text = if (isPersian) "فیلتر و مرتب‌سازی کتابخانه" else "FILTER & SORT COLLECTION",
                style = MaterialTheme.typography.labelLarge,
                color = FazoraGold
            )
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(TrackFilterMode.entries) { mode ->
                    val selected = filterMode == mode
                    OutlinedButton(
                        onClick = { onSelectFilterMode(mode) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) FazoraGold else FazoraCarbonElevated,
                            contentColor = if (selected) FazoraObsidian else FazoraIvory
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("filter_mode_${mode.name}")
                    ) {
                        Text(if (isPersian) mode.labelFa else mode.labelEn)
                    }
                }
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(TrackSortOrder.entries) { order ->
                    val selected = sortOrder == order
                    OutlinedButton(
                        onClick = { onSelectSortOrder(order) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) FazoraGoldMuted else FazoraCarbonElevated,
                            contentColor = if (selected) FazoraGold else FazoraSteel
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sort_order_${order.name}")
                    ) {
                        Text(if (isPersian) order.labelFa else order.labelEn)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackHistorySection(
    history: List<PlaybackHistoryEntry>,
    onResumeHistory: (PlaybackHistoryEntry) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("playback_history_section")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Offline Playback History (${history.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FazoraIvory
                )
                Text(
                    text = "Room-cached audio metadata & resume positions",
                    style = MaterialTheme.typography.labelMedium,
                    color = FazoraSteel
                )
            }
            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearHistory,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("clear_playback_history_button")
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = FazoraGold)
                    Spacer(modifier = Modifier.width(PhiTokens.Space8))
                    Text("Clear", color = FazoraIvory)
                }
            }
        }

        Spacer(modifier = Modifier.height(PhiTokens.Space13))

        if (history.isEmpty()) {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
            ) {
                Text(
                    text = "No offline playback history recorded yet. Play any track to cache its metadata and resume position in Room.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FazoraSteel,
                    modifier = Modifier.padding(PhiTokens.Space13)
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                items(history, key = { it.track.id }) { entry ->
                    Surface(
                        color = FazoraCarbon,
                        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                        modifier = Modifier
                            .width(260.dp)
                            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
                            .testTag("history_card_${entry.track.id}")
                    ) {
                        Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                            Text(
                                text = entry.track.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = FazoraIvory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${entry.track.artist} • ${entry.track.album}",
                                style = MaterialTheme.typography.labelMedium,
                                color = FazoraSteel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${entry.track.sampleRateHz / 1000.0}kHz • ${entry.track.bitrateKbps}kbps • " +
                                    "Plays: ${entry.playCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FazoraGold
                            )
                            Spacer(modifier = Modifier.height(PhiTokens.Space8))
                            Button(
                                onClick = { onResumeHistory(entry) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FazoraGold,
                                    contentColor = FazoraObsidian
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("resume_history_${entry.track.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                                val resumeLabel = if (entry.lastPositionMs > 0L) {
                                    "Resume ${Track.formatDurationMs(entry.lastPositionMs)}"
                                } else {
                                    "Play Cached"
                                }
                                Text(resumeLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicScreen(
    uiState: FazoraUiState,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit = {},
    onAutoScanMedia: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
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
        onAutoScanMedia()
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("music_screen")
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
                        text = "VELORA HIGH-RESOLUTION AUDIO",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "${if (isPersian) "مجموعه آهنگ‌ها" else "Music Collection"} (${uiState.audioTracks.size})",
                        style = MaterialTheme.typography.headlineLarge,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FazoraGold,
                                contentColor = FazoraObsidian
                            ),
                            modifier = Modifier
                                .weight(PhiTokens.PRIMARY_WEIGHT)
                                .minimumInteractiveComponentSize()
                                .testTag("music_play_all_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(if (isPersian) "پخش همه (${uiState.audioTracks.size})" else "Play All (${uiState.audioTracks.size})")
                        }

                        OutlinedButton(
                            onClick = onShuffleAll,
                            modifier = Modifier
                                .weight(PhiTokens.SECONDARY_WEIGHT)
                                .minimumInteractiveComponentSize()
                                .testTag("music_shuffle_all_button")
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = FazoraGold)
                            Spacer(modifier = Modifier.width(PhiTokens.Space8))
                            Text(if (isPersian) "تصادفی" else "Shuffle", color = FazoraIvory)
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    OutlinedButton(
                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("music_auto_scan_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = FazoraGold)
                        Spacer(modifier = Modifier.width(PhiTokens.Space8))
                        Text(
                            if (isPersian) "🔄 جست‌وجو و اسکن خودکار آهنگ‌های حافظه" else "🔄 Auto-Scan Device Music Storage",
                            color = FazoraIvory
                        )
                    }
                }
            }
        }

        items(uiState.audioTracks, key = { it.id }) { track ->
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

@Composable
fun ArtistScreen(
    uiState: FazoraUiState,
    onSelectArtist: (String) -> Unit,
    onPlayArtist: (String, Boolean) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val artists = uiState.artistsMap.keys.toList()
    val activeArtist = uiState.selectedArtist ?: artists.firstOrNull()
    val artistTracks = if (activeArtist != null) uiState.artistsMap[activeArtist].orEmpty() else emptyList()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .padding(PhiTokens.Space21)
            .testTag("artist_screen")
    ) {
        val isWide = maxWidth >= 600.dp
        if (isWide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
                modifier = Modifier.fillMaxSize()
            ) {
                ArtistSelectorColumn(
                    artists = artists,
                    activeArtist = activeArtist,
                    artistsMap = uiState.artistsMap,
                    onSelectArtist = onSelectArtist,
                    modifier = Modifier
                        .weight(PhiTokens.SECONDARY_WEIGHT)
                        .fillMaxHeight()
                )
                ArtistDetailColumn(
                    activeArtist = activeArtist,
                    artistTracks = artistTracks,
                    uiState = uiState,
                    onPlayArtist = onPlayArtist,
                    onPlayTrack = onPlayTrack,
                    onAddTrackToQueue = onAddTrackToQueue,
                    onAddTrackToPlaylist = onAddTrackToPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier
                        .weight(PhiTokens.PRIMARY_WEIGHT)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
                modifier = Modifier.fillMaxSize()
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(artists) { artist ->
                        val isSelected = artist == activeArtist
                        Surface(
                            color = if (isSelected) FazoraGold else FazoraCarbon,
                            shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
                                .clickable { onSelectArtist(artist) }
                                .testTag("artist_chip_${artist.hashCode()}")
                        ) {
                            Text(
                                text = "$artist (${uiState.artistsMap[artist]?.size ?: 0})",
                                color = if (isSelected) FazoraObsidian else FazoraIvory,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                            )
                        }
                    }
                }

                ArtistDetailColumn(
                    activeArtist = activeArtist,
                    artistTracks = artistTracks,
                    uiState = uiState,
                    onPlayArtist = onPlayArtist,
                    onPlayTrack = onPlayTrack,
                    onAddTrackToQueue = onAddTrackToQueue,
                    onAddTrackToPlaylist = onAddTrackToPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ArtistSelectorColumn(
    artists: List<String>,
    activeArtist: String?,
    artistsMap: Map<String, List<Track>>,
    onSelectArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
        modifier = modifier
    ) {
        item {
            Text(
                text = "ARTISTS (${artists.size})",
                style = MaterialTheme.typography.labelLarge,
                color = FazoraGold
            )
        }
        items(artists) { artist ->
            val isSelected = artist == activeArtist
            Surface(
                color = if (isSelected) FazoraGoldMuted else FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSelected) FazoraGold else FazoraCarbonBorder,
                        RoundedCornerShape(PhiTokens.CornerRadius13)
                    )
                    .clickable { onSelectArtist(artist) }
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = FazoraIvory
                    )
                    Text(
                        text = "${artistsMap[artist]?.size ?: 0} tracks",
                        style = MaterialTheme.typography.labelMedium,
                        color = FazoraSteel
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistDetailColumn(
    activeArtist: String?,
    artistTracks: List<Track>,
    uiState: FazoraUiState,
    onPlayArtist: (String, Boolean) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = modifier
    ) {
        if (activeArtist != null) {
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
                            text = "ARTIST DISCOGRAPHY",
                            style = MaterialTheme.typography.labelLarge,
                            color = FazoraGold
                        )
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        Text(
                            text = activeArtist,
                            style = MaterialTheme.typography.headlineLarge,
                            color = FazoraIvory
                        )
                        Spacer(modifier = Modifier.height(PhiTokens.Space13))
                        Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                            Button(
                                onClick = { onPlayArtist(activeArtist, false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FazoraGold,
                                    contentColor = FazoraObsidian
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("artist_play_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                                Text("Play Artist")
                            }
                            OutlinedButton(
                                onClick = { onPlayArtist(activeArtist, true) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("artist_shuffle_button")
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, tint = FazoraGold)
                                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                                Text("Shuffle Artist", color = FazoraIvory)
                            }
                        }
                    }
                }
            }

            items(artistTracks, key = { it.id }) { track ->
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
