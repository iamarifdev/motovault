package com.motovault.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.motovault.app.application.capture.VoiceCaptureState
import com.motovault.app.domain.capture.MAX_CAPTURE_TEXT_LENGTH

private const val MAX_VOICE_CAPTURE_MILLIS = 60_000L

private class AndroidVoiceSession(private val context: android.content.Context,
    private val state: (VoiceCaptureState) -> Unit, private val text: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var generation = 0
    private val handler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null
    val local get() = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    val provider get() = SpeechRecognizer.isRecognitionAvailable(context)
    fun start(localOnly: Boolean) {
        cancel()
        if (localOnly && !local) { state(VoiceCaptureState.LOCAL_UNAVAILABLE); return }
        if (!localOnly && !provider) { state(VoiceCaptureState.UNAVAILABLE); return }
        val token = generation
        try {
            val engine = if (localOnly && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                else SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { if (token == generation) state(VoiceCaptureState.LISTENING) }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { if (token == generation) state(VoiceCaptureState.PROCESSING) }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onError(error: Int) {
                    if (token != generation) return
                    val next = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceCaptureState.DENIED
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceCaptureState.NO_SPEECH
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> if (localOnly) VoiceCaptureState.LOCAL_UNAVAILABLE else VoiceCaptureState.UNAVAILABLE
                        else -> VoiceCaptureState.FAILED
                    }
                    cancel(); state(next)
                }
                override fun onResults(results: Bundle?) {
                    if (token != generation) return
                    val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    cancel()
                    if (transcript.isNullOrBlank() || transcript.length > MAX_CAPTURE_TEXT_LENGTH) state(VoiceCaptureState.NO_SPEECH)
                    else { text(transcript); state(VoiceCaptureState.IDLE) }
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            state(VoiceCaptureState.REQUESTING)
            engine.startListening(intent)
            timeout = Runnable { if (token == generation) { cancel(); state(VoiceCaptureState.FAILED) } }
                .also { handler.postDelayed(it, MAX_VOICE_CAPTURE_MILLIS) }
        } catch (_: Exception) { cancel(); state(VoiceCaptureState.FAILED) }
    }
    fun stop() { recognizer?.stopListening(); state(VoiceCaptureState.PROCESSING) }
    fun cancel() {
        generation++; timeout?.let(handler::removeCallbacks); timeout = null
        recognizer?.cancel(); recognizer?.destroy(); recognizer = null
    }
}

@Composable
internal actual fun rememberVoiceCapture(onState: (VoiceCaptureState) -> Unit, onText: (String) -> Unit): VoiceCaptureActions {
    val context = LocalContext.current
    val state by rememberUpdatedState(onState)
    val text by rememberUpdatedState(onText)
    val lifecycle = LocalLifecycleOwner.current
    val session = remember(context) { AndroidVoiceSession(context, { state(it) }, { text(it) }) }
    var local by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (pending) { pending = false; if (granted) session.start(local) else state(VoiceCaptureState.DENIED) }
    }
    DisposableEffect(session, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) {
            pending = false; session.cancel(); state(VoiceCaptureState.IDLE)
        } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { pending = false; lifecycle.lifecycle.removeObserver(observer); session.cancel() }
    }
    return VoiceCaptureActions(session.local, session.provider, start = { localOnly ->
        local = localOnly; pending = true; state(VoiceCaptureState.REQUESTING)
        permission.launch(Manifest.permission.RECORD_AUDIO)
    }, stop = session::stop, cancel = { pending = false; session.cancel(); state(VoiceCaptureState.IDLE) })
}
