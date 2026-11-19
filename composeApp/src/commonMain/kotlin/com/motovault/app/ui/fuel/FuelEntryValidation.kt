package com.motovault.app.ui.fuel

import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.FuelEntryRules
import com.motovault.app.domain.fuel.FuelTimelineEntry
import com.motovault.app.domain.fuel.MAX_FUEL_LITRES
import com.motovault.app.domain.input.NormalizedNumbers
import com.motovault.app.ui.EntryDateKey
import kotlinx.datetime.LocalDate

private const val MAX_ODOMETER_KM = 9_999_999.0
private const val HIGH_LITRES_WARNING = 40.0

internal enum class FuelValidationReason {
    ENTER_AMOUNT,
    ENTER_LITRES,
    ENTER_PRICE,
    CONFIRM_PRICE,
    CONFIRM_ODOMETER,
    LITRES_TOO_HIGH,
    INVALID_ODOMETER,
    ODOMETER_TOO_LOW,
    TIMELINE_CONFLICT,
    INVALID_DATE,
    FUTURE_DATE,
    DATE_BEFORE_LAST_FILL,
    RECORD_MISSING
}

internal data class FuelValidationSnapshot(
    val taka: Double,
    val litres: Double,
    val price: Double,
    val savedPrice: Double?,
    val odometer: Double,
    val date: LocalDate?,
    val priceKnown: Boolean,
    val priceConfirmed: Boolean,
    val priceKey: String,
    val literWarning: Boolean,
    val odometerError: FuelValidationReason?,
    val reason: FuelValidationReason?
)

internal object FuelEntryValidation {
    fun evaluate(
        input: FuelScreenInput,
        existing: FuelExistingEntry?,
        today: LocalDate,
        mode: FuelEntryMode,
        fuelType: String,
        dateKey: EntryDateKey,
        customDate: String,
        amountRaw: String,
        litresRaw: String,
        priceRaw: String,
        odoRaw: String,
        priceTouched: Boolean,
        confirmedPriceKey: String?,
        odometerTouched: Boolean,
        fallbackPrice: Double
    ): FuelValidationSnapshot {
        val price = NormalizedNumbers.parse(priceRaw) ?: 0.0
        val amountVal = NormalizedNumbers.parse(amountRaw) ?: 0.0
        val litresVal = NormalizedNumbers.parse(litresRaw) ?: 0.0
        val parsedOdo = NormalizedNumbers.parse(odoRaw)
        val odoVal = parsedOdo ?: 0.0

        val calculation = FuelEntryRules.calculate(
            mode, amountVal, litresVal, price, odoVal,
            if (existing == null) input.lastFuelOdometer else 0.0
        )
        val editCalculation = existing?.let {
            val initialDisplayed = it.fuelPrice ?: fallbackPrice
            FuelEntryRules.calculateEdit(
                mode, amountVal, litresVal, price,
                it.cost, it.fuelLiters, it.fuelPrice, initialDisplayed,
                fuelType == it.fuelType, priceTouched
            )
        }
        val taka = editCalculation?.amount ?: calculation.amount
        val litres = editCalculation?.litres ?: calculation.litres
        val savedPrice = if (editCalculation == null) price else editCalculation.price

        val date = resolveDate(dateKey, customDate, today)
        val priceKey = "$fuelType:$savedPrice"
        val priceKnown = (savedPrice != null && input.confirmedPrices[fuelType]?.takaPerLitre == savedPrice) ||
            (existing != null && existing.fuelPrice == savedPrice && existing.fuelType == fuelType)
        val priceConfirmed = priceKnown || confirmedPriceKey == priceKey

        val timelineIssue = resolveTimelineIssue(input, existing, parsedOdo, odoVal, date)
        val odometerError = when {
            existing == null && parsedOdo != null && calculation.odometerTooLow -> FuelValidationReason.ODOMETER_TOO_LOW
            existing != null && timelineIssue == FuelEntryIssue.TIMELINE_CONFLICT -> FuelValidationReason.TIMELINE_CONFLICT
            else -> null
        }

        val reason = firstReason(
            input, existing, today, mode, amountVal, litresVal,
            taka, litres, price, savedPrice, priceConfirmed, odometerTouched,
            parsedOdo, odoVal, odometerError, timelineIssue, date
        )
        return FuelValidationSnapshot(
            taka = taka,
            litres = litres,
            price = price,
            savedPrice = savedPrice,
            odometer = odoVal,
            date = date,
            priceKnown = priceKnown,
            priceConfirmed = priceConfirmed,
            priceKey = priceKey,
            literWarning = litres > HIGH_LITRES_WARNING,
            odometerError = odometerError,
            reason = reason
        )
    }

    private fun resolveDate(dateKey: EntryDateKey, customDate: String, today: LocalDate): LocalDate? =
        when (dateKey) {
            EntryDateKey.TODAY -> today
            EntryDateKey.YESTERDAY -> LocalDate.fromEpochDays(today.toEpochDays() - 1)
            EntryDateKey.CUSTOM -> runCatching { LocalDate.parse(NormalizedNumbers.ascii(customDate)) }.getOrNull()
        }

    private fun resolveTimelineIssue(
        input: FuelScreenInput,
        existing: FuelExistingEntry?,
        parsedOdo: Double?,
        odoVal: Double,
        date: LocalDate?
    ): FuelEntryIssue? {
        if (existing == null) return null
        if (input.timeline.any { it == null }) return FuelEntryIssue.TIMELINE_CONFLICT
        if (date == null || parsedOdo == null) return null
        return FuelEntryRules.validateEditOrder(
            FuelTimelineEntry(existing.id, odoVal, date),
            input.timeline.filterNotNull()
        )
    }

    private fun firstReason(
        input: FuelScreenInput,
        existing: FuelExistingEntry?,
        today: LocalDate,
        mode: FuelEntryMode,
        amountVal: Double,
        litresVal: Double,
        taka: Double,
        litres: Double,
        price: Double,
        savedPrice: Double?,
        priceConfirmed: Boolean,
        odometerTouched: Boolean,
        parsedOdo: Double?,
        odoVal: Double,
        odometerError: FuelValidationReason?,
        timelineIssue: FuelEntryIssue?,
        date: LocalDate?
    ): FuelValidationReason? {
        if (existing == null) {
            val primaryMissing = if (mode == FuelEntryMode.AMOUNT) !amountVal.isFinite() || amountVal <= 0.0
            else !litresVal.isFinite() || litresVal <= 0.0
            if (primaryMissing) {
                return if (mode == FuelEntryMode.AMOUNT) FuelValidationReason.ENTER_AMOUNT
                else FuelValidationReason.ENTER_LITRES
            }
            if (!price.isFinite() || price <= 0.0) return FuelValidationReason.ENTER_PRICE
        } else {
            if (existing.fuelLiters == null && mode == FuelEntryMode.AMOUNT) return FuelValidationReason.ENTER_LITRES
            if (!taka.isFinite() || taka < 0.0) return FuelValidationReason.ENTER_AMOUNT
            if (!litres.isFinite() || litres <= 0.0) return FuelValidationReason.ENTER_LITRES
            if (savedPrice != null && (!savedPrice.isFinite() || savedPrice <= 0.0)) return FuelValidationReason.ENTER_PRICE
        }
        return when {
            !priceConfirmed -> FuelValidationReason.CONFIRM_PRICE
            !odometerTouched -> FuelValidationReason.CONFIRM_ODOMETER
            litres > MAX_FUEL_LITRES -> FuelValidationReason.LITRES_TOO_HIGH
            parsedOdo == null || !odoVal.isFinite() || odoVal !in 0.0..MAX_ODOMETER_KM -> FuelValidationReason.INVALID_ODOMETER
            odometerError != null -> odometerError
            date == null -> FuelValidationReason.INVALID_DATE
            date > today -> FuelValidationReason.FUTURE_DATE
            existing == null && input.lastFuelDate != null && date < input.lastFuelDate -> FuelValidationReason.DATE_BEFORE_LAST_FILL
            timelineIssue == FuelEntryIssue.RECORD_MISSING -> FuelValidationReason.RECORD_MISSING
            else -> null
        }
    }
}
