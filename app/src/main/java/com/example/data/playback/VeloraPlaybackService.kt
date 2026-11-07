package com.example.data.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.domain.engine.PlayerEngine
import org.koin.android.ext.android.inject

class VeloraPlaybackService : MediaSessionService() {

    private val playerEngine: PlayerEngine by inject()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        runCatching {
            val media3Engine = playerEngine as? Media3PlayerEngine
            val exoPlayer = media3Engine?.exoPlayer ?: return

            mediaSession?.release()
            val activityIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val session = MediaSession.Builder(this, exoPlayer)
                .setId("velora_media_session_${System.currentTimeMillis()}")
                .setSessionActivity(pendingIntent)
                .build()
            mediaSession = session
            addSession(session)
        }.onFailure { err ->
            Log.w("VeloraPlaybackService", "MediaSession initialization skipped: ${err.message}")
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

    override fun onDestroy() {
        runCatching {
            mediaSession?.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
