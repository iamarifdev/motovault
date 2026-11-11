package com.motovault.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motovault.app.domain.model.*
import com.motovault.app.domain.ledger.LedgerPreferences
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*
import com.motovault.app.application.fuel.FuelEntryBuilder
import com.motovault.app.ui.fuel.FuelSheet

/** Called only by isolated debug hosts. Renders production controls using synthetic state. */
@Composable
internal fun CostRuntimeFixture(fuelBuilder: FuelEntryBuilder<VaultState>, failSave: Boolean = false) {
    var state by remember { mutableStateOf(costFixtureState()) }
    var undo by remember { mutableStateOf<VaultState?>(null) }
    var sheet by remember { mutableStateOf<ActiveSheet?>(null) }
    var dark by remember { mutableStateOf(false) }
    val save: (VaultState, (Boolean) -> Unit) -> Unit = { next, done ->
        if (failSave) done(false) else { undo = state; state = next; done(true) }
    }
    AppResourceEnvironment(state.language) {
        VaultTheme("yamaha", if (dark) Appearance.DARK else Appearance.LIGHT,
            reduceMotion = true, textScale = if (dark) 1.3f else 1f, digits = state.digits, language = state.language) {
            Surface(Modifier.fillMaxSize(), color = CanvasColor) {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    CostFixtureControls(state, { state = it }, { dark = !dark }, { undo?.let { state = it; undo = null } }, { sheet = ActiveSheet("fuel") })
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        HistoryScreen(state, onDeleteLog = { undo = state; state = state.deleteLog(it) },
                            onEditFuel = {}, onOpenCost = { name, log -> sheet = ActiveSheet(name, log) })
                    }
                }
                sheet?.let {
                    if (it.name == "fuel") state.fuelScreenInput(today())?.let { input ->
                        FuelSheet(input, onDismiss = { sheet = null }, onSave = { request, _, done ->
                            save(fuelBuilder.build(state, request)) { ok -> if (ok) { done(); sheet = null } }
                        })
                    } else CostSheetHost(it, state, { sheet = null }, save, { undo?.let { previous -> state = previous } }, {})
                }
            }
        }
    }
}

@Composable
private fun CostFixtureControls(state: VaultState, onChange: (VaultState) -> Unit, onDisplay: () -> Unit, onUndo: () -> Unit, onFuel: () -> Unit) {
    Column {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton({ onChange(state.copy(language = AppLanguage.EN)) }) { Text(appStringResource(Res.string.cost_fixture_en)) }
        TextButton({ onChange(state.copy(language = AppLanguage.BN)) }) { Text(appStringResource(Res.string.cost_fixture_bn)) }
        TextButton({ onChange(state.copy(digits = if (state.digits == Digits.EN) Digits.BN else Digits.EN)) }) { Text(appStringResource(Res.string.cost_fixture_digits)) }
        TextButton(onDisplay) { Text(appStringResource(Res.string.cost_fixture_display)) }
        TextButton(onUndo) { Text(appStringResource(Res.string.action_undo)) }
    }
    TextButton(onFuel) { Text(appStringResource(Res.string.cost_fixture_fuel)) }
    }
}

private fun costFixtureState(): VaultState {
    val today = today()
    fun date(days: Int) = LocalDate.fromEpochDays(today.toEpochDays() - days).toString()
    val logs = listOf(
        RideLog("", "", date(10), 1000.0, id = "cost-fuel-start", kind = "fuel", liters = 8.0, cost = 500.0),
        RideLog("", "", date(5), 1060.0, id = "cost-fuel-partial", kind = "fuel", liters = 2.0, fullTank = false, cost = 200.0),
        RideLog("", "", date(0), 1150.0, id = "cost-fuel-end", kind = "fuel", liters = 3.0, cost = 300.0),
        RideLog("", "", date(0), 1150.0, id = "cost-renewal", kind = "document_fee", cost = 1250.0),
        RideLog("", "", date(0), 1150.0, id = "cost-parking", kind = "expense", cost = 0.5, costPoisha = 50, expenseCategory = "parking")
    )
    return VaultState(ride = Ride("yamaha", "Synthetic", 2026, 1150.0), logs = logs,
        ledger = LedgerPreferences(monthlyBudgetPoisha = 300000))
}
