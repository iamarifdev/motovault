package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

@Composable
fun VaultScreen(
    state: VaultState,
    onOpenAddDoc: () -> Unit,
    onOpenDocView: (VaultDocument) -> Unit,
    onToggleDocReminders: (Boolean) -> Unit,
    onReminderSettings: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {}
) {
    val f = currentFmt
    val haptic = currentHaptic
    val sortedDocs = remember(state.documents) {
        state.documents.sortedBy { it.daysRemaining ?: Int.MAX_VALUE }
    }
    val nextRenewal = sortedDocs.firstOrNull { it.daysRemaining != null }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Page Header
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = appStringResource(Res.string.nav_vault), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            VaultIconButton(
                icon = "fi_plus.svg",
                label = appStringResource(Res.string.vault_add_document),
                tone = IconTone.TINT,
                onClick = onOpenAddDoc
            )
        }

        // Hero Next Renewal Card
        if (nextRenewal != null) {
            val palette = LocalVaultPalette.current
            val days = nextRenewal.daysRemaining ?: 0
            val (statusBg, statusBorder) = when {
                days < 0 -> palette.err.copy(alpha = 0.09f) to palette.err.copy(alpha = 0.4f)
                days <= 30 -> palette.warn.copy(alpha = 0.09f) to palette.warn.copy(alpha = 0.4f)
                else -> palette.ok.copy(alpha = 0.08f) to palette.ok.copy(alpha = 0.35f)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusBg)
                    .border(1.5.dp, statusBorder, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onOpenDocView(nextRenewal)
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = appStringResource(Res.string.vault_next_renewal), fontSize = 13.sp, color = TextSecondary)
                Text(text = nextRenewal.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val expDate = runCatching { LocalDate.parse(nextRenewal.expiry) }.getOrNull()
                    Text(
                        text = appStringResource(Res.string.vault_expiry_summary, if (expDate != null) f.dateFull(expDate) else nextRenewal.expiry),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    ExpiryChip(doc = nextRenewal, warningDays = nextRenewal.reminderWarningDays ?: state.reminderPolicy.warningDays)
                }
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderStrongColor, RoundedCornerShape(16.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                V3Icon(path = "feather_icon/fi_scan-line.svg", description = null, modifier = Modifier.size(36.dp), tint = TextSecondary)
                Text(
                    text = if (sortedDocs.isEmpty()) appStringResource(Res.string.vault_empty_add_first) else appStringResource(Res.string.vault_no_expiry_dates),
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                VaultButton(text = appStringResource(Res.string.vault_scan_document), icon = "fi_scan-line.svg", onClick = onOpenAddDoc)
            }
        }

        // Wallet Stack of All Documents
        if (sortedDocs.isNotEmpty()) {
            SectionTitle(appStringResource(Res.string.vault_all_documents))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sortedDocs.forEach { d ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.perform(HapticKind.TICK)
                                onOpenDocView(d)
                            }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = d.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            ExpiryChip(doc = d, warningDays = d.reminderWarningDays ?: state.reminderPolicy.warningDays)
                        }
                        if (state.privacy.enabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceColor2)
                                    .padding(horizontal = 14.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                V3Icon(path = "feather_icon/fi_scan-line.svg", description = null, modifier = Modifier.size(24.dp), tint = TextSecondary)
                                Text(appStringResource(Res.string.vault_hidden_hint), fontSize = 13.sp, color = TextSecondary)
                            }
                        } else {
                            DocArt(doc = d)
                        }
                    }
                }
            }
        }

        VaultSwitch(
            title = appStringResource(Res.string.vault_reminder_title),
            subtitle = appStringResource(Res.string.vault_reminder_desc, f.n(state.reminderPolicy.warningDays.toLong())),
            checked = state.docReminders,
            onCheckedChange = onToggleDocReminders
        )
        ReminderAccessPanel(rememberReminderAccess(), state)
        VaultButton(appStringResource(Res.string.vault_reminder_action), onClick = onReminderSettings, full = true, kind = ButtonKind.SECONDARY)

        if (sortedDocs.isNotEmpty()) {
            VaultButton(
                text = appStringResource(Res.string.vault_inspection_mode),
                kind = ButtonKind.GHOST,
                full = true,
                onClick = { onOpenDocView(sortedDocs.first()) }
            )
        }
    }
}

// ==========================================
// 3. HISTORY SCREEN
// ==========================================
