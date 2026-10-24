package com.motovault.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.storage.activeVaultSession
import dev.sky.design.*
import dev.sky.design.tokens.SkyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToLong

data class BrandWorld(val id: String, val name: String, val accent: Color, val tone: String)

val worlds = listOf(
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

enum class Tab(val id: String, val title: String, val icon: String) {
    GARAGE("garage", "গ্যারেজ", "fi_home.svg"),
    VAULT("vault", "কাগজপত্র", "fi_folder.svg"),
    HISTORY("history", "ইতিহাস", "fi_clock.svg"),
    ME("me", "আমি", "fi_user.svg")
}

data class ActiveSheet(
    val name: String,
    val props: Any? = null
)

fun bengaliDigits(value: Int): String = value.toString().map { digit ->
    if (digit in '0'..'9') ('০'.code + digit.code - '0'.code).toChar() else digit
}.joinToString("")

fun brandToneName(brandId: String): String = when (brandId) {
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

fun componentSuffix(rule: ComponentRule): String {
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

fun componentLabel(rule: ComponentRule): String {
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
    return bengaliName ?: rule.name
}

fun BrandWorld.logo(): DrawableResource = when (id) {
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
    // Refresh actual OS permission and committed requests when returning from system settings.
    rememberReminderAccess()
    val revision = snapshot.revision
    val storageError = snapshot.error
    val loading = snapshot.loading

    var tab by remember {
        mutableStateOf(
            when (LaunchConfig.initialTab?.lowercase()) {
                "vault" -> Tab.VAULT
                "history" -> Tab.HISTORY
                "me" -> Tab.ME
                else -> Tab.GARAGE
            }
        )
    }
    var editingRide by remember { mutableStateOf(LaunchConfig.initialOnboarding) }
    var activeSheet by remember {
        mutableStateOf(
            LaunchConfig.initialSheet?.let { ActiveSheet(it) }
        )
    }
    var currentToast by remember { mutableStateOf<ToastData?>(null) }

    val scope = rememberCoroutineScope()

    fun commit(next: VaultState, onDone: ((Boolean) -> Unit)? = null) {
        session.commit(next, revision) { ok ->
            if (scope.isActive) {
                onDone?.invoke(ok)
            }
        }
    }

    fun undo(savedRevision: Long) {
        session.undo(savedRevision) { ok ->
            val canRetry = session.snapshots.value.revision == savedRevision
            currentToast = if (ok) null else ToastData(
                id = "undo_failed_${Clock.System.now().toEpochMilliseconds()}",
                message = if (canRetry) "ফিরিয়ে আনা যায়নি। আবার চেষ্টা করুন।" else "আরেকটি পরিবর্তন হয়েছে। রেকর্ড দেখে নিন।",
                undoAction = if (canRetry) { { undo(savedRevision) } } else null
            )
        }
    }

    val reminderRequest by ReminderNavigation.requests.collectAsState()
    LaunchedEffect(reminderRequest, loading) {
        val payload = reminderRequest ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        val target = decodeReminderPayload(payload)?.let { reminderTarget(state, it) }
        if (target == null) currentToast = ToastData(message = "এই রিমাইন্ডারের রেকর্ড বদলেছে বা মুছে গেছে। তালিকা দেখে নিন।")
        else if (target.type == "document") {
            tab = Tab.VAULT
            activeSheet = ActiveSheet("docView", target.id)
        } else {
            tab = Tab.GARAGE
            activeSheet = ActiveSheet("service", target.id)
        }
        ReminderNavigation.handled(payload)
    }

    LaunchedEffect(LaunchConfig.initialSeed, loading) {
        if (LaunchConfig.initialSeed && !loading) {
            val seeded = VaultState.seedData(today())
            commit(seeded)
        }
    }

    val activeBrandId = state.brandOverride ?: state.ride?.brandId ?: "yamaha"

    VaultTheme(
        brandId = activeBrandId,
        appearance = state.appearance,
        reduceMotion = state.reduceMotion,
        textScale = state.textScale,
        digits = state.digits
    ) {
        PlatformAppearance(LocalVaultPalette.current.dark)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasColor)
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
            ) {
                storageError?.let {
                    Text(
                        text = it,
                        color = ErrorColor,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        fontSize = 13.sp
                    )
                }

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandColor)
                    }
                } else if (state.ride == null || editingRide) {
                    OnboardingScreen(
                        onDismiss = if (state.ride != null) { { editingRide = false } } else null,
                        onComplete = { brandId, modelId, name, odometer, knowsLastService ->
                            val today = today()
                            val seeded = VaultState.seedData(today)
                            val rules = seeded.rules.map { rule ->
                                rule.copy(
                                    lastServicedOdometerKm = odometer,
                                    lastServicedEpochDays = today.toEpochDays().toLong(),
                                    baselineCertainty = if (knowsLastService) com.motovault.app.domain.model.BaselineCertainty.OWNER_ESTIMATE else com.motovault.app.domain.model.BaselineCertainty.UNKNOWN
                                )
                            }
                            val ride = Ride(
                                brandId = brandId,
                                modelId = modelId,
                                year = today.year - 2,
                                odometer = odometer,
                                edition = name,
                                odometerConfirmedDate = today.toString()
                            )
                            val nextState = state.copy(
                                ride = ride,
                                rules = rules,
                                logs = emptyList(),
                                documents = state.documents
                            )
                            commit(nextState) { ok ->
                                if (ok) {
                                    editingRide = false
                                    tab = Tab.GARAGE
                                }
                            }
                        }
                    )
                } else {
                    // Screen pages
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                fadeIn(VaultMotion.pageTween()) togetherWith fadeOut(VaultMotion.pageTween())
                            },
                            label = "VaultScreenPage",
                            modifier = Modifier.fillMaxSize()
                        ) { destination ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                when (destination) {
                                    Tab.GARAGE -> GarageScreen(
                                        state = state,
                                        onOpenSheet = { name, payload -> activeSheet = ActiveSheet(name, payload) },
                                        onSwitchTab = { nextTab -> tab = nextTab },
                                        onOpenServiceForItem = { itemKey -> activeSheet = ActiveSheet("service", itemKey) }
                                    )
                                    Tab.VAULT -> VaultScreen(
                                        state = state,
                                        onOpenAddDoc = { activeSheet = ActiveSheet("docAdd") },
                                        onOpenDocView = { doc -> activeSheet = ActiveSheet("docView", doc.id) },
                                        onToggleDocReminders = { enabled -> commit(state.copy(docReminders = enabled)) },
                                        onReminderSettings = { activeSheet = ActiveSheet("reminders") }
                                    )
                                    Tab.HISTORY -> HistoryScreen(
                                        state = state,
                                        onDeleteLog = { log ->
                                            val next = runCatching { state.deleteLog(log) }.getOrNull()
                                            if (next != null) {
                                                commit(next) { ok ->
                                                    if (ok) {
                                                        currentToast = ToastData(
                                                            id = "del_${log.id}",
                                                            message = "রেকর্ড মোছা হয়েছে",
                                                            undoAction = {
                                                                undo(revision + 1)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Tab.ME -> MeScreen(
                                        state = state,
                                        onUpdateState = { next -> commit(next) },
                                        onStartOnboarding = { editingRide = true },
                                        onResetDemo = {
                                            val seeded = VaultState.seedData(today())
                                            commit(seeded)
                                        },
                                        onOpenSheet = { name, payload -> activeSheet = ActiveSheet(name, payload) }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Navigation Bar with Center "+" Button
                    VaultBottomNavigation(
                        selected = tab,
                        onSelect = { tab = it },
                        onQuickAdd = { activeSheet = ActiveSheet("quickAdd") }
                    )
                }
            }

            // Floating Toast notification with Undo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            ) {
                ToastLayer(
                    toast = currentToast,
                    onDismiss = { currentToast = null }
                )
            }

            // Sheets Orchestration
            if (!loading && (state.ride != null || activeSheet?.name == "paste" || activeSheet?.name == "quickAdd")) {
                activeSheet?.let { sheet ->
                when (sheet.name) {
                    "gaugeDetail" -> GaugeDetailSheet(state, sheet.props as? String,
                        onDismiss = { activeSheet = null }, onOpen = { name, id ->
                            activeSheet = if (name == "serviceReminder") ActiveSheet("reminders", "service" to id)
                                else ActiveSheet(name, id)
                        })
                    "reminders" -> {
                        val target = sheet.props as? Pair<*, *>
                        ReminderSettingsSheet(state, target?.first as? String, target?.second as? String,
                            onDismiss = { activeSheet = null },
                            onOpen = { type, id -> activeSheet = ActiveSheet("reminders", type to id) },
                            onSave = { next, done -> commit(next, done) })
                    }
                    "quickAdd" -> QuickAddSheet(
                        onDismiss = { activeSheet = null },
                        onSelectFlow = { flow -> activeSheet = ActiveSheet(flow) }
                    )
                    "fuel" -> FuelSheet(
                        state = state,
                        initialAmount = (sheet.props as? Number)?.toDouble(),
                        onDismiss = { activeSheet = null },
                        onSave = { nextState, msg, onDone ->
                            commit(nextState) { ok ->
                                if (ok) {
                                    currentToast = ToastData(
                                        id = "fuel_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            undo(revision + 1)
                                        }
                                    )
                                    onDone()
                                    activeSheet = null
                                }
                            }
                        }
                    )
                    "service" -> {
                        val itemKey = sheet.props as? String
                        val parsedRows = (sheet.props as? List<*>)?.filterIsInstance<ParsedLine>()
                        ServiceSheet(
                            state = state,
                            initialItemKey = itemKey,
                            initialRows = parsedRows,
                            onDismiss = { activeSheet = null },
                            onSave = { nextState, msg, onDone ->
                                commit(nextState) { ok ->
                                    if (ok) {
                                        currentToast = ToastData(
                                            id = "svc_${Clock.System.now().toEpochMilliseconds()}",
                                            message = msg,
                                            undoAction = {
                                                undo(revision + 1)
                                            }
                                        )
                                        onDone()
                                        activeSheet = null
                                    }
                                }
                            }
                        )
                    }
                    "odometer" -> OdometerSheet(
                        state = state,
                        onDismiss = { activeSheet = null },
                        onSave = { nextState, msg, onDone ->
                            commit(nextState) { ok ->
                                if (ok) {
                                    currentToast = ToastData(
                                        id = "odo_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            undo(revision + 1)
                                        }
                                    )
                                    onDone()
                                    activeSheet = null
                                }
                            }
                        }
                    )
                    "docAdd" -> DocAddSheet(
                        state = state,
                        onDismiss = { activeSheet = null },
                        onSave = { nextState, msg, onDone ->
                            commit(nextState) { ok ->
                                if (ok) {
                                    currentToast = ToastData(
                                        id = "doc_${Clock.System.now().toEpochMilliseconds()}",
                                        message = msg,
                                        undoAction = {
                                            undo(revision + 1)
                                        }
                                    )
                                    onDone()
                                    activeSheet = null
                                }
                            }
                        }
                    )
                    "docView" -> {
                        val docId = sheet.props as? String
                        val doc = state.documents.firstOrNull { it.id == docId }
                        if (doc != null) {
                            DocViewSheet(
                                doc = doc,
                                onDismiss = { activeSheet = null },
                                onUpdateReminder = { enabled ->
                                    val updated = state.documents.map { if (it.id == doc.id) it.copy(reminder = enabled) else it }
                                    commit(state.copy(documents = updated))
                                },
                                onReminderSettings = { activeSheet = ActiveSheet("reminders", "document" to doc.id) },
                                warningDays = doc.reminderWarningDays ?: state.reminderPolicy.warningDays
                            )
                        }
                    }
                    "paste" -> PasteSheet(
                        onDismiss = { activeSheet = null },
                        onSelectFlow = { flow, payload ->
                            activeSheet = ActiveSheet(flow, payload)
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun VaultBottomNavigation(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onQuickAdd: () -> Unit
) {
    val haptic = currentHaptic
    val brand = BrandColor
    val onBrand = OnBrandColor
    val surface = SurfaceColor
    val line = BorderColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
    ) {
        HorizontalDivider(color = line, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: GARAGE
            BottomNavItem(
                tab = Tab.GARAGE,
                isSelected = selected == Tab.GARAGE,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.GARAGE)
                },
                modifier = Modifier.weight(1f)
            )

            // Tab 2: VAULT
            BottomNavItem(
                tab = Tab.VAULT,
                isSelected = selected == Tab.VAULT,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.VAULT)
                },
                modifier = Modifier.weight(1f)
            )

            // Center slot: Tab Plus button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                var pressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.92f else 1f,
                    animationSpec = VaultMotion.pressSpring(),
                    label = "plusScale"
                )

                Box(
                    modifier = Modifier
                        .offset(y = (-14).dp)
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(surface)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(brand)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.perform(HapticKind.SUCCESS)
                            onQuickAdd()
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = "নতুন রেকর্ড যোগ করুন"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SkySvgIcon(
                        path = "feather_icon/fi_plus.svg",
                        description = null,
                        modifier = Modifier.size(28.dp),
                        tint = onBrand
                    )
                }
            }

            // Tab 3: HISTORY
            BottomNavItem(
                tab = Tab.HISTORY,
                isSelected = selected == Tab.HISTORY,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.HISTORY)
                },
                modifier = Modifier.weight(1f)
            )

            // Tab 4: ME
            BottomNavItem(
                tab = Tab.ME,
                isSelected = selected == Tab.ME,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.ME)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) BrandColor else TextTertiary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SkySvgIcon(
            path = "feather_icon/${tab.icon}",
            description = tab.title,
            modifier = Modifier.size(24.dp),
            tint = color
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
            lineHeight = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        // Marker
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 3.dp)
                .background(if (isSelected) BrandColor else Color.Transparent, RoundedCornerShape(2.dp))
        )
    }
}

// =========================================================================
// Legacy UI Utilities (Retained for binary and backward compatibility)
// =========================================================================

fun oneDecimal(value: Double): String = ((value * 10).roundToLong() / 10.0).toString()

fun grouped(value: Int): String = value.toString().reversed().chunked(3).joinToString(",").reversed()

@Composable
fun Top(title: String, caption: String, onAppearance: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(5.dp).background(HealthyColor, CircleShape))
                Text(caption, color = TextSecondary, fontSize = 12.sp)
            }
        }
        AppearanceButton(onAppearance)
    }
}

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) =
    Text(text.uppercase(), color = color, fontSize = 12.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)

@Composable
fun Heading(text: String) = SkyGradientHeading(text)

@Composable
fun Panel(onClick: (() -> Unit)? = null, tint: Color = SurfaceColor, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(SkyTokens.Radii.menu))
            .background(tint)
            .border(1.dp, BorderColor, RoundedCornerShape(SkyTokens.Radii.menu))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun Primary(text: String, icon: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    SkyButton(text, onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled)
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, style = SkyTheme.typography.label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            singleLine = true,
            textStyle = SkyTheme.typography.body,
            shape = RoundedCornerShape(SkyTokens.Radii.field),
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SkyTheme.colors.fieldBorder,
                focusedBorderColor = SkyTheme.colors.accent,
                unfocusedContainerColor = SkyTheme.colors.input,
                focusedContainerColor = SkyTheme.colors.input
            )
        )
    }
}

@Composable
fun Glyph(name: String, color: Color = TextPrimary, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) {
        val scale = size.minDimension / 24f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * scale, y1 * scale), Offset(x2 * scale, y2 * scale), 1.6f * scale, StrokeCap.Round)
        fun path(vararg points: Pair<Float, Float>, closed: Boolean = false) {
            val p = Path()
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) p.moveTo(x * scale, y * scale) else p.lineTo(x * scale, y * scale)
            }
            if (closed) p.close()
            drawPath(p, color, style = Stroke(1.6f * scale, cap = StrokeCap.Round))
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
