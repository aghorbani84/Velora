package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.model.ExperienceTier
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.Playlist
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.Track
import com.example.domain.model.TrackFilterMode
import com.example.domain.model.TrackSortOrder
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState

@Composable
internal fun FilterAndSortSection(
    totalTracks: Int,
    filterMode: TrackFilterMode,
    sortOrder: TrackSortOrder,
    isPersian: Boolean,
    onSelectFilterMode: (TrackFilterMode) -> Unit,
    onSelectSortOrder: (TrackSortOrder) -> Unit
) {
    VeloraSectionCard(
        eyebrow = if (isPersian) "مرتب‌سازی و فیلتر" else "INDEXED MEDIA CATALOG",
        title = if (isPersian) "فهرست رسانه‌ها ($totalTracks)" else "Media Catalog ($totalTracks)"
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
            items(TrackFilterMode.entries) { mode ->
                VeloraSecondaryButton(
                    text = if (isPersian) mode.labelFa else mode.labelEn,
                    selected = filterMode == mode,
                    onClick = { onSelectFilterMode(mode) },
                    tag = "filter_mode_${mode.name}"
                )
            }
        }
        Spacer(modifier = Modifier.height(PhiTokens.Space8))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
            items(TrackSortOrder.entries) { order ->
                VeloraSecondaryButton(
                    text = if (isPersian) order.labelFa else order.labelEn,
                    selected = sortOrder == order,
                    onClick = { onSelectSortOrder(order) },
                    tag = "sort_order_${order.name}"
                )
            }
        }
    }
}

@Composable
internal fun SmartPlaylistsAndAnalyticsSection(
    uiState: FazoraUiState,
    isPersian: Boolean,
    tier: ExperienceTier,
    onPlaySmartPlaylist: (SmartPlaylist, Boolean) -> Unit,
    onOpenSmartRuleBuilder: () -> Unit
) {
    val stats = uiState.listeningStats
    val topArtist = stats.topArtists.firstOrNull()?.first ?: "VELORA"
    VeloraSectionCard(
        eyebrow = if (isPersian) "هوش مصنوعی محلی و آمار" else "SMART PLAYLISTS & ANALYTICS",
        title = if (isPersian) {
            "پلی‌لیست‌های هوشمند (${uiState.smartPlaylists.size})"
        } else {
            "Smart Mixes (${uiState.smartPlaylists.size})"
        },
        subtitle = if (isPersian) {
            "مجموع پخش: ${stats.totalPlaysCount} بار • ${stats.hiResTracksCount} ترک استودیویی Hi-Res"
        } else {
            "Total Plays: ${stats.totalPlaysCount} • Top Artist: $topArtist • ${stats.hiResTracksCount} Hi-Res"
        },
        modifier = Modifier.testTag("listening_stats_recap_card"),
        headerAction = if (tier == ExperienceTier.PRO) {
            {
                VeloraSecondaryButton(
                    text = if (isPersian) "قانون جدید" else "Smart Rule",
                    icon = Icons.Default.AutoAwesome,
                    onClick = onOpenSmartRuleBuilder,
                    tag = "open_smart_rule_builder_button"
                )
            }
        } else {
            null
        }
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
            items(uiState.smartPlaylists, key = { "${it.category.id}_${it.explainReasonEn}" }) { mix ->
                val mixId = mix.category.id
                VeloraSectionCard(
                    title = if (isPersian) mix.category.titleFa else mix.category.titleEn,
                    subtitle = "${mix.tracks.size} tracks • ${if (isPersian) mix.explainReasonFa else mix.explainReasonEn}",
                    modifier = Modifier
                        .width(PhiTokens.CardWidthCompact)
                        .testTag("smart_playlist_$mixId")
                ) {
                    VeloraPrimaryButton(
                        text = if (isPersian) "پخش میکس" else "Play Mix",
                        icon = Icons.Default.PlayArrow,
                        onClick = { onPlaySmartPlaylist(mix, false) },
                        tag = "play_smart_playlist_$mixId",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlaybackHistorySection(
    history: List<PlaybackHistoryEntry>,
    isPersian: Boolean,
    onResumeHistory: (PlaybackHistoryEntry) -> Unit,
    onClearHistory: () -> Unit
) {
    VeloraSectionCard(
        eyebrow = if (isPersian) "ماشین زمان رسانه" else "MEDIA TIME MACHINE",
        title = if (isPersian) "تاریخچه پخش (${history.size})" else "Playback History (${history.size})",
        modifier = Modifier.testTag("playback_history_section"),
        headerAction = if (history.isNotEmpty()) {
            {
                VeloraSecondaryButton(
                    text = if (isPersian) "پاک کردن" else "Clear",
                    icon = Icons.Default.DeleteOutline,
                    onClick = onClearHistory,
                    tag = "clear_playback_history_button"
                )
            }
        } else {
            null
        }
    ) {
        if (history.isEmpty()) {
            Text(
                text = if (isPersian) {
                    "هنوز سابقه پخشی ثبت نشده است."
                } else {
                    "No playback history recorded yet. Play any track to save your resume point."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                items(history, key = { it.track.id }) { entry ->
                    VeloraSectionCard(
                        title = entry.track.title,
                        subtitle = "${entry.track.artist} • Plays: ${entry.playCount}",
                        modifier = Modifier
                            .width(PhiTokens.CardWidthCompact)
                            .testTag("history_card_${entry.track.id}")
                    ) {
                        val resumeLabel = if (entry.lastPositionMs > 0L) {
                            "Resume ${Track.formatDurationMs(entry.lastPositionMs)}"
                        } else {
                            if (isPersian) "پخش مجدد" else "Play Cached"
                        }
                        VeloraPrimaryButton(
                            text = resumeLabel,
                            icon = Icons.Default.PlayArrow,
                            onClick = { onResumeHistory(entry) },
                            tag = "resume_history_${entry.track.id}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartRuleBuilderDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSaveRule: (String, Int, Int, Int, Boolean, String) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    var minSampleRate by remember { mutableStateOf("48000") }
    var favoritesOnly by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = if (isPersian) "ساخت پلی‌لیست هوشمند سفارشی" else "Custom Smart Playlist Rule",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.testTag("smart_rule_builder_dialog")
            ) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text(if (isPersian) "نام قانون" else "Rule Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_rule_name_input")
                )
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(if (isPersian) "کلمه کلیدی یا ژانر" else "Genre / Artist Keyword") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_rule_genre_input")
                )
                OutlinedTextField(
                    value = minSampleRate,
                    onValueChange = { minSampleRate = it },
                    label = { Text(if (isPersian) "حداقل نرخ نمونه‌برداری (Hz)" else "Min Sample Rate (Hz)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_rule_min_sr_input")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = favoritesOnly,
                        onCheckedChange = { favoritesOnly = it },
                        modifier = Modifier.testTag("smart_rule_favorites_checkbox")
                    )
                    Text(
                        text = if (isPersian) "فقط علاقه‌مندی‌ها" else "Favorites Only",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            VeloraPrimaryButton(
                text = if (isPersian) "اعمال قانون" else "Apply Rule",
                onClick = {
                    val sr = minSampleRate.toIntOrNull() ?: 44100
                    onSaveRule(
                        ruleName.ifBlank { "Custom Studio Rule" },
                        0,
                        sr,
                        0,
                        favoritesOnly,
                        keyword
                    )
                },
                tag = "run_smart_rule_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "بستن" else "Cancel")
            }
        }
    )
}

@Composable
fun M3u8ManagerDialog(
    playlists: List<Playlist>,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onExportPlaylist: (Long) -> String,
    onImportPlaylist: (String, String) -> Unit
) {
    var importName by remember { mutableStateOf("Imported M3U8") }
    var m3u8Text by remember { mutableStateOf("#EXTM3U\n") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = if (isPersian) "مدیریت پلی‌لیست M3U8" else "M3U8 Playlist Import / Export",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.testTag("m3u8_manager_dialog")
            ) {
                if (playlists.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        items(playlists, key = { it.id }) { pl ->
                            VeloraSecondaryButton(
                                text = "Export ${pl.name}",
                                onClick = { m3u8Text = onExportPlaylist(pl.id) },
                                tag = "export_m3u8_button"
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = importName,
                    onValueChange = { importName = it },
                    label = { Text(if (isPersian) "نام پلی‌لیست" else "Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = m3u8Text,
                    onValueChange = { m3u8Text = it },
                    label = { Text("M3U8 Content") },
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("m3u8_text_input")
                )
            }
        },
        confirmButton = {
            VeloraPrimaryButton(
                text = if (isPersian) "درون‌ریزی M3U8" else "Import M3U8",
                onClick = { onImportPlaylist(importName, m3u8Text) },
                tag = "import_m3u8_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "بستن" else "Close")
            }
        }
    )
}
