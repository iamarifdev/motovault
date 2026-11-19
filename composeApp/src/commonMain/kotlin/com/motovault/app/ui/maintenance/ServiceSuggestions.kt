package com.motovault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.service_find_other_work
import motovault.composeapp.generated.resources.service_suggestions_desc
import motovault.composeapp.generated.resources.service_what_done

@Composable
internal fun ServiceSuggestions(
    suggestions: List<String>,
    dueKeys: Set<String>,
    lastItemPrices: Map<String, Double>,
    onPickSuggestion: (String) -> Unit,
    onOpenPartsPicker: () -> Unit
) {
    val f = currentFmt
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = appStringResource(Res.string.service_what_done),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        ChipLine(contentDescription = appStringResource(Res.string.service_suggestions_desc)) {
            suggestions.forEach { key ->
                val item = catalogItemOf(key)
                if (item != null) {
                    EntryChip(
                        text = catalogShortLabel(item),
                        active = false,
                        icon = "fi_plus.svg",
                        hint = lastItemPrices[key]?.let(f::money),
                        warnDot = key in dueKeys,
                        onClick = { onPickSuggestion(key) }
                    )
                }
            }
        }
        PickerField(
            text = appStringResource(Res.string.service_find_other_work),
            isSet = false,
            leadingIcon = "fi_plus.svg",
            onClick = onOpenPartsPicker
        )
    }
}
