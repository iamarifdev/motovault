@file:OptIn(ExperimentalLayoutApi::class)
package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.input.NormalizedNumbers
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_add_error_expiry
import motovault.composeapp.generated.resources.doc_add_error_missing
import motovault.composeapp.generated.resources.doc_add_expiry
import motovault.composeapp.generated.resources.doc_add_expiry_group_desc
import motovault.composeapp.generated.resources.doc_add_holder_optional
import motovault.composeapp.generated.resources.doc_add_holder_placeholder
import motovault.composeapp.generated.resources.doc_add_number_optional
import motovault.composeapp.generated.resources.doc_add_number_placeholder
import motovault.composeapp.generated.resources.doc_add_optional_hide
import motovault.composeapp.generated.resources.doc_add_optional_show
import motovault.composeapp.generated.resources.doc_add_photo_attached
import motovault.composeapp.generated.resources.doc_add_reminder_date_sub
import motovault.composeapp.generated.resources.doc_add_reminder_lead_title
import motovault.composeapp.generated.resources.doc_add_reminder_no_date_sub
import motovault.composeapp.generated.resources.doc_add_save
import motovault.composeapp.generated.resources.doc_add_update_warning
import motovault.composeapp.generated.resources.doc_add_years_later

private val EXPIRY_YEAR_PRESETS = listOf(1, 2, 5, 10)
private const val MAX_DOC_TEXT_LENGTH = 80

@Composable
internal fun DocExpiryStep(
    kindName: String,
    isEdit: Boolean,
    missingExisting: Boolean,
    existingSameKind: Boolean,
    hasPhoto: Boolean,
    expiry: String,
    reminder: Boolean,
    number: String,
    holder: String,
    showOptional: Boolean,
    leadDays: Int,
    today: LocalDate,
    saveError: String?,
    onExpiryChange: (String) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onToggleOptional: () -> Unit,
    onNumberChange: (String) -> Unit,
    onHolderChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val haptic = currentHaptic
    val parsedExpiry = runCatching { LocalDate.parse(NormalizedNumbers.ascii(expiry.trim())) }.getOrNull()
    val reason = when {
        missingExisting -> appStringResource(Res.string.doc_add_error_missing)
        parsedExpiry == null -> appStringResource(Res.string.doc_add_error_expiry)
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (hasPhoto) DocPhotoAttachedBanner()
        if (!isEdit && existingSameKind) {
            Text(
                text = appStringResource(Res.string.doc_add_update_warning, kindName),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = WarningColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        val expiryLabel = appStringResource(Res.string.doc_add_expiry)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = expiryLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            // v4: a new document cannot expire before today; an existing one may already be expired.
            DateField(expiry, onExpiryChange, expiryLabel, fallback = today, min = if (isEdit) null else today, attention = true)
        }
        DocExpiryPresets(expiry, today, onExpiryChange)
        DocReminderCard(parsedExpiry, leadDays, reminder, onReminderChange)
        DocOptionalDetailsSection(showOptional, number, holder, onToggleOptional, onNumberChange, onHolderChange)
        saveError?.let { err -> Text(text = err, fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.SemiBold) }
        VaultButton(
            text = reason ?: appStringResource(Res.string.doc_add_save),
            full = true,
            enabled = reason == null,
            onClick = { if (reason != null) haptic.perform(HapticKind.ERROR) else onSave() }
        )
    }
}

@Composable
private fun DocPhotoAttachedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandTintColor)
            .border(1.dp, BrandLineColor, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        V3Icon(path = "feather_icon/fi_circle-check.svg", description = null, modifier = Modifier.size(20.dp), tint = HealthyColor)
        Text(
            text = appStringResource(Res.string.doc_add_photo_attached),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DocExpiryPresets(expiry: String, today: LocalDate, onSelect: (String) -> Unit) {
    val f = currentFmt
    val groupDesc = appStringResource(Res.string.doc_add_expiry_group_desc)
    FlowRow(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = groupDesc },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EXPIRY_YEAR_PRESETS.forEach { years ->
            val target = LocalDate.fromEpochDays(today.toEpochDays() + 365 * years).toString()
            VaultChip(
                text = appStringResource(Res.string.doc_add_years_later, f.d(years.toString())),
                active = expiry.trim() == target,
                onClick = { onSelect(target) }
            )
        }
    }
}

@Composable
private fun DocReminderCard(
    parsedExpiry: LocalDate?,
    leadDays: Int,
    reminder: Boolean,
    onReminderChange: (Boolean) -> Unit
) {
    val f = currentFmt
    val reminderSub = if (parsedExpiry != null) {
        val notifyDate = LocalDate.fromEpochDays(parsedExpiry.toEpochDays() - leadDays)
        appStringResource(Res.string.doc_add_reminder_date_sub, f.date(notifyDate))
    } else {
        appStringResource(Res.string.doc_add_reminder_no_date_sub)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        VaultSwitch(
            title = appStringResource(Res.string.doc_add_reminder_lead_title, f.n(leadDays.toLong())),
            subtitle = reminderSub,
            checked = reminder,
            onCheckedChange = onReminderChange
        )
    }
}

@Composable
private fun DocOptionalDetailsSection(
    showOptional: Boolean,
    number: String,
    holder: String,
    onToggleOptional: () -> Unit,
    onNumberChange: (String) -> Unit,
    onHolderChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onToggleOptional),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appStringResource(if (showOptional) Res.string.doc_add_optional_hide else Res.string.doc_add_optional_show),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandColor
        )
    }
    AnimatedVisibility(visible = showOptional) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DocLabeledInput(
                label = appStringResource(Res.string.doc_add_number_optional),
                value = number,
                placeholder = appStringResource(Res.string.doc_add_number_placeholder),
                attention = false,
                keyboard = FieldKeyboards.code,
                onValueChange = { if (it.length <= MAX_DOC_TEXT_LENGTH) onNumberChange(it) }
            )
            DocLabeledInput(
                label = appStringResource(Res.string.doc_add_holder_optional),
                value = holder,
                placeholder = appStringResource(Res.string.doc_add_holder_placeholder),
                attention = false,
                keyboard = FieldKeyboards.name,
                onValueChange = { if (it.length <= MAX_DOC_TEXT_LENGTH) onHolderChange(it) }
            )
        }
    }
}

@Composable
private fun DocLabeledInput(
    label: String,
    value: String,
    placeholder: String,
    attention: Boolean,
    keyboard: androidx.compose.foundation.text.KeyboardOptions,
    onValueChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(SurfaceColor2)
                .border(1.5.dp, if (attention) WarningColor else BorderStrongColor, shape)
                .padding(14.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                cursorBrush = SolidColor(BrandColor),
                keyboardOptions = keyboard,
                keyboardActions = rememberFieldKeyboardActions(),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                decorationBox = { inner ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(text = placeholder, fontSize = 15.sp, color = TextTertiary)
                    }
                    inner()
                }
            )
        }
    }
}
