package com.motovault.app.ui.privacy

import motovault.composeapp.generated.resources.*
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
    val authMessages = rememberAuthMessages()
    val turnOffTitle = appStringResource(Res.string.privacy_turn_off_title)
    val turnOffReason = appStringResource(Res.string.privacy_turn_off_reason)
    val turnedOff = appStringResource(Res.string.privacy_turned_off)
    val verificationFailed = appStringResource(Res.string.privacy_verification_failed)
    val turnOnTitle = appStringResource(Res.string.privacy_turn_on_title)
    val turnOnReason = appStringResource(Res.string.privacy_turn_on_reason)
    val verificationCancelled = appStringResource(Res.string.privacy_verification_cancelled)
    val turnedOn = appStringResource(Res.string.privacy_turned_on)
    val assetFailed = appStringResource(Res.string.privacy_asset_verification_failed)
    val enableFailedTemplate = appStringResource(Res.string.privacy_enable_failed, "{message}")

    Sheet(
        title = appStringResource(Res.string.privacy_title),
        detent = SheetDetent.TALL,
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = appStringResource(Res.string.privacy_done),
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
                    Text(appStringResource(Res.string.privacy_docs_under_control), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(appStringResource(Res.string.privacy_handover_desc), fontSize = 13.sp, color = TextSecondary)
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
                        Text(appStringResource(Res.string.privacy_lock_enable_failed), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.err)
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
                    text = appStringResource(if (privacy.enabled) Res.string.privacy_enabled else Res.string.privacy_disabled),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = appStringResource(Res.string.privacy_summary, f.n(state.documents.size.toLong()), if (state.documents.isNotEmpty()) appStringResource(Res.string.privacy_numbers_hidden) else appStringResource(Res.string.privacy_numbers_visible)),
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                if (privacy.enabled) {
                    Text(appStringResource(Res.string.privacy_auto_lock_title), fontSize = 13.sp, color = TextSecondary)
                    VaultSegmented(
                        options = listOf(
                            60 to appStringResource(Res.string.privacy_auto_lock_1m),
                            300 to appStringResource(Res.string.privacy_auto_lock_5m),
                            900 to appStringResource(Res.string.privacy_auto_lock_15m)
                        ),
                        selected = privacy.timeoutSeconds,
                        onSelect = { seconds ->
                            onUpdatePrivacy(privacy.copy(timeoutSeconds = seconds))
                        }
                    )

                    Text(appStringResource(Res.string.privacy_bg_lock_desc), fontSize = 12.sp, color = TextSecondary)

                    VaultButton(
                        text = appStringResource(Res.string.privacy_lock_now),
                        kind = ButtonKind.SECONDARY,
                        full = true,
                        icon = "fi_lock.svg",
                        onClick = {
                            onDismiss()
                            onLockNow()
                        }
                    )

                    VaultButton(
                        text = appStringResource(Res.string.privacy_turn_off_with_lock),
                        kind = ButtonKind.GHOST,
                        full = true,
                        enabled = !busy,
                        onClick = {
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val result = NativeSystemAuth.authenticate(
                                    turnOffTitle,
                                    turnOffReason
                                )
                                busy = false
                                if (result.isSuccess) {
                                    onUpdatePrivacy(privacy.copy(enabled = false))
                                    successNotice = turnedOff
                                } else {
                                    errorMessage = authMessages.forStatus(result.status)
                                }
                            }
                        }
                    )
                } else {
                    VaultButton(
                        text = appStringResource(Res.string.privacy_turn_on_with_lock),
                        kind = ButtonKind.PRIMARY,
                        full = true,
                        icon = "fi_lock.svg",
                        enabled = !busy,
                        onClick = {
                            busy = true
                            errorMessage = null
                            scope.launch {
                                val result = NativeSystemAuth.authenticate(
                                    turnOnTitle,
                                    turnOnReason
                                )
                                if (!result.isSuccess) {
                                    busy = false
                                    errorMessage = authMessages.forStatus(result.status)
                                    return@launch
                                }
                                // Run asset protection / verification
                                try {
                                    val referenced = state.documents.flatMap { it.assetIds() }.toSet()
                                    if (referenced.isNotEmpty()) {
                                        protectDocumentAssets(referenced)
                                    }
                                    onUpdatePrivacy(privacy.copy(enabled = true))
                                    successNotice = turnedOn
                                } catch (e: Exception) {
                                    errorMessage = enableFailedTemplate.replace("{message}", e.message ?: assetFailed)
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
                Text(appStringResource(Res.string.privacy_scope_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    text = appStringResource(Res.string.privacy_scope_desc),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Text(
                    text = appStringResource(Res.string.privacy_export_note),
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
                    Text(appStringResource(Res.string.privacy_key_lost_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = appStringResource(Res.string.privacy_key_lost_desc),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
