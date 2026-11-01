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

private const val HISTORY_SUMMARY_DAYS = 30

@Composable
fun HistoryScreen(
    state: VaultState,
    onDeleteLog: (RideLog) -> Unit,
    onEditFuel: (RideLog) -> Unit
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

    val thirtyDaysAgo = remember(today) { LocalDate.fromEpochDays(today.toEpochDays() - HISTORY_SUMMARY_DAYS).toString() }
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
        Text(text = appStringResource(Res.string.history_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

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
                Text(text = appStringResource(Res.string.history_spend, f.d(HISTORY_SUMMARY_DAYS.toString())), fontSize = 12.sp, color = TextSecondary)
                Text(text = f.money(spend30), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = appStringResource(Res.string.history_distance_label), fontSize = 12.sp, color = TextSecondary)
                Text(text = appStringResource(Res.string.history_distance, f.n(km30)), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = appStringResource(Res.string.history_economy_label), fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = if (avgEconomy != null) appStringResource(Res.string.history_economy, f.n(avgEconomy, 1)) else "--",
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
                VaultChip(text = appStringResource(Res.string.history_all), active = filter == "all", onClick = { filter = "all" })
                VaultChip(text = appStringResource(Res.string.history_service), active = filter == "service", onClick = { filter = "service" })
                VaultChip(text = appStringResource(Res.string.history_fuel), active = filter == "fuel", onClick = { filter = "fuel" })
            }
            Text(text = appPluralStringResource(Res.plurals.history_record_count, filteredLogs.size, f.d(filteredLogs.size.toString())), fontSize = 13.sp, color = TextSecondary)
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
                Text(text = appStringResource(Res.string.history_empty), fontSize = 15.sp, color = TextSecondary)
            }
        }

        groups.forEach { (yearMonth, items) ->
            val monthDate = runCatching { LocalDate.parse("$yearMonth-01") }.getOrNull()
            val monthLabel = if (monthDate != null) appStringResource(Res.string.history_month_year,
                localizedMonthName(state.language, monthDate.monthNumber - 1), f.d(monthDate.year.toString())) else yearMonth

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
                                val title = if (log.isFuel) appStringResource(Res.string.history_fuel_title,
                                    appStringResource(Res.string.fuel_litres_value, f.n(log.fuelLiters ?: 0.0, 2)))
                                    else log.detail.ifBlank { appStringResource(Res.string.history_service) }
                                val dateLabel = if (logDate != null) appStringResource(Res.string.history_day_month,
                                    f.d(logDate.dayOfMonth.toString()), localizedMonthName(state.language, logDate.monthNumber - 1)) else log.date
                                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    text = appStringResource(Res.string.history_row_subtitle, dateLabel,
                                        appStringResource(Res.string.history_distance, f.n(log.odometer))),
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
                                    log.fuelPrice?.let { Text(appStringResource(Res.string.history_price_fuel, f.money(it),
                                        appStringResource(if (log.fuelType == "petrol") Res.string.history_fuel_petrol else Res.string.history_fuel_octane)), fontSize = 13.sp, color = TextSecondary) }
                                    Text(appStringResource(if (log.fullTank) Res.string.history_full_tank else Res.string.history_partial_tank), fontSize = 13.sp, color = TextSecondary)
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
                                                text = l.unitPricePoisha?.let { f.money(it / 100.0 * l.quantity) } ?: appStringResource(Res.string.history_price_unknown),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                    log.serviceLabourPoisha?.takeIf { it > 0 }?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(appStringResource(Res.string.history_labour), fontSize = 13.sp, color = TextSecondary)
                                            Text(f.money(it / 100.0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        }
                                    }
                                    log.serviceDiscountPoisha?.takeIf { it > 0 }?.let {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(appStringResource(Res.string.history_discount), fontSize = 13.sp, color = TextSecondary)
                                            Text("−${f.money(it / 100.0)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        }
                                    }
                                    if (log.workshop.isNotBlank()) Text(log.workshop, fontSize = 13.sp, color = TextSecondary)
                                }

                                if (log.isFuel) {
                                    TextButton(onClick = { onEditFuel(log) }, contentPadding = PaddingValues(0.dp)) {
                                        Text(appStringResource(Res.string.fuel_edit_action), fontSize = 13.sp)
                                    }
                                }
                                Row(
                                    modifier = Modifier.clickable { onDeleteLog(log) }.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    V3Icon(path = "feather_icon/fi_trash-2.svg", description = null, modifier = Modifier.size(14.dp), tint = ErrorColor)
                                    Text(appStringResource(Res.string.history_delete), fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.SemiBold)
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
