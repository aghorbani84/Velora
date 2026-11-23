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
 * A compact viewport mitigation now reduces artwork, spectrum height and card
 * padding below 600dp, with a display-level test for a 320x470dp window. This
 * still does not reserve the full 317dp controls + queue floor or guarantee a
 * queue row is visible; the floor remains a specification/helper, not a fully
 * wired production constraint. The original short-window CI failure was run #77
 * of PR #4, where transport tags were outside the measured viewport.
 *
 * This object holds the number and arithmetic in one place. It is not wired
 * into production layout yet; issue #5 remains open for the complete measured
 * controls/queue reservation and one-row queue viewport.
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
        val safeHeight = availableHeight.coerceAtLeast(0.dp)
        val spectrumHeight = minOf(PhiTokens.SpectrumHeight, safeHeight)
        val artworkByWidth = maxWidth.coerceAtLeast(0.dp) * PhiTokens.ARTWORK_STAGE_MAX_FRACTION
        val artworkByRemainder =
            safeHeight - CONTROLS_AREA_MIN_HEIGHT - spectrumHeight
        val artwork = minOf(artworkByWidth, artworkByRemainder).coerceAtLeast(0.dp)
        return artwork + spectrumHeight
    }

    /**
     * Height left for controls + queue. Equals or exceeds
     * [CONTROLS_AREA_MIN_HEIGHT] whenever the window itself is tall enough;
     * on a window shorter than the floor it reports the real remainder rather
     * than pretending the floor was met.
     */
    fun controlsAreaHeight(maxWidth: Dp, availableHeight: Dp): Dp =
        (availableHeight.coerceAtLeast(0.dp) - stageBlockHeightCap(maxWidth, availableHeight))
            .coerceAtLeast(0.dp)
}
