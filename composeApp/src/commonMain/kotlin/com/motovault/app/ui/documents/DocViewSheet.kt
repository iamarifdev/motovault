package com.motovault.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.documents.RENEWAL_QR_VERIFICATION_URL
import com.motovault.app.domain.documents.RenewalChecklists
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.model.stableKind
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_side_back
import motovault.composeapp.generated.resources.doc_side_front
import motovault.composeapp.generated.resources.docview_delete
import motovault.composeapp.generated.resources.docview_edit_details
import motovault.composeapp.generated.resources.docview_enable_show
import motovault.composeapp.generated.resources.docview_renewal_reminder
import motovault.composeapp.generated.resources.docview_show_only
import motovault.composeapp.generated.resources.renewal_action
import motovault.composeapp.generated.resources.renewal_link_failed
import motovault.composeapp.generated.resources.renewal_verify
import motovault.composeapp.generated.resources.renewal_verify_note
import motovault.composeapp.generated.resources.vault_doc_no_expiry
import motovault.composeapp.generated.resources.vault_expiry_summary

@Composable
fun DocViewSheet(
    doc: VaultDocument,
    onDismiss: () -> Unit,
    onUpdateReminder: (Boolean) -> Unit,
    warningDays: Int = doc.reminderWarningDays ?: 30,
    privacyEnabled: Boolean = false,
    onInspect: (() -> Unit)? = null,
    onOpenPrivacy: (() -> Unit)? = null,
    onRenew: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    language: AppLanguage = AppLanguage.BN
) {
    val guide = RenewalChecklists.of(doc.stableKind())
    val uriHandler = LocalUriHandler.current
    var selectedPage by remember(doc.id) { mutableStateOf(0) }
    var verifyFailed by remember(doc.id) { mutableStateOf(false) }

    InspectionBrightness()

    Sheet(
        title = documentDisplayName(doc.kind.ifBlank { doc.title }),
        onDismiss = onDismiss,
        detent = SheetDetent.TALL
    ) {
        DocViewCard(doc, warningDays, selectedPage) { selectedPage = it }
        VaultButton(
            text = appStringResource(if (privacyEnabled) Res.string.docview_show_only else Res.string.docview_enable_show),
            kind = ButtonKind.SECONDARY,
            full = true,
            onClick = { if (privacyEnabled) onInspect?.invoke() else onOpenPrivacy?.invoke() }
        )
        if (doc.expiry.isNotBlank()) {
            VaultSwitch(
                title = appStringResource(Res.string.docview_renewal_reminder),
                checked = doc.reminder,
                onCheckedChange = onUpdateReminder
            )
        }
        VaultButton(appStringResource(Res.string.renewal_action), icon = "fi_refresh-cw.svg", full = true, onClick = onRenew)
        VaultButton(appStringResource(Res.string.docview_edit_details), kind = ButtonKind.SECONDARY, icon = "fi_edit-2.svg", full = true, onClick = onEdit)
        if (guide?.qrVerification == true) {
            DocViewQrSection(verifyFailed) {
                runCatching { uriHandler.openUri(RENEWAL_QR_VERIFICATION_URL) }.onFailure { verifyFailed = true }
            }
        }
        RenewalHistory(doc)
        DocViewDeleteLink(onDelete)
    }
}

@Composable
private fun DocViewCard(
    doc: VaultDocument,
    warningDays: Int,
    selectedPage: Int,
    onSelectPage: (Int) -> Unit
) {
    val f = currentFmt
    val pageCount = when {
        doc.pages.size > 1 -> doc.pages.size
        doc.frontPhoto != null && doc.backPhoto != null -> 2
        else -> 0
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor2)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DocArt(doc = doc, large = true, pageIndex = selectedPage)
        if (pageCount > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pageCount) { index ->
                    val label = doc.pages.getOrNull(index)?.side?.let { documentSideName(it) }
                        ?: appStringResource(if (index == 0) Res.string.doc_side_front else Res.string.doc_side_back)
                    VaultChip(text = label, active = selectedPage == index, onClick = { onSelectPage(index) })
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpiryChip(doc = doc, warningDays = warningDays)
            val expiryDate = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
            Text(
                text = if (doc.expiry.isBlank()) {
                    appStringResource(Res.string.vault_doc_no_expiry)
                } else {
                    appStringResource(Res.string.vault_expiry_summary, expiryDate?.let(f::dateFull) ?: doc.expiry)
                },
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DocViewQrSection(verifyFailed: Boolean, onVerify: () -> Unit) {
    VaultButton(
        text = appStringResource(Res.string.renewal_verify),
        kind = ButtonKind.SECONDARY,
        icon = "fi_external-link.svg",
        full = true,
        onClick = onVerify
    )
    Text(
        text = appStringResource(Res.string.renewal_verify_note),
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = TextSecondary
    )
    if (verifyFailed) {
        Text(
            text = appStringResource(Res.string.renewal_link_failed),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = WarningColor
        )
    }
}

@Composable
private fun DocViewDeleteLink(onDelete: () -> Unit) {
    val haptic = currentHaptic
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) {
                haptic.perform(HapticKind.WARNING)
                onDelete()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            V3Icon(path = "feather_icon/fi_trash-2.svg", description = null, modifier = Modifier.size(16.dp), tint = ErrorColor)
            Text(
                text = appStringResource(Res.string.docview_delete),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ErrorColor
            )
        }
    }
}
