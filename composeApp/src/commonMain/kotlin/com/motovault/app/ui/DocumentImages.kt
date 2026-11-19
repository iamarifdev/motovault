@file:OptIn(ExperimentalResourceApi::class, ExperimentalEncodingApi::class)
package com.motovault.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_image_opening
import motovault.composeapp.generated.resources.doc_image_original_desc
import motovault.composeapp.generated.resources.doc_image_page_desc
import motovault.composeapp.generated.resources.doc_image_preview_failed
import motovault.composeapp.generated.resources.doc_image_unavailable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable expect fun DocumentWindowAppearance(dark: Boolean)
expect fun documentDialogProperties(): DialogProperties

@Composable
internal fun DocumentCopy(text: String) {
    Text(documentDisplayName(text), color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp)
}

@Composable
internal fun DocumentNote(text: String) {
    Text(documentDisplayName(text), color = TextSecondary, fontSize = 10.sp, lineHeight = 15.sp)
}

@Composable
internal fun DocumentSecondary(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(11.dp),
        modifier = modifier.heightIn(min = 44.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceColor, contentColor = TextPrimary)
    ) {
        Text(documentDisplayName(text), fontSize = 12.sp)
    }
}

@Composable
internal fun DocumentLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(vertical = 8.dp)) {
        Text(documentDisplayName(text), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
internal fun DocumentImage(
    page: DocumentPage,
    modifier: Modifier = Modifier,
    accepted: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit
) {
    val id = if (accepted) page.acceptedId ?: page.previewId else page.previewId
    var failed by remember(id) { mutableStateOf(false) }
    val bitmap by produceState<ImageBitmap?>(null, id) {
        val loaded = runCatching { readDocumentAsset(id).decodeToImageBitmap() }
        value = loaded.getOrNull()
        failed = loaded.isFailure
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = appStringResource(Res.string.doc_image_page_desc, documentSideName(page.side)),
            modifier = modifier.fillMaxWidth(),
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier.fillMaxWidth().height(85.dp), contentAlignment = Alignment.Center) {
            DocumentNote(
                appStringResource(if (failed) Res.string.doc_image_preview_failed else Res.string.doc_image_opening)
            )
        }
    }
}

@Composable
internal fun LegacyDocumentImage(
    value: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap = remember(value) { runCatching { Base64.decode(value).decodeToImageBitmap() }.getOrNull() }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = appStringResource(Res.string.doc_image_original_desc),
            modifier = modifier.fillMaxWidth(),
            contentScale = contentScale
        )
    } else {
        Box(modifier = modifier.fillMaxWidth().height(85.dp), contentAlignment = Alignment.Center) {
            DocumentNote(appStringResource(Res.string.doc_image_unavailable))
        }
    }
}
