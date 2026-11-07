package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VeloraSaffronColorScheme = darkColorScheme(
    primary = VeloraSaffron,
    onPrimary = VeloraObsidian,
    primaryContainer = FazoraGoldMuted,
    onPrimaryContainer = VeloraPrimaryText,
    secondary = VeloraChampagneGold,
    onSecondary = VeloraObsidian,
    secondaryContainer = VeloraElevatedSurface,
    onSecondaryContainer = VeloraPrimaryText,
    tertiary = VeloraBrightSaffron,
    onTertiary = VeloraObsidian,
    background = VeloraObsidian,
    onBackground = VeloraPrimaryText,
    surface = VeloraSecondaryBg,
    onSurface = VeloraPrimaryText,
    surfaceVariant = VeloraCardSurface,
    onSurfaceVariant = VeloraSecondaryText,
    outline = VeloraDivider,
    error = FazoraCrimsonError,
    onError = VeloraObsidian
)

val FazoraShapes = Shapes(
    small = RoundedCornerShape(PhiTokens.CornerRadius8),
    medium = RoundedCornerShape(PhiTokens.CornerRadius13),
    large = RoundedCornerShape(PhiTokens.CornerRadius21)
)

@Composable
fun FazoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VeloraSaffronColorScheme,
        typography = FazoraTypography,
        shapes = FazoraShapes,
        content = content
    )
}
