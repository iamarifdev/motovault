package com.motovault.app

import androidx.room.Room
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.motovault.app.storage.*
import com.motovault.app.ui.*
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.*

class VaultDatabaseTest {
    private fun path() = NSTemporaryDirectory() + "motovault-test-${recordId()}.db"
    private fun open(path: String) = buildVaultDatabase(Room.databaseBuilder<VaultDatabase>(name = path))
    private fun fixture(): VaultState {
        val state = VaultState.create(Ride("ktm", "Legacy imported edition", 2023, 1000.0))
            .logFuel(1000.0, 5.0).logFuel(1100.0, 2.0, fullTank = false).logFuel(1200.0, 3.0)
        val document = VaultDocument("Registration", "TEST-001", "Test Owner", "", id = "document-1",
            frontPhoto = "legacy-base64-kept", detailsConfirmed = true, details = mapOf("chassis" to "TEST-CHASSIS"),
            pages = listOf(DocumentPage("raw-1.jpg", "preview-1.jpg", "image/jpeg", 120, 80, "Front", acceptedId = "accepted-1.jpg")))
        return state.copy(documents = listOf(document), appearance = Appearance.LIGHT)
    }

    @Test fun versionOneDatabaseMigratesWithoutReplacingOwnerRows() = runBlocking {
        val location = path()
        val original = fixture()
        BundledSQLiteDriver().open(location).use { connection ->
            connection.execSQL("CREATE TABLE vehicle (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
            listOf("schedule", "ledger", "document").forEach { table ->
                connection.execSQL("CREATE TABLE $table (id TEXT NOT NULL PRIMARY KEY, position INTEGER NOT NULL, payload TEXT NOT NULL)")
            }
            connection.execSQL("CREATE TABLE asset_reference (documentId TEXT NOT NULL, assetId TEXT NOT NULL, PRIMARY KEY(documentId, assetId))")
            connection.execSQL("CREATE TABLE vault_meta (id INTEGER NOT NULL PRIMARY KEY, appearance TEXT NOT NULL, legacyImported INTEGER NOT NULL)")
            connection.execSQL("INSERT INTO vault_meta VALUES(1, 'LIGHT', 1)")
            connection.prepare("INSERT INTO vehicle VALUES('primary', ?)").use { statement ->
                statement.bindText(1, kotlinx.serialization.json.Json.encodeToString(Ride.serializer(), original.ride!!)); statement.step()
            }
            fun insert(table: String, id: String, position: Int, payload: String) {
                connection.prepare("INSERT INTO $table VALUES(?, ?, ?)").use { statement ->
                    statement.bindText(1, id); statement.bindLong(2, position.toLong()); statement.bindText(3, payload); statement.step()
                }
            }
            original.rules.forEachIndexed { index, rule -> insert("schedule", rule.id, index,
                kotlinx.serialization.json.Json.encodeToString(com.motovault.app.domain.model.ComponentRule.serializer(), rule)) }
            original.logs.forEachIndexed { index, log -> insert("ledger", log.id, index,
                kotlinx.serialization.json.Json.encodeToString(RideLog.serializer(), log)) }
            original.documents.forEachIndexed { index, document ->
                insert("document", document.id, index, kotlinx.serialization.json.Json.encodeToString(VaultDocument.serializer(), document))
                document.assetIds().forEach { asset ->
                    connection.prepare("INSERT INTO asset_reference VALUES(?, ?)").use { statement ->
                        statement.bindText(1, document.id); statement.bindText(2, asset); statement.step()
                    }
                }
            }
            connection.execSQL("PRAGMA user_version = 1")
        }
        val db = open(location)
        assertEquals(original, db.vault().read())
        assertTrue(db.vault().meta()!!.legacyImported)
        assertNull(db.vault().pendingSync())
        db.vault().prepareLocalSync()
        val pending = db.vault().pendingSync()!!
        assertEquals(1L, pending.revision)
        db.close()
        val reopened = open(location)
        assertEquals(pending, reopened.vault().pendingSync())
        assertEquals(original, reopened.vault().read())
        reopened.close()
    }

    @Test fun outboxAndOwnerWriteRollBackTogetherAndSurviveReopen() = runBlocking {
        val location = path()
        val db = open(location)
        val original = fixture()
        db.vault().replace(original)
        val first = db.vault().pendingSync()!!
        db.useWriterConnection { connection ->
            connection.usePrepared("CREATE TRIGGER reject_sync BEFORE INSERT ON sync_outbox BEGIN SELECT RAISE(ABORT, 'simulated queue failure'); END") { it.step() }
        }
        assertFails { db.vault().replace(original.copy(documents = emptyList())) }
        assertEquals(original, db.vault().read())
        assertEquals(first, db.vault().pendingSync())
        db.close()
        val reopened = open(location)
        assertEquals(original, reopened.vault().read())
        assertEquals(first, reopened.vault().pendingSync())
        reopened.close()
    }

    @Test fun coalescedQueueRejectsStaleFailureAndPreservesIdentity() = runBlocking {
        val location = path()
        val db = open(location)
        val original = fixture()
        db.vault().replace(original)
        val first = db.vault().pendingSync()!!
        db.vault().replace(original.copy(documents = emptyList()))
        val second = db.vault().pendingSync()!!
        assertEquals(first.installationId, second.installationId)
        assertEquals(first.revision + 1, second.revision)
        assertFalse(db.vault().recordSyncFailure(first.revision, 100, "network"))
        assertTrue(db.vault().recordSyncFailure(second.revision, 100, "network"))
        db.close()
        val reopened = open(location)
        assertEquals(1, reopened.vault().pendingSync()!!.attemptCount)
        assertEquals(1100L, reopened.vault().pendingSync()!!.retryAfterEpochMillis)
        assertEquals(second.tombstones, reopened.vault().pendingSync()!!.tombstones)
        reopened.close()
    }

    @Test fun legacyMigrationIsRepeatableAndSurvivesDatabaseReopen() = runBlocking {
        val location = path()
        val original = fixture()
        val db = open(location)
        assertEquals(original, db.vault().migrate(VaultState.encode(original)))
        assertEquals(original, db.vault().migrate("invalid stale source"))
        assertTrue(db.vault().meta()!!.legacyImported)
        assertEquals(3, db.vault().references().size)
        db.close()
        val reopened = open(location)
        assertEquals(original, reopened.vault().read())
        reopened.close()
    }

    @Test fun corruptLegacyDoesNotMarkMigrationComplete() = runBlocking {
        val db = open(path())
        assertFailsWith<IllegalArgumentException> { db.vault().migrate("{broken") }
        assertNull(db.vault().read())
        val original = fixture()
        assertEquals(original, db.vault().migrate(VaultState.encode(original)))
        db.close()
    }

    @Test fun failedInsertRollsBackEveryTable() = runBlocking {
        val db = open(path())
        val original = fixture()
        db.vault().replace(original)
        db.useWriterConnection { connection ->
            connection.usePrepared("CREATE TRIGGER reject_ledger BEFORE INSERT ON ledger BEGIN SELECT RAISE(ABORT, 'simulated disk write failure'); END") { it.step() }
        }
        assertFails { db.vault().replace(original.copy(ride = original.ride!!.copy(odometer = 9999.0), documents = emptyList(), appearance = Appearance.DARK)) }
        assertEquals(original, db.vault().read())
        db.close()
    }

    @Test fun sharedAssetsRemainReferencedAndBikeReplacementKeepsDocuments() = runBlocking {
        val db = open(path())
        val original = fixture()
        val second = original.documents.single().copy(id = "document-2")
        db.vault().replace(original.copy(documents = original.documents + second))
        db.vault().replace(original.copy(documents = listOf(second)))
        assertEquals(setOf("document-2"), db.vault().references().map { it.documentId }.toSet())
        val replacement = VaultState.create(Ride("honda", "", 2024, 0.0)).copy(documents = listOf(second), appearance = Appearance.LIGHT)
        db.vault().replace(replacement)
        assertEquals(replacement, db.vault().read())
        db.close()
    }

    @Test fun migrationUsesFuelHistoryInsteadOfStaleStoredEconomy() = runBlocking {
        val db = open(path())
        val original = fixture()
        val migrated = db.vault().migrate(VaultState.encode(original.copy(economy = 38.5, lastFuelOdometer = 99999.0)))
        assertEquals(original.economy, migrated.economy)
        assertEquals(1200.0, migrated.lastFuelOdometer)
        db.close()
    }

    @Test fun damagedAssetReferencesDoNotFallBackToStaleLegacyRecords() = runBlocking {
        val db = open(path())
        val original = fixture()
        db.vault().migrate(VaultState.encode(original))
        db.vault().clearReferences()
        assertFailsWith<IllegalStateException> { db.vault().migrate(VaultState.encode(original.copy(documents = emptyList()))) }
        assertEquals(1, db.vault().documents().size)
        assertTrue(db.vault().meta()!!.legacyImported)
        db.close()
    }

    @Test fun documentPayloadWithDifferentIdIsRejectedWithoutOverwrite() = runBlocking {
        val db = open(path())
        val original = fixture()
        db.vault().replace(original)
        db.useWriterConnection { connection ->
            connection.usePrepared("UPDATE document SET id = 'damaged-id'") { it.step() }
        }
        assertFailsWith<IllegalStateException> { db.vault().migrate(VaultState.encode(original)) }
        assertEquals("damaged-id", db.vault().documents().single().id)
        db.close()
    }
}
