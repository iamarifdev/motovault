package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import motovault.composeapp.generated.resources.*
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.painterResource
@Composable
fun MeScreen(
    state: VaultState,
    onUpdateState: (VaultState) -> Unit,
    onStartOnboarding: () -> Unit,
    onResetDemo: () -> Unit,
    onOpenSheet: (String, Any?) -> Unit
) {
    val ride = state.ride ?: return
    val brand = worlds.firstOrNull { it.id == ride.brandId } ?: worlds.first()
    val model = ride.selectedModel
    val activeBrandId = state.brandOverride ?: ride.brandId

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SettingsProfileSection(brand, model, ride, onStartOnboarding)
        SettingsAppearanceSection(state, activeBrandId, ride.brandId, onUpdateState)
        SettingsPreferencesSection(state, onUpdateState)
        SettingsRemindersSection(state, onUpdateState, onOpenSheet)
        SettingsPrivacySection(state, onUpdateState, onOpenSheet)
        SettingsBackupSection(state, onOpenSheet)
        SettingsDemoSection(ride, onOpenSheet)
        SettingsAboutSection(onResetDemo)
    }
}

// ==========================================
// 5. ONBOARDING (3 STEPS) SCREEN
// ==========================================
