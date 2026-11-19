package com.motovault.app.ui.fuel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.motovault.app.application.capture.VoiceCaptureState
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.domain.capture.PumpMeterReading
import com.motovault.app.domain.capture.SpokenFuelParser
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.ui.EntryDateKey
import com.motovault.app.ui.HapticKind
import com.motovault.app.ui.NumberEntryState
import com.motovault.app.ui.currentHaptic
import com.motovault.app.ui.VoiceProviderConsent
import com.motovault.app.ui.rememberLocalFirstVoice
import com.motovault.app.ui.startProviderWithConsent
import kotlinx.datetime.LocalDate
import kotlin.math.abs

/** Transient draft state for the open fuel sheet; defaults follow v4 (`fullTank = false`). */
@Stable
internal class FuelDraft(existing: FuelExistingEntry?, today: LocalDate) {
    var fuelType by mutableStateOf(existing?.fuelType ?: FuelDefaults.OCTANE)
    var mode by mutableStateOf(FuelEntryMode.AMOUNT)
    var fullTank by mutableStateOf(existing?.fullTank ?: false)
    var missedFill by mutableStateOf(existing?.missedFill ?: false)
    var dateSelection by mutableStateOf(if (existing == null) EntryDateKey.TODAY else EntryDateKey.CUSTOM)
    var customDate by mutableStateOf(existing?.date ?: today.toString())
    var place by mutableStateOf(existing?.locationLabel.orEmpty())
    var odometerTouched by mutableStateOf(existing != null)
    var priceTouched by mutableStateOf(false)
    var confirmedPriceKey by mutableStateOf<String?>(null)
}

internal sealed interface FuelVoiceBanner {
    data class Parsed(val transcript: String) : FuelVoiceBanner
    data class Unparsed(val transcript: String) : FuelVoiceBanner
    data object Retry : FuelVoiceBanner
    data object Unavailable : FuelVoiceBanner
}

internal class FuelVoiceController(
    val active: Boolean,
    val banner: FuelVoiceBanner?,
    val disclosureOpen: Boolean,
    val toggleMic: () -> Unit,
    val cancel: () -> Unit,
    val confirmProvider: () -> Unit,
    val dismissProvider: () -> Unit
)

@Composable
internal fun rememberFuelVoiceController(
    input: FuelScreenInput,
    existing: FuelExistingEntry?,
    draft: FuelDraft,
    entry: NumberEntryState
): FuelVoiceController {
    val haptic = currentHaptic
    var status by remember { mutableStateOf(VoiceCaptureState.IDLE) }
    var banner by remember { mutableStateOf<FuelVoiceBanner?>(null) }
    var disclosureOpen by remember { mutableStateOf(false) }

    val voice = rememberLocalFirstVoice(
        onNeedsDisclosure = { disclosureOpen = true },
        onState = { next ->
            status = next
            when (next) {
                VoiceCaptureState.FAILED, VoiceCaptureState.NO_SPEECH, VoiceCaptureState.DENIED ->
                    banner = FuelVoiceBanner.Retry
                VoiceCaptureState.UNAVAILABLE, VoiceCaptureState.LOCAL_UNAVAILABLE ->
                    banner = FuelVoiceBanner.Unavailable
                else -> {}
            }
        },
        onText = { transcript ->
            val parsed = SpokenFuelParser.parse(transcript)
            if (parsed == null) {
                banner = FuelVoiceBanner.Unparsed(transcript)
            } else {
                draft.mode = FuelEntryMode.AMOUNT
                parsed.fuelType?.let { nextType ->
                    if (nextType != draft.fuelType) {
                        draft.fuelType = nextType
                        draft.priceTouched = false
                        entry.set("price", formatFuelInputNumber(defaultPriceFor(input, existing, nextType)))
                    }
                }
                entry.set("amount", parsed.amountTaka.toString())
                entry.setActive("amount")
                banner = FuelVoiceBanner.Parsed(transcript)
                haptic.perform(HapticKind.SUCCESS)
            }
        }
    )
    DisposableEffect(Unit) { onDispose { voice.cancel() } }
    val active = status in setOf(
        VoiceCaptureState.REQUESTING,
        VoiceCaptureState.LISTENING,
        VoiceCaptureState.PROCESSING
    )
    return FuelVoiceController(
        active = active,
        banner = banner,
        disclosureOpen = disclosureOpen,
        toggleMic = {
            when {
                active -> voice.stop()
                voice.localAvailable -> { banner = null; voice.start(true) }
                voice.providerAvailable -> { banner = null; if (VoiceProviderConsent.granted) voice.start(false) else disclosureOpen = !disclosureOpen }
                else -> banner = FuelVoiceBanner.Unavailable
            }
        },
        cancel = { voice.cancel(); disclosureOpen = false },
        confirmProvider = { disclosureOpen = false; voice.startProviderWithConsent() },
        dismissProvider = { disclosureOpen = false }
    )
}

internal fun applyPumpMeterReading(
    reading: PumpMeterReading,
    input: FuelScreenInput,
    existing: FuelExistingEntry?,
    draft: FuelDraft,
    entry: NumberEntryState
) {
    reading.fuelType?.let { nextType ->
        draft.fuelType = nextType
    }
    if (reading.pricePerLitre != null) {
        entry.set("price", formatFuelInputNumber(reading.pricePerLitre))
        draft.priceTouched = true
        draft.confirmedPriceKey = null
    } else if (reading.fuelType != null && !draft.priceTouched) {
        entry.set("price", formatFuelInputNumber(defaultPriceFor(input, existing, reading.fuelType)))
    }
    if (reading.amountTaka != null) {
        draft.mode = FuelEntryMode.AMOUNT
        entry.set("amount", formatFuelInputNumber(reading.amountTaka))
        entry.setActive("amount")
    } else if (reading.litres != null) {
        draft.mode = FuelEntryMode.LITRES
        entry.set("litres", formatFuelInputNumber(reading.litres))
        entry.setActive("litres")
    }
}

internal fun evaluateFuelDraft(
    input: FuelScreenInput,
    existing: FuelExistingEntry?,
    today: LocalDate,
    draft: FuelDraft,
    entry: NumberEntryState
): FuelValidationSnapshot = FuelEntryValidation.evaluate(
    input = input,
    existing = existing,
    today = today,
    mode = draft.mode,
    fuelType = draft.fuelType,
    dateKey = draft.dateSelection,
    customDate = draft.customDate,
    amountRaw = entry.values["amount"].orEmpty(),
    litresRaw = entry.values["litres"].orEmpty(),
    priceRaw = entry.values["price"].orEmpty(),
    odoRaw = entry.values["odo"].orEmpty(),
    priceTouched = draft.priceTouched,
    confirmedPriceKey = draft.confirmedPriceKey,
    odometerTouched = draft.odometerTouched,
    fallbackPrice = input.lastPrices[draft.fuelType] ?: defaultPriceForFuel(draft.fuelType)
)

internal fun submitFuelSave(
    eval: FuelValidationSnapshot,
    existing: FuelExistingEntry?,
    draft: FuelDraft,
    legacyDetail: String,
    savedMessage: String,
    onSave: (FuelEntryRequest, String, () -> Unit) -> Unit,
    onEdit: (FuelEditRequest, String) -> Unit
) {
    val savedDate = requireNotNull(eval.date)
    if (existing != null) {
        val unchangedDetail = eval.litres == existing.fuelLiters && draft.fullTank == existing.fullTank
        onEdit(
            FuelEditRequest(
                id = existing.id,
                amount = eval.taka,
                litres = eval.litres,
                price = eval.savedPrice,
                odometer = eval.odometer,
                date = savedDate,
                fullTank = draft.fullTank,
                fuelType = draft.fuelType,
                place = draft.place,
                updatedLegacyDetail = if (unchangedDetail) null else legacyDetail,
                priceConfirmed = eval.priceConfirmed,
                missedFill = draft.missedFill
            ),
            savedMessage
        )
    } else {
        onSave(
            FuelEntryRequest(
                legacyDetail = legacyDetail,
                date = savedDate,
                odometer = eval.odometer,
                litres = eval.litres,
                fullTank = draft.fullTank,
                amount = eval.taka,
                fuelType = draft.fuelType,
                price = eval.price,
                place = draft.place,
                priceConfirmed = eval.priceConfirmed,
                missedFill = draft.missedFill
            ),
            savedMessage
        ) {}
    }
}

internal fun defaultPriceForFuel(fuelType: String): Double =
    if (fuelType == FuelDefaults.PETROL) FuelDefaults.LEGACY_PETROL_TAKA_PER_LITRE
    else FuelDefaults.LEGACY_OCTANE_TAKA_PER_LITRE

internal fun defaultPriceFor(
    input: FuelScreenInput,
    existing: FuelExistingEntry?,
    fuelType: String
): Double = existing?.takeIf { it.fuelType == fuelType }?.fuelPrice
    ?: input.lastPrices[fuelType]
    ?: defaultPriceForFuel(fuelType)

internal fun formatFuelInputNumber(value: Double): String {
    if (!value.isFinite()) return ""
    return if (value % 1.0 == 0.0 && abs(value) <= 1e15) value.toLong().toString() else value.toString()
}
