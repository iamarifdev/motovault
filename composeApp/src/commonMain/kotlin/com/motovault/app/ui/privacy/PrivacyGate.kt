package com.motovault.app.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.domain.privacy.VaultAccessMode
import com.motovault.app.ui.AppLanguage
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.CanvasColor
import com.motovault.app.ui.DocArt
import com.motovault.app.ui.ErrorColor
import com.motovault.app.ui.LocalVaultPalette
import com.motovault.app.ui.SurfaceColor
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.V3Icon
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.documentDisplayName
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.privacy_cannot_open
import motovault.composeapp.generated.resources.privacy_gate_subtitle
import motovault.composeapp.generated.resources.privacy_gate_title
import motovault.composeapp.generated.resources.privacy_key_warning
import motovault.composeapp.generated.resources.privacy_lock_icon
import motovault.composeapp.generated.resources.privacy_lock_return
import motovault.composeapp.generated.resources.privacy_other_hidden
import motovault.composeapp.generated.resources.privacy_preview_badge
import motovault.composeapp.generated.resources.privacy_secure_copy
import motovault.composeapp.generated.resources.privacy_unlock_button
import motovault.composeapp.generated.resources.privacy_unlock_return

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
    val isInspection = mode == VaultAccessMode.INSPECTION && selectedDoc != null
    val bg = if (isInspection) Color.White else CanvasColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 36.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (isInspection && selectedDoc != null) {
                InspectionGateBody(selectedDoc, authMessage, onExitInspection, onLock)
            } else {
                LockedGateBody(authMessage, onUnlock)
            }
        }
    }
}

@Composable
private fun InspectionGateBody(
    doc: VaultDocument,
    authMessage: String?,
    onExitInspection: () -> Unit,
    onLock: () -> Unit
) {
    val inspectionInk = Color(0xFF18251E)
    val inspectionMuted = Color(0xFF3D5146)
    val inspectionBorder = Color(0xFF8D9E94)
    val title = documentDisplayName(doc.title)
    val secureCopyDescription = appStringResource(Res.string.privacy_secure_copy, doc.title)

    Text(text = appStringResource(Res.string.privacy_preview_badge), fontSize = 13.sp, lineHeight = 19.5.sp, color = inspectionMuted)
    Text(text = title, fontSize = 28.sp, lineHeight = 37.8.sp, fontWeight = FontWeight.SemiBold, color = inspectionInk)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, inspectionBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .semantics { contentDescription = secureCopyDescription }
    ) {
        DocArt(doc = doc, large = true)
    }
    Text(text = appStringResource(Res.string.privacy_other_hidden), fontSize = 16.sp, lineHeight = 24.8.sp, color = inspectionInk)
    if (!authMessage.isNullOrBlank()) {
        Text(text = authMessage, fontSize = 13.sp, color = ErrorColor)
    }
    VaultButton(text = appStringResource(Res.string.privacy_unlock_return), kind = ButtonKind.PRIMARY, full = true, onClick = onExitInspection)
    VaultButton(text = appStringResource(Res.string.privacy_lock_return), kind = ButtonKind.SECONDARY, full = true, onClick = onLock)
}

@Composable
private fun LockedGateBody(
    authMessage: String?,
    onUnlock: () -> Unit
) {
    val palette = LocalVaultPalette.current
    var helpExpanded by remember { mutableStateOf(false) }

    V3Icon(
        path = "feather_icon/fi_lock.svg",
        description = appStringResource(Res.string.privacy_lock_icon),
        modifier = Modifier.size(42.dp),
        tint = TextPrimary
    )
    Text(
        text = appStringResource(Res.string.privacy_gate_title),
        fontSize = 28.sp,
        lineHeight = 37.8.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
    Text(
        text = appStringResource(Res.string.privacy_gate_subtitle),
        fontSize = 16.sp,
        lineHeight = 24.8.sp,
        color = TextPrimary
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
            Text(text = authMessage, fontSize = 13.sp, color = palette.err)
        }
    }
    VaultButton(
        text = appStringResource(Res.string.privacy_unlock_button),
        kind = ButtonKind.PRIMARY,
        full = true,
        onClick = onUnlock
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) { helpExpanded = !helpExpanded }
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = appStringResource(Res.string.privacy_cannot_open),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            V3Icon(
                path = "feather_icon/fi_chevron-down.svg",
                description = null,
                modifier = Modifier.size(16.dp),
                tint = TextSecondary
            )
        }
        if (helpExpanded) {
            Text(
                text = appStringResource(Res.string.privacy_key_warning),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
    }
}
