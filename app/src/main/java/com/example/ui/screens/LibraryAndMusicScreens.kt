package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.FazoraGoldBright
import com.example.ui.theme.FazoraGoldDeep
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSoftGold
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
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showNetworkStreamDialog by remember { mutableStateOf(false) }
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

    val isPersian = uiState.settings.appLanguageCode == "fa"

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("library_screen")
    ) {
        item {
            LibraryStudioHeroCard(
                uiState = uiState,
                isPersian = isPersian,
                onTriggerScan = { permissionLauncher.launch(permissionsToRequest) },
                onPickSafFolder = { folderPickerLauncher.launch(null) },
                onOpenCreateFolder = { showCreateFolderDialog = true },
                onOpenNetworkStream = { showNetworkStreamDialog = true },
                onRemoveFolder = onRemoveFolder
            )
        }

        item {
            ListeningStatsRecapCard(
                stats = uiState.listeningStats,
                isPersian = isPersian,
                onOpenRuleBuilder = { showSmartRuleDialog = true },
                onOpenM3uManager = { showM3uManagerDialog = true }
            )
        }

        if (uiState.smartPlaylists.isNotEmpty()) {
            item {
                SmartPlaylistsSection(
                    smartPlaylists = uiState.smartPlaylists,
                    isPersian = isPersian,
                    onPlaySmartPlaylist = onPlaySmartPlaylist
                )
            }
        }

        item {
            LibraryPlaylistsSection(
                playlists = uiState.playlists,
                isPersian = isPersian,
                onOpenCreatePlaylist = { showCreatePlaylistDialog = true },
                onDeletePlaylist = onDeletePlaylist,
                onPlayPlaylist = onPlayPlaylist
            )
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
                isPersian = isPersian,
                onSelectSortOrder = onSelectSortOrder,
                onSelectFilterMode = onSelectFilterMode
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPersian) {
                        "فهرست جامع رسانه‌ها (${uiState.allTracks.size})"
                    } else {
                        "All Indexed Media (${uiState.allTracks.size})"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = FazoraIvory
                )
                Surface(
                    color = FazoraGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPersian) "آماده پخش" else "64-BIT READY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FazoraGoldBright,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
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
            isPersian = isPersian,
            onDismiss = { showSmartRuleDialog = false },
            onCreateRule = onAddCustomSmartRule
        )
    }

    if (showM3uManagerDialog) {
        M3u8ManagerDialog(
            playlists = uiState.playlists,
            isPersian = isPersian,
            onDismiss = { showM3uManagerDialog = false },
            onExportM3u8 = onExportM3u8,
            onImportM3u8 = onImportM3u8
        )
    }

    if (showNetworkStreamDialog) {
        NetworkStreamDialog(
            onDismiss = { showNetworkStreamDialog = false },
            onPlayNetworkStream = onPlayNetworkStream
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onAddFolderUri = onAddFolderUri,
            onCreateCustomFolder = onCreateCustomFolder
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreatePlaylist = onCreatePlaylist
        )
    }
}

@Composable
private fun LibraryStudioHeroCard(
    uiState: FazoraUiState,
    isPersian: Boolean,
    onTriggerScan: () -> Unit,
    onPickSafFolder: () -> Unit,
    onOpenCreateFolder: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        FazoraGold.copy(alpha = 0.55f),
                        FazoraCarbonBorder,
                        FazoraGoldDeep.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(PhiTokens.CornerRadius21)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            FazoraCarbonElevated,
                            FazoraCarbon
                        )
                    )
                )
                .padding(PhiTokens.Space21)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.library_header_badge),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FazoraGoldBright
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = FazoraIvory
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FazoraGold.copy(alpha = 0.16f))
                        .border(1.dp, FazoraGold.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = FazoraGoldBright,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            // 4-Metric Studio Telemetry Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VaultMetricTile(
                    value = "${uiState.audioTracks.size}",
                    label = if (isPersian) "ترک صوتی" else "AUDIO",
                    accentColor = FazoraGoldBright,
                    modifier = Modifier.weight(1f)
                )
                VaultMetricTile(
                    value = "${uiState.videoTracks.size}",
                    label = if (isPersian) "ویدیو" else "VIDEO",
                    accentColor = VeloraChampagneGold,
                    modifier = Modifier.weight(1f)
                )
                VaultMetricTile(
                    value = "${uiState.favoriteTracks.size}",
                    label = if (isPersian) "محبوب‌ها" else "FAVORITES",
                    accentColor = FazoraGold,
                    modifier = Modifier.weight(1f)
                )
                VaultMetricTile(
                    value = "${uiState.scanProgress.lastScanDurationMs}ms",
                    label = if (isPersian) "سرعت اسکن" else "INDEX TIME",
                    accentColor = FazoraSoftGold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.scanProgress.isScanning) {
                Spacer(modifier = Modifier.height(PhiTokens.Space13))
                LinearProgressIndicator(
                    color = FazoraGoldBright,
                    trackColor = FazoraObsidian,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Scanning: ${uiState.scanProgress.currentSourceLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = FazoraGoldBright
                )
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onTriggerScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FazoraGold,
                        contentColor = FazoraObsidian
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.25f)
                        .minimumInteractiveComponentSize()
                        .testTag("scan_library_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "اسکن رسانه" else "Scan Media",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onPickSafFolder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("pick_saf_folder_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = FazoraGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAF", color = FazoraIvory)
                }

                OutlinedButton(
                    onClick = onOpenCreateFolder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("create_folder_button")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = FazoraGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+Folder", color = FazoraIvory)
                }

                OutlinedButton(
                    onClick = onOpenNetworkStream,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_stream_dialog_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VeloraChampagneGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(2.dp))
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
                            .padding(vertical = 3.dp)
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

@Composable
private fun VaultMetricTile(
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FazoraObsidian.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, FazoraCarbonBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FazoraSteel,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LibraryPlaylistsSection(
    playlists: List<Playlist>,
    isPersian: Boolean,
    onOpenCreatePlaylist: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (Playlist, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = if (isPersian) "مجموعه‌های شخصی" else "CURATED COLLECTIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = FazoraGold
                )
                Text(
                    text = "Playlists (${playlists.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = FazoraIvory
                )
            }
            OutlinedButton(
                onClick = onOpenCreatePlaylist,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("create_playlist_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = FazoraGold)
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Text(if (isPersian) "پلی‌لیست جدید" else "New Playlist", color = FazoraIvory)
            }
        }

        Spacer(modifier = Modifier.height(PhiTokens.Space13))

        if (playlists.isEmpty()) {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
            ) {
                Text(
                    text = if (isPersian) {
                        "هنوز پلی‌لیستی نساخته‌اید. روی «پلی‌لیست جدید» بزنید."
                    } else {
                        "No playlists yet. Tap 'New Playlist' to create a persistent Room playlist."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = FazoraSteel,
                    modifier = Modifier.padding(PhiTokens.Space13)
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                items(playlists, key = { it.id }) { playlist ->
                    Surface(
                        color = FazoraCarbon,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(250.dp)
                            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(16.dp))
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
                                    fontWeight = FontWeight.Bold,
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
                                color = VeloraChampagneGold
                            )
                            Spacer(modifier = Modifier.height(PhiTokens.Space8))
                            Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                                Button(
                                    onClick = { onPlayPlaylist(playlist, false) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FazoraGold,
                                        contentColor = FazoraObsidian
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("play_playlist_${playlist.id}")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Text(if (isPersian) "پخش" else "Play", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onPlayPlaylist(playlist, true) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("shuffle_playlist_${playlist.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Shuffle,
                                        contentDescription = "Shuffle playlist",
                                        tint = FazoraGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkStreamDialog(
    onDismiss: () -> Unit,
    onPlayNetworkStream: (String, String) -> Unit
) {
    var streamUrlInput by remember { mutableStateOf("") }
    var streamTitleInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier.testTag("confirm_play_stream_button")
            ) {
                Text("Launch Stream")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FazoraSteel)
            }
        }
    )
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onAddFolderUri: (String) -> Unit,
    onCreateCustomFolder: (String) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier.testTag("confirm_create_folder_button")
            ) {
                Text("Add & Scan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FazoraSteel)
            }
        }
    )
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FazoraGold,
                    contentColor = FazoraObsidian
                ),
                modifier = Modifier.testTag("confirm_create_playlist_button")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FazoraSteel)
            }
        }
    )
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
            text = if (isPersian) "ساخته‌شده برای شما (هوش بیزین و ابینگهاوس)" else "MADE FOR YOU • BAYESIAN ACOUSTIC RANKING",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = FazoraGold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isPersian) "میکس‌های هوشمند و پویا" else "Smart Personalization Mixes (${smartPlaylists.size})",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FazoraIvory
        )
        Spacer(modifier = Modifier.height(PhiTokens.Space13))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
            items(smartPlaylists, key = { it.category.id }) { smart ->
                Surface(
                    color = FazoraCarbon,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(270.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    FazoraGold.copy(alpha = 0.6f),
                                    FazoraCarbonBorder
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("smart_playlist_card_${smart.category.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(FazoraCarbonElevated, FazoraCarbon)
                                )
                            )
                            .padding(PhiTokens.Space13)
                    ) {
                        Text(
                            text = if (isPersian) smart.category.titleFa else smart.category.titleEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = FazoraGoldBright,
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
                        Spacer(modifier = Modifier.height(6.dp))
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("play_smart_playlist_${smart.category.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Mix", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onPlaySmartPlaylist(smart, true) },
                                shape = RoundedCornerShape(10.dp),
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
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(16.dp))
            .testTag("library_sort_filter_bar")
    ) {
        Column(modifier = Modifier.padding(PhiTokens.Space13)) {
            Text(
                text = if (isPersian) "فیلتر و مرتب‌سازی استودیویی" else "FILTER & SORT COLLECTION",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = FazoraGold
            )
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(TrackFilterMode.entries) { mode ->
                    val selected = filterMode == mode
                    OutlinedButton(
                        onClick = { onSelectFilterMode(mode) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) FazoraGold else FazoraCarbonElevated,
                            contentColor = if (selected) FazoraObsidian else FazoraIvory
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("filter_mode_${mode.name}")
                    ) {
                        Text(
                            text = if (isPersian) mode.labelFa else mode.labelEn,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(TrackSortOrder.entries) { order ->
                    val selected = sortOrder == order
                    OutlinedButton(
                        onClick = { onSelectSortOrder(order) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) FazoraGoldMuted else FazoraCarbonElevated,
                            contentColor = if (selected) FazoraGoldBright else FazoraSteel
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
                    text = "MEDIA TIME MACHINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = FazoraGold
                )
                Text(
                    text = "Offline Playback History (${history.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = FazoraIvory
                )
            }
            if (history.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearHistory,
                    shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(260.dp)
                            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(16.dp))
                            .testTag("history_card_${entry.track.id}")
                    ) {
                        Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                            Text(
                                text = entry.track.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
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
                                color = VeloraChampagneGold
                            )
                            Spacer(modifier = Modifier.height(PhiTokens.Space8))
                            Button(
                                onClick = { onResumeHistory(entry) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FazoraGold,
                                    contentColor = FazoraObsidian
                                ),
                                shape = RoundedCornerShape(10.dp),
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
                                Text(resumeLabel, fontWeight = FontWeight.Bold)
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
    var quickFilterQuery by remember { mutableStateOf("") }
    var showOnlyHiRes by remember { mutableStateOf(false) }

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

    val hiResCount = remember(uiState.audioTracks) {
        uiState.audioTracks.count { it.sampleRateHz >= 48_000 }
    }

    val displayedTracks = remember(uiState.audioTracks, quickFilterQuery, showOnlyHiRes) {
        uiState.audioTracks.filter { track ->
            val matchesHiRes = !showOnlyHiRes || track.sampleRateHz >= 48_000
            val matchesQuery = quickFilterQuery.isBlank() ||
                track.title.contains(quickFilterQuery, ignoreCase = true) ||
                track.artist.contains(quickFilterQuery, ignoreCase = true) ||
                track.album.contains(quickFilterQuery, ignoreCase = true)
            matchesHiRes && matchesQuery
        }
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
            MusicAcousticStageHeroCard(
                totalCount = uiState.audioTracks.size,
                hiResCount = hiResCount,
                favoritesCount = uiState.favoriteTracks.size,
                isPersian = isPersian,
                quickFilterQuery = quickFilterQuery,
                showOnlyHiRes = showOnlyHiRes,
                onQueryChange = { quickFilterQuery = it },
                onToggleHiRes = { showOnlyHiRes = !showOnlyHiRes },
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll,
                onAutoScan = { permissionLauncher.launch(permissionsToRequest) }
            )
        }

        items(displayedTracks, key = { it.id }) { track ->
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
private fun MusicAcousticStageHeroCard(
    totalCount: Int,
    hiResCount: Int,
    favoritesCount: Int,
    isPersian: Boolean,
    quickFilterQuery: String,
    showOnlyHiRes: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleHiRes: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onAutoScan: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        FazoraGold.copy(alpha = 0.6f),
                        FazoraCarbonBorder,
                        FazoraGoldDeep.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(PhiTokens.CornerRadius21)
            )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FazoraCarbonElevated, FazoraCarbon)
                    )
                )
                .padding(PhiTokens.Space21)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPersian) "موتور صوتی با وضوح بالا" else "VELORA HIGH-RESOLUTION STAGE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FazoraGoldBright
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (isPersian) "مجموعه آهنگ‌ها" else "Music Collection"} ($totalCount)",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) {
                            "$hiResCount ترک استودیویی Hi-Res • $favoritesCount محبوب"
                        } else {
                            "$hiResCount Studio Hi-Res (≥48kHz) • $favoritesCount Favorites"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraChampagneGold
                    )
                }

                // Sculpted Vinyl & Soundwave Crest
                Canvas(modifier = Modifier.size(58.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(FazoraGold.copy(alpha = 0.35f), FazoraObsidian),
                            center = c,
                            radius = size.minDimension / 2f
                        ),
                        radius = size.minDimension / 2f,
                        center = c
                    )
                    drawCircle(
                        color = FazoraGold.copy(alpha = 0.65f),
                        radius = size.minDimension * 0.45f,
                        center = c,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = VeloraChampagneGold.copy(alpha = 0.35f),
                        radius = size.minDimension * 0.31f,
                        center = c,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = FazoraGoldBright,
                        radius = size.minDimension * 0.14f,
                        center = c
                    )
                }
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            // Instant Filter Bar + Hi-Res Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = quickFilterQuery,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            if (isPersian) "فیلتر سریع نام آهنگ یا خواننده..." else "Quick filter tracks or artists...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = FazoraGold)
                    },
                    trailingIcon = {
                        if (quickFilterQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = FazoraSteel)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onToggleHiRes,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (showOnlyHiRes) FazoraGold else FazoraCarbon,
                        contentColor = if (showOnlyHiRes) FazoraObsidian else FazoraGoldBright
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("HI-RES", fontWeight = FontWeight.Bold)
                }
            }

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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(PhiTokens.PRIMARY_WEIGHT)
                        .minimumInteractiveComponentSize()
                        .testTag("music_play_all_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) "پخش همه ($totalCount)" else "Play All ($totalCount)",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onShuffleAll,
                    shape = RoundedCornerShape(12.dp),
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

            Spacer(modifier = Modifier.height(PhiTokens.Space8))

            OutlinedButton(
                onClick = onAutoScan,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("music_auto_scan_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = FazoraGold)
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Text(
                    if (isPersian) "🔄 اسکن خودکار آهنگ‌های حافظه دستگاه" else "🔄 Auto-Scan Device Music Storage",
                    color = FazoraIvory
                )
            }
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) FazoraGoldBright else FazoraCarbonBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectArtist(artist) }
                                .testTag("artist_chip_${artist.hashCode()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = if (isSelected) FazoraObsidian else FazoraGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$artist (${uiState.artistsMap[artist]?.size ?: 0})",
                                    color = if (isSelected) FazoraObsidian else FazoraIvory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
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
                fontWeight = FontWeight.Bold,
                color = FazoraGold
            )
        }
        items(artists) { artist ->
            val isSelected = artist == activeArtist
            Surface(
                color = if (isSelected) FazoraGoldMuted else FazoraCarbon,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSelected) FazoraGold else FazoraCarbonBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectArtist(artist) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(PhiTokens.Space13)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) FazoraGold else FazoraCarbonElevated)
                    ) {
                        Text(
                            text = artist.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) FazoraObsidian else FazoraGold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
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
                    color = Color.Transparent,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FazoraGold.copy(alpha = 0.45f), RoundedCornerShape(PhiTokens.CornerRadius21))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(FazoraCarbonElevated, FazoraCarbon)
                                )
                            )
                            .padding(PhiTokens.Space21)
                    ) {
                        Text(
                            text = "ARTIST DISCOGRAPHY STAGE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FazoraGoldBright
                        )
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        Text(
                            text = activeArtist,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = FazoraIvory
                        )
                        Text(
                            text = "${artistTracks.size} indexed studio tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraChampagneGold
                        )
                        Spacer(modifier = Modifier.height(PhiTokens.Space13))
                        Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                            Button(
                                onClick = { onPlayArtist(activeArtist, false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FazoraGold,
                                    contentColor = FazoraObsidian
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("artist_play_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                                Text("Play Artist", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onPlayArtist(activeArtist, true) },
                                shape = RoundedCornerShape(12.dp),
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
