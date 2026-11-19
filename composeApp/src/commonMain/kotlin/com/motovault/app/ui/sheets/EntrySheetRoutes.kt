package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.application.fuel.FuelSaveOutcome
import com.motovault.app.domain.ledger.Money
import com.motovault.app.ui.fuel.FuelSheet
import com.motovault.app.ui.history.WorkLedgerSheet
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.fuel_create_conflict
import motovault.composeapp.generated.resources.fuel_create_save_failed
import motovault.composeapp.generated.resources.fuel_edit_conflict
import motovault.composeapp.generated.resources.fuel_edit_save_failed

@Composable
internal fun RenderEntrySheet(ctx: SheetRouteContext) {
    val sheet = ctx.sheet
    when (sheet.name) {
        "expense" -> CostSheetHost(
            sheet = sheet,
            state = ctx.scopedState,
            onDismiss = { ctx.onSheet(null) },
            onSave = { next, done -> ctx.onCommitScoped(next, done) },
            onUndo = { ctx.onUndo(ctx.revision + 1) },
            onToast = ctx.onToast
        )
        "fuel" -> FuelCreateRoute(ctx)
        "fuelEdit" -> FuelEditRoute(ctx)
        "service" -> ServiceRoute(ctx, existing = null)
        "serviceEdit" -> (sheet.props as? RideLog)?.let { ServiceRoute(ctx, existing = it) }
        "paste", "capture" -> CaptureReviewSheet(
            input = sheet.props as? CaptureInput ?: CaptureInput(),
            state = ctx.state,
            factory = ctx.captureDrafts,
            onDismiss = { ctx.onSheet(null) },
            onContinue = { flow, prefill -> ctx.onSheet(ActiveSheet(flow, prefill)) }
        )
        "work-ledger" -> WorkLedgerSheet(
            state = ctx.state,
            onToggleWorkMode = { enabled ->
                ctx.onCommit(ctx.state.copy(ledger = ctx.state.ledger.copy(workModeEnabled = enabled)), null)
            },
            onSaveShift = { nextState, msg ->
                ctx.onCommit(nextState) { ok ->
                    if (ok) {
                        ctx.onToast(
                            ToastData(
                                id = "shift_${Clock.System.now().toEpochMilliseconds()}",
                                message = msg,
                                undoAction = { ctx.onUndo(ctx.revision + 1) }
                            )
                        )
                    }
                }
            },
            onDismiss = { ctx.onSheet(null) }
        )
    }
}

@Composable
private fun FuelCreateRoute(ctx: SheetRouteContext) {
    val sheet = ctx.sheet
    val fuelInput = ctx.state.fuelScreenInput(today()) ?: return
    val failureText = when (ctx.fuelCreateError) {
        FuelCreateError.SAVE_FAILED -> appStringResource(Res.string.fuel_create_save_failed)
        FuelCreateError.CONFLICT -> appStringResource(Res.string.fuel_create_conflict)
        null -> null
    }
    val prefill = sheet.props as? CapturePrefill
    FuelSheet(
        input = fuelInput,
        editError = failureText,
        initialAmount = prefill?.amount?.let(Money::taka) ?: (sheet.props as? Number)?.toDouble(),
        initialOdometer = prefill?.odometer,
        onDismiss = { ctx.onSheet(null); ctx.onFuelCreateError(null) },
        onSave = { request, msg, onDone ->
            val targetBikeId = prefill?.targetBikeId ?: ctx.state.ride?.id
            ctx.fuelWorkflow.create(request.copy(captureReceiptId = prefill?.receipt, targetBikeId = targetBikeId), ctx.revision) { outcome ->
                if (!ctx.scope.isActive) return@create
                if (outcome == FuelSaveOutcome.SAVED) {
                    ctx.onFuelCreateError(null)
                    ctx.onToast(
                        ToastData(
                            id = "fuel_${Clock.System.now().toEpochMilliseconds()}",
                            message = msg,
                            undoAction = { ctx.onUndo(ctx.revision + 1) }
                        )
                    )
                    onDone()
                    ctx.onSheet(null)
                } else {
                    ctx.onFuelCreateError(
                        if (outcome == FuelSaveOutcome.CONFLICT || outcome == FuelSaveOutcome.INVALID) FuelCreateError.CONFLICT
                        else FuelCreateError.SAVE_FAILED
                    )
                }
            }
        }
    )
}

@Composable
private fun FuelEditRoute(ctx: SheetRouteContext) {
    val target = ctx.sheet.props as? FuelEditTarget ?: return
    val fuelInput = ctx.state.fuelScreenInput(today()) ?: return
    val conflictText = appStringResource(Res.string.fuel_edit_conflict)
    val failureText = appStringResource(Res.string.fuel_edit_save_failed)
    FuelSheet(
        input = fuelInput,
        existing = target.record.fuelExistingEntry(),
        editError = ctx.fuelEditError,
        onDismiss = { ctx.onSheet(null); ctx.onFuelEditError(null) },
        onSave = { _, _, _ -> },
        onEdit = { request, message ->
            val scopedRequest = request.copy(targetBikeId = target.record.vehicleId.ifBlank { ctx.state.ride?.id ?: "primary" })
            ctx.fuelWorkflow.edit(scopedRequest, target.revision) { outcome ->
                if (!ctx.scope.isActive) return@edit
                if (outcome == FuelSaveOutcome.SAVED) {
                    ctx.onToast(ToastData(id = "fuel_edit_${request.id}", message = message, undoAction = { ctx.onUndo(target.revision + 1) }))
                    ctx.onSheet(null)
                    ctx.onFuelEditError(null)
                } else {
                    ctx.onFuelEditError(if (outcome == FuelSaveOutcome.FAILED) failureText else conflictText)
                }
            }
        }
    )
}

@Composable
private fun ServiceRoute(ctx: SheetRouteContext, existing: RideLog?) {
    val sheet = ctx.sheet
    val parsedRows = (sheet.props as? CapturePrefill)?.rows ?: (sheet.props as? List<*>)?.filterIsInstance<ParsedLine>()
    ServiceSheet(
        state = ctx.scopedState,
        initialItemKey = if (existing == null) sheet.props as? String else null,
        initialRows = if (existing == null) parsedRows else null,
        existing = existing,
        initialOdometer = if (existing == null) (sheet.props as? CapturePrefill)?.odometer else null,
        onDismiss = { ctx.onSheet(null) },
        onSave = { nextState, msg, onDone ->
            ctx.onCommitScoped(nextState) { ok ->
                if (ok) {
                    ctx.onToast(
                        ToastData(
                            id = "svc_${Clock.System.now().toEpochMilliseconds()}",
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
}
