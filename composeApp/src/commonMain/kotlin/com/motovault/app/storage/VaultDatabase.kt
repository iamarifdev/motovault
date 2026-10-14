package com.motovault.app.storage

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import androidx.room.migration.Migration
import com.motovault.app.domain.model.ComponentRule
import com.motovault.app.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "vehicle")
data class VehicleRow(@PrimaryKey val id: String = "primary", val payload: String)
@Entity(tableName = "schedule")
data class ScheduleRow(@PrimaryKey val id: String, val position: Int, val payload: String)
@Entity(tableName = "ledger")
data class LedgerRow(@PrimaryKey val id: String, val position: Int, val payload: String)
@Entity(tableName = "document")
data class DocumentRow(@PrimaryKey val id: String, val position: Int, val payload: String)
@Entity(tableName = "asset_reference", primaryKeys = ["documentId", "assetId"])
data class AssetReferenceRow(val documentId: String, val assetId: String)
@Entity(tableName = "vault_meta")
data class VaultMetaRow(
    @PrimaryKey val id: Int = 1,
    val appearance: String,
    val legacyImported: Boolean,
    @ColumnInfo(defaultValue = "0") val autoLocation: Boolean = false
)

@Dao
abstract class VaultDao {
    @Query("SELECT * FROM sync_outbox WHERE id = 1") abstract suspend fun pendingSync(): SyncOutboxRow?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putPendingSync(row: SyncOutboxRow)
    @Query("SELECT * FROM vault_meta WHERE id = 1") abstract suspend fun meta(): VaultMetaRow?
    @Query("SELECT * FROM vehicle WHERE id = 'primary'") abstract suspend fun vehicle(): VehicleRow?
    @Query("SELECT * FROM schedule ORDER BY position") abstract suspend fun schedules(): List<ScheduleRow>
    @Query("SELECT * FROM ledger ORDER BY position") abstract suspend fun ledger(): List<LedgerRow>
    @Query("SELECT * FROM document ORDER BY position") abstract suspend fun documents(): List<DocumentRow>
    @Query("SELECT * FROM asset_reference ORDER BY documentId, assetId") abstract suspend fun references(): List<AssetReferenceRow>
    @Query("DELETE FROM vehicle") abstract suspend fun clearVehicle()
    @Query("DELETE FROM schedule") abstract suspend fun clearSchedules()
    @Query("DELETE FROM ledger") abstract suspend fun clearLedger()
    @Query("DELETE FROM document") abstract suspend fun clearDocuments()
    @Query("DELETE FROM asset_reference") abstract suspend fun clearReferences()
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putVehicle(row: VehicleRow)
    @Insert abstract suspend fun putSchedules(rows: List<ScheduleRow>)
    @Insert abstract suspend fun putLedger(rows: List<LedgerRow>)
    @Insert abstract suspend fun putDocuments(rows: List<DocumentRow>)
    @Insert abstract suspend fun putReferences(rows: List<AssetReferenceRow>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putMeta(row: VaultMetaRow)

    @Transaction
    open suspend fun read(): VaultState? {
        val settings = meta() ?: return null
        val logs = ledger().map { row -> codec.decodeFromString<RideLog>(row.payload).also { check(it.id == row.id) { "Ledger record does not match its stored ID." } } }
        val docs = documents().map { row -> codec.decodeFromString<VaultDocument>(row.payload).also { check(it.id == row.id) { "Document record does not match its stored ID." } } }
        check(references().toSet() == assetRows(docs).toSet()) { "Document asset references could not be read. Existing storage has been preserved." }
        return VaultState(
            ride = vehicle()?.let { codec.decodeFromString<Ride>(it.payload) },
            rules = schedules().map { row -> codec.decodeFromString<ComponentRule>(row.payload).also { check(it.id == row.id) { "Schedule does not match its stored ID." } } },
            logs = logs, documents = docs, appearance = Appearance.valueOf(settings.appearance), autoLocation = settings.autoLocation,
            economy = VaultState.calculateEconomy(logs), lastFuelOdometer = logs.filter { it.isFuel }.maxOfOrNull { it.odometer }
        )
    }

    @Transaction
    open suspend fun replace(state: VaultState, legacyImported: Boolean? = null) {
        val previousState = read()
        val previousSync = pendingSync()
        val imported = legacyImported ?: meta()?.legacyImported ?: false
        require(state.rules.map { it.id }.distinct().size == state.rules.size) { "Duplicate schedule IDs." }
        require(state.logs.map { it.id }.distinct().size == state.logs.size) { "Duplicate ledger IDs." }
        require(state.documents.map { it.id }.distinct().size == state.documents.size) { "Duplicate document IDs." }
        val refs = assetRows(state.documents)
        clearReferences(); clearDocuments(); clearLedger(); clearSchedules(); clearVehicle()
        state.ride?.let { putVehicle(VehicleRow(payload = codec.encodeToString(it))) }
        putSchedules(state.rules.mapIndexed { index, rule -> ScheduleRow(rule.id, index, codec.encodeToString(rule)) })
        putLedger(state.logs.mapIndexed { index, entry -> LedgerRow(entry.id, index, codec.encodeToString(entry)) })
        putDocuments(state.documents.mapIndexed { index, document -> DocumentRow(document.id, index, codec.encodeToString(document)) })
        putReferences(refs)
        putMeta(VaultMetaRow(appearance = state.appearance.name, legacyImported = imported, autoLocation = state.autoLocation))
        putPendingSync(LocalSyncProtocol.next(previousSync, previousState, state, recordId()))
    }

    @Transaction
    open suspend fun prepareLocalSync() {
        if (pendingSync() != null) return
        val state = read() ?: return
        putPendingSync(LocalSyncProtocol.next(null, null, state, recordId()))
    }

    @Transaction
    open suspend fun recordSyncFailure(expectedRevision: Long, now: Long, code: String): Boolean {
        val pending = pendingSync() ?: return false
        if (pending.revision != expectedRevision) return false
        putPendingSync(LocalSyncProtocol.failed(pending, now, code))
        return true
    }

    @Transaction
    open suspend fun migrate(legacy: String?): VaultState {
        read()?.let { return it }
        val state = legacy?.let {
            requireNotNull(VaultState.decode(it)) { "Stored records could not be read. Existing storage has been preserved." }
        } ?: VaultState()
        replace(state, legacyImported = legacy != null)
        return requireNotNull(read())
    }

    companion object {
        private val codec = Json { ignoreUnknownKeys = true }
        private fun assetRows(documents: List<VaultDocument>) = documents.flatMap { document ->
            document.assetIds().map { asset -> validateDocumentAssetId(asset); AssetReferenceRow(document.id, asset) }
        }
    }
}

@Database(entities = [VehicleRow::class, ScheduleRow::class, LedgerRow::class, DocumentRow::class, AssetReferenceRow::class, VaultMetaRow::class, SyncOutboxRow::class], version = 3, exportSchema = true)
@ConstructedBy(VaultDatabaseConstructor::class)
abstract class VaultDatabase : RoomDatabase() { abstract fun vault(): VaultDao }

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object VaultDatabaseConstructor : RoomDatabaseConstructor<VaultDatabase> {
    override fun initialize(): VaultDatabase
}

fun buildVaultDatabase(builder: RoomDatabase.Builder<VaultDatabase>): VaultDatabase = builder
    .addMigrations(VaultMigration1To2, VaultMigration2To3)
    .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()

object VaultMigration1To2 : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sync_outbox` (`id` INTEGER NOT NULL, `installationId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `payload` TEXT NOT NULL, `tombstones` TEXT NOT NULL, `attemptCount` INTEGER NOT NULL, `retryAfterEpochMillis` INTEGER NOT NULL, `failureCode` TEXT, PRIMARY KEY(`id`))")
    }
}

object VaultMigration2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `vault_meta` ADD COLUMN `autoLocation` INTEGER NOT NULL DEFAULT 0")
    }
}
