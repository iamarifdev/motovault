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
fun QuickAddSheet(
    onDismiss: () -> Unit,
    onSelectFlow: (String) -> Unit
) {
    val haptic = currentHaptic
    Sheet(
        title = "কী যোগ করবেন?",
        onDismiss = onDismiss
    ) {
        val options = listOf(
            Triple("fi_fuel.svg", "তেল ভরা", "টাকা লিখুন, লিটার নিজেই হবে" to "fuel"),
            Triple("fi_tool.svg", "সার্ভিস", "এক ট্যাপে পার্টস যোগ" to "service"),
            Triple("fi_gauge.svg", "ওডোমিটার", "আনুমানিক কিমি ঠিক করুন" to "odometer"),
            Triple("fi_file-text.svg", "কাগজ", "স্ক্যান করে মেয়াদ রাখুন" to "docAdd")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (icon, label, subToGo) ->
                        val (sub, flow) = subToGo
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .heightIn(min = 140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceColor2)
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    onDismiss()
                                    onSelectFlow(flow)
                                }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BrandColor),
                                contentAlignment = Alignment.Center
                            ) {
                                V3Icon(
                                    path = "feather_icon/$icon",
                                    description = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = OnBrandColor
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = sub, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // Paste shortcut button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .v3DashedBorder(BrandLineColor, 16.dp)
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onDismiss()
                        onSelectFlow("paste")
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                V3Icon(
                    path = "feather_icon/fi_sparkles.svg",
                    description = null,
                    modifier = Modifier.size(20.dp),
                    tint = BrandColor
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = "লিখে বা পেস্ট করে সাজান", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(text = "মেকানিকের মেসেজ, বিকাশ বা নগদের টেক্সট", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}
