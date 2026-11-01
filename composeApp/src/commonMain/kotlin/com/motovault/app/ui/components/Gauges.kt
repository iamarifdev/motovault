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
fun Gauge(
    wear: Wear,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val f = currentFmt
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val palette = LocalVaultPalette.current

    val arcColor = when (wear.status) {
        WearStatus.OK -> palette.ok
        WearStatus.SOON -> palette.warn
        WearStatus.OVERDUE -> palette.err
        WearStatus.UNKNOWN -> TextSecondary
    }

    val iconName = when (wear.status) {
        WearStatus.OK -> "fi_circle-check.svg"
        WearStatus.SOON -> "fi_timer.svg"
        WearStatus.OVERDUE -> "fi_triangle-alert.svg"
        WearStatus.UNKNOWN -> "fi_help-circle.svg"
    }

    val text = if (wear.status == WearStatus.OVERDUE) WearCalculator.describeWear(wear, f)
               else WearCalculator.statusLabel(wear.status)
    val partName = wear.rule.name.substringBefore(" ও ").substringBefore(" পরিষ্কার")

    val targetSweep = if (wear.status == WearStatus.UNKNOWN) 0f else (wear.life / 100f).coerceIn(0.02f, 1f)
    val animSweep = remember { Animatable(if (reducedMotion) targetSweep else 0f) }

    LaunchedEffect(wear.life, wear.status, reducedMotion) {
        if (reducedMotion || wear.status == WearStatus.UNKNOWN) {
            animSweep.snapTo(targetSweep)
        } else {
            delay((index * 70).toLong())
            animSweep.animateTo(targetSweep, tween(VaultMotion.SweepMs, easing = VaultMotion.SkyCurve))
        }
    }

    Column(
        modifier = modifier
            .width(124.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$partName, " + if (wear.status == WearStatus.UNKNOWN) "আগের সার্ভিস নিশ্চিত নয়" else "সার্ভিসের ব্যবধান ${f.n(wear.life.toLong())} শতাংশ বাকি, $text"
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (wear.status == WearStatus.OVERDUE) palette.err.copy(alpha = 0.07f) else SurfaceColor)
            .border(
                1.dp,
                if (wear.status == WearStatus.OVERDUE) palette.err.copy(alpha = 0.35f) else BorderColor,
                RoundedCornerShape(16.dp)
            )
            .clickable {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(start = 8.dp, top = 14.dp, end = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(84.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.minDimension * 6f / 80f
                val radius = size.minDimension * 32f / 80f
                val center = Offset(size.width / 2, size.height / 2)

                drawCircle(
                    color = palette.line,
                    radius = radius,
                    center = center,
                    style = Stroke(strokeWidth)
                )

                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = animSweep.value * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }

            if (wear.status == WearStatus.UNKNOWN) Text("?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                CountUp(
                    value = wear.life.toDouble(),
                    format = { n -> f.d(kotlin.math.round(n).toInt().toString()) },
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Text(
                    text = "%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, start = 1.dp)
                )
            }
        }

        Text(
            text = partName,
            fontSize = 14.sp,
            lineHeight = 18.9.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = Int.MAX_VALUE
        )

        Text("সার্ভিসের ব্যবধান বাকি", fontSize = 13.333.sp, lineHeight = 20.667.sp, color = TextPrimary, textAlign = TextAlign.Center)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            V3Icon(
                path = "feather_icon/$iconName",
                description = null,
                modifier = Modifier.size(14.dp),
                tint = arcColor,
                strokeWidth = 2.2f
            )
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 15.6.sp,
                fontWeight = FontWeight.SemiBold,
                color = arcColor,
                textAlign = TextAlign.Center,
                maxLines = Int.MAX_VALUE
            )
        }
    }
}

@Composable
fun Spark(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = BrandColor
) {
    if (values.size < 2) return
    val reducedMotion = isReducedMotion
    val animProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(values) {
        if (!reducedMotion) {
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, tween(VaultMotion.SweepMs, easing = VaultMotion.SkyCurve))
        }
    }

    Canvas(modifier = modifier.size(width = 72.dp, height = 26.dp)) {
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val span = maxOf(0.5, max - min)
        val w = size.width
        val h = size.height

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = (i.toFloat() / (values.size - 1)) * w
            val y = (h - 3.dp.toPx() - ((v - min) / span).toFloat() * (h - 6.dp.toPx()))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
