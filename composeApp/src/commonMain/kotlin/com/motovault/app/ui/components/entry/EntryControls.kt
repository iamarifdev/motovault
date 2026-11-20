package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_cancel
import motovault.composeapp.generated.resources.capture_voice_provider_detail
import motovault.composeapp.generated.resources.capture_voice_start
import motovault.composeapp.generated.resources.date_other
import motovault.composeapp.generated.resources.date_today
import motovault.composeapp.generated.resources.date_yesterday

enum class PillTone {
    DEFAULT,
    ACTIVE,
    CONFIRM_PENDING,
    CONFIRM_DONE
}

enum class EntryDateKey {
    TODAY,
    YESTERDAY,
    CUSTOM
}

@Composable
fun ConfirmPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.DEFAULT,
    icon: String? = null,
    trailingIcon: String? = null,
    contentDescription: String? = null
) {
    val haptic = currentHaptic
    val shape = RoundedCornerShape(999.dp)
    val bg = when (tone) {
        PillTone.DEFAULT -> SurfaceColor2
        PillTone.ACTIVE -> BrandTintColor
        PillTone.CONFIRM_PENDING -> SurfaceColor2
        PillTone.CONFIRM_DONE -> colorMix(HealthyColor, SurfaceColor, 0.12f)
    }
    val border = when (tone) {
        PillTone.DEFAULT -> BorderColor
        PillTone.ACTIVE -> BrandColor
        PillTone.CONFIRM_PENDING -> WarningColor
        PillTone.CONFIRM_DONE -> HealthyColor
    }
    val fg = when (tone) {
        PillTone.DEFAULT, PillTone.ACTIVE -> TextPrimary
        PillTone.CONFIRM_PENDING -> WarningColor
        PillTone.CONFIRM_DONE -> HealthyColor
    }

    Row(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(shape)
            .background(bg)
            .border(1.5.dp, border, shape)
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            V3Icon(path = "feather_icon/$icon", description = null, modifier = Modifier.size(13.dp), tint = fg)
        }
        Text(text = text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (trailingIcon != null) {
            V3Icon(path = "feather_icon/$trailingIcon", description = null, modifier = Modifier.size(13.dp), tint = fg)
        }
    }
}

@Composable
fun PickerField(
    text: String,
    isSet: Boolean,
    leadingIcon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = currentHaptic
    val shape = RoundedCornerShape(16.dp)
    val borderModifier = if (isSet) {
        Modifier.border(1.5.dp, BorderStrongColor, shape)
    } else {
        Modifier.v3DashedBorder(BorderStrongColor, 16.dp)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(shape)
            .then(borderModifier)
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Icon(
            path = "feather_icon/$leadingIcon",
            description = null,
            modifier = Modifier.size(18.dp),
            tint = if (isSet) BrandColor else TextSecondary
        )
        Text(
            text = text,
            color = if (isSet) TextPrimary else TextSecondary,
            fontSize = 15.sp,
            fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        V3Icon(
            path = "feather_icon/fi_chevron-right.svg",
            description = null,
            modifier = Modifier.size(18.dp),
            tint = TextTertiary
        )
    }
}

@Composable
fun OdoRow(
    label: String,
    valueText: String,
    active: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = when {
        isError -> ErrorColor
        active -> BrandColor
        else -> BorderColor
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) BrandTintColor else SurfaceColor)
            .border(2.dp, borderColor, shape)
            .padding(start = 12.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(20.dp), tint = TextPrimary)
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isError) ErrorColor else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = valueText,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
        }
        trailing?.invoke(this)
    }
}

@Composable
fun RowScope.DateChips(
    dateKey: EntryDateKey,
    customDate: String,
    onSelectKey: (EntryDateKey) -> Unit,
    onCustomDateChange: (String) -> Unit
) {
    val f = currentFmt
    val todayDate = today()
    var pickerOpen by remember { mutableStateOf(false) }
    EntryChip(
        text = appStringResource(Res.string.date_today),
        active = dateKey == EntryDateKey.TODAY,
        onClick = { onSelectKey(EntryDateKey.TODAY) }
    )
    EntryChip(
        text = appStringResource(Res.string.date_yesterday),
        active = dateKey == EntryDateKey.YESTERDAY,
        onClick = { onSelectKey(EntryDateKey.YESTERDAY) }
    )
    // Review N1: "অন্য দিন" opens the calendar sheet; the chip then shows the picked date.
    val picked = parseIsoDateOrNull(customDate).takeIf { dateKey == EntryDateKey.CUSTOM }
    EntryChip(
        text = picked?.let { f.date(it) } ?: appStringResource(Res.string.date_other),
        active = picked != null,
        onClick = { pickerOpen = true }
    )
    if (pickerOpen) {
        DatePickerSheet(
            initial = picked ?: LocalDate.fromEpochDays(todayDate.toEpochDays() - 2),
            max = todayDate,
            onDismiss = { pickerOpen = false },
            onPick = { date ->
                pickerOpen = false
                onCustomDateChange(date.toString())
                onSelectKey(EntryDateKey.CUSTOM)
            }
        )
    }
}

@Composable
internal fun VoiceDisclosureCard(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.dp, BorderColor, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = appStringResource(Res.string.capture_voice_provider_detail),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultButton(text = appStringResource(Res.string.capture_voice_start), size = ButtonSize.SM, onClick = onConfirm)
            VaultButton(
                text = appStringResource(Res.string.action_cancel),
                size = ButtonSize.SM,
                kind = ButtonKind.SECONDARY,
                onClick = onCancel
            )
        }
    }
}
