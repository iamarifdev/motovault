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
fun Top(title: String, caption: String, onAppearance: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(5.dp).background(HealthyColor, CircleShape))
                Text(caption, color = TextSecondary, fontSize = 12.sp)
            }
        }
        AppearanceButton(onAppearance)
    }
}

@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) =
    Text(text.uppercase(), color = color, fontSize = 12.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)

@Composable
fun Heading(text: String) = SkyGradientHeading(text)

@Composable
fun Panel(onClick: (() -> Unit)? = null, tint: Color = SurfaceColor, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(SkyTokens.Radii.menu))
            .background(tint)
            .border(1.dp, BorderColor, RoundedCornerShape(SkyTokens.Radii.menu))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun Primary(text: String, icon: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    SkyButton(text, onClick, modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = enabled)
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, style = SkyTheme.typography.label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            singleLine = true,
            textStyle = SkyTheme.typography.body,
            shape = RoundedCornerShape(SkyTokens.Radii.field),
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SkyTheme.colors.fieldBorder,
                focusedBorderColor = SkyTheme.colors.accent,
                unfocusedContainerColor = SkyTheme.colors.input,
                focusedContainerColor = SkyTheme.colors.input
            )
        )
    }
}

@Composable
fun Glyph(name: String, color: Color = TextPrimary, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier) {
        val scale = size.minDimension / 24f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * scale, y1 * scale), Offset(x2 * scale, y2 * scale), 1.6f * scale, StrokeCap.Round)
        fun path(vararg points: Pair<Float, Float>, closed: Boolean = false) {
            val p = Path()
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) p.moveTo(x * scale, y * scale) else p.lineTo(x * scale, y * scale)
            }
            if (closed) p.close()
            drawPath(p, color, style = Stroke(1.6f * scale, cap = StrokeCap.Round))
        }
        when (name) {
            "arrow" -> { line(4f, 12f, 20f, 12f); path(14f to 6f, 20f to 12f, 14f to 18f) }
            "check" -> path(4f to 12f, 9f to 17f, 20f to 6f)
            "add" -> { line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f) }
            "close" -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
            "garage" -> { path(3f to 10f, 12f to 3f, 21f to 10f, 21f to 21f, 3f to 21f, closed = true); line(7f, 14f, 17f, 14f); line(7f, 18f, 17f, 18f) }
            "vault" -> { path(5f to 10f, 19f to 10f, 19f to 21f, 5f to 21f, closed = true); drawArc(color, 180f, 180f, false, Offset(8f * scale, 2f * scale), Size(8f * scale, 12f * scale), style = Stroke(1.6f * scale)); line(12f, 14f, 12f, 17f) }
            "fuel" -> { path(4f to 21f, 4f to 4f, 13f to 4f, 13f to 21f, closed = true); line(4f, 10f, 13f, 10f); path(13f to 12f, 17f to 12f, 17f to 19f, 21f to 19f, 21f to 8f, 18f to 5f) }
            "service" -> { path(6f to 3f, 6f to 8f, 11f to 11f, 16f to 8f, 16f to 3f); path(10f to 11f, 10f to 21f, 13f to 21f, 13f to 11f) }
            "history" -> { drawArc(color, -80f, 300f, false, Offset(4f * scale, 4f * scale), Size(16f * scale, 16f * scale), style = Stroke(1.6f * scale)); path(2f to 4f, 2f to 10f, 8f to 10f); path(12f to 7f, 12f to 12f, 16f to 14f) }
            "mark" -> { val p = Path(); p.moveTo(0f, 0f); p.lineTo(12f * scale, 5f * scale); p.lineTo(24f * scale, 0f); p.lineTo(24f * scale, 16f * scale); p.lineTo(12f * scale, 24f * scale); p.lineTo(0f, 16f * scale); p.close(); drawPath(p, color); path(5f to 7f, 12f to 12f, 19f to 7f) }
            else -> { path(5f to 3f, 15f to 3f, 20f to 8f, 20f to 21f, 5f to 21f, closed = true); line(9f, 12f, 16f, 12f); line(9f, 16f, 16f, 16f) }
        }
    }
}
