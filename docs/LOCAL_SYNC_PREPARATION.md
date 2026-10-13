# Local sync preparation

User scope: prepare local sync now; Supabase project/account details later. This phase adds local preparation, not working backup. No account is assigned, credentials are stored, upload worker is installed, or connected status is shown. Local database files and the outbox payload are plaintext app-private storage. Encryption and recoverable backup are not implemented.

## Delivered local behavior

Room database version 2 adds a singleton `sync_outbox`. The explicit additive `VaultMigration1To2` preserves version 1 tables; no destructive migration fallback exists. Loading an existing vault seeds its queue once. Every successful owner save replaces the relational records and coalesced snapshot in the same Room transaction. A queue failure therefore fails the owner save too. Installation ID is generated once, is unassigned to any account, and survives coalescing/reopen. Local revisions increase monotonically; overflow fails the transaction. The snapshot preserves owner records, document originals' managed IDs and legacy Base64 values. Fuel display remains derived from ledger history when reading Room.

Deleted document, ledger, schedule and asset IDs accumulate as sorted tombstones; IDs restored before connection leave the tombstone set. Assets still referenced by any document are never marked deleted. Replacing the bike retains the existing document behavior. The queue references only current assets and does not pin deleted originals: no remote reader exists in this phase. Existing save-before-asset-cleanup ordering remains required. Neither retry nor queue creation deletes assets.

Retry metadata is durable and limited to five non-sensitive error codes. Delay doubles from one second to one hour and attempts cap at 30. A failure for an old revision cannot modify a newer snapshot. Conflict/authentication/encryption failures are explicit codes, not proof that automatic retry is safe; a future sender must pause for owner action or approved reconciliation as appropriate. Persisted row invariants reject malformed retry metadata.

## Future transport contract and required gates

`EncryptedVaultTransport` is an unimplemented seam accepting ciphertext, owner/vault identity, key version, remote revision precondition and immutable encrypted object IDs. It does not accept the plaintext `SyncOutboxRow` directly. No encryption implementation or sender is provided.

Before connecting: select account/authentication method, establish owner consent and immutable account-to-vault binding, review authenticated encryption and associated-data binding, key storage/rotation/recovery, threat model and restore flow. A public publishable key is client configuration; service-role keys must never be in the app. Logout/account switching must stop workers and prevent assigning the same local records to another owner without explicit review.

Future publication must freeze a revision, validate/read all originals, encrypt and upload immutable assets, then atomically compare the expected remote revision and commit ciphertext plus an idempotent operation receipt. Operation ID is `installationId:revision`; server receipts must reject reuse with different content. A stale remote revision returns conflict without overwriting either side. The owner must choose a reviewed reconciliation; do not merge opaque ciphertext or use client-clock last-write-wins. Restore verifies/decrypts every referenced asset before committing local records.

Future acknowledgment must retain installation identity, monotonic local revision and durable remote baseline outside a cleared queue. This phase intentionally exposes no acknowledgment/delete API. Removing the singleton now would reset identity/revision and is unsafe. An acknowledgment of revision N must not remove revision N+1. Tombstones must remain until an accepted receipt/reconciliation establishes their disposition. Remote object deletion follows acknowledged manifests with a recovery/grace policy, never local cleanup alone. Missing uploads and conflicts retain the frozen encrypted asset set until safely abandoned; unreferenced uploads require separate server cleanup.

## Backend preparation

[local-sync-preparation.sql](backend/local-sync-preparation.sql) is an unexecuted SQL review artifact. It proposes ciphertext-only snapshot/receipt tables, owner-scoped read RLS, no direct client table write grant, and a private Storage bucket with owner-prefix read/insert policies. No overwrite or deletion policy is granted. A future owner-checked publication transaction/RPC is required and deliberately absent. Existing policies in a real project must be audited because permissive policies combine. Run this only in an explicitly approved project after the encryption/account design and security review.

Primary references checked: [Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security) for authenticated owner policies and explicit grants; [Storage access control](https://supabase.com/docs/guides/storage/security/access-control) for private bucket object policies. Live account A/B isolation, SQL execution, bucket limits, RPC concurrency/idempotency and recoverable restore are future validation gates. This SQL has not been executed or integration-tested.

## Verification

Common protocol tests cover coalescing/identity/revision, legacy payload preservation, tombstone delete/recreate, shared assets, retry limits/reset, safe error codes, revision overflow and malformed persisted retry bounds. iOS bundled-SQLite tests exercise a real version 1 database containing vehicle, schedules, fuel ledger, documents, original references and legacy Base64 upgraded by Room to version 2 and reopened, queue-trigger failure rolling back owner records and queue, and stale-revision retry guards across reopen.

13 October 2026 fresh verification with JDK 17: `:composeApp:assembleDebug`, `:composeApp:compileKotlinIosArm64`, `:composeApp:testDebugUnitTest` and `:composeApp:iosSimulatorArm64Test` completed successfully. Android: 90 tests, iOS simulator: 101 tests; zero failures/errors/skips. Seven local protocol tests run on each target; all ten real Room database tests run on iOS (three added here). XML evidence lives in `composeApp/build/test-results/testDebugUnitTest/` and `composeApp/build/test-results/iosSimulatorArm64Test/`. One initial short tool timeout interrupted compilation; the complete final rerun passed in 27 seconds.

Exported schema identity: v1 `9144767f0e36edf91fcfa381a4c50c24`, v2 `7994bb72468e3228a3e072dfdf65f912`. Both schemas are retained under `composeApp/schemas/com.motovault.app.storage.VaultDatabase/`. Physical Android database-upgrade QA and real-device process interruption remain outside this simulator/unit evidence.

No live upload, login, encrypted restore, physical-device backup or RLS-isolation claim is made.
