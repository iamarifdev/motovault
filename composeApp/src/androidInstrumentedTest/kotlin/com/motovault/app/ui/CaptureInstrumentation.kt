package com.motovault.app.ui

import android.graphics.*
import android.content.Intent
import com.motovault.app.sharedCaptureText
import com.motovault.app.domain.capture.*

internal object CaptureOcrCorpus {
    suspend fun runLifetime(): String {
        checkOdometerTaskLifetime()
        check(read("ODO 012345 km", 1000.0).odometers.single().value == 12345.0)
        return "PASS 4 Google Task lifetime/terminal checks and 1 production ML Kit printed-display case\n"
    }
    suspend fun run(): String {
        val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "Fuel Tk500.50")
        check(sharedCaptureText(share) == "Fuel Tk500.50")
        check(sharedCaptureText(Intent(share).setType("image/jpeg")) == null)
        check(sharedCaptureText(Intent(share).putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse("content://synthetic/item"))) == null)
        check(CaptureTextNumbers.money("Payment Tk500.50. Balance Tk2,140.50").candidates.single().value == 50050L)
        check(CaptureServiceParser.parse("ইঞ্জিন অয়েল ৯০০ টাকা, অয়েল ফিল্টার ২৫০ টাকা").lines.size == 2)
        check(CaptureServiceParser.parse("সার্ভিসচার্জ ৫০ টাকা").lines.isEmpty())
        check(CaptureTextNumbers.money("Tk500abc").malformed)
        check(read("ODO 012345 km", 1000.0).odometers.single().value == 12345.0)
        check(read("ODO 000999 km", 1000.0).odometers.isEmpty())
        check(read("ODO 99999999 km", 1000.0).odometers.isEmpty())
        check(read("", 1000.0).odometers.isEmpty())
        val multiple = read("12345 km\n23456 km", 1000.0)
        check(multiple.odometers.map { it.value }.toSet() == setOf(12345.0, 23456.0))
        check(multiple.requiresChoice)
        return "PASS 5 printed-display cases through production ML Kit OCR and 4 shared-parser + 3 native share-boundary regressions\n"
    }
    private suspend fun read(text: String, baseline: Double): CaptureDraft {
        val bitmap = Bitmap.createBitmap(1600, 500, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 120f; typeface = Typeface.MONOSPACE }
            text.lines().forEachIndexed { i, line -> canvas.drawText(line, 50f, 160f + i * 170f, paint) }
            val photo = recognizeOdometerPhoto(bitmap)
            check(photo.preview.isNotEmpty())
            return CaptureDraftParser.parse(photo.text, CaptureSource.PHOTO, "", baseline, CaptureIntent.ODOMETER)
        } finally { bitmap.recycle() }
    }
}
