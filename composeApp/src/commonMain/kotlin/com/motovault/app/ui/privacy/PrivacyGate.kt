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
    authMessage: String? = null,
    language: AppLanguage = AppLanguage.BN
) {
    val palette = LocalVaultPalette.current
    val isEn = language == AppLanguage.EN
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
                    text = if (isEn) "Selected document · Preview" else "নির্বাচিত ডকুমেন্ট · প্রিভিউ",
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
                            contentDescription = if (isEn) "${selectedDoc.title} selected secure copy" else "${selectedDoc.title} এর নির্বাচিত সুরক্ষিত কপি"
                        }
                    ) {
                        DocArt(doc = selectedDoc, large = true)
                    }
                }

                Text(
                    text = if (isEn) "Other documents, edit and share options are hidden in this mode." else "এই মোডে অন্য কোনো ডকুমেন্ট বা এডিট অপশন দেখা যাবে না।",
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
                    text = if (isEn) "Unlock with phone lock" else "ফোনের লক দিয়ে ফিরুন",
                    kind = ButtonKind.PRIMARY,
                    full = true,
                    onClick = onExitInspection
                )

                VaultButton(
                    text = if (isEn) "Lock and return to vault" else "লক করে ভল্টে ফিরুন",
                    kind = ButtonKind.SECONDARY,
                    full = true,
                    onClick = onLock
                )
            } else {
                V3Icon(
                    path = "feather_icon/fi_lock.svg",
                    description = if (isEn) "Lock" else "লক",
                    modifier = Modifier.size(48.dp),
                    tint = BrandColor
                )

                Text(
                    text = if (isEn) "MotoVault is locked" else "MotoVault লক করা আছে",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isEn) "Unlock with your phone screen lock or biometrics." else "লক খুলতে ফোনের স্ক্রিন লক বা ফিঙ্গারপ্রিন্ট দিন।",
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
                    text = if (isEn) "Unlock" else "আনলক করুন",
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
                        Text(if (isEn) "Cannot open files?" else "ফাইল খুলছে না?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(if (helpExpanded) "▲" else "▼", fontSize = 12.sp, color = TextSecondary)
                    }
                    if (helpExpanded) {
                        Text(
                            text = if (isEn) "Do not uninstall or clear app data. If security keys are cleared, encrypted files cannot be restored with screen lock." else "অ্যাপ আনইনস্টল বা অ্যাপ ডেটা ক্লিয়ার করবেন না। সিকিউরিটি কি হারিয়ে গেলে ফোনের স্ক্রিন লক দিয়ে এনক্রিপ্ট করা ফাইল রিকভার করা সম্ভব নয়।",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Text(
                    text = if (isEn) "Protected with device PIN, passcode or biometrics." else "ফোনের PIN, পাসকোড বা বায়োমেট্রিক দিয়ে সুরক্ষিত।",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
