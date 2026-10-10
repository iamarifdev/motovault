package com.motovault.app.ui.redesign

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
internal fun FuelDialog(state: VaultState, onDismiss: () -> Unit, onSave: (VaultState) -> Boolean, onHistory: () -> Unit) {
    var odometer by remember { mutableStateOf(state.ride!!.odometer.toInt().toString()) }
    var liters by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today().toString()) }
    var full by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val odo = odometer.toDoubleOrNull()
    val fuel = liters.toDoubleOrNull()
    val amount = cost.toDoubleOrNull()
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    Sheet("Keep every fill in view.", onDismiss) {
        Eyebrow("Fuel log")
        Text("Partial fills count toward the next full tank. Two full-tank readings establish a complete cycle.", color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        Field("Odometer (km)", odometer, { odometer = it }, true)
        Field("Date (YYYY-MM-DD)", date, { date = it })
        Field("Litres added", liters, { liters = it }, true)
        Field("Total cost (৳)", cost, { cost = it }, true)
        Row(Modifier.fillMaxWidth().clickable { full = !full }, verticalAlignment = Alignment.CenterVertically) { Checkbox(full, onCheckedChange = { full = it }); Text("Filled to a full tank", fontSize = 13.sp) }
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary("Save fill-up", "check", enabled = odo != null && fuel != null && amount != null && day != null) {
            runCatching { state.logFuel(odo!!, fuel!!, amount!!, day!!, full) }.onSuccess {
                if (onSave(it)) onDismiss() else error = "Could not save. Your previous records are unchanged."
            }.onFailure { error = it.message }
        }
        TextButton(onClick = onHistory) { Text("View fuel history ↗") }
    }
}

@Composable
internal fun ComponentsList(state: VaultState, onDismiss: () -> Unit, onSelect: (ComponentRule) -> Unit, onAdd: () -> Unit) {
    Sheet("Care, at the right time.", onDismiss) {
        Text("Owner-defined schedules. Whichever comes first: distance or time.", color = TextSecondary, fontSize = 13.sp)
        state.rules.forEach { rule ->
            val health = CalculateWearUseCase().execute(rule, state.ride!!.odometer, today().toEpochDays().toLong())
            Panel(onClick = { onSelect(rule) }) {
                Text(rule.name, fontWeight = FontWeight.Bold)
                Text("${if (rule.intervalKm > 0) "${rule.intervalKm.toInt()} km" else "No distance limit"} / ${if (rule.intervalDays > 0) "${rule.intervalDays} days" else "No time limit"}", color = TextSecondary, fontSize = 12.sp)
                Text("Last service: ${LocalDate.fromEpochDays(rule.lastServicedEpochDays.toInt())} · ${rule.lastServicedOdometerKm.toInt()} km", color = TextSecondary, fontSize = 11.sp)
                Text(if (health.wearPercentage >= 100) "Overdue ↗" else if (health.wearPercentage >= 75) "Service soon ↗" else "Healthy ↗", color = if (health.wearPercentage >= 75) WarningColor else HealthyColor, fontSize = 11.sp)
            }
        }
        Primary("Add component", "add", onClick = onAdd)
    }
}

@Composable
internal fun ServiceDialog(state: VaultState, initial: ComponentRule, onDismiss: () -> Unit, onSave: (VaultState) -> Boolean, onEdit: (ComponentRule) -> Unit, onHistory: () -> Unit) {
    var componentId by remember { mutableStateOf(initial.id) }
    var expanded by remember { mutableStateOf(false) }
    val component = state.rules.first { it.id == componentId }
    var odometer by remember { mutableStateOf(state.ride!!.odometer.toInt().toString()) }
    var date by remember { mutableStateOf(today().toString()) }
    var cost by remember { mutableStateOf("0") }
    var workshop by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val odo = odometer.toDoubleOrNull()
    val amount = cost.toDoubleOrNull()
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    Sheet("A little care goes a long way.", onDismiss) {
        Eyebrow("Service record")
        Text("Record actual mileage and date. The component baseline updates to this service.", color = TextSecondary, fontSize = 13.sp)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(component.name) }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) { state.rules.forEach { rule -> DropdownMenuItem(text = { Text(rule.name) }, onClick = { componentId = rule.id; expanded = false }) } }
        }
        Field("Odometer (km)", odometer, { odometer = it }, true)
        Field("Service date (YYYY-MM-DD)", date, { date = it })
        Field("Cost (৳)", cost, { cost = it }, true)
        Field("Workshop (optional)", workshop, { workshop = it.take(80) })
        OutlinedTextField(notes, { notes = it.take(500) }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary("Save service", "check", enabled = odo != null && amount != null && day != null) {
            runCatching { state.logService(component, odo!!, day!!, amount!!, workshop, notes) }.onSuccess {
                if (onSave(it)) onDismiss() else error = "Could not save. Your previous records are unchanged."
            }.onFailure { error = it.message }
        }
        TextButton(onClick = { onEdit(component) }) { Text("Edit this schedule") }
        TextButton(onClick = onHistory) { Text("Service history") }
    }
}

@Composable
internal fun ScheduleDialog(state: VaultState, existing: ComponentRule?, onDismiss: () -> Unit, onSave: (VaultState) -> Boolean) {
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
    Sheet(if (existing == null) "Add a component." else "Edit service interval.", onDismiss) {
        Eyebrow("Owner-defined schedule")
        Text("Use your manual or workshop advice. At least one interval is required.", color = TextSecondary, fontSize = 13.sp)
        Field("Component name", name, { name = it.take(40) })
        Field("Interval (km, 0 to disable)", km, { km = it }, true)
        Field("Interval (days, 0 to disable)", days, { days = it }, true)
        Field("Last serviced mileage", odometer, { odometer = it }, true)
        Field("Last serviced date (YYYY-MM-DD)", date, { date = it })
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary("Save schedule", "check", enabled = distance != null && intervalDays != null && odo != null && day != null && name.isNotBlank()) {
            runCatching { state.saveSchedule(ComponentRule(existing?.id ?: recordId(), "primary", name.trim(), "Owner-defined schedule", distance!!, intervalDays!!, odo!!, day!!.toEpochDays().toLong())) }.onSuccess {
                if (onSave(it)) onDismiss() else error = "Could not save. Your previous records are unchanged."
            }.onFailure { error = it.message }
        }
    }
}

@Composable
internal fun History(state: VaultState, filter: String, onAppearance: () -> Unit, onFilter: (String) -> Unit, onDelete: (RideLog) -> Unit, onFuel: () -> Unit, onService: () -> Unit) {
    Top("Ride history", "On this device", onAppearance)
    Heading("Your bike’s story.")
    Text("Local records for ${state.ride!!.displayName}.", color = TextSecondary, fontSize = 13.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val totals = listOf("Service spend" to "৳${oneDecimal(state.logs.filterNot { it.isFuel }.sumOf { it.cost })}", "Fuel spend" to "৳${oneDecimal(state.logs.filter { it.isFuel }.sumOf { it.cost })}", "Cycle km/L" to (state.economy?.let { oneDecimal(it) } ?: "--"))
        totals.forEach { (label, value) -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(label, color = TextSecondary, fontSize = 10.sp) } }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("all", "service", "fuel").forEach { kind -> FilterChip(filter == kind, onClick = { onFilter(kind) }, label = { Text(kind.replaceFirstChar { it.uppercase() }) }) } }
    val logs = state.logs.filter { filter == "all" || if (filter == "fuel") it.isFuel else !it.isFuel }.sortedWith(compareByDescending<RideLog> { it.date }.thenByDescending { it.odometer })
    if (logs.isEmpty()) Panel { Glyph("history", TextSecondary, Modifier.size(36.dp)); Text("The story starts here.", fontWeight = FontWeight.Bold); Text("Log a service or fill-up to build your ownership history.", color = TextSecondary, lineHeight = 20.sp) }
    logs.forEach { log -> Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph(if (log.isFuel) "fuel" else "service", MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (log.isFuel) "${log.fuelLiters ?: "?"} L · ${if (log.fullTank) "Full tank" else "Partial fill"}" else log.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${log.date} · ${grouped(log.odometer.toInt())} km · ৳${log.cost}", color = TextSecondary, fontSize = 11.sp)
                if (log.workshop.isNotBlank()) Text(log.workshop, color = TextSecondary, fontSize = 12.sp)
                if (log.notes.isNotBlank()) Text(log.notes, color = TextSecondary, fontSize = 12.sp)
            }
            TextButton(onClick = { onDelete(log) }, contentPadding = PaddingValues(0.dp)) { Text("Delete", color = TextSecondary, fontSize = 11.sp) }
        }
    } }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Primary("Log service", "service", Modifier.weight(1f), onClick = onService); Primary("Log fuel", "fuel", Modifier.weight(1f), onClick = onFuel) }
    Text("Owner-entered records. PDF service-passport export is a later app feature.", color = TextSecondary, fontSize = 11.sp)
}

@Composable
internal fun DocumentDialog(state: VaultState, existing: VaultDocument?, onDismiss: () -> Unit, onSave: (VaultState) -> Boolean) {
    val types = listOf("Registration smart card", "Driving licence", "Tax token", "Insurance", "Fitness certificate")
    var type by remember { mutableStateOf(existing?.title ?: types.first()) }
    var expanded by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf(existing?.number ?: "") }
    var holder by remember { mutableStateOf(existing?.holder ?: "") }
    var issued by remember { mutableStateOf(existing?.issued ?: "") }
    var expiry by remember { mutableStateOf(existing?.expiry ?: "") }
    var front by remember { mutableStateOf(existing?.frontPhoto) }
    var back by remember { mutableStateOf(existing?.backPhoto) }
    var error by remember { mutableStateOf<String?>(null) }
    val id = remember { existing?.id ?: recordId() }
    Sheet(if (existing == null) "Keep the essentials close." else "Edit your document.", onDismiss) {
        Eyebrow("Document vault")
        Text("Add details and optional photos. Files stay on this device.", color = TextSecondary, fontSize = 12.sp)
        Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(type) }; DropdownMenu(expanded, onDismissRequest = { expanded = false }) { types.forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = { type = value; expanded = false }) } } }
        Field("Document number", number, { number = it.take(80) })
        Field("Holder name", holder, { holder = it.take(80) })
        Field("Issue date (optional, YYYY-MM-DD)", issued, { issued = it })
        Field("Expiry date (optional, YYYY-MM-DD)", expiry, { expiry = it })
        Text("JPEG, PNG, or WebP photos, up to 1 MB each.", color = TextSecondary, fontSize = 11.sp)
        AttachmentPicker(if (front == null) "Add front photo" else "Replace front photo", onSelected = { front = it }, onError = { error = it })
        front?.let { Attachment(it, "Front of $type") }
        AttachmentPicker(if (back == null) "Add back photo" else "Replace back photo", onSelected = { back = it }, onError = { error = it })
        back?.let { Attachment(it, "Back of $type") }
        if (front != null || back != null) TextButton(onClick = { front = null; back = null }) { Text("Remove attached photos") }
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary("Save document", "check", enabled = number.isNotBlank() && holder.isNotBlank()) {
            runCatching { state.saveDocument(VaultDocument(type, number.trim(), holder.trim(), expiry.trim(), id, issued.trim(), front, back)) }.onSuccess {
                if (onSave(it)) onDismiss() else error = "Could not save. Your previous records are unchanged."
            }.onFailure { error = if (it is IllegalArgumentException) it.message ?: "Enter valid dates as YYYY-MM-DD." else "Enter valid dates as YYYY-MM-DD." }
        }
    }
}

@Composable
internal fun DocumentDetail(document: VaultDocument, inspection: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onInspectionList: () -> Unit) {
    var reverse by remember { mutableStateOf(false) }
    Sheet(document.title, onDismiss, light = inspection) {
        val textColor = TextPrimary
        Text(if (inspection) "Inspection · Stored copy" else "Owner-entered record", color = textColor, fontSize = 12.sp)
        Column(Modifier.fillMaxWidth().border(1.dp, if (inspection) Color.Gray else BorderColor, RoundedCornerShape(14.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(document.holder, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(document.number, color = textColor, fontSize = 18.sp)
            Text("${if (reverse) "BACK" else "FRONT"} · ${document.expiryLabel}", color = textColor, fontSize = 12.sp)
        }
        val photo = if (reverse) document.backPhoto else document.frontPhoto
        if (photo != null) Attachment(photo, "${if (reverse) "Back" else "Front"} of ${document.title}")
        else Text("No ${if (reverse) "back" else "front"} photo attached.", color = textColor, fontSize = 13.sp)
        Text("Issued: ${document.issued.ifBlank { "Not set" }}\nExpires: ${document.expiry.ifBlank { "Not set" }}", color = textColor, fontSize = 13.sp)
        Primary(if (reverse) "Show front" else "Show back", "document") { reverse = !reverse }
        if (inspection) TextButton(onClick = onInspectionList) { Text("All documents") }
        else { TextButton(onClick = onEdit) { Text("Edit document") }; TextButton(onClick = onDelete) { Text("Delete document", color = WarningColor) } }
    }
}

@Composable
internal fun Inspection(documents: List<VaultDocument>, onDismiss: () -> Unit, onDocument: (VaultDocument) -> Unit) {
    Sheet("Vehicle documents", onDismiss, light = true) {
        Text("Only document details and attachments are shown.", color = TextPrimary, fontSize = 13.sp)
        if (documents.isEmpty()) Text("No documents stored yet. Exit this mode and add your first document.", color = TextPrimary)
        documents.forEach { document -> Column(Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)).clickable { onDocument(document) }.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(document.title, color = TextPrimary, fontWeight = FontWeight.Bold); Text(document.number, color = TextPrimary, fontSize = 18.sp); Text(document.expiryLabel + " ↗", color = TextPrimary, fontSize = 12.sp)
        } }
        Text("Stored copies are owner-provided. Carry originals where required.", color = TextPrimary, fontSize = 11.sp)
    }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
internal fun Attachment(value: String, description: String) {
    val bitmap = remember(value) { runCatching { Base64.decode(value).decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, description, Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit)
    else Text("Could not display this photo.", color = WarningColor, fontSize = 12.sp)
}
