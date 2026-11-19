package com.motovault.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.history_jump_top

@Composable
internal fun JumpToTopPill(listState: LazyListState) {
    val haptic = currentHaptic
    val reducedMotion = isReducedMotion
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(999.dp)

    Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(SurfaceColor)
                .border(1.dp, BorderStrongColor, shape)
                .clickable(role = Role.Button) {
                    haptic.perform(HapticKind.TICK)
                    scope.launch {
                        if (reducedMotion) listState.scrollToItem(0) else listState.animateScrollToItem(0)
                    }
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            V3Icon(path = "feather_icon/fi_arrow-up.svg", description = null, modifier = Modifier.size(15.dp), tint = TextPrimary)
            Text(text = appStringResource(Res.string.history_jump_top), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}
