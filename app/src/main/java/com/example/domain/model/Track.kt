package com.example.domain.model

enum class MediaType {
    AUDIO,
    VIDEO
}

data class Track(
    val id: String,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val folderPath: String,
    val mediaType: MediaType,
    val artworkUri: String? = null,
    val addedTimestampMs: Long = 0L,
    val sampleRateHz: Int = 44100,
    val bitrateKbps: Int = 320,
    val isFavorite: Boolean = false
) {
    val durationFormatted: String
        get() = formatDurationMs(durationMs)

    fun isLikelyValidSong(): Boolean {
        if (mediaType == MediaType.VIDEO) return true
        if (durationMs < 35_000L) return false

        val titleLower = title.lowercase()
        val uriLower = uri.lowercase()

        val isChatAudio = titleLower.startsWith("aud-") || titleLower.startsWith("ptt-") ||
            listOf("whatsapp", "/ptt/", "/opus/").any { uriLower.contains(it) }
        if (isChatAudio) return false

        val isRecording = listOf("rec_", "rec-", "recording", "voice ", "voice_", "call_").any {
            titleLower.startsWith(it)
        } || listOf("call_rec", "sound_recorder", "voicenotes").any {
            uriLower.contains(it)
        }
        if (isRecording) return false

        val isSystemSound = listOf("/notifications/", "/ringtones/", "/alarms/", "/ui/").any {
            uriLower.contains(it)
        }
        if (isSystemSound) return false

        return title.isNotBlank()
    }

    companion object {
        fun formatDurationMs(ms: Long): String {
            val clamped = ms.coerceAtLeast(0L)
            val totalSeconds = clamped / 1000L
            val hours = totalSeconds / 3600L
            val minutes = (totalSeconds % 3600L) / 60L
            val seconds = totalSeconds % 60L
            return if (hours > 0L) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
    }
}

data class PlaybackHistoryEntry(
    val track: Track,
    val lastPlayedAtMs: Long,
    val lastPositionMs: Long,
    val playCount: Int,
    val completedCount: Int = 0
) {
    val lastPositionFormatted: String
        get() = Track.formatDurationMs(lastPositionMs)
}

