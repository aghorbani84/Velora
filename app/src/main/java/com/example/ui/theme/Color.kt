package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * VELORA — "Beyond Playback." Hybrid Design System v2 (Obsidian Saffron, Daylight Ivory & OLED).
 * Every text/background pair is verified via WCAG 2.1 luminance math to exceed 4.5:1 AA contrast:
 * - Primary Text Ivory (#F5F2EB) on Obsidian (#0B0B0D) = 17.46:1
 * - Primary Text Ivory (#F5F2EB) on Secondary Background (#121216) = 16.51:1
 * - Primary Saffron (#E5A817) on Obsidian (#0B0B0D) = 9.39:1
 * - Champagne Gold (#D9BD83) on Obsidian (#0B0B0D) = 10.74:1
 * - Secondary Text (#A5A29B) on Secondary Background (#121216) = 7.36:1
 * - Upgraded Muted Text (#8F8B83) on Secondary Background (#121216) >= 5.3:1 (retired #77746E)
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
val VeloraAccentContainer = Color(0xFF2C2312)
val VeloraOnAccentContainer = Color(0xFFFFDEA3)
val VeloraPrimaryText = Color(0xFFF5F2EB)
val VeloraSecondaryText = Color(0xFFA5A29B)
val VeloraMutedText = Color(0xFF8F8B83)
val VeloraTextDisabled = Color(0xFF6B6862)
val VeloraControlDisabled = Color(0xFF2D2D34)
val VeloraDivider = Color(0xFF2A2927)

// Daylight Ivory (Light Theme) tokens
val VeloraDaylightIvory = Color(0xFFF9F6F0)
val VeloraDaylightSurface = Color(0xFFFFFFFF)
val VeloraDaylightElevated = Color(0xFFEDE7DC)
val VeloraDaylightCard = Color(0xFFE5DEC9)
val VeloraDaylightSaffron = Color(0xFF946200)
val VeloraDaylightContainer = Color(0xFFFFDEA3)
val VeloraDaylightOnContainer = Color(0xFF2F1E00)
val VeloraDaylightInk = Color(0xFF121216)
val VeloraDaylightMutedInk = Color(0xFF4C4639)
val VeloraDaylightTextDisabled = Color(0xFF7B7468)
val VeloraDaylightControlDisabled = Color(0xFFDCD5C7)
val VeloraDaylightBorder = Color(0xFFCFC8B8)
val VeloraDaylightError = Color(0xFFBA1A1A)

// OLED High-Contrast Monochrome tokens
val VeloraOledBlack = Color(0xFF000000)
val VeloraOledSurface = Color(0xFF0F0F12)
val VeloraOledElevated = Color(0xFF1C1C20)
val VeloraOledCard = Color(0xFF26262C)
val VeloraOledWhite = Color(0xFFFFFFFF)
val VeloraOledSilver = Color(0xFFD0D0D0)
val VeloraOledMuted = Color(0xFFB3B3B3)
val VeloraOledTextDisabled = Color(0xFF808088)
val VeloraOledControlDisabled = Color(0xFF2E2E35)
val VeloraOledBorder = Color(0xFF3E3E44)
val VeloraOledError = Color(0xFFFF5252)

// Semantic aliases used across screens & components
val FazoraObsidian = VeloraObsidian
val FazoraCarbon = VeloraSecondaryBg
val FazoraCarbonElevated = VeloraElevatedSurface
val FazoraCarbonBorder = VeloraDivider
val FazoraIvory = VeloraPrimaryText
val FazoraSteel = VeloraSecondaryText
val FazoraGold = VeloraSaffron
val FazoraGoldBright = VeloraBrightSaffron
val FazoraGoldDeep = VeloraDeepSaffron
val FazoraSoftGold = VeloraSoftGold
val FazoraGoldMuted = VeloraAccentContainer
val FazoraCrimsonError = Color(0xFFFF6B6B)
val FazoraEmeraldOk = Color(0xFF4ADE80)

// Raw 24-bit RGB ints for programmatic WCAG AA verification tests
const val HEX_OBSIDIAN = 0x0B0B0D
const val HEX_CARBON = 0x121216
const val HEX_IVORY = 0xF5F2EB
const val HEX_STEEL = 0xA5A29B
const val HEX_MUTED_TEXT = 0x8F8B83
const val HEX_GOLD = 0xE5A817
const val HEX_CHAMPAGNE_GOLD = 0xD9BD83

/**
 * Rule R-C4: Exactly three permitted translucent alpha levels across VELORA surfaces.
 */
object VeloraAlphaTokens {
    const val ERROR_SURFACE_ALPHA = 0.14f
    const val ACCENT_BORDER_ALPHA = 0.45f
    const val SCRIM_BACKGROUND_ALPHA = 0.60f
}

object VeloraElevation {
    val Level0 = 0.dp
    val Level1 = 2.dp
    val Level2 = 4.dp
    val Level3 = 8.dp
}

object VeloraMotionTokens {
    const val SkeletonPulseMs = 250
    const val TransitionFastMs = 150
    const val TransitionMediumMs = 250

    // Stageshow choreography & two-curve motion tokens translated to Jetpack Compose
    const val FeedbackMs = 150
    const val ContentSwapMs = 220
    const val StaggerStepMs = 55
    const val SheetSettleMs = 340
    const val SheetSpringStiffness = 420f
    const val SheetSpringDamping = 0.85f

    /** Departure -> clear travel -> soft arrival without bounce (Stageshow Travel curve). */
    val TravelEasing = CubicBezierEasing(0.45f, 0f, 0.1f, 1f)

    /** Arrival from nowhere / retained-frame content settle (Stageshow Arrival curve). */
    val ArrivalEasing = CubicBezierEasing(0.25f, 0.8f, 0.25f, 1f)

    /** Immediate touch-response easing for pills, tabs, and active row transitions. */
    val TouchResponseEasing = CubicBezierEasing(0.3f, 0.75f, 0.1f, 1f)
}

@Immutable
data class VeloraColors(
    val bg: Color,
    val bgSecondary: Color,
    val surfaceElevated: Color,
    val surfaceCard: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentBright: Color,
    val accentGold: Color,
    val accentSoftGold: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val controlDisabled: Color,
    val divider: Color,
    val error: Color,
    val ok: Color,
    val isDark: Boolean,
    val surfaceWell: Color = bgSecondary,
    val surfaceRaised: Color = surfaceElevated
)

val VeloraSaffronColors = VeloraColors(
    bg = VeloraObsidian,
    bgSecondary = VeloraSecondaryBg,
    surfaceElevated = VeloraElevatedSurface,
    surfaceCard = VeloraCardSurface,
    accent = VeloraSaffron,
    accentDeep = VeloraDeepSaffron,
    accentBright = VeloraBrightSaffron,
    accentGold = VeloraChampagneGold,
    accentSoftGold = VeloraSoftGold,
    accentContainer = VeloraAccentContainer,
    onAccentContainer = VeloraOnAccentContainer,
    textPrimary = VeloraPrimaryText,
    textSecondary = VeloraSecondaryText,
    textMuted = VeloraMutedText,
    textDisabled = VeloraTextDisabled,
    controlDisabled = VeloraControlDisabled,
    divider = VeloraDivider,
    error = FazoraCrimsonError,
    ok = FazoraEmeraldOk,
    isDark = true,
    surfaceWell = Color(0xFF15151B),
    surfaceRaised = Color(0xFF24242D)
)

val VeloraDaylightColors = VeloraColors(
    bg = VeloraDaylightIvory,
    bgSecondary = VeloraDaylightSurface,
    surfaceElevated = VeloraDaylightElevated,
    surfaceCard = VeloraDaylightCard,
    accent = VeloraDaylightSaffron,
    accentDeep = VeloraDeepSaffron,
    accentBright = VeloraSaffron,
    accentGold = VeloraSoftGold,
    accentSoftGold = VeloraDaylightSaffron,
    accentContainer = VeloraDaylightContainer,
    onAccentContainer = VeloraDaylightOnContainer,
    textPrimary = VeloraDaylightInk,
    textSecondary = VeloraDaylightMutedInk,
    textMuted = VeloraDaylightMutedInk,
    textDisabled = VeloraDaylightTextDisabled,
    controlDisabled = VeloraDaylightControlDisabled,
    divider = VeloraDaylightBorder,
    error = VeloraDaylightError,
    ok = Color(0xFF15803D),
    isDark = false,
    surfaceWell = Color(0xFFF2ECE1),
    surfaceRaised = VeloraDaylightSurface
)

val VeloraHighContrastColors = VeloraColors(
    bg = VeloraOledBlack,
    bgSecondary = VeloraOledSurface,
    surfaceElevated = VeloraOledElevated,
    surfaceCard = VeloraOledCard,
    accent = VeloraOledWhite,
    accentDeep = VeloraOledSilver,
    accentBright = VeloraOledWhite,
    accentGold = VeloraOledSilver,
    accentSoftGold = VeloraOledSilver,
    accentContainer = VeloraOledElevated,
    onAccentContainer = VeloraOledWhite,
    textPrimary = VeloraOledWhite,
    textSecondary = VeloraOledSilver,
    textMuted = VeloraOledMuted,
    textDisabled = VeloraOledTextDisabled,
    controlDisabled = VeloraOledControlDisabled,
    divider = VeloraOledBorder,
    error = VeloraOledError,
    ok = Color(0xFF4ADE80),
    isDark = true,
    surfaceWell = VeloraOledSurface,
    surfaceRaised = VeloraOledCard
)

/**
 * Golden Ratio (Phi = 1.61803398875) & Fibonacci Spacing Scale (4 / 8 / 13 / 21 / 34 / 55 dp).
 * All dimensions in screens and components reference these tokens — zero raw .dp literals.
 */
object PhiTokens {
    const val PHI = 1.618034f
    const val PRIMARY_WEIGHT = 0.618f
    const val SECONDARY_WEIGHT = 0.382f
    const val ARTWORK_STAGE_MAX_FRACTION = 0.618f

    // Fibonacci spacing scale
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space13 = 13.dp
    val Space21 = 21.dp
    val Space34 = 34.dp
    val Space55 = 55.dp

    // Accessibility & structural dimensions
    val BorderHairline = 1.dp
    val BorderAccent = 1.5.dp
    val MinTouchTarget = 48.dp
    val IconSize16 = 16.dp
    val IconSize20 = 20.dp
    val IconSize24 = 24.dp
    val IconSize34 = 34.dp
    val AvatarSize48 = 48.dp
    val HeroCrestSize = 55.dp
    val SpectrumHeight = 144.dp
    val VideoSurfaceHeight = 233.dp
    val CardWidthCompact = 233.dp
    val ShelfCardWidth = 156.dp
    val ShelfCardArtSize = 132.dp
    val MaxContentWidth = 610.dp
    val WideScreenBreakpoint = 600.dp
    val DesktopSidebarBreakpoint = 720.dp
    val SidebarExpandedWidth = 240.dp
    val SidebarCollapsedWidth = 76.dp

    // Corner radii
    val CornerRadius8 = 8.dp
    val CornerRadius13 = 13.dp
    val CornerRadius21 = 21.dp
    val CornerRadius34 = 34.dp
}
