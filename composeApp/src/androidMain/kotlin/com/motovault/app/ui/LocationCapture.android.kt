package com.motovault.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun LocationCaptureButton(requestKey: Int, enabled: Boolean, onCaptured: (GeoCapture?) -> Unit) {
    val context = LocalContext.current
    val latestResult by rememberUpdatedState(onCaptured)
    val manager = remember(context) { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var busy by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    val cancellation = remember { mutableStateOf<CancellationSignal?>(null) }
    val oldListener = remember { mutableStateOf<LocationListener?>(null) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(manager, enabled) {
        active = enabled
        if (!enabled) {
            cancellation.value?.cancel()
            cancellation.value = null
            oldListener.value?.let { manager.removeUpdates(it) }
            oldListener.value = null
            handler.removeCallbacksAndMessages(null)
            busy = false
        }
        onDispose {
            active = false
            cancellation.value?.cancel()
            cancellation.value = null
            oldListener.value?.let { manager.removeUpdates(it) }
            oldListener.value = null
            handler.removeCallbacksAndMessages(null)
        }
    }

    fun finish(location: Location?) {
        if (!active) return
        busy = false
        val capture = location?.let { GeoCapture(it.latitude, it.longitude, it.accuracy.toDouble(), it.time, "android_foreground_gps") }
        latestResult(capture?.takeIf { it.isValid() && it.isFresh() })
    }

    fun requestFix() {
        if (!active || !enabled) return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) { finish(null); return }
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (provider == null) { finish(null); return }
        busy = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val signal = CancellationSignal()
                cancellation.value = signal
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { cancellation.value = null; finish(it) }
            } catch (_: SecurityException) { finish(null) }
        } else {
            var completed = false
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!completed) { completed = true; handler.removeCallbacksAndMessages(null); manager.removeUpdates(this); oldListener.value = null; finish(location) }
                }
                @Deprecated("Deprecated by Android") override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) { if (!completed) { completed = true; manager.removeUpdates(this); oldListener.value = null; finish(null) } }
            }
            try {
                oldListener.value = listener
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                handler.postDelayed({ if (!completed) { completed = true; manager.removeUpdates(listener); oldListener.value = null; finish(null) } }, 15_000L)
            } catch (_: SecurityException) { finish(null) }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (!active || !enabled) return@rememberLauncherForActivityResult
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) requestFix()
        else finish(null)
    }
    fun requestPermissionOrFix() {
        if (!active || !enabled) return
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) requestFix()
        else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(requestKey) { if (requestKey > 0 && enabled) requestPermissionOrFix() }
    OutlinedButton(onClick = { requestPermissionOrFix() }, enabled = enabled && !busy) {
        Text(if (busy) "লোকেশন খোঁজা হচ্ছে · Finding location…" else "জিপিএস দিয়ে লোকেশন যোগ করুন · Add GPS location")
    }
}
