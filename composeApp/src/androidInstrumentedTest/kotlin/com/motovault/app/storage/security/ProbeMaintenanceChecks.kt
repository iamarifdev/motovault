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

internal fun NativeAssetInstrumentation.checkMaintenanceRuntime() {
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


internal fun NativeAssetInstrumentation.checkMaintenanceUi() {
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


