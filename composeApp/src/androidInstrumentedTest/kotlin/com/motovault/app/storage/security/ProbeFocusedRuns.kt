package com.motovault.app.storage.security

import android.app.Activity
import android.os.Bundle

internal fun NativeAssetInstrumentation.runFocusedProbe(backup: Boolean, picker: Boolean, roundTrip: Boolean, history: Boolean, costs: Boolean): Boolean {
    if (!backup && !picker && !roundTrip && !history && !costs) return false
    val result = Bundle()
    try {
        val message = when {
            costs -> checkCostRuntime()
            history -> { checkHistoryLocale("en", "bn"); checkHistoryLocale("bn", "en"); "synthetic history language and digit combinations" }
            roundTrip -> { checkBackupRoundTripUi(); "synthetic backup UI restore and Room reopen" }
            else -> { checkBackupCreateUi(); "synthetic backup creation UI" }
        }
        result.putString("stream", "PASS $message\n")
        finish(Activity.RESULT_OK, result)
    } catch (failure: Throwable) {
        result.putString("stream", "FAIL focused native probe: ${failure.stackTraceToString()}\n")
        finish(Activity.RESULT_CANCELED, result)
    }
    return true
}
