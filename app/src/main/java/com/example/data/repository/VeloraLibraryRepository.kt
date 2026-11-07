package com.example.data.repository

import android.content.Context
import com.example.data.local.CustomEqPresetEntity
import com.example.data.local.EqPresetDao
import com.example.data.local.PlaybackHistoryDao
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackDao
import com.example.data.local.TrackEntity
import com.example.data.scanner.BundledMediaSynthesizer
import com.example.data.scanner.MediaLibraryScanner
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
import com.example.domain.model.MediaType
import com.example.domain.model.PhiDesignMetrics
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.Playlist
import com.example.domain.model.ScanProgress
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class VeloraLibraryRepository(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val eqPresetDao: EqPresetDao,
    private val scanner: MediaLibraryScanner,
    private val playbackHistoryDao: PlaybackHistoryDao? = null
) {
    val scanProgress: StateFlow<ScanProgress> = scanner.scanProgress

    val allTracks: Flow<List<Track>> = trackDao.observeAllTracks()
        .map { list -> list.map { it.toDomain() } }

    val audioTracks: Flow<List<Track>> = trackDao.observeTracksByType(MediaType.AUDIO.name)
        .map { list -> list.map { it.toDomain() } }

    val videoTracks: Flow<List<Track>> = trackDao.observeTracksByType(MediaType.VIDEO.name)
        .map { list -> list.map { it.toDomain() } }

    val favoriteTracks: Flow<List<Track>> = trackDao.observeFavoriteTracks()
        .map { list -> list.map { it.toDomain() } }

    val playlists: Flow<List<Playlist>> = playlistDao.observePlaylistsWithTracks()
        .map { list -> list.map { it.toDomain() } }

    val playbackHistory: Flow<List<PlaybackHistoryEntry>> =
        playbackHistoryDao?.observeRecentHistory(limit = 50)
            ?.map { relations -> relations.mapNotNull { it.toDomain() } }
            ?: flowOf(emptyList())

    val allEqPresets: Flow<List<EqPreset>> = eqPresetDao.observeCustomPresets()
        .map { customEntities ->
            EqPreset.BUILT_IN_PRESETS + customEntities.map { it.toDomain() }
        }

    fun searchTracks(query: String): Flow<List<Track>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            allTracks
        } else {
            trackDao.searchTracks(trimmed).map { list -> list.map { it.toDomain() } }
        }
    }

    suspend fun getAllTracksOnce(): List<Track> = withContext(Dispatchers.IO) {
        trackDao.getAllTracksOnce().map { it.toDomain() }
    }

    suspend fun performScan(
        allowedFolderUris: Set<String>,
        includeMediaStore: Boolean = true
    ): List<Track> = withContext(Dispatchers.IO) {
        val existingFavorites = trackDao.getAllTracksOnce()
            .filter { it.isFavorite }
            .map { it.id }
            .toSet()
        val discovered = scanner.scanAllSources(allowedFolderUris, includeMediaStore)
        val uniqueEntities = discovered
            .distinctBy { it.mediaStoreId }
            .distinctBy { it.uri }
            .map { track ->
                val preservedFav = track.isFavorite || (track.id in existingFavorites)
                TrackEntity.fromDomain(track.copy(isFavorite = preservedFav))
            }
        trackDao.upsertTracks(uniqueEntities)
        uniqueEntities.map { it.toDomain() }
    }

    suspend fun toggleFavorite(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val existing = trackDao.getTrackById(trackId) ?: return@withContext false
        val newFav = !existing.isFavorite
        trackDao.updateFavoriteStatus(trackId, newFav)
        newFav
    }

    suspend fun createAndScanCustomAcousticFolder(folderTitle: String): String? = withContext(Dispatchers.IO) {
        val cleanName = folderTitle.trim()
            .replace(Regex("[^a-zA-Z0-9 _-]"), "")
            .ifBlank { "Custom Session" }
        val targetDir = File(context.filesDir, "user_folders/${cleanName.replace(' ', '_')}")
        if (!PhiDesignMetrics.isSafeMediaFolder(targetDir.absolutePath)) return@withContext null
        targetDir.mkdirs()

        val trackFile1 = File(targetDir, "${cleanName.lowercase().replace(' ', '_')}_chords.wav")
        val trackFile2 = File(targetDir, "${cleanName.lowercase().replace(' ', '_')}_lead.wav")

        if (!trackFile1.exists()) {
            BundledMediaSynthesizer.writeStereoWavFile(
                targetFile = trackFile1,
                durationSeconds = 9,
                frequenciesHz = listOf(130.81, 196.00, 261.63, 392.00, 523.25),
                modRateHz = 1.2
            )
        }
        if (!trackFile2.exists()) {
            BundledMediaSynthesizer.writeStereoWavFile(
                targetFile = trackFile2,
                durationSeconds = 11,
                frequenciesHz = listOf(220.0, 330.0, 440.0, 880.0, 1760.0),
                modRateHz = 1.8
            )
        }

        val scanned = scanner.scanLocalDirectory(targetDir, targetDir.absolutePath)
        trackDao.upsertTracks(scanned.map { TrackEntity.fromDomain(it) })
        targetDir.absolutePath
    }

    suspend fun removeFolderTracks(folderPath: String) = withContext(Dispatchers.IO) {
        trackDao.deleteTracksByFolder(folderPath)
    }

    suspend fun createPlaylist(name: String, initialTrackIds: List<String> = emptyList()): Long =
        withContext(Dispatchers.IO) {
            val cleanName = name.trim().ifBlank { "Untitled Playlist" }
            val playlistId = playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = cleanName,
                    createdAtMs = System.currentTimeMillis()
                )
            )
            initialTrackIds.forEachIndexed { idx, trackId ->
                playlistDao.addTrackToPlaylist(
                    PlaylistTrackCrossRef(
                        playlistId = playlistId,
                        trackId = trackId,
                        position = idx
                    )
                )
            }
            playlistId
        }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistTracks(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = (System.currentTimeMillis() % 100_000).toInt()
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun saveCustomEqPreset(name: String, gainsDb: List<Float>): EqPreset =
        withContext(Dispatchers.IO) {
            val cleanName = name.trim().ifBlank { "Custom EQ" }
            val normalizedGains = EqSpecification.normalizeGains(gainsDb)
            val presetId = "custom_eq_${cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}_${System.currentTimeMillis()}"
            val preset = EqPreset(
                id = presetId,
                name = cleanName,
                gainsDb = normalizedGains,
                isBuiltIn = false
            )
            eqPresetDao.upsertPreset(CustomEqPresetEntity.fromDomain(preset))
            preset
        }

    suspend fun deleteCustomEqPreset(presetId: String) = withContext(Dispatchers.IO) {
        if (!presetId.startsWith("builtin_")) {
            eqPresetDao.deletePreset(presetId)
        }
    }

    /**
     * Caches audio/video file metadata in the local Room database for offline access.
     */
    suspend fun cacheTrackMetadata(track: Track) = withContext(Dispatchers.IO) {
        val existingFav = trackDao.getTrackById(track.id)?.isFavorite ?: track.isFavorite
        trackDao.upsertTracks(listOf(TrackEntity.fromDomain(track.copy(isFavorite = existingFav))))
    }

    /**
     * Caches the track's full metadata and records a playback event in Room for offline history.
     */
    suspend fun recordTrackPlayed(
        track: Track,
        startPositionMs: Long = 0L,
        playedAtMs: Long = System.currentTimeMillis()
    ): PlaybackHistoryEntry = withContext(Dispatchers.IO) {
        val existingFav = trackDao.getTrackById(track.id)?.isFavorite ?: track.isFavorite
        val cachedTrack = track.copy(isFavorite = existingFav)
        trackDao.upsertTracks(listOf(TrackEntity.fromDomain(cachedTrack)))
        val historyDao = playbackHistoryDao
        val existing = historyDao?.getHistoryForTrack(track.id)
        val updatedPlayCount = (existing?.playCount ?: 0) + 1
        val completedCount = existing?.completedCount ?: 0
        val clampedPosition = startPositionMs.coerceIn(0L, track.durationMs.coerceAtLeast(0L))
        val entity = PlaybackHistoryEntity(
            trackId = track.id,
            lastPlayedAtMs = playedAtMs,
            lastPositionMs = clampedPosition,
            playCount = updatedPlayCount,
            completedCount = completedCount
        )
        historyDao?.upsertHistory(entity)
        PlaybackHistoryEntry(
            track = cachedTrack,
            lastPlayedAtMs = playedAtMs,
            lastPositionMs = clampedPosition,
            playCount = updatedPlayCount,
            completedCount = completedCount
        )
    }

    /**
     * Updates the last known playback position (and optional completion count) in offline Room history.
     */
    suspend fun updateTrackHistoryPosition(
        trackId: String,
        positionMs: Long,
        completed: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val historyDao = playbackHistoryDao ?: return@withContext
        val existing = historyDao.getHistoryForTrack(trackId) ?: return@withContext
        val updated = existing.copy(
            lastPositionMs = positionMs.coerceAtLeast(0L),
            lastPlayedAtMs = System.currentTimeMillis(),
            completedCount = if (completed) existing.completedCount + 1 else existing.completedCount
        )
        historyDao.upsertHistory(updated)
    }

    /**
     * Clears all offline playback history entries while preserving cached track metadata.
     */
    suspend fun clearPlaybackHistory() = withContext(Dispatchers.IO) {
        playbackHistoryDao?.clearAllHistory()
    }
}
