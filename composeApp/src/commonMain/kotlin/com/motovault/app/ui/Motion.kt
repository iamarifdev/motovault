package com.motovault.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

object VaultMotion {
    const val InstantMs = 100
    const val QuickMs = 180
    const val ExpandMs = 200
    const val PageMs = 300
    const val SheetInMs = 250
    const val SheetOutMs = 200
    const val SweepMs = 600

    val SkyCurve = CubicBezierEasing(0f, 0f, 0.2f, 1f)
    val ExpandCurve = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    fun <T> pageTween(reducedMotion: Boolean = false) = if (reducedMotion) tween<T>(0) else tween<T>(PageMs, easing = SkyCurve)
    fun <T> sheetInTween(reducedMotion: Boolean = false) = if (reducedMotion) tween<T>(0) else tween<T>(SheetInMs, easing = SkyCurve)
    fun <T> sheetOutTween(reducedMotion: Boolean = false) = if (reducedMotion) tween<T>(0) else tween<T>(SheetOutMs, easing = SkyCurve)
    fun <T> quickTween(reducedMotion: Boolean = false) = if (reducedMotion) tween<T>(0) else tween<T>(QuickMs, easing = SkyCurve)
    fun <T> sweepTween(reducedMotion: Boolean = false) = if (reducedMotion) tween<T>(0) else tween<T>(SweepMs, easing = SkyCurve)

    fun <T> sheetSpring(reducedMotion: Boolean = false) =
        if (reducedMotion) spring<T>(stiffness = Spring.StiffnessHigh)
        else spring<T>(dampingRatio = 0.85f, stiffness = 420f)

    fun <T> pressSpring(reducedMotion: Boolean = false) =
        if (reducedMotion) spring<T>(stiffness = Spring.StiffnessHigh)
        else spring<T>(dampingRatio = 0.8f, stiffness = 600f)

    fun <T> listSpring(reducedMotion: Boolean = false) =
        if (reducedMotion) spring<T>(stiffness = Spring.StiffnessHigh)
        else spring<T>(dampingRatio = 0.82f, stiffness = 500f)

    fun <T> markerSpring(reducedMotion: Boolean = false) =
        if (reducedMotion) spring<T>(stiffness = Spring.StiffnessHigh)
        else spring<T>(dampingRatio = 0.8f, stiffness = 520f)
}

val LocalReducedMotion = staticCompositionLocalOf { false }
val isReducedMotion: Boolean
    @Composable get() = LocalReducedMotion.current
