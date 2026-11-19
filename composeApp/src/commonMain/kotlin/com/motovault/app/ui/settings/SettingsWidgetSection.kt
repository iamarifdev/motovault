package com.motovault.app.ui

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.Role
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
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.garage_quick_service
import motovault.composeapp.generated.resources.settings_widget_fuel
import motovault.composeapp.generated.resources.settings_widget_km
import motovault.composeapp.generated.resources.settings_widget_privacy_locked
import motovault.composeapp.generated.resources.settings_widget_setup_hint
import motovault.composeapp.generated.resources.settings_widget_status_none
import motovault.composeapp.generated.resources.settings_widget_status_ok
import motovault.composeapp.generated.resources.settings_widget_status_overdue
import motovault.composeapp.generated.resources.settings_widget_status_soon
import motovault.composeapp.generated.resources.settings_widget_status_unknown

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
internal fun WidgetPreviewContent(state: VaultState) {
    val f = currentFmt
    val haptic = currentHaptic
    val todayEpoch = today().toEpochDays().toLong()
    val nowMillis = Clock.System.now().toEpochMilliseconds()
    val snapshot = remember(state, todayEpoch) {
        WidgetSnapshotProjector.project(state, todayEpoch, nowMillis)
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier = Modifier
                .widthIn(max = 220.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(BrandTintColor)
                .border(1.dp, BrandLineColor, RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (snapshot.privacyRedacted || snapshot.taskStatus == WidgetTaskStatus.LOCKED) {
                Text(
                    text = appStringResource(Res.string.settings_widget_privacy_locked),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(16.dp), tint = BrandColor)
                    Text(text = snapshot.bikeName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandColor, modifier = Modifier.weight(1f))
                    snapshot.odometerKm?.let { Text(text = f.km(it), fontSize = 12.sp, color = TextSecondary) }
                }
                WidgetTaskLine(state = state, snapshotBikeId = snapshot.bikeId, taskItemKey = snapshot.taskItemKey, taskStatus = snapshot.taskStatus)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetShortcutButton(appStringResource(Res.string.settings_widget_fuel), Modifier.weight(1f)) {
                    haptic.perform(HapticKind.TICK)
                    WidgetNavigation.open(WidgetDeepLink("fuel", snapshot.bikeId).toUri())
                }
                WidgetShortcutButton(appStringResource(Res.string.settings_widget_km), Modifier.weight(1f)) {
                    haptic.perform(HapticKind.TICK)
                    WidgetNavigation.open(WidgetDeepLink("odometer", snapshot.bikeId).toUri())
                }
            }
        }
        Text(text = appStringResource(Res.string.settings_widget_setup_hint), fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun WidgetTaskLine(
    state: VaultState,
    snapshotBikeId: String?,
    taskItemKey: String?,
    taskStatus: WidgetTaskStatus
) {
    val activeBikes = state.allBikes().filterNot { it.archived }
    val selectedBike = activeBikes.firstOrNull { it.id == snapshotBikeId }
    val ruleTitle = selectedBike?.let { bike ->
        val rules = if (bike.id == state.ride?.id) state.rules
        else state.garageBikes.firstOrNull { it.ride.id == bike.id }?.rules.orEmpty()
        rules.firstOrNull { it.id == taskItemKey }?.name
    }?.substringBefore(" ও ")?.substringBefore(" পরিষ্কার")
        ?: appStringResource(Res.string.garage_quick_service)

    val (statusText, statusColor) = when (taskStatus) {
        WidgetTaskStatus.OVERDUE -> appStringResource(Res.string.settings_widget_status_overdue, ruleTitle) to ErrorColor
        WidgetTaskStatus.SOON -> appStringResource(Res.string.settings_widget_status_soon, ruleTitle) to WarningColor
        WidgetTaskStatus.OK -> appStringResource(Res.string.settings_widget_status_ok, ruleTitle) to HealthyColor
        WidgetTaskStatus.UNKNOWN -> appStringResource(Res.string.settings_widget_status_unknown, ruleTitle) to TextSecondary
        else -> appStringResource(Res.string.settings_widget_status_none) to TextSecondary
    }
    Text(text = statusText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
}

@Composable
private fun WidgetShortcutButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}
