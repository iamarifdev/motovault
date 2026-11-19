@file:OptIn(ExperimentalLayoutApi::class)
package com.motovault.app.ui.fuel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.capture.PumpMeterOcrParser
import com.motovault.app.domain.capture.PumpMeterReading
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.ButtonSize
import com.motovault.app.ui.OcrScanCard
import com.motovault.app.ui.OdometerCaptureState
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import com.motovault.app.ui.ocrCameraErrorText
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_cancel
import motovault.composeapp.generated.resources.capture_photo_retry
import motovault.composeapp.generated.resources.fuel_camera_action
import motovault.composeapp.generated.resources.fuel_camera_apply
import motovault.composeapp.generated.resources.fuel_camera_empty
import motovault.composeapp.generated.resources.fuel_camera_found
import motovault.composeapp.generated.resources.fuel_camera_found_litres
import motovault.composeapp.generated.resources.fuel_camera_found_price
import motovault.composeapp.generated.resources.fuel_camera_scanning
import motovault.composeapp.generated.resources.fuel_octane
import motovault.composeapp.generated.resources.fuel_petrol

private val CameraSecondaryInk = Color(0xFFC6D2CC)

@Composable
internal fun FuelPumpCameraCard(
    capture: OdometerCaptureState,
    onApply: (PumpMeterReading) -> Unit
) {
    val photo = capture.photo
    val error = capture.error
    val reading = remember(photo) { photo?.let { PumpMeterOcrParser.parse(it.text) } }

    OcrScanCard(
        photo = photo,
        scanning = photo == null && error == null,
        previewDescription = appStringResource(Res.string.fuel_camera_action)
    ) {
        when {
            error != null -> {
                CameraStatusMessage(ocrCameraErrorText(error))
                CameraRetryCancelRow(onRetry = capture::start, onCancel = capture::close)
            }
            photo == null -> {
                CameraStatusMessage(appStringResource(Res.string.fuel_camera_scanning))
                VaultButton(
                    text = appStringResource(Res.string.action_cancel),
                    kind = ButtonKind.SECONDARY,
                    size = ButtonSize.SM,
                    onClick = capture::close
                )
            }
            reading == null -> {
                CameraStatusMessage(appStringResource(Res.string.fuel_camera_empty))
                CameraRetryCancelRow(onRetry = capture::start, onCancel = capture::close)
            }
            else -> {
                PumpReadingSummary(reading = reading)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultButton(
                        text = appStringResource(Res.string.fuel_camera_apply),
                        size = ButtonSize.SM,
                        onClick = { onApply(reading) }
                    )
                    VaultButton(
                        text = appStringResource(Res.string.capture_photo_retry),
                        kind = ButtonKind.SECONDARY,
                        size = ButtonSize.SM,
                        onClick = capture::start
                    )
                    VaultButton(
                        text = appStringResource(Res.string.action_cancel),
                        kind = ButtonKind.SECONDARY,
                        size = ButtonSize.SM,
                        onClick = capture::close
                    )
                }
            }
        }
    }
}

@Composable
private fun PumpReadingSummary(reading: PumpMeterReading) {
    val f = currentFmt
    val fuelLabel = when (reading.fuelType) {
        FuelDefaults.PETROL -> appStringResource(Res.string.fuel_petrol)
        FuelDefaults.OCTANE -> appStringResource(Res.string.fuel_octane)
        else -> null
    }
    val primaryLine = when {
        reading.amountTaka != null && fuelLabel != null ->
            appStringResource(Res.string.fuel_camera_found, f.money(reading.amountTaka), fuelLabel)
        reading.amountTaka != null && reading.litres != null ->
            appStringResource(Res.string.fuel_camera_found, f.money(reading.amountTaka), f.liters(reading.litres))
        reading.amountTaka != null -> f.money(reading.amountTaka)
        reading.litres != null -> appStringResource(Res.string.fuel_camera_found_litres, f.liters(reading.litres))
        else -> null
    }
    primaryLine?.let { line ->
        Text(
            text = line,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
    if (reading.litres != null && reading.amountTaka != null && fuelLabel != null) {
        Text(
            text = appStringResource(Res.string.fuel_camera_found_litres, f.liters(reading.litres)),
            color = CameraSecondaryInk,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
    reading.pricePerLitre?.let { rate ->
        Text(
            text = appStringResource(Res.string.fuel_camera_found_price, f.money(rate)),
            color = CameraSecondaryInk,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CameraStatusMessage(text: String) {
    Text(text = text, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
}

@Composable
private fun CameraRetryCancelRow(onRetry: () -> Unit, onCancel: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VaultButton(
            text = appStringResource(Res.string.capture_photo_retry),
            kind = ButtonKind.SECONDARY,
            size = ButtonSize.SM,
            onClick = onRetry
        )
        VaultButton(
            text = appStringResource(Res.string.action_cancel),
            kind = ButtonKind.SECONDARY,
            size = ButtonSize.SM,
            onClick = onCancel
        )
    }
}
