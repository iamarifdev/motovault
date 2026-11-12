@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui

import com.motovault.app.domain.capture.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*

/** Debug host invokes this with synthetic printed images and the production Vision adapter. */
fun runIosCaptureProbe(): String = runCatching {
    check(CaptureTextNumbers.money("Payment Tk500.50. Balance Tk2,140.50").candidates.single().value == 50050L)
    check(CaptureServiceParser.parse("ইঞ্জিন অয়েল ৯০০ টাকা, অয়েল ফিল্টার ২৫০ টাকা").lines.size == 2)
    check(CaptureServiceParser.parse("সার্ভিসচার্জ ৫০ টাকা").lines.isEmpty())
    check(CaptureTextNumbers.money("Tk500abc").malformed)
    val cases = listOf("ODO 012345 km" to setOf(12345.0), "ODO 000999 km" to emptySet(),
        "ODO 99999999 km" to emptySet(), "" to emptySet(), "12345 km\n23456 km" to setOf(12345.0, 23456.0))
    cases.forEach { (text, expected) ->
        val photo = recognizeOdometerPhoto(printedImage(text))
        check(photo.preview.isNotEmpty())
        val draft = CaptureDraftParser.parse(photo.text, CaptureSource.PHOTO, "", 1000.0, CaptureIntent.ODOMETER)
        check(draft.odometers.map { it.value }.toSet() == expected) { "Unexpected synthetic OCR candidates: ${draft.odometers.map { it.value }}" }
    }
    "PASS 5 printed-display cases through production Vision OCR and 4 shared-parser regressions"
}.getOrElse { "FAIL capture OCR corpus: ${it.stackTraceToString()}\nCAUSE: ${it.cause?.stackTraceToString()}" }

internal fun printedImage(text: String): UIImage {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(1600.0, 500.0), true, 1.0)
    return try {
        UIColor.whiteColor.setFill()
        UIRectFill(CGRectMake(0.0, 0.0, 1600.0, 500.0))
        val attributes = mapOf<Any?, Any>(NSFontAttributeName to UIFont.monospacedDigitSystemFontOfSize(120.0, UIFontWeightRegular), NSForegroundColorAttributeName to UIColor.blackColor)
        (text as platform.Foundation.NSString).drawInRect(CGRectMake(50.0, 40.0, 1500.0, 450.0), attributes)
        requireNotNull(UIGraphicsGetImageFromCurrentImageContext())
    } finally { UIGraphicsEndImageContext() }
}
