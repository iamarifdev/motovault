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
        GarageHeaderSection(ride, brandWorld, model) {
            haptic.perform(HapticKind.TICK)
            onSwitchTab(Tab.ME)
        }

        if (topWear != null) {
            GarageHeroSection(topWear) {
                haptic.perform(HapticKind.TICK)
                onOpenServiceForItem(topWear.rule.id)
            }
        }

        // Quick Action Row (4 columns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val quickActions = listOf(
                Triple("fi_fuel.svg", appStringResource(Res.string.garage_quick_fuel), "fuel"),
                Triple("fi_tool.svg", appStringResource(Res.string.garage_quick_service), "service"),
                Triple("fi_gauge.svg", appStringResource(Res.string.garage_quick_km), "odometer"),
                Triple("fi_file-text.svg", appStringResource(Res.string.garage_quick_document), "docAdd")
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
                    text = appStringResource(Res.string.garage_since_fuel, f.d(sinceFuelDays.toString())),
                    fontSize = 14.sp,
                    lineHeight = 19.6.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                VaultButton(
                    text = appStringResource(Res.string.garage_add),
                    size = ButtonSize.SM,
                    kind = ButtonKind.SECONDARY,
                    onClick = { onOpenSheet("fuel", null) }
                )
                VaultIconButton(
                    icon = "fi_x.svg",
                    label = appStringResource(Res.string.garage_hide),
                    onClick = { nudgeHidden = true }
                )
            }
        }

        GarageStatsSection(
            ride = ride,
            economy = economy,
            series = series,
            estimate = estimate,
            onOdometer = { onOpenSheet("odometer", null) },
            onHistory = { onSwitchTab(Tab.HISTORY) }
        )

        // Gauges Section
        Column {
        SectionTitle(appStringResource(Res.string.garage_service_interval_remaining))
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
            SectionTitle(appStringResource(Res.string.garage_doc_expiry))
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
                                    text = if (isExpired) appStringResource(Res.string.garage_expired_ago, f.days(-leftDays))
                                    else appStringResource(Res.string.garage_expires_in, f.days(leftDays)),
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

        GarageBikeCardSection(ride, brandWorld, model)
    }
}

internal data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
