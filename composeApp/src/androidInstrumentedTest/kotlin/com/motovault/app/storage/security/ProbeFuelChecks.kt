package com.motovault.app.storage.security

import com.motovault.app.storage.backupSnapshot
import com.motovault.app.storage.backupRideDisplayName

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.MotionEvent
import java.io.File
import java.security.KeyStore
import java.util.UUID
import com.motovault.app.ui.*
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import com.motovault.app.domain.backup.*
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import kotlin.experimental.xor
import androidx.room.Room
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import com.motovault.app.storage.BackupRestoreStaging
import com.motovault.app.storage.StagedRestore
import com.motovault.app.storage.RecoverySnapshotStore
import com.motovault.app.storage.CoordinatedRecovery
import com.motovault.app.storage.FuelEntryAdapter
import com.motovault.app.storage.VaultSession
import com.motovault.app.application.fuel.FuelEditRequest
import kotlinx.datetime.LocalDate
import com.motovault.app.application.PreparedVaultChange
import com.motovault.app.application.VaultCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import java.util.Locale
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.fuel_entry_count
import motovault.composeapp.generated.resources.fuel_title
import motovault.composeapp.generated.resources.history_record_count

internal fun NativeAssetInstrumentation.checkDurableFuelEdit(root: File) = runBlocking {
        val path = File(root, "synthetic-fuel-edit.db").absolutePath
        fun open() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(targetContext, path))
        var database = open()
        try {
            val previous = RideLog("Fuel fill-up", "legacy detail", "2020-01-01", 1000.0, id = "fuel-first", kind = "fuel", liters = 5.0, cost = 590.0, fuelPrice = null, notes = "synthetic note")
            val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0), logs = listOf(previous),
                documents = listOf(VaultDocument("Synthetic document", "TEST", "Synthetic", "2030-01-01", id = "synthetic-doc")))
            database.vault().replace(original)
            val baseline = requireNotNull(database.vault().read())
            val writer = VaultSession(this, restore = { requireNotNull(database.vault().read()) }, write = { database.vault().replace(it) }, cleanup = {}, recover = {})
            writer.snapshots.first { !it.loading }
            val request = FuelEditRequest(previous.id, 600.0, 5.0, null, 1000.0, LocalDate(2020, 1, 1), true, "octane", "", null)
            val changed = FuelEntryAdapter.edit(writer.snapshots.value.state, request)
            val saved = CompletableDeferred<Boolean>()
            writer.commit(changed, 0) { saved.complete(it) }
            check(saved.await())
            database.close()
            database = open()
            val reopened = requireNotNull(database.vault().read())
            check(reopened.logs.single().cost == 600.0)
            check(reopened.logs.single().id == previous.id && reopened.logs.single().notes == previous.notes && reopened.logs.single().fuelPrice == null)
            check(reopened.documents == baseline.documents)
            val stale = CompletableDeferred<Boolean>()
            writer.commit(baseline, 0) { stale.complete(it) }
            check(!stale.await())
            check(database.vault().read()?.logs?.single()?.cost == 600.0)
            val undone = CompletableDeferred<Boolean>()
            writer.undo(1) { undone.complete(it) }
            check(undone.await())
            database.close()
            database = open()
            check(database.vault().read() == baseline)
        } finally {
            database.close()
        }
    }

internal fun NativeAssetInstrumentation.checkFuelEditUi() {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.FuelEditFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("language", "en")
        val activity = startActivitySync(intent) as com.motovault.app.FuelEditFixtureActivity
        try {
            clickText("Fuel fill · ৪.৯০ L")
            clickText("Edit fuel record")
            waitForText("How much did you spend?")
            waitForText("≈ ৪.৯০ L")
            clickText("৬")
            clickText("০")
            clickText("০")
            waitForText("৬০০")
            uiAutomation.takeScreenshot()?.let { bitmap ->
                File(targetContext.cacheDir, "synthetic-fuel-edit-footer.png").outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            }
            waitForText("Save ৳৬০০")
            clickText("Save ৳৬০০")
            // The toast is transient; verify the committed edit through the
            // screen and a fresh connection, then use the durable Undo path.
            waitForText("Fuel fill · ৫.০০ L")
            check(activity.savedFuelMatches(600.0, 5.0))
            check(activity.undoLast()) { "Durable Undo failed" }
            waitForText("Fuel fill · ৪.৯০ L")
            check(activity.savedFuelMatches(590.0, 4.9))
        } finally {
            runOnMainSync { activity.finish() }
        }
    }

internal fun NativeAssetInstrumentation.checkFuelCreateFailureUi() {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.FuelEditFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("language", "en").putExtra("failWrites", true)
        val activity = startActivitySync(intent) as com.motovault.app.FuelEditFixtureActivity
        try {
            val before = activity.savedFuelCount()
            waitForText("How much did you spend?")
            clickText("৬")
            waitForText("≈ ০.০৫ L")
            clickText("০")
            waitForText("≈ ০.৫০ L")
            clickText("০")
            waitForText("৬০০")
            clickText("Save ৳৬০০")
            waitForText("Could not save. Your draft is still here. Try again.")
            waitForText("Save ৳৬০০")
            check(activity.savedFuelCount() == before)
        } finally {
            runOnMainSync { activity.finish() }
        }
    }

internal fun NativeAssetInstrumentation.checkHistoryLocale(language: String, digits: String) {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.FuelEditFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("language", language).putExtra("digits", digits)
        val activity = startActivitySync(intent)
        try {
            val date = LocalDate.fromEpochDays(today().toEpochDays() - 11)
            val formatter = Fmt(if (digits == "en") Digits.EN else Digits.BN)
            val month = java.text.DateFormatSymbols.getInstance(Locale.forLanguageTag(language)).months[date.monthNumber - 1]
            val unit = if (language == "en") "km" else "কিমি"
            waitForText("${formatter.d(date.dayOfMonth.toString())} $month · ${formatter.n(12340L)} $unit")
            if (language == "en") {
                waitForText("Cost, last ৩০ days")
                waitForText("Fuel fill · ৪.৯০ L")
                waitForText("Fuel economy")
                waitForText("Documents")
            } else {
                waitForText("গত 30 দিনের খরচ")
                waitForText("তেল ভরা · 4.90 লিটার")
                waitForText("জ্বালানি গড়")
                waitForText("কাগজপত্র")
            }
            uiAutomation.takeScreenshot()?.let { bitmap ->
                File(targetContext.cacheDir, "synthetic-history-$language-$digits.png").outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            }
        } finally { runOnMainSync { activity.finish() } }
    }


