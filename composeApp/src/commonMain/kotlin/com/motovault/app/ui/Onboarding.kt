package com.motovault.app.ui

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
            Row(verticalAlignment = Alignment.CenterVertically) { Text("ধাপ ${bengaliDigits(step)} / ৩", color = TextSecondary, fontSize = 11.sp); AppearanceButton(onAppearance) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { (1..3).forEach { Box(Modifier.weight(1f).height(3.dp).background(if (it <= step) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(2.dp))) } }
        if (step == 1) {
            Eyebrow("আপনার গ্যারেজ শুরু হোক")
            Heading("কোন বাইক চালান?")
            Text("ব্র্যান্ড বেছে নিন। আপনার বাইকের জন্যই সাজবে এই জায়গা।", color = TextSecondary, lineHeight = 21.sp)
            worlds.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { world ->
                    Column(Modifier.weight(1f).heightIn(min = 80.dp).clip(RoundedCornerShape(12.dp)).background(if (brand.id == world.id) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else SurfaceColor)
                        .border(1.dp, if (brand.id == world.id) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(12.dp)).clickable {
                            if (brand.id != world.id) { modelId = null; modelName = "" }
                            onBrand(world.id); picker = true
                        }.semantics { contentDescription = "${world.name} বেছে নিন, ${BikeCatalogue.forBrand(world.id).size}টি মডেল" }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Image(painterResource(world.logo()), world.name, Modifier.width(66.dp).height(28.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(if (brand.id == world.id) MaterialTheme.colorScheme.primary else TextPrimary))
                        Spacer(Modifier.height(9.dp)); Text("${BikeCatalogue.forBrand(world.id).size}টি মডেল", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } }
            Panel(onClick = { picker = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    chosen?.let { ModelPhoto(it, Modifier.width(72.dp).height(48.dp)) }
                    Column(Modifier.weight(1f)) { Eyebrow(if (modelName.isBlank()) "আপনার বাইক" else "নির্বাচিত মডেল", TextSecondary); Text(modelName.ifBlank { "মডেল বেছে নিন" }, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    Glyph("arrow", MaterialTheme.colorScheme.primary)
                }
            }
            Panel(tint = MaterialTheme.colorScheme.primary.copy(alpha = .07f)) { Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))); Column { Text(brandToneName(brand.id), fontWeight = FontWeight.Bold); Text("ব্র্যান্ড অনুযায়ী অ্যাকসেন্ট রঙ বদলাবে।", color = TextSecondary, fontSize = 12.sp) }
            } }
            Primary("এগিয়ে যান", "arrow") { if (modelName.isBlank()) picker = true else step = 3 }
            Text("একটি বাইক। আপনার তথ্য এই ডিভাইসেই থাকবে।", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Eyebrow("০৩ / বাইকের তথ্য")
            Heading("বাইকের তথ্য যোগ করুন")
            Text("${brand.name} $modelName", color = TextSecondary)
            chosen?.let { ModelPhoto(it, Modifier.fillMaxWidth().height(160.dp)) }
            TextButton(onClick = { picker = true }) { Text("মডেল বদলান") }
            Field("ভ্যারিয়েন্ট / সংস্করণ (ঐচ্ছিক)", edition, { edition = it.take(60) })
            Field("মডেল বছর · Model year", year, { year = it }, numeric = true)
            if (validYear == null) Text("বছর ১৯৮০ থেকে ২১০০-এর মধ্যে দিন।", color = WarningColor, fontSize = 12.sp)
            Text("কোন বাজারের মডেল? · Original market", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            RideMarket.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { market = option }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(market == option, onClick = { market = option }); Text(
                        when (option) {
                            RideMarket.UNKNOWN -> "নিশ্চিত নই / অন্য বাজার · Not sure / other market"
                            RideMarket.INDIA -> "ভারত · India"
                            RideMarket.EUROPE -> "ইউরোপ / যুক্তরাজ্য · Europe / UK"
                            RideMarket.BANGLADESH -> "বাংলাদেশ · Bangladesh"
                        }, fontSize = 12.sp
                    )
                }
            }
            Text("কাগজে থাকা নির্গমন মান · Emission standard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("গাড়ির কাগজ দেখে লিখুন। মডেল বা বছর থেকে অ্যাপ এটি যাচাই করে না।", color = TextSecondary, fontSize = 12.sp)
            emissions.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { emission = option }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(emission == option, onClick = { emission = option }); Text(
                        when (option) {
                            RideEmission.UNKNOWN -> "নিশ্চিত নই · Not confirmed"
                            RideEmission.BS4 -> "BS4"
                            RideEmission.BS6 -> "BS6"
                            RideEmission.EURO4 -> "Euro 4"
                            RideEmission.EURO5 -> "Euro 5"
                            RideEmission.EURO5_PLUS -> "Euro 5+"
                        }, fontSize = 12.sp
                    )
                }
            }
            Field("কোম্পানির মডেল কোড (ঐচ্ছিক)", modelCode, { modelCode = it.take(30) })
            if (profile != null) {
                val uri = androidx.compose.ui.platform.LocalUriHandler.current
                TextButton(onClick = { uri.openUri(profile.manualUrl) }) { Text("মিল থাকা KTM ম্যানুয়াল দেখুন") }
                Row(Modifier.fillMaxWidth().clickable { useManual = !useManual; confirmed = false }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(useManual, onCheckedChange = { useManual = it; confirmed = false })
                    Text("ম্যানুয়ালের নিয়মিত অয়েল সার্ভিসের সময়সূচি নিন", fontSize = 12.sp)
                }
            } else Text("এই মডেলের ম্যানুয়াল নেই। সময়সূচি আপনি ঠিক করবেন।", color = TextSecondary, fontSize = 12.sp)
            Field("বর্তমান ওডোমিটার · km", odometer, { odometer = it }, numeric = true)
            Field("রেজিস্ট্রেশন (ঐচ্ছিক)", registration, { registration = it.take(80) })
            Field("শেষ সার্ভিসে ওডোমিটার · km", baseline, { baseline = it }, numeric = true)
            Field("শেষ সার্ভিসের তারিখ · YYYY-MM-DD", baselineDate, { baselineDate = it })
            if (base == null) Text("শেষ সার্ভিসের কিলোমিটার শূন্য থেকে বর্তমান ওডোমিটারের মধ্যে দিন।", color = WarningColor, fontSize = 12.sp)
            if (date == null) Text("আজকের তারিখের বেশি নয়, এমন সঠিক তারিখ দিন।", color = WarningColor, fontSize = 12.sp)
            Text(if (useManual && profile != null) "ম্যানুয়াল অনুযায়ী: প্রথম সার্ভিস ১,০০০ কিমিতে। এরপর অয়েল, ফিল্টার ও স্ক্রিন ৭,৫০০ কিমি বা ১২ মাস পরপর। সময়ের জন্য অ্যাপে ৩৬৫ দিনের রিমাইন্ডার। চেইনের সময়সূচি আপনি বদলাতে পারবেন। স্থানীয় ব্যবহার নিয়ে ডিলারের সঙ্গে মিলিয়ে নিন। এটি সম্পূর্ণ ওয়ার্কশপ চেকলিস্ট নয়।" else "শুরুর উদাহরণ: চেইন ৫০০ কিমি / ৩০ দিন, অয়েল ৩,০০০ কিমি / ১৮০ দিন, এয়ার ফিল্টার ৫,০০০ কিমি / ১৮০ দিন। এগুলো বদলানো যায়, কোম্পানির নিয়ম নয়। শেষ কবে সার্ভিস করেছেন লিখুন, পরে বাইক অনুযায়ী সময় ঠিক করুন।", color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp)
            Row(Modifier.fillMaxWidth().clickable { confirmed = !confirmed }, verticalAlignment = Alignment.CenterVertically) { Checkbox(confirmed, onCheckedChange = { confirmed = it }); Text("সময় ও শেষ সার্ভিসের তথ্য মিলিয়ে দেখেছি।", fontSize = 12.sp) }
            if (useManual && (base ?: 0.0) < 1000) Text("১,০০০ কিমির প্রথম সার্ভিসের পরের তথ্য দিয়ে এই সময়সূচি চালু করুন। আপাতত নিজের রিমাইন্ডার ব্যবহার করুন।", color = WarningColor, fontSize = 12.sp)
            Primary(if (existing == null) "গ্যারেজ তৈরি করুন" else "বাইক বদলানো পর্যালোচনা করুন", "arrow", enabled = confirmed && odo != null && base != null && date != null && validYear != null && modelName.isNotBlank() && (!useManual || base >= 1000)) {
                onCreate(Ride(brand.id, edition.trim().ifBlank { "নিজের সেটআপ · Owner-defined" }, validYear!!, odo!!, modelId, modelName, registration.trim(), market, emission, modelCode.trim().uppercase(), profile?.id?.takeIf { useManual }), base!!, date!!)
            }
            TextButton(onClick = { step = 1 }) { Text("ব্র্যান্ডে ফিরে যান") }
        }
        onCancel?.let { TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("বর্তমান গ্যারেজ রাখুন", color = TextSecondary) } }
    }
    if (picker) ModelPicker(brand, modelId, onDismiss = { picker = false }, onManual = { picker = false; manual = true }) {
        modelId = it.id; modelName = it.name; picker = false; step = 3
    }
    if (manual) {
        var name by rememberSaveable { mutableStateOf(if (modelId == null) modelName else "") }
        Sheet("আপনার বাইকও এখানে যোগ করুন", onDismiss = { manual = false }) {
            Text("নিজের মতো মডেলের নাম লিখুন। নিজে যোগ করা মডেলের সঙ্গে রেফারেন্স ছবি থাকবে না।", color = TextSecondary, fontSize = 13.sp)
            Field("বাইকের মডেল · Model name", name, { name = it.take(60) })
            Primary("তথ্য দিতে এগিয়ে যান", "arrow", enabled = name.isNotBlank()) { modelName = name.trim(); modelId = null; manual = false; step = 3 }
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
                Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(brand.logo()), brand.name, Modifier.width(58.dp).height(27.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)); Spacer(Modifier.width(12.dp)); Text("${brand.name} · ${BikeCatalogue.forBrand(brand.id).size}টি মডেল", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f)); IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "মডেল বাছাই বন্ধ করুন" }) { Glyph("close") } }
                    Eyebrow("০২ / মডেল বেছে নিন"); Heading("আপনার বাইকটি খুঁজুন")
                    Text("ছবি দেখে বাছুন, পরে সংস্করণ, বছর ও কিলোমিটার মিলিয়ে নিন।", color = TextSecondary, fontSize = 12.sp)
                    Field("${brand.name} মডেল খুঁজুন · Search", query, { query = it })
                }
                if (models.isEmpty()) Column(Modifier.weight(1f).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("মডেল পাওয়া যায়নি", fontWeight = FontWeight.Bold); Text("ছোট নাম লিখে খুঁজুন, অথবা নিজে মডেল যোগ করুন।", color = TextSecondary) }
                else BoxWithConstraints(Modifier.weight(1f)) {
                    LazyVerticalGrid(columns = GridCells.Fixed(if (maxWidth < 600.dp) 2 else 3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(models, key = { it.id }) { model ->
                        val selected = candidateId == model.id
                        Column(Modifier.clip(RoundedCornerShape(13.dp)).background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else SurfaceColor)
                            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else BorderColor, RoundedCornerShape(13.dp)).clickable { candidateId = model.id }
                            .semantics { contentDescription = "${model.name} বেছে নিন${if (selected) ", নির্বাচিত" else ""}" }) {
                            Box { ModelPhoto(model, Modifier.fillMaxWidth().height(130.dp), picker = true); if (selected) Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(20.dp), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) { Glyph("check", MaterialTheme.colorScheme.onPrimary, Modifier.padding(4.dp).size(18.dp)) } }
                            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(model.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp); Text(if (model.note.contains("Bangladesh") && !model.note.contains("Third-party")) "বাংলাদেশের তালিকা" else "নমুনা ছবি", color = TextSecondary, fontSize = 10.sp) }
                        }
                    }
                    }
                }
                Column(Modifier.fillMaxWidth().border(1.dp, BorderColor).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onManual, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("আমার মডেল তালিকায় নেই", color = TextSecondary) }
                    Primary("${candidate?.name ?: "মডেল"} বেছে এগিয়ে যান", "arrow", enabled = candidate != null) { onConfirm(candidate!!) }
                    Text("ছবিগুলো নমুনা। বছর, রঙ ও যন্ত্রাংশে পার্থক্য থাকতে পারে।", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
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
