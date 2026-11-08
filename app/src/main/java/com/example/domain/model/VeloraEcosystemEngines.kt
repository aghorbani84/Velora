package com.example.domain.model

import java.util.Locale

/**
 * Classification of feature readiness across target platforms (Part II, Section 4).
 */
enum class CapabilityStatus(val labelEn: String, val labelFa: String) {
    SUPPORTED_AND_TESTED("Supported & Tested", "پشتیبانی و تست‌شده"),
    SUPPORTED_WITH_LIMITATIONS("Supported with Limitations", "پشتیبانی با محدودیت پلتفرم"),
    PLANNED("Planned", "برنامه‌ریزی‌شده"),
    UNSUPPORTED("Unsupported", "پشتیبانی‌نشده در این پلتفرم")
}

enum class PlatformTarget(val displayName: String) {
    ANDROID("Android (Media3 / Room / Jetpack Compose)"),
    IOS("iOS (AVFoundation / Swift Interop)"),
    DESKTOP("Desktop (Windows / macOS / Linux)")
}

data class PlatformCapabilityEntry(
    val featureId: String,
    val featureName: String,
    val androidStatus: CapabilityStatus,
    val iosStatus: CapabilityStatus,
    val desktopStatus: CapabilityStatus,
    val technicalNotes: String
)

object VeloraPlatformMatrix {
    val CAPABILITY_MATRIX: List<PlatformCapabilityEntry> = listOf(
        PlatformCapabilityEntry(
            featureId = "playback.dsp.10band",
            featureName = "10-Band Biquad EQ & Goertzel RTA Visualizer",
            androidStatus = CapabilityStatus.SUPPORTED_AND_TESTED,
            iosStatus = CapabilityStatus.PLANNED,
            desktopStatus = CapabilityStatus.PLANNED,
            technicalNotes = "Verified via Media3 TenBandBiquadAudioProcessor & PCM 16-bit pipeline"
        ),
        PlatformCapabilityEntry(
            featureId = "storage.room.offline",
            featureName = "Offline Library Index, Favorites & Playback History",
            androidStatus = CapabilityStatus.SUPPORTED_AND_TESTED,
            iosStatus = CapabilityStatus.PLANNED,
            desktopStatus = CapabilityStatus.PLANNED,
            technicalNotes = "Room SQLite v3 with deduplication & ReplaceFileCorruptionHandler DataStore"
        ),
        PlatformCapabilityEntry(
            featureId = "streaming.hls.dash",
            featureName = "Authorized HLS (.m3u8), MPEG-DASH (.mpd) & HTTP Streaming",
            androidStatus = CapabilityStatus.SUPPORTED_AND_TESTED,
            iosStatus = CapabilityStatus.UNSUPPORTED,
            desktopStatus = CapabilityStatus.UNSUPPORTED,
            technicalNotes = "Secure Stream Validation (HTTPS/HLS/DASH URL sanitization) enforced before playback"
        ),
        PlatformCapabilityEntry(
            featureId = "subtitles.lrc.srt.vtt",
            featureName = "Synchronized LRC, Multi-Script RTL & SRT/WebVTT Export",
            androidStatus = CapabilityStatus.SUPPORTED_AND_TESTED,
            iosStatus = CapabilityStatus.UNSUPPORTED,
            desktopStatus = CapabilityStatus.UNSUPPORTED,
            technicalNotes = "Pure Kotlin domain parser and exporter in Android app module"
        ),
        PlatformCapabilityEntry(
            featureId = "plugins.declarative.tier1",
            featureName = "Tier 1 Declarative & Sandboxed Plugin Ecosystem",
            androidStatus = CapabilityStatus.SUPPORTED_AND_TESTED,
            iosStatus = CapabilityStatus.UNSUPPORTED,
            desktopStatus = CapabilityStatus.UNSUPPORTED,
            technicalNotes = "Complies with Google Play no-dynamic-code-loading policies"
        )
    )
}

/**
 * Exports synchronized lyric/subtitle lines to standard SRT and WebVTT formats (Part V, Section 15.B).
 */
object VeloraSubtitleExporter {

    fun exportToSrt(lines: List<SyncedLyricLine>, defaultDurationMs: Long = 3_500L): String {
        if (lines.isEmpty()) return ""
        val sb = StringBuilder()
        for (i in lines.indices) {
            val current = lines[i]
            val startMs = current.timestampMs.coerceAtLeast(0L)
            val nextStartMs = lines.getOrNull(i + 1)?.timestampMs ?: (startMs + defaultDurationMs)
            val endMs = nextStartMs.coerceAtLeast(startMs + 500L)

            sb.append(i + 1).append('\n')
            sb.append(formatSrtTimestamp(startMs))
                .append(" --> ")
                .append(formatSrtTimestamp(endMs))
                .append('\n')
            sb.append(current.text.trim()).append('\n')
            if (current.translationFa.isNotBlank()) {
                sb.append(current.translationFa.trim()).append('\n')
            }
            sb.append('\n')
        }
        return sb.toString().trimEnd()
    }

    fun exportToWebVtt(lines: List<SyncedLyricLine>, defaultDurationMs: Long = 3_500L): String {
        val sb = StringBuilder("WEBVTT\n\n")
        for (i in lines.indices) {
            val current = lines[i]
            val startMs = current.timestampMs.coerceAtLeast(0L)
            val nextStartMs = lines.getOrNull(i + 1)?.timestampMs ?: (startMs + defaultDurationMs)
            val endMs = nextStartMs.coerceAtLeast(startMs + 500L)

            sb.append(formatVttTimestamp(startMs))
                .append(" --> ")
                .append(formatVttTimestamp(endMs))
                .append('\n')
            sb.append(current.text.trim()).append('\n')
            if (current.translationFa.isNotBlank()) {
                sb.append(current.translationFa.trim()).append('\n')
            }
            sb.append('\n')
        }
        return sb.toString().trimEnd()
    }

    fun formatSrtTimestamp(ms: Long): String {
        val clamped = ms.coerceAtLeast(0L)
        val hours = clamped / 3_600_000L
        val minutes = (clamped % 3_600_000L) / 60_000L
        val seconds = (clamped % 60_000L) / 1_000L
        val millis = clamped % 1_000L
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    fun formatVttTimestamp(ms: Long): String {
        val clamped = ms.coerceAtLeast(0L)
        val hours = clamped / 3_600_000L
        val minutes = (clamped % 3_600_000L) / 60_000L
        val seconds = (clamped % 60_000L) / 1_000L
        val millis = clamped % 1_000L
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}

/**
 * Security validator for Download & Authorized Torrent/Magnet metadata (Part IV, Sections 11 & 13, Part IX).
 */
object VeloraDownloadValidator {
    private val BTIH_MAGNET_REGEX = Regex(
        pattern = """^magnet:\?xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})(?:&.*)?$"""
    )

    fun isValidAuthorizedMagnetUri(uri: String): Boolean {
        val trimmed = uri.trim()
        return BTIH_MAGNET_REGEX.matches(trimmed)
    }

    fun isSafeDownloadTargetFileName(relativePath: String): Boolean {
        val trimmed = relativePath.trim()
        if (trimmed.isEmpty()) return false
        val normalized = trimmed.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.contains("/..")) {
            return false
        }
        if (normalized.contains('\u0000') || normalized.contains(':')) {
            return false
        }
        return true
    }
}

/**
 * Configurable and reversible Adaptive Experience modes (Part VIII, Section 29).
 */
enum class AdaptiveExperienceMode(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String
) {
    STANDARD_STUDIO(
        id = "standard_studio",
        titleEn = "Studio Reference",
        titleFa = "مرجع استودیویی",
        descriptionEn = "Balanced dual RTA spectrum + oscilloscope with standard controls"
    ),
    NIGHT_SAFFRON(
        id = "night_saffron",
        titleEn = "Night Saffron Cinema",
        titleFa = "حالت شب زعفرانی",
        descriptionEn = "Reduced visual luminance and warm low-glare contrast for dark rooms"
    ),
    DRIVING_LARGE_CONTROLS(
        id = "driving_mode",
        titleEn = "Driving & Large Touch",
        titleFa = "حالت رانندگی",
        descriptionEn = "Prioritizes oversized transport controls and voice-friendly navigation"
    ),
    HEADPHONE_AUDIOPHILE(
        id = "headphone_audiophile",
        titleEn = "Headphone Audiophile",
        titleFa = "هدفون حرفه‌ای",
        descriptionEn = "Enables 10-band biquad EQ stage and detailed bitrate/sample-rate telemetry"
    ),
    LOW_BATTERY_SAVER(
        id = "low_battery_saver",
        titleEn = "Low-Power Acoustic",
        titleFa = "ذخیره باتری",
        descriptionEn = "Reduces canvas frame rate while preserving uninterrupted audio playback"
    )
}

/**
 * Deterministic Media Copilot command interpreter (Part VI, Section 23).
 */
enum class CopilotCommandIntent {
    PLAY_FAVORITES,
    RESUME_LAST_SESSION,
    EXPORT_SUBTITLES_SRT,
    EXPORT_SUBTITLES_VTT,
    FIND_HI_RES_TRACKS,
    FIND_NIGHT_DRIVE_MUSIC,
    EXPLAIN_RECOMMENDATIONS
}

data class CopilotActionResult(
    val intent: CopilotCommandIntent,
    val summaryEn: String,
    val summaryFa: String,
    val requiresConfirmation: Boolean = false
)

object VeloraMediaCopilot {
    fun interpretCommand(rawCommand: String): CopilotActionResult {
        val lower = rawCommand.trim().lowercase(Locale.ROOT)
        return when {
            lower.contains("vtt") || lower.contains("webvtt") -> CopilotActionResult(
                intent = CopilotCommandIntent.EXPORT_SUBTITLES_VTT,
                summaryEn = "Exported active synchronized lyrics/subtitles to WebVTT (.vtt) format",
                summaryFa = "زیرنویس/متن ترانه فعال با فرمت استاندارد WebVTT استخراج شد"
            )
            lower.contains("srt") || lower.contains("subtitle") || lower.contains("زیرنویس") -> CopilotActionResult(
                intent = CopilotCommandIntent.EXPORT_SUBTITLES_SRT,
                summaryEn = "Exported active synchronized lyrics/subtitles to SubRip (.srt) format",
                summaryFa = "زیرنویس/متن ترانه فعال با فرمت استاندارد SRT استخراج شد"
            )
            lower.contains("resume") || lower.contains("continue") || lower.contains("unfinished") ||
                lower.contains("ادامه") || lower.contains("ناتمام") -> CopilotActionResult(
                intent = CopilotCommandIntent.RESUME_LAST_SESSION,
                summaryEn = "Resuming most recent unfinished track from Room playback history",
                summaryFa = "ادامه پخش آخرین قطعه ناتمام از تاریخچه آفلاین Room"
            )
            lower.contains("favorite") || lower.contains("starred") ||
                lower.contains("علاقه") || lower.contains("محبوب") -> CopilotActionResult(
                intent = CopilotCommandIntent.PLAY_FAVORITES,
                summaryEn = "Playing your starred Favorite Collection queue",
                summaryFa = "پخش مجموعه قطعات مورد علاقه شما"
            )
            lower.contains("hi-res") || lower.contains("lossless") || lower.contains("flac") ||
                lower.contains("استودیو") -> CopilotActionResult(
                intent = CopilotCommandIntent.FIND_HI_RES_TRACKS,
                summaryEn = "Filtered library to Studio Hi-Res (>= 48 kHz / Lossless) masters",
                summaryFa = "فیلتر کتابخانه بر اساس قطعات استودیویی Hi-Res"
            )
            lower.contains("night") || lower.contains("drive") || lower.contains("calm") ||
                lower.contains("شب") || lower.contains("آرام") -> CopilotActionResult(
                intent = CopilotCommandIntent.FIND_NIGHT_DRIVE_MUSIC,
                summaryEn = "Curated atmospheric Night Drive & Calm session from local library",
                summaryFa = "آماده‌سازی لیست پخش آرام و مناسب رانندگی شبانه"
            )
            else -> CopilotActionResult(
                intent = CopilotCommandIntent.EXPLAIN_RECOMMENDATIONS,
                summaryEn = "Recommendations are ranked 100% locally by Favorite status (+35), " +
                    "Play frequency (up to +40), Completion ratio (+24), and Hi-Res fidelity (+12).",
                summaryFa = "پیشنهادها ۱۰۰٪ به‌صورت محلی بر اساس علاقه‌مندی (+۳۵)، دفعات پخش (+۴۰)، " +
                    "نرخ تکمیل (+۲۴) و کیفیت استودیویی (+۱۲) رتبه‌بندی شده‌اند."
            )
        }
    }
}

/**
 * Personal Media Graph linking verified artists, albums, tracks, and formats (Part VI, Section 21).
 */
data class MediaGraphSummary(
    val totalNodes: Int,
    val totalEdges: Int,
    val topConnectedArtists: List<Pair<String, Int>>,
    val formatDistribution: Map<String, Int>
)

object VeloraPersonalMediaGraph {
    fun buildGraphSummary(tracks: List<Track>): MediaGraphSummary {
        if (tracks.isEmpty()) {
            return MediaGraphSummary(0, 0, emptyList(), emptyMap())
        }
        val artists = tracks.map { it.artist }.distinct()
        val albums = tracks.map { it.album }.distinct()
        val formats = tracks.groupingBy {
            VeloraCompatibilityMatrix.inspectMediaFormat(it.uri, it.mediaType).containerName
        }.eachCount()

        val nodeCount = tracks.size + artists.size + albums.size + formats.size
        // Each track links to its Artist, Album, and Format (3 verified edges per track)
        val edgeCount = tracks.size * 3
        val topArtists = tracks
            .groupingBy { it.artist }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        return MediaGraphSummary(
            totalNodes = nodeCount,
            totalEdges = edgeCount,
            topConnectedArtists = topArtists,
            formatDistribution = formats
        )
    }
}
