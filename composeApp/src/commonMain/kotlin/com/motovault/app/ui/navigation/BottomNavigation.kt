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
internal fun VaultBottomNavigation(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onQuickAdd: () -> Unit
) {
    val haptic = currentHaptic
    val brand = BrandColor
    val onBrand = OnBrandColor
    val surface = SurfaceColor
    val line = BorderColor
    val reducedMotion = isReducedMotion
    val addRecordDescription = appStringResource(Res.string.nav_add_record)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
    ) {
        HorizontalDivider(color = line, thickness = 0.5.dp)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val slotWidth = maxWidth / 5
        val slot = when (selected) {
            Tab.GARAGE -> 0
            Tab.VAULT -> 1
            Tab.HISTORY -> 3
            Tab.ME -> 4
        }
        val markerX by androidx.compose.animation.core.animateDpAsState(
            targetValue = slotWidth * slot + (slotWidth - 28.dp) / 2,
            animationSpec = VaultMotion.markerSpring(reducedMotion),
            label = "tabMarker"
        )
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: GARAGE
            BottomNavItem(
                tab = Tab.GARAGE,
                isSelected = selected == Tab.GARAGE,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.GARAGE)
                },
                modifier = Modifier.weight(1f)
            )

            // Tab 2: VAULT
            BottomNavItem(
                tab = Tab.VAULT,
                isSelected = selected == Tab.VAULT,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.VAULT)
                },
                modifier = Modifier.weight(1f)
            )

            // Center slot: Tab Plus button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.92f else 1f,
                    animationSpec = VaultMotion.pressSpring(reducedMotion),
                    label = "plusScale"
                )

                Box(
                    modifier = Modifier
                        .offset(y = (-23).dp)
                        .size(70.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .background(surface, CircleShape)
                        .padding(5.dp)
                        .shadow(8.dp, CircleShape, ambientColor = brand.copy(alpha = 0.45f), spotColor = brand.copy(alpha = 0.45f))
                        .clip(CircleShape)
                        .background(brand)
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) {
                            haptic.perform(HapticKind.SUCCESS)
                            onQuickAdd()
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = addRecordDescription
                        },
                    contentAlignment = Alignment.Center
                ) {
                    V3Icon(
                        path = "feather_icon/fi_plus.svg",
                        description = null,
                        modifier = Modifier.size(30.dp),
                        tint = onBrand,
                        strokeWidth = 2.4f
                    )
                }
            }

            // Tab 3: HISTORY
            BottomNavItem(
                tab = Tab.HISTORY,
                isSelected = selected == Tab.HISTORY,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.HISTORY)
                },
                modifier = Modifier.weight(1f)
            )

            // Tab 4: ME
            BottomNavItem(
                tab = Tab.ME,
                isSelected = selected == Tab.ME,
                onClick = {
                    haptic.perform(HapticKind.TICK)
                    onSelect(Tab.ME)
                },
                modifier = Modifier.weight(1f)
            )
        }
        Box(Modifier.align(Alignment.BottomStart).offset(x = markerX, y = (-5).dp)
            .size(28.dp, 4.dp).background(brand, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: Tab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) BrandColor else TextTertiary
    val title = appStringResource(when (tab) {
        Tab.GARAGE -> Res.string.nav_garage
        Tab.VAULT -> Res.string.nav_vault
        Tab.HISTORY -> Res.string.nav_records
        Tab.ME -> Res.string.nav_profile
    })
    Column(
        modifier = modifier
            .heightIn(min = 70.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        V3Icon(
            path = "feather_icon/${tab.icon}",
            description = title,
            modifier = Modifier.size(28.dp),
            tint = color,
            strokeWidth = if (isSelected) 2.4f else 2f
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            lineHeight = 20.sp
        )
    }
}

// =========================================================================
// Legacy UI Utilities (Retained for binary and backward compatibility)
// =========================================================================
