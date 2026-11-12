@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.motovault.app.ui

import androidx.compose.runtime.*
import com.motovault.app.storage.security.assetBytes
import kotlinx.coroutines.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

internal fun recognizeOdometerPhoto(image: UIImage): OdometerPhoto {
    val small = preview(image)
    val bytes = requireNotNull(UIImageJPEGRepresentation(small, .90)).assetBytes()
    return OdometerPhoto(bytes, recognizeImageText(small))
}

@Composable
actual fun rememberOdometerCamera(
    onResult: (OdometerPhoto) -> Unit,
    onIssue: (OdometerCameraIssue) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    val result by rememberUpdatedState(onResult)
    val issue by rememberUpdatedState(onIssue)
    var picker by remember { mutableStateOf<UIImagePickerController?>(null) }
    var active by remember { mutableStateOf(false) }
    val delegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                active = false
                issue(OdometerCameraIssue.CANCELLED)
            }
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                picker.dismissViewControllerAnimated(true, null)
                scope.launch {
                    try {
                        requireNotNull(image)
                        result(withContext(Dispatchers.Default) { recognizeOdometerPhoto(image) })
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { issue(OdometerCameraIssue.FAILED) }
                    finally { active = false }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { picker?.dismissViewControllerAnimated(false, null); picker?.delegate = null; picker = null }
    }
    return {
        if (!active) scope.launch {
            active = true
            val available = UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
            if (!available) { active = false; issue(OdometerCameraIssue.UNAVAILABLE) }
            else {
                val allowed = suspendCancellableCoroutine<Boolean> { continuation ->
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) {
                        if (continuation.isActive) continuation.resume(it)
                    }
                }
                if (!allowed) { active = false; issue(OdometerCameraIssue.DENIED) }
                else {
                    var host = UIApplication.sharedApplication.keyWindow?.rootViewController
                    while (host?.presentedViewController != null) host = host.presentedViewController
                    if (host == null) { active = false; issue(OdometerCameraIssue.UNAVAILABLE) }
                    else {
                        val camera = UIImagePickerController()
                        camera.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                        camera.delegate = delegate
                        picker = camera
                        host.presentViewController(camera, true, null)
                    }
                }
            }
        }
    }
}
