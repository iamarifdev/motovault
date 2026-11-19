package com.motovault.app.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.privacy.VaultPrivacySettings
import com.motovault.app.ui.BorderColor
import com.motovault.app.ui.BrandColor
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.LocalVaultPalette
import com.motovault.app.ui.SurfaceColor
import com.motovault.app.ui.SurfaceColor2
import com.motovault.app.ui.TextPrimary
import com.motovault.app.ui.TextSecondary
import com.motovault.app.ui.V3Icon
import com.motovault.app.ui.VaultButton
import com.motovault.app.ui.VaultSegmented
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.currentFmt
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.privacy_auto_lock_15m
import motovault.composeapp.generated.resources.privacy_auto_lock_1m
import motovault.composeapp.generated.resources.privacy_auto_lock_5m
import motovault.composeapp.generated.resources.privacy_auto_lock_title
import motovault.composeapp.generated.resources.privacy_bg_lock_desc
import motovault.composeapp.generated.resources.privacy_disabled
import motovault.composeapp.generated.resources.privacy_docs_under_control
import motovault.composeapp.generated.resources.privacy_done
import motovault.composeapp.generated.resources.privacy_enabled
import motovault.composeapp.generated.resources.privacy_export_note
import motovault.composeapp.generated.resources.privacy_handover_desc
import motovault.composeapp.generated.resources.privacy_key_lost_desc
import motovault.composeapp.generated.resources.privacy_key_lost_title
import motovault.composeapp.generated.resources.privacy_lock_enable_failed
import motovault.composeapp.generated.resources.privacy_lock_now
import motovault.composeapp.generated.resources.privacy_numbers_hidden
import motovault.composeapp.generated.resources.privacy_numbers_visible
import motovault.composeapp.generated.resources.privacy_retry_action
import motovault.composeapp.generated.resources.privacy_scope_desc
import motovault.composeapp.generated.resources.privacy_scope_title
import motovault.composeapp.generated.resources.privacy_stop_action
import motovault.composeapp.generated.resources.privacy_summary
import motovault.composeapp.generated.resources.privacy_turn_off_with_lock
import motovault.composeapp.generated.resources.privacy_turn_on_with_lock

@Composable
internal fun PrivacyHeroBanner() {
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
}

@Composable
internal fun PrivacyErrorBanner(error: String) {
    val palette = LocalVaultPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.err.copy(alpha = 0.1f))
            .border(1.dp, palette.err.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(appStringResource(Res.string.privacy_lock_enable_failed), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = palette.err)
        Text(error, fontSize = 13.sp, color = palette.err)
    }
}

@Composable
internal fun PrivacySuccessBanner(notice: String) {
    val palette = LocalVaultPalette.current
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

@Composable
internal fun PrivacyStatusCard(
    state: VaultState,
    busy: Boolean,
    onUpdatePrivacy: (VaultPrivacySettings) -> Unit,
    onTurnOffWithLock: () -> Unit
) {
    val f = currentFmt
    val privacy = state.privacy
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
        val numbersStatus = appStringResource(
            if (state.documents.isNotEmpty()) Res.string.privacy_numbers_hidden else Res.string.privacy_numbers_visible
        )
        Text(
            text = appStringResource(Res.string.privacy_summary, f.n(state.documents.size.toLong()), numbersStatus),
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
                onSelect = { seconds -> onUpdatePrivacy(privacy.copy(timeoutSeconds = seconds)) }
            )
            Text(appStringResource(Res.string.privacy_bg_lock_desc), fontSize = 12.sp, color = TextSecondary)
            VaultButton(
                text = appStringResource(Res.string.privacy_turn_off_with_lock),
                kind = ButtonKind.GHOST,
                full = true,
                enabled = !busy,
                onClick = onTurnOffWithLock
            )
        }
    }
}

@Composable
internal fun PrivacyScopeAndKeyCards() {
    val palette = LocalVaultPalette.current
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
        Text(text = appStringResource(Res.string.privacy_scope_desc), fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
        Text(text = appStringResource(Res.string.privacy_export_note), fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.warn.copy(alpha = 0.08f))
            .border(1.dp, palette.warn.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(appStringResource(Res.string.privacy_key_lost_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(text = appStringResource(Res.string.privacy_key_lost_desc), fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}

@Composable
internal fun PrivacyFooterBar(
    step: PrivacyViewStep,
    privacyEnabled: Boolean,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onEnable: () -> Unit,
    onLockNow: () -> Unit,
    onDone: () -> Unit
) {
    when (step) {
        PrivacyViewStep.WORKING -> {
            VaultButton(text = appStringResource(Res.string.privacy_stop_action), kind = ButtonKind.SECONDARY, full = true, onClick = onStop)
        }
        PrivacyViewStep.FAILED -> {
            VaultButton(text = appStringResource(Res.string.privacy_retry_action), kind = ButtonKind.PRIMARY, full = true, onClick = onRetry)
        }
        PrivacyViewStep.REVIEW, PrivacyViewStep.SAVED -> {
            if (privacyEnabled) {
                VaultButton(text = appStringResource(Res.string.privacy_lock_now), kind = ButtonKind.PRIMARY, full = true, icon = "fi_lock.svg", onClick = onLockNow)
            } else {
                VaultButton(text = appStringResource(Res.string.privacy_turn_on_with_lock), kind = ButtonKind.PRIMARY, full = true, icon = "fi_lock.svg", onClick = onEnable)
            }
            VaultButton(text = appStringResource(Res.string.privacy_done), kind = ButtonKind.SECONDARY, full = true, onClick = onDone)
        }
    }
}
