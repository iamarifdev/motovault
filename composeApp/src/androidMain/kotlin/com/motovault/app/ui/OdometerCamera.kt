package com.motovault.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File

internal suspend fun recognizeOdometerPhoto(bitmap: Bitmap): OdometerPhoto {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        val text = awaitOdometerTask(recognizer.process(InputImage.fromBitmap(bitmap, 0))).text
        val bytes = ByteArrayOutputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it))
            it.toByteArray()
        }
        OdometerPhoto(bytes, text)
    } finally { recognizer.close() }
}

@Composable
actual fun rememberOdometerCamera(
    onResult: (OdometerPhoto) -> Unit,
    onIssue: (OdometerCameraIssue) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val result by rememberUpdatedState(onResult)
    val issue by rememberUpdatedState(onIssue)
    var pending by remember { mutableStateOf<File?>(null) }
    var processing by remember { mutableStateOf<Job?>(null) }
    var active by remember { mutableStateOf(false) }
    val mounted = remember { booleanArrayOf(true) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { accepted ->
        if (!mounted[0]) return@rememberLauncherForActivityResult
        val file = pending
        pending = null
        if (!accepted || file == null) {
            file?.delete()
            active = false
            issue(OdometerCameraIssue.CANCELLED)
        } else processing = scope.launch {
            try {
                val photo = withContext(Dispatchers.Default) {
                    val bitmap = try { previewBitmap(file) } finally { file.delete() }
                    try { recognizeOdometerPhoto(bitmap) } finally { bitmap.recycle() }
                }
                result(photo)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { issue(OdometerCameraIssue.FAILED) }
            finally { file.delete(); active = false }
        }
    }
    val openCamera = {
        if (mounted[0]) try {
            val directory = File(context.cacheDir, "odometer-capture").apply { mkdirs() }
            val file = File.createTempFile("odo-", ".jpg", directory)
            pending = file
            camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.documents", file))
        } catch (_: Exception) {
            pending?.delete(); pending = null; active = false
            issue(OdometerCameraIssue.UNAVAILABLE)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!mounted[0]) return@rememberLauncherForActivityResult
        if (granted) openCamera() else { active = false; issue(OdometerCameraIssue.DENIED) }
    }
    DisposableEffect(Unit) {
        onDispose { mounted[0] = false; processing?.cancel(); pending?.delete(); pending = null }
    }
    return {
        if (!active) {
            active = true
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
            else permission.launch(Manifest.permission.CAMERA)
        }
    }
}
