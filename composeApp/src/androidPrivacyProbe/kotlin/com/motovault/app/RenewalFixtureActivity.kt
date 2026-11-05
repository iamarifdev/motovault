package com.motovault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VaultDocument
import com.motovault.app.storage.security.ProtectedDocumentStorage
import com.motovault.app.ui.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Isolated synthetic renewal screens for the P05 evidence. It never opens the owner database. */
class RenewalFixtureActivity : ComponentActivity() {
    companion object {
        @Volatile var lastState: VaultState? = null
        @Volatile var stagedReceiptId: String? = null
        @Volatile var originalPdfId: String? = null
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".privacyprobe"))
        val stamp = System.currentTimeMillis().toString()
        val originalId = "original-$stamp.pdf"
        ProtectedDocumentStorage.writeNew(originalId, "%PDF-1.4\n% synthetic P05 original\n".encodeToByteArray())
        originalPdfId = originalId
        val receiptId = if (intent.getBooleanExtra("receipt", false)) stageReceipt(stamp) else null
        stagedReceiptId = receiptId
        val initial = syntheticDocument(stamp, originalId)
        lastState = initial
        setContent {
            RenewalFixtureContent(
                initial = initial,
                receiptId = receiptId,
                startScreen = intent.getStringExtra("screen") ?: "view",
                afterSave = intent.getStringExtra("afterSave") ?: "view",
                failSave = intent.getBooleanExtra("failSave", false),
                privacy = intent.getBooleanExtra("privacy", false),
                dark = intent.getBooleanExtra("dark", false)
            )
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun stageReceipt(stamp: String): String {
    val id = "receipt-$stamp.png"
    ProtectedDocumentStorage.writeNew(
        id,
        Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
    )
    return id
}

private fun syntheticDocument(stamp: String, originalId: String): VaultState {
    val expiry = LocalDate.fromEpochDays(today().toEpochDays() + 20).toString()
    return VaultState(
        ride = Ride("yamaha", "BD", 2023, 6000.0),
        documents = listOf(
            VaultDocument("Tax token", "TT-1", "Synthetic owner", expiry, id = "doc-$stamp", kind = "tax",
                reminder = true, originalPdf = originalId)
        )
    )
}

@Composable
private fun RenewalFixtureContent(
    initial: VaultState, receiptId: String?, startScreen: String, afterSave: String,
    failSave: Boolean, privacy: Boolean, dark: Boolean
) {
    var state by remember { mutableStateOf(initial) }
    var screen by remember { mutableStateOf(startScreen) }
    AppResourceEnvironment(AppLanguage.BN) {
        VaultTheme("yamaha", if (dark) Appearance.DARK else Appearance.LIGHT) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val doc = state.documents.first()
                when (screen) {
                    "renew" -> RenewalSheet(
                        state = state, doc = doc,
                        onDismiss = { screen = "view" },
                        onChecklist = { step ->
                            val current = RenewalFixtureActivity.lastState ?: state
                            val checked = current.documents.first { it.id == doc.id }.checkedRenewalSteps
                            val steps = if (step in checked) checked - step else checked + step
                            state = current.setRenewalChecklist(doc.id, steps)
                            RenewalFixtureActivity.lastState = state
                        },
                        onSave = { next, done ->
                            if (failSave) {
                                done(false)
                            } else {
                                state = next
                                RenewalFixtureActivity.lastState = next
                                done(true)
                                screen = afterSave
                            }
                        },
                        initialReceiptId = receiptId
                    )
                    "history" -> HistoryScreen(state, onDeleteLog = {}, onEditFuel = {})
                    "reminder" -> ReminderHandoff(state, doc) { screen = "view" }
                    else -> DocViewSheet(
                        doc = doc,
                        onDismiss = {},
                        onUpdateReminder = { enabled ->
                            state = state.copy(documents = state.documents.map {
                                if (it.id == doc.id) it.copy(reminder = enabled) else it
                            })
                            RenewalFixtureActivity.lastState = state
                        },
                        onRenew = { screen = "renew" },
                        privacyEnabled = privacy,
                        onInspect = {},
                        onOpenPrivacy = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderHandoff(state: VaultState, doc: VaultDocument, onOpen: () -> Unit) {
    val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    val plan = renewalPlan(state.documents, now, emptyList(), emptySet()).firstOrNull { it.documentId == doc.id }
    val target = ReminderTarget("document", doc.id, doc.title, state.digits, state.language)
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (plan == null) {
            Text("No reminder")
        } else {
            Text(reminderHeading(plan, target), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(reminderMessage(plan, target), fontSize = 15.sp)
        }
        VaultButton(text = "কাগজ খুলুন", onClick = onOpen)
    }
}
