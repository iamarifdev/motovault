package com.motovault.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sky.design.*
import dev.sky.design.SkySvgIcon
import dev.sky.design.tokens.SkyTokens
import kotlinx.serialization.Serializable
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.anek_bangla
import motovault.composeapp.generated.resources.barlow_condensed
import org.jetbrains.compose.resources.Font

@Serializable
enum class Appearance(val label: String) {
    SYSTEM("ডিভাইসের মতো"),
    LIGHT("লাইট"),
    DARK("ডার্ক")
}

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
    val photoWellEnd: Color
) {
    val raised: Color get() = surface2
    val outline: Color get() = lineStrong
    val muted: Color get() = text2
    val healthy: Color get() = ok
    val warning: Color get() = warn
    val error: Color get() = err
}

fun vaultPalette(brandId: String, dark: Boolean): VaultPalette {
    val brandColors = BRAND_COLOR_MAP[brandId] ?: BRAND_COLOR_MAP["ktm"]!!
    val accent = Color(if (dark) brandColors.dark else brandColors.light)
    val onAccent = if (dark) Color(0xFF101215) else Color.White
    val surface = if (dark) Color(0xFF202723) else Color.White
    val brandTint = accent.copy(alpha = if (dark) 0.18f else 0.11f).compositeOver(surface)
    val brandLine = accent.copy(alpha = if (dark) 0.50f else 0.40f).compositeOver(surface)

    return if (dark) {
        VaultPalette(
            dark = true,
            canvas = Color(0xFF101411),
            surface = Color(0xFF202723),
            surface2 = Color(0xFF181C1A),
            text = Color(0xFFF1F2F3),
            text2 = Color(0xFFC4CAD0),
            text3 = Color(0xFF9DA5AF),
            line = Color(0xFF2F3833),
            lineStrong = Color(0xFF78828F),
            ok = Color(0xFF7BD3AB),
            warn = Color(0xFFEFC277),
            err = Color(0xFFFF929A),
            accent = accent,
            onAccent = onAccent,
            brandTint = brandTint,
            brandLine = brandLine,
            key = Color(0xFF2A332E),
            keyDown = Color(0xFF38443D),
            photoWellStart = accent.copy(alpha = 0.22f).compositeOver(Color(0xFF202723)),
            photoWellEnd = Color(0xFF181C1A)
        )
    } else {
        VaultPalette(
            dark = false,
            canvas = Color.White,
            surface = Color.White,
            surface2 = Color(0xFFF3F5F4),
            text = Color(0xFF181818),
            text2 = Color(0xFF595959),
            text3 = Color(0xFF66707C),
            line = Color(0xFFE3E6E4),
            lineStrong = Color(0xFF737E8B),
            ok = Color(0xFF236D4F),
            warn = Color(0xFF805817),
            err = Color(0xFFAD2938),
            accent = accent,
            onAccent = onAccent,
            brandTint = brandTint,
            brandLine = brandLine,
            key = Color(0xFFF3F5F4),
            keyDown = Color(0xFFDFE4E1),
            photoWellStart = accent.copy(alpha = 0.14f).compositeOver(Color.White),
            photoWellEnd = Color(0xFFF3F5F4)
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

@Composable
expect fun PlatformAppearance(dark: Boolean)

internal expect fun sheetDialogProperties(): androidx.compose.ui.window.DialogProperties

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
    val display = TextStyle(fontFamily = barlow, fontSize = (38 * s).sp, fontWeight = FontWeight.Bold, lineHeight = (42 * s).sp, letterSpacing = 0.sp)
    val title = TextStyle(fontFamily = anekBangla, fontSize = (24 * s).sp, fontWeight = FontWeight.SemiBold, lineHeight = (30 * s).sp, letterSpacing = 0.sp)
    val head = TextStyle(fontFamily = anekBangla, fontSize = (20 * s).sp, fontWeight = FontWeight.SemiBold, lineHeight = (26 * s).sp, letterSpacing = 0.sp)
    val body = TextStyle(fontFamily = anekBangla, fontSize = (16 * s).sp, fontWeight = FontWeight.Normal, lineHeight = (24 * s).sp, letterSpacing = 0.sp)
    val label = TextStyle(fontFamily = anekBangla, fontSize = (14 * s).sp, fontWeight = FontWeight.SemiBold, lineHeight = (20 * s).sp, letterSpacing = 0.sp)
    val caption = TextStyle(fontFamily = anekBangla, fontSize = (13 * s).sp, fontWeight = FontWeight.Normal, lineHeight = (18 * s).sp, letterSpacing = 0.sp)

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

@Composable
internal fun VaultTheme(
    brandId: String,
    appearance: Appearance,
    reduceMotion: Boolean = false,
    textScale: Float = 1.0f,
    digits: Digits = Digits.BN,
    content: @Composable () -> Unit
) {
    val dark = appearance.usesDarkPalette(isSystemInDarkTheme())
    val palette = vaultPalette(brandId, dark)
    val (materialTypography, vaultTypography) = rememberBanglaTypography(textScale)

    val baseScheme = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = baseScheme.copy(
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

    val skyColors = SkyColors(if (dark) SkyTokens.darkSemantic else SkyTokens.lightSemantic).with(
        "action" to palette.accent,
        "onAction" to palette.onAccent,
        "accent" to palette.accent,
        "textPrimary" to palette.text,
        "textSecondary" to palette.text2,
        "textMuted" to palette.text3,
        "canvas" to palette.canvas,
        "surface" to palette.surface,
        "input" to palette.surface2,
        "border" to palette.line,
        "fieldBorder" to palette.lineStrong
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
        motion = SkyMotionPolicy(reducedMotion = reduceMotion),
        brand = SkyBrand(gradientStart = palette.accent, gradientEnd = palette.accent) // Solid brand ink for headings
    ) {
        MaterialTheme(colorScheme = scheme, typography = materialTypography) {
            CompositionLocalProvider(
                LocalVaultPalette provides palette,
                LocalVaultBrandId provides brandId,
                LocalVaultTypography provides vaultTypography,
                LocalReducedMotion provides reduceMotion,
                LocalFmt provides Fmt(digits),
                LocalContentColor provides palette.text
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun AppearanceButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        SkySvgIcon("feather_icon/fi_sun.svg", "থিম বা অবস্থান বদলান", Modifier.size(20.dp), LocalVaultPalette.current.text2)
    }
}

