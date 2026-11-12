package com.motovault.app.ui

import kotlin.test.*

class OdometerCaptureStateTest {
    @Test fun resultRemainsPendingUntilReviewExplicitlyCloses() {
        var starts = 0
        val capture = OdometerCaptureState().apply { launchNative = { starts++ } }
        capture.start()
        val photo = OdometerPhoto(byteArrayOf(1), "12345 km")
        capture.received(photo)
        assertTrue(capture.open)
        assertSame(photo, capture.photo)
        assertEquals(1, starts)
        capture.close()
        assertFalse(capture.open)
        assertNull(capture.photo)
    }

    @Test fun dismissedPendingRequestCannotBecomeANewReview() {
        var starts = 0
        val capture = OdometerCaptureState().apply { launchNative = { starts++ } }
        capture.start()
        capture.close()
        capture.start()
        assertEquals(1, starts)
        assertFalse(capture.open)
        capture.received(OdometerPhoto(byteArrayOf(1), "stale result"))
        assertNull(capture.photo)
        capture.start()
        assertEquals(2, starts)
        assertTrue(capture.open)
    }

    @Test fun cancellationReturnsToManualAndFailureCanBeRetried() {
        val capture = OdometerCaptureState()
        capture.start()
        capture.failed(OdometerCameraIssue.CANCELLED)
        assertFalse(capture.open)
        assertNull(capture.error)
        capture.start()
        capture.failed(OdometerCameraIssue.DENIED)
        assertTrue(capture.open)
        assertEquals(OdometerCameraIssue.DENIED, capture.error)
        capture.start()
        assertTrue(capture.open)
        assertNull(capture.error)
    }
}
