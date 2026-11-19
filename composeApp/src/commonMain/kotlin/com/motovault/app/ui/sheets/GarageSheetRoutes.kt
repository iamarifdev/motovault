package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.ui.garage.BikeSwitcherSheet
import com.motovault.app.ui.garage.BreakdownHelpSheet
import kotlinx.datetime.Clock

@Composable
internal fun RenderGarageSheet(ctx: SheetRouteContext) {
    val sheet = ctx.sheet
    val state = ctx.state
    when (sheet.name) {
        "quickAdd" -> QuickAddSheet(
            onDismiss = { ctx.onSheet(null) },
            onSelectFlow = { flow -> ctx.onSheet(ActiveSheet(flow)) }
        )
        "gaugeDetail" -> GaugeDetailSheet(
            state = state,
            id = sheet.props as? String,
            onDismiss = { ctx.onSheet(null) },
            onOpen = { name, id -> ctx.onSheet(ActiveSheet(name, id)) },
            onSaveSchedule = { next, msg, closeAfter ->
                ctx.onCommit(next) { ok ->
                    if (ok) {
                        ctx.onToast(
                            ToastData(
                                id = "gauge_${Clock.System.now().toEpochMilliseconds()}",
                                message = msg,
                                undoAction = { ctx.onUndo(ctx.revision + 1) }
                            )
                        )
                        if (closeAfter) ctx.onSheet(null)
                    }
                }
            }
        )
        "odometer" -> OdometerSheet(
            state = ctx.scopedState,
            initialOdometer = (sheet.props as? CapturePrefill)?.odometer,
            onDismiss = { ctx.onSheet(null) },
            onSave = { nextState, msg, onDone ->
                ctx.onCommitScoped(nextState) { ok ->
                    if (ok) {
                        ctx.onToast(
                            ToastData(
                                id = "odo_${Clock.System.now().toEpochMilliseconds()}",
                                message = msg,
                                undoAction = { ctx.onUndo(ctx.revision + 1) }
                            )
                        )
                        onDone()
                        ctx.onSheet(null)
                    }
                }
            }
        )
        "bikeSwitcher" -> BikeSwitcherSheetRoute(ctx)
        "breakdown" -> BreakdownHelpSheet(
            state = state,
            onDismiss = { ctx.onSheet(null) },
            onOpenDoc = { docId -> ctx.onSheet(ActiveSheet("docView", docId)) },
            onOpenVaultTab = { ctx.onSheet(null); ctx.onSwitchTab(Tab.VAULT) }
        )
    }
}

@Composable
private fun BikeSwitcherSheetRoute(ctx: SheetRouteContext) {
    BikeSwitcherSheet(
        state = ctx.state,
        onDismiss = { ctx.onSheet(null) },
        onSwitchBike = { next, msg ->
            ctx.onCommit(next) { ok ->
                if (ok) {
                    ctx.onToast(ToastData(id = "bike_${Clock.System.now().toEpochMilliseconds()}", message = msg))
                    ctx.onSheet(null)
                }
            }
        },
        onToggleArchive = { next, msg, _ ->
            ctx.onCommit(next) { ok ->
                if (ok) {
                    ctx.onToast(
                        ToastData(
                            id = "bike_arch_${Clock.System.now().toEpochMilliseconds()}",
                            message = msg,
                            undoAction = { ctx.onUndo(ctx.revision + 1) }
                        )
                    )
                }
            }
        },
        onAddBike = ctx.onStartOnboarding
    )
}
