package com.motovault.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.usecase.CalculateWearUseCase
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import com.motovault.app.storage.activeVaultSession
import androidx.compose.ui.window.DialogProperties
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.Font
import kotlin.math.roundToLong

internal data class BrandWorld(val id: String, val name: String, val accent: Color, val tone: String)

internal val worlds = listOf(
    BrandWorld("ktm", "KTM", Color(0xFFFF7900), "Racing orange"),
    BrandWorld("cfmoto", "CFMOTO", Color(0xFF69D2DB), "Performance cyan"),
    BrandWorld("yamaha", "Yamaha", Color(0xFF88A8FF), "Performance blue"),
    BrandWorld("honda", "Honda", Color(0xFFFF7B84), "Honda red"),
    BrandWorld("suzuki", "Suzuki", Color(0xFF7EBAF5), "Sport blue"),
    BrandWorld("bajaj", "Bajaj", Color(0xFF96B9FA), "Pulsar blue"),
    BrandWorld("tvs", "TVS", Color(0xFFF4828B), "Racing red"),
    BrandWorld("hero", "Hero", Color(0xFFFF8983), "Hero red"),
    BrandWorld("enfield", "Royal Enfield", Color(0xFFDBB77C), "Heritage gold")
)

private enum class Tab(val title: String, val icon: String) { GARAGE("গ্যারেজ", "garage"), VAULT("কাগজপত্র", "vault"), HISTORY("ইতিহাস", "history") }

internal fun bengaliDigits(value: Int): String = value.toString().map { digit ->
    if (digit in '0'..'9') ('০'.code + digit.code - '0'.code).toChar() else digit
}.joinToString("")

internal fun brandToneName(brandId: String): String = when (brandId) {
    "cfmoto" -> "নীলাভ সবুজ"
    "yamaha" -> "পারফরম্যান্স নীল"
    "honda" -> "হোন্ডা লাল"
    "suzuki" -> "স্পোর্টি নীল"
    "bajaj" -> "পালসার নীল"
    "tvs" -> "রেসিং লাল"
    "hero" -> "হিরো লাল"
    "enfield" -> "ঐতিহ্যের সোনালি"
    else -> "রেসিং কমলা"
}

private fun componentSuffix(rule: ComponentRule): String {
    val knownSuffix = listOf("air_filter", "spark_plug", "brake_pads", "brake_fluid", "chain", "oil", "coolant")
        .firstOrNull { rule.id.endsWith("_$it") }
    return knownSuffix ?: when (rule.name.trim().lowercase()) {
        "chain" -> "chain"
        "engine oil", "engine oil & oil filter" -> "oil"
        "air filter" -> "air_filter"
        "spark plug" -> "spark_plug"
        "brake pads (front & rear)" -> "brake_pads"
        "engine coolant" -> "coolant"
        "brake fluid (dot 4)" -> "brake_fluid"
        else -> rule.id.substringAfterLast('_')
    }
}

internal fun componentLabel(rule: ComponentRule): String {
    val suffix = componentSuffix(rule)
    val bengaliName = when (suffix) {
        "chain" -> "চেইন পরিষ্কার ও লুব্রিকেন্ট"
        "oil" -> "ইঞ্জিন অয়েল ও অয়েল ফিল্টার"
        "air_filter" -> "এয়ার ফিল্টার"
        "spark_plug" -> "স্পার্ক প্লাগ"
        "brake_pads" -> "সামনে ও পেছনের ব্রেক প্যাড"
        "coolant" -> "ইঞ্জিন কুল্যান্ট"
        "brake_fluid" -> "ব্রেক ফ্লুইড (DOT 4)"
        else -> null
    }
    if (bengaliName == null) return rule.name
    return "$bengaliName · ${rule.name}"
}

private fun componentShortLabel(rule: ComponentRule): String = when (componentSuffix(rule)) {
    "chain" -> "চেইন · Chain"
    "oil" -> "ইঞ্জিন অয়েল · Engine oil"
    "air_filter" -> "এয়ার ফিল্টার · Air filter"
    "spark_plug" -> "স্পার্ক প্লাগ · Spark plug"
    "brake_pads" -> "ব্রেক প্যাড · Brake pads"
    "coolant" -> "কুল্যান্ট · Coolant"
    "brake_fluid" -> "ব্রেক ফ্লুইড · Brake fluid"
    else -> rule.name
}

internal fun BrandWorld.logo(): DrawableResource = when (id) {
    "ktm" -> Res.drawable.logo_ktm
    "cfmoto" -> Res.drawable.logo_cfmoto
    "yamaha" -> Res.drawable.logo_yamaha
    "honda" -> Res.drawable.logo_honda
    "suzuki" -> Res.drawable.logo_suzuki
    "bajaj" -> Res.drawable.logo_bajaj
    "tvs" -> Res.drawable.logo_tvs
    "hero" -> Res.drawable.logo_hero
    else -> Res.drawable.logo_enfield
}

@Composable
fun MotoVault() {
    val session = remember { activeVaultSession }
    val snapshot by session.snapshots.collectAsState()
    val state = snapshot.state
    val revision = snapshot.revision
    val storageError = snapshot.error
    val loading = snapshot.loading
    val saving = snapshot.saving
    var tab by remember { mutableStateOf(Tab.GARAGE) }
    var historyFilter by remember { mutableStateOf("all") }
    var editingRide by remember { mutableStateOf(false) }
    var pendingGarage by remember { mutableStateOf<VaultState?>(null) }
    var selectedBrand by remember { mutableStateOf(state.ride?.brandId ?: "ktm") }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRule by remember { mutableStateOf<ComponentRule?>(null) }
    var selectedLogId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedLog = state.logs.firstOrNull { it.id == selectedLogId }
    var selectedDocumentId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDocument = state.documents.firstOrNull { it.id == selectedDocumentId }
    var manualDocumentType by rememberSaveable { mutableStateOf(documentTypes.first()) }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var deleteKind by remember { mutableStateOf("log") }
    val brand = worlds.firstOrNull { it.id == selectedBrand } ?: worlds.first()
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val initialRevision = remember { snapshot.revision }
    LaunchedEffect(loading) { if (!loading) selectedBrand = state.ride?.brandId ?: "ktm" }
    LaunchedEffect(snapshot.revision) {
        if (snapshot.revision > initialRevision) dialog = null
    }
    fun commit(next: VaultState, completed: (Boolean) -> Unit) {
        session.commit(next, revision) { saved ->
            if (scope.isActive) {
                completed(saved)
                if (saved) scope.launch { snackbars.showSnackbar("রেকর্ড সংরক্ষণ হয়েছে", duration = SnackbarDuration.Short) }
            }
        }
    }
    fun history(kind: String) { historyFilter = kind; tab = Tab.HISTORY; dialog = null }
    val sans = FontFamily(
        Font(Res.font.anek_bangla, weight = FontWeight.Normal),
        Font(Res.font.anek_bangla, weight = FontWeight.Medium),
        Font(Res.font.anek_bangla, weight = FontWeight.SemiBold),
        Font(Res.font.anek_bangla, weight = FontWeight.Bold)
    )
    val typography = Typography().let { base -> base.copy(
        bodyLarge = base.bodyLarge.copy(fontFamily = sans), bodyMedium = base.bodyMedium.copy(fontFamily = sans),
        bodySmall = base.bodySmall.copy(fontFamily = sans), labelLarge = base.labelLarge.copy(fontFamily = sans),
        labelMedium = base.labelMedium.copy(fontFamily = sans), labelSmall = base.labelSmall.copy(fontFamily = sans),
        titleLarge = base.titleLarge.copy(fontFamily = sans), titleMedium = base.titleMedium.copy(fontFamily = sans)
    ) }
    VaultTheme(brand.id, state.appearance, typography) {
        PlatformAppearance(LocalVaultPalette.current.dark)
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(SurfaceColor, CanvasColor))).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
                storageError?.let { Text(it, color = WarningColor, modifier = Modifier.fillMaxWidth().padding(16.dp), fontSize = 12.sp) }
                if (loading) {
                    CircularProgressIndicator(Modifier.padding(32.dp))
                } else if (state.ride == null || editingRide) {
                    Setup(brand, existing = state.ride, onAppearance = { dialog = "appearance" }, onBrand = { selectedBrand = it }, onCreate = { ride, baseline, date ->
                        val draft = VaultState.create(ride, baseline, date).copy(documents = state.documents, appearance = state.appearance, autoLocation = state.autoLocation)
                        if (state.ride != null) { pendingGarage = draft; dialog = "replace" }
                        else commit(draft) { if (it) editingRide = false }
                    }, onCancel = if (state.ride != null) ({ editingRide = false; selectedBrand = state.ride!!.brandId }) else null)
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        when (tab) {
                            Tab.GARAGE -> Garage(state, brand, onAppearance = { dialog = "appearance" }, onOdometer = { dialog = "odometer" }, onFuel = { dialog = "fuel" },
                                onRule = { selectedRule = it; dialog = "service" }, onComponents = { dialog = "components" },
                                onChangeBike = { editingRide = true }, onFuelHistory = { history("fuel") }, onVault = { tab = Tab.VAULT })
                            Tab.VAULT -> Vault(state.documents, onAppearance = { dialog = "appearance" }, onAdd = { selectedDocumentId = null; dialog = "documentCapture" },
                                onDocument = { selectedDocumentId = it.id; dialog = "detail" }, onInspection = { selectedDocumentId = state.documents.firstOrNull()?.id; dialog = if (selectedDocumentId == null) "inspection" else "inspectDetail" })
                            Tab.HISTORY -> History(state, historyFilter, onAppearance = { dialog = "appearance" }, onFilter = { historyFilter = it },
                                onDelete = { deleteId = it.id; deleteKind = "log"; dialog = "delete" },
                                onEdit = { selectedLogId = it.id; dialog = if (it.isFuel) "editFuel" else "editService" },
                                onFuel = { dialog = "fuel" }, onService = { selectedRule = state.rules.firstOrNull(); dialog = if (selectedRule == null) "components" else "service" })
                        }
                    }
                    NavigationBar(containerColor = CanvasColor, tonalElevation = 0.dp, modifier = Modifier.border(1.dp, BorderColor)) {
                        Tab.entries.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item; historyFilter = "all" },
                            icon = { Glyph(item.icon, if (tab == item) MaterialTheme.colorScheme.primary else TextSecondary) }, label = { Text(item.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .10f), selectedTextColor = MaterialTheme.colorScheme.primary, unselectedTextColor = TextSecondary)) }
                    }
                }
            }
            when (dialog) {
                "appearance" -> AppearanceDialog(state.appearance, state.autoLocation, onDismiss = { dialog = null }, onSelect = { choice -> commit(state.copy(appearance = choice)) { if (it) dialog = null } }, onAutoLocationChange = { enabled -> commit(state.copy(autoLocation = enabled)) {} })
                "replace" -> Sheet("Start a new bike record?", onDismiss = { dialog = null }) {
                    Text("This removes the current bike’s service and fuel records. Documents stay in your vault.", color = TextSecondary)
                    Primary("Replace garage", "check") { pendingGarage?.let { draft -> commit(draft) { if (it) { editingRide = false; dialog = null; tab = Tab.GARAGE } } } }
                    TextButton(onClick = { dialog = null; editingRide = false; selectedBrand = state.ride!!.brandId }) { Text("Keep current garage") }
                }
                "odometer" -> NumberDialog("Update odometer", "Current distance (km)", state.ride!!.odometer.toInt().toString(), minimum = state.ride!!.odometer, onDismiss = { dialog = null }) { value ->
                    commit(state.copy(ride = state.ride!!.copy(odometer = value))) { if (it) dialog = null }
                }
                "fuel" -> FuelDialog(state, onDismiss = { dialog = null }, onSave = ::commit, onHistory = { history("fuel") })
                "editFuel" -> selectedLog?.let { log -> FuelDialog(state, onDismiss = { dialog = null }, onSave = ::commit, onHistory = { history("fuel") }, existing = log) }
                "service" -> ServiceDialog(state, selectedRule, onDismiss = { dialog = null }, onSave = ::commit,
                    onEdit = { selectedRule = it; dialog = "schedule" }, onHistory = { history("service") })
                "editService" -> selectedLog?.let { log -> ServiceDialog(state, null, onDismiss = { dialog = null }, onSave = ::commit,
                    onEdit = { selectedRule = it; dialog = "schedule" }, onHistory = { history("service") }, existing = log) }
                "components" -> ComponentsList(state, onDismiss = { dialog = null }, onSelect = { selectedRule = it; dialog = "service" },
                    onAdd = { selectedRule = null; dialog = "schedule" })
                "schedule" -> ScheduleDialog(state, selectedRule, onDismiss = { dialog = null }, onSave = ::commit)
                "documentCapture" -> DocumentCaptureFlow(state, selectedDocument, onDismiss = { dialog = null }, onManual = { manualDocumentType = it; dialog = "document" }, onSave = ::commit)
                "document" -> DocumentEditFlow(state, selectedDocument, manualDocumentType, onDismiss = { dialog = null }, onSave = ::commit)
                "detail", "inspectDetail" -> selectedDocument?.let { document -> VaultTheme(brand.id, if (dialog == "inspectDetail") Appearance.LIGHT else state.appearance, typography) { OriginalDocumentDetail(document, inspection = dialog == "inspectDetail",
                    onDismiss = { dialog = null }, onEdit = { dialog = "document" }, onScan = { dialog = "documentCapture" },
                    onDelete = { deleteId = document.id; deleteKind = "document"; dialog = "delete" }, onInspectionList = { dialog = "inspection" }, onInspect = { dialog = "inspectDetail" },
                    onRecordFee = { dialog = "documentFee" }) } }
                "documentFee" -> selectedDocument?.let { document -> DocumentFeeDialog(state, document, onDismiss = { dialog = "detail" }, onSave = ::commit) }
                "inspection" -> DocumentInspection(state.documents, onDismiss = { dialog = null }, onDocument = { selectedDocumentId = it.id; dialog = "inspectDetail" })
                "delete" -> {
                    var error by remember { mutableStateOf<String?>(null) }
                    Sheet("Remove this entry?", onDismiss = { dialog = null }) {
                        Text("This cannot be undone.", color = TextSecondary)
                        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
                        Primary("Delete record", "close") {
                            runCatching {
                                if (deleteKind == "document") state.copy(documents = state.documents.filterNot { it.id == deleteId })
                                else state.deleteLog(state.logs.first { it.id == deleteId })
                            }.onSuccess { next -> commit(next) { if (it) dialog = null else error = storageError } }.onFailure { error = it.message }
                        }
                    }
                }
            }
            SnackbarHost(snackbars, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp))
            if (saving) Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
                Surface(shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Text("Saving records…") } }
            }
        }
    }
}



@Composable
private fun ColumnScope.Garage(state: VaultState, brand: BrandWorld, onAppearance: () -> Unit, onOdometer: () -> Unit, onFuel: () -> Unit, onRule: (ComponentRule) -> Unit, onComponents: () -> Unit, onChangeBike: () -> Unit, onFuelHistory: () -> Unit, onVault: () -> Unit) {
    val ride = state.ride!!
    Top("আমার গ্যারেজ", "ডিভাইসে সংরক্ষিত", onAppearance)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { Eyebrow("আপনার বাইকের সঙ্গী"); Text(ride.displayName, fontFamily = FontFamily(Font(Res.font.barlow_condensed)), fontSize = if (ride.displayName.length > 14) 29.sp else 38.sp, fontWeight = FontWeight.SemiBold, lineHeight = 42.sp); val edition = if (ride.edition == "Owner-defined setup") "নিজের সেটআপ · Owner-defined" else ride.edition; Text("$edition · ${ride.year}" + if (ride.registration.isBlank()) "" else " · ${ride.registration}", color = TextSecondary, fontSize = 12.sp) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onChangeBike, modifier = Modifier.semantics { contentDescription = "বাইক বদলান" }) { Image(painterResource(brand.logo()), brand.name, Modifier.size(44.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)) }
            Text("বাইক বদলান", color = TextSecondary, fontSize = 10.sp)
        }
    }
    BikePhoto(ride)
    Row(Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(14.dp)).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).clickable(onClick = onOdometer), verticalArrangement = Arrangement.spacedBy(6.dp)) { Eyebrow("ওডোমিটার · Odometer", TextSecondary); Row(verticalAlignment = Alignment.Bottom) { Text(grouped(ride.odometer.toInt()), fontSize = 29.sp, fontWeight = FontWeight.Bold); Text(" km", color = TextSecondary, fontSize = 11.sp) }; Text("ছুঁয়ে আপডেট করুন", color = TextSecondary, fontSize = 10.sp) }
        Box(Modifier.width(1.dp).height(64.dp).background(BorderColor))
        Column(Modifier.weight(1f).clickable(onClick = onFuelHistory), verticalArrangement = Arrangement.spacedBy(6.dp)) { Eyebrow("জ্বালানি · Fuel economy", TextSecondary); Row(verticalAlignment = Alignment.Bottom) { Text(state.economy?.let { oneDecimal(it) } ?: "--", fontSize = 29.sp, fontWeight = FontWeight.Bold); Text(" km/L", color = TextSecondary, fontSize = 11.sp) }; Text("সম্পূর্ণ ট্যাংক ধরে গড়", color = TextSecondary, fontSize = 10.sp) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("যন্ত্রাংশের অবস্থা", fontWeight = FontWeight.SemiBold); TextButton(onClick = onComponents, contentPadding = PaddingValues(0.dp)) { Text("সব যন্ত্রাংশ ↗", color = TextSecondary, fontSize = 11.sp) } }
    if (state.rules.isEmpty()) {
        Panel { Text("সার্ভিসের সময়সূচি", fontWeight = FontWeight.Bold); Text("ম্যানুয়াল বা ওয়ার্কশপের পরামর্শে সময় ঠিক করুন।", color = TextSecondary, fontSize = 13.sp) }
    } else {
        val calculations = CalculateWearUseCase().executeAll(state.rules, ride.odometer, today().toEpochDays().toLong())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.rules.take(3).forEachIndexed { index, rule ->
                val calc = calculations[index]
                Gauge(componentShortLabel(rule), (100 - calc.wearPercentage).coerceIn(0, 100), Modifier.weight(1f)) { onRule(rule) }
            }
        }
        val priority = calculations.maxBy { it.wearPercentage }
        val priorityRule = state.rules.first { it.id == priority.componentId }
        Panel(onClick = { onRule(priorityRule) }, tint = WarningColor.copy(alpha = .06f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glyph("service", WarningColor)
                Column(Modifier.weight(1f)) { Text("${componentShortLabel(priorityRule)} · " + if (priority.wearPercentage >= 100) "সময় পেরিয়েছে" else if (priority.wearPercentage >= 75) "শিগগির সার্ভিস" else "ঠিক আছে", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(if (priorityRule.intervalKm > 0) "${grouped(priority.remainingDistanceKm.toInt())} km" else null, if (priorityRule.intervalDays > 0) "${bengaliDigits(priority.remainingDays)} দিন" else null).joinToString(" / ") + " বাকি", color = TextSecondary, fontSize = 11.sp) }
                Glyph("arrow", WarningColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Primary(if (state.rules.any { it.name.contains("Chain", ignoreCase = true) }) "চেইনের যত্ন লিখুন" else "সার্ভিস যোগ করুন", "service", Modifier.weight(1f)) { onRule(state.rules.firstOrNull { it.name.contains("Chain", ignoreCase = true) } ?: state.rules.first()) }
            OutlinedButton(onClick = onFuel, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) { Glyph("fuel"); Spacer(Modifier.width(8.dp)); Text("তেল ভরা লিখুন", color = TextPrimary, fontSize = 12.sp) }
        }
        if (state.rules.size > 3) Eyebrow("নিজের সময়সূচি", TextSecondary)
        state.rules.drop(3).forEach { rule -> Panel(onClick = { onRule(rule) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(componentLabel(rule), fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text("${grouped(rule.intervalKm.toInt())} km · ${bengaliDigits(rule.intervalDays)} দিন", fontSize = 11.sp, color = TextSecondary) }; Glyph("arrow", TextSecondary)
            }
        } }
    }
    if (state.rules.isEmpty()) {
        Primary("যন্ত্রাংশ যোগ করুন", "add", onClick = onComponents)
        Primary("তেল ভরা লিখুন", "fuel", onClick = onFuel)
    }
    val renewals = state.documents.count { (it.daysRemaining ?: Int.MAX_VALUE) <= RENEWAL_WARNING_DAYS }
    if (renewals > 0) Panel(onClick = onVault, tint = WarningColor.copy(alpha = .06f)) { Text("${bengaliDigits(renewals)}টি কাগজের মেয়াদ দেখুন", color = WarningColor, fontSize = 13.sp); Text("কাগজপত্র খুলুন ↗", color = TextSecondary, fontSize = 11.sp) }
}

@Composable
private fun BikePhoto(ride: Ride) {
    val uriHandler = LocalUriHandler.current
    Column {
        val model = ride.selectedModel
        if (model != null) ModelPhoto(model, Modifier.fillMaxWidth().height(190.dp))
        else Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFEFEFED)), contentAlignment = Alignment.Center) {
            Text("আপনার মডেল · ছবি যোগ করা হয়নি", color = Color(0xFF535B65), fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ride.displayName + " · নমুনা ছবি", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(top = 6.dp))
            model?.let { TextButton(onClick = { uriHandler.openUri(it.source) }, contentPadding = PaddingValues(4.dp)) { Text("ছবির সূত্র ↗", color = TextSecondary, fontSize = 12.sp) } }
        }
    }
}

@Composable
private fun Gauge(label: String, life: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = if (life == 0) ErrorColor else if (life <= 25) WarningColor else HealthyColor
    Column(modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp).semantics { contentDescription = "$label, ${bengaliDigits(life)} শতাংশ বাকি" }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
            val border = BorderColor
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(border, style = Stroke(5.dp.toPx()), radius = size.minDimension / 2 - 8.dp.toPx())
                drawArc(color, -90f, life * 3.6f, false, topLeft = Offset(8.dp.toPx(), 8.dp.toPx()), size = Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            }
            Row(verticalAlignment = Alignment.Bottom) { Text("$life", fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("%", fontSize = 10.sp, color = TextSecondary) }
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(if (life == 0) "সার্ভিস বাকি" else if (life <= 25) "শিগগির সার্ভিস" else "ভালো আছে", color = color, fontSize = 12.sp)
    }
}

@Composable
private fun ColumnScope.Vault(documents: List<VaultDocument>, onAppearance: () -> Unit, onAdd: () -> Unit, onDocument: (VaultDocument) -> Unit, onInspection: () -> Unit) = DocumentWallet(documents, onAppearance, onAdd, onDocument, onInspection)

@Composable
internal fun Top(title: String, caption: String, onAppearance: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(5.dp).background(HealthyColor, CircleShape))
                Text(caption, color = TextSecondary, fontSize = 9.sp)
            }
        }
        AppearanceButton(onAppearance)
    }
}

@Composable
internal fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) = Text(text.uppercase(), color = color, fontSize = 9.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)

@Composable
internal fun Heading(text: String) = Text(text, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp, letterSpacing = (-.8).sp)

@Composable
internal fun Panel(onClick: (() -> Unit)? = null, tint: Color = SurfaceColor, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tint).border(1.dp, BorderColor, RoundedCornerShape(14.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
internal fun Primary(text: String, icon: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) { Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.width(10.dp)); Glyph(icon, if (enabled) MaterialTheme.colorScheme.onPrimary else TextSecondary) }
}

@Composable
internal fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(label, fontSize = 12.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}

@Composable
internal fun Sheet(title: String, onDismiss: () -> Unit, light: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val brandId = LocalVaultBrandId.current
    val parentDark = LocalVaultPalette.current.dark
    val appearance = if (light || !parentDark) Appearance.LIGHT else Appearance.DARK
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PlatformAppearance(parentDark)
        VaultTheme(brandId, appearance) {
            Column(Modifier.safeDrawingPadding().imePadding().padding(16.dp).widthIn(max = 500.dp).fillMaxWidth().heightIn(max = 650.dp).clip(RoundedCornerShape(22.dp)).background(SurfaceColor).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "বন্ধ করুন" }) { Glyph("close", TextPrimary) }
                }
                content()
            }
        }
    }
}

@Composable
private fun NumberDialog(title: String, label: String, initial: String, minimum: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val value = text.toDoubleOrNull()?.takeIf { it.isFinite() && it in minimum..9999999.0 }
    Sheet(title, onDismiss) { Field(label, text, { text = it }, true); Text("Reading must be at least ${minimum.toInt()} km.", color = TextSecondary, fontSize = 12.sp); Primary("Save reading", "check", enabled = value != null) { onSave(value!!) } }
}



internal fun oneDecimal(value: Double) = ((value * 10).roundToLong() / 10.0).toString()

internal fun grouped(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed()

@Composable
internal fun Glyph(name: String, color: Color = TextPrimary, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) {
        val scale = size.minDimension / 24f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(x1 * scale, y1 * scale), Offset(x2 * scale, y2 * scale), 1.6f * scale, StrokeCap.Round)
        fun path(vararg points: Pair<Float, Float>, closed: Boolean = false) {
            val p = Path(); points.forEachIndexed { index, (x, y) -> if (index == 0) p.moveTo(x * scale, y * scale) else p.lineTo(x * scale, y * scale) }; if (closed) p.close(); drawPath(p, color, style = Stroke(1.6f * scale, cap = StrokeCap.Round))
        }
        when (name) {
            "arrow" -> { line(4f, 12f, 20f, 12f); path(14f to 6f, 20f to 12f, 14f to 18f) }
            "check" -> path(4f to 12f, 9f to 17f, 20f to 6f)
            "add" -> { line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f) }
            "close" -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            "garage" -> { path(3f to 10f, 12f to 3f, 21f to 10f, 21f to 21f, 3f to 21f, closed = true); line(7f, 14f, 17f, 14f); line(7f, 18f, 17f, 18f) }
            "vault" -> { path(5f to 10f, 19f to 10f, 19f to 21f, 5f to 21f, closed = true); drawArc(color, 180f, 180f, false, Offset(8f * scale, 2f * scale), Size(8f * scale, 12f * scale), style = Stroke(1.6f * scale)); line(12f, 14f, 12f, 17f) }
            "fuel" -> { path(4f to 21f, 4f to 4f, 13f to 4f, 13f to 21f, closed = true); line(4f, 10f, 13f, 10f); path(13f to 12f, 17f to 12f, 17f to 19f, 21f to 19f, 21f to 8f, 18f to 5f) }
            "service" -> { path(6f to 3f, 6f to 8f, 11f to 11f, 16f to 8f, 16f to 3f); path(10f to 11f, 10f to 21f, 13f to 21f, 13f to 11f) }
            "history" -> { drawArc(color, -80f, 300f, false, Offset(4f * scale, 4f * scale), Size(16f * scale, 16f * scale), style = Stroke(1.6f * scale)); path(2f to 4f, 2f to 10f, 8f to 10f); path(12f to 7f, 12f to 12f, 16f to 14f) }
            "mark" -> { val p = Path(); p.moveTo(0f, 0f); p.lineTo(12f * scale, 5f * scale); p.lineTo(24f * scale, 0f); p.lineTo(24f * scale, 16f * scale); p.lineTo(12f * scale, 24f * scale); p.lineTo(0f, 16f * scale); p.close(); drawPath(p, color); path(5f to 7f, 12f to 12f, 19f to 7f) }
            else -> { path(5f to 3f, 15f to 3f, 20f to 8f, 20f to 21f, 5f to 21f, closed = true); line(9f, 12f, 16f, 12f); line(9f, 16f, 16f, 16f) }
        }
    }
}
