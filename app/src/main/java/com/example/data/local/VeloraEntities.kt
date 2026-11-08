package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.EqPreset
import com.example.domain.model.EqSpecification
import com.example.domain.model.MediaType
import com.example.domain.model.PlaybackHistoryEntry
import com.example.domain.model.Playlist
import com.example.domain.model.Track

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["uri"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"])
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val folderPath: String,
    val mediaType: String,
    val artworkUri: String?,
    val addedTimestampMs: Long,
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val isFavorite: Boolean = false
) {
    fun toDomain(): Track = Track(
        id = id,
        mediaStoreId = mediaStoreId,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = uri,
        folderPath = folderPath,
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.AUDIO),
        artworkUri = artworkUri,
        addedTimestampMs = addedTimestampMs,
        sampleRateHz = sampleRateHz,
        bitrateKbps = bitrateKbps,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            mediaStoreId = track.mediaStoreId,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            uri = track.uri,
            folderPath = track.folderPath,
            mediaType = track.mediaType.name,
            artworkUri = track.artworkUri,
            addedTimestampMs = track.addedTimestampMs,
            sampleRateHz = track.sampleRateHz,
            bitrateKbps = track.bitrateKbps,
            isFavorite = track.isFavorite
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAtMs: Long
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index(value = ["trackId"]), Index(value = ["playlistId"])]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val position: Int = 0
)

data class PlaylistWithTracksRelation(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "trackId"
        )
    )
    val tracks: List<TrackEntity>
) {
    fun toDomain(): Playlist = Playlist(
        id = playlist.id,
        name = playlist.name,
        createdAtMs = playlist.createdAtMs,
        tracks = tracks.map { it.toDomain() }
    )
}

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gainsCsv: String,
    val createdAtMs: Long
) {
    fun toDomain(): EqPreset {
        val parsed = gainsCsv.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
        val normalized = EqSpecification.normalizeGains(parsed)
        return EqPreset(
            id = id,
            name = name,
            gainsDb = normalized,
            isBuiltIn = false
        )
    }

    companion object {
        fun fromDomain(preset: EqPreset, createdAtMs: Long = System.currentTimeMillis()): CustomEqPresetEntity {
            val normalized = EqSpecification.normalizeGains(preset.gainsDb)
            return CustomEqPresetEntity(
                id = preset.id,
                name = preset.name,
                gainsCsv = normalized.joinToString(",") { "%.2f".format(java.util.Locale.US, it) },
                createdAtMs = createdAtMs
            )
        }
    }
}

@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["lastPlayedAtMs"]),
        Index(value = ["playCount"])
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey val trackId: String,
    val lastPlayedAtMs: Long,
    val lastPositionMs: Long,
    val playCount: Int,
    val completedCount: Int = 0
)

data class PlaybackHistoryWithTrackRelation(
    @Embedded val history: PlaybackHistoryEntity,
    @Relation(
        parentColumn = "trackId",
        entityColumn = "id"
    )
    val track: TrackEntity?
) {
    fun toDomain(): PlaybackHistoryEntry? {
        val domainTrack = track?.toDomain() ?: return null
        return PlaybackHistoryEntry(
            track = domainTrack,
            lastPlayedAtMs = history.lastPlayedAtMs,
            lastPositionMs = history.lastPositionMs,
            playCount = history.playCount,
            completedCount = history.completedCount
        )
    }
}

