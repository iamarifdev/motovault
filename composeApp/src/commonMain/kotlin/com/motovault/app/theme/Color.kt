package com.motovault.app.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Rivian & Bosch Obsidian Canvas & Surface Colors
val CanvasTftDark = Color(0xFF07080B)
val SurfaceCardDark = Color(0xFF12151D)
val SurfaceCardGradientStart = Color(0xFF161922)
val SurfaceCardGradientEnd = Color(0xFF111319)
val SurfaceElevatedDark = Color(0xFF1A1D26)
val GlassSurfaceDark = Color(0xBF12151C)

// Specular Hairline Borders
val BorderSubtleDark = Color(0x1AFFFFFF) // 10% white
val BorderSpecularDark = Color(0x14FFFFFF) // 8% white hairline
val BorderSpecularHighlight = Color(0x26FFFFFF) // 15% top highlight

// Ambient Halos
val HaloGlowOrange = Color(0x1FFF6600)
val HaloGlowGreen = Color(0x1F10B981)

// Brand Dynamic Accent Colors
val KtmApexOrange = Color(0xFFFF6600)
val YamahaRacingBlue = Color(0xFF0066FF)
val KawasakiLimeGreen = Color(0xFF00E676)
val EvCyberTeal = Color(0xFF00F2FE)
val ReHeritageAmber = Color(0xFFD4A373)

// Universal Status Colors
val StatusHealthyGreen = Color(0xFF10B981)
val StatusWarningYellow = Color(0xFFF59E0B)
val StatusOverdueRed = Color(0xFFEF4444)

// Text Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Bangladeshi Document Gradients
val BrtaRegStart = Color(0xFF0B3323)
val BrtaRegMid = Color(0xFF144C35)
val BrtaRegEnd = Color(0xFF082217)

val LicenseBlueStart = Color(0xFF132A4A)
val LicenseBlueMid = Color(0xFF1A3D6B)
val LicenseBlueEnd = Color(0xFF0C1E36)

val TaxTokenAmberStart = Color(0xFF3B2A0F)
val TaxTokenAmberMid = Color(0xFF543B14)
val TaxTokenAmberEnd = Color(0xFF241A09)

// Brushes
val SpecularCardBrush = Brush.verticalGradient(
    colors = listOf(SurfaceCardGradientStart, SurfaceCardGradientEnd)
)

