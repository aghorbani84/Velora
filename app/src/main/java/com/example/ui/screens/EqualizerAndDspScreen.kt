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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
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
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import java.util.Locale

@Composable
internal fun EqualizerAndStudioDspSection(
    uiState: FazoraUiState,
    isPersian: Boolean,
    onSetEqEnabled: (Boolean) -> Unit,
    onSetEqBandGain: (Int, Float) -> Unit,
    onApplyPreset: (EqPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onSetPreampDb: (Float) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetStereoWidth: (Int) -> Unit,
    onSetCrossfadeSec: (Int) -> Unit,
    onSetLoopPointA: () -> Unit,
    onSetLoopPointB: () -> Unit,
    onClearLoop: () -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit
) {
    val engine = uiState.engineState
    val dsp = uiState.studioDspState
    val autoHeadroomDb = remember(engine.eqBandGainsDb) {
        BiquadEqDsp.computeOptimalAutoPreampDb(engine.eqBandGainsDb)
    }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    if (showSavePresetDialog) {
        SaveEqPresetDialog(
            isPersian = isPersian,
            onDismiss = { showSavePresetDialog = false },
            onSaveCustomPreset = onSaveCustomPreset
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21)) {
        VeloraSectionCard(
            eyebrow = "10-BAND BIQUAD DSP EQUALIZER",
            title = if (isPersian) "اکولایزر ۱۰ باندی استودیویی" else "10-Band Parametric Equalizer",
            subtitle = String.format(
                Locale.US,
                "Cascaded DF-II IIR (Q = 1.414) • Auto-Headroom: %+.1f dB",
                autoHeadroomDb
            ),
            modifier = Modifier.testTag("eq_rack_card"),
            headerAction = {
                Switch(
                    checked = engine.eqEnabled,
                    onCheckedChange = onSetEqEnabled,
                    modifier = Modifier.testTag("eq_enable_switch")
                )
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                VeloraSecondaryButton(
                    text = if (isPersian) "تنظیم صاف (Flat)" else "Flat",
                    icon = Icons.Default.Restore,
                    onClick = { onApplyPreset(EqPreset.FLAT) },
                    tag = "eq_reset_flat_button"
                )
                VeloraPrimaryButton(
                    text = if (isPersian) "ذخیره پریست" else "Save Preset",
                    icon = Icons.Default.BookmarkAdd,
                    onClick = { showSavePresetDialog = true },
                    tag = "eq_save_preset_button"
                )
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space8))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(uiState.eqPresets, key = { it.id }) { preset ->
                    val isSelected = uiState.activeEqPresetId == preset.id
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VeloraSecondaryButton(
                            text = preset.name,
                            selected = isSelected,
                            onClick = { onApplyPreset(preset) },
                            tag = "eq_preset_chip_${preset.name.lowercase().replace(' ', '_')}"
                        )
                        if (!preset.isBuiltIn) {
                            IconButton(
                                onClick = { onDeleteCustomPreset(preset.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Preset ${preset.name}",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(PhiTokens.IconSize16)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space13))

            EqSpecification.CENTER_FREQUENCIES_HZ.forEachIndexed { index, _ ->
                val label = EqSpecification.BAND_LABELS[index]
                val gain = engine.eqBandGainsDb.getOrElse(index) { 0f }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${label}Hz",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(PhiTokens.Space55)
                    )
                    Slider(
                        value = gain,
                        onValueChange = { onSetEqBandGain(index, it) },
                        valueRange = EqSpecification.MIN_GAIN_DB..EqSpecification.MAX_GAIN_DB,
                        enabled = engine.eqEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_band_slider_$index")
                    )
                    Text(
                        text = String.format(Locale.US, "%+.1fdB", gain),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .width(PhiTokens.Space55)
                            .testTag("eq_band_value_$index")
                    )
                }
            }
        }

        VeloraSectionCard(
            eyebrow = "STUDIO DSP RACK",
            title = if (isPersian) "پردازشگر پیشرفته صدا و حلقه A-B" else "Studio DSP & A-B Loop Rack",
            subtitle = "Preamp: ${dsp.preampGainDb}dB • Bass: ${dsp.bassBoostPercent}% • Width: ${dsp.stereoWidthPercent}%",
            modifier = Modifier.testTag("studio_dsp_rack_card")
        ) {
            Text(
                text = if (isPersian) "پیش‌تقویت‌کننده (Preamp)" else "Preamp Gain (${dsp.preampGainDb} dB)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = dsp.preampGainDb,
                onValueChange = onSetPreampDb,
                valueRange = -12f..12f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dsp_preamp_slider")
            )
            Text(
                text = if (isPersian) "تقویت ساب‌بیس (${dsp.bassBoostPercent}%)" else "Sub-Bass Harmonic Boost (${dsp.bassBoostPercent}%)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = dsp.bassBoostPercent.toFloat(),
                onValueChange = { onSetBassBoost(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dsp_bass_boost_slider")
            )
            Text(
                text = if (isPersian) "پهنای استریو سه‌بعدی (${dsp.stereoWidthPercent}%)" else "3D Stereo Width (${dsp.stereoWidthPercent}%)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = dsp.stereoWidthPercent.toFloat(),
                onValueChange = { onSetStereoWidth(it.toInt()) },
                valueRange = 0f..200f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dsp_stereo_width_slider")
            )

            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                VeloraSecondaryButton(
                    text = "Set A",
                    onClick = onSetLoopPointA,
                    tag = "dsp_ab_set_a_button"
                )
                VeloraSecondaryButton(
                    text = "Set B",
                    onClick = onSetLoopPointB,
                    tag = "dsp_ab_set_b_button"
                )
                VeloraSecondaryButton(
                    text = if (isPersian) "حذف حلقه" else "Clear A-B",
                    onClick = onClearLoop,
                    tag = "dsp_ab_clear_button"
                )
            }

            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                items(listOf(0, 3, 6, 12)) { sec ->
                    VeloraSecondaryButton(
                        text = "Crossfade ${sec}s",
                        selected = dsp.crossfadeSeconds == sec,
                        onClick = { onSetCrossfadeSec(sec) },
                        tag = "dsp_crossfade_$sec"
                    )
                }
                items(listOf(0, 15, 30, 60)) { mins ->
                    VeloraSecondaryButton(
                        text = if (mins == 0) "Sleep Off" else "Sleep ${mins}m",
                        selected = (dsp.sleepTimerRemainingSec / 60) == mins,
                        onClick = { onSetSleepTimerMinutes(mins) },
                        tag = "dsp_sleep_timer_$mins"
                    )
                }
            }
        }
    }
}
