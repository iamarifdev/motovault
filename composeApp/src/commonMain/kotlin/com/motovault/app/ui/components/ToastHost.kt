package com.motovault.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal data class SheetToastBinding(
    val toast: ToastData? = null,
    val onDismiss: () -> Unit = {}
)

internal val LocalSheetToast = compositionLocalOf { SheetToastBinding() }

@Composable
internal fun VaultToastHost(
    toast: ToastData?,
    locked: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(start = 12.dp, end = 12.dp, bottom = 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        ToastLayer(
            toast = if (locked) null else toast,
            onDismiss = onDismiss
        )
    }
}
