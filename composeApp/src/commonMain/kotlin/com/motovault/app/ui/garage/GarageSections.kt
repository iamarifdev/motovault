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
internal fun GarageHeaderSection(
    ride: Ride,
    brandWorld: BrandWorld,
    model: BikeModel?,
    onSettings: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
        // Home Header
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = appStringResource(Res.string.garage_title), fontSize = 13.sp, color = TextSecondary)
                Text(
                    text = ride.displayName,
                    fontFamily = FontFamily(Font(Res.font.barlow_condensed)),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 37.8.sp
                )
                val brandLine = "${brandWorld.name} · ${f.d(ride.year.toString())}"
                Text(text = brandLine, fontSize = 13.sp, color = TextSecondary)
            }
            // Avatar Button to Bike Settings / Me tab
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd)))
                    .border(1.5.dp, BrandLineColor, CircleShape)
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onSettings()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (model != null) {
                    Image(
                        painter = painterResource(model.photo),
                        contentDescription = appStringResource(Res.string.garage_bike_settings),
                        modifier = Modifier.requiredSize(66.08.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    V3Icon(path = "feather_icon/fi_user.svg", description = appStringResource(Res.string.garage_bike_settings), modifier = Modifier.size(24.dp), tint = BrandColor)
                }
            }
        }

}

@Composable
internal fun GarageHeroSection(
    topWear: Wear,
    onService: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
        // Hero "Next Up" Card
        if (topWear != null) {
            val palette = LocalVaultPalette.current
            val (statusBg, statusBorder, statusColor, statusLabel) = when (topWear.status) {
                WearStatus.OVERDUE -> Quad(palette.err.copy(alpha = 0.09f), palette.err.copy(alpha = 0.4f), palette.err, appStringResource(Res.string.garage_status_overdue))
                WearStatus.SOON -> Quad(palette.warn.copy(alpha = 0.09f), palette.warn.copy(alpha = 0.4f), palette.warn, appStringResource(Res.string.garage_status_soon))
                WearStatus.OK -> Quad(palette.ok.copy(alpha = 0.08f), palette.ok.copy(alpha = 0.35f), palette.ok, appStringResource(Res.string.garage_status_remaining))
                WearStatus.UNKNOWN -> Quad(palette.surface, palette.line, palette.text2, appStringResource(Res.string.garage_status_unknown))
            }
            val statusIcon = when (topWear.status) {
                WearStatus.OVERDUE -> "fi_triangle-alert.svg"
                WearStatus.SOON -> "fi_timer.svg"
                WearStatus.OK -> "fi_circle-check.svg"
                WearStatus.UNKNOWN -> "fi_triangle-alert.svg"
            }
            val topItem = catalogItemOf(topWear.rule.id) ?: catalogItemOf(topWear.rule.name.lowercase())
            val partTitle = topWear.rule.name.substringBefore(" ও ").substringBefore(" পরিষ্কার")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(statusBg)
                    .border(1.5.dp, statusBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(statusColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        V3Icon(path = "feather_icon/$statusIcon", description = null, modifier = Modifier.size(26.dp), tint = statusColor, strokeWidth = 2.2f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = statusLabel, fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
                        Text(text = partTitle, fontSize = 22.sp, lineHeight = 29.7.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(text = WearCalculator.describeWear(topWear, f), fontSize = 16.sp, lineHeight = 24.8.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }

                // Meter progress bar
                if (topWear.status != WearStatus.UNKNOWN) Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(topWear.used.toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                    )
                }

                val lastDateParsed = runCatching { LocalDate.fromEpochDays(topWear.rule.lastServicedEpochDays.toInt()) }.getOrNull()
                val lastDateStr = if (lastDateParsed != null) f.date(lastDateParsed) else ""
                Text(
                    text = if (topWear.status == WearStatus.UNKNOWN) appStringResource(Res.string.garage_unknown_percent)
                    else appStringResource(Res.string.garage_last_confirmed_date, "$lastDateStr · ${f.km(topWear.rule.lastServicedOdometerKm)}"),
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                val logItemLabel = topItem?.let { catalogShortLabel(it) }
                VaultButton(
                    text = logItemLabel?.let { appStringResource(Res.string.garage_log_item, it) } ?: appStringResource(Res.string.garage_log_service),
                    icon = "fi_tool.svg",
                    full = true,
                    onClick = {
                        haptic.perform(HapticKind.TICK)
                        onService()
                    }
                )
            }
        }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GarageStatsSection(
    ride: Ride,
    economy: Double?,
    series: List<Double>,
    estimate: Double,
    onOdometer: () -> Unit,
    onHistory: () -> Unit
) {
    val f = currentFmt
    val haptic = currentHaptic
        // Stats Row (Odometer & Fuel economy)
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Odometer card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onOdometer()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = appStringResource(Res.string.garage_odometer), fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CountUp(
                        value = ride.odometer,
                        format = { n -> f.n(n.toLong()) },
                        modifier = Modifier.alignByBaseline(),
                        style = TextStyle(fontSize = 30.sp, lineHeight = 37.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = appStringResource(Res.string.garage_unit_km),
                        fontSize = 13.125.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Text(
                    text = appStringResource(Res.string.garage_estimated_today, f.n(estimate.toLong())),
                    fontSize = 12.sp,
                    lineHeight = 18.6.sp,
                    color = TextSecondary,
                    modifier = Modifier.heightIn(min = 26.dp).wrapContentHeight(Alignment.CenterVertically)
                )
                val confirmedDate = ride.odometerConfirmedDate?.let { runCatching { f.date(LocalDate.parse(it)) }.getOrNull() }
                Text(text = appStringResource(Res.string.garage_last_confirmed_date, confirmedDate ?: appStringResource(Res.string.garage_date_unknown)), fontSize = 12.sp, lineHeight = 18.6.sp, color = TextSecondary,
                    modifier = Modifier.heightIn(min = 26.dp).wrapContentHeight(Alignment.CenterVertically))
            }

            // Economy card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.perform(HapticKind.TICK)
                        onHistory()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = appStringResource(Res.string.garage_fuel_average), fontSize = 13.sp, lineHeight = 19.5.sp, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (economy != null) {
                        CountUp(
                            value = economy,
                            format = { n -> f.d(formatLakh(n, 1)) },
                            modifier = Modifier.alignByBaseline(),
                            style = TextStyle(fontSize = 30.sp, lineHeight = 37.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                    } else {
                        val placeholder = "--"
                        Text(text = placeholder, modifier = Modifier.alignByBaseline(), fontSize = 30.sp, lineHeight = 37.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Text(
                        text = appStringResource(Res.string.garage_unit_km_per_litre),
                        fontSize = 13.125.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.CenterStart) {
                    Spark(values = series.takeLast(6))
                }
            }
        }

}

@Composable
internal fun GarageBikeCardSection(
    ride: Ride,
    brandWorld: BrandWorld,
    model: BikeModel?
) {
        // Bike Card
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd)))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (model != null) {
                    val bikeDescription = "${brandWorld.name} ${ride.displayName}"
                    Image(
                        painter = painterResource(model.photo),
                        contentDescription = bikeDescription,
                        modifier = Modifier.fillMaxWidth(0.8f).widthIn(max = 340.dp).fillMaxHeight(0.9f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(64.dp), tint = BrandColor)
                }
            }
            Text(
                text = appStringResource(Res.string.garage_sample_photo),
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

