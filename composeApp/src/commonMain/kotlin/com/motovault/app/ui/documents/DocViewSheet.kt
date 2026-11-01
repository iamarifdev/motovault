package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.sky.design.tokens.SkyTokens
import com.motovault.app.domain.model.FuelDefaults
import com.motovault.app.domain.fuel.FuelEntryIssue
import com.motovault.app.domain.fuel.FuelEntryMode
import com.motovault.app.domain.fuel.FuelEntryRules
import com.motovault.app.domain.fuel.MAX_FUEL_LITRES
import com.motovault.app.application.fuel.FuelEntryRequest
import com.motovault.app.application.fuel.FuelEditRequest
import com.motovault.app.domain.fuel.FuelTimelineEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.*

@Composable
fun DocViewSheet(
    doc: VaultDocument,
    onDismiss: () -> Unit,
    onUpdateReminder: (Boolean) -> Unit,
    onReminderSettings: () -> Unit = {},
    warningDays: Int = doc.reminderWarningDays ?: 30,
    privacyEnabled: Boolean = false,
    onInspect: (() -> Unit)? = null,
    onOpenPrivacy: (() -> Unit)? = null,
    language: AppLanguage = AppLanguage.BN
) {
    val f = currentFmt
    val isEn = language == AppLanguage.EN
    var roadsideMode by remember { mutableStateOf(false) }

    Sheet(
        title = doc.title,
        onDismiss = onDismiss,
        detent = SheetDetent.TALL,
        footer = {
            VaultButton(
                text = if (isEn) "Back" else "ফিরে যান",
                kind = ButtonKind.SECONDARY,
                full = true,
                icon = "fi_sun.svg",
                onClick = onDismiss
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (roadsideMode) Color.White else SurfaceColor2)
                .then(if (roadsideMode) Modifier.border(2.dp, Color.Black, RoundedCornerShape(16.dp)) else Modifier)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DocArt(doc = doc, large = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpiryChip(doc = doc, warningDays = warningDays)
                val expiryDate = runCatching { LocalDate.parse(doc.expiry) }.getOrNull()
                Text(
                    text = if (doc.expiry.isBlank()) (if (isEn) "No expiry" else "মেয়াদ নেই") else (if (isEn) "Expires: " else "মেয়াদ শেষ: ") + if (expiryDate != null) f.dateFull(expiryDate) else doc.expiry,
                    fontSize = 13.sp,
                    color = if (roadsideMode) Color(0xFF3A3A3A) else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        VaultButton(
            text = if (privacyEnabled) (if (isEn) "Show this document only" else "শুধু এই ডকুমেন্ট দেখান") else (if (isEn) "Enable protection & show document" else "সুরক্ষা চালু করে ডকুমেন্ট দেখান"),
            kind = ButtonKind.SECONDARY,
            full = true,
            onClick = {
                if (privacyEnabled) onInspect?.invoke() else onOpenPrivacy?.invoke()
            }
        )

        VaultSwitch(
            title = if (isEn) "Police check mode" else "পুলিশ বা ট্রাফিক চেকিং মোড",
            subtitle = if (isEn) "Full brightness and clear display" else "ফুল ব্রাইটনেস ও পরিষ্কার ভিউ",
            checked = roadsideMode,
            onCheckedChange = { roadsideMode = it }
        )

        if (doc.expiry.isNotBlank()) VaultSwitch(
            title = if (isEn) "Renewal reminder" else "এই ডকুমেন্টের রিমাইন্ডার",
            checked = doc.reminder,
            onCheckedChange = { onUpdateReminder(it) }
        )
        VaultButton(if (isEn) "Reminder settings & snooze" else "অনুমতি, সময় ও পরে জানান", onClick = onReminderSettings, full = true, kind = ButtonKind.SECONDARY)
    }
}
