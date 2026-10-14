package com.motovault.app.ui

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
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), enabled = state.ride != null && exportBlock == null) { Text("সার্ভিস পাসপোর্ট · Service passport") }
    exportBlock?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
    TextButton(onClick = { verify = true }) { Text("পাসপোর্ট যাচাই · Verify passport") }
    Text("রেকর্ডগুলো বাইক-মালিকের দেওয়া। যাচাই শুধু তথ্য ঠিক আছে কি না দেখে, সার্ভিসের সত্যতা নিশ্চিত করে না।", color = TextSecondary, fontSize = 11.sp)
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
    fun edit(value: PassportIdentity) { identity = value; confirmed = false; exported = null }
    Sheet("Review your service passport.", dismiss) {
        Eyebrow("Owner confirmation")
        Text("${state.logs.count { !it.isFuel && !it.isDocumentFee }} সার্ভিস, পুরোনো থেকে নতুন। নিশ্চিত করার আগে ইতিহাস দেখে নিন। জ্বালানি, নথির খরচ ও আসল ফাইল বাদ থাকবে · Services, oldest first. Fuel, document fees and originals are excluded.", color = TextSecondary, fontSize = 13.sp)
        Text("Confirmed registration details supply editable defaults only. Check they belong to this motorcycle.", color = TextSecondary, fontSize = 12.sp)
        PassportField("Owner name", identity.owner, !busy) { edit(identity.copy(owner = it.take(160))) }
        PassportField("Vehicle brand", identity.brand, !busy) { edit(identity.copy(brand = it.take(100))) }
        PassportField("Vehicle model", identity.model, !busy) { edit(identity.copy(model = it.take(160))) }
        PassportField("Edition", identity.edition, !busy) { edit(identity.copy(edition = it.take(160))) }
        PassportField("Registration (optional)", identity.registration, !busy) { edit(identity.copy(registration = it.take(160))) }
        PassportField("Chassis (optional)", identity.chassis, !busy) { edit(identity.copy(chassis = it.take(160))) }
        PassportField("Engine (optional)", identity.engine, !busy) { edit(identity.copy(engine = it.take(160))) }
        Text("Year: ${identity.year} / Current mileage: ${passportNumberValue(identity.odometerKmBits)} km", color = TextSecondary, fontSize = 12.sp)
        Row { Checkbox(confirmed, { confirmed = it }, enabled = !busy); Text("I reviewed this identity and saved service history. These are owner-entered records, not independently certified.", fontSize = 12.sp, modifier = Modifier.weight(1f)) }
        Primary(if (busy) "Preparing passport..." else "Confirm and export", "check", enabled = confirmed && !busy && identity.owner.isNotBlank()) {
            busy = true; error = null
            val snapshot = state
            val reviewed = identity
            val approved = confirmed
            scope.launch {
                try {
                    val bundle = ExportServicePassportUseCase().execute(snapshot, reviewed, approved)
                    val result = renderServicePassport(bundle)
                    if (latestState == snapshot) { exported = result; digest = bundle.sha256 }
                    else { discardPassportExport(result); error = "Your saved records changed while exporting. Review and confirm again." }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { error = failure.message ?: "Could not export this passport." }
                finally { busy = false }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        exported?.let { Text("Created PDF and verification JSON. SHA-256: $digest", fontSize = 11.sp); PassportNativeActions(it) { message -> error = message } }
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
    Sheet("Verify passport data.", dismiss) {
        Text("Import the paired JSON. This checks covered data, not the PDF pixels or whether a service happened.", color = TextSecondary, fontSize = 13.sp)
        PassportVerificationImport({ json = it; message = "File loaded. Run the check below." }, { message = it })
        Field("Expected SHA-256 or QR text (optional)", expected, { expected = it.take(100); message = null })
        Text("Paste the versioned QR text or a 64-character digest received separately. A digest bundled with changed data is not independent proof.", color = TextSecondary, fontSize = 12.sp)
        Primary("Check integrity", "check", enabled = json != null) {
            message = runCatching { verifyPassportBundle(json!!, expected) }.fold(
                { if (it.matchesExpected) "Covered data matches the independently supplied digest. Service occurrence is not certified."
                  else "Covered data matches its bundled digest. No independent expected digest was supplied." },
                { it.message ?: "Could not verify this file." })
        }
        message?.let { Text(it, fontSize = 12.sp) }
    }
}
