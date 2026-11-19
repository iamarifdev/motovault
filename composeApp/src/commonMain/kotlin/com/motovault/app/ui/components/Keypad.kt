package com.motovault.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.keypad_backspace
import motovault.composeapp.generated.resources.keypad_decimal

@Composable
fun VaultKeypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
    compact: Boolean = LocalSheetCompact.current
) {
    val keyHeight = if (compact) 46.dp else 52.dp
    val keyGap = if (compact) 6.dp else 8.dp
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        if (decimal) "." else "", "0", "back"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(keyGap)) {
        keys.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(keyGap)) {
                row.forEach { key ->
                    if (key.isEmpty()) {
                        Spacer(Modifier.weight(1f).height(keyHeight))
                    } else {
                        KeypadButton(
                            key = key,
                            height = keyHeight,
                            onKey = onKey,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    height: Dp,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val f = currentFmt
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val backspaceLabel = appStringResource(Res.string.keypad_backspace)
    val decimalLabel = appStringResource(Res.string.keypad_decimal)
    var pressed by remember(key) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) KeyDownColor else KeyColor)
            .pointerInput(key, onKey) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    haptic.perform(HapticKind.TICK)
                    onKey(key)
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
            .semantics {
                role = Role.Button
                when (key) {
                    "back" -> contentDescription = backspaceLabel
                    "." -> contentDescription = decimalLabel
                }
                onClick {
                    haptic.perform(HapticKind.TICK)
                    onKey(key)
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "back" -> V3Icon(
                path = "feather_icon/fi_delete.svg",
                description = null,
                modifier = Modifier.size(24.dp),
                tint = TextSecondary
            )
            "." -> Text(".", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            else -> Text(f.d(key), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

class NumberEntryState(
    initialValues: Map<String, String>,
    firstField: String
) {
    var activeField by mutableStateOf(firstField)
    val values = mutableStateMapOf<String, String>().apply { putAll(initialValues) }
    private val fresh = mutableSetOf(firstField)

    fun setActive(field: String) {
        fresh.add(field)
        activeField = field
    }

    fun set(field: String, value: String) {
        fresh.remove(field)
        values[field] = value
    }

    fun press(key: String) {
        val field = activeField
        val replace = fresh.contains(field)
        fresh.remove(field)
        val cur = if (replace) "" else (values[field] ?: "")
        var next = cur
        when {
            key == "back" -> next = if (replace) "" else cur.dropLast(1)
            key == "." -> next = if (cur.contains(".")) cur else if (cur.isEmpty()) "0." else "$cur."
            key.matches(Regex("""\d""")) -> {
                val frac = cur.substringAfter('.', "")
                next = if (cur.length >= 9 || (cur.contains('.') && frac.length >= 2)) cur
                else if (cur == "0") key else cur + key
            }
        }
        values[field] = next
    }
}

@Composable
fun rememberNumberEntry(
    initialValues: Map<String, String>,
    firstField: String
): NumberEntryState {
    return remember { NumberEntryState(initialValues, firstField) }
}
