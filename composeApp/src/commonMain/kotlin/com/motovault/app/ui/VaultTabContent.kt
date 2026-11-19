package com.motovault.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.cost_expense_deleted

@Composable
internal fun ColumnScope.VaultTabContent(
    tab: Tab,
    state: VaultState,
    revision: Long,
    recordDeletedMessage: String,
    onSheet: (ActiveSheet) -> Unit,
    onSwitchTab: (Tab) -> Unit,
    onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit,
    onEditFuel: (RideLog) -> Unit,
    onUndo: (Long) -> Unit,
    onToast: (ToastData?) -> Unit
) {
    val reducedMotion = isReducedMotion
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 10.dp.roundToPx() }
    val garageScroll = rememberScrollState()
    val vaultScroll = rememberScrollState()
    val meScroll = rememberScrollState()
    val historyListState = rememberLazyListState()

    LaunchedEffect(LaunchConfig.scrollToBottom, tab) {
        if (!LaunchConfig.scrollToBottom) return@LaunchedEffect
        when (tab) {
            Tab.GARAGE -> garageScroll.scrollTo(garageScroll.maxValue)
            Tab.VAULT -> vaultScroll.scrollTo(vaultScroll.maxValue)
            Tab.ME -> meScroll.scrollTo(meScroll.maxValue)
            Tab.HISTORY -> historyListState.scrollToItem(maxOf(0, historyListState.layoutInfo.totalItemsCount - 1))
        }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(VaultMotion.pageTween(reducedMotion)) +
                    slideInVertically(VaultMotion.pageTween(reducedMotion)) { slideOffsetPx }) togetherWith
                    (fadeOut(VaultMotion.pageTween(reducedMotion)) +
                        slideOutVertically(VaultMotion.pageTween(reducedMotion)) { slideOffsetPx })
            },
            label = "VaultScreenPage",
            modifier = Modifier.fillMaxSize()
        ) { destination ->
            TabDestinationBody(
                destination = destination,
                state = state,
                revision = revision,
                recordDeletedMessage = recordDeletedMessage,
                scrollState = when (destination) {
                    Tab.GARAGE -> garageScroll
                    Tab.VAULT -> vaultScroll
                    Tab.ME -> meScroll
                    Tab.HISTORY -> null
                },
                historyListState = historyListState,
                onSheet = onSheet,
                onSwitchTab = onSwitchTab,
                onCommit = onCommit,
                onEditFuel = onEditFuel,
                onUndo = onUndo,
                onToast = onToast
            )
        }
    }
}

@Composable
private fun TabDestinationBody(
    destination: Tab,
    state: VaultState,
    revision: Long,
    recordDeletedMessage: String,
    scrollState: ScrollState?,
    historyListState: LazyListState,
    onSheet: (ActiveSheet) -> Unit,
    onSwitchTab: (Tab) -> Unit,
    onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit,
    onEditFuel: (RideLog) -> Unit,
    onUndo: (Long) -> Unit,
    onToast: (ToastData?) -> Unit
) {
    val expenseDeletedMessage = appStringResource(Res.string.cost_expense_deleted)
    val deleteWithToast: (RideLog, String) -> Unit = { log, msg ->
        val next = runCatching { state.deleteLog(log) }.getOrNull()
        if (next != null) {
            onCommit(next) { ok ->
                if (ok) onToast(ToastData(id = "del_${log.id}", message = msg, undoAction = { onUndo(revision + 1) }))
            }
        }
    }
    val modifier = if (scrollState == null) Modifier.fillMaxSize() else Modifier.fillMaxSize().verticalScroll(scrollState)
    Box(modifier = modifier) {
        when (destination) {
            Tab.GARAGE -> GarageScreen(
                state = state,
                onOpenSheet = { name, payload -> onSheet(ActiveSheet(name, payload)) },
                onSwitchTab = onSwitchTab,
                onOpenServiceForItem = { itemKey -> onSheet(ActiveSheet("service", itemKey)) },
                onUpdateState = { next -> onCommit(next, null) },
                onCommitState = { next, done -> onCommit(next, done) }
            )
            Tab.VAULT -> VaultScreen(
                state = state,
                onOpenAddDoc = { onSheet(ActiveSheet("docAdd")) },
                onOpenDocView = { doc -> onSheet(ActiveSheet("docView", doc.id)) },
                onToggleDocReminders = { enabled -> onCommit(state.copy(docReminders = enabled), null) }
            )
            Tab.HISTORY -> HistoryScreen(
                state = state,
                onDeleteLog = { log -> deleteWithToast(log, recordDeletedMessage) },
                onEditFuel = onEditFuel,
                onOpenCost = { name, log -> onSheet(ActiveSheet(name, log)) },
                onEditService = { log -> onSheet(ActiveSheet("serviceEdit", log)) },
                onDeleteExpense = { log -> deleteWithToast(log, expenseDeletedMessage) },
                listState = historyListState
            )
            Tab.ME -> MeScreen(
                state = state,
                onOpenSheet = { name, payload -> onSheet(ActiveSheet(name, payload)) }
            )
        }
    }
}
