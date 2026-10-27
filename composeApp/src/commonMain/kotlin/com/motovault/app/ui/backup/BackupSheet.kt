package com.motovault.app.ui.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.backup.*
import com.motovault.app.storage.activeVaultSession
import com.motovault.app.storage.security.ProtectedDocumentStorage
import com.motovault.app.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class BackupSheetTab {
    CREATE,
    RESTORE
}

@Composable
fun BackupSheet(
    state: VaultState,
    initialTab: BackupSheetTab = BackupSheetTab.CREATE,
    onDismiss: () -> Unit,
    onBackupSaved: (Long) -> Unit,
    onRestored: (VaultState) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(initialTab) }

    // --- CREATE BACKUP STATE ---
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportStatusText by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }
    var generatedArchive by remember { mutableStateOf<ByteArray?>(null) }
    var createError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }

    // --- RESTORE BACKUP STATE ---
    var pickedFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pickedFileName by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var restorePasswordVisible by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var restorePreview by remember { mutableStateOf<BackupPreview?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreSuccess by remember { mutableStateOf(false) }

    Sheet(
        title = "ব্যাকআপ ও রিস্টোর",
        onDismiss = onDismiss,
        detent = SheetDetent.TALL
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Selector
            VaultSegmented(
                options = listOf(
                    BackupSheetTab.CREATE to "ব্যাকআপ তৈরি",
                    BackupSheetTab.RESTORE to "রিস্টোর"
                ),
                selected = activeTab,
                onSelect = {
                    haptic.perform(HapticKind.TICK)
                    activeTab = it
                }
            )

            // ==========================================
            // TAB 1: CREATE BACKUP
            // ==========================================
            if (activeTab == BackupSheetTab.CREATE) {
                // Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandTintColor)
                        .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V3Icon("feather_icon/fi_shield.svg", null, modifier = Modifier.size(18.dp), tint = BrandColor)
                            Text("নিরাপদ ও এনক্রিপ্টেড ব্যাকআপ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = "আপনার বাইকের রেকর্ড ও সমস্ত কাগজের ছবি একটি সুরক্ষিত ফাইলে পোর্টেবল আকারে সংরক্ষিত হবে। ফাইলটি গুগল ড্রাইভ, পেনড্রাইভ বা মেমোরিতে রাখতে পারবেন।",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Summary of data being backed up
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ব্যাকআপে অন্তর্ভুক্ত থাকবে:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("বাইক:", fontSize = 13.sp, color = TextPrimary)
                        Text(state.ride?.displayName ?: "বাইক নেই", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("বর্তমান ওডোমিটার:", fontSize = 13.sp, color = TextPrimary)
                        Text(f.km(state.ride?.odometer ?: 0.0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("সার্ভিস ও তেলের রেকর্ড:", fontSize = 13.sp, color = TextPrimary)
                        Text("${f.n(state.logs.size.toLong())} টি রেকর্ড", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("কাগজপত্রের ছবি ও ফাইল:", fontSize = 13.sp, color = TextPrimary)
                        Text("${f.n(state.documents.size.toLong())} টি ডকুমেন্ট", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                if (generatedArchive == null) {
                    // Password Inputs
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ব্যাকআপের পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = password,
                                onValueChange = { password = it; createError = null },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("পাসওয়ার্ড নিশ্চিত করুন", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceColor2)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            BasicTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; createError = null },
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (passwordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন",
                            fontSize = 12.sp,
                            color = BrandColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Security Warning Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningColor.copy(alpha = 0.1f))
                            .border(1.dp, WarningColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V3Icon("feather_icon/fi_alert-triangle.svg", null, modifier = Modifier.size(16.dp), tint = WarningColor)
                            Text(
                                text = "জরুরি সতর্কতা: এই পাসওয়ার্ডটি মনে রাখুন। পাসওয়ার্ড ভুলে গেলে ব্যাকআপ থেকে কোনো ডেটাই পুনরুদ্ধার করা সম্ভব হবে না।",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Error text if any
                    createError?.let {
                        Text(text = it, fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                    }

                    // Action Button or Progress
                    if (isExporting) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { exportProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = BrandColor
                            )
                            Text(text = exportStatusText, fontSize = 12.sp, color = TextSecondary)
                        }
                    } else {
                        VaultButton(
                            text = "ব্যাকআপ তৈরি করুন",
                            kind = ButtonKind.PRIMARY,
                            full = true,
                            onClick = {
                                if (password.length < 8) {
                                    createError = "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।"
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }
                                if (password != confirmPassword) {
                                    createError = "উভয় পাসওয়ার্ড এক হতে হবে।"
                                    haptic.perform(HapticKind.WARNING)
                                    return@VaultButton
                                }

                                createError = null
                                isExporting = true
                                scope.launch {
                                    try {
                                        val bytes = BackupUseCase.createBackup(
                                            state = state,
                                            vaultRevision = activeVaultSession.snapshots.value.revision,
                                            passphrase = password,
                                            assetReader = { id ->
                                                withContext(Dispatchers.IO) { ProtectedDocumentStorage.read(id) }
                                            },
                                            progress = { p, text ->
                                                exportProgress = p
                                                exportStatusText = text
                                            }
                                        )
                                        generatedArchive = bytes
                                        haptic.perform(HapticKind.SUCCESS)
                                    } catch (e: Exception) {
                                        createError = "ব্যাকআপ তৈরিতে সমস্যা হয়েছে: ${e.message}"
                                        haptic.perform(HapticKind.WARNING)
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // Archive Ready - Native Save / Share
                    val archive = generatedArchive!!
                    val filename = "motovault-backup-${today()}.mva"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HealthyColor.copy(alpha = 0.12f))
                            .border(1.dp, HealthyColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            V3Icon("feather_icon/fi_check-circle.svg", null, modifier = Modifier.size(20.dp), tint = HealthyColor)
                            Text("এনক্রিপ্টেড ব্যাকআপ প্রস্তুত!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = "ফাইলের আকার: ${(archive.size / 1024).coerceAtLeast(1)} KB। ফাইলটি সুরক্ষিত কোথাও সেভ করুন।",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    BackupNativeActions(
                        archiveBytes = archive,
                        suggestedFilename = filename,
                        onSaved = {
                            saveSuccess = true
                            haptic.perform(HapticKind.SUCCESS)
                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            onBackupSaved(now)
                        },
                        onShareOpened = {
                            saveSuccess = true
                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                            onBackupSaved(now)
                        },
                        onError = {
                            createError = it
                            haptic.perform(HapticKind.WARNING)
                        }
                    )

                    if (saveSuccess) {
                        Text(
                            text = "✓ ব্যাকআপ ফাইলটি সুরক্ষিতভাবে সংরক্ষণ করা হয়েছে।",
                            fontSize = 13.sp,
                            color = HealthyColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            // ==========================================
            // TAB 2: RESTORE BACKUP
            // ==========================================
            } else {
                // Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandTintColor)
                        .border(1.dp, BrandLineColor, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ব্যাকআপ ফাইল থেকে রিস্টোর", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = "পূর্বে তৈরি করা MotoVault ব্যাকআপ ফাইল (.mva) নির্বাচন করে আপনার বাইক ও কাগজের তথ্য ফিরিয়ে আনুন।",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // File Picker Card
                if (pickedFileBytes == null) {
                    BackupFilePicker(
                        onFileSelected = { bytes, name ->
                            pickedFileBytes = bytes
                            pickedFileName = name
                            restoreError = null
                            restorePreview = null
                            haptic.perform(HapticKind.TICK)
                        },
                        onError = {
                            restoreError = it
                            haptic.perform(HapticKind.WARNING)
                        }
                    ) { openPicker ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceColor)
                                .border(1.5.dp, BorderStrongColor, RoundedCornerShape(14.dp))
                                .clickable { openPicker() }
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                V3Icon("feather_icon/fi_upload-cloud.svg", null, modifier = Modifier.size(32.dp), tint = BrandColor)
                                Text("ব্যাকআপ ফাইল (.mva) নির্বাচন করুন", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("গুগল ড্রাইভ বা ফোন থেকে ফাইলটি বেছে নিন", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    // File Chosen Summary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceColor)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pickedFileName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${((pickedFileBytes?.size ?: 0) / 1024).coerceAtLeast(1)} KB", fontSize = 12.sp, color = TextSecondary)
                        }
                        BackupFilePicker(
                            onFileSelected = { bytes, name ->
                                pickedFileBytes = bytes
                                pickedFileName = name
                                restorePreview = null
                                restoreError = null
                            },
                            onError = { restoreError = it }
                        ) { openPicker ->
                            VaultButton(
                                text = "বদলান",
                                kind = ButtonKind.SECONDARY,
                                size = ButtonSize.SM,
                                onClick = openPicker
                            )
                        }
                    }

                    if (restorePreview == null) {
                        // Enter Password to Validate
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ব্যাকআপের পাসওয়ার্ড দিন", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceColor2)
                                    .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = restorePassword,
                                    onValueChange = { restorePassword = it; restoreError = null },
                                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                    cursorBrush = SolidColor(BrandColor),
                                    visualTransformation = if (restorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.clickable { restorePasswordVisible = !restorePasswordVisible },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (restorePasswordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন",
                                fontSize = 12.sp,
                                color = BrandColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (isValidating) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandColor)
                                Spacer(Modifier.width(10.dp))
                                Text("ব্যাকআপ যাচাই করা হচ্ছে...", fontSize = 13.sp, color = TextSecondary)
                            }
                        } else {
                            VaultButton(
                                text = "ব্যাকআপ যাচাই করুন",
                                kind = ButtonKind.PRIMARY,
                                full = true,
                                onClick = {
                                    if (restorePassword.isBlank()) {
                                        restoreError = "দয়া করে পাসওয়ার্ড প্রদান করুন।"
                                        haptic.perform(HapticKind.WARNING)
                                        return@VaultButton
                                    }
                                    isValidating = true
                                    restoreError = null
                                    scope.launch {
                                        try {
                                            val preview = RestoreUseCase.validateAndPreview(
                                                archiveBytes = pickedFileBytes!!,
                                                passphrase = restorePassword,
                                                currentState = state
                                            )
                                            restorePreview = preview
                                            haptic.perform(HapticKind.SUCCESS)
                                        } catch (e: BackupSecurityException) {
                                            restoreError = "ভুল পাসওয়ার্ড বা ফাইলটি অন্য কোনো সুরক্ষায় তৈরি।"
                                            haptic.perform(HapticKind.WARNING)
                                        } catch (e: Exception) {
                                            restoreError = "যাচাইকরণ ব্যর্থ: ${e.message}"
                                            haptic.perform(HapticKind.WARNING)
                                        } finally {
                                            isValidating = false
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        // ==========================================
                        // RESTORE PREVIEW & CONFLICT CONFIRMATION
                        // ==========================================
                        val preview = restorePreview!!
                        val backupDate = runCatching {
                            Instant.fromEpochMilliseconds(preview.manifest.createdAtEpochMillis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                        }.getOrDefault("")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceColor)
                                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("ব্যাকআপ ফাইলের বিবরণ:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("বাইক:", fontSize = 13.sp, color = TextPrimary)
                                Text(preview.manifest.summary.bikeDisplayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ওডোমিটার:", fontSize = 13.sp, color = TextPrimary)
                                Text(f.km(preview.manifest.summary.odometer), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("রেকর্ড সংখ্যা:", fontSize = 13.sp, color = TextPrimary)
                                Text("${f.n(preview.manifest.summary.logCount.toLong())} টি সার্ভিস / খরচ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("কাগজ সংখ্যা:", fontSize = 13.sp, color = TextPrimary)
                                Text("${f.n(preview.manifest.summary.documentCount.toLong())} টি (${preview.validatedAssetCount} ফাইল)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ব্যাকআপের তারিখ:", fontSize = 13.sp, color = TextPrimary)
                                Text(f.d(backupDate), fontSize = 13.sp, color = TextSecondary)
                            }
                        }

                        // Destructive Overwrite Warning Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ErrorColor.copy(alpha = 0.1f))
                                .border(1.dp, ErrorColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                V3Icon("feather_icon/fi_alert-triangle.svg", null, modifier = Modifier.size(18.dp), tint = ErrorColor)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("সতর্কতা: বর্তমান তথ্য প্রতিস্থাপিত হবে", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorColor)
                                    Text(
                                        text = "রিস্টোর সম্পন্ন হলে আপনার বর্তমান অ্যাপের বাইক, সার্ভিস রেকর্ড ও কাগজপত্র এই ব্যাকআপের ডেটা দিয়ে প্রতিস্থাপন করা হবে।",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Restore Action Buttons
                        if (isRestoring) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ErrorColor)
                                Spacer(Modifier.width(10.dp))
                                Text("ডাটাবেজ ও ফাইল রিস্টোর হচ্ছে...", fontSize = 13.sp, color = TextSecondary)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VaultButton(
                                    text = "বাতিল",
                                    kind = ButtonKind.SECONDARY,
                                    full = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        restorePreview = null
                                    }
                                )
                                VaultButton(
                                    text = "রিস্টোর নিশ্চিত করুন",
                                    kind = ButtonKind.PRIMARY,
                                    full = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        isRestoring = true
                                        restoreError = null
                                        scope.launch {
                                            try {
                                                // Execute transactional restore on database
                                                val db = activeVaultDatabase()
                                                RestoreUseCase.executeRestore(
                                                    archiveBytes = pickedFileBytes!!,
                                                    passphrase = restorePassword,
                                                    database = db,
                                                    assetWriter = { id, bytes ->
                                                        withContext(Dispatchers.IO) {
                                                            ProtectedDocumentStorage.writeNew(id, bytes)
                                                        }
                                                    },
                                                    onComplete = { nextState ->
                                                        restoreSuccess = true
                                                        haptic.perform(HapticKind.SUCCESS)
                                                        onRestored(nextState)
                                                    }
                                                )
                                            } catch (e: Exception) {
                                                restoreError = "রিস্টোর সম্পন্ন হতে পারেনি: ${e.message}"
                                                haptic.perform(HapticKind.WARNING)
                                            } finally {
                                                isRestoring = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Error Display for Restore
                restoreError?.let {
                    Text(text = it, fontSize = 13.sp, color = ErrorColor, fontWeight = FontWeight.Medium)
                }

                if (restoreSuccess) {
                    Text(
                        text = "✓ ডাটা সফলভাবে রিস্টোর সম্পন্ন হয়েছে!",
                        fontSize = 14.sp,
                        color = HealthyColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
