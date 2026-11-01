package com.motovault.app.ui

import motovault.composeapp.generated.resources.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
    val defaults = remember(state) { passportDefaults(state) }
    var identity by remember(defaults) { mutableStateOf(defaults) }
    var confirmed by remember(state) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var exported by remember(state) { mutableStateOf<PassportExport?>(null) }
    var digest by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val latestState by rememberUpdatedState(state)
    val authMessages = rememberAuthMessages()
    val authTitle = appStringResource(Res.string.passport_auth_title)
    val authReason = appStringResource(Res.string.passport_auth_reason)
    val authRequired = appStringResource(Res.string.passport_auth_required)
    val exportChanged = appStringResource(Res.string.passport_export_changed)
    val exportFailed = appStringResource(Res.string.passport_export_failed)
    fun edit(value: PassportIdentity) { identity = value; confirmed = false; exported = null }
    Sheet(appStringResource(Res.string.passport_review_title), dismiss) {
        Eyebrow(appStringResource(Res.string.passport_owner_guarantee))
        Text(appStringResource(Res.string.passport_service_count, state.logs.count { !it.isFuel && !it.isDocumentFee }.toString()), color = TextSecondary, fontSize = 13.sp)
        Text(appStringResource(Res.string.passport_registration_hint), color = TextSecondary, fontSize = 12.sp)
        PassportField(appStringResource(Res.string.passport_owner_name), identity.owner, !busy) { edit(identity.copy(owner = it.take(160))) }
        PassportField(appStringResource(Res.string.passport_bike_brand), identity.brand, !busy) { edit(identity.copy(brand = it.take(100))) }
        PassportField(appStringResource(Res.string.passport_bike_model), identity.model, !busy) { edit(identity.copy(model = it.take(160))) }
        PassportField(appStringResource(Res.string.passport_bike_edition), identity.edition, !busy) { edit(identity.copy(edition = it.take(160))) }
        PassportField(appStringResource(Res.string.passport_registration_field), identity.registration, !busy) { edit(identity.copy(registration = it.take(160))) }
        PassportField(appStringResource(Res.string.passport_chassis_field), identity.chassis, !busy) { edit(identity.copy(chassis = it.take(160))) }
        PassportField(appStringResource(Res.string.passport_engine_field), identity.engine, !busy) { edit(identity.copy(engine = it.take(160))) }
        Text(appStringResource(Res.string.passport_year_odo, identity.year.toString(), passportNumberValue(identity.odometerKmBits)), color = TextSecondary, fontSize = 12.sp)
        Row { Checkbox(confirmed, { confirmed = it }, enabled = !busy); Text(appStringResource(Res.string.passport_confirm_checkbox), fontSize = 12.sp, modifier = Modifier.weight(1f)) }
        Primary(if (busy) appStringResource(Res.string.passport_generating) else appStringResource(Res.string.passport_export_button), "check", enabled = confirmed && !busy && identity.owner.isNotBlank()) {
            busy = true; error = null
            val snapshot = state
            val reviewed = identity
            val approved = confirmed
            scope.launch {
                try {
                    if (snapshot.privacy.enabled) {
                        val auth = com.motovault.app.domain.privacy.NativeSystemAuth.authenticate(authTitle, authReason)
                        if (!auth.isSuccess) {
                            error = authMessages.forStatus(auth.status)
                            busy = false
                            return@launch
                        }
                    }
                    val bundle = ExportServicePassportUseCase().execute(snapshot, reviewed, approved)
                    val result = renderServicePassport(bundle)
                    if (latestState == snapshot) { exported = result; digest = bundle.sha256 }
                    else { discardPassportExport(result); error = exportChanged }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { error = failure.message ?: exportFailed }
                finally { busy = false }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        exported?.let { Text(appStringResource(Res.string.passport_created, digest), fontSize = 11.sp); PassportNativeActions(it) { message -> error = message } }
        error?.let { Text(it, color = WarningColor, fontSize = 12.sp) }
    }
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
    val signatureOk = appStringResource(Res.string.passport_signature_ok)
    val internalOk = appStringResource(Res.string.passport_internal_ok)
    val verifyFailed = appStringResource(Res.string.passport_verify_failed)
    val imported = appStringResource(Res.string.passport_imported)
    Sheet(appStringResource(Res.string.passport_verify_title), dismiss) {
        Text(appStringResource(Res.string.passport_verify_desc), color = TextSecondary, fontSize = 13.sp)
        PassportVerificationImport({ json = it; message = imported }, { message = it })
        Field(appStringResource(Res.string.passport_hash_input), expected, { expected = it.take(100); message = null })
        Text(appStringResource(Res.string.passport_manual_hash_hint), color = TextSecondary, fontSize = 12.sp)
        Primary(appStringResource(Res.string.passport_verify_action), "check", enabled = json != null) {
            message = runCatching { verifyPassportBundle(json!!, expected) }.fold(
                { if (it.matchesExpected) signatureOk
                  else internalOk },
                { it.message ?: verifyFailed })
        }
        message?.let { Text(it, fontSize = 12.sp) }
    }
}
