package com.motovault.app.ui

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class VaultSettingsTest {
    @Test fun legacySettingsUseThirtyDayLocalPolicyWithoutClaimingPermission() {
        val state = requireNotNull(VaultState.decode("""{"appearance":"LIGHT","docReminders":false}"""))
        assertFalse(state.docReminders)
        assertEquals(ReminderPolicy(30, 540), state.reminderPolicy)
        assertEquals(state, VaultSettings.from(state).applyTo(state))
    }

    @Test fun preferencesRoundTripWithoutEmbeddingOwnerRecords() {
        val state = VaultState.seedData().copy(digits = Digits.EN, textScale = 1.3f, haptics = false,
            reduceMotion = true, brandOverride = "suzuki", serviceReminders = false, docReminders = false,
            reminderPolicy = ReminderPolicy(7, 600), workshops = listOf("Owner text"),
            lastPrices = mapOf("petrol" to 117.25), lastItemPrices = mapOf("oil" to 900.0))
        val payload = Json.encodeToString(VaultSettings.from(state))
        assertFalse(payload.contains("documents"))
        assertFalse(payload.contains("\"ride\""))
        assertEquals(state, Json.decodeFromString<VaultSettings>(payload).applyTo(state.copy(digits = Digits.BN)))
    }

    @Test fun invalidPolicyFailsInsteadOfQuietlyRescheduling() {
        assertFailsWith<IllegalArgumentException> { ReminderPolicy(0) }
        assertFailsWith<IllegalArgumentException> { ReminderPolicy(366) }
        assertFailsWith<IllegalArgumentException> { ReminderPolicy(minuteOfDay = 1440) }
        assertFails { Json.decodeFromString<VaultSettings>("""{"reminderPolicy":{"warningDays":-1}}""") }
    }
}
