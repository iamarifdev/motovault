package com.motovault.app.application.capture

import com.motovault.app.domain.capture.MAX_CAPTURE_TEXT_LENGTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One explicit share, held in memory until the unlocked owner opens its review. */
object ShareCaptureInbox {
    private val mutable = MutableStateFlow<String?>(null)
    val pending = mutable.asStateFlow()
    fun offer(text: String): Boolean {
        if (text.isBlank() || text.length > MAX_CAPTURE_TEXT_LENGTH || '\u0000' in text) return false
        return mutable.compareAndSet(null, text)
    }
    fun take(text: String): Boolean = mutable.compareAndSet(text, null)
}

/** Host supplies only explicitly shared text. Reading a clipboard or message inbox is not allowed. */
interface SharedCaptureBridge { fun takeText(): String? }
object IosSharedCaptureRegistry { var bridge: SharedCaptureBridge? = null }
