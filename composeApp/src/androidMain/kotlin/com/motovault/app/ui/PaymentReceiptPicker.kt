package com.motovault.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.motovault.app.storage.security.ProtectedDocumentStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.ByteArrayOutputStream
import kotlin.random.Random

private const val MAX_RECEIPT_BYTES = 1024 * 1024

@Composable
actual fun PaymentReceiptPicker(label: String, onPicked: (String) -> Unit, onError: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            try {
                val assetId = withContext(Dispatchers.IO) {
                    val mime = context.contentResolver.getType(uri)
                    val extension = when (mime) {
                        "image/png" -> "png"
                        "image/webp" -> "webp"
                        "image/jpeg" -> "jpg"
                        else -> error("Choose a JPEG, PNG or WebP receipt.")
                    }
                    val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= MAX_RECEIPT_BYTES) { "The receipt must be 1 MB or smaller." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    require(bytes.isNotEmpty()) { "The receipt is empty." }
                    val id = "receipt-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(1_000_000)}.$extension"
                    try {
                        ProtectedDocumentStorage.writeNew(id, bytes)
                    } catch (failure: Exception) {
                        runCatching { ProtectedDocumentStorage.discard(setOf(id)) }
                        throw failure
                    }
                    id
                }
                picked(assetId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed()
            } finally {
                busy = false
            }
        }
    }
    OutlinedButton(onClick = { launcher.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(label) }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
}
