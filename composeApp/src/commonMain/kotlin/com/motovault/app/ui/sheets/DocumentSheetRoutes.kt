package com.motovault.app.ui

import androidx.compose.runtime.Composable
import com.motovault.app.domain.vault.deleteDocument
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_deleted
import motovault.composeapp.generated.resources.renewal_saved

@Composable
internal fun RenderDocumentSheet(ctx: SheetRouteContext) {
    val sheet = ctx.sheet
    val state = ctx.state
    when (sheet.name) {
        "docAdd" -> DocAddRoute(ctx)
        "docView" -> DocViewRoute(ctx)
        "renewal" -> RenewalRoute(ctx)
    }
}

@Composable
private fun DocAddRoute(ctx: SheetRouteContext) {
    val existingId = ctx.sheet.props as? String
    DocAddSheet(
        state = ctx.state,
        existingId = existingId,
        onDismiss = {
            if (existingId != null) ctx.onSheet(ActiveSheet("docView", existingId)) else ctx.onSheet(null)
        },
        onSave = { nextState, msg, onDone ->
            val targetRevision = ctx.revision + 1
            ctx.onCommit(nextState) { ok ->
                if (ok) {
                    ctx.onToast(
                        ToastData(
                            id = "doc_${Clock.System.now().toEpochMilliseconds()}",
                            message = msg,
                            undoAction = { ctx.onUndo(targetRevision) }
                        )
                    )
                    onDone(true)
                    ctx.onSheet(if (existingId != null) ActiveSheet("docView", existingId) else null)
                } else {
                    onDone(false)
                }
            }
        }
    )
}

@Composable
private fun DocViewRoute(ctx: SheetRouteContext) {
    val docId = ctx.sheet.props as? String ?: return
    val doc = ctx.state.documents.firstOrNull { it.id == docId } ?: return
    val deletedMsg = appStringResource(
        Res.string.doc_deleted,
        documentDisplayName(doc.kind.ifBlank { doc.title })
    )
    DocViewSheet(
        doc = doc,
        onDismiss = { ctx.onSheet(null) },
        onUpdateReminder = { enabled ->
            val updated = ctx.state.documents.map { if (it.id == doc.id) it.copy(reminder = enabled) else it }
            ctx.onCommit(ctx.state.copy(documents = updated), null)
        },
        warningDays = doc.reminderWarningDays ?: ctx.state.reminderPolicy.warningDays,
        privacyEnabled = ctx.state.privacy.enabled,
        onInspect = { ctx.onInspect(doc.id) },
        onOpenPrivacy = { ctx.onSheet(ActiveSheet("privacy")) },
        onRenew = { ctx.onSheet(ActiveSheet("renewal", doc.id)) },
        onEdit = { ctx.onSheet(ActiveSheet("docAdd", doc.id)) },
        onDelete = {
            val undoRevision = ctx.revision + 1
            ctx.onCommit(ctx.state.deleteDocument(doc.id)) { ok ->
                if (ok) {
                    ctx.onSheet(null)
                    ctx.onToast(
                        ToastData(
                            id = "doc_del_${doc.id}_${Clock.System.now().toEpochMilliseconds()}",
                            message = deletedMsg,
                            undoAction = { ctx.onUndo(undoRevision) }
                        )
                    )
                }
            }
        },
        language = ctx.state.language
    )
}

@Composable
private fun RenewalRoute(ctx: SheetRouteContext) {
    val state = ctx.state
    val docId = ctx.sheet.props as? String
    val doc = state.documents.firstOrNull { it.id == docId } ?: return
    val savedMessage = appStringResource(
        Res.string.renewal_saved,
        documentDisplayName(doc.kind.ifBlank { doc.title })
    )
    RenewalSheet(
        state = state,
        doc = doc,
        onDismiss = { ctx.onSheet(ActiveSheet("docView", doc.id)) },
        onChecklist = { step ->
            val latest = ctx.session.snapshots.value
            val current = latest.state.documents.firstOrNull { it.id == doc.id } ?: doc
            val steps = if (step in current.checkedRenewalSteps) current.checkedRenewalSteps - step
            else current.checkedRenewalSteps + step
            runCatching { latest.state.setRenewalChecklist(doc.id, steps) }.getOrNull()
                ?.let { next -> ctx.session.commit(next, latest.revision) { } }
        },
        onSave = { next, done ->
            ctx.session.commit(next, ctx.session.snapshots.value.revision) { ok ->
                if (ok) {
                    val savedRevision = ctx.session.snapshots.value.revision
                    ctx.onToast(
                        ToastData(
                            id = "renewal_${Clock.System.now().toEpochMilliseconds()}",
                            message = savedMessage,
                            undoAction = { ctx.onUndo(savedRevision) }
                        )
                    )
                    done(true)
                    ctx.onSheet(ActiveSheet("docView", doc.id))
                } else {
                    done(false)
                }
            }
        }
    )
}
