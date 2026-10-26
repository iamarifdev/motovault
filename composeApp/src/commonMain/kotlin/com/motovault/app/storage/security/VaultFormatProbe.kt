package com.motovault.app.storage.security

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.motovault.app.storage.VaultDatabase
import com.motovault.app.ui.*

/** Called only by isolated native probes with a newly allocated synthetic database path. */
internal suspend fun verifyVaultFormatMigration(path: String, open: () -> VaultDatabase) {
    val original = VaultState.create(Ride("yamaha", "Synthetic schema fixture", 2026, 1000.0))
        .logFuel(1000.0, 5.0).copy(digits = Digits.EN, docReminders = false, reduceMotion = true,
            documents = listOf(VaultDocument("Tax token", "SYNTHETIC-SCHEMA", "SYNTHETIC OWNER", "2030-01-01", id = "synthetic-document",
                pages = listOf(DocumentPage("synthetic-original.jpg", "synthetic-preview.jpg", "image/jpeg", 80, 40, "Front")))))
    val seeded = open()
    val pending = try { seeded.vault().replace(original); requireNotNull(seeded.vault().pendingSync()) } finally { seeded.close() }
    // Reconstruct the exact previous meta shape and Room identity after seeding synthetic rows.
    BundledSQLiteDriver().open(path).use { connection ->
        connection.execSQL("ALTER TABLE vault_meta DROP COLUMN assetFormatVersion")
        connection.execSQL("UPDATE room_master_table SET identity_hash = 'c1132b50953dd810ad66179e86703edd' WHERE id = 42")
        connection.execSQL("PRAGMA user_version = 4")
    }
    val upgraded = open()
    try {
        check(upgraded.vault().read() == original)
        check(upgraded.vault().pendingSync() == pending)
        check(upgraded.vault().references().size == 2)
        check(upgraded.vault().meta()?.assetFormatVersion == 1)
    } finally { upgraded.close() }
    val reopened = open()
    try { check(reopened.vault().read() == original); check(reopened.vault().pendingSync() == pending) } finally { reopened.close() }
    val preservedPayload = BundledSQLiteDriver().open(path).use { connection ->
        connection.execSQL("UPDATE vault_meta SET assetFormatVersion = 99 WHERE id = 1")
        connection.prepare("SELECT payload FROM document WHERE id = 'synthetic-document'").use { query -> check(query.step()); query.getText(0) }
    }
    val unsupported = open()
    try {
        var refused = false
        try { unsupported.vault().replace(original.copy(documents = emptyList())) } catch (_: Exception) { refused = true }
        check(refused)
    } finally { unsupported.close() }
    BundledSQLiteDriver().open(path).use { connection ->
        connection.prepare("SELECT payload FROM document WHERE id = 'synthetic-document'").use { query -> check(query.step()); check(query.getText(0) == preservedPayload) }
        connection.execSQL("UPDATE vault_meta SET assetFormatVersion = 1 WHERE id = 1")
        connection.execSQL("PRAGMA user_version = 6")
    }
    // Current Room must refuse an unsupported future schema without a destructive fallback.
    val future = open()
    try {
        var refused = false
        try { future.vault().read() } catch (_: Exception) { refused = true }
        check(refused)
    } finally { future.close() }
    BundledSQLiteDriver().open(path).use { connection ->
        connection.prepare("PRAGMA user_version").use { query -> check(query.step()); check(query.getLong(0) == 6L) }
        connection.prepare("SELECT payload FROM document WHERE id = 'synthetic-document'").use { query -> check(query.step()); check(query.getText(0) == preservedPayload) }
    }
}
