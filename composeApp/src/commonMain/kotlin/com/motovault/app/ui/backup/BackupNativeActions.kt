package com.motovault.app.ui.backup

import androidx.compose.runtime.Composable

@Composable
expect fun BackupNativeActions(
    archiveBytes: ByteArray,
    suggestedFilename: String,
    onSaved: () -> Unit,
    onShareOpened: () -> Unit,
    onError: (String) -> Unit
)

@Composable
expect fun BackupFilePicker(
    onFileSelected: (ByteArray, String) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (openPicker: () -> Unit) -> Unit
)
