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

    @Test
    fun `every tier shows exactly transport seek and fullscreen on the overlay`() {
        for (tier in ExperienceTier.entries) {
            val direct = VideoControlGroup.entries.filter {
                FullscreenPolicy.isVideoControlVisible(tier, it)
            }
            org.junit.Assert.assertEquals(
                setOf(VideoControlGroup.TRANSPORT, VideoControlGroup.SEEK, VideoControlGroup.FULLSCREEN),
                direct.toSet()
            )
        }
    }

    @Test
    fun `more sheet exists only in PRO tier`() {
        for (tier in listOf(ExperienceTier.SIMPLE, ExperienceTier.STANDARD)) {
            for (group in VideoControlGroup.entries) {
                assertFalse(FullscreenPolicy.isInMoreSheet(tier, group))
            }
            assertFalse(FullscreenPolicy.isPickVideoVisible(tier))
        }
        val proSheet = VideoControlGroup.entries.filter {
            FullscreenPolicy.isInMoreSheet(ExperienceTier.PRO, it)
        }.toSet()
        org.junit.Assert.assertEquals(
            setOf(
                VideoControlGroup.PIP,
                VideoControlGroup.LOCK,
                VideoControlGroup.SPEED,
                VideoControlGroup.ASPECT,
                VideoControlGroup.SUBTITLES,
                VideoControlGroup.AUDIO_TRACK,
                VideoControlGroup.BOOKMARK
            ),
            proSheet
        )
        assertTrue(FullscreenPolicy.isPickVideoVisible(ExperienceTier.PRO))
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
    fun `SIMPLE tier shows exactly 4 navigation destinations with MUSIC as home while STANDARD and PRO use 5 mobile tabs with all 9 routes reachable`() {
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
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.HOME))
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.LIBRARY))
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.ARTISTS))
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.RADIO))
        assertFalse(simpleVisible.contains(com.example.ui.navigation.VeloraDestination.SEARCH))
        org.junit.Assert.assertEquals(
            com.example.ui.navigation.VeloraDestination.MUSIC,
            com.example.ui.navigation.VeloraDestination.homeForTier(ExperienceTier.SIMPLE)
        )

        val standardTabs = com.example.ui.navigation.VeloraDestination.visibleForTier(ExperienceTier.STANDARD)
        val proTabs = com.example.ui.navigation.VeloraDestination.visibleForTier(ExperienceTier.PRO)
        org.junit.Assert.assertEquals(
            listOf(
                com.example.ui.navigation.VeloraDestination.HOME,
                com.example.ui.navigation.VeloraDestination.SEARCH,
                com.example.ui.navigation.VeloraDestination.LIBRARY,
                com.example.ui.navigation.VeloraDestination.VIDEO,
                com.example.ui.navigation.VeloraDestination.SETTINGS
            ),
            standardTabs
        )
        org.junit.Assert.assertEquals(5, standardTabs.size)
        org.junit.Assert.assertEquals(5, proTabs.size)
        org.junit.Assert.assertEquals(
            com.example.ui.navigation.VeloraDestination.HOME,
            com.example.ui.navigation.VeloraDestination.homeForTier(ExperienceTier.STANDARD)
        )
        org.junit.Assert.assertEquals(
            9,
            com.example.ui.navigation.VeloraDestination.allReachableForTier(ExperienceTier.STANDARD).size
        )
        org.junit.Assert.assertEquals(
            9,
            com.example.ui.navigation.VeloraDestination.allReachableForTier(ExperienceTier.PRO).size
        )
    }
}
