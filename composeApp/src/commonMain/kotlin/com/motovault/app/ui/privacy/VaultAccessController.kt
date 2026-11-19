package com.motovault.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.motovault.app.domain.privacy.NativeSystemAuth
import com.motovault.app.domain.privacy.VaultAccessMode
import com.motovault.app.domain.privacy.VaultAccessPolicy
import com.motovault.app.domain.privacy.VaultAuthAction
import com.motovault.app.domain.privacy.VaultPrivacySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

internal class VaultAccessController(
    val policy: VaultAccessPolicy,
    private val scope: CoroutineScope,
    private val textProvider: () -> VaultUiText,
    private val authMessagesProvider: () -> AuthMessages,
    private val onCloseSheet: () -> Unit
) {
    var mode by mutableStateOf(policy.mode)
        private set
    var selectedInspectionDocId by mutableStateOf(policy.selectedDocumentId)
        private set
    var authMessage by mutableStateOf<String?>(null)
        private set

    fun syncPolicy(privacy: VaultPrivacySettings) {
        policy.settingsCommitted(privacy)
        syncState()
    }

    fun tick() {
        policy.tick()
        if (policy.mode != mode) syncState()
    }

    fun onBackground() {
        policy.background()
        mode = policy.mode
        selectedInspectionDocId = null
    }

    fun onResume() {
        policy.active(true)
        mode = policy.mode
    }

    fun unlock() {
        val text = textProvider()
        val request = runCatching { policy.beginAuthentication(VaultAuthAction.UNLOCK) }.getOrNull()
        if (request == null) {
            authMessage = text.privacyStartFailed
            return
        }
        scope.launch {
            val result = NativeSystemAuth.authenticate(text.unlockTitle, text.unlockReason)
            val grant = policy.completeAuthentication(request, result.isSuccess)
            if (grant != null) {
                syncState()
                selectedInspectionDocId = null
                authMessage = null
            } else {
                authMessage = authMessagesProvider().forStatus(result.status)
            }
        }
    }

    fun exitInspection() {
        val text = textProvider()
        val request = runCatching { policy.beginAuthentication(VaultAuthAction.EXIT_INSPECTION) }.getOrNull()
        if (request == null) {
            policy.lock()
            syncState()
            selectedInspectionDocId = null
            return
        }
        scope.launch {
            val result = NativeSystemAuth.authenticate(text.exitTitle, text.exitReason)
            val grant = policy.completeAuthentication(request, result.isSuccess)
            if (grant != null) {
                syncState()
                selectedInspectionDocId = null
                authMessage = null
            } else {
                authMessage = authMessagesProvider().forStatus(result.status)
            }
        }
    }

    fun lockNow() {
        policy.lock()
        mode = policy.mode
        selectedInspectionDocId = null
        onCloseSheet()
    }

    fun inspectDocument(docId: String) {
        policy.inspect(docId)
        syncState()
        onCloseSheet()
    }

    private fun syncState() {
        mode = policy.mode
        selectedInspectionDocId = policy.selectedDocumentId
    }
}

@Composable
internal fun rememberVaultAccessController(
    state: VaultState,
    text: VaultUiText,
    authMessages: AuthMessages,
    onCloseSheet: () -> Unit
): VaultAccessController {
    val scope = rememberCoroutineScope()
    val latestText = rememberUpdatedState(text)
    val latestAuth = rememberUpdatedState(authMessages)
    val latestClose = rememberUpdatedState(onCloseSheet)
    val controller = remember {
        VaultAccessController(
            policy = VaultAccessPolicy(state.privacy) { Clock.System.now().toEpochMilliseconds() },
            scope = scope,
            textProvider = { latestText.value },
            authMessagesProvider = { latestAuth.value },
            onCloseSheet = { latestClose.value() }
        )
    }
    LaunchedEffect(state.privacy) { controller.syncPolicy(state.privacy) }
    LaunchedEffect(controller.mode, state.privacy.enabled) {
        if (!state.privacy.enabled || controller.mode == VaultAccessMode.LOCKED) return@LaunchedEffect
        while (isActive) {
            delay(1000L)
            controller.tick()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) controller.onBackground()
            else if (event == Lifecycle.Event.ON_RESUME) controller.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return controller
}
