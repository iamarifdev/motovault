package com.motovault.app

import android.graphics.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.motovault.app.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CaptureFixtureActivity : ComponentActivity() {
    @Volatile internal var captureOwner: OdometerCaptureState? = null
    @Volatile internal var consumerStarts = 0
    @Volatile internal var consumerStops = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName == "com.motovault.app.privacyprobe")
        vaultContext = applicationContext
        if (intent.getBooleanExtra("captureOwner", false)) {
            setContent { CaptureOwnerFixture({ captureOwner = it }, { if (it) consumerStarts++ else consumerStops++ }) }
            return
        }
        setContent {
            var photo by remember { mutableStateOf<OdometerPhoto?>(null) }
            LaunchedEffect(Unit) {
                photo = withContext(Dispatchers.Default) {
                    val bitmap = Bitmap.createBitmap(1600, 500, Bitmap.Config.ARGB_8888)
                    try {
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 120f; typeface = Typeface.MONOSPACE }
                        canvas.drawText("12345 km", 50f, 160f, paint)
                        canvas.drawText("23456 km", 50f, 330f, paint)
                        recognizeOdometerPhoto(bitmap)
                    } finally { bitmap.recycle() }
                }
            }
            CaptureRuntimeFixture(AppComposition.captureDrafts, photo)
        }
    }
}
