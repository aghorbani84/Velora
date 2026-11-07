package com.example.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class MediaFormatDescriptor(
    val extension: String,
    val containerName: String,
    val primaryCodec: String,
    val mediaType: MediaType,
    val isLosslessOrHiRes: Boolean,
    val isNetworkStream: Boolean,
    val hardwareAccelerated: Boolean = true
)

object VeloraCompatibilityMatrix {

    val SUPPORTED_AUDIO_CONTAINERS = listOf(
        "WAV (PCM 16/24/32-bit)",
        "FLAC (Lossless)",
        "MP3 (MPEG-1/2 Audio Layer III)",
        "AAC / M4A (Advanced Audio Coding)",
        "ALAC (Apple Lossless in M4A)",
        "OGG Vorbis",
        "Opus (RFC 6716)",
        "AIFF (Audio Interchange)",
        "AMR-NB / AMR-WB",
        "MIDI (SMF Type 0/1)"
    )

    val SUPPORTED_VIDEO_CONTAINERS = listOf(
        "MP4 (ISO BMFF H.264 / HEVC / AV1)",
        "MKV (Matroska Multimedia Container)",
        "WebM (VP8 / VP9 / AV1)",
        "MOV (QuickTime Container)",
        "3GP / 3G2 (Mobile Multimedia)",
        "MPEG-TS (.ts Transport Stream)",
        "HLS (.m3u8 Adaptive Streaming)",
        "MPEG-DASH (.mpd Dynamic Adaptive)"
    )

    fun inspectMediaFormat(uriOrPath: String, fallbackType: MediaType = MediaType.AUDIO): MediaFormatDescriptor {
        val clean = uriOrPath.trim().lowercase(Locale.ROOT)
        val isHttp = clean.startsWith("http://") || clean.startsWith("https://")
        val ext = clean.substringBefore('?').substringAfterLast('.', "")

        return when {
            ext == "m3u8" -> MediaFormatDescriptor("m3u8", "HLS Adaptive Stream", "AAC / H.264 / HEVC", MediaType.VIDEO, false, true)
            ext == "mpd" -> MediaFormatDescriptor("mpd", "MPEG-DASH Stream", "AAC / VP9 / AV1", MediaType.VIDEO, false, true)
            ext == "flac" -> MediaFormatDescriptor("flac", "FLAC", "Free Lossless Audio Codec", MediaType.AUDIO, true, isHttp)
            ext == "wav" -> MediaFormatDescriptor("wav", "RIFF WAVE", "Linear PCM", MediaType.AUDIO, true, isHttp)
            ext == "alac" -> MediaFormatDescriptor("alac", "M4A / ALAC", "Apple Lossless", MediaType.AUDIO, true, isHttp)
            ext == "aiff" || ext == "aif" -> MediaFormatDescriptor("aiff", "AIFF", "Linear PCM Big-Endian", MediaType.AUDIO, true, isHttp)
            ext == "mp3" -> MediaFormatDescriptor("mp3", "MP3", "MPEG-1 Audio Layer III", MediaType.AUDIO, false, isHttp)
            ext == "aac" || ext == "m4a" -> MediaFormatDescriptor("m4a", "MP4 Audio", "AAC-LC / HE-AAC", MediaType.AUDIO, false, isHttp)
            ext == "ogg" || ext == "oga" -> MediaFormatDescriptor("ogg", "Ogg Container", "Vorbis / Opus", MediaType.AUDIO, false, isHttp)
            ext == "opus" -> MediaFormatDescriptor("opus", "Ogg Opus", "Opus Interactive Audio", MediaType.AUDIO, false, isHttp)
            ext == "mp4" || ext == "m4v" -> MediaFormatDescriptor("mp4", "MPEG-4 Part 14", "H.264 / HEVC + AAC", MediaType.VIDEO, false, isHttp)
            ext == "mkv" -> MediaFormatDescriptor("mkv", "Matroska", "HEVC / VP9 / Opus", MediaType.VIDEO, false, isHttp)
            ext == "webm" -> MediaFormatDescriptor("webm", "WebM", "VP9 / AV1 + Opus", MediaType.VIDEO, false, isHttp)
            ext == "mov" -> MediaFormatDescriptor("mov", "QuickTime MOV", "H.264 / ProRes", MediaType.VIDEO, false, isHttp)
            ext == "ts" -> MediaFormatDescriptor("ts", "MPEG Transport Stream", "H.264 / AAC", MediaType.VIDEO, false, isHttp)
            else -> MediaFormatDescriptor(
                extension = ext.ifEmpty { "raw" },
                containerName = if (isHttp) "Progressive Network Stream" else "Standard Container",
                primaryCodec = if (fallbackType == MediaType.VIDEO) "Hardware Video Decoder" else "PCM / Media3 Decoder",
                mediaType = fallbackType,
                isLosslessOrHiRes = false,
                isNetworkStream = isHttp
            )
        }
    }

    fun isValidAuthorizedStreamUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.length < 10) return false
        val lower = trimmed.lowercase(Locale.ROOT)
        return (lower.startsWith("https://") || lower.startsWith("http://")) &&
            !lower.contains(" ") &&
            !lower.contains("javascript:") &&
            !lower.contains("file://") &&
            !lower.contains("://127.") &&
            !lower.contains("://localhost") &&
            !lower.contains("://0.0.0.0") &&
            !lower.contains("://169.254.")
    }
}

data class SyncedLyricLine(
    val timestampMs: Long,
    val text: String,
    val translationFa: String = "",
    val translationJa: String = ""
)

object VeloraLyricsAndSubtitleEngine {

    private val LRC_LINE_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?](.*)""")

    fun parseLrcOrPlain(rawContent: String): List<SyncedLyricLine> {
        if (rawContent.isBlank()) return emptyList()
        val parsed = mutableListOf<SyncedLyricLine>()
        val rawLines = rawContent.lines()

        rawLines.forEachIndexed { idx, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            val match = LRC_LINE_REGEX.matchEntire(trimmed)
            if (match != null) {
                val minVal = match.groupValues[1].toLongOrNull() ?: 0L
                val secVal = match.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = match.groupValues[3]
                val msVal = when (fracStr.length) {
                    1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                    3 -> fracStr.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val totalMs = minVal * 60_000L + secVal * 1_000L + msVal
                val lyricText = match.groupValues[4].trim()
                if (lyricText.isNotEmpty()) {
                    parsed.add(SyncedLyricLine(timestampMs = totalMs, text = lyricText))
                }
            } else if (!trimmed.startsWith("[") && !trimmed.matches(Regex("^\\d+$")) && !trimmed.contains("-->")) {
                parsed.add(
                    SyncedLyricLine(
                        timestampMs = idx * 4_000L,
                        text = trimmed
                    )
                )
            }
        }
        return parsed.sortedBy { it.timestampMs }
    }

    fun resolveAutomaticLyricsForTrack(track: Track): List<SyncedLyricLine> {
        val duration = track.durationMs.coerceAtLeast(12_000L)
        val step = (duration / 6L).coerceAtLeast(1_500L)
        return listOf(
            SyncedLyricLine(
                timestampMs = 0L,
                text = "♪ ${track.title} — ${track.artist}",
                translationFa = "♪ ${track.title} — ${track.artist} (نسخه استودیویی ولورا)",
                translationJa = "♪ ${track.title} — ${track.artist} (VELORA スタジオ)"
            ),
            SyncedLyricLine(
                timestampMs = step,
                text = "Golden ratio harmonic resonance in ${track.album}",
                translationFa = "طنین هارمونیک نسبت طلایی در آلبوم ${track.album}",
                translationJa = "黄金比ハーモニック・レゾナンス (${track.album})"
            ),
            SyncedLyricLine(
                timestampMs = step * 2,
                text = "Precision acoustic stage at ${track.sampleRateHz / 1000.0} kHz • ${track.bitrateKbps} kbps",
                translationFa = "صحنه آکوستیک دقیق با نرخ نمونه‌برداری ${track.sampleRateHz / 1000.0} کیلوهرتز",
                translationJa = "高精度アコースティックステージ ${track.sampleRateHz / 1000.0} kHz"
            ),
            SyncedLyricLine(
                timestampMs = step * 3,
                text = "Saffron warmth meets deep obsidian silence",
                translationFa = "گرمای زعفرانی در تلاقی با سکوت عمیق ابسیدین",
                translationJa = "サフランの温もりと深い黒曜石の静寂"
            ),
            SyncedLyricLine(
                timestampMs = step * 4,
                text = "10-band biquad phase-coherent frequency sculpting",
                translationFa = "پردازش فرکانسی ۱۰ باندی Biquad با حفظ پیوستگی فاز",
                translationJa = "10バンド・バイクアッド周波数スカルプティング"
            ),
            SyncedLyricLine(
                timestampMs = step * 5,
                text = "VELORA — Beyond Playback.",
                translationFa = "ولورا — فراتر از پخش.",
                translationJa = "VELORA — 再生を超えて。"
            )
        )
    }

    fun findActiveLineIndex(
        lines: List<SyncedLyricLine>,
        positionMs: Long,
        offsetMs: Long = 0L
    ): Int {
        if (lines.isEmpty()) return -1
        val adjustedPos = (positionMs + offsetMs).coerceAtLeast(0L)
        var activeIdx = 0
        for (i in lines.indices) {
            if (adjustedPos >= lines[i].timestampMs) {
                activeIdx = i
            } else {
                break
            }
        }
        return activeIdx
    }
}

enum class SmartPlaylistCategory(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String
) {
    DAILY_SAFFRON_MIX(
        id = "smart_daily_saffron_mix",
        titleEn = "Daily Saffron Mix",
        titleFa = "میکس روزانه زعفرانی",
        subtitleEn = "Balanced blend of your favorites, recent rotation & studio discoveries",
        subtitleFa = "ترکیبی متوازن از علاقه‌مندی‌ها، پخش‌های اخیر و کشف‌های استودیویی"
    ),
    MOST_PLAYED(
        id = "smart_most_played",
        titleEn = "Most Played",
        titleFa = "پربازدیدترین قطعات",
        subtitleEn = "Ranked by verified play frequency and completion ratio",
        subtitleFa = "رتبه‌بندی‌شده بر اساس تعداد پخش و نرخ تکمیل گوش دادن"
    ),
    CONTINUE_LISTENING(
        id = "smart_continue_listening",
        titleEn = "Continue Listening",
        titleFa = "ادامه شنیدن",
        subtitleEn = "Resume tracks from your exact saved timestamp",
        subtitleFa = "ادامه پخش قطعات دقیقاً از ثانیه ذخیره شده"
    ),
    HIDDEN_GEMS(
        id = "smart_hidden_gems",
        titleEn = "Hidden Gems & Hi-Res",
        titleFa = "گوهرهای پنهان و Hi-Res",
        subtitleEn = "High-resolution masters and unplayed acoustic discoveries",
        subtitleFa = "مسترهای با کیفیت بالا و قطعات کمتر شنیده شده"
    ),
    FAVORITE_COLLECTION(
        id = "smart_favorites",
        titleEn = "Favorite Collection",
        titleFa = "مجموعه علاقه‌مندی‌ها",
        subtitleEn = "Your starred tracks and curated studio highlights",
        subtitleFa = "قطعات نشان‌شده و برگزیده شما"
    )
}

data class SmartPlaylist(
    val category: SmartPlaylistCategory,
    val tracks: List<Track>,
    val explainReasonEn: String,
    val explainReasonFa: String
)

object VeloraSmartPlaylistEngine {

    fun scoreTrackAffinity(
        track: Track,
        historyEntry: PlaybackHistoryEntry?,
        nowMs: Long = System.currentTimeMillis()
    ): Double {
        var score = 10.0
        if (track.isFavorite) score += 35.0
        if (track.sampleRateHz >= 48_000 || track.bitrateKbps >= 1000) score += 12.0

        if (historyEntry != null) {
            score += min(historyEntry.playCount * 8.0, 40.0)
            score += min(historyEntry.completedCount * 6.0, 24.0)
            val ageHours = ((nowMs - historyEntry.lastPlayedAtMs).coerceAtLeast(0L)) / 3_600_000.0
            val recencyBonus = 20.0 / (1.0 + ageHours * 0.15)
            score += recencyBonus
        }
        return score
    }

    fun generateSmartPlaylists(
        allTracks: List<Track>,
        history: List<PlaybackHistoryEntry>,
        nowMs: Long = System.currentTimeMillis()
    ): List<SmartPlaylist> {
        if (allTracks.isEmpty()) return emptyList()
        val historyByTrackId = history.associateBy { it.track.id }

        val scoredTracks = allTracks
            .map { track -> track to scoreTrackAffinity(track, historyByTrackId[track.id], nowMs) }
            .sortedByDescending { it.second }
            .map { it.first }

        val dailyMix = scoredTracks.take(8)
        val mostPlayed = history
            .sortedWith(compareByDescending<PlaybackHistoryEntry> { it.playCount }.thenByDescending { it.completedCount })
            .map { it.track }
            .ifEmpty { allTracks.take(5) }

        val continueListening = history
            .sortedByDescending { it.lastPlayedAtMs }
            .map { it.track }
            .ifEmpty { allTracks.take(4) }

        val hiddenGems = allTracks
            .filter { (historyByTrackId[it.id]?.playCount ?: 0) <= 1 }
            .sortedByDescending { it.bitrateKbps }
            .ifEmpty { allTracks.reversed().take(5) }

        val favorites = allTracks.filter { it.isFavorite }

        val playlists = mutableListOf(
            SmartPlaylist(
                category = SmartPlaylistCategory.DAILY_SAFFRON_MIX,
                tracks = dailyMix,
                explainReasonEn = "Weighted affinity score (Favorites + Recency + Hi-Res)",
                explainReasonFa = "امتیاز ترکیبی هوشمند (علاقه‌مندی + تازگی + کیفیت صدا)"
            ),
            SmartPlaylist(
                category = SmartPlaylistCategory.MOST_PLAYED,
                tracks = mostPlayed,
                explainReasonEn = if (history.isNotEmpty()) {
                    "Top ${mostPlayed.size} tracks by play & completion count"
                } else {
                    "Ready to rank as you listen"
                },
                explainReasonFa = "رتبه‌بندی بر اساس بیشترین دفعات پخش"
            ),
            SmartPlaylist(
                category = SmartPlaylistCategory.CONTINUE_LISTENING,
                tracks = continueListening,
                explainReasonEn = "Recently active sessions cached in Room",
                explainReasonFa = "آخرین قطعات پخش‌شده در حافظه آفلاین Room"
            ),
            SmartPlaylist(
                category = SmartPlaylistCategory.HIDDEN_GEMS,
                tracks = hiddenGems,
                explainReasonEn = "Unplayed or low-rotation studio tracks",
                explainReasonFa = "قطعات استودیویی کمتر شنیده شده"
            )
        )

        if (favorites.isNotEmpty()) {
            playlists.add(
                0,
                SmartPlaylist(
                    category = SmartPlaylistCategory.FAVORITE_COLLECTION,
                    tracks = favorites,
                    explainReasonEn = "${favorites.size} tracks marked as Favorite",
                    explainReasonFa = "${favorites.size} قطعه در لیست علاقه‌مندی‌های شما"
                )
            )
        }

        return playlists
    }
}

data class OmniSearchMatch(
    val track: Track,
    val relevanceScore: Int,
    val matchReasonEn: String,
    val matchReasonFa: String
)

object VeloraOmniSearchEngine {

    fun searchWithSemanticAndFuzzyRanking(
        query: String,
        allTracks: List<Track>,
        history: List<PlaybackHistoryEntry>
    ): List<OmniSearchMatch> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return allTracks.map {
                OmniSearchMatch(it, 50, "Library Index", "فهرست کتابخانه")
            }
        }
        val lowerQuery = trimmed.lowercase(Locale.ROOT)
        val historyMap = history.associateBy { it.track.id }

        val semanticUnfinished = lowerQuery.contains("unfinished") ||
            lowerQuery.contains("resume") ||
            lowerQuery.contains("ناتمام") ||
            lowerQuery.contains("ادامه")
        val semanticFavorite = lowerQuery.contains("favorite") ||
            lowerQuery.contains("starred") ||
            lowerQuery.contains("علاقه") ||
            lowerQuery.contains("محبوب")
        val semanticHiRes = lowerQuery.contains("hi-res") ||
            lowerQuery.contains("lossless") ||
            lowerQuery.contains("studio") ||
            lowerQuery.contains("استودیو")
        val semanticNightOrCalm = lowerQuery.contains("night") ||
            lowerQuery.contains("calm") ||
            lowerQuery.contains("relax") ||
            lowerQuery.contains("آرام") ||
            lowerQuery.contains("شب")

        return allTracks.mapNotNull { track ->
            var score = 0
            var reasonEn = "Matched metadata"
            var reasonFa = "تطابق متادیتا"

            val titleLower = track.title.lowercase(Locale.ROOT)
            val artistLower = track.artist.lowercase(Locale.ROOT)
            val albumLower = track.album.lowercase(Locale.ROOT)

            if (titleLower.startsWith(lowerQuery)) {
                score += 100
                reasonEn = "Exact title prefix match"
                reasonFa = "تطابق دقیق ابتدای عنوان"
            } else if (titleLower.contains(lowerQuery)) {
                score += 80
                reasonEn = "Title match"
                reasonFa = "تطابق در عنوان قطعه"
            }

            if (artistLower.contains(lowerQuery)) {
                score += 70
                reasonEn = "Artist match (${track.artist})"
                reasonFa = "تطابق نام هنرمند (${track.artist})"
            }
            if (albumLower.contains(lowerQuery)) {
                score += 60
                reasonEn = "Album match (${track.album})"
                reasonFa = "تطابق نام آلبوم (${track.album})"
            }

            if (semanticFavorite && track.isFavorite) {
                score += 90
                reasonEn = "Semantic: Favorite track"
                reasonFa = "جستجوی معنایی: قطعه مورد علاقه"
            }
            val hist = historyMap[track.id]
            if (semanticUnfinished && hist != null && hist.lastPositionMs > 0L) {
                score += 95
                reasonEn = "Semantic: Unfinished at ${Track.formatDurationMs(hist.lastPositionMs)}"
                reasonFa = "جستجوی معنایی: ناتمام در ${Track.formatDurationMs(hist.lastPositionMs)}"
            }
            if (semanticHiRes && (track.sampleRateHz >= 48_000 || track.bitrateKbps >= 1000)) {
                score += 85
                reasonEn = "Semantic: Studio Hi-Res (${track.sampleRateHz / 1000.0}kHz)"
                reasonFa = "جستجوی معنایی: کیفیت استودیویی (${track.sampleRateHz / 1000.0}kHz)"
            }
            if (semanticNightOrCalm && track.mediaType == MediaType.AUDIO) {
                score += 65
                reasonEn = "Semantic: Atmospheric audio session"
                reasonFa = "جستجوی معنایی: فضای صوتی آرام و شبانه"
            }

            if (score == 0 && lowerQuery.length >= 3) {
                val dist = min(
                    levenshteinDistance(lowerQuery, titleLower.take(max(lowerQuery.length, 4))),
                    levenshteinDistance(lowerQuery, artistLower.take(max(lowerQuery.length, 4)))
                )
                if (dist <= 2) {
                    score += (55 - dist * 15)
                    reasonEn = "Fuzzy typo-tolerant match"
                    reasonFa = "تطابق هوشمند با تحمل خطای تایپی"
                }
            }

            if (score > 0) {
                OmniSearchMatch(track, score, reasonEn, reasonFa)
            } else {
                null
            }
        }.sortedByDescending { it.relevanceScore }
    }

    fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length
        if (lhsLen == 0) return rhsLen
        if (rhsLen == 0) return lhsLen

        var cost = IntArray(lhsLen + 1) { it }
        var newCost = IntArray(lhsLen + 1)

        for (i in 1..rhsLen) {
            newCost[0] = i
            for (j in 1..lhsLen) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLen]
    }
}

data class TimeMachineBookmark(
    val id: String,
    val trackId: String,
    val trackTitle: String,
    val timestampMs: Long,
    val label: String,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val formattedTimestamp: String
        get() = Track.formatDurationMs(timestampMs)
}

data class DeclarativePluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val publisher: String,
    val tier: String,
    val capabilities: List<String>,
    val verifiedSignature: Boolean,
    val isEnabled: Boolean
)

object VeloraPluginRegistry {
    val DEFAULT_DECLARATIVE_PLUGINS = listOf(
        DeclarativePluginManifest(
            id = "velora.plugin.saffron.cinema",
            name = "Saffron Dark Cinema Theme & Contrast Guard",
            version = "1.4.0",
            publisher = "VELORA Core Design",
            tier = "Tier 1 — Declarative UI Token Extension",
            capabilities = listOf("theme.palette.saffron", "wcag.contrast.guard"),
            verifiedSignature = true,
            isEnabled = true
        ),
        DeclarativePluginManifest(
            id = "velora.plugin.lyrics.multiscript",
            name = "Multi-Script LRC/SRT Synchronizer (EN / FA / JA)",
            version = "2.1.0",
            publisher = "VELORA Media Intelligence",
            tier = "Tier 1 — Declarative Metadata & Lyrics Adapter",
            capabilities = listOf("lyrics.sync.lrc", "subtitle.rtl.persian"),
            verifiedSignature = true,
            isEnabled = true
        ),
        DeclarativePluginManifest(
            id = "velora.plugin.dsp.goertzel",
            name = "10-Band Goertzel RTA & Peak-Hold Telemetry",
            version = "1.8.2",
            publisher = "VELORA Acoustic Lab",
            tier = "Tier 3 — Native Media3 AudioProcessor",
            capabilities = listOf("dsp.biquad.10band", "visualizer.goertzel.rta"),
            verifiedSignature = true,
            isEnabled = true
        ),
        DeclarativePluginManifest(
            id = "velora.plugin.smart.rotation",
            name = "Local-First Smart Playlist & Affinity Scorer",
            version = "1.2.0",
            publisher = "VELORA Personalization Engine",
            tier = "Tier 1 — Deterministic Recommendation Rules",
            capabilities = listOf("recommendations.local", "privacy.zero_telemetry"),
            verifiedSignature = true,
            isEnabled = true
        )
    )
}
