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
    GOLDEN_PHI("Phi 1.618:1"),
    SIXTEEN_NINE("16:9 Wide")
}

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
    val appLanguageCode: String = "en"
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

    fun meetsWcagAa(foregroundRgb: Int, backgroundRgb: Int, minRatio: Double = 4.5): Boolean {
        return contrastRatio(foregroundRgb, backgroundRgb) >= minRatio - 1e-6
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
