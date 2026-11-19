package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.input.NormalizedNumbers
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_cancel
import motovault.composeapp.generated.resources.action_save
import motovault.composeapp.generated.resources.gauge_edit_date_desc
import motovault.composeapp.generated.resources.gauge_edit_date_label
import motovault.composeapp.generated.resources.gauge_edit_km_desc
import motovault.composeapp.generated.resources.gauge_edit_km_label
import motovault.composeapp.generated.resources.gauge_error_date_future
import motovault.composeapp.generated.resources.gauge_error_date_invalid
import motovault.composeapp.generated.resources.gauge_error_km_above
import motovault.composeapp.generated.resources.gauge_error_km_required
import motovault.composeapp.generated.resources.gauge_last_done_label

private val ISO_DATE_RE = Regex("""\d{4}-\d{2}-\d{2}""")

@Composable
internal fun GaugeLastDoneEditor(
    initialKm: String,
    initialDate: String,
    currentOdometer: Double,
    today: LocalDate,
    onSave: (Double, LocalDate) -> Unit,
    onCancel: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    var kmText by remember(initialKm) { mutableStateOf(initialKm) }
    var dateText by remember(initialDate) { mutableStateOf(initialDate) }

    val asciiDigits = NormalizedNumbers.ascii(kmText).filter { it.isDigit() }
    val kmNum = asciiDigits.toDoubleOrNull()
    val trimmedDate = dateText.trim()
    val parsedDate = if (ISO_DATE_RE.matches(trimmedDate)) runCatching { LocalDate.parse(trimmedDate) }.getOrNull() else null
    val errorText = when {
        kmText.trim().isEmpty() -> appStringResource(Res.string.gauge_error_km_required)
        kmNum == null || kmNum > currentOdometer -> appStringResource(Res.string.gauge_error_km_above, f.n(currentOdometer.toLong()))
        parsedDate == null -> appStringResource(Res.string.gauge_error_date_invalid)
        parsedDate > today -> appStringResource(Res.string.gauge_error_date_future)
        else -> null
    }

    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = appStringResource(Res.string.gauge_last_done_label), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorField(
                label = appStringResource(Res.string.gauge_edit_km_label),
                value = kmText,
                placeholder = f.n(currentOdometer.toLong()),
                description = appStringResource(Res.string.gauge_edit_km_desc),
                keyboardType = KeyboardType.Number,
                onValueChange = { if (it.length <= 9) kmText = it },
                modifier = Modifier.weight(1f)
            )
            EditorField(
                label = appStringResource(Res.string.gauge_edit_date_label),
                value = dateText,
                placeholder = initialDate,
                description = appStringResource(Res.string.gauge_edit_date_desc),
                keyboardType = KeyboardType.Ascii,
                onValueChange = { if (it.length <= 10) dateText = it },
                modifier = Modifier.weight(1f)
            )
        }
        if (errorText != null && kmText.trim().isNotEmpty()) {
            Text(
                text = errorText,
                fontSize = 13.sp,
                color = ErrorColor,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultButton(
                text = appStringResource(Res.string.action_save),
                size = ButtonSize.SM,
                enabled = errorText == null,
                onClick = {
                    val validKm = kmNum
                    val validDate = parsedDate
                    if (validKm != null && validDate != null) {
                        onSave(validKm, validDate)
                    } else {
                        haptic.perform(HapticKind.ERROR)
                    }
                }
            )
            VaultButton(
                text = appStringResource(Res.string.action_cancel),
                size = ButtonSize.SM,
                kind = ButtonKind.SECONDARY,
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    placeholder: String,
    description: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .clip(shape)
                .background(SurfaceColor2)
                .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, shape)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, fontSize = 15.sp, color = TextTertiary, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                cursorBrush = SolidColor(BrandColor),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = description }
            )
        }
    }
}
