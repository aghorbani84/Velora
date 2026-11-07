package com.example.orchestrator

import androidx.media3.common.MediaItem
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.MediaOrchestratorState
import com.example.domain.engine.PlaybackStatus
import com.example.domain.engine.VeloraMediaEvent
import com.example.domain.model.EqPreset
import com.example.domain.model.MediaFormatDescriptor
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.Track
import com.example.domain.model.VeloraCompatibilityMatrix
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VisualizerMode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Observable playback state snapshot exposed by [IMediaOrchestrator] for UI components.
 */
data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTrack: Track? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val queue: PlaybackQueue = PlaybackQueue(),
    val engineState: EnginePlaybackState = EnginePlaybackState(),
    val activeFormat: MediaFormatDescriptor? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun fromOrchestratorState(state: MediaOrchestratorState): PlaybackState {
            val engine = state.engineState
            val track = engine.currentTrack ?: state.queue.currentTrack
            return PlaybackState(
                status = engine.status,
                currentTrack = track,
                positionMs = engine.positionMs,
                durationMs = engine.durationMs,
                isPlaying = engine.isPlaying,
                volume = engine.volume,
                isMuted = engine.isMuted,
                playbackSpeed = engine.playbackSpeed,
                queue = state.queue,
                engineState = engine,
                activeFormat = state.activeFormat,
                errorMessage = engine.errorMessage
            )
        }
    }
}

/**
 * Contract for the VELORA central media orchestration layer coordinating playback,
 * queue transitions, storage persistence, and observable [PlaybackState].
 */
interface IMediaOrchestrator {
    /**
     * Observable [PlaybackState] flow for UI components to consume.
     */
    val playbackState: StateFlow<PlaybackState>

    /**
     * Full orchestrator state including synchronized lyrics, bookmarks, and declarative plugins.
     */
    val orchestratorState: StateFlow<MediaOrchestratorState>

    /**
     * Stream of discrete media lifecycle and diagnostic events.
     */
    val events: SharedFlow<VeloraMediaEvent>

    /**
     * Plays the specified [Track] media item, optionally seeding the queue from [contextTracks]
     * and starting at [startPositionMs].
     */
    fun play(
        mediaItem: Track,
        contextTracks: List<Track> = emptyList(),
        startPositionMs: Long = 0L
    )

    /**
     * Plays an AndroidX Media3 [MediaItem] by converting its URI and metadata into a VELORA [Track].
     */
    fun play(mediaItem: MediaItem, startPositionMs: Long = 0L)

    /**
     * Resumes playback of the active media item if currently paused.
     */
    fun resume()

    /**
     * Pauses active playback and emits a pause telemetry event.
     */
    fun pause()

    /**
     * Toggles between playing and paused states, falling back to [fallbackTracks] if idle.
     */
    fun togglePlayPause(fallbackTracks: List<Track> = emptyList())

    /**
     * Advances to the next media item in the active queue according to shuffle/repeat rules.
     */
    fun skipToNext()

    /**
     * Returns to the previous media item or restarts the current track depending on position.
     */
    fun skipToPrevious()

    /**
     * Stops playback and resets the active engine buffer.
     */
    fun stop()

    /**
     * Seeks the current media item to [position] (in milliseconds) and persists progress.
     */
    fun seekTo(position: Long)

    suspend fun bootstrapAndRestoreSession()
    fun ensureQueueSeeded(tracks: List<Track>)
    fun postBanner(message: String?)
    fun playTrackPool(tracks: List<Track>, shuffle: Boolean = false, bannerMessage: String? = null)
    fun addTrackToQueue(track: Track)
    fun removeTrackFromQueue(trackId: String)
    fun selectQueueTrack(track: Track)
    fun toggleShuffle()
    fun cycleRepeatMode()
    fun close()
}

fun IMediaOrchestrator.setEqEnabled(enabled: Boolean) {
    (this as? com.example.domain.engine.MediaOrchestrator)?.setEqEnabled(enabled)
}

fun IMediaOrchestrator.setEqBandGain(bandIndex: Int, gainDb: Float) {
    (this as? com.example.domain.engine.MediaOrchestrator)?.setEqBandGain(bandIndex, gainDb)
}

fun IMediaOrchestrator.applyEqPreset(preset: EqPreset) {
    (this as? com.example.domain.engine.MediaOrchestrator)?.applyEqPreset(preset)
}

fun IMediaOrchestrator.adjustLyricsOffset(deltaMs: Long) {
    (this as? com.example.domain.engine.MediaOrchestrator)?.adjustLyricsOffset(deltaMs)
}

fun IMediaOrchestrator.addTimeMachineBookmark(label: String = "Studio Marker") {
    (this as? com.example.domain.engine.MediaOrchestrator)?.addTimeMachineBookmark(label)
}

fun IMediaOrchestrator.toggleDeclarativePlugin(pluginId: String) {
    (this as? com.example.domain.engine.MediaOrchestrator)?.toggleDeclarativePlugin(pluginId)
}

fun IMediaOrchestrator.dismissBanner() {
    postBanner(null)
}

fun IMediaOrchestrator.playTrack(
    track: Track,
    contextTracks: List<Track> = emptyList(),
    startPositionMs: Long = 0L
) {
    play(track, contextTracks, startPositionMs)
}

fun IMediaOrchestrator.playPlaylist(playlist: Playlist, shuffle: Boolean = false) {
    if (playlist.tracks.isEmpty()) {
        postBanner("Playlist '${playlist.name}' has no tracks yet")
        return
    }
    playTrackPool(playlist.tracks, shuffle)
}

fun IMediaOrchestrator.playSmartPlaylist(smartPlaylist: SmartPlaylist, shuffle: Boolean = false) {
    if (smartPlaylist.tracks.isEmpty()) return
    playTrackPool(
        tracks = smartPlaylist.tracks,
        shuffle = shuffle,
        bannerMessage = "Playing Smart Mix: ${smartPlaylist.category.titleEn}"
    )
}

fun IMediaOrchestrator.playNetworkStream(
    streamUrl: String,
    customTitle: String = "VELORA Live Stream"
) {
    if (!VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl(streamUrl)) {
        postBanner("Invalid or unauthorized stream URL")
        return
    }
    val descriptor = VeloraCompatibilityMatrix.inspectMediaFormat(streamUrl)
    val streamTrack = Track(
        id = "stream_${streamUrl.hashCode()}",
        mediaStoreId = (streamUrl.hashCode().toLong() and 0x7FFFFFFFL),
        title = customTitle.trim().ifBlank { "VELORA Network Stream (${descriptor.containerName})" },
        artist = descriptor.containerName,
        album = descriptor.primaryCodec,
        durationMs = 180_000L,
        uri = streamUrl.trim(),
        folderPath = "network://stream",
        mediaType = descriptor.mediaType,
        addedTimestampMs = System.currentTimeMillis(),
        sampleRateHz = 48_000,
        bitrateKbps = 320
    )
    play(streamTrack, listOf(streamTrack), 0L)
    postBanner("Streaming ${descriptor.containerName} (${descriptor.primaryCodec})")
}

fun IMediaOrchestrator.resumeFromHistory(entry: PlaybackHistoryEntry, fallbackPool: List<Track>) {
    val maxResumeWindow = (entry.track.durationMs - 1_500L).coerceAtLeast(1L)
    val resumePos = if (entry.lastPositionMs in 1L until maxResumeWindow) {
        entry.lastPositionMs
    } else {
        0L
    }
    play(entry.track, fallbackPool, resumePos)
}

typealias MediaOrchestrator = com.example.domain.engine.MediaOrchestrator
