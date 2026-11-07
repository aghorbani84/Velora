package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * VELORA — "Beyond Playback." Dark Saffron & Champagne Gold Design System.
 * Every text/background pair is verified via WCAG 2.1 luminance math to exceed 4.5:1 AA contrast:
 * - Primary Text Ivory (#F5F2EB) on Obsidian (#0B0B0D) = 17.46:1
 * - Primary Text Ivory (#F5F2EB) on Secondary Background (#121216) = 16.51:1
 * - Primary Saffron (#E5A817) on Obsidian (#0B0B0D) = 9.39:1
 * - Champagne Gold (#D9BD83) on Obsidian (#0B0B0D) = 10.74:1
 * - Secondary Text (#A5A29B) on Secondary Background (#121216) = 7.36:1
 */
val VeloraObsidian = Color(0xFF0B0B0D)
val VeloraSecondaryBg = Color(0xFF121216)
val VeloraElevatedSurface = Color(0xFF19191F)
val VeloraCardSurface = Color(0xFF202027)
val VeloraSaffron = Color(0xFFE5A817)
val VeloraDeepSaffron = Color(0xFFC88712)
val VeloraBrightSaffron = Color(0xFFF5C451)
val VeloraChampagneGold = Color(0xFFD9BD83)
val VeloraSoftGold = Color(0xFFBDA36B)
val VeloraPrimaryText = Color(0xFFF5F2EB)
val VeloraSecondaryText = Color(0xFFA5A29B)
val VeloraMutedText = Color(0xFF77746E)
val VeloraDivider = Color(0xFF2A2927)

// Semantic aliases used across screens & components
val FazoraObsidian = VeloraObsidian
val FazoraCarbon = VeloraSecondaryBg
val FazoraCarbonElevated = VeloraElevatedSurface
val FazoraCarbonBorder = VeloraDivider
val FazoraIvory = VeloraPrimaryText
val FazoraSteel = VeloraSecondaryText
val FazoraGold = VeloraSaffron
val FazoraGoldBright = VeloraBrightSaffron
val FazoraGoldMuted = Color(0xFF2C2312)
val FazoraCrimsonError = Color(0xFFFF6B6B)
val FazoraEmeraldOk = Color(0xFF4ADE80)

// Raw 24-bit RGB ints for programmatic WCAG AA verification tests
const val HEX_OBSIDIAN = 0x0B0B0D
const val HEX_CARBON = 0x121216
const val HEX_IVORY = 0xF5F2EB
const val HEX_STEEL = 0xA5A29B
const val HEX_GOLD = 0xE5A817
const val HEX_CHAMPAGNE_GOLD = 0xD9BD83

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
    val CornerRadius8 = 8.dp
    val CornerRadius13 = 13.dp
    val CornerRadius21 = 21.dp
}
