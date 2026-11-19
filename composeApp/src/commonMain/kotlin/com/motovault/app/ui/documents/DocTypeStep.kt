package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_add_card_sides
import motovault.composeapp.generated.resources.doc_add_full_page
import motovault.composeapp.generated.resources.doc_add_skip_photo_start

@Composable
internal fun DocTypeStep(
    onPickKind: (DocumentKindInfo) -> Unit,
    onSkipPhoto: () -> Unit
) {
    val haptic = currentHaptic
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DOCUMENT_KINDS.forEach { info ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(SurfaceColor2)
                    .border(1.dp, BorderColor, shape)
                    .clickable(role = Role.Button) {
                        haptic.perform(HapticKind.TICK)
                        onPickKind(info)
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(
                    path = "feather_icon/fi_file-text.svg",
                    description = null,
                    modifier = Modifier.size(24.dp),
                    tint = BrandColor
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = appStringResource(info.resource),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = appStringResource(
                            if (info.card) Res.string.doc_add_card_sides else Res.string.doc_add_full_page
                        ),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                V3Icon(
                    path = "feather_icon/fi_chevron-right.svg",
                    description = null,
                    modifier = Modifier.size(20.dp),
                    tint = TextTertiary
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onSkipPhoto),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appStringResource(Res.string.doc_add_skip_photo_start),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandColor
            )
        }
    }
}
