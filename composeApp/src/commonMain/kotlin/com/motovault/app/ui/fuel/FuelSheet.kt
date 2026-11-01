package com.motovault.app.ui.fuel

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

import com.motovault.app.ui.*

@Composable
internal fun FuelSheet(
    input: FuelScreenInput,
    initialAmount: Double? = null,
    existing: FuelExistingEntry? = null,
    editError: String? = null,
    onDismiss: () -> Unit,
    onSave: (FuelEntryRequest, String, () -> Unit) -> Unit,
    onEdit: (FuelEditRequest, String) -> Unit = { _, _ -> }
) {
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    val estimate = input.estimatedOdometer
    val lastOdo = input.lastFuelOdometer
    val lastFuelDate = input.lastFuelDate
    val initialFuelType = existing?.fuelType ?: FuelDefaults.OCTANE
    val lastOctanePrice = existing?.fuelPrice ?: input.lastPrices[initialFuelType] ?: FuelDefaults.LEGACY_OCTANE_TAKA_PER_LITRE

    val draft = remember { FuelDraft(existing, today) }
    var fuelType by draft::fuelType
    var mode by draft::mode
    var fullTank by draft::fullTank
    var dateKey by draft::dateSelection
    var customDate by draft::customDate
    var place by draft::place
    var odoTouched by draft::odometerTouched
    var priceTouched by draft::priceTouched

    val entry = rememberNumberEntry(
        initialValues = mapOf(
            "amount" to (existing?.cost?.toString() ?: initialAmount?.let { kotlin.math.round(it).toInt().toString() } ?: ""),
            "litres" to (existing?.fuelLiters?.toString() ?: ""),
            "price" to (existing?.fuelPrice?.toString() ?: kotlin.math.round(lastOctanePrice).toInt().toString()),
            "odo" to (existing?.odometer?.toString() ?: kotlin.math.round(estimate).toInt().toString())
        ),
        firstField = "amount"
    )

    val price = parseNum(entry.values["price"] ?: "") ?: lastOctanePrice
    val amountVal = parseNum(entry.values["amount"] ?: "") ?: 0.0
    val litresVal = parseNum(entry.values["litres"] ?: "") ?: 0.0
    val odoVal = parseNum(entry.values["odo"] ?: "") ?: 0.0

    val calculation = FuelEntryRules.calculate(mode, amountVal, litresVal, price, odoVal, if (existing == null) lastOdo else 0.0)
    val editCalculation = existing?.let {
        FuelEntryRules.calculateEdit(mode, amountVal, litresVal, price, it.cost, it.fuelLiters, it.fuelPrice,
            it.fuelPrice ?: kotlin.math.round(lastOctanePrice), fuelType == it.fuelType, priceTouched)
    }
    val calculatedTaka = editCalculation?.amount ?: calculation.amount
    val calculatedLitres = editCalculation?.litres ?: calculation.litres
    val savedPrice = if (editCalculation == null) price else editCalculation.price

    val date = when (dateKey) {
        FuelDateSelection.YESTERDAY -> LocalDate.fromEpochDays(today.toEpochDays() - 1)
        FuelDateSelection.CUSTOM -> runCatching { LocalDate.parse(toAscii(customDate)) }.getOrNull()
        FuelDateSelection.TODAY -> today
    }

    val odoError = if (calculation.odometerTooLow)
        appStringResource(Res.string.fuel_odometer_above_previous, f.n(lastOdo.toLong())) else null
    val timeline = if (existing != null) input.timeline else emptyList()
    val issue = if (existing == null) calculation.issue ?: FuelEntryRules.validateDate(date, today, lastFuelDate)
    else if (existing.fuelLiters == null && mode == FuelEntryMode.AMOUNT) FuelEntryIssue.ENTER_LITRES
    else if (date == null) FuelEntryIssue.INVALID_DATE
    else FuelEntryRules.validateEditedValues(calculatedTaka, calculatedLitres, savedPrice, odoVal, date, today)
        ?: if (timeline.any { it == null }) FuelEntryIssue.TIMELINE_CONFLICT
        else FuelEntryRules.validateEditOrder(FuelTimelineEntry(existing.id, odoVal, date), timeline.filterNotNull())
    val reason = when (issue) {
        FuelEntryIssue.ENTER_AMOUNT -> appStringResource(Res.string.fuel_enter_amount)
        FuelEntryIssue.ENTER_LITRES -> appStringResource(Res.string.fuel_enter_litres)
        FuelEntryIssue.ENTER_PRICE -> appStringResource(Res.string.fuel_enter_price)
        FuelEntryIssue.LITRES_TOO_HIGH -> appStringResource(Res.string.fuel_litres_too_high, f.n(MAX_FUEL_LITRES))
        FuelEntryIssue.INVALID_ODOMETER -> appStringResource(Res.string.fuel_invalid_odometer)
        FuelEntryIssue.ODOMETER_TOO_LOW -> appStringResource(Res.string.fuel_fix_odometer)
        FuelEntryIssue.INVALID_DATE -> appStringResource(Res.string.fuel_invalid_date)
        FuelEntryIssue.FUTURE_DATE -> appStringResource(Res.string.fuel_future_date)
        FuelEntryIssue.DATE_BEFORE_LAST_FILL -> appStringResource(Res.string.fuel_date_before_last)
        FuelEntryIssue.RECORD_MISSING -> appStringResource(Res.string.fuel_record_missing)
        FuelEntryIssue.TIMELINE_CONFLICT -> appStringResource(Res.string.fuel_timeline_conflict)
        null -> null
    }
    val moneyText = appStringResource(Res.string.fuel_money_value, f.n(calculatedTaka))
    val litresText = appStringResource(Res.string.fuel_litres_value, f.n(calculatedLitres, 2))
    val priceText = if (existing != null && savedPrice == null) appStringResource(Res.string.fuel_price_unknown)
        else appStringResource(Res.string.fuel_price_value, appStringResource(Res.string.fuel_money_value, f.n(price)))
    val saveText = appStringResource(Res.string.fuel_save_amount, moneyText)
    val savedMessage = if (existing == null) appStringResource(Res.string.fuel_added, moneyText, litresText) else appStringResource(Res.string.fuel_edit_saved)

    Sheet(
        title = appStringResource(if (existing == null) Res.string.fuel_title else Res.string.fuel_edit_title),
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = reason ?: saveText,
                contentDescription = reason ?: saveText,
                onClick = {
                    if (reason != null) {
                        haptic.perform(HapticKind.ERROR)
                    } else {
                        val legacyDetail = "${f.liters(calculatedLitres)} · ${if (fullTank) "Full tank" else "Partial fill"}"
                        if (existing != null) {
                            onEdit(FuelEditRequest(existing.id, calculatedTaka, calculatedLitres, savedPrice, odoVal, requireNotNull(date), fullTank, fuelType, place,
                                if (calculatedLitres == existing.fuelLiters && fullTank == existing.fullTank) null else legacyDetail), savedMessage)
                        } else onSave(FuelEntryRequest(
                            legacyDetail = legacyDetail,
                            date = requireNotNull(date),
                            odometer = odoVal,
                            litres = calculatedLitres,
                            fullTank = fullTank,
                            amount = calculatedTaka,
                            fuelType = fuelType,
                            price = price,
                            place = place
                        ), savedMessage) {
                            // Undo action: restore previous state
                        }
                    }
                },
                full = true,
                enabled = reason == null
            )
            VaultKeypad(onKey = { key ->
                if (entry.activeField == "price") priceTouched = true
                entry.press(key)
            })
        }
    ) {
        editError?.let { Text(text = it, color = ErrorColor, fontSize = 13.sp) }
        val amountDisplayText = if (mode == FuelEntryMode.AMOUNT) entry.values["amount"].orEmpty() else entry.values["litres"].orEmpty()
        FuelAmountHero(
            mode = mode,
            displayText = amountDisplayText,
            amountActive = entry.activeField == "amount" || entry.activeField == "litres",
            priceActive = entry.activeField == "price",
            priceText = priceText,
            secondaryText = if (mode == FuelEntryMode.AMOUNT) litresText else moneyText,
            onActivate = { entry.setActive(if (mode == FuelEntryMode.AMOUNT) "amount" else "litres") },
            onPrice = { entry.setActive("price") }
        )

        // Mode & Fuel chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultChip(text = appStringResource(Res.string.fuel_by_amount), active = mode == FuelEntryMode.AMOUNT, onClick = { mode = FuelEntryMode.AMOUNT; entry.setActive("amount") })
            VaultChip(text = appStringResource(Res.string.fuel_by_litres), active = mode == FuelEntryMode.LITRES, onClick = { mode = FuelEntryMode.LITRES; entry.setActive("litres") })
            Spacer(Modifier.width(4.dp))
            Box(Modifier.width(1.dp).height(24.dp).background(BorderColor))
            Spacer(Modifier.width(4.dp))
            VaultChip(text = appStringResource(Res.string.fuel_octane), active = fuelType == FuelDefaults.OCTANE, onClick = {
                fuelType = FuelDefaults.OCTANE
                            entry.set("price", kotlin.math.round(input.lastPrices[FuelDefaults.OCTANE] ?: FuelDefaults.LEGACY_OCTANE_TAKA_PER_LITRE).toInt().toString())
            })
            VaultChip(text = appStringResource(Res.string.fuel_petrol), active = fuelType == FuelDefaults.PETROL, onClick = {
                fuelType = FuelDefaults.PETROL
                            entry.set("price", kotlin.math.round(input.lastPrices[FuelDefaults.PETROL] ?: FuelDefaults.LEGACY_PETROL_TAKA_PER_LITRE).toInt().toString())
            })
        }

        // Full tank & date chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultChip(text = appStringResource(if (fullTank) Res.string.fuel_full_chip else Res.string.fuel_partial_chip), active = fullTank, onClick = { fullTank = !fullTank })
            VaultChip(text = appStringResource(Res.string.date_today), active = dateKey == FuelDateSelection.TODAY, onClick = { dateKey = FuelDateSelection.TODAY })
            VaultChip(text = appStringResource(Res.string.date_yesterday), active = dateKey == FuelDateSelection.YESTERDAY, onClick = { dateKey = FuelDateSelection.YESTERDAY })
            VaultChip(text = appStringResource(Res.string.date_other), active = dateKey == FuelDateSelection.CUSTOM, onClick = { dateKey = FuelDateSelection.CUSTOM })
        }

        // Custom date field if custom selected
        AnimatedVisibility(visible = dateKey == FuelDateSelection.CUSTOM) {
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
        val activeField = entry.activeField
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
                    V3Icon(
                        path = "feather_icon/fi_gauge.svg",
                        description = null,
                        modifier = Modifier.size(20.dp),
                        tint = BrandColor
                    )
                    Column {
                        Text(text = appStringResource(Res.string.garage_odometer), fontSize = 12.sp, color = TextSecondary)
                        Text(text = appStringResource(Res.string.fuel_distance_value, f.n(odoVal.toLong())), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                        Text(text = appStringResource(Res.string.odo_estimated), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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
                    val bumpLabel = "+${f.d(bump.toString())}"
                    VaultChip(
                        text = bumpLabel,
                        onClick = {
                            entry.set("odo", (odoVal + bump).toInt().toString())
                            odoTouched = true
                        }
                    )
                }
                VaultChip(
                    text = appStringResource(Res.string.odo_estimated),
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
            V3Icon(
                path = "feather_icon/fi_map-pin.svg",
                description = null,
                modifier = Modifier.size(18.dp),
                tint = TextTertiary
            )
            input.places.take(2).forEach { p ->
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
                        Text(text = appStringResource(Res.string.fuel_pump_name), fontSize = 15.sp, color = TextTertiary)
                    }
                    innerTextField()
                }
            )
        }
    }
}
