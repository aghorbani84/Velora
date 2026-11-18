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

    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album WHERE id = :trackId")
    suspend fun updateTrackMetadata(trackId: String, title: String, artist: String, album: String)

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

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getNextPositionForPlaylist(playlistId: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    @Transaction
    suspend fun createPlaylistTransaction(
        name: String,
        createdAtMs: Long,
        initialTrackIds: List<String>
    ): Long {
        val playlistId = insertPlaylist(
            PlaylistEntity(
                name = name,
                createdAtMs = createdAtMs
            )
        )
        initialTrackIds.forEachIndexed { idx, trackId ->
            addTrackToPlaylist(
                PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = trackId,
                    position = idx
                )
            )
        }
        return playlistId
    }

    @Transaction
    suspend fun deletePlaylistTransaction(playlistId: Long) {
        clearPlaylistTracks(playlistId)
        deletePlaylist(playlistId)
    }
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

    @Transaction
    suspend fun recordPlayAtomically(
        trackId: String,
        clampedPosition: Long,
        playedAtMs: Long
    ): PlaybackHistoryEntity {
        val existing = getHistoryForTrack(trackId)
        val updatedPlayCount = (existing?.playCount ?: 0) + 1
        val completedCount = existing?.completedCount ?: 0
        val entity = PlaybackHistoryEntity(
            trackId = trackId,
            lastPlayedAtMs = playedAtMs,
            lastPositionMs = clampedPosition,
            playCount = updatedPlayCount,
            completedCount = completedCount
        )
        upsertHistory(entity)
        return entity
    }

    @Transaction
    suspend fun updateHistoryPositionAtomically(
        trackId: String,
        positionMs: Long,
        completed: Boolean,
        nowMs: Long
    ) {
        val existing = getHistoryForTrack(trackId) ?: return
        val updated = existing.copy(
            lastPositionMs = positionMs.coerceAtLeast(0L),
            lastPlayedAtMs = nowMs,
            completedCount = if (completed) existing.completedCount + 1 else existing.completedCount
        )
        upsertHistory(updated)
    }

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
    version = 4,
    // P0-3: export the schema JSON for every version so MigrationTestHelper
    // can validate migrations in CI (schemas land in app/schemas/; see the
    // ksp block in app/build.gradle.kts). Config-only change, no behavior change.
    exportSchema = true
)
abstract class VeloraDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playback_history` (
                        `trackId` TEXT NOT NULL,
                        `lastPlayedAtMs` INTEGER NOT NULL,
                        `lastPositionMs` INTEGER NOT NULL,
                        `playCount` INTEGER NOT NULL,
                        `completedCount` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`trackId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playback_history_lastPlayedAtMs` ON `playback_history` (`lastPlayedAtMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playback_history_playCount` ON `playback_history` (`playCount`)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_eq_presets` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `gainsCsv` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_tracks_mediaStoreId`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_mediaStoreId` ON `tracks` (`mediaStoreId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_mediaType` ON `tracks` (`mediaType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_isFavorite` ON `tracks` (`isFavorite`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracks_folderPath` ON `tracks` (`folderPath`)")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}

