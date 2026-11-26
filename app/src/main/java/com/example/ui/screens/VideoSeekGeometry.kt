package com.example.ui.screens

// ---------------------------------------------------------------------------
// Pure seek geometry (unit-testable, no Android/Compose dependency beyond
// plain floats). The M3 Slider centers its thumb on a track that is inset
// by one thumb radius (10dp) on each side, so the thumb centre travels
// between thumbRadius and (width - thumbRadius). The overlay thumb and the
// buffered/played fills in VideoSeekBar use exactly this geometry.
//
// LTR ONLY, by design: media progress represents tape direction, not
// reading direction (Material 3 bidirectionality rule), so the seek bar
// is wrapped in an LTR layout direction at the call site and these
// helpers never mirror. Both helpers are exact inverses of each other:
// seekFractionForX(seekThumbCenterX(f)) == f for f in 0..1.
// ---------------------------------------------------------------------------

/**
 * Maps a raw x position inside a seek bar of [widthPx] to a 0..1 fraction.
 * The fraction is measured from the inset track (one [thumbRadiusPx] in
 * from each edge), matching [seekThumbCenterX].
 *
 * Guards: NaN input, non-positive width, and a bar narrower than two thumb
 * radii all return 0f — Float.NaN.coerceIn() would otherwise leak NaN.
 */
internal fun seekFractionForX(xPx: Float, widthPx: Float, thumbRadiusPx: Float): Float {
    if (xPx.isNaN() || widthPx.isNaN() || thumbRadiusPx.isNaN()) return 0f
    val radius = thumbRadiusPx.coerceAtLeast(0f)
    val travel = widthPx - 2f * radius
    if (widthPx <= 0f || travel <= 0f) return 0f
    return ((xPx - radius) / travel).coerceIn(0f, 1f)
}

/**
 * Centre-x (px) of the seek thumb for [fraction] inside a bar of [widthPx].
 * Guards NaN input and non-positive width (both return 0f).
 */
internal fun seekThumbCenterX(fraction: Float, widthPx: Float, thumbRadiusPx: Float): Float {
    if (fraction.isNaN() || widthPx.isNaN() || thumbRadiusPx.isNaN()) return 0f
    if (widthPx <= 0f) return 0f
    val radius = thumbRadiusPx.coerceAtLeast(0f)
    val travel = (widthPx - 2f * radius).coerceAtLeast(0f)
    return radius + fraction.coerceIn(0f, 1f) * travel
}
