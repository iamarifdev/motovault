package com.motovault.app.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaunchConfigTest {
    @Test
    fun syntheticSeedRequiresExplicitProbeAuthorizationAndIsConsumedOnce() {
        LaunchConfig.applyArgs(listOf("--seed"))
        assertFalse(LaunchConfig.initialSeed)
        LaunchConfig.applyArgs(listOf("--seed"), allowSyntheticSeed = true)
        assertTrue(LaunchConfig.initialSeed)
        assertTrue(LaunchConfig.consumeSeed())
        assertFalse(LaunchConfig.consumeSeed())
        LaunchConfig.applyArgs(listOf("--seed"), allowSyntheticSeed = false)
        assertFalse(LaunchConfig.initialSeed)
    }
}
