package com.motovault.app.ui.redesign

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable

@Serializable
enum class Appearance(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

fun Appearance.usesDarkPalette(systemDark: Boolean): Boolean = when (this) {
    Appearance.SYSTEM -> systemDark
    Appearance.LIGHT -> false
    Appearance.DARK -> true
}

data class VaultPalette(
    val dark: Boolean, val canvas: Color, val surface: Color, val raised: Color,
    val text: Color, val muted: Color, val line: Color, val outline: Color,
    val healthy: Color, val warning: Color, val error: Color, val accent: Color, val onAccent: Color
)

fun vaultPalette(brandId: String, dark: Boolean): VaultPalette {
    val darkAccent = when (brandId) {
        "cfmoto" -> 0xFF69D2DB; "yamaha" -> 0xFF88A8FF; "honda" -> 0xFFFF7B84
        "suzuki" -> 0xFF7EBAF5; "bajaj" -> 0xFF96B9FA; "tvs" -> 0xFFF4828B
        "hero" -> 0xFFFF8983; "enfield" -> 0xFFDBB77C; else -> 0xFFFF7900
    }
    val lightAccent = when (brandId) {
        "cfmoto" -> 0xFF00717A; "yamaha" -> 0xFF315ABE; "honda" -> 0xFFB42A38
        "suzuki" -> 0xFF246294; "bajaj" -> 0xFF355D9E; "tvs" -> 0xFFA52B41
        "hero" -> 0xFFB1362C; "enfield" -> 0xFF805E28; else -> 0xFFAA4700
    }
    return if (dark) VaultPalette(true, Color(0xFF101215), Color(0xFF171A1E), Color(0xFF22262C),
        Color(0xFFF1F2F3), Color(0xFF9DA5AF), Color(0xFF353B44), Color(0xFF78828F),
        Color(0xFF7BD3AB), Color(0xFFEFC277), Color(0xFFFF929A), Color(darkAccent), Color(0xFF101215))
    else VaultPalette(false, Color(0xFFF5F4EF), Color(0xFFFFFFFF), Color(0xFFE9ECF0),
        Color(0xFF181C22), Color(0xFF535E6B), Color(0xFFD4D9DF), Color(0xFF737E8B),
        Color(0xFF236D4F), Color(0xFF805817), Color(0xFFAD2938), Color(lightAccent), Color.White)
}

internal val LocalVaultPalette = staticCompositionLocalOf { vaultPalette("ktm", false) }
internal val LocalVaultBrandId = staticCompositionLocalOf { "ktm" }
internal val CanvasColor: Color @Composable get() = LocalVaultPalette.current.canvas
internal val SurfaceColor: Color @Composable get() = LocalVaultPalette.current.surface
internal val TextPrimary: Color @Composable get() = LocalVaultPalette.current.text
internal val TextSecondary: Color @Composable get() = LocalVaultPalette.current.muted
internal val BorderColor: Color @Composable get() = LocalVaultPalette.current.line
internal val HealthyColor: Color @Composable get() = LocalVaultPalette.current.healthy
internal val WarningColor: Color @Composable get() = LocalVaultPalette.current.warning
internal val ErrorColor: Color @Composable get() = LocalVaultPalette.current.error

@Composable
expect fun PlatformAppearance(dark: Boolean)

@Composable
internal fun VaultTheme(brandId: String, appearance: Appearance, typography: Typography = MaterialTheme.typography, content: @Composable () -> Unit) {
    val dark = appearance.usesDarkPalette(isSystemInDarkTheme())
    val palette = vaultPalette(brandId, dark)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = palette.accent, onPrimary = palette.onAccent,
        primaryContainer = palette.accent.copy(alpha = if (dark) .17f else .09f).compositeOver(palette.surface), onPrimaryContainer = palette.text,
        secondary = palette.accent, onSecondary = palette.onAccent,
        secondaryContainer = palette.accent.copy(alpha = if (dark) .17f else .09f).compositeOver(palette.surface), onSecondaryContainer = palette.text,
        tertiary = palette.healthy, onTertiary = palette.onAccent,
        background = palette.canvas, onBackground = palette.text,
        surface = palette.surface, onSurface = palette.text,
        surfaceVariant = palette.raised, onSurfaceVariant = palette.muted,
        surfaceContainer = palette.surface, surfaceContainerLow = palette.surface,
        surfaceContainerLowest = palette.canvas, surfaceContainerHigh = palette.raised,
        surfaceContainerHighest = palette.raised, surfaceBright = palette.raised, surfaceDim = palette.canvas,
        surfaceTint = palette.accent, outline = palette.outline, outlineVariant = palette.line,
        error = palette.error, onError = palette.onAccent,
        inverseSurface = palette.text, inverseOnSurface = palette.canvas
    )
    MaterialTheme(colorScheme = scheme, typography = typography) {
        CompositionLocalProvider(LocalVaultPalette provides palette, LocalVaultBrandId provides brandId, LocalContentColor provides palette.text) { content() }
    }
}

@Composable
internal fun AppearanceButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = "Change appearance" }) { Text("Appearance", fontSize = 10.sp) }
}

@Composable
internal fun AppearanceDialog(current: Appearance, onDismiss: () -> Unit, onSelect: (Appearance) -> Unit) {
    Sheet("Appearance", onDismiss) {
        Text("Your bike’s colors, in the light that suits you.", color = TextSecondary, fontSize = 13.sp)
        Appearance.entries.forEach { choice ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(choice) }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(current == choice, onClick = { onSelect(choice) })
                Column {
                    Text(choice.label)
                    Text(when (choice) { Appearance.SYSTEM -> "Follow your device"; Appearance.LIGHT -> "Light surfaces"; Appearance.DARK -> "Dark surfaces" }, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
