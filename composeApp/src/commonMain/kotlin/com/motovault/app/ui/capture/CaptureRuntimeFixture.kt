package com.motovault.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motovault.app.application.capture.CaptureDraftFactory
import com.motovault.app.domain.capture.CaptureSource
import com.motovault.app.domain.model.*
import motovault.composeapp.generated.resources.*

/** Isolated debug hosts only. Synthetic state exercises the production review controls. */
@Composable
internal fun CaptureRuntimeFixture(factory: CaptureDraftFactory, photo: OdometerPhoto?) {
    var state by remember { mutableStateOf(VaultState(ride = Ride("yamaha", "Synthetic", 2026, 1000.0))) }
    var sheet by remember { mutableStateOf<String?>(null) }
    val odometerCapture = rememberOdometerCaptureState()
    LaunchedEffect(sheet) { if (sheet != "odometer") odometerCapture.close() }
    var prefill by remember { mutableStateOf<CapturePrefill?>(null) }
    var dark by remember { mutableStateOf(false) }
    AppResourceEnvironment(state.language) {
        VaultTheme("yamaha", if (dark) Appearance.DARK else Appearance.LIGHT, reduceMotion = true,
            textScale = if (dark) 1.3f else 1f, digits = state.digits, language = state.language) {
            Surface(Modifier.fillMaxSize(), color = CanvasColor) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(appStringResource(Res.string.capture_review_title))
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        TextButton({ state = state.copy(language = AppLanguage.EN) }) { Text(appStringResource(Res.string.cost_fixture_en)) }
                        TextButton({ state = state.copy(language = AppLanguage.BN) }) { Text(appStringResource(Res.string.cost_fixture_bn)) }
                        TextButton({ state = state.copy(digits = if (state.digits == Digits.BN) Digits.EN else Digits.BN) }) { Text(appStringResource(Res.string.cost_fixture_digits)) }
                        TextButton({ dark = !dark }) { Text(appStringResource(Res.string.cost_fixture_display)) }
                    }
                    Text(currentFmt.km(requireNotNull(state.ride).odometer))
                    VaultButton(appStringResource(Res.string.capture_photo_review), full = true, enabled = photo != null,
                        onClick = { sheet = "photo" })
                    VaultButton(appStringResource(Res.string.quickadd_odometer), full = true,
                        onClick = { prefill = null; sheet = "odometer" })
                    VaultButton(appStringResource(Res.string.capture_review_title), full = true,
                        onClick = { sheet = "capture" })
                }
                CompositionLocalProvider(LocalOdometerCapture provides odometerCapture) {
                when (sheet) {
                    "photo" -> Sheet(appStringResource(Res.string.capture_photo_review), { sheet = null }) {
                        OdometerPhotoReviewBody(photo, null, 1000.0, onApply = {
                            prefill = CapturePrefill(null, null, it, emptyList()); sheet = "odometer"
                        }, onClose = { sheet = null }, onRetry = { sheet = null })
                    }
                    "odometer" -> OdometerSheet(state, onDismiss = { sheet = null }, initialOdometer = prefill?.odometer,
                        onSave = { next, _, done -> state = next; done(); sheet = null })
                    "capture" -> CaptureReviewSheet(CaptureInput(appStringResource(Res.string.capture_fixture_payment), CaptureSource.SHARE),
                        state, factory, onDismiss = { sheet = null }, onContinue = { target, value ->
                            prefill = value; sheet = target
                        })
                    "expense" -> ExpenseSheet(state, null, { sheet = null }, { next, done -> state = next; done(true) }, prefill?.amount)
                    "service" -> ServiceSheet(state, initialRows = prefill?.rows, onDismiss = { sheet = null },
                        onSave = { next, _, done -> state = next; done(); sheet = null })
                    "fuel" -> state.fuelScreenInput(today())?.let { input ->
                        com.motovault.app.ui.fuel.FuelSheet(input, initialAmount = prefill?.amount?.let(com.motovault.app.domain.ledger.Money::taka),
                            onDismiss = { sheet = null }, onSave = { _, _, _ -> sheet = null })
                    }
                }
                }
            }
        }
    }
}
