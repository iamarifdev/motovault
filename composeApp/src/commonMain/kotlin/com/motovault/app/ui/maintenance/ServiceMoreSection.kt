package com.motovault.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.VisitRecordState
import motovault.composeapp.generated.resources.*

private const val MAX_WORKSHOP_PHONE_LENGTH = 20
private const val MAX_REPAIR_CASE_LENGTH = 80
private val WARRANTY_OPTIONS = listOf(0, 6, 12)

@Composable
internal fun ServiceMoreSection(
    open: Boolean,
    visitState: VisitRecordState,
    workshopPhone: String,
    caseTitle: String,
    warrantyMonths: Int,
    onToggleOpen: () -> Unit,
    onHideKeypad: () -> Unit,
    onToggleQuote: () -> Unit,
    onWorkshopPhoneChange: (String) -> Unit,
    onCaseTitleChange: (String) -> Unit,
    onWarrantyMonthsChange: (Int) -> Unit
) {
    val f = currentFmt
    val toggleLabel = if (open) {
        appStringResource(Res.string.service_more_hide)
    } else {
        buildString {
            append(appStringResource(Res.string.service_more_title))
            if (visitState.isOpenQuote) append(appStringResource(Res.string.service_more_quote_tag))
            if (warrantyMonths > 0) append(appStringResource(Res.string.service_more_warranty_tag, f.d(warrantyMonths.toString())))
            append(appStringResource(Res.string.service_more_hint))
        }
    }

    Text(
        text = toggleLabel,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = BrandColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleOpen
            )
            .padding(vertical = 6.dp)
    )

    AnimatedVisibility(visible = open) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EntryChip(
                text = appStringResource(Res.string.service_quote_chip),
                active = visitState.isOpenQuote,
                onClick = onToggleQuote
            )
            if (visitState.isOpenQuote) {
                Text(
                    text = appStringResource(Res.string.service_quote_note),
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            ServiceMoreTextField(
                value = workshopPhone,
                placeholder = appStringResource(Res.string.service_workshop_phone),
                keyboardType = KeyboardType.Phone,
                onHideKeypad = onHideKeypad,
                onValueChange = { if (it.length <= MAX_WORKSHOP_PHONE_LENGTH) onWorkshopPhoneChange(it) }
            )
            ServiceMoreTextField(
                value = caseTitle,
                placeholder = appStringResource(Res.string.service_repair_case_placeholder),
                keyboardType = KeyboardType.Text,
                onHideKeypad = onHideKeypad,
                onValueChange = { if (it.length <= MAX_REPAIR_CASE_LENGTH) onCaseTitleChange(it) }
            )
            ChipLine(contentDescription = appStringResource(Res.string.service_warranty_desc)) {
                WARRANTY_OPTIONS.forEach { months ->
                    val label = if (months == 0) {
                        appStringResource(Res.string.service_warranty_none)
                    } else {
                        appStringResource(Res.string.service_warranty_months, f.d(months.toString()))
                    }
                    EntryChip(text = label, active = warrantyMonths == months, onClick = { onWarrantyMonthsChange(months) })
                }
            }
        }
    }
}

@Composable
private fun ServiceMoreTextField(
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onHideKeypad: () -> Unit,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(SurfaceColor2)
            .border(1.5.dp, if (focused) BrandColor else BorderStrongColor, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = if (keyboardType == KeyboardType.Phone) FieldKeyboards.phone else FieldKeyboards.sentence,
            keyboardActions = rememberFieldKeyboardActions(),
            textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
            cursorBrush = SolidColor(BrandColor),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onHideKeypad()
                },
            decorationBox = { inner ->
                if (value.isEmpty()) Text(text = placeholder, fontSize = 15.sp, color = TextTertiary)
                inner()
            }
        )
    }
}
