package com.motovault.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sky.design.SkyBrand
import dev.sky.design.SkyColors
import dev.sky.design.SkyMotionPolicy
import dev.sky.design.SkySvgIcon
import dev.sky.design.SkyTheme
import dev.sky.design.SkyTypography
import dev.sky.design.defaultSkyTypography
import dev.sky.design.tokens.SkyTokens
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.anek_bangla
import motovault.composeapp.generated.resources.appearance_change_desc
import motovault.composeapp.generated.resources.barlow_condensed
import motovault.composeapp.generated.resources.unit_days
import motovault.composeapp.generated.resources.unit_km
import motovault.composeapp.generated.resources.unit_litre
import org.jetbrains.compose.resources.Font

fun Appearance.usesDarkPalette(systemDark: Boolean): Boolean = when (this) {
    Appearance.SYSTEM -> systemDark
    Appearance.LIGHT -> false
    Appearance.DARK -> true
}

data class BrandColors(val light: Long, val dark: Long)

val BRAND_COLOR_MAP: Map<String, BrandColors> = mapOf(
    "ktm" to BrandColors(0xFFAA4700, 0xFFFF7900),
    "cfmoto" to BrandColors(0xFF00717A, 0xFF69D2DB),
    "yamaha" to BrandColors(0xFF315ABE, 0xFF88A8FF),
    "honda" to BrandColors(0xFFB42A38, 0xFFFF7B84),
    "suzuki" to BrandColors(0xFF246294, 0xFF7EBAF5),
    "bajaj" to BrandColors(0xFF355D9E, 0xFF96B9FA),
    "tvs" to BrandColors(0xFFA52B41, 0xFFF4828B),
    "hero" to BrandColors(0xFFB1362C, 0xFFFF8983),
    "enfield" to BrandColors(0xFF805E28, 0xFFDBB77C)
)

/** Linear mix in sRGB, matching CSS `color-mix(in srgb, first (firstWeight*100)%, second)`. */
fun colorMix(first: Color, second: Color, firstWeight: Float): Color {
    val w = firstWeight.coerceIn(0f, 1f)
    val inv = 1f - w
    return Color(
        red = first.red * w + second.red * inv,
        green = first.green * w + second.green * inv,
        blue = first.blue * w + second.blue * inv,
        alpha = first.alpha * w + second.alpha * inv
    )
}

fun colorMixTransparent(color: Color, weight: Float): Color =
    color.copy(alpha = color.alpha * weight.coerceIn(0f, 1f))

enum class StatusTone { OK, SOON, OVERDUE }

data class VaultPalette(
    val dark: Boolean,
    val canvas: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val line: Color,
    val lineStrong: Color,
    val ok: Color,
    val warn: Color,
    val err: Color,
    val accent: Color,
    val onAccent: Color,
    val brandTint: Color,
    val brandLine: Color,
    val key: Color,
    val keyDown: Color,
    val photoWellStart: Color,
    val photoWellEnd: Color,
    val wash: Color,
    val sheetWash: Color,
    val shadow: Color,
    val scrim: Color,
    val segPill: Color
) {
    val raised: Color get() = surface2
    val outline: Color get() = lineStrong
    val muted: Color get() = text2
    val healthy: Color get() = ok
    val warning: Color get() = warn
    val error: Color get() = err

    fun statusBadgeBg(tone: StatusTone): Color = if (dark) {
        when (tone) {
            StatusTone.OK -> colorMix(Color(0xFF22C55E), Color(0xFF111814), 0.22f)
            StatusTone.SOON -> colorMix(Color(0xFFF59E0B), Color(0xFF1A150B), 0.24f)
            StatusTone.OVERDUE -> colorMix(Color(0xFFEF4444), Color(0xFF240D0F), 0.26f)
        }
    } else {
        when (tone) {
            StatusTone.OK -> colorMix(ok, surface, 0.12f)
            StatusTone.SOON -> colorMix(warn, surface, 0.14f)
            StatusTone.OVERDUE -> colorMix(err, surface, 0.14f)
        }
    }

    fun statusBadgeFg(tone: StatusTone): Color = if (dark) {
        when (tone) {
            StatusTone.OK -> Color(0xFF86EFAC)
            StatusTone.SOON -> Color(0xFFFDE68A)
            StatusTone.OVERDUE -> Color(0xFFFECACA)
        }
    } else {
        when (tone) {
            StatusTone.OK -> ok
            StatusTone.SOON -> warn
            StatusTone.OVERDUE -> err
        }
    }
}

fun vaultPalette(brandId: String, dark: Boolean): VaultPalette {
    val brandColors = BRAND_COLOR_MAP[brandId] ?: BRAND_COLOR_MAP["ktm"]!!
    val lightAccent = Color(brandColors.light)
    val darkAccent = Color(brandColors.dark)
    val accent = if (dark) darkAccent else lightAccent
    val onAccent = if (dark) Color(0xFF101215) else Color.White
    val canvas = if (dark) Color(0xFF101411) else Color.White
    val surface = if (dark) Color(0xFF202723) else Color.White
    val brandTint = colorMix(accent, surface, 0.11f)
    val brandLine = colorMix(accent, surface, 0.45f)
    val scrim = Color.Black.copy(alpha = 0.60f)

    return if (dark) {
        VaultPalette(
            dark = true, canvas = canvas, surface = surface, surface2 = Color(0xFF181C1A),
            text = Color(0xFFF1F2F3), text2 = Color(0xFFC4CAD0), text3 = Color(0xFF9DA5AF),
            line = Color(0xFF2F3833), lineStrong = Color(0xFF78828F),
            ok = Color(0xFF86EFAC), warn = Color(0xFFFCD34D), err = Color(0xFFFCA5A5),
            accent = accent, onAccent = onAccent, brandTint = brandTint, brandLine = brandLine,
            key = Color(0xFF2A332E), keyDown = Color(0xFF38443D),
            photoWellStart = colorMix(accent, Color(0xFF202723), 0.22f), photoWellEnd = Color(0xFF181C1A),
            wash = colorMix(accent, canvas, 0.30f), sheetWash = colorMix(accent, surface, 0.24f),
            shadow = Color.Black.copy(alpha = 0.50f), scrim = scrim, segPill = Color(0xFF2F3833)
        )
    } else {
        VaultPalette(
            dark = false, canvas = canvas, surface = surface, surface2 = Color(0xFFF3F5F4),
            text = Color(0xFF181818), text2 = Color(0xFF595959), text3 = Color(0xFF66707C),
            line = Color(0xFFE3E6E4), lineStrong = Color(0xFF737E8B),
            ok = Color(0xFF236D4F), warn = Color(0xFF805817), err = Color(0xFFAD2938),
            accent = accent, onAccent = onAccent, brandTint = brandTint, brandLine = brandLine,
            key = Color(0xFFF3F5F4), keyDown = Color(0xFFDFE4E1),
            photoWellStart = colorMix(accent, Color.White, 0.14f), photoWellEnd = Color(0xFFF3F5F4),
            wash = colorMix(accent, canvas, 0.18f), sheetWash = colorMix(accent, surface, 0.16f),
            shadow = Color(0x24101411), scrim = scrim, segPill = surface
        )
    }
}

val LocalVaultPalette = staticCompositionLocalOf { vaultPalette("ktm", false) }
val LocalVaultBrandId = staticCompositionLocalOf { "ktm" }

val CanvasColor: Color @Composable get() = LocalVaultPalette.current.canvas
val SurfaceColor: Color @Composable get() = LocalVaultPalette.current.surface
val SurfaceColor2: Color @Composable get() = LocalVaultPalette.current.surface2
val TextPrimary: Color @Composable get() = LocalVaultPalette.current.text
val TextSecondary: Color @Composable get() = LocalVaultPalette.current.text2
val TextTertiary: Color @Composable get() = LocalVaultPalette.current.text3
val BorderColor: Color @Composable get() = LocalVaultPalette.current.line
val BorderStrongColor: Color @Composable get() = LocalVaultPalette.current.lineStrong
val HealthyColor: Color @Composable get() = LocalVaultPalette.current.ok
val WarningColor: Color @Composable get() = LocalVaultPalette.current.warn
val ErrorColor: Color @Composable get() = LocalVaultPalette.current.err
val BrandColor: Color @Composable get() = LocalVaultPalette.current.accent
val OnBrandColor: Color @Composable get() = LocalVaultPalette.current.onAccent
val BrandTintColor: Color @Composable get() = LocalVaultPalette.current.brandTint
val BrandLineColor: Color @Composable get() = LocalVaultPalette.current.brandLine
val KeyColor: Color @Composable get() = LocalVaultPalette.current.key
val KeyDownColor: Color @Composable get() = LocalVaultPalette.current.keyDown
val WashColor: Color @Composable get() = LocalVaultPalette.current.wash
val SheetWashColor: Color @Composable get() = LocalVaultPalette.current.sheetWash
val ShadowColor: Color @Composable get() = LocalVaultPalette.current.shadow
val ScrimColor: Color @Composable get() = LocalVaultPalette.current.scrim

@Composable
expect fun PlatformAppearance(dark: Boolean)

internal expect fun sheetDialogProperties(): androidx.compose.ui.window.DialogProperties

@Composable
internal expect fun SheetWindowPolicy()

data class VaultTypography(
    val display: TextStyle,
    val title: TextStyle,
    val head: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle
)

val LocalVaultTypography = staticCompositionLocalOf {
    VaultTypography(
        display = TextStyle.Default,
        title = TextStyle.Default,
        head = TextStyle.Default,
        body = TextStyle.Default,
        label = TextStyle.Default,
        caption = TextStyle.Default
    )
}

val currentTypography: VaultTypography
    @Composable get() = LocalVaultTypography.current

@Composable
internal fun rememberBanglaTypography(scale: Float = 1.0f): Pair<Typography, VaultTypography> {
    val anekBangla = FontFamily(
        Font(Res.font.anek_bangla, weight = FontWeight.Normal),
        Font(Res.font.anek_bangla, weight = FontWeight.Medium),
        Font(Res.font.anek_bangla, weight = FontWeight.SemiBold),
        Font(Res.font.anek_bangla, weight = FontWeight.Bold)
    )
    val barlow = FontFamily(
        Font(Res.font.barlow_condensed, weight = FontWeight.SemiBold),
        Font(Res.font.barlow_condensed, weight = FontWeight.Bold)
    )

    val s = scale.coerceIn(1.0f, 1.3f)
    val display = TextStyle(fontFamily = barlow, fontSize = (40 * s).sp, fontWeight = FontWeight.Bold, lineHeight = (46 * s).sp, letterSpacing = 0.sp)
    val title = TextStyle(fontFamily = anekBangla, fontSize = (24 * s).sp, fontWeight = FontWeight.SemiBold, lineHeight = (32.4 * s).sp, letterSpacing = 0.sp)
    val head = TextStyle(fontFamily = anekBangla, fontSize = (20 * s).sp, fontWeight = FontWeight.SemiBold, lineHeight = (28 * s).sp, letterSpacing = 0.sp)
    val body = TextStyle(fontFamily = anekBangla, fontSize = (16 * s).sp, fontWeight = FontWeight.Normal, lineHeight = (24.8 * s).sp, letterSpacing = 0.sp)
    val label = TextStyle(fontFamily = anekBangla, fontSize = (14 * s).sp, fontWeight = FontWeight.SemiBold, lineHeight = (21 * s).sp, letterSpacing = 0.sp)
    val caption = TextStyle(fontFamily = anekBangla, fontSize = (13 * s).sp, fontWeight = FontWeight.Normal, lineHeight = (19.5 * s).sp, letterSpacing = 0.sp)

    val materialTypography = Typography(
        displayLarge = display,
        titleLarge = title,
        titleMedium = head,
        bodyLarge = body,
        bodyMedium = label,
        bodySmall = caption,
        labelLarge = label,
        labelMedium = caption,
        labelSmall = caption.copy(fontSize = 12.sp)
    )

    val vaultTypography = VaultTypography(
        display = display,
        title = title,
        head = head,
        body = body,
        label = label,
        caption = caption
    )

    return materialTypography to vaultTypography
}

private fun vaultColorScheme(palette: VaultPalette, dark: Boolean) =
    (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        primaryContainer = palette.brandTint,
        onPrimaryContainer = palette.text,
        secondary = palette.accent,
        onSecondary = palette.onAccent,
        secondaryContainer = palette.brandTint,
        onSecondaryContainer = palette.text,
        tertiary = palette.ok,
        onTertiary = palette.onAccent,
        background = palette.canvas,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.surface2,
        onSurfaceVariant = palette.text2,
        surfaceContainer = palette.surface,
        surfaceContainerLow = palette.surface,
        surfaceContainerLowest = palette.canvas,
        surfaceContainerHigh = palette.surface2,
        surfaceContainerHighest = palette.surface2,
        outline = palette.lineStrong,
        outlineVariant = palette.line,
        error = palette.err,
        onError = palette.onAccent
    )

@Composable
internal fun VaultTheme(
    brandId: String,
    appearance: Appearance,
    reduceMotion: Boolean = false,
    textScale: Float = 1.0f,
    digits: Digits = Digits.BN,
    language: AppLanguage = AppLanguage.BN,
    haptics: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = appearance.usesDarkPalette(isSystemInDarkTheme())
    val effectiveReducedMotion = systemReducedMotion() || reduceMotion
    val palette = vaultPalette(brandId, dark)
    val density = LocalDensity.current
    val scaledDensity = remember(density, textScale) {
        Density(density.density, density.fontScale * textScale)
    }
    val (materialTypography, vaultTypography) = rememberBanglaTypography(1f)
    val platformHaptic = rememberPlatformHaptic(haptics)
    val skyColors = SkyColors(if (dark) SkyTokens.darkSemantic else SkyTokens.lightSemantic).with(
        "action" to palette.accent, "onAction" to palette.onAccent, "accent" to palette.accent,
        "textPrimary" to palette.text, "textSecondary" to palette.text2, "textMuted" to palette.text3,
        "canvas" to palette.canvas, "surface" to palette.surface, "input" to palette.surface2,
        "border" to palette.line, "fieldBorder" to palette.lineStrong
    )
    val skyType = defaultSkyTypography().let { baseType ->
        SkyTypography(baseType.roles.mapValues { (_, style) ->
            style.copy(fontFamily = materialTypography.bodyLarge.fontFamily, letterSpacing = 0.sp)
        })
    }

    SkyTheme(
        dark = dark,
        colors = skyColors,
        typography = skyType,
        motion = SkyMotionPolicy(reducedMotion = effectiveReducedMotion),
        brand = SkyBrand(gradientStart = palette.accent, gradientEnd = palette.accent)
    ) {
        MaterialTheme(colorScheme = vaultColorScheme(palette, dark), typography = materialTypography) {
            CompositionLocalProvider(
                LocalDensity provides scaledDensity,
                LocalVaultPalette provides palette,
                LocalVaultBrandId provides brandId,
                LocalVaultTypography provides vaultTypography,
                LocalReducedMotion provides effectiveReducedMotion,
                LocalVaultHaptic provides platformHaptic,
                LocalFmt provides Fmt(digits, language, appStringResource(Res.string.unit_km), appStringResource(Res.string.unit_litre), appStringResource(Res.string.unit_days)),
                LocalContentColor provides palette.text
            ) {
                content()
            }
        }
    }
}
