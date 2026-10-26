package com.motovault.app.ui

import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.model.ComponentRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.*

class DeliveredReminderTest {
    private val beforeDelivery = LocalDateTime.parse("2026-10-24T08:00")
    private fun document() = VaultDocument("Synthetic expiry", "P01-TEST", "SYNTHETIC", "2026-10-24", id = "document", reminder = true)
    private fun documentState() = VaultState(documents = listOf(document()))
    private fun documentPlan() = reminderPlan(documentState(), beforeDelivery, emptyList(), emptySet()).single()
    private fun serviceState() = VaultState(ride = Ride("yamaha", "Synthetic", 2023, 1600.0),
        rules = listOf(ComponentRule("chain", "primary", "Chain", "Owner interval", 500.0, 30,
            1000.0, LocalDate.parse("2026-10-01").toEpochDays().toLong(), baselineCertainty = BaselineCertainty.CONFIRMED)))
    private fun servicePlan() = reminderPlan(serviceState(), beforeDelivery, emptyList(), emptySet()).single()

    @Test fun consumedDocumentEventStaysVisibleWithoutBeingScheduledAgain() {
        val state = documentState()
        val delivered = documentPlan()
        assertTrue(reminderPlan(state, beforeDelivery, listOf(delivered), setOf(delivered.key)).isEmpty())
        assertTrue(deliveredReminderIsCurrent(state, delivered))
        assertTrue(reminderPlan(state, LocalDateTime.parse("2026-10-26T10:00"), emptyList(), setOf(delivered.key)).isEmpty())
        assertTrue(deliveredReminderIsCurrent(state, delivered))
    }

    @Test fun deletedChangedOrDisabledDocumentsRemoveTheirDeliveredAlert() {
        val state = documentState()
        val delivered = documentPlan()
        assertFalse(deliveredReminderIsCurrent(state.copy(docReminders = false), delivered))
        assertFalse(deliveredReminderIsCurrent(state.copy(documents = emptyList()), delivered))
        assertFalse(deliveredReminderIsCurrent(state.copy(documents = listOf(document().copy(expiry = "2026-10-25"))), delivered))
        assertFalse(deliveredReminderIsCurrent(state.copy(documents = listOf(document().copy(reminder = false))), delivered))
    }

    @Test fun documentSnoozeReplacesTheOldAlertWithoutCompletingTheDocument() {
        val original = documentState()
        val snoozed = original.copy(documents = listOf(document().copy(reminderSnoozedUntil = "2026-10-25T09:00")))
        val delivered = reminderPlan(snoozed, beforeDelivery, emptyList(), emptySet()).single()
        assertFalse(deliveredReminderIsCurrent(snoozed, documentPlan()))
        assertTrue(deliveredReminderIsCurrent(snoozed, delivered))
        assertFalse(deliveredReminderIsCurrent(original, delivered))
        assertFalse(deliveredReminderIsCurrent(snoozed.copy(documents = listOf(snoozed.documents.single().copy(reminderSnoozedUntil = "2026-10-26T09:00"))), delivered))
        assertEquals(original.documents.single().expiry, snoozed.documents.single().expiry)
    }

    @Test fun consumedServiceAlertSurvivesOpeningAnotherRecord() {
        val state = serviceState()
        val delivered = servicePlan()
        assertTrue(reminderPlan(state, beforeDelivery, listOf(delivered), setOf(delivered.key)).isEmpty())
        assertTrue(deliveredReminderIsCurrent(state, delivered))
        assertFalse(deliveredReminderIsCurrent(state.copy(serviceReminders = false), delivered))
        assertFalse(deliveredReminderIsCurrent(state.copy(ride = null), delivered))
        assertFalse(deliveredReminderIsCurrent(state.copy(rules = emptyList()), delivered))
        listOf(state.rules.single().copy(reminder = false),
            state.rules.single().copy(lastServicedOdometerKm = 1500.0),
            state.rules.single().copy(baselineCertainty = BaselineCertainty.UNKNOWN)).forEach {
            assertFalse(deliveredReminderIsCurrent(state.copy(rules = listOf(it)), delivered))
        }
    }

    @Test fun serviceSnoozeReplacesItsOldAlertAndCannotSurviveAReset() {
        val original = serviceState()
        val snoozed = original.copy(rules = listOf(original.rules.single().copy(reminderSnoozedUntil = "2026-10-25T09:00")))
        val delivered = reminderPlan(snoozed, beforeDelivery, emptyList(), emptySet()).single()
        assertFalse(deliveredReminderIsCurrent(snoozed, servicePlan()))
        assertTrue(deliveredReminderIsCurrent(snoozed, delivered))
        assertFalse(deliveredReminderIsCurrent(original, delivered))
        assertFalse(deliveredReminderIsCurrent(snoozed.copy(rules = listOf(snoozed.rules.single().copy(lastServicedOdometerKm = 1600.0))), delivered))
    }

    @Test fun malformedDeliveredDataIsRemovedWithoutAffectingOtherAlerts() {
        val state = documentState()
        val delivered = documentPlan()
        assertFalse(deliveredReminderIsCurrent(state, delivered.copy(date = "invalid", kind = "snooze")))
        assertFalse(deliveredReminderIsCurrent(state, delivered.copy(targetType = "unknown")))
        assertTrue(deliveredReminderIsCurrent(state, delivered))
    }
}
