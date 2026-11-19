@file:OptIn(ExperimentalLayoutApi::class)
package com.motovault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.capture.CaptureDraftParser
import com.motovault.app.domain.capture.CaptureIntent
import com.motovault.app.domain.capture.CaptureIssue
import com.motovault.app.domain.capture.CaptureSource
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.capture_photo_choose
import motovault.composeapp.generated.resources.capture_photo_empty
import motovault.composeapp.generated.resources.capture_photo_preview
import motovault.composeapp.generated.resources.capture_photo_rejected
import motovault.composeapp.generated.resources.capture_photo_retry
import motovault.composeapp.generated.resources.capture_photo_review
import motovault.composeapp.generated.resources.onboarding_camera_manual
import motovault.composeapp.generated.resources.onboarding_camera_scanning
import motovault.composeapp.generated.resources.onboarding_camera_use_km
import motovault.composeapp.generated.resources.onboarding_cancel

private val DarkCardTextMuted = Color(0xFFC6D2CC)

@Composable
internal fun OdometerCameraCard(
    capture: OdometerCaptureState,
    onApply: (Double) -> Unit
) {
    val f = currentFmt
    val photo = capture.photo
    val error = capture.error
    val draft = remember(photo) {
        photo?.let { CaptureDraftParser.parse(it.text, CaptureSource.PHOTO, "", 0.0, CaptureIntent.ODOMETER) }
    }
    val candidates = draft?.odometers.orEmpty()
    var selected by remember(photo) {
        mutableStateOf(candidates.singleOrNull()?.value)
    }

    OcrScanCard(
        photo = photo,
        scanning = photo == null && error == null,
        previewDescription = appStringResource(Res.string.capture_photo_preview)
    ) {
        when {
            error != null -> {
                DarkStatusText(ocrCameraErrorText(error))
                CameraRetryManualRow(onRetry = capture::start, onManual = capture::close)
            }
            photo == null -> {
                DarkStatusText(appStringResource(Res.string.onboarding_camera_scanning))
                VaultButton(
                    text = appStringResource(Res.string.onboarding_cancel),
                    kind = ButtonKind.SECONDARY,
                    size = ButtonSize.SM,
                    onClick = capture::close
                )
            }
            candidates.isEmpty() -> {
                DarkStatusText(appStringResource(Res.string.capture_photo_empty))
                CameraRetryManualRow(onRetry = capture::start, onManual = capture::close)
            }
            else -> {
                val prompt = if (candidates.size > 1) Res.string.capture_photo_choose else Res.string.capture_photo_review
                DarkStatusText(appStringResource(prompt))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    candidates.forEach { candidate ->
                        VaultChip(
                            text = f.km(candidate.value),
                            active = selected == candidate.value,
                            onClick = { selected = candidate.value }
                        )
                    }
                }
                if (draft != null && (CaptureIssue.ODOMETER_REGRESSION in draft.issues || CaptureIssue.INVALID_ODOMETER in draft.issues)) {
                    Text(text = appStringResource(Res.string.capture_photo_rejected), fontSize = 12.sp, color = ErrorColor, textAlign = TextAlign.Center)
                }
                CameraResultActionRow(
                    selected = selected,
                    onApply = { selected?.let(onApply) },
                    onManual = capture::close,
                    onRetry = capture::start
                )
            }
        }
    }
}

@Composable
private fun DarkStatusText(message: String) {
    Text(
        text = message,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = DarkCardTextMuted,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun CameraRetryManualRow(onRetry: () -> Unit, onManual: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultButton(text = appStringResource(Res.string.capture_photo_retry), kind = ButtonKind.SECONDARY, size = ButtonSize.SM, onClick = onRetry)
        VaultButton(text = appStringResource(Res.string.onboarding_camera_manual), kind = ButtonKind.SECONDARY, size = ButtonSize.SM, onClick = onManual)
    }
}

@Composable
private fun CameraResultActionRow(selected: Double?, onApply: () -> Unit, onManual: () -> Unit, onRetry: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultButton(text = appStringResource(Res.string.onboarding_camera_use_km), size = ButtonSize.SM, enabled = selected != null, onClick = onApply)
        VaultButton(text = appStringResource(Res.string.onboarding_camera_manual), kind = ButtonKind.SECONDARY, size = ButtonSize.SM, onClick = onManual)
        VaultButton(text = appStringResource(Res.string.capture_photo_retry), kind = ButtonKind.SECONDARY, size = ButtonSize.SM, onClick = onRetry)
    }
}
