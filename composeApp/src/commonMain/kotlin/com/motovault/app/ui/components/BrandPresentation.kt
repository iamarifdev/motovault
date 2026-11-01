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


fun componentSuffix(rule: ComponentRule): String {
    val knownSuffix = listOf("air_filter", "spark_plug", "brake_pads", "brake_fluid", "chain", "oil", "coolant")
        .firstOrNull { rule.id.endsWith("_$it") }
    return knownSuffix ?: when (rule.name.trim().lowercase()) {
        "chain" -> "chain"
        "engine oil", "engine oil & oil filter" -> "oil"
        "air filter" -> "air_filter"
        "spark plug" -> "spark_plug"
        "brake pads (front & rear)" -> "brake_pads"
        "engine coolant" -> "coolant"
        "brake fluid (dot 4)" -> "brake_fluid"
        else -> rule.id.substringAfterLast('_')
    }
}

@Composable
fun componentLabel(rule: ComponentRule): String {
    rule.catalogueId?.let(BangladeshServiceCatalogue::find)?.let { item ->
        return catalogueLabel(item.id)
    }
    val suffix = componentSuffix(rule)
    val bengaliName = when (suffix) {
        "chain" -> appStringResource(Res.string.component_chain)
        "oil" -> appStringResource(Res.string.component_oil)
        "air_filter" -> appStringResource(Res.string.component_air_filter)
        "spark_plug" -> appStringResource(Res.string.component_spark_plug)
        "brake_pads" -> appStringResource(Res.string.component_brake_pads)
        "coolant" -> appStringResource(Res.string.component_coolant)
        "brake_fluid" -> appStringResource(Res.string.component_brake_fluid)
        else -> null
    }
    return bengaliName ?: rule.name
}

fun BrandWorld.logo(): DrawableResource = when (id) {
    "ktm" -> Res.drawable.logo_ktm
    "cfmoto" -> Res.drawable.logo_cfmoto
    "yamaha" -> Res.drawable.logo_yamaha
    "honda" -> Res.drawable.logo_honda
    "suzuki" -> Res.drawable.logo_suzuki
    "bajaj" -> Res.drawable.logo_bajaj
    "tvs" -> Res.drawable.logo_tvs
    "hero" -> Res.drawable.logo_hero
    else -> Res.drawable.logo_enfield
}
