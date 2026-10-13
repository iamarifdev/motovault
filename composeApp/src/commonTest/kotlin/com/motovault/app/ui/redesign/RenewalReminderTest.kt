package com.motovault.app.ui.redesign

import kotlinx.datetime.LocalDateTime
import kotlin.test.*

class RenewalReminderTest {
    private fun document(id: String = "document-1", expiry: String = "2026-12-11") = VaultDocument("Tax token", "SAMPLE", "Synthetic", expiry, id = id)
    private fun plan(now: String, docs: List<VaultDocument> = listOf(document()), previous: List<RenewalReminder> = emptyList(), consumed: Set<String> = emptySet()) = renewalPlan(docs, LocalDateTime.parse(now), previous, consumed)
    @Test fun sixtyDayWarningAndExpiryAreAtNine() {
        val reminders = plan("2026-10-01T12:00")
        assertEquals(listOf("2026-10-12", "2026-12-11"), reminders.map { it.date })
        assertTrue(reminders.all { it.localDateTime().hour == 9 })
    }
    @Test fun insideWindowUsesNextMorningAndDoesNotMoveOnRelaunch() {
        val first = plan("2026-11-01T15:00")
        assertEquals("2026-11-02", first.first().date)
        assertEquals(first, plan("2026-11-03T10:00", previous = first))
    }
    @Test fun beforeNineUsesTodayAndAtNineUsesTomorrow() {
        assertEquals("2026-11-01", plan("2026-11-01T08:59").first().date)
        assertEquals("2026-11-02", plan("2026-11-01T09:00").first().date)
    }
    @Test fun expiredMissingAndInvalidDatesHaveNoNativeReminders() {
        assertTrue(plan("2026-12-12T12:00", listOf(document(), document("none", ""), document("bad", "invalid"))).isEmpty())
    }
    @Test fun expiryDayUsesOneExpiryAlertIncludingLateAddition() {
        assertEquals(listOf("expiry"), plan("2026-12-11T12:00").map { it.kind })
    }
    @Test fun editingExpiryReplacesIdentityAndDeletingCancelsEverything() {
        val old = plan("2026-10-01T12:00")
        val changed = plan("2026-10-01T12:00", listOf(document(expiry = "2027-01-01")), old)
        assertTrue(changed.none { item -> old.any { it.key == item.key } })
        assertTrue(plan("2026-10-01T12:00", emptyList(), old).isEmpty())
    }
    @Test fun consumedReminderNeverReappearsEvenAfterClockMovesBack() {
        val first = plan("2026-10-01T12:00")
        val remaining = plan("2026-09-01T12:00", previous = first, consumed = setOf(first.first().key))
        assertEquals(listOf("expiry"), remaining.map { it.kind })
    }
    @Test fun fullIdsAvoidJavaHashCollisionsAndDisplayEditKeepsIdentity() {
        val events = plan("2026-10-01T12:00", listOf(document("Aa"), document("BB")))
        assertEquals(4, events.map { it.key }.toSet().size)
        assertEquals(plan("2026-10-01T12:00").map { it.key }, plan("2026-10-01T12:00", listOf(document().copy(title = "Changed"))).map { it.key })
    }
}
