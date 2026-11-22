package com.example.ui.components

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
 * Buffered position of the underlying player in ms (0 when unknown). Read through the
 * same platform adapter boundary as [VeloraVideoSurface], so screens never touch ExoPlayer.
 */
@OptIn(UnstableApi::class)
fun PlayerEngine?.bufferedPositionMs(): Long =
    (this as? Media3PlayerEngine)?.exoPlayer?.bufferedPosition?.coerceAtLeast(0L) ?: 0L

/**
 * Shared cinema gesture modifier for Now Playing, inline Video, and Fullscreen Video surfaces (F16).
 * Handles tap/double-tap seek, vertical brightness/volume swipe, and horizontal timeline drag scrub.
 */
@Composable
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
    onHudNotice: (String) -> Unit,
    // Off for the embedded video player: it sits in a scrollable list and vertical
    // swipes would fight list scrolling. Fullscreen and Now Playing keep them.
    verticalSwipesEnabled: Boolean = true
): Modifier {
    val lockedState by rememberUpdatedState(controlsLocked)
    val positionState by rememberUpdatedState(positionMs)
    val durationState by rememberUpdatedState(durationMs)
    val brightnessState by rememberUpdatedState(currentBrightness)
    val volumeState by rememberUpdatedState(currentVolume)
    val toggleControlsState by rememberUpdatedState(onToggleControls)
    val interactionState by rememberUpdatedState(onInteraction)
    val seekToState by rememberUpdatedState(onSeekTo)
    val togglePlayPauseState by rememberUpdatedState(onTogglePlayPause)
    val setBrightnessState by rememberUpdatedState(onSetBrightness)
    val setVolumeState by rememberUpdatedState(onSetVolume)
    val hudNoticeState by rememberUpdatedState(onHudNotice)
    val verticalSwipesState by rememberUpdatedState(verticalSwipesEnabled)

    return this
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { offset ->
                    if (!lockedState) {
                        interactionState()
                        val res = VeloraGestureEngine.resolveDoubleTapGesture(
                            tapX = offset.x,
                            surfaceWidth = size.width.toFloat(),
                            currentPositionMs = positionState,
                            durationMs = durationState
                        )
                        when (res.action) {
                            GestureActionType.SEEK_BACKWARD,
                            GestureActionType.SEEK_FORWARD -> {
                                seekToState(res.targetPositionMs)
                                hudNoticeState(res.hudLabel)
                            }
                            else -> {
                                togglePlayPauseState()
                                hudNoticeState(res.hudLabel)
                            }
                        }
                    }
                },
                onTap = {
                    toggleControlsState()
                    interactionState()
                }
            )
        }
        .pointerInput(Unit) {
            var dragStartX = 0f
            detectVerticalDragGestures(
                onDragStart = { offset -> dragStartX = offset.x },
                onVerticalDrag = { change, dragAmount ->
                    if (!lockedState && verticalSwipesState) {
                        change.consume()
                        val swipe = VeloraGestureEngine.resolveVerticalSwipeGesture(
                            startX = dragStartX,
                            surfaceWidth = size.width.toFloat(),
                            surfaceHeight = size.height.toFloat(),
                            dragDeltaY = dragAmount,
                            currentBrightness = brightnessState,
                            currentVolume = volumeState
                        )
                        if (swipe.action == GestureActionType.ADJUST_BRIGHTNESS) {
                            setBrightnessState(swipe.newValue)
                        } else {
                            setVolumeState(swipe.newValue)
                        }
                        hudNoticeState(swipe.hudLabel)
                    }
                }
            )
        }
        .pointerInput(Unit) {
            var scrubAnchorMs = 0L
            var totalDragX = 0f
            var pendingTargetMs: Long? = null
            detectHorizontalDragGestures(
                onDragStart = {
                    if (!lockedState) {
                        interactionState()
                        scrubAnchorMs = positionState
                        totalDragX = 0f
                        pendingTargetMs = null
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (!lockedState && durationState > 0L) {
                        change.consume()
                        totalDragX += dragAmount
                        val scrub = VeloraGestureEngine.resolveHorizontalScrubGesture(
                            dragDeltaX = totalDragX,
                            surfaceWidth = size.width.toFloat(),
                            currentPositionMs = scrubAnchorMs,
                            durationMs = durationState
                        )
                        pendingTargetMs = scrub.targetPositionMs
                        hudNoticeState(scrub.hudLabel)
                    }
                },
                onDragEnd = {
                    val target = pendingTargetMs
                    if (!lockedState && target != null) {
                        seekToState(target)
                    }
                    totalDragX = 0f
                    pendingTargetMs = null
                },
                onDragCancel = {
                    totalDragX = 0f
                    pendingTargetMs = null
                }
            )
        }
}
