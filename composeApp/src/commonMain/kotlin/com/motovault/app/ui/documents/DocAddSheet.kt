package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.sky.design.tokens.SkyTokens
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.FuelEntryRules
import com.motovault.app.domain.fuel.MAX_FUEL_LITRES
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.domain.fuel.FuelTimelineEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

@Composable
fun DocAddSheet(
    state: VaultState,
    onDismiss: () -> Unit,
    onSave: (VaultState, String, () -> Unit) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val today = today()
    var step by remember { mutableStateOf(1) } // 1: Type, 2: Scan, 3: Review, 4: Remind
    var selectedKind by remember { mutableStateOf("registration") }
    var number by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var scanFlash by remember { mutableStateOf(false) }

    Sheet(
        title = when (step) {
            1 -> "কোন কাগজ?"
            2 -> "কাগজ স্ক্যান"
            3 -> "তথ্য মিলিয়ে নিন"
            else -> "মনে করিয়ে দেব?"
        },
        onDismiss = onDismiss,
        detent = SheetDetent.TALL
    ) {
        when (step) {
            1 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DOCUMENT_KINDS.forEach { kindInfo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceColor2)
                                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    selectedKind = kindInfo.kind
                                    step = 2
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                V3Icon(
                                    path = "feather_icon/fi_file-text.svg",
                                    description = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = BrandColor
                                )
                                Column {
                                    Text(text = kindInfo.bn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(text = if (kindInfo.card) "সামনে ও পেছনের ছবি" else "পুরো পৃষ্ঠা", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                            V3Icon(
                                path = "feather_icon/fi_chevron-right.svg",
                                description = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextTertiary
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            number = if (selectedKind == "licence") "DL-1990-123456" else "ঢাকা মেট্রো-হ ১১-২২৩৪"
                            holder = "মো. রাফিদ হাসান"
                            expiry = LocalDate.fromEpochDays(today.toEpochDays() + 365).toString()
                            step = 3
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.language == AppLanguage.EN) "Type manually without scan" else "স্ক্যান ছাড়া নিজে লিখুন", color = BrandColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            2 -> {
                // Viewfinder Scanner
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (scanFlash) Color.White else Color(0xFF0D1210)),
                        contentAlignment = Alignment.Center
                    ) {
                        V3Icon(
                            path = "feather_icon/fi_camera.svg",
                            description = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.White.copy(alpha = 0.35f)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("কাগজ সমতল জায়গায় রাখুন", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(text = "ডেমো: আসল অ্যাপে এখানে লাইভ ক্যামেরা দেখাবে", fontSize = 13.sp, color = TextSecondary)
                    // Shutter button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, BrandColor, CircleShape)
                            .clickable {
                                haptic.perform(HapticKind.SUCCESS)
                                scanFlash = true
                                number = if (selectedKind == "licence") "DL-1990-123456" else "ঢাকা মেট্রো-হ ১১-২২৩৪"
                                holder = "মো. রাফিদ হাসান"
                                expiry = LocalDate.fromEpochDays(today.toEpochDays() + 730).toString()
                                step = 3
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(BrandColor))
                    }
                }
            }
            3 -> {
                // Review
                val reason = when {
                    number.isBlank() -> "কাগজের নম্বর লিখুন"
                    holder.isBlank() -> "মালিকের নাম লিখুন"
                    expiry.isBlank() -> "মেয়াদ শেষের তারিখ দিন"
                    !checked -> "আসল কাগজের সঙ্গে মিলিয়ে টিক দিন"
                    else -> null
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("স্ক্যান থেকে যা পাওয়া গেছে তা দেখুন। ভুল থাকলে বদলান।", color = TextSecondary, fontSize = 14.sp)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("কাগজের নম্বর", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp)).padding(14.dp)
                        ) {
                            BasicTextField(value = number, onValueChange = { number = it }, textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("মালিকের নাম", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp)).padding(14.dp)
                        ) {
                            BasicTextField(value = holder, onValueChange = { holder = it }, textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("মেয়াদ শেষের তারিখ", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceColor2)
                                .border(1.5.dp, WarningColor, RoundedCornerShape(12.dp)).padding(14.dp)
                        ) {
                            BasicTextField(value = expiry, onValueChange = { expiry = it }, textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 5).forEach { y ->
                            val targetDate = LocalDate.fromEpochDays(today.toEpochDays() + 365 * y).toString()
                            VaultChip(
                                text = "${f.d(y.toString())} বছর পর",
                                active = expiry == targetDate,
                                onClick = { expiry = targetDate }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { checked = !checked }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (checked) BrandColor else Color.Transparent)
                                .border(2.dp, if (checked) BrandColor else BorderStrongColor, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) Text("✓", color = OnBrandColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("আসল কাগজ দেখে তথ্য মিলিয়ে নিয়েছি", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    VaultButton(
                        text = reason ?: "কাগজপত্রে সেভ করুন",
                        full = true,
                        enabled = reason == null,
                        onClick = {
                            if (reason != null) haptic.perform(HapticKind.ERROR)
                            else step = 4
                        }
                    )
                }
            }
            4 -> {
                // Reminder prompt
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(BrandTintColor),
                        contentAlignment = Alignment.Center
                    ) {
                        V3Icon(path = "feather_icon/fi_bell.svg", description = null, modifier = Modifier.size(34.dp), tint = BrandColor)
                    }
                    Text(if (state.language == AppLanguage.EN) "Get reminder 30 days before expiry?" else "মেয়াদ শেষ হওয়ার ৩০ দিন আগে রিমাইন্ডার চান?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Text(if (state.language == AppLanguage.EN) "You will receive a notification at 9 AM with renewal instructions." else "সকাল ৯টায় একটি নোটিফিকেশন পাবেন। নবায়নের জন্য যথেষ্ট সময় থাকবে।", fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    VaultButton(
                        text = if (state.language == AppLanguage.EN) "Yes, remind me" else "হ্যাঁ, রিমাইন্ডার দিন",
                        full = true,
                        icon = "fi_bell.svg",
                        onClick = {
                            val newDoc = VaultDocument(
                                title = docNameBn(selectedKind),
                                number = number.trim(),
                                holder = holder.trim(),
                                expiry = expiry.trim(),
                                kind = selectedKind,
                                reminder = true
                            )
                            val next = state.copy(documents = listOf(newDoc) + state.documents.filterNot { it.kind == selectedKind })
                            val toastMsg = "${docNameBn(selectedKind)} সেভ হয়েছে · ফোনের অনুমতি থাকলে জানানো হবে"
                            onSave(next, toastMsg) {}
                        }
                    )
                    VaultButton(
                        text = "এখন নয়",
                        kind = ButtonKind.GHOST,
                        full = true,
                        onClick = {
                            val newDoc = VaultDocument(
                                title = docNameBn(selectedKind),
                                number = number.trim(),
                                holder = holder.trim(),
                                expiry = expiry.trim(),
                                kind = selectedKind,
                                reminder = false
                            )
                            val next = state.copy(documents = listOf(newDoc) + state.documents.filterNot { it.kind == selectedKind })
                            val toastMsg = "${docNameBn(selectedKind)} সেভ হয়েছে"
                            onSave(next, toastMsg) {}
                        }
                    )
                }
            }
        }
    }
}
