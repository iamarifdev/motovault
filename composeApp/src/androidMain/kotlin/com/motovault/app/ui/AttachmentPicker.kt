package com.motovault.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import motovault.composeapp.generated.resources.*
import android.util.Base64
import java.io.ByteArrayOutputStream

@Composable
actual fun AttachmentPicker(label: String, onSelected: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val invalidType = appStringResource(Res.string.pick_invalid_type)
    val tooLarge = appStringResource(Res.string.pick_too_large)
    val empty = appStringResource(Res.string.pick_empty)
    val readFailed = appStringResource(Res.string.pick_read_failed)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            require(context.contentResolver.getType(uri) in listOf("image/jpeg", "image/png", "image/webp")) { invalidType }
            val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1024 * 1024) { tooLarge }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            require(bytes.isNotEmpty()) { empty }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.onSuccess(onSelected).onFailure { onError(readFailed) }
    }
    OutlinedButton(onClick = { launcher.launch("image/*") }) { Text(label) }
}
