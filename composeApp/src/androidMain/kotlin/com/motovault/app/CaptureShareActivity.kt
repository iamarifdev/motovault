package com.motovault.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.motovault.app.application.capture.ShareCaptureInbox
import com.motovault.app.domain.capture.MAX_CAPTURE_TEXT_LENGTH

internal fun sharedCaptureText(intent: Intent): String? = runCatching {
    if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain" || intent.hasExtra(Intent.EXTRA_STREAM) ||
        (intent.clipData?.itemCount ?: 0) > 1) return null
    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT) ?: return null
    text.takeIf { it.length <= MAX_CAPTURE_TEXT_LENGTH }?.toString()
}.getOrNull()

/** Only an explicit ACTION_SEND text payload enters the private review queue. */
class CaptureShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = sharedCaptureText(intent)
        if (text != null && ShareCaptureInbox.offer(text)) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .setAction(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, text)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
        } else AlertDialog.Builder(this).setMessage(R.string.capture_share_unavailable)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setOnCancelListener { finish() }.show()
    }
}
