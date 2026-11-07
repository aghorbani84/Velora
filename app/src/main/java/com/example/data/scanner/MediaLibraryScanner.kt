package com.example.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.domain.model.MediaType
import com.example.domain.model.PhiDesignMetrics
import com.example.domain.model.ScanProgress
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class MediaLibraryScanner(
    private val context: Context
) {
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    suspend fun scanAllSources(
        allowedFolderUris: Set<String>,
        includeMediaStore: Boolean = true
    ): List<Track> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        _scanProgress.value = ScanProgress(
            isScanning = true,
            scannedCount = 0,
            indexedCount = 0,
            currentSourceLabel = "Fazora Studio Reference Masters",
            errorMessage = null
        )

        val dedupByMediaStoreId = linkedMapOf<Long, Track>()
        val dedupByUri = linkedSetOf<String>()

        fun registerTrack(track: Track) {
            if (!PhiDesignMetrics.isSafeMediaFolder(track.folderPath)) return
            if (dedupByUri.contains(track.uri)) return
            if (dedupByMediaStoreId.containsKey(track.mediaStoreId)) return
            dedupByUri.add(track.uri)
            dedupByMediaStoreId[track.mediaStoreId] = track
            _scanProgress.update {
                it.copy(
                    scannedCount = it.scannedCount + 1,
                    indexedCount = dedupByMediaStoreId.size
                )
            }
        }

        try {
            val bundled = BundledMediaSynthesizer.ensureStudioMastersExist(context)
            bundled.forEach { registerTrack(it) }

            if (includeMediaStore) {
                _scanProgress.update { it.copy(currentSourceLabel = "Device MediaStore Audio") }
                queryMediaStoreAudio().forEach { registerTrack(it) }

                _scanProgress.update { it.copy(currentSourceLabel = "Device MediaStore Video") }
                queryMediaStoreVideo().forEach { registerTrack(it) }
            }

            for (folderUriOrPath in allowedFolderUris) {
                if (!PhiDesignMetrics.isSafeMediaFolder(folderUriOrPath)) {
                    Log.w(TAG, "Skipping disallowed folder during scan: $folderUriOrPath")
                    continue
                }
                _scanProgress.update { it.copy(currentSourceLabel = folderUriOrPath) }
                if (folderUriOrPath.startsWith("content://")) {
                    scanSafTreeUri(Uri.parse(folderUriOrPath)).forEach { registerTrack(it) }
                } else {
                    val dir = File(folderUriOrPath)
                    if (dir.exists() && dir.isDirectory) {
                        scanLocalDirectory(dir, dir.absolutePath).forEach { registerTrack(it) }
                    }
                }
            }

            val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            _scanProgress.update {
                it.copy(
                    isScanning = false,
                    indexedCount = dedupByMediaStoreId.size,
                    currentSourceLabel = "Complete (${dedupByMediaStoreId.size} tracks)",
                    lastScanDurationMs = elapsed
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Media scan error", e)
            val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            _scanProgress.update {
                it.copy(
                    isScanning = false,
                    lastScanDurationMs = elapsed,
                    errorMessage = "Scan interrupted: ${e.localizedMessage ?: "Permission or I/O error"}"
                )
            }
        }

        dedupByMediaStoreId.values.toList()
    }

    private fun queryMediaStoreAudio(): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Include all audio tracks >= 5 seconds or flagged as music, capturing downloads and recordings
        val selection = "(${MediaStore.Audio.Media.DURATION} >= 5000) OR (${MediaStore.Audio.Media.IS_MUSIC} != 0)"
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val title = (if (titleCol >= 0) cursor.getString(titleCol) else null)?.takeIf { it.isNotBlank() } ?: "Audio $mediaId"
                    val artist = (if (artistCol >= 0) cursor.getString(artistCol) else null)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Device Music"
                    val album = (if (albumCol >= 0) cursor.getString(albumCol) else null)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Music Library"
                    val durationMs = (if (durationCol >= 0) cursor.getLong(durationCol) else 0L).coerceAtLeast(1000L)
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L
                    val dateAdded = (if (dateCol >= 0) cursor.getLong(dateCol) else 0L) * 1000L

                    val contentUri = ContentUris.withAppendedId(collection, mediaId).toString()
                    val artworkUri = if (albumId >= 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else {
                        null
                    }

                    tracks.add(
                        Track(
                            id = "ms_audio_$mediaId",
                            mediaStoreId = mediaId,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            uri = contentUri,
                            folderPath = "/storage/emulated/0/Music",
                            mediaType = MediaType.AUDIO,
                            artworkUri = artworkUri,
                            addedTimestampMs = dateAdded
                        )
                    )
                }
            }
        }.onFailure { Log.w(TAG, "MediaStore audio query warning: ${it.message}") }
        return tracks
    }

    private fun queryMediaStoreVideo(): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        // Note: MediaStore.Video.Media does NOT have ARTIST or ALBUM columns in Android SDK.
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_ADDED
        )

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val displayName = if (displayCol >= 0) cursor.getString(displayCol) else null
                    val rawTitle = if (titleCol >= 0) cursor.getString(titleCol) else null
                    val title = (rawTitle ?: displayName)?.takeIf { it.isNotBlank() } ?: "Video $mediaId"
                    val durationMs = (if (durationCol >= 0) cursor.getLong(durationCol) else 0L).coerceAtLeast(1000L)
                    val dateAdded = (if (dateCol >= 0) cursor.getLong(dateCol) else 0L) * 1000L

                    val contentUri = ContentUris.withAppendedId(collection, mediaId).toString()

                    tracks.add(
                        Track(
                            id = "ms_video_$mediaId",
                            mediaStoreId = 1_000_000_000L + mediaId,
                            title = title,
                            artist = "Local Video",
                            album = "Device Videos",
                            durationMs = durationMs,
                            uri = contentUri,
                            folderPath = "/storage/emulated/0/Movies",
                            mediaType = MediaType.VIDEO,
                            artworkUri = null,
                            addedTimestampMs = dateAdded
                        )
                    )
                }
            }
        }.onFailure { Log.w(TAG, "MediaStore video query warning: ${it.message}") }
        return tracks
    }

    private fun scanSafTreeUri(treeUri: Uri): List<Track> {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val found = mutableListOf<Track>()

        fun walk(doc: DocumentFile, depth: Int) {
            if (depth > 6) return
            if (doc.isDirectory) {
                val name = doc.name ?: ""
                if (name.startsWith(".")) return
                doc.listFiles().forEach { child -> walk(child, depth + 1) }
            } else if (doc.isFile) {
                val name = doc.name ?: return
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in SUPPORTED_AUDIO_EXTENSIONS || ext in SUPPORTED_VIDEO_EXTENSIONS) {
                    val mediaType = if (ext in SUPPORTED_VIDEO_EXTENSIONS) MediaType.VIDEO else MediaType.AUDIO
                    val uriStr = doc.uri.toString()
                    val stableHash = abs(uriStr.hashCode().toLong()) + 500_000_000L
                    val enriched = extractMetadataFromUri(
                        uri = doc.uri,
                        fallbackTitle = name.substringBeforeLast('.'),
                        fallbackFolder = treeUri.toString(),
                        mediaType = mediaType,
                        stableId = stableHash
                    )
                    found.add(enriched)
                }
            }
        }

        walk(rootDoc, 0)
        return found
    }

    fun scanLocalDirectory(dir: File, rootLabel: String): List<Track> {
        if (!PhiDesignMetrics.isSafeMediaFolder(dir.absolutePath)) return emptyList()
        val found = mutableListOf<Track>()

        fun walk(current: File, depth: Int) {
            if (depth > 6 || !current.exists()) return
            if (current.name.startsWith(".")) return
            if (current.isDirectory) {
                current.listFiles()?.forEach { child -> walk(child, depth + 1) }
            } else if (current.isFile) {
                val ext = current.extension.lowercase()
                if (ext in SUPPORTED_AUDIO_EXTENSIONS || ext in SUPPORTED_VIDEO_EXTENSIONS) {
                    val mediaType = if (ext in SUPPORTED_VIDEO_EXTENSIONS) MediaType.VIDEO else MediaType.AUDIO
                    val stableHash = abs(current.absolutePath.hashCode().toLong()) + 700_000_000L
                    val enriched = extractMetadataFromFile(
                        file = current,
                        folderPath = rootLabel,
                        mediaType = mediaType,
                        stableId = stableHash
                    )
                    found.add(enriched)
                }
            }
        }

        walk(dir, 0)
        return found
    }

    private fun extractMetadataFromUri(
        uri: Uri,
        fallbackTitle: String,
        fallbackFolder: String,
        mediaType: MediaType,
        stableId: Long
    ): Track {
        val retriever = MediaMetadataRetriever()
        var title = fallbackTitle
        var artist = "Local Artist"
        var album = "Folder Collection"
        var durationMs = 10_000L
        var bitrateKbps = 320

        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.takeIf { it > 0L }?.let { durationMs = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.takeIf { it > 0 }?.let { bitrateKbps = (it / 1000).coerceAtLeast(64) }
        } catch (_: Throwable) {
        } finally {
            runCatching { retriever.release() }
        }

        return Track(
            id = "saf_$stableId",
            mediaStoreId = stableId,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = uri.toString(),
            folderPath = fallbackFolder,
            mediaType = mediaType,
            artworkUri = null,
            addedTimestampMs = System.currentTimeMillis(),
            bitrateKbps = bitrateKbps
        )
    }

    private fun extractMetadataFromFile(
        file: File,
        folderPath: String,
        mediaType: MediaType,
        stableId: Long
    ): Track {
        val retriever = MediaMetadataRetriever()
        var title = file.nameWithoutExtension
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
        var artist = "Local Studio"
        var album = file.parentFile?.name ?: "Local Folder"
        var durationMs = 12_000L

        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }?.let { artist = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.takeIf { it > 0L }?.let { durationMs = it }
        } catch (_: Throwable) {
        } finally {
            runCatching { retriever.release() }
        }

        return Track(
            id = "file_$stableId",
            mediaStoreId = stableId,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            uri = file.absolutePath,
            folderPath = folderPath,
            mediaType = mediaType,
            artworkUri = null,
            addedTimestampMs = file.lastModified()
        )
    }

    companion object {
        private const val TAG = "MediaLibraryScanner"

        val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "opus")
        val SUPPORTED_VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "mov", "3gp")
    }
}
