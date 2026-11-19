package com.motovault.app.ui

import androidx.compose.animation.core.animateDpAsState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.motovault.app.domain.model.Ride
import kotlinx.coroutines.flow.distinctUntilChanged
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.garage_bike_title_full
import motovault.composeapp.generated.resources.garage_carousel_desc
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

@Composable
internal fun ShowroomCarousel(
    bikes: List<Ride>,
    activeBikeId: String,
    onSwitchBike: (String, (Boolean) -> Unit) -> Unit
) {
    val multi = bikes.size > 1
    val carouselDesc = appStringResource(Res.string.garage_carousel_desc)
    val activeIndex = bikes.indexOfFirst { it.id == activeBikeId }.coerceAtLeast(0)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!multi) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .semantics { contentDescription = carouselDesc },
                contentAlignment = Alignment.Center
            ) {
                bikes.firstOrNull()?.let { bike -> BikeStageSlide(bike = bike, active = true, onTap = {}) }
            }
        } else {
            val position = remember { Animatable(activeIndex.toFloat()) }
            val scope = rememberCoroutineScope()
            val reducedMotion = isReducedMotion
            val goTo: (Int) -> Unit = { page ->
                scope.launch {
                    if (reducedMotion) position.snapTo(page.toFloat()) else position.animateTo(page.toFloat(), showroomSnapSpring)
                }
            }
            ShowroomTrack(
                position = position,
                bikes = bikes,
                activeIndex = activeIndex,
                carouselDesc = carouselDesc,
                onGoTo = goTo,
                onSwitchBike = onSwitchBike
            )
            Spacer(Modifier.height(2.dp))
            ShowroomDots(bikes = bikes, activeIndex = activeIndex, onSelect = goTo)
        }
    }
}

/** v4 spring 420/40/mass .9, expressed as Compose damping ratio and stiffness (k/m). */
private val showroomSnapSpring = spring<Float>(dampingRatio = 1.03f, stiffness = 466.7f)

/** v4 BikeShowroom: slides are 84% wide; a swipe past 22% of a slide (offset + velocity x 0.2) moves one bike. */
private const val SHOWROOM_SLIDE_FRACTION = 0.84f
private const val SHOWROOM_SWIPE_THRESHOLD = 0.22f
private const val SHOWROOM_VELOCITY_FACTOR = 0.2f

@Composable
private fun ShowroomTrack(
    position: Animatable<Float, AnimationVector1D>,
    bikes: List<Ride>,
    activeIndex: Int,
    carouselDesc: String,
    onGoTo: (Int) -> Unit,
    onSwitchBike: (String, (Boolean) -> Unit) -> Unit
) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val scope = rememberCoroutineScope()
    val latestActive by rememberUpdatedState(activeIndex)
    val latestBikes by rememberUpdatedState(bikes)
    val latestOnSwitch by rememberUpdatedState(onSwitchBike)
    var failedSwitches by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragStart by remember { mutableFloatStateOf(0f) }

    // A settled slide that differs from the active bike switches to it (v4 switches after the slide settles).
    LaunchedEffect(position) {
        snapshotFlow { if (position.isRunning || dragging) null else position.value }
            .filterNotNull()
            .map { it.roundToInt() }
            .distinctUntilChanged()
            .collect { page ->
                val list = latestBikes
                if (page in list.indices && page != latestActive) {
                    haptic.perform(HapticKind.TICK)
                    latestOnSwitch(list[page].id) { ok -> if (!ok) failedSwitches++ }
                }
            }
    }
    // The active bike changed elsewhere (switcher sheet) or a switch failed: bring the stage back in line.
    LaunchedEffect(activeIndex, bikes.size, failedSwitches) {
        if (!dragging && activeIndex in bikes.indices && position.value != activeIndex.toFloat()) {
            if (reducedMotion) position.snapTo(activeIndex.toFloat()) else position.animateTo(activeIndex.toFloat(), showroomSnapSpring)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clipToBounds()
            .semantics { contentDescription = carouselDesc }
    ) {
        val slideWidth = maxWidth * SHOWROOM_SLIDE_FRACTION
        val slideWidthPx = with(LocalDensity.current) { slideWidth.toPx() }
        val lastIndex = bikes.lastIndex.toFloat()
        val dragState = rememberDraggableState { deltaPx ->
            scope.launch { position.snapTo((position.value - deltaPx / slideWidthPx).coerceIn(-0.15f, lastIndex + 0.15f)) }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { dragging = true; position.stop(); dragStart = position.value },
                    onDragStopped = { velocityPx ->
                        val moved = (position.value - dragStart) - (velocityPx / slideWidthPx) * SHOWROOM_VELOCITY_FACTOR
                        val base = dragStart.roundToInt()
                        val target = when {
                            moved > SHOWROOM_SWIPE_THRESHOLD -> base + 1
                            moved < -SHOWROOM_SWIPE_THRESHOLD -> base - 1
                            else -> base
                        }.coerceIn(0, bikes.lastIndex)
                        dragging = false
                        if (reducedMotion) position.snapTo(target.toFloat()) else position.animateTo(target.toFloat(), showroomSnapSpring)
                    }
                )
        ) {
            bikes.forEachIndexed { page, bike ->
                val distance = page - position.value
                val offset = abs(distance).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .width(slideWidth)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = distance * slideWidthPx
                            alpha = lerp(1f, 0.4f, offset)
                            scaleX = lerp(1f, 0.82f, offset)
                            scaleY = lerp(1f, 0.82f, offset)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BikeStageSlide(
                        bike = bike,
                        active = page == activeIndex,
                        onTap = { if (page.toFloat() != position.value) onGoTo(page) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BikeStageSlide(bike: Ride, active: Boolean, onTap: () -> Unit) {
    val model = bike.selectedModel
    val brand = worlds.firstOrNull { it.id == bike.brandId } ?: worlds.first()
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = interaction, indication = null, enabled = !active, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            Canvas(modifier = Modifier.fillMaxWidth(0.68f).height(26.dp).align(Alignment.BottomCenter).padding(bottom = 6.dp)) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.width / 2f
                    ),
                    size = Size(size.width, size.height)
                )
            }
            Image(
                painter = painterResource(model.photo),
                contentDescription = if (active) "${brand.name} ${bike.displayName}" else null,
                modifier = Modifier.fillMaxWidth(0.96f).heightIn(max = 190.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            val fullTitle = appStringResource(Res.string.garage_bike_title_full, brand.name, bike.displayName)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                V3Icon(path = "feather_icon/fi_gauge.svg", description = null, modifier = Modifier.size(40.dp), tint = TextSecondary)
                Text(text = fullTitle, fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ShowroomDots(bikes: List<Ride>, activeIndex: Int, onSelect: (Int) -> Unit) {
    val reducedMotion = isReducedMotion
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        bikes.forEachIndexed { index, bike ->
            val on = index == activeIndex
            val width by animateDpAsState(
                targetValue = if (on) 18.dp else 7.dp,
                animationSpec = VaultMotion.quickTween(reducedMotion),
                label = "showroomDotWidth"
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab
                    ) {
                        if (!on) onSelect(index)
                    }
                    .semantics {
                        role = Role.Tab
                        selected = on
                        contentDescription = bike.displayName
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = width, height = 7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (on) BrandColor else BorderStrongColor)
                )
            }
        }
    }
}
