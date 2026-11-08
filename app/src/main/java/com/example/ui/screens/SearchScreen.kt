package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.Track
import com.example.domain.model.VeloraPersonalMediaGraph
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
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
    modifier: Modifier = Modifier
) {
    val semanticPresets = remember {
        listOf(
            "favorite" to "Favorites",
            "unfinished" to "Continue",
            "hi-res" to "Hi-Res / FLAC",
            "night" to "Calm / Night"
        )
    }
    val copilotCommands = remember {
        listOf(
            "resume last" to "Resume Last",
            "play favorites" to "Play Favorites",
            "export srt" to "Export .SRT",
            "export vtt" to "Export .VTT",
            "explain" to "Why Recommended?"
        )
    }
    val graphSummary = remember(uiState.allTracks) {
        VeloraPersonalMediaGraph.buildGraphSummary(uiState.allTracks)
    }
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val reasonByTrackId = remember(uiState.omniSearchMatches, isPersian) {
        uiState.omniSearchMatches.associate { match ->
            match.track.id to (if (isPersian) match.matchReasonFa else match.matchReasonEn)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("search_screen")
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
                        text = "OMNISEARCH • FUZZY & SEMANTIC INDEX",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onQueryChange,
                        label = { Text("Search title, artist, album, or mood (favorite, hi-res, night)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FazoraGold) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("search_clear_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = FazoraSteel)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_query_input")
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val onlyValid = uiState.settings.onlyValidMusicInSearch
                        Button(
                            onClick = { onToggleOnlyValidMusic(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (onlyValid) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (onlyValid) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("filter_valid_music_btn")
                        ) {
                            Text(if (isPersian) "🎵 فقط آهنگ‌های معتبر" else "🎵 Valid Music Only")
                        }
                        Button(
                            onClick = { onToggleOnlyValidMusic(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!onlyValid) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (!onlyValid) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("filter_all_audio_btn")
                        ) {
                            Text(if (isPersian) "📱 تمام صداهای گوشی" else "📱 All Device Audio")
                        }
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(semanticPresets, key = { it.first }) { (queryToken, label) ->
                            val isSelected = uiState.searchQuery.equals(queryToken, ignoreCase = true)
                            OutlinedButton(
                                onClick = {
                                    onQueryChange(if (isSelected) "" else queryToken)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                    contentColor = if (isSelected) FazoraObsidian else FazoraIvory
                                ),
                                contentPadding = PaddingValues(horizontal = PhiTokens.Space13, vertical = 4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("semantic_chip_$queryToken")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    val countLabel = if (uiState.settings.onlyValidMusicInSearch) {
                        if (isPersian) {
                            "${uiState.searchResults.size} آهنگ معتبر یافت شد (بدون ویس و صداهای ضبط‌شده)"
                        } else {
                            "${uiState.searchResults.size} valid songs found (ignoring short voice notes)"
                        }
                    } else {
                        if (isPersian) {
                            "${uiState.searchResults.size} فایل صوتی و ضبط یافت شد"
                        } else {
                            "${uiState.searchResults.size} total audio tracks & recordings found"
                        }
                    }
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = FazoraSteel
                    )
                }
            }
        }

        item {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
                    .testTag("media_copilot_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "MEDIA COPILOT & PERSONAL MEDIA GRAPH",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Graph Nodes: ${graphSummary.totalNodes} • Verified Edges: ${graphSummary.totalEdges} • " +
                            "Formats: ${graphSummary.formatDistribution.entries.joinToString { "${it.key} (${it.value})" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(copilotCommands, key = { it.first }) { (cmd, label) ->
                            OutlinedButton(
                                onClick = { onExecuteCopilotCommand(cmd) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = FazoraCarbonElevated,
                                    contentColor = FazoraIvory
                                ),
                                contentPadding = PaddingValues(horizontal = PhiTokens.Space13, vertical = 4.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copilot_cmd_${cmd.replace(' ', '_')}")
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        items(uiState.searchResults, key = { "search_${it.id}" }) { track ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val reason = reasonByTrackId[track.id]
                if (!reason.isNullOrBlank() && uiState.searchQuery.isNotBlank()) {
                    Text(
                        text = "Match: $reason",
                        style = MaterialTheme.typography.labelSmall,
                        color = FazoraGold,
                        modifier = Modifier.padding(horizontal = PhiTokens.Space8)
                    )
                }
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
