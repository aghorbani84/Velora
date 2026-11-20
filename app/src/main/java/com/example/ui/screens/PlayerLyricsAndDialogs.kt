package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.domain.model.ExperienceTier
import com.example.domain.model.FullscreenPolicy
import com.example.domain.model.MusicControlGroup
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.TimeMachineBookmark
import com.example.domain.model.Track
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.components.formattedDuration
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.mixedScriptText
import com.example.ui.viewmodel.FazoraUiState

@Composable
internal fun SynchronizedLyricsAndBookmarksSection(
    lyrics: List<SyncedLyricLine>,
    positionMs: Long,
    lyricsOffsetMs: Long,
    bookmarks: List<TimeMachineBookmark>,
    tier: ExperienceTier,
    isPersian: Boolean,
    onAdjustOffset: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onAddBookmark: () -> Unit,
    onStampLyricLine: (Int) -> Unit,
    onAddNewLyricLineAtPos: (String) -> Unit,
    onExportLrc: () -> String
) {
    val activeIdx = VeloraLyricsAndSubtitleEngine.findActiveLineIndex(lyrics, positionMs, lyricsOffsetMs)
    var showLrcStudioDialog by remember { mutableStateOf(false) }

    if (showLrcStudioDialog && FullscreenPolicy.isMusicControlVisible(tier, MusicControlGroup.LRC_TIMESTAMP_STUDIO)) {
        InteractiveLrcTimestampStudioDialog(
            lyrics = lyrics,
            currentPositionMs = positionMs,
            isPersian = isPersian,
            onDismiss = { showLrcStudioDialog = false },
            onStampLine = onStampLyricLine,
            onAddNewLineAtPos = onAddNewLyricLineAtPos,
            onExportLrc = onExportLrc
        )
    }

    VeloraSectionCard(
        eyebrow = if (isPersian) "متن همگام آهنگ" else "SYNCHRONIZED LYRICS",
        title = if (isPersian) "متن آهنگ (آفست: ${lyricsOffsetMs}ms)" else "Synced Lyrics (Offset: ${lyricsOffsetMs}ms)",
        modifier = Modifier.testTag("synchronized_lyrics_card"),
        headerAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space4)) {
                if (FullscreenPolicy.isMusicControlVisible(tier, MusicControlGroup.LRC_TIMESTAMP_STUDIO)) {
                    VeloraPrimaryButton(
                        text = if (isPersian) "استودیو LRC" else "LRC Studio",
                        onClick = { showLrcStudioDialog = true },
                        tag = "open_lrc_timestamp_studio_button"
                    )
                }
                VeloraSecondaryButton(
                    text = "-250ms",
                    onClick = { onAdjustOffset(-250L) },
                    tag = "lyrics_offset_minus_button"
                )
                VeloraSecondaryButton(
                    text = "+250ms",
                    onClick = { onAdjustOffset(250L) },
                    tag = "lyrics_offset_plus_button"
                )
            }
        }
    ) {
        lyrics.forEachIndexed { idx, line ->
            val isCurrent = idx == activeIdx
            Surface(
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PhiTokens.Space2)
                    .border(
                        width = if (isCurrent) PhiTokens.BorderHairline else PhiTokens.Space2 - PhiTokens.Space2,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(PhiTokens.CornerRadius13)
                    )
                    .clickable { onSeekTo(line.timestampMs) }
                    .testTag("lyric_line_$idx")
            ) {
                Column(modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)) {
                    val lineText = if (isPersian && line.translationFa.isNotBlank()) line.translationFa else line.text
                    Text(
                        text = mixedScriptText(lineText),
                        style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (FullscreenPolicy.isMusicControlVisible(tier, MusicControlGroup.TIME_MACHINE_BOOKMARKS)) {
            Spacer(modifier = Modifier.height(PhiTokens.Space13))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPersian) "نشانک‌های زمانی (${bookmarks.size})" else "Bookmarks (${bookmarks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                VeloraSecondaryButton(
                    text = if (isPersian) "ثبت نشانک" else "Bookmark Pos",
                    icon = Icons.Default.BookmarkAdd,
                    onClick = onAddBookmark,
                    tag = "add_time_machine_bookmark_button"
                )
            }
            if (bookmarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(PhiTokens.Space8))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(bookmarks, key = { it.id }) { bm ->
                        VeloraSecondaryButton(
                            text = "${bm.formattedTimestamp} • ${bm.trackTitle}",
                            onClick = { onSeekTo(bm.timestampMs) },
                            tag = "bookmark_chip_${bm.id}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun QueueSectionCard(
    uiState: FazoraUiState,
    isPersian: Boolean,
    onSelectQueueTrack: (Track) -> Unit,
    onRemoveFromQueue: (String) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> }
) {
    val queueItems = uiState.queue.tracks
    VeloraSectionCard(
        eyebrow = if (isPersian) "صف پخش" else "UP NEXT QUEUE",
        title = if (isPersian) "صف پخش (${queueItems.size})" else "Playback Queue (${queueItems.size})",
        modifier = Modifier.testTag("queue_list_section")
    ) {
        queueItems.forEachIndexed { index, item ->
            val isCurrent = index == uiState.queue.currentIndex
            Surface(
                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PhiTokens.Space4)
                    .clickable { onSelectQueueTrack(item) }
                    .testTag("queue_item_$index")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mixedScriptText(item.title),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = mixedScriptText("${item.artist} • ${item.formattedDuration}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onMoveQueueItem(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("move_queue_up_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isPersian) "انتقال به بالا" else "Move Up",
                                tint = if (index > 0) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                        IconButton(
                            onClick = { onMoveQueueItem(index, index + 1) },
                            enabled = index < queueItems.lastIndex,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("move_queue_down_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isPersian) "انتقال به پایین" else "Move Down",
                                tint = if (index < queueItems.lastIndex) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                        IconButton(
                            onClick = { onRemoveFromQueue(item.id) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("remove_queue_item_$index")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove from Queue",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SaveEqPresetDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSaveCustomPreset: (String) -> Unit
) {
    var customPresetName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (isPersian) "ذخیره پریست اکولایزر" else "Save Custom 10-Band EQ Preset") },
        text = {
            OutlinedTextField(
                value = customPresetName,
                onValueChange = { customPresetName = it },
                label = { Text(if (isPersian) "نام پریست" else "Preset Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("eq_preset_name_input")
            )
        },
        confirmButton = {
            VeloraPrimaryButton(
                text = if (isPersian) "ذخیره" else "Save Preset",
                onClick = {
                    if (customPresetName.isNotBlank()) {
                        onSaveCustomPreset(customPresetName.trim())
                        onDismiss()
                    }
                },
                tag = "confirm_save_eq_preset_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}

@Composable
internal fun ExactSeekDialog(
    initialValue: String,
    effectivePositionMs: Long,
    durationMs: Long,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSeekTo: (Long) -> Unit
) {
    var exactSeekInput by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(if (isPersian) "پرش دقیق به زمان" else "Precision Exact Seek (ms or mm:ss)")
        },
        text = {
            OutlinedTextField(
                value = exactSeekInput,
                onValueChange = { exactSeekInput = it },
                label = { Text("e.g. 42500 or 01:25") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exact_seek_input")
            )
        },
        confirmButton = {
            VeloraPrimaryButton(
                text = if (isPersian) "پرش" else "Seek",
                onClick = {
                    val trimmed = exactSeekInput.trim()
                    val parsedMs = if (trimmed.contains(":")) {
                        val parts = trimmed.split(":")
                        val mins = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                        val secs = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                        (mins * 60_000L) + (secs * 1000L)
                    } else {
                        trimmed.toLongOrNull() ?: effectivePositionMs
                    }
                    onSeekTo(PlaybackQueue.clampSeekPosition(parsedMs, durationMs))
                    onDismiss()
                },
                tag = "confirm_exact_seek_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
        }
    )
}

@Composable
internal fun MetadataTagEditorDialog(
    track: Track,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onEditTrackMetadata: (String, String, String, String) -> Unit
) {
    var editTitle by remember(track.id) { mutableStateOf(track.title) }
    var editArtist by remember(track.id) { mutableStateOf(track.artist) }
    var editAlbum by remember(track.id) { mutableStateOf(track.album) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (isPersian) "ویرایش متادیتای آهنگ" else "Edit Track Metadata Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text(if (isPersian) "عنوان آهنگ" else "Track Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_edit_title_input")
                )
                OutlinedTextField(
                    value = editArtist,
                    onValueChange = { editArtist = it },
                    label = { Text(if (isPersian) "نام هنرمند" else "Artist") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_edit_artist_input")
                )
                OutlinedTextField(
                    value = editAlbum,
                    onValueChange = { editAlbum = it },
                    label = { Text(if (isPersian) "آلبوم" else "Album") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tag_edit_album_input")
                )
            }
        },
        confirmButton = {
            VeloraPrimaryButton(
                text = if (isPersian) "ذخیره تگ" else "Save Tags",
                onClick = {
                    onEditTrackMetadata(
                        track.id,
                        editTitle.ifBlank { track.title },
                        editArtist.ifBlank { track.artist },
                        editAlbum.ifBlank { track.album }
                    )
                    onDismiss()
                },
                tag = "confirm_save_tags_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "انصراف" else "Cancel") }
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
    var newLineText by remember { mutableStateOf("") }
    var exportedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (isPersian) "استودیو زمان‌بندی LRC" else "Interactive LRC Timestamp Studio") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.testTag("lrc_timestamp_studio_dialog")
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    OutlinedTextField(
                        value = newLineText,
                        onValueChange = { newLineText = it },
                        label = { Text(if (isPersian) "متن خط جدید" else "New Lyric Line") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    VeloraPrimaryButton(
                        text = "+",
                        onClick = {
                            if (newLineText.isNotBlank()) {
                                onAddNewLineAtPos(newLineText.trim())
                                newLineText = ""
                            }
                        },
                        tag = "add_lrc_line_button"
                    )
                }
                lyrics.take(4).forEachIndexed { idx, line ->
                    VeloraSecondaryButton(
                        text = "Stamp [${Track.formatDurationMs(currentPositionMs)}]: ${line.text}",
                        onClick = { onStampLine(idx) },
                        tag = "stamp_lrc_line_$idx",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (exportedText.isNotBlank()) {
                    Text(
                        text = exportedText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            VeloraPrimaryButton(
                text = if (isPersian) "خروجی LRC" else "Export LRC",
                onClick = { exportedText = onExportLrc() },
                tag = "export_lrc_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )
}
