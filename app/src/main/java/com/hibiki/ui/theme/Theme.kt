package com.hibiki.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val CyberpunkColors = darkColorScheme(
    primary = Cyberpunk.NeonCyan,
    onPrimary = Cyberpunk.Void,
    primaryContainer = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.18f),
    onPrimaryContainer = Cyberpunk.TextPrimary,
    inversePrimary = Cyberpunk.NeonCyan,
    secondary = Cyberpunk.NeonMagenta,
    onSecondary = Cyberpunk.Void,
    secondaryContainer = Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.18f),
    onSecondaryContainer = Cyberpunk.TextPrimary,
    tertiary = Cyberpunk.NeonLime,
    onTertiary = Cyberpunk.Void,
    tertiaryContainer = Cyberpunk.withAlpha(Cyberpunk.NeonLime, 0.18f),
    onTertiaryContainer = Cyberpunk.Void,
    background = Cyberpunk.Void,
    onBackground = Cyberpunk.TextPrimary,
    surface = Cyberpunk.Deep,
    onSurface = Cyberpunk.TextPrimary,
    surfaceVariant = Cyberpunk.Panel,
    onSurfaceVariant = Cyberpunk.TextMuted,
    surfaceTint = Cyberpunk.Transparent,
    inverseSurface = Cyberpunk.TextPrimary,
    inverseOnSurface = Cyberpunk.Void,
    error = Cyberpunk.NeonMagenta,
    onError = Cyberpunk.Void,
    errorContainer = Cyberpunk.withAlpha(Cyberpunk.NeonMagenta, 0.18f),
    onErrorContainer = Cyberpunk.TextPrimary,
    outline = Cyberpunk.withAlpha(Cyberpunk.NeonCyan, 0.35f),
    outlineVariant = Cyberpunk.GridLine,
    scrim = Cyberpunk.withAlpha(Cyberpunk.Void, 0.72f),
)

val HibikiTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 120.sp,
        lineHeight = 120.sp,
        letterSpacing = 2.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 4.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 2.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.5.sp,
    ),
)

@Composable
fun HibikiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberpunkColors,
        typography = HibikiTypography,
        content = content,
    )
}
