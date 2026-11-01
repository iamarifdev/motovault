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
internal fun SettingsRemindersSection(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onOpenSheet: (String, Any?) -> Unit
) {
    val f = currentFmt
        // Reminders section
        SectionTitle(appStringResource(Res.string.settings_reminders))
        ReminderAccessPanel(rememberReminderAccess(), state)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            VaultSwitch(
                title = appStringResource(Res.string.settings_service_reminders_title),
                subtitle = appStringResource(Res.string.settings_service_reminders_desc, "${f.d((state.reminderPolicy.minuteOfDay / 60).toString().padStart(2, '0'))}:${f.d((state.reminderPolicy.minuteOfDay % 60).toString().padStart(2, '0'))}"),
                checked = state.serviceReminders,
                onCheckedChange = { onUpdateState(state.copy(serviceReminders = it)) }
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            VaultSwitch(
                title = appStringResource(Res.string.settings_doc_reminders_title),
                subtitle = appStringResource(Res.string.settings_doc_reminders_desc, f.n(state.reminderPolicy.warningDays.toLong())),
                checked = state.docReminders,
                onCheckedChange = { onUpdateState(state.copy(docReminders = it)) }
            )
        }
        VaultButton(appStringResource(Res.string.settings_reminder_settings), kind = ButtonKind.SECONDARY, full = true, onClick = { onOpenSheet("reminders", null) })

}

@Composable
internal fun SettingsPrivacySection(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onOpenSheet: (String, Any?) -> Unit
) {
        // Privacy section
        SectionTitle(appStringResource(Res.string.settings_privacy))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = appStringResource(if (state.privacy.enabled) Res.string.settings_privacy_on else Res.string.settings_privacy_off),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = appStringResource(Res.string.settings_privacy_desc),
                fontSize = 13.sp,
                color = TextSecondary
            )
            VaultButton(
                text = appStringResource(Res.string.settings_privacy_action),
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = { onOpenSheet("privacy", null) }
            )
        }

}

@Composable
internal fun SettingsBackupSection(
    state: VaultState,
    onOpenSheet: (String, Any?) -> Unit
) {
    val f = currentFmt
        // Backup section
        SectionTitle(appStringResource(Res.string.settings_backup_restore))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val lastBackupDate = state.lastBackupEpochMillis?.let { millis ->
                runCatching {
                    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                    f.d(date.toString())
                }.getOrNull()
            }
            val lastBackupStr = lastBackupDate?.let { appStringResource(Res.string.settings_backup_last, it) }
                ?: appStringResource(Res.string.settings_backup_none)

            Text(
                text = appStringResource(Res.string.settings_backup_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = lastBackupStr,
                fontSize = 13.sp,
                color = if (state.lastBackupEpochMillis != null) BrandColor else TextSecondary
            )
            Text(
                text = appStringResource(Res.string.settings_backup_desc),
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VaultButton(
                    text = appStringResource(Res.string.settings_backup_create),
                    kind = ButtonKind.SECONDARY,
                    full = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet("backup", com.motovault.app.ui.backup.BackupSheetTab.CREATE) }
                )
                VaultButton(
                    text = appStringResource(Res.string.settings_backup_restore_action),
                    kind = ButtonKind.SECONDARY,
                    full = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet("backup", com.motovault.app.ui.backup.BackupSheetTab.RESTORE) }
                )
            }
            VaultButton(text = appStringResource(Res.string.recovery_entry), kind = ButtonKind.SECONDARY, full = true,
                onClick = { onOpenSheet("recovery", null) })
        }

}

@Composable
internal fun SettingsDemoSection(
    ride: Ride,
    onOpenSheet: (String, Any?) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    var widgetOpen by remember { mutableStateOf(false) }
        // New concepts (demo)
        SectionTitle(appStringResource(Res.string.settings_demo))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSheet("paste", null) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(path = "feather_icon/fi_share-2.svg", description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
                Column {
                    Text(appStringResource(Res.string.settings_paste_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(appStringResource(Res.string.settings_paste_desc), fontSize = 13.sp, color = TextSecondary)
                }
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        widgetOpen = !widgetOpen
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(path = "feather_icon/fi_grid.svg", description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
                Column {
                    Text(appStringResource(Res.string.settings_widget_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(appStringResource(Res.string.settings_widget_desc), fontSize = 13.sp, color = TextSecondary)
                }
            }

            // Widget preview
            AnimatedVisibility(visible = widgetOpen) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandTintColor)
                            .border(1.dp, BrandLineColor, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ride.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandColor)
                            Text(text = f.km(ride.odometer), fontSize = 12.sp, color = TextSecondary)
                        }
                        Text(text = appStringResource(Res.string.settings_widget_due), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceColor).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text(appStringResource(Res.string.settings_widget_fuel), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceColor).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text(appStringResource(Res.string.settings_widget_km), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

}

@Composable
internal fun SettingsAboutSection(onResetDemo: () -> Unit) {
        // About & Reset
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = appStringResource(Res.string.settings_about),
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            VaultButton(
                text = appStringResource(Res.string.settings_reset_demo),
                kind = ButtonKind.GHOST,
                full = true,
                onClick = onResetDemo
            )
        }}
