package com.motovault.app.application.backup

import com.motovault.app.domain.backup.toHex

enum class CloudConnectionStatus { DISCONNECTED, PENDING, RUNNING, FAILED, LAST_SUCCESS }

data class CloudAccountBinding(
    val accountId: String,
    val displayEmail: String,
    val vaultId: String
)

data class CloudAssetObject(
    val assetId: String,
    val sha256Hex: String,
    val cipherBytes: ByteArray
)

data class CloudRevisionManifest(
    val vaultId: String,
    val ownerAccountId: String,
    val revision: Long,
    val expectedPreviousRevision: Long?,
    val operationId: String,
    val archiveCipherBytes: ByteArray,
    val archiveSha256Hex: String,
    val assetHashes: Map<String, String>,
    val createdAtEpochMillis: Long
)

sealed interface CloudPublishOutcome {
    data class Published(val revision: Long, val manifest: CloudRevisionManifest) : CloudPublishOutcome
    data object Unauthorized : CloudPublishOutcome
    data object CrossOwnerDenied : CloudPublishOutcome
    data class MissingAsset(val assetId: String) : CloudPublishOutcome
    data object CiphertextTampered : CloudPublishOutcome
    data class DuplicateOperationId(val operationId: String) : CloudPublishOutcome
    data class RevisionConflict(val actualRemoteRevision: Long?) : CloudPublishOutcome
    data object NetworkUnavailable : CloudPublishOutcome
    data object Cancelled : CloudPublishOutcome
}

interface StagedCloudTransport {
    fun uploadAsset(callerAccountId: String, vaultId: String, asset: CloudAssetObject): CloudPublishOutcome?
    fun publishManifest(callerAccountId: String, manifest: CloudRevisionManifest): CloudPublishOutcome
    fun fetchLatestManifest(callerAccountId: String, vaultId: String): Pair<CloudPublishOutcome?, CloudRevisionManifest?>
    fun fetchAsset(callerAccountId: String, vaultId: String, assetId: String): Pair<CloudPublishOutcome?, CloudAssetObject?>
}

class InMemoryStagedCloudTransport : StagedCloudTransport {
    var networkAvailable: Boolean = true
    private val vaultOwners = mutableMapOf<String, String>()
    private val assetsByVault = mutableMapOf<String, MutableMap<String, CloudAssetObject>>()
    private val manifestsByVault = mutableMapOf<String, MutableList<CloudRevisionManifest>>()
    private val seenOperations = mutableSetOf<String>()

    override fun uploadAsset(callerAccountId: String, vaultId: String, asset: CloudAssetObject): CloudPublishOutcome? {
        if (callerAccountId.isBlank()) return CloudPublishOutcome.Unauthorized
        if (!networkAvailable) return CloudPublishOutcome.NetworkUnavailable
        val existingOwner = vaultOwners[vaultId]
        if (existingOwner != null && existingOwner != callerAccountId) return CloudPublishOutcome.CrossOwnerDenied
        if (sha256Hex(asset.cipherBytes) != asset.sha256Hex.lowercase()) return CloudPublishOutcome.CiphertextTampered
        if (vaultId !in vaultOwners) vaultOwners[vaultId] = callerAccountId
        assetsByVault.getOrPut(vaultId) { mutableMapOf() }[asset.assetId] = asset
        return null
    }

    override fun publishManifest(callerAccountId: String, manifest: CloudRevisionManifest): CloudPublishOutcome {
        if (callerAccountId.isBlank()) return CloudPublishOutcome.Unauthorized
        if (!networkAvailable) return CloudPublishOutcome.NetworkUnavailable
        val owner = vaultOwners[manifest.vaultId]
        if ((owner != null && owner != callerAccountId) || manifest.ownerAccountId != callerAccountId) {
            return CloudPublishOutcome.CrossOwnerDenied
        }
        val opKey = "${manifest.ownerAccountId}:${manifest.vaultId}:${manifest.operationId}"
        if (opKey in seenOperations) return CloudPublishOutcome.DuplicateOperationId(manifest.operationId)
        if (sha256Hex(manifest.archiveCipherBytes) != manifest.archiveSha256Hex.lowercase()) {
            return CloudPublishOutcome.CiphertextTampered
        }
        val vaultAssets = assetsByVault[manifest.vaultId].orEmpty()
        for ((assetId, expectedHash) in manifest.assetHashes) {
            val stored = vaultAssets[assetId] ?: return CloudPublishOutcome.MissingAsset(assetId)
            if (stored.sha256Hex.lowercase() != expectedHash.lowercase()) return CloudPublishOutcome.CiphertextTampered
        }
        val currentRev = manifestsByVault[manifest.vaultId]?.lastOrNull()?.revision
        if (manifest.expectedPreviousRevision != currentRev) {
            return CloudPublishOutcome.RevisionConflict(currentRev)
        }
        if (manifest.vaultId !in vaultOwners) vaultOwners[manifest.vaultId] = callerAccountId
        seenOperations += opKey
        manifestsByVault.getOrPut(manifest.vaultId) { mutableListOf() }.add(manifest)
        return CloudPublishOutcome.Published(manifest.revision, manifest)
    }

    override fun fetchLatestManifest(
        callerAccountId: String,
        vaultId: String
    ): Pair<CloudPublishOutcome?, CloudRevisionManifest?> {
        if (callerAccountId.isBlank()) return CloudPublishOutcome.Unauthorized to null
        val owner = vaultOwners[vaultId]
        if (owner != null && owner != callerAccountId) return CloudPublishOutcome.CrossOwnerDenied to null
        return null to manifestsByVault[vaultId]?.lastOrNull()
    }

    override fun fetchAsset(
        callerAccountId: String,
        vaultId: String,
        assetId: String
    ): Pair<CloudPublishOutcome?, CloudAssetObject?> {
        if (callerAccountId.isBlank()) return CloudPublishOutcome.Unauthorized to null
        val owner = vaultOwners[vaultId]
        if (owner != null && owner != callerAccountId) return CloudPublishOutcome.CrossOwnerDenied to null
        return null to assetsByVault[vaultId]?.get(assetId)
    }
}

class CloudBackupCoordinator(private val transport: StagedCloudTransport) {
    var status: CloudConnectionStatus = CloudConnectionStatus.DISCONNECTED
        private set
    var binding: CloudAccountBinding? = null
        private set
    var latestQueuedRevision: Long? = null
        private set
    var publishedRevision: Long? = null
        private set
    private var sessionEpoch: Int = 0

    fun connect(account: CloudAccountBinding) {
        sessionEpoch++
        binding = account
        status = CloudConnectionStatus.PENDING
    }

    fun logout() {
        sessionEpoch++
        binding = null
        publishedRevision = null
        status = CloudConnectionStatus.DISCONNECTED
    }

    fun switchAccount(nextAccount: CloudAccountBinding) {
        logout()
        connect(nextAccount)
    }

    fun queueLocalRevision(revision: Long) {
        latestQueuedRevision = maxOf(latestQueuedRevision ?: revision, revision)
        if (binding != null && status == CloudConnectionStatus.LAST_SUCCESS) {
            status = CloudConnectionStatus.PENDING
        }
    }

    fun uploadFrozenRevision(
        revision: Long,
        operationId: String,
        archiveCipherBytes: ByteArray,
        assets: List<CloudAssetObject>,
        createdAtEpochMillis: Long
    ): CloudPublishOutcome {
        val currentBinding = binding ?: return CloudPublishOutcome.Unauthorized
        val capturedEpoch = sessionEpoch
        status = CloudConnectionStatus.RUNNING
        queueLocalRevision(revision)

        for (asset in assets) {
            if (sessionEpoch != capturedEpoch) return CloudPublishOutcome.Cancelled
            val err = transport.uploadAsset(currentBinding.accountId, currentBinding.vaultId, asset)
            if (err != null) {
                status = CloudConnectionStatus.FAILED
                return err
            }
        }
        if (sessionEpoch != capturedEpoch) return CloudPublishOutcome.Cancelled

        val (_, remote) = transport.fetchLatestManifest(currentBinding.accountId, currentBinding.vaultId)
        val manifest = CloudRevisionManifest(
            vaultId = currentBinding.vaultId,
            ownerAccountId = currentBinding.accountId,
            revision = revision,
            expectedPreviousRevision = remote?.revision,
            operationId = operationId,
            archiveCipherBytes = archiveCipherBytes,
            archiveSha256Hex = sha256Hex(archiveCipherBytes),
            assetHashes = assets.associate { it.assetId to it.sha256Hex },
            createdAtEpochMillis = createdAtEpochMillis
        )
        if (sessionEpoch != capturedEpoch) return CloudPublishOutcome.Cancelled
        val outcome = transport.publishManifest(currentBinding.accountId, manifest)
        when (outcome) {
            is CloudPublishOutcome.Published -> {
                publishedRevision = revision
                status = if ((latestQueuedRevision ?: revision) > revision) {
                    CloudConnectionStatus.PENDING
                } else {
                    CloudConnectionStatus.LAST_SUCCESS
                }
            }
            else -> status = CloudConnectionStatus.FAILED
        }
        return outcome
    }

    companion object {
        fun digestHex(bytes: ByteArray): String = sha256Hex(bytes)
    }
}

private val SHA256_K = intArrayOf(
    0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
    -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
    -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
    -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0x0bf1ca7b, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e
)

private fun sha256Hex(input: ByteArray): String {
    val paddedLen = ((input.size + 9 + 63) / 64) * 64
    val msg = ByteArray(paddedLen)
    input.copyInto(msg)
    msg[input.size] = 0x80.toByte()
    val bitLen = input.size.toLong() * 8L
    for (i in 0 until 8) msg[paddedLen - 1 - i] = ((bitLen ushr (8 * i)) and 0xFF).toByte()
    val h = intArrayOf(0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6, 0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19)
    val w = IntArray(64)
    for (off in 0 until paddedLen step 64) {
        compressBlock(msg, off, h, w)
    }
    val out = ByteArray(32)
    for (i in 0 until 8) for (b in 0 until 4) out[i * 4 + b] = ((h[i] ushr (24 - b * 8)) and 0xFF).toByte()
    return out.toHex()
}

private fun compressBlock(msg: ByteArray, off: Int, h: IntArray, w: IntArray) {
    for (i in 0 until 16) {
        val j = off + i * 4
        w[i] = ((msg[j].toInt() and 0xFF) shl 24) or ((msg[j + 1].toInt() and 0xFF) shl 16) or
            ((msg[j + 2].toInt() and 0xFF) shl 8) or (msg[j + 3].toInt() and 0xFF)
    }
    for (i in 16 until 64) {
        val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
        val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
        w[i] = w[i - 16] + s0 + w[i - 7] + s1
    }
    var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
    var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
    for (i in 0 until 64) {
        val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
        val ch = (e and f) xor (e.inv() and g)
        val temp1 = hh + s1 + ch + SHA256_K[i] + w[i]
        val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
        val maj = (a and b) xor (a and c) xor (b and c)
        val temp2 = s0 + maj
        hh = g; g = f; f = e; e = d + temp1; d = c; c = b; b = a; a = temp1 + temp2
    }
    h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh
}
