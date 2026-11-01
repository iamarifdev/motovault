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
import motovault.composeapp.generated.resources.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable expect fun DocumentWindowAppearance(dark: Boolean)
expect fun documentDialogProperties(): DialogProperties

@Composable internal fun DocumentScreen(title: String, onDismiss: () -> Unit, onBack: () -> Unit = onDismiss, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(title) { scroll.scrollTo(0) }
    val isInspection = title == appStringResource(Res.string.inspection_title)
    val backDescription = appStringResource(Res.string.action_back)
    val screenBadge = if (isInspection) appStringResource(Res.string.docflow_saved_badge) else appStringResource(Res.string.docflow_device_badge)
    Dialog(onDismissRequest = onDismiss, properties = documentDialogProperties()) {
        DocumentWindowAppearance(LocalVaultPalette.current.dark)
        Column(Modifier.fillMaxSize().background(if (isInspection) Color.White else CanvasColor).safeDrawingPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(SurfaceColor).border(1.dp, BorderColor, CircleShape)
                    .clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = backDescription },
                    contentAlignment = Alignment.Center) { Text("‹", fontSize = 24.sp) }
                Text(documentDisplayName(title), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                DocumentBadge(screenBadge)
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 20.dp).padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
@Composable internal fun DocumentHeading(text: String) { Text(documentDisplayName(text), fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp, letterSpacing = 0.sp) }
@Composable internal fun DocumentCopy(text: String) { Text(documentDisplayName(text), color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp) }
@Composable internal fun DocumentNote(text: String) { Text(documentDisplayName(text), color = TextSecondary, fontSize = 10.sp, lineHeight = 15.sp) }
@Composable internal fun DocumentPrimary(text: String, enabled: Boolean = true, onClick: () -> Unit) { Button(onClick, enabled = enabled, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(14.dp)) { Text(documentDisplayName(text), fontWeight = FontWeight.Bold, fontSize = 13.sp) } }
@Composable internal fun DocumentSecondary(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) { OutlinedButton(onClick, enabled = enabled, shape = RoundedCornerShape(11.dp), modifier = modifier.heightIn(min = 44.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceColor, contentColor = TextPrimary)) { Text(documentDisplayName(text), fontSize = 12.sp) } }
@Composable internal fun DocumentLink(text: String, onClick: () -> Unit) { TextButton(onClick, contentPadding = PaddingValues(vertical = 8.dp)) { Text(documentDisplayName(text), fontWeight = FontWeight.SemiBold, fontSize = 12.sp) } }
@Composable internal fun DocumentBadge(text: String, warning: Boolean = false, good: Boolean = false) { Text(documentDisplayName(text), fontSize = if (good || warning) 10.sp else 9.sp, color = if (warning) WarningColor else if (good) HealthyColor else TextSecondary, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (warning) WarningColor.copy(alpha = .12f) else LocalVaultPalette.current.raised).padding(horizontal = 8.dp, vertical = 5.dp)) }
@Composable internal fun DocumentWarning(text: String) { Text(documentDisplayName(text), fontSize = 12.sp, lineHeight = 18.sp, color = WarningColor, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(WarningColor.copy(alpha = .12f)).padding(12.dp)) }

@OptIn(ExperimentalResourceApi::class)
@Composable internal fun DocumentImage(page: DocumentPage, modifier: Modifier = Modifier, accepted: Boolean = false) {
    val id = if (accepted) page.acceptedId ?: page.previewId else page.previewId
    var failed by remember(id) { mutableStateOf(false) }
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, id) { val loaded = runCatching { readDocumentAsset(id).decodeToImageBitmap() }; value = loaded.getOrNull(); failed = loaded.isFailure }
    if (bitmap != null) Image(bitmap!!, "${page.side} stored document", modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    else Box(modifier.fillMaxWidth().height(85.dp), contentAlignment = Alignment.Center) { DocumentNote(if (failed) appStringResource(Res.string.docflow_preview_failed) else appStringResource(Res.string.docflow_opening)) }
}

@Composable private fun DocumentThumbnails(type: String, pages: List<DocumentPage>, selected: Int, onSelect: (Int) -> Unit) {
    val count = if (isCardDocument(type)) 2 else pages.size
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(count) { index ->
        val page = pages.getOrNull(index)
        Column(Modifier.width(if (count <= 2) 164.dp else 110.dp).clip(RoundedCornerShape(9.dp)).background(SurfaceColor).border(1.dp, if (index == selected) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(9.dp)).then(if (page != null) Modifier.clickable { onSelect(index) } else Modifier).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (page != null) DocumentImage(page, Modifier.height(85.dp)) else Box(Modifier.height(85.dp), contentAlignment = Alignment.Center) { DocumentNote(appStringResource(Res.string.docflow_back_missing)) }
            DocumentNote(documentSideName(page?.side ?: "Back"))
        }
    } }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable private fun LegacyDocumentImage(value: String, modifier: Modifier = Modifier) {
    val bitmap = remember(value) { runCatching { Base64.decode(value).decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, "Stored original attachment", modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
    else DocumentWarning(appStringResource(Res.string.docflow_preview_unavailable))
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
    val cropError = appStringResource(Res.string.docflow_crop_error)
    val title = when (stage) { "choose" -> appStringResource(Res.string.docflow_title_add); "scanFront", "scanBack" -> appStringResource(Res.string.docflow_title_scan); "crop" -> appStringResource(Res.string.docflow_title_crop); else -> appStringResource(Res.string.docflow_title_confirm) }
    DocumentScreen(title, ::leave, onBack = { if (!busy) { when (stage) { "review" -> stage = "crop"; "crop" -> scan(false); "scanBack" -> scan(false); "scanFront" -> stage = "choose"; else -> leave() }; error = null } }) {
        when (stage) {
            "choose" -> {
                Eyebrow(appStringResource(Res.string.docflow_start_eyebrow)); DocumentHeading(appStringResource(Res.string.docflow_choose_heading)); DocumentCopy(appStringResource(Res.string.docflow_choose_copy))
                Spacer(Modifier.height(4.dp))
                var moreTypes by remember { mutableStateOf(false) }
                (if (moreTypes) documentTypes else documentTypes.take(3)).forEach { kind -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(SurfaceColor).border(1.dp, BorderColor, RoundedCornerShape(13.dp)).clickable { type = kind; discard(); draftJson = null; reviewValues = null; scan(false) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(documentDisplayName(kind), fontSize = 14.sp, fontWeight = FontWeight.SemiBold); DocumentNote(if (isCardDocument(kind)) appStringResource(Res.string.docflow_card_sides) else appStringResource(Res.string.docflow_page_copy))
                } }
                DocumentLink(if (moreTypes) appStringResource(Res.string.docflow_more_types) else appStringResource(Res.string.docflow_insurance_fitness)) { moreTypes = !moreTypes }
                Panel {
                    Text(appStringResource(Res.string.docflow_existing_copy), fontSize = 16.sp, fontWeight = FontWeight.Bold); DocumentCopy(appStringResource(Res.string.docflow_existing_desc))
                    var expanded by remember { mutableStateOf(false) }
                    Box { DocumentSecondary(documentDisplayName(type), Modifier.fillMaxWidth()) { expanded = true }; DropdownMenu(expanded, { expanded = false }) { documentTypes.forEach { kind -> DropdownMenuItem(text = { Text(documentDisplayName(kind)) }, onClick = { type = kind; expanded = false }) } } }
                    DocumentImportActions(type, false, ::acceptImport, { error = it })
                }
                DocumentLink(appStringResource(Res.string.docflow_manual)) { discard(); onManual(type) }
            }
            "scanFront", "scanBack" -> {
                val back = stage == "scanBack"
                Eyebrow(if (back) appStringResource(Res.string.docflow_step_back) else appStringResource(Res.string.docflow_step_front, appStringResource(if (isCardDocument(type)) Res.string.docflow_step_front_side else Res.string.docflow_step_page)))
                DocumentHeading(if (back) appStringResource(Res.string.docflow_back_heading) else appStringResource(Res.string.docflow_front_heading))
                DocumentCopy(if (back) appStringResource(Res.string.docflow_back_copy) else appStringResource(Res.string.docflow_front_copy))
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { DocumentNote(appStringResource(Res.string.docflow_edges_note)); Text(if (quality.glare) appStringResource(Res.string.docflow_glare) else if (quality.blurred) appStringResource(Res.string.docflow_blur) else appStringResource(Res.string.docflow_true_color), fontSize = 10.sp, color = if (quality.glare || quality.blurred) WarningColor else TextSecondary) }
                DocumentPrimary(if (busy) appStringResource(Res.string.docflow_capturing) else if (back) appStringResource(Res.string.docflow_capture_back) else appStringResource(if (isCardDocument(type)) Res.string.docflow_capture_front else Res.string.docflow_capture_page), quality.canCapture && !busy) { busy = true; error = null; request++ }
                if (back) DocumentLink(appStringResource(Res.string.docflow_keep_front)) { selected = 0; stage = "crop" }
                DocumentLink(appStringResource(Res.string.docflow_use_saved)) { importVisible = !importVisible }
                if (importVisible) DocumentImportActions(type, back || addingPage || retakeIndex != null, ::acceptImport, { error = it })
            }
            "crop" -> draft?.let { captures ->
                Eyebrow(appStringResource(Res.string.docflow_crop_eyebrow)); DocumentHeading(appStringResource(Res.string.docflow_crop_heading)); DocumentCopy(appStringResource(Res.string.docflow_crop_copy))
                val page = captures.pages[selected.coerceIn(captures.pages.indices)]
                var editing by remember { mutableStateOf(false) }; var crop by remember(page.previewId) { mutableStateOf(DocumentCrop()) }
                if (editing) CropEditor(page, crop) { crop = it } else DocumentImage(page, Modifier.heightIn(max = 410.dp).clip(RoundedCornerShape(if (isCardDocument(type)) 9.dp else 4.dp)))
                DocumentNote(appStringResource(Res.string.docflow_side_note, documentSideName(page.side)))
                DocumentThumbnails(type, captures.pages, selected) { selected = it; editing = false }
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { DocumentSecondary(appStringResource(Res.string.docflow_retake), Modifier.weight(1f), !busy) { scan(selected == 1 && isCardDocument(type), if (isCardDocument(type)) null else selected) }; DocumentSecondary(appStringResource(Res.string.docflow_rotate), Modifier.weight(1f), !busy) { changePage(page, 1, DocumentCrop()) } }
                DocumentSecondary(if (editing) appStringResource(Res.string.docflow_keep_crop) else appStringResource(Res.string.docflow_crop_action), Modifier.fillMaxWidth(), !busy) { if (!editing) editing = true else changePage(page, 0, crop) { editing = false } }
                if (isCardDocument(type) && captures.pages.size == 1) DocumentLink(appStringResource(Res.string.docflow_add_back)) { scan(true) }
                if (isCardDocument(type) && captures.pages.size == 2) DocumentLink(appStringResource(Res.string.docflow_swap_sides)) { setDraft(captures.copy(pages = captures.pages.reversed().mapIndexed { index, item -> item.copy(side = if (index == 0) "Front" else "Back") }, recognizedText = "")); selected = 0 }
                if (!isCardDocument(type) && captures.pages.size < documentPageLimit(type)) DocumentLink(appStringResource(Res.string.docflow_add_page)) { scan(false, append = true) }
                DocumentNote(appStringResource(Res.string.docflow_colors_note))
                DocumentPrimary(if (busy) appStringResource(Res.string.docflow_processing) else appStringResource(Res.string.docflow_use_photos), !busy && !editing) {
                    busy = true; scope.launch { try {
                        val accepted = captures.copy(pages = captures.pages.map { page -> if (page.acceptedId == null) adjustDocumentPage(page, 0, DocumentCrop()) else page })
                        setDraft(accepted); removeDocumentAssets(captures.assetIds() - accepted.assetIds() - protected)
                        setDraft(recognizeDocument(accepted)); stage = "review"
                    } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { error = failure.message ?: cropError } finally { busy = false } }
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
    DocumentNote(appStringResource(Res.string.docflow_crop_hint))
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
        if (attention) Text(appStringResource(Res.string.docflow_check_original), color = WarningColor, fontSize = 10.sp)
    }
}

@Composable private fun ColumnScope.DocumentConfirmation(state: VaultState, type: String, existing: VaultDocument?, draft: CapturedDocument, onSave: VaultSave, onDismiss: () -> Unit, selected: Int, onSelect: (Int) -> Unit, captureChanged: Boolean = true, initialValues: String? = null, onValues: (String) -> Unit = {}) {
    val suggestions = remember(draft.recognizedText) { DocumentTextReader.suggestions(type, draft.recognizedText) }
    var valuesJson by rememberSaveable(draft.pages.map { it.previewId }.joinToString()) { mutableStateOf(initialValues ?: Json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), (existing?.details ?: suggestions) + mapOf("number" to (existing?.number ?: suggestions["number"].orEmpty()), "holder" to (existing?.holder ?: suggestions["holder"].orEmpty()), "issued" to (existing?.issued ?: suggestions["issued"].orEmpty()), "expiry" to (existing?.expiry ?: suggestions["expiry"].orEmpty())))) }
    SideEffect { onValues(valuesJson) }
    val hadValues = remember { initialValues != null }
    val values = Json.decodeFromString<Map<String, String>>(valuesJson)
    var confirmed by rememberSaveable { mutableStateOf(false) }; var expanded by rememberSaveable { mutableStateOf(false) }; var viewedExtras by rememberSaveable { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val readFailedWarning = appStringResource(Res.string.docflow_read_failed)
    val confirmNumberWarning = appStringResource(Res.string.docflow_confirm_number)
    val confirmExpiryWarning = appStringResource(Res.string.docflow_confirm_expiry)
    val saveFailed = appStringResource(Res.string.docflow_save_failed)
    val dateFormat = appStringResource(Res.string.docflow_date_format)
    fun update(key: String, value: String) { valuesJson = Json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), values + (key to value.take(if (key in listOf("holder", "number")) 80 else 160))); confirmed = false }
    Eyebrow(appStringResource(Res.string.docflow_confirm_eyebrow)); DocumentHeading(appStringResource(Res.string.docflow_confirm_heading)); DocumentCopy(appStringResource(Res.string.docflow_confirm_copy))
    if (draft.pages.isNotEmpty()) DocumentThumbnails(type, draft.pages, selected, onSelect)
    DocumentWarning(if (draft.readingFailed) readFailedWarning else if (type == "Registration smart card") confirmNumberWarning else confirmExpiryWarning)
    if ((existing != null || hadValues) && suggestions.isNotEmpty()) DocumentLink(appStringResource(Res.string.docflow_use_scan)) { valuesJson = Json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), values + suggestions); confirmed = false }
    DocumentField(appStringResource(Res.string.docflow_field_number), values["number"].orEmpty(), { update("number", it) }, type == "Registration smart card" || values["number"].isNullOrBlank())
    DocumentField(appStringResource(Res.string.docflow_field_holder), values["holder"].orEmpty(), { update("holder", it) }, values["holder"].isNullOrBlank())
    DocumentField(appStringResource(Res.string.docflow_field_issued), values["issued"].orEmpty(), { update("issued", it) }, values["issued"].isNullOrBlank() && draft.recognizedText.contains("Issue", ignoreCase = true))
    if (type == "Registration smart card") DocumentField(appStringResource(Res.string.docflow_field_registration), values["registration"].orEmpty(), { update("registration", it) }, values["registration"].isNullOrBlank()) else DocumentField(appStringResource(Res.string.docflow_field_expiry), values["expiry"].orEmpty(), { update("expiry", it) }, true)
    DocumentLink(if (expanded) appStringResource(Res.string.docflow_extras_hide) else appStringResource(Res.string.docflow_extras_show)) { expanded = !expanded; if (expanded) { viewedExtras = true; confirmed = false } }
    if (expanded) {
        if (type == "Registration smart card") DocumentField(appStringResource(Res.string.docflow_field_expiry_optional), values["expiry"].orEmpty(), { update("expiry", it) })
        val extra = if (type == "Driving licence") listOf("bloodGroup" to appStringResource(Res.string.docflow_blood_group), "vehicleClass" to appStringResource(Res.string.docflow_vehicle_class), "birthDate" to appStringResource(Res.string.docflow_birth_date)) else (if (type == "Registration smart card") emptyList() else listOf("registration" to appStringResource(Res.string.docflow_vehicle_registration))) + listOf("chassis" to appStringResource(Res.string.docflow_chassis), "engine" to appStringResource(Res.string.docflow_engine))
        extra.forEach { (key, label) -> DocumentField(appStringResource(Res.string.docflow_field_optional, label), values[key].orEmpty(), { update(key, it) }) }
        DocumentNote(appStringResource(Res.string.docflow_english_only))
    }
    Row(Modifier.fillMaxWidth().toggleable(confirmed, role = Role.Checkbox, onValueChange = { confirmed = it }), verticalAlignment = Alignment.CenterVertically) { Checkbox(confirmed, null); Spacer(Modifier.width(8.dp)); Text(appStringResource(Res.string.docflow_confirm_checkbox), modifier = Modifier.weight(1f), fontSize = 12.sp, lineHeight = 18.sp) }
    error?.let { DocumentWarning(it) }
    DocumentPrimary(appStringResource(Res.string.docflow_save), confirmed && !values["number"].isNullOrBlank() && !values["holder"].isNullOrBlank()) {
        val document = (existing ?: VaultDocument(type, "", "", "")).copy(title = type, number = values["number"].orEmpty().trim(), holder = values["holder"].orEmpty().trim(), issued = values["issued"].orEmpty().trim(), expiry = if (type != "Registration smart card" || viewedExtras) values["expiry"].orEmpty().trim() else existing?.expiry.orEmpty(), pages = draft.pages, originalPdf = draft.originalPdf, pdfPageCount = draft.pdfPageCount, details = reviewedDocumentDetails(type, values, existing?.details.orEmpty(), viewedExtras), detailsConfirmed = true, extractionCandidates = if (captureChanged) suggestions else existing?.extractionCandidates.orEmpty(), capturedAt = if (captureChanged) Clock.System.now().toString() else existing?.capturedAt.orEmpty())
        runCatching { state.saveDocument(document) }.onSuccess { next -> onSave(next) { if (it) onDismiss() else error = saveFailed } }.onFailure { error = it.message ?: dateFormat }
    }
}

@Composable internal fun DocumentEditFlow(state: VaultState, existing: VaultDocument?, initialType: String, onDismiss: () -> Unit, onSave: VaultSave) {
    var type by rememberSaveable { mutableStateOf(existing?.title ?: initialType) }
    var selected by rememberSaveable { mutableStateOf(0) }
    DocumentScreen(appStringResource(Res.string.docflow_edit_title), onDismiss) {
        if (existing == null) {
            var menu by remember { mutableStateOf(false) }
            Box { DocumentSecondary(type, Modifier.fillMaxWidth()) { menu = true }; DropdownMenu(menu, { menu = false }) { documentTypes.forEach { kind -> DropdownMenuItem(text = { Text(documentDisplayName(kind)) }, onClick = { type = kind; menu = false }) } } }
            DocumentNote(appStringResource(Res.string.docflow_info_only_note))
        }
        key(type) { DocumentConfirmation(state, type, existing, CapturedDocument(existing?.pages.orEmpty(), existing?.originalPdf, existing?.pdfPageCount ?: 0), onSave, onDismiss, selected, { selected = it }, captureChanged = false) }
    }
}

@Composable internal fun OriginalDocumentDetail(document: VaultDocument, inspection: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onScan: () -> Unit, onDelete: () -> Unit, onInspectionList: () -> Unit, onInspect: () -> Unit = onInspectionList, onRecordFee: () -> Unit = {}) {
    var selected by rememberSaveable { mutableStateOf(if (document.pages.isEmpty() && document.frontPhoto == null && document.backPhoto != null) 1 else 0) }; var zoom by remember { mutableStateOf(false) }
    val legacyPhoto = if (selected == 0) document.frontPhoto else document.backPhoto
    DocumentScreen(if (inspection) appStringResource(Res.string.inspection_title) else document.title, onDismiss) {
        if (inspection) InspectionBrightness()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Eyebrow(if (inspection) appStringResource(Res.string.docflow_inspection_eyebrow) else appStringResource(Res.string.docflow_your_doc_eyebrow)); DocumentBadge(document.expiryLabel, warning = (document.daysRemaining ?: Int.MAX_VALUE) <= RENEWAL_WARNING_DAYS, good = (document.daysRemaining ?: 0) > RENEWAL_WARNING_DAYS) }
        DocumentHeading(document.title); DocumentCopy(if (inspection) appStringResource(Res.string.docflow_inspection_only) else appStringResource(Res.string.docflow_open_saved_copy))
        if (document.pages.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(11.dp)).border(1.dp, BorderColor, RoundedCornerShape(11.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { val count = if (isCardDocument(document.title)) 2 else document.pages.size; repeat(count) { index -> val page = document.pages.getOrNull(index); val sideLabel = page?.side?.let { documentSideName(it) } ?: appStringResource(Res.string.doc_back_photo_missing); TextButton(onClick = { selected = index }, enabled = page != null, shape = RoundedCornerShape(7.dp), colors = ButtonDefaults.textButtonColors(containerColor = if (selected == index) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (selected == index) MaterialTheme.colorScheme.onPrimary else TextSecondary), modifier = if (count <= 2) Modifier.weight(1f) else Modifier) { Text(sideLabel, fontSize = 12.sp) } } }
            val page = document.pages[selected.coerceIn(document.pages.indices)]
            DocumentImage(page, Modifier.clip(RoundedCornerShape(if (isCardDocument(document.title)) 9.dp else 4.dp)))
            DocumentNote(appStringResource(Res.string.docflow_original_side, documentSideName(page.side)))
            if (inspection) DocumentPrimary(appStringResource(Res.string.docflow_open_full)) { zoom = true } else DocumentSecondary(appStringResource(Res.string.docflow_open_full), Modifier.fillMaxWidth()) { zoom = true }
            if (!inspection) { OpenOriginalAssetButton(page.acceptedId ?: page.originalId, if (page.acceptedId != null) "image/jpeg" else page.mimeType, "${appStringResource(Res.string.docflow_open_selected)}"); if (page.acceptedId != null) OpenOriginalAssetButton(page.originalId, page.mimeType, "${appStringResource(Res.string.docflow_open_original)}"); document.originalPdf?.let { OpenOriginalAssetButton(it, "application/pdf", "${appStringResource(Res.string.docflow_open_pdf, document.pdfPageCount.toString())}") } }
        } else if (document.frontPhoto != null || document.backPhoto != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(appStringResource(Res.string.docflow_legacy_front) to document.frontPhoto, appStringResource(Res.string.docflow_legacy_back) to document.backPhoto).forEachIndexed { index, (label, photo) ->
                    DocumentSecondary(if (selected == index) "$label ✓" else label, Modifier.weight(1f), photo != null) { selected = index }
                }
            }
            legacyPhoto?.let { LegacyDocumentImage(it) }
            DocumentNote(appStringResource(Res.string.docflow_original_side, appStringResource(if (selected == 0) Res.string.docflow_legacy_front else Res.string.docflow_legacy_back)))
            DocumentSecondary(appStringResource(Res.string.docflow_open_full), Modifier.fillMaxWidth()) { zoom = true }
        } else Panel { DocumentCopy(appStringResource(Res.string.docflow_info_only_scan)) }
        if (!inspection) {
            val fields = listOf(appStringResource(Res.string.docflow_short_number) to document.number, appStringResource(Res.string.docflow_short_holder) to document.holder, appStringResource(Res.string.docflow_short_issued) to document.issued.ifBlank { appStringResource(Res.string.docflow_not_given) }, appStringResource(Res.string.docflow_short_expiry) to document.expiry.ifBlank { appStringResource(Res.string.docflow_not_given) }) + document.details.map { (key, value) -> (when (key) { "registration" -> appStringResource(Res.string.docflow_vehicle_registration); "chassis" -> appStringResource(Res.string.docflow_chassis); "engine" -> appStringResource(Res.string.docflow_engine); "bloodGroup" -> appStringResource(Res.string.docflow_blood_group); "vehicleClass" -> appStringResource(Res.string.docflow_vehicle_class); "birthDate" -> appStringResource(Res.string.docflow_birth_date); else -> key }) to value }
            fields.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) { row.forEach { (label, value) -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { DocumentNote(label); Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
            DocumentSecondary(appStringResource(Res.string.docflow_add_fee), Modifier.fillMaxWidth(), onClick = onRecordFee)
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { DocumentSecondary(appStringResource(Res.string.docflow_edit_details), Modifier.weight(1f), onClick = onEdit); DocumentSecondary(appStringResource(Res.string.docflow_rescan), Modifier.weight(1f), onClick = onScan) }
            DocumentLink(appStringResource(Res.string.docflow_open_inspection), onInspect); DocumentLink(appStringResource(Res.string.docflow_delete), onDelete)
        } else { DocumentNote(appStringResource(Res.string.docflow_saved_note)); DocumentSecondary(appStringResource(Res.string.docflow_all_docs), Modifier.fillMaxWidth(), onClick = onInspectionList); DocumentSecondary(appStringResource(Res.string.docflow_back_action), Modifier.fillMaxWidth(), onClick = onDismiss) }
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(appStringResource(Res.string.docflow_zoom_title), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f)); TextButton(onClick = { scale = if (scale >= 2f) 1f else scale + .5f; offset = Offset.Zero }) { Text(appStringResource(Res.string.docflow_zoom_in), color = Color.White) }; TextButton(onClick = onDismiss) { Text(appStringResource(Res.string.docflow_done), color = Color.White) } }
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(4.dp)).transformable(transform), contentAlignment = Alignment.Center) {
                val imageModifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                if (page != null) DocumentImage(page, imageModifier, accepted = true) else legacy?.let { LegacyDocumentImage(it, imageModifier) }
            }
            Text(appStringResource(Res.string.docflow_pinch_zoom), color = Color(0xFFD4D9DF), fontSize = 11.sp)
        }
    }
}
