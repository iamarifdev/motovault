package com.motovault.app.application.capture

enum class VoiceCaptureState { IDLE, REQUESTING, LISTENING, PROCESSING, UNAVAILABLE, LOCAL_UNAVAILABLE, DENIED, FAILED, NO_SPEECH }
interface VoiceCaptureListener {
    fun state(value: VoiceCaptureState)
    fun transcript(text: String)
}
interface IosVoiceCaptureBridge {
    fun supportsLocal(): Boolean
    fun available(): Boolean
    fun start(localOnly: Boolean, listener: VoiceCaptureListener)
    fun stop()
    fun cancel()
}
object IosVoiceCaptureRegistry { var bridge: IosVoiceCaptureBridge? = null }
