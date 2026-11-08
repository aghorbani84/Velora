package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.audio.VeloraAudioDeviceManager
import com.example.data.firebase.VeloraFirebaseRepository
import com.example.data.playback.Media3PlayerEngine
import com.example.data.repository.VeloraLibraryRepository
import com.example.data.settings.VeloraSettingsRepository
import com.example.domain.engine.MediaOrchestrator
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.AdaptiveAudioMode
import com.example.domain.model.AudioRouteType
import com.example.domain.model.EqPreset
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.Playlist
import com.example.domain.model.RadioStation
import com.example.domain.model.SmartRuleCriterion
import com.example.domain.model.StudioDspState
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.Track
import com.example.domain.model.TrackFilterMode
import com.example.domain.model.TrackSortOrder
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VideoCinemaSettings
import com.example.domain.model.VisualizerMode
import com.example.orchestrator.IMediaOrchestrator
import com.example.orchestrator.PlaybackState
import com.example.orchestrator.applyEqPreset
import com.example.orchestrator.dismissBanner
import com.example.orchestrator.playPlaylist
import com.example.orchestrator.resumeFromHistory
import com.example.orchestrator.setEqBandGain
import com.example.orchestrator.setEqEnabled
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
