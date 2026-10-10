package com.motovault.app.ui.redesign

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AttachmentPicker(label: String, onSelected: (String) -> Unit, onError: (String) -> Unit) {
    val selected by rememberUpdatedState(onSelected)
    val reportError by rememberUpdatedState(onError)
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                runCatching {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: error("Could not read this photo.")
                    require(url.pathExtension?.lowercase() in listOf("jpg", "jpeg", "png", "webp")) { "Choose a JPEG, PNG, or WebP photo." }
                    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(requireNotNull(url.path), null)
                    val size = (attributes?.get(NSFileSize) as? NSNumber)?.unsignedLongLongValue
                    require(size != null && size in 1uL..1048576uL) { "Each photo must be 1 MB or smaller." }
                    val data = NSData.dataWithContentsOfURL(url) ?: error("Could not read this photo.")
                    data.base64EncodedStringWithOptions(0uL)
                }.onSuccess { selected(it) }.onFailure { reportError(it.message ?: "Could not read this photo.") }
            }
        }
    }
    OutlinedButton(onClick = {
        val picker = UIDocumentPickerViewController(documentTypes = listOf("public.jpeg", "public.png", "org.webmproject.webp"), inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
        picker.delegate = delegate
        var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (presenter?.presentedViewController != null) presenter = presenter.presentedViewController
        if (presenter == null) reportError("Could not open the photo picker.") else presenter.presentViewController(picker, animated = true, completion = null)
    }) { Text(label) }
}
