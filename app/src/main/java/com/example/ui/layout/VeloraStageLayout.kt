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
     * The stage is limited by both its width cap and the height left after
     * reserving the controls/queue floor. The requested spectrum strip is
     * reduced too when the viewport cannot fit it without violating that
     * floor; artwork is reduced first, then the spectrum strip.
     *
     * [preferredSpectrumHeight] allows the compact layout to request a smaller
     * strip without changing the R-S3 arithmetic.
     */
    fun stageBlockHeightCap(
        maxWidth: Dp,
        availableHeight: Dp,
        preferredSpectrumHeight: Dp = PhiTokens.SpectrumHeight
    ): Dp {
        val safeHeight = availableHeight.coerceAtLeast(0.dp)
        val controlsFloor = minOf(CONTROLS_AREA_MIN_HEIGHT, safeHeight)
        val stageBudget = (safeHeight - controlsFloor).coerceAtLeast(0.dp)
        val spectrumHeight = minOf(preferredSpectrumHeight.coerceAtLeast(0.dp), stageBudget)
        val artworkByWidth = maxWidth.coerceAtLeast(0.dp) * PhiTokens.ARTWORK_STAGE_MAX_FRACTION
        val artworkByRemainder = (stageBudget - spectrumHeight).coerceAtLeast(0.dp)
        val artwork = minOf(artworkByWidth, artworkByRemainder).coerceAtLeast(0.dp)
        return artwork + spectrumHeight
    }

    /**
     * Height left for controls + queue. It is at least [CONTROLS_AREA_MIN_HEIGHT]
     * whenever the viewport itself is tall enough; below that viewport height,
     * it reports the real available height instead of claiming the floor was met.
     */
    fun controlsAreaHeight(
        maxWidth: Dp,
        availableHeight: Dp,
        preferredSpectrumHeight: Dp = PhiTokens.SpectrumHeight
    ): Dp =
        (availableHeight.coerceAtLeast(0.dp) -
            stageBlockHeightCap(maxWidth, availableHeight, preferredSpectrumHeight))
            .coerceAtLeast(0.dp)
}
