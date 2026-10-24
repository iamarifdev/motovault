package com.motovault.app.ui

import com.motovault.app.domain.model.ComponentRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.*

class ReminderPlanTest {
    private val now = LocalDateTime.parse("2026-10-24T12:00")
    private fun state() = VaultState(ride = Ride("yamaha", "Synthetic", 2023, 1200.0),
        rules = listOf(ComponentRule("Aa", "primary", "Chain", "Owner interval", 500.0, 30,
            1000.0, LocalDate.parse("2026-10-01").toEpochDays().toLong(),
            baselineCertainty = com.motovault.app.domain.model.BaselineCertainty.CONFIRMED)))
    private fun plan(state: VaultState, old: List<RenewalReminder> = emptyList(), consumed: Set<String> = emptySet()) = reminderPlan(state, now, old, consumed)

    @Test fun whicheverLimitFirstAndMileageUpdatesBringCalendarRequestForward() {
        val original = state()
        val first = plan(original)
        assertEquals("2026-10-31", first.single().date)
        val distanceDue = plan(original.copy(ride = original.ride!!.copy(odometer = 1600.0)), first)
        assertEquals("2026-10-25", distanceDue.single().date)
        assertEquals(first.single().key, distanceDue.single().key)
    }

    @Test fun consumedServiceCannotReplayButNewBaselineHasANewIdentity() {
        val original = state()
        val first = plan(original)
        assertTrue(plan(original, first, setOf(first.single().key)).isEmpty())
        val changed = original.copy(rules = original.rules.map { it.copy(lastServicedOdometerKm = 1200.0) })
        assertNotEquals(first.single().key, plan(changed).single().key)
    }

    @Test fun disabledDeletedAndDistanceOnlyNotYetDueProduceNoRequests() {
        assertTrue(plan(state().copy(serviceReminders = false)).isEmpty())
        assertTrue(plan(state().copy(rules = emptyList())).isEmpty())
        assertTrue(plan(state().copy(rules = state().rules.map { it.copy(intervalDays = 0) })).isEmpty())
        assertTrue(plan(state().copy(rules = state().rules.map { it.copy(reminder = false) })).isEmpty())
    }

    @Test fun collisionIdsAndChangedOrMissingNotificationTargetsAreSafe() {
        val state = state().copy(rules = state().rules + state().rules.single().copy(id = "BB"))
        val first = plan(state)
        assertEquals(2, first.map { it.key }.toSet().size)
        val payload = reminderPayload(first.first())
        val request = requireNotNull(decodeReminderPayload(payload))
        assertEquals("Aa", reminderTarget(state, request)!!.id)
        assertNull(reminderTarget(state.copy(rules = emptyList()), request))
        assertNull(reminderTarget(state.copy(rules = state.rules.map { it.copy(intervalDays = 99) }), request))
        assertNull(decodeReminderPayload("invalid"))
    }

    @Test fun snoozeCreatesAnExplicitRequestWithoutErasingConsumedHistory() {
        val original = state()
        val first = plan(original).single()
        val snoozed = original.copy(rules = original.rules.map { it.copy(reminderSnoozedUntil = "2026-11-02T09:00") })
        val next = plan(snoozed, consumed = setOf(first.key)).single()
        assertNotEquals(first.key, next.key)
        assertEquals("2026-11-02T09:00", next.localDateTime().toString())
        assertEquals(original.rules.single().lastServicedEpochDays, snoozed.rules.single().lastServicedEpochDays)
    }

    @Test fun snoozedExpiryMessageKeepsItsActualDateAndChosenDigits() {
        val request = RenewalReminder("synthetic", "document", "2026-10-20", "2026-10-25", "expiry")
        val bn = reminderMessage(request, ReminderTarget("document", "document", "Owner title", Digits.BN))
        assertTrue(bn.contains("২০ অক্টোবর ২০২৬"))
        assertFalse(bn.contains("today"))
        val en = reminderMessage(request, ReminderTarget("document", "document", "Owner title", Digits.EN))
        assertTrue(en.contains("20 October 2026"))
        assertTrue(en.startsWith("Owner title"))
    }

    @Test fun tomorrowSnoozeOfAnOverdueServiceSurvivesRestartWithoutDailyReplay() {
        val overdue = state().copy(ride = state().ride!!.copy(odometer = 1600.0))
        val original = plan(overdue).single()
        val snoozed = overdue.copy(rules = overdue.rules.map { it.copy(reminderSnoozedUntil = "2026-10-25T09:00") })
        val next = plan(snoozed, consumed = setOf(original.key)).single()
        assertNotEquals(original.key, next.key)
        assertEquals("2026-10-25T09:00", next.localDateTime().toString())
        assertTrue(reminderPlan(snoozed, LocalDateTime.parse("2026-10-26T12:00"), emptyList(), setOf(next.key)).isEmpty())
    }
}
