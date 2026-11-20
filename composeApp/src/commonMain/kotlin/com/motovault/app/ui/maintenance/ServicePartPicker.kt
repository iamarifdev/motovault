package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.*

private const val MAX_PART_QUERY_LENGTH = 40

@Composable
internal fun ServicePartPickerSheet(
    selectedKeys: Set<String>,
    lastItemPrices: Map<String, Double>,
    dueKeys: Set<String>,
    onDone: (Set<String>, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    var picked by remember(selectedKeys) { mutableStateOf(selectedKeys) }
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }

    val catalogEntries = CATALOG_ITEMS.map { item -> Triple(item, catalogLabel(item), catalogShortLabel(item)) }
    val trimmed = query.trim()
    val needle = trimmed.lowercase()
    val matched = catalogEntries.filter { (_, label, short) ->
        needle.isEmpty() || label.lowercase().contains(needle) || short.lowercase().contains(needle)
    }
    val hasExact = catalogEntries.any { (_, label, short) -> label == trimmed || short == trimmed }
    val regular = matched.filter { (item, _, _) -> item.ruleSuffix != null }
    val other = matched.filter { (item, _, _) -> item.ruleSuffix == null }
    val added = picked.count { it !in selectedKeys }
    val removed = selectedKeys.count { it !in picked }
    val footerLabel = when {
        added > 0 -> appStringResource(Res.string.service_picker_done_add, f.d(added.toString()))
        removed > 0 -> appStringResource(Res.string.service_picker_done_remove)
        else -> appStringResource(Res.string.service_picker_done_empty)
    }

    Sheet(
        title = appStringResource(Res.string.service_picker_title),
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        closeLabel = appStringResource(Res.string.service_picker_close),
        footer = {
            VaultButton(
                text = footerLabel,
                full = true,
                enabled = added > 0 || removed > 0,
                onClick = { haptic.perform(HapticKind.SUCCESS); onDone(picked, null) }
            )
        }
    ) {
        ServicePartPickerBody(
            query = query,
            trimmed = trimmed,
            hasExact = hasExact,
            focused = focused,
            regular = regular,
            other = other,
            picked = picked,
            dueKeys = dueKeys,
            lastItemPrices = lastItemPrices,
            onFocusChange = { focused = it },
            onQueryChange = { if (it.length <= MAX_PART_QUERY_LENGTH) query = it },
            onAddCustom = { haptic.perform(HapticKind.SUCCESS); onDone(picked, trimmed) },
            onToggle = { key ->
                haptic.perform(HapticKind.TICK)
                picked = if (key in picked) picked - key else picked + key
            }
        )
    }
}

@Composable
private fun ServicePartPickerBody(
    query: String,
    trimmed: String,
    hasExact: Boolean,
    focused: Boolean,
    regular: List<Triple<ServiceCatalogItem, String, String>>,
    other: List<Triple<ServiceCatalogItem, String, String>>,
    picked: Set<String>,
    dueKeys: Set<String>,
    lastItemPrices: Map<String, Double>,
    onFocusChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onAddCustom: () -> Unit,
    onToggle: (String) -> Unit
) {
    PartSearchField(query, focused, onFocusChange, onQueryChange)
    if (trimmed.isNotEmpty() && !hasExact) {
        CustomAddCard(trimmed, onAddCustom)
    }
    if (regular.isNotEmpty()) {
        PartPickerGroup(
            title = appStringResource(Res.string.service_picker_group_regular),
            items = regular,
            picked = picked,
            dueKeys = dueKeys,
            lastItemPrices = lastItemPrices,
            onToggle = onToggle
        )
    }
    if (other.isNotEmpty()) {
        PartPickerGroup(
            title = appStringResource(Res.string.service_picker_group_other),
            items = other,
            picked = picked,
            dueKeys = dueKeys,
            lastItemPrices = lastItemPrices,
            onToggle = onToggle
        )
    }
}

@Composable
private fun PartSearchField(
    query: String,
    focused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit
) {
    val placeholder = appStringResource(Res.string.service_picker_search)
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        V3Icon(path = "feather_icon/fi_search.svg", description = null, modifier = Modifier.size(18.dp), tint = TextTertiary)
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            keyboardOptions = FieldKeyboards.search,
            keyboardActions = rememberFieldKeyboardActions(),
            modifier = Modifier.weight(1f).onFocusChanged { onFocusChange(it.isFocused) },
            decorationBox = { inner ->
                if (query.isEmpty()) Text(text = placeholder, fontSize = 15.sp, color = TextTertiary)
                inner()
            }
        )
    }
}

@Composable
private fun CustomAddCard(name: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        V3Icon(path = "feather_icon/fi_plus.svg", description = null, modifier = Modifier.size(18.dp), tint = BrandColor)
        Text(
            text = appStringResource(Res.string.service_picker_add_custom, name),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun PartPickerGroup(
    title: String,
    items: List<Triple<ServiceCatalogItem, String, String>>,
    picked: Set<String>,
    dueKeys: Set<String>,
    lastItemPrices: Map<String, Double>,
    onToggle: (String) -> Unit
) {
    val f = currentFmt
    Text(text = title, fontSize = 13.sp, color = TextSecondary)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape)
    ) {
        items.forEachIndexed { index, (item, label, _) ->
            if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
            val checked = item.key in picked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(if (checked) BrandTintColor else SurfaceColor)
                    .clickable(role = Role.Checkbox) { onToggle(item.key) }
                    .semantics { selected = checked }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (checked) BrandColor else SurfaceColor)
                        .border(2.dp, if (checked) BrandColor else BorderStrongColor, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) {
                        V3Icon(path = "feather_icon/fi_check.svg", description = null, modifier = Modifier.size(14.dp), tint = OnBrandColor)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = label, fontSize = 15.sp, color = TextPrimary)
                    if (item.key in dueKeys) {
                        Text(text = appStringResource(Res.string.service_due_indicator), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WarningColor)
                    }
                }
                lastItemPrices[item.key]?.let { price ->
                    Text(
                        text = appStringResource(Res.string.service_picker_last_price, f.money(price)),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
