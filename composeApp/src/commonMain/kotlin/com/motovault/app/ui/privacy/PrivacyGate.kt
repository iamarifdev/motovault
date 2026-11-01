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
            val secureCopyDescription = selectedDoc?.let { appStringResource(Res.string.privacy_secure_copy, it.title) }.orEmpty()
            if (mode == VaultAccessMode.INSPECTION && selectedDoc != null) {
                Text(
                    text = appStringResource(Res.string.privacy_preview_badge),
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
                            contentDescription = secureCopyDescription
                        }
                    ) {
                        DocArt(doc = selectedDoc, large = true)
                    }
                }

                Text(
                    text = appStringResource(Res.string.privacy_other_hidden),
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
                    text = appStringResource(Res.string.privacy_unlock_return),
                    kind = ButtonKind.PRIMARY,
                    full = true,
                    onClick = onExitInspection
                )

                VaultButton(
                    text = appStringResource(Res.string.privacy_lock_return),
                    kind = ButtonKind.SECONDARY,
                    full = true,
                    onClick = onLock
                )
            } else {
                V3Icon(
                    path = "feather_icon/fi_lock.svg",
                    description = appStringResource(Res.string.privacy_lock_icon),
                    modifier = Modifier.size(48.dp),
                    tint = BrandColor
                )

                Text(
                    text = appStringResource(Res.string.privacy_gate_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = appStringResource(Res.string.privacy_gate_subtitle),
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
                    text = appStringResource(Res.string.privacy_unlock_button),
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
                        Text(appStringResource(Res.string.privacy_cannot_open), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(if (helpExpanded) "▲" else "▼", fontSize = 12.sp, color = TextSecondary)
                    }
                    if (helpExpanded) {
                        Text(
                            text = appStringResource(Res.string.privacy_key_warning),
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Text(
                    text = appStringResource(Res.string.privacy_protected_device),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
