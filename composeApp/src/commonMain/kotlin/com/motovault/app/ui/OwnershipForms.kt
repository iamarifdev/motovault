package com.motovault.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.serialization.json.Json
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.usecase.CalculateWearUseCase
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
expect fun AttachmentPicker(label: String, onSelected: (String) -> Unit, onError: (String) -> Unit)

@Composable
internal fun FuelDialog(state: VaultState, onDismiss: () -> Unit, onSave: VaultSave, onHistory: () -> Unit, existing: RideLog? = null) {
    var odometer by remember(existing?.id) { mutableStateOf((existing?.odometer ?: state.ride!!.odometer).toInt().toString()) }
    var liters by remember(existing?.id) { mutableStateOf(existing?.fuelLiters?.toString().orEmpty()) }
    var cost by remember(existing?.id) { mutableStateOf(existing?.cost?.toString().orEmpty()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: today().toString()) }
    var full by remember(existing?.id) { mutableStateOf(existing?.fullTank ?: false) }
    var location by remember(existing?.id) { mutableStateOf(existing?.location) }
    var locationLabel by remember(existing?.id) { mutableStateOf(existing?.locationLabel.orEmpty()) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val odo = odometer.toDoubleOrNull()
    val fuel = liters.toDoubleOrNull()
    val amount = cost.toDoubleOrNull()
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    Sheet(if (existing == null) "জ্বালানির হিসাব রাখুন" else "জ্বালানির রেকর্ড ঠিক করুন", onDismiss) {
        Eyebrow("জ্বালানি")
        Text("আংশিক জ্বালানিও পরের পূর্ণ ট্যাংকের সঙ্গে হিসাব হবে।", color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        Field("ওডোমিটার (কিমি)", odometer, { odometer = it }, true)
        Field("তারিখ (YYYY-MM-DD)", date, { value ->
            date = value
            if (runCatching { LocalDate.parse(value) }.getOrNull() != today()) { location = null; locationStatus = "পুরোনো রেকর্ডে বর্তমান GPS যোগ হবে না" }
        })
        Field("কত লিটার", liters, { liters = it }, true)
        Field("মোট দাম ৳", cost, { cost = it }, true)
        Row(Modifier.fillMaxWidth().clickable { full = !full }, verticalAlignment = Alignment.CenterVertically) { Checkbox(full, onCheckedChange = { full = it }); Text("ট্যাংক পূর্ণ করেছি", fontSize = 13.sp) }
        Field("পাম্প / এলাকা (ঐচ্ছিক)", locationLabel, { locationLabel = it.take(100) })
        LocationCaptureButton(requestKey = if (state.autoLocation && day == today()) 1 else 0, enabled = day == today()) { captured ->
            if (day != today()) {
                location = null
                locationStatus = "পুরোনো রেকর্ডে বর্তমান GPS যোগ হবে না"
            } else if (captured != null) {
                location = captured
                locationStatus = "আপনার বর্তমান জিপিএস লোকেশন যোগ হয়েছে"
            }
            else locationStatus = "লোকেশন পাওয়া যায়নি, জায়গার নাম লিখতে পারেন"
        }
        if (location != null) TextButton(onClick = { location = null; locationStatus = "GPS লোকেশন সরানো হয়েছে" }) { Text("GPS লোকেশন সরান") }
        location?.let { Text("GPS: ±${it.accuracyMeters.toInt()} মিটার · ${it.latitude}, ${it.longitude}", color = TextSecondary, fontSize = 11.sp) }
        locationStatus?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary(if (existing == null) "জ্বালানির হিসাব সেভ করুন" else "পরিবর্তন সেভ করুন", "check", enabled = odo != null && fuel != null && amount != null && day != null) {
            runCatching { if (existing == null) state.logFuel(odo!!, fuel!!, amount!!, day!!, full, location, locationLabel) else state.replaceFuelLog(existing.id, odo!!, fuel!!, amount!!, day!!, full, location, locationLabel) }.onSuccess {
                onSave(it) { saved -> if (saved) onDismiss() else error = "সেভ করা যায়নি। আগের রেকর্ড অপরিবর্তিত আছে।" }
            }.onFailure { error = it.message }
        }
        TextButton(onClick = onHistory) { Text("জ্বালানির ইতিহাস দেখুন") }
    }
}

@Composable
internal fun ComponentsList(state: VaultState, onDismiss: () -> Unit, onSelect: (ComponentRule) -> Unit, onAdd: () -> Unit) {
    Sheet("সময়মতো যত্ন নিন।", onDismiss) {
        Text("নিজের সার্ভিস সূচি। দূরত্ব বা সময়, যেটি আগে হয়।", color = TextSecondary, fontSize = 13.sp)
        state.rules.forEach { rule ->
            val health = CalculateWearUseCase().execute(rule, state.ride!!.odometer, today().toEpochDays().toLong())
            Panel(onClick = { onSelect(rule) }) {
                Text(componentLabel(rule), fontWeight = FontWeight.Bold)
                Text("${if (rule.intervalKm > 0) "${rule.intervalKm.toInt()} কিমি" else "দূরত্বের সীমা নেই"} / ${if (rule.intervalDays > 0) "${rule.intervalDays} দিন" else "সময়ের সীমা নেই"}", color = TextSecondary, fontSize = 12.sp)
                Text("শেষ সার্ভিস: ${LocalDate.fromEpochDays(rule.lastServicedEpochDays.toInt())} · ${rule.lastServicedOdometerKm.toInt()} কিমি", color = TextSecondary, fontSize = 11.sp)
                Text(if (health.wearPercentage >= 100) "সময় পেরিয়েছে ↗" else if (health.wearPercentage >= 75) "শিগগির সার্ভিস করুন ↗" else "ঠিক আছে ↗", color = if (health.wearPercentage >= 75) WarningColor else HealthyColor, fontSize = 11.sp)
            }
        }
        Primary("যন্ত্রাংশ যোগ করুন", "add", onClick = onAdd)
    }
}

@Composable
internal fun ServiceDialog(state: VaultState, initial: ComponentRule?, onDismiss: () -> Unit, onSave: VaultSave, onEdit: (ComponentRule) -> Unit, onHistory: () -> Unit, existing: RideLog? = null) {
    val initialItem = remember(initial?.id, existing?.id) {
        existing?.serviceLines?.firstOrNull()?.catalogueId?.let(BangladeshServiceCatalogue::find)
            ?: initial?.let { BangladeshServiceCatalogue.itemForScheduleName(it.name) }
            ?: BangladeshServiceCatalogue.items.first { it.id == "general_service" }
    }
    val initialScheduleId = remember(initial?.id, existing?.id) { existing?.serviceLines?.firstOrNull()?.scheduleId.orEmpty().ifBlank { initial?.id?.takeIf { BangladeshServiceCatalogue.scheduleNameMatchesItem(initial.name, initialItem.id) }.orEmpty() } }
    val initialItemId = existing?.serviceLines?.firstOrNull()?.let { it.catalogueId ?: "custom_item" } ?: initialItem.id
    var itemId by remember(initial?.id, existing?.id) { mutableStateOf(initialItemId) }
    var itemExpanded by remember { mutableStateOf(false) }
    var customName by remember(existing?.id) { mutableStateOf(existing?.serviceLines?.firstOrNull { it.catalogueId == null }?.customName.orEmpty()) }
    var action by remember(initial?.id, existing?.id) { mutableStateOf(existing?.serviceLines?.firstOrNull()?.action ?: if (initialItem.id == "chain_sprocket") "service" else initialItem.action) }
    var actionExpanded by remember { mutableStateOf(false) }
    var scheduleId by remember(initial?.id, existing?.id) { mutableStateOf(initialScheduleId) }
    var scheduleExpanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var oilType by remember { mutableStateOf("mineral") }
    var oilTypeExpanded by remember { mutableStateOf(false) }
    var oilGrade by remember { mutableStateOf("") }
    var api by remember { mutableStateOf("") }
    var jaso by remember { mutableStateOf("") }
    var product by remember { mutableStateOf("") }
    var purchaseBasis by remember { mutableStateOf("bottle") }
    var purchaseBasisExpanded by remember { mutableStateOf(false) }
    var packMl by remember { mutableStateOf("") }
    var boughtQuantity by remember { mutableStateOf("1") }
    var usedMl by remember { mutableStateOf("") }
    var lines by remember(existing?.id) { mutableStateOf(existing?.serviceLines ?: emptyList()) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }
    var odometer by remember(existing?.id) { mutableStateOf((existing?.odometer ?: state.ride!!.odometer).toInt().toString()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: today().toString()) }
    var labour by remember(existing?.id) { mutableStateOf(existing?.serviceLabourPoisha?.let { formatPoisha(it) }.orEmpty()) }
    var miscellaneous by remember(existing?.id) { mutableStateOf(existing?.serviceMiscellaneousPoisha?.let { formatPoisha(it) }.orEmpty()) }
    var discount by remember(existing?.id) { mutableStateOf(existing?.serviceDiscountPoisha?.let { formatPoisha(it) }.orEmpty()) }
    var workshop by remember(existing?.id) { mutableStateOf(existing?.workshop.orEmpty()) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var receipt by remember(existing?.id) { mutableStateOf(existing?.receiptBase64) }
    var smartText by remember { mutableStateOf("") }
    var smartDraft by remember { mutableStateOf<SmartServiceDraft?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var location by remember(existing?.id) { mutableStateOf(existing?.location) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    val odo = odometer.toDoubleOrNull()
    fun poisha(value: String): Int? {
        val raw = value.trim().takeIf { it.isNotEmpty() } ?: return null
        val parts = raw.split('.')
        if (parts.size > 2 || parts[0].isEmpty() || parts.any { p -> p.any { !it.isDigit() } }) return null
        val whole = parts[0].toLongOrNull() ?: return null
        val fraction = parts.getOrElse(1) { "" }.padEnd(3, '0')
        val cents = fraction.take(2).toIntOrNull() ?: 0
        val rounded = cents + if (fraction.getOrNull(2)?.digitToIntOrNull()?.let { it >= 5 } == true) 1 else 0
        val amount = whole * 100 + rounded
        return amount.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()
    }
    val labourPoisha = poisha(labour)
    val miscPoisha = poisha(miscellaneous)
    val discountPoisha = poisha(discount)
    val unitPoisha = poisha(unitPrice)
    val quantityValue = quantity.toDoubleOrNull()
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    val selectedItem = BangladeshServiceCatalogue.find(itemId) ?: BangladeshServiceCatalogue.items.first()
    val matchingSchedules = state.rules.filter { BangladeshServiceCatalogue.scheduleNameMatchesItem(it.name, itemId) }
    fun optionalIntIsValid(value: String) = value.isBlank() || value.toIntOrNull()?.let { it > 0 } == true
    fun optionalDoubleIsValid(value: String) = value.isBlank() || value.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    val oilNumbersValid = optionalIntIsValid(packMl) && optionalDoubleIsValid(boughtQuantity) && optionalIntIsValid(usedMl)
    val ancillaryAmountsValid = listOf(labour to labourPoisha, miscellaneous to miscPoisha, discount to discountPoisha)
        .all { (raw, parsed) -> raw.isBlank() || parsed != null }
    val currentOil = if (itemId == "engine_oil") EngineOilMetadata(
        oilType, oilGrade.trim(), api.trim(), jaso.trim(), product.trim(), purchaseBasis,
        packMl.toIntOrNull(), boughtQuantity.toDoubleOrNull(), usedMl.toIntOrNull()
    ) else null
    val currentDraftLine = ServiceLineItem(
        catalogueId = itemId.takeUnless { it == "custom_item" }, customName = if (itemId == "custom_item") customName.trim() else "",
        action = action, quantity = quantityValue ?: 1.0,
        unitBn = if (itemId == "custom_item") "টি" else if (itemId == "engine_oil") if (purchaseBasis == "bottle") "বোতল" else "লিটার" else selectedItem.unitBn,
        unitPricePoisha = unitPoisha,
        scheduleId = scheduleId.takeIf { it.isNotBlank() && action in setOf("replace", "service") },
        resetsScheduleBaseline = scheduleId.isNotBlank() && action in setOf("replace", "service"), oil = currentOil
    )
    val computedTotal = serviceTotalPoisha(lines, labourPoisha, miscPoisha, discountPoisha)
    Sheet(if (existing == null) "এক ভিজিটে সব কাজ লিখুন" else "সার্ভিস রেকর্ড ঠিক করুন", onDismiss) {
        Eyebrow(if (existing == null) "সার্ভিস রেকর্ড" else "রেকর্ড সংশোধন")
        Text("একটি ভিজিটে একাধিক কাজ ও পার্টস যোগ করুন। দাম না জানলে ফাঁকা রাখুন, ফ্রি হলে ০ লিখুন।", color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        OutlinedTextField(smartText, { smartText = it.take(1000); smartDraft = null }, label = { Text("দ্রুত লিখুন") }, placeholder = { Text("Engine oil 1200 taka\nOil filter 350 taka") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))
        TextButton(onClick = { smartDraft = SmartServiceDraftParser.parse(smartText) }, enabled = smartText.isNotBlank()) { Text("লেখা থেকে খসড়া সাজান") }
        smartDraft?.let { draft ->
            Text("যা বুঝেছি, দেখে তালিকায় যোগ করুন", color = TextSecondary, fontSize = 11.sp)
            draft.suggestions.forEach { suggestion ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${suggestion.line.displayNameBn} · ${suggestion.line.unitPricePoisha?.let { "৳${formatPoisha(it)}" } ?: "দাম অজানা"}", Modifier.weight(1f), fontSize = 12.sp)
                    TextButton(onClick = { lines = lines + suggestion.line; smartDraft = draft.copy(suggestions = draft.suggestions - suggestion) }) { Text("যোগ") }
                }
            }
            draft.unmatched.forEach { raw -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("চেনা যায়নি: $raw", Modifier.weight(1f), color = WarningColor, fontSize = 11.sp)
                TextButton(onClick = { itemId = "custom_item"; customName = raw.take(60); unitPrice = ""; quantity = "1" }) { Text("কাস্টম আইটেম যোগ") }
            } }
        }
        Box {
            OutlinedButton(onClick = { itemExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(if (itemId == "custom_item") "নিজের আইটেম" else selectedItem.labelBn) }
            DropdownMenu(itemExpanded, onDismissRequest = { itemExpanded = false }) {
                BangladeshServiceCatalogue.items.forEach { item -> DropdownMenuItem(text = { Text(item.labelBn) }, onClick = { itemId = item.id; customName = ""; action = item.action; scheduleId = ""; itemExpanded = false }) }
                DropdownMenuItem(text = { Text("নিজের পার্টস / কাজ") }, onClick = { itemId = "custom_item"; action = "replace"; scheduleId = ""; itemExpanded = false })
            }
        }
        if (itemId == "custom_item") Field("আইটেমের নাম", customName, { customName = it.take(60) })
        Box {
            OutlinedButton(onClick = { actionExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(when (action) { "replace" -> "বদলানো"; "service" -> "সার্ভিস করা"; "inspect" -> "পরীক্ষা"; "clean" -> "পরিষ্কার"; else -> "অ্যাডজাস্ট" }) }
            DropdownMenu(actionExpanded, onDismissRequest = { actionExpanded = false }) {
                listOf("replace" to "বদলানো", "service" to "সার্ভিস করা", "inspect" to "পরীক্ষা", "clean" to "পরিষ্কার", "adjust" to "অ্যাডজাস্ট").forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { action = key; if (key !in setOf("replace", "service")) scheduleId = ""; actionExpanded = false }) }
            }
        }
        if (action in setOf("replace", "service")) Box {
            OutlinedButton(onClick = { scheduleExpanded = true }, enabled = matchingSchedules.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text(if (scheduleId.isBlank()) "মিল থাকা রিমাইন্ডার নেই" else state.rules.firstOrNull { it.id == scheduleId }?.name ?: "রিমাইন্ডার বাছাই করুন") }
            DropdownMenu(scheduleExpanded, onDismissRequest = { scheduleExpanded = false }) {
                DropdownMenuItem(text = { Text("কোনো রিমাইন্ডার নয়") }, onClick = { scheduleId = ""; scheduleExpanded = false })
                matchingSchedules.forEach { rule -> DropdownMenuItem(text = { Text(rule.name) }, onClick = { scheduleId = rule.id; scheduleExpanded = false }) }
            }
        }
        Field("পরিমাণ", quantity, { quantity = it }, true)
        Field("একক দাম ৳", unitPrice, { unitPrice = it }, true)
        if (itemId == "engine_oil") {
            Text("ইঞ্জিন অয়েলের তথ্য", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Box { OutlinedButton(onClick = { oilTypeExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(when (oilType) { "semi_synthetic" -> "সেমি-সিনথেটিক"; "full_synthetic" -> "ফুল সিনথেটিক"; else -> "মিনারেল" }) }; DropdownMenu(oilTypeExpanded, onDismissRequest = { oilTypeExpanded = false }) {
                listOf("mineral" to "মিনারেল", "semi_synthetic" to "সেমি-সিনথেটিক", "full_synthetic" to "ফুল সিনথেটিক").forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { oilType = key; oilTypeExpanded = false }) }
            } }
            Field("SAE গ্রেড", oilGrade, { oilGrade = it.take(24) })
            Field("API", api, { api = it.take(24) })
            Field("JASO", jaso, { jaso = it.take(24) })
            Field("ব্র্যান্ড / পণ্য", product, { product = it.take(60) })
            Box { OutlinedButton(onClick = { purchaseBasisExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(if (purchaseBasis == "loose") "খোলা তেল" else "বোতল") }; DropdownMenu(purchaseBasisExpanded, onDismissRequest = { purchaseBasisExpanded = false }) {
                DropdownMenuItem(text = { Text("বোতল") }, onClick = { purchaseBasis = "bottle"; purchaseBasisExpanded = false })
                DropdownMenuItem(text = { Text("খোলা") }, onClick = { purchaseBasis = "loose"; purchaseBasisExpanded = false })
            } }
            Field("প্যাক মিলি", packMl, { packMl = it }, true)
            Field("কেনা পরিমাণ", boughtQuantity, { boughtQuantity = it }, true)
            Field("ব্যবহৃত মিলি", usedMl, { usedMl = it }, true)
            if (!oilNumbersValid) Text("পরিমাণে শুধু সঠিক সংখ্যা লিখুন", color = WarningColor, fontSize = 12.sp)
        }
        TextButton(onClick = {
            if (itemId == "custom_item" && customName.isBlank()) error = "আইটেমের নাম লিখুন"
            else if (quantityValue == null || !quantityValue.isFinite() || quantityValue <= 0) error = "পরিমাণ ঠিক করুন"
            else if (unitPrice.isNotBlank() && unitPoisha == null) error = "দাম ঠিক করুন"
            else if (itemId == "engine_oil" && !oilNumbersValid) error = "অয়েলের পরিমাণ ঠিক করুন"
            else { val index = editingLineIndex; lines = if (index == null) lines + currentDraftLine else lines.mapIndexed { i, line -> if (i == index) currentDraftLine else line }; editingLineIndex = null; unitPrice = ""; quantity = "1"; scheduleId = ""; error = null }
        }) { Text(if (editingLineIndex == null) "＋ তালিকায় যোগ করুন" else "Փ পরিবর্তন যোগ করুন") }
        lines.forEachIndexed { index, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${line.displayNameBn} · ${line.quantity} ${line.unitBn} · ${line.lineTotalPoisha?.let { "৳${formatPoisha(it)}" } ?: "দাম জানা নেই"}", modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = {
                    editingLineIndex = index; itemId = line.catalogueId ?: "custom_item"; customName = line.customName; action = line.action
                    quantity = line.quantity.toString(); unitPrice = line.unitPricePoisha?.let { formatPoisha(it) }.orEmpty(); scheduleId = line.scheduleId.orEmpty()
                    line.oil?.let { oil -> oilType = oil.baseType; oilGrade = oil.saeGrade; api = oil.apiStandard; jaso = oil.jasoStandard; product = oil.productName; purchaseBasis = oil.purchaseBasis; packMl = oil.packVolumeMl?.toString().orEmpty(); boughtQuantity = oil.boughtQuantity?.toString() ?: "1"; usedMl = oil.usedQuantityMl?.toString().orEmpty() }
                }) { Text("বদলান") }
                TextButton(onClick = { lines = lines.filterIndexed { i, _ -> i != index } }) { Text("বাদ") }
            }
        }
        Field("ওডোমিটার (কিমি)", odometer, { odometer = it }, true)
    Field("সার্ভিসের তারিখ (YYYY-MM-DD)", date, { value ->
        date = value
        if (runCatching { LocalDate.parse(value) }.getOrNull() != today()) { location = null; locationStatus = "পুরোনো রেকর্ডে বর্তমান GPS যোগ হবে না" }
    })
        Field("শ্রম ৳", labour, { labour = it }, true)
        Field("অন্যান্য ৳", miscellaneous, { miscellaneous = it }, true)
        Field("ছাড় ৳", discount, { discount = it }, true)
        if (!ancillaryAmountsValid) Text("দাম সংখ্যায় লিখুন, অজানা হলে ফাঁকা রাখুন", color = WarningColor, fontSize = 12.sp)
        Text("মোট: ${computedTotal?.let { "৳${formatPoisha(it)}" } ?: "দাম সম্পূর্ণ জানা নেই"}", fontWeight = FontWeight.Bold)
        Field("ওয়ার্কশপ / এলাকা (ঐচ্ছিক)", workshop, { workshop = it.take(80) })
        LocationCaptureButton(requestKey = if (state.autoLocation && day == today()) 1 else 0, enabled = day == today()) { captured ->
            if (day != today()) {
                location = null
                locationStatus = "পুরোনো রেকর্ডে বর্তমান GPS যোগ হবে না"
            } else if (captured != null) {
                location = captured
                locationStatus = "আপনার বর্তমান জিপিএস লোকেশন যোগ হয়েছে"
            }
            else locationStatus = "জিপিএস পাওয়া যায়নি, ওয়ার্কশপ / এলাকা লিখে রেকর্ড সেভ করুন"
        }
        if (location != null) TextButton(onClick = { location = null; locationStatus = "GPS লোকেশন সরানো হয়েছে" }) { Text("GPS লোকেশন সরান") }
        location?.let { Text("GPS: ±${it.accuracyMeters.toInt()} মিটার · ${it.latitude}, ${it.longitude}", color = TextSecondary, fontSize = 11.sp) }
        locationStatus?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
        OutlinedTextField(notes, { notes = it.take(500) }, label = { Text("নোট") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))
        Text("রসিদ সংযুক্তি ঐচ্ছিক", color = TextSecondary, fontSize = 11.sp)
        AttachmentPicker(if (receipt == null) "রসিদের ছবি যোগ করুন" else "রসিদের ছবি বদলান", onSelected = { receipt = it }, onError = { error = it })
        receipt?.let { Attachment(it, "সার্ভিসের রসিদ") }
        if (receipt != null) TextButton(onClick = { receipt = null }) { Text("রসিদ সরান") }
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary(if (existing == null) "সার্ভিস সেভ করুন" else "পরিবর্তন সেভ করুন", "check", enabled = lines.isNotEmpty() && odo != null && day != null && ancillaryAmountsValid) {
            runCatching { if (existing == null) state.logServiceVisit(lines, odo!!, day!!, labourPoisha, miscPoisha, discountPoisha, workshop, notes, location, receipt) else state.replaceServiceVisit(existing.id, lines, odo!!, day!!, labourPoisha, miscPoisha, discountPoisha, workshop, notes, location, receipt) }.onSuccess {
                onSave(it) { saved -> if (saved) onDismiss() else error = "সেভ করা যায়নি। আগের রেকর্ড অপরিবর্তিত আছে।" }
            }.onFailure { error = it.message }
        }
        initial?.let { rule -> TextButton(onClick = { onEdit(rule) }) { Text("এই সূচি বদলান") } }
        TextButton(onClick = onHistory) { Text("সার্ভিসের ইতিহাস") }
    }
}

private fun formatPoisha(amount: Int): String = "${amount / 100}.${(amount % 100).toString().padStart(2, '0')}"

@Composable
internal fun DocumentFeeDialog(state: VaultState, document: VaultDocument, onDismiss: () -> Unit, onSave: VaultSave) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today().toString()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val poisha = amount.trim().replace(",", "").toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 && it <= Int.MAX_VALUE / 100.0 }?.let { (it * 100).toLong().takeIf { value -> value <= Int.MAX_VALUE }?.toInt() }
    val previous = state.logs.filter { it.isDocumentFee && it.title == "${document.title} renewal" }.sortedByDescending { it.date }
    Sheet("নবায়ন ফি লিখুন", onDismiss) {
        Eyebrow(document.title)
        Text("প্রতিটি পেমেন্ট আলাদা রেকর্ড হবে। নথির তথ্য সম্পাদনা করলে আগের খরচ বদলাবে না।", color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        previous.forEach { Text("${it.date} · ৳${oneDecimal(it.cost)}", color = TextSecondary, fontSize = 12.sp) }
        Field("পরিমাণ ৳", amount, { amount = it }, true)
        Field("পেমেন্টের তারিখ", date, { date = it })
        Field("নোট (ঐচ্ছিক)", notes, { notes = it.take(160) })
        Text("এই খরচ ব্যক্তিগত। সার্ভিস পাসপোর্টে যাবে না।", color = TextSecondary, fontSize = 11.sp)
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary("ফি সেভ করুন", "check", enabled = poisha != null && parsedDate != null) {
            runCatching { state.logDocumentFee(document.title, poisha!!, parsedDate!!, notes) }.onSuccess {
                onSave(it) { saved -> if (saved) onDismiss() else error = "সেভ করা যায়নি। আগের রেকর্ড অপরিবর্তিত আছে।" }
            }.onFailure { error = it.message }
        }
    }
}

@Composable
internal fun ScheduleDialog(state: VaultState, existing: ComponentRule?, onDismiss: () -> Unit, onSave: VaultSave) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var km by remember { mutableStateOf(existing?.intervalKm?.toInt()?.toString() ?: "0") }
    var days by remember { mutableStateOf(existing?.intervalDays?.toString() ?: "0") }
    var odometer by remember { mutableStateOf((existing?.lastServicedOdometerKm ?: state.ride!!.odometer).toInt().toString()) }
    var date by remember { mutableStateOf(existing?.lastServicedEpochDays?.let { LocalDate.fromEpochDays(it.toInt()).toString() } ?: today().toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val distance = km.toDoubleOrNull()
    val intervalDays = days.toIntOrNull()
    val odo = odometer.toDoubleOrNull()
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    Sheet(if (existing == null) "যন্ত্রাংশ যোগ করুন।" else "সার্ভিসের ব্যবধান বদলান।", onDismiss) {
        Eyebrow("নিজের সার্ভিস সূচি")
        Text("ম্যানুয়াল বা মেকানিকের পরামর্শ মেনে চলুন। দূরত্ব বা সময়ের অন্তত একটি সীমা দিন।", color = TextSecondary, fontSize = 13.sp)
        Field("যন্ত্রাংশের নাম", name, { name = it.take(40) })
        Field("কত কিমি পরপর (বন্ধ রাখতে 0)", km, { km = it }, true)
        Field("কত দিন পরপর (বন্ধ রাখতে 0)", days, { days = it }, true)
        Field("শেষ সার্ভিসের সময় ওডোমিটার", odometer, { odometer = it }, true)
        Field("শেষ সার্ভিসের তারিখ (YYYY-MM-DD)", date, { date = it })
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary("সূচি সেভ করুন", "check", enabled = distance != null && intervalDays != null && odo != null && day != null && name.isNotBlank()) {
            runCatching { state.saveSchedule(ComponentRule(existing?.id ?: recordId(), "primary", name.trim(), "নিজের সার্ভিস সূচি", distance!!, intervalDays!!, odo!!, day!!.toEpochDays().toLong())) }.onSuccess {
                onSave(it) { saved -> if (saved) onDismiss() else error = "সেভ করা যায়নি। আগের রেকর্ড অপরিবর্তিত আছে।" }
            }.onFailure { error = it.message }
        }
    }
}

@Composable
internal fun History(state: VaultState, filter: String, onAppearance: () -> Unit, onFilter: (String) -> Unit, onDelete: (RideLog) -> Unit, onEdit: (RideLog) -> Unit, onFuel: () -> Unit, onService: () -> Unit) {
    var receiptToView by remember { mutableStateOf<RideLog?>(null) }
    Top("রাইডের ইতিহাস", "এই ডিভাইসে সংরক্ষিত", onAppearance)
    Heading("আপনার বাইকের সব রেকর্ড")
    Text("${state.ride!!.displayName}-এর সার্ভিস ও জ্বালানির হিসাব।", color = TextSecondary, fontSize = 13.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val knownServiceSpend = state.logs.filter { !it.isFuel && !it.isDocumentFee }.sumOf { log ->
            if (log.serviceLines.isNotEmpty()) (log.serviceTotalPoisha?.div(100.0) ?: 0.0) else log.cost
        }
        val unknownServiceCosts = state.logs.count { !it.isFuel && !it.isDocumentFee && it.serviceLines.isNotEmpty() && it.serviceTotalPoisha == null }
        val serviceLabel = "জানা সার্ভিস খরচ" + (if (unknownServiceCosts > 0) " · ${unknownServiceCosts}টির দাম অজানা" else "")
        val totals = listOf(serviceLabel to "৳${oneDecimal(knownServiceSpend)}", "জ্বালানি খরচ" to "৳${oneDecimal(state.logs.filter { it.isFuel }.sumOf { it.cost })}", "গড় মাইলেজ" to (state.economy?.let { oneDecimal(it) } ?: "--"))
        totals.forEach { (label, value) -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(label, color = TextSecondary, fontSize = 10.sp) } }
    }
    val documentSpend = state.logs.filter { it.isDocumentFee }.sumOf { it.cost }
    if (state.logs.any { it.isDocumentFee }) Text("নথি নবায়নের খরচ: ৳${oneDecimal(documentSpend)}", color = TextSecondary, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("all", "service", "fuel", "documents").forEach { kind -> FilterChip(filter == kind, onClick = { onFilter(kind) }, label = { Text(when (kind) { "all" -> "সব"; "service" -> "সার্ভিস"; "fuel" -> "জ্বালানি"; else -> "নথি" }) }) } }
    PassportExportButton(state)
    val logs = state.logs.filter { log -> filter == "all" || when (filter) { "fuel" -> log.isFuel; "documents" -> log.isDocumentFee; else -> !log.isFuel && !log.isDocumentFee } }.sortedWith(compareByDescending<RideLog> { it.date }.thenByDescending { it.odometer })
    if (logs.isEmpty()) Panel { Glyph("history", TextSecondary, Modifier.size(36.dp)); Text("এখান থেকেই হিসাব শুরু করুন", fontWeight = FontWeight.Bold); Text("সার্ভিস বা জ্বালানি যোগ করলে আপনার বাইকের ইতিহাস এখানে দেখা যাবে।", color = TextSecondary, lineHeight = 20.sp) }
    logs.forEach { log -> Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph(if (log.isFuel) "fuel" else "service", MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (log.isFuel) "${log.fuelLiters ?: "?"} L · ${if (log.fullTank) "ট্যাংক পূর্ণ" else "আংশিক জ্বালানি"}" else log.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${log.date} · ${grouped(log.odometer.toInt())} km · ${if (log.serviceLines.isNotEmpty()) log.serviceTotalPoisha?.let { "৳${formatPoisha(it)}" } ?: "দাম জানা নেই" else "৳${log.cost}"}", color = TextSecondary, fontSize = 11.sp)
                log.serviceLines.forEach { line ->
                    val actionBn = when (line.action) { "replace" -> "বদল"; "service" -> "সার্ভিস"; "inspect" -> "পরীক্ষা"; "clean" -> "পরিষ্কার"; else -> "অ্যাডজাস্ট" }
                    Text("  ${line.displayNameBn} · $actionBn · ${line.quantity} ${line.unitBn} · ${line.lineTotalPoisha?.let { "৳${formatPoisha(it)}" } ?: "দাম জানা নেই"}", color = TextSecondary, fontSize = 11.sp)
                    line.oil?.let { oil ->
                        val oilTypeBn = when (oil.baseType) { "semi_synthetic" -> "সেমি-সিনথেটিক"; "full_synthetic" -> "ফুল সিনথেটিক"; else -> "মিনারেল" }
                        val details = listOfNotNull(oilTypeBn, oil.saeGrade.takeIf(String::isNotBlank), oil.apiStandard.takeIf(String::isNotBlank)?.let { "API $it" }, oil.jasoStandard.takeIf(String::isNotBlank)?.let { "JASO $it" }, oil.productName.takeIf(String::isNotBlank), oil.usedQuantityMl?.let { "ব্যবহার ${it} মিলি" })
                        if (details.isNotEmpty()) Text("    ${details.joinToString(" · ")}", color = TextSecondary, fontSize = 10.sp)
                    }
                }
                if (log.locationLabel.isNotBlank()) Text(log.locationLabel, color = TextSecondary, fontSize = 11.sp)
                log.location?.let { Text("জিপিএস · GPS ±${it.accuracyMeters.toInt()} মিটার", color = TextSecondary, fontSize = 10.sp) }
                if (log.receiptBase64 != null) TextButton(onClick = { receiptToView = log }, contentPadding = PaddingValues(0.dp)) { Text("রসিদ দেখুন", fontSize = 11.sp) }
                if (log.workshop.isNotBlank()) Text(log.workshop, color = TextSecondary, fontSize = 12.sp)
                if (log.notes.isNotBlank()) Text(log.notes, color = TextSecondary, fontSize = 12.sp)
            }
            if (log.isFuel || (!log.isDocumentFee && log.serviceLines.isNotEmpty())) TextButton(onClick = { onEdit(log) }, contentPadding = PaddingValues(0.dp)) { Text("সংশোধন", fontSize = 10.sp) }
            TextButton(onClick = { onDelete(log) }, contentPadding = PaddingValues(0.dp)) { Text("মুছুন", color = TextSecondary, fontSize = 10.sp) }
        }
    } }
    receiptToView?.let { record -> Sheet("রসিদ", onDismiss = { receiptToView = null }) {
        record.receiptBase64?.let { Attachment(it, "${record.date} service receipt") }
        Primary("বন্ধ করুন", "close", onClick = { receiptToView = null })
    } }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Primary("সার্ভিস যোগ করুন", "service", Modifier.weight(1f), onClick = onService); Primary("জ্বালানি যোগ করুন", "fuel", Modifier.weight(1f), onClick = onFuel) }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
internal fun Attachment(value: String, description: String) {
    val bitmap = remember(value) { runCatching { Base64.decode(value).decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, description, Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit)
    else Text("ছবিটি দেখানো যায়নি।", color = WarningColor, fontSize = 12.sp)
}
