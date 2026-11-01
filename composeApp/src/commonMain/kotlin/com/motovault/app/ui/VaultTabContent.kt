package com.motovault.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.motovault.app.ui.backup.BackupSheet
import com.motovault.app.ui.fuel.FuelSheet
import com.motovault.app.ui.backup.BackupSheetTab
import com.motovault.app.ui.backup.RecoverySheet
import com.motovault.app.application.VaultRecovery
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.domain.privacy.*
import com.motovault.app.application.VaultWriter
import com.motovault.app.application.VaultError
import com.motovault.app.application.fuel.FuelEntryBuilder
import com.motovault.app.application.fuel.FuelEntryEditor
import com.motovault.app.application.VaultBackupRestorer
import com.motovault.app.ui.privacy.*
import dev.sky.design.*
import dev.sky.design.tokens.SkyTokens
import kotlinx.coroutines.delay
import com.motovault.app.application.fuel.FuelWorkflow
import com.motovault.app.application.fuel.FuelSaveOutcome
import com.motovault.app.application.VaultBackupOperations
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToLong

@Composable
internal fun ColumnScope.VaultTabContent(
    tab: Tab,
    state: VaultState,
    revision: Long,
    reducedMotion: Boolean,
    recordDeletedMessage: String,
    onSheet: (ActiveSheet) -> Unit,
    onSwitchTab: (Tab) -> Unit,
    onCommit: (VaultState, ((Boolean) -> Unit)?) -> Unit,
    onEditFuel: (RideLog) -> Unit,
    onUndo: (Long) -> Unit,
    onToast: (ToastData?) -> Unit,
    onStartOnboarding: () -> Unit,
    onResetDemo: () -> Unit
) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val reducedMotion = isReducedMotion
                        AnimatedContent(
                            targetState = tab,
                            transitionSpec = {
                                fadeIn(VaultMotion.pageTween(reducedMotion)) togetherWith fadeOut(VaultMotion.pageTween(reducedMotion))
                            },
                            label = "VaultScreenPage",
                            modifier = Modifier.fillMaxSize()
                        ) { destination ->
                            val mainScrollState = rememberScrollState()
                            LaunchedEffect(LaunchConfig.scrollToBottom, destination) {
                                if (LaunchConfig.scrollToBottom) {
                                    mainScrollState.scrollTo(mainScrollState.maxValue)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(mainScrollState)
                            ) {
                                when (destination) {
                                    Tab.GARAGE -> GarageScreen(
                                        state = state,
                                        onOpenSheet = { name, payload -> onSheet(ActiveSheet(name, payload)) },
                                        onSwitchTab = { nextTab -> onSwitchTab(nextTab) },
                                        onOpenServiceForItem = { itemKey -> onSheet(ActiveSheet("service", itemKey)) }
                                    )
                                    Tab.VAULT -> VaultScreen(
                                        state = state,
                                        onOpenAddDoc = { onSheet(ActiveSheet("docAdd")) },
                                        onOpenDocView = { doc -> onSheet(ActiveSheet("docView", doc.id)) },
                                        onToggleDocReminders = { enabled -> onCommit(state.copy(docReminders = enabled), null) },
                                        onReminderSettings = { onSheet(ActiveSheet("reminders")) },
                                        onOpenPrivacy = { onSheet(ActiveSheet("privacy")) }
                                    )
                                    Tab.HISTORY -> HistoryScreen(
                                        state = state,
                                        onEditFuel = { log -> onEditFuel(log) },
                                        onDeleteLog = { log ->
                                            val next = runCatching { state.deleteLog(log) }.getOrNull()
                                            if (next != null) {
                                                onCommit(next) { ok ->
                                                    if (ok) {
                                                        onToast(ToastData(
                                                            id = "del_${log.id}",
                                                            message = recordDeletedMessage,
                                                            undoAction = {
                                                                onUndo(revision + 1)
                                                            }
                                                        ))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Tab.ME -> MeScreen(
                                        state = state,
                                        onUpdateState = { next -> onCommit(next, null) },
                                        onStartOnboarding = { onStartOnboarding() },
                                        onResetDemo = {
                                            val seeded = VaultState.seedData(today())
                                            onCommit(seeded, null)
                                        },
                                        onOpenSheet = { name, payload -> onSheet(ActiveSheet(name, payload)) }
                                    )
                                }
                            }
                        }
                    }

}
