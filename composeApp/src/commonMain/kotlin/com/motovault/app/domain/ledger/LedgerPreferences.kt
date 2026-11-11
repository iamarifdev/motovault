package com.motovault.app.domain.ledger

import kotlinx.serialization.Serializable

@Serializable
data class ConfirmedFuelPrice(val takaPerLitre: Double, val effectiveDate: String)

@Serializable
data class LedgerPreferences(
    val monthlyBudgetPoisha: Long? = null,
    val maintenanceAllowancePoisha: Long? = null,
    val confirmedPrices: Map<String, ConfirmedFuelPrice> = emptyMap()
) {
    init {
        require(monthlyBudgetPoisha == null || monthlyBudgetPoisha in 0..MAX_COST_POISHA)
        require(maintenanceAllowancePoisha == null || maintenanceAllowancePoisha in 0..MAX_COST_POISHA)
        confirmedPrices.values.forEach {
            require(it.takaPerLitre.isFinite() && it.takaPerLitre > 0)
            kotlinx.datetime.LocalDate.parse(it.effectiveDate)
        }
    }

    fun confirmPrice(type: String, price: Double, effectiveDate: String): LedgerPreferences {
        require(price.isFinite() && price > 0)
        kotlinx.datetime.LocalDate.parse(effectiveDate)
        val prior = confirmedPrices[type]
        return if (prior != null && prior.effectiveDate > effectiveDate) this
        else copy(confirmedPrices = confirmedPrices + (type to ConfirmedFuelPrice(price, effectiveDate)))
    }
}
