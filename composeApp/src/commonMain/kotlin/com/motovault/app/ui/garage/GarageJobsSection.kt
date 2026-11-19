package com.motovault.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.BaselineCertainty
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.garage_expired_ago
import motovault.composeapp.generated.resources.garage_expires_in
import motovault.composeapp.generated.resources.garage_jobs_all_clear
import motovault.composeapp.generated.resources.garage_jobs_filter_all
import motovault.composeapp.generated.resources.garage_jobs_filter_due
import motovault.composeapp.generated.resources.garage_jobs_heading
import motovault.composeapp.generated.resources.garage_jobs_see_less
import motovault.composeapp.generated.resources.garage_jobs_see_more
import motovault.composeapp.generated.resources.garage_quick_document
import motovault.composeapp.generated.resources.garage_wear_last_done
import motovault.composeapp.generated.resources.garage_wear_last_unknown

@Composable
internal fun GarageJobsSection(
    snapshot: GarageHomeSnapshot,
    onOpenWear: (String) -> Unit,
    onOpenDoc: (String) -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
    var requestedFilter by remember { mutableStateOf(GarageJobFilter.DUE) }
    var showAll by remember { mutableStateOf(false) }
    val activeFilter = snapshot.effectiveFilter(requestedFilter)
    val visible = snapshot.visibleJobs(requestedFilter, showAll)
    val hiddenExtra = snapshot.hiddenExtraCount(requestedFilter, showAll)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = appStringResource(Res.string.garage_jobs_heading), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (snapshot.dueJobs.isNotEmpty()) {
                    JobFilterChip(
                        label = appStringResource(Res.string.garage_jobs_filter_due, f.d(snapshot.dueJobs.size.toString())),
                        active = activeFilter == GarageJobFilter.DUE,
                        onClick = { haptic.perform(HapticKind.TICK); requestedFilter = GarageJobFilter.DUE }
                    )
                }
                JobFilterChip(
                    label = appStringResource(Res.string.garage_jobs_filter_all, f.d(snapshot.allJobs.size.toString())),
                    active = activeFilter == GarageJobFilter.ALL,
                    onClick = { haptic.perform(HapticKind.TICK); requestedFilter = GarageJobFilter.ALL }
                )
            }
        }

        if (snapshot.dueJobs.isEmpty()) CalmAllClearCard()

        if (visible.isNotEmpty()) {
            val shape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier.fillMaxWidth().clip(shape).background(SurfaceColor).border(1.dp, BorderColor, shape)
            ) {
                visible.forEachIndexed { index, job ->
                    if (index > 0) HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    when (job) {
                        is GarageJob.WearJob -> WearJobRow(job, index, f) { onOpenWear(job.wear.rule.id) }
                        is GarageJob.DocJob -> DocJobRow(job, f) { onOpenDoc(job.doc.id) }
                    }
                }
            }
        }

        if (activeFilter == GarageJobFilter.ALL && snapshot.allJobs.size > 5) {
            val label = if (showAll) appStringResource(Res.string.garage_jobs_see_less)
            else appStringResource(Res.string.garage_jobs_see_more, f.d(hiddenExtra.toString()))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { haptic.perform(HapticKind.TICK); showAll = !showAll }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
            }
        }
    }
}

@Composable
private fun JobFilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(shape)
            .background(if (active) BrandTintColor else SurfaceColor)
            .border(1.5.dp, if (active) BrandColor else BorderColor, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { selected = active }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun CalmAllClearCard() {
    val palette = LocalVaultPalette.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colorMix(palette.ok, palette.surface, 0.08f))
            .border(1.dp, colorMix(palette.ok, palette.line, 0.22f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        V3Icon(path = "feather_icon/fi_circle-check.svg", description = null, modifier = Modifier.size(20.dp), tint = palette.ok, strokeWidth = 2.2f)
        Text(text = appStringResource(Res.string.garage_jobs_all_clear), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun WearJobRow(job: GarageJob.WearJob, index: Int, f: Fmt, onClick: () -> Unit) {
    val haptic = currentHaptic
    val wear = job.wear
    val rule = wear.rule
    val iconName = when (wear.status) {
        WearStatus.OK -> "fi_circle-check.svg"
        WearStatus.SOON -> "fi_timer.svg"
        WearStatus.OVERDUE, WearStatus.UNKNOWN -> "fi_triangle-alert.svg"
    }
    val metaText = if (wear.status == WearStatus.UNKNOWN || rule.baselineCertainty != BaselineCertainty.CONFIRMED) {
        appStringResource(Res.string.garage_wear_last_unknown)
    } else {
        val lastDate = f.date(LocalDate.fromEpochDays(rule.lastServicedEpochDays.toInt()))
        appStringResource(Res.string.garage_wear_last_done, f.km(rule.lastServicedOdometerKm), lastDate)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { haptic.perform(HapticKind.TICK); onClick() }
            .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JobRowHeader(
            iconName = iconName,
            status = wear.status,
            title = componentShortLabel(rule),
            meta = metaText,
            badgeText = WearCalculator.describeWear(wear, f)
        )
        WearProgressBar(status = wear.status, targetProgress = job.barProgress, index = index)
    }
}

@Composable
private fun DocJobRow(job: GarageJob.DocJob, f: Fmt, onClick: () -> Unit) {
    val haptic = currentHaptic
    val status = if (job.daysLeft < 0) WearStatus.OVERDUE else WearStatus.SOON
    val badgeText = if (job.daysLeft < 0) appStringResource(Res.string.garage_expired_ago, f.days(-job.daysLeft))
    else appStringResource(Res.string.garage_expires_in, f.days(job.daysLeft))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { haptic.perform(HapticKind.TICK); onClick() }
            .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JobRowHeader(
            iconName = "fi_file-text.svg",
            status = status,
            title = documentDisplayName(job.doc.title),
            meta = appStringResource(Res.string.garage_quick_document),
            badgeText = badgeText
        )
    }
}

@Composable
private fun JobRowHeader(iconName: String, status: WearStatus, title: String, meta: String, badgeText: String) {
    val palette = LocalVaultPalette.current
    val (iconBg, iconFg) = when (status) {
        WearStatus.OK -> colorMix(palette.ok, palette.surface, 0.12f) to palette.ok
        WearStatus.SOON -> colorMix(palette.warn, palette.surface, 0.14f) to palette.warn
        WearStatus.OVERDUE -> colorMix(palette.err, palette.surface, 0.14f) to palette.err
        WearStatus.UNKNOWN -> palette.surface2 to palette.text3
    }
    val (badgeBg, badgeFg) = when (status) {
        WearStatus.OK -> palette.statusBadgeBg(StatusTone.OK) to palette.statusBadgeFg(StatusTone.OK)
        WearStatus.SOON -> palette.statusBadgeBg(StatusTone.SOON) to palette.statusBadgeFg(StatusTone.SOON)
        WearStatus.OVERDUE -> palette.statusBadgeBg(StatusTone.OVERDUE) to palette.statusBadgeFg(StatusTone.OVERDUE)
        WearStatus.UNKNOWN -> palette.surface2 to palette.text2
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            V3Icon(path = "feather_icon/$iconName", description = null, modifier = Modifier.size(17.dp), tint = iconFg, strokeWidth = 2.2f)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, lineHeight = 18.2.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = meta, fontSize = 11.5.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(badgeBg).padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(text = badgeText, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = badgeFg, maxLines = 1)
            }
            V3Icon(path = "feather_icon/fi_chevron-right.svg", description = null, modifier = Modifier.size(16.dp), tint = TextTertiary)
        }
    }
}

@Composable
private fun WearProgressBar(status: WearStatus, targetProgress: Float, index: Int) {
    val palette = LocalVaultPalette.current
    val reducedMotion = isReducedMotion
    val progress = remember { Animatable(if (reducedMotion) targetProgress else 0f) }
    LaunchedEffect(targetProgress, reducedMotion) {
        if (reducedMotion) progress.snapTo(targetProgress)
        else {
            delay(minOf(280L, index * 40L))
            progress.animateTo(targetProgress, tween(VaultMotion.SweepMs, easing = VaultMotion.SkyCurve))
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)).background(SurfaceColor2)
    ) {
        if (status == WearStatus.UNKNOWN) {
            val stripeColor = BorderStrongColor.copy(alpha = 0.65f)
            Canvas(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = progress.value
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
            ) {
                val seg = 6.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawRect(stripeColor, topLeft = Offset(x, 0f), size = Size(minOf(seg, size.width - x), size.height))
                    x += seg * 2f
                }
            }
        } else {
            val barColor = when (status) {
                WearStatus.OK -> palette.ok
                WearStatus.SOON -> palette.warn
                WearStatus.OVERDUE -> palette.err
                WearStatus.UNKNOWN -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = progress.value
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clip(RoundedCornerShape(999.dp))
                    .background(barColor)
            )
        }
    }
}
