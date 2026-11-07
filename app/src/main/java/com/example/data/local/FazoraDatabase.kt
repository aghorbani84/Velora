package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE mediaType = :mediaType ORDER BY title COLLATE NOCASE ASC")
    fun observeTracksByType(mediaType: String): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE title LIKE '%' || :query || '%'
           OR artist LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllTracksOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun getTrackById(trackId: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun updateFavoriteStatus(trackId: String, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: String)

    @Query("DELETE FROM tracks WHERE folderPath = :folderPath")
    suspend fun deleteTracksByFolder(folderPath: String)

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
}

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY createdAtMs DESC")
    fun observePlaylistsWithTracks(): Flow<List<PlaylistWithTracksRelation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)
}

@Dao
interface EqPresetDao {
    @Query("SELECT * FROM custom_eq_presets ORDER BY createdAtMs ASC")
    fun observeCustomPresets(): Flow<List<CustomEqPresetEntity>>

    @Query("SELECT * FROM custom_eq_presets ORDER BY createdAtMs ASC")
    suspend fun getCustomPresetsOnce(): List<CustomEqPresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreset(preset: CustomEqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE id = :presetId")
    suspend fun deletePreset(presetId: String)
}

@Dao
interface PlaybackHistoryDao {
    @Transaction
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAtMs DESC LIMIT :limit")
    fun observeRecentHistory(limit: Int = 50): Flow<List<PlaybackHistoryWithTrackRelation>>

    @Transaction
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAtMs DESC LIMIT :limit")
    suspend fun getRecentHistoryOnce(limit: Int = 50): List<PlaybackHistoryWithTrackRelation>

    @Query("SELECT * FROM playback_history WHERE trackId = :trackId LIMIT 1")
    suspend fun getHistoryForTrack(trackId: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE trackId = :trackId")
    suspend fun deleteHistoryByTrackId(trackId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearAllHistory()
}

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        CustomEqPresetEntity::class,
        PlaybackHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FazoraDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
}
