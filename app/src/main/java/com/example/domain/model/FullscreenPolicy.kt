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
    BOOKMARK
}

object FullscreenPolicy {

    /**
     * Req 3: lock to landscape only for landscape video. Portrait/square
     * video stays in the current orientation. A ratio of exactly 1.0 does
     * not lock.
     */
    fun shouldLockLandscape(nativeRatio: Float): Boolean = nativeRatio > 1.0f

    /**
     * Req 1: in Simple Mode only the easy controls stay visible — transport,
     * seek, fullscreen, PiP and picking a video. Speed, aspect, subtitles,
     * audio-track selection and bookmarks are Pro (Studio) controls.
     * In Studio Mode everything is visible.
     */
    fun isProControlVisible(isSimpleMode: Boolean, group: VideoControlGroup): Boolean {
        if (!isSimpleMode) return true
        return group == VideoControlGroup.TRANSPORT ||
            group == VideoControlGroup.SEEK ||
            group == VideoControlGroup.FULLSCREEN ||
            group == VideoControlGroup.PIP ||
            group == VideoControlGroup.PICK_VIDEO
    }
}
