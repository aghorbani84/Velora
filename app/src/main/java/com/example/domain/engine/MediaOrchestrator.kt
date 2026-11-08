package com.example.domain.engine

import androidx.media3.common.MediaItem
import com.example.data.repository.VeloraLibraryRepository
import com.example.data.settings.VeloraSettingsRepository
import com.example.domain.model.DeclarativePluginManifest
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
import com.example.domain.model.MediaFormatDescriptor
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.QueueMode
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.TimeMachineBookmark
import com.example.domain.model.Track
import com.example.domain.model.VeloraCompatibilityMatrix
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VeloraPluginRegistry
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VisualizerMode
import com.example.orchestrator.IMediaOrchestrator
import com.example.orchestrator.PlaybackState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Domain events emitted by [MediaOrchestrator] for observability, UI banners, and plugin hooks.
 */
sealed interface VeloraMediaEvent {
    data class PlaybackStarted(val track: Track, val format: MediaFormatDescriptor) : VeloraMediaEvent
    data class PlaybackPaused(val track: Track?, val positionMs: Long) : VeloraMediaEvent
    data class PlaybackCompleted(val trackId: String) : VeloraMediaEvent
    data class MediaDiscovered(val trackCount: Int) : VeloraMediaEvent
    data class LyricsResolved(val trackId: String, val lineCount: Int) : VeloraMediaEvent
    data class BookmarkAdded(val bookmark: TimeMachineBookmark) : VeloraMediaEvent
    data class PluginToggled(val pluginId: String, val isEnabled: Boolean) : VeloraMediaEvent
    data class StatusNotification(val message: String) : VeloraMediaEvent
}

/**
 * Unified, immutable snapshot of orchestrated playback, queue, lyrics, bookmarks, and active format.
 */
data class MediaOrchestratorState(
    val queue: PlaybackQueue = PlaybackQueue(),
    val engineState: EnginePlaybackState = EnginePlaybackState(),
    val activeFormat: MediaFormatDescriptor? = null,
    val activeLyrics: List<SyncedLyricLine> = emptyList(),
    val lyricsOffsetMs: Long = 0L,
    val timeMachineBookmarks: List<TimeMachineBookmark> = emptyList(),
    val declarativePlugins: List<DeclarativePluginManifest> = VeloraPluginRegistry.DEFAULT_DECLARATIVE_PLUGINS,
    val lastBannerMessage: String? = null
)

/**
 * Central coordination layer between [PlayerEngine], [VeloraLibraryRepository] (Room),
 * and [VeloraSettingsRepository] (DataStore), implementing [IMediaOrchestrator].
 */
class MediaOrchestrator(
    private val repository: VeloraLibraryRepository,
    private val settingsRepository: VeloraSettingsRepository,
    val playerEngine: PlayerEngine,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : IMediaOrchestrator {

    private val orchestratorScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val queueFlow = MutableStateFlow(PlaybackQueue())
    private val lyricsOffsetMsFlow = MutableStateFlow(0L)
    private val bookmarksFlow = MutableStateFlow<List<TimeMachineBookmark>>(emptyList())
    private val pluginsFlow = MutableStateFlow(VeloraPluginRegistry.DEFAULT_DECLARATIVE_PLUGINS)
    private val bannerFlow = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<VeloraMediaEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<VeloraMediaEvent> = _events.asSharedFlow()

    private var initialRestoreCompleted = false

    override val orchestratorState: StateFlow<MediaOrchestratorState> = combine(
        queueFlow,
        playerEngine.playbackState,
        lyricsOffsetMsFlow,
        bookmarksFlow,
        pluginsFlow,
        bannerFlow,
        transform = ::buildOrchestratorState
    ).stateIn(
        scope = orchestratorScope,
        started = SharingStarted.Eagerly,
        initialValue = MediaOrchestratorState()
    )

    override val playbackState: StateFlow<PlaybackState> = orchestratorState
        .map(PlaybackState::fromOrchestratorState)
        .stateIn(
            scope = orchestratorScope,
            started = SharingStarted.Eagerly,
            initialValue = PlaybackState.fromOrchestratorState(orchestratorState.value)
        )

    init {
        playerEngine.setOnTrackEndedListener {
            handleTrackAutomaticallyEnded()
        }
    }

    override suspend fun bootstrapAndRestoreSession() {
        runCatching {
            val savedSettings = settingsRepository.getSettingsOnce()
            playerEngine.setEqEnabled(savedSettings.eqEnabled)
            playerEngine.setEqBandGains(savedSettings.customEqGainsDb)
            playerEngine.setPlaybackSpeed(savedSettings.defaultPlaybackSpeed)
            playerEngine.setVideoAspectMode(savedSettings.videoAspectMode)

            var tracks = repository.getAllTracksOnce()
            if (tracks.isEmpty() || savedSettings.autoScanOnStartup) {
                tracks = repository.performScan(savedSettings.allowedFolderUris)
                _events.tryEmit(VeloraMediaEvent.MediaDiscovered(tracks.size))
            }

            if (!initialRestoreCompleted && tracks.isNotEmpty()) {
                initialRestoreCompleted = true
                val trackById = tracks.associateBy { it.id }
                val restoredQueueTracks = savedSettings.lastQueueTrackIds
                    .mapNotNull { trackById[it] }
                    .ifEmpty { tracks }
                val startIdx = savedSettings.lastActiveTrackId?.let { activeId ->
                    restoredQueueTracks.indexOfFirst { it.id == activeId }.takeIf { it >= 0 }
                } ?: 0

                val restoredQueue = PlaybackQueue(
                    mode = savedSettings.queueMode,
                    repeatMode = savedSettings.repeatMode
                ).setQueue(restoredQueueTracks, startIdx)

                queueFlow.value = restoredQueue
            }
        }
    }

    override fun ensureQueueSeeded(tracks: List<Track>) {
        if (queueFlow.value.isEmpty && tracks.isNotEmpty()) {
            queueFlow.value = queueFlow.value.setQueue(tracks, 0)
        }
    }

    override fun postBanner(message: String?) {
        bannerFlow.value = message
        if (message != null) {
            _events.tryEmit(VeloraMediaEvent.StatusNotification(message))
        }
    }

    override fun play(mediaItem: Track, contextTracks: List<Track>, startPositionMs: Long) {
        val pool = contextTracks.ifEmpty {
            queueFlow.value.tracks.ifEmpty { listOf(mediaItem) }
        }
        val startIdx = pool.indexOfFirst { it.id == mediaItem.id }.coerceAtLeast(0)
        val updatedQueue = queueFlow.value.setQueue(pool, startIdx)
        queueFlow.value = updatedQueue
        startTrackOnEngine(mediaItem, updatedQueue, startPositionMs)
    }

    override fun play(mediaItem: MediaItem, startPositionMs: Long) {
        val convertedTrack = convertMedia3ItemToTrack(mediaItem)
        play(convertedTrack, listOf(convertedTrack), startPositionMs)
    }

    override fun resume() {
        playerEngine.play()
    }

    override fun pause() {
        playerEngine.pause()
        val afterState = playerEngine.playbackState.value
        _events.tryEmit(VeloraMediaEvent.PlaybackPaused(afterState.currentTrack, afterState.positionMs))
    }

    override fun playTrackPool(tracks: List<Track>, shuffle: Boolean, bannerMessage: String?) {
        if (tracks.isEmpty()) return
        val desiredMode = if (shuffle) QueueMode.SHUFFLE else QueueMode.NORMAL
        val updatedQueue = queueFlow.value
            .setMode(desiredMode)
            .setQueue(tracks, 0)
        val firstIdx = updatedQueue.activeOrder.firstOrNull() ?: 0
        val firstTrack = updatedQueue.tracks.getOrNull(firstIdx) ?: return
        val syncedQueue = updatedQueue.copy(currentIndex = firstIdx)
        queueFlow.value = syncedQueue
        if (bannerMessage != null) {
            postBanner(bannerMessage)
        }
        startTrackOnEngine(firstTrack, syncedQueue, 0L)
        orchestratorScope.launch {
            settingsRepository.updateQueueModes(syncedQueue.mode, syncedQueue.repeatMode)
        }
    }

    override fun addTrackToQueue(track: Track) {
        val updated = queueFlow.value.appendTrack(track)
        queueFlow.value = updated
        postBanner("Added '${track.title}' to queue")
        persistQueueState(updated, playerEngine.playbackState.value.positionMs)
    }

    override fun playNextInQueue(track: Track) {
        val updated = queueFlow.value.playNext(track)
        queueFlow.value = updated
        postBanner("Playing next: '${track.title}'")
        persistQueueState(updated, playerEngine.playbackState.value.positionMs)
    }

    override fun removeTrackFromQueue(trackId: String) {
        val updated = queueFlow.value.removeTrack(trackId)
        queueFlow.value = updated
        persistQueueState(updated, playerEngine.playbackState.value.positionMs)
    }

    override fun selectQueueTrack(track: Track) {
        val updated = queueFlow.value.selectTrack(track.id)
        queueFlow.value = updated
        startTrackOnEngine(track, updated, 0L)
    }

    override fun togglePlayPause(fallbackTracks: List<Track>) {
        val currentEngineState = playerEngine.playbackState.value
        if (currentEngineState.currentTrack == null) {
            val fallbackTrack = queueFlow.value.currentTrack ?: fallbackTracks.firstOrNull()
            if (fallbackTrack != null) {
                play(fallbackTrack, fallbackTracks)
            }
            return
        }
        playerEngine.togglePlayPause()
        val afterState = playerEngine.playbackState.value
        if (!afterState.isPlaying) {
            _events.tryEmit(VeloraMediaEvent.PlaybackPaused(afterState.currentTrack, afterState.positionMs))
        }
    }

    override fun stop() {
        playerEngine.stop()
    }

    fun stopPlayback() {
        stop()
    }

    override fun seekTo(position: Long) {
        playerEngine.seekTo(position)
        val currentPos = playerEngine.playbackState.value.positionMs
        val currentTrackId = playerEngine.playbackState.value.currentTrack?.id
        if (currentTrackId != null) {
            orchestratorScope.launch {
                repository.updateTrackHistoryPosition(currentTrackId, currentPos, completed = false)
            }
        }
        persistQueueState(queueFlow.value, currentPos)
    }

    override fun skipToNext() {
        val result = queueFlow.value.moveNext()
        queueFlow.value = result.queue
        when {
            result.shouldStopPlayback -> playerEngine.stop()
            result.seekToStartOnly -> {
                playerEngine.seekTo(0L)
                playerEngine.play()
            }
            result.nextTrack != null -> startTrackOnEngine(result.nextTrack, result.queue, 0L)
        }
    }

    override fun skipToPrevious() {
        val pos = playerEngine.playbackState.value.positionMs
        val result = queueFlow.value.movePrevious(pos)
        queueFlow.value = result.queue
        when {
            result.shouldStopPlayback -> playerEngine.stop()
            result.seekToStartOnly -> {
                playerEngine.seekTo(0L)
                playerEngine.play()
            }
            result.nextTrack != null -> startTrackOnEngine(result.nextTrack, result.queue, 0L)
        }
    }

    override fun toggleShuffle() {
        val updated = queueFlow.value.toggleShuffle()
        queueFlow.value = updated
        orchestratorScope.launch {
            settingsRepository.updateQueueModes(updated.mode, updated.repeatMode)
        }
    }

    override fun cycleRepeatMode() {
        val updated = queueFlow.value.cycleRepeatMode()
        queueFlow.value = updated
        orchestratorScope.launch {
            settingsRepository.updateQueueModes(updated.mode, updated.repeatMode)
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        playerEngine.setEqEnabled(enabled)
        orchestratorScope.launch {
            settingsRepository.updateEqEnabled(enabled)
        }
    }

    fun setEqBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0 until EqSpecification.BAND_COUNT) return
        val currentGains = playerEngine.playbackState.value.eqBandGainsDb.toMutableList()
        currentGains[bandIndex] = EqSpecification.clampGainDb(gainDb)
        playerEngine.setEqBandGains(currentGains)
        orchestratorScope.launch {
            settingsRepository.updateEqPresetAndGains("custom_live", currentGains)
        }
    }

    fun applyEqPreset(preset: EqPreset) {
        playerEngine.setEqBandGains(preset.gainsDb)
        orchestratorScope.launch {
            settingsRepository.updateEqPresetAndGains(preset.id, preset.gainsDb)
        }
        postBanner("Applied EQ preset: ${preset.name}")
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
        orchestratorScope.launch {
            settingsRepository.updatePlaybackSpeed(speed)
        }
    }

    fun setVideoAspectMode(mode: VideoAspectMode) {
        playerEngine.setVideoAspectMode(mode)
        orchestratorScope.launch {
            settingsRepository.updateVideoAspectMode(mode)
        }
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        playerEngine.setVisualizerMode(mode)
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        lyricsOffsetMsFlow.value = (lyricsOffsetMsFlow.value + deltaMs).coerceIn(-5_000L, 5_000L)
    }

    fun addTimeMachineBookmark(label: String = "Studio Marker") {
        val activeTrack = playerEngine.playbackState.value.currentTrack ?: queueFlow.value.currentTrack ?: return
        val pos = playerEngine.playbackState.value.positionMs
        val bookmark = TimeMachineBookmark(
            id = "bm_${System.currentTimeMillis()}",
            trackId = activeTrack.id,
            trackTitle = activeTrack.title,
            timestampMs = pos,
            label = label.trim().ifBlank { "Studio Marker" }
        )
        bookmarksFlow.value = listOf(bookmark) + bookmarksFlow.value.take(24)
        postBanner("Bookmarked ${activeTrack.title} at ${bookmark.formattedTimestamp}")
        _events.tryEmit(VeloraMediaEvent.BookmarkAdded(bookmark))
    }

    fun toggleDeclarativePlugin(pluginId: String) {
        var newState = true
        pluginsFlow.value = pluginsFlow.value.map { plugin ->
            if (plugin.id == pluginId) {
                newState = !plugin.isEnabled
                plugin.copy(isEnabled = newState)
            } else {
                plugin
            }
        }
        _events.tryEmit(VeloraMediaEvent.PluginToggled(pluginId, newState))
    }

    private fun startTrackOnEngine(track: Track, queue: PlaybackQueue, startPositionMs: Long) {
        playerEngine.loadAndPlay(track, startPositionMs)
        val descriptor = VeloraCompatibilityMatrix.inspectMediaFormat(track.uri, track.mediaType)
        val lyrics = VeloraLyricsAndSubtitleEngine.resolveAutomaticLyricsForTrack(track)
        _events.tryEmit(VeloraMediaEvent.PlaybackStarted(track, descriptor))
        _events.tryEmit(VeloraMediaEvent.LyricsResolved(track.id, lyrics.size))
        orchestratorScope.launch {
            repository.recordTrackPlayed(track, startPositionMs)
        }
        persistQueueState(queue, startPositionMs)
    }

    private fun handleTrackAutomaticallyEnded() {
        val finishedTrackId = playerEngine.playbackState.value.currentTrack?.id
        if (finishedTrackId != null) {
            _events.tryEmit(VeloraMediaEvent.PlaybackCompleted(finishedTrackId))
            orchestratorScope.launch {
                repository.updateTrackHistoryPosition(finishedTrackId, 0L, completed = true)
            }
        }
        skipToNext()
    }

    private fun persistQueueState(queue: PlaybackQueue, positionMs: Long) {
        orchestratorScope.launch {
            settingsRepository.saveQueueSession(
                queueTrackIds = queue.tracks.map { it.id },
                activeTrackId = queue.currentTrack?.id,
                positionMs = positionMs
            )
        }
    }

    override fun close() {
        orchestratorScope.cancel()
    }
}

private fun buildOrchestratorState(values: Array<Any?>): MediaOrchestratorState {
    val queue = values[0] as PlaybackQueue
    val engineState = values[1] as EnginePlaybackState
    val lyricsOffsetMs = values[2] as Long
    @Suppress("UNCHECKED_CAST")
    val bookmarks = values[3] as List<TimeMachineBookmark>
    @Suppress("UNCHECKED_CAST")
    val plugins = values[4] as List<DeclarativePluginManifest>
    val banner = values[5] as String?

    val activeTrack = engineState.currentTrack ?: queue.currentTrack
    val format = activeTrack?.let {
        VeloraCompatibilityMatrix.inspectMediaFormat(it.uri, it.mediaType)
    }
    val lyrics = activeTrack?.let {
        VeloraLyricsAndSubtitleEngine.resolveAutomaticLyricsForTrack(it)
    }.orEmpty()

    return MediaOrchestratorState(
        queue = queue,
        engineState = engineState,
        activeFormat = format,
        activeLyrics = lyrics,
        lyricsOffsetMs = lyricsOffsetMs,
        timeMachineBookmarks = bookmarks,
        declarativePlugins = plugins,
        lastBannerMessage = banner
    )
}

private fun convertMedia3ItemToTrack(mediaItem: MediaItem): Track {
    val uriStr = mediaItem.localConfiguration?.uri?.toString().orEmpty()
    val descriptor = VeloraCompatibilityMatrix.inspectMediaFormat(uriStr)
    val meta = mediaItem.mediaMetadata
    val rawId = mediaItem.mediaId.ifBlank { "media3_${uriStr.hashCode()}" }
    return Track(
        id = rawId,
        mediaStoreId = (rawId.hashCode().toLong() and 0x7FFFFFFFL),
        title = meta.title?.toString()?.takeIf { it.isNotBlank() } ?: rawId,
        artist = meta.artist?.toString()?.takeIf { it.isNotBlank() } ?: descriptor.containerName,
        album = meta.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: descriptor.primaryCodec,
        durationMs = 180_000L,
        uri = uriStr,
        folderPath = uriStr.substringBeforeLast('/', "/storage/emulated/0/Music"),
        mediaType = descriptor.mediaType,
        addedTimestampMs = System.currentTimeMillis(),
        sampleRateHz = 48_000,
        bitrateKbps = 320
    )
}
