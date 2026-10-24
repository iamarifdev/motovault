# P00-05: data and migration contract review

Date: 24 October 2026, Asia/Dhaka. Reviewer: Codex (source and fixture self-review).
Base: `main` / `50eda3a` plus P00 test/fixture/documentation tooling. No independent
security approval, future migration implementation or cloud review is claimed.

Reviewed deliverable: [shared data and migration contract](../DATA-MIGRATION-CONTRACT.md).

| Required area | Current evidence inspected | Decision retained |
| --- | --- | --- |
| Stable bike IDs | `VaultDatabase.kt` primary vehicle, `ComponentRule.vehicleId`, `Ride`/`RideLog`/document DTOs | Generate and persist one primary-to-bike mapping; preserve legacy IDs; P09 scopes all later queries |
| Person/bike documents | `VaultDocument`, typed document labels/capture/ownership fields | Owner licence versus bike paperwork; no guessed owner assignment or automatic sharing |
| Money | Current Double fuel/cost fields plus service poisha fields and full/partial fill calculation | Signed 64-bit poisha for new contracts, decimal half-up boundary conversion, reconciliation and one counted expense |
| Dates/time | `today()`, native renewal plan, prototype dates and dated fixtures | Date-only ISO versus UTC instant; explicit reminder local-time/zone policy and baseline certainty |
| Asset graph | `DocumentPage`, original/accepted/preview IDs, `assetRows`, native import limits | Immutable originals, hashes, bounded import, staged references and retained recovery/Undo assets |
| Migration/rollback | Exported schemas 1/2/3, `migrate`, `replace`, `VaultSession` | Preserve current data on failure; validate counts/references/money/assets, explicit schema adapters and idempotent mappings |
| Durable Undo | Native callback and revision guard, actual Android/iOS observations | Inverse command uses an appropriate revision; a toast is not proof that Undo committed |
| Backup/version limits | LocalSync preparation, service passport versions, native legacy inputs | Separate portable backup format from Room/passport; proposed bounded v1 import and fail-closed future-major policy |
| Settings persistence | Room `read`/`replace` and synthetic native database observations | DTO-compatible settings are not all restored by Room; add durable settings/consent migration in P01/P02/P06 |
| All future phase models | P01-P16 coverage table in the contract | Each model has scope, ownership/version and recovery decisions; optional rider/cloud enablement remains separate |

## Retained compatibility inputs

- [Paired native/prototype scenarios](../fixtures/p00/manifest.json): empty,
  overdue, partial fill, document and unknown-history inputs with matching IDs,
  amounts, mileage and dates.
- [Legacy/current compatibility inputs and original asset hashes](../fixtures/compatibility/manifest.json).
- [Expected money/ownership mappings](../fixtures/compatibility/expected-mappings.json).
- Schema 1/2/3 exports retained under `../fixtures/compatibility/`.
- Exact generated native inputs in
  `composeApp/src/commonTest/kotlin/com/motovault/app/ui/P00FixtureData.kt`, checked
  against the JSON source by `tools/baseline/compatibility.py`.

## Verification actually performed

1. `rtk run python3 tools/baseline/compatibility.py` passed paired-field/source
   parity, input hashes, decimal money examples and itemized total reconciliation.
2. The host check executed the exact existing 1->2 and 2->3 SQL against exported
   schema structures, preserving the synthetic primary payload and setting
   `autoLocation=0`. Final columns matched schema 3. This is host SQLite evidence,
   not a native Room validator/upgrade result.
3. [Android/JVM XML](p00/tests/android-compatibility.xml): seven tests, zero failures/errors.
4. [iOS simulator XML](p00/tests/ios-compatibility.xml): seven tests, zero failures/errors.
   Native cases cover all paired inputs, legacy defaults, stable round-trip IDs,
   partial-fill economy, settings DTO serialization, original/crop/preview IDs,
   exact service amount and malformed JSON rejection.
5. [Native persistence observations](p00/persistence-observations.json) confirm
   the recorded non-restoring Undo on both isolated test apps (12350 remains
   durable instead of 12340). The current metadata schema and outbox fields are
   retained without real owner data.

## Review result and limits

P00-05 acceptance is satisfied: the data/migration/evidence decisions are written,
all planned model families were reviewed against actual current boundaries, and
representative compatibility cases are retained and exercised. The contract
identifies present gaps rather than treating decisions as implemented features.

Real future Room upgrades, secure storage/key recovery, portable archive creation,
cross-device restore, physical-device behavior and cloud isolation remain gates
of their respective phases. Proposed backup ceilings require device measurement
and review in P03 before becoming a shipped promise.
