package com.motovault.app

import com.motovault.app.storage.BackupOperationsAdapter
import com.motovault.app.storage.security.documentSha256

/** Probe fixtures never read the production asset store. */
internal val syntheticBackupOperations = BackupOperationsAdapter(::documentSha256) { error("Synthetic asset unavailable") }
