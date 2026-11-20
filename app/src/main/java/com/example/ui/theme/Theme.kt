package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val InstitutionalLightColorScheme = lightColorScheme(
    primary = CyanAccent,
    onPrimary = Color.White,
    primaryContainer = TerminalSurfaceHighlight,
    onPrimaryContainer = RoyalBlueDark,
    secondary = ProfitGreen,
    onSecondary = Color.White,
    secondaryContainer = ProfitGreenMuted,
    onSecondaryContainer = ProfitGreen,
    tertiary = IndigoCurve,
    onTertiary = Color.White,
    error = LossRed,
    onError = Color.White,
    errorContainer = LossRedMuted,
    onErrorContainer = LossRed,
    background = TerminalBg,
    onBackground = TextPrimary,
    surface = TerminalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TerminalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TerminalBorder,
    outlineVariant = TerminalBorder
)

@Composable
fun StrikeDeskTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = InstitutionalLightColorScheme,
        typography = StrikeDeskTypography,
        content = content
    )
}
