@file:JvmName("AndroidDocumentCapture")
package com.motovault.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import java.io.File
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import com.motovault.app.storage.security.ProtectedDocumentStorage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val MAX_ASSET_BYTES = 32L * 1024 * 1024
private val scratchCleaned = java.util.concurrent.atomic.AtomicBoolean(false)
internal fun cleanDocumentScratchOnProcessStart() {
    if (!scratchCleaned.compareAndSet(false, true)) return
    for ((name, prefix) in listOf("document-processing" to "pdf-", "document-exports" to "copy-")) {
        val directory = File(vaultContext.cacheDir.canonicalFile, name)
        check(directory.canonicalFile == directory.absoluteFile) { "Invalid document cache." }
        if (!directory.exists()) continue
        requireNotNull(directory.listFiles()).forEach { file ->
            if (file.name.startsWith(prefix) && file.isFile && file.canonicalFile == file.absoluteFile) check(file.delete()) { "Could not remove a temporary document copy." }
        }
    }
}
internal fun assetFile(id: String) = File(File(vaultContext.filesDir.canonicalFile, "documents").apply { mkdirs() }, validateDocumentAssetId(id))
private tailrec fun Context.captureActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.captureActivity(); else -> null }

private suspend fun <T> Task<T>.awaitValue(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

private fun copyOriginal(uri: Uri, mime: String, operation: DocumentAssetOperation): String {
    val extension = when (mime) { "application/pdf" -> "pdf"; "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
    val id = operation.register(recordId() + "." + extension)
    val output = ByteArrayOutputStream()
        requireNotNull(vaultContext.contentResolver.openInputStream(uri)).use { input ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_ASSET_BYTES) { "Each original must be 32 MB or smaller." }
                    output.write(buffer, 0, count)
                }
                require(total > 0) { "This file is empty." }
        }
    ProtectedDocumentStorage.writeNew(id, output.toByteArray())
    return id
}

internal fun previewBitmap(file: File, maximum: Int = 1800): Bitmap {
    require(file.isFile && file.length() in 1..MAX_ASSET_BYTES) { "Could not read this image." }
    return previewBitmap(file.readBytes(), maximum)
}

internal fun previewAssetBitmap(id: String, maximum: Int = 1800): Bitmap = previewBitmap(ProtectedDocumentStorage.read(id), maximum)

private fun previewBitmap(bytes: ByteArray, maximum: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Could not read this image." }
    var sampling = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampling > maximum) sampling *= 2
    val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampling })) { "Could not read this image." }
    val orientation = runCatching { ByteArrayInputStream(bytes).use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) } }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
    }
    if (matrix.isIdentity) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { if (it !== bitmap) bitmap.recycle() }
}

internal fun saveBitmap(bitmap: Bitmap, operation: DocumentAssetOperation? = null): String {
    val id = recordId() + ".jpg"
    operation?.register(id)
    val bytes = ByteArrayOutputStream().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, output)) { "Could not save the scan." }
        output.toByteArray()
    }
    ProtectedDocumentStorage.writeNew(id, bytes)
    return id
}

private suspend fun processImages(uris: List<Uri>, type: String, operation: DocumentAssetOperation): CapturedDocument {
    require(uris.isNotEmpty() && uris.size <= documentPageLimit(type)) { "Choose up to ${documentPageLimit(type)} pages." }
    val pages = mutableListOf<DocumentPage>()
    val text = StringBuilder()
    var failed = false
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        for ((index, uri) in uris.withIndex()) {
            currentCoroutineContext().ensureActive()
            val mime = vaultContext.contentResolver.getType(uri) ?: "image/jpeg"
            require(mime in listOf("image/jpeg", "image/png", "image/webp")) { "Choose JPEG, PNG, or WebP images." }
            val original = withContext(Dispatchers.IO) { copyOriginal(uri, mime, operation) }
            val bitmap = withContext(Dispatchers.IO) { previewAssetBitmap(original) }
            try {
                val preview = withContext(Dispatchers.IO) { saveBitmap(bitmap, operation) }
                pages += DocumentPage(original, preview, mime, bitmap.width, bitmap.height, if (isCardDocument(type)) (if (index == 0) "Front" else "Back") else "Page ${index + 1}")
                try { text.append(recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitValue().text).append('\n') }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { failed = true }
            } finally { bitmap.recycle() }
        }
    } finally { recognizer.close() }
    return CapturedDocument(pages, recognizedText = text.toString().take(30000), readingFailed = failed || text.isBlank())
}

internal suspend fun processPdf(uri: Uri, type: String, operation: DocumentAssetOperation): CapturedDocument {
    val original = withContext(Dispatchers.IO) { copyOriginal(uri, "application/pdf", operation) }
    val pages = mutableListOf<DocumentPage>()
    val text = StringBuilder()
    var readingFailed = false
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try { withContext(Dispatchers.IO) {
        val descriptor = openDocumentProcessingDescriptor(original)
        val pdfRenderer = try { PdfRenderer(descriptor) } catch (failure: Exception) { descriptor.close(); throw failure }
        pdfRenderer.use { renderer ->
            require(renderer.pageCount in 1..documentPageLimit(type)) { "Choose a PDF with up to ${documentPageLimit(type)} pages." }
            for (index in 0 until renderer.pageCount) {
                currentCoroutineContext().ensureActive()
                renderer.openPage(index).use { page ->
                    val factor = 1800.0 / maxOf(page.width, page.height)
                    val bitmap = Bitmap.createBitmap((page.width * factor).toInt().coerceAtLeast(1), (page.height * factor).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val pageId = saveBitmap(bitmap, operation)
                        val previewId = saveBitmap(bitmap, operation)
                        pages += DocumentPage(pageId, previewId, "image/jpeg", bitmap.width, bitmap.height,
                            if (isCardDocument(type)) (if (index == 0) "Front" else "Back") else "Page ${index + 1}")
                        try { text.append(recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitValue().text).append('\n') }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { readingFailed = true }
                    } finally { bitmap.recycle() }
                }
            }
        }
    } } finally { recognizer.close() }
    return CapturedDocument(pages, originalPdf = original, pdfPageCount = pages.size, recognizedText = text.toString().take(30000), readingFailed = readingFailed || text.isBlank())
}

/** PdfRenderer requires seekable storage. Unlink the private scratch copy as soon as its FD is open. */
private fun openDocumentProcessingDescriptor(id: String): ParcelFileDescriptor {
    val directory = File(vaultContext.cacheDir.canonicalFile, "document-processing").apply { check(isDirectory || mkdirs()) }
    check(directory.canonicalFile == directory.absoluteFile)
    val file = File.createTempFile("pdf-", ".tmp", directory)
    var descriptor: ParcelFileDescriptor? = null
    try {
        file.writeBytes(ProtectedDocumentStorage.read(id))
        descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        check(file.delete()) { "Could not close the temporary document copy." }
        return descriptor
    } catch (failure: Throwable) { descriptor?.close(); throw failure }
    finally { file.delete() }
}

@Composable
actual fun DocumentImportActions(type: String, backOnly: Boolean, onResult: (CapturedDocument) -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val result by rememberUpdatedState(onResult)
    val error by rememberUpdatedState(onError)
    fun import(block: suspend (DocumentAssetOperation) -> CapturedDocument) {
        busy = true
        scope.launch {
            try { documentAssetOperation { operation -> result(block(operation)) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error(failure.message ?: "Could not import this copy.") }
            finally { busy = false }
        }
    }
    val images = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) import { operation -> require(!backOnly || uris.size == 1) { "Choose one back photo." }; processImages(uris, type, operation) } }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) import { operation -> processPdf(uri, type, operation) } }
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        DocumentSecondary(if (backOnly && isCardDocument(type)) "পেছনের ছবি যোগ করুন" else if (backOnly) "পৃষ্ঠার ছবি যোগ করুন" else "ছবি যোগ করুন", Modifier.weight(1f), enabled = !busy) { images.launch("image/*") }
        if (!backOnly) DocumentSecondary("PDF যোগ করুন", Modifier.weight(1f), enabled = !busy) { pdf.launch("application/pdf") }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
}

actual suspend fun adjustDocumentPage(page: DocumentPage, turns: Int, crop: DocumentCrop): DocumentPage = documentAssetOperation { operation -> withContext(Dispatchers.IO) {
    crop.checked()
    val source = previewAssetBitmap(page.acceptedId ?: page.originalId, 4096)
    val rotated = if (turns % 4 == 0) source else Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply { postRotate((turns % 4) * 90f) }, true)
    try {
        val x = (crop.left * rotated.width).toInt(); val y = (crop.top * rotated.height).toInt()
        val width = ((crop.right - crop.left) * rotated.width).toInt().coerceIn(1, rotated.width - x)
        val height = ((crop.bottom - crop.top) * rotated.height).toInt().coerceIn(1, rotated.height - y)
        val accepted = Bitmap.createBitmap(rotated, x, y, width, height)
        try {
            val id = saveBitmap(accepted, operation)
            val preview = previewAssetBitmap(id)
            try { page.copy(acceptedId = id, previewId = saveBitmap(preview, operation), width = preview.width, height = preview.height) } finally { preview.recycle() }
        } finally { if (accepted !== rotated) accepted.recycle() }
    } finally { if (rotated !== source) rotated.recycle(); source.recycle() }
} }

actual suspend fun recognizeDocument(draft: CapturedDocument): CapturedDocument {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    var failed = false
    val text = StringBuilder()
    try { draft.pages.forEach { page ->
        val bitmap = withContext(Dispatchers.IO) { previewAssetBitmap(page.acceptedId ?: page.originalId) }
        try { text.append(recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitValue().text).append('\n') }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
        finally { bitmap.recycle() }
    } } finally { recognizer.close() }
    return draft.copy(recognizedText = text.toString().take(30000), readingFailed = failed || text.isBlank())
}

actual suspend fun readDocumentAsset(id: String): ByteArray = withContext(Dispatchers.IO) {
    ProtectedDocumentStorage.read(id)
}

actual suspend fun protectDocumentAssets(referenced: Set<String>) = withContext(Dispatchers.IO) { ProtectedDocumentStorage.protectInventory(referenced) }

actual fun removeDocumentAssets(ids: Set<String>) { ProtectedDocumentStorage.discard(ids) }

private val orphanRecoveryStarted = java.util.concurrent.atomic.AtomicBoolean(false)
actual suspend fun recoverOrphanDocumentAssets(referenced: Set<String>) {
    if (!orphanRecoveryStarted.compareAndSet(false, true)) return
    withContext(Dispatchers.IO) {
        val root = vaultContext.filesDir.canonicalFile
        quarantineDocumentFiles(File(root, "documents"), File(root, "documents-recovery"), referenced, System.currentTimeMillis())
    }
}

internal fun quarantineDocumentFiles(directory: File, recovery: File, referenced: Set<String>, nowMillis: Long) {
    val files = directory.listFiles().orEmpty().map { DocumentAssetFile(it.name, it.lastModified(), it.isFile && it.canonicalPath == it.absolutePath) }
    val candidates = orphanDocumentAssets(files, referenced, nowMillis)
    if (candidates.isEmpty()) return
    if (recovery.exists() && recovery.canonicalPath != recovery.absolutePath) return
    if (!recovery.isDirectory && !recovery.mkdirs()) return
    candidates.forEach { id ->
        val source = File(directory, id)
        if (!source.isFile || source.canonicalPath != source.absolutePath) return@forEach
        val existingNames = recovery.list()?.toSet() ?: return@forEach
        var target = File(recovery, id)
        while (target.name in existingNames) target = File(recovery, id.substringBeforeLast('.') + "-recovered-" + recordId() + "." + id.substringAfterLast('.'))
        // Both directories are on private internal storage. Rename retains bytes and timestamps;
        // a failed move leaves the source intact. Recovery files are never automatically purged.
        source.renameTo(target)
    }
}

actual suspend fun restoreRecoveredDocumentAsset(id: String): String = withContext(Dispatchers.IO) {
    ProtectedDocumentStorage.restoreRecovery(id)
}

internal fun restoreRecoveredDocumentFile(directory: File, recovery: File, id: String): String {
    validateDocumentAssetId(id)
    require(recovery.canonicalPath == recovery.absolutePath) { "Recovery storage is unavailable." }
    val source = File(recovery, id)
    require(source.isFile && source.canonicalPath == source.absolutePath) { "This recovered original is unavailable." }
    require(directory.isDirectory && directory.canonicalPath == directory.absolutePath) { "Document storage is unavailable." }
    val target = File(directory, id)
    require(directory.list()?.contains(id) == false) { "A managed file already uses this name. Both originals are retained." }
    check(source.renameTo(target)) { "Could not restore this original. It remains in recovery storage." }
    return id
}

@Composable
actual fun OpenOriginalAssetButton(id: String, mimeType: String, label: String) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var exported by remember { mutableStateOf<DocumentExportCopy?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun clearExport(): Boolean {
        val copy = exported ?: return true
        val cleared = runCatching { copy.close() }.isSuccess
        if (cleared) exported = null else error = "Could not remove the temporary copy. Close the viewer and try again."
        return cleared
    }
    val viewer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { clearExport(); busy = false }
    DisposableEffect(Unit) { onDispose { clearExport() } }
    TextButton(onClick = {
        busy = true; error = null
        scope.launch {
            var pending: DocumentExportCopy? = null
            try {
                val auth = com.motovault.app.domain.privacy.NativeSystemAuth.authenticate("আসল ফাইল খুলুন", "সংবেদনশীল ফাইল দেখতে ফোনের লক দিয়ে যাচাই করুন")
                if (!auth.isSuccess) {
                    error = auth.message ?: "যাচাই ছাড়া ফাইল খোলা যাবে না।"
                    busy = false
                    return@launch
                }
                check(clearExport())
                val copy = withContext(Dispatchers.IO) {
                    createDocumentExportCopy(id).also { pending = it }
                }
                exported = copy
                pending = null
                val uri = copy.uri
                viewer.launch(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType)
                    .apply { clipData = android.content.ClipData.newRawUri("Document copy", uri) }
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Open original copy"))
            } catch (cancelled: CancellationException) { runCatching { pending?.close() }; clearExport(); busy = false; throw cancelled }
            catch (_: Exception) { runCatching { pending?.close() }; clearExport(); busy = false; error = "Could not open a viewer. You can still view the saved pages here." }
        }
    }, enabled = !busy) { Text(label) }
    error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
}

internal class DocumentExportCopy(val file: File, val uri: Uri, private val context: Context) : java.io.Closeable {
    override fun close() {
        runCatching { context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        check(!file.exists() || file.delete()) { "Could not remove the temporary document copy." }
    }
}

internal fun createDocumentExportCopy(id: String): DocumentExportCopy {
    val data = ProtectedDocumentStorage.read(id)
    val directory = File(vaultContext.cacheDir.canonicalFile, "document-exports").apply { check(isDirectory || mkdirs()) }
    check(directory.canonicalFile == directory.absoluteFile)
    val file = File.createTempFile("copy-", "." + validateDocumentAssetId(id).substringAfterLast('.'), directory)
    try {
        file.writeBytes(data)
        val uri = FileProvider.getUriForFile(vaultContext, vaultContext.packageName + ".documents", file)
        return DocumentExportCopy(file, uri, vaultContext)
    } catch (failure: Throwable) { file.delete(); throw failure }
}
