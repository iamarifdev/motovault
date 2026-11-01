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

/** No owner database, production key alias or primary app installation is used by this probe. */
class NativeAssetInstrumentation : Instrumentation() {
    private var backupOnly = false
    private var pickerOnly = false
    private var roundTripOnly = false
    private var historyLocaleOnly = false
    private fun checkRestoreRecoveryTransaction(root: File) = runBlocking {
        val path = File(root, "synthetic-restore-recovery.db").absolutePath
        val recoveryAlias = "motovault-recovery-probe-${UUID.randomUUID()}"
        AndroidAssetKey.load(true, recoveryAlias)
        try {
        val cipher = RecoverySnapshotCipher(AndroidAssetCipher { AndroidAssetKey.load(false, recoveryAlias) })
        val recoveryId = "synthetic-recovery.pdf"
        fun open() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(targetContext, path))
        var db = open()
        val original = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0, modelName = "Original"),
            documents = listOf(VaultDocument("Synthetic", "1", "Synthetic", "2030-01-01", id = "recovery-doc",
                pages = listOf(DocumentPage("recovery-original.jpg", "recovery-preview.jpg", "image/jpeg", 10, 10, "Front")))))
        val replacement = VaultState(ride = Ride("test", "Synthetic", 2021, 2000.0, modelName = "Restored"))
        db.vault().replace(original)
        val previous = VaultState.encode(requireNotNull(db.vault().read()))
        val sealed = cipher.seal(recoveryId, previous.encodeToByteArray())
        var staleRejected = false
        try { db.vault().replaceWithRecovery(replacement, recoveryId, "stale", sealed) }
        catch (_: IllegalStateException) { staleRejected = true }
        check(staleRejected && db.vault().restoreRecoveryRows().isEmpty())
        check(db.vault().read()?.ride?.modelName == "Original")
        val badVerifier = RecoverySnapshotStore(db.vault(), { recoveryId }, cipher::seal, { _, _ -> byteArrayOf(0) })
        var verificationRejected = false
        try { badVerifier.replace(requireNotNull(db.vault().read()), replacement) }
        catch (_: IllegalStateException) { verificationRejected = true }
        check(verificationRejected && db.vault().restoreRecoveryRows().isEmpty())
        check(db.vault().read()?.ride?.modelName == "Original")
        val snapshots = RecoverySnapshotStore(db.vault(), { recoveryId }, cipher::seal, cipher::open)
        snapshots.replace(requireNotNull(db.vault().read()), replacement)
        check(db.vault().read()?.ride?.modelName == "Restored")
        check(snapshots.referencedAssets() == setOf("recovery-original.jpg", "recovery-preview.jpg"))
        val summary = snapshots.summaries().single()
        check(summary.id == recoveryId && summary.createdAtEpochMillis == null)
        check(summary.bikeName == "Original" && summary.documentCount == 1 && summary.recordCount == 0)
        check(!db.vault().restoreRecoveryRows().single().payload.decodeToString().contains("Original"))
        check(VaultState.decode(cipher.open(recoveryId, db.vault().restoreRecoveryRows().single().payload).decodeToString())?.ride?.modelName == "Original")
        try {
            val nextPrevious = VaultState.encode(requireNotNull(db.vault().read()))
            db.vault().replaceWithRecovery(original, recoveryId, nextPrevious, cipher.seal(recoveryId, nextPrevious.encodeToByteArray()))
            error("Duplicate recovery ID was accepted")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // The existing recovery record must not be overwritten.
        } catch (_: androidx.sqlite.SQLiteException) {
            // Bundled SQLite reports a constraint error through this type.
        }
        check(db.vault().read()?.ride?.modelName == "Restored")
        db.close()
        db = open()
        check(db.vault().read()?.ride?.modelName == "Restored")
        check(VaultState.decode(cipher.open(recoveryId, db.vault().restoreRecoveryRows().single().payload).decodeToString())?.ride?.modelName == "Original")
        check(RecoverySnapshotStore(db.vault(), { recoveryId }, cipher::seal, cipher::open).referencedAssets() == setOf("recovery-original.jpg", "recovery-preview.jpg"))
        val savedBlob = db.vault().restoreRecoveryRows().single().payload.copyOf()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(recoveryAlias) }
        var unavailableRejected = false
        try { RecoverySnapshotStore(db.vault(), { recoveryId }, cipher::seal, cipher::open).referencedAssets() }
        catch (_: Exception) { unavailableRejected = true }
        check(unavailableRejected)
        check(db.vault().restoreRecoveryRows().single().payload.contentEquals(savedBlob))
        check(db.vault().read()?.ride?.modelName == "Restored")
        var attemptedKeyCreation = false
        val failClosedStore = RecoverySnapshotStore(db.vault(), { "next-recovery.pdf" },
            seal = { id, bytes ->
                attemptedKeyCreation = true
                RecoverySnapshotCipher(AndroidAssetCipher { AndroidAssetKey.load(true, recoveryAlias) }).seal(id, bytes)
            }, open = cipher::open)
        var replacementRejected = false
        try { failClosedStore.replace(requireNotNull(db.vault().read()), original) }
        catch (_: Exception) { replacementRejected = true }
        check(replacementRejected && !attemptedKeyCreation)
        check(db.vault().read()?.ride?.modelName == "Restored")
        check(db.vault().restoreRecoveryRows().single().payload.contentEquals(savedBlob))
        db.close()
        } finally {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(recoveryAlias) }
        }
    }

    private fun checkDurableFuelEdit(root: File) = runBlocking {
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

    private fun nodeWithText(text: String): AccessibilityNodeInfo? {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            node.refresh()
            if (node.text?.toString() == text || node.contentDescription?.toString() == text) return node
            val indices = if (text == "আনডু") (node.childCount - 1 downTo 0) else (0 until node.childCount)
            for (index in indices) find(node.getChild(index))?.let { return it }
            return null
        }
        return find(uiAutomation.rootInActiveWindow)
    }

    private fun waitForAnyText(vararg texts: String): AccessibilityNodeInfo {
        repeat(80) {
            for (text in texts) nodeWithText(text)?.let { return it }
            SystemClock.sleep(250)
        }
        val root = uiAutomation.rootInActiveWindow
        if (root?.packageName?.toString() != targetContext.packageName) {
            error("Synthetic UI is not active; foreground package=${root?.packageName}")
        }
        fun describe(node: AccessibilityNodeInfo?, depth: Int): List<String> {
            if (node == null || depth > 20) return emptyList()
            val current = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            return current + (0 until node.childCount).flatMap { describe(node.getChild(it), depth + 1) }
        }
        val visible = describe(root, 0)
        error("Synthetic UI label not found: ${texts.joinToString("/")}; root=$root; matching=$visible")
    }

    private fun waitForText(text: String): AccessibilityNodeInfo {
        repeat(80) {
            nodeWithText(text)?.let { return it }
            SystemClock.sleep(250)
        }
        val root = uiAutomation.rootInActiveWindow
        if (root?.packageName?.toString() != targetContext.packageName) {
            error("Synthetic UI is not active; foreground package=${root?.packageName}")
        }
        fun describe(node: AccessibilityNodeInfo?, depth: Int): List<String> {
            if (node == null || depth > 20) return emptyList()
            val current = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            return current + (0 until node.childCount).flatMap { describe(node.getChild(it), depth + 1) }
        }
        val visible = describe(root, 0)
        error("Synthetic UI label not found: $text; root=$root; matching=$visible")
    }

    private fun clickText(text: String) {
        var node: AccessibilityNodeInfo? = waitForText(text)
        while (node != null && !node.isClickable) node = node.parent
        check(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { "Synthetic UI control not clickable: $text" }
    }

    private fun clickAnyText(vararg texts: String) {
        val label = waitForAnyText(*texts)
        var node: AccessibilityNodeInfo? = label
        // Compose may expose the toast label as a non-clickable text node whose
        // parent performs the action only through ACTION_CLICK, so try each level.
        while (node != null) {
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
            node = node.parent
        }
        // Last resort for visible Compose controls without an exposed click action.
        val bounds = Rect()
        label.getBoundsInScreen(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            val now = SystemClock.uptimeMillis()
            val x = bounds.exactCenterX()
            val y = bounds.exactCenterY()
            val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
            val up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, x, y, 0)
            if (uiAutomation.injectInputEvent(down, true) && uiAutomation.injectInputEvent(up, true)) {
                down.recycle(); up.recycle()
                SystemClock.sleep(300)
                return
            }
            down.recycle(); up.recycle()
        }
        error("Synthetic UI control not clickable: ${texts.joinToString("/")}")
    }

    private fun tryClickAnyText(timeoutMs: Long, vararg texts: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            for (text in texts) {
                val found = nodeWithText(text) ?: continue
                var node: AccessibilityNodeInfo? = found
                while (node != null) {
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                    node = node.parent
                }
                val bounds = Rect()
                found.getBoundsInScreen(bounds)
                if (bounds.width() > 0 && bounds.height() > 0) {
                    val now = SystemClock.uptimeMillis()
                    val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                    val up = MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_UP, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                    val sent = uiAutomation.injectInputEvent(down, true) && uiAutomation.injectInputEvent(up, true)
                    down.recycle(); up.recycle()
                    if (sent) { SystemClock.sleep(300); return true }
                }
            }
            SystemClock.sleep(150)
        }
        return false
    }

    private fun setEditable(description: String, value: String) {
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable && node.contentDescription?.toString()?.startsWith(description) == true) return node
            for (index in 0 until node.childCount) find(node.getChild(index))?.let { return it }
            return null
        }
        var target: AccessibilityNodeInfo? = null
        for (attempt in 0 until 80) {
            target = find(uiAutomation.rootInActiveWindow)
            if (target != null) break
            SystemClock.sleep(250)
        }
        val field = requireNotNull(target) { "Synthetic password field not found: $description" }
        field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        check(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
        SystemClock.sleep(500)
    }

    private fun scrollSheetForward() {
        fun scrollables(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
            if (node == null) return emptyList()
            return (0 until node.childCount).flatMap { scrollables(node.getChild(it)) } + if (node.isScrollable) listOf(node) else emptyList()
        }
        val scrollable = scrollables(uiAutomation.rootInActiveWindow).firstOrNull() ?: error("Synthetic sheet is not scrollable")
        check(scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
        SystemClock.sleep(500)
    }

    private fun clickLastText(text: String) {
        fun collect(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
            if (node == null) return emptyList()
            node.refresh()
            val found = if (node.text?.toString() == text || node.contentDescription?.toString() == text) listOf(node) else emptyList()
            return found + (0 until node.childCount).flatMap { collect(node.getChild(it)) }
        }
        fun lastClickable(): AccessibilityNodeInfo? = collect(uiAutomation.rootInActiveWindow).asReversed().firstNotNullOfOrNull { found ->
            var node: AccessibilityNodeInfo? = found
            while (node != null && !node.isClickable) node = node.parent
            node
        }
        val initial = requireNotNull(lastClickable()) { "Synthetic control not clickable: $text" }
        initial.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        SystemClock.sleep(500)
        check(requireNotNull(lastClickable()).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }


    private fun checkMaintenanceRuntime() {
        val ride = Ride("yamaha", "BD", 2023, 6000.0, "yamaha-r15-v4", "R15 V4",
            market = RideMarket.BANGLADESH, emission = RideEmission.BS6, modelCode = "YZF155-A",
            factoryProfileId = "yamaha-r15-v4-2023-bd-155")
        val known = VaultState.create(ride, 1000.0)
        check(known.rules.any { it.action == com.motovault.app.domain.maintenance.MaintenanceAction.LUBRICATE })
        check(known.rules.any { it.action == com.motovault.app.domain.maintenance.MaintenanceAction.ADJUST })
        check(known.rules.any { it.action == com.motovault.app.domain.maintenance.MaintenanceAction.INSPECT })
        check(known.rules.first { it.name == "Engine oil" }.provenance.revision == "BGN-F819V-E0")
        check(VaultState.decode(VaultState.encode(known)) == known)
        val unknown = VaultState.create(ride, null)
        check(unknown.logs.isEmpty())
        check(unknown.rules.all { it.baselineCertainty == com.motovault.app.domain.model.BaselineCertainty.UNKNOWN })
        val unsupported = VaultState.create(Ride("yamaha", "BD", 2023, 6000.0, "yamaha-r15-v3", "R15 V3",
            market = RideMarket.BANGLADESH, emission = RideEmission.BS6, modelCode = "YZF155-A"))
        check(unsupported.rules.all { it.provenance.source == com.motovault.app.domain.maintenance.ScheduleSource.OWNER })
        val runningState = VaultState.create(ride, 0.0)
        val runningIn = runningState.rules.first { it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RUNNING_IN }
        val oil = runningState.rules.first { it.name == "Engine oil" && it.phase == com.motovault.app.domain.maintenance.SchedulePhase.RECURRING }
        val serviced = runningState.logService(runningIn, odometer = 1000.0, date = today())
        check(serviced.rules.none { it.id == runningIn.id })
        check(serviced.rules.first { it.id == oil.id }.lastServicedOdometerKm == 1000.0)
    }


    private fun checkMaintenanceUi() {
        fun shot(name: String) {
            uiAutomation.takeScreenshot()?.let { bitmap ->
                File(targetContext.cacheDir, name).outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            }
        }
        val onboarding = startActivitySync(
            Intent().setClassName(targetContext.packageName, "com.motovault.app.MaintenanceFixtureActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("screen", "onboarding").putExtra("modelId", "yamaha-r15-v4")
        ) as com.motovault.app.MaintenanceFixtureActivity
        try {
            waitForText("শেষ সার্ভিস মনে নেই")
            shot("synthetic-p04-onboarding-unknown.png")
            clickText("শেষ সার্ভিস মনে আছে")
            waitForText("শেষ সার্ভিসের ওডোমিটার")
            shot("synthetic-p04-onboarding-known.png")
        } finally {
            runOnMainSync { onboarding.finish() }
        }
        val details = startActivitySync(
            Intent().setClassName(targetContext.packageName, "com.motovault.app.MaintenanceFixtureActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("screen", "details").putExtra("modelId", "yamaha-r15-v4")
        ) as com.motovault.app.MaintenanceFixtureActivity
        try {
            waitForText("Yamaha R15 V4 owner's manual · rev BGN-F819V-E0 · p. 8-5")
            waitForText("যেসব মডেলে প্রযোজ্য: India-market Yamaha owner's manual for the same model code; Bangladesh catalogue model. Confirm year, variant and local paperwork.")
            shot("synthetic-p04-schedule-details.png")
        } finally {
            runOnMainSync { details.finish() }
        }
    }

    private fun checkFuelEditUi() {
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

    private fun checkFuelCreateFailureUi() {
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

    private fun checkHistoryLocale(language: String, digits: String) {
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

    private fun checkRecoveryUi(language: String, conflict: Boolean) {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.RecoveryFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("language", language).putExtra("conflict", conflict)
        val activity = startActivitySync(intent)
        try {
            val english = language == "en"
            waitForText("Demo Bike")
            clickText(if (english) "Review vault" else "ভল্টটি দেখুন")
            waitForText(if (english) "Return to this vault?" else "এই ভল্টে ফিরে যাবেন?")
            clickText(if (english) "Return to vault" else "ফিরে যান")
            val progress = if (english) "Restoring the vault…" else "ভল্ট রিস্টোর করা হচ্ছে…"
            waitForText(progress)
            check(uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
            waitForText(progress)
            val outcome = if (conflict) {
                if (english) "Your current records changed. Review the vault again before continuing."
                else "আপনার বর্তমান তথ্য বদলেছে। আবার দেখে নিশ্চিত করুন।"
            } else {
                if (english) "The previous vault is restored." else "আগের ভল্টে ফিরে গেছেন।"
            }
            waitForText(outcome)
            uiAutomation.takeScreenshot()?.let { bitmap ->
                File(targetContext.cacheDir, "synthetic-recovery-$language.png").outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            }
        } finally {
            runOnMainSync { activity.finish() }
        }
    }

    private fun checkDurableRecoveryUi() {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.RecoveryDurableFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = startActivitySync(intent) as com.motovault.app.RecoveryDurableFixtureActivity
        try {
            waitForText("Previous Bike")
            clickText("Review vault")
            clickText("Return to vault")
            waitForText("The previous vault is restored.")
            check(activity.verifyReopened())
        } finally {
            runOnMainSync { activity.finish() }
        }
    }

    private fun checkBackupLocalizationUi(language: String) {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.RecoveryFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("language", language).putExtra("sheet", "backup")
        val activity = startActivitySync(intent)
        try {
            val english = language == "en"
            waitForText(if (english) "Backup and restore" else "ব্যাকআপ ও রিস্টোর")
            waitForText(if (english) "Secure encrypted backup" else "নিরাপদ ও এনক্রিপ্টেড ব্যাকআপ")
            clickText(if (english) "Restore" else "রিস্টোর")
            waitForText(if (english) "Restore from a backup" else "ব্যাকআপ ফাইল থেকে রিস্টোর")
            waitForText(if (english) "Choose a backup file (.mva)" else "ব্যাকআপ ফাইল (.mva) নির্বাচন করুন")
            uiAutomation.takeScreenshot()?.let { bitmap ->
                File(targetContext.cacheDir, "synthetic-backup-$language.png").outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            }
        } finally {
            runOnMainSync { activity.finish() }
        }
    }

    private fun checkBackupCreateUi() {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.RecoveryFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("language", "en").putExtra("sheet", "backup")
        val activity = startActivitySync(intent)
        try {
            waitForText("Secure encrypted backup")
            setEditable("Backup password (at least", "synthetic-passphrase")
            setEditable("Confirm password", "synthetic-passphrase")
            scrollSheetForward()
            uiAutomation.takeScreenshot()?.let { bitmap ->
                File(targetContext.cacheDir, "synthetic-backup-create-before.png").outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            }
            clickLastText("Create backup")
            waitForText("Encrypted backup ready")
            uiAutomation.takeScreenshot()?.let { bitmap ->
                File(targetContext.cacheDir, "synthetic-backup-created.png").outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
            }
            if (pickerOnly) {
                clickText("Save to files")
                clickText("SAVE")
                waitForText("Backup file saved.")
            }
        } finally {
            runOnMainSync { activity.finish() }
        }
    }

    private fun setPickerFilename(name: String) {
        waitForText("SAVE")
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isEditable && node.packageName?.toString() == "com.google.android.documentsui") return node
            for (index in 0 until node.childCount) find(node.getChild(index))?.let { return it }
            return null
        }
        var field: AccessibilityNodeInfo? = null
        repeat(80) {
            if (field == null) {
                field = find(uiAutomation.rootInActiveWindow)
                if (field == null) SystemClock.sleep(250)
            }
        }
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, name) }
        check(requireNotNull(field) { "System picker filename field not found" }.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args))
    }

    private fun checkBackupRoundTripUi() {
        val intent = Intent().setClassName(targetContext.packageName, "com.motovault.app.BackupRoundTripFixtureActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = startActivitySync(intent) as com.motovault.app.BackupRoundTripFixtureActivity
        val filename = "motovault-synthetic-${UUID.randomUUID()}.mva"
        try {
            waitForText("Secure encrypted backup")
            setEditable("Backup password (at least", "synthetic-passphrase")
            setEditable("Confirm password", "synthetic-passphrase")
            scrollSheetForward()
            clickLastText("Create backup")
            waitForText("Encrypted backup ready")
            clickText("Save to files")
            setPickerFilename(filename)
            clickText("SAVE")
            waitForText("Backup file saved.")
            val changed = CompletableDeferred<Boolean>()
            runOnMainSync { activity.replaceSyntheticCurrent { changed.complete(it) } }
            check(runBlocking { changed.await() })
            check(activity.isCurrentBike())
            clickText("Restore")
            clickText("Choose a backup file (.mva)")
            clickText(filename)
            setEditable("Enter the backup password", "synthetic-passphrase")
            clickLastText("Check backup")
            waitForText("Confirm restore")
            clickText("Confirm restore")
            waitForText("KTM Archive Bike")
            check(activity.verifyReopened())
        } finally {
            runOnMainSync { activity.finish() }
        }
    }

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        backupOnly = arguments?.getString("onlyBackup") == "true"
        pickerOnly = arguments?.getString("onlyPicker") == "true"
        roundTripOnly = arguments?.getString("onlyRoundTrip") == "true"
        historyLocaleOnly = arguments?.getString("onlyHistoryLocale") == "true"
        start()
    }

    override fun onStart() {
        if (backupOnly || pickerOnly || roundTripOnly || historyLocaleOnly) {
            val result = Bundle()
            try {
                when {
                    historyLocaleOnly -> { checkHistoryLocale("en", "bn"); checkHistoryLocale("bn", "en") }
                    roundTripOnly -> checkBackupRoundTripUi()
                    else -> checkBackupCreateUi()
                }
                result.putString("stream", when {
                    historyLocaleOnly -> "PASS synthetic history language and digit combinations\n"
                    roundTripOnly -> "PASS synthetic backup UI restore and Room reopen\n"
                    else -> "PASS synthetic backup creation UI\n"
                })
                finish(Activity.RESULT_OK, result)
            } catch (failure: Throwable) {
                val testName = when { historyLocaleOnly -> "history language and digits"; roundTripOnly -> "backup UI restore"; else -> "backup creation UI" }
                result.putString("stream", "FAIL synthetic $testName: ${failure.stackTraceToString()}\n")
                finish(Activity.RESULT_CANCELED, result)
            }
            return
        }
        val results = Bundle()
        val id = UUID.randomUUID().toString()
        val root = File(targetContext.filesDir.canonicalFile, "synthetic-privacy-probe-$id")
        val alias = "com.motovault.synthetic.privacy-probe.$id"
        val backupAlias = "$alias.backup"
        val checks = mutableListOf<String>()
        var resultCode = Activity.RESULT_CANCELED
        var owned: DocumentAssetOperation? = null
        val extraAssets = mutableSetOf<String>()
        val exportCopies = mutableListOf<DocumentExportCopy>()
        val recoveryId = "probe-recovery-$id.pdf"
        try {
            check(targetContext.packageName.endsWith(".privacyprobe")) { "Probe package required" }
            vaultContext = targetContext.applicationContext
            check(root.mkdirs())
            checkFuelEditUi()
            checks += "synthetic-native-fuel-ui-room-reopen-and-undo"
            checkFuelCreateFailureUi()
            checks += "synthetic-fuel-create-failure-preserves-draft-and-room"
            checkMaintenanceRuntime()
            checks += "native-maintenance-profile-actions-provenance-and-unknown-baseline"
            checkMaintenanceUi()
            checks += "native-maintenance-onboarding-and-schedule-details-ui"
            checkRecoveryUi("en", conflict = false)
            checkRecoveryUi("bn", conflict = true)
            checks += "synthetic-recovery-ui-success-conflict-and-busy-dismissal"
            checkDurableRecoveryUi()
            checks += "synthetic-recovery-ui-encrypted-room-reopen"
            checkBackupLocalizationUi("en")
            checkBackupLocalizationUi("bn")
            checks += "synthetic-backup-create-restore-localization-ui"
            checkBackupCreateUi()
            checks += "synthetic-backup-ui-to-injected-create-adapter"
            val reminder = RenewalReminder("synthetic", "document", "2026-10-20", "2026-10-25", "expiry")
            val englishTarget = ReminderTarget("document", "document", "Owner title", Digits.BN, AppLanguage.EN)
            val englishNotification = renewalNotification(targetContext, reminder, englishTarget)
            check(englishNotification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString() == "Document reminder")
            check(englishNotification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("২০ October ২০২৬"))
            val notificationManager = targetContext.getSystemService(android.app.NotificationManager::class.java)
            check(notificationManager.getNotificationChannel(englishNotification.channelId).name.toString() == "Document and service reminders")
            val banglaTarget = englishTarget.copy(language = AppLanguage.BN, digits = Digits.EN)
            val banglaNotification = renewalNotification(targetContext, reminder, banglaTarget)
            check(banglaNotification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString() == "কাগজের রিমাইন্ডার")
            check(banglaNotification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("20 অক্টোবর 2026"))
            check(notificationManager.getNotificationChannel(banglaNotification.channelId).name.toString() == "কাগজ ও সার্ভিসের রিমাইন্ডার")
            checks += "native-reminder-language-and-digit-preferences"
            val previousLocale = Locale.getDefault()
            try {
                Locale.setDefault(Locale.ENGLISH)
                check(runBlocking { getPluralString(Res.plurals.fuel_entry_count, 1, "1") } == "1 fuel entry")
                check(runBlocking { getPluralString(Res.plurals.fuel_entry_count, 2, "2") } == "2 fuel entries")
                check(runBlocking { getPluralString(Res.plurals.history_record_count, 1, "১") } == "১ record")
                check(runBlocking { getPluralString(Res.plurals.history_record_count, 2, "২") } == "২ records")
                Locale.setDefault(Locale.forLanguageTag("bn"))
                check(runBlocking { getPluralString(Res.plurals.fuel_entry_count, 2, "২") } == "২টি জ্বালানির রেকর্ড")
                Locale.setDefault(Locale.FRENCH)
                check(runBlocking { getString(Res.string.fuel_title) } == "Fuel")
            } finally {
                Locale.setDefault(previousLocale)
            }
            checks += "native-compose-plural-and-fallback"
            checkDurableFuelEdit(root)
            checkRestoreRecoveryTransaction(root)
            checks += "native-restore-recovery-transaction-and-reopen"
            checks += "native-fuel-edit-reopen-stale-conflict-and-undo"
            owned = DocumentAssetOperation()
            val originals = File(root, "documents").apply { check(mkdirs()) }
            val encrypted = File(root, "documents-protected")
            val source = "SYNTHETIC original, no owner data".encodeToByteArray()
            val original = File(originals, "synthetic.pdf").apply { writeBytes(source) }
            val files = AndroidAssetFiles(originals, encrypted)
            val cipher = AndroidAssetCipher { AndroidAssetKey.load(false, alias) }
            val envelope = AssetEnvelope(cipher)
            val store = SecureAssetStore(files, envelope)
            expectFailure { AndroidAssetKey.load(false, alias) }
            AndroidAssetKey.load(true, alias)
            checks += "native-key-provision-and-missing-key-refusal"

            // Uses native file staging/fsync, then aborts verification before publication.
            expectFailure { files.publishEncrypted("synthetic.pdf", envelope.seal("synthetic.pdf", source)) { error("Synthetic verification interruption") } }
            check(original.readBytes().contentEquals(source))
            check(encrypted.listFiles().orEmpty().isEmpty())
            checks += "native-interrupted-stage-retains-original-and-cleans-stage"
            val noSpace = SecureAssetStore(AndroidAssetFiles(originals, encrypted, availableBytes = { 0L }), envelope)
            expectFailure { noSpace.migrate("synthetic.pdf") }
            check(original.readBytes().contentEquals(source))
            check(encrypted.listFiles().orEmpty().isEmpty())
            checks += "native-zero-space-preflight-retains-original"

            store.migrate("synthetic.pdf")
            check(!original.exists())
            check(store.read("synthetic.pdf").contentEquals(source))
            check(File(encrypted, "synthetic.pdf.committed").isFile)
            val committed = File(encrypted, "synthetic.pdf.mva").readBytes()
            store.migrate("synthetic.pdf")
            check(File(encrypted, "synthetic.pdf.mva").readBytes().contentEquals(committed))
            check(encrypted.listFiles().orEmpty().none { it.name.startsWith(".stage-") })
            checks += "native-durable-migration-original-bytes-and-idempotent-reopen"

            val receipt = store.receipt("synthetic.pdf")
            check(receipt.assetId == "synthetic.pdf" && receipt.plaintextBytes == source.size && receipt.sha256.length == 64)
            val inventoryFile = File(encrypted, "synthetic.pdf.committed")
            inventoryFile.writeBytes(inventoryFile.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
            expectFailure { store.receipt("synthetic.pdf") }
            store.migrate("synthetic.pdf")
            check(store.receipt("synthetic.pdf") == receipt)
            check(File(encrypted, "synthetic.pdf.mva").readBytes().contentEquals(committed))
            checks += "native-authenticated-sha256-inventory-and-repair"
            val redirected = File(root, "redirected")
            android.system.Os.symlink(encrypted.absolutePath, redirected.absolutePath)
            expectFailure { SecureAssetStore(AndroidAssetFiles(originals, redirected), envelope).read("synthetic.pdf") }
            checks += "native-redirected-storage-directory-rejected"

            original.writeBytes(source)
            File(encrypted, "synthetic.pdf.mva").writeBytes(committed.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
            expectFailure { store.read("synthetic.pdf") }
            check(original.readBytes().contentEquals(source))
            checks += "native-tamper-refuses-plaintext-fallback"
            File(encrypted, "synthetic.pdf.mva").writeBytes(committed)

            val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keys.deleteEntry(alias)
            expectFailure { store.read("synthetic.pdf") }
            expectFailure { AndroidAssetKey.load(false, alias) }
            check(File(encrypted, "synthetic.pdf.mva").readBytes().contentEquals(committed))
            checks += "native-lost-key-retains-files-without-replacement"

            val fixtureOperation = requireNotNull(owned)
            val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
            val imageId = try { saveBitmap(bitmap, fixtureOperation) } finally { bitmap.recycle() }
            check(!assetFile(imageId).exists())
            val imageBytes = runBlocking { readDocumentAsset(imageId) }
            check(imageBytes.isNotEmpty())
            val decoded = previewAssetBitmap(imageId)
            try { check(decoded.width == 80 && decoded.height == 40) } finally { decoded.recycle() }
            val adjusted = runBlocking { adjustDocumentPage(DocumentPage(imageId, imageId, "image/jpeg", 80, 40, "Front"), 1, DocumentCrop(.1f, .1f, .9f, .9f)) }
            extraAssets += listOfNotNull(adjusted.acceptedId, adjusted.previewId)
            check(adjusted.width in 1..39 && adjusted.height in 1..79)
            val acceptedImage = previewAssetBitmap(requireNotNull(adjusted.acceptedId))
            try { check(acceptedImage.width == adjusted.width && acceptedImage.height == adjusted.height) } finally { acceptedImage.recycle() }
            checks += "native-encrypted-image-display-and-crop-readers"
            val exported = createDocumentExportCopy(imageId).also { exportCopies += it }
            check(exported.file.readBytes().contentEquals(imageBytes))
            check(exported.uri.authority == targetContext.packageName + ".documents")
            exported.close()
            check(!exported.file.exists())
            exported.close()
            checks += "native-export-copy-close-is-idempotent-and-removes-plaintext"
            val interruptedExport = createDocumentExportCopy(imageId).also { exportCopies += it }
            cleanDocumentScratchOnProcessStart()
            check(!interruptedExport.file.exists())
            checks += "native-cold-start-removes-interrupted-export-copy"

            val legacyId = "probe-legacy-$id.jpg"
            extraAssets += legacyId
            assetFile(legacyId).writeBytes(imageBytes)
            val recoveryDirectory = File(targetContext.filesDir.canonicalFile, "documents-recovery").apply { check(isDirectory || mkdirs()) }
            File(recoveryDirectory, recoveryId).writeBytes(source)
            runBlocking { protectDocumentAssets(setOf(legacyId, imageId)) }
            check(!assetFile(legacyId).exists())
            check(runBlocking { readDocumentAsset(legacyId) }.contentEquals(imageBytes))
            check(!File(recoveryDirectory, recoveryId).exists())
            check(runBlocking { restoreRecoveredDocumentAsset(recoveryId) } == recoveryId)
            extraAssets += recoveryId
            check(runBlocking { readDocumentAsset(recoveryId) }.contentEquals(source))
            expectFailure { runBlocking { restoreRecoveredDocumentAsset(recoveryId) } }
            checks += "native-legacy-and-recovery-migration-without-overwrite"
            val protectedRecovery = File(targetContext.filesDir.canonicalFile, "documents-recovery-protected/$recoveryId.mva")
            val recoveryCiphertext = protectedRecovery.readBytes()
            protectedRecovery.writeBytes(recoveryCiphertext.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
            expectFailure { runBlocking { protectDocumentAssets(setOf(legacyId, imageId)) } }
            protectedRecovery.writeBytes(recoveryCiphertext)
            runBlocking { protectDocumentAssets(setOf(legacyId, imageId)) }
            checks += "native-inventory-revalidates-protected-recovery"

            val pdfBytes = ByteArrayOutputStream().use { output ->
                val pdf = PdfDocument()
                try {
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(160, 120, 1).create())
                    page.canvas.drawColor(android.graphics.Color.WHITE)
                    pdf.finishPage(page)
                    pdf.writeTo(output)
                } finally { pdf.close() }
                output.toByteArray()
            }
            val pdfSource = File(root, "import-fixture.pdf").apply { writeBytes(pdfBytes) }
            val imported = runBlocking { processPdf(Uri.fromFile(pdfSource), "Tax token", fixtureOperation) }
            check(imported.pages.size == 1 && imported.pdfPageCount == 1)
            check(runBlocking { readDocumentAsset(requireNotNull(imported.originalPdf)) }.contentEquals(pdfBytes))
            imported.pages.forEach { check(previewAssetBitmap(it.previewId).let { image -> try { image.width > 0 } finally { image.recycle() } }) }
            check(File(targetContext.cacheDir.canonicalFile, "document-processing").listFiles().orEmpty().isEmpty())
            checks += "native-encrypted-pdf-import-and-no-processing-scratch-leak"
            val databasePath = File(root, "synthetic-format.db").absolutePath
            runBlocking { verifyVaultFormatMigration(databasePath) { buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(targetContext, databasePath)) } }
            checks += "native-room4-to5-owner-preservation-and-future-format-refusal"

            // --- P03 Encrypted Local Backup & Transactional Restore Suite ---
            val testRide = Ride(
                brandId = "yamaha",
                edition = "Standard",
                year = 2023,
                odometer = 12500.0,
                modelName = "MT-15"
            )
            val testRule = ComponentRule(
                id = "engine-oil",
                vehicleId = "primary",
                name = "Engine Oil",
                specification = "10W40 JASO MA2",
                intervalKm = 3000.0,
                intervalDays = 90,
                baselineCertainty = BaselineCertainty.CONFIRMED
            )
            val testLog = RideLog(
                title = "Engine Oil Change",
                detail = "Motul 10W40",
                date = "2026-10-01",
                odometer = 12000.0,
                cost = 850.0
            )
            val syntheticAssetId = "asset-reg-card.jpg"
            val syntheticAssetBytes = "SYNTHETIC-RAW-IMAGE-BYTES-12345".encodeToByteArray()
            val testDoc = VaultDocument(
                id = "doc-reg",
                title = "রেজিস্ট্রেশন স্মার্ট কার্ড",
                number = "DHAKA-METRO-LA-12-3456",
                holder = "Arif",
                expiry = "2027-01-01",
                issued = "2025-01-01",
                pages = listOf(
                    DocumentPage(
                        originalId = syntheticAssetId,
                        previewId = syntheticAssetId,
                        mimeType = "image/jpeg",
                        width = 800,
                        height = 600,
                        side = "Front"
                    )
                )
            )

            val vaultState = VaultState(
                ride = testRide,
                rules = listOf(testRule),
                logs = listOf(testLog),
                documents = listOf(testDoc),
                language = AppLanguage.EN, digits = Digits.EN, haptics = false, reduceMotion = true,
                reminderPolicy = ReminderPolicy(warningDays = 14, minuteOfDay = 600)
            )

            val assetStorage = mutableMapOf(syntheticAssetId to syntheticAssetBytes)
            checks += "native-backup-synthetic-vault-provision"

            val backupPassphrase = "StrongSecretPassword#2026!"
            val archiveBytes = runBlocking {
                BackupUseCase.createBackup(::documentSha256, 
                    snapshot = vaultState.backupSnapshot(),
                    vaultRevision = 42,
                    passphrase = backupPassphrase,
                    installationId = "android-probe-id",
                    assetReader = { id -> assetStorage[id] ?: error("Asset not found: $id") }
                )
            }
            check(archiveBytes.isNotEmpty())
            checks += "native-backup-export-success"

            // Zero plaintext leakage check
            val archiveStr = archiveBytes.decodeToString()
            check(!archiveStr.contains("DHAKA-METRO-LA-12-3456"))
            check(!archiveStr.contains("Engine Oil Change"))
            check(!archiveStr.contains("Motul 10W40"))
            check(!archiveStr.contains("SYNTHETIC-RAW-IMAGE-BYTES-12345"))
            check(!archiveStr.contains("Arif"))
            checks += "native-backup-zero-plaintext-leakage"

            // Staged validation & preview
            val preview = runBlocking {
                RestoreUseCase.validateAndPreview(::documentSha256, ::backupRideDisplayName, archiveBytes, backupPassphrase)
            }
            check(preview.manifest.formatVersion == 1)
            check(preview.manifest.summary.bikeDisplayName == "MT-15")
            check(preview.manifest.summary.odometer == 12500.0)
            check(preview.manifest.summary.logCount == 1)
            check(preview.manifest.summary.documentCount == 1)
            check(preview.validatedAssetCount == 1)
            checks += "native-backup-staged-validation-preview"

            // Wrong password refusal
            expectFailure {
                runBlocking {
                    RestoreUseCase.validateAndPreview(::documentSha256, ::backupRideDisplayName, archiveBytes, "WrongPassword#999")
                }
            }
            checks += "native-backup-wrong-password-refusal"

            // Tampered payload refusal
            val tamperedPayloadArchive = runBlocking {
                val entries = MvaZipArchive.read(archiveBytes).toMutableMap()
                val enc = entries["vault.enc"]!!.copyOf()
                enc[enc.size - 1] = (enc[enc.size - 1] xor 0xFF.toByte())
                entries["vault.enc"] = enc
                MvaZipArchive.write(entries)
            }
            expectFailure {
                runBlocking {
                    RestoreUseCase.validateAndPreview(::documentSha256, ::backupRideDisplayName, tamperedPayloadArchive, backupPassphrase)
                }
            }
            checks += "native-backup-tampered-payload-refusal"

            // Tampered asset refusal
            val tamperedAssetArchive = runBlocking {
                val entries = MvaZipArchive.read(archiveBytes).toMutableMap()
                val assetKey = entries.keys.first { it.startsWith("assets/") }
                val assetEnc = entries[assetKey]!!.copyOf()
                assetEnc[assetEnc.size - 1] = (assetEnc[assetEnc.size - 1] xor 0xFF.toByte())
                entries[assetKey] = assetEnc
                MvaZipArchive.write(entries)
            }
            expectFailure {
                runBlocking {
                    RestoreUseCase.validateAndPreview(::documentSha256, ::backupRideDisplayName, tamperedAssetArchive, backupPassphrase)
                }
            }
            checks += "native-backup-tampered-asset-refusal"

            // Transactional restore into fresh database
            val restoredDbPath = File(root, "synthetic-restored.db").absolutePath
            val restoredDb = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(targetContext, restoredDbPath))
            val restoredAssetDirectory = File(root, "restored-protected")
            AndroidAssetKey.load(true, backupAlias)
            val restoredStore = SecureAssetStore(
                AndroidAssetFiles(File(root, "restored-originals"), restoredAssetDirectory),
                AssetEnvelope(AndroidAssetCipher { AndroidAssetKey.load(false, backupAlias) })
            )
            var restoredStateCaptured: VaultState? = null

            runBlocking {
                val coordinator = VaultCoordinator(
                    scope = this,
                    initialState = VaultState(),
                    restore = { restoredDb.vault().read() ?: VaultState() },
                    write = { restoredDb.vault().replace(it) },
                    assetIds = { state -> state.documents.flatMap { it.assetIds() }.toSet() },
                    cleanup = {}, recover = {}, reconcile = {}
                )
                coordinator.snapshots.first { !it.loading }
                val completed = CompletableDeferred<Result<Unit>>()
                coordinator.commitPrepared(0, prepare = {
                    val staged = BackupRestoreStaging.prepare(
                        archiveBytes = archiveBytes,
                        passphrase = backupPassphrase,
                        assetWriter = { id, bytes -> restoredStore.stageRestore(id, bytes) },
                        discardAssets = { ids -> ids.forEach { id -> File(restoredAssetDirectory, "$id.mva").delete() } }
                    )
                    restoredStateCaptured = staged.state
                    PreparedVaultChange(staged.state) { staged.createdAssets.forEach { id -> File(restoredAssetDirectory, "$id.mva").delete() } }
                }) { completed.complete(it) }
                completed.await().getOrThrow()
            }

            check(restoredStateCaptured != null)
            check(restoredStateCaptured?.ride?.modelName == "MT-15")
            check(restoredStateCaptured?.logs?.size == 1)
            check(restoredStateCaptured?.documents?.first()?.number == "DHAKA-METRO-LA-12-3456")
            check(restoredStore.read(syntheticAssetId).contentEquals(syntheticAssetBytes))
            check(!restoredStore.stageRestore(syntheticAssetId, syntheticAssetBytes))
            expectFailure { restoredStore.stageRestore(syntheticAssetId, "different synthetic bytes".encodeToByteArray()) }
            check(restoredStore.read(syntheticAssetId).contentEquals(syntheticAssetBytes))
            val dbState = runBlocking { restoredDb.vault().read() }
            check(dbState?.ride?.modelName == "MT-15")
            check(dbState?.documents?.first()?.id == "doc-reg")
            check(dbState?.language == vaultState.language && dbState.digits == vaultState.digits)
            check(dbState?.haptics == false && dbState.reduceMotion && dbState.reminderPolicy == vaultState.reminderPolicy)
            checks += "native-backup-transactional-restore-success"

            val incomingBytes = "different synthetic incoming original".encodeToByteArray()
            val collidingArchive = runBlocking {
                BackupUseCase.createBackup(::documentSha256, vaultState.backupSnapshot(), 43, backupPassphrase, assetReader = { incomingBytes })
            }
            val remappedId = "synthetic-restore-remapped.jpg"
            val collisionRecoveryId = "collision-recovery.pdf"
            val recoveryCipher = RecoverySnapshotCipher(AndroidAssetCipher { AndroidAssetKey.load(false, backupAlias) })
            val recoveryStore = RecoverySnapshotStore(restoredDb.vault(), { collisionRecoveryId }, recoveryCipher::seal, recoveryCipher::open)
            val collisionStage = runBlocking {
                val coordinator = VaultCoordinator(
                    scope = this, initialState = VaultState(),
                    restore = { restoredDb.vault().read() ?: VaultState() },
                    write = { restoredDb.vault().replace(it) },
                    assetIds = { state -> state.documents.flatMap { it.assetIds() }.toSet() },
                    cleanup = { ids -> ids.forEach { id -> File(restoredAssetDirectory, "$id.mva").delete() } },
                    recover = {}, reconcile = {},
                    writePrepared = recoveryStore::replace,
                    recoveryAssetIds = recoveryStore::referencedAssets
                )
                coordinator.snapshots.first { !it.loading }
                var captured: StagedRestore? = null
                val completed = CompletableDeferred<Result<Unit>>()
                coordinator.commitPrepared(0, prepare = {
                    val staged = BackupRestoreStaging.prepare(
                        collidingArchive, backupPassphrase,
                        assetWriter = { id, bytes -> restoredStore.stageRestore(id, bytes) },
                        discardAssets = { ids -> ids.forEach { id -> File(restoredAssetDirectory, "$id.mva").delete() } },
                        newAssetId = { remappedId }
                    )
                    captured = staged
                    PreparedVaultChange(staged.state, preservePreviousAssets = true) {
                        staged.createdAssets.forEach { id -> File(restoredAssetDirectory, "$id.mva").delete() }
                    }
                }) { completed.complete(it) }
                completed.await().getOrThrow()
                val nextCommit = CompletableDeferred<Boolean>()
                coordinator.commit(coordinator.snapshots.value.state, 1) { nextCommit.complete(it) }
                check(nextCommit.await())
                requireNotNull(captured)
            }
            check(collisionStage.createdAssets == setOf(remappedId))
            val remappedPage = collisionStage.state.documents.single().pages.single()
            check(remappedPage.originalId == remappedId && remappedPage.previewId == remappedId)
            check(restoredStore.read(syntheticAssetId).contentEquals(syntheticAssetBytes))
            check(restoredStore.read(remappedId).contentEquals(incomingBytes))
            restoredDb.close()
            val reopened = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(targetContext, restoredDbPath))
            val persistedPage = runBlocking { reopened.vault().read()!!.documents.single().pages.single() }
            check(persistedPage.originalId == remappedId && persistedPage.previewId == remappedId)
            val recoveredSnapshot = runBlocking { reopened.vault().restoreRecoveryRows().single() }
            check(recoveredSnapshot.id == collisionRecoveryId)
            val priorState = requireNotNull(VaultState.decode(recoveryCipher.open(recoveredSnapshot.id, recoveredSnapshot.payload).decodeToString()))
            check(priorState.documents.single().pages.single().originalId == syntheticAssetId)
            check(runBlocking { RecoverySnapshotStore(reopened.vault(), { collisionRecoveryId }, recoveryCipher::seal, recoveryCipher::open).referencedAssets() } == setOf(syntheticAssetId))
            check(restoredStore.read(syntheticAssetId).contentEquals(syntheticAssetBytes))
            check(restoredStore.read(remappedId).contentEquals(incomingBytes))
            runBlocking {
                val snapshots = RecoverySnapshotStore(reopened.vault(), { "recovered-current.pdf" }, recoveryCipher::seal, recoveryCipher::open)
                val writer = VaultCoordinator(
                    scope = this, initialState = VaultState(),
                    restore = { reopened.vault().read() ?: VaultState() },
                    write = { reopened.vault().replace(it) },
                    assetIds = { state -> state.documents.flatMap { it.assetIds() }.toSet() },
                    cleanup = { ids -> ids.forEach { id -> File(restoredAssetDirectory, "$id.mva").delete() } },
                    recover = {}, reconcile = {}, writePrepared = snapshots::replace,
                    recoveryAssetIds = snapshots::referencedAssets
                )
                writer.snapshots.first { !it.loading }
                val missing = CompletableDeferred<Result<Unit>>()
                CoordinatedRecovery(writer, snapshots) { error("synthetic missing original") }
                    .restore(collisionRecoveryId, 0) { missing.complete(it) }
                check(missing.await().isFailure)
                check(reopened.vault().read()!!.documents.single().pages.single().originalId == remappedId)
                check(reopened.vault().restoreRecoveryRows().size == 1)
                val result = CompletableDeferred<Result<Unit>>()
                CoordinatedRecovery(writer, snapshots) { id -> restoredStore.read(id) }
                    .restore(collisionRecoveryId, 0) { result.complete(it) }
                result.await().getOrThrow()
                check(reopened.vault().read()!!.documents.single().pages.single().originalId == syntheticAssetId)
                check(reopened.vault().restoreRecoveryRows().size == 2)
                check(snapshots.referencedAssets() == setOf(syntheticAssetId, remappedId))
                check(restoredStore.read(remappedId).contentEquals(incomingBytes))
            }
            reopened.close()
            checks += "native-backup-collision-remap-reopen"

            results.putString("stream", "PASS ${checks.size} native storage checks: ${checks.joinToString(", ")}\n")
            results.putInt("checks", checks.size)
            resultCode = Activity.RESULT_OK
        } catch (failure: Throwable) {
            results.putString("stream", "FAIL native storage probe after ${checks.size} checks: ${failure.stackTraceToString()}\n")
        } finally {
            // Only the UUID directory and alias allocated by this synthetic test are removed.
            root.deleteRecursively()
            runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) } }
            runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(backupAlias) } }
            runCatching { owned?.rollback(); removeDocumentAssets(extraAssets) }
            exportCopies.forEach { runCatching { it.close() } }
            for (name in listOf("documents-recovery", "documents-recovery-protected")) {
                for (suffix in listOf("", ".mva", ".committed")) File(targetContext.filesDir.canonicalFile, "$name/$recoveryId$suffix").delete()
            }
        }
        finish(resultCode, results)
    }

    private fun expectFailure(block: () -> Unit) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        check(failed) { "Expected failure was accepted" }
    }
}
