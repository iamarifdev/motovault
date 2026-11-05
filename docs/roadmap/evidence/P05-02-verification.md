# P05-02 verification

Status: domain, persistence and backup verification passed on 2026-11-05. Native
runtime is not part of this slice.

## Revision and tree

- HEAD: `55c1e43` (main), plus the uncommitted P05-02 change set.
- Dirty files: the domain/backup sources and tests listed in
  [P05-02-implementation.md](P05-02-implementation.md), the P05-01 evidence, the
  regenerated roadmap views and the separate v3 prototype extension.
- The evidence describes the working tree, not a clean committed revision.

## Commands and results

```text
GRADLE_USER_HOME=$PWD/.gradle-home ANDROID_USER_HOME=$PWD/.android-home \
  ~/.gradle/wrapper/dists/gradle-8.10.2-bin/.../gradle-8.10.2/bin/gradle \
  :composeApp:testDebugUnitTest --offline --no-daemon \
  -Pkotlin.compiler.execution.strategy=in-process
```

Result: **289 tests, 0 failures, 0 errors, 0 skipped** across 47 result files.

```text
rtk run python3 tools/architecture_dependencies.py
rtk run python3 tools/architecture_package_cycles.py
rtk run python3 tools/responsibility_gate.py
rtk run python3 tools/roadmap.py check
```

Results:

- dependency import baseline matches (0 existing violations);
- shared domain/application graph: **11 packages / 16 edges, no cycle**;
- responsibility baseline matches (2 files, 7 functions);
- roadmap valid: 19 phases, 93 tasks; generated views current.

The Gradle runner used a workspace-local `GRADLE_USER_HOME` because the file
sandbox cannot write the user-level Gradle cache; the toolchain itself is the
checked-in 8.10.2 distribution and the build ran `--offline`.

## New tests

`DocumentRenewalTest` covers:

- previous/new expiry, kind, paid fee, receipt, notes, revision and linked log;
- snooze and checklist reset in the same save;
- no ledger entry for a zero fee;
- rejection of a non-extending or duplicate expiry, stale document, invalid fee,
  invalid receipt ID and over-long notes;
- renewal without a bike allowed until a fee is recorded;
- checklist accepts only step IDs from the document's guide;
- legacy document JSON without renewal fields decodes with defaults;
- every stored kind resolves to a sourced guide and unknown kinds do not.

`BackupPayloadCompatibilityTest` adds:

- pre-P05 document JSON keeps `revision = 0`, empty renewals/checklist and the
  original asset set;
- a renewal with a receipt round-trips through the encrypted-payload model, keeps
  the receipt in the asset set and links the fee log to the renewal.

`VaultCoordinatorTest` adds:

- Undo after a renewal restores the old expiry, removes the fee log and the
  renewal event, and the restored document no longer references the receipt.

## Not verified in this slice

- No native sheet, picker or browser launch exists yet (P05-03).
- No Android emulator or iOS simulator run exists yet (P05-04).
- `compileKotlinIosSimulatorArm64` was not part of this domain-only check; it is
  required before the phase handoff.
- The coordinator retains the receipt for the undo snapshot by design; restart
  recovery is the existing quarantine path and is covered by P03/P04A evidence,
  not re-run here.
