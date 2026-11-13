package com.motovault.app.domain.vault

import com.motovault.app.domain.model.AppLanguage
import com.motovault.app.domain.model.Digits
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.model.GarageBikeRecord
import com.motovault.app.domain.model.ReminderPolicy
import com.motovault.app.domain.privacy.VaultPrivacySettings
import com.motovault.app.domain.model.PartWarranty
import com.motovault.app.domain.model.RepairCase
import com.motovault.app.domain.model.WorkshopContact
import kotlinx.serialization.Serializable

/** Preferences are committed with owner records; notification permission remains an OS decision. */
@Serializable
data class VaultSettings(
    val language: AppLanguage = AppLanguage.BN,
    val digits: Digits = Digits.BN,
    val haptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val textScale: Float = 1f,
    val brandOverride: String? = null,
    val serviceReminders: Boolean = true,
    val docReminders: Boolean = true,
    val lastPrices: Map<String, Double> = FuelDefaults.legacyPrices(),
    val lastItemPrices: Map<String, Double> = emptyMap(),
    val workshops: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val reminderPolicy: ReminderPolicy = ReminderPolicy(),
    val privacy: VaultPrivacySettings = VaultPrivacySettings(),
    val lastBackupEpochMillis: Long? = null,
    val ledger: com.motovault.app.domain.ledger.LedgerPreferences = com.motovault.app.domain.ledger.LedgerPreferences(),
    val captureReceipts: List<String> = emptyList(),
    val garageBikes: List<GarageBikeRecord> = emptyList(),
    val workshopContacts: List<WorkshopContact> = emptyList(),
    val repairCases: List<RepairCase> = emptyList(),
    val warranties: List<PartWarranty> = emptyList(),
    val widgetBikeId: String? = null,
    val checkedSeasonalItems: Set<String> = emptySet(),
    val dismissedSeasonKeys: Set<String> = emptySet(),
    val breakdownContacts: List<com.motovault.app.domain.model.BreakdownContact> = emptyList(),
    val weatherCache: com.motovault.app.domain.model.WeatherCacheSnapshot = com.motovault.app.domain.model.WeatherCacheSnapshot()
) {
    fun applyTo(state: VaultState) = state.copy(
        language = language, digits = digits, haptics = haptics, reduceMotion = reduceMotion, textScale = textScale,
        brandOverride = brandOverride, serviceReminders = serviceReminders, docReminders = docReminders,
        lastPrices = lastPrices, lastItemPrices = lastItemPrices, workshops = workshops, places = places,
        reminderPolicy = reminderPolicy, privacy = privacy, lastBackupEpochMillis = lastBackupEpochMillis, ledger = ledger,
        captureReceipts = captureReceipts, garageBikes = garageBikes,
        workshopContacts = workshopContacts, repairCases = repairCases, warranties = warranties,
        widgetBikeId = widgetBikeId,
        checkedSeasonalItems = checkedSeasonalItems,
        dismissedSeasonKeys = dismissedSeasonKeys,
        breakdownContacts = breakdownContacts,
        weatherCache = weatherCache
    )

    companion object {
        fun from(state: VaultState) = VaultSettings(
            state.language, state.digits, state.haptics, state.reduceMotion, state.textScale, state.brandOverride,
            state.serviceReminders, state.docReminders, state.lastPrices, state.lastItemPrices,
            state.workshops, state.places, state.reminderPolicy, state.privacy, state.lastBackupEpochMillis, state.ledger,
            state.captureReceipts, state.garageBikes, state.workshopContacts, state.repairCases, state.warranties,
            state.widgetBikeId, state.checkedSeasonalItems, state.dismissedSeasonKeys,
            state.breakdownContacts, state.weatherCache
        )
    }
}
