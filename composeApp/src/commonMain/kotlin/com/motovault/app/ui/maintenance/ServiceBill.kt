package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.input.NormalizedNumbers
import motovault.composeapp.generated.resources.*

@Composable
internal fun ServiceBillCard(
    rows: List<ServiceRowState>,
    entry: NumberEntryState,
    keysOpen: Boolean,
    showExtras: Boolean,
    existingMiscPoisha: Int,
    openGradeUid: String?,
    onFocusField: (String) -> Unit,
    onToggleGradeUid: (String?) -> Unit,
    onSelectGrade: (String, String?) -> Unit,
    onAdjustQty: (String, Int) -> Unit,
    onRemoveRow: (String) -> Unit,
    onOpenPartsPicker: () -> Unit,
    onEnableExtras: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape)
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
            ServiceBillLineRow(
                row = row,
                active = keysOpen && entry.activeField == "p:${row.uid}",
                gradeOpen = openGradeUid == row.uid,
                unitPrice = NormalizedNumbers.parse(entry.values["p:${row.uid}"].orEmpty()),
                onFocusPrice = { onFocusField("p:${row.uid}") },
                onToggleGrade = { onToggleGradeUid(if (openGradeUid == row.uid) null else row.uid) },
                onSelectGrade = { grade -> onSelectGrade(row.uid, grade); onToggleGradeUid(null) },
                onAdjustQty = { delta -> onAdjustQty(row.uid, delta) },
                onRemove = { onRemoveRow(row.uid) }
            )
        }
        if (showExtras) {
            ServiceExtrasRows(entry, keysOpen, existingMiscPoisha, onFocusField)
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        ServiceBillFooterBar(showExtras, onOpenPartsPicker, onEnableExtras)
    }
}

@Composable
private fun ServiceBillLineRow(
    row: ServiceRowState,
    active: Boolean,
    gradeOpen: Boolean,
    unitPrice: Double?,
    onFocusPrice: () -> Unit,
    onToggleGrade: () -> Unit,
    onSelectGrade: (String?) -> Unit,
    onAdjustQty: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val f = currentFmt
    val shortName = row.catalogueId?.let { catalogItemOf(it)?.let { item -> catalogShortLabel(item) } } ?: row.name
    val displayTitle = if (row.qty > 1) {
        appStringResource(Res.string.service_qty_inline, shortName, f.d(row.qty.toString()))
    } else {
        shortName
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) BrandTintColor else SurfaceColor)
            .padding(start = 14.dp, top = 8.dp, end = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ServiceBillMainRow(row, displayTitle, active, unitPrice, onFocusPrice, onToggleGrade, onRemove)
        if (gradeOpen && row.catalogueId == "engine_oil") {
            ChipLine(contentDescription = appStringResource(Res.string.service_grade_desc)) {
                VISCOSITY_GRADES.forEach { grade ->
                    EntryChip(
                        text = grade,
                        active = row.detail == grade,
                        onClick = { onSelectGrade(if (row.detail == grade) null else grade) }
                    )
                }
            }
        }
        if (active && row.catalogueId != "general_service") {
            ServiceQuantityStepper(row.qty, onAdjustQty)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceBillMainRow(
    row: ServiceRowState,
    displayTitle: String,
    active: Boolean,
    unitPrice: Double?,
    onFocusPrice: () -> Unit,
    onToggleGrade: () -> Unit,
    onRemove: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = displayTitle,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            if (row.catalogueId == "engine_oil") {
                ConfirmPill(
                    text = row.detail ?: appStringResource(Res.string.service_grade_prompt),
                    tone = if (row.detail != null) PillTone.ACTIVE else PillTone.DEFAULT,
                    onClick = onToggleGrade
                )
            }
        }
        ServicePriceButton(
            text = if (unitPrice == null) appStringResource(Res.string.service_price_unknown) else f.money(unitPrice * row.qty),
            active = active,
            unknown = unitPrice == null,
            contentDesc = appStringResource(Res.string.service_item_price_desc, row.name),
            onClick = onFocusPrice
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { haptic.perform(HapticKind.TICK); onRemove() },
            contentAlignment = Alignment.Center
        ) {
            V3Icon(
                path = "feather_icon/fi_x.svg",
                description = appStringResource(Res.string.service_item_remove_desc, row.name),
                modifier = Modifier.size(18.dp),
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun ServiceQuantityStepper(qty: Int, onAdjustQty: (Int) -> Unit) {
    val f = currentFmt
    val haptic = currentHaptic
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = appStringResource(Res.string.service_qty_label), fontSize = 13.sp, color = TextSecondary)
        Row(
            modifier = Modifier.clip(shape).background(SurfaceColor).border(1.dp, BorderColor, shape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(34.dp).clickable(role = Role.Button) {
                    if (qty > 1) { haptic.perform(HapticKind.TICK); onAdjustQty(-1) }
                },
                contentAlignment = Alignment.Center
            ) {
                V3Icon(
                    path = "feather_icon/fi_minus.svg",
                    description = appStringResource(Res.string.service_qty_decrease),
                    modifier = Modifier.size(16.dp),
                    tint = TextPrimary
                )
            }
            Text(
                text = f.d(qty.toString()),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 24.dp)
            )
            Box(
                modifier = Modifier.size(34.dp).clickable(role = Role.Button) {
                    if (qty < 20) { haptic.perform(HapticKind.TICK); onAdjustQty(1) }
                },
                contentAlignment = Alignment.Center
            ) {
                V3Icon(
                    path = "feather_icon/fi_plus.svg",
                    description = appStringResource(Res.string.service_qty_increase),
                    modifier = Modifier.size(16.dp),
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ServiceExtrasRows(
    entry: NumberEntryState,
    keysOpen: Boolean,
    existingMiscPoisha: Int,
    onFocusField: (String) -> Unit
) {
    val f = currentFmt
    val extraFields = listOf("labour" to Res.string.service_labour, "discount" to Res.string.service_discount)
    extraFields.forEach { (key, labelRes) ->
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        val raw = entry.values[key].orEmpty()
        val parsed = NormalizedNumbers.parse(raw)
        val active = keysOpen && entry.activeField == key
        val valueText = if (parsed != null) {
            val money = f.money(parsed)
            if (key == "discount") appStringResource(Res.string.service_discount_value, money) else money
        } else {
            f.money(0.0)
        }
        val labelText = appStringResource(labelRes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (active) BrandTintColor else SurfaceColor)
                .padding(start = 14.dp, top = 8.dp, end = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = labelText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            ServicePriceButton(
                text = valueText,
                active = active,
                unknown = false,
                contentDesc = labelText,
                onClick = { onFocusField(key) }
            )
        }
    }
    if (existingMiscPoisha > 0) {
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, end = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = appStringResource(Res.string.service_misc), fontSize = 14.sp, color = TextSecondary)
            Text(text = f.money(existingMiscPoisha / 100.0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        }
    }
}

@Composable
private fun ServiceBillFooterBar(
    showExtras: Boolean,
    onOpenPartsPicker: () -> Unit,
    onEnableExtras: () -> Unit
) {
    val haptic = currentHaptic
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button) { haptic.perform(HapticKind.TICK); onOpenPartsPicker() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            V3Icon(path = "feather_icon/fi_plus.svg", description = null, modifier = Modifier.size(16.dp), tint = BrandColor)
            Text(text = appStringResource(Res.string.service_add_more_work), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
        }
        if (!showExtras) {
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(BorderColor))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button) { haptic.perform(HapticKind.TICK); onEnableExtras() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                V3Icon(path = "feather_icon/fi_plus.svg", description = null, modifier = Modifier.size(16.dp), tint = BrandColor)
                Text(text = appStringResource(Res.string.service_add_charge_discount), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
            }
        }
    }
}

@Composable
private fun ServicePriceButton(
    text: String,
    active: Boolean,
    unknown: Boolean,
    contentDesc: String,
    onClick: () -> Unit
) {
    val haptic = currentHaptic
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (active) BrandTintColor else SurfaceColor)
            .border(2.dp, if (active) BrandColor else BorderColor, shape)
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .semantics { contentDescription = contentDesc }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (unknown) BrandColor else TextPrimary
        )
    }
}
