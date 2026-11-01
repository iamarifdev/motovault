package com.motovault.app.storage

import com.motovault.app.domain.backup.BackupDigits
import com.motovault.app.domain.backup.BackupLanguage
import com.motovault.app.domain.backup.BackupPreferences
import com.motovault.app.domain.backup.BackupReminderPolicy
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.Digits
import com.motovault.app.ui.ReminderPolicy
import com.motovault.app.ui.VaultSettings

internal fun VaultSettings.toBackupPreferences() = BackupPreferences(
    language = BackupLanguage.valueOf(language.name), digits = BackupDigits.valueOf(digits.name),
    haptics = haptics, reduceMotion = reduceMotion, textScale = textScale, brandOverride = brandOverride,
    serviceReminders = serviceReminders, docReminders = docReminders,
    lastPrices = lastPrices, lastItemPrices = lastItemPrices, workshops = workshops, places = places,
    reminderPolicy = BackupReminderPolicy(reminderPolicy.warningDays, reminderPolicy.minuteOfDay),
    privacy = privacy, lastBackupEpochMillis = lastBackupEpochMillis
)

internal fun BackupPreferences.toVaultSettings() = VaultSettings(
    language = AppLanguage.valueOf(language.name), digits = Digits.valueOf(digits.name),
    haptics = haptics, reduceMotion = reduceMotion, textScale = textScale, brandOverride = brandOverride,
    serviceReminders = serviceReminders, docReminders = docReminders,
    lastPrices = lastPrices, lastItemPrices = lastItemPrices, workshops = workshops, places = places,
    reminderPolicy = ReminderPolicy(reminderPolicy.warningDays, reminderPolicy.minuteOfDay),
    privacy = privacy, lastBackupEpochMillis = lastBackupEpochMillis
)
