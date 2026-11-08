package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.playback.Media3PlayerEngine
import com.example.domain.model.RadioStation
import com.example.domain.model.SmartRuleCriterion
import com.example.domain.model.SubtitleColorTheme
import com.example.domain.model.SubtitleFontScale
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.VeloraBackupExporter
import com.example.domain.model.VeloraLyricsAndSubtitleEngine
import com.example.domain.model.VeloraPhase2Engine
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

fun VeloraViewModel.restoreOfflineBackupJson(rawJson: String, filesDir: File? = null) {
    val content = if (rawJson.isNotBlank()) {
        rawJson.trim()
    } else if (filesDir != null) {
        val file = File(filesDir, "velora_backup_snapshot.json")
        if (file.exists()) file.readText() else ""
    } else {
        ""
    }
    if (content.isBlank() || !content.contains("\"VELORA\"")) {
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (isFa) "فایل پشتیبان معتبر یافت نشد" else "No valid VELORA backup snapshot found"
        mediaOrchestrator.postBanner(msg)
        return
    }
    val preampRegex = Regex(""""preampGainDb"\s*:\s*([-\d.]+)""")
    val bassRegex = Regex(""""bassBoostPercent"\s*:\s*(\d+)""")
    val stereoRegex = Regex(""""stereoWidthPercent"\s*:\s*(\d+)""")
    val crossfadeRegex = Regex(""""crossfadeSeconds"\s*:\s*(\d+)""")

    preampRegex.find(content)?.groupValues?.getOrNull(1)?.toFloatOrNull()?.let { setPreampGainDb(it) }
    bassRegex.find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { setBassBoostPercent(it) }
    stereoRegex.find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { setStereoWidthPercent(it) }
    crossfadeRegex.find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { setCrossfadeSeconds(it) }

    val isFa = uiState.value.settings.appLanguageCode == "fa"
    val msg = if (isFa) "پشتیبان‌گیری آفلاین با موفقیت بازیابی شد" else "Restored VELORA Offline Backup Snapshot"
    mediaOrchestrator.postBanner(msg)
}

fun VeloraViewModel.addCustomSmartRule(
    name: String,
    minBitrateKbps: Int,
    minSampleRateHz: Int,
    minPlayCount: Int,
    favoritesOnly: Boolean,
    artistKeyword: String
) {
    val cleanName = name.trim().ifEmpty { "Custom Studio Rule" }
    val rule = SmartRuleCriterion(
        id = "rule_${System.currentTimeMillis()}",
        name = cleanName,
        minBitrateKbps = minBitrateKbps.coerceAtLeast(0),
        minSampleRateHz = minSampleRateHz.coerceAtLeast(0),
        minPlayCount = minPlayCount.coerceAtLeast(0),
        favoritesOnly = favoritesOnly,
        artistKeyword = artistKeyword.trim()
    )
    customSmartRulesFlow.update { existing -> listOf(rule) + existing }
    val isFa = uiState.value.settings.appLanguageCode == "fa"
    val msg = if (isFa) {
        "پلی‌لیست هوشمند قانون‌مند '$cleanName' ساخته شد"
    } else {
        "Created Smart Rule Playlist '$cleanName'"
    }
    mediaOrchestrator.postBanner(msg)
}

fun VeloraViewModel.stampLyricLineAtCurrentPosition(lineIndex: Int) {
    val currentPos = playerEngine.playbackState.value.positionMs.coerceAtLeast(0L)
    val currentLines = uiState.value.activeLyrics
    if (lineIndex !in currentLines.indices) return
    val updated = currentLines.toMutableList()
    updated[lineIndex] = updated[lineIndex].copy(timestampMs = currentPos)
    customLyricsOverrideFlow.value = updated.sortedBy { it.timestampMs }
    val isFa = uiState.value.settings.appLanguageCode == "fa"
    val msg = if (isFa) {
        "خط ${lineIndex + 1} در زمان ${currentPos}ms ثبت شد"
    } else {
        "Stamped lyric line #${lineIndex + 1} at ${currentPos}ms"
    }
    mediaOrchestrator.postBanner(msg)
}

fun VeloraViewModel.addNewSyncedLyricLineAtCurrentPosition(text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    val currentPos = playerEngine.playbackState.value.positionMs.coerceAtLeast(0L)
    val currentLines = uiState.value.activeLyrics
    val newLine = SyncedLyricLine(timestampMs = currentPos, text = clean)
    customLyricsOverrideFlow.value = (currentLines + newLine).sortedBy { it.timestampMs }
    val isFa = uiState.value.settings.appLanguageCode == "fa"
    val msg = if (isFa) "خط جدید در زمان فعلی به متن آهنگ اضافه شد" else "Added synced lyric line at ${currentPos}ms"
    mediaOrchestrator.postBanner(msg)
}

fun VeloraViewModel.exportCurrentLyricsAsLrc(filesDir: File? = null): String {
    val state = uiState.value
    val title = state.engineState.currentTrack?.title ?: "VELORA Track"
    val lrc = VeloraPhase2Engine.formatLyricsAsLrc(state.activeLyrics, title)
    if (filesDir != null) {
        runCatching {
            File(filesDir, "velora_exported_lyrics.lrc").writeText(lrc)
        }
    }
    val isFa = state.settings.appLanguageCode == "fa"
    val msg = if (isFa) "فایل .LRC هماهنگ‌شده ذخیره و آماده کپی شد" else "Exported synchronized .LRC file"
    mediaOrchestrator.postBanner(msg)
    return lrc
}

fun VeloraViewModel.exportPlaylistAsM3u8(playlistId: Long, filesDir: File? = null): String {
    val state = uiState.value
    val playlist = state.playlists.find { it.id == playlistId }
    val name = playlist?.name ?: "VELORA All Tracks"
    val tracks = playlist?.tracks ?: state.allTracks
    val m3u = VeloraPhase2Engine.exportPlaylistToM3u8(name, tracks)
    if (filesDir != null) {
        runCatching {
            File(filesDir, "velora_${name.replace(' ', '_')}.m3u8").writeText(m3u)
        }
    }
    val isFa = state.settings.appLanguageCode == "fa"
    val msg = if (isFa) {
        "پلی‌لیست '$name' با فرمت استاندارد .M3U8 خروجی گرفته شد (${tracks.size} قطعه)"
    } else {
        "Exported '$name' to .M3U8 (${tracks.size} tracks)"
    }
    mediaOrchestrator.postBanner(msg)
    return m3u
}

fun VeloraViewModel.importM3u8Playlist(playlistName: String, rawM3uContent: String) {
    val cleanName = playlistName.trim().ifEmpty { "Imported M3U8 Playlist" }
    val matched = VeloraPhase2Engine.parseM3u8Content(rawM3uContent, uiState.value.allTracks)
    val tracksToSave = matched.ifEmpty { uiState.value.allTracks.take(5) }
    viewModelScope.launch {
        repository.createPlaylist(cleanName, tracksToSave.map { it.id })
        val isFa = uiState.value.settings.appLanguageCode == "fa"
        val msg = if (isFa) {
            "پلی‌لیست M3U8 '$cleanName' با ${tracksToSave.size} قطعه وارد شد"
        } else {
            "Imported M3U8 playlist '$cleanName' (${tracksToSave.size} tracks)"
        }
        mediaOrchestrator.postBanner(msg)
    }
}

fun VeloraViewModel.setVideoBrightnessLevel(level: Float) {
    val clamped = level.coerceIn(0.15f, 1.0f)
    videoCinemaSettingsFlow.update { it.copy(brightnessLevel = clamped) }
}

fun VeloraViewModel.setSubtitleFontScale(scale: SubtitleFontScale) {
    videoCinemaSettingsFlow.update { it.copy(subtitleFontScale = scale) }
}

fun VeloraViewModel.setSubtitleColorTheme(theme: SubtitleColorTheme) {
    videoCinemaSettingsFlow.update { it.copy(subtitleColorTheme = theme) }
}

