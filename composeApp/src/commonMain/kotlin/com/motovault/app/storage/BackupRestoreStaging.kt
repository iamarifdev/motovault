package com.motovault.app.storage

import com.motovault.app.storage.security.documentSha256
import com.motovault.app.storage.security.RestoreAssetCollision

import com.motovault.app.domain.backup.BackupCrypto
import com.motovault.app.domain.backup.MvaZipArchive
import com.motovault.app.domain.backup.RestoreUseCase
import com.motovault.app.domain.backup.hexToByteArray
import com.motovault.app.domain.backup.assetAuthenticationData
import com.motovault.app.ui.Appearance
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.recordId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal data class StagedRestore(val state: VaultState, val createdAssets: Set<String>)
private const val MAX_RESTORE_ID_ATTEMPTS = 32

/** Legacy archive/model adapter: it stages files but never writes the database. */
internal object BackupRestoreStaging {
    suspend fun prepare(
        archiveBytes: ByteArray,
        passphrase: String,
        assetWriter: suspend (id: String, bytes: ByteArray) -> Boolean,
        discardAssets: suspend (Set<String>) -> Unit,
        newAssetId: (String) -> String = { original -> "${recordId()}.${original.substringAfterLast('.')}" }
    ): StagedRestore {
        val created = mutableSetOf<String>()
        try {
            val (preview, entries, key) = withContext(Dispatchers.Default) {
                val validated = RestoreUseCase.validateAndPreview(::documentSha256, ::backupRideDisplayName, archiveBytes, passphrase)
                val archiveEntries = MvaZipArchive.read(archiveBytes)
                val salt = validated.manifest.kdf.saltHex.hexToByteArray()
                val derived = BackupCrypto.deriveKey(passphrase, salt, iterations = validated.manifest.kdf.iterations)
                Triple(validated, archiveEntries, derived)
            }
            val referenced = preview.payload.documents.flatMap { it.assetIds() }.toSet()
            val replacements = mutableMapOf<String, String>()
            for (asset in preview.manifest.assets.filter { it.assetId in referenced }) {
                currentCoroutineContext().ensureActive()
                val encrypted = requireNotNull(entries[asset.path])
                val aad = assetAuthenticationData(asset.assetId)
                val plain = withContext(Dispatchers.Default) { BackupCrypto.decrypt(key, encrypted, aad) }
                // Record ownership before a cancelled caller can resume and request cleanup.
                withContext(NonCancellable) {
                    var target = asset.assetId
                    var attempts = 0
                    while (true) {
                        try {
                            if (assetWriter(target, plain)) created += target
                            replacements[asset.assetId] = target
                            break
                        } catch (collision: RestoreAssetCollision) {
                            do {
                                check(attempts++ < MAX_RESTORE_ID_ATTEMPTS) { "Could not allocate a restore asset ID." }
                                target = newAssetId(asset.assetId)
                            } while (target in referenced || target in replacements.values)
                        }
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            val appearance = runCatching { Appearance.valueOf(preview.payload.appearance) }.getOrDefault(Appearance.SYSTEM)
            val restoredLogs = preview.payload.logs.map { it.toRideLog() }
            val state = preview.payload.preferences.toVaultSettings().applyTo(
                VaultState(
                    ride = preview.payload.ride?.toRide(),
                    rules = preview.payload.rules,
                    logs = restoredLogs,
                    documents = preview.payload.documents.map { document ->
                        fun mapped(id: String?) = id?.let { replacements[it] ?: it }
                        val restored = document.toVaultDocument()
                        restored.copy(
                            originalPdf = mapped(document.originalPdf),
                            pages = restored.pages.map { page -> page.copy(
                                originalId = mapped(page.originalId)!!,
                                previewId = mapped(page.previewId)!!,
                                acceptedId = mapped(page.acceptedId)
                            ) }
                        )
                    },
                    appearance = appearance,
                    autoLocation = preview.payload.autoLocation,
                    economy = VaultState.calculateEconomy(restoredLogs),
                    lastFuelOdometer = restoredLogs.filter { it.isFuel }.maxOfOrNull { it.odometer }
                )
            )
            return StagedRestore(state, created.toSet())
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                try { discardAssets(created.toSet()) } catch (cleanupFailure: Exception) {
                    failure.addSuppressed(cleanupFailure)
                }
            }
            throw failure
        }
    }
}
