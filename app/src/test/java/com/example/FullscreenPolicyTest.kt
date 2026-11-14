package com.example

import com.example.domain.model.FullscreenPolicy
import com.example.domain.model.VideoControlGroup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Req 1 + Req 3: pure-policy tests for the video player. No device needed.
 * Failing-first: written against the verified gaps (video screen ignored
 * Simple Mode; fullscreen had no immersive/orientation behaviour).
 */
class FullscreenPolicyTest {

    @Test
    fun `landscape video locks to landscape`() {
        assertTrue(FullscreenPolicy.shouldLockLandscape(16f / 9f))
        assertTrue(FullscreenPolicy.shouldLockLandscape(2.39f))
    }

    @Test
    fun `portrait and square video do not lock orientation`() {
        assertFalse(FullscreenPolicy.shouldLockLandscape(9f / 16f))
        assertFalse(FullscreenPolicy.shouldLockLandscape(1.0f))
        assertFalse(FullscreenPolicy.shouldLockLandscape(0.5f))
    }

    @Test
    fun `simple mode keeps only easy video controls visible`() {
        val visible = VideoControlGroup.entries.filter {
            FullscreenPolicy.isProControlVisible(true, it)
        }
        assertTrue(visible.contains(VideoControlGroup.TRANSPORT))
        assertTrue(visible.contains(VideoControlGroup.SEEK))
        assertTrue(visible.contains(VideoControlGroup.FULLSCREEN))
        // UX pass 2026-11-13: PiP moved behind the "more" sheet (Studio
        // only). It was listed as simple-visible before, but the screen
        // never actually showed it in Simple Mode.
        assertFalse(visible.contains(VideoControlGroup.PIP))
        assertFalse(visible.contains(VideoControlGroup.SPEED))
        assertFalse(visible.contains(VideoControlGroup.ASPECT))
        assertFalse(visible.contains(VideoControlGroup.SUBTITLES))
        assertFalse(visible.contains(VideoControlGroup.AUDIO_TRACK))
        assertFalse(visible.contains(VideoControlGroup.BOOKMARK))
        assertFalse(visible.contains(VideoControlGroup.LOCK))
        assertFalse(visible.contains(VideoControlGroup.PICK_VIDEO))
    }

    @Test
    fun `simple mode has no more sheet`() {
        for (group in VideoControlGroup.entries) {
            assertFalse(FullscreenPolicy.isInMoreSheet(true, group))
        }
    }

    @Test
    fun `studio mode shows every video control`() {
        // Directly on the overlay: transport, seek, fullscreen.
        val direct = VideoControlGroup.entries.filter {
            FullscreenPolicy.isProControlVisible(false, it)
        }
        assertTrue(direct.contains(VideoControlGroup.TRANSPORT))
        assertTrue(direct.contains(VideoControlGroup.SEEK))
        assertTrue(direct.contains(VideoControlGroup.FULLSCREEN))
        assertFalse(direct.contains(VideoControlGroup.PIP))
        assertFalse(direct.contains(VideoControlGroup.SPEED))
        assertFalse(direct.contains(VideoControlGroup.ASPECT))
        assertFalse(direct.contains(VideoControlGroup.SUBTITLES))
        assertFalse(direct.contains(VideoControlGroup.AUDIO_TRACK))
        assertFalse(direct.contains(VideoControlGroup.BOOKMARK))
        assertFalse(direct.contains(VideoControlGroup.LOCK))
        assertFalse(direct.contains(VideoControlGroup.PICK_VIDEO))

        // Behind the single "more" sheet: everything else except the
        // below-player library action PICK_VIDEO.
        val sheet = VideoControlGroup.entries.filter {
            FullscreenPolicy.isInMoreSheet(false, it)
        }
        assertTrue(sheet.contains(VideoControlGroup.PIP))
        assertTrue(sheet.contains(VideoControlGroup.LOCK))
        assertTrue(sheet.contains(VideoControlGroup.SPEED))
        assertTrue(sheet.contains(VideoControlGroup.ASPECT))
        assertTrue(sheet.contains(VideoControlGroup.SUBTITLES))
        assertTrue(sheet.contains(VideoControlGroup.AUDIO_TRACK))
        assertTrue(sheet.contains(VideoControlGroup.BOOKMARK))
        assertFalse(sheet.contains(VideoControlGroup.TRANSPORT))
        assertFalse(sheet.contains(VideoControlGroup.SEEK))
        assertFalse(sheet.contains(VideoControlGroup.FULLSCREEN))
        assertFalse(sheet.contains(VideoControlGroup.PICK_VIDEO))
    }
}
