package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VaultDocument
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.vault_empty_add_first
import motovault.composeapp.generated.resources.vault_expiry_summary
import motovault.composeapp.generated.resources.vault_next_renewal
import motovault.composeapp.generated.resources.vault_no_expiry_dates
import motovault.composeapp.generated.resources.vault_scan_document

@Composable
internal fun NextRenewalCard(
    doc: VaultDocument,
    leadDays: Int,
    onClick: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val palette = LocalVaultPalette.current
    val info = documentExpiryInfo(doc, leadDays)
    val accent = when (info.status) {
        DocExpiryStatus.OVERDUE -> palette.err
        DocExpiryStatus.SOON -> palette.warn
        DocExpiryStatus.OK -> palette.ok
        DocExpiryStatus.NONE -> palette.text2
    }
    val (bgAlpha, borderAlpha) = when (info.status) {
        DocExpiryStatus.OVERDUE -> 0.09f to 0.40f
        DocExpiryStatus.SOON -> 0.09f to 0.40f
        DocExpiryStatus.OK -> 0.08f to 0.35f
        DocExpiryStatus.NONE -> 0.0f to 0.0f
    }
    val bgColor = if (bgAlpha > 0f) accent.copy(alpha = bgAlpha).compositeOver(palette.surface) else palette.surface
    val borderColor = if (borderAlpha > 0f) accent.copy(alpha = borderAlpha).compositeOver(palette.line) else palette.line
    val shape = RoundedCornerShape(16.dp)
    val expDate = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .border(1.5.dp, borderColor, shape)
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = appStringResource(Res.string.vault_next_renewal), fontSize = 13.sp, color = TextSecondary)
            Text(
                text = documentDisplayName(doc.kind.ifBlank { doc.title }),
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = appStringResource(
                    Res.string.vault_expiry_summary,
                    if (expDate != null) f.dateFull(expDate) else doc.expiry
                ),
                fontSize = 13.sp,
                color = TextSecondary
            )
            ExpiryChip(doc = doc, warningDays = doc.reminderWarningDays ?: leadDays)
        }
    }
}

@Composable
internal fun VaultEmptyCard(
    hasDocsWithoutExpiry: Boolean,
    onScan: () -> Unit
) {
    val dashColor = BorderStrongColor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val radius = 16.dp.toPx()
                drawRoundRect(
                    color = dashColor,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        V3Icon(
            path = "feather_icon/fi_scan-line.svg",
            description = null,
            modifier = Modifier.size(36.dp),
            tint = TextSecondary
        )
        Text(
            text = appStringResource(
                if (hasDocsWithoutExpiry) Res.string.vault_no_expiry_dates else Res.string.vault_empty_add_first
            ),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        VaultButton(
            text = appStringResource(Res.string.vault_scan_document),
            icon = "fi_scan-line.svg",
            onClick = onScan
        )
    }
}
