package com.motovault.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.toArgb

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

actual fun documentDialogProperties() = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)

@Suppress("DEPRECATION")
@Composable
actual fun DocumentWindowAppearance(dark: Boolean) {
    val view = LocalView.current
    val background = if (dark) android.graphics.Color.rgb(14, 21, 29) else CanvasColor.toArgb()
    DisposableEffect(view, dark, background) {
        fun configure() {
            val window = generateSequence(view as View?) { it.parent as? View }
                .filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                if (Build.VERSION.SDK_INT >= 30 && it.attributes.fitInsetsTypes != 0) {
                    it.attributes = it.attributes.apply { setFitInsetsTypes(0) }
                }
                if (it.attributes.width != WindowManager.LayoutParams.MATCH_PARENT || it.attributes.height != WindowManager.LayoutParams.MATCH_PARENT) {
                    it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                }
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                it.setDimAmount(0f)
                it.statusBarColor = background
                it.navigationBarColor = background
                val flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                it.decorView.systemUiVisibility = if (dark) it.decorView.systemUiVisibility and flags.inv() else it.decorView.systemUiVisibility or flags
                WindowCompat.getInsetsController(it, it.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused -> if (focused) configure() }
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { configure() }
        val attached = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { v.post { configure() } }
            override fun onViewDetachedFromWindow(v: View) = Unit
        }
        view.addOnAttachStateChangeListener(attached)
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        view.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        view.post { configure() }
        onDispose {
            view.removeOnAttachStateChangeListener(attached)
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
    }
}

@Composable
actual fun PlatformAppearance(dark: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: context.activity()?.window
    SideEffect {
        window?.let {
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
}
