package com.motovault.app.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import motovault.composeapp.generated.resources.*
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AttachmentPicker(label: String, onSelected: (String) -> Unit, onError: (String) -> Unit) {
    val selected by rememberUpdatedState(onSelected)
    val reportError by rememberUpdatedState(onError)
    val invalidType = appStringResource(Res.string.pick_invalid_type)
    val tooLarge = appStringResource(Res.string.pick_too_large)
    val readFailed = appStringResource(Res.string.pick_read_failed)
    val openFailed = appStringResource(Res.string.pick_open_failed)
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                runCatching {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: error(readFailed)
                    require(url.pathExtension?.lowercase() in listOf("jpg", "jpeg", "png", "webp")) { invalidType }
                    val attributes = NSFileManager.defaultManager.attributesOfItemAtPath(requireNotNull(url.path), null)
                    val size = (attributes?.get(NSFileSize) as? NSNumber)?.unsignedLongLongValue
                    require(size != null && size in 1uL..1048576uL) { tooLarge }
                    val data = NSData.dataWithContentsOfURL(url) ?: error(readFailed)
                    data.base64EncodedStringWithOptions(0uL)
                }.onSuccess { selected(it) }.onFailure { reportError(readFailed) }
            }
        }
    }
    OutlinedButton(onClick = {
        val picker = UIDocumentPickerViewController(documentTypes = listOf("public.jpeg", "public.png", "org.webmproject.webp"), inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
        picker.delegate = delegate
        var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (presenter?.presentedViewController != null) presenter = presenter.presentedViewController
        if (presenter == null) reportError(openFailed) else presenter.presentViewController(picker, animated = true, completion = null)
    }) { Text(label) }
}
