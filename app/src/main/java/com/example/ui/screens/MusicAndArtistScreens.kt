package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.ui.viewmodel.FazoraUiState

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
    val tier = uiState.settings.experienceTier
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
    ) { onAutoScanMedia() }

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
            .background(MaterialTheme.colorScheme.background)
            .testTag("music_screen")
    ) {
        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "مجموعه صوتی ولورا" else "VELORA AUDIO COLLECTION",
                title = "${if (isPersian) "آهنگ‌ها" else "Music"} (${uiState.audioTracks.size})",
                subtitle = if (isPersian) {
                    "$hiResCount ترک با وضوح بالا • ${uiState.favoriteTracks.size} علاقه‌مندی"
                } else {
                    "$hiResCount Hi-Res (≥48kHz) • ${uiState.favoriteTracks.size} Favorites"
                }
            ) {
                if (tier != ExperienceTier.SIMPLE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = quickFilterQuery,
                            onValueChange = { quickFilterQuery = it },
                            placeholder = {
                                Text(
                                    if (isPersian) "فیلتر سریع آهنگ یا خواننده..." else "Quick filter tracks or artists...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (quickFilterQuery.isNotEmpty()) {
                                    IconButton(onClick = { quickFilterQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        VeloraSecondaryButton(
                            text = "HI-RES",
                            selected = showOnlyHiRes,
                            onClick = { showOnlyHiRes = !showOnlyHiRes },
                            tag = "music_hires_filter_button"
                        )
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VeloraPrimaryButton(
                        text = if (isPersian) "پخش همه (${uiState.audioTracks.size})" else "Play All (${uiState.audioTracks.size})",
                        icon = Icons.Default.PlayArrow,
                        onClick = onPlayAll,
                        tag = "music_play_all_button",
                        modifier = Modifier.weight(PhiTokens.PRIMARY_WEIGHT)
                    )
                    if (tier != ExperienceTier.SIMPLE) {
                        VeloraSecondaryButton(
                            text = if (isPersian) "تصادفی" else "Shuffle",
                            icon = Icons.Default.Shuffle,
                            onClick = onShuffleAll,
                            tag = "music_shuffle_all_button",
                            modifier = Modifier.weight(PhiTokens.SECONDARY_WEIGHT)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(PhiTokens.Space8))
                VeloraSecondaryButton(
                    text = if (isPersian) "اسکن خودکار آهنگ‌های دستگاه" else "Auto-Scan Device Music Storage",
                    icon = Icons.Default.Refresh,
                    onClick = { permissionLauncher.launch(permissionsToRequest) },
                    tag = "music_auto_scan_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (displayedTracks.isEmpty()) {
            item {
                VeloraEmptyState(
                    icon = Icons.Default.MusicNote,
                    title = if (isPersian) "آهنگی یافت نشد" else "No Audio Tracks Found",
                    subtitle = if (isPersian) {
                        "برای بارگذاری آهنگ‌های دستگاه دکمه اسکن خودکار را بزنید."
                    } else {
                        "Scan your device storage to index local songs."
                    },
                    actionLabel = if (isPersian) "اسکن حافظه" else "Scan Storage",
                    onAction = { permissionLauncher.launch(permissionsToRequest) }
                )
            }
        } else {
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
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val artists = uiState.artistsMap.keys.toList()
    val activeArtist = uiState.selectedArtist ?: artists.firstOrNull()
    val artistTracks = if (activeArtist != null) uiState.artistsMap[activeArtist].orEmpty() else emptyList()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(PhiTokens.Space21)
            .testTag("artist_screen")
    ) {
        val isWide = maxWidth >= PhiTokens.WideScreenBreakpoint
        if (isWide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                    modifier = Modifier
                        .weight(PhiTokens.SECONDARY_WEIGHT)
                        .fillMaxHeight()
                ) {
                    items(artists) { artist ->
                        VeloraSecondaryButton(
                            text = "$artist (${uiState.artistsMap[artist]?.size ?: 0})",
                            icon = Icons.Default.Album,
                            selected = artist == activeArtist,
                            onClick = { onSelectArtist(artist) },
                            tag = "artist_chip_${artist.hashCode()}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ArtistDetailList(
                    activeArtist = activeArtist,
                    artistTracks = artistTracks,
                    uiState = uiState,
                    isPersian = isPersian,
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
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                modifier = Modifier.fillMaxSize()
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(artists) { artist ->
                        VeloraSecondaryButton(
                            text = "$artist (${uiState.artistsMap[artist]?.size ?: 0})",
                            icon = Icons.Default.Album,
                            selected = artist == activeArtist,
                            onClick = { onSelectArtist(artist) },
                            tag = "artist_chip_${artist.hashCode()}"
                        )
                    }
                }
                ArtistDetailList(
                    activeArtist = activeArtist,
                    artistTracks = artistTracks,
                    uiState = uiState,
                    isPersian = isPersian,
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
private fun ArtistDetailList(
    activeArtist: String?,
    artistTracks: List<Track>,
    uiState: FazoraUiState,
    isPersian: Boolean,
    onPlayArtist: (String, Boolean) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = modifier
    ) {
        if (activeArtist != null) {
            item {
                VeloraSectionCard(
                    eyebrow = if (isPersian) "دیسکوگرافی هنرمند" else "ARTIST DISCOGRAPHY",
                    title = activeArtist,
                    subtitle = "${artistTracks.size} indexed tracks"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                        VeloraPrimaryButton(
                            text = if (isPersian) "پخش هنرمند" else "Play Artist",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onPlayArtist(activeArtist, false) },
                            tag = "artist_play_button"
                        )
                        VeloraSecondaryButton(
                            text = if (isPersian) "پخش تصادفی" else "Shuffle Artist",
                            icon = Icons.Default.Shuffle,
                            onClick = { onPlayArtist(activeArtist, true) },
                            tag = "artist_shuffle_button"
                        )
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
                    onAddToPlaylist = { pid -> onAddTrackToPlaylist(pid, track.id) },
                    onToggleFavorite = { onToggleFavorite(track) }
                )
            }
        }
    }
}
