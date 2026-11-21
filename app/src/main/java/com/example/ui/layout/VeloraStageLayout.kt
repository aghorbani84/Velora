package com.example.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PhiTokens

/**
 * Rule R-S3 - the Now Playing vertical split - as one testable function.
 *
 * The rule is stated in DESIGN-SYSTEM.md (branch feature/ui-rebuild-phase0-1)
 * lines 352-358:
 *
 * > the stage (cover art / spectrum) takes **up to 61.8%** of the available
 * > height; the controls + queue area takes the remainder with a **minimum of
 * > 317dp**; when the remainder is smaller than 317dp the stage shrinks below
 * > 61.8% (**never the controls**)
 *
 * Production currently enforces only the width half of that sentence:
 * `NowPlayingScreen.kt:411-416` caps the stage at
 * `maxWidth * ARTWORK_STAGE_MAX_FRACTION + SpectrumHeight`, so no height is
 * ever reserved for the controls and the stage cannot shrink when the window
 * is short. The consequence was measured, not guessed - CI run #77 of PR #4:
 * `assertIsDisplayed()` failed on `transport_play_pause_button`
 * (SimpleModeCompositionTest.kt:326) and on `transport_shuffle_button`
 * (`:371`), i.e. `VeloraSeekBar` (NowPlayingScreen.kt:524) and
 * `VeloraTransportControls` (`:532`) had been laid out below the window.
 *
 * This object holds the number and the arithmetic in one place. It is not yet
 * wired into `NowPlayingStageCard`: doing so needs the available height from
 * `LocalWindowInfo` (a `BoxWithConstraints` inside a `LazyColumn` item sees
 * infinite height), and that edit ships together with a display-level
 * assertion on a small window. See issue #5.
 */
internal object VeloraStageLayout {

    /**
     * R-S3 floor for the controls + queue area, including the §5.1 step 3
     * allow-list (play/pause, prev, next, seek) and one visible queue row.
     * Documented derivation (DESIGN-SYSTEM.md lines 344-353): 237dp of
     * controls + 24dp queue header + 56dp queue row = 317dp.
     */
    val CONTROLS_AREA_MIN_HEIGHT: Dp = 317.dp

    /**
     * Maximum height of the stage block (artwork row + spectrum strip) inside
     * a window that offers [availableHeight], when the row is [maxWidth] wide.
     *
     * Two independent limits compete and the tighter one wins:
     * - the golden-ratio fraction of the width;
     * - whatever is left after [CONTROLS_AREA_MIN_HEIGHT] and the fixed
     *   [PhiTokens.SpectrumHeight] strip are taken.
     *
     * When the second one goes negative the artwork row collapses to zero and
     * the block keeps only the spectrum strip, so the controls never lose
     * height to the stage.
     */
    fun stageBlockHeightCap(maxWidth: Dp, availableHeight: Dp): Dp {
        val artworkByWidth = maxWidth * PhiTokens.ARTWORK_STAGE_MAX_FRACTION
        val artworkByRemainder =
            availableHeight - CONTROLS_AREA_MIN_HEIGHT - PhiTokens.SpectrumHeight
        val artwork = minOf(artworkByWidth, artworkByRemainder).coerceAtLeast(0.dp)
        return artwork + PhiTokens.SpectrumHeight
    }

    /**
     * Height left for controls + queue. Equals or exceeds
     * [CONTROLS_AREA_MIN_HEIGHT] whenever the window itself is tall enough;
     * on a window shorter than the floor it reports the real remainder rather
     * than pretending the floor was met.
     */
    fun controlsAreaHeight(maxWidth: Dp, availableHeight: Dp): Dp =
        (availableHeight - stageBlockHeightCap(maxWidth, availableHeight))
            .coerceAtLeast(0.dp)
}
