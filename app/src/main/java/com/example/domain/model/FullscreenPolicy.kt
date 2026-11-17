package com.example.domain.model

/**
 * Pure, framework-free decisions for experience-tier gating (SIMPLE, STANDARD, PRO)
 * and video fullscreen orientation locking. Kept free of Android/Compose imports so
 * the rules are unit-testable without a device or emulator.
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

enum class MusicControlGroup {
    BASIC_TRANSPORT,
    SEEK_VOLUME,
    QUEUE_AND_MODES,
    LYRICS_VIEWER,
    TEN_BAND_EQ,
    STUDIO_DSP_RACK,
    LRC_TIMESTAMP_STUDIO,
    TIME_MACHINE_BOOKMARKS
}

object FullscreenPolicy {

    /**
     * Lock to landscape only for landscape video. Portrait/square
     * video stays in the current orientation. A ratio of exactly 1.0 does
     * not lock.
     */
    fun shouldLockLandscape(nativeRatio: Float): Boolean = nativeRatio > 1.0f

    /**
     * Legacy boolean overload for backward compatibility with existing tests.
     */
    fun isProControlVisible(isSimpleMode: Boolean, group: VideoControlGroup): Boolean {
        val tier = if (isSimpleMode) ExperienceTier.SIMPLE else ExperienceTier.PRO
        return isVideoControlVisible(tier, group)
    }

    /**
     * 3-Tier Video Control Policy:
     * - SIMPLE & STANDARD: Only basic transport, seek, fullscreen, PiP, and picking a video.
     * - PRO: All controls including speed, aspect ratio, subtitles, audio-track selection, and bookmarks.
     */
    fun isVideoControlVisible(tier: ExperienceTier, group: VideoControlGroup): Boolean {
        if (tier == ExperienceTier.PRO) return true
        return group == VideoControlGroup.TRANSPORT ||
            group == VideoControlGroup.SEEK ||
            group == VideoControlGroup.FULLSCREEN ||
            group == VideoControlGroup.PIP ||
            group == VideoControlGroup.PICK_VIDEO
    }

    /**
     * 3-Tier Music & Audio Control Policy:
     * - SIMPLE: Only BASIC_TRANSPORT and SEEK_VOLUME.
     * - STANDARD: Adds QUEUE_AND_MODES and LYRICS_VIEWER.
     * - PRO: Adds TEN_BAND_EQ, STUDIO_DSP_RACK, LRC_TIMESTAMP_STUDIO, and TIME_MACHINE_BOOKMARKS.
     */
    fun isMusicControlVisible(tier: ExperienceTier, group: MusicControlGroup): Boolean {
        return when (tier) {
            ExperienceTier.SIMPLE -> {
                group == MusicControlGroup.BASIC_TRANSPORT ||
                    group == MusicControlGroup.SEEK_VOLUME
            }
            ExperienceTier.STANDARD -> {
                group == MusicControlGroup.BASIC_TRANSPORT ||
                    group == MusicControlGroup.SEEK_VOLUME ||
                    group == MusicControlGroup.QUEUE_AND_MODES ||
                    group == MusicControlGroup.LYRICS_VIEWER
            }
            ExperienceTier.PRO -> true
        }
    }
}
