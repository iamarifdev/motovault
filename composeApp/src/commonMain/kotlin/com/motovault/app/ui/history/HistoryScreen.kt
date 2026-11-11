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

import com.motovault.app.domain.ledger.*

@Composable
fun HistoryScreen(
    state: VaultState,
    onDeleteLog: (RideLog) -> Unit,
    onEditFuel: (RideLog) -> Unit,
    onOpenCost: (String, Any?) -> Unit = { _, _ -> }
) {
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    var period by remember { mutableStateOf(CostPeriod.RECENT) }
    var category by remember { mutableStateOf<CostCategory?>(null) }
    val summary = remember(state.logs, today, period, category) { CostLedger.summarize(state.logs, today, period, category) }
    val filteredLogs = summary.logs
    val groups = remember(filteredLogs) { filteredLogs.groupBy { it.date.take(7) }.toList() }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = appStringResource(Res.string.history_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

        CostOverview(state, summary, period, category, { period = it }, { category = it }, { onOpenCost(it, if (it == "cost-details") CostDetailsSelection(period, category) else null) })

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
                                val title = when {
                                    log.isFuel -> appStringResource(Res.string.history_fuel_title,
                                        appStringResource(Res.string.fuel_litres_value, f.n(log.fuelLiters ?: 0.0, 2)))
                                    log.isDocumentFee -> {
                                        val document = state.documents.firstOrNull { it.id == log.documentId }
                                        val name = document?.let { documentDisplayName(it.title) }
                                            ?: appStringResource(Res.string.history_document_fee_unknown)
                                        appStringResource(Res.string.history_document_fee, name)
                                    }
                                    log.kind == "expense" -> costCategoryLabel(CostLedger.category(log))
                                    else -> log.detail.ifBlank { appStringResource(Res.string.history_service) }
                                }
                                val dateLabel = if (logDate != null) appStringResource(Res.string.history_day_month,
                                    f.d(logDate.dayOfMonth.toString()), localizedMonthName(state.language, logDate.monthNumber - 1)) else log.date
                                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    text = if (log.kind == "expense" || log.isDocumentFee) dateLabel else appStringResource(Res.string.history_row_subtitle, dateLabel,
                                        appStringResource(Res.string.history_distance, f.n(log.odometer))),
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(text = CostLedger.amount(log)?.let { costMoney(it) } ?: appStringResource(Res.string.cost_unavailable), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                                text = serviceLineLabel(l) + (l.detail?.let { " · $it" } ?: "") + if (l.quantity > 1) " × ${f.d(l.quantity.toInt().toString())}" else "",
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

                                if (log.notes.isNotBlank()) Text(log.notes, color = TextSecondary, fontSize = 13.sp)
                                if (log.kind == "expense") {
                                    TextButton(onClick = { onOpenCost("expense", log) }) { Text(appStringResource(Res.string.cost_edit)) }
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
