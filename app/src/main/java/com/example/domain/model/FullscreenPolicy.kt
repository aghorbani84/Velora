package com.example.domain.model

/**
 * Pure, framework-free decisions for experience-tier gating (SIMPLE, STANDARD, PRO)
 * and video fullscreen orientation locking. Kept free of Android/Compose imports so
 * the rules are unit-testable without a device or emulator.
 *
 * Video player UX (ported from PR #2 onto the 3-tier system): the overlay shows
 * exactly transport + seek + fullscreen in every tier. PiP, lock, speed, aspect,
 * subtitles, audio-track and bookmarks live behind ONE "more" bottom sheet that
 * exists only in the PRO tier; SIMPLE and STANDARD hide those controls.
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
     * Legacy boolean overload kept for existing tests: `true` maps to SIMPLE,
     * `false` to PRO.
     */
    fun isProControlVisible(isSimpleMode: Boolean, group: VideoControlGroup): Boolean =
        isVideoControlVisible(tierOf(isSimpleMode), group)

    /**
     * Directly visible on the player overlay (embedded and fullscreen).
     * Identical in every tier: transport, seek, fullscreen. Everything else
     * lives behind the "more" sheet (PRO) or is hidden (SIMPLE, STANDARD).
     * PICK_VIDEO is not an overlay control: it is a library action below the
     * player, shown in the PRO tier only.
     */
    @Suppress("UnusedParameter") // Intentional: overlay visibility is tier-independent by design; kept for symmetry with isInMoreSheet.
    fun isVideoControlVisible(tier: ExperienceTier, group: VideoControlGroup): Boolean {
        return group == VideoControlGroup.TRANSPORT ||
            group == VideoControlGroup.SEEK ||
            group == VideoControlGroup.FULLSCREEN
    }

    /** Legacy boolean overload kept for existing tests. */
    fun isInMoreSheet(isSimpleMode: Boolean, group: VideoControlGroup): Boolean =
        isInMoreSheet(tierOf(isSimpleMode), group)

    /**
     * Lives behind the single "more" bottom sheet. The sheet exists only in
     * the PRO tier; in SIMPLE and STANDARD these controls are hidden entirely.
     */
    fun isInMoreSheet(tier: ExperienceTier, group: VideoControlGroup): Boolean {
        if (tier != ExperienceTier.PRO) return false
        return group == VideoControlGroup.PIP ||
            group == VideoControlGroup.LOCK ||
            group == VideoControlGroup.SPEED ||
            group == VideoControlGroup.ASPECT ||
            group == VideoControlGroup.SUBTITLES ||
            group == VideoControlGroup.AUDIO_TRACK ||
            group == VideoControlGroup.BOOKMARK
    }

    /** True when the PRO-only "pick a video" library action is shown below the player. */
    fun isPickVideoVisible(tier: ExperienceTier): Boolean = tier == ExperienceTier.PRO

    private fun tierOf(isSimpleMode: Boolean): ExperienceTier =
        if (isSimpleMode) ExperienceTier.SIMPLE else ExperienceTier.PRO

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
