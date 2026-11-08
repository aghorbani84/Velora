package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.firebase.VeloraFirebaseRepository
import com.example.data.playback.Media3PlayerEngine
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.MediaOrchestratorState
import com.example.domain.model.AdaptiveAudioMode
import com.example.domain.model.AppSettings
import com.example.domain.model.AudioRouteInfo
import com.example.domain.model.CopilotCommandIntent
import com.example.domain.model.DeclarativePluginManifest
import com.example.domain.model.EqPreset
import com.example.domain.model.ListeningStatsSummary
import com.example.domain.model.MediaType
import com.example.domain.model.OmniSearchMatch
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.Playlist
import com.example.domain.model.RadioStation
import com.example.domain.model.ScanProgress
import com.example.domain.model.SmartPlaylist
import com.example.domain.model.SmartRuleCriterion
import com.example.domain.model.StudioDspState
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.TimeMachineBookmark
import com.example.domain.model.Track
import com.example.domain.model.TrackFilterMode
import com.example.domain.model.TrackSortOrder
import com.example.domain.model.VeloraMediaCopilot
import com.example.domain.model.VeloraOmniSearchEngine
import com.example.domain.model.VeloraPhase2Engine
import com.example.domain.model.VeloraPluginRegistry
import com.example.domain.model.VeloraRadioCatalog
import com.example.domain.model.VeloraSmartPlaylistEngine
import com.example.domain.model.VeloraSubtitleExporter
import com.example.domain.model.VideoCinemaSettings
import com.example.orchestrator.PlaybackState
import com.example.orchestrator.addTimeMachineBookmark
import com.example.orchestrator.adjustLyricsOffset
import com.example.orchestrator.playNetworkStream
import com.example.orchestrator.playSmartPlaylist
import com.example.orchestrator.toggleDeclarativePlugin
import com.google.firebase.auth.FirebaseUser
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
    val result = VeloraMediaCopilot.interpretCommand(rawCommand)
    val state = uiState.value
    val isPersian = state.settings.appLanguageCode == "fa"
    val baseSummary = if (isPersian) result.summaryFa else result.summaryEn

    when (result.intent) {
        CopilotCommandIntent.PLAY_FAVORITES -> {
            val favs = state.favoriteTracks.ifEmpty { state.allTracks }
            mediaOrchestrator.playTrackPool(favs, shuffle = false, bannerMessage = baseSummary)
        }
        CopilotCommandIntent.RESUME_LAST_SESSION -> {
            val latest = state.playbackHistory.firstOrNull()
            if (latest != null) {
                resumeFromHistory(latest)
                mediaOrchestrator.postBanner(baseSummary)
            } else {
                playAllMusic(shuffle = false)
                mediaOrchestrator.postBanner(baseSummary)
            }
        }
        CopilotCommandIntent.EXPORT_SUBTITLES_SRT -> {
            val srt = VeloraSubtitleExporter.exportToSrt(state.activeLyrics)
            val linesCount = state.activeLyrics.size
            mediaOrchestrator.postBanner("$baseSummary ($linesCount cues • ${srt.length} chars)")
        }
        CopilotCommandIntent.EXPORT_SUBTITLES_VTT -> {
            val vtt = VeloraSubtitleExporter.exportToWebVtt(state.activeLyrics)
            val linesCount = state.activeLyrics.size
            mediaOrchestrator.postBanner("$baseSummary ($linesCount cues • ${vtt.length} chars)")
        }
        CopilotCommandIntent.FIND_HI_RES_TRACKS -> {
            setFilterMode(TrackFilterMode.HI_RES_ONLY)
            mediaOrchestrator.postBanner(baseSummary)
        }
        CopilotCommandIntent.FIND_NIGHT_DRIVE_MUSIC -> {
            playAllMusic(shuffle = false)
            mediaOrchestrator.postBanner(baseSummary)
        }
        CopilotCommandIntent.EXPLAIN_RECOMMENDATIONS -> {
            mediaOrchestrator.postBanner(baseSummary)
        }
    }
}

internal suspend fun syncCloudDataForUser(
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

internal fun assembleVeloraUiState(values: Array<Any?>): VeloraUiState {
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
