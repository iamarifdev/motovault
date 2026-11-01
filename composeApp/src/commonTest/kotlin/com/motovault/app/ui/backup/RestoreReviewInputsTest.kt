package com.motovault.app.ui.backup

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestoreReviewInputsTest {
    @Test
    fun reviewRejectsChangedFileOrPassword() {
        val bytes = byteArrayOf(1, 2)
        val review = RestoreReviewInputs(bytes, "test password", 4)
        assertTrue(review.matches(bytes, "test password"))
        assertFalse(review.matches(bytes.copyOf(), "test password"))
        assertFalse(review.matches(bytes, "changed password"))
        assertFalse(review.matches(null, "test password"))
    }
}
