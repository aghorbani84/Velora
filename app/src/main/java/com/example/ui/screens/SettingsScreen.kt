package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import com.example.domain.model.AdaptiveAudioMode
import com.example.domain.model.ExperienceTier
import com.example.domain.model.ThemePaletteMode
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState

@Composable
fun SettingsScreen(
    uiState: FazoraUiState,
    onSetAutoHideMs: (Long) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onSetLanguageCode: (String) -> Unit = {},
    onTogglePlugin: (String) -> Unit = {},
    onExportBackupJson: () -> String = { "" },
    onRestoreBackupJson: (String) -> Unit = {},
    onToggleSimpleMode: (Boolean) -> Unit = {},
    onSetExperienceTier: (ExperienceTier) -> Unit = { tier ->
        onToggleSimpleMode(tier == ExperienceTier.SIMPLE)
    },
    onToggleAutoScanOnStartup: (Boolean) -> Unit = {},
    onAutoScanMedia: () -> Unit = {},
    onToggleOnlyValidMusic: (Boolean) -> Unit = {},
    onToggleBackgroundVideoAudio: (Boolean) -> Unit = {},
    onToggleLowBatteryMode: (Boolean) -> Unit = {},
    onSetAdaptiveAudioMode: (AdaptiveAudioMode) -> Unit = {},
    onSetThemePaletteMode: (ThemePaletteMode) -> Unit = {},
    onToggleIncludeDemoSamples: (Boolean) -> Unit = {},
    onPurgeDemoSamples: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val isPersian = settings.appLanguageCode == "fa"
    val tier = settings.experienceTier
    var backupJsonText by remember { mutableStateOf("") }

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

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "سطح تجربه کاربری (فوری)" else "EXPERIENCE TIER (INSTANT SWITCH)",
                title = if (isPersian) "انتخاب سطح پیچیدگی رابط کاربری" else "Interface Complexity Tier",
                subtitle = if (isPersian) {
                    "تغییر آنی بدون نیاز به راه‌اندازی مجدد برنامه"
                } else {
                    "Switch immediately between Simple, Standard, and Studio Pro without restart"
                }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExperienceTier.entries.forEach { option ->
                        VeloraSecondaryButton(
                            text = if (isPersian) option.labelFa else option.labelEn,
                            icon = Icons.Default.Tune,
                            selected = tier == option,
                            onClick = { onSetExperienceTier(option) },
                            tag = "tier_option_${option.name}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "پوسته و زبان" else "THEME & TYPOGRAPHY",
                title = if (isPersian) "تم رنگی و زبان برنامه" else "Color Theme & Language",
                subtitle = if (isPersian) "پشتیبانی کامل از فارسی راست‌به‌چپ (وزیرمتن)" else "WCAG 2.1 AA/AAA verified palettes & bilingual RTL/LTR"
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VeloraSecondaryButton(
                        text = "English (LTR)",
                        icon = Icons.Default.Language,
                        selected = !isPersian,
                        onClick = { onSetLanguageCode("en") },
                        tag = "language_option_en",
                        modifier = Modifier.weight(1f)
                    )
                    VeloraSecondaryButton(
                        text = "فارسی (راست‌به‌چپ)",
                        icon = Icons.Default.Language,
                        selected = isPersian,
                        onClick = { onSetLanguageCode("fa") },
                        tag = "language_option_fa",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(PhiTokens.Space13))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(ThemePaletteMode.entries) { palette ->
                        VeloraSecondaryButton(
                            text = if (isPersian) palette.labelFa else palette.labelEn,
                            icon = Icons.Default.Palette,
                            selected = settings.themePaletteMode == palette,
                            onClick = { onSetThemePaletteMode(palette) },
                            tag = "theme_palette_${palette.name}"
                        )
                    }
                }
            }
        }

        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "موتور صوتی و اسکن" else "AUDIO & PLAYBACK ENGINE",
                title = if (isPersian) "تنظیمات پخش، اسکن و مصرف باتری" else "Audio Route, Scanning & Power",
                subtitle = "Active Route: ${uiState.audioRoute.deviceName} (${uiState.audioRoute.sampleRateHz}Hz)"
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(AdaptiveAudioMode.entries) { mode ->
                        VeloraSecondaryButton(
                            text = if (isPersian) mode.labelFa else mode.labelEn,
                            selected = settings.adaptiveAudioMode == mode,
                            onClick = { onSetAdaptiveAudioMode(mode) },
                            tag = "adaptive_audio_${mode.name}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(PhiTokens.Space13))

                SettingsToggleRow(
                    label = if (isPersian) "اسکن خودکار هنگام ورود" else "Auto-Scan Device Media on Launch",
                    checked = settings.autoScanOnStartup,
                    onCheckedChange = onToggleAutoScanOnStartup,
                    tag = "settings_auto_scan_switch"
                )
                SettingsToggleRow(
                    label = if (isPersian) "فقط آهنگ‌های معتبر در جستجو" else "Valid Songs Only (Filter Short Clips)",
                    checked = settings.onlyValidMusicInSearch,
                    onCheckedChange = onToggleOnlyValidMusic,
                    tag = "settings_valid_music_switch"
                )
                SettingsToggleRow(
                    label = if (isPersian) "پخش صدای ویدیو در پس‌زمینه" else "Background Video Audio Playback",
                    checked = settings.backgroundVideoAudioEnabled,
                    onCheckedChange = onToggleBackgroundVideoAudio,
                    tag = "settings_bg_video_audio_switch"
                )
                SettingsToggleRow(
                    label = if (isPersian) "حالت ذخیره باتری (کاهش بار DSP)" else "Low-Battery DSP Saver Mode",
                    checked = settings.lowBatteryModeEnabled,
                    onCheckedChange = onToggleLowBatteryMode,
                    tag = "settings_low_battery_switch"
                )
                SettingsToggleRow(
                    label = if (isPersian) "نمایش ترک‌های نمونه آزمایشی" else "Include Bundled Studio Demo Samples",
                    checked = settings.includeDemoSamples,
                    onCheckedChange = onToggleIncludeDemoSamples,
                    tag = "settings_demo_samples_switch"
                )

                Spacer(modifier = Modifier.height(PhiTokens.Space8))

                Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    VeloraPrimaryButton(
                        text = if (isPersian) "اسکن مجدد حافظه" else "Scan Device Now",
                        icon = Icons.Default.Refresh,
                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                        tag = "settings_scan_now_button",
                        modifier = Modifier.weight(1f)
                    )
                    VeloraSecondaryButton(
                        text = if (isPersian) "حذف ترک‌های نمونه" else "Purge Demos",
                        icon = Icons.Default.DeleteSweep,
                        onClick = onPurgeDemoSamples,
                        tag = "purge_demo_samples_button",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (tier == ExperienceTier.PRO) {
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Text(
                        text = if (isPersian) {
                            "زمان مخفی شدن کنترل‌های ویدیو: ${settings.autoHideVideoControlsMs}ms"
                        } else {
                            "Video Controls Auto-Hide Timeout: ${settings.autoHideVideoControlsMs}ms"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = settings.autoHideVideoControlsMs.toFloat(),
                        onValueChange = { onSetAutoHideMs(it.toLong()) },
                        valueRange = 1500f..15000f,
                        modifier = Modifier.testTag("video_auto_hide_slider")
                    )
                }
            }
        }

        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "همگام‌سازی ابری و پشتیبان‌گیری" else "CLOUD CONTINUITY & BACKUP",
                title = if (isPersian) "حساب ابری و پشتیبان JSON" else "Cloud Sync & Offline JSON Backup",
                modifier = Modifier.testTag("firebase_sync_card")
            ) {
                val user = uiState.firebaseUser
                if (user != null) {
                    Text(
                        text = "Signed in: ${user.displayName ?: user.email ?: user.uid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    VeloraSecondaryButton(
                        text = if (isPersian) "خروج از حساب" else "Sign Out",
                        icon = Icons.Default.CloudDone,
                        onClick = onSignOut,
                        tag = "google_sign_out_button"
                    )
                } else {
                    VeloraPrimaryButton(
                        text = if (isPersian) "ورود با حساب گوگل" else "Sign In with Google",
                        icon = Icons.Default.CloudQueue,
                        onClick = onSignInWithGoogle,
                        enabled = !uiState.isSigningIn,
                        tag = "google_sign_in_button"
                    )
                }

                Spacer(modifier = Modifier.height(PhiTokens.Space13))

                Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    VeloraSecondaryButton(
                        text = if (isPersian) "خروجی پشتیبان" else "Export Backup",
                        icon = Icons.Default.SaveAlt,
                        onClick = { backupJsonText = onExportBackupJson() },
                        tag = "export_backup_button",
                        modifier = Modifier.weight(1f)
                    )
                    VeloraSecondaryButton(
                        text = if (isPersian) "بازگردانی" else "Restore Backup",
                        icon = Icons.Default.Restore,
                        onClick = {
                            if (backupJsonText.isNotBlank()) {
                                onRestoreBackupJson(backupJsonText)
                            }
                        },
                        tag = "import_backup_button",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (backupJsonText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        label = { Text("Backup JSON Payload") },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_json_input")
                    )
                }
            }
        }

        if (tier == ExperienceTier.PRO) {
            item {
                VeloraSectionCard(
                    eyebrow = if (isPersian) "پلاگین‌های سندباکس" else "DECLARATIVE PLUGINS",
                    title = if (isPersian) {
                        "اکوسیستم پلاگین‌ها (${uiState.declarativePlugins.size})"
                    } else {
                        "Sandboxed Plugins (${uiState.declarativePlugins.size})"
                    }
                ) {
                    uiState.declarativePlugins.forEach { plugin ->
                        SettingsToggleRow(
                            label = "${plugin.name} (${plugin.version})",
                            checked = plugin.isEnabled,
                            onCheckedChange = { onTogglePlugin(plugin.id) },
                            tag = "plugin_toggle_${plugin.id}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
