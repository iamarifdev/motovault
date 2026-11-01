package com.motovault.app.ui.backup

import androidx.compose.runtime.Composable

enum class BackupNativeFailure { SAVE, SHARE }

@Composable
expect fun BackupNativeActions(
    archiveBytes: ByteArray,
    suggestedFilename: String,
    onSaved: () -> Unit,
    onShareOpened: () -> Unit,
    onError: (BackupNativeFailure) -> Unit
)

@Composable
expect fun BackupFilePicker(
    onFileSelected: (ByteArray, String) -> Unit,
    onError: () -> Unit,
    content: @Composable (openPicker: () -> Unit) -> Unit
)
