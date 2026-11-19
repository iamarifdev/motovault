package com.motovault.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motovault.app.domain.model.isCardDocument
import kotlinx.coroutines.delay
import motovault.composeapp.generated.resources.Res
import motovault.composeapp.generated.resources.doc_add_continue_no_photo
import motovault.composeapp.generated.resources.doc_add_hint_edges
import motovault.composeapp.generated.resources.doc_add_hint_flat
import motovault.composeapp.generated.resources.doc_add_hint_glare
import motovault.composeapp.generated.resources.doc_add_hint_steady
import motovault.composeapp.generated.resources.doc_add_keep_front_only
import motovault.composeapp.generated.resources.doc_add_scan_back_prompt
import motovault.composeapp.generated.resources.doc_add_shutter_desc

private val ViewfinderBgDark = Color(0xFF0D1210)
private val ViewfinderCornerColor = Color(0xFF7EE2A7)
private val ViewfinderGoodPillBg = Color(0xFF1F7A52)

@Composable
internal fun DocPhotoStep(
    docKind: DocumentKindInfo,
    captureBack: Boolean,
    busy: Boolean,
    errorMessage: String?,
    onCaptureBusyChange: (Boolean) -> Unit,
    onCameraPageCaptured: (DocumentPage) -> Unit,
    onImported: (CapturedDocument) -> Unit,
    onError: (String) -> Unit,
    onKeepFrontOnly: () -> Unit,
    onSkipPhoto: () -> Unit
) {
    val haptic = currentHaptic
    val isCard = isCardDocument(docKind.kind)
    var quality by remember { mutableStateOf(CaptureQuality()) }
    var captureRequest by remember(docKind.kind, captureBack) { mutableStateOf(0) }
    var hintIndex by remember(docKind.kind, captureBack) { mutableStateOf(0) }
    var flashVisible by remember { mutableStateOf(false) }

    LaunchedEffect(docKind.kind, captureBack) {
        while (true) {
            delay(1100L)
            hintIndex = (hintIndex + 1) % 4
        }
    }
    LaunchedEffect(flashVisible) {
        if (flashVisible) {
            delay(220L)
            flashVisible = false
        }
    }

    val hintRes = when {
        captureBack -> Res.string.doc_add_scan_back_prompt
        hintIndex == 0 -> Res.string.doc_add_hint_flat
        hintIndex == 1 -> Res.string.doc_add_hint_glare
        hintIndex == 2 -> Res.string.doc_add_hint_edges
        else -> Res.string.doc_add_hint_steady
    }
    val goodHint = quality.canCapture || hintIndex == 3

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DocViewfinderBox(
            docKind = docKind.kind,
            isCard = isCard,
            captureBack = captureBack,
            captureRequest = captureRequest,
            hintText = appStringResource(hintRes),
            goodHint = goodHint,
            flashVisible = flashVisible,
            onQuality = { quality = it },
            onCaptured = { page ->
                flashVisible = true
                onCaptureBusyChange(false)
                haptic.perform(HapticKind.SUCCESS)
                onCameraPageCaptured(page)
            },
            onError = { msg ->
                onCaptureBusyChange(false)
                onError(msg)
            }
        )

        DocPhotoActionsColumn(
            docKindTitle = docKind.title,
            captureBack = captureBack,
            canShoot = quality.canCapture && !busy,
            errorMessage = errorMessage,
            onShutterClick = {
                haptic.perform(HapticKind.TICK)
                onCaptureBusyChange(true)
                captureRequest += 1
            },
            onImported = onImported,
            onError = onError,
            onKeepFrontOnly = onKeepFrontOnly,
            onSkipPhoto = onSkipPhoto
        )
    }
}

@Composable
private fun DocPhotoActionsColumn(
    docKindTitle: String,
    captureBack: Boolean,
    canShoot: Boolean,
    errorMessage: String?,
    onShutterClick: () -> Unit,
    onImported: (CapturedDocument) -> Unit,
    onError: (String) -> Unit,
    onKeepFrontOnly: () -> Unit,
    onSkipPhoto: () -> Unit
) {
    errorMessage?.let { msg ->
        Text(text = msg, fontSize = 13.sp, color = ErrorColor, textAlign = TextAlign.Center)
    }
    val shutterLabel = appStringResource(Res.string.doc_add_shutter_desc)
    Box(
        modifier = Modifier
            .size(76.dp)
            .alpha(if (canShoot) 1f else 0.45f)
            .clip(CircleShape)
            .border(4.dp, BrandColor, CircleShape)
            .clickable(enabled = canShoot, role = Role.Button, onClick = onShutterClick)
            .semantics { contentDescription = shutterLabel },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(BrandColor))
    }
    DocumentImportActions(
        type = docKindTitle,
        backOnly = captureBack,
        onResult = onImported,
        onError = onError
    )
    if (captureBack) {
        DocStepTextLink(text = appStringResource(Res.string.doc_add_keep_front_only), onClick = onKeepFrontOnly)
    }
    DocStepTextLink(text = appStringResource(Res.string.doc_add_continue_no_photo), onClick = onSkipPhoto)
}

@Composable
private fun DocViewfinderBox(
    docKind: String,
    isCard: Boolean,
    captureBack: Boolean,
    captureRequest: Int,
    hintText: String,
    goodHint: Boolean,
    flashVisible: Boolean,
    onQuality: (CaptureQuality) -> Unit,
    onCaptured: (DocumentPage) -> Unit,
    onError: (String) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val vfHeight = if (isCard) 250.dp else 340.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(vfHeight)
            .clip(shape)
            .background(
                Brush.radialGradient(colors = listOf(Color(0xFF26332C), ViewfinderBgDark))
            )
    ) {
        key(docKind, captureBack) {
            DocumentCamera(
                modifier = Modifier.fillMaxSize(),
                captureRequest = captureRequest,
                onQuality = onQuality,
                onCaptured = onCaptured,
                onError = onError
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 22.dp.toPx()
            val arm = 34.dp.toPx()
            val stroke = 4.dp.toPx()
            val right = size.width - inset
            val bottom = size.height - inset
            drawLine(ViewfinderCornerColor, Offset(inset, inset), Offset(inset + arm, inset), stroke)
            drawLine(ViewfinderCornerColor, Offset(inset, inset), Offset(inset, inset + arm), stroke)
            drawLine(ViewfinderCornerColor, Offset(right, inset), Offset(right - arm, inset), stroke)
            drawLine(ViewfinderCornerColor, Offset(right, inset), Offset(right, inset + arm), stroke)
            drawLine(ViewfinderCornerColor, Offset(inset, bottom), Offset(inset + arm, bottom), stroke)
            drawLine(ViewfinderCornerColor, Offset(inset, bottom), Offset(inset, bottom - arm), stroke)
            drawLine(ViewfinderCornerColor, Offset(right, bottom), Offset(right - arm, bottom), stroke)
            drawLine(ViewfinderCornerColor, Offset(right, bottom), Offset(right, bottom - arm), stroke)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (goodHint) ViewfinderGoodPillBg else Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(text = hintText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (flashVisible) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)))
        }
    }
}

@Composable
private fun DocStepTextLink(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandColor)
    }
}
