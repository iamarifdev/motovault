package com.motovault.app.domain.privacy

import kotlin.test.*
import kotlinx.serialization.json.Json
import com.motovault.app.ui.VaultSettings
import com.motovault.app.ui.VaultState

class VaultAccessPolicyTest {
    private var now = 1_000L
    private fun policy(enabled: Boolean = true) = VaultAccessPolicy(VaultPrivacySettings(enabled = enabled)) { now }
    private fun unlock(policy: VaultAccessPolicy): VaultAuthGrant = requireNotNull(policy.completeAuthentication(policy.beginAuthentication(VaultAuthAction.UNLOCK), true))

    @Test fun coldStartCancellationAndInactiveCompletionNeverGrantAccess() {
        val p = policy()
        assertFalse(p.canOpenDocument("a"))
        assertNull(p.completeAuthentication(p.beginAuthentication(VaultAuthAction.UNLOCK), false))
        val request = p.beginAuthentication(VaultAuthAction.UNLOCK)
        p.active(false)
        assertNull(p.completeAuthentication(request, true))
        assertFalse(p.canOpenDocument("a"))
        p.active(true)
        assertNotNull(p.completeAuthentication(request, true))
    }

    @Test fun explicitLockBackgroundAndReplacementRequestsRejectStaleCallbacks() {
        val p = policy()
        val first = p.beginAuthentication(VaultAuthAction.UNLOCK)
        val second = p.beginAuthentication(VaultAuthAction.UNLOCK)
        assertNull(p.completeAuthentication(first, true))
        p.background(); p.active(true)
        assertNull(p.completeAuthentication(second, true))
        val third = p.beginAuthentication(VaultAuthAction.UNLOCK)
        p.lock()
        assertNull(p.completeAuthentication(third, true))
    }

    @Test fun authenticationAndGrantsExpireAndCannotBeReusedOrRetargeted() {
        val p = policy()
        val expired = p.beginAuthentication(VaultAuthAction.UNLOCK)
        now += 30_000
        assertNull(p.completeAuthentication(expired, true))
        unlock(p)
        val export = requireNotNull(p.completeAuthentication(p.beginAuthentication(VaultAuthAction.EXPORT, "chosen"), true))
        assertFalse(p.consume(export, VaultAuthAction.EXPORT, "other"))
        assertFalse(p.consume(export, VaultAuthAction.DISABLE))
        assertTrue(p.consume(export, VaultAuthAction.EXPORT, "chosen"))
        assertFalse(p.consume(export, VaultAuthAction.EXPORT, "chosen"))
        val late = requireNotNull(p.completeAuthentication(p.beginAuthentication(VaultAuthAction.EXPORT, "chosen"), true))
        now += 30_000
        assertFalse(p.consume(late, VaultAuthAction.EXPORT, "chosen"))
    }

    @Test fun selectedDocumentCannotExposeAnotherDocumentOrExport() {
        val p = policy(); unlock(p); p.inspect("chosen")
        assertTrue(p.canOpenDocument("chosen"))
        assertFalse(p.canOpenDocument("other"))
        assertFails { p.beginAuthentication(VaultAuthAction.EXPORT, "chosen") }
        assertFails { p.beginAuthentication(VaultAuthAction.DISABLE) }
        assertNull(p.completeAuthentication(p.beginAuthentication(VaultAuthAction.EXIT_INSPECTION), false))
        assertEquals(VaultAccessMode.INSPECTION, p.mode)
        assertNotNull(p.completeAuthentication(p.beginAuthentication(VaultAuthAction.EXIT_INSPECTION), true))
        assertTrue(p.canOpenDocument("other"))
        assertNull(p.selectedDocumentId)
    }

    @Test fun idleTimeoutAndBackgroundRemoveTheSelectedDocument() {
        val p = policy(); unlock(p)
        now += 59_999; assertTrue(p.canOpenDocument("a"))
        now++; assertFalse(p.canOpenDocument("a"))
        unlock(p); p.inspect("chosen")
        p.background(); p.active(true)
        assertEquals(VaultAccessMode.LOCKED, p.mode)
        assertNull(p.selectedDocumentId)
        unlock(p); p.inspect("chosen")
        now += 60_000; p.tick()
        assertNull(p.selectedDocumentId)
    }

    @Test fun activityExtendsOnlyALiveSessionAndOwnedCredentialHandoffIsBounded() {
        val p = policy(); unlock(p)
        now += 50_000; p.activity(); now += 20_000
        assertTrue(p.canOpenDocument("a"))
        val request = p.beginAuthentication(VaultAuthAction.EXPORT, "a")
        p.background(preserveOwnedSystemAuthentication = true)
        assertFalse(p.canOpenDocument("a"))
        p.active(true)
        val grant = requireNotNull(p.completeAuthentication(request, true))
        assertTrue(p.consume(grant, VaultAuthAction.EXPORT, "a"))
        val stale = p.beginAuthentication(VaultAuthAction.EXPORT, "a")
        p.background(preserveOwnedSystemAuthentication = true); now += 30_000; p.active(true)
        assertNull(p.completeAuthentication(stale, true))
        p.activity(); assertEquals(VaultAccessMode.LOCKED, p.mode)
    }

    @Test fun enablingAndDisablingRequireAppropriateFreshProof() {
        val p = policy(false)
        val enable = requireNotNull(p.completeAuthentication(p.beginAuthentication(VaultAuthAction.ENABLE), true))
        assertTrue(p.settingsCommitted(VaultPrivacySettings(enabled = true), enable))
        assertEquals(VaultAccessMode.OWNER, p.mode)
        assertFalse(p.settingsCommitted(VaultPrivacySettings(enabled = false)))
        assertTrue(p.settings.enabled)
        assertEquals(VaultAccessMode.LOCKED, p.mode)
        unlock(p)
        val disable = requireNotNull(p.completeAuthentication(p.beginAuthentication(VaultAuthAction.DISABLE), true))
        assertTrue(p.settingsCommitted(VaultPrivacySettings(enabled = false), disable))
        assertFalse(p.settings.enabled)
    }

    @Test fun onlyPreferencesRoundTripNeverAuthorizationOrSelectedScope() {
        val setting = VaultPrivacySettings(enabled = true, timeoutSeconds = 300)
        val state = VaultSettings.from(VaultState(privacy = setting)).applyTo(VaultState())
        assertEquals(setting, state.privacy)
        val restored = Json.decodeFromString<VaultPrivacySettings>(Json.encodeToString(VaultPrivacySettings.serializer(), setting))
        assertEquals(VaultAccessMode.LOCKED, VaultAccessPolicy(restored) { now }.mode)
        assertFails { Json.decodeFromString<VaultPrivacySettings>("{\"version\":99}") }
        assertFails { VaultPrivacySettings(timeoutSeconds = 1) }
        assertFalse(Json.decodeFromString<VaultPrivacySettings>("{}").enabled)
    }
}
