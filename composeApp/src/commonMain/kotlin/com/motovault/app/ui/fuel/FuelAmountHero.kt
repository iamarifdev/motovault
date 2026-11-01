package com.motovault.app.ui.fuel

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

import com.motovault.app.ui.*

@Composable
internal fun FuelAmountHero(
    mode: FuelEntryMode,
    displayText: String,
    amountActive: Boolean,
    priceActive: Boolean,
    priceText: String,
    secondaryText: String,
    onActivate: () -> Unit,
    onPrice: () -> Unit
) {
    val f = currentFmt
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (amountActive) BrandTintColor else SurfaceColor2)
            .border(2.dp, if (amountActive) BrandColor else BorderColor, RoundedCornerShape(16.dp))
            .clickable { onActivate() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (mode == FuelEntryMode.AMOUNT) appStringResource(Res.string.fuel_amount_question) else appStringResource(Res.string.fuel_litres_question),
                fontSize = 13.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mode == FuelEntryMode.AMOUNT) Text(appStringResource(Res.string.fuel_currency_symbol), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text(text = f.editableNumber(displayText), fontSize = 38.sp, fontWeight = FontWeight.Bold, color = if (displayText.isEmpty()) TextTertiary else TextPrimary)
                if (mode == FuelEntryMode.LITRES) Text(appStringResource(Res.string.fuel_liters), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = appStringResource(Res.string.fuel_approximate_value, secondaryText), fontSize = 14.sp, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (priceActive) BrandTintColor else SurfaceColor)
                        .border(1.5.dp, if (priceActive) BrandColor else BorderColor, RoundedCornerShape(999.dp))
                        .clickable { onPrice() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = priceText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    V3Icon(path = "feather_icon/fi_edit-2.svg", description = null, modifier = Modifier.size(12.dp), tint = TextSecondary)
                }
            }
        }
    }
}
