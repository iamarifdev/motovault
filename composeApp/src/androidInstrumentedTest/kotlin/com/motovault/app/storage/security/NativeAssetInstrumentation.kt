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
    internal var pickerOnly = false
    private var roundTripOnly = false
    private var historyLocaleOnly = false
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

}
