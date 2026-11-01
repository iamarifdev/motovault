package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SettingsAppearanceSection(
    state: VaultState,
    activeBrandId: String,
    rideBrandId: String,
    onUpdateState: (VaultState) -> Unit
) {
    val haptic = currentHaptic
        // Appearance section
        SectionTitle(appStringResource(Res.string.settings_appearance))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            // Theme
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appStringResource(Res.string.settings_theme), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

            // Brand color swatches
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appStringResource(Res.string.settings_color), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BRAND_COLOR_MAP.forEach { (bId, colors) ->
                        val isSelected = activeBrandId == bId
                        val swatchColor = Color(if (LocalVaultPalette.current.dark) colors.dark else colors.light)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    if (isSelected) 3.dp else 1.5.dp,
                                    if (isSelected) TextPrimary else BorderStrongColor,
                                    CircleShape
                                )
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    onUpdateState(state.copy(brandOverride = if (bId == rideBrandId) null else bId))
                                }
                        )
                    }
                }
                Text(
                    text = state.brandOverride?.let { id ->
                        appStringResource(Res.string.settings_brand_color_named, worlds.firstOrNull { it.id == id }?.name.orEmpty())
                    } ?: appStringResource(Res.string.settings_brand_color),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Language
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appStringResource(Res.string.settings_language), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

            // Digits
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appStringResource(Res.string.settings_digits), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(
                        Digits.BN to appStringResource(Res.string.settings_digits_bn),
                        Digits.EN to appStringResource(Res.string.settings_digits_en)
                    ),
                    selected = state.digits,
                    onSelect = { onUpdateState(state.copy(digits = it)) }
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Text scale
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appStringResource(Res.string.settings_text_size), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
internal fun SettingsPreferencesSection(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit
) {
        // Behavior section
        SectionTitle(appStringResource(Res.string.settings_preferences))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
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
