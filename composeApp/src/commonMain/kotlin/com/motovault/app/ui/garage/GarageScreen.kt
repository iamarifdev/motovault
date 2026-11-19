package com.motovault.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.motovault.app.domain.garage.switchToBike
import com.motovault.app.ui.garage.SeasonalCareSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun GarageScreen(
    state: VaultState,
    onOpenSheet: (String, Any?) -> Unit,
    onSwitchTab: (Tab) -> Unit,
    onOpenServiceForItem: (String) -> Unit,
    onUpdateState: (VaultState) -> Unit = {},
    onCommitState: (VaultState, (Boolean) -> Unit) -> Unit = { next, done -> onUpdateState(next); done(true) }
) {
    val currentDay = today()
    val snapshot = remember(state.ride, state.rules, state.logs, state.documents, state.reminderPolicy, currentDay) {
        buildGarageHomeSnapshot(state, currentDay)
    } ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        GarageZone(index = 0) {
            BikeShowroom(
                state = state,
                snapshot = snapshot,
                onOpenBikeSwitcher = { onOpenSheet("bikeSwitcher", null) },
                onSwitchBike = { targetId, done ->
                    val next = runCatching { state.switchToBike(targetId) }.getOrNull()
                    if (next == null) done(false) else onCommitState(next, done)
                },
                onOpenOdometer = { onOpenSheet("odometer", null) },
                onOpenHistory = { onSwitchTab(Tab.HISTORY) },
                onOpenNextJob = {
                    val job = snapshot.nextJob
                    if (job != null) onOpenSheet("gaugeDetail", job.rule.id) else onOpenSheet("service", null)
                }
            )
        }
        GarageZone(index = 1) {
            GarageQuickActions(
                fuelDot = snapshot.fuelWarningDot,
                docDot = snapshot.docWarningDot,
                onAction = { sheetName -> onOpenSheet(sheetName, null) }
            )
        }
        GarageZone(index = 2) {
            GarageJobsSection(
                snapshot = snapshot,
                onOpenWear = { ruleId -> onOpenSheet("gaugeDetail", ruleId) },
                onOpenDoc = { docId -> onOpenSheet("docView", docId) }
            )
        }
        GarageZone(index = 3) {
            SeasonalCareSection(onOpenBreakdown = { onOpenSheet("breakdown", null) })
        }
    }
}

@Composable
private fun GarageZone(index: Int, content: @Composable () -> Unit) {
    val reducedMotion = isReducedMotion
    val density = LocalDensity.current
    val slidePx = with(density) { 12.dp.toPx() }
    val alpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val offsetY = remember { Animatable(if (reducedMotion) 0f else slidePx) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            alpha.snapTo(1f)
            offsetY.snapTo(0f)
        } else {
            delay(index * 50L)
            launch { alpha.animateTo(1f, tween(VaultMotion.PageMs, easing = VaultMotion.SkyCurve)) }
            launch { offsetY.animateTo(0f, tween(VaultMotion.PageMs, easing = VaultMotion.SkyCurve)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
            }
    ) {
        content()
    }
}
