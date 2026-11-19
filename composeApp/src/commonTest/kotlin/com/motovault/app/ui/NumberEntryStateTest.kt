package com.motovault.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberEntryStateTest {
    @Test
    fun freshFieldReplacesOnFirstDigitThenAppends() {
        val state = NumberEntryState(mapOf("amount" to "500"), "amount")
        state.press("2")
        assertEquals("2", state.values["amount"])
        state.press("5")
        assertEquals("25", state.values["amount"])
    }

    @Test
    fun backspaceClearsFreshFieldOrDropsLastCharAfterEditing() {
        val freshState = NumberEntryState(mapOf("amount" to "500"), "amount")
        freshState.press("back")
        assertEquals("", freshState.values["amount"])

        val editedState = NumberEntryState(mapOf("amount" to "50"), "amount")
        editedState.press("1")
        editedState.press("2")
        editedState.press("3")
        assertEquals("123", editedState.values["amount"])
        editedState.press("back")
        assertEquals("12", editedState.values["amount"])
    }

    @Test
    fun decimalAndDigitConstraintsHold() {
        val state = NumberEntryState(mapOf("amount" to "10"), "amount")
        state.press("back")
        state.press(".")
        assertEquals("0.", state.values["amount"])
        state.press("5")
        state.press(".")
        assertEquals("0.5", state.values["amount"])
        state.press("2")
        state.press("9")
        assertEquals("0.52", state.values["amount"])

        state.set("amount", "0")
        state.press("7")
        assertEquals("7", state.values["amount"])

        state.set("amount", "123456789")
        state.press("0")
        assertEquals("123456789", state.values["amount"])
    }

    @Test
    fun setActiveMarksTargetFreshAndSetClearsFresh() {
        val state = NumberEntryState(mapOf("a" to "100", "b" to "200"), "a")
        state.setActive("b")
        assertEquals("b", state.activeField)
        state.press("9")
        assertEquals("9", state.values["b"])

        state.set("b", "45")
        state.press("6")
        assertEquals("456", state.values["b"])
    }
}
