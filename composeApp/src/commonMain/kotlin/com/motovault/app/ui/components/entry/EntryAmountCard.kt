package com.motovault.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryAmountCard(
    caption: String,
    valueText: String,
    isPlaceholder: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    amountDescription: String? = null,
    leadingUnit: String? = null,
    trailingUnit: String? = null,
    topTrailing: (@Composable () -> Unit)? = null,
    subContent: (@Composable RowScope.() -> Unit)? = null,
    message: String? = null,
    messageIsWarning: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) BrandTintColor else SurfaceColor2)
            .border(2.dp, if (active) BrandColor else BorderColor, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = caption, fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
            topTrailing?.invoke()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 46.dp)
                .clickable(
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .then(if (amountDescription != null) Modifier.semantics { contentDescription = amountDescription } else Modifier),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!leadingUnit.isNullOrEmpty()) {
                Text(
                    text = leadingUnit,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = valueText,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlaceholder) TextTertiary else TextPrimary
            )
            if (active) {
                BlinkingCaret()
            }
            if (!trailingUnit.isNullOrEmpty()) {
                Text(
                    text = trailingUnit,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        if (subContent != null) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                subContent()
            }
        }

        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = if (messageIsWarning) FontWeight.SemiBold else FontWeight.Normal,
                color = if (messageIsWarning) WarningColor else TextSecondary,
                modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

@Composable
private fun BlinkingCaret() {
    val reducedMotion = isReducedMotion
    val alpha = if (reducedMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "entryCaret")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
            label = "caretAlpha"
        )
        animated
    }
    Box(
        modifier = Modifier
            .padding(bottom = 2.dp)
            .width(3.dp)
            .height(44.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(2.dp))
            .background(BrandColor)
    )
}
