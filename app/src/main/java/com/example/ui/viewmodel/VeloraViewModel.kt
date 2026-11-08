package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.firebase.VeloraFirebaseRepository
import com.example.data.playback.Media3PlayerEngine
import com.example.data.repository.VeloraLibraryRepository
import com.example.data.settings.VeloraSettingsRepository
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.MediaOrchestrator
import com.example.domain.engine.MediaOrchestratorState
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.AppSettings
import com.example.domain.model.DeclarativePluginManifest
import com.example.domain.model.EqPreset
import com.example.domain.model.MediaType
import com.example.domain.model.OmniSearchMatch
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.ScanProgress
import com.example.domain.model.RadioStation
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.StudioDspState
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.TimeMachineBookmark
import com.example.domain.model.Track
import com.example.domain.model.TrackFilterMode
import com.example.domain.model.TrackSortOrder
import com.example.domain.model.VeloraOmniSearchEngine
import com.example.domain.model.VeloraPluginRegistry
import com.example.domain.model.VeloraRadioCatalog
import com.example.domain.model.VeloraSmartPlaylistEngine
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VisualizerMode
import com.example.orchestrator.IMediaOrchestrator
import com.example.orchestrator.PlaybackState
import com.example.orchestrator.addTimeMachineBookmark
import com.example.orchestrator.adjustLyricsOffset
import com.example.orchestrator.applyEqPreset
import com.example.orchestrator.dismissBanner
import com.example.orchestrator.playNetworkStream
import com.example.orchestrator.playPlaylist
import com.example.orchestrator.playSmartPlaylist
import com.example.orchestrator.resumeFromHistory
import com.example.orchestrator.setEqBandGain
import com.example.orchestrator.setEqEnabled
import com.example.orchestrator.toggleDeclarativePlugin
import com.example.data.audio.VeloraAudioDeviceManager
import com.example.domain.model.AdaptiveAudioMode
import com.example.domain.model.AudioRouteInfo
import com.example.domain.model.AudioRouteType
import com.example.domain.model.ListeningStatsSummary
import com.example.domain.model.SmartRuleCriterion
import com.example.domain.model.VeloraPhase2Engine
import com.example.domain.model.VideoCinemaSettings
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

typealias FazoraUiState = VeloraUiState

data class VeloraUiState(
    val allTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
    val videoTracks: List<Track> = emptyList(),
    val favoriteTracks: List<Track> = emptyList(),
    val smartPlaylists: List<SmartPlaylist> = emptyList(),
    val customSmartRules: List<SmartRuleCriterion> = emptyList(),
    val listeningStats: ListeningStatsSummary = ListeningStatsSummary(),
    val videoCinemaSettings: VideoCinemaSettings = VideoCinemaSettings(),
    val omniSearchMatches: List<OmniSearchMatch> = emptyList(),
    val activeLyrics: List<SyncedLyricLine> = emptyList(),
    val lyricsOffsetMs: Long = 0L,
    val timeMachineBookmarks: List<TimeMachineBookmark> = emptyList(),
    val declarativePlugins: List<DeclarativePluginManifest> = VeloraPluginRegistry.DEFAULT_DECLARATIVE_PLUGINS,
    val radioStations: List<RadioStation> = VeloraRadioCatalog.BUILT_IN_STATIONS,
    val studioDspState: StudioDspState = StudioDspState(),
    val audioRoute: AudioRouteInfo = AudioRouteInfo(),
    val sortOrder: TrackSortOrder = TrackSortOrder.TITLE_ASC,
    val filterMode: TrackFilterMode = TrackFilterMode.ALL,
    val artistsMap: Map<String, List<Track>> = emptyMap(),
    val selectedArtist: String? = null,
    val playlists: List<Playlist> = emptyList(),
    val playbackHistory: List<PlaybackHistoryEntry> = emptyList(),
    val queue: PlaybackQueue = PlaybackQueue(),
    val engineState: EnginePlaybackState = EnginePlaybackState(),
    val playbackState: PlaybackState = PlaybackState(),
    val eqPresets: List<EqPreset> = EqPreset.BUILT_IN_PRESETS,
    val activeEqPresetId: String = EqPreset.FLAT.id,
    val searchQuery: String = "",
    val searchResults: List<Track> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val scanProgress: ScanProgress = ScanProgress(),
    val statusBannerMessage: String? = null,
    val firebaseUser: FirebaseUser? = null,
    val isSigningIn: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class VeloraViewModel(
    val repository: VeloraLibraryRepository,
    val settingsRepository: VeloraSettingsRepository,
    val playerEngine: PlayerEngine,
    val firebaseRepository: VeloraFirebaseRepository,
    val audioDeviceManager: VeloraAudioDeviceManager = VeloraAudioDeviceManager(repository.context),
    val mediaOrchestrator: IMediaOrchestrator = MediaOrchestrator(repository, settingsRepository, playerEngine)
) : ViewModel() {

    val exoPlayer: ExoPlayer?
        get() = (playerEngine as? Media3PlayerEngine)?.exoPlayer

    private val _searchQuery = MutableStateFlow("")
    private val _selectedArtist = MutableStateFlow<String?>(null)
    private val _isSigningIn = MutableStateFlow(false)
    private val _sortOrder = MutableStateFlow(TrackSortOrder.TITLE_ASC)
    private val _filterMode = MutableStateFlow(TrackFilterMode.ALL)

    internal val customRadioStationsFlow = MutableStateFlow<List<RadioStation>>(emptyList())
    internal val studioDspStateFlow = MutableStateFlow(StudioDspState())
    internal val customLyricsOverrideFlow = MutableStateFlow<List<SyncedLyricLine>?>(null)
    internal val customSmartRulesFlow = MutableStateFlow<List<SmartRuleCriterion>>(emptyList())
    internal val videoCinemaSettingsFlow = MutableStateFlow(VideoCinemaSettings())
    internal var sleepTimerJob: Job? = null

    val playbackState: StateFlow<PlaybackState> = mediaOrchestrator.playbackState

    private val searchResultsFlow = _searchQuery.flatMapLatest { query ->
        repository.searchTracks(query)
    }

    val uiState: StateFlow<VeloraUiState> = combine(
        repository.allTracks,
        repository.playlists,
        repository.allEqPresets,
        settingsRepository.settingsFlow,
        repository.scanProgress,
        mediaOrchestrator.orchestratorState,
        _searchQuery,
        searchResultsFlow,
        _selectedArtist,
        firebaseRepository.authStateFlow(),
        _isSigningIn,
        repository.playbackHistory,
        _sortOrder,
        _filterMode,
        customRadioStationsFlow,
        studioDspStateFlow,
        customLyricsOverrideFlow,
        audioDeviceManager.activeRoute,
        customSmartRulesFlow,
        videoCinemaSettingsFlow,
        transform = ::assembleVeloraUiState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VeloraUiState()
    )

    init {
        viewModelScope.launch {
            mediaOrchestrator.bootstrapAndRestoreSession()
        }
        viewModelScope.launch {
            playerEngine.playbackState.collect { engine ->
                val dsp = studioDspStateFlow.value
                val endMs = dsp.loopEndMs ?: Long.MAX_VALUE
                if (dsp.isAbLoopActive && engine.positionMs >= endMs) {
                    mediaOrchestrator.seekTo(dsp.loopStartMs ?: 0L)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { s ->
                (playerEngine as? Media3PlayerEngine)?.biquadProcessor?.setLowBatteryMode(s.lowBatteryModeEnabled)
            }
        }
        viewModelScope.launch {
            audioDeviceManager.activeRoute.collect { route ->
                val s = settingsRepository.getSettingsOnce()
                if (s.adaptiveAudioMode == AdaptiveAudioMode.ADAPTIVE_AI) {
                    when (route.type) {
                        AudioRouteType.BLUETOOTH -> mediaOrchestrator.applyEqPreset(EqPreset.BASS_BOOST)
                        AudioRouteType.WIRED_HEADPHONES, AudioRouteType.USB_DAC -> {
                            mediaOrchestrator.applyEqPreset(EqPreset.FLAT)
                        }
                        AudioRouteType.SPEAKER -> mediaOrchestrator.applyEqPreset(EqPreset.VOCAL)
                        AudioRouteType.UNKNOWN -> {}
                    }
                }
            }
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsOnce()
            val scanned = repository.performScan(settings.allowedFolderUris)
            mediaOrchestrator.ensureQueueSeeded(scanned)
            mediaOrchestrator.postBanner("Indexed ${scanned.size} media tracks")
        }
    }

    fun autoScanDeviceMedia() {
        scanLibrary()
    }

    fun setSimpleMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSimpleMode(enabled)
            val msg = if (enabled) {
                if (uiState.value.settings.appLanguageCode == "fa") "به حالت ساده و خلوت تغییر یافت" else "Switched to Simple Mode"
            } else {
                if (uiState.value.settings.appLanguageCode == "fa") "به حالت استودیوی پیشرفته تغییر یافت" else "Switched to Studio Advanced Mode"
            }
            mediaOrchestrator.postBanner(msg)
        }
    }

    fun addFolderUri(uriOrPath: String) {
        viewModelScope.launch {
            val accepted = settingsRepository.addAllowedFolder(uriOrPath)
            if (!accepted) {
                mediaOrchestrator.postBanner("Blocked unsafe system path: $uriOrPath")
                return@launch
            }
            val updatedSettings = settingsRepository.getSettingsOnce()
            val scanned = repository.performScan(updatedSettings.allowedFolderUris)
            mediaOrchestrator.postBanner("Added folder & indexed ${scanned.size} tracks")
        }
    }

    fun createAndScanCustomFolder(folderName: String) {
        viewModelScope.launch {
            val path = repository.createAndScanCustomAcousticFolder(folderName)
            if (path == null) {
                mediaOrchestrator.postBanner("Rejected invalid folder name")
                return@launch
            }
            settingsRepository.addAllowedFolder(path)
            val updatedSettings = settingsRepository.getSettingsOnce()
            val scanned = repository.performScan(updatedSettings.allowedFolderUris)
            mediaOrchestrator.postBanner("Created folder '$folderName' (${scanned.size} total tracks)")
        }
    }

    fun removeFolder(uriOrPath: String) {
        viewModelScope.launch {
            settingsRepository.removeAllowedFolder(uriOrPath)
            repository.removeFolderTracks(uriOrPath)
            mediaOrchestrator.postBanner("Removed folder from library")
        }
    }

    fun playTrack(track: Track, contextTracks: List<Track> = emptyList(), startPositionMs: Long = 0L) {
        val pool = contextTracks.ifEmpty {
            uiState.value.allTracks.ifEmpty { listOf(track) }
        }
        mediaOrchestrator.play(track, pool, startPositionMs)
    }

    fun resumeFromHistory(entry: PlaybackHistoryEntry) {
        mediaOrchestrator.resumeFromHistory(entry, uiState.value.allTracks)
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            repository.clearPlaybackHistory()
            mediaOrchestrator.postBanner("Cleared offline playback history")
        }
    }

    fun playAllMusic(shuffle: Boolean = false) {
        val musicTracks = uiState.value.audioTracks.ifEmpty { uiState.value.allTracks }
        mediaOrchestrator.playTrackPool(musicTracks, shuffle)
    }

    fun playArtistTracks(artist: String, shuffle: Boolean = false) {
        val artistTracks = uiState.value.artistsMap[artist].orEmpty()
        mediaOrchestrator.playTrackPool(artistTracks, shuffle)
    }

    fun addTrackToQueue(track: Track) {
        mediaOrchestrator.addTrackToQueue(track)
    }

    fun removeTrackFromQueue(trackId: String) {
        mediaOrchestrator.removeTrackFromQueue(trackId)
    }

    fun selectQueueTrack(track: Track) {
        mediaOrchestrator.selectQueueTrack(track)
    }

    fun togglePlayPause() {
        mediaOrchestrator.togglePlayPause(uiState.value.allTracks)
    }

    fun stopPlayback() {
        mediaOrchestrator.stop()
    }

    fun seekTo(positionMs: Long) {
        mediaOrchestrator.seekTo(positionMs)
    }

    fun skipToNext() {
        mediaOrchestrator.skipToNext()
    }

    fun skipToPrevious() {
        mediaOrchestrator.skipToPrevious()
    }

    fun toggleShuffle() {
        mediaOrchestrator.toggleShuffle()
    }

    fun cycleRepeatMode() {
        mediaOrchestrator.cycleRepeatMode()
    }

    fun setVolume(volume: Float) {
        playerEngine.setVolume(volume)
    }

    fun toggleMute() {
        playerEngine.toggleMute()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
        viewModelScope.launch {
            settingsRepository.updatePlaybackSpeed(speed)
        }
    }

    fun setVideoAspectMode(mode: VideoAspectMode) {
        playerEngine.setVideoAspectMode(mode)
        viewModelScope.launch {
            settingsRepository.updateVideoAspectMode(mode)
        }
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        playerEngine.setSubtitlesEnabled(enabled)
    }

    fun selectSubtitleTrack(trackId: String?) {
        playerEngine.selectSubtitleTrack(trackId)
    }

    fun selectAudioTrack(trackId: String) {
        playerEngine.selectAudioTrack(trackId)
    }

    fun setEqEnabled(enabled: Boolean) {
        mediaOrchestrator.setEqEnabled(enabled)
    }

    fun setEqBandGain(bandIndex: Int, gainDb: Float) {
        mediaOrchestrator.setEqBandGain(bandIndex, gainDb)
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        playerEngine.setVisualizerMode(mode)
    }

    fun applyEqPreset(preset: EqPreset) {
        mediaOrchestrator.applyEqPreset(preset)
    }

    fun saveCustomEqPreset(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            mediaOrchestrator.postBanner("Enter a preset name before saving")
            return
        }
        viewModelScope.launch {
            val currentGains = playerEngine.playbackState.value.eqBandGainsDb
            val savedPreset = repository.saveCustomEqPreset(trimmed, currentGains)
            settingsRepository.updateEqPresetAndGains(savedPreset.id, savedPreset.gainsDb)
            mediaOrchestrator.postBanner("Saved 10-band EQ preset '${savedPreset.name}'")
        }
    }

    fun deleteCustomEqPreset(presetId: String) {
        viewModelScope.launch {
            repository.deleteCustomEqPreset(presetId)
            mediaOrchestrator.postBanner("Deleted custom EQ preset")
        }
    }

    fun createPlaylist(name: String, initialTrackId: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val initialIds = listOfNotNull(initialTrackId)
            repository.createPlaylist(trimmed, initialIds)
            mediaOrchestrator.postBanner("Created playlist '$trimmed'")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            mediaOrchestrator.postBanner("Deleted playlist")
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
            mediaOrchestrator.postBanner("Added track to playlist")
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun playPlaylist(playlist: Playlist, shuffle: Boolean = false) {
        mediaOrchestrator.playPlaylist(playlist, shuffle)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectArtist(artist: String?) {
        _selectedArtist.value = artist
    }

    fun updateAutoHideControlsMs(timeoutMs: Long) {
        viewModelScope.launch {
            settingsRepository.updateAutoHideControlsMs(timeoutMs)
        }
    }

    fun setAppLanguageCode(languageCode: String) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguageCode(languageCode)
            val msg = if (languageCode == "fa") {
                "زبان برنامه به فارسی (RTL) تغییر یافت"
            } else {
                "Interface language set to English (LTR)"
            }
            mediaOrchestrator.postBanner(msg)
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(track.id)
            val msg = if (isNowFav) {
                "Added '${track.title}' to Favorites"
            } else {
                "Removed '${track.title}' from Favorites"
            }
            mediaOrchestrator.postBanner(msg)
        }
    }

    fun setSortOrder(order: TrackSortOrder) {
        _sortOrder.value = order
    }

    fun setFilterMode(mode: TrackFilterMode) {
        _filterMode.value = mode
    }

    fun signInWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            _isSigningIn.value = true
            val result = firebaseRepository.signInWithGoogle(context)
            _isSigningIn.value = false
            result.onSuccess { user ->
                mediaOrchestrator.postBanner("Signed in as ${user.displayName ?: user.email ?: "Audiophile"}")
                syncCloudDataForUser(firebaseRepository, uiState.value, user.uid)
            }.onFailure { err ->
                mediaOrchestrator.postBanner("Sign-in cancelled or failed: ${err.localizedMessage ?: "Unknown"}")
            }
        }
    }

    fun signOut() {
        firebaseRepository.signOut()
        mediaOrchestrator.postBanner("Signed out of Firebase")
    }

    fun dismissBanner() {
        mediaOrchestrator.dismissBanner()
    }

    fun clearEngineError() {
        playerEngine.clearError()
    }
}

fun VeloraViewModel.playSmartPlaylist(smartPlaylist: SmartPlaylist, shuffle: Boolean = false) {
    mediaOrchestrator.playSmartPlaylist(smartPlaylist, shuffle)
}

fun VeloraViewModel.playNetworkStream(streamUrl: String, customTitle: String = "VELORA Live Stream") {
    mediaOrchestrator.playNetworkStream(streamUrl, customTitle)
}

fun VeloraViewModel.adjustLyricsOffset(deltaMs: Long) {
    mediaOrchestrator.adjustLyricsOffset(deltaMs)
}

fun VeloraViewModel.addTimeMachineBookmark(label: String = "Studio Marker") {
    mediaOrchestrator.addTimeMachineBookmark(label)
}

fun VeloraViewModel.setAutoScanOnStartup(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.updateAutoScanOnStartup(enabled)
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (enabled) {
            if (isFa) "اسکن خودکار هنگام ورود فعال شد" else "Auto-scan on launch enabled"
        } else {
            if (isFa) "اسکن خودکار هنگام ورود غیرفعال شد" else "Auto-scan on launch disabled"
        }
        mediaOrchestrator.postBanner(msg)
    }
}

fun VeloraViewModel.setOnlyValidMusicInSearch(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.updateOnlyValidMusic(enabled)
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (enabled) {
            if (isFa) "فیلتر فعال: فقط آهنگ‌های دارای مشخصات معتبر" else "Filter: Valid Music Only"
        } else {
            if (isFa) "فیلتر غیرفعال: تمام فایل‌های صوتی و ضبط‌ها" else "Filter: All Audio & Recordings"
        }
        mediaOrchestrator.postBanner(msg)
    }
}

fun VeloraViewModel.setBackgroundVideoAudio(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.updateBackgroundVideoAudio(enabled)
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (enabled) {
            if (isFa) "پخش صدای ویدیو در پس‌زمینه فعال شد" else "Background video audio enabled"
        } else {
            if (isFa) "پخش صدای ویدیو در پس‌زمینه غیرفعال شد" else "Background video audio disabled"
        }
        mediaOrchestrator.postBanner(msg)
    }
}

fun VeloraViewModel.setLowBatteryMode(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.updateLowBatteryMode(enabled)
        (playerEngine as? Media3PlayerEngine)?.biquadProcessor?.setLowBatteryMode(enabled)
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (enabled) {
            if (isFa) "حالت بهینه‌سازی باتری فعال شد (کاهش بار پردازشی)" else "Battery Saver Mode enabled"
        } else {
            if (isFa) "حالت بهینه‌سازی باتری خاموش شد" else "Battery Saver Mode disabled"
        }
        mediaOrchestrator.postBanner(msg)
    }
}

fun VeloraViewModel.setAdaptiveAudioMode(mode: AdaptiveAudioMode) {
    viewModelScope.launch {
        settingsRepository.updateAdaptiveAudioMode(mode)
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (isFa) {
            "حالت صوتی به ${mode.labelFa} تغییر یافت"
        } else {
            "Audio mode switched to ${mode.labelEn}"
        }
        mediaOrchestrator.postBanner(msg)
    }
}

fun VeloraViewModel.updateTrackMetadata(trackId: String, title: String, artist: String, album: String) {
    viewModelScope.launch {
        repository.updateTrackMetadata(trackId, title, artist, album)
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (isFa) "تگ‌های متادیتا ذخیره شدند" else "Track metadata tags saved"
        mediaOrchestrator.postBanner(msg)
    }
}

fun VeloraViewModel.playNextInQueue(track: Track) {
    mediaOrchestrator.playNextInQueue(track)
}

fun VeloraViewModel.toggleDeclarativePlugin(pluginId: String) {
    mediaOrchestrator.toggleDeclarativePlugin(pluginId)
}

fun VeloraViewModel.executeCopilotCommand(rawCommand: String) {
    val result = com.example.domain.model.VeloraMediaCopilot.interpretCommand(rawCommand)
    val state = uiState.value
    val isPersian = state.settings.appLanguageCode == "fa"
    val baseSummary = if (isPersian) result.summaryFa else result.summaryEn

    when (result.intent) {
        com.example.domain.model.CopilotCommandIntent.PLAY_FAVORITES -> {
            val favs = state.favoriteTracks.ifEmpty { state.allTracks }
            mediaOrchestrator.playTrackPool(favs, shuffle = false, bannerMessage = baseSummary)
        }
        com.example.domain.model.CopilotCommandIntent.RESUME_LAST_SESSION -> {
            val latest = state.playbackHistory.firstOrNull()
            if (latest != null) {
                resumeFromHistory(latest)
                mediaOrchestrator.postBanner(baseSummary)
            } else {
                playAllMusic(shuffle = false)
                mediaOrchestrator.postBanner(baseSummary)
            }
        }
        com.example.domain.model.CopilotCommandIntent.EXPORT_SUBTITLES_SRT -> {
            val srt = com.example.domain.model.VeloraSubtitleExporter.exportToSrt(state.activeLyrics)
            val linesCount = state.activeLyrics.size
            mediaOrchestrator.postBanner("$baseSummary ($linesCount cues • ${srt.length} chars)")
        }
        com.example.domain.model.CopilotCommandIntent.EXPORT_SUBTITLES_VTT -> {
            val vtt = com.example.domain.model.VeloraSubtitleExporter.exportToWebVtt(state.activeLyrics)
            val linesCount = state.activeLyrics.size
            mediaOrchestrator.postBanner("$baseSummary ($linesCount cues • ${vtt.length} chars)")
        }
        com.example.domain.model.CopilotCommandIntent.FIND_HI_RES_TRACKS -> {
            setFilterMode(TrackFilterMode.HI_RES_ONLY)
            mediaOrchestrator.postBanner(baseSummary)
        }
        com.example.domain.model.CopilotCommandIntent.FIND_NIGHT_DRIVE_MUSIC -> {
            playAllMusic(shuffle = false)
            mediaOrchestrator.postBanner(baseSummary)
        }
        com.example.domain.model.CopilotCommandIntent.EXPLAIN_RECOMMENDATIONS -> {
            mediaOrchestrator.postBanner(baseSummary)
        }
    }
}

private suspend fun syncCloudDataForUser(
    firebaseRepository: VeloraFirebaseRepository,
    snapshot: VeloraUiState,
    userId: String
) {
    snapshot.playlists.forEach { playlist ->
        runCatching {
            firebaseRepository.saveCloudPlaylist(
                userId = userId,
                playlistId = playlist.id.toString(),
                name = playlist.name,
                trackIds = playlist.tracks.map { it.id }
            )
        }
    }
    snapshot.eqPresets.filter { !it.isBuiltIn }.forEach { preset ->
        runCatching {
            firebaseRepository.saveCloudEqPreset(
                userId = userId,
                presetId = preset.id,
                name = preset.name,
                gainsDb = preset.gainsDb.map { it.toDouble() }
            )
        }
    }
}

private fun assembleVeloraUiState(values: Array<Any?>): VeloraUiState {
    @Suppress("UNCHECKED_CAST")
    val rawTracks = values[0] as List<Track>
    @Suppress("UNCHECKED_CAST")
    val playlists = values[1] as List<Playlist>
    @Suppress("UNCHECKED_CAST")
    val eqPresets = values[2] as List<EqPreset>
    val settings = values[3] as AppSettings
    val scanProgress = values[4] as ScanProgress
    val orchestrator = values[5] as MediaOrchestratorState
    val searchQuery = values[6] as String
    @Suppress("UNCHECKED_CAST")
    val dbSearchResults = values[7] as List<Track>
    val selectedArtist = values[8] as String?
    val firebaseUser = values[9] as FirebaseUser?
    val isSigningIn = values[10] as Boolean
    @Suppress("UNCHECKED_CAST")
    val playbackHistory = values[11] as List<PlaybackHistoryEntry>
    val sortOrder = values[12] as TrackSortOrder
    val filterMode = values[13] as TrackFilterMode
    @Suppress("UNCHECKED_CAST")
    val customRadios = (values.getOrNull(14) as? List<RadioStation>).orEmpty()
    val studioDsp = (values.getOrNull(15) as? StudioDspState) ?: StudioDspState()
    @Suppress("UNCHECKED_CAST")
    val customLyrics = values.getOrNull(16) as? List<SyncedLyricLine>
    val audioRoute = (values.getOrNull(17) as? AudioRouteInfo) ?: AudioRouteInfo()
    @Suppress("UNCHECKED_CAST")
    val customSmartRules = (values.getOrNull(18) as? List<SmartRuleCriterion>).orEmpty()
    val videoCinemaSettings = (values.getOrNull(19) as? VideoCinemaSettings) ?: VideoCinemaSettings()

    val filteredTracks = when (filterMode) {
        TrackFilterMode.ALL -> rawTracks
        TrackFilterMode.FAVORITES_ONLY -> rawTracks.filter { it.isFavorite }
        TrackFilterMode.HI_RES_ONLY -> rawTracks.filter { it.sampleRateHz >= 48_000 }
    }
    val sortedTracks = when (sortOrder) {
        TrackSortOrder.TITLE_ASC -> filteredTracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        TrackSortOrder.ARTIST_ASC -> filteredTracks.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist })
        TrackSortOrder.DURATION_DESC -> filteredTracks.sortedByDescending { it.durationMs }
        TrackSortOrder.RECENTLY_ADDED -> filteredTracks.sortedByDescending { it.addedTimestampMs }
    }

    val audioTracks = if (settings.onlyValidMusicInSearch) {
        sortedTracks.filter { it.mediaType == MediaType.AUDIO && it.isLikelyValidSong() }
    } else {
        sortedTracks.filter { it.mediaType == MediaType.AUDIO }
    }
    val videoTracks = sortedTracks.filter { it.mediaType == MediaType.VIDEO }
    val favoriteTracks = rawTracks.filter { it.isFavorite }
    val artistsMap = rawTracks
        .filter { it.mediaType == MediaType.AUDIO && (!settings.onlyValidMusicInSearch || it.isLikelyValidSong()) }
        .groupBy { it.artist }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    val rulePlaylists = customSmartRules.map { rule ->
        VeloraPhase2Engine.evaluateSmartRule(rule, rawTracks, playbackHistory)
    }
    val smartPlaylists = rulePlaylists + VeloraSmartPlaylistEngine.generateSmartPlaylists(rawTracks, playbackHistory)
    val listeningStats = VeloraPhase2Engine.computeListeningStats(rawTracks, playbackHistory)
    val omniMatches = VeloraOmniSearchEngine.searchWithSemanticAndFuzzyRanking(
        query = searchQuery,
        allTracks = rawTracks,
        history = playbackHistory
    )
    val rawSearchResults = if (searchQuery.isBlank()) {
        dbSearchResults
    } else {
        (omniMatches.map { it.track } + dbSearchResults).distinctBy { it.id }
    }
    val combinedSearchResults = if (settings.onlyValidMusicInSearch) {
        rawSearchResults.filter { it.isLikelyValidSong() }
    } else {
        rawSearchResults
    }

    return VeloraUiState(
        allTracks = sortedTracks,
        audioTracks = audioTracks,
        videoTracks = videoTracks,
        favoriteTracks = favoriteTracks,
        smartPlaylists = smartPlaylists,
        customSmartRules = customSmartRules,
        listeningStats = listeningStats,
        videoCinemaSettings = videoCinemaSettings,
        omniSearchMatches = omniMatches,
        activeLyrics = customLyrics ?: orchestrator.activeLyrics,
        lyricsOffsetMs = orchestrator.lyricsOffsetMs,
        timeMachineBookmarks = orchestrator.timeMachineBookmarks,
        declarativePlugins = orchestrator.declarativePlugins,
        radioStations = customRadios + VeloraRadioCatalog.BUILT_IN_STATIONS,
        studioDspState = studioDsp,
        audioRoute = audioRoute,
        sortOrder = sortOrder,
        filterMode = filterMode,
        artistsMap = artistsMap,
        selectedArtist = selectedArtist ?: artistsMap.keys.firstOrNull(),
        playlists = playlists,
        playbackHistory = playbackHistory,
        queue = orchestrator.queue,
        engineState = orchestrator.engineState,
        playbackState = PlaybackState.fromOrchestratorState(orchestrator),
        eqPresets = eqPresets,
        activeEqPresetId = settings.activeEqPresetId,
        searchQuery = searchQuery,
        searchResults = combinedSearchResults,
        settings = settings,
        scanProgress = scanProgress,
        statusBannerMessage = orchestrator.lastBannerMessage,
        firebaseUser = firebaseUser,
        isSigningIn = isSigningIn
    )
}
