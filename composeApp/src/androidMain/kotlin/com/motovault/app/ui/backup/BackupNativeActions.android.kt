package com.motovault.app.ui.backup

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.motovault.app.domain.backup.MvaZipArchive
import com.motovault.app.ui.ButtonKind
import com.motovault.app.ui.VaultButton
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun BackupNativeActions(
    archiveBytes: ByteArray,
    suggestedFilename: String,
    onSaved: () -> Unit,
    onShareOpened: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val savedState by rememberUpdatedState(onSaved)
    val shareState by rememberUpdatedState(onShareOpened)
    val errorState by rememberUpdatedState(onError)

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destination: Uri? ->
        if (destination != null) {
            runCatching {
                requireNotNull(context.contentResolver.openOutputStream(destination)) {
                    "Could not open target location."
                }.use { output ->
                    output.write(archiveBytes)
                    output.flush()
                }
                savedState()
            }.onFailure {
                errorState("ব্যাকআপ ফাইল সেভ করতে ব্যর্থ: ${it.message}")
            }
        }
        // If destination is null, user cancelled; do not call savedState()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        VaultButton(
            text = "ফাইলে সেভ করুন",
            kind = ButtonKind.PRIMARY,
            full = true,
            modifier = Modifier.weight(1f),
            onClick = {
                saveLauncher.launch(suggestedFilename)
            }
        )
        VaultButton(
            text = "শেয়ার করুন",
            kind = ButtonKind.SECONDARY,
            full = true,
            modifier = Modifier.weight(1f),
            onClick = {
                runCatching {
                    val exportDir = File(context.cacheDir, "document-exports")
                    if (!exportDir.exists()) exportDir.mkdirs()
                    val tempFile = File(exportDir, suggestedFilename)
                    tempFile.outputStream().use { it.write(archiveBytes) }

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.documents",
                        tempFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = ClipData.newUri(context.contentResolver, "MotoVault Backup", uri)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "MotoVault ব্যাকআপ পাঠান"))
                    shareState()
                }.onFailure {
                    errorState("শেয়ার করতে সমস্যা হয়েছে: ${it.message}")
                }
            }
        )
    }
}

@Composable
actual fun BackupFilePicker(
    onFileSelected: (ByteArray, String) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (openPicker: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val onSelectedState by rememberUpdatedState(onFileSelected)
    val onErrorState by rememberUpdatedState(onError)

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                var displayName = "motovault-backup.mva"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }

                val output = ByteArrayOutputStream()
                requireNotNull(context.contentResolver.openInputStream(uri)) {
                    "ফাইল খুলতে ব্যর্থ হয়েছে।"
                }.use { input ->
                    val buf = ByteArray(16384)
                    var total = 0L
                    while (true) {
                        val read = input.read(buf)
                        if (read < 0) break
                        total += read
                        require(total <= MvaZipArchive.DEFAULT_MAX_TOTAL_BYTES) {
                            "ব্যাকআপ ফাইলটি সীমার চেয়ে বড় (সর্বোচ্চ ১৫০ মেগাবাইট)।"
                        }
                        output.write(buf, 0, read)
                    }
                }
                onSelectedState(output.toByteArray(), displayName)
            }.onFailure {
                onErrorState("ফাইল লোড করতে ব্যর্থ: ${it.message}")
            }
        }
    }

    content { pickerLauncher.launch("*/*") }
}
