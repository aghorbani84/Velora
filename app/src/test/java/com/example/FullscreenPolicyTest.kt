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
        assertTrue(visible.contains(VideoControlGroup.PIP))
        assertTrue(visible.contains(VideoControlGroup.PICK_VIDEO))
        assertFalse(visible.contains(VideoControlGroup.SPEED))
        assertFalse(visible.contains(VideoControlGroup.ASPECT))
        assertFalse(visible.contains(VideoControlGroup.SUBTITLES))
        assertFalse(visible.contains(VideoControlGroup.AUDIO_TRACK))
        assertFalse(visible.contains(VideoControlGroup.BOOKMARK))
    }

    @Test
    fun `studio mode shows every video control`() {
        for (group in VideoControlGroup.entries) {
            assertTrue(FullscreenPolicy.isProControlVisible(false, group))
        }
    }
}
