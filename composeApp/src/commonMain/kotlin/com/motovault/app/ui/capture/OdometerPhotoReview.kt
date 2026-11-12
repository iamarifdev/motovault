@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.motovault.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import com.motovault.app.domain.capture.*
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
internal fun OdometerPhotoReview(baseline: Double, onApply: (Double) -> Unit, onActive: (Boolean) -> Unit = {}) {
    val capture = LocalOdometerCapture.current ?: rememberOdometerCaptureState()
    VaultButton(appStringResource(Res.string.capture_photo_action), icon = "fi_camera.svg",
        kind = ButtonKind.SECONDARY, full = true, enabled = !capture.open, onClick = capture::start)
    DisposableEffect(capture.open) { onActive(capture.open); onDispose { onActive(false) } }
    if (capture.open) OdometerPhotoReviewBody(capture.photo, capture.error, baseline,
        onApply = { onApply(it); capture.close() }, onClose = capture::close, onRetry = capture::start)
}

@Composable
internal fun OdometerPhotoReviewBody(photo: OdometerPhoto?, error: OdometerCameraIssue?, baseline: Double,
    onApply: (Double) -> Unit, onClose: () -> Unit, onRetry: () -> Unit
) {
    var selected by remember(photo) { mutableStateOf<Double?>(null) }
    val draft = remember(photo, baseline) { photo?.let {
        CaptureDraftParser.parse(it.text, CaptureSource.PHOTO, "", baseline, CaptureIntent.ODOMETER)
    } }
    val bitmap = remember(photo) { photo?.let { runCatching { it.preview.decodeToImageBitmap() }.getOrNull() } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        bitmap?.let { Image(it, appStringResource(Res.string.capture_photo_preview),
            Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit) }
        val message = when {
            error == OdometerCameraIssue.DENIED -> Res.string.capture_camera_denied
            error == OdometerCameraIssue.UNAVAILABLE -> Res.string.capture_camera_unavailable
            error != null -> Res.string.capture_photo_failed
            draft == null -> Res.string.capture_photo_reading
            draft.odometers.isEmpty() -> Res.string.capture_photo_empty
            draft.odometers.size > 1 -> Res.string.capture_photo_choose
            else -> Res.string.capture_photo_review
        }
        Text(appStringResource(message), color = TextSecondary)
        draft?.odometers?.forEach { candidate ->
            VaultChip(text = currentFmt.n(candidate.value, if (candidate.value % 1.0 == 0.0) 0 else 1),
                active = selected == candidate.value, onClick = { selected = candidate.value })
        }
        if (draft != null && (CaptureIssue.ODOMETER_REGRESSION in draft.issues || CaptureIssue.INVALID_ODOMETER in draft.issues)) {
            Text(appStringResource(Res.string.capture_photo_rejected), color = ErrorColor)
        }
        if (draft?.odometers?.isNotEmpty() == true) {
            Text(appStringResource(Res.string.capture_photo_source), color = TextSecondary)
            VaultButton(appStringResource(Res.string.capture_photo_use), full = true,
                enabled = selected != null, onClick = { selected?.let(onApply) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultButton(appStringResource(Res.string.capture_manual), kind = ButtonKind.SECONDARY,
                size = ButtonSize.SM, onClick = onClose)
            if (photo != null || error != null) VaultButton(appStringResource(Res.string.capture_photo_retry),
                kind = ButtonKind.SECONDARY, size = ButtonSize.SM, onClick = onRetry)
        }
    }
}
