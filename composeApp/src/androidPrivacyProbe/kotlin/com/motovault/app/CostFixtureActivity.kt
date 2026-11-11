package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.motovault.app.ui.CostRuntimeFixture
import com.motovault.app.storage.FuelEntryAdapter

/** Isolated UI fixture; never opens the owner's database. */
class CostFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        setContent { CostRuntimeFixture(FuelEntryAdapter, failSave = intent.getBooleanExtra("failSave", false)) }
    }
}
