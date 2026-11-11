# P06 verification

Base revision: `f2ca12f` on `main`, initially clean. Verification applies to the
P06 working change, not a committed release. Dates: 11–12 November 2026 BDT.
Exact source and artifact hashes are in [the manifest](P06-artifact-manifest.json).
Source identity: `ff35f7a0589d799d31a245dc9c0feeced4cac9be80c81a8a8fd86e2704d02615`.

## Unit and build evidence

- The complete Android-hosted common unit suite ran once: 305 tests, 303 passed
  and two existing fuel tests failed. One exposed an unnecessary change to an
  unchanged legacy record; the other fixture had not explicitly confirmed its
  newly entered price. The adapter and fixture were corrected.
- Only the affected `FuelEntryAdapterTest` and `FuelPriceProvenanceTest` classes
  were rerun: **9 tests, zero failures/errors/skips**. The other 296 tests were
  not rerun. This is cumulative coverage of 305 distinct tests, not a claim that
  a second full suite ran.
- Android debug app, isolated privacy-probe app/test APKs, iOS simulator shared
  compilation and debug framework linking passed. Initial compiler attempts
  found an iOS interop opt-in and stale incremental native-test metadata. A
  source/resource edit during a running build invalidated another compiler
  attempt. Those attempts did not rerun the complete unit suite.
- The v3 TypeScript/Vite production build passed. The new History controls were
  walked through in the local browser using demo records: decimal parking
  entry, updated recorded total, budget entry and category breakdown.

The Gradle run used the existing `.gradle-home` and `.konan` caches, JDK 17,
`--offline --no-daemon`, and `-Pkotlin.incremental=false` to avoid the stale
instrumentation metadata. It requested `testDebugUnitTest`, `assembleDebug`,
`assemblePrivacyProbe`, `assemblePrivacyProbeAndroidTest`,
`compileKotlinIosSimulatorArm64` and `linkDebugFrameworkIosSimulatorArm64`.
The retry filtered the two affected test classes with `--tests`.

## Business and compatibility checks

The 16 new unit tests cover half-up legacy conversion, exact poisha, invalid
amounts, inclusive 30-day/month windows, future/backdated records, duplicate
record/renewal IDs, category totals, real versus copied mileage, absent and
contradictory distance, refunds, corrections, optional budgets, confirmed price
dates, decimal/Bangla/Arabic-Indic input and partial/missed fuel cycles. The
archive test uses the real production crypto implementation and checks all new
log and preference fields after encryption and preview.

New fields are optional in existing Room JSON payloads and backup models. A
no-op edit preserves a legacy fuel record's absent integer field. New or changed
amounts retain canonical poisha plus readable legacy `cost`. No database schema
version, original document or existing record ID is changed by migration.

## Portable gates

Passed: UI literal baseline, empty fuel literal pilot, Bangla-copy lint,
responsibility ratchet, domain/application dependencies, package cycles,
resource parity and fuel numeric policy. The tooling suite passed all 54 tests;
resource parity was checked again after final resource additions. No baseline
exception was added. The existing instrumentation entry shrank from 568 to
551 lines by extracting its focused-run routing. New production files remain
below 400 lines. Roadmap and diff checks close with the tracker update. Detailed gate output is
in [P06-portable-gates.txt](P06-portable-gates.txt); cumulative test counts are
in [P06-unit-results.json](P06-unit-results.json).

## Native evidence

Both hosts passed the production-writer/Room/reopen/encrypted-backup fixture.
[Android](P06-android.md) additionally asserted cost UI save/Undo, retained failed
drafts and all four language/digit combinations. The final labeled-checkbox
retry passed independently without repeating storage checks.

[iOS](P06-ios.md) has 21 reviewed visual checkpoints covering all four
language/digit combinations, exact totals, category filtering, expense
save/Undo, failed save/cancel, budgets, details, price/missed-fill controls and
dark/large text. Its three checkpoint methods ran without runner failures;
rendered values and copy were verified by manual image inspection, not inferred
from the runner status. Native durable-state assertions are separate.

The initial iOS selector-based UI checks failed because XCTest exposed only the
Compose root. This limitation is preserved in the report. The owner explicitly
approved tracking iOS VoiceOver under **P08-02** on 12 November; the
[accessibility handoff](P06-ios-accessibility.md) is attached to that task.
VoiceOver is not claimed as passed. No physical-device, motion-recording or
release/publication claim follows from these emulator/simulator checks.
