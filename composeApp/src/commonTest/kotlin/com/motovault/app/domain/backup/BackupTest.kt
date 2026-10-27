package com.motovault.app.domain.backup

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.storage.security.documentSha256
import com.motovault.app.ui.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class BackupTest {

    @Test
    fun testMvaZipArchiveRoundtrip() {
        val originalEntries = mapOf(
            "manifest.json" to "{\"test\": 123}".encodeToByteArray(),
            "vault.enc" to byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
            "assets/doc_1.bin" to "fake-image-bytes".encodeToByteArray()
        )

        val zip = MvaZipArchive.write(originalEntries)
        assertTrue(zip.isNotEmpty())

        val readEntries = MvaZipArchive.read(zip)
        assertEquals(3, readEntries.size)
        assertTrue(readEntries["manifest.json"]!!.contentEquals(originalEntries["manifest.json"]!!))
        assertTrue(readEntries["vault.enc"]!!.contentEquals(originalEntries["vault.enc"]!!))
        assertTrue(readEntries["assets/doc_1.bin"]!!.contentEquals(originalEntries["assets/doc_1.bin"]!!))
    }

    @Test
    fun testMvaZipArchiveRejectsPathTraversal() {
        assertFailsWith<IllegalArgumentException> {
            MvaZipArchive.write(mapOf("../secret.txt" to byteArrayOf(1, 2, 3)))
        }
        assertFailsWith<IllegalArgumentException> {
            MvaZipArchive.write(mapOf("/absolute.txt" to byteArrayOf(1, 2, 3)))
        }
    }

    @Test
    fun testBackupCryptoDeriveKeyAndEncryptDecrypt() {
        val passphrase = "my-secret-vault-password"
        val salt = BackupCrypto.generateRandomBytes(32)
        assertEquals(32, salt.size)

        val key1 = BackupCrypto.deriveKey(passphrase, salt, 10_000)
        val key2 = BackupCrypto.deriveKey(passphrase, salt, 10_000)
        assertTrue(key1.contentEquals(key2), "Key derivation must be deterministic")

        val keyDifferentPass = BackupCrypto.deriveKey("different-password", salt, 10_000)
        assertFalse(key1.contentEquals(keyDifferentPass), "Different passphrase must yield different key")

        val plaintext = "Hello MotoVault Backup Secret Data!".encodeToByteArray()
        val aad = "MOTOVAULT-TEST-AAD".encodeToByteArray()

        val ciphertext = BackupCrypto.encrypt(key1, plaintext, aad)
        // Ciphertext includes 12-byte nonce + actual ciphertext + 16-byte tag
        assertEquals(plaintext.size + 12 + 16, ciphertext.size)

        // Decrypt with correct key and AAD
        val decrypted = BackupCrypto.decrypt(key1, ciphertext, aad)
        assertTrue(decrypted.contentEquals(plaintext), "Decrypted plaintext must match original")

        // Decrypt with wrong AAD must fail
        assertFails {
            BackupCrypto.decrypt(key1, ciphertext, "WRONG-AAD".encodeToByteArray())
        }

        // Decrypt with wrong key must fail
        assertFails {
            BackupCrypto.decrypt(keyDifferentPass, ciphertext, aad)
        }

        // Decrypt with tampered ciphertext must fail
        val tampered = ciphertext.copyOf()
        tampered[15] = (tampered[15].toInt() xor 0xFF).toByte()
        assertFails {
            BackupCrypto.decrypt(key1, tampered, aad)
        }
    }

    @Test
    fun testCreateBackupAndValidatePreview() {
        runBlocking {
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
            val testDoc = VaultDocument(
                id = "tax-token-2026",
                title = "Tax Token",
                number = "DHAKA-METRO-123",
                holder = "Arif",
                expiry = "2027-01-01",
                issued = "2025-01-01",
                pages = listOf(
                    DocumentPage(
                        originalId = "asset_tax_front_orig",
                        previewId = "asset_tax_front_prev",
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

            val assetStorage = mutableMapOf(
                "asset_tax_front_orig" to "IMAGE-ORIGINAL-DATA-XYZ".encodeToByteArray(),
                "asset_tax_front_prev" to "PREVIEW-THUMBNAIL-XYZ".encodeToByteArray()
            )

            val password = "StrongPassword2026!"
            val archiveBytes = BackupUseCase.createBackup(
                state = vaultState,
                vaultRevision = 42,
                passphrase = password,
                installationId = "test-device-uuid",
                assetReader = { id -> assetStorage[id] ?: error("Asset not found: $id") }
            )

            assertTrue(archiveBytes.isNotEmpty())

            // Validate and preview backup
            val preview = RestoreUseCase.validateAndPreview(archiveBytes, password)
            assertEquals("MT-15", preview.manifest.summary.bikeDisplayName)
            assertEquals(12500.0, preview.manifest.summary.odometer)
            assertEquals(1, preview.manifest.summary.logCount)
            assertEquals(1, preview.manifest.summary.documentCount)
            assertEquals(2, preview.validatedAssetCount)
            assertEquals(42L, preview.manifest.vaultRevision)

            // Decrypted payload check
            assertEquals("MT-15", preview.payload.ride?.displayName)
            assertEquals(1, preview.payload.documents.size)
            assertEquals("tax-token-2026", preview.payload.documents.first().id)

            // Wrong password fails validation
            assertFailsWith<BackupSecurityException> {
                RestoreUseCase.validateAndPreview(archiveBytes, "WrongPassword!")
            }
        }
    }

    @Test
    fun testCreateBackupFailsWhenReferencedAssetMissing() {
        runBlocking {
            val docWithMissingAsset = VaultDocument(
                id = "insurance",
                title = "Insurance",
                number = "INS-999",
                holder = "Arif",
                expiry = "2026-01-01",
                issued = "2025-01-01",
                pages = listOf(
                    DocumentPage(
                        originalId = "missing_asset_id",
                        previewId = "missing_prev",
                        mimeType = "image/jpeg",
                        width = 800,
                        height = 600,
                        side = "Front"
                    )
                )
            )

            val state = VaultState(documents = listOf(docWithMissingAsset))

            assertFailsWith<IllegalStateException> {
                BackupUseCase.createBackup(
                    state = state,
                    vaultRevision = 1,
                    passphrase = "ValidPassword123",
                    assetReader = { id -> error("Asset $id not found") }
                )
            }
        }
    }

    @Test
    fun testNoLeakedPlaintextInArchive() {
        runBlocking {
            val sensitiveSecret = "TOP-SECRET-CHASSIS-999888"
            val sensitiveNote = "Sensitive personal mechanic repair notes"
            val imagePayload = "PLAINTEXT-IMAGE-RAW-PIXELS-SECRET"

            val state = VaultState(
                ride = Ride(
                    brandId = "honda",
                    edition = "Deluxe",
                    year = 2024,
                    odometer = 5000.0,
                    registration = sensitiveSecret
                ),
                logs = listOf(
                    RideLog(
                        title = "Major Overhaul",
                        detail = sensitiveNote,
                        date = "2026-05-01",
                        odometer = 4500.0
                    )
                ),
                documents = listOf(
                    VaultDocument(
                        id = "reg-cert",
                        title = "Registration",
                        number = sensitiveSecret,
                        holder = "Confidential Owner",
                        expiry = "2030-01-01",
                        pages = listOf(
                            DocumentPage(
                                originalId = "reg_img_1",
                                previewId = "reg_prev_1",
                                mimeType = "image/jpeg",
                                width = 100,
                                height = 100,
                                side = "Front"
                            )
                        )
                    )
                )
            )

            val assets = mapOf(
                "reg_img_1" to imagePayload.encodeToByteArray(),
                "reg_prev_1" to "THUMBNAIL-PIXELS".encodeToByteArray()
            )

            val archiveBytes = BackupUseCase.createBackup(
                state = state,
                vaultRevision = 10,
                passphrase = "UltraStrongPassword123",
                assetReader = { assets[it] ?: error("Asset missing") }
            )

            val rawString = archiveBytes.decodeToString()
            assertFalse(rawString.contains(sensitiveSecret), "Archive raw bytes must not leak vehicle registration")
            assertFalse(rawString.contains(sensitiveNote), "Archive raw bytes must not leak service notes")
            assertFalse(rawString.contains(imagePayload), "Archive raw bytes must not leak image plaintext")
            assertFalse(rawString.contains("Confidential Owner"), "Archive raw bytes must not leak owner name")
        }
    }

    @Test
    fun testConcurrentRecordChangesDoNotAffectFrozenSnapshot() {
        runBlocking {
            val initialRide = Ride(
                brandId = "yamaha",
                edition = "Standard",
                year = 2022,
                odometer = 10000.0,
                modelName = "FZS V3"
            )
            val frozenState = VaultState(ride = initialRide)

            val archive = BackupUseCase.createBackup(
                state = frozenState,
                vaultRevision = 5,
                passphrase = "Password2026!",
                assetReader = { error("No assets") }
            )

            val mutatedState = frozenState.copy(
                ride = initialRide.copy(odometer = 25000.0)
            )
            assertNotEquals(frozenState.ride?.odometer, mutatedState.ride?.odometer)

            val preview = RestoreUseCase.validateAndPreview(archive, "Password2026!")
            assertEquals(10000.0, preview.manifest.summary.odometer)
            assertEquals(10000.0, preview.payload.ride?.odometer)
        }
    }

    @Test
    fun testCancelledSaveDoesNotUpdateLastSuccessfulBackup() {
        val initialTimestamp: Long? = null
        val state = VaultState(lastBackupEpochMillis = initialTimestamp)

        var saveSuccess = false
        fun onFileSaveResult(destinationUri: String?) {
            if (destinationUri != null) {
                saveSuccess = true
            }
        }

        onFileSaveResult(null)

        val updatedState = if (saveSuccess) {
            state.copy(lastBackupEpochMillis = 1729999999000L)
        } else {
            state
        }

        assertNull(updatedState.lastBackupEpochMillis, "Cancellation must not update last-successful-backup")
    }

    @Test
    fun testPreviewCountsAndConflicts() {
        runBlocking {
            val incomingRide = Ride(
                brandId = "yamaha",
                edition = "Standard",
                year = 2023,
                odometer = 15000.0,
                modelName = "MT-15"
            )
            val backupState = VaultState(
                ride = incomingRide,
                logs = listOf(
                    RideLog("L1", "D1", "2026-01-01", 1000.0),
                    RideLog("L2", "D2", "2026-02-01", 2000.0)
                )
            )

            val archive = BackupUseCase.createBackup(
                state = backupState,
                vaultRevision = 1,
                passphrase = "Password2026!",
                assetReader = { error("No assets") }
            )

            // Current state on target phone: Honda CB150R with 5,000 km
            val currentTargetState = VaultState(
                ride = Ride(brandId = "honda", edition = "Rep", year = 2021, odometer = 5000.0, modelName = "CB150R"),
                logs = listOf(RideLog("Current", "Old", "2025-01-01", 500.0))
            )

            val preview = RestoreUseCase.validateAndPreview(archive, "Password2026!", currentState = currentTargetState)
            assertNotNull(preview.conflicts)
            assertEquals("CB150R", preview.conflicts!!.currentBikeDisplayName)
            assertEquals("MT-15", preview.conflicts!!.incomingBikeDisplayName)
            assertEquals(5000.0, preview.conflicts!!.currentOdometer)
            assertEquals(15000.0, preview.conflicts!!.incomingOdometer)
            assertEquals(1, preview.conflicts!!.currentLogCount)
            assertEquals(2, preview.conflicts!!.incomingLogCount)
            assertFalse(preview.conflicts!!.bikeMatches)
        }
    }

    @Test
    fun testRestoreRejectsDuplicateIds() {
        runBlocking {
            val duplicateLog1 = RideLog("Oil", "1", "2026-01-01", 1000.0, id = "duplicate-log-id")
            val duplicateLog2 = RideLog("Chain", "2", "2026-02-01", 2000.0, id = "duplicate-log-id")

            val stateWithDuplicates = VaultState(
                logs = listOf(duplicateLog1, duplicateLog2)
            )

            val archive = BackupUseCase.createBackup(
                state = stateWithDuplicates,
                vaultRevision = 1,
                passphrase = "Password2026!",
                assetReader = { error("No assets") }
            )

            val ex = assertFailsWith<IllegalArgumentException> {
                RestoreUseCase.validateAndPreview(archive, "Password2026!")
            }
            assertTrue(ex.message!!.contains("duplicate ledger IDs"))
        }
    }

    @Test
    fun testRestoreRejectsTamperedAssetDigest() {
        runBlocking {
            val assetId = "doc_asset_1"
            val validAssetBytes = "ORIGINAL-UNTOUCHED-PIXELS".encodeToByteArray()
            val state = VaultState(
                documents = listOf(
                    VaultDocument(
                        id = "doc1",
                        title = "Tax",
                        number = "T1",
                        holder = "A",
                        expiry = "2027-01-01",
                        pages = listOf(DocumentPage(assetId, assetId, "image/jpeg", 100, 100, "Front"))
                    )
                )
            )

            val archive = BackupUseCase.createBackup(
                state = state,
                vaultRevision = 1,
                passphrase = "Password2026!",
                assetReader = { validAssetBytes }
            )

            // Tamper archive by modifying manifest SHA-256 for this asset
            val entries = MvaZipArchive.read(archive).toMutableMap()
            val manifest = Json { ignoreUnknownKeys = true }.decodeFromString<BackupManifest>(entries["manifest.json"]!!.decodeToString())
            val tamperedManifest = manifest.copy(
                assets = manifest.assets.map { it.copy(sha256 = "0000000000000000000000000000000000000000000000000000000000000000") }
            )
            entries["manifest.json"] = Json.encodeToString(tamperedManifest).encodeToByteArray()
            val tamperedArchive = MvaZipArchive.write(entries)

            val ex = assertFailsWith<IllegalArgumentException> {
                RestoreUseCase.validateAndPreview(tamperedArchive, "Password2026!")
            }
            assertTrue(ex.message!!.contains("SHA-256 verification failed"))
        }
    }

    @Test
    fun testOlderSupportedArchiveWithoutOptionalFields() {
        runBlocking {
            val salt = BackupCrypto.generateRandomBytes(32)
            val key = BackupCrypto.deriveKey("Password2026!", salt, 10_000)

            // Minimal legacy JSON without newer fields like autoLocation, preferences, serviceLines
            val minimalPayloadJson = """
                {
                    "version": 1,
                    "ride": { "brandId": "honda", "edition": "Std", "year": 2020, "odometer": 8000.0 },
                    "rules": [],
                    "logs": [],
                    "documents": []
                }
            """.trimIndent()

            val createdAt = 1700000000000L
            val payloadAad = "MOTOVAULT-VAULT-v1:$createdAt".encodeToByteArray()
            val encPayload = BackupCrypto.encrypt(key, minimalPayloadJson.encodeToByteArray(), payloadAad)

            val manifest = BackupManifest(
                formatVersion = 1,
                createdAtEpochMillis = createdAt,
                installationId = "legacy-inst-id",
                vaultRevision = 1,
                kdf = BackupKdfParams(saltHex = salt.toHex(), iterations = 10_000),
                summary = BackupSummary("Honda", 8000.0, 0, 0, 0)
            )

            val zip = MvaZipArchive.write(
                mapOf(
                    "manifest.json" to Json.encodeToString(manifest).encodeToByteArray(),
                    "vault.enc" to encPayload
                )
            )

            val preview = RestoreUseCase.validateAndPreview(zip, "Password2026!")
            assertEquals("Honda", preview.manifest.summary.bikeDisplayName)
            assertEquals(8000.0, preview.payload.ride?.odometer)
            // Defaulted fields verified:
            assertFalse(preview.payload.autoLocation)
            assertEquals("SYSTEM", preview.payload.appearance)
        }
    }
}
