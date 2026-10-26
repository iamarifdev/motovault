package com.motovault.app.ui

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val code: String, val label: String) {
    BN("bn", "বাংলা"),
    EN("en", "English")
}

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
    val lastPrices: Map<String, Double> = mapOf("octane" to 120.0, "petrol" to 115.0),
    val lastItemPrices: Map<String, Double> = emptyMap(),
    val workshops: List<String> = emptyList(),
    val places: List<String> = emptyList(),
    val reminderPolicy: ReminderPolicy = ReminderPolicy(),
    val privacy: com.motovault.app.domain.privacy.VaultPrivacySettings = com.motovault.app.domain.privacy.VaultPrivacySettings()
) {
    fun applyTo(state: VaultState) = state.copy(
        language = language, digits = digits, haptics = haptics, reduceMotion = reduceMotion, textScale = textScale,
        brandOverride = brandOverride, serviceReminders = serviceReminders, docReminders = docReminders,
        lastPrices = lastPrices, lastItemPrices = lastItemPrices, workshops = workshops, places = places,
        reminderPolicy = reminderPolicy, privacy = privacy
    )

    companion object {
        fun from(state: VaultState) = VaultSettings(
            state.language, state.digits, state.haptics, state.reduceMotion, state.textScale, state.brandOverride,
            state.serviceReminders, state.docReminders, state.lastPrices, state.lastItemPrices,
            state.workshops, state.places, state.reminderPolicy, state.privacy
        )
    }
}

@Serializable
data class ReminderPolicy(val warningDays: Int = 30, val minuteOfDay: Int = 540) {
    init {
        require(warningDays in 1..365) { "Choose a warning between 1 and 365 days." }
        require(minuteOfDay in 0..1439) { "Choose a valid local reminder time." }
    }
}
