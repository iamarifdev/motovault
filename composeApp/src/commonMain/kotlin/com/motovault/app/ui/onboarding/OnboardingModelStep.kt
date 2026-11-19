package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.onboarding_choose_one_model
import motovault.composeapp.generated.resources.onboarding_enter_model_name
import motovault.composeapp.generated.resources.onboarding_model_lead
import motovault.composeapp.generated.resources.onboarding_model_title
import motovault.composeapp.generated.resources.onboarding_no_models_found
import motovault.composeapp.generated.resources.onboarding_not_listed
import motovault.composeapp.generated.resources.onboarding_search_desc
import motovault.composeapp.generated.resources.onboarding_search_placeholder
import motovault.composeapp.generated.resources.onboarding_select_named

@Composable
internal fun OnboardingModelStep(
    brand: BrandWorld,
    query: String,
    families: List<BikeFamily>,
    selectedFamilyId: String?,
    selectedVariantId: String?,
    customMode: Boolean,
    customName: String,
    customCc: String,
    onQueryChange: (String) -> Unit,
    onSelectFamily: (BikeFamily) -> Unit,
    onSelectVariant: (BikeVariant) -> Unit,
    onEnableCustom: () -> Unit,
    onDisableCustom: () -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomCcChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val activeVariant = remember(families, selectedFamilyId, selectedVariantId) {
        val fam = families.firstOrNull { it.id == selectedFamilyId }
        fam?.variants?.firstOrNull { it.modelId == selectedVariantId } ?: fam?.variants?.firstOrNull()
    }
    val canProceed = if (customMode) customName.trim().isNotEmpty() else activeVariant != null
    val ctaText = when {
        customMode && customName.trim().isNotEmpty() -> appStringResource(Res.string.onboarding_select_named, customName.trim())
        customMode -> appStringResource(Res.string.onboarding_enter_model_name)
        activeVariant != null -> appStringResource(Res.string.onboarding_select_named, activeVariant.name)
        else -> appStringResource(Res.string.onboarding_choose_one_model)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = appStringResource(Res.string.onboarding_model_title, brand.name),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(text = appStringResource(Res.string.onboarding_model_lead), fontSize = 16.sp, color = TextSecondary)
        ModelSearchField(query = query, onQueryChange = onQueryChange)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            families.forEach { family ->
                val isSelected = !customMode && selectedFamilyId == family.id
                val rowVariant = if (isSelected) (activeVariant ?: family.variants.first()) else family.variants.first()
                ModelFamilyRow(
                    family = family,
                    selected = isSelected,
                    activeVariant = rowVariant,
                    onSelectFamily = { onSelectFamily(family) },
                    onSelectVariant = onSelectVariant
                )
            }
            if (families.isEmpty()) {
                Text(text = appStringResource(Res.string.onboarding_no_models_found), fontSize = 14.sp, color = TextSecondary)
            }
        }
        if (customMode) {
            CustomBikeCard(customName, customCc, onCustomNameChange, onCustomCcChange, onShowList = onDisableCustom)
        } else {
            Text(
                text = appStringResource(Res.string.onboarding_not_listed),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandColor,
                modifier = Modifier.clickable(role = Role.Button, onClick = onEnableCustom).padding(vertical = 4.dp)
            )
        }
        VaultButton(text = ctaText, full = true, enabled = canProceed, onClick = onContinue)
    }
}

@Composable
private fun ModelSearchField(query: String, onQueryChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val searchDesc = appStringResource(Res.string.onboarding_search_desc)
    val placeholder = appStringResource(Res.string.onboarding_search_placeholder)
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Icon(path = "feather_icon/fi_search.svg", description = null, modifier = Modifier.size(20.dp), tint = TextTertiary)
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                cursorBrush = SolidColor(BrandColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = searchDesc },
                decorationBox = { inner ->
                    if (query.isEmpty()) Text(text = placeholder, fontSize = 15.sp, color = TextTertiary)
                    inner()
                }
            )
        }
    }
}
