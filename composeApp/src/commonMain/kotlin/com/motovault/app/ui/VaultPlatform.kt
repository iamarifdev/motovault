package com.motovault.app.ui

import com.motovault.app.domain.vault.VaultState
import com.motovault.app.domain.model.DOCUMENT_RENEWAL_WARNING_DAYS

expect suspend fun loadVault(): String?
expect suspend fun saveVault(value: String)
expect fun activeVaultDatabase(): com.motovault.app.storage.VaultDatabase

typealias VaultSave = (VaultState, (Boolean) -> Unit) -> Unit

fun today() = com.motovault.app.domain.model.today()
fun recordId() = com.motovault.app.domain.model.recordId()
const val RENEWAL_WARNING_DAYS = DOCUMENT_RENEWAL_WARNING_DAYS
