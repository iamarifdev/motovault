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

internal fun NativeAssetInstrumentation.checkRestoreRecoveryTransaction(root: File) = runBlocking {
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

internal fun NativeAssetInstrumentation.checkRecoveryUi(language: String, conflict: Boolean) {
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

internal fun NativeAssetInstrumentation.checkDurableRecoveryUi() {
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


