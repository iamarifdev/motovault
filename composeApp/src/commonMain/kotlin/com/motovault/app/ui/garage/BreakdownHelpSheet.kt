package com.motovault.app.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.BreakdownContact
import com.motovault.app.domain.model.GeoCapture
import com.motovault.app.domain.seasonal.NATIONAL_EMERGENCY_NUMBER
import com.motovault.app.domain.seasonal.SeasonalCarePolicy
import com.motovault.app.ui.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*
import kotlin.math.roundToLong

@Composable
internal fun BreakdownHelpSheet(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onToast: (ToastData) -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val haptic = currentHaptic
    val bikeLabel = state.ride?.let { r -> r.modelName.ifBlank { r.edition }.ifBlank { r.brandId } } ?: ""
    var note by remember { mutableStateOf("") }
    var geo by remember { mutableStateOf<GeoCapture?>(null) }
    var captureKey by remember { mutableIntStateOf(0) }
    val copiedMsg = appStringResource(Res.string.breakdown_share_copied)
    val shareMessage = remember(bikeLabel, state.ride?.odometer, note, geo) {
        SeasonalCarePolicy.formatBreakdownShareText(bikeLabel, state.ride?.odometer, note, geo)
    }

    Sheet(
        title = appStringResource(Res.string.breakdown_sheet_title),
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = appStringResource(Res.string.breakdown_share_action),
                full = true,
                onClick = {
                    haptic.perform(HapticKind.SUCCESS)
                    clipboard.setText(AnnotatedString(shareMessage))
                    onToast(ToastData(id = "bd_${Clock.System.now().toEpochMilliseconds()}", message = copiedMsg))
                }
            )
        }
    ) {
        Text(text = appStringResource(Res.string.breakdown_sheet_subtitle), fontSize = 13.sp, color = TextSecondary)
        BreakdownBoundaryCard(onCall999 = { runCatching { uriHandler.openUri("tel:$NATIONAL_EMERGENCY_NUMBER") } })
        BreakdownContactsCard(
            state = state,
            onCallPhone = { phone -> runCatching { uriHandler.openUri("tel:${phone.trim()}") } },
            onSaveContact = { contact -> onUpdateState(state.copy(breakdownContacts = listOf(contact) + state.breakdownContacts)) }
        )
        BreakdownLocationCard(
            captureKey = captureKey,
            geo = geo,
            note = note,
            sharePreview = shareMessage,
            onNoteChange = { note = it },
            onCaptured = { captured -> geo = captured }
        )
    }
}

@Composable
private fun BreakdownBoundaryCard(onCall999: () -> Unit) {
    Panel(tint = WarningColor.copy(alpha = 0.10f)) {
        Text(
            text = appStringResource(Res.string.breakdown_boundary_notice),
            fontSize = 13.sp,
            color = TextPrimary
        )
        VaultChip(
            text = appStringResource(Res.string.breakdown_call_999_action),
            onClick = onCall999
        )
    }
}

@Composable
private fun BreakdownContactsCard(
    state: VaultState,
    onCallPhone: (String) -> Unit,
    onSaveContact: (BreakdownContact) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val allContacts = remember(state.breakdownContacts, state.workshopContacts) {
        val workshopDerived = state.workshopContacts
            .filter { it.phone.isNotBlank() }
            .map { BreakdownContact(id = "ws_${it.id}", name = it.name, phone = it.phone, roleCode = "workshop") }
        (state.breakdownContacts + workshopDerived).distinctBy { it.phone.trim() }
    }

    Panel {
        Text(text = appStringResource(Res.string.breakdown_contacts_title), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (allContacts.isEmpty()) {
            Text(text = appStringResource(Res.string.breakdown_no_contacts), fontSize = 12.sp, color = TextSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allContacts.forEach { contact ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = contact.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = contact.phone, fontSize = 12.sp, color = TextSecondary)
                        }
                        VaultButton(
                            text = appStringResource(Res.string.breakdown_call_action),
                            size = ButtonSize.SM,
                            kind = ButtonKind.SECONDARY,
                            onClick = { onCallPhone(contact.phone) }
                        )
                    }
                }
            }
        }
        Field(label = appStringResource(Res.string.breakdown_add_contact_name), value = name, onChange = { name = it })
        Field(label = appStringResource(Res.string.breakdown_add_contact_phone), value = phone, onChange = { phone = it })
        VaultButton(
            text = appStringResource(Res.string.breakdown_add_contact_save),
            size = ButtonSize.SM,
            kind = ButtonKind.SECONDARY,
            enabled = name.trim().isNotEmpty() && phone.trim().isNotEmpty(),
            onClick = {
                val id = "bd_${Clock.System.now().toEpochMilliseconds()}"
                onSaveContact(BreakdownContact(id = id, name = name.trim(), phone = phone.trim()))
                name = ""
                phone = ""
            }
        )
    }
}

@Composable
private fun BreakdownLocationCard(
    captureKey: Int,
    geo: GeoCapture?,
    note: String,
    sharePreview: String,
    onNoteChange: (String) -> Unit,
    onCaptured: (GeoCapture?) -> Unit
) {
    Panel {
        Text(text = appStringResource(Res.string.breakdown_location_title), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (geo != null && geo.isValid()) {
            val acc = geo.accuracyMeters.roundToLong().coerceAtLeast(0L).toString()
            val dt = Instant.fromEpochMilliseconds(geo.capturedAtEpochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
            val timeStr = listOf(dt.hour.toString().padStart(2, '0'), dt.minute.toString().padStart(2, '0')).joinToString(":")
            Text(
                text = appStringResource(Res.string.breakdown_location_attached, acc, timeStr),
                fontSize = 12.sp,
                color = LocalVaultPalette.current.ok
            )
        } else {
            Text(text = appStringResource(Res.string.breakdown_location_none), fontSize = 12.sp, color = TextSecondary)
        }
        LocationCaptureButton(
            requestKey = captureKey,
            enabled = true,
            onCaptured = onCaptured
        )
        Field(label = appStringResource(Res.string.breakdown_note_label), value = note, onChange = onNoteChange)
        Text(text = sharePreview, fontSize = 12.sp, color = TextSecondary)
    }
}
