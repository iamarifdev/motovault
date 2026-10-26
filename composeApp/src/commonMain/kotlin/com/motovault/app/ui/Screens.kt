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
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.barlow_condensed
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

// ==========================================
// 1. GARAGE (HOME) SCREEN
// ==========================================
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

// ==========================================
// 2. VAULT (DOCUMENTS) SCREEN
// ==========================================
@Composable
fun VaultScreen(
    state: VaultState,
    onOpenAddDoc: () -> Unit,
    onOpenDocView: (VaultDocument) -> Unit,
    onToggleDocReminders: (Boolean) -> Unit,
    onReminderSettings: () -> Unit = {}
) {
    val f = currentFmt
    val haptic = currentHaptic
    val sortedDocs = remember(state.documents) {
        state.documents.sortedBy { it.daysRemaining ?: Int.MAX_VALUE }
    }
    val nextRenewal = sortedDocs.firstOrNull { it.daysRemaining != null }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Page Header
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "কাগজপত্র", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultIconButton(
                icon = "fi_plus.svg",
                label = "কাগজ যোগ করুন",
                tone = IconTone.TINT,
                onClick = onOpenAddDoc
            )
        }

        // Hero Next Renewal Card
        if (nextRenewal != null) {
            val palette = LocalVaultPalette.current
            val days = nextRenewal.daysRemaining ?: 0
            val (statusBg, statusBorder) = when {
                days < 0 -> palette.err.copy(alpha = 0.09f) to palette.err.copy(alpha = 0.4f)
                days <= 30 -> palette.warn.copy(alpha = 0.09f) to palette.warn.copy(alpha = 0.4f)
                else -> palette.ok.copy(alpha = 0.08f) to palette.ok.copy(alpha = 0.35f)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusBg)
                    .border(1.5.dp, statusBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onOpenDocView(nextRenewal)
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "পরের নবায়ন", fontSize = 13.sp, color = TextSecondary)
                Text(text = nextRenewal.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val expDate = runCatching { LocalDate.parse(nextRenewal.expiry) }.getOrNull()
                    Text(
                        text = "মেয়াদ শেষ: " + if (expDate != null) f.dateFull(expDate) else nextRenewal.expiry,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    ExpiryChip(doc = nextRenewal, warningDays = nextRenewal.reminderWarningDays ?: state.reminderPolicy.warningDays)
                }
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderStrongColor, RoundedCornerShape(16.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                V3Icon(path = "feather_icon/fi_scan-line.svg", description = null, modifier = Modifier.size(36.dp), tint = TextSecondary)
                Text(
                    text = if (sortedDocs.isEmpty()) "প্রথম কাগজটি যোগ করুন। অনুমতি দিলে মেয়াদের আগে জানানো হবে।" else "এই কাগজগুলোর মেয়াদ শেষের তারিখ নেই।",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                VaultButton(text = "কাগজ স্ক্যান করুন", icon = "fi_scan-line.svg", onClick = onOpenAddDoc)
            }
        }

        // Wallet Stack of All Documents
        if (sortedDocs.isNotEmpty()) {
            SectionTitle("সব কাগজ")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sortedDocs.forEach { d ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.perform(HapticKind.TICK)
                                onOpenDocView(d)
                            }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = d.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            ExpiryChip(doc = d, warningDays = d.reminderWarningDays ?: state.reminderPolicy.warningDays)
                        }
                        DocArt(doc = d)
                    }
                }
            }
        }

        VaultSwitch(
            title = "মেয়াদের আগে জানান",
            subtitle = "${f.n(state.reminderPolicy.warningDays.toLong())} দিন আগে এবং মেয়াদের দিন। ফোনের অনুমতি প্রয়োজন।",
            checked = state.docReminders,
            onCheckedChange = onToggleDocReminders
        )
        ReminderAccessPanel(rememberReminderAccess(), state)
        VaultButton("অনুমতি, সময় ও পরে জানান", onClick = onReminderSettings, full = true, kind = ButtonKind.SECONDARY)

        if (sortedDocs.isNotEmpty()) {
            VaultButton(
                text = "রাস্তায় দেখানোর মোড",
                kind = ButtonKind.GHOST,
                full = true,
                onClick = { onOpenDocView(sortedDocs.first()) }
            )
        }
    }
}

// ==========================================
// 3. HISTORY SCREEN
// ==========================================
@Composable
fun HistoryScreen(
    state: VaultState,
    onDeleteLog: (RideLog) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    var filter by remember { mutableStateOf("all") } // "all", "service", "fuel"

    val filteredLogs = remember(state.logs, filter) {
        state.logs.filter {
            filter == "all" || (filter == "fuel" && it.isFuel) || (filter == "service" && !it.isFuel)
        }.sortedWith(compareByDescending<RideLog> { it.date }.thenByDescending { it.odometer })
    }

    val groups = remember(filteredLogs) {
        filteredLogs.groupBy { it.date.take(7) }.toList()
    }

    val thirtyDaysAgo = remember(today) { LocalDate.fromEpochDays(today.toEpochDays() - 30).toString() }
    val recentLogs = remember(state.logs, thirtyDaysAgo) { state.logs.filter { it.date >= thirtyDaysAgo } }
    val spend30 = remember(recentLogs) { recentLogs.sumOf { it.cost } }
    val km30 = remember(recentLogs) {
        if (recentLogs.size > 1) {
            val maxOdo = recentLogs.maxOf { it.odometer }
            val minOdo = recentLogs.minOf { it.odometer }
            maxOdo - minOdo
        } else 0.0
    }
    val avgEconomy = remember(state.logs) { WearCalculator.economyOf(state.logs) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "ইতিহাস", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

        // 3-stat summary banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "গত ৩০ দিনের খরচ", fontSize = 12.sp, color = TextSecondary)
                Text(text = f.money(spend30), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "চালানো", fontSize = 12.sp, color = TextSecondary)
                Text(text = f.km(km30), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "জ্বালানি গড়", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = if (avgEconomy != null) "${f.d(formatLakh(avgEconomy, 1))} ${if (f.digits == Digits.BN) "কিমি/লি" else "km/L"}" else "--",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultChip(text = "সব", active = filter == "all", onClick = { filter = "all" })
                VaultChip(text = "সার্ভিস", active = filter == "service", onClick = { filter = "service" })
                VaultChip(text = "জ্বালানি", active = filter == "fuel", onClick = { filter = "fuel" })
            }
            Text(text = "${f.d(filteredLogs.size.toString())}টি রেকর্ড", fontSize = 13.sp, color = TextSecondary)
        }

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "এখনও কিছু নেই। নিচের + চাপুন।", fontSize = 15.sp, color = TextSecondary)
            }
        }

        groups.forEach { (yearMonth, items) ->
            val monthDate = runCatching { LocalDate.parse("$yearMonth-01") }.getOrNull()
            val monthLabel = if (monthDate != null) f.month(monthDate) else yearMonth

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = monthLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)

                items.forEach { log ->
                    var expanded by remember { mutableStateOf(false) }
                    val rot by animateFloatAsState(if (expanded) 180f else 0f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    expanded = !expanded
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (log.isFuel) BrandTintColor else HealthyColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                V3Icon(
                                    path = if (log.isFuel) "feather_icon/fi_fuel.svg" else "feather_icon/fi_tool.svg",
                                    description = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (log.isFuel) BrandColor else HealthyColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                val logDate = runCatching { LocalDate.parse(log.date) }.getOrNull()
                                val title = if (log.isFuel) "তেল ভরা · ${f.liters(log.fuelLiters ?: 0.0)}" else log.detail.ifBlank { "সার্ভিস" }
                                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    text = "${if (logDate != null) f.date(logDate) else log.date} · ${f.km(log.odometer)}",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(text = f.money(log.cost), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            V3Icon(
                                path = "feather_icon/fi_chevron-down.svg",
                                description = null,
                                modifier = Modifier.size(18.dp).rotate(rot),
                                tint = TextTertiary
                            )
                        }

                        // Expanded row details
                        AnimatedVisibility(visible = expanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 70.dp, end = 14.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (log.isFuel) {
                                    log.fuelPrice?.let { Text("${f.money(it)} প্রতি লিটার · ${if (log.fuelType == "petrol") "পেট্রোল" else "অকটেন"}", fontSize = 13.sp, color = TextSecondary) }
                                    Text(if (log.fullTank) "ট্যাংক পূর্ণ করা হয়েছে" else "আংশিক, পরের পূর্ণ ট্যাংকে হিসাব হবে", fontSize = 13.sp, color = TextSecondary)
                                    if (log.locationLabel.isNotBlank()) Text(log.locationLabel, fontSize = 13.sp, color = TextSecondary)
                                } else {
                                    log.serviceLines.forEach { l ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = l.displayNameBn + (l.detail?.let { " · $it" } ?: "") + if (l.quantity > 1) " × ${f.d(l.quantity.toInt().toString())}" else "",
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = l.unitPricePoisha?.let { f.money(it / 100.0 * l.quantity) } ?: "দাম জানা নেই",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                    log.serviceLabourPoisha?.takeIf { it > 0 }?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("শ্রম", fontSize = 13.sp, color = TextSecondary)
                                            Text(f.money(it / 100.0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        }
                                    }
                                    log.serviceDiscountPoisha?.takeIf { it > 0 }?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("ছাড়", fontSize = 13.sp, color = TextSecondary)
                                            Text("−${f.money(it / 100.0)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        }
                                    }
                                    if (log.workshop.isNotBlank()) Text(log.workshop, fontSize = 13.sp, color = TextSecondary)
                                }

                                Row(
                                    modifier = Modifier.clickable { onDeleteLog(log) }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    V3Icon(path = "feather_icon/fi_trash-2.svg", description = null, modifier = Modifier.size(14.dp), tint = ErrorColor)
                                    Text("রেকর্ড মুছুন", fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. ME (SETTINGS) SCREEN
// ==========================================
@Composable
fun MeScreen(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onStartOnboarding: () -> Unit,
    onResetDemo: () -> Unit,
    onOpenSheet: (String, Any?) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val ride = state.ride ?: return
    val brand = worlds.firstOrNull { it.id == ride.brandId } ?: worlds.first()
    val model = ride.selectedModel
    val activeBrandId = state.brandOverride ?: ride.brandId
    var widgetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(text = "আমি", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

        // Profile card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd))),
                contentAlignment = Alignment.Center
            ) {
                if (model != null) {
                    Image(painter = painterResource(model.photo), contentDescription = null, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                } else {
                    V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(32.dp), tint = BrandColor)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "${brand.name} ${ride.displayName}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "${f.d(ride.year.toString())} · ${f.km(ride.odometer)}", fontSize = 13.sp, color = TextSecondary)
            }
            VaultButton(
                text = "বাইক বদলান",
                size = ButtonSize.SM,
                kind = ButtonKind.SECONDARY,
                onClick = onStartOnboarding
            )
        }

        // Appearance section
        SectionTitle("চেহারা")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            // Theme
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("থিম", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(Appearance.SYSTEM to "ডিভাইসের মতো", Appearance.LIGHT to "লাইট", Appearance.DARK to "ডার্ক"),
                    selected = state.appearance,
                    onSelect = { onUpdateState(state.copy(appearance = it)) }
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Brand color swatches
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("রঙ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BRAND_COLOR_MAP.forEach { (bId, colors) ->
                        val isSelected = activeBrandId == bId
                        val swatchColor = Color(if (LocalVaultPalette.current.dark) colors.dark else colors.light)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    if (isSelected) 3.dp else 1.5.dp,
                                    if (isSelected) TextPrimary else BorderStrongColor,
                                    CircleShape
                                )
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    onUpdateState(state.copy(brandOverride = if (bId == ride.brandId) null else bId))
                                }
                        )
                    }
                }
                Text(
                    text = if (state.brandOverride != null) "${worlds.firstOrNull { it.id == state.brandOverride }?.name ?: ""} এর রঙ" else "আপনার বাইকের ব্র্যান্ডের রঙ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Digits
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("সংখ্যা", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(Digits.BN to "১২৩ বাংলা", Digits.EN to "123 English"),
                    selected = state.digits,
                    onSelect = { onUpdateState(state.copy(digits = it)) }
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Text scale
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("লেখার আকার", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(1.0f to "স্বাভাবিক", 1.15f to "বড়", 1.3f to "অনেক বড়"),
                    selected = state.textScale,
                    onSelect = { onUpdateState(state.copy(textScale = it)) }
                )
            }
        }

        // Behavior section
        SectionTitle("আচরণ")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            VaultSwitch(
                title = "ভাইব্রেশন ফিডব্যাক",
                subtitle = "সেভ, ভুল ও ট্যাপে হালকা কাঁপুনি",
                checked = state.haptics,
                onCheckedChange = { onUpdateState(state.copy(haptics = it)) }
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            VaultSwitch(
                title = "অ্যানিমেশন কমান",
                subtitle = "ফোনের সেটিং চালু থাকলে এটি নিজেই চালু হয়",
                checked = state.reduceMotion,
                onCheckedChange = { onUpdateState(state.copy(reduceMotion = it)) }
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            VaultSwitch(
                title = "তেল ও সার্ভিসে বর্তমান লোকেশন",
                subtitle = "অনুমতি না দিলেও রেকর্ড সেভ হবে",
                checked = state.autoLocation,
                onCheckedChange = { onUpdateState(state.copy(autoLocation = it)) }
            )
        }

        // Reminders section
        SectionTitle("রিমাইন্ডার")
        ReminderAccessPanel(rememberReminderAccess(), state)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            VaultSwitch(
                title = "সার্ভিসের সময় হলে জানান",
                subtitle = "চেইন, অয়েল ও অন্য যন্ত্রাংশ · ${f.d((state.reminderPolicy.minuteOfDay / 60).toString().padStart(2, '0'))}:${f.d((state.reminderPolicy.minuteOfDay % 60).toString().padStart(2, '0'))}",
                checked = state.serviceReminders,
                onCheckedChange = { onUpdateState(state.copy(serviceReminders = it)) }
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            VaultSwitch(
                title = "কাগজের মেয়াদের আগে জানান",
                subtitle = "${f.n(state.reminderPolicy.warningDays.toLong())} দিন আগে এবং মেয়াদের দিন। ফোনের অনুমতি প্রয়োজন।",
                checked = state.docReminders,
                onCheckedChange = { onUpdateState(state.copy(docReminders = it)) }
            )
        }
        VaultButton("অনুমতি, সময় ও পরে জানান", kind = ButtonKind.SECONDARY, full = true, onClick = { onOpenSheet("reminders", null) })

        // New concepts (demo)
        SectionTitle("নতুন ধারণা (ডেমো)")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSheet("paste", null) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(path = "feather_icon/fi_share-2.svg", description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
                Column {
                    Text("শেয়ার বা পেস্ট করে যোগ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("বিকাশ, নগদ বা মেকানিকের মেসেজ থেকে খসড়া", fontSize = 13.sp, color = TextSecondary)
                }
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        widgetOpen = !widgetOpen
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(path = "feather_icon/fi_grid.svg", description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
                Column {
                    Text("হোম স্ক্রিন উইজেট", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("অ্যাপ না খুলেই পরের কাজ আর দুটি বোতাম", fontSize = 13.sp, color = TextSecondary)
                }
            }

            // Widget preview
            AnimatedVisibility(visible = widgetOpen) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandTintColor)
                            .border(1.dp, BrandLineColor, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ride.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandColor)
                            Text(text = f.km(ride.odometer), fontSize = 12.sp, color = TextSecondary)
                        }
                        Text(text = "চেইন · এখনই", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceColor).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text("⛽ তেল", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceColor).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text("📏 কিমি", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // About & Reset
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MotoVault প্রোটোটাইপ · সব তথ্য এই ডিভাইসেই থাকে",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            VaultButton(
                text = "ডেমো ডেটা নতুন করে দিন",
                kind = ButtonKind.GHOST,
                full = true,
                onClick = onResetDemo
            )
        }
    }
}

// ==========================================
// 5. ONBOARDING (3 STEPS) SCREEN
// ==========================================
@Composable
fun OnboardingScreen(
    onDismiss: (() -> Unit)? = null,
    onComplete: (brandId: String, modelId: String, name: String, odometer: Double, knowsLastService: Boolean) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    var step by remember { mutableStateOf(0) } // 0: Brand, 1: Model, 2: Odometer
    var selectedBrandId by remember { mutableStateOf<String?>(null) }
    var selectedModelId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var knowsLastService by remember { mutableStateOf(false) }

    val odoEntry = rememberNumberEntry(
        initialValues = mapOf("odo" to ""),
        firstField = "odo"
    )
    val odoVal = parseNum(odoEntry.values["odo"] ?: "") ?: 0.0

    val selectedBrand = worlds.firstOrNull { it.id == selectedBrandId }
    val brandModels = remember(selectedBrandId) {
        if (selectedBrandId != null) BikeCatalogue.forBrand(selectedBrandId!!) else emptyList()
    }
    val filteredModels = remember(brandModels, searchQuery) {
        if (searchQuery.isBlank()) brandModels
        else brandModels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step progress header
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (step > 0) {
                VaultIconButton(
                    icon = "fi_arrow-left.svg",
                    label = "পেছনে",
                    onClick = { step -= 1 }
                )
            } else if (onDismiss != null) {
                VaultIconButton(
                    icon = "fi_arrow-left.svg",
                    label = "ফিরে যান",
                    onClick = onDismiss
                )
            } else {
                V3Icon(path = "feather_icon/fi_bike.svg", description = null, modifier = Modifier.size(26.dp), tint = BrandColor)
            }
            // Step dots
            Row(modifier = Modifier.weight(1f).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..2).forEach { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= step) BrandColor else BorderColor)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "${f.d((step + 1).toString())} / ${f.d("3")}", fontSize = 13.sp, color = TextSecondary)
                if (onDismiss != null) {
                    VaultIconButton(
                        icon = "fi_x.svg",
                        label = "বন্ধ করুন",
                        onClick = onDismiss
                    )
                }
            }
        }

        when (step) {
            0 -> {
                // Step 0: "কোন বাইক চালান?"
                // EXCEPTION: uses bike brand logos aligning prototype!
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "কোন বাইক চালান?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "ব্র্যান্ড বেছে নিন। অ্যাপের রঙ আপনার বাইকের সঙ্গে মিলবে।", fontSize = 15.sp, color = TextSecondary)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        worlds.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { w ->
                                    val isSelected = selectedBrandId == w.id
                                    val count = BikeCatalogue.forBrand(w.id).size
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) BrandTintColor else SurfaceColor)
                                            .border(1.5.dp, if (isSelected) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptic.perform(HapticKind.TICK)
                                                selectedBrandId = w.id
                                                step = 1
                                            }
                                            .padding(vertical = 14.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Brand vector logo (honoring user exception)
                                        Image(
                                            painter = painterResource(w.logo()),
                                            contentDescription = w.name,
                                            modifier = Modifier.width(52.dp).height(28.dp),
                                            contentScale = ContentScale.Fit,
                                            colorFilter = ColorFilter.tint(if (isSelected) BrandColor else TextPrimary)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(text = w.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text(text = "${f.d(count.toString())}টি মডেল", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    if (onDismiss != null) {
                        VaultButton(
                            text = "বাতিল করুন",
                            kind = ButtonKind.SECONDARY,
                            onClick = onDismiss,
                            full = true
                        )
                    }
                    Text(
                        text = "আপনার সব তথ্য এই ফোনেই থাকবে। কোনো অ্যাকাউন্ট লাগবে না।",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
            }
            1 -> {
                // Step 1: "{Brand} এর কোনটি?"
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "${selectedBrand?.name ?: ""} এর কোনটি?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Search box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceColor2)
                            .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            V3Icon(path = "feather_icon/fi_search.svg", description = null, modifier = Modifier.size(20.dp), tint = TextTertiary)
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text("মডেলের নাম খুঁজুন", fontSize = 15.sp, color = TextTertiary)
                                    inner()
                                }
                            )
                        }
                    }

                    // Model list
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredModels.forEach { m ->
                            val isSelected = selectedModelId == m.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) BrandTintColor else SurfaceColor)
                                    .border(1.5.dp, if (isSelected) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.perform(HapticKind.TICK)
                                        selectedModelId = m.id
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 104.dp, height = 68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(painter = painterResource(m.photo), contentDescription = null, modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
                                }
                                Text(text = m.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.size(30.dp).clip(CircleShape).background(BrandColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓", color = OnBrandColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            selectedModelId = brandModels.firstOrNull()?.id
                            step = 2
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("আমার মডেল তালিকায় নেই", color = BrandColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    VaultButton(
                        text = if (selectedModelId != null) "${brandModels.firstOrNull { it.id == selectedModelId }?.name ?: ""} নিয়ে এগিয়ে যান" else "মডেল বেছে নিন",
                        full = true,
                        enabled = selectedModelId != null,
                        onClick = { step = 2 }
                    )
                }
            }
            2 -> {
                // Step 2: "এখন ওডোমিটারে কত?"
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "এখন ওডোমিটারে কত?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val text = odoEntry.values["odo"].orEmpty()
                            Text(
                                text = if (text.isEmpty()) f.d("0") else f.d(formatLakh(text.toDoubleOrNull() ?: 0.0)),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (text.isEmpty()) TextTertiary else TextPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = if (f.digits == Digits.BN) "কিমি" else "km", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VaultChip(text = "শেষ সার্ভিস মনে নেই", active = !knowsLastService, onClick = { knowsLastService = false })
                        VaultChip(text = "মোটামুটি মনে আছে", active = knowsLastService, onClick = { knowsLastService = true })
                    }

                    Text(
                        text = if (knowsLastService) "অনুমান থেকে শতাংশ দেখানো হবে না। সার্ভিসের তথ্য দিয়ে নিশ্চিত করুন।"
                               else "শেষ সার্ভিস অজানা থাকবে। সার্ভিস লিখলে ব্যবধানের হিসাব শুরু হবে।",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.weight(1f))

                    VaultButton(
                        text = if (odoVal <= 0) "কিমি লিখুন" else "গ্যারেজ তৈরি করুন",
                        full = true,
                        enabled = odoVal > 0,
                        onClick = {
                            haptic.perform(HapticKind.SUCCESS)
                            val chosenModel = brandModels.firstOrNull { it.id == selectedModelId }
                            onComplete(
                                selectedBrandId ?: "yamaha",
                                selectedModelId ?: "yamaha-r15-v4",
                                chosenModel?.name ?: "R15 V4",
                                odoVal,
                                knowsLastService
                            )
                        }
                    )

                    VaultKeypad(onKey = odoEntry::press, decimal = false)
                }
            }
        }
    }
}
