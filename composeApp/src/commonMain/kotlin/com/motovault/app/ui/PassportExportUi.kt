package com.motovault.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.garage.activeBikes
import com.motovault.app.domain.model.BaselineCertainty
import com.motovault.app.domain.privacy.NativeSystemAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.*

@Composable
expect fun PassportNativeActions(export: PassportExport, onError: (String) -> Unit)
@Composable
expect fun PassportVerificationImport(onRead: (String) -> Unit, onError: (String) -> Unit)

@Composable
fun PassportExportButton(state: VaultState) {
    var open by remember { mutableStateOf(false) }
    var verify by remember { mutableStateOf(false) }
    val exportBlock = passportV1BlockReason(state)
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), enabled = state.ride != null && exportBlock == null) { Text(appStringResource(Res.string.passport_open)) }
    exportBlock?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
    TextButton(onClick = { verify = true }) { Text(appStringResource(Res.string.passport_verify)) }
    Text(appStringResource(Res.string.passport_owner_data_note), color = TextSecondary, fontSize = 11.sp)
    if (open) PassportReview(state) { open = false }
    if (verify) PassportVerifier { verify = false }
}

@Composable
private fun PassportReview(state: VaultState, dismiss: () -> Unit) {
    var selectedBikeId by remember(state) { mutableStateOf(state.ride?.id) }
    val defaults = remember(state, selectedBikeId) { passportDefaults(state, selectedBikeId) }
    var identity by remember(defaults) { mutableStateOf(defaults) }
    var redaction by remember { mutableStateOf(PassportRedactionOptions(includeRegistration = false, includeChassisAndEngine = false)) }
    var confirmed by remember(state, selectedBikeId) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var exported by remember(state, selectedBikeId) { mutableStateOf<PassportExport?>(null) }
    var digest by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val latestState by rememberUpdatedState(state)
    val authMessages = rememberAuthMessages()
    val authTitle = appStringResource(Res.string.passport_auth_title)
    val authReason = appStringResource(Res.string.passport_auth_reason)
    val exportChanged = appStringResource(Res.string.passport_export_changed)
    val exportFailed = appStringResource(Res.string.passport_export_failed)
    val shareFailed = appStringResource(Res.string.passport_share_failed)
    fun edit(value: PassportIdentity) { identity = value; confirmed = false; exported = null }
    Sheet(appStringResource(Res.string.passport_review_title), dismiss) {
        Eyebrow(appStringResource(Res.string.passport_owner_guarantee))
        PassportHandoverControls(state, selectedBikeId, { selectedBikeId = it; confirmed = false; exported = null }, redaction) {
            redaction = it; confirmed = false; exported = null
        }
        PassportField(appStringResource(Res.string.passport_owner_name), identity.owner, !busy) { edit(identity.copy(owner = it.take(160))) }
        PassportField(appStringResource(Res.string.passport_bike_brand), identity.brand, !busy) { edit(identity.copy(brand = it.take(100))) }
        PassportField(appStringResource(Res.string.passport_bike_model), identity.model, !busy) { edit(identity.copy(model = it.take(160))) }
        PassportField(appStringResource(Res.string.passport_bike_edition), identity.edition, !busy) { edit(identity.copy(edition = it.take(160))) }
        if (redaction.includeRegistration) PassportField(appStringResource(Res.string.passport_registration_field), identity.registration, !busy) { edit(identity.copy(registration = it.take(160))) }
        if (redaction.includeChassisAndEngine) {
            PassportField(appStringResource(Res.string.passport_chassis_field), identity.chassis, !busy) { edit(identity.copy(chassis = it.take(160))) }
            PassportField(appStringResource(Res.string.passport_engine_field), identity.engine, !busy) { edit(identity.copy(engine = it.take(160))) }
        }
        Text(appStringResource(Res.string.passport_year_odo, identity.year.toString(), passportNumberValue(identity.odometerKmBits)), color = TextSecondary, fontSize = 12.sp)
        Text(appStringResource(Res.string.passport_brta_checklist), color = TextSecondary, fontSize = 11.sp)
        Row { Checkbox(confirmed, { confirmed = it }, enabled = !busy); Text(appStringResource(Res.string.passport_confirm_checkbox), fontSize = 12.sp, modifier = Modifier.weight(1f)) }
        Primary(if (busy) appStringResource(Res.string.passport_generating) else appStringResource(Res.string.passport_export_button), "check", enabled = confirmed && !busy && identity.owner.isNotBlank()) {
            busy = true; error = null
            val snapshot = state; val reviewed = identity; val approved = confirmed; val opts = redaction; val targetBike = selectedBikeId
            scope.launch {
                try {
                    if (snapshot.privacy.enabled) {
                        val auth = NativeSystemAuth.authenticate(authTitle, authReason)
                        if (!auth.isSuccess) { error = authMessages.forStatus(auth.status); busy = false; return@launch }
                    }
                    val bundle = ExportServicePassportUseCase().execute(snapshot, reviewed, approved, options = opts, bikeId = targetBike)
                    val result = renderServicePassport(bundle)
                    if (latestState == snapshot) { exported = result; digest = bundle.sha256 } else { discardPassportExport(result); error = exportChanged }
                } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { error = exportFailed } finally { busy = false }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        exported?.let { Text(appStringResource(Res.string.passport_created, digest), fontSize = 11.sp); PassportNativeActions(it) { error = shareFailed } }
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
    }
}

@Composable
private fun PassportHandoverControls(
    state: VaultState,
    selectedBikeId: String?,
    onSelectBike: (String) -> Unit,
    redaction: PassportRedactionOptions,
    onRedaction: (PassportRedactionOptions) -> Unit
) {
    val activeBikes = remember(state) { state.activeBikes() }
    val targetRecord = remember(state, selectedBikeId) {
        if (selectedBikeId == null || state.ride?.id == selectedBikeId) null else state.garageBikes.firstOrNull { it.ride.id == selectedBikeId }
    }
    val logs = if (targetRecord == null) state.logs else targetRecord.logs
    val rules = if (targetRecord == null) state.rules else targetRecord.rules
    val serviceCount = logs.count { it.isPaidService }
    val unknownCount = rules.count { it.baselineCertainty != BaselineCertainty.CONFIRMED }
    if (activeBikes.size > 1) {
        Text(appStringResource(Res.string.passport_select_bike), color = TextSecondary, fontSize = 12.sp)
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            activeBikes.forEach { bike -> VaultChip(text = bike.displayName, active = bike.id == selectedBikeId, onClick = { onSelectBike(bike.id) }) }
        }
    }
    Text(appStringResource(Res.string.passport_service_count, serviceCount.toString()), color = TextSecondary, fontSize = 13.sp)
    if (unknownCount > 0) Text(appStringResource(Res.string.passport_unknown_baselines, unknownCount.toString()), color = WarningColor, fontSize = 12.sp)
    Text(appStringResource(Res.string.passport_privacy_title), color = TextSecondary, fontSize = 12.sp)
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VaultChip(text = appStringResource(Res.string.passport_include_reg), active = redaction.includeRegistration, onClick = { onRedaction(redaction.copy(includeRegistration = !redaction.includeRegistration)) })
        VaultChip(text = appStringResource(Res.string.passport_include_vin), active = redaction.includeChassisAndEngine, onClick = { onRedaction(redaction.copy(includeChassisAndEngine = !redaction.includeChassisAndEngine)) })
        VaultChip(text = appStringResource(Res.string.passport_include_costs), active = redaction.includeCosts, onClick = { onRedaction(redaction.copy(includeCosts = !redaction.includeCosts)) })
        VaultChip(text = appStringResource(Res.string.passport_include_workshops), active = redaction.includeWorkshops, onClick = { onRedaction(redaction.copy(includeWorkshops = !redaction.includeWorkshops)) })
        VaultChip(text = appStringResource(Res.string.passport_include_notes), active = redaction.includeNotes, onClick = { onRedaction(redaction.copy(includeNotes = !redaction.includeNotes)) })
    }
    Text(appStringResource(Res.string.passport_registration_hint), color = TextSecondary, fontSize = 12.sp)
}

@Composable
private fun PassportField(label: String, value: String, enabled: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, change, enabled = enabled, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
}

@Composable
private fun PassportVerifier(dismiss: () -> Unit) {
    var json by remember { mutableStateOf<String?>(null) }
    var expected by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var verifiedBundle by remember { mutableStateOf<PassportBundle?>(null) }
    val signatureOk = appStringResource(Res.string.passport_signature_ok)
    val internalOk = appStringResource(Res.string.passport_internal_ok)
    val verifyFailed = appStringResource(Res.string.passport_verify_failed)
    val imported = appStringResource(Res.string.passport_imported)
    Sheet(appStringResource(Res.string.passport_verify_title), dismiss) {
        Text(appStringResource(Res.string.passport_verify_desc), color = TextSecondary, fontSize = 13.sp)
        PassportVerificationImport({ json = it; verifiedBundle = null; message = imported }, { verifiedBundle = null; message = verifyFailed })
        Field(appStringResource(Res.string.passport_hash_input), expected, { expected = it.take(100); verifiedBundle = null; message = null })
        Text(appStringResource(Res.string.passport_manual_hash_hint), color = TextSecondary, fontSize = 12.sp)
        Primary(appStringResource(Res.string.passport_verify_action), "check", enabled = json != null) {
            runCatching { verifyPassportBundle(json!!, expected) }.fold(
                { verifiedBundle = it.bundle; message = if (it.matchesExpected) signatureOk else internalOk },
                { verifiedBundle = null; message = verifyFailed })
        }
        message?.let { Text(it, fontSize = 12.sp) }
        verifiedBundle?.passport?.redactedFields?.takeIf { it.isNotEmpty() }?.let { redacted ->
            Text(appStringResource(Res.string.passport_redacted_notice, redacted.joinToString(", ")), fontSize = 12.sp, color = TextSecondary)
        }
        verifiedBundle?.passport?.unknownBaselineCount?.takeIf { it > 0 }?.let { count ->
            Text(appStringResource(Res.string.passport_unknown_baselines, count.toString()), fontSize = 12.sp, color = WarningColor)
        }
    }
}
