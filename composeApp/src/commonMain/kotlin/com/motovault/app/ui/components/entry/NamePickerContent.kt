package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class RecentNameOption(
    val name: String,
    val subtitle: String? = null
)

@Composable
fun NamePickerContent(
    value: String,
    recent: List<RecentNameOption>,
    placeholder: String,
    recentHeader: String,
    useTypedLabel: @Composable (String) -> String,
    clearLabel: String,
    onPick: (String) -> Unit,
    maxLength: Int = 60
) {
    val haptic = currentHaptic
    var query by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    val trimmed = query.trim()
    val shown = remember(recent, trimmed) {
        val needle = trimmed.lowercase()
        recent.filter { needle.isEmpty() || it.name.lowercase().contains(needle) }
    }
    val hasExactMatch = recent.any { it.name == trimmed }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicTextField(
            value = query,
            onValueChange = { if (it.length <= maxLength) query = it },
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor2)
                .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, RoundedCornerShape(12.dp))
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(text = placeholder, fontSize = 16.sp, color = TextTertiary)
                }
                inner()
            }
        )

        if (shown.isNotEmpty()) {
            Text(text = recentHeader, fontSize = 13.sp, color = TextSecondary)
        }

        val showUseTyped = trimmed.isNotEmpty() && !hasExactMatch
        if (showUseTyped || shown.isNotEmpty()) {
            val listShape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(listShape)
                    .border(1.dp, BorderColor, listShape)
                    .background(SurfaceColor)
            ) {
                var needsDivider = false
                if (showUseTyped) {
                    NamePickerRow(
                        icon = "fi_plus.svg",
                        label = useTypedLabel(trimmed),
                        subtitle = null,
                        selected = false,
                        onClick = {
                            haptic.perform(HapticKind.TICK)
                            onPick(trimmed)
                        }
                    )
                    needsDivider = true
                }
                shown.forEach { option ->
                    if (needsDivider) HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    NamePickerRow(
                        icon = "fi_history.svg",
                        label = option.name,
                        subtitle = option.subtitle,
                        selected = option.name == value,
                        onClick = {
                            haptic.perform(HapticKind.TICK)
                            onPick(option.name)
                        }
                    )
                    needsDivider = true
                }
            }
        }

        if (value.isNotBlank()) {
            VaultButton(
                text = clearLabel,
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = { onPick("") }
            )
        }
    }
}

@Composable
private fun NamePickerRow(
    icon: String,
    label: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(if (selected) BrandTintColor else SurfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        V3Icon(path = "feather_icon/$icon", description = null, modifier = Modifier.size(18.dp), tint = TextTertiary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 15.sp, color = TextPrimary)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
