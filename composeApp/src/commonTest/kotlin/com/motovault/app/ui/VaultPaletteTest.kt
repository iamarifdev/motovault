package com.motovault.app.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultPaletteTest {
    @Test
    fun colorMixBlendsAndClampsWeights() {
        val red = Color(1f, 0f, 0f, 1f)
        val blue = Color(0f, 0f, 1f, 1f)

        assertColorClose(blue, colorMix(red, blue, 0f))
        assertColorClose(red, colorMix(red, blue, 1f))
        assertColorClose(Color(0.5f, 0f, 0.5f, 1f), colorMix(red, blue, 0.5f))
        assertColorClose(blue, colorMix(red, blue, -0.5f))
        assertColorClose(red, colorMix(red, blue, 1.5f))
        assertColorClose(Color(1f, 0f, 0f, 0.25f), colorMixTransparent(red, 0.25f))
        assertColorClose(Color(1f, 0f, 0f, 0f), colorMixTransparent(red, -1f))
        assertColorClose(red, colorMixTransparent(red, 2f))
    }

    @Test
    fun allBrandsExposeV4TokensAndReadableStatusBadges() {
        BRAND_COLOR_MAP.forEach { (brandId, brandColors) ->
            listOf(false, true).forEach { dark ->
                val p = vaultPalette(brandId, dark)
                assertEquals(Color(if (dark) brandColors.dark else brandColors.light), p.accent)
                assertEquals(if (dark) Color(0xFF101215) else Color.White, p.onAccent)
                assertColorClose(Color.Black.copy(alpha = 0.60f), p.scrim)
                assertEquals(if (dark) Color(0xFF2F3833) else p.surface, p.segPill)
                assertColorClose(colorMix(p.accent, p.surface, 0.11f), p.brandTint)
                assertColorClose(colorMix(p.accent, p.surface, 0.45f), p.brandLine)
                assertColorClose(colorMix(p.accent, p.canvas, if (dark) 0.30f else 0.18f), p.wash)
                assertColorClose(colorMix(p.accent, p.surface, if (dark) 0.24f else 0.16f), p.sheetWash)
                if (dark) {
                    assertEquals(Color(0xFF86EFAC), p.ok)
                    assertEquals(Color(0xFFFCD34D), p.warn)
                    assertEquals(Color(0xFFFCA5A5), p.err)
                }
                StatusTone.entries.forEach { tone ->
                    val ratio = contrast(p.statusBadgeFg(tone), p.statusBadgeBg(tone))
                    assertTrue(ratio >= 4.5, "$brandId dark=$dark tone=$tone contrast=$ratio")
                }
            }
        }
    }

    private fun assertColorClose(expected: Color, actual: Color) {
        assertTrue(abs(expected.red - actual.red) < 1e-4f, "red: $expected vs $actual")
        assertTrue(abs(expected.green - actual.green) < 1e-4f, "green: $expected vs $actual")
        assertTrue(abs(expected.blue - actual.blue) < 1e-4f, "blue: $expected vs $actual")
        assertTrue(abs(expected.alpha - actual.alpha) < 1e-4f, "alpha: $expected vs $actual")
    }

    private fun contrast(a: Color, b: Color): Double {
        fun luminance(c: Color): Double {
            fun ch(v: Float): Double = if (v <= .04045f) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
            return .2126 * ch(c.red) + .7152 * ch(c.green) + .0722 * ch(c.blue)
        }
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (maxOf(l1, l2) + .05) / (minOf(l1, l2) + .05)
    }
}
