package com.motovault.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.CostBreakdown
import com.motovault.app.domain.ledger.CostCategory
import com.motovault.app.domain.ledger.CostLedger
import com.motovault.app.domain.ledger.CostPeriod
import com.motovault.app.domain.ledger.CostSummary
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.ledger.toBreakdown
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_delete_short
import motovault.composeapp.generated.resources.cost_add
import motovault.composeapp.generated.resources.cost_bar_desc
import motovault.composeapp.generated.resources.cost_expense_row
import motovault.composeapp.generated.resources.cost_expense_row_with_note
import motovault.composeapp.generated.resources.cost_legend_fuel
import motovault.composeapp.generated.resources.cost_legend_other
import motovault.composeapp.generated.resources.cost_legend_service
import motovault.composeapp.generated.resources.cost_total_title
import motovault.composeapp.generated.resources.cost_unavailable

private const val BAR_ANIM_MS = 350
private const val MIN_SEGMENT_FRACTION = 0.04f

@Composable
internal fun CostInsightsCard(
    state: VaultState,
    summary: CostSummary,
    period: CostPeriod,
    category: CostCategory?,
    onPeriod: (CostPeriod) -> Unit,
    onCategory: (CostCategory?) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (RideLog) -> Unit,
    onDeleteExpense: (RideLog) -> Unit
) {
    val f = currentFmt
    val totalPoisha = summary.totalPoisha
    val breakdown = summary.toBreakdown()
    val extraExpenses = state.logs.filter { it.kind == "expense" }.sortedByDescending { it.date }
    val totalText = totalPoisha?.let { f.money(Money.taka(it)) }
        ?: appStringResource(Res.string.cost_unavailable)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = appStringResource(Res.string.cost_total_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            VaultButton(text = appStringResource(Res.string.cost_add), size = ButtonSize.SM, kind = ButtonKind.SECONDARY, icon = "fi_plus.svg", onClick = onAddExpense)
        }
        CostFilterChips(period = period, category = category, onPeriod = onPeriod, onCategory = onCategory)
        Text(
            text = totalText,
            fontSize = if (totalPoisha != null) 28.sp else 14.sp,
            fontWeight = if (totalPoisha != null) FontWeight.Bold else FontWeight.Medium,
            color = if (totalPoisha != null) TextPrimary else WarningColor
        )
        if (totalPoisha != null && totalPoisha > 0L) {
            CostBreakdownBar(totalPoisha = totalPoisha, breakdown = breakdown)
            CostLegendRow(breakdown = breakdown)
        }
        extraExpenses.forEach { row ->
            CostExpenseRow(row = row, onEdit = { onEditExpense(row) }, onDelete = { onDeleteExpense(row) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CostFilterChips(
    period: CostPeriod,
    category: CostCategory?,
    onPeriod: (CostPeriod) -> Unit,
    onCategory: (CostCategory?) -> Unit
) {
    val categories = listOf(null, CostCategory.FUEL, CostCategory.SERVICE)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CostPeriod.entries.forEach { item ->
            VaultChip(text = costPeriodLabel(item), active = period == item, onClick = { onPeriod(item) })
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { item ->
            VaultChip(text = costCategoryLabel(item), active = category == item, onClick = { onCategory(item) })
        }
    }
}

@Composable
private fun CostBreakdownBar(totalPoisha: Long, breakdown: CostBreakdown) {
    val reducedMotion = isReducedMotion
    val animSpec = if (reducedMotion) snap<Float>() else tween<Float>(BAR_ANIM_MS, easing = VaultMotion.SkyCurve)
    val fuelTarget = if (breakdown.fuelPoisha > 0L) (breakdown.fuelPoisha.toFloat() / totalPoisha).coerceAtLeast(MIN_SEGMENT_FRACTION) else 0f
    val svcTarget = if (breakdown.servicePoisha > 0L) (breakdown.servicePoisha.toFloat() / totalPoisha).coerceAtLeast(MIN_SEGMENT_FRACTION) else 0f
    val otherTarget = if (breakdown.otherPoisha > 0L) (breakdown.otherPoisha.toFloat() / totalPoisha).coerceAtLeast(MIN_SEGMENT_FRACTION) else 0f
    val fuelWeight by animateFloatAsState(fuelTarget, animSpec, label = "fuelBar")
    val svcWeight by animateFloatAsState(svcTarget, animSpec, label = "svcBar")
    val otherWeight by animateFloatAsState(otherTarget, animSpec, label = "otherBar")
    val barDesc = appStringResource(Res.string.cost_bar_desc)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(SurfaceColor2)
            .semantics { contentDescription = barDesc },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (breakdown.fuelPoisha > 0L) Box(Modifier.weight(fuelWeight.coerceAtLeast(MIN_SEGMENT_FRACTION)).fillMaxHeight().background(BrandColor))
        if (breakdown.servicePoisha > 0L) Box(Modifier.weight(svcWeight.coerceAtLeast(MIN_SEGMENT_FRACTION)).fillMaxHeight().background(HealthyColor))
        if (breakdown.otherPoisha > 0L) Box(Modifier.weight(otherWeight.coerceAtLeast(MIN_SEGMENT_FRACTION)).fillMaxHeight().background(WarningColor))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CostLegendRow(breakdown: CostBreakdown) {
    val f = currentFmt
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (breakdown.fuelPoisha > 0L) {
            CostLegendItem(BrandColor, appStringResource(Res.string.cost_legend_fuel, f.money(Money.taka(breakdown.fuelPoisha))))
        }
        if (breakdown.servicePoisha > 0L) {
            CostLegendItem(HealthyColor, appStringResource(Res.string.cost_legend_service, f.money(Money.taka(breakdown.servicePoisha))))
        }
        if (breakdown.otherPoisha > 0L) {
            CostLegendItem(WarningColor, appStringResource(Res.string.cost_legend_other, f.money(Money.taka(breakdown.otherPoisha))))
        }
    }
}

@Composable
private fun CostLegendItem(dotColor: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun CostExpenseRow(row: RideLog, onEdit: () -> Unit, onDelete: () -> Unit) {
    val f = currentFmt
    val catLabel = costCategoryLabel(CostLedger.category(row))
    val amountLabel = CostLedger.amount(row)?.let { f.money(Money.taka(it)) } ?: f.money(row.cost)
    val note = row.notes.trim()
    val label = if (note.isNotEmpty()) {
        appStringResource(Res.string.cost_expense_row_with_note, catLabel, note, amountLabel)
    } else {
        appStringResource(Res.string.cost_expense_row, catLabel, amountLabel)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onEdit)
                .padding(vertical = 4.dp)
        )
        Text(
            text = appStringResource(Res.string.action_delete_short),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ErrorColor,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onDelete)
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
        )
    }
}
