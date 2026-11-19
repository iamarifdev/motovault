package com.motovault.app.ui.garage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.seasonal.NATIONAL_EMERGENCY_NUMBER
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.BrandColor
import com.motovault.app.ui.BrandTintColor
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.HapticKind
import com.motovault.app.ui.Sheet
import com.motovault.app.ui.SurfaceColor
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.V3Icon
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentHaptic
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.breakdown_999_desc
import motovault.composeapp.generated.resources.breakdown_999_title
import motovault.composeapp.generated.resources.breakdown_call_999_action
import motovault.composeapp.generated.resources.breakdown_open_docs
import motovault.composeapp.generated.resources.breakdown_open_handover
import motovault.composeapp.generated.resources.breakdown_sheet_title
import motovault.composeapp.generated.resources.breakdown_tip_docs_detail
import motovault.composeapp.generated.resources.breakdown_tip_docs_title
import motovault.composeapp.generated.resources.breakdown_tip_start_detail
import motovault.composeapp.generated.resources.breakdown_tip_start_title
import motovault.composeapp.generated.resources.breakdown_tip_tyre_detail
import motovault.composeapp.generated.resources.breakdown_tip_tyre_title
import org.jetbrains.compose.resources.StringResource

private data class BreakdownTip(val icon: String, val titleRes: StringResource, val detailRes: StringResource)

private val BREAKDOWN_TIPS = listOf(
    BreakdownTip("fi_tool.svg", Res.string.breakdown_tip_start_title, Res.string.breakdown_tip_start_detail),
    BreakdownTip("fi_shield-alert.svg", Res.string.breakdown_tip_tyre_title, Res.string.breakdown_tip_tyre_detail),
    BreakdownTip("fi_file-text.svg", Res.string.breakdown_tip_docs_title, Res.string.breakdown_tip_docs_detail)
)

@Composable
internal fun BreakdownHelpSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onOpenDoc: (String) -> Unit,
    onOpenVaultTab: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val haptic = currentHaptic
    val firstDoc = state.documents.firstOrNull()

    Sheet(title = appStringResource(Res.string.breakdown_sheet_title), onDismiss = onDismiss) {
        val emergencyShape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(emergencyShape)
                .background(SurfaceColor)
                .border(1.dp, BorderColor, emergencyShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = appStringResource(Res.string.breakdown_999_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = appStringResource(Res.string.breakdown_999_desc), fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary)
            VaultButton(
                text = appStringResource(Res.string.breakdown_call_999_action),
                icon = "fi_phone-call.svg",
                full = true,
                onClick = {
                    haptic.perform(HapticKind.WARNING)
                    runCatching { uriHandler.openUri("tel:$NATIONAL_EMERGENCY_NUMBER") }
                }
            )
        }

        val tipsShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(tipsShape)
                .background(SurfaceColor)
                .border(1.dp, BorderColor, tipsShape)
        ) {
            BREAKDOWN_TIPS.forEachIndexed { index, tip ->
                if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(BrandTintColor),
                            contentAlignment = Alignment.Center
                        ) {
                            V3Icon(path = "feather_icon/${tip.icon}", description = null, modifier = Modifier.size(18.dp), tint = BrandColor)
                        }
                        Text(text = appStringResource(tip.titleRes), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(text = appStringResource(tip.detailRes), fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
                }
            }
        }

        VaultButton(
            text = appStringResource(if (firstDoc != null) Res.string.breakdown_open_handover else Res.string.breakdown_open_docs),
            kind = ButtonKind.SECONDARY,
            full = true,
            icon = "fi_file-text.svg",
            onClick = {
                if (firstDoc != null) onOpenDoc(firstDoc.id) else onOpenVaultTab()
            }
        )
    }
}
