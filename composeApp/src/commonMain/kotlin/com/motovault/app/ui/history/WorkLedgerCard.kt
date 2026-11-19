package com.motovault.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.ledger.Money
import com.motovault.app.domain.ledger.WorkLedger
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.BrandColor
import com.motovault.app.ui.HapticKind
import com.motovault.app.ui.SurfaceColor
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import com.motovault.app.ui.currentHaptic
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.work_ledger_card_subtitle_off
import motovault.composeapp.generated.resources.work_ledger_card_subtitle_on
import motovault.composeapp.generated.resources.work_ledger_card_title
import motovault.composeapp.generated.resources.work_ledger_open_action

@Composable
internal fun WorkLedgerCard(
    state: VaultState,
    onOpenWorkSheet: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val bikeId = state.ride?.id
    val summary = remember(state.logs, state.ledger, bikeId) {
        WorkLedger.reconcile(
            logs = state.logs,
            shifts = state.ledger.workShifts,
            bikeId = bikeId,
            allowancePoisha = null
        )
    }
    val subtitle = if (state.ledger.workModeEnabled) {
        appStringResource(
            Res.string.work_ledger_card_subtitle_on,
            f.n(summary.shiftCount.toLong()),
            f.money(Money.taka(summary.netAfterAllocatedExpensePoisha))
        )
    } else {
        appStringResource(Res.string.work_ledger_card_subtitle_off)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.TICK)
                onOpenWorkSheet()
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = appStringResource(Res.string.work_ledger_card_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = subtitle, fontSize = 13.sp, color = TextSecondary)
        Text(text = appStringResource(Res.string.work_ledger_open_action), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor, modifier = Modifier.padding(top = 4.dp))
    }
}
