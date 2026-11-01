package com.motovault.app.domain.model

import com.motovault.app.domain.privacy.VaultPrivacySettings
import kotlinx.serialization.Serializable

@Serializable
enum class Appearance {
    SYSTEM, LIGHT, DARK
}

@Serializable
enum class AppLanguage(val code: String) {
    BN("bn"), EN("en")
}

@Serializable
enum class Digits {
    BN, EN
}

@Serializable
data class ReminderPolicy(val warningDays: Int = 30, val minuteOfDay: Int = 540) {
    init {
        require(warningDays in 1..365) { "Choose a warning between 1 and 365 days." }
        require(minuteOfDay in 0..1439) { "Choose a valid local reminder time." }
    }
}

@Serializable
enum class RideMarket {
    UNKNOWN, INDIA, EUROPE, BANGLADESH
}

@Serializable
enum class RideEmission {
    UNKNOWN, BS4, BS6, EURO4, EURO5, EURO5_PLUS
}
