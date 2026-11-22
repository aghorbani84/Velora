package com.example.domain.engine

import com.example.domain.model.AudioTrackInfo
import com.example.domain.model.EqSpecification
import com.example.domain.model.SubtitleTrackInfo
import com.example.domain.model.Track
import com.example.domain.model.VeloraCompatibilityMatrix
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VisualizerMode
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

data class EnginePlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTrack: Track? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val eqEnabled: Boolean = true,
    val eqBandGainsDb: List<Float> = List(EqSpecification.BAND_COUNT) { 0f },
    val spectrumBandsDb: List<Float> = List(EqSpecification.BAND_COUNT) { -60f },
    val spectrumPeaksDb: List<Float> = List(EqSpecification.BAND_COUNT) { -60f },
    val waveformSamples: List<Float> = List(EqSpecification.WAVEFORM_POINT_COUNT) { 0f },
    val rmsLevelDb: Float = -60f,
    val peakLevelDb: Float = -60f,
    val visualizerMode: VisualizerMode = VisualizerMode.DUAL_STUDIO,
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList(),
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitlesEnabled: Boolean = true,
    val videoAspectMode: VideoAspectMode = VideoAspectMode.FIT,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioBitDepth: Int = 0,
    val audioChannelCount: Int = 0,
    val engineAvailable: Boolean = false,
    val audioSessionId: Int = 0,
    val errorMessage: String? = null
) {
    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING

    fun audioSpecSummary(track: Track, formatBadge: String): String {
        val parts = mutableListOf(
            formatBadge,
            "${track.sampleRateHz}Hz",
            "${track.bitrateKbps} kbps"
        )
        if (audioBitDepth > 0) {
            parts.add("${audioBitDepth}-bit")
        } else if (VeloraCompatibilityMatrix.inspectMediaFormat(track.uri, track.mediaType).isLosslessOrHiRes) {
            parts.add("Lossless")
        }
        if (audioChannelCount > 0) {
            parts.add(formatAudioChannelLabel(audioChannelCount))
        }
        return parts.joinToString(" • ")
    }
}

fun formatAudioChannelLabel(channelCount: Int): String = when (channelCount) {
    1 -> "Mono"
    2 -> "Stereo"
    6 -> "5.1"
    8 -> "7.1"
    else -> "${channelCount}ch"
}

interface PlayerEngine {
    val playbackState: StateFlow<EnginePlaybackState>

    fun loadAndPlay(track: Track, startPositionMs: Long = 0L)
    fun play()
    fun pause()
    fun togglePlayPause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setMuted(muted: Boolean)
    fun toggleMute()
    fun setPlaybackSpeed(speed: Float)
    fun setEqEnabled(enabled: Boolean)
    fun setEqBandGains(gainsDb: List<Float>)
    fun setVisualizerMode(mode: VisualizerMode)
    fun setSubtitlesEnabled(enabled: Boolean)
    fun selectSubtitleTrack(trackId: String?)

    /** Buffered position of the current media in ms; 0 when the engine cannot tell. */
    fun bufferedPositionMs(): Long = 0L
    fun selectAudioTrack(trackId: String)
    fun setVideoAspectMode(mode: VideoAspectMode)
    fun clearError()
    fun setOnTrackEndedListener(listener: () -> Unit)
    fun release()
}
