package com.motovault.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@Composable
actual fun DocumentCamera(modifier: Modifier, captureRequest: Int, onQuality: (CaptureQuality) -> Unit, onCaptured: (DocumentPage) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val captured by rememberUpdatedState(onCaptured)
    val quality by rememberUpdatedState(onQuality)
    val error by rememberUpdatedState(onError)
    var permitted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed -> permitted = allowed; denied = !allowed }
    LaunchedEffect(Unit) { if (!permitted) permission.launch(Manifest.permission.CAMERA) }
    val preview = remember { PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
        clipToOutline = true
        outlineProvider = android.view.ViewOutlineProvider.BOUNDS
        clipChildren = true
    } }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val cameraPreview = remember { Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider } }
    val analyzer = remember { ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var bound by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    DisposableEffect(permitted, lifecycle) {
        var alive = true
        if (permitted) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({ if (alive) runCatching {
                val cameraProvider = future.get(); provider = cameraProvider
                var previous = 0L
                analyzer.setAnalyzer(executor) { image ->
                    try {
                        val now = System.currentTimeMillis()
                        if (now - previous > 450) {
                            previous = now
                            val plane = image.planes[0]; val buffer = plane.buffer
                            var sum = 0L; var bright = 0; var count = 0; var edges = 0L
                            val step = 12
                            for (y in image.height / 5 until image.height * 4 / 5 step step) for (x in image.width / 5 until image.width * 4 / 5 step step) {
                                val index = y * plane.rowStride + x * plane.pixelStride
                                if (index + plane.pixelStride < buffer.limit()) {
                                    val value = buffer.get(index).toInt() and 255
                                    sum += value; if (value >= 252) bright++; count++
                                    edges += kotlin.math.abs(value - (buffer.get(index + plane.pixelStride).toInt() and 255))
                                }
                            }
                            if (count > 0) {
                                val report = CaptureQuality(true, bright.toDouble() / count > .20, sum.toDouble() / count < 35, edges.toDouble() / count < .45)
                                ContextCompat.getMainExecutor(context).execute { if (alive) quality(report) }
                            }
                        }
                    } finally { image.close() }
                }
                val camera = cameraProvider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, cameraPreview, imageCapture, analyzer)
                preview.setOnTouchListener { _, event -> if (event.action == android.view.MotionEvent.ACTION_UP) camera.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(preview.meteringPointFactory.createPoint(event.x, event.y)).build()); true }
                bound = true
            }.onFailure { error("Camera unavailable. Import a photo or PDF instead.") } }, ContextCompat.getMainExecutor(context))
        }
        onDispose { alive = false; bound = false; analyzer.clearAnalyzer(); provider?.unbind(cameraPreview, imageCapture, analyzer); provider = null }
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    LaunchedEffect(captureRequest) {
        if (captureRequest > 0 && bound && !busy) {
            busy = true
            imageCapture.targetRotation = preview.display?.rotation ?: android.view.Surface.ROTATION_0
            val id = recordId() + ".jpg"
            val operation = DocumentAssetOperation().also { it.register(id) }
            imageCapture.takePicture(ImageCapture.OutputFileOptions.Builder(assetFile(id)).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    var delivered = false
                    scope.launch { try {
                        val page = withContext(Dispatchers.IO) {
                            val bitmap = previewBitmap(assetFile(id))
                            try {
                                com.motovault.app.storage.security.ProtectedDocumentStorage.migrate(setOf(id))
                                DocumentPage(id, saveBitmap(bitmap, operation), "image/jpeg", bitmap.width, bitmap.height, "Front")
                            } finally { bitmap.recycle() }
                        }
                        captured(page)
                        delivered = true
                    } catch (cancelled: CancellationException) { if (!delivered) operation.rollback(); throw cancelled } catch (_: Exception) { if (!delivered) operation.rollback(); error("Could not save this capture. Try again.") } finally { busy = false } }.invokeOnCompletion { failure -> if (failure != null && !delivered) operation.rollback() }
                }
                override fun onError(exception: ImageCaptureException) { busy = false; operation.rollback(); error("Could not capture. Check focus and try again.") }
            })
        }
    }
    if (permitted) AndroidView(factory = { preview }, modifier = modifier)
    else Box(modifier, contentAlignment = Alignment.Center) { Text(if (denied) "ক্যামেরার অনুমতি নেই। আগে রাখা কপি ব্যবহার করুন অথবা সেটিংসে অনুমতি দিন।" else "কাগজ স্ক্যান করতে ক্যামেরার অনুমতি দিন।", color = androidx.compose.ui.graphics.Color.White) }
}
