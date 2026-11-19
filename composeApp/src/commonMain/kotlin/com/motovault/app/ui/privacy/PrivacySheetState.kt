package com.motovault.app.ui.privacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.motovault.app.domain.garage.allDocumentAssetIds
import com.motovault.app.domain.privacy.NativeSystemAuth
import com.motovault.app.domain.privacy.VaultPrivacySettings
import com.motovault.app.ui.AuthMessages
import com.motovault.app.ui.VaultState
import com.motovault.app.ui.appStringResource
import com.motovault.app.ui.protectDocumentAssets
import com.motovault.app.ui.rememberAuthMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.privacy_asset_verification_failed
import motovault.composeapp.generated.resources.privacy_enable_failed
import motovault.composeapp.generated.resources.privacy_turn_off_reason
import motovault.composeapp.generated.resources.privacy_turn_off_title
import motovault.composeapp.generated.resources.privacy_turn_on_reason
import motovault.composeapp.generated.resources.privacy_turn_on_title
import motovault.composeapp.generated.resources.privacy_turned_off
import motovault.composeapp.generated.resources.privacy_turned_on

internal enum class PrivacyViewStep {
    REVIEW,
    WORKING,
    FAILED,
    SAVED
}

internal data class PrivacyAuthStrings(
    val turnOffTitle: String,
    val turnOffReason: String,
    val turnedOff: String,
    val turnOnTitle: String,
    val turnOnReason: String,
    val turnedOn: String,
    val assetFailed: String,
    val enableFailedTemplate: String
)

@Composable
internal fun rememberPrivacyAuthStrings(): PrivacyAuthStrings = PrivacyAuthStrings(
    turnOffTitle = appStringResource(Res.string.privacy_turn_off_title),
    turnOffReason = appStringResource(Res.string.privacy_turn_off_reason),
    turnedOff = appStringResource(Res.string.privacy_turned_off),
    turnOnTitle = appStringResource(Res.string.privacy_turn_on_title),
    turnOnReason = appStringResource(Res.string.privacy_turn_on_reason),
    turnedOn = appStringResource(Res.string.privacy_turned_on),
    assetFailed = appStringResource(Res.string.privacy_asset_verification_failed),
    enableFailedTemplate = appStringResource(Res.string.privacy_enable_failed, "{message}")
)

@Stable
internal class PrivacySheetController(
    private val scope: CoroutineScope,
    private val authMessages: AuthMessages,
    private val strings: PrivacyAuthStrings
) {
    var step by mutableStateOf(PrivacyViewStep.REVIEW)
    var errorMessage by mutableStateOf<String?>(null)
    var successNotice by mutableStateOf<String?>(null)
    private var activeJob: Job? = null

    fun cancelWorking() {
        activeJob?.cancel()
        activeJob = null
        step = PrivacyViewStep.REVIEW
    }

    fun enableWithLock(state: VaultState, onUpdatePrivacy: (VaultPrivacySettings) -> Unit) {
        if (step == PrivacyViewStep.WORKING) return
        step = PrivacyViewStep.WORKING
        errorMessage = null
        successNotice = null
        activeJob = scope.launch {
            try {
                val result = NativeSystemAuth.authenticate(strings.turnOnTitle, strings.turnOnReason)
                if (!result.isSuccess) {
                    errorMessage = authMessages.forStatus(result.status)
                    step = PrivacyViewStep.FAILED
                    return@launch
                }
                val referenced = state.allDocumentAssetIds()
                if (referenced.isNotEmpty()) {
                    protectDocumentAssets(referenced)
                }
                onUpdatePrivacy(state.privacy.copy(enabled = true))
                successNotice = strings.turnedOn
                step = PrivacyViewStep.SAVED
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                errorMessage = strings.enableFailedTemplate.replace("{message}", e.message ?: strings.assetFailed)
                step = PrivacyViewStep.FAILED
            } finally {
                activeJob = null
            }
        }
    }

    fun disableWithLock(privacy: VaultPrivacySettings, onUpdatePrivacy: (VaultPrivacySettings) -> Unit) {
        if (step == PrivacyViewStep.WORKING) return
        step = PrivacyViewStep.WORKING
        errorMessage = null
        successNotice = null
        activeJob = scope.launch {
            try {
                val result = NativeSystemAuth.authenticate(strings.turnOffTitle, strings.turnOffReason)
                if (result.isSuccess) {
                    onUpdatePrivacy(privacy.copy(enabled = false))
                    successNotice = strings.turnedOff
                    step = PrivacyViewStep.REVIEW
                } else {
                    errorMessage = authMessages.forStatus(result.status)
                    step = PrivacyViewStep.REVIEW
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                activeJob = null
            }
        }
    }
}

@Composable
internal fun rememberPrivacySheetController(): PrivacySheetController {
    val scope = rememberCoroutineScope()
    val authMessages = rememberAuthMessages()
    val strings = rememberPrivacyAuthStrings()
    return remember(scope, authMessages, strings) {
        PrivacySheetController(scope, authMessages, strings)
    }
}
