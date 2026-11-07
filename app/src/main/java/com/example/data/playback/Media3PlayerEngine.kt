package com.example.data.playback

import android.content.Context
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.PlaybackStatus
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.AudioTrackInfo
import com.example.domain.model.BiquadEqDsp
import com.example.domain.model.EqSpecification
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.SubtitleTrackInfo
import com.example.domain.model.Track
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VisualizerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
class Media3PlayerEngine(
    private val context: Context,
    private val mainScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : PlayerEngine {

    val biquadProcessor = TenBandBiquadAudioProcessor()

    private val _playbackState = MutableStateFlow(EnginePlaybackState())
    override val playbackState: StateFlow<EnginePlaybackState> = _playbackState.asStateFlow()

    private var exoPlayerInstance: ExoPlayer? = null
    private var hardwareEqualizer: Equalizer? = null
    private var positionTickerJob: Job? = null
    private var spectrumCollectorJob: Job? = null
    private var onTrackEndedCallback: (() -> Unit)? = null
    private var preMuteVolume: Float = 1.0f

    init {
        initializeExoPlayer()
    }

    val exoPlayer: ExoPlayer?
        get() = exoPlayerInstance

    private fun initializeExoPlayer() {
        try {
            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink {
                    return DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .setAudioProcessors(arrayOf(biquadProcessor))
                        .build()
                }
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val player = ExoPlayer.Builder(context, renderersFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build()

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    syncStateFromPlayer(player)
                    if (playbackState == Player.STATE_ENDED) {
                        onTrackEndedCallback?.invoke()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    syncStateFromPlayer(player)
                    if (isPlaying) {
                        startPositionTicker()
                    } else {
                        stopPositionTicker()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "ExoPlayer playback failure: ${error.errorCodeName}", error)
                    stopPositionTicker()
                    _playbackState.update { state ->
                        state.copy(
                            status = PlaybackStatus.ERROR,
                            errorMessage = "Playback failed (${error.errorCodeName}): ${error.localizedMessage ?: "Unsupported or corrupt media stream"}"
                        )
                    }
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    _playbackState.update { it.copy(audioSessionId = audioSessionId) }
                    attachHardwareEqualizerIfSupported(audioSessionId)
                }

                override fun onTracksChanged(tracks: Tracks) {
                    extractAvailableTracks(tracks)
                }
            })

            exoPlayerInstance = player
            _playbackState.update {
                it.copy(
                    engineAvailable = true,
                    audioSessionId = player.audioSessionId
                )
            }

            spectrumCollectorJob?.cancel()
            spectrumCollectorJob = mainScope.launch {
                biquadProcessor.visualizerSnapshot.collect { snapshot ->
                    if (_playbackState.value.isPlaying) {
                        _playbackState.update {
                            it.copy(
                                spectrumBandsDb = snapshot.spectrumBandsDb,
                                spectrumPeaksDb = snapshot.spectrumPeaksDb,
                                waveformSamples = snapshot.waveformSamples,
                                rmsLevelDb = snapshot.rmsLevelDb,
                                peakLevelDb = snapshot.peakLevelDb
                            )
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "ExoPlayer native initialization unavailable in this environment: ${e.message}")
            _playbackState.update {
                it.copy(engineAvailable = false)
            }
        }
    }

    private fun attachHardwareEqualizerIfSupported(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return
        try {
            hardwareEqualizer?.release()
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = _playbackState.value.eqEnabled
            hardwareEqualizer = eq
            syncHardwareEqualizerBands(_playbackState.value.eqBandGainsDb)
        } catch (e: Throwable) {
            Log.d(TAG, "Hardware Equalizer effect not attached: ${e.message}")
        }
    }

    private fun syncHardwareEqualizerBands(gainsDb: List<Float>) {
        val eq = hardwareEqualizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: -1200
            val maxMb = range.getOrNull(1)?.toInt() ?: 1200
            val mappedMillibels = BiquadEqDsp.mapTenBandsToHardwareMillibels(
                gainsDb = gainsDb,
                hardwareBandCount = numBands,
                minMillibels = minMb,
                maxMillibels = maxMb
            )
            for (b in 0 until numBands) {
                eq.setBandLevel(b.toShort(), mappedMillibels[b].toShort())
            }
        } catch (_: Throwable) {
        }
    }

    private fun extractAvailableTracks(tracks: Tracks) {
        val subtitles = mutableListOf<SubtitleTrackInfo>()
        val audios = mutableListOf<AudioTrackInfo>()

        for ((groupIdx, group) in tracks.groups.withIndex()) {
            when (group.type) {
                C.TRACK_TYPE_TEXT -> {
                    for (trackIdx in 0 until group.length) {
                        val format = group.getTrackFormat(trackIdx)
                        val id = "$groupIdx:$trackIdx"
                        val label = format.label ?: format.language ?: "Subtitle ${subtitles.size + 1}"
                        subtitles.add(
                            SubtitleTrackInfo(
                                id = id,
                                label = label,
                                language = format.language ?: "und",
                                isSelected = group.isTrackSelected(trackIdx)
                            )
                        )
                    }
                }
                C.TRACK_TYPE_AUDIO -> {
                    for (trackIdx in 0 until group.length) {
                        val format = group.getTrackFormat(trackIdx)
                        val id = "$groupIdx:$trackIdx"
                        val label = format.label ?: format.language ?: "Audio ${audios.size + 1}"
                        audios.add(
                            AudioTrackInfo(
                                id = id,
                                label = label,
                                language = format.language ?: "und",
                                isSelected = group.isTrackSelected(trackIdx)
                            )
                        )
                    }
                }
            }
        }

        _playbackState.update {
            it.copy(
                subtitleTracks = subtitles,
                audioTracks = audios
            )
        }
    }

    override fun loadAndPlay(track: Track, startPositionMs: Long) {
        if (track.uri.startsWith("/") || track.uri.startsWith("file://")) {
            val path = track.uri.removePrefix("file://")
            val file = File(path)
            if (!file.exists() || !file.canRead() || file.length() == 0L) {
                _playbackState.update {
                    it.copy(
                        currentTrack = track,
                        status = PlaybackStatus.ERROR,
                        positionMs = 0L,
                        durationMs = track.durationMs.coerceAtLeast(0L),
                        errorMessage = "Media file not found or unreadable: ${track.title}"
                    )
                }
                return
            }
        }

        val clampedStart = PlaybackQueue.clampSeekPosition(startPositionMs, track.durationMs)
        _playbackState.update {
            it.copy(
                currentTrack = track,
                positionMs = clampedStart,
                durationMs = track.durationMs.coerceAtLeast(0L),
                status = PlaybackStatus.BUFFERING,
                errorMessage = null
            )
        }

        val player = exoPlayerInstance
        if (player == null) {
            _playbackState.update {
                it.copy(
                    status = PlaybackStatus.PLAYING,
                    positionMs = clampedStart
                )
            }
            return
        }

        val parsedUri = if (track.uri.startsWith("/")) {
            Uri.fromFile(File(track.uri))
        } else {
            Uri.parse(track.uri)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(parsedUri)
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem, clampedStart)
        player.prepare()
        player.playWhenReady = true
    }

    override fun play() {
        val current = _playbackState.value
        if (current.status == PlaybackStatus.ERROR) return
        val player = exoPlayerInstance
        if (player != null) {
            if (player.playbackState == Player.STATE_IDLE && current.currentTrack != null) {
                loadAndPlay(current.currentTrack, current.positionMs)
            } else {
                player.play()
            }
        } else if (current.currentTrack != null) {
            _playbackState.update { it.copy(status = PlaybackStatus.PLAYING) }
        }
    }

    override fun pause() {
        exoPlayerInstance?.pause()
        _playbackState.update { state ->
            if (state.status == PlaybackStatus.PLAYING || state.status == PlaybackStatus.BUFFERING) {
                state.copy(status = PlaybackStatus.PAUSED)
            } else {
                state
            }
        }
    }

    override fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun stop() {
        stopPositionTicker()
        exoPlayerInstance?.stop()
        _playbackState.update {
            it.copy(
                status = PlaybackStatus.IDLE,
                positionMs = 0L,
                spectrumBandsDb = List(EqSpecification.BAND_COUNT) { -60f },
                spectrumPeaksDb = List(EqSpecification.BAND_COUNT) { -60f },
                waveformSamples = List(EqSpecification.WAVEFORM_POINT_COUNT) { 0f },
                rmsLevelDb = -60f,
                peakLevelDb = -60f
            )
        }
    }

    override fun seekTo(positionMs: Long) {
        val state = _playbackState.value
        val effectiveDuration = state.durationMs.takeIf { it > 0L }
            ?: state.currentTrack?.durationMs
            ?: 0L
        val clamped = PlaybackQueue.clampSeekPosition(positionMs, effectiveDuration)
        exoPlayerInstance?.seekTo(clamped)
        _playbackState.update { it.copy(positionMs = clamped) }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        if (clamped > 0f) {
            preMuteVolume = clamped
        }
        val muted = clamped == 0.0f
        exoPlayerInstance?.volume = clamped
        _playbackState.update {
            it.copy(
                volume = clamped,
                isMuted = muted
            )
        }
    }

    override fun setMuted(muted: Boolean) {
        if (muted) {
            val currentVol = _playbackState.value.volume
            if (currentVol > 0f) preMuteVolume = currentVol
            exoPlayerInstance?.volume = 0f
            _playbackState.update { it.copy(isMuted = true, volume = 0f) }
        } else {
            val restored = preMuteVolume.coerceIn(0.1f, 1.0f)
            exoPlayerInstance?.volume = restored
            _playbackState.update { it.copy(isMuted = false, volume = restored) }
        }
    }

    override fun toggleMute() {
        setMuted(!_playbackState.value.isMuted)
    }

    override fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        exoPlayerInstance?.playbackParameters = PlaybackParameters(safeSpeed)
        _playbackState.update { it.copy(playbackSpeed = safeSpeed) }
    }

    override fun setEqEnabled(enabled: Boolean) {
        biquadProcessor.setEnabled(enabled)
        try {
            hardwareEqualizer?.enabled = enabled
        } catch (_: Throwable) {
        }
        _playbackState.update { it.copy(eqEnabled = enabled) }
    }

    override fun setEqBandGains(gainsDb: List<Float>) {
        val normalized = EqSpecification.normalizeGains(gainsDb)
        biquadProcessor.setBandGains(normalized)
        syncHardwareEqualizerBands(normalized)
        _playbackState.update { it.copy(eqBandGainsDb = normalized) }
    }

    override fun setVisualizerMode(mode: VisualizerMode) {
        _playbackState.update { it.copy(visualizerMode = mode) }
    }

    override fun setSubtitlesEnabled(enabled: Boolean) {
        val player = exoPlayerInstance
        if (player != null) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                .build()
        }
        _playbackState.update { it.copy(subtitlesEnabled = enabled) }
    }

    override fun selectSubtitleTrack(trackId: String?) {
        val player = exoPlayerInstance ?: return
        if (trackId == null) {
            setSubtitlesEnabled(false)
            return
        }
        setSubtitlesEnabled(true)
        applyTrackSelectionOverride(player, C.TRACK_TYPE_TEXT, trackId)
    }

    override fun selectAudioTrack(trackId: String) {
        val player = exoPlayerInstance ?: return
        applyTrackSelectionOverride(player, C.TRACK_TYPE_AUDIO, trackId)
    }

    private fun applyTrackSelectionOverride(player: ExoPlayer, trackType: Int, trackId: String) {
        val parts = trackId.split(":")
        val groupIdx = parts.getOrNull(0)?.toIntOrNull() ?: return
        val trackIdx = parts.getOrNull(1)?.toIntOrNull() ?: return
        val groups = player.currentTracks.groups
        if (groupIdx !in groups.indices) return
        val group = groups[groupIdx]
        if (group.type != trackType || trackIdx !in 0 until group.length) return
        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIdx))
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
    }

    override fun setVideoAspectMode(mode: VideoAspectMode) {
        _playbackState.update { it.copy(videoAspectMode = mode) }
    }

    override fun clearError() {
        _playbackState.update {
            it.copy(
                errorMessage = null,
                status = if (it.status == PlaybackStatus.ERROR) PlaybackStatus.IDLE else it.status
            )
        }
    }

    override fun setOnTrackEndedListener(listener: () -> Unit) {
        onTrackEndedCallback = listener
    }

    private fun syncStateFromPlayer(player: ExoPlayer) {
        val rawDuration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
            ?: _playbackState.value.currentTrack?.durationMs
            ?: 0L
        val rawPosition = player.currentPosition.coerceAtLeast(0L)
        val clampedPos = PlaybackQueue.clampSeekPosition(rawPosition, rawDuration)

        val mappedStatus = when {
            player.playerError != null -> PlaybackStatus.ERROR
            player.playbackState == Player.STATE_ENDED -> PlaybackStatus.ENDED
            player.isPlaying -> PlaybackStatus.PLAYING
            player.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
            player.playbackState == Player.STATE_READY -> PlaybackStatus.PAUSED
            else -> PlaybackStatus.IDLE
        }

        _playbackState.update { state ->
            state.copy(
                status = mappedStatus,
                positionMs = clampedPos,
                durationMs = rawDuration
            )
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = mainScope.launch {
            while (isActive) {
                val player = exoPlayerInstance
                if (player != null && player.isPlaying) {
                    syncStateFromPlayer(player)
                }
                delay(100L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    override fun release() {
        stopPositionTicker()
        spectrumCollectorJob?.cancel()
        try {
            hardwareEqualizer?.release()
        } catch (_: Throwable) {
        }
        hardwareEqualizer = null
        exoPlayerInstance?.release()
        exoPlayerInstance = null
    }

    companion object {
        private const val TAG = "Media3PlayerEngine"
    }
}
