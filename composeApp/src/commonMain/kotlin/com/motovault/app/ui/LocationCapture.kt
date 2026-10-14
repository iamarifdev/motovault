package com.motovault.app.ui

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

/** A single user-requested foreground fix. Coordinates are never requested in the background. */
@Serializable
data class GeoCapture(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val capturedAtEpochMillis: Long,
    val source: String
) {
    fun isValid(): Boolean = latitude.isFinite() && latitude in -90.0..90.0 &&
        longitude.isFinite() && longitude in -180.0..180.0 &&
        accuracyMeters.isFinite() && accuracyMeters >= 0.0 && capturedAtEpochMillis > 0 && source.isNotBlank()

    fun isFresh(nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds()): Boolean =
        capturedAtEpochMillis in (nowEpochMillis - 120_000)..(nowEpochMillis + 30_000)
}

@Composable
expect fun LocationCaptureButton(requestKey: Int, enabled: Boolean, onCaptured: (GeoCapture?) -> Unit)
