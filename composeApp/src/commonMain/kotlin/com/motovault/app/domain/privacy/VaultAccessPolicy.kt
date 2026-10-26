package com.motovault.app.domain.privacy

import kotlinx.serialization.Serializable

@Serializable
data class VaultPrivacySettings(val version: Int = 1, val enabled: Boolean = false, val timeoutSeconds: Int = 60) {
    init {
        require(version == 1) { "Unsupported privacy settings. Existing records have been preserved." }
        require(timeoutSeconds in setOf(60, 300, 900)) { "Choose a supported lock timeout." }
    }
}

internal enum class VaultAccessMode { LOCKED, OWNER, INSPECTION }
internal enum class VaultAuthAction { UNLOCK, ENABLE, DISABLE, EXIT_INSPECTION, EXPORT }
internal data class VaultAuthRequest(val id: Long, val generation: Long, val action: VaultAuthAction, val documentId: String?, val expiresAt: Long)
internal data class VaultAuthGrant(val request: VaultAuthRequest, val expiresAt: Long)

/** Pure access rules. Only a native OS-authentication bridge may report successful verification. */
internal class VaultAccessPolicy(initial: VaultPrivacySettings, private val elapsedMillis: () -> Long) {
    var settings = initial; private set
    var mode = if (initial.enabled) VaultAccessMode.LOCKED else VaultAccessMode.OWNER; private set
    var selectedDocumentId: String? = null; private set
    var foreground = true; private set
    var generation = 0L; private set
    private var ownerDeadline: Long? = null
    private var requestSequence = 0L
    private var pending: VaultAuthRequest? = null
    private val grants = mutableMapOf<Long, VaultAuthGrant>()

    fun beginAuthentication(action: VaultAuthAction, documentId: String? = null): VaultAuthRequest {
        tick()
        check(foreground) { "Return to the app before verifying." }
        check(when (action) {
            VaultAuthAction.UNLOCK -> settings.enabled && mode == VaultAccessMode.LOCKED
            VaultAuthAction.ENABLE -> !settings.enabled && mode == VaultAccessMode.OWNER
            VaultAuthAction.DISABLE -> settings.enabled && mode == VaultAccessMode.OWNER
            VaultAuthAction.EXIT_INSPECTION -> mode == VaultAccessMode.INSPECTION
            VaultAuthAction.EXPORT -> mode == VaultAccessMode.OWNER && !documentId.isNullOrBlank()
        }) { "This action is outside the current access scope." }
        grants.clear()
        return VaultAuthRequest(++requestSequence, generation, action, documentId, elapsedMillis() + 30_000L).also { pending = it }
    }

    fun completeAuthentication(request: VaultAuthRequest, successful: Boolean): VaultAuthGrant? {
        tick()
        if (!foreground || pending != request || request.generation != generation || elapsedMillis() >= request.expiresAt) return null
        pending = null
        if (!successful) return null
        mode = VaultAccessMode.OWNER
        selectedDocumentId = null
        ownerDeadline = if (settings.enabled) elapsedMillis() + settings.timeoutSeconds * 1000L else null
        return VaultAuthGrant(request, elapsedMillis() + 30_000L).also { grants[request.id] = it }
    }

    fun consume(grant: VaultAuthGrant, action: VaultAuthAction, documentId: String? = null): Boolean {
        tick()
        if (!foreground || mode != VaultAccessMode.OWNER || grants[grant.request.id] != grant || grant.request.generation != generation ||
            elapsedMillis() >= grant.expiresAt || grant.request.action != action || grant.request.documentId != documentId) return false
        grants.remove(grant.request.id)
        return true
    }

    /** Invoke only after the preference transaction succeeds. An unapproved disable fails closed. */
    fun settingsCommitted(next: VaultPrivacySettings, authorization: VaultAuthGrant? = null): Boolean {
        tick()
        if (next == settings) return true
        val enabling = !settings.enabled && next.enabled
        val disabling = settings.enabled && !next.enabled
        val approvedEnable = enabling && authorization?.let { consume(it, VaultAuthAction.ENABLE) } == true
        val approvedDisable = disabling && authorization?.let { consume(it, VaultAuthAction.DISABLE) } == true
        if (disabling && !approvedDisable) { lock(); return false }
        val keepOwner = foreground && mode == VaultAccessMode.OWNER && !enabling
        settings = next
        lock()
        if (!next.enabled || approvedEnable || keepOwner) {
            mode = VaultAccessMode.OWNER
            ownerDeadline = if (next.enabled) elapsedMillis() + next.timeoutSeconds * 1000L else null
        }
        return true
    }

    fun inspect(documentId: String) {
        tick()
        check(settings.enabled && foreground && mode == VaultAccessMode.OWNER && documentId.isNotBlank()) { "Unlock before choosing a document." }
        generation++
        pending = null; grants.clear()
        mode = VaultAccessMode.INSPECTION
        selectedDocumentId = documentId
        ownerDeadline = elapsedMillis() + settings.timeoutSeconds * 1000L
    }

    fun canOpenDocument(documentId: String): Boolean {
        tick()
        return foreground && (mode == VaultAccessMode.OWNER || (mode == VaultAccessMode.INSPECTION && documentId == selectedDocumentId))
    }

    fun activity() {
        tick()
        if (foreground && settings.enabled && mode != VaultAccessMode.LOCKED) ownerDeadline = elapsedMillis() + settings.timeoutSeconds * 1000L
    }

    /** Inactive native-auth overlays are covered; a true background transition uses background(). */
    fun active(value: Boolean) {
        foreground = value
        tick()
        if (value && !settings.enabled && mode == VaultAccessMode.LOCKED) mode = VaultAccessMode.OWNER
    }

    fun background(preserveOwnedSystemAuthentication: Boolean = false) {
        tick()
        foreground = false
        val preserve = preserveOwnedSystemAuthentication && pending != null
        lock(invalidateAuthentication = !preserve)
    }

    fun lock(invalidateAuthentication: Boolean = true) {
        mode = VaultAccessMode.LOCKED; selectedDocumentId = null; ownerDeadline = null; grants.clear()
        if (invalidateAuthentication) { generation++; pending = null }
    }

    fun tick() {
        val now = elapsedMillis()
        if (ownerDeadline?.let { now >= it } == true) { lock(); return }
        if (pending?.let { now >= it.expiresAt } == true) pending = null
        grants.entries.removeAll { now >= it.value.expiresAt }
    }
}
