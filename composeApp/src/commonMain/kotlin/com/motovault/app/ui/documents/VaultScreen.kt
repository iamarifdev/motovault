package com.motovault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VaultDocument
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.nav_vault
import motovault.composeapp.generated.resources.vault_add_document
import motovault.composeapp.generated.resources.vault_all_documents
import motovault.composeapp.generated.resources.vault_reminder_desc
import motovault.composeapp.generated.resources.vault_reminder_title

@Composable
fun VaultScreen(
    state: VaultState,
    onOpenAddDoc: () -> Unit,
    onOpenDocView: (VaultDocument) -> Unit,
    onToggleDocReminders: (Boolean) -> Unit
) {
    val f = currentFmt
    val sortedDocs = remember(state.documents) {
        state.documents.sortedBy { it.daysRemaining ?: Int.MAX_VALUE }
    }
    val nextRenewal = sortedDocs.firstOrNull { it.expiry.isNotBlank() && it.daysRemaining != null }
    val leadDays = state.reminderPolicy.warningDays

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = appStringResource(Res.string.nav_vault),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            VaultIconButton(
                icon = "fi_plus.svg",
                label = appStringResource(Res.string.vault_add_document),
                tone = IconTone.TINT,
                onClick = onOpenAddDoc
            )
        }

        if (nextRenewal != null) {
            NextRenewalCard(
                doc = nextRenewal,
                leadDays = leadDays,
                onClick = { onOpenDocView(nextRenewal) }
            )
        } else {
            VaultEmptyCard(
                hasDocsWithoutExpiry = sortedDocs.isNotEmpty(),
                onScan = onOpenAddDoc
            )
        }

        if (sortedDocs.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(appStringResource(Res.string.vault_all_documents))
                DocumentWalletStack(
                    docs = sortedDocs,
                    privacyEnabled = state.privacy.enabled,
                    leadDays = leadDays,
                    onOpenDoc = onOpenDocView
                )
            }
        }

        VaultSwitch(
            title = appStringResource(Res.string.vault_reminder_title),
            subtitle = appStringResource(Res.string.vault_reminder_desc, f.n(leadDays.toLong())),
            checked = state.docReminders,
            onCheckedChange = onToggleDocReminders
        )
    }
}
