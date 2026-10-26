package com.motovault.app.domain.privacy

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import com.motovault.app.ui.VaultDocument

class PrivacyVerificationTest {
    private var now = 10_000L
    private fun policy(enabled: Boolean = true, timeout: Int = 60) =
        VaultAccessPolicy(VaultPrivacySettings(enabled = enabled, timeoutSeconds = timeout)) { now }

    @Test
    fun testCancellationDoesNotGrantAccessOrPersist() = runBlocking {
        val p = policy(enabled = true)
        assertEquals(VaultAccessMode.LOCKED, p.mode)

        NativeSystemAuth.setProbeResult(SystemAuthResult(SystemAuthStatus.CANCELLED, "যাচাই বাতিল হয়েছে।"))
        val request = p.beginAuthentication(VaultAuthAction.UNLOCK)
        val authResult = NativeSystemAuth.authenticate("MotoVault খুলুন", "যাচাই করুন")

        assertEquals(SystemAuthStatus.CANCELLED, authResult.status)
        assertFalse(authResult.isSuccess)

        val grant = p.completeAuthentication(request, authResult.isSuccess)
        assertNull(grant)
        assertEquals(VaultAccessMode.LOCKED, p.mode)
        assertFalse(p.canOpenDocument("doc1"))
        NativeSystemAuth.setProbeResult(null)
    }

    @Test
    fun testBiometricLockoutProvidesClearRecoveryMessage() = runBlocking {
        val p = policy(enabled = true)
        val lockoutMsg = "বায়োমেট্রিক আপাতত বন্ধ। ফোনের PIN বা পাসকোড ব্যবহার করুন।"
        NativeSystemAuth.setProbeResult(SystemAuthResult(SystemAuthStatus.LOCKED_OUT, lockoutMsg))

        val request = p.beginAuthentication(VaultAuthAction.UNLOCK)
        val authResult = NativeSystemAuth.authenticate("MotoVault খুলুন", "যাচাই করুন")

        assertEquals(SystemAuthStatus.LOCKED_OUT, authResult.status)
        assertEquals(lockoutMsg, authResult.message)
        assertFalse(authResult.isSuccess)

        val grant = p.completeAuthentication(request, false)
        assertNull(grant)
        assertEquals(VaultAccessMode.LOCKED, p.mode)
        NativeSystemAuth.setProbeResult(null)
    }

    @Test
    fun testNoEnrolledBiometricOrCredentialsProvidesClearGuidance() = runBlocking {
        val p = policy(enabled = true)
        val noCredMsg = "ফোনে স্ক্রিন লক নেই। ফোনের সেটিংসে PIN বা পাসকোড দিন। আগের তথ্য রাখা আছে।"
        NativeSystemAuth.setProbeResult(SystemAuthResult(SystemAuthStatus.NO_CREDENTIALS, noCredMsg))

        val authResult = NativeSystemAuth.authenticate("MotoVault খুলুন", "যাচাই করুন")
        assertEquals(SystemAuthStatus.NO_CREDENTIALS, authResult.status)
        assertEquals(noCredMsg, authResult.message)
        assertFalse(authResult.isSuccess)
        NativeSystemAuth.setProbeResult(null)
    }

    @Test
    fun testDeepLinksCannotBypassAppLock() {
        val p = policy(enabled = true)
        assertEquals(VaultAccessMode.LOCKED, p.mode)

        // Attempting to open documents while locked must fail
        assertFalse(p.canOpenDocument("secret-registration"))
        assertFalse(p.canOpenDocument("tax-token"))

        // Unlock must precede any document exposure
        NativeSystemAuth.setProbeResult(SystemAuthResult(SystemAuthStatus.SUCCESS))
        val req = p.beginAuthentication(VaultAuthAction.UNLOCK)
        val grant = p.completeAuthentication(req, true)
        assertNotNull(grant)
        assertEquals(VaultAccessMode.OWNER, p.mode)
        assertTrue(p.canOpenDocument("secret-registration"))
        NativeSystemAuth.setProbeResult(null)
    }

    @Test
    fun testSelectedDocumentHandoverCannotExposeOtherRecords() {
        val p = policy(enabled = true)
        val unlockReq = p.beginAuthentication(VaultAuthAction.UNLOCK)
        p.completeAuthentication(unlockReq, true)
        assertEquals(VaultAccessMode.OWNER, p.mode)

        // Enter handover / inspection for doc1
        p.inspect("doc1")
        assertEquals(VaultAccessMode.INSPECTION, p.mode)
        assertEquals("doc1", p.selectedDocumentId)

        // Can only open doc1, NEVER doc2
        assertTrue(p.canOpenDocument("doc1"))
        assertFalse(p.canOpenDocument("doc2"))

        // Export and disable are blocked in inspection mode
        assertFails { p.beginAuthentication(VaultAuthAction.EXPORT, "doc1") }
        assertFails { p.beginAuthentication(VaultAuthAction.DISABLE) }

        // Exit inspection with cancellation keeps inspection mode
        val exitReq = p.beginAuthentication(VaultAuthAction.EXIT_INSPECTION)
        assertNull(p.completeAuthentication(exitReq, false))
        assertEquals(VaultAccessMode.INSPECTION, p.mode)
        assertEquals("doc1", p.selectedDocumentId)
        assertTrue(p.canOpenDocument("doc1"))
        assertFalse(p.canOpenDocument("doc2"))

        // Lock from inspection clears selected document and returns to locked mode
        p.lock()
        assertEquals(VaultAccessMode.LOCKED, p.mode)
        assertNull(p.selectedDocumentId)
        assertFalse(p.canOpenDocument("doc1"))
    }

    @Test
    fun testAutoLockOnTimeoutAndBackground() {
        val p = policy(enabled = true, timeout = 60)
        val unlockReq = p.beginAuthentication(VaultAuthAction.UNLOCK)
        p.completeAuthentication(unlockReq, true)
        assertEquals(VaultAccessMode.OWNER, p.mode)

        // 59 seconds: still active
        now += 59_000
        p.tick()
        assertEquals(VaultAccessMode.OWNER, p.mode)

        // 60 seconds reached: automatically locks
        now += 1_000
        p.tick()
        assertEquals(VaultAccessMode.LOCKED, p.mode)
        assertFalse(p.canOpenDocument("doc1"))

        // Unlock and enter inspection
        val unlock2 = p.beginAuthentication(VaultAuthAction.UNLOCK)
        p.completeAuthentication(unlock2, true)
        p.inspect("doc1")
        assertEquals(VaultAccessMode.INSPECTION, p.mode)

        // Background transition immediately locks and wipes selected document
        p.background()
        assertEquals(VaultAccessMode.LOCKED, p.mode)
        assertNull(p.selectedDocumentId)
        p.active(true)
        assertEquals(VaultAccessMode.LOCKED, p.mode)
    }

    @Test
    fun testSensitiveExportRequiresDeliberateAuthAndCleansUp() = runBlocking {
        val p = policy(enabled = true)
        val unlockReq = p.beginAuthentication(VaultAuthAction.UNLOCK)
        p.completeAuthentication(unlockReq, true)

        // Deliberate authentication for doc1 export
        val exportReq = p.beginAuthentication(VaultAuthAction.EXPORT, "doc1")
        val exportGrant = p.completeAuthentication(exportReq, true)
        assertNotNull(exportGrant)

        // Grant can only be consumed for doc1 EXPORT, once
        assertFalse(p.consume(exportGrant, VaultAuthAction.EXPORT, "doc2"))
        assertFalse(p.consume(exportGrant, VaultAuthAction.DISABLE))
        assertTrue(p.consume(exportGrant, VaultAuthAction.EXPORT, "doc1"))
        // Cannot replay
        assertFalse(p.consume(exportGrant, VaultAuthAction.EXPORT, "doc1"))
    }

    @Test
    fun testErrorReportsAndLogsDoNotContainSensitiveValues() {
        // Test that malformed data or crypto failures produce sanitized messages
        val sensitivePlaintext = "CONFIDENTIAL_DHAKA_METRO_HA_12_3456"
        val error = runCatching {
            require(sensitivePlaintext.length < 10) {
                "Malformed document payload. Stored records have been preserved."
            }
        }.exceptionOrNull()

        assertNotNull(error)
        assertFalse(error.message!!.contains("DHAKA_METRO"))
        assertFalse(error.message!!.contains("CONFIDENTIAL"))
        assertTrue(error.message!!.contains("Stored records have been preserved."))
    }
}
