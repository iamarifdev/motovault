@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.motovault.app.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motovault.app.domain.backup.MvaZipArchive
import com.motovault.app.domain.backup.toNSData
import com.motovault.app.domain.backup.toByteArray
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.VaultButton
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject

private fun iosPresenter(): UIViewController? {
    var root = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (root?.presentedViewController != null) root = root.presentedViewController
    return root
}

@Composable
actual fun BackupNativeActions(
    archiveBytes: ByteArray,
    suggestedFilename: String,
    onSaved: () -> Unit,
    onShareOpened: () -> Unit,
    onError: (String) -> Unit
) {
    val savedState by rememberUpdatedState(onSaved)
    val shareState by rememberUpdatedState(onShareOpened)
    val errorState by rememberUpdatedState(onError)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VaultButton(
            text = "ফাইলে সেভ বা শেয়ার করুন",
            kind = ButtonKind.PRIMARY,
            full = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                runCatching {
                    val presenter = requireNotNull(iosPresenter()) { "Presenter view is unavailable." }
                    val tempPath = NSTemporaryDirectory() + suggestedFilename
                    val fileUrl = NSURL.fileURLWithPath(tempPath)
                    archiveBytes.toNSData().writeToURL(fileUrl, atomically = true)

                    val activitySheet = UIActivityViewController(listOf(fileUrl), null)
                    activitySheet.popoverPresentationController?.sourceView = presenter.view
                    activitySheet.popoverPresentationController?.sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
                    activitySheet.completionWithItemsHandler = { _, completed, _, _ ->
                        if (completed) {
                            savedState()
                        }
                    }
                    presenter.presentViewController(activitySheet, animated = true, completion = null)
                    shareState()
                }.onFailure {
                    errorState("শেয়ার শীট খুলতে ব্যর্থ হয়েছে: ${it.message}")
                }
            }
        )
    }
}

@Composable
actual fun BackupFilePicker(
    onFileSelected: (ByteArray, String) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (openPicker: () -> Unit) -> Unit
) {
    val onSelectedState by rememberUpdatedState(onFileSelected)
    val onErrorState by rememberUpdatedState(onError)

    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                runCatching {
                    val url = requireNotNull(didPickDocumentsAtURLs.firstOrNull() as? NSURL) { "No file chosen." }
                    val startSuccess = url.startAccessingSecurityScopedResource()
                    val data = try {
                        val handle = requireNotNull(NSFileHandle.fileHandleForReadingFromURL(url, error = null)) {
                            "Could not open file handle."
                        }
                        val accumulated = NSMutableData()
                        try {
                            while (true) {
                                val chunk = handle.readDataOfLength(16384uL)
                                if (chunk.length == 0uL) break
                                require(accumulated.length + chunk.length <= MvaZipArchive.DEFAULT_MAX_TOTAL_BYTES.toULong()) {
                                    "File exceeds maximum allowed size (150 MB)."
                                }
                                accumulated.appendData(chunk)
                            }
                        } finally {
                            handle.closeFile()
                        }
                        accumulated
                    } finally {
                        if (startSuccess) url.stopAccessingSecurityScopedResource()
                    }

                    val filename = url.lastPathComponent ?: "backup.mva"
                    onSelectedState(data.toByteArray(), filename)
                }.onFailure {
                    onErrorState("ফাইল পড়তে ব্যর্থ হয়েছে: ${it.message}")
                }
            }
        }
    }

    content {
        runCatching {
            val presenter = requireNotNull(iosPresenter()) { "Presenter view is unavailable." }
            val picker = UIDocumentPickerViewController(
                documentTypes = listOf("public.data", "public.item", "public.archive", "public.content"),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            picker.delegate = delegate
            picker.popoverPresentationController?.sourceView = presenter.view
            picker.popoverPresentationController?.sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
            presenter.presentViewController(picker, animated = true, completion = null)
        }.onFailure {
            onErrorState("ফাইল বাছাইকারী খুলতে ব্যর্থ হয়েছে: ${it.message}")
        }
    }
}
