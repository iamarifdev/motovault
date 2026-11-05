# P05-04 verification: full renewal on Android and iOS

Status: passed on the Android 17 emulator and the iOS 27 simulator on
2026-11-05. No physical device was used.

## Revision

HEAD `55c1e43` plus the uncommitted P05 change set (domain, backup, resources, UI,
prototype, probes, evidence). Reports describe the working tree.

## Build and tests

```text
GRADLE_USER_HOME=$PWD/.gradle-home KONAN_DATA_DIR=$PWD/.konan \
  gradle-8.10.2/bin/gradle :composeApp:testDebugUnitTest \
  :composeApp:compileKotlinIosSimulatorArm64 --offline --no-daemon
```

Result: **289 tests, 0 failures, 0 errors, 0 skipped** (47 result files); shared
iOS compilation passed. Android probe app and test APK built and installed on the
emulator. iOS probe app built with `xcodebuild` for the pinned simulator.

## Android runtime

Full detail and APK hashes: [P05-04-android.md](P05-04-android.md).

```text
PASS 37 native storage checks
```

The renewal check starts from a reminder handoff using the production reminder
heading/message, then covers the checklist, synthetic receipt attachment, save,
renewed document/history, fee row, new reminder plan, original bytes and cleanup.
Cancellation and failed-save branches are included in the same check. Dark,
large-text, privacy and unavailable-link states have captures.

## iOS runtime

Full detail and binary hash: [P05-04-ios.md](P05-04-ios.md).

```text
PASS 22 native iOS storage checks
```

The renewal check covers previous/new expiry, revision, paid fee, fee-log link,
checklist reset, reminder plan, backup mapping and JSON round-trip.

## Acceptance mapping

| Criterion | Evidence |
| --- | --- |
| Run from reminder to checklist | Reminder-plan event asserted before the flow; document detail to sheet; checklist ticks persist. |
| Payment attachment | Synthetic receipt written through `ProtectedDocumentStorage`, attached, saved and listed as attached; original PDF bytes unchanged. |
| Renewed document | New expiry, revision 1 and one append-only event; detail/history screenshot. |
| Expense | One `document_fee` ledger row, ৳১,২৫০, linked IDs; records screenshot. |
| New reminder | Reminder-plan event for the new expiry asserted after save. |
| Synthetic documents and source freshness | Synthetic tax token and owner; source date 5 November 2026 visible in captures. |
| Deny/cancel/failure | Dismissed sheet keeps the document unchanged, discards the staged receipt and leaves original bytes intact; failed save shows localized copy with no renewal/fee and discards the receipt. |
| Native evidence | Android instrumentation plus screenshots; iOS simulator probe. |

## Gates

- literal gate 18 occurrences; fuel pilot 0;
- Bangla copy 15 regression phrases pass;
- tool fixtures: 54 tests pass;
- responsibility baseline 2 files / 7 functions;
- dependency baseline 0 violations; shared packages 11 / 16 edges, no cycle;
- fuel numeric policy passes;
- `roadmap.py check`: 19 phases, 93 tasks, views current.

## Limits

- No physical Android or iPhone; P08 keeps device and release gates.
- iOS runtime is a native state/archive/reminder probe, not a screenshot-driven
  production sheet.
- The receipt picker's successful system file selection is not automated; the
  test stages the receipt through the same protected-write API.
- The flow starts from a reminder handoff screen using the production reminder
  copy, not a delivered notification tap (P01 covers delivery/taps).
- Official BRTA links and fee rules remain external and can change; the source
  date is displayed.
