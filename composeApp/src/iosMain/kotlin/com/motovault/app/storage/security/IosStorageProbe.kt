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
