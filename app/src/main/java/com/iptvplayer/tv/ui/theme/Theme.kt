package com.iptvplayer.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// Live TV Color Palette
object NovaColors {
    // Backgrounds
    val Background = Color(0xFF0a0a0f)
    val BackgroundDark = Color(0xFF04140c)
    val Surface = Color(0xFF16161f)
    val SurfaceVariant = Color(0xFF2a2a3a)

    // Primary - Green accent
    val Primary = Color(0xFF1ce783)
    val PrimaryDark = Color(0xFF12b56a)
    val PrimaryContainer = Color(0xFF04140c)
    val OnPrimary = Color(0xFF04140c)

    // Secondary - Red/Pink accent
    val Secondary = Color(0xFFff3b5c)
    val SecondaryContainer = Color(0xFF2a1215)

    // Text colors
    val TextPrimary = Color(0xFFf4f4f6)
    val TextSecondary = Color(0xFFc7c7d2)
    val TextMuted = Color(0xFF8f8f9c)
    val TextDim = Color(0xFF7a7a88)

    // Other
    val Border = Color(0xFF2a2a3a)
    val Error = Color(0xFFff3b5c)
    val Success = Color(0xFF1ce783)

    // Gradients (for manual use)
    val GradientStart = Color(0xFF1ce783)
    val GradientEnd = Color(0xFF12b56a)
}

private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaColors.Primary,
    onPrimary = NovaColors.OnPrimary,
    primaryContainer = NovaColors.PrimaryContainer,
    onPrimaryContainer = NovaColors.TextPrimary,
    secondary = NovaColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = NovaColors.SecondaryContainer,
    onSecondaryContainer = NovaColors.TextPrimary,
    tertiary = NovaColors.Primary,
    onTertiary = NovaColors.OnPrimary,
    tertiaryContainer = NovaColors.PrimaryContainer,
    onTertiaryContainer = NovaColors.TextPrimary,
    error = NovaColors.Error,
    onError = Color.White,
    errorContainer = NovaColors.SecondaryContainer,
    onErrorContainer = NovaColors.TextPrimary,
    background = NovaColors.Background,
    onBackground = NovaColors.TextPrimary,
    surface = NovaColors.Surface,
    onSurface = NovaColors.TextPrimary,
    surfaceVariant = NovaColors.SurfaceVariant,
    onSurfaceVariant = NovaColors.TextSecondary,
    border = NovaColors.Border,
    borderVariant = NovaColors.SurfaceVariant,
    scrim = Color.Black,
)

@Composable
fun IPTVPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NovaDarkColorScheme,
        content = content
    )
}
