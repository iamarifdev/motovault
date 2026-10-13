package com.motovault.app.ui.redesign

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class RenewalJournalTest {
    private val now = LocalDateTime.parse("2026-10-13T12:00")
    private val due = RenewalReminder("renewal:7:fixture:2026-10-13:expiry", "fixture", "2026-10-13", "2026-10-13", "expiry")
    private fun restart(journal: RenewalJournal) = Json.decodeFromString<RenewalJournal>(Json.encodeToString(journal))

    @Test fun acceptedThenTerminatedBeforeCompletionCannotReplay() {
        val durableBeforeOsAdd = RenewalJournal().reserve(due)
        // OS accepts and delivers; process dies without any post-acceptance journal write.
        val recovered = restart(durableBeforeOsAdd).recover(now, emptySet(), emptySet())
        assertTrue(due.key in recovered.consumed)
        val document = VaultDocument("Fixture", "SAMPLE", "Synthetic", due.expiry, id = due.documentId)
        assertTrue(renewalPlan(listOf(document), now, recovered.requests, recovered.consumed).isEmpty())
    }

    @Test fun interruptedBeforeSubmissionHasExplicitAtMostOnceTradeoff() {
        assertTrue(due.key in restart(RenewalJournal().reserve(due)).recover(now, emptySet(), emptySet()).consumed)
    }

    @Test fun explicitRejectionRemovesReservationAndRelaunchRetries() {
        val recovered = restart(RenewalJournal().reserve(due).rejected(due.key)).recover(now, emptySet(), emptySet())
        val document = VaultDocument("Fixture", "SAMPLE", "Synthetic", due.expiry, id = due.documentId)
        assertEquals(listOf(due.documentId), renewalPlan(listOf(document), now, recovered.requests, recovered.consumed).map { it.documentId })
    }

    @Test fun futureUnsentReservationStillRetriesAndPendingElapsedIsPreserved() {
        val future = due.copy(key = "future", date = "2026-10-14")
        val recovered = restart(RenewalJournal().reserve(due).reserve(future)).recover(now, setOf(due.key), emptySet())
        assertTrue(recovered.consumed.isEmpty())
        assertEquals(listOf(due, future), recovered.requests)
    }

    @Test fun deliveredIdentitySurvivesBackwardClockAndCancellation() {
        val future = due.copy(date = "2026-10-14")
        val recovered = restart(RenewalJournal().reserve(future)).recover(now, emptySet(), setOf(due.key)).retain(emptySet())
        assertTrue(due.key in recovered.consumed)
        assertTrue(recovered.requests.isEmpty())
    }
}
