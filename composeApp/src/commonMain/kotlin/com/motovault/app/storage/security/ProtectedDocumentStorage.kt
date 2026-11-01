package com.motovault.app.storage.security

/** Local document-file boundary. This does not encrypt Room, legacy JSON or embedded photos. */
internal expect object ProtectedDocumentStorage {
    fun read(id: String): ByteArray
    fun writeNew(id: String, plaintext: ByteArray)
    /** Returns true only when this call created a new protected asset. */
    fun stageRestore(id: String, plaintext: ByteArray): Boolean
    fun migrate(ids: Set<String>)
    fun discard(ids: Set<String>)
    fun protectInventory(referenced: Set<String>)
    fun restoreRecovery(id: String): String
    fun sealRecoverySnapshot(id: String, plaintext: ByteArray): ByteArray
    fun openRecoverySnapshot(id: String, ciphertext: ByteArray): ByteArray
}
