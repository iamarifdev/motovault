package com.motovault.app.storage.security

internal actual fun documentSha256(bytes: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
