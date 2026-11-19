@file:OptIn(ExperimentalLayoutApi::class)
package com.motovault.app.ui

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
import com.motovault.app.domain.capture.ServiceBillOcrParser
import com.motovault.app.domain.ledger.Money
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_cancel
import motovault.composeapp.generated.resources.capture_photo_retry
import motovault.composeapp.generated.resources.service_camera_action
import motovault.composeapp.generated.resources.service_camera_apply
import motovault.composeapp.generated.resources.service_camera_empty
import motovault.composeapp.generated.resources.service_camera_found
import motovault.composeapp.generated.resources.service_camera_scanning

@Composable
internal fun ServiceBillCameraCard(
    capture: OdometerCaptureState,
    catalogLabels: Map<String, String>,
    onApply: (List<ParsedLine>) -> Unit
) {
    val photo = capture.photo
    val error = capture.error
    val parsedLines = remember(photo) {
        photo?.let { capturedPhoto ->
            ServiceBillOcrParser.parse(capturedPhoto.text).lines.mapNotNull { line ->
                val catId = line.line.catalogueId ?: return@mapNotNull null
                ParsedLine(
                    item = catId,
                    qty = line.line.quantity,
                    price = line.line.unitPricePoisha?.let { Money.taka(it.toLong()) },
                    source = line.source
                )
            }
        }.orEmpty()
    }

    OcrScanCard(
        photo = photo,
        scanning = photo == null && error == null,
        previewDescription = appStringResource(Res.string.service_camera_action)
    ) {
        when {
            error != null -> {
                Text(text = ocrCameraErrorText(error), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                BillCameraRetryCancelRow(onRetry = capture::start, onCancel = capture::close)
            }
            photo == null -> {
                Text(text = appStringResource(Res.string.service_camera_scanning), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                VaultButton(
                    text = appStringResource(Res.string.action_cancel),
                    kind = ButtonKind.SECONDARY,
                    size = ButtonSize.SM,
                    onClick = capture::close
                )
            }
            parsedLines.isEmpty() -> {
                Text(text = appStringResource(Res.string.service_camera_empty), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                BillCameraRetryCancelRow(onRetry = capture::start, onCancel = capture::close)
            }
            else -> {
                val summary = formatBillSummary(parsedLines, catalogLabels, currentFmt)
                Text(
                    text = appStringResource(Res.string.service_camera_found, summary),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultButton(
                        text = appStringResource(Res.string.service_camera_apply),
                        size = ButtonSize.SM,
                        onClick = { onApply(parsedLines) }
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
private fun BillCameraRetryCancelRow(onRetry: () -> Unit, onCancel: () -> Unit) {
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

private fun formatBillSummary(
    lines: List<ParsedLine>,
    catalogLabels: Map<String, String>,
    f: Fmt
): String = lines.joinToString(", ") { line ->
    val name = catalogLabels[line.item] ?: line.item
    val price = line.price
    if (price != null && price > 0.0) "$name ${f.money(price)}" else name
}
