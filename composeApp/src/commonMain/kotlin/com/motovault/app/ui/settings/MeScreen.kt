package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource

@Composable
fun MeScreen(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onStartOnboarding: () -> Unit,
    onResetDemo: () -> Unit,
    onOpenSheet: (String, Any?) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val ride = state.ride ?: return
    val brand = worlds.firstOrNull { it.id == ride.brandId } ?: worlds.first()
    val model = ride.selectedModel
    val activeBrandId = state.brandOverride ?: ride.brandId
    var widgetOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(text = if (state.language == AppLanguage.EN) "Profile" else "প্রোফাইল", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

        // Profile card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd))),
                contentAlignment = Alignment.Center
            ) {
                if (model != null) {
                    Image(painter = painterResource(model.photo), contentDescription = null, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                } else {
                    V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(32.dp), tint = BrandColor)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = "${brand.name} ${ride.displayName}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "${f.d(ride.year.toString())} · ${f.km(ride.odometer)}", fontSize = 13.sp, color = TextSecondary)
            }
            VaultButton(
                text = "বাইক বদলান",
                size = ButtonSize.SM,
                kind = ButtonKind.SECONDARY,
                onClick = onStartOnboarding
            )
        }

        // Appearance section
        SectionTitle("চেহারা")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            // Theme
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("থিম", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(Appearance.SYSTEM to "ডিভাইসের মতো", Appearance.LIGHT to "লাইট", Appearance.DARK to "ডার্ক"),
                    selected = state.appearance,
                    onSelect = { onUpdateState(state.copy(appearance = it)) }
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Brand color swatches
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("রঙ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BRAND_COLOR_MAP.forEach { (bId, colors) ->
                        val isSelected = activeBrandId == bId
                        val swatchColor = Color(if (LocalVaultPalette.current.dark) colors.dark else colors.light)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    if (isSelected) 3.dp else 1.5.dp,
                                    if (isSelected) TextPrimary else BorderStrongColor,
                                    CircleShape
                                )
                                .clickable {
                                    haptic.perform(HapticKind.TICK)
                                    onUpdateState(state.copy(brandOverride = if (bId == ride.brandId) null else bId))
                                }
                        )
                    }
                }
                Text(
                    text = if (state.brandOverride != null) "${worlds.firstOrNull { it.id == state.brandOverride }?.name ?: ""} এর রঙ" else "আপনার বাইকের ব্র্যান্ডের রঙ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Language
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.language == AppLanguage.EN) "Language" else "ভাষা (Language)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(AppLanguage.BN to "বাংলা", AppLanguage.EN to "English"),
                    selected = state.language,
                    onSelect = { onUpdateState(state.copy(language = it)) }
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Digits
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.language == AppLanguage.EN) "Number style" else "সংখ্যার ধরন", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(Digits.BN to "১২৩ বাংলা", Digits.EN to "123 English"),
                    selected = state.digits,
                    onSelect = { onUpdateState(state.copy(digits = it)) }
                )
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

            // Text scale
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("লেখার আকার", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                VaultSegmented(
                    options = listOf(1.0f to "স্বাভাবিক", 1.15f to "বড়", 1.3f to "অনেক বড়"),
                    selected = state.textScale,
                    onSelect = { onUpdateState(state.copy(textScale = it)) }
                )
            }
        }

        // Behavior section
        SectionTitle(if (state.language == AppLanguage.EN) "Preferences" else "ব্যবহার ও সতর্কতা")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            VaultSwitch(
                title = if (state.language == AppLanguage.EN) "Touch vibration (Haptic)" else "টাচ ভাইব্রেশন (হ্যাপটিক)",
                subtitle = if (state.language == AppLanguage.EN) "Gentle vibration on button taps and actions" else "বাটন প্রেস ও সেভ করার সময় হালকা ভাইব্রেশন",
                checked = state.haptics,
                onCheckedChange = { onUpdateState(state.copy(haptics = it)) }
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            VaultSwitch(
                title = if (state.language == AppLanguage.EN) "Reduce motion" else "অ্যানিমেশন কমান",
                subtitle = if (state.language == AppLanguage.EN) "Minimize screen transitions and animations" else "স্ক্রিন ট্রানজিশন ও অ্যানিমেশনের নড়াচড়া কমান",
                checked = state.reduceMotion,
                onCheckedChange = { onUpdateState(state.copy(reduceMotion = it)) }
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            VaultSwitch(
                title = "তেল ও সার্ভিসে বর্তমান লোকেশন",
                subtitle = "অনুমতি না দিলেও রেকর্ড সেভ হবে",
                checked = state.autoLocation,
                onCheckedChange = { onUpdateState(state.copy(autoLocation = it)) }
            )
        }

        // Reminders section
        SectionTitle("রিমাইন্ডার")
        ReminderAccessPanel(rememberReminderAccess(), state)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            VaultSwitch(
                title = "সার্ভিসের সময় হলে জানান",
                subtitle = "চেইন, অয়েল ও অন্য যন্ত্রাংশ · ${f.d((state.reminderPolicy.minuteOfDay / 60).toString().padStart(2, '0'))}:${f.d((state.reminderPolicy.minuteOfDay % 60).toString().padStart(2, '0'))}",
                checked = state.serviceReminders,
                onCheckedChange = { onUpdateState(state.copy(serviceReminders = it)) }
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            VaultSwitch(
                title = "কাগজের মেয়াদের আগে জানান",
                subtitle = "${f.n(state.reminderPolicy.warningDays.toLong())} দিন আগে এবং মেয়াদের দিন। ফোনের অনুমতি প্রয়োজন।",
                checked = state.docReminders,
                onCheckedChange = { onUpdateState(state.copy(docReminders = it)) }
            )
        }
        VaultButton("অনুমতি, সময় ও পরে জানান", kind = ButtonKind.SECONDARY, full = true, onClick = { onOpenSheet("reminders", null) })

        // Privacy section
        SectionTitle("গোপনীয়তা")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (state.privacy.enabled) "অ্যাপ লক চালু" else "কাগজ আপনার নিয়ন্ত্রণে",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "ফোনের লক, নিজে থেকে লক ও শুধু বেছে নেওয়া কাগজ দেখানো।",
                fontSize = 13.sp,
                color = TextSecondary
            )
            VaultButton(
                text = "গোপনীয়তার সেটিংস",
                kind = ButtonKind.SECONDARY,
                full = true,
                onClick = { onOpenSheet("privacy", null) }
            )
        }

        // Backup section
        SectionTitle(if (state.language == AppLanguage.EN) "Backup & Restore" else "ব্যাকআপ ও রিস্টোর")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val lastBackupStr = state.lastBackupEpochMillis?.let { millis ->
                runCatching {
                    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
                    if (state.language == AppLanguage.EN) "Last backup: $date"
                    else "সর্বশেষ ব্যাকআপ: ${f.d(date.toString())}"
                }.getOrNull()
            } ?: if (state.language == AppLanguage.EN) "No backup created yet" else "এখনও কোনো ব্যাকআপ নেওয়া হয়নি"

            Text(
                text = if (state.language == AppLanguage.EN) "Encrypted Local Backup" else "এনক্রিপ্টেড লোকাল ব্যাকআপ",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = lastBackupStr,
                fontSize = 13.sp,
                color = if (state.lastBackupEpochMillis != null) BrandColor else TextSecondary
            )
            Text(
                text = if (state.language == AppLanguage.EN)
                    "Create a portable encrypted backup file or restore your garage from an existing backup."
                else
                    "বাইকের রেকর্ড ও কাগজের ফাইল এনক্রিপ্ট করে ব্যাকআপ রাখুন বা পূর্বের ব্যাকআপ ফাইল থেকে রিস্টোর করুন।",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VaultButton(
                    text = if (state.language == AppLanguage.EN) "Create Backup" else "ব্যাকআপ তৈরি",
                    kind = ButtonKind.SECONDARY,
                    full = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet("backup", com.motovault.app.ui.backup.BackupSheetTab.CREATE) }
                )
                VaultButton(
                    text = if (state.language == AppLanguage.EN) "Restore" else "রিস্টোর করুন",
                    kind = ButtonKind.SECONDARY,
                    full = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenSheet("backup", com.motovault.app.ui.backup.BackupSheetTab.RESTORE) }
                )
            }
            VaultButton(text = appStringResource(Res.string.recovery_entry), kind = ButtonKind.SECONDARY, full = true,
                onClick = { onOpenSheet("recovery", null) })
        }

        // New concepts (demo)
        SectionTitle("নতুন ধারণা (ডেমো)")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSheet("paste", null) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(path = "feather_icon/fi_share-2.svg", description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
                Column {
                    Text("শেয়ার বা পেস্ট করে যোগ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("বিকাশ, নগদ বা মেকানিকের মেসেজ থেকে খসড়া", fontSize = 13.sp, color = TextSecondary)
                }
            }
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        widgetOpen = !widgetOpen
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                V3Icon(path = "feather_icon/fi_grid.svg", description = null, modifier = Modifier.size(22.dp), tint = BrandColor)
                Column {
                    Text("হোম স্ক্রিন উইজেট", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("অ্যাপ না খুলেই পরের কাজ আর দুটি বোতাম", fontSize = 13.sp, color = TextSecondary)
                }
            }

            // Widget preview
            AnimatedVisibility(visible = widgetOpen) {
                Box(modifier = Modifier.padding(14.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandTintColor)
                            .border(1.dp, BrandLineColor, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = ride.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandColor)
                            Text(text = f.km(ride.odometer), fontSize = 12.sp, color = TextSecondary)
                        }
                        Text(text = "চেইন · এখনই", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceColor).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text("⛽ তেল", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceColor).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text("📏 কিমি", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // About & Reset
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MotoVault প্রোটোটাইপ · সব তথ্য এই ডিভাইসেই থাকে",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            VaultButton(
                text = "ডেমো ডেটা নতুন করে দিন",
                kind = ButtonKind.GHOST,
                full = true,
                onClick = onResetDemo
            )
        }
    }
}

// ==========================================
// 5. ONBOARDING (3 STEPS) SCREEN
// ==========================================
