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
internal fun PrivacyGate(
    mode: VaultAccessMode,
    selectedDoc: VaultDocument?,
    onUnlock: () -> Unit,
    onExitInspection: () -> Unit,
    onLock: () -> Unit,
    authMessage: String? = null
) {
    val palette = LocalVaultPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasColor)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mode == VaultAccessMode.INSPECTION && selectedDoc != null) {
                Text(
                    text = "শুধু এই কাগজ · নির্বাচিত কপি",
                    fontSize = 13.sp,
                    color = BrandColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = selectedDoc.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, BorderStrongColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.semantics {
                            contentDescription = "${selectedDoc.title} এর নির্বাচিত সুরক্ষিত কপি"
                        }
                    ) {
                        DocArt(doc = selectedDoc, large = true)
                    }
                }

                Text(
                    text = "এই মোডে অন্য কাগজ, সম্পাদনা বা পাঠানোর বোতাম নেই।",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (!authMessage.isNullOrBlank()) {
                    Text(
                        text = authMessage,
                        fontSize = 13.sp,
                        color = palette.err,
                        textAlign = TextAlign.Center
                    )
                }

                VaultButton(
                    text = "ফোনের লক দিয়ে ফিরুন",
                    kind = ButtonKind.PRIMARY,
                    full = true,
                    onClick = onExitInspection
                )

                VaultButton(
                    text = "কাগজ লুকিয়ে লক করুন",
                    kind = ButtonKind.SECONDARY,
                    full = true,
                    onClick = onLock
                )
            } else {
                V3Icon(
                    path = "feather_icon/fi_lock.svg",
                    description = "লক",
                    modifier = Modifier.size(48.dp),
                    tint = BrandColor
                )

                Text(
                    text = "MotoVault লক করা আছে",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "ফোনের লক দিয়ে আপনার তথ্য খুলুন।",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (!authMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.err.copy(alpha = 0.1f))
                            .border(1.dp, palette.err.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = authMessage,
                            fontSize = 13.sp,
                            color = palette.err,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                VaultButton(
                    text = "ফোনের লক দিয়ে খুলুন",
                    kind = ButtonKind.PRIMARY,
                    full = true,
                    onClick = onUnlock
                )

                var helpExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { helpExpanded = !helpExpanded }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ফাইল খুলছে না?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(if (helpExpanded) "▲" else "▼", fontSize = 12.sp, color = TextSecondary)
                    }
                    if (helpExpanded) {
                        Text(
                            text = "অ্যাপ মুছবেন না বা ডেটা সাফ করবেন না। চাবি না পাওয়া গেলে আসল ফাইল থেকে আবার যোগ করার সাহায্য নিন। Phones PIN হারানো ফাইলের চাবি ফেরায় না।",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Text(
                    text = "ফোনের PIN, পাসকোড বা বায়োমেট্রিক দিয়ে সুরক্ষিত।",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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

    Sheet(
        title = "গোপনীয়তা",
        detent = SheetDetent.TALL,
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = "হয়েছে",
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
                    Text("কাগজ আপনার নিয়ন্ত্রণে", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("ফোন অন্য কাউকে দেওয়ার আগে শুধু বেছে নেওয়া কাগজটি দেখান।", fontSize = 13.sp, color = TextSecondary)
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
                        Text("সুরক্ষা চালু করা যায়নি", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.err)
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
                    text = if (privacy.enabled) "অ্যাপ লক চালু" else "অ্যাপ লক বন্ধ",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "${f.n(state.documents.size.toLong())}টি কাগজ। ${if (state.documents.isNotEmpty()) "তালিকায় নম্বর ও ছবি লুকানো থাকবে।" else "এখনও কাগজ নেই। পরে যোগ করা কাগজেও একই সুরক্ষা থাকবে।"}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                if (privacy.enabled) {
                    Text("নিজে থেকে লক হওয়ার সময়", fontSize = 13.sp, color = TextSecondary)
                    VaultSegmented(
                        options = listOf(
                            60 to "১ মিনিট",
                            300 to "৫ মিনিট",
                            900 to "১৫ মিনিট"
                        ),
                        selected = privacy.timeoutSeconds,
                        onSelect = { seconds ->
                            onUpdatePrivacy(privacy.copy(timeoutSeconds = seconds))
                        }
                    )

                    Text("অ্যাপ পেছনে গেলে সঙ্গে সঙ্গে লক হবে।", fontSize = 12.sp, color = TextSecondary)

                    VaultButton(
                        text = "এখন লক করুন",
                        kind = ButtonKind.SECONDARY,
                        full = true,
                        icon = "fi_lock.svg",
                        onClick = {
                            onDismiss()
                            onLockNow()
                        }
                    )

                    VaultButton(
                        text = "ফোনের লক দিয়ে অ্যাপ লক বন্ধ করুন",
                        kind = ButtonKind.GHOST,
                        full = true,
                        enabled = !busy,
                        onClick = {
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val result = NativeSystemAuth.authenticate("MotoVault লক বন্ধ করুন", "সুরক্ষা বন্ধ করতে ফোনের লক দিয়ে যাচাই করুন")
                                busy = false
                                if (result.isSuccess) {
                                    onUpdatePrivacy(privacy.copy(enabled = false))
                                    successNotice = "অ্যাপ লক বন্ধ করা হয়েছে।"
                                } else {
                                    errorMessage = result.message ?: "যাচাই সম্পন্ন করা যায়নি।"
                                }
                            }
                        }
                    )
                } else {
                    VaultButton(
                        text = "ফোনের লক দিয়ে চালু করুন",
                        kind = ButtonKind.PRIMARY,
                        full = true,
                        icon = "fi_lock.svg",
                        enabled = !busy,
                        onClick = {
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val result = NativeSystemAuth.authenticate("MotoVault লক চালু করুন", "সুরক্ষা চালু করতে ফোনের লক দিয়ে যাচাই করুন")
                                if (!result.isSuccess) {
                                    busy = false
                                    errorMessage = result.message ?: "যাচাই বাতিল বা ব্যর্থ হয়েছে।"
                                    return@launch
                                }
                                // Run asset protection / verification
                                try {
                                    val referenced = state.documents.flatMap { it.assetIds() }.toSet()
                                    if (referenced.isNotEmpty()) {
                                        protectDocumentAssets(referenced)
                                    }
                                    onUpdatePrivacy(privacy.copy(enabled = true))
                                    successNotice = "অ্যাপ লক চালু হয়েছে। অ্যাপ ছাড়লে বা সময় শেষ হলে নিজে থেকে লক হবে।"
                                } catch (e: Exception) {
                                    errorMessage = "সুরক্ষা চালু করা যায়নি: ${e.message ?: "ফাইল যাচাই ব্যর্থ"}. আগের রেকর্ড ও আসল কপি রাখা আছে।"
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
                Text("কোন তথ্য সুরক্ষিত হবে", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    text = "আসল ফাইল, কাটা ছবি, প্রিভিউ ও উদ্ধার করা ফাইল সুরক্ষিত হবে। নাম, কাগজের নম্বর, তারিখ এবং আগে রেকর্ডের ভেতরে রাখা ছবি এই সুরক্ষার বাইরে থাকতে পারে। অ্যাপের সব তথ্য এনক্রিপ্ট হচ্ছে না।",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Text(
                    text = "অন্য অ্যাপে পাঠানো কপি সেই অ্যাপ রাখতে পারে। পাঠানোর আগে আবার ফোনের লক দিয়ে যাচাই করতে হবে।",
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
                    Text("চাবি হারালে", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = "ফোনের PIN দিয়ে হারানো এনক্রিপশন চাবি ফেরানো যায় না। অ্যাপ মুছবেন না বা ডেটা সাফ করবেন না। ফাইল না খুললে আসল কপি থেকে আবার যোগ করার সাহায্য নিন। এই ফোনের এনক্রিপ্ট করা ফাইল অন্য ফোনে কপি করলেই খুলবে না।",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
