package com.example.data.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.PhiDesignMetrics
import com.example.domain.model.VeloraCompatibilityMatrix
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.koin.android.ext.android.inject

class VeloraPlaybackService : MediaSessionService() {

    private val playerEngine: PlayerEngine by inject()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        runCatching {
            val media3Engine = playerEngine as? Media3PlayerEngine
            val exoPlayer = media3Engine?.exoPlayer
            if (exoPlayer == null) {
                Log.w(TAG, "ExoPlayer instance unavailable; stopping VeloraPlaybackService")
                stopSelf()
                return
            }

            mediaSession?.release()
            val activityIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val session = MediaSession.Builder(this, exoPlayer)
                .setId(SESSION_ID)
                .setSessionActivity(pendingIntent)
                .setCallback(
                    object : MediaSession.Callback {
                        override fun onAddMediaItems(
                            mediaSession: MediaSession,
                            controller: MediaSession.ControllerInfo,
                            mediaItems: MutableList<MediaItem>
                        ): ListenableFuture<MutableList<MediaItem>> {
                            val validated = mutableListOf<MediaItem>()
                            for (item in mediaItems) {
                                val rawUri = (
                                    item.localConfiguration?.uri
                                        ?: item.requestMetadata.mediaUri
                                    )?.toString()?.trim().orEmpty()
                                if (!isAllowedSessionMediaUri(rawUri)) {
                                    Log.w(TAG, "Rejected unauthorized MediaSession item URI: $rawUri")
                                    return Futures.immediateFailedFuture(
                                        SecurityException("Rejected unauthorized media URI")
                                    )
                                }
                                validated.add(item)
                            }
                            return Futures.immediateFuture(validated)
                        }
                    }
                )
                .build()
            mediaSession = session
            addSession(session)
        }.onFailure { err ->
            Log.w(TAG, "MediaSession initialization skipped: ${err.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return runCatching {
            super.onStartCommand(intent, flags, startId)
        }.getOrDefault(START_NOT_STICKY)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching {
            mediaSession?.player?.pause()
            mediaSession?.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "VeloraPlaybackService"
        const val SESSION_ID = "velora_media_session"

        fun isAllowedSessionMediaUri(rawUri: String): Boolean {
            val trimmed = rawUri.trim()
            if (trimmed.isEmpty()) return false
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("http://") || lower.startsWith("https://") ->
                    VeloraCompatibilityMatrix.isValidAuthorizedStreamUrl(trimmed)
                lower.startsWith("content://") ->
                    !trimmed.contains("..")
                lower.startsWith("file://") ->
                    PhiDesignMetrics.isSafeMediaFolder(trimmed.removePrefix("file://").substringBeforeLast('/'))
                trimmed.startsWith("/") ->
                    PhiDesignMetrics.isSafeMediaFolder(trimmed.substringBeforeLast('/'))
                else -> false
            }
        }
    }
}
