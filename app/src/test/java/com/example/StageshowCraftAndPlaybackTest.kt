package com.example

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.VeloraDatabase
import com.example.data.repository.VeloraLibraryRepository
import com.example.data.settings.VeloraSettingsRepository
import com.example.domain.engine.EnginePlaybackState
import com.example.domain.engine.MediaOrchestrator
import com.example.domain.engine.PlaybackStatus
import com.example.domain.engine.PlayerEngine
import com.example.domain.model.AppSettings
import com.example.domain.model.ExperienceTier
import com.example.domain.model.MediaType
import com.example.domain.model.Track
import com.example.domain.model.VideoAspectMode
import com.example.domain.model.VisualizerMode
import com.example.ui.components.VeloraAnimatedReveal
import com.example.ui.components.VeloraLoadingSkeleton
import com.example.ui.components.VeloraRetainedContentSwap
import com.example.ui.components.VeloraRollingLabel
import com.example.ui.screens.MusicScreen
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraDaylightColors
import com.example.ui.theme.VeloraHighContrastColors
import com.example.ui.theme.VeloraMotionTokens
import com.example.ui.theme.VeloraSaffronColors
import com.example.ui.theme.VeloraTheme
import com.example.ui.viewmodel.VeloraUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class StageshowCraftAndPlaybackTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makeTrack(
        id: String,
        title: String,
        album: String = "Obsidian Master",
        mediaType: MediaType = MediaType.AUDIO
    ) = Track(
        id = id,
        mediaStoreId = id.hashCode().toLong(),
        title = title,
        artist = "Velora Ensemble",
        album = album,
        durationMs = 210_000L,
        uri = "/storage/emulated/0/Media/$id.${if (mediaType == MediaType.VIDEO) "mp4" else "flac"}",
        folderPath = "/storage/emulated/0/Media",
        mediaType = mediaType
    )

    @Test
    fun `Stageshow motion curves, timings, Phi ratio, and surface-step ladder are properly tokenized`() {
        assertEquals(1.618034f, PhiTokens.PHI, 0.0001f)
        assertEquals(150, VeloraMotionTokens.FeedbackMs)
        assertEquals(220, VeloraMotionTokens.ContentSwapMs)
        assertEquals(55, VeloraMotionTokens.StaggerStepMs)
        assertEquals(340, VeloraMotionTokens.SheetSettleMs)

        // Verify two-curve easing endpoints & monotonic midpoints
        assertEquals(0f, VeloraMotionTokens.TravelEasing.transform(0f), 0.001f)
        assertEquals(1f, VeloraMotionTokens.TravelEasing.transform(1f), 0.001f)
        assertTrue(VeloraMotionTokens.ArrivalEasing.transform(0.5f) > 0.5f)
        assertTrue(VeloraMotionTokens.TouchResponseEasing.transform(0.5f) > 0.5f)

        // Verify surface-step depth ladder across all 3 canonical palettes
        assertNotEquals(VeloraSaffronColors.bg, VeloraSaffronColors.bgSecondary)
        assertNotEquals(VeloraSaffronColors.bgSecondary, VeloraSaffronColors.surfaceRaised)
        assertNotEquals(VeloraDaylightColors.bg, VeloraDaylightColors.surfaceWell)
        assertNotEquals(VeloraHighContrastColors.bg, VeloraHighContrastColors.surfaceRaised)
    }

    @Test
    fun `backgroundVideoAudioEnabled controls ON_STOP pause only for VIDEO without breaking background audio`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, VeloraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repository = VeloraLibraryRepository(
            context = context,
            trackDao = db.trackDao(),
            playlistDao = db.playlistDao(),
            eqPresetDao = db.eqPresetDao(),
            scanner = com.example.data.scanner.MediaLibraryScanner(context)
        )
        val settingsRepo = VeloraSettingsRepository(
            context = context,
            scope = scope,
            fileName = "bg_video_audio_${System.currentTimeMillis()}"
        )
        val fakeEngine = FakePlaybackEngine()
        val orchestrator = MediaOrchestrator(
            repository = repository,
            settingsRepository = settingsRepo,
            playerEngine = fakeEngine,
            dispatcher = Dispatchers.Unconfined
        )

        try {
            val audioTrack = makeTrack("a1", "Saffron Song", mediaType = MediaType.AUDIO)
            val videoTrack = makeTrack("v1", "Cinema Reel", mediaType = MediaType.VIDEO)

            // 1. Background music ALWAYS keeps playing even when backgroundVideoAudioEnabled = false
            fakeEngine.loadAndPlay(audioTrack, 0L)
            assertTrue(fakeEngine.playbackState.value.isPlaying)
            orchestrator.onAppBackgrounded(backgroundVideoAudioEnabled = false)
            assertTrue(
                "Background audio must never be paused by backgroundVideoAudioEnabled",
                fakeEngine.playbackState.value.isPlaying
            )

            // 2. Background video audio keeps playing when setting is enabled (default)
            fakeEngine.loadAndPlay(videoTrack, 0L)
            assertTrue(fakeEngine.playbackState.value.isPlaying)
            orchestrator.onAppBackgrounded(backgroundVideoAudioEnabled = true)
            assertTrue(
                "Video audio must continue in background when backgroundVideoAudioEnabled = true",
                fakeEngine.playbackState.value.isPlaying
            )

            // 3. Background video pauses when setting is disabled
            orchestrator.onAppBackgrounded(backgroundVideoAudioEnabled = false)
            assertFalse(
                "Video must pause on ON_STOP when backgroundVideoAudioEnabled = false",
                fakeEngine.playbackState.value.isPlaying
            )
            assertEquals(PlaybackStatus.PAUSED, fakeEngine.playbackState.value.status)
        } finally {
            orchestrator.close()
            scope.cancel()
            db.close()
        }
    }

    @Test
    fun `MusicScreen exposes album grouping filter chips and filters displayed tracks`() {
        val t1 = makeTrack("m1", "Obsidian Prelude", album = "Obsidian Sessions")
        val t2 = makeTrack("m2", "Ivory Nocturne", album = "Daylight Suite")

        composeRule.setContent {
            VeloraTheme {
                MusicScreen(
                    uiState = VeloraUiState(
                        settings = AppSettings(isSimpleMode = false, experienceTier = ExperienceTier.STANDARD),
                        audioTracks = listOf(t1, t2),
                        allTracks = listOf(t1, t2)
                    ),
                    onPlayAll = {},
                    onShuffleAll = {},
                    onPlayTrack = {},
                    onAddTrackToQueue = {},
                    onAddTrackToPlaylist = { _, _ -> }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_album_filter_row").assertIsDisplayed()
        composeRule.onNodeWithTag("music_album_chip_all").assertIsDisplayed()
        composeRule.onNodeWithTag("music_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("track_row_m1"))
        composeRule.onNodeWithTag("track_row_m1").assertIsDisplayed()
        composeRule.onNodeWithTag("music_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("track_row_m2"))
        composeRule.onNodeWithTag("track_row_m2").assertIsDisplayed()

        composeRule.onNodeWithTag("music_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("music_album_filter_row"))
        composeRule.onNodeWithTag("music_album_chip_${"Daylight Suite".hashCode()}").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("track_row_m2"))
        composeRule.onNodeWithTag("track_row_m2").assertIsDisplayed()
        composeRule.onNodeWithTag("track_row_m1").assertDoesNotExist()
    }

    @Test
    fun `Stageshow Compose choreography primitives transition smoothly and keep tags intact`() {
        var tabState by mutableStateOf("A")
        var revealed by mutableStateOf(true)
        var rollingTitle by mutableStateOf("Track One")

        composeRule.setContent {
            VeloraTheme {
                VeloraRetainedContentSwap(targetState = tabState) { active ->
                    Text(
                        text = "Pane $active",
                        modifier = Modifier.testTag("choreo_pane_$active")
                    )
                }
                VeloraAnimatedReveal(visible = revealed) {
                    Text(
                        text = "HUD Active",
                        modifier = Modifier.testTag("choreo_hud")
                    )
                }
                VeloraRollingLabel(
                    text = rollingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("choreo_rolling")
                )
                VeloraLoadingSkeleton(rowCount = 2, reducedMotion = true)
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("choreo_pane_A").assertIsDisplayed()
        composeRule.onNodeWithTag("choreo_hud").assertIsDisplayed()
        composeRule.onNodeWithTag("choreo_rolling").assertIsDisplayed()
        composeRule.onNodeWithTag("velora_loading_skeleton").assertIsDisplayed()

        composeRule.runOnIdle {
            tabState = "B"
            revealed = false
            rollingTitle = "Track Two"
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("choreo_pane_B").assertIsDisplayed()
        composeRule.onNodeWithTag("choreo_hud").assertDoesNotExist()
    }
}

private class FakePlaybackEngine : PlayerEngine {
    private val _state = MutableStateFlow(EnginePlaybackState(engineAvailable = true))
    override val playbackState: StateFlow<EnginePlaybackState> = _state.asStateFlow()

    override fun loadAndPlay(track: Track, startPositionMs: Long) {
        _state.update {
            it.copy(
                currentTrack = track,
                status = PlaybackStatus.PLAYING,
                positionMs = startPositionMs,
                durationMs = track.durationMs
            )
        }
    }

    override fun play() {
        _state.update { it.copy(status = PlaybackStatus.PLAYING) }
    }

    override fun pause() {
        _state.update { it.copy(status = PlaybackStatus.PAUSED) }
    }

    override fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    override fun stop() {
        _state.update { it.copy(status = PlaybackStatus.IDLE, positionMs = 0L) }
    }

    override fun seekTo(positionMs: Long) {
        _state.update { it.copy(positionMs = positionMs) }
    }

    override fun setVolume(volume: Float) {
        _state.update { it.copy(volume = volume) }
    }

    override fun setMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
    }

    override fun toggleMute() {
        setMuted(!_state.value.isMuted)
    }

    override fun setPlaybackSpeed(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
    }

    override fun setEqEnabled(enabled: Boolean) {
        _state.update { it.copy(eqEnabled = enabled) }
    }

    override fun setEqBandGains(gainsDb: List<Float>) {
        _state.update { it.copy(eqBandGainsDb = gainsDb) }
    }

    override fun setVisualizerMode(mode: VisualizerMode) {
        _state.update { it.copy(visualizerMode = mode) }
    }

    override fun setSubtitlesEnabled(enabled: Boolean) {
        _state.update { it.copy(subtitlesEnabled = enabled) }
    }

    override fun selectSubtitleTrack(trackId: String?) = Unit
    override fun selectAudioTrack(trackId: String) = Unit

    override fun setVideoAspectMode(mode: VideoAspectMode) {
        _state.update { it.copy(videoAspectMode = mode) }
    }

    override fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun setOnTrackEndedListener(listener: () -> Unit) = Unit
    override fun release() = Unit
}
