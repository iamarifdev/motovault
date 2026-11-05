@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.motovault.app.storage.security.ProtectedDocumentStorage
import kotlinx.cinterop.*
import kotlinx.datetime.Clock
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.random.Random

private const val MAX_RECEIPT_BYTES = 1024L * 1024L

@Composable
actual fun PaymentReceiptPicker(label: String, onPicked: (String) -> Unit, onError: () -> Unit) {
    val busyState = remember { mutableStateOf(false) }
    val busy = busyState.value
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onError)
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                try {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: error("Could not read this receipt.")
                    val extension = when (url.pathExtension?.lowercase()) {
                        "png" -> "png"
                        "webp" -> "webp"
                        "jpg", "jpeg" -> "jpg"
                        else -> error("Choose a JPEG, PNG or WebP receipt.")
                    }
                    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(requireNotNull(url.path), null)
                    val size = (attributes?.get(NSFileSize) as? NSNumber)?.unsignedLongLongValue
                    require(size != null && size in 1uL..MAX_RECEIPT_BYTES.toULong()) { "The receipt must be 1 MB or smaller." }
                    val data = NSData.dataWithContentsOfURL(url) ?: error("Could not read this receipt.")
                    val bytes = ByteArray(data.length.toInt())
                    require(bytes.isNotEmpty()) { "The receipt is empty." }
                    bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
                    val id = "receipt-${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(1_000_000)}.$extension"
                    try {
                        ProtectedDocumentStorage.writeNew(id, bytes)
                    } catch (failure: Exception) {
                        runCatching { ProtectedDocumentStorage.discard(setOf(id)) }
                        throw failure
                    }
                    picked(id)
                } catch (_: Exception) {
                    failed()
                } finally {
                    busyState.value = false
                }
            }
        }
    }
    OutlinedButton(onClick = {
        runCatching {
            busyState.value = true
            val picker = UIDocumentPickerViewController(
                documentTypes = listOf("public.jpeg", "public.png", "org.webmproject.webp"),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            picker.delegate = delegate
            var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
            while (presenter?.presentedViewController != null) presenter = presenter.presentedViewController
            requireNotNull(presenter).presentViewController(picker, animated = true, completion = null)
        }.onFailure { busyState.value = false; failed() }
    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(label) }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
}
