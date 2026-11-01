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
fun ExpiryChip(
    doc: VaultDocument,
    modifier: Modifier = Modifier,
    warningDays: Int = doc.reminderWarningDays ?: 30
) {
    val f = currentFmt
    val palette = LocalVaultPalette.current
    val days = doc.daysRemaining

    val (icon, text, color) = when {
        days == null -> Triple("fi_file-text.svg", if (doc.expiry.isBlank()) "মেয়াদ নেই" else "তারিখ যাচাই করুন", palette.text2)
        days < 0 -> Triple("fi_alert-triangle.svg", "${f.days(-days)} আগে শেষ", palette.err)
        days <= warningDays -> Triple("fi_clock.svg", "${f.days(days)} বাকি", palette.warn)
        else -> Triple("fi_check-circle.svg", "মেয়াদ আছে", palette.ok)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        V3Icon(
            path = "feather_icon/$icon",
            description = null,
            modifier = Modifier.size(13.dp),
            tint = color
        )
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DocArt(
    doc: VaultDocument,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val f = currentFmt
    val kind = doc.kind.ifBlank { docKindInfo(doc.title).kind }
    val (gradStart, gradEnd) = when (kind) {
        "registration" -> Color(0xFF1F6E55) to Color(0xFF2F8F77)
        "licence" -> Color(0xFF2C4F9C) to Color(0xFF4574C9)
        "tax" -> Color(0xFF8A5A1C) to Color(0xFFB8802F)
        "insurance" -> Color(0xFF5B3C8F) to Color(0xFF7D58B8)
        else -> Color(0xFF3E4A55) to Color(0xFF5D6B78)
    }

    val pPad = if (large) 18.dp else 12.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(gradStart, gradEnd)))
            .padding(pPad)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = docNameBn(kind),
                    fontSize = if (large) 16.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "নমুনা · ডেমো",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Microchip graphic
            Box(
                modifier = Modifier
                    .size(width = if (large) 48.dp else 38.dp, height = if (large) 34.dp else 26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFE7D49A), Color(0xFFB99B4C))))
            )

            val expiryDate = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
            val fields = listOf(
                "নম্বর" to doc.number,
                "নাম" to doc.holder,
                "মেয়াদ" to (expiryDate?.let(f::dateFull) ?: doc.expiry.ifBlank { "মেয়াদ নেই" })
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                fields.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.widthIn(min = 40.dp), fontSize = if (large) 12.sp else 10.sp,
                            lineHeight = if (large) 20.sp else 17.sp, color = Color.White.copy(alpha = 0.8f))
                        Text(value, Modifier.weight(1f), fontSize = if (large) 15.sp else 12.sp,
                            lineHeight = if (large) 20.sp else 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}
