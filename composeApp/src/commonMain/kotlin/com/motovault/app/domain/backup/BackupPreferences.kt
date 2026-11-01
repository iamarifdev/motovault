package com.motovault.app.domain.backup

import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.privacy.VaultPrivacySettings
import kotlinx.serialization.Serializable

private const val DEFAULT_REMINDER_WARNING_DAYS = 30
private const val DEFAULT_REMINDER_MINUTE_OF_DAY = 540
private const val MIN_REMINDER_WARNING_DAYS = 1
private const val MAX_REMINDER_WARNING_DAYS = 365
private const val MIN_MINUTE_OF_DAY = 0
private const val MAX_MINUTE_OF_DAY = 1439

@Serializable
enum class BackupLanguage { BN, EN }

@Serializable
enum class BackupDigits { BN, EN }

@Serializable
data class BackupReminderPolicy(val warningDays: Int = DEFAULT_REMINDER_WARNING_DAYS, val minuteOfDay: Int = DEFAULT_REMINDER_MINUTE_OF_DAY) {
    init {
        require(warningDays in MIN_REMINDER_WARNING_DAYS..MAX_REMINDER_WARNING_DAYS)
        require(minuteOfDay in MIN_MINUTE_OF_DAY..MAX_MINUTE_OF_DAY)
    }
}

/** V1 preference fields and defaults are part of the portable archive contract. */
@Serializable
data class BackupPreferences(
    val language: BackupLanguage = BackupLanguage.BN,
    val digits: BackupDigits = BackupDigits.BN,
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
    val reminderPolicy: BackupReminderPolicy = BackupReminderPolicy(),
    val privacy: VaultPrivacySettings = VaultPrivacySettings(),
    val lastBackupEpochMillis: Long? = null
)
