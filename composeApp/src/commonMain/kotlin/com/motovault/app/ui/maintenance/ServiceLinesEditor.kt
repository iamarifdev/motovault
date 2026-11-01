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
import androidx.compose.runtime.snapshots.SnapshotStateList
import motovault.composeapp.generated.resources.*

@Composable
internal fun ServiceLinesEditor(
    rows: SnapshotStateList<ServiceRowState>,
    entry: NumberEntryState,
    f: Fmt,
    onPrice: (String) -> Unit,
    onRemove: (ServiceRowState) -> Unit
) {
    val haptic = currentHaptic
        if (rows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { r ->
                    val isPriceActive = entry.activeField == "p:${r.uid}"
                    val pVal = parseNum(entry.values["p:${r.uid}"] ?: "")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceColor2)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = r.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                            // Stepper
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(SurfaceColor)
                                    .border(1.dp, BorderColor, RoundedCornerShape(999.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(34.dp).clickable {
                                        if (r.qty > 1) {
                                            haptic.perform(HapticKind.TICK)
                                            r.qty -= 1
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Text(
                                    text = f.d(r.qty.toInt().toString()),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Box(
                                    modifier = Modifier.size(34.dp).clickable {
                                        if (r.qty < 20) {
                                            haptic.perform(HapticKind.TICK)
                                            r.qty += 1
                                        }
                                    },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            // Price button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isPriceActive) BrandTintColor else SurfaceColor)
                                    .border(2.dp, if (isPriceActive) BrandColor else BorderColor, RoundedCornerShape(10.dp))
                                    .clickable { onPrice("p:${r.uid}") }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (pVal == null) appStringResource(Res.string.service_price_unknown) else f.money(pVal * r.qty),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pVal == null) BrandColor else TextPrimary
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            VaultIconButton(
                                icon = "fi_trash-2.svg",
                                label = appStringResource(Res.string.service_remove),
                                onClick = { rows.remove(r) }
                            )
                        }

                        // Viscosity chips if engine oil
                        if (r.itemKey == "engine_oil") {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VISCOSITY_GRADES.forEach { visc ->
                                    VaultChip(
                                        text = visc,
                                        active = r.detail == visc,
                                        onClick = { r.detail = if (r.detail == visc) null else visc }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

}
