package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.Playlist
import com.example.domain.model.Track
import com.example.ui.components.TrackRowItem
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraLargeTitleHeader
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.veloraColors
import com.example.ui.viewmodel.FazoraUiState

@Composable
fun SearchScreen(
    uiState: FazoraUiState,
    onQueryChange: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrackToQueue: (Track) -> Unit,
    onAddTrackToPlaylist: (Long, String) -> Unit,
    onToggleFavorite: (Track) -> Unit = {},
    onExecuteCopilotCommand: (String) -> Unit = {},
    onToggleOnlyValidMusic: (Boolean) -> Unit = {},
    onPlayPlaylist: (Playlist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val tier = uiState.settings.experienceTier
    var copilotInput by remember { mutableStateOf("") }
    val trimmedQuery = uiState.searchQuery.trim()
    val audioResults = remember(uiState.searchResults) {
        uiState.searchResults.filter { it.mediaType == MediaType.AUDIO }
    }
    val videoResults = remember(uiState.searchResults) {
        uiState.searchResults.filter { it.mediaType == MediaType.VIDEO }
    }
    val matchingArtists = remember(trimmedQuery, uiState.artistsMap) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            uiState.artistsMap.entries.filter { (artist, _) ->
                artist.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }
    val matchingPlaylists = remember(trimmedQuery, uiState.playlists) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            uiState.playlists.filter { playlist ->
                playlist.name.contains(trimmedQuery, ignoreCase = true)
            }
        }
    }
    val semanticHighlights = remember(trimmedQuery, uiState.omniSearchMatches) {
        if (trimmedQuery.isBlank()) {
            emptyList()
        } else {
            uiState.omniSearchMatches.take(5)
        }
    }
    val hasAnyMatches = audioResults.isNotEmpty() ||
        videoResults.isNotEmpty() ||
        matchingArtists.isNotEmpty() ||
        matchingPlaylists.isNotEmpty()

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("search_screen")
    ) {
        item {
            VeloraLargeTitleHeader(
                eyebrow = if (isPersian) "جستجوی یکپارچه" else "OMNISEARCH & COPILOT",
                title = if (isPersian) "جستجو" else "Search",
                subtitle = if (isPersian) {
                    "یافتن فوری آهنگ، هنرمند، متن و فرمان‌های هوشمند"
                } else {
                    "Instant discovery across tracks, artists, synced lyrics, and Copilot"
                }
            )
        }
        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "موتور جستجوی هوشمند ولورا" else "VELORA OMNISEARCH",
                title = if (isPersian) "جستجوی آهنگ، هنرمند، آلبوم و متن" else "Universal Media Search",
                subtitle = if (isPersian) {
                    "${uiState.searchResults.size} نتیجه تطبیقی"
                } else {
                    "${uiState.searchResults.size} matched media items"
                }
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onQueryChange,
                    label = {
                        Text(
                            if (isPersian) {
                                "عنوان، خواننده، آلبوم یا فرمت (FLAC, MP4)..."
                            } else {
                                "Search title, artist, album, or format (FLAC, MP4)..."
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.testTag("omnisearch_clear_button")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("omnisearch_query_input")
                )

                Spacer(modifier = Modifier.height(PhiTokens.Space13))

                val semanticChips = remember {
                    listOf("Favorites", "Hi-Res", "Unfinished", "Night Drive")
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    itemsIndexed(semanticChips) { idx, chip ->
                        VeloraSecondaryButton(
                            text = chip,
                            selected = uiState.searchQuery.equals(chip, ignoreCase = true),
                            onClick = { onQueryChange(chip) },
                            tag = "semantic_chip_$idx"
                        )
                    }
                }

                if (tier != ExperienceTier.SIMPLE) {
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isPersian) "فقط آهنگ‌های کامل و معتبر" else "Valid Songs Only (Skip Short Clips)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = uiState.settings.onlyValidMusicInSearch,
                            onCheckedChange = onToggleOnlyValidMusic,
                            modifier = Modifier.testTag("search_valid_music_switch")
                        )
                    }
                }
            }
        }

        if (tier != ExperienceTier.SIMPLE) {
            item {
                VeloraSectionCard(
                    eyebrow = if (isPersian) "دستیار رسانه محلی" else "VELORA MEDIA COPILOT",
                    title = if (isPersian) "فرمان صوتی/متنی هوشمند" else "Natural-Language Media Commands",
                    subtitle = if (isPersian) {
                        "مثال: Play favorites • Resume session • Find Hi-Res"
                    } else {
                        "Try: 'Play favorites', 'Resume last session', 'Find Hi-Res tracks', 'Export SRT'"
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = copilotInput,
                            onValueChange = { copilotInput = it },
                            placeholder = {
                                Text(if (isPersian) "دستور خود را بنویسید..." else "Enter media command...")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copilot_command_input")
                        )
                        VeloraPrimaryButton(
                            text = if (isPersian) "اجرا" else "Run",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                if (copilotInput.isNotBlank()) {
                                    onExecuteCopilotCommand(copilotInput.trim())
                                    copilotInput = ""
                                }
                            },
                            tag = "copilot_execute_button"
                        )
                    }
                }
            }
        }

        if (!hasAnyMatches) {
            item {
                VeloraEmptyState(
                    icon = Icons.Default.Search,
                    title = if (isPersian) "نتیجه‌ای یافت نشد" else "No Matching Media Found",
                    subtitle = if (isPersian) {
                        "عبارت دیگری را امتحان کنید یا فیلتر جستجو را پاک کنید."
                    } else {
                        "Try a different keyword, artist name, or clear the search query."
                    },
                    actionLabel = if (uiState.searchQuery.isNotEmpty()) {
                        if (isPersian) "پاک کردن جستجو" else "Clear Query"
                    } else {
                        null
                    },
                    actionTag = "search_empty_clear_button",
                    onAction = if (uiState.searchQuery.isNotEmpty()) {
                        { onQueryChange("") }
                    } else {
                        null
                    }
                )
            }
        } else {
            if (matchingArtists.isNotEmpty()) {
                item {
                    VeloraSectionCard(
                        eyebrow = if (isPersian) "دسته‌بندی هنرمندان" else "MATCHING ARTISTS",
                        title = if (isPersian) {
                            "هنرمندان (${matchingArtists.size})"
                        } else {
                            "Artists (${matchingArtists.size})"
                        },
                        modifier = Modifier.testTag("search_section_artists")
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            items(matchingArtists, key = { it.key }) { (artist, tracks) ->
                                VeloraSecondaryButton(
                                    text = "$artist (${tracks.size})",
                                    icon = Icons.Default.Album,
                                    onClick = { tracks.firstOrNull()?.let(onPlayTrack) },
                                    tag = "search_artist_chip_${artist.hashCode()}"
                                )
                            }
                        }
                    }
                }
            }

            if (matchingPlaylists.isNotEmpty()) {
                item {
                    VeloraSectionCard(
                        eyebrow = if (isPersian) "دسته‌بندی پلی‌لیست‌ها" else "MATCHING PLAYLISTS",
                        title = if (isPersian) {
                            "پلی‌لیست‌ها (${matchingPlaylists.size})"
                        } else {
                            "Playlists (${matchingPlaylists.size})"
                        },
                        modifier = Modifier.testTag("search_section_playlists")
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            items(matchingPlaylists, key = { it.id }) { playlist ->
                                VeloraSecondaryButton(
                                    text = "${playlist.name} (${playlist.trackCount})",
                                    icon = Icons.Default.LibraryMusic,
                                    onClick = { onPlayPlaylist(playlist) },
                                    tag = "search_playlist_card_${playlist.id}"
                                )
                            }
                        }
                    }
                }
            }

            if (semanticHighlights.isNotEmpty()) {
                item {
                    VeloraSectionCard(
                        eyebrow = if (isPersian) "تطبیق معنایی و متن" else "SEMANTIC & LYRICS HIGHLIGHTS",
                        title = if (isPersian) {
                            "برگزیده‌های هوشمند (${semanticHighlights.size})"
                        } else {
                            "Smart Matches (${semanticHighlights.size})"
                        },
                        modifier = Modifier.testTag("search_section_lyrics")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                            semanticHighlights.forEach { match ->
                                val reason = if (isPersian) match.matchReasonFa else match.matchReasonEn
                                VeloraSecondaryButton(
                                    text = "${match.track.title} • $reason",
                                    icon = Icons.Default.AutoAwesome,
                                    onClick = { onPlayTrack(match.track) },
                                    tag = "search_semantic_match_${match.track.id}",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (audioResults.isNotEmpty()) {
                item {
                    Text(
                        text = if (isPersian) {
                            "موسیقی و قطعات صوتی (${audioResults.size})"
                        } else {
                            "Audio Tracks (${audioResults.size})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.veloraColors.accent,
                        modifier = Modifier.testTag("search_section_audio")
                    )
                }
                items(audioResults, key = { "audio_${it.id}" }) { track ->
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

            if (videoResults.isNotEmpty()) {
                item {
                    Text(
                        text = if (isPersian) {
                            "ویدیوها (${videoResults.size})"
                        } else {
                            "Videos (${videoResults.size})"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.veloraColors.accentGold,
                        modifier = Modifier.testTag("search_section_video")
                    )
                }
                items(videoResults, key = { "video_${it.id}" }) { track ->
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
}
