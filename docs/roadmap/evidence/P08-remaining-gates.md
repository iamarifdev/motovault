# P08 remaining gates and external inputs

Date: 12 November 2026 (Asia/Dhaka). Purpose: handoff summary for the P08 core
ownership release gate after the simulator/emulator work was completed. It lists
what is already proven, what cannot be proven with simulators, and the exact
external input needed to close each remaining task.

## Current state

| Task | Status | Evidence |
| --- | --- | --- |
| P08-01 Run core regression, migration and design matrix | **Done** | [core regression/migration](P08-01-core-regression.md), [design matrix](P08-01-design.md), [iOS](p08/ios/ios-capture-notes.md) / [Android](p08/android/android-capture-notes.md) runtime notes |
| P08-05 Complete app-wide localization and close the text-debt baseline | **In review** | [implementation + gates](P08-05-localization.md), [review + sign-off list](P08-05-review.md), [iOS](p08/localization/ios/ios-localization-notes.md) / [Android](p08/localization/android/android-localization-notes.md) language-switch notes |
| P08-02 Physical-device accessibility, reliability and performance pass | **In review** | Android physical slice done ([report](P08-02-android-device.md)); physical iPhone still needed |
| P08-03 Pilot with representative Bangladesh owners | Planned | needs human participants |
| P08-04 Prepare and verify core release candidate and recovery handoff | Planned | needs signing material and owner authorization |

The Android emulator and iOS simulator are left in their original state: no
device was booted, erased, reset or reconfigured, no OS locale/appearance/font
scale was changed, the production apps were not launched, and only isolated
synthetic probe data was used. Nothing is committed.

## P08-05: closing input

Only one item remains: a **named human review** of the changed/new strings listed
in [P08-05-review.md](P08-05-review.md#strings-changed-or-added-in-this-slice-human-review-list).
The table covers:

- changed: `cost_empty_amount`, `quickadd_paste_title`, `quickadd_paste_desc`,
  `settings_reminder_settings`, `vault_reminder_action`;
- new: the `capture_*`, `pick_*` and `passport_*` error strings;
- the `onboarding_step` wording now displayed.

When the reviewer confirms the English and Bangla copy, record the reviewer name
and date in that section and in a tracker note, then move P08-05 to `done`. The
runtime and static evidence is already complete.

## P08-02: physical device pass

Required evidence kinds: verification, design, android_device, ios_device.

**Android slice completed 12 November 2026** on the owner's connected Motorola
Edge 50 Fusion (Android 16/API 36) in one consolidated run: native probe PASS 37
on device, language/layout/permission smoke, denial fallbacks, and startup /
memory / frame measurements (see [the report](P08-02-android-device.md)). Device
settings and permissions were restored. Still open for this task:

- **physical iPhone** (`ios_device` evidence; waived by the owner while no iPhone
  is available — WD-01 in [P08-owner-decisions.md](P08-owner-decisions.md); a
  one-run plan is ready at [P08-02-ios-device-plan.md](P08-02-ios-device-plan.md)):
  the same pass on an iPhone, including spoken VoiceOver and native iPhone
  camera/share/voice behaviour;
- spoken **TalkBack** output/focus on physical hardware (the swatches now have
  accessible names and radio semantics in the emulator/simulator trees via
  [the swatch fix](P08-02-swatch-accessibility.md), but no spoken session was
  run and the Android physical pass predates the fix);
- notification **delivery/background** behaviour (the prompt was captured; the
  allow path was not confirmed and permission was left at its original false);
- the full locked/duplicate/unsupported/cancel share matrix and physical
  share/voice flows (cold/warm share, stream/mime rejection and the backup
  restore picker were verified on the physical phone);
- long-run battery/sustained-load budgets and repeat performance runs; the
  physical large-vault run (1,200 logs) already shows 15.03 % janky frames vs the
  provisional ≤5 % budget — needs a release decision (budget or optimization).

Needed externally:
(the iOS deployment target is unchanged; the iOS 27 simulator restriction does not
relax physical coverage). Exact checks from the task and carried findings:

- TalkBack/VoiceOver **spoken** output, focus order, modal announcements, large
  text, keyboard/focus and native back/gesture behaviour. The simulator/emulator
  semantics tree is not spoken-output proof. Carried P08-01 finding: the nine
  brand-colour swatches are unlabeled and the ninth is clipped at rest; verify
  after a semantics fix.
- Notification/background delivery, camera, biometric/device credentials, restore,
  document/file pickers and denied permissions on the real hardware.
- Startup, scroll, large-vault operations, memory and battery measurements on the
  lower-resource Android plus the iPhone; define budgets from those measurements.
- Carried from P07: physical iPhone camera/share/voice, Android
  locked/duplicate/unsupported/cancel share matrix and the accessibility passes.
- Suggested environment commands once devices are attached: build/install the
  isolated probe APK (`assemblePrivacyProbe`, `installPrivacyProbe`) and run
  `NativeAssetInstrumentation` for storage regressions; run the XCTest capture
  and localization suites with the physical destination for iOS UI checks.

## P08-03: representative owner pilot (humans)

Needed externally: representative Bangladesh owners (commuters, a first-time or
used-bike owner, an enthusiast), consent for anonymized observations, and the
recorded P07 pilot protocol. A ready-to-run kit (participants, consent scope,
35–45 minute protocol, observation/decision templates, isolation and evidence
rules) is at [P08-03-pilot-kit.md](P08-03-pilot-kit.md); it is prepared, not run,
and records no participant result. The task must record backup/restore success, reminder
usefulness, log effort and cost comprehension, and close data-loss/privacy/
incorrect-guidance defects. The 1.3-scale Records-row and Profile-theme-segment
clipping found by P08-01 was fixed (see
[P08-01-layout-resolutions.md](P08-01-layout-resolutions.md)); still open for a
product decision are the residual demo claims ("New concepts (demo)", "Reset demo
data", Bangla demo labels in the English demo state), the expense date digit
style, and whether to restructure the backup sheet's primary action into a pinned
footer.

## P08-04: release candidate and recovery handoff (signing/distribution)

Needed externally: owner authorization and signing/distribution material. Then:
record the release build and signing state, enabled features, privacy/store
declarations and known limits; prepare release notes, support/recovery
instructions and rollout/rollback criteria; install the signed candidate on both
platforms and repeat the restore/upgrade smoke path. Publication remains a
separate authorized action. The current uncommitted working tree is not a
release candidate.

## Simulator evidence that is already complete

- Core regression/migration: fresh 327-test unit run (0 failures), iOS storage
  probe PASS 22, Android native probe PASS 37, portable/workstation gates pass.
- Design matrix: iOS 8/8 tests, 36 screenshots, 2 motion recordings; Android 42
  screenshots + 2 recordings + retained prior-probe states; nine-brand contrast
  test; review findings recorded in [P08-01-design.md](P08-01-design.md).
- Localization: expanded literal gate at 9 justified data-only glyphs, 327 unit
  tests, Android/iOS language-switch captures with native notification-content
  assertions, copy defects fixed.

## Decisions needed from the owner

1. Approve (or correct) the P08-05 copy list so that task can close.
2. Decide the remaining P08-01 product items before the release candidate:
   demo-claim/data presentation, the expense date digit style, and the optional
   backup-sheet pinned-footer refactor (the 1.3-scale clipping was fixed).
3. Provide physical devices, pilot participants and signing authorization when
   the phase can proceed beyond simulator evidence.
