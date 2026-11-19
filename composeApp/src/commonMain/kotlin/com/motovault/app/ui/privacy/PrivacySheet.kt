package com.motovault.app.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motovault.app.domain.privacy.VaultPrivacySettings
import com.motovault.app.ui.Sheet
import com.motovault.app.ui.SheetDetent
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.appStringResource
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.privacy_title

@Composable
fun PrivacySheet(
    state: VaultState,
    onUpdatePrivacy: (VaultPrivacySettings) -> Unit,
    onLockNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val controller = rememberPrivacySheetController()

    Sheet(
        title = appStringResource(Res.string.privacy_title),
        detent = SheetDetent.TALL,
        dismissEnabled = controller.step != PrivacyViewStep.WORKING,
        onDismiss = onDismiss,
        footer = {
            PrivacyFooterBar(
                step = controller.step,
                privacyEnabled = state.privacy.enabled,
                onStop = controller::cancelWorking,
                onRetry = { controller.enableWithLock(state, onUpdatePrivacy) },
                onEnable = { controller.enableWithLock(state, onUpdatePrivacy) },
                onLockNow = {
                    onDismiss()
                    onLockNow()
                },
                onDone = onDismiss
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PrivacyHeroBanner()
            controller.errorMessage?.let { PrivacyErrorBanner(it) }
            controller.successNotice?.let { PrivacySuccessBanner(it) }
            PrivacyStatusCard(
                state = state,
                busy = controller.step == PrivacyViewStep.WORKING,
                onUpdatePrivacy = onUpdatePrivacy,
                onTurnOffWithLock = { controller.disableWithLock(state.privacy, onUpdatePrivacy) }
            )
            PrivacyScopeAndKeyCards()
        }
    }
}
