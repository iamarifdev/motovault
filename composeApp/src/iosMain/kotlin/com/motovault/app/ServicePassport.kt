@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui.redesign

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.CoreImage.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy

private fun passportDirectory(): String {
    val path = (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String) + "/passports"
    check(NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null)) { "Could not create passport storage." }
    return path
}
private fun passportPresenter(): UIViewController? {
    var root = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (root?.presentedViewController != null) root = root.presentedViewController
    return root
}

actual suspend fun discardPassportExport(export: PassportExport) = withContext(Dispatchers.Default) {
    val root = passportDirectory()
    listOf(export.pdfPath,export.jsonPath).forEach { path ->
        require(path.substringBeforeLast('/') == root && path.substringAfterLast('/').startsWith("service-passport-"))
        if(NSFileManager.defaultManager.fileExistsAtPath(path)) check(NSFileManager.defaultManager.removeItemAtPath(path,error=null)) { "Could not remove the outdated export." }
    }
}

actual suspend fun renderServicePassport(bundle: PassportBundle): PassportExport = withContext(Dispatchers.Main) {
    verifyPassportBundle(encodePassportBundle(bundle), bundle.sha256)
    val stem = "${passportDirectory()}/service-passport-${recordId()}"
    val pdf = "$stem.pdf"; val json = "$stem.json"
    try {
        check(UIGraphicsBeginPDFContextToFile(pdf, CGRectMake(0.0,0.0,595.0,842.0), null)) { "Could not create PDF." }
        try {
            var page = 0; var y = 42.0
            fun start() {
                page++; y = 42.0
                UIGraphicsBeginPDFPage()
                val footer = NSAttributedString.create(string = "MotoVault / Owner-reviewed service record / Page $page", attributes = mapOf(NSFontAttributeName to UIFont.systemFontOfSize(9.0), NSForegroundColorAttributeName to UIColor.darkGrayColor))
                footer.drawInRect(CGRectMake(42.0,810.0,511.0,20.0))
            }
            start()
            for (paragraph in passportParagraphs(bundle)) {
                val attributes: Map<Any?, Any> = mapOf(NSFontAttributeName to if(paragraph.heading) UIFont.boldSystemFontOfSize(14.0) else UIFont.systemFontOfSize(11.0), NSForegroundColorAttributeName to UIColor.blackColor)
                // Measure with the same native font/fallback used to draw, preserving Unicode clusters.
                for (source in paragraph.text.split('\n')) {
                    val native = NSString.create(string = source)
                    var offset = 0uL
                    if (native.length == 0uL) { y += 20.0; continue }
                    while (offset < native.length) {
                        var end = offset; var lastBreak = offset
                        while(end < native.length) {
                            val cluster = native.rangeOfComposedCharacterSequenceAtIndex(end)
                            val next = cluster.useContents { location + length }
                            val candidate = native.substringWithRange(NSMakeRange(offset, next-offset))
                            val size = NSString.create(string = candidate).sizeWithAttributes(attributes)
                            if(size.useContents { width } > 511.0 && end > offset) break
                            end = next
                            if(candidate.lastOrNull()?.isWhitespace() == true) lastBreak = end
                        }
                        if(end < native.length && lastBreak > offset) end = lastBreak
                        check(end > offset)
                        if(y + 20.0 > 792.0) start()
                        NSAttributedString.create(string = native.substringWithRange(NSMakeRange(offset,end-offset)), attributes = attributes).drawInRect(CGRectMake(42.0,y,511.0,24.0))
                        y += 20.0; offset = end
                    }
                }
                y += if(paragraph.heading) 8.0 else 14.0
            }
            if(y + 128.0 > 792.0) start()
            val filter = requireNotNull(CIFilter.filterWithName("CIQRCodeGenerator"))
            filter.setValue(NSString.create(string = "motovault:passport:v1:${bundle.sha256}").dataUsingEncoding(NSUTF8StringEncoding), forKey = "inputMessage")
            filter.setValue("M", forKey = "inputCorrectionLevel")
            val image = requireNotNull(filter.outputImage)
            val context = CIContext.contextWithOptions(null)
            val cgImage = requireNotNull(context.createCGImage(image, image.extent))
            try {
                CGContextSetInterpolationQuality(requireNotNull(UIGraphicsGetCurrentContext()), kCGInterpolationNone)
                UIImage(cGImage = cgImage).drawInRect(CGRectMake(50.0,y+8.0,112.0,112.0))
            } finally { CGImageRelease(cgImage) }
        } finally { UIGraphicsEndPDFContext() }
        val data = requireNotNull(NSString.create(string = encodePassportBundle(bundle)).dataUsingEncoding(NSUTF8StringEncoding))
        check(data.writeToFile(json, options = NSDataWritingAtomic, error = null)) { "Could not write verification JSON." }
        PassportExport(pdf,json)
    } catch(failure: Throwable) {
        NSFileManager.defaultManager.removeItemAtPath(pdf,null); NSFileManager.defaultManager.removeItemAtPath(json,null)
        throw failure
    }
}

@Composable
actual fun PassportNativeActions(export: PassportExport, onError: (String) -> Unit) {
    var viewer by remember { mutableStateOf<UIDocumentInteractionController?>(null) }
    val delegate = remember {
        object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
            override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController = requireNotNull(passportPresenter())
        }
    }
    TextButton(onClick = {
        runCatching {
            viewer = UIDocumentInteractionController.interactionControllerWithURL(NSURL.fileURLWithPath(export.pdfPath))
            viewer?.delegate = delegate
            check(viewer?.presentPreviewAnimated(true) == true)
        }.onFailure { onError("Could not open this PDF.") }
    }) { Text("View PDF") }
    TextButton(onClick = {
        runCatching {
            val presenter = requireNotNull(passportPresenter())
            val sheet = UIActivityViewController(listOf(NSURL.fileURLWithPath(export.pdfPath),NSURL.fileURLWithPath(export.jsonPath)), null)
            sheet.popoverPresentationController?.sourceView = presenter.view
            sheet.popoverPresentationController?.sourceRect = CGRectMake(0.0,0.0,1.0,1.0)
            presenter.presentViewController(sheet,true,null)
        }.onFailure { onError("Could not open sharing. Use Save to Files in the share sheet to keep both files.") }
    }) { Text("Save to Files or share PDF and JSON") }
}

@Composable
actual fun PassportVerificationImport(onRead: (String) -> Unit, onError: (String) -> Unit) {
    val read by rememberUpdatedState(onRead); val error by rememberUpdatedState(onError)
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                runCatching {
                    val url = requireNotNull(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
                    val handle = requireNotNull(NSFileHandle.fileHandleForReadingFromURL(url, error = null)) { "Could not open verification JSON." }
                    val data = NSMutableData()
                    try {
                        while (true) {
                            val chunk = handle.readDataOfLength(8192uL)
                            if(chunk.length == 0uL) break
                            require(data.length + chunk.length <= 8uL * 1024uL * 1024uL) { "Verification file is too large." }
                            data.appendData(chunk)
                        }
                    } finally { handle.closeFile() }
                    val bytes = ByteArray(data.length.toInt())
                    if(bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
                    read(bytes.decodeToString())
                }.onFailure { error(it.message ?: "Could not read verification JSON.") }
            }
        }
    }
    TextButton(onClick = {
        val picker = UIDocumentPickerViewController(documentTypes = listOf("public.json"), inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
        picker.delegate = delegate; picker.allowsMultipleSelection = false
        passportPresenter()?.presentViewController(picker,true,null) ?: error("Could not open the JSON picker.")
    }) { Text("Import verification JSON") }
}
