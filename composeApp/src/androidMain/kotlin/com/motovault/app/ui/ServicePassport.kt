@file:JvmName("AndroidServicePassport")
package com.motovault.app.ui

import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual suspend fun renderServicePassport(bundle: PassportBundle): PassportExport = withContext(Dispatchers.IO) {
    verifyPassportBundle(encodePassportBundle(bundle), bundle.sha256)
    val directory = File(requireNotNull(vaultContext).filesDir, "passports").also { check(it.mkdirs() || it.isDirectory) }
    val stem = "service-passport-${recordId()}"
    val pdf = File(directory, "$stem.pdf"); val json = File(directory, "$stem.json")
    try {
        renderAndroidPassport(bundle, pdf)
        json.writeText(encodePassportBundle(bundle), Charsets.UTF_8)
        PassportExport(pdf.absolutePath, json.absolutePath)
    } catch (failure: Throwable) { pdf.delete(); json.delete(); throw failure }
}

actual suspend fun discardPassportExport(export: PassportExport) = withContext(Dispatchers.IO) {
    val root = File(requireNotNull(vaultContext).filesDir,"passports").canonicalFile
    listOf(export.pdfPath,export.jsonPath).forEach { path ->
        val file = File(path).canonicalFile
        require(file.parentFile == root)
        check(!file.exists() || file.delete()) { "Could not remove the outdated export." }
    }
}

internal fun renderAndroidPassport(bundle: PassportBundle, file: File) {
    val pdf = PdfDocument()
    try {
        val margin = 42f; val bottom = 792f; val width = 511
        var pageNumber = 0
        fun start(): PdfDocument.Page {
            pageNumber++
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawText("MotoVault / Owner-reviewed service record / Page $pageNumber", margin, 822f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 9f })
            return page
        }
        var page = start(); var y = margin
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28,39,44) }
        for (paragraph in passportParagraphs(bundle)) {
            paint.textSize = if (paragraph.heading) 14f else 11f
            paint.typeface = if (paragraph.heading) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            val layout = StaticLayout.Builder.obtain(paragraph.text, 0, paragraph.text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).setLineSpacing(3f, 1f).build()
            var line = 0
            while (line < layout.lineCount) {
                if (bottom - y < layout.getLineBottom(line) - layout.getLineTop(line)) { pdf.finishPage(page); page = start(); y = margin }
                val first = line
                val top = layout.getLineTop(first)
                while (line < layout.lineCount && layout.getLineBottom(line) - top <= bottom - y) line++
                check(line > first)
                val height = layout.getLineBottom(line - 1) - top
                page.canvas.save()
                page.canvas.clipRect(margin, y, margin + width, y + height)
                page.canvas.translate(margin, y - top)
                layout.draw(page.canvas)
                page.canvas.restore()
                y += height
                if (line < layout.lineCount) { pdf.finishPage(page); page = start(); y = margin }
            }
            y += if (paragraph.heading) 8f else 14f
        }
        if (y + 128 > bottom) { pdf.finishPage(page); page = start(); y = margin }
        val matrix = QRCodeWriter().encode("motovault:passport:v1:${bundle.sha256}", BarcodeFormat.QR_CODE, 256, 256)
        val ink = Paint().apply { color = Color.BLACK }
        val scale = 128f / matrix.width
        for (x in 0 until matrix.width) for (z in 0 until matrix.height) if(matrix[x,z])
            page.canvas.drawRect(margin+x*scale,y+z*scale,margin+(x+1)*scale,y+(z+1)*scale,ink)
        pdf.finishPage(page)
        file.outputStream().use(pdf::writeTo)
    } finally { pdf.close() }
}

@Composable
actual fun PassportNativeActions(export: PassportExport, onError: (String) -> Unit) {
    val context = LocalContext.current
    val error by rememberUpdatedState(onError)
    fun uri(path: String): Uri {
        val file = File(path)
        val root = File(context.filesDir, "passports").canonicalFile
        require(file.canonicalFile.parentFile == root && file.isFile)
        return FileProvider.getUriForFile(context, "${context.packageName}.documents", file)
    }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destination ->
        if(destination != null) runCatching { requireNotNull(context.contentResolver.openOutputStream(destination)).use { output -> File(export.pdfPath).inputStream().use { it.copyTo(output) } } }.onFailure { error("Could not save PDF: ${it.message}") }
    }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { destination ->
        if(destination != null) runCatching { requireNotNull(context.contentResolver.openOutputStream(destination)).use { output -> File(export.jsonPath).inputStream().use { it.copyTo(output) } } }.onFailure { error("Could not save JSON: ${it.message}") }
    }
    TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri(export.pdfPath), "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.onFailure { error("No PDF viewer is available. Save or share the PDF to open it elsewhere.") } }) { Text("View PDF") }
    TextButton(onClick = { savePdf.launch(File(export.pdfPath).name) }) { Text("Save PDF") }
    TextButton(onClick = { saveJson.launch(File(export.jsonPath).name) }) { Text("Save verification JSON") }
    TextButton(onClick = {
        runCatching {
            val pdfUri = uri(export.pdfPath); val jsonUri = uri(export.jsonPath)
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/octet-stream")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(pdfUri,jsonUri))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newUri(context.contentResolver, "Service passport", pdfUri).apply { addItem(ClipData.Item(jsonUri)) }
            context.startActivity(Intent.createChooser(intent, "Share PDF and verification JSON"))
        }.onFailure { error("Could not open sharing: ${it.message}") }
    }) { Text("Share PDF and JSON") }
}

@Composable
actual fun PassportVerificationImport(onRead: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val read by rememberUpdatedState(onRead); val error by rememberUpdatedState(onError)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri != null) runCatching {
            requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) {
                    val count = stream.read(chunk)
                    if(count < 0) break
                    require(output.size() + count <= 8 * 1024 * 1024) { "Verification file is too large." }
                    output.write(chunk,0,count)
                }
                val bytes = output.toByteArray()
                require(bytes.size <= 8 * 1024 * 1024) { "Verification file is too large." }
                read(bytes.toString(Charsets.UTF_8))
            }
        }.onFailure { error(it.message ?: "Could not read verification JSON.") }
    }
    TextButton(onClick = { picker.launch("application/json") }) { Text("Import verification JSON") }
}
