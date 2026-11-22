package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.domain.model.AudioTrackInfo
import com.example.domain.model.SubtitleTrackInfo
import com.example.domain.model.SubtitleColorTheme
import com.example.domain.model.SubtitleFontScale
import com.example.domain.model.VideoCinemaSettings
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraAlphaTokens
import com.example.ui.theme.veloraColors
import com.example.ui.viewmodel.FazoraUiState

@Composable
fun VideoSubtitleOverlay(
    text: String,
    cinemaSettings: VideoCinemaSettings,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.veloraColors.bg.copy(alpha = VeloraAlphaTokens.SCRIM_BACKGROUND_ALPHA),
        shape = RoundedCornerShape(PhiTokens.CornerRadius8),
        modifier = modifier.testTag("video_subtitle_overlay")
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = when (cinemaSettings.subtitleColorTheme) {
                SubtitleColorTheme.SAFFRON_ON_DARK -> MaterialTheme.colorScheme.primary
                SubtitleColorTheme.YELLOW_HIGH_CONTRAST -> MaterialTheme.colorScheme.secondary
                SubtitleColorTheme.CYAN_STUDIO -> MaterialTheme.colorScheme.tertiary
                SubtitleColorTheme.WHITE_ON_DARK -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space4)
        )
    }
}

@Composable
internal fun VideoSubtitlesDialog(
    uiState: FazoraUiState,
    isPersian: Boolean,
    onSetSubtitlesEnabled: (Boolean) -> Unit,
    onSetSubtitleFontScale: (SubtitleFontScale) -> Unit,
    onSetSubtitleColorTheme: (SubtitleColorTheme) -> Unit,
    onDismiss: () -> Unit,
    subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    onSelectSubtitleTrack: (String?) -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (isPersian) "تنظیمات زیرنویس" else "Subtitles & Caption Styling") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.testTag("subtitle_style_picker_section")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isPersian) "نمایش زیرنویس" else "Enable Subtitles")
                    Switch(
                        checked = uiState.engineState.subtitlesEnabled,
                        onCheckedChange = onSetSubtitlesEnabled,
                        modifier = Modifier.testTag("video_subtitle_enable_switch")
                    )
                }
                if (subtitleTracks.isEmpty()) {
                    Text(
                        text = if (isPersian) {
                            "این ویدیو زیرنویس توکار ندارد (زیرنویس هماهنگ‌شده LRC/SRT فعال است)."
                        } else {
                            "No embedded tracks; synchronized LRC/SRT overlay is active."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    subtitleTracks.forEach { sub ->
                        VeloraSecondaryButton(
                            text = "${sub.label} (${sub.language})",
                            selected = sub.isSelected && uiState.engineState.subtitlesEnabled,
                            onClick = {
                                // Selecting a track also turns subtitles on: tapping a
                                // track used to only enable subtitles without selecting it.
                                onSelectSubtitleTrack(sub.id)
                                onSetSubtitlesEnabled(true)
                            },
                            tag = "subtitle_track_option_${sub.id}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                    modifier = Modifier.testTag("subtitle_font_scale_slider")
                ) {
                    items(SubtitleFontScale.entries) { scale ->
                        VeloraSecondaryButton(
                            text = if (isPersian) scale.labelFa else scale.labelEn,
                            selected = uiState.videoCinemaSettings.subtitleFontScale == scale,
                            onClick = { onSetSubtitleFontScale(scale) },
                            tag = "subtitle_scale_${scale.name}"
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(SubtitleColorTheme.entries) { theme ->
                        VeloraSecondaryButton(
                            text = theme.labelEn,
                            selected = uiState.videoCinemaSettings.subtitleColorTheme == theme,
                            onClick = { onSetSubtitleColorTheme(theme) },
                            tag = "subtitle_color_${theme.name}"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Done") }
        }
    )
}

@Composable
internal fun VideoAudioTrackDialog(
    audioTracks: List<AudioTrackInfo>,
    isPersian: Boolean,
    onSelectAudioTrack: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text(if (isPersian) "انتخاب ترک صوتی" else "Select Audio Track") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                if (audioTracks.isEmpty()) {
                    Text(
                        text = if (isPersian) "ترک صوتی پیش‌فرض فعال است." else "Default embedded audio stream active.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    audioTracks.forEach { track ->
                        VeloraSecondaryButton(
                            text = "${track.label} (${track.language})",
                            selected = track.isSelected,
                            onClick = {
                                onSelectAudioTrack(track.id)
                                onDismiss()
                            },
                            tag = "audio_track_option_${track.id}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isPersian) "بستن" else "Close") }
        }
    )
}
