@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
package com.motovault.app.storage.security

import com.motovault.app.ui.*
import kotlinx.cinterop.useContents
import kotlinx.coroutines.runBlocking
import kotlin.native.Platform
import platform.Foundation.*
import platform.CoreGraphics.*
import platform.UIKit.*
import androidx.room.Room
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.storage.buildVaultDatabase
import com.motovault.app.storage.RecoverySnapshotStore
import com.motovault.app.storage.BackupOperationsAdapter
import com.motovault.app.storage.BackupRestoreStaging
import com.motovault.app.application.PreparedVaultChange
import com.motovault.app.application.VaultCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.fuel_entry_count
import motovault.composeapp.generated.resources.fuel_title
import motovault.composeapp.generated.resources.history_record_count

/** Native storage verification can run only in a debug build with the isolated probe bundle ID. */
fun runIosStorageProbe(provider: IosAssetCryptography, invalidateSyntheticKey: () -> Unit, cleanupTemporaryCopies: () -> Unit): String {
    if (!Platform.isDebugBinary || NSBundle.mainBundle.bundleIdentifier?.endsWith(".privacyprobe") != true) return "REFUSED: isolated debug probe required"
    val manager = NSFileManager.defaultManager
    val applicationRoot = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    val root = "$applicationRoot/synthetic-privacy-probe-${NSUUID().UUIDString}"
    val checks = mutableListOf<String>()
    val fixtureOperation = DocumentAssetOperation()
    val extraAssets = mutableSetOf<String>()
    val exportCopies = mutableListOf<DocumentExportCopy>()
    val recoveryId = "probe-recovery-${NSUUID().UUIDString}.pdf"
    var stage = "setup"
    try {
        stage = "reminder-localization"
        val reminder = RenewalReminder("synthetic", "document", "2026-10-20", "2026-10-25", "expiry")
        val english = ReminderTarget("document", "document", "Owner title", Digits.BN, AppLanguage.EN)
        check(reminderHeading(reminder, english) == "Document reminder")
        check(reminderMessage(reminder, english).contains("২০ October ২০২৬"))
        val bangla = english.copy(language = AppLanguage.BN, digits = Digits.EN)
        check(reminderHeading(reminder, bangla) == "কাগজের রিমাইন্ডার")
        check(reminderMessage(reminder, bangla).contains("20 অক্টোবর 2026"))
        checks += "native-reminder-language-and-digit-preferences"
        stage = "compose-localization"
        val defaults = NSUserDefaults.standardUserDefaults
        val priorLanguages = defaults.objectForKey("AppleLanguages")
        try {
            defaults.setObject(listOf("en"), forKey = "AppleLanguages")
            check(runBlocking { getPluralString(Res.plurals.fuel_entry_count, 1, "1") } == "1 fuel entry")
            check(runBlocking { getPluralString(Res.plurals.fuel_entry_count, 2, "2") } == "2 fuel entries")
            check(runBlocking { getPluralString(Res.plurals.history_record_count, 1, "১") } == "১ record")
            check(runBlocking { getPluralString(Res.plurals.history_record_count, 2, "২") } == "২ records")
            defaults.setObject(listOf("bn"), forKey = "AppleLanguages")
            check(runBlocking { getPluralString(Res.plurals.fuel_entry_count, 2, "২") } == "২টি জ্বালানির রেকর্ড")
            defaults.setObject(listOf("fr"), forKey = "AppleLanguages")
            check(runBlocking { getString(Res.string.fuel_title) } == "Fuel")
        } finally {
            if (priorLanguages == null) defaults.removeObjectForKey("AppleLanguages")
            else defaults.setObject(priorLanguages, forKey = "AppleLanguages")
        }
        checks += "native-compose-plural-and-fallback"
        val originals = "$root/documents"
        val encrypted = "$root/documents-protected"
        check(manager.createDirectoryAtPath(originals, true, null, null))
        val originalPath = "$originals/synthetic.pdf"
        val source = "SYNTHETIC original, no owner data".encodeToByteArray()
        check(source.assetData().writeToFile(originalPath, true))
        val files = IosAssetFiles(originals, encrypted)
        val envelope = AssetEnvelope(IosAssetCipher { provider })
        val store = SecureAssetStore(files, envelope)
        expectProbeFailure { envelope.seal("synthetic.pdf", source) }
        stage = "key-provision"
        check(provider.prepareFreshStore())
        checks += "native-key-provision-and-missing-key-refusal"
        stage = "backup-create-preview"
        runBlocking {
            val assetId = "synthetic-backup.pdf"
            val synthetic = VaultState(
                ride = Ride("test", "Synthetic", 2020, 1000.0, modelName = "Backup Bike"),
                logs = listOf(RideLog("Synthetic fuel", "4 L", "2026-01-01", 1000.0, id = "backup-log", kind = "fuel", liters = 4.0)),
                documents = listOf(VaultDocument("Synthetic", "1", "Synthetic", "2030-01-01", id = "backup-doc", originalPdf = assetId))
            )
            val adapter = BackupOperationsAdapter(
                digest = { bytes -> provider.sha256(bytes.assetData()).assetBytes() },
                readAsset = { id -> check(id == assetId); source }
            )
            val archive = adapter.create(synthetic, 3L, "synthetic-passphrase") { _, _ -> }
            val preview = adapter.preview(synthetic, archive, "synthetic-passphrase")
            check(preview.validatedAssetCount == 1 && preview.payload.documents.single().originalPdf == assetId)
            check(preview.verifiedSummary.bikeDisplayName == "Backup Bike")
            var rejectedWrongPassword = false
            try { adapter.preview(synthetic, archive, "incorrect-passphrase") } catch (_: Exception) { rejectedWrongPassword = true }
            check(rejectedWrongPassword)

            stage = "backup-staged-restore-collision"
            val restoreEncrypted = "$root/backup-restored-protected"
            val restoreStore = SecureAssetStore(IosAssetFiles("$root/backup-restored-originals", restoreEncrypted), envelope)
            val existingBytes = "SYNTHETIC existing original".encodeToByteArray()
            check(restoreStore.stageRestore(assetId, existingBytes))
            val remappedId = "synthetic-remapped.pdf"
            suspend fun discardSyntheticAssets(ids: Set<String>) {
                ids.forEach { id ->
                    listOf("$restoreEncrypted/$id.mva", "$restoreEncrypted/$id.committed").forEach { path ->
                        if (manager.fileExistsAtPath(path)) check(manager.removeItemAtPath(path, null))
                    }
                }
            }
            val restorePath = "$root/synthetic-backup-restore.db"
            fun openRestoreDatabase() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(name = restorePath))
            var restoredDatabase = openRestoreDatabase()
            val previous = VaultState(ride = Ride("test", "Synthetic", 2019, 500.0, modelName = "Previous Bike"),
                documents = listOf(VaultDocument("Previous", "1", "Synthetic", "2030-01-01", id = "previous-doc", originalPdf = assetId)))
            restoredDatabase.vault().replace(previous)
            val recoveryCipher = RecoverySnapshotCipher(IosAssetCipher { provider })
            val recoveryStore = RecoverySnapshotStore(restoredDatabase.vault(), { "synthetic-previous.pdf" }, recoveryCipher::seal, recoveryCipher::open)
            val coordinator = VaultCoordinator(
                scope = this, initialState = VaultState(), restore = { requireNotNull(restoredDatabase.vault().read()) },
                write = { restoredDatabase.vault().replace(it) }, assetIds = { state -> state.documents.flatMap { it.assetIds() }.toSet() },
                cleanup = {}, recover = {}, reconcile = {}, writePrepared = { before, after -> recoveryStore.replace(before, after) },
                recoveryAssetIds = { recoveryStore.referencedAssets() }
            )
            coordinator.snapshots.first { !it.loading }
            val rejected = CompletableDeferred<Result<Unit>>()
            coordinator.commitPrepared(coordinator.snapshots.value.revision, prepare = {
                val invalid = BackupRestoreStaging.prepare(
                    archive, "incorrect-passphrase", assetWriter = { id, bytes -> restoreStore.stageRestore(id, bytes) },
                    discardAssets = ::discardSyntheticAssets, newAssetId = { remappedId }
                )
                PreparedVaultChange(invalid.state, preservePreviousAssets = true) { discardSyntheticAssets(invalid.createdAssets) }
            }) { rejected.complete(it) }
            check(rejected.await().isFailure)
            check(restoredDatabase.vault().read()?.ride?.modelName == "Previous Bike")
            check(restoredDatabase.vault().restoreRecoveryRows().isEmpty())
            check(!restoreStore.contains(remappedId) && restoreStore.read(assetId).contentEquals(existingBytes))
            val completion = CompletableDeferred<Result<Unit>>()
            coordinator.commitPrepared(coordinator.snapshots.value.revision, prepare = {
                val staged = BackupRestoreStaging.prepare(
                    archive, "synthetic-passphrase", assetWriter = { id, bytes -> restoreStore.stageRestore(id, bytes) },
                    discardAssets = ::discardSyntheticAssets, newAssetId = { remappedId }
                )
                check(staged.createdAssets == setOf(remappedId))
                check(staged.state.documents.single().originalPdf == remappedId)
                PreparedVaultChange(staged.state, preservePreviousAssets = true) { discardSyntheticAssets(staged.createdAssets) }
            }) { completion.complete(it) }
            completion.await().getOrThrow()
            check(recoveryStore.read("synthetic-previous.pdf").ride?.modelName == "Previous Bike")
            check(recoveryStore.referencedAssets() == setOf(assetId))
            check(restoreStore.read(assetId).contentEquals(existingBytes))
            check(restoreStore.read(remappedId).contentEquals(source))
            restoredDatabase.close()
            restoredDatabase = openRestoreDatabase()
            val reopened = requireNotNull(restoredDatabase.vault().read())
            check(reopened.ride?.modelName == "Backup Bike")
            check(reopened.logs.single().id == "backup-log" && reopened.logs.single().liters == 4.0)
            check(reopened.documents.single().originalPdf == remappedId)
            check(restoreStore.read(remappedId).contentEquals(source))
            restoredDatabase.close()
            checks += "native-backup-coordinated-recovery-collision-room-reopen"
        }
        checks += "native-backup-adapter-create-preview-wrong-password"
        stage = "recovery-snapshot"
        runBlocking {
            val path = "$root/synthetic-recovery.db"
            fun openDatabase() = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(name = path))
            var database = openDatabase()
            val prior = VaultState(ride = Ride("test", "Synthetic", 2020, 1000.0, modelName = "Original"),
                documents = listOf(VaultDocument("Synthetic", "1", "Synthetic", "2030-01-01", id = "recovery-doc",
                    pages = listOf(DocumentPage("recovery-original.jpg", "recovery-preview.jpg", "image/jpeg", 10, 10, "Front")))))
            val replacement = VaultState(ride = Ride("test", "Synthetic", 2021, 2000.0, modelName = "Restored"))
            database.vault().replace(prior)
            val recoveryId = "synthetic-recovery.pdf"
            val cipher = RecoverySnapshotCipher(IosAssetCipher { provider })
            val snapshots = RecoverySnapshotStore(database.vault(), { recoveryId }, cipher::seal, cipher::open)
            snapshots.replace(requireNotNull(database.vault().read()), replacement)
            check(snapshots.referencedAssets() == setOf("recovery-original.jpg", "recovery-preview.jpg"))
            val summary = snapshots.summaries().single()
            check(summary.id == recoveryId && summary.createdAtEpochMillis == null)
            check(summary.bikeName == "Original" && summary.documentCount == 1 && summary.recordCount == 0)
            val encryptedPayload = database.vault().restoreRecoveryRows().single().payload
            check(!encryptedPayload.decodeToString().contains("Original"))
            database.close()
            database = openDatabase()
            check(database.vault().read()?.ride?.modelName == "Restored")
            check(VaultState.decode(cipher.open(recoveryId, database.vault().restoreRecoveryRows().single().payload).decodeToString())?.ride?.modelName == "Original")
            val tampered = encryptedPayload.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            expectProbeFailure { cipher.open(recoveryId, tampered) }
            database.close()
        }
        checks += "native-encrypted-recovery-snapshot-and-reopen"
        stage = "interrupted-stage"
        expectProbeFailure { files.publishEncrypted("synthetic.pdf", envelope.seal("synthetic.pdf", source)) { error("Synthetic interruption") } }
        check(requireNotNull(NSData.dataWithContentsOfFile(originalPath)).assetBytes().contentEquals(source))
        check(manager.contentsOfDirectoryAtPath(encrypted, null).orEmpty().isEmpty())
        checks += "native-interrupted-stage-retains-original-and-cleans-stage"
        stage = "space-preflight"
        val noSpace = SecureAssetStore(IosAssetFiles(originals, encrypted, availableBytes = { 0L }), envelope)
        expectProbeFailure { noSpace.migrate("synthetic.pdf") }
        check(requireNotNull(NSData.dataWithContentsOfFile(originalPath)).assetBytes().contentEquals(source))
        check(manager.contentsOfDirectoryAtPath(encrypted, null).orEmpty().isEmpty())
        checks += "native-zero-space-preflight-retains-original"
        stage = "migration"
        store.migrate("synthetic.pdf")
        check(!manager.fileExistsAtPath(originalPath))
        check(store.read("synthetic.pdf").contentEquals(source))
        val committed = requireNotNull(NSData.dataWithContentsOfFile("$encrypted/synthetic.pdf.mva")).assetBytes()
        store.migrate("synthetic.pdf")
        check(requireNotNull(NSData.dataWithContentsOfFile("$encrypted/synthetic.pdf.mva")).assetBytes().contentEquals(committed))
        checks += "native-durable-migration-original-bytes-and-idempotent-reopen"
        stage = "hash-inventory"
        val receipt = store.receipt("synthetic.pdf")
        check(receipt.assetId == "synthetic.pdf" && receipt.plaintextBytes == source.size && receipt.sha256.length == 64)
        val inventoryPath = "$encrypted/synthetic.pdf.committed"
        val changedInventory = requireNotNull(NSData.dataWithContentsOfFile(inventoryPath)).assetBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        check(changedInventory.assetData().writeToFile(inventoryPath, true))
        expectProbeFailure { store.receipt("synthetic.pdf") }
        store.migrate("synthetic.pdf")
        check(store.receipt("synthetic.pdf") == receipt)
        check(requireNotNull(NSData.dataWithContentsOfFile("$encrypted/synthetic.pdf.mva")).assetBytes().contentEquals(committed))
        checks += "native-authenticated-sha256-inventory-and-repair"
        stage = "redirected-directory"
        val redirected = "$root/redirected"
        check(manager.createSymbolicLinkAtPath(redirected, encrypted, null))
        expectProbeFailure { SecureAssetStore(IosAssetFiles(originals, redirected), envelope).read("synthetic.pdf") }
        checks += "native-redirected-storage-directory-rejected"
        stage = "tamper"
        check(source.assetData().writeToFile(originalPath, true))
        val changed = committed.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        check(changed.assetData().writeToFile("$encrypted/synthetic.pdf.mva", true))
        expectProbeFailure { store.read("synthetic.pdf") }
        check(committed.assetData().writeToFile("$encrypted/synthetic.pdf.mva", true))
        checks += "native-tamper-refuses-plaintext-fallback"
        stage = "lost-key"
        invalidateSyntheticKey()
        expectProbeFailure { store.read("synthetic.pdf") }
        check(requireNotNull(NSData.dataWithContentsOfFile("$encrypted/synthetic.pdf.mva")).assetBytes().contentEquals(committed))
        checks += "native-lost-key-retains-files-without-replacement"
        stage = "image-create"

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(80.0, 40.0), true, 1.0)
        val jpeg = try {
            UIColor.whiteColor.setFill(); UIRectFill(CGRectMake(0.0, 0.0, 80.0, 40.0))
            requireNotNull(UIImageJPEGRepresentation(requireNotNull(UIGraphicsGetImageFromCurrentImageContext()), .96))
        } finally { UIGraphicsEndImageContext() }
        stage = "image-save"
        val imageId = saveData(jpeg, "jpg", fixtureOperation)
        check(!manager.fileExistsAtPath(assetPath(imageId)))
        stage = "image-read"
        val imageBytes = runBlocking { readDocumentAsset(imageId) }
        val decoded = requireNotNull(UIImage(data = imageBytes.assetData()))
        stage = "image-size"
        check(decoded.size.useContents { width == 80.0 && height == 40.0 })
        stage = "crop-read"
        val adjusted = runBlocking { adjustDocumentPage(DocumentPage(imageId, imageId, "image/jpeg", 80, 40, "Front"), 1, DocumentCrop(.1f, .1f, .9f, .9f)) }
        extraAssets += listOfNotNull(adjusted.acceptedId, adjusted.previewId)
        stage = "crop-size-${adjusted.width}x${adjusted.height}"
        check(adjusted.width in 1..39 && adjusted.height in 1..79)
        val acceptedImage = requireNotNull(UIImage(data = runBlocking { readDocumentAsset(requireNotNull(adjusted.acceptedId)) }.assetData()))
        check(acceptedImage.size.useContents { this.width.toInt() == adjusted.width && this.height.toInt() == adjusted.height })
        checks += "native-encrypted-image-display-and-crop-readers"
        stage = "export-copy-cleanup"
        val exported = createDocumentExportCopy(imageId).also { exportCopies += it }
        check(requireNotNull(NSData.dataWithContentsOfFile(exported.path)).assetBytes().contentEquals(imageBytes))
        check(exported.close() && !manager.fileExistsAtPath(exported.path))
        check(exported.close())
        checks += "native-export-copy-close-is-idempotent-and-removes-plaintext"
        val interruptedExport = createDocumentExportCopy(imageId).also { exportCopies += it }
        cleanupTemporaryCopies()
        check(!manager.fileExistsAtPath(interruptedExport.path))
        checks += "native-cold-start-removes-interrupted-export-copy"
        stage = "legacy-recovery"

        val legacyId = "probe-legacy-${NSUUID().UUIDString}.jpg"
        extraAssets += legacyId
        check(jpeg.writeToFile(assetPath(legacyId), true))
        val recoveryDirectory = "$applicationRoot/documents-recovery"
        check(manager.createDirectoryAtPath(recoveryDirectory, true, null, null))
        check(source.assetData().writeToFile("$recoveryDirectory/$recoveryId", true))
        runBlocking { protectDocumentAssets(setOf(legacyId, imageId)) }
        check(!manager.fileExistsAtPath(assetPath(legacyId)))
        check(runBlocking { readDocumentAsset(legacyId) }.contentEquals(imageBytes))
        check(!manager.fileExistsAtPath("$recoveryDirectory/$recoveryId"))
        check(runBlocking { restoreRecoveredDocumentAsset(recoveryId) } == recoveryId)
        extraAssets += recoveryId
        check(runBlocking { readDocumentAsset(recoveryId) }.contentEquals(source))
        expectProbeFailure { runBlocking { restoreRecoveredDocumentAsset(recoveryId) } }
        checks += "native-legacy-and-recovery-migration-without-overwrite"
        stage = "protected-recovery-revalidation"
        val protectedRecovery = "$applicationRoot/documents-recovery-protected/$recoveryId.mva"
        val recoveryCiphertext = requireNotNull(NSData.dataWithContentsOfFile(protectedRecovery)).assetBytes()
        check(recoveryCiphertext.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }.assetData().writeToFile(protectedRecovery, true))
        expectProbeFailure { runBlocking { protectDocumentAssets(setOf(legacyId, imageId)) } }
        check(recoveryCiphertext.assetData().writeToFile(protectedRecovery, true))
        runBlocking { protectDocumentAssets(setOf(legacyId, imageId)) }
        checks += "native-inventory-revalidates-protected-recovery"
        stage = "pdf-import"

        val pdfData = NSMutableData()
        UIGraphicsBeginPDFContextToData(pdfData, CGRectMake(0.0, 0.0, 160.0, 120.0), null)
        try { UIGraphicsBeginPDFPage(); UIColor.whiteColor.setFill(); UIRectFill(CGRectMake(0.0, 0.0, 160.0, 120.0)) } finally { UIGraphicsEndPDFContext() }
        val imported = processPdf(pdfData, "Tax token", fixtureOperation)
        check(imported.pages.size == 1 && imported.pdfPageCount == 1)
        check(runBlocking { readDocumentAsset(requireNotNull(imported.originalPdf)) }.contentEquals(pdfData.assetBytes()))
        imported.pages.forEach { page -> check(UIImage(data = runBlocking { readDocumentAsset(page.previewId) }.assetData()) != null) }
        checks += "native-encrypted-pdf-import-without-plaintext-file"
        stage = "database-format-migration"
        val databasePath = "$root/synthetic-format.db"
        runBlocking { verifyVaultFormatMigration(databasePath) { buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(name = databasePath)) } }
        checks += "native-room4-to5-owner-preservation-and-future-format-refusal"
        return "PASS ${checks.size} native iOS storage checks: ${checks.joinToString(", ")}"
    } catch (failure: Throwable) {
        return "FAIL native iOS storage probe at $stage after ${checks.size} checks (${failure::class.simpleName})"
    } finally {
        manager.removeItemAtPath(root, null)
        runCatching { fixtureOperation.rollback(); removeDocumentAssets(extraAssets) }
        exportCopies.forEach { it.close() }
        for (name in listOf("documents-recovery", "documents-recovery-protected")) for (suffix in listOf("", ".mva", ".committed")) manager.removeItemAtPath("$applicationRoot/$name/$recoveryId$suffix", null)
    }
}

private fun expectProbeFailure(block: () -> Unit) {
    var failed = false
    try { block() } catch (_: Exception) { failed = true }
    check(failed) { "Expected failure was accepted" }
}
