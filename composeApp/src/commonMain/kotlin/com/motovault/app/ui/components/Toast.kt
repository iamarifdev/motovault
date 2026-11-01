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


data class ToastData(
    val id: String = "",
    val message: String,
    val undoAction: (() -> Unit)? = null
)

@Composable
fun ToastLayer(
    toast: ToastData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ToastLayer(
        message = toast?.message,
        onUndo = toast?.undoAction,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
fun ToastLayer(
    message: String?,
    onUndo: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)
    ) {
        if (message != null) {
            val haptic = currentHaptic
            val dark = LocalVaultPalette.current.dark
            val toastBackground = if (dark) Color(0xFFE9ECEA) else Color(0xFF181C1A)
            val toastText = if (dark) Color(0xFF181818) else Color(0xFFF1F2F3)
            val undoColor = if (dark) Color(0xFF1E47A8) else Color(0xFF8FB6FF)
            val progress = remember { Animatable(1f) }

            LaunchedEffect(message) {
                progress.snapTo(1f)
                progress.animateTo(0f, tween(5000))
                onDismiss()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(toastBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = message,
                        color = toastText,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (onUndo != null) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.perform(HapticKind.WARNING)
                                    onUndo()
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            V3Icon(
                                path = "feather_icon/fi_undo-2.svg",
                                description = null,
                                modifier = Modifier.size(16.dp),
                                tint = undoColor
                            )
                            Text(
                                text = appStringResource(Res.string.action_undo),
                                color = undoColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // Progress bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.value)
                        .height(3.dp)
                        .background(toastText.copy(alpha = 0.4f))
                )
            }
        }
    }
}
