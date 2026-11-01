package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun GarageScreen(
    state: VaultState,
    onOpenSheet: (String, Any?) -> Unit,
    onSwitchTab: (Tab) -> Unit,
    onOpenServiceForItem: (String) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    val ride = state.ride ?: return
    val wears = remember(state.rules, state.logs, ride.odometer, today) {
        WearCalculator.byUrgency(state.confirmedRules(), ride.odometer, today.toEpochDays().toLong())
    }
    val topWear = wears.firstOrNull()
    val economy = remember(state.logs) { WearCalculator.economyOf(state.logs) }
    val series = remember(state.logs) { WearCalculator.economySeries(state.logs) }
    val estimate = remember(state.logs, ride, today) { WearCalculator.estimateOdometer(ride, state.logs, today) }

    val lastFuel = state.logs.filter { it.isFuel }.maxByOrNull { it.odometer }
    val sinceFuelDays = lastFuel?.let {
        runCatching { today.toEpochDays() - LocalDate.parse(it.date).toEpochDays() }.getOrNull()
    }
    var nudgeHidden by remember { mutableStateOf(false) }

    val renewals = remember(state.documents, state.reminderPolicy, today) {
        state.documents.mapNotNull { doc ->
            val days = doc.daysRemaining
            if (days != null && days <= (doc.reminderWarningDays ?: state.reminderPolicy.warningDays)) doc to days else null
        }.sortedBy { it.second }
    }

    val model = ride.selectedModel
    val brandWorld = worlds.firstOrNull { it.id == ride.brandId } ?: worlds.first()

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Home Header
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "আমার গ্যারেজ", fontSize = 13.sp, color = TextSecondary)
                Text(
                    text = ride.displayName,
                    fontFamily = FontFamily(Font(Res.font.barlow_condensed)),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 37.8.sp
                )
                Text(text = "${brandWorld.name} · ${f.d(ride.year.toString())}", fontSize = 13.sp, color = TextSecondary)
            }
            // Avatar Button to Bike Settings / Me tab
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd)))
                    .border(1.5.dp, BrandLineColor, CircleShape)
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onSwitchTab(Tab.ME)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (model != null) {
                    Image(
                        painter = painterResource(model.photo),
                        contentDescription = "বাইকের সেটিংস",
                        modifier = Modifier.requiredSize(66.08.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    V3Icon(path = "feather_icon/fi_user.svg", description = "বাইকের সেটিংস", modifier = Modifier.size(24.dp), tint = BrandColor)
                }
            }
        }

        // Hero "Next Up" Card
        if (topWear != null) {
            val palette = LocalVaultPalette.current
            val (statusBg, statusBorder, statusColor, statusLabel) = when (topWear.status) {
                WearStatus.OVERDUE -> Quad(palette.err.copy(alpha = 0.09f), palette.err.copy(alpha = 0.4f), palette.err, "এখনই দরকার")
                WearStatus.SOON -> Quad(palette.warn.copy(alpha = 0.09f), palette.warn.copy(alpha = 0.4f), palette.warn, "শিগগির দরকার")
                WearStatus.OK -> Quad(palette.ok.copy(alpha = 0.08f), palette.ok.copy(alpha = 0.35f), palette.ok, "ব্যবধান বাকি · পরের কাজ")
                WearStatus.UNKNOWN -> Quad(palette.surface, palette.line, palette.text2, "সার্ভিসের তথ্য নিশ্চিত করুন")
            }
            val statusIcon = when (topWear.status) {
                WearStatus.OVERDUE -> "fi_triangle-alert.svg"
                WearStatus.SOON -> "fi_timer.svg"
                WearStatus.OK -> "fi_circle-check.svg"
                WearStatus.UNKNOWN -> "fi_triangle-alert.svg"
            }
            val topItem = catalogItemOf(topWear.rule.id) ?: catalogItemOf(topWear.rule.name.lowercase())
            val partTitle = topWear.rule.name.substringBefore(" ও ").substringBefore(" পরিষ্কার")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusBg)
                    .border(1.5.dp, statusBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(statusColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        V3Icon(path = "feather_icon/$statusIcon", description = null, modifier = Modifier.size(26.dp), tint = statusColor, strokeWidth = 2.2f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = statusLabel, fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
                        Text(text = partTitle, fontSize = 22.sp, lineHeight = 29.7.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(text = WearCalculator.describeWear(topWear, f), fontSize = 16.sp, lineHeight = 24.8.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }

                // Meter progress bar
                if (topWear.status != WearStatus.UNKNOWN) Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(topWear.used.toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                    )
                }

                val lastDateParsed = runCatching { LocalDate.fromEpochDays(topWear.rule.lastServicedEpochDays.toInt()) }.getOrNull()
                val lastDateStr = if (lastDateParsed != null) f.date(lastDateParsed) else ""
                Text(
                    text = if (topWear.status == WearStatus.UNKNOWN) "অজানা তথ্য থেকে শতাংশ দেখানো হয় না" else "শেষ নিশ্চিত সার্ভিস: $lastDateStr · ${f.km(topWear.rule.lastServicedOdometerKm)}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                VaultButton(
                    text = "${topItem?.short ?: "সার্ভিস"} লিখুন",
                    icon = "fi_tool.svg",
                    full = true,
                    onClick = {
                        haptic.perform(HapticKind.TICK)
                        onOpenServiceForItem(topWear.rule.id)
                    }
                )
            }
        }

        // Quick Action Row (4 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val quickActions = listOf(
                Triple("fi_fuel.svg", "তেল ভরা", "fuel"),
                Triple("fi_tool.svg", "সার্ভিস", "service"),
                Triple("fi_gauge.svg", "কিমি", "odometer"),
                Triple("fi_file-text.svg", "কাগজ", "docAdd")
            )
            quickActions.forEach { (icon, label, sheet) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor2)
                        .clickable {
                            haptic.perform(HapticKind.TICK)
                            onOpenSheet(sheet, null)
                        }
                         .padding(start = 4.dp, top = 12.dp, end = 4.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandTintColor)
                            .border(1.dp, BrandLineColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        V3Icon(path = "feather_icon/$icon", description = null, modifier = Modifier.size(26.dp), tint = BrandColor)
                    }
                    Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }

        // Nudge banner (if >= 10 days since fuel)
        if (!nudgeHidden && sinceFuelDays != null && sinceFuelDays >= 10) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandTintColor)
                    .border(1.dp, BrandLineColor, RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                V3Icon(path = "feather_icon/fi_droplets.svg", description = null, modifier = Modifier.size(20.dp), tint = BrandColor)
                Text(
                    text = "${f.d(sinceFuelDays.toString())} দিন ধরে তেলের হিসাব নেই।",
                    fontSize = 14.sp,
                    lineHeight = 19.6.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                VaultButton(
                    text = "যোগ করুন",
                    size = ButtonSize.SM,
                    kind = ButtonKind.SECONDARY,
                    onClick = { onOpenSheet("fuel", null) }
                )
                VaultIconButton(
                    icon = "fi_x.svg",
                    label = "লুকান",
                    onClick = { nudgeHidden = true }
                )
            }
        }

        // Stats Row (Odometer & Fuel economy)
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Odometer card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onOpenSheet("odometer", null)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = "ওডোমিটার", fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CountUp(
                        value = ride.odometer,
                        format = { n -> f.n(n.toLong()) },
                        modifier = Modifier.alignByBaseline(),
                        style = TextStyle(fontSize = 30.sp, lineHeight = 37.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = if (f.digits == Digits.BN) "কিমি" else "km",
                        fontSize = 13.125.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Text(
                    text = "আজ আনুমানিক ${f.n(estimate.toLong())} · আপডেট",
                    fontSize = 12.sp,
                    lineHeight = 18.6.sp,
                    color = TextSecondary,
                    modifier = Modifier.heightIn(min = 26.dp).wrapContentHeight(Alignment.CenterVertically)
                )
                val confirmedDate = ride.odometerConfirmedDate?.let { runCatching { f.date(LocalDate.parse(it)) }.getOrNull() }
                Text(text = "শেষ নিশ্চিত: ${confirmedDate ?: "তারিখ জানা নেই"}", fontSize = 12.sp, lineHeight = 18.6.sp, color = TextSecondary,
                    modifier = Modifier.heightIn(min = 26.dp).wrapContentHeight(Alignment.CenterVertically))
            }

            // Economy card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onSwitchTab(Tab.HISTORY)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = "জ্বালানি গড়", fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (economy != null) {
                        CountUp(
                            value = economy,
                            format = { n -> f.d(formatLakh(n, 1)) },
                            modifier = Modifier.alignByBaseline(),
                            style = TextStyle(fontSize = 30.sp, lineHeight = 37.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    } else {
                        Text(text = "--", modifier = Modifier.alignByBaseline(), fontSize = 30.sp, lineHeight = 37.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(
                        text = if (f.digits == Digits.BN) "কিমি/লি" else "km/L",
                        fontSize = 13.125.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) {
                    Spark(values = series.takeLast(6))
                }
            }
        }

        // Gauges Section
        Column {
        SectionTitle("সার্ভিসের ব্যবধান বাকি")
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                .horizontalScroll(rememberScrollState()).padding(top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            wears.forEachIndexed { i, wear ->
                Gauge(
                    wear = wear,
                    index = i,
                    modifier = Modifier.fillMaxHeight(),
                    onClick = {
                        haptic.perform(HapticKind.TICK)
                        onOpenSheet("gaugeDetail", wear.rule.id)
                    }
                )
            }
        }
        }

        // Renewals Section (if any within 30 days)
        if (renewals.isNotEmpty()) {
            Column {
            SectionTitle("কাগজের মেয়াদ")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                renewals.forEach { (doc, leftDays) ->
                    val isExpired = leftDays < 0
                    val (textColor, borderColor, bg) = if (isExpired) {
                        Triple(ErrorColor, ErrorColor.copy(alpha = 0.4f), ErrorColor.copy(alpha = 0.07f))
                    } else {
                        Triple(WarningColor, WarningColor.copy(alpha = 0.4f), SurfaceColor)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.perform(HapticKind.TICK)
                                onSwitchTab(Tab.VAULT)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            V3Icon(
                                path = if (isExpired) "feather_icon/fi_triangle-alert.svg" else "feather_icon/fi_timer.svg",
                                description = null,
                                modifier = Modifier.size(20.dp),
                                tint = textColor
                            )
                            Column(Modifier.weight(1f)) {
                                Text(text = doc.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    text = if (isExpired) "${f.days(-leftDays)} আগে মেয়াদ শেষ" else "মেয়াদ শেষ হতে ${f.days(leftDays)}",
                                    fontSize = 13.sp,
                                    color = textColor
                                )
                            }
                        }
                        V3Icon(path = "feather_icon/fi_chevron-right.svg", description = null, modifier = Modifier.size(20.dp), tint = TextTertiary)
                    }
                }
            }
            }
        }

        // Bike Card
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd)))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (model != null) {
                    Image(
                        painter = painterResource(model.photo),
                        contentDescription = "${brandWorld.name} ${ride.displayName}",
                        modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 340.dp).fillMaxHeight(0.9f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(64.dp), tint = BrandColor)
                }
            }
            Text(
                text = "নমুনা ছবি · রঙ ও যন্ত্রাংশে পার্থক্য থাকতে পারে",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
