package com.motovault.app.ui

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatePickerGridTest {
    @Test fun monthStartsOnItsWeekdayColumn() {
        // 1 November 2026 is a Sunday, so it sits in the first column with no empty cells.
        val nov = monthGrid(LocalDate(2026, 11, 20))
        assertEquals(LocalDate(2026, 11, 1), nov.first())
        assertEquals(30, nov.filterNotNull().size)
        // 1 December 2026 is a Tuesday: two empty cells come first.
        val dec = monthGrid(LocalDate(2026, 12, 5))
        assertNull(dec[0]); assertNull(dec[1])
        assertEquals(LocalDate(2026, 12, 1), dec[2])
    }

    @Test fun leapFebruaryHas29Days() {
        assertEquals(29, monthGrid(LocalDate(2028, 2, 10)).filterNotNull().size)
    }

    @Test fun limitsAreInclusive() {
        val today = LocalDate(2026, 11, 20)
        assertTrue(dateAllowed(today, null, today))
        assertFalse(dateAllowed(LocalDate(2026, 11, 21), null, today))
        assertTrue(dateAllowed(LocalDate(2026, 11, 21), today, null))
        assertFalse(dateAllowed(LocalDate(2026, 11, 19), today, null))
    }

    @Test fun storedDatesAcceptBanglaDigits() {
        assertEquals(LocalDate(2026, 11, 20), parseIsoDateOrNull("২০২৬-১১-২০"))
        assertNull(parseIsoDateOrNull("20/11/2026"))
    }
}
