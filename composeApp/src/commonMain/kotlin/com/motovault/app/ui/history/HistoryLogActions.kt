package com.motovault.app.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.action_delete_short
import motovault.composeapp.generated.resources.history_edit_action

@Composable
internal fun HistoryLogActions(onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryActionButton(
            text = appStringResource(Res.string.history_edit_action),
            icon = "feather_icon/fi_edit-2.svg",
            color = BrandColor,
            onClick = onEdit,
            modifier = Modifier.weight(1f)
        )
        HistoryActionButton(
            text = appStringResource(Res.string.action_delete_short),
            icon = "feather_icon/fi_trash-2.svg",
            color = ErrorColor,
            onClick = onDelete,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistoryActionButton(
    text: String,
    icon: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .border(1.5.dp, BorderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        V3Icon(path = icon, description = null, modifier = Modifier.size(16.dp), tint = color)
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
