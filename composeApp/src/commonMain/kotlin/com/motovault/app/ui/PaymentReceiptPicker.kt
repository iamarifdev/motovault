package com.motovault.app.ui

import androidx.compose.runtime.Composable

/**
 * Copies an owner-chosen receipt photo into protected document storage.
 * The callback receives a managed asset ID; nothing is uploaded and no original is replaced.
 */
@Composable
expect fun PaymentReceiptPicker(label: String, onPicked: (String) -> Unit, onError: () -> Unit)
