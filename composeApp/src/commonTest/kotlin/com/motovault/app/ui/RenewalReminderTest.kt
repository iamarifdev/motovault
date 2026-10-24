package com.motovault.app.ui

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.test.*

class RenewalReminderTest {
    private fun document(id: String = "document-1", expiry: String = "2026-12-11") = VaultDocument("Tax token", "SAMPLE", "Synthetic", expiry, id = id, reminder = true)
    private fun plan(now: String, docs: List<VaultDocument> = listOf(document()), previous: List<RenewalReminder> = emptyList(), consumed: Set<String> = emptySet()) = renewalPlan(docs, LocalDateTime.parse(now), previous, consumed)
    @Test fun thirtyDayWarningAndExpiryAreAtNine() {
        val reminders = plan("2026-10-01T12:00")
        assertEquals(listOf("2026-11-11", "2026-12-11"), reminders.map { it.date })
        assertTrue(reminders.all { it.localDateTime().hour == 9 })
    }
    @Test fun insideWindowUsesNextMorningAndDoesNotMoveOnRelaunch() {
        val first = plan("2026-12-01T15:00")
        assertEquals("2026-12-02", first.first().date)
        assertEquals(first, plan("2026-12-03T10:00", previous = first))
    }
    @Test fun beforeNineUsesTodayAndAtNineUsesTomorrow() {
        assertEquals("2026-12-01", plan("2026-12-01T08:59").first().date)
        assertEquals("2026-12-02", plan("2026-12-01T09:00").first().date)
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

    @Test fun recordOptOutAndMissingExpiryNeverSchedule() {
        assertTrue(plan("2026-10-01T12:00", listOf(document().copy(reminder = false), document("none", ""))).isEmpty())
    }

    @Test fun globalTimeAndPerRecordLeadOverrideUseLocalCalendar() {
        val events = renewalPlan(listOf(document().copy(reminderWarningDays = 7)), LocalDateTime.parse("2026-10-01T12:00"), emptyList(), emptySet(), ReminderPolicy(30, 18 * 60 + 15))
        assertEquals("2026-12-04T18:15", events.first().localDateTime().toString())
        // The same local request follows device timezone; it is not a fixed UTC instant.
        val dhaka = events.first().localDateTime().toInstant(kotlinx.datetime.TimeZone.of("Asia/Dhaka"))
        val utc = events.first().localDateTime().toInstant(kotlinx.datetime.TimeZone.UTC)
        assertEquals(21600, utc.epochSeconds - dhaka.epochSeconds)
    }

    @Test fun migrationReplacesOldSixtyDayDateWithoutReplayingConsumedEvents() {
        val old = listOf(RenewalReminder("renewal:10:document-1:2026-12-11:warning", "document-1", "2026-12-11", "2026-10-12", "warning"))
        assertEquals("2026-11-11", plan("2026-10-01T12:00", previous = old).first().date)
        assertEquals(listOf("expiry"), plan("2026-10-01T12:00", previous = old, consumed = setOf(old.single().key)).map { it.kind })
    }

    @Test fun snoozeDefersWithoutChangingExpiryAndDeduplicatesSameTime() {
        val doc = document().copy(reminderSnoozedUntil = "2026-12-12T09:00")
        val result = plan("2026-12-01T12:00", listOf(doc))
        assertEquals(1, result.size)
        assertEquals("2026-12-12T09:00", result.single().localDateTime().toString())
        assertEquals("2026-12-11", doc.expiry)
    }

    @Test fun consumedWarningCanSnoozeTomorrowWithoutRestoringTheOldCatchUp() {
        val first = plan("2026-12-01T12:00")
        val doc = document().copy(reminderSnoozedUntil = "2026-12-02T09:00")
        val next = plan("2026-12-01T12:00", listOf(doc), consumed = setOf(first.first().key))
        val snoozed = next.single { it.kind == "snooze" }
        assertEquals("2026-12-02T09:00", snoozed.localDateTime().toString())
        val restarted = plan("2026-12-03T12:00", listOf(doc), consumed = setOf(snoozed.key))
        assertEquals(listOf("expiry"), restarted.map { it.kind })
    }
}
