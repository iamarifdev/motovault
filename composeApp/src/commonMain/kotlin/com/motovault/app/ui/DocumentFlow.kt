package com.motovault.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable expect fun DocumentWindowAppearance(dark: Boolean)
expect fun documentDialogProperties(): DialogProperties

@Composable internal fun DocumentScreen(title: String, onDismiss: () -> Unit, onBack: () -> Unit = onDismiss, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(title) { scroll.scrollTo(0) }
    Dialog(onDismissRequest = onDismiss, properties = documentDialogProperties()) {
        DocumentWindowAppearance(LocalVaultPalette.current.dark)
        Column(Modifier.fillMaxSize().background(if (title == "Roadside inspection") Color.White else CanvasColor).safeDrawingPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(SurfaceColor).border(1.dp, BorderColor, CircleShape)
                    .clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = "ফিরে যান · Back" },
                    contentAlignment = Alignment.Center) { Text("‹", fontSize = 24.sp) }
                Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                DocumentBadge(if (title == "Roadside inspection") "Saved copy" else "On this device")
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 20.dp).padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
            Row(Modifier.fillMaxWidth().border(1.dp, BorderColor).padding(horizontal = 15.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("Garage", "Vault", "History").forEach { name -> TextButton(onClick = onDismiss, enabled = name == "Vault") { Text(name, fontSize = 11.sp, color = if (name == "Vault") MaterialTheme.colorScheme.primary else TextSecondary) } }
            }
        }
    }
}
@Composable internal fun DocumentHeading(text: String) { Text(text, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 31.sp, letterSpacing = (-1).sp) }
@Composable internal fun DocumentCopy(text: String) { Text(text, color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp) }
@Composable internal fun DocumentNote(text: String) { Text(text, color = TextSecondary, fontSize = 10.sp, lineHeight = 15.sp) }
@Composable internal fun DocumentPrimary(text: String, enabled: Boolean = true, onClick: () -> Unit) { Button(onClick, enabled = enabled, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(14.dp)) { Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp) } }
@Composable internal fun DocumentSecondary(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) { OutlinedButton(onClick, enabled = enabled, shape = RoundedCornerShape(11.dp), modifier = modifier.heightIn(min = 44.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceColor, contentColor = TextPrimary)) { Text(text, fontSize = 12.sp) } }
@Composable internal fun DocumentLink(text: String, onClick: () -> Unit) { TextButton(onClick, contentPadding = PaddingValues(vertical = 8.dp)) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) } }
@Composable internal fun DocumentBadge(text: String, warning: Boolean = false, good: Boolean = false) { Text(text, fontSize = if (good || warning) 10.sp else 9.sp, color = if (warning) WarningColor else if (good) HealthyColor else TextSecondary, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (warning) WarningColor.copy(alpha = .12f) else LocalVaultPalette.current.raised).padding(horizontal = 8.dp, vertical = 5.dp)) }
@Composable internal fun DocumentWarning(text: String) { Text(text, fontSize = 12.sp, lineHeight = 18.sp, color = WarningColor, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(WarningColor.copy(alpha = .12f)).padding(12.dp)) }

@OptIn(ExperimentalResourceApi::class)
@Composable internal fun DocumentImage(page: DocumentPage, modifier: Modifier = Modifier, accepted: Boolean = false) {
    val id = if (accepted) page.acceptedId ?: page.previewId else page.previewId
    var failed by remember(id) { mutableStateOf(false) }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, id) { val loaded = runCatching { readDocumentAsset(id).decodeToImageBitmap() }; value = loaded.getOrNull(); failed = loaded.isFailure }
    if (bitmap != null) Image(bitmap!!, "${page.side} stored document", modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    else Box(modifier.fillMaxWidth().height(85.dp), contentAlignment = Alignment.Center) { DocumentNote(if (failed) "Preview unavailable. Open the original file." else "Loading document…") }
}

@Composable private fun DocumentThumbnails(type: String, pages: List<DocumentPage>, selected: Int, onSelect: (Int) -> Unit) {
    val count = if (isCardDocument(type)) 2 else pages.size
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(count) { index ->
        val page = pages.getOrNull(index)
        Column(Modifier.width(if (count <= 2) 164.dp else 110.dp).clip(RoundedCornerShape(9.dp)).background(SurfaceColor).border(1.dp, if (index == selected) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(9.dp)).then(if (page != null) Modifier.clickable { onSelect(index) } else Modifier).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (page != null) DocumentImage(page, Modifier.height(85.dp)) else Box(Modifier.height(85.dp), contentAlignment = Alignment.Center) { DocumentNote("Back not added") }
            DocumentNote(page?.side ?: "Back")
        }
    } }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable private fun LegacyDocumentImage(value: String, modifier: Modifier = Modifier) {
    val bitmap = remember(value) { runCatching { Base64.decode(value).decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, "Stored original attachment", modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    else DocumentWarning("This stored attachment could not be displayed.")
}

@Composable internal fun DocumentCaptureFlow(state: VaultState, existing: VaultDocument?, onDismiss: () -> Unit, onManual: (String) -> Unit, onSave: VaultSave) {
    var type by rememberSaveable { mutableStateOf(existing?.title ?: documentTypes.first()) }
    var stage by rememberSaveable { mutableStateOf(if (existing == null) "choose" else "scanFront") }
    var draftJson by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewValues by rememberSaveable { mutableStateOf<String?>(null) }
    var retakeIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var addingPage by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(0) }
    var request by remember { mutableStateOf(0) }; var quality by remember { mutableStateOf(CaptureQuality()) }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var importVisible by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val draft = draftJson?.let { Json.decodeFromString<CapturedDocument>(it) }
    val protected = state.documents.flatMap { it.assetIds() }.toSet()
    fun setDraft(value: CapturedDocument) { draftJson = Json.encodeToString(CapturedDocument.serializer(), value) }
    fun discard() { draft?.let { removeDocumentAssets(it.assetIds() - protected) } }
    fun leave() { if (!busy) { discard(); onDismiss() } }
    fun scan(back: Boolean, index: Int? = null, append: Boolean = false) { retakeIndex = index; addingPage = append; request = 0; quality = CaptureQuality(); error = null; importVisible = false; stage = if (back) "scanBack" else "scanFront" }
    fun acceptImport(value: CapturedDocument) {
        if (stage == "scanBack") {
            val merged = CapturedDocument(listOfNotNull(draft?.pages?.firstOrNull()) + value.pages.first().copy(side = "Back"))
            removeDocumentAssets(value.assetIds() - merged.assetIds() - protected); setDraft(merged)
        } else if (!isCardDocument(type) && draft != null && stage == "scanFront" && (addingPage || retakeIndex != null)) {
            val merged = if (addingPage) draft.appendPage(value.pages.first(), type) else draft.replacePage(retakeIndex!!, value.pages.first())
            removeDocumentAssets((draft.assetIds() + value.assetIds()) - merged.assetIds() - protected); setDraft(merged)
        } else { discard(); setDraft(value) }
        selected = 0; stage = "crop"; importVisible = false; error = null
    }
    fun changePage(page: DocumentPage, turns: Int, crop: DocumentCrop, done: () -> Unit = {}) {
        val captures = draft ?: return
        busy = true; scope.launch { try {
            val updated = adjustDocumentPage(page, turns, crop)
            val next = captures.copy(pages = captures.pages.mapIndexed { i, item -> if (i == selected) updated else item }, recognizedText = "")
            setDraft(next); removeDocumentAssets(captures.assetIds() - next.assetIds() - protected); done()
        } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { error = failure.message } finally { busy = false } }
    }
    val title = when (stage) { "choose" -> "Add a document"; "scanFront", "scanBack" -> "Scan document"; "crop" -> "Review capture"; else -> "Confirm details" }
    DocumentScreen(title, ::leave, onBack = { if (!busy) { when (stage) { "review" -> stage = "crop"; "crop" -> scan(false); "scanBack" -> scan(false); "scanFront" -> stage = "choose"; else -> leave() }; error = null } }) {
        when (stage) {
            "choose" -> {
                Eyebrow("Start with your original"); DocumentHeading("What are we saving?"); DocumentCopy("Cards, paper certificates, or an existing digital copy.")
                Spacer(Modifier.height(4.dp))
                var moreTypes by remember { mutableStateOf(false) }
                (if (moreTypes) documentTypes else documentTypes.take(3)).forEach { kind -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(13.dp)).clickable { type = kind; discard(); draftJson = null; reviewValues = null; scan(false) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(kind, fontSize = 14.sp, fontWeight = FontWeight.SemiBold); DocumentNote(if (isCardDocument(kind)) "Physical card · Capture front and back" else "Paper or e-document · Keep the whole page")
                } }
                DocumentLink(if (moreTypes) "Show essential documents" else "Insurance and fitness certificate") { moreTypes = !moreTypes }
                Panel {
                    Text("Already have a copy?", fontSize = 16.sp, fontWeight = FontWeight.Bold); DocumentCopy("Import a photo or PDF, then check the details before saving.")
                    var expanded by remember { mutableStateOf(false) }
                    Box { DocumentSecondary(type, Modifier.fillMaxWidth()) { expanded = true }; DropdownMenu(expanded, { expanded = false }) { documentTypes.forEach { kind -> DropdownMenuItem(text = { Text(kind) }, onClick = { type = kind; expanded = false }) } } }
                    DocumentImportActions(type, false, ::acceptImport, { error = it })
                }
                DocumentLink("Enter details manually") { discard(); onManual(type) }
            }
            "scanFront", "scanBack" -> {
                val back = stage == "scanBack"
                Eyebrow(if (back) "02 / Back side" else "01 / ${if (isCardDocument(type)) "Front side" else "Whole page"}")
                DocumentHeading(if (back) "Now the other side." else "Keep every edge in view.")
                DocumentCopy(if (back) "Turn the card over. Keep the chip and barcode clear." else "Place the document on a flat surface, away from glare.")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(if (isCardDocument(type)) 2 else 1) { index -> Box(Modifier.weight(1f).height(3.dp).background(if (index == 0 || back) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(4.dp))) } }
                Box(Modifier.fillMaxWidth().layout { measurable, constraints ->
                    val width = constraints.maxWidth + 40.dp.roundToPx()
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(constraints.maxWidth, placeable.height) { placeable.placeRelative(-20.dp.roundToPx(), 0) }
                }.height(if (isCardDocument(type)) 360.dp else 410.dp).clipToBounds().background(Color(0xFF252B28))) {
                    key(stage) { DocumentCamera(Modifier.fillMaxSize(), request, { quality = it }, { page ->
                        val pages = if (back) listOfNotNull(draft?.pages?.firstOrNull()) + page.copy(side = "Back") else listOf(page.copy(side = if (isCardDocument(type)) "Front" else "Page 1"))
                        val next = if (!isCardDocument(type) && draft != null && addingPage) draft.appendPage(page, type) else if (!isCardDocument(type) && draft != null && retakeIndex != null) draft.replacePage(retakeIndex!!, page) else CapturedDocument(pages)
                        draft?.let { removeDocumentAssets(it.assetIds() - next.assetIds() - protected) }
                        setDraft(next); selected = if (retakeIndex != null) retakeIndex!!.coerceIn(next.pages.indices) else if (addingPage) next.pages.lastIndex else 0; busy = false; request = 0; quality = CaptureQuality(); stage = if (!back && isCardDocument(type)) "scanBack" else "crop"
                    }, { error = it; busy = false }) }
                    CameraGuide(isCardDocument(type), quality)
                    Text(quality.guidance, fontSize = 11.sp, color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xEB14261C)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { DocumentNote("Align all edges"); Text(if (quality.glare) "! Glare detected" else if (quality.blurred) "Check focus" else "Original colour", fontSize = 10.sp, color = if (quality.glare || quality.blurred) WarningColor else TextSecondary) }
                DocumentPrimary(if (busy) "Capturing…" else if (back) "Capture back  ◉" else "Capture ${if (isCardDocument(type)) "front" else "page"}  ◉", quality.canCapture && !busy) { busy = true; error = null; request++ }
                if (back) DocumentLink("Save front only for now") { selected = 0; stage = "crop" }
                DocumentLink("Use an existing copy") { importVisible = !importVisible }
                if (importVisible) DocumentImportActions(type, back || addingPage || retakeIndex != null, ::acceptImport, { error = it })
            }
            "crop" -> draft?.let { captures ->
                Eyebrow("Before we read the details"); DocumentHeading("A clear copy matters."); DocumentCopy("Keep the complete document. Retake if any detail is blurred or covered.")
                val page = captures.pages[selected.coerceIn(captures.pages.indices)]
                var editing by remember { mutableStateOf(false) }; var crop by remember(page.previewId) { mutableStateOf(DocumentCrop()) }
                if (editing) CropEditor(page, crop) { crop = it } else DocumentImage(page, Modifier.heightIn(max = 410.dp).clip(RoundedCornerShape(if (isCardDocument(type)) 9.dp else 4.dp)))
                DocumentNote("${page.side} · Original colour · Full document")
                DocumentThumbnails(type, captures.pages, selected) { selected = it; editing = false }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { DocumentSecondary("Retake", Modifier.weight(1f), !busy) { scan(selected == 1 && isCardDocument(type), if (isCardDocument(type)) null else selected) }; DocumentSecondary("Rotate ↻", Modifier.weight(1f), !busy) { changePage(page, 1, DocumentCrop()) } }
                DocumentSecondary(if (editing) "Apply crop" else "Adjust crop", Modifier.fillMaxWidth(), !busy) { if (!editing) editing = true else changePage(page, 0, crop) { editing = false } }
                if (isCardDocument(type) && captures.pages.size == 1) DocumentLink("Add the back side") { scan(true) }
                if (isCardDocument(type) && captures.pages.size == 2) DocumentLink("Swap front and back") { setDraft(captures.copy(pages = captures.pages.reversed().mapIndexed { index, item -> item.copy(side = if (index == 0) "Front" else "Back") }, recognizedText = "")); selected = 0 }
                if (!isCardDocument(type) && captures.pages.size < documentPageLimit(type)) DocumentLink("Add another page") { scan(false, append = true) }
                DocumentNote("Colour and detail stay intact. No filters change the document’s content. Raw capture is kept separately.")
                DocumentPrimary(if (busy) "Reading details…" else "Use these captures →", !busy && !editing) {
                    busy = true; scope.launch { try {
                        val accepted = captures.copy(pages = captures.pages.map { page -> if (page.acceptedId == null) adjustDocumentPage(page, 0, DocumentCrop()) else page })
                        setDraft(accepted); removeDocumentAssets(captures.assetIds() - accepted.assetIds() - protected)
                        setDraft(recognizeDocument(accepted)); stage = "review"
                    } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { error = failure.message ?: "Could not prepare the accepted copy." } finally { busy = false } }
                }
            }
            "review" -> draft?.let { DocumentConfirmation(state, type, existing, it, onSave, onDismiss, selected, { index -> selected = index }, initialValues = reviewValues, onValues = { values -> reviewValues = values }) }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth()); error?.let { DocumentWarning(it) }
    }
}

@Composable private fun CameraGuide(card: Boolean, quality: CaptureQuality) {
    Canvas(Modifier.fillMaxSize()) {
        val width = size.width * if (card) .88f else .64f; val height = if (card) width / 1.586f else width / .75f
        val x = (size.width - width) / 2; val y = (size.height - height) / 2
        drawRect(Color.White.copy(alpha = .18f), Offset(x, y), Size(width, height), style = Stroke(1.dp.toPx()))
        val color = if (quality.glare || quality.dark) Color(0xFFEFC277) else Color(0xFFA7E5BB); val arm = 25.dp.toPx(); val stroke = 3.dp.toPx()
        listOf(Offset(x, y) to Offset(1f, 1f), Offset(x + width, y) to Offset(-1f, 1f), Offset(x, y + height) to Offset(1f, -1f), Offset(x + width, y + height) to Offset(-1f, -1f)).forEach { (point, direction) -> drawLine(color, point, point + Offset(arm * direction.x, 0f), stroke); drawLine(color, point, point + Offset(0f, arm * direction.y), stroke) }
    }
}

@Composable private fun CropEditor(page: DocumentPage, crop: DocumentCrop, onCrop: (DocumentCrop) -> Unit) {
    DocumentNote("Drag a corner to keep the complete document inside the frame.")
    val current by rememberUpdatedState(crop)
    Box(Modifier.fillMaxWidth().aspectRatio(page.width.toFloat() / page.height)) {
        DocumentImage(page, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize().pointerInput(page.previewId) {
            var corner = 0
            detectDragGestures(onDragStart = { position -> val points = listOf(Offset(current.left * size.width, current.top * size.height), Offset(current.right * size.width, current.top * size.height), Offset(current.right * size.width, current.bottom * size.height), Offset(current.left * size.width, current.bottom * size.height)); corner = points.indices.minBy { (points[it] - position).getDistance() } }) { change, delta ->
                change.consume(); val c = current; val dx = delta.x / size.width; val dy = delta.y / size.height
                onCrop(when (corner) { 0 -> c.copy(left = (c.left + dx).coerceIn(0f, c.right - .1f), top = (c.top + dy).coerceIn(0f, c.bottom - .1f)); 1 -> c.copy(right = (c.right + dx).coerceIn(c.left + .1f, 1f), top = (c.top + dy).coerceIn(0f, c.bottom - .1f)); 2 -> c.copy(right = (c.right + dx).coerceIn(c.left + .1f, 1f), bottom = (c.bottom + dy).coerceIn(c.top + .1f, 1f)); else -> c.copy(left = (c.left + dx).coerceIn(0f, c.right - .1f), bottom = (c.bottom + dy).coerceIn(c.top + .1f, 1f)) })
            }
        }) {
            val x = crop.left * size.width; val y = crop.top * size.height; val r = crop.right * size.width; val b = crop.bottom * size.height
            drawRect(Color.Black.copy(alpha = .4f), Offset.Zero, Size(size.width, y)); drawRect(Color.Black.copy(alpha = .4f), Offset(0f, b), Size(size.width, size.height - b)); drawRect(Color.Black.copy(alpha = .4f), Offset(0f, y), Size(x, b - y)); drawRect(Color.Black.copy(alpha = .4f), Offset(r, y), Size(size.width - r, b - y))
            drawRect(Color(0xFFA7E5BB), Offset(x, y), Size(r - x, b - y), style = Stroke(2.dp.toPx())); listOf(Offset(x, y), Offset(r, y), Offset(r, b), Offset(x, b)).forEach { drawCircle(Color(0xFFA7E5BB), 7.dp.toPx(), it) }
        }
    }
}

@Composable private fun DocumentField(label: String, value: String, onChange: (String) -> Unit, attention: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        BasicTextField(value, onChange, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 14.sp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceColor).border(if (attention) 2.dp else 1.dp, if (attention) WarningColor else LocalVaultPalette.current.outline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 12.dp))
        if (attention) Text("Check this against your original document.", color = WarningColor, fontSize = 10.sp)
    }
}

@Composable private fun ColumnScope.DocumentConfirmation(state: VaultState, type: String, existing: VaultDocument?, draft: CapturedDocument, onSave: VaultSave, onDismiss: () -> Unit, selected: Int, onSelect: (Int) -> Unit, captureChanged: Boolean = true, initialValues: String? = null, onValues: (String) -> Unit = {}) {
    val suggestions = remember(draft.recognizedText) { DocumentTextReader.suggestions(type, draft.recognizedText) }
    var valuesJson by rememberSaveable(draft.pages.map { it.previewId }.joinToString()) { mutableStateOf(initialValues ?: Json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), (existing?.details ?: suggestions) + mapOf("number" to (existing?.number ?: suggestions["number"].orEmpty()), "holder" to (existing?.holder ?: suggestions["holder"].orEmpty()), "issued" to (existing?.issued ?: suggestions["issued"].orEmpty()), "expiry" to (existing?.expiry ?: suggestions["expiry"].orEmpty())))) }
    SideEffect { onValues(valuesJson) }
    val hadValues = remember { initialValues != null }
    val values = Json.decodeFromString<Map<String, String>>(valuesJson)
    var confirmed by rememberSaveable { mutableStateOf(false) }; var expanded by rememberSaveable { mutableStateOf(false) }; var viewedExtras by rememberSaveable { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    fun update(key: String, value: String) { valuesJson = Json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), values + (key to value.take(if (key in listOf("holder", "number")) 80 else 160))); confirmed = false }
    Eyebrow("You make the final check"); DocumentHeading("Does everything match?"); DocumentCopy("Check the details against your original. You can change anything we could not read clearly.")
    if (draft.pages.isNotEmpty()) DocumentThumbnails(type, draft.pages, selected, onSelect)
    DocumentWarning(if (draft.readingFailed) "Some text could not be read. Enter it manually and check every detail. Your original scan is kept either way." else if (type == "Registration smart card") "The document number needs your confirmation. Your original scan is kept either way." else "Please confirm the expiry date. Your original scan is kept either way.")
    if ((existing != null || hadValues) && suggestions.isNotEmpty()) DocumentLink("Use suggestions from this scan") { valuesJson = Json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), values + suggestions); confirmed = false }
    DocumentField("Document number", values["number"].orEmpty(), { update("number", it) }, type == "Registration smart card" || values["number"].isNullOrBlank())
    DocumentField("Holder name", values["holder"].orEmpty(), { update("holder", it) }, values["holder"].isNullOrBlank())
    DocumentField("Issue date (YYYY-MM-DD)", values["issued"].orEmpty(), { update("issued", it) }, values["issued"].isNullOrBlank() && draft.recognizedText.contains("Issue", ignoreCase = true))
    if (type == "Registration smart card") DocumentField("Registration number", values["registration"].orEmpty(), { update("registration", it) }, values["registration"].isNullOrBlank()) else DocumentField("Expiry date (YYYY-MM-DD)", values["expiry"].orEmpty(), { update("expiry", it) }, true)
    DocumentLink(if (expanded) "Hide additional details" else "Additional document details") { expanded = !expanded; if (expanded) { viewedExtras = true; confirmed = false } }
    if (expanded) {
        if (type == "Registration smart card") DocumentField("Expiry date (optional, YYYY-MM-DD)", values["expiry"].orEmpty(), { update("expiry", it) })
        val extra = if (type == "Driving licence") listOf("bloodGroup" to "Blood group", "vehicleClass" to "Authorised vehicle classes", "birthDate" to "Date of birth") else (if (type == "Registration smart card") emptyList() else listOf("registration" to "Vehicle registration")) + listOf("chassis" to "Chassis number", "engine" to "Engine number")
        extra.forEach { (key, label) -> DocumentField("$label (optional)", values[key].orEmpty(), { update(key, it) }) }
        DocumentNote("English text is suggested on this device. Enter Bangla fields manually.")
    }
    Row(Modifier.fillMaxWidth().toggleable(confirmed, role = Role.Checkbox, onValueChange = { confirmed = it }), verticalAlignment = Alignment.CenterVertically) { Checkbox(confirmed, null); Text("I checked these details against my document.", fontSize = 12.sp, lineHeight = 18.sp) }
    error?.let { DocumentWarning(it) }
    DocumentPrimary("Save to my vault →", confirmed && !values["number"].isNullOrBlank() && !values["holder"].isNullOrBlank()) {
        val document = (existing ?: VaultDocument(type, "", "", "")).copy(title = type, number = values["number"].orEmpty().trim(), holder = values["holder"].orEmpty().trim(), issued = values["issued"].orEmpty().trim(), expiry = if (type != "Registration smart card" || viewedExtras) values["expiry"].orEmpty().trim() else existing?.expiry.orEmpty(), pages = draft.pages, originalPdf = draft.originalPdf, pdfPageCount = draft.pdfPageCount, details = reviewedDocumentDetails(type, values, existing?.details.orEmpty(), viewedExtras), detailsConfirmed = true, extractionCandidates = if (captureChanged) suggestions else existing?.extractionCandidates.orEmpty(), capturedAt = if (captureChanged) Clock.System.now().toString() else existing?.capturedAt.orEmpty())
        runCatching { state.saveDocument(document) }.onSuccess { next -> onSave(next) { if (it) onDismiss() else error = "Could not save on this device." } }.onFailure { error = it.message ?: "Enter dates as YYYY-MM-DD." }
    }
}

@Composable internal fun DocumentEditFlow(state: VaultState, existing: VaultDocument?, initialType: String, onDismiss: () -> Unit, onSave: VaultSave) {
    var type by rememberSaveable { mutableStateOf(existing?.title ?: initialType) }
    var selected by rememberSaveable { mutableStateOf(0) }
    DocumentScreen("Confirm details", onDismiss) {
        if (existing == null) {
            var menu by remember { mutableStateOf(false) }
            Box { DocumentSecondary(type, Modifier.fillMaxWidth()) { menu = true }; DropdownMenu(menu, { menu = false }) { documentTypes.forEach { kind -> DropdownMenuItem(text = { Text(kind) }, onClick = { type = kind; menu = false }) } } }
            DocumentNote("Details only. You can add an original scan later.")
        }
        key(type) { DocumentConfirmation(state, type, existing, CapturedDocument(existing?.pages.orEmpty(), existing?.originalPdf, existing?.pdfPageCount ?: 0), onSave, onDismiss, selected, { selected = it }, captureChanged = false) }
    }
}

@Composable internal fun OriginalDocumentDetail(document: VaultDocument, inspection: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onScan: () -> Unit, onDelete: () -> Unit, onInspectionList: () -> Unit, onInspect: () -> Unit = onInspectionList, onRecordFee: () -> Unit = {}) {
    var selected by rememberSaveable { mutableStateOf(if (document.pages.isEmpty() && document.frontPhoto == null && document.backPhoto != null) 1 else 0) }; var zoom by remember { mutableStateOf(false) }
    val legacyPhoto = if (selected == 0) document.frontPhoto else document.backPhoto
    DocumentScreen(if (inspection) "Roadside inspection" else document.title, onDismiss) {
        if (inspection) InspectionBrightness()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Eyebrow(if (inspection) "Stored document copy" else "Your original document"); DocumentBadge(document.expiryLabel, warning = (document.daysRemaining ?: Int.MAX_VALUE) <= RENEWAL_WARNING_DAYS, good = (document.daysRemaining ?: 0) > RENEWAL_WARNING_DAYS) }
        DocumentHeading(document.title); DocumentCopy(if (inspection) "Only this document is shown." else "Open the saved image exactly as it was captured.")
        if (document.pages.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(11.dp)).border(1.dp, BorderColor, RoundedCornerShape(11.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { val count = if (isCardDocument(document.title)) 2 else document.pages.size; repeat(count) { index -> val page = document.pages.getOrNull(index); TextButton(onClick = { selected = index }, enabled = page != null, shape = RoundedCornerShape(7.dp), colors = ButtonDefaults.textButtonColors(containerColor = if (selected == index) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (selected == index) MaterialTheme.colorScheme.onPrimary else TextSecondary), modifier = if (count <= 2) Modifier.weight(1f) else Modifier) { Text(page?.side ?: "Back not added", fontSize = 12.sp) } } }
            val page = document.pages[selected.coerceIn(document.pages.indices)]
            DocumentImage(page, Modifier.clip(RoundedCornerShape(if (isCardDocument(document.title)) 9.dp else 4.dp)))
            DocumentNote("Original ${page.side.lowercase()} copy · Printed colour preserved")
            if (inspection) DocumentPrimary("Open full size ⤢") { zoom = true } else DocumentSecondary("Open full size ⤢", Modifier.fillMaxWidth()) { zoom = true }
            if (!inspection) { OpenOriginalAssetButton(page.acceptedId ?: page.originalId, if (page.acceptedId != null) "image/jpeg" else page.mimeType, "Open accepted copy"); if (page.acceptedId != null) OpenOriginalAssetButton(page.originalId, page.mimeType, "Open raw capture"); document.originalPdf?.let { OpenOriginalAssetButton(it, "application/pdf", "Open original PDF (${document.pdfPageCount} pages)") } }
        } else if (document.frontPhoto != null || document.backPhoto != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Front" to document.frontPhoto, "Back" to document.backPhoto).forEachIndexed { index, (label, photo) ->
                    DocumentSecondary(if (selected == index) "$label ✓" else label, Modifier.weight(1f), photo != null) { selected = index }
                }
            }
            legacyPhoto?.let { LegacyDocumentImage(it) }
            DocumentNote("Original ${if (selected == 0) "front" else "back"} copy · Printed colour preserved")
            DocumentSecondary("Open full size ⤢", Modifier.fillMaxWidth()) { zoom = true }
        } else Panel { DocumentCopy("Details only. Add your original scan.") }
        if (!inspection) {
            val fields = listOf("Document number" to document.number, "Holder" to document.holder, "Issue date" to document.issued.ifBlank { "Not stated" }, "Expiry" to document.expiry.ifBlank { "Not stated" }) + document.details.map { (key, value) -> (when (key) { "registration" -> "Registration"; "chassis" -> "Chassis number"; "engine" -> "Engine number"; "bloodGroup" -> "Blood group"; "vehicleClass" -> "Vehicle classes"; "birthDate" -> "Date of birth"; else -> key }) to value }
            fields.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) { row.forEach { (label, value) -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { DocumentNote(label); Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
            DocumentSecondary("নবায়ন ফি যোগ করুন · Record renewal fee", Modifier.fillMaxWidth(), onClick = onRecordFee)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { DocumentSecondary("Edit details", Modifier.weight(1f), onClick = onEdit); DocumentSecondary("Replace scan", Modifier.weight(1f), onClick = onScan) }
            DocumentLink("Open inspection view ↗", onInspect); DocumentLink("Delete document", onDelete)
        } else { DocumentNote("Saved copy · Carry originals where required."); DocumentSecondary("All documents", Modifier.fillMaxWidth(), onClick = onInspectionList); DocumentSecondary("Exit inspection", Modifier.fillMaxWidth(), onClick = onDismiss) }
    }
    if (zoom && document.pages.isNotEmpty()) DocumentZoom(document.pages[selected.coerceIn(document.pages.indices)], inspection = inspection) { zoom = false }
    else if (zoom && legacyPhoto != null) DocumentZoom(null, legacyPhoto, inspection = inspection) { zoom = false }
}

@Composable private fun DocumentZoom(page: DocumentPage?, legacy: String? = null, inspection: Boolean = false, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = documentDialogProperties()) {
        DocumentWindowAppearance(true)
        if (inspection) InspectionBrightness()
        var scale by remember { mutableStateOf(1f) }; var offset by remember { mutableStateOf(Offset.Zero) }
        val transform = rememberTransformableState { factor, pan, _ -> scale = (scale * factor).coerceIn(1f, 5f); offset = if (scale == 1f) Offset.Zero else offset + pan }
        Column(Modifier.fillMaxSize().background(Color(0xFF0E151D)).safeDrawingPadding().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Saved original", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f)); TextButton(onClick = { scale = if (scale >= 2f) 1f else scale + .5f; offset = Offset.Zero }) { Text("Zoom +", color = Color.White) }; TextButton(onClick = onDismiss) { Text("Done", color = Color.White) } }
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(4.dp)).transformable(transform), contentAlignment = Alignment.Center) {
                val imageModifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                if (page != null) DocumentImage(page, imageModifier, accepted = true) else legacy?.let { LegacyDocumentImage(it, imageModifier) }
            }
            Text("Pinch to zoom · Drag to move", color = Color(0xFFD4D9DF), fontSize = 11.sp)
        }
    }
}
