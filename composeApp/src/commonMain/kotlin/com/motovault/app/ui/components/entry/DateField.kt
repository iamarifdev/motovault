package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.input.NormalizedNumbers
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.date_field_pick

/** Parses a stored yyyy-mm-dd value, accepting Bangla digits. */
internal fun parseIsoDateOrNull(value: String): LocalDate? =
    runCatching { LocalDate.parse(NormalizedNumbers.ascii(value.trim())) }.getOrNull()

/**
 * A field that shows the chosen date in words and opens [DatePickerSheet] on tap (review N1).
 * The value stays yyyy-mm-dd so existing validation keeps working.
 */
@Composable
internal fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    description: String,
    fallback: LocalDate,
    modifier: Modifier = Modifier,
    min: LocalDate? = null,
    max: LocalDate? = null,
    attention: Boolean = false
) {
    val f = currentFmt
    val parsed = parseIsoDateOrNull(value)
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (attention) WarningColor else BorderStrongColor, shape)
            .clickable(role = Role.Button) { open = true }
            .semantics { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = parsed?.let { f.dateFull(it) } ?: appStringResource(Res.string.date_field_pick),
            fontSize = 15.sp,
            fontWeight = if (parsed != null) FontWeight.SemiBold else FontWeight.Normal,
            color = if (parsed != null) TextPrimary else TextTertiary,
            modifier = Modifier.weight(1f)
        )
        V3Icon(
            path = "feather_icon/fi_chevron-down.svg",
            description = null,
            modifier = Modifier.size(18.dp),
            tint = TextTertiary
        )
    }
    if (open) {
        DatePickerSheet(
            initial = parsed ?: fallback,
            min = min,
            max = max,
            title = description,
            onDismiss = { open = false },
            onPick = { onValueChange(it.toString()); open = false }
        )
    }
}
