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
    val catalogLabels = CATALOG_ITEMS.associate { it.key to appStringResource(it.label) }; val catalogShorts = CATALOG_ITEMS.associate { it.key to appStringResource(it.shortLabel) }; val baselineOrder = appStringResource(Res.string.service_baseline_order)
    val rows = remember {
        mutableStateListOf<ServiceRowState>().apply {
            if (initialRows != null && initialRows.isNotEmpty()) {
                initialRows.forEachIndexed { i, p ->
                    add(ServiceRowState("$i-${p.item}", p.item, catalogLabels[p.item] ?: p.item, p.qty))
                }
            } else if (initialCatalogueKey != null) {
                add(ServiceRowState("0-$initialCatalogueKey", initialCatalogueKey, catalogLabels[initialCatalogueKey] ?: initialCatalogueKey, 1.0))
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
    val serviceAddedMessage = appStringResource(Res.string.service_added, f.money(total))
    val serviceSaveError = appStringResource(Res.string.service_save_error)
    val demoServiceText = appStringResource(Res.string.service_demo_text)

    val reason = when {
        rows.isEmpty() -> appStringResource(Res.string.service_error_no_rows)
        odoVal <= 0 -> appStringResource(Res.string.service_error_km)
        else -> null
    }

    Sheet(
        title = appStringResource(Res.string.service_title),
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = appStringResource(Res.string.service_total) + if (unknownPrices > 0) appStringResource(Res.string.service_unknown_prices, f.d(unknownPrices.toString())) else "",
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
                text = reason ?: appStringResource(Res.string.service_save),
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
                            detail = rows.joinToString(", ") { it.name },
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
                            saveIssue = if (it.message == "service_baseline_invalid") baselineOrder else it.message ?: serviceSaveError
                            return@VaultButton
                        }
                        val next = recorded.copy(
                            lastItemPrices = updatedPrices,
                            workshops = updatedWorkshops.take(6)
                        )
                        val toastMsg = serviceAddedMessage
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
                            description = appStringResource(Res.string.service_hide),
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
        selectedRule?.let { Text(appStringResource(Res.string.service_schedule_selected, it.name), fontSize = 14.sp, color = TextPrimary) }
        saveIssue?.let { Text(it, color = ErrorColor, fontSize = 14.sp) }
        Text(appStringResource(Res.string.service_schedule_update), fontSize = 13.sp, color = TextSecondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (lastService != null) {
                VaultButton(
                    text = appStringResource(Res.string.service_like_previous),
                    kind = ButtonKind.SECONDARY,
                    size = ButtonSize.SM,
                    icon = "fi_copy.svg",
                    onClick = {
                        haptic.perform(HapticKind.SUCCESS)
                        rows.clear()
                        lastService.serviceLines.forEachIndexed { i, l ->
                            val itemKey = l.catalogueId ?: "custom"
                            val r = ServiceRowState("$i-$itemKey", itemKey, catalogLabels[itemKey] ?: l.displayName, l.quantity, l.detail)
                            rows.add(r)
                            l.unitPricePoisha?.let { entry.set("p:${r.uid}", (it / 100).toString()) }
                        }
                        keysOpen = false
                    }
                )
            }
            VaultButton(
                text = appStringResource(Res.string.service_type_or_speak),
                kind = ButtonKind.SECONDARY,
                size = ButtonSize.SM,
                icon = "fi_zap.svg",
                onClick = { smartOpen = !smartOpen }
            )
        }

        val smartParsed = remember(smartText) { SmartParse.parseServiceText(smartText) }
        ServiceSmartInput(
            open = smartOpen,
            text = smartText,
            parsedCount = smartParsed.lines.size,
            onTextChange = { smartText = it },
            onAdd = {
                haptic.perform(HapticKind.SUCCESS)
                val baseIndex = rows.size
                smartParsed.lines.forEachIndexed { i, p ->
                    val r = ServiceRowState("${baseIndex + i}-${p.item}", p.item, catalogLabels[p.item] ?: p.item, p.qty)
                    rows.add(r)
                    val pr = p.price ?: state.lastItemPrices[p.item]
                    if (pr != null) entry.set("p:${r.uid}", pr.toInt().toString())
                }
                smartOpen = false
                smartText = ""
            },
            onClose = { smartOpen = false; smartText = "" }
        )

        // Service items chips grid
        Text(text = appStringResource(Res.string.service_what_done), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CATALOG_ITEMS.forEach { item ->
                val active = rows.any { it.itemKey == item.key }
                val lastPrice = state.lastItemPrices[item.key]
                VaultChip(
                    text = catalogShorts[item.key] ?: item.key,
                    active = active,
                    hint = lastPrice?.let { f.money(it) },
                    onClick = {
                        val existingIndex = rows.indexOfFirst { it.itemKey == item.key }
                        if (existingIndex >= 0) {
                            rows.removeAt(existingIndex)
                        } else {
                            val r = ServiceRowState("${rows.size}-${item.key}", item.key, catalogLabels[item.key] ?: item.key, 1.0)
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

        ServiceLinesEditor(
            rows = rows,
            entry = entry,
            f = f,
            onPrice = { entry.setActive(it); keysOpen = true },
            onRemove = { rows.remove(it) }
        )

        ServiceDateOdometerSection(
            dateKey = dateKey,
            onDate = { dateKey = it },
            odoVal = odoVal,
            odoActive = entry.activeField == "odo" && keysOpen,
            onOdo = { entry.setActive("odo"); keysOpen = true },
            f = f
        )

        ServiceWorkshopField(workshop, state.workshops, { workshop = it })

        // More toggle for Labour, Misc, Discount
        TextButton(
            onClick = { moreOpen = !moreOpen },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (moreOpen) appStringResource(Res.string.service_hide_more) else appStringResource(Res.string.service_more_expenses),
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
                listOf("labour" to appStringResource(Res.string.service_labour), "misc" to appStringResource(Res.string.service_misc), "discount" to appStringResource(Res.string.service_discount)).forEach { (k, label) ->
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
