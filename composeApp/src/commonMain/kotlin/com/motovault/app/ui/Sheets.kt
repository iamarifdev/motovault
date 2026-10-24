package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.sky.design.SkySvgIcon
import dev.sky.design.tokens.SkyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

enum class SheetDetent {
    CONTENT,
    TALL
}

@Composable
fun Sheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    detent: SheetDetent = SheetDetent.CONTENT,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = currentHaptic
    val density = LocalDensity.current
    val reducedMotion = isReducedMotion
    var closing by remember { mutableStateOf(false) }
    val offsetY = remember { Animatable(if (reducedMotion) 0f else 600f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            offsetY.animateTo(0f, VaultMotion.sheetSpring())
        }
    }

    fun dismiss() {
        if (closing) return
        closing = true
        scope.launch {
            if (!reducedMotion) {
                offsetY.animateTo(600f, tween(VaultMotion.SheetOutMs, easing = VaultMotion.SkyCurve))
            }
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { dismiss() },
        properties = sheetDialogProperties()
    ) {
        PlatformAppearance(LocalVaultPalette.current.dark)
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val maxH = maxHeight - with(density) { WindowInsets.safeDrawing.getTop(this).toDp() } - 40.dp
            val sheetHeight = if (detent == SheetDetent.TALL) maxH else maxH

            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { dismiss() }
            )

            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetHeight)
                    .then(if (detent == SheetDetent.TALL) Modifier.fillMaxHeight(0.92f) else Modifier)
                    .offset(y = offsetY.value.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (offsetY.value > 120f) {
                                    haptic.perform(HapticKind.TICK)
                                    dismiss()
                                } else {
                                    scope.launch { offsetY.animateTo(0f, VaultMotion.pressSpring(reducedMotion)) }
                                }
                            },
                            onDrag = { _, dragAmount ->
                                if (dragAmount.y > 0 || offsetY.value > 0) {
                                    scope.launch { offsetY.snapTo((offsetY.value + dragAmount.y * 0.7f).coerceAtLeast(0f)) }
                                }
                            }
                        )
                    },
                color = SurfaceColor,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    )
                ) {
                    // Grab Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(BorderStrongColor.copy(alpha = 0.6f))
                        )
                    }

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        VaultIconButton(
                            icon = "fi_x.svg",
                            label = "বন্ধ করুন",
                            onClick = { dismiss() }
                        )
                    }

                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content
                    )

                    // Pinned Footer
                    if (footer != null) {
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceColor)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            footer()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSelectFlow: (String) -> Unit
) {
    val haptic = currentHaptic
    Sheet(
        title = "কী যোগ করবেন?",
        onDismiss = onDismiss
    ) {
        val options = listOf(
            Triple("fi_droplet.svg", "তেল ভরা", "টাকা লিখুন, লিটার নিজেই হবে" to "fuel"),
            Triple("fi_tool.svg", "সার্ভিস", "এক ট্যাপে পার্টস যোগ" to "service"),
            Triple("fi_activity.svg", "ওডোমিটার", "আনুমানিক কিমি ঠিক করুন" to "odometer"),
            Triple("fi_file-text.svg", "কাগজ", "স্ক্যান করে মেয়াদ রাখুন" to "docAdd")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (icon, label, subToGo) ->
                        val (sub, flow) = subToGo
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceColor2)
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    onDismiss()
                                    onSelectFlow(flow)
                                }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BrandColor),
                                contentAlignment = Alignment.Center
                            ) {
                                SkySvgIcon(
                                    path = "feather_icon/$icon",
                                    description = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = OnBrandColor
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = sub, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // Paste shortcut button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, BrandLineColor, RoundedCornerShape(16.dp))
                    .background(BrandTintColor.copy(alpha = 0.3f))
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onDismiss()
                        onSelectFlow("paste")
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkySvgIcon(
                    path = "feather_icon/fi_zap.svg",
                    description = null,
                    modifier = Modifier.size(22.dp),
                    tint = BrandColor
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "লিখে বা পেস্ট করে সাজান", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = "মেকানিকের মেসেজ, বিকাশ বা নগদের টেক্সট", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun FuelSheet(
    state: VaultState,
    initialAmount: Double? = null,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val ride = state.ride ?: return
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    val estimate = remember(ride, state.logs) { WearCalculator.estimateOdometer(ride, state.logs, today) }
    val lastOdo = remember { state.logs.filter { it.isFuel }.maxOfOrNull { it.odometer } ?: 0.0 }
    val lastOctanePrice = state.lastPrices["octane"] ?: 120.0

    var fuelType by remember { mutableStateOf("octane") }
    var mode by remember { mutableStateOf("taka") } // "taka" or "litre"
    var fullTank by remember { mutableStateOf(true) }
    var dateKey by remember { mutableStateOf("today") } // "today", "yesterday", "custom"
    var customDate by remember { mutableStateOf(today.toString()) }
    var place by remember { mutableStateOf("") }
    var odoTouched by remember { mutableStateOf(false) }

    val entry = rememberNumberEntry(
        initialValues = mapOf(
            "amount" to (initialAmount?.let { kotlin.math.round(it).toInt().toString() } ?: ""),
            "litres" to "",
            "price" to kotlin.math.round(lastOctanePrice).toInt().toString(),
            "odo" to kotlin.math.round(estimate).toInt().toString()
        ),
        firstField = "amount"
    )

    val price = parseNum(entry.values["price"] ?: "") ?: lastOctanePrice
    val amountVal = parseNum(entry.values["amount"] ?: "") ?: 0.0
    val litresVal = parseNum(entry.values["litres"] ?: "") ?: 0.0
    val odoVal = parseNum(entry.values["odo"] ?: "") ?: 0.0

    val calculatedTaka = if (mode == "taka") amountVal else litresVal * price
    val calculatedLitres = if (mode == "taka") (if (price > 0) amountVal / price else 0.0) else litresVal

    val date = when (dateKey) {
        "yesterday" -> LocalDate.fromEpochDays(today.toEpochDays() - 1)
        "custom" -> runCatching { LocalDate.parse(customDate) }.getOrDefault(today)
        else -> today
    }

    val odoError = if (odoVal <= lastOdo && lastOdo > 0) "শেষ জ্বালানির কিমি (${f.n(lastOdo.toLong())}) এর চেয়ে বেশি দিন" else null
    val reason = when {
        calculatedTaka <= 0 -> if (mode == "taka") "টাকার পরিমাণ লিখুন" else "লিটার লিখুন"
        price <= 0 -> "প্রতি লিটারের দাম লিখুন"
        odoError != null -> "কিমি ঠিক করুন"
        else -> null
    }

    Sheet(
        title = "জ্বালানি",
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = reason ?: "${f.money(calculatedTaka)} সেভ করুন",
                onClick = {
                    if (reason != null) {
                        haptic.perform(HapticKind.ERROR)
                    } else {
                        val newLog = RideLog(
                            title = "Fuel fill-up",
                            detail = "${f.liters(calculatedLitres)} · ${if (fullTank) "Full tank" else "Partial fill"}",
                            date = date.toString(),
                            odometer = odoVal,
                            kind = "fuel",
                            liters = calculatedLitres,
                            fullTank = fullTank,
                            cost = calculatedTaka,
                            fuelType = fuelType,
                            fuelPrice = price,
                            locationLabel = place.trim()
                        )
                        val updatedPlaces = if (place.isNotBlank()) listOf(place.trim()) + state.places.filterNot { it == place.trim() } else state.places
                        val updatedPrices = state.lastPrices + (fuelType to price)
                        val next = state.copy(
                            ride = ride.confirmedMileage(odoVal, date),
                            logs = listOf(newLog) + state.logs,
                            places = updatedPlaces.take(6),
                            lastPrices = updatedPrices
                        )
                        val toastMsg = "${f.money(calculatedTaka)} · ${f.liters(calculatedLitres)} যোগ হয়েছে"
                        onSave(next, toastMsg) {
                            // Undo action: restore previous state
                        }
                    }
                },
                full = true,
                enabled = reason == null
            )
            VaultKeypad(onKey = entry::press)
        }
    ) {
        // Amount Hero Card
        val activeField = entry.activeField
        val isAmountActive = activeField == "amount" || activeField == "litres"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isAmountActive) BrandTintColor else SurfaceColor2)
                .border(2.dp, if (isAmountActive) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                .clickable { entry.setActive(if (mode == "taka") "amount" else "litres") }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (mode == "taka") "কত টাকার তেল?" else "কত লিটার?",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (mode == "taka") {
                        Text("৳", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    }
                    val displayText = if (mode == "taka") entry.values["amount"].orEmpty() else entry.values["litres"].orEmpty()
                    Text(
                        text = if (displayText.isEmpty()) f.d("0") else f.d(formatLakh(displayText.toDoubleOrNull() ?: 0.0)),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (displayText.isEmpty()) TextTertiary else TextPrimary
                    )
                    if (mode == "litre") {
                        Text("লিটার", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (mode == "taka") "≈ ${f.liters(calculatedLitres)}" else "≈ ${f.money(calculatedTaka)}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    // Editable price pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (activeField == "price") BrandTintColor else SurfaceColor)
                            .border(1.5.dp, if (activeField == "price") BrandColor else BorderColor, RoundedCornerShape(999.dp))
                            .clickable { entry.setActive("price") }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${f.money(price)}/${if (f.digits == Digits.BN) "লি" else "L"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        SkySvgIcon(
                            path = "feather_icon/fi_edit-2.svg",
                            description = null,
                            modifier = Modifier.size(12.dp),
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Mode & Fuel chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultChip(text = "টাকা ধরে", active = mode == "taka", onClick = { mode = "taka"; entry.setActive("amount") })
            VaultChip(text = "লিটার ধরে", active = mode == "litre", onClick = { mode = "litre"; entry.setActive("litres") })
            Spacer(Modifier.width(4.dp))
            Box(Modifier.width(1.dp).height(24.dp).background(BorderColor))
            Spacer(Modifier.width(4.dp))
            VaultChip(text = "অকটেন", active = fuelType == "octane", onClick = {
                fuelType = "octane"
                entry.set("price", kotlin.math.round(state.lastPrices["octane"] ?: 120.0).toInt().toString())
            })
            VaultChip(text = "পেট্রোল", active = fuelType == "petrol", onClick = {
                fuelType = "petrol"
                entry.set("price", kotlin.math.round(state.lastPrices["petrol"] ?: 115.0).toInt().toString())
            })
        }

        // Full tank & date chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultChip(text = if (fullTank) "ট্যাংক পূর্ণ" else "আংশিক ভরেছি", active = fullTank, onClick = { fullTank = !fullTank })
            VaultChip(text = "আজ", active = dateKey == "today", onClick = { dateKey = "today" })
            VaultChip(text = "গতকাল", active = dateKey == "yesterday", onClick = { dateKey = "yesterday" })
            VaultChip(text = "অন্য দিন", active = dateKey == "custom", onClick = { dateKey = "custom" })
        }

        // Custom date field if custom selected
        AnimatedVisibility(visible = dateKey == "custom") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceColor2)
                    .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = customDate,
                    onValueChange = { customDate = it },
                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary)
                )
            }
        }

        // Odometer Row
        val isOdoActive = activeField == "odo"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isOdoActive) BrandTintColor else SurfaceColor)
                .border(2.dp, if (odoError != null) ErrorColor else if (isOdoActive) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                .clickable { entry.setActive("odo") }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkySvgIcon(
                        path = "feather_icon/fi_activity.svg",
                        description = null,
                        modifier = Modifier.size(20.dp),
                        tint = BrandColor
                    )
                    Column {
                        Text(text = "ওডোমিটার", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "${f.n(odoVal.toLong())} ${if (f.digits == Digits.BN) "কিমি" else "km"}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                if (!odoTouched) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(BrandTintColor)
                            .border(1.dp, BrandLineColor, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(text = "আনুমানিক", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }

        if (odoError != null) {
            Text(text = odoError, fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.SemiBold)
        }

        // Odometer bump chips
        AnimatedVisibility(visible = isOdoActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 50, 100).forEach { bump ->
                    VaultChip(
                        text = "+${f.d(bump.toString())}",
                        onClick = {
                            entry.set("odo", (odoVal + bump).toInt().toString())
                            odoTouched = true
                        }
                    )
                }
                VaultChip(
                    text = "আনুমানিক",
                    onClick = {
                        entry.set("odo", estimate.toInt().toString())
                        odoTouched = false
                    }
                )
            }
        }

        // Pump / Filling station input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkySvgIcon(
                path = "feather_icon/fi_map-pin.svg",
                description = null,
                modifier = Modifier.size(18.dp),
                tint = TextTertiary
            )
            state.places.take(2).forEach { p ->
                VaultChip(text = p, active = place == p, onClick = { place = if (place == p) "" else p })
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor2)
                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = place,
                onValueChange = { place = it },
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                cursorBrush = SolidColor(BrandColor),
                decorationBox = { innerTextField ->
                    if (place.isEmpty()) {
                        Text(text = "পাম্পের নাম (ঐচ্ছিক)", fontSize = 15.sp, color = TextTertiary)
                    }
                    innerTextField()
                }
            )
        }
    }
}

data class ServiceRowState(
    val uid: String,
    val itemKey: String,
    val name: String,
    var qty: Double = 1.0,
    var detail: String? = null
)

@Composable
fun ServiceSheet(
    state: VaultState,
    initialItemKey: String? = null,
    initialRows: List<ParsedLine>? = null,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val ride = state.ride ?: return
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    val estimate = remember(ride, state.logs) { WearCalculator.estimateOdometer(ride, state.logs, today) }
    val lastService = remember { state.logs.firstOrNull { it.kind == "service" && !it.isFuel } }
    val selectedRule = state.rules.firstOrNull { it.id == initialItemKey }
    val initialCatalogueKey = selectedRule?.let { BangladeshServiceCatalogue.itemForScheduleName(it.name).id } ?: initialItemKey
    var saveIssue by remember { mutableStateOf<String?>(null) }

    val rows = remember {
        mutableStateListOf<ServiceRowState>().apply {
            if (initialRows != null && initialRows.isNotEmpty()) {
                initialRows.forEachIndexed { i, p ->
                    add(ServiceRowState("$i-${p.item}", p.item, catalogItemOf(p.item)?.bn ?: p.item, p.qty))
                }
            } else if (initialCatalogueKey != null) {
                add(ServiceRowState("0-$initialCatalogueKey", initialCatalogueKey, catalogItemOf(initialCatalogueKey)?.bn ?: initialCatalogueKey, 1.0))
            }
        }
    }

    val entry = rememberNumberEntry(
        initialValues = buildMap {
            put("labour", "")
            put("misc", "")
            put("discount", "")
            put("odo", estimate.toInt().toString())
            rows.forEach { r ->
                val p = initialRows?.firstOrNull { it.item == r.itemKey }?.price
                    ?: state.lastItemPrices[r.itemKey]
                put("p:${r.uid}", p?.toInt()?.toString() ?: "")
            }
        },
        firstField = if (rows.isNotEmpty()) "p:${rows.first().uid}" else "odo"
    )

    var dateKey by remember { mutableStateOf("today") }
    var customDate by remember { mutableStateOf(today.toString()) }
    var workshop by remember { mutableStateOf("") }
    var smartOpen by remember { mutableStateOf(false) }
    var smartText by remember { mutableStateOf("") }
    var moreOpen by remember { mutableStateOf(false) }
    var keysOpen by remember { mutableStateOf(rows.isNotEmpty()) }

    val date = when (dateKey) {
        "yesterday" -> LocalDate.fromEpochDays(today.toEpochDays() - 1)
        "custom" -> runCatching { LocalDate.parse(customDate) }.getOrDefault(today)
        else -> today
    }

    val subtotal = rows.sumOf { r ->
        val price = parseNum(entry.values["p:${r.uid}"] ?: "") ?: 0.0
        price * r.qty
    }
    val labour = parseNum(entry.values["labour"] ?: "") ?: 0.0
    val misc = parseNum(entry.values["misc"] ?: "") ?: 0.0
    val discount = parseNum(entry.values["discount"] ?: "") ?: 0.0
    val total = maxOf(0.0, subtotal + labour + misc - discount)
    val unknownPrices = rows.count { parseNum(entry.values["p:${it.uid}"] ?: "") == null }
    val odoVal = parseNum(entry.values["odo"] ?: "") ?: 0.0

    val reason = when {
        rows.isEmpty() -> "কী কী করালেন বেছে নিন"
        odoVal <= 0 -> "কিমি লিখুন"
        else -> null
    }

    Sheet(
        title = "সার্ভিস",
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "মোট" + if (unknownPrices > 0) " · ${f.d(unknownPrices.toString())}টির দাম জানা নেই" else "",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = f.money(total),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            VaultButton(
                text = reason ?: "সার্ভিস সেভ করুন",
                onClick = {
                    if (reason != null) {
                        haptic.perform(HapticKind.ERROR)
                    } else {
                        val lines = rows.map { r ->
                            val p = parseNum(entry.values["p:${r.uid}"] ?: "")
                            ServiceLineItem(
                                catalogueId = r.itemKey,
                                customName = r.name,
                                action = "replace",
                                quantity = r.qty,
                                unitPricePoisha = p?.let { (it * 100).toInt() },
                                detail = r.detail
                            )
                        }
                        val newLog = RideLog(
                            title = "Service",
                            detail = if (rows.size > 1) "${rows[0].name} ও আরও ${f.d((rows.size - 1).toString())}টি" else rows[0].name,
                            date = date.toString(),
                            odometer = odoVal,
                            kind = "service",
                            serviceLines = lines,
                            serviceLabourPoisha = (labour * 100).toInt(),
                            serviceMiscellaneousPoisha = (misc * 100).toInt(),
                            serviceDiscountPoisha = (discount * 100).toInt(),
                            serviceTotalPoisha = (total * 100).toInt(),
                            cost = total,
                            workshop = workshop.trim()
                        )
                        val updatedPrices = state.lastItemPrices.toMutableMap()
                        rows.forEach { r ->
                            val p = parseNum(entry.values["p:${r.uid}"] ?: "")
                            if (p != null) updatedPrices[r.itemKey] = p
                        }
                        val updatedWorkshops = if (workshop.isNotBlank()) listOf(workshop.trim()) + state.workshops.filterNot { it == workshop.trim() } else state.workshops
                        val recorded = runCatching { state.saveQuickService(newLog, selectedRule?.id) }.getOrElse {
                            saveIssue = it.message ?: "সার্ভিসের তথ্য ঠিক করুন।"
                            return@VaultButton
                        }
                        val next = recorded.copy(
                            lastItemPrices = updatedPrices,
                            workshops = updatedWorkshops.take(6)
                        )
                        val toastMsg = "সার্ভিস যোগ হয়েছে · ${f.money(total)}"
                        onSave(next, toastMsg) {}
                    }
                },
                full = true,
                enabled = reason == null,
                icon = "fi_tool.svg"
            )
            AnimatedVisibility(visible = keysOpen) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth().clickable { keysOpen = false }.padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SkySvgIcon(
                            path = "feather_icon/fi_chevron-down.svg",
                            description = "লুকান",
                            modifier = Modifier.size(20.dp),
                            tint = TextTertiary
                        )
                    }
                    VaultKeypad(onKey = entry::press)
                }
            }
        }
    ) {
        // Top action row
        selectedRule?.let { Text("সময়সূচি: ${it.name}", fontSize = 14.sp, color = TextPrimary) }
        saveIssue?.let { Text(it, color = ErrorColor, fontSize = 14.sp) }
        Text("মিল থাকা সময়সূচির শেষ সার্ভিস এই কিমি ও তারিখে আপডেট হবে।", fontSize = 13.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (lastService != null) {
                VaultButton(
                    text = "আগের সার্ভিসের মতো",
                    kind = ButtonKind.SECONDARY,
                    size = ButtonSize.SM,
                    icon = "fi_copy.svg",
                    onClick = {
                        haptic.perform(HapticKind.SUCCESS)
                        rows.clear()
                        lastService.serviceLines.forEachIndexed { i, l ->
                            val itemKey = l.catalogueId ?: "custom"
                            val r = ServiceRowState("$i-$itemKey", itemKey, l.displayNameBn, l.quantity, l.detail)
                            rows.add(r)
                            l.unitPricePoisha?.let { entry.set("p:${r.uid}", (it / 100).toString()) }
                        }
                        keysOpen = false
                    }
                )
            }
            VaultButton(
                text = "লিখে বা বলে সাজান",
                kind = ButtonKind.SECONDARY,
                size = ButtonSize.SM,
                icon = "fi_zap.svg",
                onClick = { smartOpen = !smartOpen }
            )
        }

        // Smart Text / Voice Input
        AnimatedVisibility(visible = smartOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceColor2)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                ) {
                    BasicTextField(
                        value = smartText,
                        onValueChange = { smartText = it },
                        textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                        cursorBrush = SolidColor(BrandColor),
                        decorationBox = { inner ->
                            if (smartText.isEmpty()) {
                                Text("যেমন: ইঞ্জিন অয়েল ৯০০ টাকা, ফিল্টার ৩০০ টাকা", fontSize = 14.sp, color = TextTertiary)
                            }
                            inner()
                        }
                    )
                }
                val parsed = remember(smartText) { SmartParse.parseServiceText(smartText) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VaultButton(
                        text = "ডেমো ভয়েস",
                        kind = ButtonKind.SECONDARY,
                        size = ButtonSize.SM,
                        icon = "fi_mic.svg",
                        onClick = { smartText = "ইঞ্জিন অয়েল ৯০০ টাকা, অয়েল ফিল্টার ৩০০ টাকা" }
                    )
                    VaultButton(
                        text = "যোগ করুন (${f.d(parsed.lines.size.toString())})",
                        size = ButtonSize.SM,
                        enabled = parsed.lines.isNotEmpty(),
                        onClick = {
                            haptic.perform(HapticKind.SUCCESS)
                            val baseIndex = rows.size
                            parsed.lines.forEachIndexed { i, p ->
                                val r = ServiceRowState("${baseIndex + i}-${p.item}", p.item, catalogItemOf(p.item)?.bn ?: p.item, p.qty)
                                rows.add(r)
                                val pr = p.price ?: state.lastItemPrices[p.item]
                                if (pr != null) entry.set("p:${r.uid}", pr.toInt().toString())
                            }
                            smartOpen = false
                            smartText = ""
                        }
                    )
                }
            }
        }

        // Service items chips grid
        Text(text = "কী করালেন", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CATALOG_ITEMS.forEach { item ->
                val active = rows.any { it.itemKey == item.key }
                val lastPrice = state.lastItemPrices[item.key]
                VaultChip(
                    text = item.short,
                    active = active,
                    hint = lastPrice?.let { f.money(it) },
                    onClick = {
                        val existingIndex = rows.indexOfFirst { it.itemKey == item.key }
                        if (existingIndex >= 0) {
                            rows.removeAt(existingIndex)
                        } else {
                            val r = ServiceRowState("${rows.size}-${item.key}", item.key, item.bn, 1.0)
                            rows.add(r)
                            val p = state.lastItemPrices[item.key]
                            if (p != null) entry.set("p:${r.uid}", p.toInt().toString())
                            entry.setActive("p:${r.uid}")
                            keysOpen = true
                        }
                    }
                )
            }
        }

        // Added lines list
        if (rows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { r ->
                    val isPriceActive = entry.activeField == "p:${r.uid}" && keysOpen
                    val pVal = parseNum(entry.values["p:${r.uid}"] ?: "")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceColor2)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = r.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                            // Stepper
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(SurfaceColor)
                                    .border(1.dp, BorderColor, RoundedCornerShape(999.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(34.dp).clickable {
                                        if (r.qty > 1) {
                                            haptic.perform(HapticKind.TICK)
                                            r.qty -= 1
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Text(
                                    text = f.d(r.qty.toInt().toString()),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Box(
                                    modifier = Modifier.size(34.dp).clickable {
                                        if (r.qty < 20) {
                                            haptic.perform(HapticKind.TICK)
                                            r.qty += 1
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            // Price button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPriceActive) BrandTintColor else SurfaceColor)
                                    .border(2.dp, if (isPriceActive) BrandColor else BorderColor, RoundedCornerShape(10.dp))
                                    .clickable {
                                        entry.setActive("p:${r.uid}")
                                        keysOpen = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (pVal == null) "দাম?" else f.money(pVal * r.qty),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pVal == null) BrandColor else TextPrimary
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            VaultIconButton(
                                icon = "fi_trash-2.svg",
                                label = "বাদ দিন",
                                onClick = { rows.remove(r) }
                            )
                        }

                        // Viscosity chips if engine oil
                        if (r.itemKey == "engine_oil") {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VISCOSITY_GRADES.forEach { visc ->
                                    VaultChip(
                                        text = visc,
                                        active = r.detail == visc,
                                        onClick = { r.detail = if (r.detail == visc) null else visc }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Date selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultChip(text = "আজ", active = dateKey == "today", onClick = { dateKey = "today" })
            VaultChip(text = "গতকাল", active = dateKey == "yesterday", onClick = { dateKey = "yesterday" })
            VaultChip(text = "অন্য দিন", active = dateKey == "custom", onClick = { dateKey = "custom" })
        }

        // Odometer row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (entry.activeField == "odo" && keysOpen) BrandTintColor else SurfaceColor)
                .border(2.dp, if (entry.activeField == "odo" && keysOpen) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                .clickable {
                    entry.setActive("odo")
                    keysOpen = true
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkySvgIcon(
                        path = "feather_icon/fi_activity.svg",
                        description = null,
                        modifier = Modifier.size(20.dp),
                        tint = BrandColor
                    )
                    Column {
                        Text(text = "ওডোমিটার", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "${f.n(odoVal.toLong())} ${if (f.digits == Digits.BN) "কিমি" else "km"}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }

        // Workshop input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.workshops.take(2).forEach { w ->
                VaultChip(text = w, active = workshop == w, onClick = { workshop = if (workshop == w) "" else w })
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor2)
                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = workshop,
                onValueChange = { workshop = it },
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                cursorBrush = SolidColor(BrandColor),
                decorationBox = { inner ->
                    if (workshop.isEmpty()) {
                        Text("ওয়ার্কশপ বা মেকানিক (ঐচ্ছিক)", fontSize = 15.sp, color = TextTertiary)
                    }
                    inner()
                }
            )
        }

        // More toggle for Labour, Misc, Discount
        TextButton(
            onClick = { moreOpen = !moreOpen },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (moreOpen) "শ্রম ও অন্যান্য লুকান" else "শ্রম, অন্যান্য খরচ বা ছাড় আছে?",
                color = BrandColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        AnimatedVisibility(visible = moreOpen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("labour" to "শ্রম", "misc" to "অন্যান্য", "discount" to "ছাড়").forEach { (k, label) ->
                    val isActive = entry.activeField == k && keysOpen
                    val v = parseNum(entry.values[k] ?: "")
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) BrandTintColor else SurfaceColor)
                            .border(2.dp, if (isActive) BrandColor else BorderColor, RoundedCornerShape(12.dp))
                            .clickable {
                                entry.setActive(k)
                                keysOpen = true
                            }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(text = label, fontSize = 12.sp, color = TextSecondary)
                        Text(text = if (v != null) f.money(v) else "—", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun OdometerSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val ride = state.ride ?: return
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    val estimate = remember(ride, state.logs) { WearCalculator.estimateOdometer(ride, state.logs, today) }
    val entry = rememberNumberEntry(
        initialValues = mapOf("odo" to estimate.toInt().toString()),
        firstField = "odo"
    )
    var touched by remember { mutableStateOf(false) }
    var camScanning by remember { mutableStateOf(false) }
    var camDone by remember { mutableStateOf(false) }

    val odoVal = parseNum(entry.values["odo"] ?: "") ?: 0.0
    val badError = if (odoVal < ride.odometer) "বর্তমান কিমির (${f.n(ride.odometer.toLong())}) চেয়ে কম হতে পারে না" else null

    Sheet(
        title = "ওডোমিটার",
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = if (badError != null) "কিমি ঠিক করুন" else if (touched) "সেভ করুন" else "ঠিক আছে, সেভ করুন",
                onClick = {
                    if (badError != null) {
                        haptic.perform(HapticKind.ERROR)
                    } else {
                        val next = state.copy(ride = ride.copy(odometer = odoVal, odometerConfirmedDate = today.toString()))
                        val toastMsg = "ওডোমিটার ${f.km(odoVal)} করা হয়েছে"
                        onSave(next, toastMsg) {}
                    }
                },
                full = true,
                enabled = badError == null,
                icon = "fi_check.svg"
            )
            VaultKeypad(
                onKey = { k ->
                    touched = true
                    entry.press(k)
                },
                decimal = false
            )
        }
    ) {
        // Hero reading
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (touched) "আপনার লেখা কিমি" else "আজ আনুমানিক",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountUp(
                    value = odoVal,
                    format = { n -> f.d(formatLakh(n.toLong())) },
                    style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (f.digits == Digits.BN) "কিমি" else "km",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
            Text(
                text = "বর্তমান ওডোমিটার: ${f.km(ride.odometer)}",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        if (badError != null) {
            Text(text = badError, color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Bump chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10, 50, 100, 500).forEach { bump ->
                VaultChip(
                    text = "+${f.d(bump.toString())}",
                    onClick = {
                        entry.set("odo", (odoVal + bump).toInt().toString())
                        touched = true
                    }
                )
            }
            VaultChip(
                text = "আনুমানিক",
                onClick = {
                    entry.set("odo", estimate.toInt().toString())
                    touched = false
                }
            )
        }

        // LCD photo recognition simulated demo
        VaultButton(
            text = "ডিসপ্লের ছবি তুলে পড়ুন (ডেমো)",
            kind = ButtonKind.SECONDARY,
            icon = "fi_camera.svg",
            full = true,
            onClick = {
                camScanning = true
                camDone = false
            }
        )

        AnimatedVisibility(visible = camScanning) {
            LaunchedEffect(Unit) {
                delay(1700)
                camDone = true
                haptic.perform(HapticKind.SUCCESS)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D1210))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF9BB59A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (estimate.toInt() + 3).toString().padStart(6, '0'),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B2A1B)
                    )
                }
                if (!camDone) {
                    Text(text = "পড়া হচ্ছে… ফোন স্থির রাখুন", color = Color.White, fontSize = 14.sp)
                } else {
                    Text(text = "পড়া হয়েছে: ${f.n((estimate + 3).toLong())} ${if (f.digits == Digits.BN) "কিমি" else "km"}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VaultButton(
                            text = "ঠিক আছে",
                            size = ButtonSize.SM,
                            onClick = {
                                entry.set("odo", (estimate.toInt() + 3).toString())
                                touched = true
                                camScanning = false
                            }
                        )
                        VaultButton(
                            text = "নিজে লিখব",
                            kind = ButtonKind.SECONDARY,
                            size = ButtonSize.SM,
                            onClick = { camScanning = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocAddSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    var step by remember { mutableStateOf(1) } // 1: Type, 2: Scan, 3: Review, 4: Remind
    var selectedKind by remember { mutableStateOf("registration") }
    var number by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var scanFlash by remember { mutableStateOf(false) }

    Sheet(
        title = when (step) {
            1 -> "কোন কাগজ?"
            2 -> "কাগজ স্ক্যান"
            3 -> "তথ্য মিলিয়ে নিন"
            else -> "মনে করিয়ে দেব?"
        },
        onDismiss = onDismiss,
        detent = SheetDetent.TALL
    ) {
        when (step) {
            1 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DOCUMENT_KINDS.forEach { kindInfo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceColor2)
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    selectedKind = kindInfo.kind
                                    step = 2
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SkySvgIcon(
                                    path = "feather_icon/fi_file-text.svg",
                                    description = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = BrandColor
                                )
                                Column {
                                    Text(text = kindInfo.bn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(text = if (kindInfo.card) "সামনে ও পেছনের ছবি" else "পুরো পৃষ্ঠা", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                            SkySvgIcon(
                                path = "feather_icon/fi_chevron-right.svg",
                                description = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextTertiary
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            number = if (selectedKind == "licence") "DL-1990-123456" else "ঢাকা মেট্রো-হ ১১-২২৩৪"
                            holder = "মো. রাফিদ হাসান"
                            expiry = LocalDate.fromEpochDays(today.toEpochDays() + 365).toString()
                            step = 3
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("স্ক্যান ছাড়া নিজে লিখব", color = BrandColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            2 -> {
                // Viewfinder Scanner
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (scanFlash) Color.White else Color(0xFF0D1210)),
                        contentAlignment = Alignment.Center
                    ) {
                        SkySvgIcon(
                            path = "feather_icon/fi_camera.svg",
                            description = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.White.copy(alpha = 0.35f)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("কাগজ সমতল জায়গায় রাখুন", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(text = "ডেমো: আসল অ্যাপে এখানে লাইভ ক্যামেরা দেখাবে", fontSize = 13.sp, color = TextSecondary)
                    // Shutter button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, BrandColor, CircleShape)
                            .clickable {
                                haptic.perform(HapticKind.SUCCESS)
                                scanFlash = true
                                number = if (selectedKind == "licence") "DL-1990-123456" else "ঢাকা মেট্রো-হ ১১-২২৩৪"
                                holder = "মো. রাফিদ হাসান"
                                expiry = LocalDate.fromEpochDays(today.toEpochDays() + 730).toString()
                                step = 3
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(BrandColor))
                    }
                }
            }
            3 -> {
                // Review
                val reason = when {
                    number.isBlank() -> "কাগজের নম্বর লিখুন"
                    holder.isBlank() -> "মালিকের নাম লিখুন"
                    expiry.isBlank() -> "মেয়াদ শেষের তারিখ দিন"
                    !checked -> "আসল কাগজের সঙ্গে মিলিয়ে টিক দিন"
                    else -> null
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("স্ক্যান থেকে যা পাওয়া গেছে তা দেখুন। ভুল থাকলে বদলান।", color = TextSecondary, fontSize = 14.sp)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("কাগজের নম্বর", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp)).padding(14.dp)
                        ) {
                            BasicTextField(value = number, onValueChange = { number = it }, textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("মালিকের নাম", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp)).padding(14.dp)
                        ) {
                            BasicTextField(value = holder, onValueChange = { holder = it }, textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("মেয়াদ শেষের তারিখ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceColor2)
                                .border(1.5.dp, WarningColor, RoundedCornerShape(12.dp)).padding(14.dp)
                        ) {
                            BasicTextField(value = expiry, onValueChange = { expiry = it }, textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 5).forEach { y ->
                            val targetDate = LocalDate.fromEpochDays(today.toEpochDays() + 365 * y).toString()
                            VaultChip(
                                text = "${f.d(y.toString())} বছর পর",
                                active = expiry == targetDate,
                                onClick = { expiry = targetDate }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { checked = !checked }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (checked) BrandColor else Color.Transparent)
                                .border(2.dp, if (checked) BrandColor else BorderStrongColor, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) Text("✓", color = OnBrandColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("আসল কাগজ দেখে তথ্য মিলিয়ে নিয়েছি", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    VaultButton(
                        text = reason ?: "কাগজপত্রে সেভ করুন",
                        full = true,
                        enabled = reason == null,
                        onClick = {
                            if (reason != null) haptic.perform(HapticKind.ERROR)
                            else step = 4
                        }
                    )
                }
            }
            4 -> {
                // Reminder prompt
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(BrandTintColor),
                        contentAlignment = Alignment.Center
                    ) {
                        SkySvgIcon(path = "feather_icon/fi_bell.svg", description = null, modifier = Modifier.size(34.dp), tint = BrandColor)
                    }
                    Text("মেয়াদ শেষ হওয়ার ৩০ দিন আগে জানাব?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text("সকাল ৯টায় একটি নোটিফিকেশন যাবে। নবায়নের সময় থাকবে।", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    VaultButton(
                        text = "হ্যাঁ, জানান",
                        full = true,
                        icon = "fi_bell.svg",
                        onClick = {
                            val newDoc = VaultDocument(
                                title = docNameBn(selectedKind),
                                number = number.trim(),
                                holder = holder.trim(),
                                expiry = expiry.trim(),
                                kind = selectedKind,
                                reminder = true
                            )
                            val next = state.copy(documents = listOf(newDoc) + state.documents.filterNot { it.kind == selectedKind })
                            val toastMsg = "${docNameBn(selectedKind)} সেভ হয়েছে · ফোনের অনুমতি থাকলে জানানো হবে"
                            onSave(next, toastMsg) {}
                        }
                    )
                    VaultButton(
                        text = "এখন নয়",
                        kind = ButtonKind.GHOST,
                        full = true,
                        onClick = {
                            val newDoc = VaultDocument(
                                title = docNameBn(selectedKind),
                                number = number.trim(),
                                holder = holder.trim(),
                                expiry = expiry.trim(),
                                kind = selectedKind,
                                reminder = false
                            )
                            val next = state.copy(documents = listOf(newDoc) + state.documents.filterNot { it.kind == selectedKind })
                            val toastMsg = "${docNameBn(selectedKind)} সেভ হয়েছে"
                            onSave(next, toastMsg) {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DocViewSheet(
    doc: VaultDocument,
    onDismiss: () -> Unit,
    onUpdateReminder: (Boolean) -> Unit,
    onReminderSettings: () -> Unit = {},
    warningDays: Int = doc.reminderWarningDays ?: 30
) {
    val f = currentFmt
    var roadsideMode by remember { mutableStateOf(false) }

    Sheet(
        title = doc.title,
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        footer = {
            VaultButton(
                text = "ফিরে যান",
                kind = ButtonKind.SECONDARY,
                full = true,
                icon = "fi_sun.svg",
                onClick = onDismiss
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (roadsideMode) Color.White else SurfaceColor2)
                .then(if (roadsideMode) Modifier.border(2.dp, Color.Black, RoundedCornerShape(16.dp)) else Modifier)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DocArt(doc = doc, large = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpiryChip(doc = doc, warningDays = warningDays)
                val expiryDate = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
                Text(
                    text = if (doc.expiry.isBlank()) "মেয়াদ নেই" else "মেয়াদ শেষ: " + if (expiryDate != null) f.dateFull(expiryDate) else doc.expiry,
                    fontSize = 13.sp,
                    color = if (roadsideMode) Color(0xFF3A3A3A) else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        VaultSwitch(
            title = "রাস্তায় দেখানোর মোড",
            subtitle = "সাদা পটভূমি, বড় ছবি, স্পষ্ট লেখা",
            checked = roadsideMode,
            onCheckedChange = { roadsideMode = it }
        )

        if (doc.expiry.isNotBlank()) VaultSwitch(
            title = "এই কাগজের রিমাইন্ডার",
            checked = doc.reminder,
            onCheckedChange = { onUpdateReminder(it) }
        )
        VaultButton("অনুমতি, সময় ও পরে জানান", onClick = onReminderSettings, full = true, kind = ButtonKind.SECONDARY)
    }
}

@Composable
fun PasteSheet(
    onDismiss: () -> Unit,
    onSelectFlow: (String, Any?) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    var text by remember { mutableStateOf("") }
    val parsedService = remember(text) { SmartParse.parseServiceText(text) }
    val parsedAmount = remember(text) { SmartParse.parseAmount(text) }
    val isFuel = parsedAmount != null && SmartParse.FUEL_WORDS.containsMatchIn(text) && parsedService.lines.isEmpty()
    val hasService = parsedService.lines.isNotEmpty()

    Sheet(
        title = "লিখে বা পেস্ট করে সাজান",
        onDismiss = onDismiss
    ) {
        Text("বিকাশ, নগদ বা মেকানিকের মেসেজ এখানে দিন। কিছু নিজে নিজে পড়া হয় না।", color = TextSecondary, fontSize = 14.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultChip(
                text = "নগদ/বিকাশ মেসেজ",
                onClick = { text = "Payment Tk 500.00 to Padma Filling Station successful. TrxID 9K3L2M. Balance Tk 2,140.50" }
            )
            VaultChip(
                text = "মেকানিকের মেসেজ",
                onClick = { text = "ভাই আপনার বাইকের ইঞ্জিন অয়েল ৯০০ টাকা, অয়েল ফিল্টার ২৫০ টাকা, চেইন লুব ১৫০ টাকা" }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor2)
                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                cursorBrush = SolidColor(BrandColor),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text("এখানে পেস্ট করুন", fontSize = 15.sp, color = TextTertiary)
                    inner()
                }
            )
        }

        if (isFuel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandTintColor)
                    .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkySvgIcon(path = "feather_icon/fi_droplet.svg", description = null, modifier = Modifier.size(20.dp), tint = BrandColor)
                    Text("তেলের খরচ ${f.money(parsedAmount!!)} মনে হচ্ছে।", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                VaultButton(
                    text = "তেল ভরা খুলুন",
                    size = ButtonSize.SM,
                    onClick = {
                        haptic.perform(HapticKind.SUCCESS)
                        onDismiss()
                        onSelectFlow("fuel", parsedAmount)
                    }
                )
            }
        }

        if (hasService) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandTintColor)
                    .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkySvgIcon(path = "feather_icon/fi_tool.svg", description = null, modifier = Modifier.size(20.dp), tint = BrandColor)
                    val summary = parsedService.lines.joinToString(", ") { catalogItemOf(it.item)?.short ?: it.item }
                    Text(summary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                VaultButton(
                    text = "সার্ভিস খুলুন",
                    size = ButtonSize.SM,
                    onClick = {
                        haptic.perform(HapticKind.SUCCESS)
                        onDismiss()
                        onSelectFlow("service", parsedService.lines)
                    }
                )
            }
        }
    }
}
