package com.example

import com.example.domain.model.ExperienceTier
import com.example.domain.model.FullscreenPolicy
import com.example.domain.model.MusicControlGroup
import com.example.domain.model.VideoControlGroup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-policy tests for the 3-tier UI (`SIMPLE`, `STANDARD`, `PRO`) and
 * video fullscreen orientation locking.
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

    @Test
    fun `three-tier music policy hides all pro and queue controls in SIMPLE tier`() {
        assertTrue(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.BASIC_TRANSPORT))
        assertTrue(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.SEEK_VOLUME))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.QUEUE_AND_MODES))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.LYRICS_VIEWER))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.TEN_BAND_EQ))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.STUDIO_DSP_RACK))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.LRC_TIMESTAMP_STUDIO))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.SIMPLE, MusicControlGroup.TIME_MACHINE_BOOKMARKS))
    }

    @Test
    fun `three-tier music policy enables everyday queue and lyrics in STANDARD tier while keeping EQ and DSP exclusive to PRO`() {
        assertTrue(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.BASIC_TRANSPORT))
        assertTrue(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.SEEK_VOLUME))
        assertTrue(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.QUEUE_AND_MODES))
        assertTrue(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.LYRICS_VIEWER))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.TEN_BAND_EQ))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.STUDIO_DSP_RACK))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.LRC_TIMESTAMP_STUDIO))
        assertFalse(FullscreenPolicy.isMusicControlVisible(ExperienceTier.STANDARD, MusicControlGroup.TIME_MACHINE_BOOKMARKS))

        for (group in MusicControlGroup.entries) {
            assertTrue(FullscreenPolicy.isMusicControlVisible(ExperienceTier.PRO, group))
        }
    }

    @Test
    fun `SIMPLE tier shows exactly 4 navigation destinations with MUSIC as home while STANDARD and PRO show all 8`() {
        val simpleVisible = com.example.ui.navigation.VeloraDestination.visibleForTier(ExperienceTier.SIMPLE)
        org.junit.Assert.assertEquals(
            listOf(
                com.example.ui.navigation.VeloraDestination.MUSIC,
                com.example.ui.navigation.VeloraDestination.VIDEO,
                com.example.ui.navigation.VeloraDestination.PLAYER,
                com.example.ui.navigation.VeloraDestination.SETTINGS
            ),
            simpleVisible
        )
        org.junit.Assert.assertEquals(4, simpleVisible.size)
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.LIBRARY))
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.ARTISTS))
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.RADIO))
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.SEARCH))
        org.junit.Assert.assertEquals(
            com.example.ui.navigation.VeloraDestination.MUSIC,
            com.example.ui.navigation.VeloraDestination.homeForTier(ExperienceTier.SIMPLE)
        )
        org.junit.Assert.assertEquals(
            8,
            com.example.ui.navigation.VeloraDestination.visibleForTier(ExperienceTier.STANDARD).size
        )
        org.junit.Assert.assertEquals(
            8,
            com.example.ui.navigation.VeloraDestination.visibleForTier(ExperienceTier.PRO).size
        )
    }
}
