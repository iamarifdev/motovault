package com.motovault.app.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecoverySummaryTest {
    @Test fun timestampIsTakenOnlyFromTheKnownRecoveryIdFormat() {
        assertEquals(1729999999000L, recoveryCreatedAt("1729999999000--123.pdf"))
        assertEquals(1729999999000L, recoveryCreatedAt("1729999999000-123.pdf"))
        assertNull(recoveryCreatedAt("synthetic-recovery.pdf"))
        assertNull(recoveryCreatedAt("1729999999000-123.jpg"))
    }
}
