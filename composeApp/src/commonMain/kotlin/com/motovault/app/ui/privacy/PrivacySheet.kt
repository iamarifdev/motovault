package com.motovault.app.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.privacy.*
import com.motovault.app.ui.*
import kotlinx.coroutines.launch


@Composable
fun PrivacySheet(
    state: VaultState,
    onUpdatePrivacy: (VaultPrivacySettings) -> Unit,
    onLockNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val f = currentFmt
    val palette = LocalVaultPalette.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }
    val privacy = state.privacy

    val isEn = state.language == AppLanguage.EN

    Sheet(
        title = if (isEn) "Privacy & App Lock" else "গোপনীয়তা ও সুরক্ষা",
        detent = SheetDetent.TALL,
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = if (isEn) "Done" else "সম্পন্ন",
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = onDismiss
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor2)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(path = "feather_icon/fi_shield.svg", description = null, modifier = Modifier.size(32.dp), tint = BrandColor)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (isEn) "Documents under your control" else "ডকুমেন্টস আপনার নিয়ন্ত্রণে", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(if (isEn) "Hand over your phone safely by showing only the selected document." else "কাউকে ফোন দেওয়ার আগে শুধু বেছে নেওয়া ডকুমেন্টটি প্রদর্শন করুন।", fontSize = 13.sp, color = TextSecondary)
                }
            }

            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.err.copy(alpha = 0.1f))
                        .border(1.dp, palette.err.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isEn) "Could not enable app lock" else "সুরক্ষা চালু করা যায়নি", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.err)
                        Text(error, fontSize = 13.sp, color = palette.err)
                    }
                }
            }

            successNotice?.let { notice ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.ok.copy(alpha = 0.1f))
                        .border(1.dp, palette.ok.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(notice, fontSize = 13.sp, color = palette.ok)
                }
            }

            // Main Control Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (privacy.enabled) (if (isEn) "App lock is ON" else "অ্যাপ লক চালু") else (if (isEn) "App lock is OFF" else "অ্যাপ লক বন্ধ"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "${f.n(state.documents.size.toLong())} ${if (isEn) "documents." else "টি কাগজ।" } ${if (state.documents.isNotEmpty()) (if (isEn) "Numbers and previews are hidden in list." else "তালিকায় নম্বর ও ছবি লুকানো থাকবে।") else (if (isEn) "No documents yet. Same protection applies to documents added later." else "এখনও কাগজ নেই। পরে যোগ করা কাগজেও একই সুরক্ষা থাকবে।")}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                if (privacy.enabled) {
                    Text(if (isEn) "Auto-lock after" else "অটো-লক সময়", fontSize = 13.sp, color = TextSecondary)
                    VaultSegmented(
                        options = listOf(
                            60 to (if (isEn) "1 min" else "১ মিনিট"),
                            300 to (if (isEn) "5 min" else "৫ মিনিট"),
                            900 to (if (isEn) "15 min" else "১৫ মিনিট")
                        ),
                        selected = privacy.timeoutSeconds,
                        onSelect = { seconds ->
                            onUpdatePrivacy(privacy.copy(timeoutSeconds = seconds))
                        }
                    )

                    Text(if (isEn) "Locks immediately when minimized or sent to background." else "অ্যাপ মিনিমাইজ বা ব্যাকগ্রাউন্ডে পাঠালে সাথে সাথে লক হবে।", fontSize = 12.sp, color = TextSecondary)

                    VaultButton(
                        text = if (isEn) "Lock now" else "এখন লক করুন",
                        kind = ButtonKind.SECONDARY,
                        full = true,
                        icon = "fi_lock.svg",
                        onClick = {
                            onDismiss()
                            onLockNow()
                        }
                    )

                    VaultButton(
                        text = if (isEn) "Turn OFF app lock with phone lock" else "ফোনের লক দিয়ে অ্যাপ লক বন্ধ করুন",
                        kind = ButtonKind.GHOST,
                        full = true,
                        enabled = !busy,
                        onClick = {
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val result = NativeSystemAuth.authenticate(
                                    if (isEn) "Turn OFF MotoVault lock" else "MotoVault লক বন্ধ করুন",
                                    if (isEn) "Verify phone lock to turn off protection" else "সুরক্ষা বন্ধ করতে ফোনের লক দিয়ে যাচাই করুন"
                                )
                                busy = false
                                if (result.isSuccess) {
                                    onUpdatePrivacy(privacy.copy(enabled = false))
                                    successNotice = if (isEn) "App lock turned off." else "অ্যাপ লক বন্ধ করা হয়েছে।"
                                } else {
                                    errorMessage = result.message ?: (if (isEn) "Verification failed." else "যাচাই সম্পন্ন করা যায়নি।")
                                }
                            }
                        }
                    )
                } else {
                    VaultButton(
                        text = if (isEn) "Turn ON with phone lock" else "ফোনের লক দিয়ে চালু করুন",
                        kind = ButtonKind.PRIMARY,
                        full = true,
                        icon = "fi_lock.svg",
                        enabled = !busy,
                        onClick = {
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val result = NativeSystemAuth.authenticate(
                                    if (isEn) "Turn ON MotoVault lock" else "MotoVault লক চালু করুন",
                                    if (isEn) "Verify phone lock to enable protection" else "সুরক্ষা চালু করতে ফোনের লক দিয়ে যাচাই করুন"
                                )
                                if (!result.isSuccess) {
                                    busy = false
                                    errorMessage = result.message ?: (if (isEn) "Verification cancelled or failed." else "যাচাই বাতিল বা ব্যর্থ হয়েছে।")
                                    return@launch
                                }
                                // Run asset protection / verification
                                try {
                                    val referenced = state.documents.flatMap { it.assetIds() }.toSet()
                                    if (referenced.isNotEmpty()) {
                                        protectDocumentAssets(referenced)
                                    }
                                    onUpdatePrivacy(privacy.copy(enabled = true))
                                    successNotice = if (isEn) "App lock turned on. Locks on exit or timeout." else "অ্যাপ লক চালু হয়েছে। অ্যাপ থেকে বের হলে বা সময় শেষ হলে অটো-লক হবে।"
                                } catch (e: Exception) {
                                    errorMessage = if (isEn) "Could not enable protection: ${e.message ?: "Asset verification failed"}. Existing records preserved." else "সুরক্ষা চালু করা যায়নি: ${e.message ?: "ফাইল যাচাই ব্যর্থ"}. আগের রেকর্ড ও আসল কপি রাখা আছে।"
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    )
                }
            }

            // What is protected card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (isEn) "What is protected" else "কোন তথ্য সুরক্ষিত থাকবে", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    text = if (isEn) "Original files, cropped photos, and saved documents are encrypted with AES-256. Metadata like title, document number, and date may remain outside encryption." else "মূল ফাইল, ক্রপ করা ছবি ও সেভ করা ডকুমেন্টস AES-256 দিয়ে সুরক্ষিত থাকবে। নাম, কাগজের নম্বর ও তারিখ সুরক্ষার বাইরে থাকতে পারে। অ্যাপের সব তথ্য এনক্রিপ্ট হচ্ছে না।",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Text(
                    text = if (isEn) "Documents shared to other apps are outside this vault. Phone lock is required before exporting." else "অন্য অ্যাপে পাঠানো কপি সেই অ্যাপ রাখতে পারে। পাঠানোর আগে আবার ফোনের লক দিয়ে যাচাই করতে হবে।",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Lost key warning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.warn.copy(alpha = 0.08f))
                    .border(1.dp, palette.warn.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isEn) "If security key is lost" else "সিকিউরিটি কি (Security Key) হারিয়ে গেলে", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = if (isEn) "Encrypted files cannot be restored with screen lock if keys are cleared. Do not uninstall the app or clear app data. Files encrypted on this phone cannot be opened on another phone." else "ফোনের স্ক্রিন লক দিয়ে হারানো এনক্রিপশন কি (Key) ফেরানো যায় না। অ্যাপ আনইনস্টল বা ডেটা ক্লিয়ার করবেন না। ফাইল না খুললে আসল কপি থেকে আবার যোগ করার সাহায্য নিন। এই ফোনের এনক্রিপ্ট করা ফাইল অন্য ফোনে কপি করলেও খুলবে না।",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
