package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.date_picker_confirm
import motovault.composeapp.generated.resources.date_picker_next_month
import motovault.composeapp.generated.resources.date_picker_pick_year
import motovault.composeapp.generated.resources.date_picker_prev_month
import motovault.composeapp.generated.resources.date_picker_title
import motovault.composeapp.generated.resources.date_wd_fri
import motovault.composeapp.generated.resources.date_wd_mon
import motovault.composeapp.generated.resources.date_wd_sat
import motovault.composeapp.generated.resources.date_wd_sun
import motovault.composeapp.generated.resources.date_wd_thu
import motovault.composeapp.generated.resources.date_wd_tue
import motovault.composeapp.generated.resources.date_wd_wed

private val WEEKDAY_LABELS = listOf(
    Res.string.date_wd_sun, Res.string.date_wd_mon, Res.string.date_wd_tue, Res.string.date_wd_wed,
    Res.string.date_wd_thu, Res.string.date_wd_fri, Res.string.date_wd_sat
)

/** First of the month for [date]. */
internal fun monthStart(date: LocalDate): LocalDate = LocalDate(date.year, date.monthNumber, 1)

/** Days of [month] laid out Sunday-first, with nulls for the empty cells before day 1. */
internal fun monthGrid(month: LocalDate): List<LocalDate?> {
    val first = monthStart(month)
    val next = first.plus(1, DateTimeUnit.MONTH)
    val lead = first.dayOfWeek.isoDayNumber % 7
    val days = generateSequence(first) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it < next }.toList()
    return List(lead) { null } + days
}

/** True when [date] is inside the optional [min]..[max] range. */
internal fun dateAllowed(date: LocalDate, min: LocalDate?, max: LocalDate?): Boolean =
    (min == null || date >= min) && (max == null || date <= max)

/**
 * Bottom-sheet month calendar (review N1). Replaces free-text yyyy-mm-dd boxes everywhere.
 * Days outside [min]..[max] are greyed and cannot be picked; tapping the month title opens a year list.
 */
@Composable
internal fun DatePickerSheet(
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    min: LocalDate? = null,
    max: LocalDate? = null,
    title: String = appStringResource(Res.string.date_picker_title)
) {
    val haptic = currentHaptic
    var selected by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(monthStart(initial)) }
    var yearMode by remember { mutableStateOf(false) }
    Sheet(
        title = title,
        onDismiss = onDismiss,
        footer = {
            VaultButton(
                text = appStringResource(Res.string.date_picker_confirm),
                full = true,
                enabled = dateAllowed(selected, min, max),
                onClick = { haptic.perform(HapticKind.SUCCESS); onPick(selected) }
            )
        }
    ) {
        CalendarHeader(month, min, max, yearMode, onToggleYear = { yearMode = !yearMode }) { month = it }
        if (yearMode) {
            YearList(month.year, min, max) { year ->
                haptic.perform(HapticKind.TICK)
                month = LocalDate(year, month.monthNumber, 1)
                yearMode = false
            }
        } else {
            CalendarGrid(month, selected, min, max) { day ->
                haptic.perform(HapticKind.TICK)
                selected = day
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    month: LocalDate,
    min: LocalDate?,
    max: LocalDate?,
    yearMode: Boolean,
    onToggleYear: () -> Unit,
    onMonth: (LocalDate) -> Unit
) {
    val f = currentFmt
    val prev = month.plus(-1, DateTimeUnit.MONTH)
    val next = month.plus(1, DateTimeUnit.MONTH)
    // The previous month is reachable when its last day is not before min; the next when its first day is not after max.
    val prevAllowed = min == null || month.plus(-1, DateTimeUnit.DAY) >= min
    val nextAllowed = max == null || next <= max
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MonthArrow(appStringResource(Res.string.date_picker_prev_month), prevAllowed && !yearMode, flipped = true) { onMonth(prev) }
        val yearLabel = appStringResource(Res.string.date_picker_pick_year)
        Text(
            text = f.month(month),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClickLabel = yearLabel, onClick = onToggleYear)
                .padding(vertical = 8.dp)
        )
        MonthArrow(appStringResource(Res.string.date_picker_next_month), nextAllowed && !yearMode, flipped = false) { onMonth(next) }
    }
}

@Composable
private fun MonthArrow(label: String, enabled: Boolean, flipped: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        V3Icon(
            path = "feather_icon/fi_chevron-right.svg",
            description = null,
            modifier = Modifier.size(20.dp).rotate(if (flipped) 180f else 0f),
            tint = if (enabled) TextPrimary else TextTertiary
        )
    }
}

@Composable
private fun CalendarGrid(
    month: LocalDate,
    selected: LocalDate,
    min: LocalDate?,
    max: LocalDate?,
    onSelect: (LocalDate) -> Unit
) {
    val f = currentFmt
    val cells = monthGrid(month)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = appStringResource(label),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            DayCell(f.d(day.dayOfMonth.toString()), f.dateFull(day), day == selected, dateAllowed(day, min, max)) { onSelect(day) }
                        }
                    }
                }
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(text: String, description: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (active) BrandColor else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description; selected = active },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = when {
                active -> OnBrandColor
                enabled -> TextPrimary
                else -> TextTertiary.copy(alpha = 0.5f)
            }
        )
    }
}

@Composable
private fun YearList(current: Int, min: LocalDate?, max: LocalDate?, onYear: (Int) -> Unit) {
    val f = currentFmt
    val first = min?.year ?: (current - 30)
    val last = max?.year ?: (current + 15)
    val shape = RoundedCornerShape(999.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (first..last).toList().reversed().chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { year ->
                    val active = year == current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(shape)
                            .background(if (active) BrandTintColor else Color.Transparent)
                            .border(if (active) 1.5.dp else 1.dp, if (active) BrandColor else BorderColor, shape)
                            .clickable(role = Role.Button) { onYear(year) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = f.d(year.toString()), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
                repeat(4 - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
