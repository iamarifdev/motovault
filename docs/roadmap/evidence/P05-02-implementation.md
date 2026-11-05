# P05-02 renewal events, fee links and original preservation

Status: implementation complete; native UI and runtime evidence follow in P05-03
and P05-04.

## Responsibility and files

The renewal event belongs to the document aggregate. A single `VaultState`
transformation updates the document expiry and revision, appends the renewal
event, writes the paid-fee ledger entry and clears the reminder snooze. The
caller commits one state, so the existing coordinator remains the only durable
writer and a failed save changes nothing.

Added:

- `composeApp/src/commonMain/kotlin/com/motovault/app/domain/model/DocumentRenewal.kt`
  — append-only event: document ID, stable kind, previous/new expiry, renewal
  date, resulting document revision, actual paid fee, receipt asset ID, notes,
  source-checked date and the linked fee-log ID.
- `composeApp/src/commonMain/kotlin/com/motovault/app/domain/model/DocumentKind.kt`
  — stable kinds and a legacy-title resolver, so renewal never depends on a
  localized title.
- `composeApp/src/commonMain/kotlin/com/motovault/app/domain/documents/RenewalChecklist.kt`
  — dated official sources, per-kind step IDs, source links, fee-calculator and QR
  applicability from [P05-01 research](P05-01-research.md).

Changed:

- `domain/model/VaultRecords.kt`: `VaultDocument` gains `revision`,
  `renewals` and `checkedRenewalSteps`; `assetIds()` includes renewal receipts;
  `RideLog` gains `documentId` and `renewalId`.
- `domain/vault/VaultState.kt`: `renewDocument(...)` and
  `setRenewalChecklist(...)`; `saveDocument` increments the document revision;
  `logDocumentFee` resolves the document ID and no longer persists a display
  label as the fee title/detail.
- `domain/backup/BackupDocument.kt`, `BackupLog.kt`,
  `storage/BackupDocumentAdapter.kt`, `storage/BackupRideLogAdapter.kt`,
  `storage/BackupRestoreStaging.kt`: archive fields with defaults, receipt asset
  collection and restore-time receipt remapping.

## Rules enforced by `renewDocument`

- the document must still exist by stable ID;
- the renewal date cannot be in the future;
- the new expiry must be after the renewal date and after the current expiry when
  one is stored;
- the same new expiry cannot be recorded twice for one document;
- the paid fee is optional, non-negative and bounded by
  `MAX_RENEWAL_FEE_POISHA`; a fee above zero needs a bike for the ledger
  odometer;
- a receipt must be a validated managed asset ID (JPEG/PNG/WebP/PDF);
- notes are bounded by `MAX_RENEWAL_NOTES_LENGTH`;
- the document revision cannot overflow.

The resulting document keeps all pages, originals and the PDF; a renewal only
changes the expiry, appends history and clears the snooze. The receipt is a new
managed asset, not a replacement for an original. Asset cleanup and recovery use
the existing `assetIds()` path, and restore remaps receipt IDs through the same
collision-safe mapper as pages.

## Persistence compatibility

- Room stores each document as JSON, so the new document fields are defaults on
  legacy rows and need no schema version change.
- The backup wire payload keeps default values out of the encoded JSON
  (`encodeDefaults` is off), so the frozen V1 fixture is unchanged.
- Old documents and logs decode with `revision = 0`, empty renewal history and
  no receipt; this is pinned by a compatibility test.
- Undo restores the previous `VaultState` (expiry, ledger, checklist, snooze).
  The receipt asset stays owned by the retained undo snapshot; if it becomes
  unreferenced after restart it enters the existing recovery/quarantine path
  instead of being deleted.

## UI wiring deferred intentionally

`renewDocument` and `setRenewalChecklist` are state/domain contracts only in this
slice. The sheet, checklist presentation, browser boundary, receipt picker and
renewal history are P05-03; Android/iOS end-to-end evidence is P05-04.
