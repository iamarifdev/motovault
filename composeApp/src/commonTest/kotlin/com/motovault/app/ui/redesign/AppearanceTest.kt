package com.motovault.app.ui.redesign

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.*

class AppearanceTest {
    @Test fun systemIsTheDefaultForNewAndExistingRecords() {
        assertEquals(Appearance.SYSTEM, VaultState().appearance)
        assertEquals(Appearance.SYSTEM, VaultState.decode("{}")!!.appearance)
        Appearance.entries.forEach { choice ->
            assertEquals(choice, VaultState.decode(VaultState.encode(VaultState(appearance = choice)))!!.appearance)
        }
    }

    @Test fun systemTracksTheDeviceAndOverridesRemainStable() {
        assertFalse(Appearance.SYSTEM.usesDarkPalette(false))
        assertTrue(Appearance.SYSTEM.usesDarkPalette(true))
        assertFalse(Appearance.LIGHT.usesDarkPalette(true))
        assertTrue(Appearance.DARK.usesDarkPalette(false))
    }

    @Test fun everyBrandHasReadableColorsInBothModes() {
        listOf("ktm", "cfmoto", "yamaha", "honda", "suzuki", "bajaj", "tvs", "hero", "enfield").forEach { brand ->
            listOf(false, true).forEach { dark ->
                val p = vaultPalette(brand, dark)
                listOf(p.canvas, p.surface, p.raised).forEach { background ->
                    listOf(p.text, p.muted, p.accent, p.healthy, p.warning, p.error).forEach { text ->
                        assertTrue(contrast(text, background) >= 4.5, "$brand dark=$dark contrast=${contrast(text, background)} for $text on $background")
                    }
                }
                assertTrue(contrast(p.onAccent, p.accent) >= 4.5, "$brand button label")
                assertTrue(contrast(p.outline, p.surface) >= 3.0, "$brand control outline")
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        fun luminance(c: Color): Double {
            fun channel(v: Float): Double = if (v <= .04045f) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
            return .2126 * channel(c.red) + .7152 * channel(c.green) + .0722 * channel(c.blue)
        }
        val first = luminance(a)
        val second = luminance(b)
        return (maxOf(first, second) + .05) / (minOf(first, second) + .05)
    }
}
