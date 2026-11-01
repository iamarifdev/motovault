package com.motovault.app.ui.backup

/** Binds the confirmation to the exact file, password and vault revision reviewed. */
internal class RestoreReviewInputs(val archive: ByteArray, val password: String, val revision: Long) {
    fun matches(selectedArchive: ByteArray?, enteredPassword: String): Boolean =
        archive === selectedArchive && password == enteredPassword
}
