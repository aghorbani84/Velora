package com.example.domain.model

import java.util.Locale

data class ListeningStatsSummary(
    val totalListeningMinutes: Long = 0L,
    val totalPlaysCount: Int = 0,
    val totalCompletedCount: Int = 0,
    val completionRatePercent: Int = 0,
    val topTracks: List<Pair<Track, Int>> = emptyList(),
    val topArtists: List<Pair<String, Int>> = emptyList(),
    val hiResTracksCount: Int = 0,
    val favoriteTracksCount: Int = 0
)

data class SmartRuleCriterion(
    val id: String,
    val name: String,
    val minBitrateKbps: Int = 0,
    val minSampleRateHz: Int = 0,
    val minPlayCount: Int = 0,
    val favoritesOnly: Boolean = false,
    val artistKeyword: String = ""
)

enum class SubtitleFontScale(val labelEn: String, val labelFa: String, val multiplier: Float) {
    SMALL("Small", "کوچک", 0.85f),
    NORMAL("Normal", "معمولی", 1.0f),
    LARGE("Large", "بزرگ", 1.25f),
    EXTRA_LARGE("Cinema XL", "خیلی بزرگ", 1.5f)
}

enum class SubtitleColorTheme(
    val labelEn: String,
    val labelFa: String,
    val textHex: Long,
    val bgHex: Long
) {
    SAFFRON_ON_DARK("Saffron Gold", "طلایی زعفرانی", 0xFFE5A817, 0xCC0B0B0D),
    WHITE_ON_DARK("Cinema White", "سفید سینمایی", 0xFFF5F2EB, 0xCC000000),
    YELLOW_HIGH_CONTRAST("High Contrast Yellow", "زرد کنتراست بالا", 0xFFFFEB3B, 0xEE000000),
    CYAN_STUDIO("Studio Cyan", "فیروزه‌ای استودیو", 0xFF4DD0E1, 0xCC0B0B0D)
}

data class VideoCinemaSettings(
    val brightnessLevel: Float = 0.85f,
    val subtitleFontScale: SubtitleFontScale = SubtitleFontScale.NORMAL,
    val subtitleColorTheme: SubtitleColorTheme = SubtitleColorTheme.SAFFRON_ON_DARK
)

object VeloraPhase2Engine {

    fun computeListeningStats(
        allTracks: List<Track>,
        history: List<PlaybackHistoryEntry>
    ): ListeningStatsSummary {
        if (allTracks.isEmpty() && history.isEmpty()) {
            return ListeningStatsSummary()
        }
        val totalPlays = history.sumOf { it.playCount }
        val totalCompleted = history.sumOf { it.completedCount }
        val totalMs = history.sumOf { entry ->
            val duration = entry.track.durationMs.coerceAtLeast(180_000L)
            (entry.playCount * duration) / 2L + entry.lastPositionMs
        }
        val totalMinutes = (totalMs / 60_000L).coerceAtLeast(if (totalPlays > 0) 1L else 0L)
        val completionPercent = if (totalPlays > 0) {
            ((totalCompleted * 100) / totalPlays).coerceIn(0, 100)
        } else {
            0
        }

        val topTracks = history
            .sortedByDescending { it.playCount }
            .take(5)
            .map { it.track to it.playCount }

        val artistPlayCounts = mutableMapOf<String, Int>()
        history.forEach { entry ->
            val artist = entry.track.artist
            artistPlayCounts[artist] = (artistPlayCounts[artist] ?: 0) + entry.playCount
        }
        if (artistPlayCounts.isEmpty()) {
            allTracks.forEach { track ->
                artistPlayCounts[track.artist] = (artistPlayCounts[track.artist] ?: 0) + 1
            }
        }
        val topArtists = artistPlayCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val hiResCount = allTracks.count { it.sampleRateHz >= 48_000 || it.bitrateKbps >= 320 }
        val favCount = allTracks.count { it.isFavorite }

        return ListeningStatsSummary(
            totalListeningMinutes = totalMinutes,
            totalPlaysCount = totalPlays,
            totalCompletedCount = totalCompleted,
            completionRatePercent = completionPercent,
            topTracks = topTracks,
            topArtists = topArtists,
            hiResTracksCount = hiResCount,
            favoriteTracksCount = favCount
        )
    }

    fun evaluateSmartRule(
        rule: SmartRuleCriterion,
        allTracks: List<Track>,
        history: List<PlaybackHistoryEntry>
    ): SmartPlaylist {
        val historyMap = history.associateBy { it.track.id }
        val matched = allTracks.filter { track ->
            val plays = historyMap[track.id]?.playCount ?: 0
            val matchesBitrate = track.bitrateKbps >= rule.minBitrateKbps
            val matchesSampleRate = track.sampleRateHz >= rule.minSampleRateHz
            val matchesPlays = plays >= rule.minPlayCount
            val matchesFav = !rule.favoritesOnly || track.isFavorite
            val matchesArtist = rule.artistKeyword.isBlank() ||
                track.artist.contains(rule.artistKeyword.trim(), ignoreCase = true) ||
                track.title.contains(rule.artistKeyword.trim(), ignoreCase = true)

            matchesBitrate && matchesSampleRate && matchesPlays && matchesFav && matchesArtist
        }

        val reasonEn = buildString {
            append("Rule: ")
            if (rule.minBitrateKbps > 0) append(">= ${rule.minBitrateKbps}kbps ")
            if (rule.minSampleRateHz > 0) append(">= ${rule.minSampleRateHz / 1000}kHz ")
            if (rule.minPlayCount > 0) append("Plays >= ${rule.minPlayCount} ")
            if (rule.favoritesOnly) append("★ Favorites ")
            if (rule.artistKeyword.isNotBlank()) append("Keyword '${rule.artistKeyword}'")
        }.trim()

        return SmartPlaylist(
            category = SmartPlaylistCategory.DAILY_SAFFRON_MIX,
            tracks = matched,
            explainReasonEn = "${rule.name} • $reasonEn",
            explainReasonFa = "${rule.name} • فیلتر قانون‌مند هوشمند (${matched.size} قطعه)"
        )
    }

    fun formatLyricsAsLrc(lines: List<SyncedLyricLine>, trackTitle: String = "VELORA Track"): String {
        val sb = StringBuilder()
        sb.appendLine("[ti:$trackTitle]")
        sb.appendLine("[by:VELORA Studio LRC Synchronizer]")
        lines.sortedBy { it.timestampMs }.forEach { line ->
            val totalSec = (line.timestampMs.coerceAtLeast(0L)) / 1000L
            val mins = totalSec / 60L
            val secs = totalSec % 60L
            val hundredths = ((line.timestampMs.coerceAtLeast(0L)) % 1000L) / 10L
            val tag = String.format(Locale.US, "[%02d:%02d.%02d]", mins, secs, hundredths)
            sb.appendLine("$tag ${line.text}")
        }
        return sb.toString().trimEnd()
    }

    fun exportPlaylistToM3u8(playlistName: String, tracks: List<Track>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:$playlistName")
        tracks.forEach { track ->
            val durationSec = (track.durationMs / 1000L).coerceAtLeast(1L)
            sb.appendLine("#EXTINF:$durationSec,${track.artist} - ${track.title}")
            sb.appendLine(track.uri)
        }
        return sb.toString().trimEnd()
    }

    fun parseM3u8Content(rawM3u: String, availableTracks: List<Track>): List<Track> {
        if (rawM3u.isBlank()) return emptyList()
        val matchedTracks = mutableListOf<Track>()
        val lines = rawM3u.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var pendingTitleHint: String? = null
        for (line in lines) {
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                pendingTitleHint = line.substringAfter(',', "").trim()
            } else if (!line.startsWith("#")) {
                val uriCandidate = line
                val byUri = availableTracks.find {
                    it.uri.equals(uriCandidate, ignoreCase = true) ||
                        it.uri.endsWith(uriCandidate.substringAfterLast('/'), ignoreCase = true)
                }
                val byTitle = if (byUri == null && !pendingTitleHint.isNullOrBlank()) {
                    val cleanHint = pendingTitleHint.substringAfter(" - ").trim()
                    availableTracks.find {
                        it.title.contains(cleanHint, ignoreCase = true) ||
                            pendingTitleHint.contains(it.title, ignoreCase = true)
                    }
                } else {
                    null
                }
                val resolved = byUri ?: byTitle
                if (resolved != null) {
                    matchedTracks.add(resolved)
                }
                pendingTitleHint = null
            }
        }
        return matchedTracks.distinctBy { it.id }
    }
}

enum class GestureActionType {
    SEEK_BACKWARD,
    SEEK_FORWARD,
    TOGGLE_PLAY_PAUSE,
    ADJUST_BRIGHTNESS,
    ADJUST_VOLUME
}

data class GestureSeekResult(
    val action: GestureActionType,
    val targetPositionMs: Long,
    val deltaSeconds: Int,
    val hudLabel: String
)

data class GestureSwipeResult(
    val action: GestureActionType,
    val newValue: Float,
    val percentInt: Int,
    val hudLabel: String
)

object VeloraGestureEngine {
    const val DEFAULT_SEEK_STEP_MS: Long = 10_000L
    const val LEFT_PHI_ZONE_RATIO: Float = 0.382f
    const val RIGHT_PHI_ZONE_RATIO: Float = 0.618f

    fun resolveDoubleTapGesture(
        tapX: Float,
        surfaceWidth: Float,
        currentPositionMs: Long,
        durationMs: Long,
        seekStepMs: Long = DEFAULT_SEEK_STEP_MS
    ): GestureSeekResult {
        val safeWidth = surfaceWidth.coerceAtLeast(1f)
        val normalizedX = (tapX / safeWidth).coerceIn(0f, 1f)
        val stepSec = (seekStepMs / 1000L).toInt()
        val effectiveMax = if (durationMs > 0L) durationMs else Long.MAX_VALUE

        return when {
            normalizedX < LEFT_PHI_ZONE_RATIO -> {
                val target = (currentPositionMs - seekStepMs).coerceAtLeast(0L)
                GestureSeekResult(
                    action = GestureActionType.SEEK_BACKWARD,
                    targetPositionMs = target,
                    deltaSeconds = -stepSec,
                    hudLabel = "⏪ -${stepSec}s"
                )
            }
            normalizedX > RIGHT_PHI_ZONE_RATIO -> {
                val target = (currentPositionMs + seekStepMs).coerceAtMost(effectiveMax)
                GestureSeekResult(
                    action = GestureActionType.SEEK_FORWARD,
                    targetPositionMs = target,
                    deltaSeconds = stepSec,
                    hudLabel = "⏩ +${stepSec}s"
                )
            }
            else -> {
                GestureSeekResult(
                    action = GestureActionType.TOGGLE_PLAY_PAUSE,
                    targetPositionMs = currentPositionMs,
                    deltaSeconds = 0,
                    hudLabel = "⏯ Play / Pause"
                )
            }
        }
    }

    fun resolveVerticalSwipeGesture(
        startX: Float,
        surfaceWidth: Float,
        surfaceHeight: Float,
        dragDeltaY: Float,
        currentBrightness: Float,
        currentVolume: Float
    ): GestureSwipeResult {
        val safeWidth = surfaceWidth.coerceAtLeast(1f)
        val safeHeight = surfaceHeight.coerceAtLeast(1f)
        val normalizedDelta = (-dragDeltaY / safeHeight).coerceIn(-0.20f, 0.20f)
        val isLeftZone = startX < safeWidth * 0.5f

        return if (isLeftZone) {
            val newBri = (currentBrightness + normalizedDelta).coerceIn(0.15f, 1.0f)
            val pct = (newBri * 100f).toInt().coerceIn(15, 100)
            GestureSwipeResult(
                action = GestureActionType.ADJUST_BRIGHTNESS,
                newValue = newBri,
                percentInt = pct,
                hudLabel = "☀️ $pct%"
            )
        } else {
            val newVol = (currentVolume + normalizedDelta).coerceIn(0.0f, 1.0f)
            val pct = (newVol * 100f).toInt().coerceIn(0, 100)
            GestureSwipeResult(
                action = GestureActionType.ADJUST_VOLUME,
                newValue = newVol,
                percentInt = pct,
                hudLabel = "🔊 $pct%"
            )
        }
    }
}

