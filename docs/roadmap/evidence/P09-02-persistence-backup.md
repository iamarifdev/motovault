# P09-02: Bike-Scoped Persistence & Backward-Compatible Backups

- Task: `P09-02` ("Implement bike-scoped persistence and backward-compatible backups")
- Status: Complete (`verification`, `implementation`)

## 1. Domain & Ownership Model

- Added explicit bike and scope fields in [VaultRecords.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/domain/model/VaultRecords.kt):
  - `Ride.id` (default `"primary"`), `Ride.archived` (default `false`)
  - `RideLog.vehicleId` (default `"primary"`)
  - `VaultDocument.vehicleId` (default `"primary"`), `VaultDocument.isOwnerScoped` (default `false`, `true` for `licence`)
  - `GarageBikeRecord(ride, maintenanceRules, logs, documents)` stored in `VaultState.garageBikes` and `VaultSettings.garageBikes`.
- Created [BikeOwnership.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/domain/garage/BikeOwnership.kt):
  - `allBikes()`, `activeBikes()`, `allDocuments()`, `allRules()`, `allLogs()`, `allDocumentAssetIds()`, `activeReminderDocuments()`, `mapDocumentsAcrossGarage()`
  - `switchToBike(bikeId)`, `addBike(newRide, newRules)`, `setBikeArchived(bikeId, archived)`, `mutateBike(targetBikeId, transform)`
  - Preserves owner-scoped documents (`isOwnerScoped` / `category == "licence"`) across bike switches and blocks archiving the last active bike or mutating an archived bike.

## 2. Room v6 Persistence & Asset Retention

- Preserved `@Database(version = 6)` in [VaultDatabase.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt) so existing single-bike Room v6 fixtures and `VaultFormatProbe` v7 rejection stay 100% compatible.
- `VaultDatabase` persists non-active `garageBikes` records in `settings` (`garageBikes`) and mirrors every bike in the `vehicle` table while tracking `state.allDocuments()` in `asset_reference`.
- Updated `VaultSession.kt`, `DocumentFlow.kt`, `PrivacySheet.kt`, `LocalSync.kt`, `RecoverySnapshotStore.kt`, and `CoordinatedRecovery.kt` to use `allDocumentAssetIds()` / `allDocuments()` so document photos for inactive or archived bikes are never deleted during pruning, recovery, or bike switching.

## 3. Backward-Compatible `.mvbk` Backup & Restore

- Added defaulted multi-bike fields to backup schemas (`BackupRide`, `BackupLog`, `BackupDocument`, `BackupPreferences.garageBikes`) and adapters (`BackupRideLogAdapter`, `BackupDocumentAdapter`, `BackupPreferencesAdapter`, `BackupCurrentSummaryAdapter`).
- Updated `BackupUseCase`, `RestoreUseCase`, and `BackupRestoreStaging` so `.mvbk` exports include all garage bikes (`allBikes()`), all garage logs/documents, and all garage asset files (`allDocumentAssetIds()`), while older v1 single-bike `.mvbk` archives restore cleanly with default `primary` IDs.

## 4. Verification

- `./gradlew :composeApp:iosSimulatorArm64Test`: 56 test files, 325 tests, 0 failed, 0 skipped (exit code 0), including:
  - `BikeOwnershipTest` (switching, archive/restore guards, owner-scoped licence retention)
  - `VaultDatabaseTest.multiBikeStateAndGarageAssetReferencesPersistAcrossReopenAndSwitch`
  - `BackupOperationsAdapterTest.multiBikeBackupIncludesNonActiveGarageAssetsAndArchivedBikes`
- `python3 tools/responsibility_gate.py`: passed (`Responsibility baseline matches (2 files, 7 functions)`).
