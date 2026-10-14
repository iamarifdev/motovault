@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.motovault.app.ui

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.cinterop.useContents
import platform.CoreLocation.*
import platform.Foundation.*
import platform.darwin.NSObject

@Composable
actual fun LocationCaptureButton(requestKey: Int, enabled: Boolean, onCaptured: (GeoCapture?) -> Unit) {
    val latestResult by rememberUpdatedState(onCaptured)
    val manager = remember { CLLocationManager().apply { desiredAccuracy = kCLLocationAccuracyNearestTenMeters } }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }

    val delegate = remember(manager) {
        object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                if (!active) return
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                busy = false
                pending = false
                if (location == null || location.horizontalAccuracy < 0.0) latestResult(null)
                else {
                    val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
                    latestResult(GeoCapture(latitude, longitude, location.horizontalAccuracy,
                        (location.timestamp.timeIntervalSince1970 * 1000.0).toLong(), "ios_foreground_gps").takeIf { it.isValid() && it.isFresh() })
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                if (!active) return
                busy = false
                pending = false
                latestResult(null)
            }

            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                if (!active || !pending) return
                when (manager.authorizationStatus) {
                    kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
                    kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                        pending = false
                        busy = false
                        latestResult(null)
                    }
                }
            }
        }
    }
    SideEffect { manager.delegate = delegate }
    DisposableEffect(manager, enabled) {
        active = enabled
        if (!enabled) {
            pending = false
            busy = false
            manager.stopUpdatingLocation()
        }
        onDispose {
            active = false
            pending = false
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }

    fun requestLocationOnce() {
        busy = true
        pending = true
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            else -> { pending = false; busy = false; latestResult(null) }
        }
    }
    LaunchedEffect(requestKey) { if (requestKey > 0 && enabled) requestLocationOnce() }
    OutlinedButton(onClick = { requestLocationOnce() }, enabled = enabled && !busy) {
        Text(if (busy) "লোকেশন খোঁজা হচ্ছে · Finding location…" else "জিপিএস দিয়ে লোকেশন যোগ করুন · Add GPS location")
    }
}
