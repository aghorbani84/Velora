package com.example.ui.components

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.playback.Media3PlayerEngine
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.GestureActionType
import com.example.domain.model.VeloraGestureEngine
import com.example.domain.model.VideoAspectMode

/**
 * Platform-specific video rendering surface boundary (`androidMain` implementation).
 * Keeps `ExoPlayer` and `PlayerView` encapsulated inside a single UI adapter component
 * so screen composables depend only on the multiplatform-ready [PlayerEngine] contract (F10, F16).
 */
@OptIn(UnstableApi::class)
@Composable
fun VeloraVideoSurface(
    playerEngine: PlayerEngine?,
    videoAspectMode: VideoAspectMode,
    surfaceTestTag: String,
    modifier: Modifier = Modifier
) {
    val exoPlayer = (playerEngine as? Media3PlayerEngine)?.exoPlayer ?: return
    AndroidView(
        factory = { ctx ->
            val playerView = LayoutInflater.from(ctx).inflate(
                R.layout.velora_video_player_view,
                null,
                false
            ) as PlayerView
            playerView.apply {
                setEnableComposeSurfaceSyncWorkaround(true)
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                useController = false
                keepScreenOn = true
                player = exoPlayer
                onResume()
            }
        },
        update = { view ->
            if (view.player !== exoPlayer) {
                view.player = exoPlayer
            }
            view.onResume()
            view.resizeMode = when (videoAspectMode) {
                VideoAspectMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoAspectMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                VideoAspectMode.GOLDEN_PHI -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                VideoAspectMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        onRelease = { view -> view.player = null },
        modifier = modifier.testTag(surfaceTestTag)
    )
}

/**
 * Shared cinema gesture modifier for both inline and fullscreen video surfaces (F16).
 */
fun Modifier.veloraCinemaGestures(
    controlsLocked: Boolean,
    positionMs: Long,
    durationMs: Long,
    currentBrightness: Float,
    currentVolume: Float,
    onToggleControls: () -> Unit,
    onInteraction: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSetBrightness: (Float) -> Unit,
    onSetVolume: (Float) -> Unit,
    onHudNotice: (String) -> Unit
): Modifier = this
    .pointerInput(controlsLocked, positionMs, durationMs) {
        detectTapGestures(
            onDoubleTap = { offset ->
                if (!controlsLocked) {
                    onInteraction()
                    val res = VeloraGestureEngine.resolveDoubleTapGesture(
                        tapX = offset.x,
                        surfaceWidth = size.width.toFloat(),
                        currentPositionMs = positionMs,
                        durationMs = durationMs
                    )
                    when (res.action) {
                        GestureActionType.SEEK_BACKWARD,
                        GestureActionType.SEEK_FORWARD -> {
                            onSeekTo(res.targetPositionMs)
                            onHudNotice(res.hudLabel)
                        }
                        else -> {
                            onTogglePlayPause()
                            onHudNotice(res.hudLabel)
                        }
                    }
                }
            },
            onTap = {
                onToggleControls()
                onInteraction()
            }
        )
    }
    .pointerInput(controlsLocked, currentBrightness, currentVolume) {
        var dragStartX = 0f
        detectVerticalDragGestures(
            onDragStart = { offset -> dragStartX = offset.x },
            onVerticalDrag = { change, dragAmount ->
                if (!controlsLocked) {
                    change.consume()
                    val swipe = VeloraGestureEngine.resolveVerticalSwipeGesture(
                        startX = dragStartX,
                        surfaceWidth = size.width.toFloat(),
                        surfaceHeight = size.height.toFloat(),
                        dragDeltaY = dragAmount,
                        currentBrightness = currentBrightness,
                        currentVolume = currentVolume
                    )
                    if (swipe.action == GestureActionType.ADJUST_BRIGHTNESS) {
                        onSetBrightness(swipe.newValue)
                    } else {
                        onSetVolume(swipe.newValue)
                    }
                    onHudNotice(swipe.hudLabel)
                }
            }
        )
    }
