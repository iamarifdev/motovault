package com.motovault.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.settings_bike_meta
import motovault.composeapp.generated.resources.settings_bike_meta_multi
import motovault.composeapp.generated.resources.settings_bike_title
import motovault.composeapp.generated.resources.settings_change_short
import org.jetbrains.compose.resources.painterResource

internal enum class SettingsValueTone { DEFAULT, OK, WARN }

@Composable
internal fun SettingsProfileCard(
    brand: BrandWorld,
    model: BikeModel?,
    ride: Ride,
    bikeCount: Int,
    onClick: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val shape = RoundedCornerShape(16.dp)
    val bikeTitle = appStringResource(Res.string.settings_bike_title, brand.name, ride.displayName)
    val yearStr = f.d(ride.year.toString())
    val kmStr = f.km(ride.odometer)
    val bikeMeta = if (bikeCount > 1) {
        appStringResource(Res.string.settings_bike_meta_multi, yearStr, kmStr, f.d(bikeCount.toString()))
    } else {
        appStringResource(Res.string.settings_bike_meta, yearStr, kmStr)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape)
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsBikeThumb(model)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = bikeTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = bikeMeta, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = appStringResource(Res.string.settings_change_short), fontSize = 13.sp, color = TextSecondary)
        V3Icon(path = "feather_icon/fi_chevron-right.svg", description = null, modifier = Modifier.size(18.dp), tint = TextTertiary)
    }
}

@Composable
private fun SettingsBikeThumb(model: BikeModel?) {
    val palette = LocalVaultPalette.current
    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.radialGradient(listOf(palette.photoWellStart, palette.photoWellEnd))),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            Image(
                painter = painterResource(model.photo),
                contentDescription = null,
                modifier = Modifier.size(width = 76.dp, height = 52.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(32.dp), tint = BrandColor)
        }
    }
}

@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        SettingsCardStack(content = content)
    }
}

@Composable
internal fun SettingsCardStack(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, shape),
        content = content
    )
}

@Composable
internal fun SettingsRow(
    icon: String,
    label: String,
    value: String? = null,
    tone: SettingsValueTone = SettingsValueTone.DEFAULT,
    showDividerAbove: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = currentHaptic
    if (showDividerAbove) {
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onClick()
            }
            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandTintColor),
            contentAlignment = Alignment.Center
        ) {
            V3Icon(path = "feather_icon/$icon", description = null, modifier = Modifier.size(19.dp), tint = BrandColor)
        }
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
        if (value != null) {
            val valueColor = when (tone) {
                SettingsValueTone.OK -> HealthyColor
                SettingsValueTone.WARN -> WarningColor
                SettingsValueTone.DEFAULT -> TextSecondary
            }
            val weight = if (tone == SettingsValueTone.DEFAULT) FontWeight.Normal else FontWeight.SemiBold
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = weight,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
        V3Icon(path = "feather_icon/fi_chevron-right.svg", description = null, modifier = Modifier.size(18.dp), tint = TextTertiary)
    }
}
