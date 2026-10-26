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
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), enabled = state.ride != null && exportBlock == null) { Text("সার্ভিস পাসপোর্ট") }
    exportBlock?.let { Text(it, color = TextSecondary, fontSize = 11.sp) }
    TextButton(onClick = { verify = true }) { Text("পাসপোর্ট যাচাই") }
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
    Sheet("সার্ভিস পাসপোর্ট মিলিয়ে নিন।", dismiss) {
        Eyebrow("মালিকের নিশ্চয়তা")
        Text("${state.logs.count { !it.isFuel && !it.isDocumentFee }} সার্ভিস, পুরোনো থেকে নতুন। নিশ্চিত করার আগে ইতিহাস দেখে নিন। জ্বালানি, নথির খরচ ও আসল ফাইল বাদ থাকবে।", color = TextSecondary, fontSize = 13.sp)
        Text("নিশ্চিত করা রেজিস্ট্রেশনের তথ্য এখানে বসানো হয়েছে। এগুলো এই বাইকের কি না মিলিয়ে নিন। প্রয়োজনে বদলান।", color = TextSecondary, fontSize = 12.sp)
        PassportField("মালিকের নাম", identity.owner, !busy) { edit(identity.copy(owner = it.take(160))) }
        PassportField("বাইকের ব্র্যান্ড", identity.brand, !busy) { edit(identity.copy(brand = it.take(100))) }
        PassportField("বাইকের মডেল", identity.model, !busy) { edit(identity.copy(model = it.take(160))) }
        PassportField("ভ্যারিয়েন্ট / সংস্করণ (ঐচ্ছিক)", identity.edition, !busy) { edit(identity.copy(edition = it.take(160))) }
        PassportField("রেজিস্ট্রেশন (ঐচ্ছিক)", identity.registration, !busy) { edit(identity.copy(registration = it.take(160))) }
        PassportField("চেসিস (ঐচ্ছিক)", identity.chassis, !busy) { edit(identity.copy(chassis = it.take(160))) }
        PassportField("ইঞ্জিন (ঐচ্ছিক)", identity.engine, !busy) { edit(identity.copy(engine = it.take(160))) }
        Text("বছর: ${identity.year} / বর্তমান ওডোমিটার: ${passportNumberValue(identity.odometerKmBits)} কিমি", color = TextSecondary, fontSize = 12.sp)
        Row { Checkbox(confirmed, { confirmed = it }, enabled = !busy); Text("পরিচয় ও সংরক্ষিত সার্ভিসের ইতিহাস মিলিয়ে নিয়েছি। এই তথ্য মালিকের দেওয়া; আলাদাভাবে প্রত্যয়ন করা হয়নি।", fontSize = 12.sp, modifier = Modifier.weight(1f)) }
        Primary(if (busy) "পাসপোর্ট তৈরি হচ্ছে…" else "নিশ্চিত করে এক্সপোর্ট করুন", "check", enabled = confirmed && !busy && identity.owner.isNotBlank()) {
            busy = true; error = null
            val snapshot = state
            val reviewed = identity
            val approved = confirmed
            scope.launch {
                try {
                    if (snapshot.privacy.enabled) {
                        val auth = com.motovault.app.domain.privacy.NativeSystemAuth.authenticate("সার্ভিস পাসপোর্ট তৈরি করুন", "সংবেদনশীল তথ্য এক্সপোর্ট করতে ফোনের লক দিয়ে যাচাই করুন")
                        if (!auth.isSuccess) {
                            error = auth.message ?: "যাচাই ছাড়া পাসপোর্ট এক্সপোর্ট করা যাবে না।"
                            busy = false
                            return@launch
                        }
                    }
                    val bundle = ExportServicePassportUseCase().execute(snapshot, reviewed, approved)
                    val result = renderServicePassport(bundle)
                    if (latestState == snapshot) { exported = result; digest = bundle.sha256 }
                    else { discardPassportExport(result); error = "এক্সপোর্টের সময় রেকর্ড বদলেছে। আবার দেখে নিশ্চিত করুন।" }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { error = failure.message ?: "পাসপোর্ট এক্সপোর্ট করা যায়নি।" }
                finally { busy = false }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        exported?.let { Text("PDF ও যাচাইয়ের JSON তৈরি হয়েছে। SHA-256: $digest", fontSize = 11.sp); PassportNativeActions(it) { message -> error = message } }
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
    Sheet("পাসপোর্টের তথ্য যাচাই করুন।", dismiss) {
        Text("পাসপোর্টের সঙ্গে পাওয়া JSON যোগ করুন। এতে রাখা তথ্যের মিল যাচাই হয়। PDF-এর ছবি বা সার্ভিস সত্যি হয়েছে কি না যাচাই হয় না।", color = TextSecondary, fontSize = 13.sp)
        PassportVerificationImport({ json = it; message = "ফাইল যোগ হয়েছে। নিচের বাটনে যাচাই করুন।" }, { message = it })
        Field("ভেরিফিকেশন হ্যাশ কোড বা QR টেক্সট (ঐচ্ছিক)", expected, { expected = it.take(100); message = null })
        Text("আলাদাভাবে পাওয়া QR-এর লেখা বা ৬৪ অক্ষরের হ্যাশ দিন। বদলানো তথ্যের সঙ্গে দেওয়া হ্যাশ আলাদা প্রমাণ নয়।", color = TextSecondary, fontSize = 12.sp)
        Primary("তথ্যের মিল যাচাই করুন", "check", enabled = json != null) {
            message = runCatching { verifyPassportBundle(json!!, expected) }.fold(
                { if (it.matchesExpected) "পাসপোর্টের ডিজিটাল সিগনেচার সফলভাবে যাচাই হয়েছে। সার্ভিস রেকর্ড সুরক্ষিত রয়েছে।"
                  else "ফাইলের অভ্যন্তরীণ হ্যাশ অক্ষত রয়েছে। আলাদা ভেরিফিকেশন কোড দেওয়া হয়নি।" },
                { it.message ?: "ফাইলটি যাচাই করা যায়নি।" })
        }
        message?.let { Text(it, fontSize = 12.sp) }
    }
}
