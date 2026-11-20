package com.motovault.app.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay

/**
 * Keyboard settings per field kind (review N2): the right keyboard, sentence or word capitals,
 * and an IME action that moves on or closes the keyboard.
 */
internal object FieldKeyboards {
    /** Free text such as notes: sentence capitals, "Done" closes the keyboard. */
    val sentence = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
    /** Multi-line text: sentence capitals, the return key adds a new line. */
    val multiline = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text)
    /** Names of people, shops and workshops. */
    val name = KeyboardOptions(capitalization = KeyboardCapitalization.Words, keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
    /** Search boxes show the search key. */
    val search = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search)
    /** Document numbers: letters and digits, no auto-correct. */
    val code = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrect = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done)
    /** Whole numbers such as km and cc. */
    val number = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    /** Money and other decimals. */
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
    /** Phone numbers. */
    val phone = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)

    fun password(last: Boolean) = KeyboardOptions(
        autoCorrect = false,
        keyboardType = KeyboardType.Password,
        imeAction = if (last) ImeAction.Done else ImeAction.Next
    )
}

/** "Done" and "Search" close the keyboard; "Next" moves focus to the next field. */
@Composable
internal fun rememberFieldKeyboardActions(): KeyboardActions {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focus, keyboard) {
        KeyboardActions(
            onDone = { focus.clearFocus() },
            onSearch = { keyboard?.hide() },
            onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }
        )
    }
}

/** Focuses the field once when it first appears, after the sheet has started to open (v4 autoFocus). */
@Composable
internal fun Modifier.autoFocusOnOpen(enabled: Boolean = true): Modifier {
    val requester = remember { FocusRequester() }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        delay(AUTO_FOCUS_DELAY_MS)
        runCatching { requester.requestFocus() }
    }
    return focusRequester(requester)
}

private const val AUTO_FOCUS_DELAY_MS = 250L
