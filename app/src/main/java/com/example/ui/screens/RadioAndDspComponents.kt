package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.PlaybackQueue
import com.example.domain.model.RadioCategory
import com.example.domain.model.RadioStation
import com.example.domain.model.StudioDspState
import com.example.domain.model.VeloraRadioCatalog
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraEmeraldOk
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldBright
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraChampagneGold
import com.example.ui.viewmodel.VeloraUiState
import java.util.Locale

@Composable
fun RadioScreen(
    uiState: VeloraUiState,
    onTuneStation: (RadioStation) -> Unit,
    onAddStationToQueue: (RadioStation) -> Unit,
    onAddCustomStation: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    var selectedCategory by remember { mutableStateOf(RadioCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddStationDialog by remember { mutableStateOf(false) }

    val filteredStations = remember(uiState.radioStations, selectedCategory, searchQuery) {
        VeloraRadioCatalog.filterStations(uiState.radioStations, selectedCategory, searchQuery)
    }
    val activeUri = uiState.engineState.currentTrack?.uri

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FazoraObsidian)
            .testTag("radio_screen_list"),
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13)
    ) {
        item {
            RadioHeroBanner(
                isPersian = isPersian,
                activeStation = uiState.radioStations.find { it.streamUrl == activeUri },
                totalStations = uiState.radioStations.size,
                onOpenAddCustomDialog = { showAddStationDialog = true }
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = FazoraGold)
                },
                placeholder = {
                    Text(
                        if (isPersian) "جستجوی ایستگاه رادیویی، سبک یا فرکانس..." else "Search radio stations, genre, or frequency...",
                        color = FazoraSteel
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radio_search_input")
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(RadioCategory.entries, key = { it.id }) { cat ->
                    val selected = selectedCategory == cat
                    val label = if (isPersian) cat.labelFa else cat.labelEn
                    Surface(
                        color = if (selected) FazoraGold else FazoraCarbonElevated,
                        shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                        modifier = Modifier
                            .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                            .clickable { selectedCategory = cat }
                            .border(
                                1.dp,
                                if (selected) FazoraGoldBright else FazoraCarbonBorder,
                                RoundedCornerShape(PhiTokens.CornerRadius8)
                            )
                            .testTag("radio_cat_${cat.id}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) FazoraObsidian else FazoraIvory,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        items(filteredStations, key = { it.id }) { station ->
            val isPlayingThis = activeUri == station.streamUrl && uiState.engineState.isPlaying
            RadioStationCard(
                station = station,
                isPersian = isPersian,
                isActive = activeUri == station.streamUrl,
                isPlaying = isPlayingThis,
                onPlay = { onTuneStation(station) },
                onAddToQueue = { onAddStationToQueue(station) }
            )
        }
    }

    if (showAddStationDialog) {
        AddCustomRadioDialog(
            isPersian = isPersian,
            onDismiss = { showAddStationDialog = false },
            onConfirm = { name, url, genre ->
                onAddCustomStation(name, url, genre)
                showAddStationDialog = false
            }
        )
    }
}

@Composable
private fun RadioHeroBanner(
    isPersian: Boolean,
    activeStation: RadioStation?,
    totalStations: Int,
    onOpenAddCustomDialog: () -> Unit
) {
    Surface(
        color = FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius13))
    ) {
        Column(modifier = Modifier.padding(PhiTokens.Space21)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (activeStation != null) FazoraEmeraldOk else FazoraGold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activeStation != null) {
                            "ON AIR • ${activeStation.frequencyMhz}"
                        } else {
                            "VELORA GLOBAL TUNER • $totalStations STATIONS"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = VeloraChampagneGold
                    )
                }
                OutlinedButton(
                    onClick = onOpenAddCustomDialog,
                    modifier = Modifier.testTag("add_custom_radio_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = FazoraGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPersian) "افزودن رادیو" else "Add Station", color = FazoraGold)
                }
            }
            Spacer(modifier = Modifier.height(PhiTokens.Space8))
            Text(
                text = if (isPersian) "رادیو اینترنتی زنده و استودیویی" else "Live World & Hi-Fi Radio Tuner",
                style = MaterialTheme.typography.headlineSmall,
                color = FazoraIvory
            )
            Text(
                text = if (activeStation != null) {
                    "Playing: ${activeStation.name} (${activeStation.codecLabel} • ${activeStation.bitrateKbps} kbps)"
                } else if (isPersian) {
                    "پخش زنده ایستگاه‌های فارسی، موسیقی ملل، Hi-Fi، چیل‌اوت و سینث‌ویو با اکولایزر ۱۰ باندی"
                } else {
                    "Direct low-latency streaming with full 10-band Biquad EQ, Bass Boost & real-time spectrum analysis."
                },
                style = MaterialTheme.typography.bodySmall,
                color = FazoraSteel
            )
        }
    }
}

@Composable
private fun RadioStationCard(
    station: RadioStation,
    isPersian: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Surface(
        color = if (isActive) FazoraGoldMuted else FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PhiTokens.CornerRadius13))
            .clickable { onPlay() }
            .border(
                1.dp,
                if (isActive) FazoraGold else FazoraCarbonBorder,
                RoundedCornerShape(PhiTokens.CornerRadius13)
            )
            .testTag("radio_station_${station.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(PhiTokens.Space13)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (isActive) FazoraGold else FazoraCarbonElevated,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.Radio,
                            contentDescription = null,
                            tint = if (isActive) FazoraObsidian else FazoraGold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(PhiTokens.Space13))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPersian) station.nameFa else station.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = FazoraIvory,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = station.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = FazoraSteel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${station.frequencyMhz} • ${station.country} • ${station.codecLabel} ${station.bitrateKbps}kbps",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeloraChampagneGold
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onAddToQueue,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Add radio station to queue",
                        tint = FazoraSteel
                    )
                }
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FazoraGold,
                        contentColor = FazoraObsidian
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isActive) "LIVE" else "Tune")
                }
            }
        }
    }
}

@Composable
private fun AddCustomRadioDialog(
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FazoraCarbon,
        titleContentColor = FazoraIvory,
        textContentColor = FazoraIvory,
        title = {
            Text(if (isPersian) "افزودن ایستگاه رادیویی جدید" else "Add Custom Radio Stream")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isPersian) "نام ایستگاه رادیویی" else "Station Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL (https://... mp3 / aac / m3u8)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(if (isPersian) "سبک یا کشور" else "Genre / Country") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, url, genre) },
                colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian)
            ) {
                Text(if (isPersian) "افزودن و پخش" else "Save & Tune")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel", color = FazoraSteel)
            }
        }
    )
}

@Composable
fun StudioDspRackPanel(
    dspState: StudioDspState,
    isPersian: Boolean,
    onSetPreampDb: (Float) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetStereoWidth: (Int) -> Unit,
    onSetCrossfadeSec: (Int) -> Unit,
    onSetLoopPointA: () -> Unit,
    onSetLoopPointB: () -> Unit,
    onClearLoop: () -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onImportCustomLrcOrSrt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportLyricsDialog by remember { mutableStateOf(false) }
    var rawLyricsInput by remember { mutableStateOf("") }

    Surface(
        color = FazoraCarbon,
        shape = RoundedCornerShape(PhiTokens.CornerRadius13),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius13))
            .testTag("studio_dsp_rack_panel")
    ) {
        Column(
            modifier = Modifier.padding(PhiTokens.Space13),
            verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPersian) "رک پردازش استودیویی (Preamp • Bass • 3D • تایمر خواب)" else "STUDIO DSP RACK & MASTERING CONTROLS",
                    style = MaterialTheme.typography.titleMedium,
                    color = VeloraChampagneGold
                )
                TextButton(onClick = {
                    onSetPreampDb(0f)
                    onSetBassBoost(0)
                    onSetStereoWidth(0)
                }) {
                    Text(if (isPersian) "ریست DSP" else "Reset DSP", color = FazoraGold)
                }
            }

            Surface(
                color = FazoraCarbonElevated,
                shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, FazoraCarbonBorder, RoundedCornerShape(PhiTokens.CornerRadius8))
                    .testTag("audio_pipeline_transparency_panel")
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = if (isPersian) "مسیر شفاف پردازش سیگنال (Audio Pipeline):"
                        else "SIGNAL PIPELINE TRANSPARENCY:",
                        style = MaterialTheme.typography.labelSmall,
                        color = FazoraGold
                    )
                    Text(
                        text = "PCM Decode ➔ Preamp (${String.format(Locale.US, "%+.1fdB", dspState.preampGainDb)}) " +
                            "➔ 10-Band Biquad IIR ➔ Sub-Bass (${dspState.bassBoostPercent}%) " +
                            "➔ M/S 3D (${dspState.stereoWidthPercent}%) ➔ Limiter ➔ Hardware DAC",
                        style = MaterialTheme.typography.labelSmall,
                        color = FazoraSteel
                    )
                }
            }

            // 1. Preamp Gain Slider (-12 dB to +12 dB)
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isPersian) "پیش‌تقویت‌کننده (Preamp Gain)" else "Preamp Headroom Gain",
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraIvory
                    )
                    Text(
                        text = String.format(Locale.US, "%+.1f dB", dspState.preampGainDb),
                        style = MaterialTheme.typography.labelLarge,
                        color = FazoraGold
                    )
                }
                Slider(
                    value = dspState.preampGainDb,
                    onValueChange = onSetPreampDb,
                    valueRange = -12f..12f,
                    colors = SliderDefaults.colors(
                        thumbColor = FazoraGold,
                        activeTrackColor = FazoraGold,
                        inactiveTrackColor = FazoraCarbonBorder
                    ),
                    modifier = Modifier.testTag("dsp_preamp_slider")
                )
            }

            // 2. Bass Boost & 3D Stereo Widener side-by-side
            Row(
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sub-Bass Boost: ${dspState.bassBoostPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = FazoraIvory
                    )
                    Slider(
                        value = dspState.bassBoostPercent.toFloat(),
                        onValueChange = { onSetBassBoost(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = FazoraGold,
                            activeTrackColor = FazoraGold
                        ),
                        modifier = Modifier.testTag("dsp_bass_boost_slider")
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3D Stereo Width: ${dspState.stereoWidthPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = FazoraIvory
                    )
                    Slider(
                        value = dspState.stereoWidthPercent.toFloat(),
                        onValueChange = { onSetStereoWidth(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = VeloraChampagneGold,
                            activeTrackColor = VeloraChampagneGold
                        ),
                        modifier = Modifier.testTag("dsp_stereo_width_slider")
                    )
                }
            }

            // 3. A-B Repeat Loop Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, tint = FazoraGold)
                    val aLabel = dspState.loopStartMs?.let { "${it / 1000}s" } ?: "--"
                    val bLabel = dspState.loopEndMs?.let { "${it / 1000}s" } ?: "--"
                    Text(
                        text = "A-B Loop ($aLabel → $bLabel)",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (dspState.isAbLoopActive) FazoraGoldBright else FazoraIvory
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onSetLoopPointA,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("ab_loop_set_a")
                    ) {
                        Text("Set A", color = FazoraGold)
                    }
                    OutlinedButton(
                        onClick = onSetLoopPointB,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("ab_loop_set_b")
                    ) {
                        Text("Set B", color = FazoraGold)
                    }
                    if (dspState.loopStartMs != null || dspState.loopEndMs != null) {
                        TextButton(onClick = onClearLoop) {
                            Text("Clear", color = FazoraSteel)
                        }
                    }
                }
            }

            // 4. Smart Sleep Timer with Smooth Fade-Out & Crossfade
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = VeloraChampagneGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) {
                                "تایمر خواب هوشمند (${dspState.formattedSleepTimer})"
                            } else {
                                "Smart Sleep Timer (${dspState.formattedSleepTimer})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = FazoraIvory
                        )
                    }
                    OutlinedButton(
                        onClick = { showImportLyricsDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("import_lrc_srt_btn")
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = FazoraGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPersian) "وارد کردن LRC/SRT" else "Import LRC/SRT", color = FazoraGold)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    val timerOptions = listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m")
                    items(timerOptions, key = { it.first }) { (mins, label) ->
                        val active = (mins == 0 && dspState.sleepTimerRemainingSec <= 0) ||
                            (mins > 0 && dspState.sleepTimerLabel == label)
                        Surface(
                            color = if (active) FazoraGold else FazoraCarbonElevated,
                            shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                            modifier = Modifier
                                .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                                .clickable { onSetSleepTimerMinutes(mins) }
                                .testTag("sleep_timer_${mins}m")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) FazoraObsidian else FazoraIvory,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    val cfOptions = listOf(0, 2, 4, 8, 12)
                    items(cfOptions, key = { "cf_$it" }) { sec ->
                        val active = dspState.crossfadeSeconds == sec
                        Surface(
                            color = if (active) FazoraGoldMuted else FazoraCarbonElevated,
                            shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                            modifier = Modifier
                                .clip(RoundedCornerShape(PhiTokens.CornerRadius8))
                                .clickable { onSetCrossfadeSec(sec) }
                                .border(
                                    1.dp,
                                    if (active) FazoraGold else FazoraCarbonBorder,
                                    RoundedCornerShape(PhiTokens.CornerRadius8)
                                )
                        ) {
                            Text(
                                text = if (sec == 0) "Crossfade: Off" else "CF: ${sec}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) FazoraGold else FazoraSteel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImportLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showImportLyricsDialog = false },
            containerColor = FazoraCarbon,
            titleContentColor = FazoraIvory,
            textContentColor = FazoraIvory,
            title = {
                Text(if (isPersian) "وارد کردن متن آهنگ (.LRC) یا زیرنویس (.SRT)" else "Import Synchronized .LRC / .SRT")
            },
            text = {
                OutlinedTextField(
                    value = rawLyricsInput,
                    onValueChange = { rawLyricsInput = it },
                    placeholder = {
                        Text("[00:05.00] First synchronized line...\n[00:10.50] Second line...")
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCustomLrcOrSrt(rawLyricsInput)
                        showImportLyricsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FazoraGold, contentColor = FazoraObsidian)
                ) {
                    Text(if (isPersian) "اعمال و سینک" else "Apply Cues")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportLyricsDialog = false }) {
                    Text(if (isPersian) "انصراف" else "Cancel", color = FazoraSteel)
                }
            }
        )
    }
}
