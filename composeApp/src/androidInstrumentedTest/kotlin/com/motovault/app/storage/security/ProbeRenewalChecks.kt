package com.motovault.app.storage.security

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import com.motovault.app.RenewalFixtureActivity
import com.motovault.app.ui.renewalPlan
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Synthetic renewal flow only: no owner database, receipt picker file or primary install is used. */
internal fun NativeAssetInstrumentation.checkRenewalUi() {
    checkRenewalFlow()
    checkRenewalExpenseUi()
    checkRenewalThemeUi()
    checkRenewalCancelAndFailure()
}

/** Maintenance and renewal probes are grouped so the probe entry stays within its review trigger. */
internal fun NativeAssetInstrumentation.checkDocumentUi(): List<String> {
    checkMaintenanceUi()
    checkRenewalUi()
    return listOf(
        "native-maintenance-onboarding-and-schedule-details-ui",
        "native-renewal-checklist-receipt-document-expense-and-reminder"
    )
}

private fun NativeAssetInstrumentation.shot(name: String) {
    uiAutomation.takeScreenshot()?.let { bitmap ->
        File(targetContext.cacheDir, name).outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
    }
}

private fun NativeAssetInstrumentation.awaitState(what: String, predicate: () -> Boolean) {
    repeat(30) {
        if (predicate()) return
        SystemClock.sleep(200)
    }
    error("Synthetic renewal state did not reach: $what")
}

private fun NativeAssetInstrumentation.awaitReceiptRemoved(id: String) {
    repeat(25) {
        if (runCatching { ProtectedDocumentStorage.read(id) }.isFailure) return
        SystemClock.sleep(200)
    }
    error("Staged receipt was not removed: $id")
}

private fun NativeAssetInstrumentation.waitScrolled(text: String) {
    repeat(6) {
        if (nodeWithText(text) != null) return
        runCatching { scrollSheetForward() }
        SystemClock.sleep(300)
    }
    waitForText(text)
}

private fun NativeAssetInstrumentation.renewalActivity(
    screen: String, receipt: Boolean, afterSave: String, dark: Boolean = false,
    privacy: Boolean = false, failSave: Boolean = false
): RenewalFixtureActivity = startActivitySync(
    Intent().setClassName(targetContext.packageName, "com.motovault.app.RenewalFixtureActivity")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra("screen", screen)
        .putExtra("receipt", receipt)
        .putExtra("afterSave", afterSave)
        .putExtra("dark", dark)
        .putExtra("privacy", privacy)
        .putExtra("failSave", failSave)
) as RenewalFixtureActivity

private fun NativeAssetInstrumentation.checkRenewalFlow() {
    val renew = renewalActivity("reminder", receipt = true, afterSave = "view")
    val initialDocument = requireNotNull(RenewalFixtureActivity.lastState).documents.single()
    val originalExpiry = initialDocument.expiry
    val before = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    check(renewalPlan(listOf(initialDocument), before, emptyList(), emptySet()).any { it.documentId == initialDocument.id })
    val originalId = requireNotNull(RenewalFixtureActivity.originalPdfId)
    val originalBytes = ProtectedDocumentStorage.read(originalId)
    try {
        waitForText("ডকুমেন্টের রিমাইন্ডার")
        clickText("কাগজ খুলুন")
        waitForText("ট্যাক্স টোকেন")
        clickText("নবায়ন")
        waitForText("যত টাকা দিয়েছেন")
        clickText("আগের ট্যাক্স টোকেন")
        awaitState("first checked step") {
            "tax_previous_token" in requireNotNull(RenewalFixtureActivity.lastState).documents.single().checkedRenewalSteps
        }
        clickText("ফি জমা")
        awaitState("two checked steps") {
            requireNotNull(RenewalFixtureActivity.lastState).documents.single().checkedRenewalSteps.size == 2
        }
        var stepNode: AccessibilityNodeInfo? = waitForText("আগের ট্যাক্স টোকেন")
        while (stepNode != null && !stepNode.isCheckable) stepNode = stepNode.parent
        check(stepNode?.isCheckable == true)
        setEditable("যত টাকা দিয়েছেন", "1250")
        shot("synthetic-p05-renewal-checklist.png")
        clickLastText("নবায়ন সেভ করুন")
        waitScrolled("নবায়নের ইতিহাস")
        waitScrolled("রসিদ সংযুক্ত")
        shot("synthetic-p05-renewed-document.png")

        val state = requireNotNull(RenewalFixtureActivity.lastState)
        val document = state.documents.single()
        val receiptId = requireNotNull(RenewalFixtureActivity.stagedReceiptId)
        check(document.renewals.size == 1)
        check(document.revision == 1)
        check(document.checkedRenewalSteps.isEmpty())
        check(document.expiry != originalExpiry)
        val renewal = document.renewals.single()
        check(renewal.previousExpiry == originalExpiry)
        check(renewal.newExpiry == document.expiry)
        check(renewal.documentRevision == 1)
        check(renewal.receiptId == receiptId)
        check(renewal.feePoisha == 125000)
        check(renewal.feeLogId != null)
        check(renewal.kind == "tax")
        check(ProtectedDocumentStorage.read(receiptId).isNotEmpty())
        check(ProtectedDocumentStorage.read(originalId).contentEquals(originalBytes))
        val fee = state.logs.single()
        check(fee.kind == "document_fee")
        check(fee.cost == 1250.0)
        check(fee.documentId == document.id)
        check(fee.renewalId == renewal.id)
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        check(renewalPlan(state.documents, now, emptyList(), emptySet()).any {
            it.documentId == document.id && it.kind in setOf("warning", "expiry") && it.date > now.date.toString()
        })
    } finally {
        runOnMainSync { renew.finish() }
    }
}

private fun NativeAssetInstrumentation.checkRenewalExpenseUi() {
    val history = renewalActivity("renew", receipt = false, afterSave = "history")
    try {
        waitForText("যত টাকা দিয়েছেন")
        setEditable("যত টাকা দিয়েছেন", "1250")
        clickLastText("নবায়ন সেভ করুন")
        waitForText("ট্যাক্স টোকেন নবায়ন")
        waitForText("৳১,২৫০.০০")
        shot("synthetic-p05-renewal-expense.png")
    } finally {
        runOnMainSync { history.finish() }
    }
}

private fun NativeAssetInstrumentation.checkRenewalCancelAndFailure() {
    val cancel = renewalActivity("view", receipt = true, afterSave = "view")
    val cancelExpiry = requireNotNull(RenewalFixtureActivity.lastState).documents.single().expiry
    val cancelReceipt = requireNotNull(RenewalFixtureActivity.stagedReceiptId)
    val cancelOriginal = requireNotNull(RenewalFixtureActivity.originalPdfId)
    val cancelBytes = ProtectedDocumentStorage.read(cancelOriginal)
    try {
        clickText("নবায়ন")
        waitForText("যত টাকা দিয়েছেন")
        setEditable("যত টাকা দিয়েছেন", "1250")
        uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        waitForText("ট্যাক্স টোকেন")
        SystemClock.sleep(500)
        check(requireNotNull(RenewalFixtureActivity.lastState).documents.single().expiry == cancelExpiry)
        awaitReceiptRemoved(cancelReceipt)
        check(ProtectedDocumentStorage.read(cancelOriginal).contentEquals(cancelBytes))
    } finally {
        runOnMainSync { cancel.finish() }
    }

    val fail = renewalActivity("renew", receipt = true, afterSave = "view", failSave = true)
    val failExpiry = requireNotNull(RenewalFixtureActivity.lastState).documents.single().expiry
    val failReceipt = requireNotNull(RenewalFixtureActivity.stagedReceiptId)
    try {
        waitForText("যত টাকা দিয়েছেন")
        setEditable("যত টাকা দিয়েছেন", "1250")
        clickLastText("নবায়ন সেভ করুন")
        waitForText("নবায়ন সেভ হয়নি। আবার চেষ্টা করুন।")
        val failed = requireNotNull(RenewalFixtureActivity.lastState)
        check(failed.documents.single().expiry == failExpiry)
        check(failed.documents.single().renewals.isEmpty())
        check(failed.logs.isEmpty())
        uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        waitForText("ট্যাক্স টোকেন")
        awaitReceiptRemoved(failReceipt)
    } finally {
        runOnMainSync { fail.finish() }
    }
}

private fun NativeAssetInstrumentation.checkRenewalThemeUi() {
    val dark = renewalActivity("renew", receipt = false, afterSave = "view", dark = true)
    try {
        waitForText("যত টাকা দিয়েছেন")
        shot("synthetic-p05-renewal-dark.png")
    } finally {
        runOnMainSync { dark.finish() }
    }

    val privacy = renewalActivity("view", receipt = false, afterSave = "view", privacy = true)
    try {
        waitScrolled("শুধু এই ডকুমেন্ট দেখান")
        shot("synthetic-p05-renewal-privacy.png")
    } finally {
        runOnMainSync { privacy.finish() }
    }
}
