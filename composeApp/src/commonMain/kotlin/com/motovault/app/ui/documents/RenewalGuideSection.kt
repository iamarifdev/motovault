package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.documents.RENEWAL_FEE_CALCULATOR_URL
import com.motovault.app.domain.documents.RENEWAL_QR_VERIFICATION_URL
import com.motovault.app.domain.documents.RenewalGuide
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.renewal_checklist_toggle
import motovault.composeapp.generated.resources.renewal_fee_calculator
import motovault.composeapp.generated.resources.renewal_late_fine
import motovault.composeapp.generated.resources.renewal_link_failed
import motovault.composeapp.generated.resources.renewal_offline_note
import motovault.composeapp.generated.resources.renewal_progress
import motovault.composeapp.generated.resources.renewal_source
import motovault.composeapp.generated.resources.renewal_verify

@Composable
internal fun RenewalGuideSection(
    guide: RenewalGuide,
    checkedSteps: Set<String>,
    open: Boolean,
    linkFailed: Boolean,
    busy: Boolean,
    onToggleOpen: () -> Unit,
    onToggleStep: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val doneCount = checkedSteps.count { it in guide.stepIds }
    val progressFraction = if (guide.steps.isEmpty()) 0f else doneCount.toFloat() / guide.steps.size
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(VaultMotion.ExpandMs, easing = VaultMotion.ExpandCurve)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onToggleOpen()
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = appStringResource(
                Res.string.renewal_checklist_toggle,
                f.d(doneCount.toString()),
                f.d(guide.steps.size.toString())
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandColor,
            modifier = Modifier.weight(1f)
        )
        V3Icon(
            path = "feather_icon/fi_chevron-down.svg",
            description = null,
            modifier = Modifier.size(18.dp).rotate(rotation),
            tint = BrandColor
        )
    }

    AnimatedVisibility(visible = open) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = appStringResource(Res.string.renewal_source, f.date(LocalDate.parse(guide.sourceCheckedOn))),
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text(
                text = appStringResource(
                    Res.string.renewal_progress,
                    f.d(doneCount.toString()),
                    f.d(guide.steps.size.toString())
                ),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)).background(SurfaceColor2)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(progressFraction).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(BrandColor)
                )
            }
            RenewalStepsList(guide, checkedSteps, busy, onToggleStep)
            RenewalLinksList(guide, linkFailed, onOpenUrl)
        }
    }
}

@Composable
private fun RenewalStepsList(
    guide: RenewalGuide,
    checkedSteps: Set<String>,
    busy: Boolean,
    onToggleStep: (String) -> Unit
) {
    val haptic = currentHaptic
    Column(modifier = Modifier.fillMaxWidth()) {
        guide.steps.forEachIndexed { index, step ->
            if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
            val copy = renewalStepText(step.id) ?: return@forEachIndexed
            val on = step.id in checkedSteps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = on, role = Role.Checkbox, enabled = !busy) {
                        haptic.perform(HapticKind.TICK)
                        onToggleStep(step.id)
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (on) BrandColor else SurfaceColor)
                        .border(2.dp, if (on) BrandColor else BorderStrongColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (on) {
                        V3Icon(path = "feather_icon/fi_check.svg", description = null, modifier = Modifier.size(14.dp), tint = OnBrandColor)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(text = copy.first, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = copy.second, fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun RenewalLinksList(
    guide: RenewalGuide,
    linkFailed: Boolean,
    onOpenUrl: (String) -> Unit
) {
    if (guide.feeCalculator) {
        RenewalExternalLinkRow(appStringResource(Res.string.renewal_fee_calculator)) { onOpenUrl(RENEWAL_FEE_CALCULATOR_URL) }
    }
    if (guide.lateFineNote) {
        Text(
            text = appStringResource(Res.string.renewal_late_fine),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = WarningColor,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(WarningColor.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
    guide.links.forEach { link ->
        RenewalExternalLinkRow(renewalLinkLabel(link.id)) { onOpenUrl(link.url) }
    }
    if (guide.qrVerification) {
        VaultButton(
            text = appStringResource(Res.string.renewal_verify),
            kind = ButtonKind.SECONDARY,
            full = true,
            onClick = { onOpenUrl(RENEWAL_QR_VERIFICATION_URL) }
        )
    }
    Text(text = appStringResource(Res.string.renewal_offline_note), fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary)
    if (linkFailed) {
        Text(text = appStringResource(Res.string.renewal_link_failed), fontSize = 13.sp, lineHeight = 19.sp, color = WarningColor)
    }
}

@Composable
private fun RenewalExternalLinkRow(label: String, onClick: () -> Unit) {
    val haptic = currentHaptic
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        V3Icon(path = "feather_icon/fi_external-link.svg", description = null, modifier = Modifier.size(16.dp), tint = BrandColor)
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
    }
}

@Composable
internal fun RenewalField(label: String, hint: String? = null, error: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        content()
        val message = error ?: hint
        if (message != null) {
            Text(text = message, fontSize = 12.sp, lineHeight = 17.sp, color = if (error != null) ErrorColor else TextSecondary)
        }
    }
}

@Composable
internal fun RenewalInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    numeric: Boolean,
    attention: Boolean,
    description: String
) {
    val allowed = { next: String -> !numeric || next.all { it.isDigit() || it in "০১২৩৪৫৬৭৮৯.,-/ " } }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (attention) WarningColor else BorderStrongColor, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { next -> if (allowed(next)) onChange(next) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            keyboardOptions = if (numeric) FieldKeyboards.decimal else FieldKeyboards.sentence,
            keyboardActions = rememberFieldKeyboardActions(),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(text = placeholder, fontSize = 15.sp, color = TextTertiary)
                }
                inner()
            }
        )
    }
}

internal fun receiptMimeType(id: String): String = when {
    id.endsWith(".png") -> "image/png"
    id.endsWith(".webp") -> "image/webp"
    else -> "image/jpeg"
}
