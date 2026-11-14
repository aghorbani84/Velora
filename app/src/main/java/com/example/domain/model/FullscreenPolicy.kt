package com.example.domain.model

/**
 * Pure, framework-free decisions for the video player — Req 1 (Simple Mode)
 * and Req 3 (immersive fullscreen). Kept free of Android/Compose imports so
 * the rules are unit-testable without a device or emulator.
 *
 * Verified against the code (2026-11-13, HEAD 74d036a):
 * - Simple Mode already exists in the shell + music player (default ON) but
 *   the video screen ignored it entirely: every pro control was always shown.
 * - Fullscreen existed only as a Dialog: no system-bar hiding, no
 *   orientation handling anywhere in the app.
 *
 * UX pass (2026-11-13, feature/video-player-ux): the overlay was narrowed
 * further. PiP used to count as Simple-visible here, but the screen never
 * actually showed it in Simple Mode, and the redesign puts PiP, lock,
 * speed, aspect, subtitles, audio-track and bookmarks behind ONE "more"
 * bottom sheet that exists only in Studio mode. So the overlay now shows
 * exactly transport + seek + fullscreen in both modes; the sheet is the
 * single home for everything else (Studio only).
 */
enum class VideoControlGroup {
    TRANSPORT,
    SEEK,
    FULLSCREEN,
    PIP,
    PICK_VIDEO,
    SPEED,
    ASPECT,
    SUBTITLES,
    AUDIO_TRACK,
    BOOKMARK,
    LOCK
}

object FullscreenPolicy {

    /**
     * Req 3: lock to landscape only for landscape video. Portrait/square
     * video stays in the current orientation. A ratio of exactly 1.0 does
     * not lock.
     */
    fun shouldLockLandscape(nativeRatio: Float): Boolean = nativeRatio > 1.0f

    /**
     * Directly visible on the player overlay (embedded and fullscreen).
     * Identical in Simple and Studio mode: transport, seek, fullscreen.
     * Everything else lives behind the "more" sheet (Studio) or is hidden
     * (Simple). PICK_VIDEO is not an overlay control at all: it is a
     * library action below the player, shown in Studio mode only.
     */
    @Suppress("UnusedParameter") // Intentional: overlay visibility is mode-independent by design (see KDoc); kept for symmetry with isInMoreSheet.
    fun isProControlVisible(isSimpleMode: Boolean, group: VideoControlGroup): Boolean {
        return group == VideoControlGroup.TRANSPORT ||
            group == VideoControlGroup.SEEK ||
            group == VideoControlGroup.FULLSCREEN
    }

    /**
     * Lives behind the single "more" bottom sheet. The sheet exists only in
     * Studio mode; in Simple Mode these controls are hidden entirely —
     * nothing is removed, only organized.
     */
    fun isInMoreSheet(isSimpleMode: Boolean, group: VideoControlGroup): Boolean {
        if (isSimpleMode) return false
        return group == VideoControlGroup.PIP ||
            group == VideoControlGroup.LOCK ||
            group == VideoControlGroup.SPEED ||
            group == VideoControlGroup.ASPECT ||
            group == VideoControlGroup.SUBTITLES ||
            group == VideoControlGroup.AUDIO_TRACK ||
            group == VideoControlGroup.BOOKMARK
    }
}
