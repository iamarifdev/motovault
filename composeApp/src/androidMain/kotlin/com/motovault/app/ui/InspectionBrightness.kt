package com.motovault.app.ui

import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun InspectionBrightness() {
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(view, lifecycle) {
        var window: Window? = null
        var previous: Float? = null
        var disposed = false
        var focusObserver: ViewTreeObserver? = null
        fun restore() { window?.let { target -> previous?.let { target.attributes = target.attributes.apply { screenBrightness = it } } } }
        fun apply() {
            if (disposed || !view.isAttachedToWindow) return
            if (window == null) {
                window = generateSequence(view as View?) { it.parent as? View }.filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
                previous = window?.attributes?.screenBrightness
            }
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && view.hasWindowFocus()) {
                window?.let { it.attributes = it.attributes.apply { screenBrightness = 1f } }
            } else restore()
        }
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_RESUME -> apply()
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> restore()
            else -> Unit
        } }
        val focus = ViewTreeObserver.OnWindowFocusChangeListener { if (it) apply() else restore() }
        fun removeFocusObserver() {
            focusObserver?.takeIf { it.isAlive }?.removeOnWindowFocusChangeListener(focus)
            focusObserver = null
        }
        fun observeFocus() {
            removeFocusObserver()
            focusObserver = view.viewTreeObserver.takeIf { it.isAlive }?.also {
                it.addOnWindowFocusChangeListener(focus)
            }
        }
        val attachment = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                if (disposed) return
                observeFocus()
                apply()
            }
            override fun onViewDetachedFromWindow(v: View) {
                restore()
                removeFocusObserver()
                window = null
                previous = null
            }
        }
        val initialApply = Runnable { apply() }
        lifecycle.addObserver(observer)
        view.addOnAttachStateChangeListener(attachment)
        if (view.isAttachedToWindow) observeFocus()
        view.post(initialApply)
        onDispose {
            disposed = true
            view.removeCallbacks(initialApply)
            restore()
            lifecycle.removeObserver(observer)
            view.removeOnAttachStateChangeListener(attachment)
            removeFocusObserver()
        }
    }
}
