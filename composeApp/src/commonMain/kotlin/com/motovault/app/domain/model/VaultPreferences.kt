package com.motovault.app.domain.model

import com.motovault.app.domain.privacy.VaultPrivacySettings
import kotlinx.serialization.Serializable

@Serializable
enum class Appearance(val label: String) {
    SYSTEM("ডিভাইসের মতো"),
    LIGHT("লাইট"),
    DARK("ডার্ক")
}

@Serializable
enum class AppLanguage(val code: String, val label: String) {
    BN("bn", "বাংলা"),
    EN("en", "English")
}

@Serializable
enum class Digits(val label: String) {
    BN("১২৩ বাংলা"),
    EN("123 English")
}

@Serializable
data class ReminderPolicy(val warningDays: Int = 30, val minuteOfDay: Int = 540) {
    init {
        require(warningDays in 1..365) { "Choose a warning between 1 and 365 days." }
        require(minuteOfDay in 0..1439) { "Choose a valid local reminder time." }
    }
}

@Serializable
enum class RideMarket(val label: String) {
    UNKNOWN("Not sure / other market"), INDIA("India"), EUROPE("Europe / UK"), BANGLADESH("Bangladesh")
}

@Serializable
enum class RideEmission(val label: String) {
    UNKNOWN("Not confirmed"), BS4("BS4"), BS6("BS6"), EURO4("Euro 4"), EURO5("Euro 5"), EURO5_PLUS("Euro 5+")
}
