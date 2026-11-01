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


data class BrandWorld(val id: String, val name: String, val accent: Color, val tone: String)

val worlds = listOf(
    BrandWorld("ktm", "KTM", Color(0xFFFF7900), "Racing orange"),
    BrandWorld("cfmoto", "CFMOTO", Color(0xFF69D2DB), "Performance cyan"),
    BrandWorld("yamaha", "Yamaha", Color(0xFF88A8FF), "Performance blue"),
    BrandWorld("honda", "Honda", Color(0xFFFF7B84), "Honda red"),
    BrandWorld("suzuki", "Suzuki", Color(0xFF7EBAF5), "Sport blue"),
    BrandWorld("bajaj", "Bajaj", Color(0xFF96B9FA), "Pulsar blue"),
    BrandWorld("tvs", "TVS", Color(0xFFF4828B), "Racing red"),
    BrandWorld("hero", "Hero", Color(0xFFFF8983), "Hero red"),
    BrandWorld("enfield", "Royal Enfield", Color(0xFFDBB77C), "Heritage gold")
)

enum class Tab(val id: String, val titleBn: String, val titleEn: String, val icon: String) {
    GARAGE("garage", "গ্যারেজ", "Garage", "fi_home.svg"),
    VAULT("vault", "কাগজপত্র", "Documents", "fi_folder-open.svg"),
    HISTORY("history", "রেকর্ড", "Records", "fi_history.svg"),
    ME("me", "প্রোফাইল", "Profile", "fi_user.svg");
    val title: String get() = titleBn
    fun label(lang: AppLanguage) = if (lang == AppLanguage.EN) titleEn else titleBn
}

data class ActiveSheet(
    val name: String,
    val props: Any? = null
)

internal data class FuelEditTarget(val record: RideLog, val revision: Long)
