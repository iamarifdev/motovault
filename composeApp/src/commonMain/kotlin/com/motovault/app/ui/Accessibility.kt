package com.motovault.app.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.invisibleToUser

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.excludeFromAccessibility() = clearAndSetSemantics { invisibleToUser() }
