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
fun PasteSheet(
    onDismiss: () -> Unit,
    onSelectFlow: (String, Any?) -> Unit,
    language: AppLanguage = AppLanguage.BN
) {
    val f = currentFmt
    val haptic = currentHaptic
    var text by remember { mutableStateOf("") }
    val parsedService = remember(text) { SmartParse.parseServiceText(text) }
    val parsedAmount = remember(text) { SmartParse.parseAmount(text) }
    val catalogShort = CATALOG_ITEMS.associate { it.key to appStringResource(it.shortLabel) }
    val exampleBkash = appStringResource(Res.string.paste_example_bkash)
    val exampleMechanic = appStringResource(Res.string.paste_example_mechanic)
    val isFuel = parsedAmount != null && SmartParse.FUEL_WORDS.containsMatchIn(text) && parsedService.lines.isEmpty()
    val hasService = parsedService.lines.isNotEmpty()

    Sheet(
        title = appStringResource(Res.string.paste_title),
        onDismiss = onDismiss
    ) {
        Text(appStringResource(Res.string.paste_desc), color = TextSecondary, fontSize = 14.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VaultChip(
                text = appStringResource(Res.string.paste_bkash),
                onClick = { text = exampleBkash }
            )
            VaultChip(
                text = appStringResource(Res.string.paste_mechanic),
                onClick = { text = exampleMechanic }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor2)
                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                cursorBrush = SolidColor(BrandColor),
                decorationBox = { inner ->
                    if (text.isEmpty()) Text(appStringResource(Res.string.paste_placeholder), fontSize = 15.sp, color = TextTertiary)
                    inner()
                }
            )
        }

        if (isFuel) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandTintColor)
                    .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V3Icon(path = "feather_icon/fi_droplet.svg", description = null, modifier = Modifier.size(20.dp), tint = BrandColor)
                    Text(appStringResource(Res.string.paste_fuel_detected, f.money(parsedAmount!!)), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                VaultButton(
                    text = appStringResource(Res.string.paste_open_fuel),
                    size = ButtonSize.SM,
                    onClick = {
                        haptic.perform(HapticKind.SUCCESS)
                        onDismiss()
                        onSelectFlow("fuel", parsedAmount)
                    }
                )
            }
        }

        if (hasService) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandTintColor)
                    .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    V3Icon(path = "feather_icon/fi_tool.svg", description = null, modifier = Modifier.size(20.dp), tint = BrandColor)
                    val summary = parsedService.lines.joinToString(", ") { catalogShort[it.item] ?: it.item }
                    Text(summary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                VaultButton(
                    text = appStringResource(Res.string.paste_open_service),
                    size = ButtonSize.SM,
                    onClick = {
                        haptic.perform(HapticKind.SUCCESS)
                        onDismiss()
                        onSelectFlow("service", parsedService.lines)
                    }
                )
            }
        }
    }
}
