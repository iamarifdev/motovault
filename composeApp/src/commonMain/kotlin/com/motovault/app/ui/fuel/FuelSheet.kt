package com.motovault.app.ui.fuel

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.MAX_FUEL_LITRES
import com.motovault.app.ui.*
import motovault.composeapp.generated.resources.*
import kotlin.math.round

private val ODO_BUMPS = listOf(10, 50, 100)

@Composable
internal fun FuelSheet(
    input: FuelScreenInput,
    initialAmount: Double? = null,
    existing: FuelExistingEntry? = null,
    editError: String? = null,
    onDismiss: () -> Unit,
    onSave: (FuelEntryRequest, String, () -> Unit) -> Unit,
    onEdit: (FuelEditRequest, String) -> Unit = { _, _ -> },
    initialOdometer: Double? = null
) {
    val currentDay = today()
    val draft = remember { FuelDraft(existing, currentDay) }
    val initialPrice = existing?.fuelPrice ?: (input.lastPrices[draft.fuelType] ?: defaultPriceForFuel(draft.fuelType))
    val entry = rememberNumberEntry(
        initialValues = mapOf(
            "amount" to (existing?.cost?.let(::formatFuelInputNumber)
                ?: initialAmount?.let(::formatFuelInputNumber).orEmpty()),
            "litres" to (existing?.fuelLiters?.let(::formatFuelInputNumber).orEmpty()),
            "price" to formatFuelInputNumber(initialPrice),
            "odo" to (existing?.odometer?.let(::formatFuelInputNumber)
                ?: initialOdometer?.let(::formatFuelInputNumber)
                ?: round(input.estimatedOdometer).toLong().toString())
        ),
        firstField = "amount"
    )
    var pumpPickerOpen by remember { mutableStateOf(false) }
    val capture = LocalOdometerCapture.current ?: rememberOdometerCaptureState()
    val haptic = currentHaptic
    val voice = rememberFuelVoiceController(input, existing, draft, entry)

    if (pumpPickerOpen) {
        PumpPickerSheet(
            value = draft.place,
            recentPumps = input.recentPumps,
            onPick = { picked -> draft.place = picked; pumpPickerOpen = false },
            onDismiss = { pumpPickerOpen = false }
        )
        return
    }

    val eval = evaluateFuelDraft(input, existing, currentDay, draft, entry)

    Sheet(
        title = appStringResource(if (existing == null) Res.string.fuel_title else Res.string.fuel_edit_title),
        onDismiss = onDismiss,
        compact = true,
        actions = if (capture.open) {
            null
        } else {
            {
                VaultIconButton(
                    icon = "fi_camera.svg",
                    label = appStringResource(Res.string.fuel_camera_action),
                    onClick = { voice.cancel(); capture.start() }
                )
                VaultIconButton(
                    icon = if (voice.active) "fi_mic-off.svg" else "fi_mic.svg",
                    label = appStringResource(if (voice.active) Res.string.odo_mic_stop else Res.string.odo_mic_start),
                    tone = if (voice.active) IconTone.TINT else IconTone.PLAIN,
                    onClick = { capture.close(); voice.toggleMic() }
                )
            }
        },
        footer = if (capture.open) {
            null
        } else {
            { FuelSaveFooter(eval, existing, editError, entry, draft, onSave, onEdit) }
        }
    ) {
        if (capture.open) {
            FuelPumpCameraCard(capture = capture) { reading ->
                applyPumpMeterReading(reading, input, existing, draft, entry)
                haptic.perform(HapticKind.SUCCESS)
                capture.close()
            }
        } else {
            FuelSheetBody(input, draft, entry, eval, existing, voice) { pumpPickerOpen = true }
        }
    }
}

@Composable
private fun FuelSheetBody(
    input: FuelScreenInput,
    draft: FuelDraft,
    entry: NumberEntryState,
    eval: FuelValidationSnapshot,
    existing: FuelExistingEntry?,
    voice: FuelVoiceController,
    onOpenPumpPicker: () -> Unit
) {
    if (voice.disclosureOpen) {
        VoiceDisclosureCard(onConfirm = voice.confirmProvider, onCancel = voice.dismissProvider)
    }
    FuelAmountSection(draft = draft, entry = entry, eval = eval, existing = existing, voiceBanner = voice.banner)
    if (draft.mode == FuelEntryMode.AMOUNT) {
        FuelPresetsLine(
            activeAmount = eval.taka,
            hasAmountText = entry.values["amount"].orEmpty().isNotBlank(),
            onSelect = { preset ->
                draft.mode = FuelEntryMode.AMOUNT
                entry.setActive("amount")
                entry.set("amount", preset.toString())
            }
        )
    }
    PickerField(
        text = draft.place.ifBlank { appStringResource(Res.string.fuel_pump_placeholder) },
        isSet = draft.place.isNotBlank(),
        leadingIcon = "fi_map-pin.svg",
        onClick = onOpenPumpPicker
    )
    FuelOdometerRow(input = input, draft = draft, entry = entry, eval = eval)
    FuelOptionsLine(
        fullTank = draft.fullTank,
        fuelType = draft.fuelType,
        dateKey = draft.dateSelection,
        customDate = draft.customDate,
        missedFill = draft.missedFill,
        onToggleFull = { draft.fullTank = !draft.fullTank },
        onSelectFuel = { nextType ->
            draft.fuelType = nextType
            draft.priceTouched = false
            entry.set("price", formatFuelInputNumber(defaultPriceFor(input, existing, nextType)))
        },
        onSelectDateKey = { draft.dateSelection = it },
        onCustomDateChange = { draft.customDate = it },
        onToggleMissedFill = { draft.missedFill = !draft.missedFill }
    )
}

@Composable
private fun FuelAmountSection(
    draft: FuelDraft,
    entry: NumberEntryState,
    eval: FuelValidationSnapshot,
    existing: FuelExistingEntry?,
    voiceBanner: FuelVoiceBanner?
) {
    val f = currentFmt
    val rawAmountText = if (draft.mode == FuelEntryMode.AMOUNT) {
        entry.values["amount"].orEmpty()
    } else {
        entry.values["litres"].orEmpty()
    }
    val questionText = appStringResource(
        if (draft.mode == FuelEntryMode.AMOUNT) Res.string.fuel_amount_question else Res.string.fuel_litres_question
    )
    EntryAmountCard(
        caption = questionText,
        valueText = f.editableNumber(rawAmountText),
        isPlaceholder = rawAmountText.isEmpty(),
        active = entry.activeField == "amount" || entry.activeField == "litres",
        amountDescription = questionText,
        leadingUnit = if (draft.mode == FuelEntryMode.AMOUNT) appStringResource(Res.string.fuel_currency_symbol) else null,
        trailingUnit = if (draft.mode == FuelEntryMode.LITRES) appStringResource(Res.string.fuel_liters) else null,
        topTrailing = {
            SegmentedPill(
                options = listOf(
                    FuelEntryMode.AMOUNT to appStringResource(Res.string.fuel_mode_taka),
                    FuelEntryMode.LITRES to appStringResource(Res.string.fuel_mode_litre)
                ),
                selected = draft.mode,
                label = appStringResource(Res.string.fuel_mode_label),
                onSelect = { nextMode ->
                    draft.mode = nextMode
                    entry.setActive(if (nextMode == FuelEntryMode.AMOUNT) "amount" else "litres")
                }
            )
        },
        subContent = { FuelAmountSubline(draft, entry, eval, existing) },
        message = fuelCardMessage(eval.literWarning, voiceBanner),
        messageIsWarning = eval.literWarning,
        onClick = { entry.setActive(if (draft.mode == FuelEntryMode.AMOUNT) "amount" else "litres") }
    )
}

@Composable
private fun FuelAmountSubline(
    draft: FuelDraft,
    entry: NumberEntryState,
    eval: FuelValidationSnapshot,
    existing: FuelExistingEntry?
) {
    val f = currentFmt
    val secondaryText = if (draft.mode == FuelEntryMode.AMOUNT) f.liters(eval.litres) else f.money(eval.taka)
    val priceActive = entry.activeField == "price"
    val priceText = if (existing != null && eval.savedPrice == null && !priceActive) {
        appStringResource(Res.string.fuel_price_unknown)
    } else {
        val rawPrice = entry.values["price"].orEmpty()
        val displayPrice = when {
            priceActive -> f.editableNumber(rawPrice)
            eval.price % 1.0 == 0.0 -> f.money(eval.price)
            else -> appStringResource(Res.string.fuel_money_value, f.n(eval.price, 2))
        }
        appStringResource(Res.string.fuel_price_value, displayPrice)
    }
    Text(
        text = appStringResource(Res.string.fuel_approximate_value, secondaryText),
        fontSize = 14.sp,
        color = TextSecondary
    )
    ConfirmPill(
        text = priceText,
        tone = if (priceActive) PillTone.ACTIVE else PillTone.DEFAULT,
        trailingIcon = "fi_edit-2.svg",
        contentDescription = appStringResource(Res.string.fuel_price_edit_desc),
        onClick = { entry.setActive("price") }
    )
    if (!eval.priceKnown && eval.price > 0.0) {
        ConfirmPill(
            text = appStringResource(if (eval.priceConfirmed) Res.string.fuel_price_ok else Res.string.fuel_price_new_check),
            icon = if (eval.priceConfirmed) "fi_check.svg" else null,
            tone = if (eval.priceConfirmed) PillTone.CONFIRM_DONE else PillTone.CONFIRM_PENDING,
            onClick = { draft.confirmedPriceKey = if (eval.priceConfirmed) null else eval.priceKey }
        )
    }
}

@Composable
private fun fuelCardMessage(literWarning: Boolean, voiceBanner: FuelVoiceBanner?): String? = when {
    literWarning -> appStringResource(Res.string.fuel_liter_warn)
    voiceBanner is FuelVoiceBanner.Parsed -> appStringResource(Res.string.fuel_voice_parsed, voiceBanner.transcript)
    voiceBanner is FuelVoiceBanner.Unparsed -> appStringResource(Res.string.fuel_voice_unparsed, voiceBanner.transcript)
    voiceBanner is FuelVoiceBanner.Retry -> appStringResource(Res.string.fuel_voice_retry)
    voiceBanner is FuelVoiceBanner.Unavailable -> appStringResource(Res.string.capture_voice_unavailable)
    else -> null
}

@Composable
private fun FuelOdometerRow(
    input: FuelScreenInput,
    draft: FuelDraft,
    entry: NumberEntryState,
    eval: FuelValidationSnapshot
) {
    val f = currentFmt
    val isOdoActive = entry.activeField == "odo"
    val errorLabel = when (eval.odometerError) {
        FuelValidationReason.ODOMETER_TOO_LOW ->
            appStringResource(Res.string.fuel_odometer_above_previous, f.n(input.lastFuelOdometer.toLong()))
        FuelValidationReason.TIMELINE_CONFLICT ->
            appStringResource(Res.string.fuel_timeline_conflict)
        else -> null
    }
    val rowLabel = errorLabel ?: appStringResource(
        if (draft.odometerTouched) Res.string.fuel_odo_label else Res.string.fuel_odo_estimated_label
    )

    OdoRow(
        label = rowLabel,
        valueText = appStringResource(
            Res.string.fuel_distance_value,
            f.editableNumber(entry.values["odo"].orEmpty())
        ),
        active = isOdoActive,
        isError = errorLabel != null,
        onClick = { entry.setActive("odo") },
        trailing = {
            if (isOdoActive) {
                ODO_BUMPS.forEach { bump ->
                    ConfirmPill(
                        text = appStringResource(Res.string.odometer_bump_chip, f.d(bump.toString())),
                        onClick = {
                            entry.set("odo", (eval.odometer + bump).toLong().toString())
                            draft.odometerTouched = true
                        }
                    )
                }
            } else if (!draft.odometerTouched) {
                ConfirmPill(
                    text = appStringResource(Res.string.odo_confirm),
                    icon = "fi_check.svg",
                    tone = PillTone.CONFIRM_PENDING,
                    onClick = { draft.odometerTouched = true }
                )
            }
        }
    )
}

@Composable
private fun FuelSaveFooter(
    eval: FuelValidationSnapshot,
    existing: FuelExistingEntry?,
    editError: String?,
    entry: NumberEntryState,
    draft: FuelDraft,
    onSave: (FuelEntryRequest, String, () -> Unit) -> Unit,
    onEdit: (FuelEditRequest, String) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val reasonText = validationMessage(eval.reason)
    val moneyText = f.money(eval.taka)
    val saveText = appStringResource(Res.string.fuel_save_amount, moneyText)
    val savedMessage = if (existing == null) {
        appStringResource(Res.string.fuel_added, moneyText, f.liters(eval.litres))
    } else {
        appStringResource(Res.string.fuel_edit_saved)
    }

    if (editError != null) {
        Text(text = editError, color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    VaultButton(
        text = reasonText ?: saveText,
        contentDescription = reasonText ?: saveText,
        full = true,
        enabled = eval.reason == null,
        onClick = {
            if (eval.reason != null) {
                haptic.perform(HapticKind.ERROR)
                return@VaultButton
            }
            submitFuelSave(eval, existing, draft, f.liters(eval.litres), savedMessage, onSave, onEdit)
            haptic.perform(HapticKind.SUCCESS)
        }
    )
    VaultKeypad(
        onKey = { key ->
            if (entry.activeField == "price") draft.priceTouched = true
            if (entry.activeField == "odo") draft.odometerTouched = true
            entry.press(key)
        }
    )
}

@Composable
private fun validationMessage(reason: FuelValidationReason?): String? {
    val f = currentFmt
    return when (reason) {
        FuelValidationReason.ENTER_AMOUNT -> appStringResource(Res.string.fuel_enter_amount)
        FuelValidationReason.ENTER_LITRES -> appStringResource(Res.string.fuel_enter_litres)
        FuelValidationReason.ENTER_PRICE -> appStringResource(Res.string.fuel_enter_price)
        FuelValidationReason.CONFIRM_PRICE -> appStringResource(Res.string.fuel_confirm_new_price)
        FuelValidationReason.CONFIRM_ODOMETER -> appStringResource(Res.string.fuel_confirm_odometer)
        FuelValidationReason.LITRES_TOO_HIGH -> appStringResource(Res.string.fuel_litres_too_high, f.n(MAX_FUEL_LITRES))
        FuelValidationReason.INVALID_ODOMETER -> appStringResource(Res.string.fuel_invalid_odometer)
        FuelValidationReason.ODOMETER_TOO_LOW -> appStringResource(Res.string.fuel_fix_odometer)
        FuelValidationReason.TIMELINE_CONFLICT -> appStringResource(Res.string.fuel_timeline_conflict)
        FuelValidationReason.INVALID_DATE -> appStringResource(Res.string.fuel_invalid_date)
        FuelValidationReason.FUTURE_DATE -> appStringResource(Res.string.fuel_future_date)
        FuelValidationReason.DATE_BEFORE_LAST_FILL -> appStringResource(Res.string.fuel_date_before_last)
        FuelValidationReason.RECORD_MISSING -> appStringResource(Res.string.fuel_record_missing)
        null -> null
    }
}
