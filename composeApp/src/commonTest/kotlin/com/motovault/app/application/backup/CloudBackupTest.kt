package com.motovault.app.application.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CloudBackupTest {

    @Test
    fun enforcesOwnerIsolationIdempotencyTamperDetectionAndConflictSafety() {
        val transport = InMemoryStagedCloudTransport()
        val alice = CloudAccountBinding("acct-alice", "alice@example.com", "vault-shared-id")
        val aliceCoord = CloudBackupCoordinator(transport).apply { connect(alice) }

        val assetBytes = "encrypted-smart-card".encodeToByteArray()
        val asset = CloudAssetObject("doc-1", CloudBackupCoordinator.digestHex(assetBytes), assetBytes)
        val archiveBytes = "encrypted-mva-rev1".encodeToByteArray()

        val first = aliceCoord.uploadFrozenRevision(1L, "op-1", archiveBytes, listOf(asset), 1_000L)
        assertIs<CloudPublishOutcome.Published>(first)
        assertEquals(CloudConnectionStatus.LAST_SUCCESS, aliceCoord.status)

        val dup = aliceCoord.uploadFrozenRevision(2L, "op-1", "rev2".encodeToByteArray(), listOf(asset), 2_000L)
        assertIs<CloudPublishOutcome.DuplicateOperationId>(dup)

        val bob = CloudAccountBinding("acct-bob", "bob@example.com", "vault-shared-id")
        val bobCoord = CloudBackupCoordinator(transport).apply { connect(bob) }
        val crossWrite = bobCoord.uploadFrozenRevision(2L, "op-bob", "evil".encodeToByteArray(), emptyList(), 3_000L)
        assertIs<CloudPublishOutcome.CrossOwnerDenied>(crossWrite)
        assertIs<CloudPublishOutcome.CrossOwnerDenied>(transport.fetchLatestManifest("acct-bob", "vault-shared-id").first)

        val tamperedManifest = CloudRevisionManifest(
            vaultId = "vault-shared-id",
            ownerAccountId = "acct-alice",
            revision = 2L,
            expectedPreviousRevision = 1L,
            operationId = "op-tamper",
            archiveCipherBytes = "tampered".encodeToByteArray(),
            archiveSha256Hex = "0".repeat(64),
            assetHashes = mapOf("doc-1" to asset.sha256Hex),
            createdAtEpochMillis = 4_000L
        )
        assertIs<CloudPublishOutcome.CiphertextTampered>(transport.publishManifest("acct-alice", tamperedManifest))

        val missingAssetManifest = CloudRevisionManifest(
            vaultId = "vault-shared-id",
            ownerAccountId = "acct-alice",
            revision = 2L,
            expectedPreviousRevision = 1L,
            operationId = "op-missing",
            archiveCipherBytes = "rev2".encodeToByteArray(),
            archiveSha256Hex = CloudBackupCoordinator.digestHex("rev2".encodeToByteArray()),
            assetHashes = mapOf("missing-doc" to "0".repeat(64)),
            createdAtEpochMillis = 5_000L
        )
        assertIs<CloudPublishOutcome.MissingAsset>(transport.publishManifest("acct-alice", missingAssetManifest))

        val staleRev = CloudRevisionManifest(
            vaultId = "vault-shared-id",
            ownerAccountId = "acct-alice",
            revision = 2L,
            expectedPreviousRevision = null,
            operationId = "op-stale",
            archiveCipherBytes = "rev2".encodeToByteArray(),
            archiveSha256Hex = CloudBackupCoordinator.digestHex("rev2".encodeToByteArray()),
            assetHashes = emptyMap(),
            createdAtEpochMillis = 6_000L
        )
        val conflict = transport.publishManifest("acct-alice", staleRev)
        assertIs<CloudPublishOutcome.RevisionConflict>(conflict)
        assertEquals(1L, conflict.actualRemoteRevision)
    }

    @Test
    fun retainsNewerQueuedRevisionStopsOnLogoutAndRestoresCrossDevice() {
        val transport = InMemoryStagedCloudTransport()
        val androidDevice = CloudBackupCoordinator(transport)
        assertEquals(CloudConnectionStatus.DISCONNECTED, androidDevice.status)

        val account = CloudAccountBinding("acct-1", "rider@example.com", "vault-1")
        androidDevice.connect(account)
        androidDevice.queueLocalRevision(5L)

        val payloadRev3 = "mva-android-rev3".encodeToByteArray()
        val assetBytes = "encrypted-tax-token".encodeToByteArray()
        val asset = CloudAssetObject("tax-1", CloudBackupCoordinator.digestHex(assetBytes), assetBytes)
        val uploaded = androidDevice.uploadFrozenRevision(3L, "op-3", payloadRev3, listOf(asset), 10_000L)
        assertIs<CloudPublishOutcome.Published>(uploaded)
        assertEquals(CloudConnectionStatus.PENDING, androidDevice.status)
        assertEquals(5L, androidDevice.latestQueuedRevision)

        // Simulate cross-device iPhone recovery pulling Android's latest manifest & encrypted asset
        val (err, remoteManifest) = transport.fetchLatestManifest(account.accountId, account.vaultId)
        assertEquals(null, err)
        assertNotNull(remoteManifest)
        assertEquals(CloudBackupCoordinator.digestHex(remoteManifest.archiveCipherBytes), remoteManifest.archiveSha256Hex)
        val (assetErr, remoteAsset) = transport.fetchAsset(account.accountId, account.vaultId, "tax-1")
        assertEquals(null, assetErr)
        assertNotNull(remoteAsset)
        assertTrue(remoteAsset.cipherBytes.contentEquals(assetBytes))

        androidDevice.logout()
        assertEquals(CloudConnectionStatus.DISCONNECTED, androidDevice.status)
        assertEquals(null, androidDevice.binding)
    }
}
