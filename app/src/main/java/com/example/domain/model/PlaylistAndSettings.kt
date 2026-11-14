package com.example.domain.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class Playlist(
    val id: Long = 0L,
    val name: String,
    val createdAtMs: Long,
    val tracks: List<Track> = emptyList()
) {
    val trackCount: Int
        get() = tracks.size

    val totalDurationMs: Long
        get() = tracks.sumOf { it.durationMs }
}

enum class VideoAspectMode(val label: String) {
    FIT("Fit (Original)"),
    FILL("Fill (Crop)"),
    GOLDEN_PHI("Cinematic (21:9)"),
    SIXTEEN_NINE("16:9 Wide")
}

enum class AdaptiveAudioMode(val labelEn: String, val labelFa: String) {
    ORIGINAL("Original", "صدای اصلی (بدون دستکاری)"),
    HI_FI("Hi-Fi Pure", "های‌فای خالص (شفافیت بالا)"),
    ENHANCED("Enhanced DSP", "تقویت‌شده (اکولایزر و باس)"),
    ADAPTIVE_AI("Adaptive AI", "تطبیقی هوشمند (بر اساس هدفون/اسپیکر)")
}

enum class AudioRouteType(val labelEn: String, val labelFa: String) {
    BLUETOOTH("Bluetooth A2DP", "هدفون/اسپیکر بلوتوث"),
    WIRED_HEADPHONES("Wired Headphones", "هدفون سیمی ۳.۵mm"),
    USB_DAC("USB DAC / Interface", "دک یا کارت صدای USB"),
    SPEAKER("Built-in Speaker", "بلندگوی دستگاه"),
    UNKNOWN("Audio Output", "خروجی صدا")
}

data class AudioRouteInfo(
    val type: AudioRouteType = AudioRouteType.SPEAKER,
    val deviceName: String = "Internal Speaker",
    val sampleRateHz: Int = 48000,
    val isHiRes: Boolean = false,
    val recommendedProfile: String = "Speaker Dynamic Tuning"
)

data class SubtitleTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

data class AudioTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

enum class TrackSortOrder(val labelEn: String, val labelFa: String) {
    TITLE_ASC("Title (A–Z)", "عنوان (الف تا ی)"),
    ARTIST_ASC("Artist (A–Z)", "هنرمند"),
    DURATION_DESC("Duration", "مدت زمان"),
    RECENTLY_ADDED("Recently Added", "افزوده اخیر")
}

enum class TrackFilterMode(val labelEn: String, val labelFa: String) {
    ALL("All Media", "همه رسانه‌ها"),
    FAVORITES_ONLY("Favorites", "علاقه‌مندی‌ها"),
    HI_RES_ONLY("Hi-Res ≥ 48kHz", "کیفیت بالا ≥ ۴۸kHz")
}

enum class ThemePaletteMode(val labelEn: String, val labelFa: String) {
    SYSTEM_DYNAMIC("Dynamic (System M3)", "پویا بر اساس تم گوشی (M3)"),
    VELORA_SAFFRON("Velora Saffron Gold", "طلای زعفرانی ولورا (پیش‌فرض)"),
    HIGH_CONTRAST_MONOCHROME("OLED High Contrast", "کنتراست بی‌نهایت اولد (سیاه و سفید خالص)")
}

data class AppSettings(
    val eqEnabled: Boolean = true,
    val activeEqPresetId: String = EqPreset.FLAT.id,
    val customEqGainsDb: List<Float> = EqPreset.FLAT.gainsDb,
    val queueMode: QueueMode = QueueMode.NORMAL,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val defaultPlaybackSpeed: Float = 1.0f,
    val videoAspectMode: VideoAspectMode = VideoAspectMode.FIT,
    val autoHideVideoControlsMs: Long = 3500L,
    val allowedFolderUris: Set<String> = emptySet(),
    val lastQueueTrackIds: List<String> = emptyList(),
    val lastActiveTrackId: String? = null,
    val lastPositionMs: Long = 0L,
    val corruptionFallbackCount: Int = 0,
    val appLanguageCode: String = "en",
    val isSimpleMode: Boolean = true,
    val videoGestureHintSeen: Boolean = false,
    val autoScanOnStartup: Boolean = true,
    val onlyValidMusicInSearch: Boolean = true,
    val backgroundVideoAudioEnabled: Boolean = true,
    val lowBatteryModeEnabled: Boolean = false,
    val adaptiveAudioMode: AdaptiveAudioMode = AdaptiveAudioMode.ADAPTIVE_AI,
    val dynamicColorEnabled: Boolean = true,
    val themePaletteMode: ThemePaletteMode = ThemePaletteMode.SYSTEM_DYNAMIC
)

data class ScanProgress(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val indexedCount: Int = 0,
    val currentSourceLabel: String = "",
    val lastScanDurationMs: Long = 0L,
    val errorMessage: String? = null
)

object PhiDesignMetrics {
    const val PHI: Double = 1.618033988749895
    const val PRIMARY_SPLIT_RATIO: Float = 0.618f
    const val SECONDARY_SPLIT_RATIO: Float = 0.382f

    val FIBONACCI_SPACING_DP: List<Int> = listOf(8, 13, 21, 34, 55)
    const val MIN_TOUCH_TARGET_DP: Int = 48

    fun deltaPercentFromPhi(width: Double, height: Double): Double {
        if (height <= 0.0) return 0.0
        val ratio = width / height
        return ((ratio - PHI) / PHI) * 100.0
    }

    fun relativeLuminance(rgbHex: Int): Double {
        val rSrgb = ((rgbHex shr 16) and 0xFF) / 255.0
        val gSrgb = ((rgbHex shr 8) and 0xFF) / 255.0
        val bSrgb = (rgbHex and 0xFF) / 255.0

        fun linearize(channel: Double): Double {
            return if (channel <= 0.03928) {
                channel / 12.92
            } else {
                ((channel + 0.055) / 1.055).pow(2.4)
            }
        }

        val r = linearize(rSrgb)
        val g = linearize(gSrgb)
        val b = linearize(bSrgb)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(foregroundRgb: Int, backgroundRgb: Int): Double {
        val l1 = relativeLuminance(foregroundRgb)
        val l2 = relativeLuminance(backgroundRgb)
        val brightest = max(l1, l2)
        val darkest = min(l1, l2)
        return (brightest + 0.05) / (darkest + 0.05)
    }

    fun contrastRatio(foregroundColor: Long, backgroundColor: Long): Double {
        val fgInt = (foregroundColor and 0xFFFFFFL).toInt()
        val bgInt = (backgroundColor and 0xFFFFFFL).toInt()
        return contrastRatio(fgInt, bgInt)
    }

    fun meetsWcagAa(foregroundRgb: Int, backgroundRgb: Int, minRatio: Double = 4.5): Boolean {
        return contrastRatio(foregroundRgb, backgroundRgb) >= minRatio - 1e-6
    }

    fun meetsWcagAa(foregroundColor: Long, backgroundColor: Long, minRatio: Double = 4.5): Boolean {
        return contrastRatio(foregroundColor, backgroundColor) >= minRatio - 1e-6
    }

    /**
     * Computes Björn Ottosson's Oklab perceptual lightness L in [0.0, 1.0] via LMS cone response
     * cube-root non-linearity, modeling human visual cortex brightness perception.
     */
    fun perceptualOklabLightness(rgbHex: Int): Double {
        val rSrgb = ((rgbHex shr 16) and 0xFF) / 255.0
        val gSrgb = ((rgbHex shr 8) and 0xFF) / 255.0
        val bSrgb = (rgbHex and 0xFF) / 255.0

        fun linearize(c: Double): Double =
            if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

        val r = linearize(rSrgb)
        val g = linearize(gSrgb)
        val b = linearize(bSrgb)

        val l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
        val m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
        val s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b

        val lPrime = kotlin.math.cbrt(l.coerceAtLeast(0.0))
        val mPrime = kotlin.math.cbrt(m.coerceAtLeast(0.0))
        val sPrime = kotlin.math.cbrt(s.coerceAtLeast(0.0))

        return (0.2104542553 * lPrime + 0.7936177850 * mPrime - 0.0040720468 * sPrime).coerceIn(0.0, 1.0)
    }

    /**
     * Computes polarity-aware APCA (Accessible Perceptual Contrast Algorithm, WCAG 3.0) Lc value.
     * Negative values indicate light-on-dark polarity (e.g., Saffron/Ivory on Obsidian); |Lc| >= 60
     * satisfies body-text perceptual legibility.
     */
    fun apcaPerceptualContrastLc(textRgbHex: Int, bgRgbHex: Int): Double {
        val yTxt = relativeLuminance(textRgbHex).coerceAtLeast(0.0)
        val yBg = relativeLuminance(bgRgbHex).coerceAtLeast(0.0)
        val sapc = if (yBg > yTxt) {
            (yBg.pow(0.56) - yTxt.pow(0.57)) * 1.14
        } else {
            (yBg.pow(0.65) - yTxt.pow(0.62)) * 1.14
        }
        return when {
            abs(sapc) < 0.1 -> 0.0
            sapc > 0.0 -> (sapc - 0.027) * 100.0
            else -> (sapc + 0.027) * 100.0
        }
    }

    private val FORBIDDEN_EXACT_PATHS = setOf("/", "/home", "/home/", "/root", "/etc")

    fun isSafeMediaFolder(pathOrUri: String): Boolean {
        val normalized = pathOrUri.trim().replace('\\', '/')
        if (normalized.isEmpty()) return false
        val lower = normalized.lowercase()
        if (lower.contains("/.ssh") || lower.endsWith(".ssh")) return false
        if (lower in FORBIDDEN_EXACT_PATHS || lower.startsWith("/etc/")) return false
        // Reject raw /tmp paths unless it is an isolated Android/Robolectric app-private filesDir sandbox
        val isAppPrivateSandbox = lower.contains("/fazora_studio_masters") || lower.contains("/user_folders")
        if ((lower == "/tmp" || lower.startsWith("/tmp/")) && !isAppPrivateSandbox) return false
        if (Regex("^[a-z]:/?$").matches(lower)) return false
        return abs(normalized.length) >= 2
    }
}
