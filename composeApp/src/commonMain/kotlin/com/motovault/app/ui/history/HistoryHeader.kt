package com.motovault.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.history_empty
import motovault.composeapp.generated.resources.history_month_year
import motovault.composeapp.generated.resources.history_title

@Composable
internal fun HistoryPageTitle() {
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
        Text(text = appStringResource(Res.string.history_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
internal fun HistoryEmptyCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .v3DashedBorder(BorderStrongColor, 16.dp)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = appStringResource(Res.string.history_empty),
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun HistoryMonthHeader(yearMonth: String, language: AppLanguage, f: Fmt) {
    val palette = LocalVaultPalette.current
    val headerBg = if (LocalWashActive.current) palette.wash.copy(alpha = 0.88f) else palette.canvas.copy(alpha = 0.92f)
    val monthDate = runCatching { LocalDate.parse("$yearMonth-01") }.getOrNull()
    val label = if (monthDate != null) {
        appStringResource(
            Res.string.history_month_year,
            localizedMonthName(language, monthDate.monthNumber - 1),
            f.d(monthDate.year.toString())
        )
    } else {
        yearMonth
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
            .padding(vertical = 6.dp)
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.historyMonthGroup(
    yearMonth: String,
    language: AppLanguage,
    f: Fmt,
    entries: List<RideLog>,
    rowContent: @Composable (RideLog) -> Unit
) {
    stickyHeader(key = "month_$yearMonth") {
        HistoryMonthHeader(yearMonth = yearMonth, language = language, f = f)
    }
    items(items = entries, key = { it.id }) { log ->
        rowContent(log)
    }
}
