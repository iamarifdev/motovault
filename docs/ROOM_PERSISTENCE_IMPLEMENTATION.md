# Room persistence

Implemented 12 October 2026 in the active MotoVault path.

## Storage and migration

The shared Room version 1 schema separates vehicle identity, ordered component schedules, ordered ledger entries, ordered documents, document asset references and vault metadata. Record payloads use the existing serializable contracts, including inline legacy Base64 attachments. Generated schema history is in `composeApp/schemas/`.

Android opens `motovault.db` through `Room.databaseBuilder(context, "motovault.db")`, using `Context.getDatabasePath("motovault.db")`, normally `databases/motovault.db`. Its retained legacy JSON remains `filesDir/motovault.json`. iOS uses Application Support `motovault.db`. Both use Room 2.7.2 with bundled SQLite 2.5.2. Kotlin 2.1.20 and matching KSP 2.1.20-1.0.32 generate constructors for each target.

The first load imports the existing atomic JSON file, or the older preferences when no JSON file exists, in one transaction. The existing JSON file, preferences and original document files remain unchanged. A complete database takes precedence over stale legacy storage. Corrupt legacy JSON does not mark migration complete; a failed transaction leaves the earlier valid database intact. Corrupt database payloads or mismatched asset references produce an explicit load failure and do not fall back to older data.

Fuel economy and the last fuel odometer are recalculated from the preserved ledger, matching the existing restore contract. Migration retains document IDs, ordered pages, original/accepted/preview asset IDs, source PDFs, confirmed details, optional extraction candidates and capture metadata.

Android also checks for the legacy AtomicFile `.bak` and lets `readFully()` recover that committed backup before considering preferences. An unfinished `.new` file is not treated as committed data. This recovery path was source checked and compiled; it still needs Android runtime evidence because local JVM tests do not execute Android `AtomicFile` implementations.

## Save ordering

Initial loading and database operations run asynchronously. Failed restoration prevents writes. While saving, a modal prevents navigation or duplicate actions. The durable transaction completes before visible state changes, form dismissal or file cleanup. A failed write keeps the earlier state and draft.

Cleanup subtracts every asset referenced by the new complete document collection from the previous collection. Deleting one document therefore does not delete assets still referenced by another document. Cleanup failure leaves unused files rather than invalidating the saved database. Callback failures are separate from durable-write failure reporting.

## Verification

The Phase 2 combined `assembleDebug`, `compileKotlinIosArm64`, `testDebugUnitTest` and `iosSimulatorArm64Test` run on 12 October passed after the backup recovery fix and setup review changes. That historical run contained 59 Android tests and 66 iOS simulator tests with no failures or errors.

Subsequent work adds the explicit Room version 1→2 migration and transactional outbox described in [LOCAL_SYNC_PREPARATION.md](LOCAL_SYNC_PREPARATION.md). [RENEWAL_RUNTIME.md](continuation-verification/RENEWAL_RUNTIME.md) records fresh Android v2 update/restart/replay verification against the retained [Oct13 asset baseline](continuation-verification/OCT13_ASSET_BASELINE.json). The current combined suite contains 95 Android and 112 iOS native-simulator tests with zero failures/errors; see [CONTINUATION_PROGRESS.md](CONTINUATION_PROGRESS.md). Those totals supersede this section's historical Phase 2 totals, while final iOS host runtime verification remains pending.

Seven iOS simulator tests execute real bundled SQLite databases and cover repeatable legacy import and database reopen, retry after corrupt JSON, rollback across all tables after an insert-aborting trigger, shared asset references and bike replacement, stale stored economy recalculation, damaged asset-reference refusal, and row/payload ID mismatch refusal without overwrite.

These Phase 2 tests establish native SQLite behavior and database reopen, not Android activity/process lifecycle or iOS camera/host operation. Subsequent [Android runtime evidence](continuation-verification/RUNTIME_PHASE_1_2.md) records installation, migration from the existing owner vault and process-kill/relaunch; the linked Oct13 report separately records fresh v2 preservation checks. Migration preserves legacy Base64 inline; converting those attachments into managed files remains a future recoverable migration. This Phase 2 report does not establish remote sync, encryption migration or orphan recovery; later local sync and reversible document quarantine are documented in their own implementation reports.
