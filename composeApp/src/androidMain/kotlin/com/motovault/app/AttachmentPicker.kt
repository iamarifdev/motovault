package com.motovault.app.ui.redesign

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.util.Base64
import java.io.ByteArrayOutputStream

@Composable
actual fun AttachmentPicker(label: String, onSelected: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            require(context.contentResolver.getType(uri) in listOf("image/jpeg", "image/png", "image/webp")) { "Choose a JPEG, PNG, or WebP photo." }
            val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1024 * 1024) { "Each photo must be 1 MB or smaller." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.isNotEmpty()) { "The photo is empty." }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.onSuccess(onSelected).onFailure { onError(it.message ?: "Could not read this photo.") }
    }
    OutlinedButton(onClick = { launcher.launch("image/*") }) { Text(label) }
}
