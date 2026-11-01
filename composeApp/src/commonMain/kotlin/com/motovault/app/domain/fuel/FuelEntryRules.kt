package com.motovault.app.domain.fuel

import kotlinx.datetime.LocalDate

enum class FuelEntryMode { AMOUNT, LITRES }

enum class FuelEntryIssue { ENTER_AMOUNT, ENTER_LITRES, ENTER_PRICE, LITRES_TOO_HIGH, INVALID_ODOMETER, ODOMETER_TOO_LOW, INVALID_DATE, FUTURE_DATE, DATE_BEFORE_LAST_FILL, RECORD_MISSING, TIMELINE_CONFLICT }

const val MAX_FUEL_LITRES = 100.0
private const val MAX_ODOMETER_KM = 9999999.0

data class FuelEntryCalculation(
    val amount: Double,
    val litres: Double,
    val odometerTooLow: Boolean,
    val issue: FuelEntryIssue?
)

data class FuelTimelineEntry(val id: String, val odometer: Double, val date: LocalDate)
data class FuelEditCalculation(val amount: Double, val litres: Double, val price: Double?)

/** Calculation and validation for a new fill, independent of display language and storage. */
object FuelEntryRules {
    fun calculateEdit(mode: FuelEntryMode, amount: Double, litres: Double, price: Double, originalAmount: Double, originalLitres: Double?, originalPrice: Double?, initialDisplayedPrice: Double, sameFuelType: Boolean, priceTouched: Boolean = false): FuelEditCalculation {
        val calculated = calculate(mode, amount, litres, price, 0.0, 0.0)
        val unchangedPrice = price == initialDisplayedPrice && sameFuelType && !priceTouched
        if (originalPrice == null && unchangedPrice) {
            return FuelEditCalculation(
                if (mode == FuelEntryMode.AMOUNT) amount else originalAmount,
                if (mode == FuelEntryMode.LITRES) litres else originalLitres ?: calculated.litres,
                null
            )
        }
        val finalAmount = if (mode == FuelEntryMode.LITRES && unchangedPrice && litres == originalLitres) originalAmount else calculated.amount
        val finalLitres = if (mode == FuelEntryMode.AMOUNT && unchangedPrice && amount == originalAmount) originalLitres ?: calculated.litres else calculated.litres
        val finalPrice = if (originalPrice == null && unchangedPrice) null else price
        return FuelEditCalculation(finalAmount, finalLitres, finalPrice)
    }

    fun calculate(mode: FuelEntryMode, amount: Double, litres: Double, price: Double, odometer: Double, previousOdometer: Double): FuelEntryCalculation {
        val computedAmount = if (mode == FuelEntryMode.AMOUNT) amount else litres * price
        val computedLitres = if (mode == FuelEntryMode.AMOUNT) {
            if (price > 0) amount / price else 0.0
        } else litres
        val odometerTooLow = odometer <= previousOdometer && previousOdometer > 0
        val issue = when {
            !computedAmount.isFinite() || computedAmount <= 0 -> if (mode == FuelEntryMode.AMOUNT) FuelEntryIssue.ENTER_AMOUNT else FuelEntryIssue.ENTER_LITRES
            !price.isFinite() || price <= 0 -> FuelEntryIssue.ENTER_PRICE
            !computedLitres.isFinite() || computedLitres <= 0 -> FuelEntryIssue.ENTER_LITRES
            computedLitres > MAX_FUEL_LITRES -> FuelEntryIssue.LITRES_TOO_HIGH
            !odometer.isFinite() || odometer < 0 || odometer > MAX_ODOMETER_KM -> FuelEntryIssue.INVALID_ODOMETER
            odometerTooLow -> FuelEntryIssue.ODOMETER_TOO_LOW
            else -> null
        }
        return FuelEntryCalculation(computedAmount, computedLitres, odometerTooLow, issue)
    }

    fun validateDate(date: LocalDate?, today: LocalDate, lastFuelDate: LocalDate?): FuelEntryIssue? = when {
        date == null -> FuelEntryIssue.INVALID_DATE
        date > today -> FuelEntryIssue.FUTURE_DATE
        lastFuelDate != null && date < lastFuelDate -> FuelEntryIssue.DATE_BEFORE_LAST_FILL
        else -> null
    }

    fun validateSaved(amount: Double, litres: Double, price: Double, odometer: Double, previousOdometer: Double, date: LocalDate, today: LocalDate, lastFuelDate: LocalDate?): FuelEntryIssue? = when {
        !amount.isFinite() || amount <= 0 -> FuelEntryIssue.ENTER_AMOUNT
        !price.isFinite() || price <= 0 -> FuelEntryIssue.ENTER_PRICE
        !litres.isFinite() || litres <= 0 -> FuelEntryIssue.ENTER_LITRES
        litres > MAX_FUEL_LITRES -> FuelEntryIssue.LITRES_TOO_HIGH
        !odometer.isFinite() || odometer < 0 || odometer > MAX_ODOMETER_KM -> FuelEntryIssue.INVALID_ODOMETER
        previousOdometer > 0 && odometer <= previousOdometer -> FuelEntryIssue.ODOMETER_TOO_LOW
        else -> validateDate(date, today, lastFuelDate)
    }

    /** Replace a record in place only when the whole fuel timeline remains ordered. */
    fun validateEditOrder(candidate: FuelTimelineEntry, entries: List<FuelTimelineEntry>): FuelEntryIssue? {
        if (entries.count { it.id == candidate.id } != 1) return FuelEntryIssue.RECORD_MISSING
        val ordered = entries.map { if (it.id == candidate.id) candidate else it }.sortedBy { it.odometer }
        return if (ordered.zipWithNext().any { (before, after) ->
                after.odometer <= before.odometer || after.date < before.date
            }) FuelEntryIssue.TIMELINE_CONFLICT else null
    }

    /** Existing records may legitimately have zero cost or no recorded unit price. */
    fun validateEditedValues(amount: Double, litres: Double, price: Double?, odometer: Double, date: LocalDate, today: LocalDate): FuelEntryIssue? = when {
        !amount.isFinite() || amount < 0 -> FuelEntryIssue.ENTER_AMOUNT
        !litres.isFinite() || litres <= 0 -> FuelEntryIssue.ENTER_LITRES
        litres > MAX_FUEL_LITRES -> FuelEntryIssue.LITRES_TOO_HIGH
        price != null && (!price.isFinite() || price < 0) -> FuelEntryIssue.ENTER_PRICE
        !odometer.isFinite() || odometer < 0 || odometer > MAX_ODOMETER_KM -> FuelEntryIssue.INVALID_ODOMETER
        else -> validateDate(date, today, null)
    }
}
