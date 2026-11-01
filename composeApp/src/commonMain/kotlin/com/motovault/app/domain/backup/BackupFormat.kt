package com.motovault.app.domain.backup

/** Version-one authenticated-data prefixes are part of the portable archive format. */
private const val ASSET_AAD_PREFIX = "MOTOVAULT-ASSET-v1:"
private const val VAULT_AAD_PREFIX = "MOTOVAULT-VAULT-v1:"

// V1 writers use 100,000; the upper bound limits work requested by an untrusted manifest.
internal const val BACKUP_KDF_ITERATIONS = 100_000
internal const val MAX_RESTORE_KDF_ITERATIONS = 1_000_000

internal fun assetAuthenticationData(id: String): ByteArray = "$ASSET_AAD_PREFIX$id".encodeToByteArray()
internal fun vaultAuthenticationData(createdAt: Long): ByteArray = "$VAULT_AAD_PREFIX$createdAt".encodeToByteArray()
