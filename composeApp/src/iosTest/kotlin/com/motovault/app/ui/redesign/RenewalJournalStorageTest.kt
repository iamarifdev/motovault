@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui.redesign

import kotlinx.datetime.LocalDateTime
import platform.Foundation.*
import kotlin.test.*

class RenewalJournalStorageTest {
    @Test fun diskReservationSurvivesNewStorageInstanceWithoutCompletionWrite() {
        val directory = NSTemporaryDirectory() + "renewal-test-" + NSUUID().UUIDString
        try {
            val request = RenewalReminder("renewal:7:fixture:2026-10-13:expiry", "fixture", "2026-10-13", "2026-10-13", "expiry")
            RenewalJournalStorage(directory).write(RenewalJournal().reserve(request))
            // Recreate the adapter after simulated OS acceptance/process termination.
            val reopened = assertNotNull(RenewalJournalStorage(directory).read())
            assertTrue(request.key in reopened.recover(LocalDateTime.parse("2026-10-13T12:00"), emptySet(), emptySet()).consumed)
            assertFalse(NSFileManager.defaultManager.fileExistsAtPath("$directory/renewal-journal.json.tmp"))
        } finally { NSFileManager.defaultManager.removeItemAtPath(directory, null) }
    }

    @Test fun replacementPersistsExplicitRejectionForRetry() {
        val directory = NSTemporaryDirectory() + "renewal-test-" + NSUUID().UUIDString
        try {
            val storage = RenewalJournalStorage(directory)
            val request = RenewalReminder("renewal:7:fixture:2026-10-13:expiry", "fixture", "2026-10-13", "2026-10-13", "expiry")
            storage.write(RenewalJournal().reserve(request))
            storage.write(assertNotNull(storage.read()).rejected(request.key))
            assertEquals(RenewalJournal(), RenewalJournalStorage(directory).read())
        } finally { NSFileManager.defaultManager.removeItemAtPath(directory, null) }
    }
}
