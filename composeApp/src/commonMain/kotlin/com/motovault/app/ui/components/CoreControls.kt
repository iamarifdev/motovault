package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sky.design.SkyButton
import dev.sky.design.SkyTheme
import dev.sky.design.tokens.SkyTokens

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) =
    Text(text.uppercase(), color = color, fontSize = 12.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)

@Composable
fun Panel(onClick: (() -> Unit)? = null, tint: Color = SurfaceColor, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(SkyTokens.Radii.menu))
            .background(tint)
            .border(1.dp, BorderColor, RoundedCornerShape(SkyTokens.Radii.menu))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun Primary(text: String, icon: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    SkyButton(text, onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled)
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, style = SkyTheme.typography.label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            singleLine = true,
            textStyle = SkyTheme.typography.body,
            shape = RoundedCornerShape(SkyTokens.Radii.field),
            keyboardOptions = if (numeric) FieldKeyboards.decimal else FieldKeyboards.sentence,
            keyboardActions = rememberFieldKeyboardActions(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SkyTheme.colors.fieldBorder,
                focusedBorderColor = SkyTheme.colors.accent,
                unfocusedContainerColor = SkyTheme.colors.input,
                focusedContainerColor = SkyTheme.colors.input
            )
        )
    }
}
