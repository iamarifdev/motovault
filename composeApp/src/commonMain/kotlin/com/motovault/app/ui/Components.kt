package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.datetime.LocalDate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sky.design.SkySvgIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ButtonKind {
    PRIMARY,
    SECONDARY,
    GHOST,
    DANGER
}

enum class ButtonSize {
    MD,
    SM
}

enum class IconTone {
    PLAIN,
    TINT
}

@Composable
fun VaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    size: ButtonSize = ButtonSize.MD,
    icon: String? = null,
    full: Boolean = false,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    val minHeight = if (size == ButtonSize.SM) 40.dp else 52.dp
    val hPad = if (size == ButtonSize.SM) 14.dp else 20.dp
    val corner = if (size == ButtonSize.SM) 10.dp else 12.dp

    val bgColor = when {
        !enabled -> SurfaceColor2
        kind == ButtonKind.PRIMARY -> BrandColor
        kind == ButtonKind.SECONDARY -> SurfaceColor
        kind == ButtonKind.GHOST -> Color.Transparent
        kind == ButtonKind.DANGER -> Color.Transparent
        else -> SurfaceColor2
    }

    val contentColor = when {
        !enabled -> TextTertiary
        kind == ButtonKind.PRIMARY -> OnBrandColor
        kind == ButtonKind.SECONDARY -> TextPrimary
        kind == ButtonKind.GHOST -> BrandColor
        kind == ButtonKind.DANGER -> ErrorColor
        else -> TextPrimary
    }

    val borderModifier = when {
        !enabled -> Modifier
        kind == ButtonKind.SECONDARY -> Modifier.border(1.5.dp, BorderStrongColor, RoundedCornerShape(corner))
        kind == ButtonKind.DANGER -> Modifier.border(1.5.dp, ErrorColor, RoundedCornerShape(corner))
        else -> Modifier
    }

    val widthModifier = if (full) Modifier.fillMaxWidth() else Modifier

    Box(
        modifier = modifier
            .then(widthModifier)
            .scale(scale)
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(corner))
            .background(bgColor)
            .then(borderModifier)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptic.perform(HapticKind.TICK)
                        onClick()
                    }
                )
            }
            .semantics {
                this.role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            }
            .padding(horizontal = hPad),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                SkySvgIcon(
                    path = "feather_icon/$icon",
                    description = null,
                    modifier = Modifier.size(if (size == ButtonSize.SM) 16.dp else 20.dp),
                    tint = contentColor
                )
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = if (size == ButtonSize.SM) 14.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VaultIconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    tone: IconTone = IconTone.PLAIN
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    val bgColor = if (tone == IconTone.TINT) BrandTintColor else Color.Transparent
    val iconColor = if (tone == IconTone.TINT) BrandColor else TextPrimary

    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptic.perform(HapticKind.TICK)
                        onClick()
                    }
                )
            }
            .semantics {
                role = Role.Button
                if (label.isNotEmpty()) contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        SkySvgIcon(
            path = "feather_icon/$icon",
            description = null,
            modifier = Modifier.size(22.dp),
            tint = iconColor
        )
    }
}

@Composable
fun VaultChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    hint: String? = null,
    icon: String? = null
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    val bgColor = if (active) BrandTintColor else SurfaceColor
    val borderColor = if (active) BrandColor else BorderColor
    val textColor = TextPrimary

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(999.dp))
            .pointerInput(active) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        haptic.perform(HapticKind.TICK)
                        onClick()
                    }
                )
            }
            .semantics { role = Role.Checkbox }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                SkySvgIcon(
                    path = "feather_icon/$icon",
                    description = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (active) BrandColor else TextSecondary
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (hint != null) {
                Text(
                    text = hint,
                    color = if (active) TextSecondary else TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun <T> VaultSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val haptic = currentHaptic
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, optLabel) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) SurfaceColor else Color.Transparent)
                    .then(if (isSelected) Modifier.border(0.5.dp, BorderColor, RoundedCornerShape(10.dp)) else Modifier)
                    .clickable {
                        if (!isSelected) {
                            haptic.perform(HapticKind.TICK)
                            onSelect(value)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optLabel,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun VaultSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = VaultMotion.pressSpring(reducedMotion)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.perform(HapticKind.TICK)
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (checked) BrandColor else BorderStrongColor)
                .padding(3.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        if (action != null) {
            action()
        }
    }
}

@Composable
fun CountUp(
    value: Double,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified
) {
    val reducedMotion = isReducedMotion
    val anim = remember { Animatable(if (reducedMotion) value.toFloat() else 0f) }

    LaunchedEffect(value, reducedMotion) {
        if (reducedMotion) {
            anim.snapTo(value.toFloat())
        } else {
            anim.animateTo(value.toFloat(), tween(800, easing = VaultMotion.SkyCurve))
        }
    }

    Text(
        text = format(anim.value.toDouble()),
        style = style,
        color = color,
        modifier = modifier
    )
}

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
    }

    val iconName = when (wear.status) {
        WearStatus.OK -> "fi_check-circle.svg"
        WearStatus.SOON -> "fi_clock.svg"
        WearStatus.OVERDUE -> "fi_alert-triangle.svg"
    }

    val text = if (wear.status == WearStatus.OVERDUE) WearCalculator.describeWear(wear, f)
               else WearCalculator.statusLabel(wear.status)
    val partName = wear.rule.name.substringBefore(" ও ").substringBefore(" পরিষ্কার")

    val targetSweep = (wear.life / 100f).coerceIn(0.02f, 1f)
    val animSweep = remember { Animatable(if (reducedMotion) targetSweep else 0f) }

    LaunchedEffect(wear.life, reducedMotion) {
        if (reducedMotion) {
            animSweep.snapTo(targetSweep)
        } else {
            delay((index * 70).toLong())
            animSweep.animateTo(targetSweep, tween(VaultMotion.SweepMs, easing = VaultMotion.SkyCurve))
        }
    }

    Column(
        modifier = modifier
            .width(124.dp)
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
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(84.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
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
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CountUp(
                    value = wear.life.toDouble(),
                    format = { n -> f.d(kotlin.math.round(n).toInt().toString()) },
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Text(
                    text = "%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, start = 1.dp)
                )
            }
        }

        Text(
            text = partName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkySvgIcon(
                path = "feather_icon/$iconName",
                description = null,
                modifier = Modifier.size(13.dp),
                tint = arcColor
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = arcColor,
                textAlign = TextAlign.Center,
                maxLines = 1
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

@Composable
fun ExpiryChip(
    doc: VaultDocument,
    modifier: Modifier = Modifier
) {
    val f = currentFmt
    val palette = LocalVaultPalette.current
    val days = doc.daysRemaining ?: 0

    val (icon, text, color) = when {
        days < 0 -> Triple("fi_alert-triangle.svg", "${f.days(-days)} আগে শেষ", palette.err)
        days <= 30 -> Triple("fi_clock.svg", "${f.days(days)} বাকি", palette.warn)
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
        SkySvgIcon(
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

    val height = if (large) 220.dp else 140.dp
    val pPad = if (large) 18.dp else 12.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(text = "নম্বর", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = doc.number, fontSize = if (large) 15.sp else 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(2.dp))
                    Text(text = "নাম", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    Text(text = doc.holder, fontSize = if (large) 14.sp else 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(text = "মেয়াদ", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    val expiryDate = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
                    Text(
                        text = if (expiryDate != null) f.dateFull(expiryDate) else doc.expiry,
                        fontSize = if (large) 14.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun VaultKeypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
    decimal: Boolean = true
) {
    val f = currentFmt
    val haptic = currentHaptic
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
                        var isPressed by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPressed) KeyDownColor else KeyColor)
                                .pointerInput(k) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            haptic.perform(HapticKind.TICK)
                                            tryAwaitRelease()
                                            isPressed = false
                                        },
                                        onTap = { onKey(k) }
                                    )
                                }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = when (k) {
                                        "back" -> "মুছুন"
                                        "." -> "দশমিক"
                                        else -> k
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (k == "back") {
                                SkySvgIcon(
                                    path = "feather_icon/fi_delete.svg",
                                    description = "মুছুন",
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        if (message != null) {
            val haptic = currentHaptic
            val progress = remember { Animatable(1f) }

            LaunchedEffect(message) {
                progress.snapTo(1f)
                progress.animateTo(0f, tween(5000))
                onDismiss()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF181C1A))
                    .border(0.5.dp, Color(0xFF353B44), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message,
                        color = Color(0xFFF1F2F3),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (onUndo != null) {
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.perform(HapticKind.WARNING)
                                    onUndo()
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SkySvgIcon(
                                path = "feather_icon/fi_rotate-ccw.svg",
                                description = null,
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFF8FB6FF)
                            )
                            Text(
                                text = "আনডু",
                                color = Color(0xFF8FB6FF),
                                fontSize = 14.sp,
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
                        .background(BrandColor)
                )
            }
        }
    }
}
