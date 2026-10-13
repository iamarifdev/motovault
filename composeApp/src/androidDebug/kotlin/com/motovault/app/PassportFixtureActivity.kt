package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.motovault.app.ui.redesign.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Debug-only, synthetic renderer probe. Does not load, replace or save the owner's vault. */
class PassportFixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultContext = applicationContext
        setContent {
            var result by remember { mutableStateOf("Preparing synthetic Unicode ledger...") }
            LaunchedEffect(Unit) {
                result = runCatching {
                    val state = VaultState(
                        ride = Ride("honda", "SAMPLE / TEST ONLY", 2024, 80000.0, modelName = "SAMPLE Unicode motorcycle"),
                        logs = (0 until 70).map { index ->
                            RideLog("SAMPLE $index / ইঞ্জিন তেল / Café", "Owner-entered synthetic fixture", LocalDate.fromEpochDays(LocalDate(2026,1,1).toEpochDays()+index).toString(),1000.25+index*1000,index.toString(),"service",cost=1250.55+index,
                                workshop = "SAMPLE / ঢাকা গ্যারেজ / 日本語",
                                notes = if(index == 30) (1..100).joinToString("\n") { "Long note line $it / বাংলা / Café / SAMPLE" } else "বাংলা নোট / SAMPLE fixture. ".repeat(8))
                        })
                    val identity = passportDefaults(state).copy(owner = "SAMPLE / আরিফ ইসলাম", registration = "SAMPLE ঢাকা-১২", chassis="SAMPLE-C",engine="SAMPLE-E")
                    val bundle = ExportServicePassportUseCase().execute(state,identity,true,"2026-10-12T00:00:00Z")
                    val export = renderServicePassport(bundle)
                    "SAMPLE ONLY\n${export.pdfPath}\n${export.jsonPath}\nSHA-256 ${bundle.sha256}"
                }.getOrElse { "Fixture failed: ${it.message}" }
            }
            MaterialTheme { Text(result) }
        }
    }
}
