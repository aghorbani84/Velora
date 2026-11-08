package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ListeningStatsSummary
import com.example.domain.model.Playlist
import com.example.domain.model.SubtitleColorTheme
import com.example.domain.model.SubtitleFontScale
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.Track
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VideoCinemaSettings
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraEmeraldOk
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldBright
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraChampagneGold

@Composable
fun ListeningStatsRecapCard(
    stats: ListeningStatsSummary,
    isPersian: Boolean,
    onOpenRuleBuilder: () -> Unit,
    onOpenM3uManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius21),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, FazoraGold.copy(alpha = 0.55f), RoundedCornerShape(PhiTokens.CornerRadius21))
            .testTag("listening_stats_recap_card")
    ) {
        Column(modifier = Modifier.padding(PhiTokens.Space21)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = FazoraGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPersian) {
                            "آمار شنیداری و کارنامه استودیویی شما"
                        } else {
                            "LISTENING STATS & STUDIO RECAP"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                }
                Text(
                    text = "100% LOCAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = FazoraEmeraldOk
                )
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatMetricBox(
                    value = "${stats.totalListeningMinutes}m",
                    label = if (isPersian) "زمان گوش دادن" else "Listen Time",
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    value = "${stats.totalPlaysCount}",
                    label = if (isPersian) "دفعات پخش" else "Total Plays",
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    value = "${stats.hiResTracksCount}",
                    label = if (isPersian) "قطعات Hi-Res" else "Hi-Res Masters",
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    value = "${stats.favoriteTracksCount}",
                    label = if (isPersian) "محبوب‌ها" else "Favorites",
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.topArtists.isNotEmpty() || stats.topTracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(PhiTokens.Space13))
                Surface(
                    color = FazoraCarbonElevated,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                        if (stats.topArtists.isNotEmpty()) {
                            val topArtistNames = stats.topArtists.joinToString(" • ") {
                                "${it.first} (${it.second})"
                            }
                            Text(
                                text = if (isPersian) {
                                    "🎤 هنرمندان برتر: $topArtistNames"
                                } else {
                                    "🎤 Top Artists: $topArtistNames"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = FazoraIvory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (stats.topTracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val topTrackNames = stats.topTracks.take(3).joinToString(" • ") {
                                "${it.first.title} (${it.second}x)"
                            }
                            Text(
                                text = if (isPersian) {
                                    "🔥 پرپخش‌ترین آهنگ‌ها: $topTrackNames"
                                } else {
                                    "🔥 Most Played: $topTrackNames"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraChampagneGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onOpenRuleBuilder,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FazoraGold,
                        contentColor = FazoraObsidian
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("open_smart_rule_builder_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPersian) "+ پلی‌لیست هوشمند قانون‌مند" else "+ Smart Rule Playlist")
                }

                OutlinedButton(
                    onClick = onOpenM3uManager,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("open_m3u_manager_button")
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = FazoraGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPersian) "ورودی / خروجی M3U8" else "M3U8 Import / Export", color = FazoraIvory)
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FazoraCarbonElevated,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier.border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FazoraGold
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
fun SmartRuleBuilderDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onCreateRule: (String, Int, Int, Int, Boolean, String) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var minBitrateInput by remember { mutableStateOf("320") }
    var minSampleRateInput by remember { mutableStateOf("44100") }
    var minPlaysInput by remember { mutableStateOf("0") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var artistKeyword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FazoraCarbonElevated,
        title = {
            Text(
                text = if (isPersian) {
                    "ساخت پلی‌لیست هوشمند قانون‌مند (Rule-Based)"
                } else {
                    "Create Rule-Based Smart Playlist"
                },
                color = FazoraIvory
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text(if (isPersian) "نام پلی‌لیست هوشمند" else "Smart Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("smart_rule_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minBitrateInput,
                        onValueChange = { minBitrateInput = it },
                        label = { Text(if (isPersian) "حداقل بیت‌ریت (kbps)" else "Min kbps") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minSampleRateInput,
                        onValueChange = { minSampleRateInput = it },
                        label = { Text(if (isPersian) "نرخ نمونه (Hz)" else "Min Hz") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minPlaysInput,
                        onValueChange = { minPlaysInput = it },
                        label = { Text(if (isPersian) "حداقل تعداد پخش" else "Min Plays") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = artistKeyword,
                        onValueChange = { artistKeyword = it },
                        label = { Text(if (isPersian) "فیلتر خواننده/عنوان" else "Artist/Title Filter") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { favoritesOnly = !favoritesOnly }
                ) {
                    Checkbox(
                        checked = favoritesOnly,
                        onCheckedChange = { favoritesOnly = it },
                        colors = CheckboxDefaults.colors(checkedColor = FazoraGold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "فقط آهنگ‌های نشان‌شده (علاقه‌مندی‌ها)" else "Starred Favorites Only",
                        color = FazoraIvory,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateRule(
                        ruleName.ifBlank { if (isPersian) "میکس هوشمند استودیو" else "Studio Smart Rule" },
                        minBitrateInput.toIntOrNull() ?: 0,
                        minSampleRateInput.toIntOrNull() ?: 0,
                        minPlaysInput.toIntOrNull() ?: 0,
                        favoritesOnly,
                        artistKeyword
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian),
                modifier = Modifier.testTag("confirm_smart_rule_button")
            ) {
                Text(if (isPersian) "ساخت پلی‌لیست" else "Build Smart Mix")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel", color = FazoraSteel)
            }
        }
    )
}

@Composable
fun M3u8ManagerDialog(
    playlists: List<Playlist>,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onExportM3u8: (Long) -> String,
    onImportM3u8: (String, String) -> Unit
) {
    var importName by remember { mutableStateOf("") }
    var m3uBuffer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FazoraCarbonElevated,
        title = {
            Text(
                text = if (isPersian) "تبادل پلی‌لیست استاندارد (.M3U / .M3U8)" else "M3U / M3U8 Playlist Manager",
                color = FazoraIvory
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                Text(
                    text = if (isPersian) {
                        "برای خروجی گرفتن روی یکی از پلی‌لیست‌ها بزنید یا محتوای M3U8 را برای وارد کردن جای‌گذاری کنید:"
                    } else {
                        "Tap a playlist to generate its .M3U8 text or paste an M3U8 file below to import:"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FazoraSteel
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { m3uBuffer = onExportM3u8(-1L) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = FazoraGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPersian) "خروجی کل کتابخانه" else "Export All Tracks", color = FazoraGold)
                    }
                    playlists.firstOrNull()?.let { firstPl ->
                        OutlinedButton(
                            onClick = { m3uBuffer = onExportM3u8(firstPl.id) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Export '${firstPl.name}'", color = FazoraIvory)
                        }
                    }
                }

                OutlinedTextField(
                    value = importName,
                    onValueChange = { importName = it },
                    label = { Text(if (isPersian) "نام پلی‌لیست ورودی" else "New Playlist Name (for Import)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("m3u_import_name_input")
                )

                OutlinedTextField(
                    value = m3uBuffer,
                    onValueChange = { m3uBuffer = it },
                    label = { Text("#EXTM3U / Track paths or URIs") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("m3u_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (m3uBuffer.isNotBlank()) {
                        onImportM3u8(
                            importName.ifBlank { "M3U8 Studio List" },
                            m3uBuffer
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian),
                modifier = Modifier.testTag("confirm_m3u_import_button")
            ) {
                Text(if (isPersian) "وارد کردن پلی‌لیست M3U8" else "Import M3U8")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "بستن" else "Close", color = FazoraSteel)
            }
        }
    )
}

@Composable
fun InteractiveLrcTimestampStudioDialog(
    lyrics: List<SyncedLyricLine>,
    currentPositionMs: Long,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onStampLine: (Int) -> Unit,
    onAddNewLineAtPos: (String) -> Unit,
    onExportLrc: () -> String
) {
    var newLyricText by remember { mutableStateOf("") }
    var exportedLrcPreview by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FazoraCarbonElevated,
        title = {
            Text(
                text = if (isPersian) {
                    "استودیو هماهنگ‌سازی متن ترانه (.LRC Timestamping)"
                } else {
                    "In-App Synced .LRC Timestamp Studio"
                },
                color = FazoraIvory
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                Text(
                    text = if (isPersian) {
                        "زمان فعلی پخش: ${Track.formatDurationMs(currentPositionMs)} (${currentPositionMs}ms)\n" +
                            "روی دکمه ⏱️ کنار هر خط بزنید تا زمان فعلی آهنگ روی آن خط قفل شود:"
                    } else {
                        "Current Playback Position: ${Track.formatDurationMs(currentPositionMs)} (${currentPositionMs}ms)\n" +
                            "Tap ⏱️ Stamp next to any line as the vocalist sings it:"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FazoraGold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newLyricText,
                        onValueChange = { newLyricText = it },
                        label = { Text(if (isPersian) "افزودن مصرع جدید در این ثانیه..." else "Add lyric line at current ms...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("lrc_studio_new_line_input")
                    )
                    Button(
                        onClick = {
                            if (newLyricText.isNotBlank()) {
                                onAddNewLineAtPos(newLyricText)
                                newLyricText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }

                lyrics.take(6).forEachIndexed { index, line ->
                    Surface(
                        color = FazoraCarbon,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FazoraIvory,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = Track.formatDurationMs(line.timestampMs) + " (${line.timestampMs}ms)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VeloraChampagneGold
                                )
                            }
                            OutlinedButton(
                                onClick = { onStampLine(index) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("stamp_lyric_line_$index")
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = FazoraGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPersian) "ثبت زمان" else "Stamp", color = FazoraGold)
                            }
                        }
                    }
                }

                if (exportedLrcPreview != null) {
                    OutlinedTextField(
                        value = exportedLrcPreview.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Generated .LRC Output") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { exportedLrcPreview = onExportLrc() },
                colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian),
                modifier = Modifier.testTag("export_lrc_studio_button")
            ) {
                Text(if (isPersian) "تولید و ذخیره فایل .LRC" else "Generate & Save .LRC")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "بستن" else "Done", color = FazoraSteel)
            }
        }
    )
}

@Composable
fun VideoSubtitleOverlay(
    lyrics: List<SyncedLyricLine>,
    positionMs: Long,
    offsetMs: Long,
    cinemaSettings: VideoCinemaSettings,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) return
    val activeIdx = VeloraLyricsAndSubtitleEngine.findActiveLineIndex(lyrics, positionMs, offsetMs)
    val line = lyrics.getOrNull(activeIdx) ?: return
    val displayText = if (isPersian && line.translationFa.isNotBlank()) line.translationFa else line.text

    val fontSizeSp = (15f * cinemaSettings.subtitleFontScale.multiplier).sp
    val textColor = Color(cinemaSettings.subtitleColorTheme.textHex)
    val bgColor = Color(cinemaSettings.subtitleColorTheme.bgHex)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("video_styled_subtitle_overlay")
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = fontSizeSp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun VideoSubtitleStylePickerSection(
    cinemaSettings: VideoCinemaSettings,
    isPersian: Boolean,
    onSelectFontScale: (SubtitleFontScale) -> Unit,
    onSelectColorTheme: (SubtitleColorTheme) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
        Text(
            text = if (isPersian) "اندازه فونت زیرنویس:" else "Subtitle Font Scale:",
            style = MaterialTheme.typography.labelLarge,
            color = FazoraGold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            SubtitleFontScale.entries.forEach { scale ->
                val selected = cinemaSettings.subtitleFontScale == scale
                OutlinedButton(
                    onClick = { onSelectFontScale(scale) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) FazoraGold else FazoraCarbon,
                        contentColor = if (selected) FazoraObsidian else FazoraIvory
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isPersian) scale.labelFa else scale.labelEn,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isPersian) "رنگ و کنتراست زیرنویس:" else "Subtitle Color Theme:",
            style = MaterialTheme.typography.labelLarge,
            color = FazoraGold
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SubtitleColorTheme.entries.forEach { theme ->
                val selected = cinemaSettings.subtitleColorTheme == theme
                Surface(
                    color = if (selected) FazoraGold.copy(alpha = 0.22f) else FazoraCarbon,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (selected) FazoraGold else FazoraCarbonBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectColorTheme(theme) }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isPersian) theme.labelFa else theme.labelEn,
                            color = Color(theme.textHex),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selected) {
                            Text("✓", color = FazoraGoldBright, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
