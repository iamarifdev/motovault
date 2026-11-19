package com.motovault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motovault.app.domain.ledger.CostCategory
import com.motovault.app.domain.ledger.CostLedger
import com.motovault.app.domain.ledger.CostPeriod
import com.motovault.app.domain.ledger.filterHistoryTimeline
import com.motovault.app.ui.history.WorkLedgerCard

private const val JUMP_TOP_THRESHOLD = 6

@Composable
fun HistoryScreen(
    state: VaultState,
    onDeleteLog: (RideLog) -> Unit,
    onEditFuel: (RideLog) -> Unit,
    onOpenCost: (String, Any?) -> Unit = { _, _ -> },
    onEditService: (RideLog) -> Unit = {},
    onDeleteExpense: (RideLog) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val f = currentFmt
    val today = today()
    var period by remember { mutableStateOf(CostPeriod.RECENT) }
    var category by remember { mutableStateOf<CostCategory?>(null) }
    val summary = remember(state.logs, today, period, category) { CostLedger.summarize(state.logs, today, period, category) }
    val timelineLogs = remember(state.logs, today, period, category) { filterHistoryTimeline(state.logs, today, period, category) }
    val groups = remember(timelineLogs) { timelineLogs.groupBy { it.date.take(7) }.toList() }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "history_title") { HistoryPageTitle() }
        item(key = "cost_insights") {
            CostInsightsCard(
                state = state,
                summary = summary,
                period = period,
                category = category,
                onPeriod = { period = it },
                onCategory = { category = it },
                onAddExpense = { onOpenCost("expense", null) },
                onEditExpense = { row -> onOpenCost("expense", row) },
                onDeleteExpense = onDeleteExpense
            )
        }
        item(key = "work_ledger_card") {
            WorkLedgerCard(state = state, onOpenWorkSheet = { onOpenCost("work-ledger", null) })
        }
        if (groups.isEmpty()) {
            item(key = "history_empty") { HistoryEmptyCard() }
        }
        groups.forEach { (yearMonth, entries) ->
            historyMonthGroup(yearMonth = yearMonth, language = state.language, f = f, entries = entries) { log ->
                HistoryLogCard(state = state, log = log, onDeleteLog = onDeleteLog, onEdit = {
                    if (log.isFuel) onEditFuel(log) else onEditService(log)
                })
            }
        }
        if (timelineLogs.size > JUMP_TOP_THRESHOLD) {
            item(key = "jump_to_top") { JumpToTopPill(listState = listState) }
        }
    }
}
