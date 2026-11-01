package com.motovault.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.serialization.json.Json
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import motovault.composeapp.generated.resources.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.usecase.CalculateWearUseCase
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
expect fun AttachmentPicker(label: String, onSelected: (String) -> Unit, onError: (String) -> Unit)

@Composable
internal fun ScheduleDialog(state: VaultState, existing: ComponentRule?, onDismiss: () -> Unit, onSave: VaultSave) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var km by remember { mutableStateOf(existing?.intervalKm?.toInt()?.toString() ?: "0") }
    var days by remember { mutableStateOf(existing?.intervalDays?.toString() ?: "0") }
    var odometer by remember { mutableStateOf((existing?.lastServicedOdometerKm ?: state.ride!!.odometer).toInt().toString()) }
    var date by remember { mutableStateOf(existing?.lastServicedEpochDays?.let { LocalDate.fromEpochDays(it.toInt()).toString() } ?: today().toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val distance = km.toDoubleOrNull()
    val intervalDays = days.toIntOrNull()
    val odo = odometer.toDoubleOrNull()
    val day = runCatching { LocalDate.parse(date) }.getOrNull()
    val f = currentFmt
    val ownerSourceLabel = appStringResource(Res.string.schedule_source_owner)
    val ownerDefinedText = appStringResource(Res.string.schedule_owner_defined)
    val saveFailedText = appStringResource(Res.string.schedule_save_failed)
    val preview = existing?.copy(intervalKm = distance ?: existing.intervalKm, intervalDays = intervalDays ?: existing.intervalDays)
    val wear = preview?.let { WearCalculator.wearOf(it, state.ride!!.odometer, today().toEpochDays().toLong()) }
    val actionLabel = appStringResource(when (existing?.action ?: com.motovault.app.domain.maintenance.MaintenanceAction.REPLACE) {
        com.motovault.app.domain.maintenance.MaintenanceAction.INSPECT -> Res.string.action_inspect
        com.motovault.app.domain.maintenance.MaintenanceAction.CLEAN -> Res.string.action_clean
        com.motovault.app.domain.maintenance.MaintenanceAction.LUBRICATE -> Res.string.action_lubricate
        com.motovault.app.domain.maintenance.MaintenanceAction.ADJUST -> Res.string.action_adjust
        com.motovault.app.domain.maintenance.MaintenanceAction.REPLACE -> Res.string.action_replace
    })
    val nextDue = when {
        wear == null || existing == null -> null
        wear.status == WearStatus.UNKNOWN -> appStringResource(Res.string.schedule_unknown_baseline)
        else -> buildList {
            wear.remainingKm?.let { add(appStringResource(Res.string.schedule_distance_km).substringBefore(" (") + " " + f.km(it.coerceAtLeast(0.0))) }
            wear.remainingDays?.let { add(appStringResource(Res.string.schedule_time_days).substringBefore(" (") + " " + f.days(it.coerceAtLeast(0))) }
        }.joinToString(" · ").ifBlank { appStringResource(Res.string.schedule_known_baseline) }
    }
    Sheet(appStringResource(Res.string.schedule_details_title), onDismiss) {
        Eyebrow(actionLabel)
        Text(if (existing?.provenance?.isManualBacked == true) existing.provenance.displayReference else ownerDefinedText, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        existing?.provenance?.let { provenance ->
            if (provenance.isManualBacked) {
                val applicabilityLine = "${appStringResource(Res.string.schedule_applicability)}: ${provenance.applicability}"
                Text(applicabilityLine, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                val uri = LocalUriHandler.current
                TextButton(onClick = { provenance.url.takeIf { it.isNotBlank() }?.let(uri::openUri) }) {
                    Text(appStringResource(Res.string.schedule_manual_open), color = BrandColor)
                }
            }
        }
        nextDue?.let { due ->
            val dueLine = "${appStringResource(Res.string.schedule_next_due)}: $due"
            Text(dueLine, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
        }
        Text(
            text = if (existing?.baselineCertainty == com.motovault.app.domain.model.BaselineCertainty.CONFIRMED) {
                appStringResource(Res.string.schedule_known_baseline)
            } else appStringResource(Res.string.schedule_unknown_baseline),
            color = TextSecondary, fontSize = 12.sp
        )
        Field(appStringResource(Res.string.schedule_name), name, { name = it.take(40) })
        Field(appStringResource(Res.string.schedule_distance_km), km, { km = it }, true)
        Field(appStringResource(Res.string.schedule_time_days), days, { days = it }, true)
        Field(appStringResource(Res.string.onboarding_last_service_km), odometer, { odometer = it }, true)
        Field(appStringResource(Res.string.onboarding_last_service_date), date, { date = it })
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
        Primary(appStringResource(Res.string.schedule_edit_intervals), "check", enabled = distance != null && intervalDays != null && odo != null && day != null && name.isNotBlank()) {
            val adjusted = existing != null && (distance != existing.intervalKm || intervalDays != existing.intervalDays)
            val provenance = when {
                existing == null -> com.motovault.app.domain.maintenance.ScheduleProvenance(
                    source = com.motovault.app.domain.maintenance.ScheduleSource.OWNER,
                    label = ownerSourceLabel
                )
                adjusted && existing.provenance.isManualBacked -> existing.provenance.copy(
                    source = com.motovault.app.domain.maintenance.ScheduleSource.OWNER,
                    label = "Owner-adjusted: ${existing.provenance.label}"
                )
                else -> existing.provenance
            }
            val rule = ComponentRule(
                existing?.id ?: recordId(), "primary", name.trim(),
                if (provenance.isManualBacked) provenance.displayReference else ownerDefinedText,
                distance!!, intervalDays!!, odo!!, day!!.toEpochDays().toLong(),
                baselineCertainty = if (existing?.baselineCertainty == com.motovault.app.domain.model.BaselineCertainty.UNKNOWN) {
                    com.motovault.app.domain.model.BaselineCertainty.OWNER_ESTIMATE
                } else com.motovault.app.domain.model.BaselineCertainty.CONFIRMED,
                action = existing?.action ?: com.motovault.app.domain.maintenance.MaintenanceAction.REPLACE,
                provenance = provenance,
                phase = existing?.phase ?: com.motovault.app.domain.maintenance.SchedulePhase.RECURRING,
                oneTime = existing?.oneTime ?: false,
                catalogueId = existing?.catalogueId
            )
            runCatching { state.saveSchedule(rule) }.onSuccess {
                onSave(it) { saved -> if (saved) onDismiss() else error = saveFailedText }
            }.onFailure { error = it.message }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalResourceApi::class)
@Composable
internal fun Attachment(value: String, description: String) {
    val bitmap = remember(value) { runCatching { Base64.decode(value).decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, description, Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit)
    else Text("ছবিটি দেখানো যায়নি।", color = WarningColor, fontSize = 12.sp)
}
