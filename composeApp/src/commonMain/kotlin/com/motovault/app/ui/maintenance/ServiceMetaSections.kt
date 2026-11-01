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
internal fun ServiceSmartInput(
    open: Boolean,
    text: String,
    parsedCount: Int,
    onTextChange: (String) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit
) {
    val demoText = appStringResource(Res.string.service_demo_text)
    AnimatedVisibility(visible = open) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor2)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                    cursorBrush = SolidColor(BrandColor),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(appStringResource(Res.string.service_example), fontSize = 14.sp, color = TextTertiary)
                        inner()
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VaultButton(
                    text = appStringResource(Res.string.service_demo_voice),
                    kind = ButtonKind.SECONDARY,
                    size = ButtonSize.SM,
                    icon = "fi_mic.svg",
                    onClick = { onTextChange(demoText) }
                )
                VaultButton(
                    text = appStringResource(Res.string.service_add_count, parsedCount.toString()),
                    size = ButtonSize.SM,
                    enabled = parsedCount > 0,
                    onClick = { onAdd(); onClose() }
                )
            }
        }
    }
}

@Composable
internal fun ServiceDateOdometerSection(
    dateKey: String,
    onDate: (String) -> Unit,
    odoVal: Double,
    odoActive: Boolean,
    onOdo: () -> Unit,
    f: Fmt
) {
        // Date selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultChip(text = appStringResource(Res.string.service_today), active = dateKey == "today", onClick = { onDate("today") })
            VaultChip(text = appStringResource(Res.string.service_yesterday), active = dateKey == "yesterday", onClick = { onDate("yesterday") })
            VaultChip(text = appStringResource(Res.string.service_other_day), active = dateKey == "custom", onClick = { onDate("custom") })
        }

        // Odometer row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (odoActive) BrandTintColor else SurfaceColor)
                .border(2.dp, if (odoActive) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                .clickable { onOdo() }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    V3Icon(
                    path = "feather_icon/fi_gauge.svg",
                        description = null,
                        modifier = Modifier.size(20.dp),
                        tint = BrandColor
                    )
                    Column {
                        Text(text = appStringResource(Res.string.service_odometer), fontSize = 12.sp, color = TextSecondary)
                        Text(text = "${f.n(odoVal.toLong())} ${appStringResource(Res.string.garage_unit_km)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }

}
