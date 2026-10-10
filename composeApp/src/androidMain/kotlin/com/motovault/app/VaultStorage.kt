package com.motovault.app.ui.redesign

import android.content.Context
import android.util.AtomicFile
import java.io.File

lateinit var vaultContext: Context
private fun file() = AtomicFile(File(vaultContext.filesDir, "motovault.json"))
actual fun loadVault(): String? = if (file().baseFile.exists()) file().readFully().decodeToString()
    else vaultContext.getSharedPreferences("motovault", Context.MODE_PRIVATE).getString("vault", null)
actual fun saveVault(value: String) {
    val storage = file()
    val stream = storage.startWrite()
    try { stream.write(value.encodeToByteArray()); storage.finishWrite(stream) }
    catch (failure: Exception) { storage.failWrite(stream); throw failure }
}
