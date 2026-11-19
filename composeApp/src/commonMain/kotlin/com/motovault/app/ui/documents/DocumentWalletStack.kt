package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.motovault.app.domain.model.VaultDocument
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.vault_hidden_hint

private val WalletCardShape = RoundedCornerShape(20.dp)
private val WalletOverlap = 142.dp
private val MinHeaderExpose = 56.dp

@Composable
internal fun DocumentWalletStack(
    docs: List<VaultDocument>,
    privacyEnabled: Boolean,
    leadDays: Int,
    onOpenDoc: (VaultDocument) -> Unit
) {
    val haptic = currentHaptic
    Layout(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        content = {
            docs.forEachIndexed { index, doc ->
                WalletDocumentCard(
                    doc = doc,
                    privacyEnabled = privacyEnabled,
                    leadDays = leadDays,
                    modifier = Modifier.zIndex(index.toFloat()),
                    onClick = {
                        haptic.perform(HapticKind.TICK)
                        onOpenDoc(doc)
                    }
                )
            }
        }
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = constraints.maxWidth)
        val placeables = measurables.map { it.measure(childConstraints) }
        val overlapPx = WalletOverlap.roundToPx()
        val minExposePx = MinHeaderExpose.roundToPx()
        val yPositions = IntArray(placeables.size)
        var currentY = 0
        placeables.forEachIndexed { index, placeable ->
            yPositions[index] = currentY
            if (index < placeables.lastIndex) {
                val maxOverlap = (placeable.height - minExposePx).coerceAtLeast(0)
                val step = placeable.height - minOf(overlapPx, maxOverlap)
                currentY += step
            }
        }
        val totalHeight = placeables.indices.maxOfOrNull { i -> yPositions[i] + placeables[i].height } ?: 0
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(0, yPositions[index], zIndex = index.toFloat())
            }
        }
    }
}

@Composable
private fun WalletDocumentCard(
    doc: VaultDocument,
    privacyEnabled: Boolean,
    leadDays: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = WalletCardShape, clip = false)
            .clip(WalletCardShape)
            .background(SurfaceColor)
            .border(1.dp, BorderColor, WalletCardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = documentDisplayName(doc.kind.ifBlank { doc.title }),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            ExpiryChip(doc = doc, warningDays = doc.reminderWarningDays ?: leadDays)
        }
        if (privacyEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                V3Icon(
                    path = "feather_icon/fi_scan-line.svg",
                    description = null,
                    modifier = Modifier.size(28.dp),
                    tint = TextSecondary
                )
                Text(
                    text = appStringResource(Res.string.vault_hidden_hint),
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        } else {
            DocArt(doc = doc, inWallet = true)
        }
    }
}
