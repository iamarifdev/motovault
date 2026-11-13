package com.motovault.app.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.RidingSeason
import com.motovault.app.domain.model.WeatherCacheState
import com.motovault.app.domain.seasonal.BMD_OFFICIAL_URL
import com.motovault.app.domain.seasonal.SeasonalCarePolicy
import com.motovault.app.domain.seasonal.WEATHER_CACHE_TTL_HOURS
import com.motovault.app.ui.*
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.*

@Composable
internal fun SeasonalCareSection(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onOpenBreakdown: () -> Unit
) {
    val date = today()
    val season = SeasonalCarePolicy.seasonForMonth(date.monthNumber)
    val bikeId = state.ride?.id
    val dismissed = SeasonalCarePolicy.isDismissed(state.dismissedSeasonKeys, season, date.year, bikeId)
    val haptic = currentHaptic

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(appStringResource(Res.string.seasonal_section_title))
        if (dismissed) {
            Panel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VaultButton(
                        text = appStringResource(Res.string.seasonal_reenable_action),
                        size = ButtonSize.SM,
                        kind = ButtonKind.SECONDARY,
                        onClick = {
                            haptic.perform(HapticKind.TICK)
                            val updated = SeasonalCarePolicy.toggleDismissedSeason(
                                state.dismissedSeasonKeys, season, date.year, bikeId
                            )
                            onUpdateState(state.copy(dismissedSeasonKeys = updated))
                        }
                    )
                    VaultButton(
                        text = appStringResource(Res.string.breakdown_open_action),
                        size = ButtonSize.SM,
                        onClick = {
                            haptic.perform(HapticKind.TICK)
                            onOpenBreakdown()
                        }
                    )
                }
            }
        } else {
            SeasonalCareCard(state, season, date.year, bikeId, onUpdateState, onOpenBreakdown)
        }
    }
}

@Composable
private fun SeasonalCareCard(
    state: VaultState,
    season: RidingSeason,
    year: Int,
    bikeId: String?,
    onUpdateState: (VaultState) -> Unit,
    onOpenBreakdown: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val haptic = currentHaptic
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val cacheState = state.weatherCache.stateAt(nowMillis, WEATHER_CACHE_TTL_HOURS)
    val items = SeasonalCarePolicy.checklistFor(season)

    Panel {
        SeasonalHeaderRow(season, cacheState)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val checked = item.id in state.checkedSeasonalItems
                SeasonalChecklistRow(
                    label = seasonalItemLabel(item.id),
                    checked = checked,
                    onToggle = {
                        haptic.perform(HapticKind.TICK)
                        val updated = SeasonalCarePolicy.toggleChecklistItem(state.checkedSeasonalItems, item.id)
                        onUpdateState(state.copy(checkedSeasonalItems = updated))
                    }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VaultChip(
                text = appStringResource(Res.string.seasonal_bmd_link_action),
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    runCatching { uriHandler.openUri(BMD_OFFICIAL_URL) }
                }
            )
            VaultChip(
                text = appStringResource(Res.string.seasonal_dismiss_action),
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    val updated = SeasonalCarePolicy.toggleDismissedSeason(
                        state.dismissedSeasonKeys, season, year, bikeId
                    )
                    onUpdateState(state.copy(dismissedSeasonKeys = updated))
                }
            )
        }
        VaultButton(
            text = appStringResource(Res.string.breakdown_open_action),
            kind = ButtonKind.SECONDARY,
            full = true,
            onClick = {
                haptic.perform(HapticKind.TICK)
                onOpenBreakdown()
            }
        )
    }
}

@Composable
private fun SeasonalHeaderRow(season: RidingSeason, cacheState: WeatherCacheState) {
    val seasonTitle = when (season) {
        RidingSeason.MONSOON -> appStringResource(Res.string.seasonal_monsoon_title)
        RidingSeason.WINTER_DUST -> appStringResource(Res.string.seasonal_winter_title)
        RidingSeason.HEAT -> appStringResource(Res.string.seasonal_heat_title)
    }
    val weatherStatus = when (cacheState) {
        WeatherCacheState.OFFLINE_ONLY -> appStringResource(Res.string.seasonal_weather_offline)
        WeatherCacheState.FRESH -> appStringResource(Res.string.seasonal_weather_fresh)
        WeatherCacheState.EXPIRED -> appStringResource(Res.string.seasonal_weather_expired)
    }
    val statusColor = if (cacheState == WeatherCacheState.EXPIRED) WarningColor else TextSecondary

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = seasonTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = appStringResource(Res.string.seasonal_region_label), fontSize = 11.sp, color = TextSecondary)
        }
        Text(text = weatherStatus, fontSize = 12.sp, color = statusColor)
    }
}

@Composable
private fun SeasonalChecklistRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val bg = if (checked) BrandTintColor else CanvasColor
    val border = if (checked) BrandLineColor else BorderColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) BrandColor else SurfaceColor)
                .border(1.dp, if (checked) BrandColor else TextSecondary, RoundedCornerShape(4.dp))
        )
        Text(text = label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun seasonalItemLabel(itemId: String): String = when (itemId) {
    "monsoon_chain" -> appStringResource(Res.string.seasonal_item_monsoon_chain)
    "monsoon_brakes" -> appStringResource(Res.string.seasonal_item_monsoon_brakes)
    "monsoon_electrical" -> appStringResource(Res.string.seasonal_item_monsoon_electrical)
    "winter_air_filter" -> appStringResource(Res.string.seasonal_item_winter_air_filter)
    "winter_battery" -> appStringResource(Res.string.seasonal_item_winter_battery)
    "winter_visibility" -> appStringResource(Res.string.seasonal_item_winter_visibility)
    "heat_coolant_oil" -> appStringResource(Res.string.seasonal_item_heat_coolant_oil)
    "heat_tire_pressure" -> appStringResource(Res.string.seasonal_item_heat_tire_pressure)
    else -> appStringResource(Res.string.seasonal_item_heat_brake_fluid)
}
