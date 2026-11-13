package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.application.widget.WidgetDeepLink
import com.motovault.app.application.widget.WidgetSnapshotProjector
import com.motovault.app.application.widget.WidgetTaskStatus
import com.motovault.app.domain.garage.allBikes
import com.motovault.app.domain.garage.switchToBike
import com.motovault.app.domain.privacy.VaultAccessMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import motovault.composeapp.generated.resources.*

object WidgetNavigation {
    private val request = MutableStateFlow<String?>(null)
    internal val requests = request.asStateFlow()
    fun open(uri: String) {
        request.value = uri
    }

    internal fun handled(uri: String) {
        if (request.value == uri) request.value = null
    }
}

@Composable
internal fun WidgetNavigationEffect(
    state: VaultState,
    loading: Boolean,
    accessMode: VaultAccessMode,
    onCommit: (VaultState) -> Unit,
    onTab: (Tab) -> Unit,
    onSheet: (ActiveSheet) -> Unit
) {
    val widgetRequest by WidgetNavigation.requests.collectAsState()
    LaunchedEffect(widgetRequest, loading, accessMode) {
        val uri = widgetRequest ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        if (state.privacy.enabled && accessMode != VaultAccessMode.OWNER) return@LaunchedEffect
        val link = WidgetDeepLink.parse(uri)
        if (link != null) {
            if (link.bikeId != null && link.bikeId != state.ride?.id) {
                runCatching { state.switchToBike(link.bikeId) }.getOrNull()?.let(onCommit)
            }
            onTab(Tab.GARAGE)
            if (link.sheet in setOf("fuel", "odometer")) {
                onSheet(ActiveSheet(link.sheet))
            }
        }
        WidgetNavigation.handled(uri)
    }
}

@Composable
internal fun SettingsWidgetSection(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val todayEpoch = today().toEpochDays().toLong()
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val snapshot = remember(state, todayEpoch) {
        WidgetSnapshotProjector.project(state, todayEpoch, nowMillis)
    }
    val activeBikes = remember(state.ride, state.garageBikes) {
        state.allBikes().filterNot { it.archived }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (activeBikes.size > 1) {
            Text(
                text = appStringResource(Res.string.settings_widget_bike_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeBikes.forEach { bike ->
                    val selected = (state.widgetBikeId ?: state.ride?.id) == bike.id
                    VaultChip(
                        text = bike.displayName,
                        active = selected,
                        onClick = {
                            haptic.perform(HapticKind.TICK)
                            onUpdateState(state.copy(widgetBikeId = bike.id))
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(20.dp))
                .background(BrandTintColor)
                .border(1.dp, BrandLineColor, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (snapshot.privacyRedacted || snapshot.taskStatus == WidgetTaskStatus.LOCKED) {
                Text(
                    text = appStringResource(Res.string.settings_widget_privacy_locked),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarningColor
                )
            } else {
                val selectedBike = activeBikes.firstOrNull { it.id == snapshot.bikeId }
                val ruleTitle = selectedBike?.let { bike ->
                    val rules = if (bike.id == state.ride?.id) state.rules
                    else state.garageBikes.firstOrNull { it.ride.id == bike.id }?.rules.orEmpty()
                    rules.firstOrNull { it.id == snapshot.taskItemKey }?.name
                }?.substringBefore(" ও ")?.substringBefore(" পরিষ্কার")
                    ?: appStringResource(Res.string.garage_quick_service)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = snapshot.bikeName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandColor
                    )
                    snapshot.odometerKm?.let {
                        Text(text = f.km(it), fontSize = 12.sp, color = TextSecondary)
                    }
                }
                val (statusText, statusColor) = when (snapshot.taskStatus) {
                    WidgetTaskStatus.OVERDUE -> appStringResource(Res.string.settings_widget_status_overdue, ruleTitle) to ErrorColor
                    WidgetTaskStatus.SOON -> appStringResource(Res.string.settings_widget_status_soon, ruleTitle) to WarningColor
                    WidgetTaskStatus.OK -> appStringResource(Res.string.settings_widget_status_ok, ruleTitle) to LocalVaultPalette.current.ok
                    WidgetTaskStatus.UNKNOWN -> appStringResource(Res.string.settings_widget_status_unknown, ruleTitle) to TextSecondary
                    else -> appStringResource(Res.string.settings_widget_status_none) to TextSecondary
                }
                Text(text = statusText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceColor)
                        .clickable {
                            haptic.perform(HapticKind.TICK)
                            WidgetNavigation.open(WidgetDeepLink("fuel", snapshot.bikeId).toUri())
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appStringResource(Res.string.settings_widget_fuel), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceColor)
                        .clickable {
                            haptic.perform(HapticKind.TICK)
                            WidgetNavigation.open(WidgetDeepLink("odometer", snapshot.bikeId).toUri())
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appStringResource(Res.string.settings_widget_km), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text(
            text = appStringResource(Res.string.settings_widget_setup_hint),
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}
