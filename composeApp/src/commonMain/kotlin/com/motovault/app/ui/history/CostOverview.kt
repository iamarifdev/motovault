package com.motovault.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.*
import com.motovault.app.domain.usecase.FuelEfficiencyUseCase
import com.motovault.app.domain.usecase.FuelFill
import motovault.composeapp.generated.resources.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CostOverview(state: VaultState, summary: CostSummary, period: CostPeriod, category: CostCategory?,
    onPeriod: (CostPeriod) -> Unit, onCategory: (CostCategory?) -> Unit, onOpen: (String) -> Unit
) {
    val f = currentFmt
    Text(appStringResource(Res.string.cost_title), fontSize = 14.sp, color = TextSecondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CostPeriod.entries.forEach { VaultChip(costPeriodLabel(it), active = period == it, onClick = { onPeriod(it) }) }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf(null) + CostCategory.entries).forEach { VaultChip(costCategoryLabel(it), active = category == it, onClick = { onCategory(it) }) }
    }
    Text(summary.totalPoisha?.let { costMoney(it) } ?: appStringResource(Res.string.cost_unavailable),
        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Text(appStringResource(Res.string.cost_incomplete), color = TextSecondary, fontSize = 13.sp)
    CostCoverage(summary)
    FuelCycleSummary(state.logs)
    MonthlyBudgetSummary(state)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultChip(appStringResource(Res.string.cost_add), onClick = { onOpen("expense") })
        VaultChip(appStringResource(Res.string.cost_budget), onClick = { onOpen("budget") })
        VaultChip(appStringResource(Res.string.cost_details), onClick = { onOpen("cost-details") })
    }
}

@Composable
internal fun CostCoverage(summary: CostSummary) {
    val f = currentFmt
    val coverage = summary.coverage
    if (coverage == null) Text(appStringResource(Res.string.cost_no_distance), color = TextSecondary, fontSize = 13.sp)
    else {
        Text(appStringResource(Res.string.cost_coverage, f.n(coverage.km), localizedCostDate(coverage.first.toString()),
            localizedCostDate(coverage.last.toString())), color = TextSecondary, fontSize = 13.sp)
        Text(appStringResource(Res.string.cost_per_km, costMoney(Money.fromLegacy(Money.taka(coverage.costPoisha) / coverage.km) ?: 0)), color = TextPrimary, fontSize = 14.sp)
        Text(appStringResource(Res.string.cost_ratio_scope), color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
internal fun FuelCycleSummary(logs: List<RideLog>) {
    val result = FuelEfficiencyUseCase().confidence(logs.filter { it.isFuel }.map { FuelFill(it.odometer, it.fuelLiters, it.fullTank, it.missedFuelFill) })
    result.economy?.let { Text(appStringResource(Res.string.cost_economy, currentFmt.n(it, 1)), color = TextPrimary, fontSize = 14.sp) }
    if (result.economy == null) Text(appStringResource(Res.string.cost_cycle_missing), color = TextSecondary, fontSize = 13.sp)
    if (result.openCycle) Text(appStringResource(Res.string.cost_cycle_open), color = TextSecondary, fontSize = 13.sp)
    if (result.brokenCycle) Text(appStringResource(Res.string.cost_cycle_broken), color = TextSecondary, fontSize = 13.sp)
}

@Composable
internal fun MonthlyBudgetSummary(state: VaultState) {
    val f = currentFmt
    val budget = state.ledger.monthlyBudgetPoisha
    val spent = CostLedger.summarize(state.logs, today(), CostPeriod.MONTH).totalPoisha
    val text = if (budget == null) appStringResource(Res.string.cost_budget_none)
    else if (spent == null) appStringResource(Res.string.cost_unavailable)
    else appStringResource(if (spent > budget) Res.string.cost_budget_over else Res.string.cost_budget_remaining,
        costMoney(spent), costMoney(kotlin.math.abs(budget - spent)))
    Text(text, color = TextSecondary, fontSize = 13.sp)
}

@Composable
internal fun localizedCostDate(value: String): String {
    return currentFmt.dateFull(kotlinx.datetime.LocalDate.parse(value))
}
