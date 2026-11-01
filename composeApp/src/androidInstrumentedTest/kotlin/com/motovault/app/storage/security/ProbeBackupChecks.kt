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

internal fun NativeAssetInstrumentation.checkBackupLocalizationUi(language: String) {
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

internal fun NativeAssetInstrumentation.checkBackupCreateUi() {
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

internal fun NativeAssetInstrumentation.setPickerFilename(name: String) {
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

internal fun NativeAssetInstrumentation.checkBackupRoundTripUi() {
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


