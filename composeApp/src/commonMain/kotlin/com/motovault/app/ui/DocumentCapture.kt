package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

val documentTypes = listOf("Registration smart card", "Driving licence", "Tax token", "Insurance", "Fitness certificate")
fun isCardDocument(type: String) = type == "Registration smart card" || type == "Driving licence"
fun documentPageLimit(type: String) = if (isCardDocument(type)) 2 else 8
fun reviewedDocumentDetails(type: String, values: Map<String, String>, previous: Map<String, String>, includeExtras: Boolean): Map<String, String> {
    val core = if (type == "Registration smart card") setOf("registration") else emptySet()
    val extras = if (type == "Driving licence") setOf("bloodGroup", "vehicleClass", "birthDate") else setOf("registration", "chassis", "engine")
    val shown = core + if (includeExtras) extras else emptySet()
    return (previous + values.filterKeys { it in shown }).filterValues { it.isNotBlank() }
}

@Serializable
data class DocumentPage(
    val originalId: String, val previewId: String, val mimeType: String,
    val width: Int, val height: Int, val side: String,
    val acceptedId: String? = null
)

@Serializable
data class CapturedDocument(
    val pages: List<DocumentPage>, val originalPdf: String? = null, val pdfPageCount: Int = 0,
    val recognizedText: String = "", val readingFailed: Boolean = false
)

fun CapturedDocument.replacePage(index: Int, page: DocumentPage): CapturedDocument {
    require(index in pages.indices) { "Choose a stored page to retake." }
    return copy(pages = pages.mapIndexed { i, old -> if (i == index) page.copy(side = old.side) else old }, recognizedText = "", readingFailed = false)
}
fun CapturedDocument.appendPage(page: DocumentPage, type: String): CapturedDocument {
    require(pages.size < documentPageLimit(type)) { "This document already has the maximum number of pages." }
    return copy(pages = pages + page.copy(side = "Page ${pages.size + 1}"), recognizedText = "", readingFailed = false)
}

fun validateDocumentAssetId(id: String): String {
    require(id.length in 1..160 && Regex("[A-Za-z0-9_-]+\\.(jpg|jpeg|png|webp|pdf)").matches(id)) { "Invalid document file." }
    return id
}

@Composable
expect fun DocumentImportActions(type: String, backOnly: Boolean, onResult: (CapturedDocument) -> Unit, onError: (String) -> Unit)
@Composable
expect fun DocumentCamera(modifier: Modifier, captureRequest: Int, onQuality: (CaptureQuality) -> Unit, onCaptured: (DocumentPage) -> Unit, onError: (String) -> Unit)
expect suspend fun adjustDocumentPage(page: DocumentPage, turns: Int, crop: DocumentCrop): DocumentPage
expect suspend fun recognizeDocument(draft: CapturedDocument): CapturedDocument

data class CaptureQuality(val ready: Boolean = false, val glare: Boolean = false, val dark: Boolean = false, val blurred: Boolean = false) {
    val canCapture get() = ready && !glare && !dark && !blurred
    val guidance get() = when { !ready -> "Preparing camera…"; glare -> "Move away from reflected light"; dark -> "Move into better light"; blurred -> "Hold steady and check focus"; else -> "Keep all edges in view · Hold steady" }
}
@Serializable
data class DocumentCrop(val left: Float = 0f, val top: Float = 0f, val right: Float = 1f, val bottom: Float = 1f) {
    fun checked(): DocumentCrop { require(left >= 0f && top >= 0f && right <= 1f && bottom <= 1f && right - left >= .1f && bottom - top >= .1f) { "Keep a complete, readable area." }; return this }
}
expect suspend fun readDocumentAsset(id: String): ByteArray
expect fun removeDocumentAssets(ids: Set<String>)
fun CapturedDocument.assetIds(): Set<String> = pages.flatMap { listOfNotNull(it.originalId, it.previewId, it.acceptedId) }.toSet() + listOfNotNull(originalPdf)
fun VaultDocument.assetIds(): Set<String> = pages.flatMap { listOfNotNull(it.originalId, it.previewId, it.acceptedId) }.toSet() + listOfNotNull(originalPdf)

/** Owns only files allocated by this operation, never existing originals or saved pages. */
internal class DocumentAssetOperation(private val remove: (Set<String>) -> Unit = ::removeDocumentAssets) {
    private val created = mutableSetOf<String>()
    fun register(id: String): String = validateDocumentAssetId(id).also { created += it }
    fun rollback() { remove(created.toSet()) }
}

internal suspend fun <T> documentAssetOperation(operation: DocumentAssetOperation = DocumentAssetOperation(), block: suspend (DocumentAssetOperation) -> T): T {
    return try { block(operation) } catch (failure: Throwable) { operation.rollback(); throw failure }
}

internal data class DocumentAssetFile(val id: String, val modifiedAtMillis: Long, val regularFile: Boolean = true)
internal const val DOCUMENT_ORPHAN_GRACE_MILLIS = 24L * 60 * 60 * 1000
internal fun orphanDocumentAssets(files: List<DocumentAssetFile>, referenced: Set<String>, nowMillis: Long): Set<String> =
    files.filter { file -> file.regularFile && file.id !in referenced &&
        Regex("[0-9]{13}-(-?[0-9]{1,19})\\.(jpg|jpeg|png|webp|pdf)").matches(file.id) &&
        file.modifiedAtMillis > 0 && file.modifiedAtMillis <= nowMillis - DOCUMENT_ORPHAN_GRACE_MILLIS
    }.map { it.id }.toSet()

/** Run once per process after a successful restore, before enabling capture. */
expect suspend fun recoverOrphanDocumentAssets(referenced: Set<String>)
/** Moves an owner-selected recovered file back without overwriting any managed original. */
expect suspend fun restoreRecoveredDocumentAsset(id: String): String

@Composable
expect fun OpenOriginalAssetButton(id: String, mimeType: String, label: String)

object DocumentTextReader {
    fun suggestions(type: String, text: String): Map<String, String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableMapOf<String, String>()
        fun label(key: String, aliases: List<String>) {
            for ((index, line) in lines.withIndex()) {
                val alias = aliases.firstOrNull { Regex("^" + Regex.escape(it) + "(?:\\s*[:.]|\\s+|$)", RegexOption.IGNORE_CASE).containsMatchIn(line) } ?: continue
                var value = line.substring(alias.length).trim(' ', ':', '.', '-', '–')
                if (value.isBlank() && index + 1 < lines.size) value = lines[index + 1]
                if (value.isNotBlank() && value.length <= 160 && !looksLikeLabel(value)) { result[key] = value; break }
            }
        }
        label("holder", listOf("Owner's Name", "Owner Name", "Holder Name", "Name"))
        label("number", if (type == "Driving licence") listOf("Licence No", "License No", "Licence Number", "License Number", "DL No")
            else if (type == "Registration smart card") listOf("Registration No", "Registration Number", "Certificate No", "Document Number")
            else listOf("Certificate No", "Certificate Number", "Token No", "Policy No", "Document Number"))
        label("issued", listOf("Issue Date", "Date of Issue", "Issued"))
        label("expiry", listOf("Expiry Date", "Date of Expiry", "Valid Until", "Valid Up To", "Expires"))
        label("registration", listOf("Registration No", "Registration Number", "Vehicle Registration"))
        label("chassis", listOf("Chassis No", "Chassis Number", "VIN"))
        label("engine", listOf("Engine No", "Engine Number"))
        label("bloodGroup", listOf("Blood Group"))
        label("vehicleClass", listOf("Vehicle Class", "Class of Vehicle", "Authorized Classes"))
        label("birthDate", listOf("Date of Birth", "DOB"))
        if (!result.containsKey("registration")) Regex("[A-Z]+\\s+METRO[- ]+[A-Z]{1,3}\\s+\\d{2}[- ]\\d{4}", RegexOption.IGNORE_CASE).find(text)?.let { result["registration"] = it.value }
        for (key in listOf("issued", "expiry", "birthDate")) result[key]?.let { date -> normalizeDate(date)?.let { result[key] = it } ?: result.remove(key) }
        return result
    }

    private fun looksLikeLabel(value: String) = Regex("(?i)^(name|issue date|expiry date|registration no|blood group|chassis no|engine no)[:.]?$").matches(value)

    fun normalizeDate(value: String): String? {
        val digits = value.map { c -> if (c in '০'..'৯') ('0'.code + (c - '০')).toChar() else c }.joinToString("")
        runCatching { LocalDate.parse(digits) }.getOrNull()?.let { return it.toString() }
        val parts = Regex("^(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})$").matchEntire(digits)?.groupValues ?: return null
        val day = parts[1].toInt()
        val month = parts[2].toInt()
        // Ambiguous day/month order stays empty for the owner to confirm.
        if (day <= 12 && month <= 12 && day != month) return null
        return runCatching { LocalDate(parts[3].toInt(), month, day).toString() }.getOrNull()
    }
}
