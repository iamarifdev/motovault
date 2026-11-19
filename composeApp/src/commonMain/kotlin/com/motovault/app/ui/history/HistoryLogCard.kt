package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.CostLedger
import com.motovault.app.domain.ledger.Money
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.cost_unavailable
import motovault.composeapp.generated.resources.fuel_litres_value
import motovault.composeapp.generated.resources.history_distance
import motovault.composeapp.generated.resources.history_fuel_title
import motovault.composeapp.generated.resources.history_partial_tank_note
import motovault.composeapp.generated.resources.history_row_subtitle
import motovault.composeapp.generated.resources.history_service
import motovault.composeapp.generated.resources.history_service_title_more

@Composable
internal fun HistoryLogCard(
    state: VaultState,
    log: RideLog,
    onDeleteLog: (RideLog) -> Unit,
    onEdit: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    var expanded by remember(log.id) { mutableStateOf(false) }
    val chevronRot by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = VaultMotion.pressSpring(reducedMotion),
        label = "historyChevron"
    )
    val shape = RoundedCornerShape(16.dp)
    val cardBorderColor = if (expanded) BorderStrongColor else BorderColor
    val cardModifier = if (expanded) {
        Modifier.fillMaxWidth().shadow(6.dp, shape, ambientColor = ShadowColor, spotColor = ShadowColor)
    } else {
        Modifier.fillMaxWidth()
    }

    SwipeRevealRow(open = expanded, onDelete = {
        haptic.perform(HapticKind.WARNING)
        onDeleteLog(log)
    }) {
        Column(
            modifier = cardModifier
                .clip(shape)
                .background(SurfaceColor)
                .border(1.dp, cardBorderColor, shape)
        ) {
            HistoryLogSummaryRow(log = log, rotation = chevronRot, f = f) {
                haptic.perform(HapticKind.TICK)
                expanded = !expanded
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 70.dp, end = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryLogKv(state = state, log = log, f = f)
                    if (!log.isFuel) {
                        HistoryServiceBadges(state = state, log = log, f = f)
                    } else if (!log.fullTank) {
                        Text(text = appStringResource(Res.string.history_partial_tank_note), fontSize = 13.sp, color = TextSecondary)
                    }
                    HistoryLogActions(onEdit = onEdit, onDelete = {
                        haptic.perform(HapticKind.WARNING)
                        onDeleteLog(log)
                    })
                }
            }
        }
    }
}

@Composable
private fun HistoryLogSummaryRow(
    log: RideLog,
    rotation: Float,
    f: Fmt,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            Text(text = historyLogTitle(log, f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = historyLogSubtitle(log, f), fontSize = 13.sp, color = TextSecondary)
        }
        Text(
            text = CostLedger.amount(log)?.let { f.money(Money.taka(it)) } ?: appStringResource(Res.string.cost_unavailable),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        V3Icon(
            path = "feather_icon/fi_chevron-down.svg",
            description = null,
            modifier = Modifier.size(18.dp).rotate(rotation),
            tint = TextTertiary
        )
    }
}

@Composable
private fun historyLogTitle(log: RideLog, f: Fmt): String = when {
    log.isFuel -> appStringResource(
        Res.string.history_fuel_title,
        appStringResource(Res.string.fuel_litres_value, f.n(log.fuelLiters ?: 0.0, 2))
    )
    log.serviceLines.size > 1 -> appStringResource(
        Res.string.history_service_title_more,
        serviceLineLabel(log.serviceLines.first()),
        f.d((log.serviceLines.size - 1).toString())
    )
    log.serviceLines.size == 1 -> serviceLineLabel(log.serviceLines.first())
    else -> log.detail.ifBlank { appStringResource(Res.string.history_service) }
}

@Composable
private fun historyLogSubtitle(log: RideLog, f: Fmt): String {
    val dateLabel = runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { f.date(it) } ?: log.date
    val kmLabel = appStringResource(Res.string.history_distance, f.n(log.odometer))
    return appStringResource(Res.string.history_row_subtitle, dateLabel, kmLabel)
}
