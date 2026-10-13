package com.motovault.app.ui.redesign

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun Setup(
    brand: BrandWorld, existing: Ride?, onAppearance: () -> Unit, onBrand: (String) -> Unit,
    onCreate: (Ride, Double, LocalDate) -> Unit, onCancel: (() -> Unit)? = null
) {
    var step by rememberSaveable { mutableStateOf(1) }
    var modelId by rememberSaveable { mutableStateOf(existing?.selectedModel?.id) }
    var modelName by rememberSaveable { mutableStateOf(existing?.displayName ?: "") }
    var picker by rememberSaveable { mutableStateOf(false) }
    var manual by rememberSaveable { mutableStateOf(false) }
    var edition by rememberSaveable { mutableStateOf(existing?.edition ?: "") }
    var year by rememberSaveable { mutableStateOf(existing?.year?.toString() ?: "2023") }
    var odometer by rememberSaveable { mutableStateOf(existing?.odometer?.toInt()?.toString() ?: "0") }
    var registration by rememberSaveable { mutableStateOf(existing?.registration ?: "") }
    var baseline by rememberSaveable { mutableStateOf(existing?.odometer?.toInt()?.toString() ?: "0") }
    var baselineDate by rememberSaveable { mutableStateOf(today().toString()) }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    var market by rememberSaveable { mutableStateOf(existing?.market ?: RideMarket.UNKNOWN) }
    var emission by rememberSaveable { mutableStateOf(existing?.emission ?: RideEmission.UNKNOWN) }
    var modelCode by rememberSaveable { mutableStateOf(existing?.modelCode ?: "") }
    var useManual by rememberSaveable { mutableStateOf(existing?.factoryProfileId != null) }
    val chosen = BikeCatalogue.find(modelId)
    val odo = odometer.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..9999999.0 }
    val base = baseline.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 && odo != null && it <= odo }
    val date = runCatching { LocalDate.parse(baselineDate) }.getOrNull()?.takeIf { it <= today() }
    val validYear = year.toIntOrNull()?.takeIf { it in 1980..2100 }
    val emissions = FactoryProfiles.emissions(market)
    val profile = FactoryProfiles.available(modelId, validYear, market).firstOrNull {
        modelCode.trim().uppercase() in it.modelCodes
    }
    LaunchedEffect(modelId, year, market, modelCode) {
        if (emission !in emissions) emission = RideEmission.UNKNOWN
        if (profile == null) useManual = false
        confirmed = false
    }
    LaunchedEffect(baseline, baselineDate, odometer, emission, useManual) { confirmed = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Glyph("mark", MaterialTheme.colorScheme.primary, Modifier.size(28.dp)); Text("MotoVault", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("0$step / 03", color = TextSecondary, fontSize = 11.sp); AppearanceButton(onAppearance) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { (1..3).forEach { Box(Modifier.weight(1f).height(3.dp).background(if (it <= step) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(2.dp))) } }
        if (step == 1) {
            Eyebrow("Your garage starts here")
            Heading("What do you ride?")
            Text("Choose your brand. We’ll make this space feel like it belongs to your bike.", color = TextSecondary, lineHeight = 21.sp)
            worlds.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { world ->
                    Column(Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(12.dp)).background(if (brand.id == world.id) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else SurfaceColor)
                        .border(1.dp, if (brand.id == world.id) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(12.dp)).clickable {
                            if (brand.id != world.id) { modelId = null; modelName = "" }
                            onBrand(world.id); picker = true
                        }.semantics { contentDescription = "Choose ${world.name}, ${BikeCatalogue.forBrand(world.id).size} models" }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Image(painterResource(world.logo()), world.name, Modifier.width(66.dp).height(28.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(if (brand.id == world.id) MaterialTheme.colorScheme.primary else TextPrimary))
                        Spacer(Modifier.height(9.dp)); Text("${BikeCatalogue.forBrand(world.id).size} MODELS", color = TextSecondary, fontSize = 8.sp, letterSpacing = .8.sp)
                    }
                }
            } }
            Panel(onClick = { picker = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    chosen?.let { ModelPhoto(it, Modifier.width(72.dp).height(48.dp)) }
                    Column(Modifier.weight(1f)) { Eyebrow(if (modelName.isBlank()) "Your motorcycle" else "Selected model", TextSecondary); Text(modelName.ifBlank { "Choose a model" }, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    Glyph("arrow", MaterialTheme.colorScheme.primary)
                }
            }
            Panel(tint = MaterialTheme.colorScheme.primary.copy(alpha = .07f)) { Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))); Column { Text(brand.tone, fontWeight = FontWeight.Bold); Text("Your bike sets the tone.", color = TextSecondary, fontSize = 12.sp) }
            } }
            Primary("Continue", "arrow") { if (modelName.isBlank()) picker = true else step = 3 }
            Text("One bike. Your records stay on this device.", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Eyebrow("03 / Ownership details")
            Heading("Make it your own.")
            Text("${brand.name} $modelName", color = TextSecondary)
            chosen?.let { ModelPhoto(it, Modifier.fillMaxWidth().height(160.dp)) }
            TextButton(onClick = { picker = true }) { Text("Change model") }
            Field("Variant / edition (optional)", edition, { edition = it.take(60) })
            Field("Release year", year, { year = it }, numeric = true)
            if (validYear == null) Text("Enter a release year between 1980 and 2100.", color = WarningColor, fontSize = 12.sp)
            Text("Original market", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            RideMarket.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { market = option }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(market == option, onClick = { market = option }); Text(option.label, fontSize = 12.sp)
                }
            }
            Text("Emission standard on your paperwork", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Copy this from your vehicle paperwork. The app does not verify emission approval from the model or year.", color = TextSecondary, fontSize = 12.sp)
            emissions.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { emission = option }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(emission == option, onClick = { emission = option }); Text(option.label, fontSize = 12.sp)
                }
            }
            Field("Manufacturer model code (optional)", modelCode, { modelCode = it.take(30) })
            if (profile != null) {
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(onClick = { uri.openUri(profile.manualUrl) }) { Text("Read the matching KTM manual") }
                Row(Modifier.fillMaxWidth().clickable { useManual = !useManual; confirmed = false }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(useManual, onCheckedChange = { useManual = it; confirmed = false })
                    Text("Use this manual’s recurring oil schedule", fontSize = 12.sp)
                }
            } else Text("No matching factory manual yet. Your schedules remain owner-defined.", color = TextSecondary, fontSize = 12.sp)
            Field("Current odometer (km)", odometer, { odometer = it }, numeric = true)
            Field("Registration (optional)", registration, { registration = it.take(80) })
            Field("Last service odometer (km)", baseline, { baseline = it }, numeric = true)
            Field("Service baseline date (YYYY-MM-DD)", baselineDate, { baselineDate = it })
            if (base == null) Text("Last service mileage must be between zero and your current reading.", color = WarningColor, fontSize = 12.sp)
            if (date == null) Text("Enter a valid baseline date, no later than today.", color = WarningColor, fontSize = 12.sp)
            Text(if (useManual && profile != null) "Oil, filter and screen: manual interval 7,500 km / 12 months after running-in. The app uses a 365-day reminder for the time interval. First service is due at 1,000 km. Chain care remains an editable owner reminder. Confirm local conditions with your dealer. These reminders do not cover the full workshop checklist." else "Starting schedules: chain 500 km / 30 days, oil 3,000 km / 180 days, air filter 5,000 km / 180 days. These are editable examples, not manufacturer specifications. Enter when these items were last serviced, then adjust each schedule for your bike.", color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp)
            Row(Modifier.fillMaxWidth().clickable { confirmed = !confirmed }, verticalAlignment = Alignment.CenterVertically) { Checkbox(confirmed, onCheckedChange = { confirmed = it }); Text("I checked the schedules and service baseline.", fontSize = 12.sp) }
            if (useManual && (base ?: 0.0) < 1000) Text("The recurring preset needs a baseline after the 1,000 km running-in service. Use owner reminders until then.", color = WarningColor, fontSize = 12.sp)
            Primary(if (existing == null) "Create my garage" else "Review replacement", "arrow", enabled = confirmed && odo != null && base != null && date != null && validYear != null && modelName.isNotBlank() && (!useManual || base >= 1000)) {
                onCreate(Ride(brand.id, edition.trim().ifBlank { "Owner-defined setup" }, validYear!!, odo!!, modelId, modelName, registration.trim(), market, emission, modelCode.trim().uppercase(), profile?.id?.takeIf { useManual }), base!!, date!!)
            }
            TextButton(onClick = { step = 1 }) { Text("Back to brands") }
        }
        onCancel?.let { TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Keep current garage", color = TextSecondary) } }
    }
    if (picker) ModelPicker(brand, modelId, onDismiss = { picker = false }, onManual = { picker = false; manual = true }) {
        modelId = it.id; modelName = it.name; picker = false; step = 3
    }
    if (manual) {
        var name by rememberSaveable { mutableStateOf(if (modelId == null) modelName else "") }
        Sheet("Your bike belongs here too.", onDismiss = { manual = false }) {
            Text("Use your own model name. A reference photo is not added for manual models.", color = TextSecondary, fontSize = 13.sp)
            Field("Model name", name, { name = it.take(60) })
            Primary("Continue to details", "arrow", enabled = name.isNotBlank()) { modelName = name.trim(); modelId = null; manual = false; step = 3 }
        }
    }
}

@Composable
internal fun ModelPicker(brand: BrandWorld, selectedId: String?, onDismiss: () -> Unit, onManual: () -> Unit, onConfirm: (BikeModel) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var candidateId by rememberSaveable { mutableStateOf(selectedId) }
    val candidate = BikeCatalogue.find(candidateId)?.takeIf { it.brandId == brand.id }
    val models = BikeCatalogue.search(brand.id, query)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PlatformAppearance(LocalVaultPalette.current.dark)
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 820.dp).fillMaxWidth().fillMaxHeight(.96f).clip(RoundedCornerShape(22.dp)).background(SurfaceColor)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(brand.logo()), brand.name, Modifier.width(58.dp).height(27.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)); Spacer(Modifier.width(12.dp)); Text("${brand.name} · ${BikeCatalogue.forBrand(brand.id).size} models", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "Close model picker" }) { Glyph("close") } }
                    Eyebrow("02 / Choose your model"); Heading("Find the one you ride.")
                    Text("Select by photo, then confirm your edition, year, and mileage.", color = TextSecondary, fontSize = 12.sp)
                    Field("Search ${brand.name} models", query, { query = it })
                }
                if (models.isEmpty()) Column(Modifier.weight(1f).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("No matching models", fontWeight = FontWeight.Bold); Text("Try a shorter name or add your model manually.", color = TextSecondary) }
                else BoxWithConstraints(Modifier.weight(1f)) {
                    LazyVerticalGrid(columns = GridCells.Fixed(if (maxWidth < 600.dp) 2 else 3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(models, key = { it.id }) { model ->
                        val selected = candidateId == model.id
                        Column(Modifier.clip(RoundedCornerShape(13.dp)).background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else SurfaceColor)
                            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(13.dp)).clickable { candidateId = model.id }
                            .semantics { contentDescription = "Select ${model.name}${if (selected) ", selected" else ""}" }) {
                            Box { ModelPhoto(model, Modifier.fillMaxWidth().height(130.dp), picker = true); if (selected) Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(20.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Glyph("check", MaterialTheme.colorScheme.onPrimary, Modifier.padding(4.dp).size(18.dp)) } }
                            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(model.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp); Text(if (model.note.contains("Bangladesh") && !model.note.contains("Third-party")) "Bangladesh catalogue" else "Reference photo", color = TextSecondary, fontSize = 9.sp) }
                        }
                    }
                    }
                }
                Column(Modifier.fillMaxWidth().border(1.dp, BorderColor).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onManual, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("My model isn’t listed", color = TextSecondary) }
                    Primary("Continue with ${candidate?.name ?: "a model"}", "arrow", enabled = candidate != null) { onConfirm(candidate!!) }
                    Text("Reference photos. Exact year, colour, and equipment may differ.", color = TextSecondary, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
internal fun ModelPhoto(model: BikeModel, modifier: Modifier, picker: Boolean = false) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(androidx.compose.ui.graphics.Color(0xFFEFEFED))) {
        val scale = if (picker && model.id in listOf("honda-hornet-2-0", "honda-sp160", "honda-sp125")) 1.55f else 1f
        Image(painterResource(model.photo), model.name + " reference photo", Modifier.fillMaxSize().padding(8.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color(0xFFEFEFED), BlendMode.Multiply))
    }
}
