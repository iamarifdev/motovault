package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.settings_brand_color
import motovault.composeapp.generated.resources.settings_brand_color_named
import motovault.composeapp.generated.resources.settings_color
import motovault.composeapp.generated.resources.settings_digits
import motovault.composeapp.generated.resources.settings_digits_bn
import motovault.composeapp.generated.resources.settings_digits_en
import motovault.composeapp.generated.resources.settings_haptics
import motovault.composeapp.generated.resources.settings_haptics_desc
import motovault.composeapp.generated.resources.settings_language
import motovault.composeapp.generated.resources.settings_language_bn
import motovault.composeapp.generated.resources.settings_language_en
import motovault.composeapp.generated.resources.settings_location
import motovault.composeapp.generated.resources.settings_location_desc
import motovault.composeapp.generated.resources.settings_reduce_motion
import motovault.composeapp.generated.resources.settings_reduce_motion_desc
import motovault.composeapp.generated.resources.settings_text_extra_large
import motovault.composeapp.generated.resources.settings_text_large
import motovault.composeapp.generated.resources.settings_text_normal
import motovault.composeapp.generated.resources.settings_text_size
import motovault.composeapp.generated.resources.settings_theme
import motovault.composeapp.generated.resources.settings_theme_dark
import motovault.composeapp.generated.resources.settings_theme_light
import motovault.composeapp.generated.resources.settings_theme_system

@Composable
internal fun AppearanceSettingsContent(
    state: VaultState,
    rideBrandId: String,
    onUpdateState: (VaultState) -> Unit
) {
    SettingsCardStack {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appStringResource(Res.string.settings_theme), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultSegmented(
                options = listOf(
                    Appearance.SYSTEM to appStringResource(Res.string.settings_theme_system),
                    Appearance.LIGHT to appStringResource(Res.string.settings_theme_light),
                    Appearance.DARK to appStringResource(Res.string.settings_theme_dark)
                ),
                selected = state.appearance,
                onSelect = { onUpdateState(state.copy(appearance = it)) }
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        BrandSwatchPicker(
            activeBrandId = state.brandOverride ?: rideBrandId,
            brandOverride = state.brandOverride,
            onSelectBrand = { bId -> onUpdateState(state.copy(brandOverride = if (bId == rideBrandId) null else bId)) }
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appStringResource(Res.string.settings_text_size), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultSegmented(
                options = listOf(
                    1.0f to appStringResource(Res.string.settings_text_normal),
                    1.15f to appStringResource(Res.string.settings_text_large),
                    1.3f to appStringResource(Res.string.settings_text_extra_large)
                ),
                selected = state.textScale,
                onSelect = { onUpdateState(state.copy(textScale = it)) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrandSwatchPicker(
    activeBrandId: String,
    brandOverride: String?,
    onSelectBrand: (String) -> Unit
) {
    val haptic = currentHaptic
    val dark = LocalVaultPalette.current.dark
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(appStringResource(Res.string.settings_color), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BRAND_COLOR_MAP.forEach { (bId, colors) ->
                val isSelected = activeBrandId == bId
                val swatchColor = Color(if (dark) colors.dark else colors.light)
                val brandName = worlds.firstOrNull { it.id == bId }?.name.orEmpty()
                val swatchLabel = appStringResource(Res.string.settings_brand_color_named, brandName)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(if (isSelected) 3.dp else 1.5.dp, if (isSelected) TextPrimary else BorderStrongColor, CircleShape)
                        .selectable(selected = isSelected, role = Role.RadioButton) {
                            haptic.perform(HapticKind.TICK)
                            onSelectBrand(bId)
                        }
                        .semantics { contentDescription = swatchLabel }
                )
            }
        }
        val caption = brandOverride?.let { id ->
            appStringResource(Res.string.settings_brand_color_named, worlds.firstOrNull { it.id == id }?.name.orEmpty())
        } ?: appStringResource(Res.string.settings_brand_color)
        Text(text = caption, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
internal fun LanguageSettingsContent(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit
) {
    SettingsCardStack {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appStringResource(Res.string.settings_language), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultSegmented(
                options = listOf(
                    AppLanguage.BN to appStringResource(Res.string.settings_language_bn),
                    AppLanguage.EN to appStringResource(Res.string.settings_language_en)
                ),
                selected = state.language,
                onSelect = { onUpdateState(state.copy(language = it)) }
            )
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(appStringResource(Res.string.settings_digits), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultSegmented(
                options = listOf(
                    Digits.BN to appStringResource(Res.string.settings_digits_bn),
                    Digits.EN to appStringResource(Res.string.settings_digits_en)
                ),
                selected = state.digits,
                onSelect = { onUpdateState(state.copy(digits = it)) }
            )
        }
    }
}

@Composable
internal fun MoreSettingsContent(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit
) {
    SettingsCardStack {
        VaultSwitch(
            title = appStringResource(Res.string.settings_haptics),
            subtitle = appStringResource(Res.string.settings_haptics_desc),
            checked = state.haptics,
            onCheckedChange = { onUpdateState(state.copy(haptics = it)) }
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        VaultSwitch(
            title = appStringResource(Res.string.settings_reduce_motion),
            subtitle = appStringResource(Res.string.settings_reduce_motion_desc),
            checked = state.reduceMotion,
            onCheckedChange = { onUpdateState(state.copy(reduceMotion = it)) }
        )
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        VaultSwitch(
            title = appStringResource(Res.string.settings_location),
            subtitle = appStringResource(Res.string.settings_location_desc),
            checked = state.autoLocation,
            onCheckedChange = { onUpdateState(state.copy(autoLocation = it)) }
        )
    }
}
