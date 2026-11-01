package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.motovault.app.ui.*
import kotlinx.datetime.LocalDate

/** Isolated synthetic screens for the P04 onboarding and schedule-detail evidence. */
class MaintenanceFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        val screen = intent.getStringExtra("screen") ?: "onboarding"
        val modelId = intent.getStringExtra("modelId") ?: "yamaha-r15-v4"
        val state = VaultState.create(
            Ride("yamaha", "BD", 2023, 6000.0, modelId, if (modelId == "yamaha-mt-15-v2") "MT-15 V2" else "R15 V4",
                market = RideMarket.BANGLADESH, emission = RideEmission.BS6,
                modelCode = if (modelId == "yamaha-mt-15-v2") "MTN155-A" else "YZF155-A",
                factoryProfileId = if (modelId == "yamaha-mt-15-v2") "yamaha-mt-15-v2-2027-bd-155" else "yamaha-r15-v4-2023-bd-155"),
            if (intent.getBooleanExtra("unknownBaseline", false)) null else 1000.0,
            LocalDate.parse("2026-01-15")
        )
        setContent {
            AppResourceEnvironment(AppLanguage.BN) {
                VaultTheme("yamaha", Appearance.LIGHT) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (screen == "details") {
                        GaugeDetailSheet(state, state.rules.first { it.name == "Engine oil" }.id, onDismiss = {}, onOpen = { _, _ -> })
                    } else {
                        OnboardingScreen(
                            initialStep = 2, initialBrandId = "yamaha", initialModelId = modelId, initialModelCode = if (modelId == "yamaha-mt-15-v2") "MTN155-A" else "YZF155-A", onComplete = { _ -> }
                        )
                    }
                    }
                }
            }
        }
    }
}
