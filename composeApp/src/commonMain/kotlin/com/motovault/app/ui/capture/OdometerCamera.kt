package com.motovault.app.ui

import androidx.compose.runtime.Composable

/** Ephemeral preview and OCR text. Neither acquisition adapter writes owner records. */
data class OdometerPhoto(val preview: ByteArray, val text: String)
enum class OdometerCameraIssue { DENIED, UNAVAILABLE, FAILED, CANCELLED }

@Composable
expect fun rememberOdometerCamera(
    onResult: (OdometerPhoto) -> Unit,
    onIssue: (OdometerCameraIssue) -> Unit
): () -> Unit
