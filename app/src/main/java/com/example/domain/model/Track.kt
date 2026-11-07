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

