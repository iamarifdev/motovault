package com.motovault.app.domain.vault

import com.motovault.app.domain.model.AppLanguage
import com.motovault.app.domain.model.Digits
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.model.ReminderPolicy
import com.motovault.app.domain.privacy.VaultPrivacySettings
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
    val lastBackupEpochMillis: Long? = null
) {
    fun applyTo(state: VaultState) = state.copy(
        language = language, digits = digits, haptics = haptics, reduceMotion = reduceMotion, textScale = textScale,
        brandOverride = brandOverride, serviceReminders = serviceReminders, docReminders = docReminders,
        lastPrices = lastPrices, lastItemPrices = lastItemPrices, workshops = workshops, places = places,
        reminderPolicy = reminderPolicy, privacy = privacy, lastBackupEpochMillis = lastBackupEpochMillis
    )

    companion object {
        fun from(state: VaultState) = VaultSettings(
            state.language, state.digits, state.haptics, state.reduceMotion, state.textScale, state.brandOverride,
            state.serviceReminders, state.docReminders, state.lastPrices, state.lastItemPrices,
            state.workshops, state.places, state.reminderPolicy, state.privacy, state.lastBackupEpochMillis
        )
    }
}
