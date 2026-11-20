package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.ui.graphics.vector.ImageVector

enum class LibrarySubTab(
    val labelEn: String,
    val labelFa: String,
    val icon: ImageVector,
    val testTag: String
) {
    PLAYLISTS("Collections", "مجموعه‌ها", Icons.Default.LibraryMusic, "library_subtab_playlists"),
    MUSIC("Music", "موسیقی", Icons.Default.MusicNote, "library_subtab_music"),
    ARTISTS("Artists", "هنرمندان", Icons.Default.Album, "library_subtab_artists"),
    RADIO("Radio", "رادیو", Icons.Default.Radio, "library_subtab_radio"),
    FOLDERS("Folders", "پوشه‌ها", Icons.Default.Folder, "library_subtab_folders")
}

internal data class FolderGroup(
    val path: String,
    val displayName: String,
    val tracks: List<com.example.domain.model.Track>
)
