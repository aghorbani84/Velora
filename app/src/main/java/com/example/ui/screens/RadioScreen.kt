package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.domain.model.RadioStation
import com.example.ui.components.VeloraEmptyState
import com.example.ui.components.VeloraPrimaryButton
import com.example.ui.components.VeloraSecondaryButton
import com.example.ui.components.VeloraSectionCard
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.FazoraUiState

@Composable
fun RadioScreen(
    uiState: FazoraUiState,
    onTuneStation: (RadioStation) -> Unit,
    onAddStationToQueue: (RadioStation) -> Unit,
    onAddCustomStation: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val genres = remember(uiState.radioStations) {
        listOf("All") + uiState.radioStations.map { it.category.labelEn }.distinct()
    }
    var selectedGenre by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredStations = remember(uiState.radioStations, selectedGenre) {
        if (selectedGenre == "All") {
            uiState.radioStations
        } else {
            uiState.radioStations.filter { it.category.labelEn.equals(selectedGenre, ignoreCase = true) }
        }
    }

    if (showAddDialog) {
        AddCustomRadioDialog(
            isPersian = isPersian,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url, genre ->
                onAddCustomStation(name, url, genre)
                showAddDialog = false
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(PhiTokens.Space21),
        verticalArrangement = Arrangement.spacedBy(PhiTokens.Space13),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("radio_screen")
    ) {
        item {
            VeloraSectionCard(
                eyebrow = if (isPersian) "رادیو اینترنتی زنده" else "LIVE STREAMING TUNER",
                title = if (isPersian) "ایستگاه‌های رادیویی (${filteredStations.size})" else "Live Radio (${filteredStations.size})",
                subtitle = if (isPersian) {
                    "پخش مستقیم استریم‌های صوتی HTTP / HLS / ICY"
                } else {
                    "Direct HTTP / HLS / ICY high-bitrate audio broadcasts"
                },
                headerAction = {
                    VeloraPrimaryButton(
                        text = if (isPersian) "افزودن موج" else "Add Stream",
                        icon = Icons.Default.Add,
                        onClick = { showAddDialog = true },
                        tag = "add_custom_radio_button"
                    )
                }
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                    items(genres) { genre ->
                        VeloraSecondaryButton(
                            text = genre,
                            selected = selectedGenre == genre,
                            onClick = { selectedGenre = genre },
                            tag = "radio_genre_chip_${genre.lowercase()}"
                        )
                    }
                }
            }
        }

        if (filteredStations.isEmpty()) {
            item {
                VeloraEmptyState(
                    icon = Icons.Default.Radio,
                    title = if (isPersian) "ایستگاهی در این دسته نیست" else "No Stations in Category",
                    subtitle = if (isPersian) "یک استریم سفارشی اضافه کنید." else "Add a custom stream URL above."
                )
            }
        } else {
            items(filteredStations, key = { it.id }) { station ->
                VeloraSectionCard(
                    eyebrow = "${station.category.labelEn} • ${station.bitrateKbps} kbps • ${station.codecLabel}",
                    title = station.name,
                    subtitle = station.country,
                    modifier = Modifier.testTag("radio_station_card_${station.id}")
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                        VeloraPrimaryButton(
                            text = if (isPersian) "پخش زنده" else "Tune Live",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onTuneStation(station) },
                            tag = "tune_station_${station.id}",
                            modifier = Modifier.weight(PhiTokens.PRIMARY_WEIGHT)
                        )
                        VeloraSecondaryButton(
                            text = if (isPersian) "صف پخش" else "Queue",
                            icon = Icons.Default.AddToQueue,
                            onClick = { onAddStationToQueue(station) },
                            tag = "queue_station_${station.id}",
                            modifier = Modifier.weight(PhiTokens.SECONDARY_WEIGHT)
                        )
                    }
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
    var url by remember { mutableStateOf("https://") }
    var genre by remember { mutableStateOf("Ambient") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(if (isPersian) "افزودن ایستگاه رادیویی سفارشی" else "Add Custom Radio Stream")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhiTokens.Space8)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isPersian) "نام ایستگاه" else "Station Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_radio_name_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("HTTPS Stream URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_radio_url_input")
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text(if (isPersian) "ژانر" else "Genre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            VeloraPrimaryButton(
                text = if (isPersian) "افزودن" else "Add Station",
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onConfirm(name.trim(), url.trim(), genre.ifBlank { "Custom" })
                    }
                },
                tag = "confirm_add_radio_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isPersian) "انصراف" else "Cancel")
            }
        }
    )
}
