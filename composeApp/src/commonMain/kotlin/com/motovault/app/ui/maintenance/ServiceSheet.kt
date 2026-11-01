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
import dev.sky.design.tokens.SkyTokens
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.FuelEntryRules
import com.motovault.app.domain.fuel.MAX_FUEL_LITRES
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.domain.fuel.FuelTimelineEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

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
                        V3Icon(
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
                    V3Icon(
                    path = "feather_icon/fi_gauge.svg",
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
