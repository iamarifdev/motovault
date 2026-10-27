package com.motovault.app.storage.security

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import java.io.File
import java.security.KeyStore
import java.util.UUID
import com.motovault.app.ui.*
import android.graphics.Bitmap
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

/** No owner database, production key alias or primary app installation is used by this probe. */
class NativeAssetInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    override fun onStart() {
        val results = Bundle()
        val id = UUID.randomUUID().toString()
        val root = File(targetContext.filesDir.canonicalFile, "synthetic-privacy-probe-$id")
        val alias = "com.motovault.synthetic.privacy-probe.$id"
        val checks = mutableListOf<String>()
        var resultCode = Activity.RESULT_CANCELED
        var owned: DocumentAssetOperation? = null
        val extraAssets = mutableSetOf<String>()
        val exportCopies = mutableListOf<DocumentExportCopy>()
        val recoveryId = "probe-recovery-$id.pdf"
        try {
            check(targetContext.packageName.endsWith(".privacyprobe")) { "Probe package required" }
            vaultContext = targetContext.applicationContext
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
                documents = listOf(testDoc)
            )

            val assetStorage = mutableMapOf(syntheticAssetId to syntheticAssetBytes)
            checks += "native-backup-synthetic-vault-provision"

            val backupPassphrase = "StrongSecretPassword#2026!"
            val archiveBytes = runBlocking {
                BackupUseCase.createBackup(
                    state = vaultState,
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
                RestoreUseCase.validateAndPreview(archiveBytes, backupPassphrase)
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
                    RestoreUseCase.validateAndPreview(archiveBytes, "WrongPassword#999")
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
                    RestoreUseCase.validateAndPreview(tamperedPayloadArchive, backupPassphrase)
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
                    RestoreUseCase.validateAndPreview(tamperedAssetArchive, backupPassphrase)
                }
            }
            checks += "native-backup-tampered-asset-refusal"

            // Transactional restore into fresh database
            val restoredDbPath = File(root, "synthetic-restored.db").absolutePath
            val restoredDb = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(targetContext, restoredDbPath))
            val restoredAssets = mutableMapOf<String, ByteArray>()
            var restoredStateCaptured: VaultState? = null

            runBlocking {
                RestoreUseCase.executeRestore(
                    archiveBytes = archiveBytes,
                    passphrase = backupPassphrase,
                    database = restoredDb,
                    assetWriter = { id, bytes -> restoredAssets[id] = bytes },
                    onComplete = { nextState -> restoredStateCaptured = nextState }
                )
            }

            check(restoredStateCaptured != null)
            check(restoredStateCaptured?.ride?.modelName == "MT-15")
            check(restoredStateCaptured?.logs?.size == 1)
            check(restoredStateCaptured?.documents?.first()?.number == "DHAKA-METRO-LA-12-3456")
            check(restoredAssets.containsKey(syntheticAssetId))
            check(restoredAssets[syntheticAssetId]!!.contentEquals(syntheticAssetBytes))
            val dbState = runBlocking { restoredDb.vault().read() }
            check(dbState?.ride?.modelName == "MT-15")
            check(dbState?.documents?.first()?.id == "doc-reg")
            checks += "native-backup-transactional-restore-success"

            results.putString("stream", "PASS ${checks.size} native storage checks: ${checks.joinToString(", ")}\n")
            results.putInt("checks", checks.size)
            resultCode = Activity.RESULT_OK
        } catch (failure: Throwable) {
            results.putString("stream", "FAIL native storage probe after ${checks.size} checks (${failure::class.simpleName})\n")
        } finally {
            // Only the UUID directory and alias allocated by this synthetic test are removed.
            root.deleteRecursively()
            runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) } }
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
