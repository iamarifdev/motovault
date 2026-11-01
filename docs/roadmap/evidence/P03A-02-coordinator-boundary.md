# P03A-02 coordinator boundary

Source baseline: `860c3ec04dac73bc10d6aba65798aff31887fdbb`; this is an uncommitted working-tree build, not the clean commit. Coordinator SHA-256 `66e5b04eb6bd84111cd3e627d90f5fe06db88fc413b23266f46eb1210e940e27`; app composition `814a2ce8115fef9385bd710b589001d53d09977b71a13c834c1b46c3a9ef554e`; compatibility adapter `7fc218f4863e8b8585db31cc109eae02f31d50b47904b56a45224b1c27cc611f`; coordinator tests `e3790977109ee7ca4ba7ac5bfa9f9bd4d3152ace1e380ce79bcc10387206de86`.

## Scope

`VaultCoordinator<T>` and `VaultWriter<T>` live in the application package and have no UI, Room, Koin, platform or storage imports. `AppComposition` constructs one app-lifetime session writer; the existing storage adapter supplies the current serialized `VaultState`, asset operations and reminder reconciliation. The UI receives the writer explicitly. Backup receives its revision from that writer instead of a separate global lookup. The existing storage format, database and original-asset representation were not migrated.

Subsequent tracing found that the original `RestoreUseCase.executeRestore` directly invoked `database.vault().replace` before notifying the session, so the task was reopened. The later changes below route restore through the coordinator and retain this earlier finding as history.

This is the first application boundary, not a claim that all existing domain code is clean. Existing UI-owned persisted types and backup infrastructure dependencies remain transitional; P03A-04 and subsequent feature migrations own those changes.

## Verification

- Offline Android and iOS simulator-source compilation passed with `:composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64` using JDK 17 and the existing Gradle cache.
- `:composeApp:testDebugUnitTest --offline` passed, including three new coordinator tests covering stale revisions, durable Undo, concurrent and failed saves, and retained asset cleanup. This is a JVM test result, not Android runtime proof.
- Focused `:composeApp:iosSimulatorArm64Test --tests com.motovault.app.VaultSessionTest --tests com.motovault.app.application.VaultCoordinatorTest --tests com.motovault.app.ui.P00CompatibilityTest --offline` passed: 8 session, 3 coordinator and 7 compatibility tests. The authorized iOS 27 iPhone 18 Pro Max simulator was the only booted simulator, verified as UDID `50709DF0-FEA9-4B34-9DE6-6172D868875D`.
- The compatibility tests cover legacy missing fields, record identity, settings and asset-reference representation; the session tests cover failed recovery, save/Undo and retained originals. No database migration or archive format was changed by this task.

## Unresolved wider test failure

The unfiltered iOS simulator suite ran 201 tests and failed 8 backup crypto tests because `IosBackupCryptoRegistry.bridge` was not initialized. The Kotlin/Native test runner does not run the Swift app delegate that registers the bridge. This has not been established as a new regression or as proof of production behavior. The focused coordinator suite above passed, but the full iOS suite did not. P03A-04 must investigate and verify the real iOS backup bridge and recovery behavior; no fake crypto or ignored failures were introduced.

## Follow-up: coordinated restore

The updated uncommitted source routes restore through `VaultBackupRestorer` and `VaultCoordinator.commitPrepared`, using the revision shown during preview. The coordinator reserves the writer before staging and rejects concurrent/stale writes. The legacy staging adapter validates the archive before staging referenced files; protected storage reuses byte-identical existing files and refuses different bytes under the same ID. New files are tracked for cleanup if preparation or the durable database write fails. Room replacement is performed through the regular writer, and Undo keeps the previous state and referenced assets. The target database's existing `legacyImported` marker is retained rather than replacing local migration history with the archive's marker.

For this second verification, the 18-file source/test subset fingerprint is `f4c27e3ab801004290ec63cd5bdf550fcb340f959e9a438017284bb6f767040e` (SHA-256 over each listed relative path, a zero byte, its file bytes, and another zero byte, in this order):

1. `application/VaultCoordinator.kt`, `application/VaultBackupRestorer.kt`, `storage/VaultSession.kt`, `storage/CoordinatedBackupRestore.kt`, `storage/BackupRestoreStaging.kt` under `composeApp/src/commonMain/kotlin/com/motovault/app/`.
2. `storage/security/SecureAssetStore.kt`, `storage/security/ProtectedDocumentStorage.kt` under that same common source root; the Android and iOS `storage/security/ProtectedDocumentStorage` actual files.
3. `domain/backup/BackupFormat.kt`, `domain/backup/BackupUseCase.kt`, `domain/backup/RestoreUseCase.kt`, `ui/backup/BackupSheet.kt`, `ui/MotoVault.kt` under that same common source root.
4. `composeApp/src/commonTest/kotlin/com/motovault/app/application/VaultCoordinatorTest.kt`, `composeApp/src/androidUnitTest/kotlin/com/motovault/app/domain/backup/RestoreStagingTest.kt`, `composeApp/src/androidUnitTest/kotlin/com/motovault/app/storage/security/SecureAssetStoreTest.kt`, and `composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/NativeAssetInstrumentation.kt`.

- Offline Android unit tests and iOS source compilation passed after the changes. Unit tests exercise stale revisions, concurrent saves, failed database writes, cancellation after a commit starts, asset reuse and cleanup of newly staged files.
- Focused iOS 27 simulator tests passed: 6 coordinator, 8 session, 7 compatibility and 12 Room database tests. The verified booted device was iPhone 18 Pro Max `50709DF0-FEA9-4B34-9DE6-6172D868875D`.
- The isolated Android 17 Pixel 10 probe passed 23 synthetic native storage checks, including real encrypted staging, same-byte reuse, conflict refusal and Room restore through the coordinator. Its first runs exposed fixture setup problems (the database was not yet loaded, and a previous lost-key test had intentionally deleted its key); those fixtures were corrected and the passing run used a separate fresh synthetic key. The primary app package was not cleared.

Limits: these checks do not demonstrate a full iOS backup restore through the Swift crypto bridge, physical-device recovery or a clean unfiltered iOS suite. A same-ID asset with different bytes is refused rather than overwritten; handling that case while preserving both originals remains P03A-04 work. A process interruption during staging can leave unreferenced protected files; recovery and cleanup evidence remain P03A-04 gates.
# Follow-up: ordinary save cancellation

An ordinary save previously propagated owner-scope cancellation after persistence began without clearing `saving` or publishing the write result. The coordinator now completes the durable write and publishes success or failure in a non-cancellable section, as it already did for prepared restores. `cancellationDuringOrdinarySaveStillPublishesDurableResult` cancels during a suspended synthetic write and checks the durable value, snapshot, revision, saving flag and callback. The Android unit suite passed, and all seven focused coordinator tests passed on the booted iOS 27 iPhone 18 Pro Max simulator. This is not physical-device or process-death evidence.

The uncommitted coordinator source SHA-256 is `8f9cb67fd71e5036d5441103fab018a444aca6f21cfda5ae113312e06ec212e9`; test SHA-256 is `efc57f3e14bb55ad66e7c0674b38d9968016466d7bf59bd502863441c732820b`. Earlier source fingerprints in this report identify earlier checks, not this follow-up.

A further regression test checks a failed write after cancellation: it reports failure, clears `saving`, and preserves the prior state and revision. The Android suite and all eight focused iOS coordinator tests passed after adding it. The newer test file SHA-256 is `ad41b351832cfff4f7a751453fee0b915d30ead4a6f8feffb2571abc556de372`.

The coordinator was subsequently changed to expose typed save/restore failures rather than user-facing text or raw restore exception messages. The Android unit suite and nine focused iOS coordinator tests passed, including a synthetic restore exception test. Previous source and test fingerprints document earlier runs; the failure screen itself still needs runtime verification.

