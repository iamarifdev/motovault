package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.datetime.LocalDate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.keypad_backspace
import motovault.composeapp.generated.resources.keypad_decimal
import motovault.composeapp.generated.resources.action_undo

private val VaultPosition = SemanticsPropertyKey<Rect>("VaultPosition")

/** Keep the native accessibility bridge informed when a button moves inside a scroll container. */

@Composable
fun VaultKeypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = true
) {
    val f = currentFmt
    val haptic = currentHaptic
    val backspaceLabel = appStringResource(Res.string.keypad_backspace)
    val decimalLabel = appStringResource(Res.string.keypad_decimal)
    val keys = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        if (decimal) "." else "", "0", "back"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { k ->
                    if (k.isEmpty()) {
                        Spacer(Modifier.weight(1f).height(52.dp))
                    } else {
                        val interaction = remember(k) { MutableInteractionSource() }
                        val isPressed by interaction.collectIsPressedAsState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPressed) KeyDownColor else KeyColor)
                                .clickable(role = Role.Button, interactionSource = interaction, indication = null) {
                                    haptic.perform(HapticKind.TICK)
                                    onKey(k)
                                }
                                .semantics {
                                    role = Role.Button
                                    when (k) {
                                        "back" -> contentDescription = backspaceLabel
                                        "." -> contentDescription = decimalLabel
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (k == "back") {
                                V3Icon(
                                    path = "feather_icon/fi_delete.svg",
                                    description = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = TextSecondary
                                )
                            } else if (k == ".") {
                                Text(".", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            } else {
                                Text(f.d(k), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
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
