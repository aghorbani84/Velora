package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.playback.Media3PlayerEngine
import com.example.domain.model.RadioStation
import com.example.domain.model.VeloraBackupExporter
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VeloraRadioCatalog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

fun VeloraViewModel.tuneRadioStation(station: RadioStation) {
    val radioPool = uiState.value.radioStations.map { it.toPlayableTrack() }
    val track = station.toPlayableTrack()
    mediaOrchestrator.play(track, radioPool, 0L)
    mediaOrchestrator.postBanner("ON AIR: ${station.name} (${station.frequencyMhz})")
}

fun VeloraViewModel.addRadioStationToQueue(station: RadioStation) {
    mediaOrchestrator.addTrackToQueue(station.toPlayableTrack())
}

fun VeloraViewModel.addCustomRadioStation(name: String, streamUrl: String, genreOrCountry: String) {
    val custom = VeloraRadioCatalog.createCustomStation(name, streamUrl, genreOrCountry)
    if (custom == null) {
        mediaOrchestrator.postBanner("Blocked invalid or unsafe stream URL: $streamUrl")
        return
    }
    customRadioStationsFlow.update { existing ->
        (listOf(custom) + existing).distinctBy { it.streamUrl }
    }
    tuneRadioStation(custom)
}

fun VeloraViewModel.setPreampGainDb(preampDb: Float) {
    val clamped = preampDb.coerceIn(-12f, 12f)
    (playerEngine as? Media3PlayerEngine)?.biquadProcessor?.setPreampGainDb(clamped)
    studioDspStateFlow.update { it.copy(preampGainDb = clamped) }
}

fun VeloraViewModel.setBassBoostPercent(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    (playerEngine as? Media3PlayerEngine)?.biquadProcessor?.setBassBoostPercent(clamped)
    studioDspStateFlow.update { it.copy(bassBoostPercent = clamped) }
}

fun VeloraViewModel.setStereoWidthPercent(percent: Int) {
    val clamped = percent.coerceIn(0, 100)
    (playerEngine as? Media3PlayerEngine)?.biquadProcessor?.setStereoWidthPercent(clamped)
    studioDspStateFlow.update { it.copy(stereoWidthPercent = clamped) }
}

fun VeloraViewModel.setCrossfadeSeconds(seconds: Int) {
    val clamped = seconds.coerceIn(0, 12)
    studioDspStateFlow.update { it.copy(crossfadeSeconds = clamped) }
    val label = if (clamped == 0) "Crossfade disabled (Gapless)" else "Crossfade set to ${clamped}s"
    mediaOrchestrator.postBanner(label)
}

fun VeloraViewModel.setAbLoopPointA() {
    val pos = playerEngine.playbackState.value.positionMs.coerceAtLeast(0L)
    studioDspStateFlow.update { state ->
        val validEnd = state.loopEndMs?.takeIf { it > pos }
        state.copy(loopStartMs = pos, loopEndMs = validEnd)
    }
    mediaOrchestrator.postBanner("Set Loop Point A at ${pos / 1000}s")
}

fun VeloraViewModel.setAbLoopPointB() {
    val pos = playerEngine.playbackState.value.positionMs.coerceAtLeast(0L)
    val start = studioDspStateFlow.value.loopStartMs ?: 0L
    if (pos <= start) {
        mediaOrchestrator.postBanner("Loop Point B must be after Point A")
        return
    }
    studioDspStateFlow.update { it.copy(loopStartMs = start, loopEndMs = pos) }
    mediaOrchestrator.postBanner("Active A-B Loop: ${start / 1000}s → ${pos / 1000}s")
}

fun VeloraViewModel.clearAbLoop() {
    studioDspStateFlow.update { it.copy(loopStartMs = null, loopEndMs = null) }
    mediaOrchestrator.postBanner("Cleared A-B Repeat Loop")
}

fun VeloraViewModel.setSleepTimerMinutes(minutes: Int) {
    sleepTimerJob?.cancel()
    if (minutes <= 0) {
        studioDspStateFlow.update {
            it.copy(sleepTimerRemainingSec = 0, sleepTimerLabel = "Off")
        }
        mediaOrchestrator.postBanner("Sleep Timer disabled")
        return
    }
    val totalSec = minutes * 60
    val label = "${minutes}m"
    studioDspStateFlow.update {
        it.copy(sleepTimerRemainingSec = totalSec, sleepTimerLabel = label)
    }
    mediaOrchestrator.postBanner("Smart Sleep Timer set for $minutes min (with smooth fade-out)")

    val initialVolume = playerEngine.playbackState.value.volume.coerceIn(0.2f, 1.0f)
    sleepTimerJob = viewModelScope.launch {
        var remaining = totalSec
        while (isActive && remaining > 0) {
            delay(1_000L)
            remaining--
            studioDspStateFlow.update { it.copy(sleepTimerRemainingSec = remaining) }
            if (remaining in 1..15) {
                val fadedVolume = (initialVolume * (remaining / 15f)).coerceIn(0.05f, initialVolume)
                playerEngine.setVolume(fadedVolume)
            }
        }
        if (remaining <= 0) {
            mediaOrchestrator.pause()
            playerEngine.setVolume(initialVolume)
            studioDspStateFlow.update {
                it.copy(sleepTimerRemainingSec = 0, sleepTimerLabel = "Off")
            }
            mediaOrchestrator.postBanner("Sleep Timer completed — playback gently paused")
        }
    }
}

fun VeloraViewModel.importCustomLyricsOrSrt(rawContent: String) {
    val parsed = VeloraLyricsAndSubtitleEngine.parseLrcOrPlain(rawContent)
    if (parsed.isEmpty()) {
        mediaOrchestrator.postBanner("No valid LRC/SRT timestamps found in input")
        return
    }
    customLyricsOverrideFlow.value = parsed
    mediaOrchestrator.postBanner("Imported ${parsed.size} synchronized lyric/subtitle cues")
}

fun VeloraViewModel.exportOfflineBackupJson(filesDir: File? = null): String {
    val state = uiState.value
    val customRadios = state.radioStations.filter { it.isCustom }
    val json = VeloraBackupExporter.exportBackupJson(
        playlists = state.playlists,
        eqPresets = state.eqPresets,
        bookmarks = state.timeMachineBookmarks,
        customStations = customRadios,
        dspState = state.studioDspState
    )
    if (filesDir != null) {
        runCatching {
            val backupFile = File(filesDir, "velora_backup_snapshot.json")
            backupFile.writeText(json)
        }
    }
    mediaOrchestrator.postBanner("Exported VELORA JSON Backup (${json.length} bytes)")
    return json
}
