package com.motovault.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests DTO compatibility using the exact generated synthetic fixture inputs, not a future Room migration. */
class P00CompatibilityTest {
    @Test
    fun pairedFixtureInputsDecodeAndRoundTripWithoutLosingRecordIdentity() {
        for ((name, payload) in P00FixtureData.cases) {
            val first = assertNotNull(VaultState.decode(payload), name)
            val second = assertNotNull(VaultState.decode(VaultState.encode(first)), name)
            assertEquals(first, second, name)
            assertEquals(first.logs.map { it.id }, second.logs.map { it.id }, name)
            assertEquals(first.documents.map { it.id }, second.documents.map { it.id }, name)
        }
    }

    @Test
    fun partialFillFixtureRetainsPartialFlagAndCompletedCycleEconomy() {
        val state = assertNotNull(VaultState.decode(P00FixtureData.cases.getValue("partial-fill")))
        assertEquals(4, state.logs.size)
        assertEquals(1, state.logs.count { !it.fullTank })
        assertEquals(450.0 / 9.9, assertNotNull(state.economy), 0.00001)
    }

    @Test
    fun metadataDocumentsHaveStableSyntheticIdsAndNoOwnerAssetReferences() {
        val state = assertNotNull(VaultState.decode(P00FixtureData.cases.getValue("documents")))
        assertEquals(listOf("p00-doc-tax", "p00-doc-licence", "p00-doc-registration"), state.documents.map { it.id })
        assertTrue(state.documents.all { it.pages.isEmpty() && it.originalPdf == null })
    }

    @Test
    fun unknownHistoryCannotYetBeDistinguishedFromEmptyRules() {
        val empty = assertNotNull(VaultState.decode(P00FixtureData.cases.getValue("empty")))
        val unknown = assertNotNull(VaultState.decode(P00FixtureData.cases.getValue("unknown-service")))
        assertEquals(empty.rules, unknown.rules)
        assertTrue(unknown.rules.isEmpty())
    }

    @Test
    fun legacyMissingFieldsReceiveCompatibleDefaultsOnFirstDecode() {
        val state = assertNotNull(VaultState.decode(P00FixtureData.cases.getValue("legacy-minimal")))
        assertEquals("P00 synthetic", assertNotNull(state.ride).edition)
        assertTrue(state.logs.single().fullTank)
        assertTrue(state.logs.single().id.isNotBlank())
        assertEquals(Digits.BN, state.digits)
        assertFalse(state.autoLocation)
    }

    @Test
    fun currentDtoPreservesSettingsAssetsAndExactServiceAmount() {
        val state = assertNotNull(VaultState.decode(P00FixtureData.cases.getValue("current-all-fields")))
        assertEquals(Digits.EN, state.digits)
        assertTrue(state.reduceMotion)
        assertEquals(1.3f, state.textScale)
        val page = state.documents.first().pages.single()
        assertEquals("p00-original.png", page.originalId)
        assertEquals("p00-accepted.png", page.acceptedId)
        assertEquals("p00-preview.png", page.previewId)
        assertEquals(160000, state.logs.last().serviceTotalPoisha)
    }

    @Test
    fun malformedJsonFailsWithoutSeedingAnOwnerVault() {
        assertNull(VaultState.decode("{"))
    }
}
