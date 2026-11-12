package com.motovault.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.motovault.app.ui.AppResourceEnvironment
import com.motovault.app.ui.MotoVault

@Composable
fun App() {
    val snapshot by AppComposition.vault.snapshots.collectAsState()
    AppResourceEnvironment(snapshot.state.language) {
        MotoVault(AppComposition.vault, AppComposition.backupRestore, AppComposition.fuelEntry,
            AppComposition.fuelEntry, AppComposition.recoveryRestore, AppComposition.backupOperations, AppComposition.captureDrafts)
    }
}
