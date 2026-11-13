# P15 Encrypted Cloud Backup & Multi-Device Sync Specification

## 1. Provider, Auth & Account Binding Model
- **Client-side envelope encryption**: All remote payloads wrap the existing versioned `.mva` encrypted backup archive (`BackupUseCase` / `RestoreUseCase`), using keys derived locally from the owner's backup passphrase/recovery secret (`BackupCrypto.deriveKey`). Privileged service credentials or raw vault keys are never embedded in client builds.
- **Explicit account & vault binding**: Connecting cloud sync binds a single `CloudAccountBinding(accountId, displayEmail, vaultId)`. Remote objects live strictly under owner-scoped prefixes `owners/{accountId}/vaults/{vaultId}/revisions/{revision}/...`.
- **Billing, quota & metadata disclosure**: Connecting requires an authenticated session; remote storage bills/counts against the owner's provider quota. Metadata stored alongside ciphertext consists only of `(ownerAccountId, vaultId, revision, operationId, assetHashes, createdAtEpochMillis)` — readable bike/expense/document contents stay inside `.mva` ciphertext.

## 2. Single-Writer Publication, Conflict & Idempotency
- Every publication freezes a monotonic vault revision (`revision`), uploads referenced immutable asset objects (`CloudAssetObject`) verified by `SHA-256` before uploading `CloudRevisionManifest`, and commits using an atomic compare-and-set precondition (`expectedPreviousRevision`).
- Server/transport enforces:
  - Cross-owner isolation (`CrossOwnerDenied` on wrong-account read or write).
  - Asset-before-manifest completeness (`MissingAsset` if any asset hash is absent or mismatched).
  - Tampered ciphertext rejection (`CiphertextTampered` when `SHA-256` digest fails).
  - Idempotent retries via unique `operationId` (`DuplicateOperationId`).
  - Conflict preservation (`RevisionConflict`) without overwriting newer remote state.

## 3. Account Switch, Logout & Local-First Continuity
- Logging out (`logout()`) or switching accounts (`switchAccount(...)`) immediately cancels any active upload job (`Cancelled`), clears in-memory session bindings, and leaves all local device records fully functional offline (`CloudConnectionStatus.DISCONNECTED`).
- Retaining newer queued local revisions when an older upload settles (`queuedRevision > publishedRevision`) ensures fast local edits are never lost or prematurely acked.
- Under `WD-02` ([P08-owner-decisions.md](evidence/P08-owner-decisions.md)), live cloud infrastructure provisioning is deferred; `StagedCloudTransport` and `InMemoryStagedCloudTransport` verify every contract rule in CI and simulator runs.
