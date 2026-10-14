@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AVFoundation.*
import platform.Foundation.*
import platform.CoreMedia.*
import platform.CoreVideo.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_queue_create

@Composable
actual fun DocumentCamera(modifier: Modifier, captureRequest: Int, onQuality: (CaptureQuality) -> Unit, onCaptured: (DocumentPage) -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val captured by rememberUpdatedState(onCaptured)
    val quality by rememberUpdatedState(onQuality)
    val error by rememberUpdatedState(onError)
    val session = remember { AVCaptureSession() }
    val output = remember { AVCapturePhotoOutput() }
    val analysis = remember { AVCaptureVideoDataOutput().apply { alwaysDiscardsLateVideoFrames = true; videoSettings = mapOf(kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_420YpCbCr8BiPlanarFullRange) } }
    val queue = remember { dispatch_queue_create("motovault.document.camera", null) }
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val delegate = remember {
        object : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
            override fun captureOutput(output: AVCapturePhotoOutput, didFinishProcessingPhoto: AVCapturePhoto, error: NSError?) {
                if (error != null) { busy = false; onError("Could not capture. Try again."); return }
                scope.launch {
                    try { documentAssetOperation { operation ->
                        val data = requireNotNull(didFinishProcessingPhoto.fileDataRepresentation())
                        val page = withContext(Dispatchers.Default) {
                            val id = saveData(data, "jpg", operation)
                            val image = preview(requireNotNull(UIImage(data = data)))
                            DocumentPage(id, saveData(requireNotNull(UIImageJPEGRepresentation(image, .96)), "jpg", operation), "image/jpeg", image.size.useContents { width.toInt() }, image.size.useContents { height.toInt() }, "Front")
                        }
                        captured(page)
                    }
                    } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { onError("Could not save the capture. Try again.") } finally { busy = false }
                }
            }
        }
    }
    val analysisDelegate = remember {
        object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
            var previous = 0.0
            override fun captureOutput(output: AVCaptureOutput, didOutputSampleBuffer: CMSampleBufferRef?, fromConnection: AVCaptureConnection) {
                val now = NSDate.timeIntervalSinceReferenceDate
                if (now - previous < .45) return
                previous = now
                val image = CMSampleBufferGetImageBuffer(didOutputSampleBuffer ?: return) ?: return
                CVPixelBufferLockBaseAddress(image, kCVPixelBufferLock_ReadOnly)
                try {
                    val width = CVPixelBufferGetWidthOfPlane(image, 0uL).toInt(); val height = CVPixelBufferGetHeightOfPlane(image, 0uL).toInt()
                    val stride = CVPixelBufferGetBytesPerRowOfPlane(image, 0uL).toInt()
                    val bytes = CVPixelBufferGetBaseAddressOfPlane(image, 0uL)?.reinterpret<UByteVar>() ?: return
                    var sum = 0L; var bright = 0; var count = 0; var edges = 0L
                    for (y in height / 5 until height * 4 / 5 step 12) for (x in width / 5 until width * 4 / 5 step 12) {
                        val value = bytes[y * stride + x].toInt(); sum += value; if (value >= 252) bright++; count++
                        edges += kotlin.math.abs(value - bytes[y * stride + x + 1].toInt())
                    }
                    if (count > 0) { val report = CaptureQuality(true, bright.toDouble() / count > .20, sum.toDouble() / count < 35, edges.toDouble() / count < .45); scope.launch { quality(report) } }
                } finally { CVPixelBufferUnlockBaseAddress(image, kCVPixelBufferLock_ReadOnly) }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (NSBundle.mainBundle.objectForInfoDictionaryKey("NSCameraUsageDescription") == null) { error("Camera access is not configured in the iOS host. Import a copy instead."); return@LaunchedEffect }
        val allowed = suspendCancellableCoroutine<Boolean> { continuation -> AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted -> if (continuation.isActive) continuation.resume(granted) {} } }
        if (!allowed) { error("Camera access denied. Allow Camera in settings or import a copy."); return@LaunchedEffect }
        try {
            val camera = requireNotNull(AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo))
            val input = requireNotNull(AVCaptureDeviceInput.deviceInputWithDevice(camera, error = null))
            session.beginConfiguration()
            session.sessionPreset = AVCaptureSessionPresetPhoto
            try {
                require(session.canAddInput(input) && session.canAddOutput(output) && session.canAddOutput(analysis))
                analysis.setSampleBufferDelegate(analysisDelegate, queue)
                session.addInput(input); session.addOutput(output); session.addOutput(analysis)
            } finally { session.commitConfiguration() }
            withContext(Dispatchers.Default) { session.startRunning() }
            ready = true
        } catch (_: Exception) { error("Camera unavailable. Import a copy instead.") }
    }
    DisposableEffect(Unit) { onDispose { analysis.setSampleBufferDelegate(null, null); session.stopRunning() } }
    LaunchedEffect(captureRequest) {
        if (captureRequest > 0 && ready && !busy) { busy = true; output.capturePhotoWithSettings(AVCapturePhotoSettings.photoSettings(), delegate) }
    }
    UIKitView(factory = {
        val previewLayer = AVCaptureVideoPreviewLayer(session = session)
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        object : UIView(frame = platform.CoreGraphics.CGRectZero.readValue()) {
            init { backgroundColor = UIColor.blackColor; clipsToBounds = true; layer.addSublayer(previewLayer) }
            override fun layoutSubviews() { super.layoutSubviews(); previewLayer.frame = bounds }
        }
    }, modifier = modifier)
}
