package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.RadioStation
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.Track
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraHorizontalMediaShelf
import com.example.ui.components.VeloraLargeTitleHeader
import com.example.ui.components.VeloraLoadingSkeleton
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraAlphaTokens
import com.example.ui.theme.mixedScriptText
import com.example.ui.theme.veloraColors
import com.example.ui.viewmodel.VeloraUiState

@Composable
fun HomeScreen(
    uiState: VeloraUiState,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onPlaySmartPlaylist: (SmartPlaylist, Boolean) -> Unit,
    onResumeHistory: (PlaybackHistoryEntry) -> Unit,
    onTuneRadio: (RadioStation) -> Unit,
    onOpenLibrarySubTab: (LibrarySubTab) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onAutoScanMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val semantic = MaterialTheme.veloraColors
    val context = LocalContext.current

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

    val triggerScan = {
        val granted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) onAutoScanMedia() else permissionLauncher.launch(permissionsToRequest)
    }

    val madeForYouTracks = remember(uiState.favoriteTracks, uiState.audioTracks) {
        (uiState.favoriteTracks + uiState.audioTracks).distinctBy { it.id }.take(12)
    }
    val recentTracks = remember(uiState.playbackHistory, uiState.allTracks) {
        if (uiState.playbackHistory.isNotEmpty()) {
            uiState.playbackHistory.map { it.track }.distinctBy { it.id }.take(12)
        } else {
            uiState.allTracks.take(12)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(semantic.bg)
            .testTag("home_screen")
    ) {
        item {
            VeloraLargeTitleHeader(
                eyebrow = if (isPersian) "فراتر از پخش" else "BEYOND PLAYBACK",
                title = if (isPersian) "هم‌اکنون بشنوید" else "Listen Now",
                subtitle = if (isPersian) {
                    "${uiState.audioTracks.size} قطعه صوتی • ${uiState.videoTracks.size} ویدیو • ${uiState.playlists.size} پلی‌لیست"
                } else {
                    "${uiState.audioTracks.size} Audio Tracks • ${uiState.videoTracks.size} Videos • ${uiState.playlists.size} Playlists"
                },
                action = {
                    VeloraSecondaryButton(
                        text = if (isPersian) "اسکن" else "Scan",
                        icon = Icons.Default.Refresh,
                        onClick = triggerScan,
                        tag = "home_quick_scan_button"
                    )
                }
            )
        }

        item {
            HomeQuickActionsRow(
                isPersian = isPersian,
                hasTracks = uiState.allTracks.isNotEmpty(),
                hasActiveTrack = uiState.engineState.currentTrack != null,
                onPlayAll = { onPlayAll(false) },
                onShuffleAll = { onPlayAll(true) },
                onOpenNowPlaying = onOpenNowPlaying,
                onOpenLibrarySubTab = onOpenLibrarySubTab
            )
        }

        if (uiState.scanProgress.isScanning) {
            item {
                VeloraLoadingSkeleton(
                    rowCount = 3,
                    label = if (isPersian) "در حال فهرست‌سازی رسانه..." else "Indexing your media library..."
                )
            }
        }

        if (uiState.allTracks.isEmpty() && !uiState.scanProgress.isScanning) {
            item {
                VeloraEmptyState(
                    icon = Icons.Default.MusicNote,
                    title = if (isPersian) "کتابخانه رسانه خالی است" else "Your Library Is Ready",
                    subtitle = if (isPersian) {
                        "برای شروع گوش دادن، پوشه‌های صوتی و تصویری دستگاه را اسکن کنید."
                    } else {
                        "Scan your device storage to discover local Hi-Res music, albums, and cinema videos."
                    },
                    actionLabel = if (isPersian) "اسکن حافظه دستگاه" else "Scan Device Media",
                    actionTag = "home_scan_storage_button",
                    onAction = triggerScan
                )
            }
        }

        if (uiState.smartPlaylists.isNotEmpty()) {
            item {
                HomeSmartMixesShelf(
                    smartPlaylists = uiState.smartPlaylists,
                    isPersian = isPersian,
                    onPlaySmartPlaylist = onPlaySmartPlaylist
                )
            }
        }

        if (madeForYouTracks.isNotEmpty()) {
            item {
                VeloraHorizontalMediaShelf(
                    title = if (isPersian) "پیشنهادی برای شما" else "Made for You",
                    subtitle = if (isPersian) "قطعات برگزیده و باکیفیت کتابخانه شما" else "Hand-picked lossless & favorite tracks",
                    tracks = madeForYouTracks,
                    activeTrackId = uiState.engineState.currentTrack?.id,
                    onPlayTrack = { track -> onPlayTrack(track, madeForYouTracks) },
                    actionLabel = if (isPersian) "پخش تصادفی" else "Shuffle",
                    actionTag = "home_made_for_you_shuffle",
                    onAction = { onPlayAll(true) },
                    shelfTag = "home_shelf_made_for_you"
                )
            }
        }

        if (recentTracks.isNotEmpty()) {
            item {
                val historyMap = remember(uiState.playbackHistory) {
                    uiState.playbackHistory.associateBy { it.track.id }
                }
                VeloraHorizontalMediaShelf(
                    title = if (isPersian) "پخش‌های اخیر" else "Recently Played",
                    subtitle = if (uiState.playbackHistory.isNotEmpty()) {
                        if (isPersian) "ادامه از آخرین موقعیت پخش" else "Pick up right where you left off"
                    } else {
                        if (isPersian) "آماده پخش از کتابخانه" else "Ready to play from your collection"
                    },
                    tracks = recentTracks,
                    activeTrackId = uiState.engineState.currentTrack?.id,
                    onPlayTrack = { track ->
                        val historyEntry = historyMap[track.id]
                        if (historyEntry != null) {
                            onResumeHistory(historyEntry)
                        } else {
                            onPlayTrack(track, recentTracks)
                        }
                    },
                    shelfTag = "home_shelf_recently_played"
                )
            }
        }

        if (uiState.radioStations.isNotEmpty()) {
            item {
                HomeRadioHighlightsShelf(
                    stations = uiState.radioStations.take(8),
                    isPersian = isPersian,
                    onTuneRadio = onTuneRadio,
                    onSeeAllRadio = { onOpenLibrarySubTab(LibrarySubTab.RADIO) }
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActionsRow(
    isPersian: Boolean,
    hasTracks: Boolean,
    hasActiveTrack: Boolean,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenLibrarySubTab: (LibrarySubTab) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_quick_actions_row")
    ) {
        if (hasTracks) {
            item {
                VeloraPrimaryButton(
                    text = if (isPersian) "پخش همه" else "Play All",
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayAll,
                    tag = "home_play_all_button"
                )
            }
            item {
                VeloraSecondaryButton(
                    text = if (isPersian) "تصادفی" else "Shuffle",
                    icon = Icons.Default.Shuffle,
                    onClick = onShuffleAll,
                    tag = "home_shuffle_all_button"
                )
            }
        }
        if (hasActiveTrack) {
            item {
                VeloraSecondaryButton(
                    text = if (isPersian) "در حال پخش" else "Now Playing",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onOpenNowPlaying,
                    tag = "home_open_player_button"
                )
            }
        }
        item {
            VeloraSecondaryButton(
                text = if (isPersian) "موسیقی" else "Music",
                icon = Icons.Default.MusicNote,
                onClick = { onOpenLibrarySubTab(LibrarySubTab.MUSIC) },
                tag = "home_jump_music_button"
            )
        }
        item {
            VeloraSecondaryButton(
                text = if (isPersian) "هنرمندان" else "Artists",
                icon = Icons.Default.Album,
                onClick = { onOpenLibrarySubTab(LibrarySubTab.ARTISTS) },
                tag = "home_jump_artists_button"
            )
        }
        item {
            VeloraSecondaryButton(
                text = if (isPersian) "رادیو زنده" else "Live Radio",
                icon = Icons.Default.Radio,
                onClick = { onOpenLibrarySubTab(LibrarySubTab.RADIO) },
                tag = "home_jump_radio_button"
            )
        }
        item {
            VeloraSecondaryButton(
                text = if (isPersian) "پلی‌لیست‌ها" else "Playlists",
                icon = Icons.Default.LibraryMusic,
                onClick = { onOpenLibrarySubTab(LibrarySubTab.PLAYLISTS) },
                tag = "home_jump_playlists_button"
            )
        }
    }
}

@Composable
private fun HomeSmartMixesShelf(
    smartPlaylists: List<SmartPlaylist>,
    isPersian: Boolean,
    onPlaySmartPlaylist: (SmartPlaylist, Boolean) -> Unit
) {
    val semantic = MaterialTheme.veloraColors
    Column(
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_shelf_top_mixes")
    ) {
        Text(
            text = if (isPersian) "میکس‌های هوشمند" else "Top Mixes",
            style = MaterialTheme.typography.headlineMedium,
            color = semantic.textPrimary,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
            items(smartPlaylists, key = { "${it.category.id}_${it.explainReasonEn}" }) { smart ->
                val mixId = smart.category.id
                val mixTitle = if (isPersian) smart.category.titleFa else smart.category.titleEn
                val mixReason = if (isPersian) smart.explainReasonFa else smart.explainReasonEn
                Surface(
                    color = semantic.surfaceCard,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                    modifier = Modifier
                        .width(PhiTokens.CardWidthCompact)
                        .border(
                            PhiTokens.BorderHairline,
                            semantic.accent.copy(alpha = VeloraAlphaTokens.ACCENT_BORDER_ALPHA),
                            RoundedCornerShape(PhiTokens.CornerRadius13)
                        )
                        .clickable { onPlaySmartPlaylist(smart, false) }
                        .testTag("home_mix_card_$mixId")
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.padding(PhiTokens.Space13)
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(semantic.accentContainer, semantic.surfaceElevated)
                                    )
                                )
                                .padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                        ) {
                            Text(
                                text = mixedScriptText(mixTitle),
                                style = MaterialTheme.typography.titleMedium,
                                color = semantic.onAccentContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = mixedScriptText(mixReason),
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${smart.tracks.size} ${if (isPersian) "قطعه" else "tracks"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRadioHighlightsShelf(
    stations: List<RadioStation>,
    isPersian: Boolean,
    onTuneRadio: (RadioStation) -> Unit,
    onSeeAllRadio: () -> Unit
) {
    val semantic = MaterialTheme.veloraColors
    VeloraSectionCard(
        eyebrow = if (isPersian) "پخش زنده جهانی" else "LIVE STREAMING TUNER",
        title = if (isPersian) "ایستگاه‌های برگزیده" else "Featured Radio Channels",
        headerAction = {
            VeloraSecondaryButton(
                text = if (isPersian) "همه" else "All",
                onClick = onSeeAllRadio,
                tag = "home_see_all_radio_button"
            )
        }
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
            items(stations, key = { it.id }) { station ->
                VeloraSecondaryButton(
                    text = "${station.name} • ${station.bitrateKbps}k",
                    icon = Icons.Default.Radio,
                    onClick = { onTuneRadio(station) },
                    tag = "home_radio_chip_${station.id}"
                )
            }
        }
    }
}
