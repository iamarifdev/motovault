package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.sky.design.tokens.SkyTokens
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.FuelEntryRules
import com.motovault.app.domain.fuel.MAX_FUEL_LITRES
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.domain.fuel.FuelTimelineEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

@Composable
fun OdometerSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val ride = state.ride ?: return
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    val estimate = remember(ride, state.logs) { WearCalculator.estimateOdometer(ride, state.logs, today) }
    val entry = rememberNumberEntry(
        initialValues = mapOf("odo" to estimate.toInt().toString()),
        firstField = "odo"
    )
    var touched by remember { mutableStateOf(false) }
    var camScanning by remember { mutableStateOf(false) }
    var camDone by remember { mutableStateOf(false) }

    val odoVal = parseNum(entry.values["odo"] ?: "") ?: 0.0
    val badError = if (odoVal < ride.odometer) appStringResource(Res.string.odo_error_below_current, f.n(ride.odometer.toLong())) else null
    val savedMessage = appStringResource(Res.string.odo_saved, f.km(odoVal))

    Sheet(
        title = appStringResource(Res.string.odo_title),
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = if (badError != null) appStringResource(Res.string.odo_fix) else if (touched) appStringResource(Res.string.odo_save) else appStringResource(Res.string.odo_ok_save),
                onClick = {
                    if (badError != null) {
                        haptic.perform(HapticKind.ERROR)
                    } else {
                        val next = state.copy(ride = ride.copy(odometer = odoVal, odometerConfirmedDate = today.toString()))
                        onSave(next, savedMessage) {}
                    }
                },
                full = true,
                enabled = badError == null,
                icon = "fi_check.svg"
            )
            VaultKeypad(
                onKey = { k ->
                    touched = true
                    entry.press(k)
                },
                decimal = false
            )
        }
    ) {
        // Hero reading
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (touched) appStringResource(Res.string.odo_typed) else appStringResource(Res.string.odo_today_estimate),
                fontSize = 13.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountUp(
                    value = odoVal,
                    format = { n -> f.d(formatLakh(n.toLong())) },
                    style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = appStringResource(Res.string.garage_unit_km),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
            Text(
                text = appStringResource(Res.string.odo_current, f.km(ride.odometer)),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        if (badError != null) {
            Text(text = badError, color = ErrorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Bump chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10, 50, 100, 500).forEach { bump ->
                VaultChip(
                    text = "+${f.d(bump.toString())}",
                    onClick = {
                        entry.set("odo", (odoVal + bump).toInt().toString())
                        touched = true
                    }
                )
            }
            VaultChip(
                text = appStringResource(Res.string.odo_estimated),
                onClick = {
                    entry.set("odo", estimate.toInt().toString())
                    touched = false
                }
            )
        }

        // LCD photo recognition simulated demo
        VaultButton(
            text = appStringResource(Res.string.odo_scan_demo),
            kind = ButtonKind.SECONDARY,
            icon = "fi_camera.svg",
            full = true,
            onClick = {
                camScanning = true
                camDone = false
            }
        )

        AnimatedVisibility(visible = camScanning) {
            LaunchedEffect(Unit) {
                delay(1700)
                camDone = true
                haptic.perform(HapticKind.SUCCESS)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D1210))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF9BB59A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (estimate.toInt() + 3).toString().padStart(6, '0'),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B2A1B)
                    )
                }
                if (!camDone) {
                    Text(
                        text = appStringResource(Res.string.odo_scan_prompt),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = appStringResource(Res.string.odo_detected, f.n((estimate + 3).toLong())),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VaultButton(
                            text = appStringResource(Res.string.odo_confirm),
                            size = ButtonSize.SM,
                            onClick = {
                                entry.set("odo", (estimate.toInt() + 3).toString())
                                touched = true
                                camScanning = false
                            }
                        )
                        VaultButton(
                            text = appStringResource(Res.string.odo_manual_entry),
                            kind = ButtonKind.SECONDARY,
                            size = ButtonSize.SM,
                            onClick = { camScanning = false }
                        )
                    }
                }
            }
        }
    }
}
