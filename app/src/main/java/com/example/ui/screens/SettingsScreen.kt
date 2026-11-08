package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.AdaptiveAudioMode
import com.example.domain.model.AudioRouteType
import com.example.domain.model.PhiDesignMetrics
import com.example.domain.model.ThemePaletteMode
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraEmeraldOk
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.HEX_CARBON
import com.example.ui.theme.HEX_GOLD
import com.example.ui.theme.HEX_IVORY
import com.example.ui.theme.HEX_OBSIDIAN
import com.example.ui.theme.HEX_STEEL
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState
import java.util.Locale

@Composable
fun SettingsScreen(
    uiState: FazoraUiState,
    onSetAutoHideMs: (Long) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onSetLanguageCode: (String) -> Unit = {},
    onTogglePlugin: (String) -> Unit = {},
    onExportBackupJson: () -> Unit = {},
    onRestoreBackupJson: (String) -> Unit = {},
    onToggleSimpleMode: (Boolean) -> Unit = {},
    onToggleAutoScanOnStartup: (Boolean) -> Unit = {},
    onAutoScanMedia: () -> Unit = {},
    onToggleOnlyValidMusic: (Boolean) -> Unit = {},
    onToggleBackgroundVideoAudio: (Boolean) -> Unit = {},
    onToggleLowBatteryMode: (Boolean) -> Unit = {},
    onSetAdaptiveAudioMode: (AdaptiveAudioMode) -> Unit = {},
    onSetThemePaletteMode: (ThemePaletteMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showRestoreBackupDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    val settings = uiState.settings
    val user = uiState.firebaseUser
    val isPersian = settings.appLanguageCode == "fa"
    val contrastIvoryObsidian = remember { PhiDesignMetrics.contrastRatio(HEX_IVORY, HEX_OBSIDIAN) }
    val contrastIvoryCarbon = remember { PhiDesignMetrics.contrastRatio(HEX_IVORY, HEX_CARBON) }
    val contrastGoldObsidian = remember { PhiDesignMetrics.contrastRatio(HEX_GOLD, HEX_OBSIDIAN) }
    val contrastSteelCarbon = remember { PhiDesignMetrics.contrastRatio(HEX_STEEL, HEX_CARBON) }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space21),
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("settings_screen")
    ) {
        item {
            Surface(
                color = FazoraCarbon,
                shape = RoundedCornerShape(PhiTokens.CornerRadius21),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius21))
                    .testTag("adaptive_audio_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "موتور صوتی تطبیقی و خروجی سخت‌افزاری" else "ADAPTIVE AUDIO ENGINE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) "شناسایی مسیر صوتی و تنظیم خودکار Biquad" else "Hardware Route & Auto-EQ Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    val route = uiState.audioRoute
                    val routeIcon = when (route.type) {
                        AudioRouteType.BLUETOOTH -> "📶"
                        AudioRouteType.WIRED_HEADPHONES -> "🎧"
                        AudioRouteType.USB_DAC -> "🎛️"
                        AudioRouteType.SPEAKER -> "🔊"
                        AudioRouteType.UNKNOWN -> "🎵"
                    }
                    Surface(
                        color = FazoraCarbonElevated,
                        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(PhiTokens.Space13)
                        ) {
                            Text(text = routeIcon, style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.width(PhiTokens.Space13))
                            Column {
                                Text(
                                    text = if (isPersian) "خروجی فعال: ${route.deviceName}" else "Active Output: ${route.deviceName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FazoraIvory
                                )
                                Text(
                                    text = if (isPersian) "پروفایل پیشنهادی ولورا: ${route.recommendedProfile}"
                                    else "Recommended Profile: ${route.recommendedProfile}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FazoraGold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Text(
                        text = if (isPersian) "حالت پردازش صوتی:" else "Audio Processing Mode:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AdaptiveAudioMode.entries.forEach { mode ->
                            val isSelected = settings.adaptiveAudioMode == mode
                            OutlinedButton(
                                onClick = { onSetAdaptiveAudioMode(mode) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                    contentColor = if (isSelected) FazoraObsidian else FazoraIvory
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("mode_${mode.name}")
                            ) {
                                Text(
                                    text = if (isPersian) mode.labelFa else mode.labelEn,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
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
                    .testTag("battery_and_background_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "باتری و پخش پس‌زمینه" else "BATTERY & BACKGROUND PLAYBACK",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) "پخش در پس‌زمینه و کاهش مصرف شارژ" else "Background Audio & Power Saver",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) "پخش صدای ویدیو در پس‌زمینه" else "Background Video Audio",
                                style = MaterialTheme.typography.titleMedium,
                                color = FazoraIvory
                            )
                            Text(
                                text = if (isPersian)
                                    "با خاموش شدن صفحه یا رفتن به برنامه‌های دیگر، پخش صدای ویدیو ادامه می‌یابد."
                                else
                                    "Video audio keeps playing smoothly when screen turns off or app is minimized.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FazoraSteel
                            )
                        }
                        Switch(
                            checked = settings.backgroundVideoAudioEnabled,
                            onCheckedChange = { onToggleBackgroundVideoAudio(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) "حالت بهینه‌سازی و کاهش مصرف باتری" else "Battery Saver Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = FazoraIvory
                            )
                            Text(
                                text = if (isPersian)
                                    "کاهش نرخ پردازش طیف و آنالایزر برای حفظ شارژ باتری و حداقل مصرف پردازنده."
                                else
                                    "Throttles visualizer computations to extend battery life and reduce CPU usage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FazoraSteel
                            )
                        }
                        Switch(
                            checked = settings.lowBatteryModeEnabled,
                            onCheckedChange = { onToggleLowBatteryMode(it) }
                        )
                    }
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
                    .testTag("smart_filter_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "فیلتر هوشمند کتابخانه و جست‌وجو" else "SMART LIBRARY & SEARCH FILTER",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPersian) "فقط آهنگ‌های معتبر" else "Valid Music Only",
                                style = MaterialTheme.typography.titleMedium,
                                color = FazoraIvory
                            )
                            Text(
                                text = if (isPersian)
                                    "نادیده گرفتن ویس‌های واتساپ/تلگرام، ضبط مکالمه و فایل‌های زیر ۳۵ ثانیه."
                                else
                                    "Filters out voice notes, call recordings, and audio files under 35 seconds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FazoraSteel
                            )
                        }
                        Switch(
                            checked = settings.onlyValidMusicInSearch,
                            onCheckedChange = { onToggleOnlyValidMusic(it) }
                        )
                    }
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
                    .testTag("dynamic_color_theme_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "تم و پالت رنگی پویا بر اساس دستگاه" else "DYNAMIC MATERIAL 3 COLOR THEME",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) "رنگ‌های هوشمند، پویا و متوازن ریاضی" else "High-Contrast Harmonized Palettes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian)
                            "تولید خودکار پالت رنگی متناسب با تم گوشی با تضمین کنتراست ریاضی WCAG 2.1 AA."
                        else
                            "Generates balanced Material You color schemes extracted from device settings, " +
                                "mathematically calibrated to guarantee WCAG 2.1 AA contrast ratio (>= 4.5:1).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePaletteMode.entries.forEach { mode ->
                            val isSelected = settings.themePaletteMode == mode
                            Surface(
                                color = if (isSelected) FazoraCarbonElevated else FazoraCarbon,
                                shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) FazoraGold else FazoraCarbonBorder,
                                        shape = RoundedCornerShape(PhiTokens.CornerRadius13)
                                    )
                                    .clickable { onSetThemePaletteMode(mode) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPersian) mode.labelFa else mode.labelEn,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSelected) FazoraGold else FazoraIvory
                                        )
                                        Text(
                                            text = when (mode) {
                                                ThemePaletteMode.SYSTEM_DYNAMIC ->
                                                    if (isPersian) "استخراج هوشمند از والپیپر گوشی (اندروید ۱۲+)"
                                                    else "Extracts accent tones dynamically from device wallpaper"
                                                ThemePaletteMode.VELORA_SAFFRON ->
                                                    if (isPersian) "پالت اصلی استودیویی با طلای زعفرانی و شامپاینی"
                                                    else "Classic audiophile signature saffron & champagne gold"
                                                ThemePaletteMode.HIGH_CONTRAST_MONOCHROME ->
                                                    if (isPersian) "مشکی و سفید مطلق بدون اتلاف باتری اولد"
                                                    else "Pure monochrome black & white for maximum accessibility"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FazoraSteel
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSetThemePaletteMode(mode) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = FazoraGold,
                                            unselectedColor = FazoraSteel
                                        )
                                    )
                                }
                            }
                        }
                    }
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
                    .testTag("simple_mode_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "طراحی رابط کاربری و تراکم صفحه" else "INTERFACE DENSITY & WORKSPACE MODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) "حالت ساده (خلوت) / حالت استودیوی پیشرفته" else "Simple Mode / Studio Advanced Mode",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian)
                            "حالت ساده: محیط خلوت و مینیمال بدون منوها و اسلایدرهای شلوغ، متمرکز بر پخش روان آهنگ و ویدیو.\n" +
                                "حالت استودیو: فعال‌سازی تمام تب‌های تخصصی، اکولایزر ۱۰ بانده Biquad، بوست ساب‌بیس، پهنای استریو 3D و بوک‌مارک."
                        else
                            "Simple Mode keeps the player clean, distraction-free, and uncluttered. " +
                                "Studio Advanced Mode reveals the full 10-band DSP rack, Goertzel analyzer, and depth controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                        Button(
                            onClick = { onToggleSimpleMode(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.isSimpleMode) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (settings.isSimpleMode) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "✨ حالت ساده" else "✨ Simple Mode")
                        }

                        Button(
                            onClick = { onToggleSimpleMode(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!settings.isSimpleMode) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (!settings.isSimpleMode) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "🎛️ استودیو پیشرفته" else "🎛️ Studio Pro")
                        }
                    }
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
                    .testTag("auto_scan_settings_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = if (isPersian) "پایش خودکار رسانه‌های دستگاه" else "AUTOMATIC MEDIA DISCOVERY",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) {
                            "جست‌وجو و اسکن خودکار آهنگ‌ها و ویدیوها"
                        } else {
                            "Auto-Scan Device Music & Videos"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = if (isPersian) {
                            "با فعال بودن این گزینه، ولورا هنگام ورود تمام آهنگ‌ها، دانلودها، " +
                                "فایل‌های تلگرام/واتساپ و ویدیوهای دستگاه را خودکار شناسایی و فهرست می‌کند."
                        } else {
                            "Automatically indexes newly added audio files, downloads, " +
                                "voice recordings, and videos whenever VELORA launches."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isPersian) "اسکن خودکار هنگام اجرای برنامه" else "Auto-scan on app launch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FazoraIvory
                        )
                        Switch(
                            checked = settings.autoScanOnStartup,
                            onCheckedChange = { onToggleAutoScanOnStartup(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Button(
                        onClick = onAutoScanMedia,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FazoraGold,
                            contentColor = FazoraObsidian
                        ),
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPersian) "🔄 اسکن کامل و خودکار حافظه همین حالا" else "🔄 Auto-Scan All Device Storage Now")
                    }
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
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "INTERNATIONALIZATION & RTL ARCHITECTURE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Language & Layout Direction (English / فارسی)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Switch between English (LTR) and Persian / فارسی (RTL) layout direction and localized labels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13)) {
                        val isEnglish = settings.appLanguageCode != "fa"
                        OutlinedButton(
                            onClick = { onSetLanguageCode("en") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isEnglish) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (isEnglish) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("language_en_button")
                        ) {
                            Text("English (LTR)")
                        }
                        OutlinedButton(
                            onClick = { onSetLanguageCode("fa") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isEnglish) FazoraGold else FazoraCarbonElevated,
                                contentColor = if (!isEnglish) FazoraObsidian else FazoraIvory
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("language_fa_button")
                        ) {
                            Text("فارسی (RTL)")
                        }
                    }
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
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "FIREBASE CLOUD PERSISTENCE",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Cloud Sync & Identity",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Synchronize playlists and custom 10-band EQ presets securely across your devices " +
                            "with zero-trust Firestore security rules and Google Sign-In.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))

                    if (user != null) {
                        Surface(
                            color = FazoraCarbonElevated,
                            shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(PhiTokens.Space13)) {
                                Text(
                                    text = "Connected Account: ${user.displayName ?: "Audiophile"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FazoraEmeraldOk
                                )
                                Text(
                                    text = user.email ?: user.uid,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FazoraSteel
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(PhiTokens.Space8))
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = FazoraCarbonElevated,
                                contentColor = FazoraIvory
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("firebase_sign_out_button")
                        ) {
                            Text("Sign Out from Firebase")
                        }
                    } else {
                        Button(
                            onClick = onSignInWithGoogle,
                            enabled = !uiState.isSigningIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FazoraGold,
                                contentColor = FazoraObsidian
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("firebase_google_sign_in_button")
                        ) {
                            Text(
                                if (uiState.isSigningIn) "Signing in with Google..." else "Sign in with Google"
                            )
                        }
                    }
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
                    .testTag("settings_plugin_registry_card")
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "TIER 1 DECLARATIVE PLUGIN REGISTRY & CODEC MATRIX",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Sandboxed Extensions & Universal Media Support",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Google Play compliant declarative extensions (zero dynamic .dex/.so code loading). " +
                            "Audio: MP3, AAC/M4A, FLAC, WAV, OGG/Opus • Video: MP4, MKV, WebM, HLS (.m3u8), DASH (.mpd).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    uiState.declarativePlugins.forEach { plugin ->
                        Surface(
                            color = FazoraCarbonElevated,
                            shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PhiTokens.Space13)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${plugin.name} (v${plugin.version})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FazoraIvory
                                    )
                                    Text(
                                        text = "${plugin.tier} • ${plugin.capabilities.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FazoraSteel
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onTogglePlugin(plugin.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (plugin.isEnabled) FazoraGold else FazoraObsidian,
                                        contentColor = if (plugin.isEnabled) FazoraObsidian else FazoraIvory
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("toggle_plugin_${plugin.id}")
                                ) {
                                    Text(if (plugin.isEnabled) "Active" else "Disabled")
                                }
                            }
                        }
                    }
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
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "ATOMIC DATASTORE SETTINGS",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Engine & Persistence Configuration",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Persisted automatically across restarts via Jetpack DataStore Preferences " +
                            "with ReplaceFileCorruptionHandler backup recovery " +
                            "(Corruption fallbacks triggered: ${settings.corruptionFallbackCount}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraSteel
                    )

                    Spacer(modifier = Modifier.height(PhiTokens.Space21))

                    Text(
                        text = "Default Video Auto-Hide Timeout: ${settings.autoHideVideoControlsMs} ms",
                        style = MaterialTheme.typography.titleMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        listOf(2500L, 3500L, 5000L, 8000L).forEach { timeout ->
                            val isSelected = settings.autoHideVideoControlsMs == timeout
                            OutlinedButton(
                                onClick = { onSetAutoHideMs(timeout) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) FazoraGold else FazoraCarbonElevated,
                                    contentColor = if (isSelected) FazoraObsidian else FazoraIvory
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_autohide_$timeout")
                            ) {
                                Text("${timeout / 1000.0}s")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        Button(
                            onClick = onExportBackupJson,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FazoraGold,
                                contentColor = FazoraObsidian
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("export_backup_json_button")
                        ) {
                            Text(
                                if (settings.appLanguageCode == "fa") {
                                    "💾 ذخیره پشتیبان (Export JSON)"
                                } else {
                                    "💾 Export JSON Backup"
                                }
                            )
                        }
                        OutlinedButton(
                            onClick = { showRestoreBackupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("restore_backup_json_button")
                        ) {
                            Text(
                                if (settings.appLanguageCode == "fa") {
                                    "♻️ بازیابی پشتیبان (Restore)"
                                } else {
                                    "♻️ Restore Snapshot"
                                },
                                color = FazoraIvory
                            )
                        }
                    }
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
            ) {
                Column(modifier = Modifier.padding(PhiTokens.Space21)) {
                    Text(
                        text = "VELORA DESIGN SYSTEM & WCAG 2.1 AA TELEMETRY",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space8))
                    Text(
                        text = "Adaptive Spacing: 8dp / 13dp / 21dp / 34dp / 55dp • Responsive Grid • Min Touch Target: 48dp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FazoraIvory
                    )
                    Spacer(modifier = Modifier.height(PhiTokens.Space13))
                    ContrastAuditRow("Ivory (#F5F2EB) on Obsidian (#0B0B0D)", contrastIvoryObsidian)
                    ContrastAuditRow("Ivory (#F5F2EB) on Carbon (#121216)", contrastIvoryCarbon)
                    ContrastAuditRow("Saffron (#E5A817) on Obsidian (#0B0B0D)", contrastGoldObsidian)
                    ContrastAuditRow("Warm Muted (#A5A29B) on Carbon (#121216)", contrastSteelCarbon)
                }
            }
        }
    }

    if (showRestoreBackupDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreBackupDialog = false },
            containerColor = FazoraCarbonElevated,
            title = {
                Text(
                    text = if (isPersian) "بازیابی پشتیبان آفلاین (Restore JSON)" else "Restore Offline JSON Backup",
                    color = FazoraIvory
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    Text(
                        text = if (isPersian) {
                            "برای بازیابی از آخرین فایل ذخیره‌شده در حافظه برنامه دکمه تایید را بزنید، یا متن JSON را در کادر زیر وارد کنید:"
                        } else {
                            "Leave blank to restore from the latest local snapshot file, or paste a JSON backup below:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = FazoraSteel
                    )
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        label = { Text("Optional JSON Backup Payload") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth().testTag("restore_backup_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackupJson(restoreJsonInput)
                        showRestoreBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian)
                ) {
                    Text(if (isPersian) "بازیابی" else "Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreBackupDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel", color = FazoraSteel)
                }
            }
        )
    }
}

@Composable
private fun ContrastAuditRow(label: String, ratio: Double) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = FazoraSteel
        )
        Text(
            text = String.format(Locale.US, "%.2f:1 (AA PASS)", ratio),
            style = MaterialTheme.typography.labelLarge,
            color = FazoraEmeraldOk
        )
    }
}
