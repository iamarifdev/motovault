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
internal fun OnboardingBrandStep(
    selectedBrandId: String?,
    onSelectBrand: (String) -> Unit,
    onDismiss: (() -> Unit)?
) {
    val f = currentFmt
    val haptic = currentHaptic

                // Step 0: "কোন বাইক চালান?"
                // EXCEPTION: uses bike brand logos aligning prototype!
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = appStringResource(Res.string.onboarding_brand_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = appStringResource(Res.string.onboarding_brand_subtitle), fontSize = 15.sp, color = TextSecondary)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        worlds.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { w ->
                                    val isSelected = selectedBrandId == w.id
                                    val count = BikeCatalogue.forBrand(w.id).size
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) BrandTintColor else SurfaceColor)
                                            .border(1.5.dp, if (isSelected) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                                            .clickable {
                                                haptic.perform(HapticKind.TICK)
                                                onSelectBrand(w.id)
                                            }
                                            .padding(vertical = 14.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Brand vector logo (honoring user exception)
                                        Image(
                                            painter = painterResource(w.logo()),
                                            contentDescription = w.name,
                                            modifier = Modifier.width(52.dp).height(28.dp),
                                            contentScale = ContentScale.Fit,
                                            colorFilter = ColorFilter.tint(if (isSelected) BrandColor else TextPrimary)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(text = w.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text(text = appStringResource(Res.string.onboarding_model_count, f.d(count.toString())), fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    if (onDismiss != null) {
                        VaultButton(
                            text = appStringResource(Res.string.action_cancel),
                            kind = ButtonKind.SECONDARY,
                            onClick = onDismiss,
                            full = true
                        )
                    }
                    Text(
                        text = appStringResource(Res.string.onboarding_privacy_note),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }
            }

@Composable
internal fun OnboardingModelStep(
    selectedBrand: BrandWorld?,
    brandModels: List<BikeModel>,
    filteredModels: List<BikeModel>,
    selectedModelId: String?,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onNext: () -> Unit,
    onManual: () -> Unit
) {
    val haptic = currentHaptic

                // Step 1: "{Brand} এর কোনটি?"
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = appStringResource(Res.string.onboarding_model_title, selectedBrand?.name.orEmpty()), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Search box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceColor2)
                            .border(1.5.dp, BorderStrongColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            V3Icon(path = "feather_icon/fi_search.svg", description = null, modifier = Modifier.size(20.dp), tint = TextTertiary)
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearch,
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(BrandColor),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text(appStringResource(Res.string.onboarding_search_placeholder), fontSize = 15.sp, color = TextTertiary)
                                    inner()
                                }
                            )
                        }
                    }

                    // Model list
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredModels.forEach { m ->
                            val isSelected = selectedModelId == m.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) BrandTintColor else SurfaceColor)
                                    .border(1.5.dp, if (isSelected) BrandColor else BorderColor, RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.perform(HapticKind.TICK)
                                        onSelectModel(m.id)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 104.dp, height = 68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.radialGradient(listOf(LocalVaultPalette.current.photoWellStart, LocalVaultPalette.current.photoWellEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(painter = painterResource(m.photo), contentDescription = null, modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
                                }
                                Text(text = m.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier.size(30.dp).clip(CircleShape).background(BrandColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓", color = OnBrandColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            onManual()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(appStringResource(Res.string.onboarding_manual_model), color = BrandColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    VaultButton(
                        text = if (selectedModelId != null) appStringResource(Res.string.onboarding_continue_with, brandModels.firstOrNull { it.id == selectedModelId }?.name.orEmpty())
                        else appStringResource(Res.string.onboarding_choose_model),
                        full = true,
                        enabled = selectedModelId != null,
                        onClick = onNext
                    )
                }
            }
