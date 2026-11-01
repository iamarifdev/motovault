package com.motovault.app.storage

import com.motovault.app.domain.privacy.VaultPrivacySettings
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.Digits
import com.motovault.app.ui.ReminderPolicy
import com.motovault.app.ui.VaultSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupPreferencesAdapterTest {
    @Test
    fun preservesAllSettingsAndWireRepresentation() {
        val settings = VaultSettings(
            language = AppLanguage.EN, digits = Digits.EN, haptics = false, reduceMotion = true,
            textScale = 1.2f, brandOverride = "synthetic", serviceReminders = false, docReminders = false,
            lastPrices = mapOf("octane" to 100.0), lastItemPrices = mapOf("oil" to 200.0),
            workshops = listOf("Synthetic workshop"), places = listOf("Synthetic place"),
            reminderPolicy = ReminderPolicy(14, 600), privacy = VaultPrivacySettings(enabled = true, timeoutSeconds = 300),
            lastBackupEpochMillis = 1000L
        )
        val backup = settings.toBackupPreferences()
        assertEquals(settings, backup.toVaultSettings())
        assertEquals(Json.parseToJsonElement(Json.encodeToString(settings)), Json.parseToJsonElement(Json.encodeToString(backup)))
    }
}
