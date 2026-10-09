package com.motovault.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MotoVaultTheme(
    dynamicAccentColor: Color = KtmApexOrange,
    content: @Composable () -> Unit
) {
    val darkColors = darkColorScheme(
        primary = dynamicAccentColor,
        onPrimary = Color.Black,
        secondary = dynamicAccentColor.copy(alpha = 0.8f),
        onSecondary = Color.Black,
        background = CanvasTftDark,
        onBackground = TextPrimary,
        surface = SurfaceCardDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceElevatedDark,
        outline = BorderSubtleDark
    )

    MaterialTheme(
        colorScheme = darkColors,
        typography = MotoVaultTypography,
        content = content
    )
}
