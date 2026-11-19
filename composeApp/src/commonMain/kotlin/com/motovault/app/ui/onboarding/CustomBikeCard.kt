package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sky.design.SkyTheme
import dev.sky.design.tokens.SkyTokens
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.onboarding_custom_cc_label
import motovault.composeapp.generated.resources.onboarding_custom_name_label
import motovault.composeapp.generated.resources.onboarding_custom_name_placeholder
import motovault.composeapp.generated.resources.onboarding_custom_title
import motovault.composeapp.generated.resources.onboarding_show_list

@Composable
internal fun CustomBikeCard(
    customName: String,
    customCc: String,
    onCustomNameChange: (String) -> Unit,
    onCustomCcChange: (String) -> Unit,
    onShowList: () -> Unit
) {
    val nameLabel = appStringResource(Res.string.onboarding_custom_name_label)
    val ccLabel = appStringResource(Res.string.onboarding_custom_cc_label)
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(BrandTintColor)
            .border(1.5.dp, BrandColor, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = appStringResource(Res.string.onboarding_custom_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                text = appStringResource(Res.string.onboarding_show_list),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandColor,
                modifier = Modifier.clickable(role = Role.Button, onClick = onShowList)
            )
        }
        Text(text = nameLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        OutlinedTextField(
            value = customName,
            onValueChange = onCustomNameChange,
            placeholder = { Text(appStringResource(Res.string.onboarding_custom_name_placeholder), color = TextTertiary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = nameLabel },
            textStyle = SkyTheme.typography.body,
            shape = RoundedCornerShape(SkyTokens.Radii.field),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SkyTheme.colors.fieldBorder,
                focusedBorderColor = BrandColor,
                unfocusedContainerColor = SurfaceColor,
                focusedContainerColor = SurfaceColor
            )
        )
        Text(text = ccLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
        OutlinedTextField(
            value = customCc,
            onValueChange = { raw -> onCustomCcChange(raw.filter { it.isDigit() || it in '০'..'৯' }) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = ccLabel },
            textStyle = SkyTheme.typography.body,
            shape = RoundedCornerShape(SkyTokens.Radii.field),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SkyTheme.colors.fieldBorder,
                focusedBorderColor = BrandColor,
                unfocusedContainerColor = SurfaceColor,
                focusedContainerColor = SurfaceColor
            )
        )
    }
}
