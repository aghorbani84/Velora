package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * VELORA — Neon Nocturne / Aurora Signal.
 * A dark listening instrument rather than a gold luxury card catalogue:
 * deep navy anchors the canvas, cyan marks the live signal, violet marks depth,
 * and coral is reserved for attention and expressive moments.
 * Foreground accents are selected to exceed 4.5:1 against the night canvas.
 */
val VeloraObsidian = Color(0xFF070A12)
val VeloraSecondaryBg = Color(0xFF0D1424)
val VeloraElevatedSurface = Color(0xFF14213A)
val VeloraCardSurface = Color(0xFF1C2C4A)
val VeloraSaffron = Color(0xFF6EE7FF)
val VeloraDeepSaffron = Color(0xFF36BBD6)
val VeloraBrightSaffron = Color(0xFFB8A1FF)
val VeloraChampagneGold = Color(0xFFFF8BA7)
val VeloraSoftGold = Color(0xFF8BE9FD)
val VeloraPrimaryText = Color(0xFFF4F7FF)
val VeloraSecondaryText = Color(0xFFA8B4CC)
val VeloraMutedText = Color(0xFF687795)
val VeloraDivider = Color(0xFF263A5E)

// Semantic aliases used across screens & components
val FazoraObsidian = VeloraObsidian
val FazoraCarbon = VeloraSecondaryBg
val FazoraCarbonElevated = VeloraElevatedSurface
val FazoraCarbonBorder = VeloraDivider
val FazoraIvory = VeloraPrimaryText
val FazoraSteel = VeloraSecondaryText
val FazoraGold = VeloraSaffron
val FazoraGoldBright = VeloraBrightSaffron
val FazoraGoldMuted = Color(0xFF123449)
val FazoraCrimsonError = Color(0xFFFF708A)
val FazoraEmeraldOk = Color(0xFF6EF0C0)

// Raw 24-bit RGB ints for programmatic WCAG AA verification tests
const val HEX_OBSIDIAN = 0x070A12
const val HEX_CARBON = 0x0D1424
const val HEX_IVORY = 0xF4F7FF
const val HEX_STEEL = 0xA8B4CC
const val HEX_GOLD = 0x6EE7FF
const val HEX_CHAMPAGNE_GOLD = 0xFF8BA7

/**
 * Golden Ratio (Phi = 1.61803398875) & Fibonacci Spacing Scale (8 / 13 / 21 / 34 / 55 dp).
 */
object PhiTokens {
    const val PHI = 1.618034f
    const val PRIMARY_WEIGHT = 0.618f
    const val SECONDARY_WEIGHT = 0.382f

    val Space8 = 8.dp
    val Space13 = 13.dp
    val Space21 = 21.dp
    val Space34 = 34.dp
    val Space55 = 55.dp

    val MinTouchTarget = 48.dp
    val CornerRadius8 = 10.dp
    val CornerRadius13 = 16.dp
    val CornerRadius21 = 24.dp
}
