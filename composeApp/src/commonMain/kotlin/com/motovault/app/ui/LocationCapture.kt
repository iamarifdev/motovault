package com.motovault.app.ui

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

/** A single user-requested foreground fix. Coordinates are never requested in the background. */
@Composable
expect fun LocationCaptureButton(requestKey: Int, enabled: Boolean, onCaptured: (GeoCapture?) -> Unit)
