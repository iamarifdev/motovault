@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.Vision.*
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.coroutineContext

internal const val documentOriginalByteLimit = 32 * 1024 * 1024

internal suspend fun <T> withDocumentReadHandle(url: NSURL, read: suspend (NSFileHandle) -> T): T {
    val scoped = url.startAccessingSecurityScopedResource()
    try {
        val handle = requireNotNull(NSFileHandle.fileHandleForReadingFromURL(url, error = null)) { "Could not open this original." }
        try { return read(handle) } finally { handle.closeFile() }
    } finally {
        if (scoped) url.stopAccessingSecurityScopedResource()
    }
}

internal suspend fun readBoundedDocumentData(url: NSURL, maximumBytes: Int = documentOriginalByteLimit): NSData {
    require(maximumBytes > 0)
    return withDocumentReadHandle(url) { handle ->
        val data = NSMutableData()
        while (true) {
            coroutineContext.ensureActive()
            // Read one byte beyond the remaining allowance to detect oversize, never truncate.
            val chunk = handle.readDataOfLength(minOf(8192uL, maximumBytes.toULong() - data.length + 1uL))
            if (chunk.length == 0uL) break
            require(data.length + chunk.length <= maximumBytes.toULong()) { "Each original must be 32 MB or smaller." }
            data.appendData(chunk)
        }
        require(data.length > 0uL) { "Could not read this original." }
        data
    }
}

private fun documentsDirectory(): String {
    val path = (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String) + "/documents"
    check(NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)) { "Could not create document storage." }
    return path
}
internal fun assetPath(id: String) = documentsDirectory() + "/" + validateDocumentAssetId(id)
internal fun saveData(data: NSData, extension: String, operation: DocumentAssetOperation? = null): String {
    require(data.length in 1uL..33554432uL) { "Each original must be 32 MB or smaller." }
    val id = recordId() + "." + extension
    operation?.register(id)
    check(data.writeToFile(assetPath(id), options = NSDataWritingAtomic, error = null)) { "Could not save this original." }
    return id
}
private fun presenter(): UIViewController? {
    var root = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (root?.presentedViewController != null) root = root.presentedViewController
    return root
}

@OptIn(ExperimentalForeignApi::class)
internal fun preview(image: UIImage, maximum: Double = 1800.0): UIImage {
    val width = image.size.useContents { width }
    val height = image.size.useContents { height }
    require(width > 0 && height > 0) { "Could not read this image." }
    val scale = minOf(1.0, maximum / maxOf(width, height))
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width * scale, height * scale), true, 1.0)
    return try {
        UIColor.whiteColor.setFill()
        UIRectFill(CGRectMake(0.0, 0.0, width * scale, height * scale))
        image.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
        requireNotNull(UIGraphicsGetImageFromCurrentImageContext())
    } finally { UIGraphicsEndImageContext() }
}

@OptIn(ExperimentalForeignApi::class)
private fun recognize(image: UIImage): String {
    val cgImage = requireNotNull(image.CGImage)
    val request = VNRecognizeTextRequest()
    request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
    request.recognitionLanguages = listOf("en-US")
    request.usesLanguageCorrection = false
    val handler = VNImageRequestHandler(cgImage, platform.ImageIO.kCGImagePropertyOrientationUp, emptyMap<Any?, Any>())
    check(handler.performRequests(listOf(request), error = null)) { "Text could not be read." }
    return request.results.orEmpty().mapNotNull { result ->
        (result as? VNRecognizedTextObservation)?.topCandidates(1uL)?.firstOrNull()?.let { (it as? VNRecognizedText)?.string }
    }.joinToString("\n")
}

@OptIn(ExperimentalForeignApi::class)
private fun processImages(images: List<UIImage>, type: String, operation: DocumentAssetOperation, originals: List<Pair<String, String>>? = null, pdf: String? = null): CapturedDocument {
    require(images.isNotEmpty() && images.size <= documentPageLimit(type)) { "Choose up to ${documentPageLimit(type)} pages." }
    var failed = false
    val text = StringBuilder()
    val pages = images.mapIndexed { index, image ->
        val original = originals?.get(index) ?: (saveData(requireNotNull(UIImageJPEGRepresentation(image, .98)), "jpg", operation) to "image/jpeg")
        val small = preview(image)
        val previewId = saveData(requireNotNull(UIImageJPEGRepresentation(small, .96)), "jpg", operation)
        try { text.append(recognize(small)).append('\n') } catch (_: Exception) { failed = true }
        DocumentPage(original.first, previewId, original.second, small.size.useContents { width.toInt() }, small.size.useContents { height.toInt() },
            if (isCardDocument(type)) (if (index == 0) "Front" else "Back") else "Page ${index + 1}")
    }
    return CapturedDocument(pages, pdf, if (pdf != null) pages.size else 0, text.toString().take(30000), failed || text.isBlank())
}

@OptIn(ExperimentalForeignApi::class)
private fun processPdf(data: NSData, type: String, operation: DocumentAssetOperation): CapturedDocument {
    val id = saveData(data, "pdf", operation)
    val bytes = assetPath(id).encodeToByteArray()
    val url = bytes.usePinned { CFURLCreateFromFileSystemRepresentation(null, it.addressOf(0).reinterpret(), bytes.size.toLong(), false) }
    requireNotNull(url)
    val document = try { requireNotNull(CGPDFDocumentCreateWithURL(url)) { "Could not read this PDF." } } finally { CFRelease(url) }
    val images = try {
        val count = CGPDFDocumentGetNumberOfPages(document).toInt()
        require(count in 1..documentPageLimit(type)) { "Choose a PDF with up to ${documentPageLimit(type)} pages." }
        (1..count).map { index ->
            val page = requireNotNull(CGPDFDocumentGetPage(document, index.toULong()))
            val bounds = CGPDFPageGetBoxRect(page, kCGPDFMediaBox)
            val width = bounds.useContents { size.width }
            val height = bounds.useContents { size.height }
            require(width > 0 && height > 0)
            val factor = 1800.0 / maxOf(width, height)
            val target = CGRectMake(0.0, 0.0, width * factor, height * factor)
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(width * factor, height * factor), true, 1.0)
            try {
                UIColor.whiteColor.setFill(); UIRectFill(target)
                val context = requireNotNull(UIGraphicsGetCurrentContext())
                CGContextTranslateCTM(context, 0.0, height * factor)
                CGContextScaleCTM(context, 1.0, -1.0)
                CGContextConcatCTM(context, CGPDFPageGetDrawingTransform(page, kCGPDFMediaBox, target, 0, true))
                CGContextDrawPDFPage(context, page)
                requireNotNull(UIGraphicsGetImageFromCurrentImageContext())
            } finally { UIGraphicsEndImageContext() }
        }
    } finally { CGPDFDocumentRelease(document) }
    return processImages(images, type, operation, pdf = id)
}

@Composable
actual fun DocumentImportActions(type: String, backOnly: Boolean, onResult: (CapturedDocument) -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val result by rememberUpdatedState(onResult)
    val error by rememberUpdatedState(onError)
    val kind by rememberUpdatedState(type)
    val back by rememberUpdatedState(backOnly)
    var busy by remember { mutableStateOf(false) }
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val urls = didPickDocumentsAtURLs.mapNotNull { it as? NSURL }
                val currentKind = kind; val onlyBack = back
                busy = true
                scope.launch {
                    try { documentAssetOperation { operation -> result(withContext(Dispatchers.Default) {
                        require(urls.isNotEmpty() && urls.size <= if (onlyBack) 1 else documentPageLimit(currentKind)) { "Choose fewer pages." }
                        if (urls.size == 1 && urls.first().pathExtension?.lowercase() == "pdf") processPdf(readBoundedDocumentData(urls.first()), currentKind, operation)
                        else {
                            val originals = mutableListOf<Pair<String, String>>()
                            val images = urls.map { url ->
                                val extension = url.pathExtension?.lowercase().orEmpty()
                                require(extension in listOf("jpg", "jpeg", "png", "webp")) { "Choose JPEG, PNG, or WebP images." }
                                val data = readBoundedDocumentData(url)
                                originals += saveData(data, extension, operation) to when (extension) { "png" -> "image/png"; "webp" -> "image/webp"; else -> "image/jpeg" }
                                requireNotNull(UIImage(data = data))
                            }
                            processImages(images, currentKind, operation, originals)
                        }
                    }) } } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { error(failure.message ?: "Could not import this copy.") } finally { busy = false }
                }
            }
        }
    }
    DocumentSecondary(if (backOnly && isCardDocument(type)) "পেছনের ছবি যোগ করুন" else if (backOnly) "পৃষ্ঠার ছবি যোগ করুন" else "ছবি বা PDF যোগ করুন", Modifier.fillMaxWidth(), !busy) {
        val picker = UIDocumentPickerViewController(documentTypes = if (backOnly) listOf("public.image") else listOf("public.image", "com.adobe.pdf"), inMode = UIDocumentPickerMode.UIDocumentPickerModeImport)
        picker.allowsMultipleSelection = !backOnly; picker.delegate = delegate
        presenter()?.presentViewController(picker, true, null) ?: error("Could not open the file picker.")
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readDocumentAsset(id: String): ByteArray = withContext(Dispatchers.Default) {
    val data = requireNotNull(NSData.dataWithContentsOfFile(assetPath(id))) { "This saved copy is unavailable." }
    require(data.length in 1uL..33554432uL)
    ByteArray(data.length.toInt()).also { bytes -> bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
}

@Composable
actual fun OpenOriginalAssetButton(id: String, mimeType: String, label: String) {
    var viewer by remember { mutableStateOf<UIDocumentInteractionController?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val delegate = remember {
        object : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
            override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController = requireNotNull(presenter())
        }
    }
    TextButton(onClick = {
        runCatching {
            require(NSFileManager.defaultManager.fileExistsAtPath(assetPath(id)))
            viewer = UIDocumentInteractionController.interactionControllerWithURL(NSURL.fileURLWithPath(assetPath(id)))
            viewer?.delegate = delegate
            check(viewer?.presentPreviewAnimated(true) == true)
        }.onFailure { error = "Could not open this original. You can still view the saved pages here." }
    }) { Text(label) }
    error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
}

actual fun removeDocumentAssets(ids: Set<String>) { ids.forEach { id -> runCatching { NSFileManager.defaultManager.removeItemAtPath(assetPath(id), error = null) } } }

actual suspend fun adjustDocumentPage(page: DocumentPage, turns: Int, crop: DocumentCrop): DocumentPage = documentAssetOperation { operation -> withContext(Dispatchers.Default) {
    crop.checked()
    val source = preview(requireNotNull(UIImage(contentsOfFile = assetPath(page.acceptedId ?: page.originalId))), 4096.0)
    val width = source.size.useContents { width }; val height = source.size.useContents { height }
    val quarter = ((turns % 4) + 4) % 4
    val targetWidth = if (quarter % 2 == 1) height else width
    val targetHeight = if (quarter % 2 == 1) width else height
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), true, 1.0)
    val rotated = try {
        val context = requireNotNull(UIGraphicsGetCurrentContext())
        CGContextTranslateCTM(context, targetWidth / 2, targetHeight / 2)
        CGContextRotateCTM(context, quarter * kotlin.math.PI / 2)
        source.drawInRect(CGRectMake(-width / 2, -height / 2, width, height))
        requireNotNull(UIGraphicsGetImageFromCurrentImageContext())
    } finally { UIGraphicsEndImageContext() }
    val cg = requireNotNull(CGImageCreateWithImageInRect(requireNotNull(rotated.CGImage), CGRectMake(crop.left * targetWidth, crop.top * targetHeight, (crop.right - crop.left) * targetWidth, (crop.bottom - crop.top) * targetHeight)))
    val image = try { UIImage(cGImage = cg) } finally { CGImageRelease(cg) }
    val accepted = saveData(requireNotNull(UIImageJPEGRepresentation(image, .98)), "jpg", operation)
    val small = preview(image)
    page.copy(acceptedId = accepted, previewId = saveData(requireNotNull(UIImageJPEGRepresentation(small, .96)), "jpg", operation), width = small.size.useContents { width.toInt() }, height = small.size.useContents { height.toInt() })
} }

private var orphanRecoveryStarted = false
actual suspend fun recoverOrphanDocumentAssets(referenced: Set<String>) {
    // Called on the UI thread before capture becomes available; activity recreation cannot repeat it.
    if (orphanRecoveryStarted) return
    orphanRecoveryStarted = true
    withContext(Dispatchers.Default) {
        val manager = NSFileManager.defaultManager
        val directory = documentsDirectory()
        val files = manager.contentsOfDirectoryAtPath(directory, error = null).orEmpty().mapNotNull { value ->
            val id = value as? String ?: return@mapNotNull null
            val attributes = manager.attributesOfItemAtPath("$directory/$id", error = null) ?: return@mapNotNull null
            val modified = attributes[NSFileModificationDate] as? NSDate ?: return@mapNotNull null
            DocumentAssetFile(id, (modified.timeIntervalSince1970 * 1000).toLong(), attributes[NSFileType] == NSFileTypeRegular)
        }
        val candidates = orphanDocumentAssets(files, referenced, (NSDate().timeIntervalSince1970 * 1000).toLong())
        if (candidates.isEmpty()) return@withContext
        val recovery = directory.substringBeforeLast('/') + "/documents-recovery"
        if (manager.fileExistsAtPath(recovery) && manager.attributesOfItemAtPath(recovery, error = null)?.get(NSFileType) != NSFileTypeDirectory) return@withContext
        if (!manager.createDirectoryAtPath(recovery, true, null, null)) return@withContext
        candidates.forEach { id ->
            val source = "$directory/$id"
            if (manager.attributesOfItemAtPath(source, error = null)?.get(NSFileType) != NSFileTypeRegular) return@forEach
            var target = "$recovery/$id"
            while (manager.contentsOfDirectoryAtPath(recovery, error = null)?.contains(target.substringAfterLast('/')) == true) target = "$recovery/" + id.substringBeforeLast('.') + "-recovered-" + recordId() + "." + id.substringAfterLast('.')
            // Same-volume move keeps the original recoverable. Failure leaves it in place.
            manager.moveItemAtPath(source, target, error = null)
        }
    }
}

actual suspend fun restoreRecoveredDocumentAsset(id: String): String = withContext(Dispatchers.Default) {
    validateDocumentAssetId(id)
    val manager = NSFileManager.defaultManager
    val directory = documentsDirectory()
    val recovery = directory.substringBeforeLast('/') + "/documents-recovery"
    require(manager.attributesOfItemAtPath(recovery, error = null)?.get(NSFileType) == NSFileTypeDirectory) { "Recovery storage is unavailable." }
    val source = "$recovery/$id"
    require(manager.attributesOfItemAtPath(source, error = null)?.get(NSFileType) == NSFileTypeRegular) { "This recovered original is unavailable." }
    val target = assetPath(id)
    require(manager.contentsOfDirectoryAtPath(directory, error = null)?.contains(id) == false) { "A managed file already uses this name. Both originals are retained." }
    check(manager.moveItemAtPath(source, target, error = null)) { "Could not restore this original. It remains in recovery storage." }
    id
}

actual suspend fun recognizeDocument(draft: CapturedDocument): CapturedDocument = withContext(Dispatchers.Default) {
    var failed = false
    val text = draft.pages.joinToString("\n") { page -> runCatching { recognize(preview(requireNotNull(UIImage(contentsOfFile = assetPath(page.acceptedId ?: page.originalId))))) }.getOrElse { failed = true; "" } }
    draft.copy(recognizedText = text.take(30000), readingFailed = failed || text.isBlank())
}
