# P08-01 core regression, migration and design-matrix evidence

Status: complete for the P08-01 scope recorded here. Core regression, migration
and the design matrix ran on the iOS 27 simulator and the Android emulator with
isolated synthetic data, and the portable/workstation gates pass on the evidence
tree. Physical-device, owner-pilot and signed-release gates remain in P08-02,
P08-03 and P08-04; runtime language-switch breadth remains in P08-05.

This is not a compile-only claim and not a physical-device result. Findings from
the design review are recorded in [P08-01-design.md](P08-01-design.md) and are
carried to the tasks listed under "Open gates".

## Revision identity

- Base commit: `e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4`.
- Working tree identity is **not clean**; the evidence build includes the P08
  evidence/capture files, the test-only probe fixes
  (`ProbeFuelChecks.kt`, `ProbeRenewalChecks.kt`, `ProbeUiHelpers.kt`), the iOS
  simulator share-extension signing script plus its project build phase,
  `tools/tests/test_localization_resources.py`, the tracker/generated files and
  the new `P08DesignMatrixTests.swift`. Do not label this build a clean-HEAD
  revision.
- Evidence was produced before the tracker/evidence documents were finalised, so
  the revision identity in an artifact is the artifact's own recorded revision;
  the platform capture notes repeat the working-tree list.

## Acceptance mapping

| P08-01 acceptance item | Evidence | Result |
| --- | --- | --- |
| Fresh install and baseline upgrade with full synthetic records and a restorable backup | Fresh synthetic vault provisioning plus migration/recovery checks on both platforms (below); accepted fresh-install restore evidence in [P03-05](P03-05-android-device.md); backup/restore and format migration in [P03A-04](P03A-04-verification.md); legacy/current fixture decoding in the unit suite (`P00CompatibilityTest`, `docs/roadmap/fixtures/compatibility/`) | Verified within the limits below |
| All four tabs, sheets, both digit styles, nine brand accents, light/dark, 1.0/1.3 text scales; motion recorded separately | [P08-01-design.md](P08-01-design.md) + [iOS notes](p08/ios/ios-capture-notes.md) + [Android notes](p08/android/android-capture-notes.md); 36 iOS xcresult PNGs, 42 new Android PNGs plus 15 retained prior-probe PNGs, 2 iOS recordings, 2 Android recordings | Verified as a representative matrix |
| Verify P03A foundation, pilot, backup migration and quality-gate evidence plus phase architecture/localization requirements | Portable gate run on this tree (below); [P03A-03](P03A-03-verification.md), [P03A-04](P03A-04-verification.md), [P03A-05](P03A-05-verification.md); 327-test unit run; architecture dependency/package/module checks; literal, copy, responsibility and fuel-policy ratchets | Pass |

### Recorded working-tree identity

Commands run in the repository root (the values were taken while finalising this
report; per-artifact SHA-256 values are in the platform capture notes and
provenance files):

```bash
git rev-parse HEAD
# e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4
git diff HEAD | shasum -a 256
# 61c3c8fe6f8bffe559a6550ebd204ad70011a86a3cdffd2924d9ed79023d5ecb
git ls-files --others --exclude-standard -z | xargs -0 shasum -a 256 | shasum -a 256
# 5e75ec6fe233cee401b80941db8dd247419d2c51b9b8892816c525130b412c36
```

The working tree contains the P08 evidence files, the tracker/generated updates,
the test-only Android probe fixes, the iOS design-matrix test and the iOS
sign-share harness. Nothing was committed.

## Portable gates

Run on `e6c59bf` + working tree during this slice:

| Gate | Result |
| --- | --- |
| `tools/check_agent_docs.py` | pass |
| `tools/ui_literal_gate.py` | pass (18 baseline occurrences) |
| `tools/ui_literal_gate.py --fuel` | pass (empty baseline) |
| `tools/bangla_copy_lint.py` | pass (15 regression phrases) |
| `tools/responsibility_gate.py` | pass (2 test-support files) |
| `tools/architecture_dependencies.py` | pass (0 violations) |
| `tools/architecture_package_cycles.py` | pass (no cycles) |
| `tools/fuel_policy_literals.py` | pass |
| `python3 -m unittest discover -s tools/tests -p 'test_*.py'` | pass (59 tests) |
| `tools/roadmap.py check` | pass |

The literal gate is a lexical ratchet, not semantic proof, and the copy lint is a
regression guard, not a translation review.

## Workstation gates

| Gate | Result |
| --- | --- |
| `./gradlew :composeApp:testDebugUnitTest --offline --rerun-tasks` | BUILD SUCCESSFUL; 327 tests, 0 failures, 0 errors, 0 skipped (57 result files) |
| `./gradlew :composeApp:compileKotlinIosSimulatorArm64 --offline` | pass (up-to-date after the same-source compile; the xcodebuild test build also recompiled/embedded the Kotlin framework for the iOS run) |
| `./gradlew architectureModuleEdges --offline` then `tools/architecture_cycles.py` | pass (1 configured edge, no cycle) |

The unit suite includes the compatibility fixtures (`P00CompatibilityTest`),
appearance/contrast (`AppearanceTest`, all nine brands in light and dark),
localization parity and the migration-facing storage tests.

## iOS runtime (simulator)

Environment: already-booted iOS 27.0 (24A434) iPhone 18 Pro Max simulator
`50709DF0-FEA9-4B34-9DE6-6172D868875D`; isolated synthetic
`com.motovault.app.privacyprobe`; Xcode 27.0 (27A266a). No simulator boot, erase,
reset, uninstall or system appearance/content-size/locale change.

- **Design matrix:** `xcodebuild test ... -only-testing:MotoVaultUITests/P08DesignMatrixTests`
  final run `build/p08-ios-runs/final-20261112-225133` — **8/8 tests passed**,
  36 screenshots exported. See
  [iOS capture notes](p08/ios/ios-capture-notes.md) and
  [final provenance](p08/ios/provenance/p08-ios-provenance-final.txt).
- **Motion:** [normal](p08/ios/recordings/p08-ios-motion-normal.mov) and
  [reduced](p08/ios/recordings/p08-ios-motion-reduced.mov) recordings made around
  single-method runs with the in-app Reduce motion switch asserted ON
  (`p08-ios-motion-reduced-enabled-check.png`).
- **Native storage/migration probe:** PASS 22 native iOS storage checks on the
  same build, including `native-room4-to5-owner-preservation-and-future-format-refusal`,
  `native-durable-migration-original-bytes-and-idempotent-reopen`,
  `native-backup-adapter-create-preview-wrong-password`,
  `native-backup-coordinated-recovery-collision-room-reopen`,
  `native-encrypted-recovery-snapshot-and-reopen`,
  `native-zero-space-preflight-retains-original`,
  `native-tamper-refuses-plaintext-fallback` and the reminder language/digit
  checks. Result retained at
  [p08-ios-storage-probe-full2-run.txt](p08/ios/provenance/p08-ios-storage-probe-full2-run.txt)
  (`syntheticKeychainStatus: -25300`; the probe uses a synthetic isolated key
  service, so this is not a real-keychain pass).
- **Capture flow regression:** the P07 `CaptureFlowTests` 8/8 result on the
  19:07 build of this same revision is cited by the earlier report revision.
  The design-matrix run in this slice rebuilt the app from the same sources; that
  rebuild is identified by its own hashes and is not labelled a clean HEAD.

## Android runtime (emulator)

Environment: existing Pixel_10 `emulator-5554`, Android 17/API 37, arm64;
isolated `com.motovault.app.privacyprobe`; probe APK
`73a3ce93f164fc890a50523abca20c43e6719f2140e5716e88704a4444812479` and test APK
`7f7c82fbae147c414896f928f57a9629282ed007f418f3c9de98c076701c7713` (installed
hashes match the local build outputs). No emulator boot, wipe, erase,
uninstall, `pm clear`, or system night-mode/font-scale/locale change.

- **Full native privacy probe** on this revision after the probe-only fixes:
  PASS 37 native storage checks (backup, fuel, maintenance, renewal, recovery,
  cost), including `native-backup-synthetic-vault-provision`,
  `native-backup-export-success`, transactional restore and
  `native-room4-to5-owner-preservation-and-future-format-refusal`. The capture
  probe (`-e onlyCapture true`) passes on the same build. The applicable part of
  this run is recorded in this report's earlier revision; its console result is
  the preceding run's recorded PASS.
- **Design matrix:** 42 PNG captures plus 2 screen recordings:
  [Android capture notes](p08/android/android-capture-notes.md). Retained prior
  probe screenshots cover backup create/review, renewal variants, recovery and
  the fuel-edit footer:
  [prior-probe-cache](p08/android/prior-probe-cache/README.md).
- **Unit suite:** included in the 327-test workstation run above.

## Fresh install, baseline upgrade and restorable backup

- **Fresh synthetic vaults / install path.** Both native probes start from fresh
  synthetic databases/stores on a freshly installed isolated app build. Android
  `native-backup-synthetic-vault-provision` and the transactional restore create
  and restore into fresh target databases; the iOS storage probe runs on a fresh
  synthetic application root. The accepted cross-platform fresh-install restore
  evidence remains [P03-05](P03-05-android-device.md) (fresh installations on the
  physical Android device and the iOS simulator), which this slice does not
  re-claim as new.
- **Baseline upgrade.** `native-room4-to5-owner-preservation-and-future-format-refusal`
  passes on both platforms, and the unit suite decodes the committed
  legacy/current compatibility fixtures (`legacy-minimal`, `current-all-fields`,
  `room-schema-1..3`). [P03A-04](P03A-04-verification.md) has the detailed
  backup/restore and migration record.
- **Restorable backup.** Android: encrypted export, zero-plaintext leakage,
  staged validation/preview, wrong-password/tamper refusal, transactional restore
  with collision remapping and Room reopen. iOS: adapter create/preview/wrong
  password, coordinated recovery collision/reopen, encrypted recovery snapshot,
  zero-space/interrupted-stage preserves. The design-matrix UI opened the backup
  sheet only; **no real backup archive was created or restored in the UI capture
  run** — the archive assertions are the native probe evidence above, not the
  screenshots.
- **Limit.** The isolated probe app's existing synthetic data was not erased to
  re-create an "uninstall then reinstall from zero" run in this session; the
  accepted P03-05 fresh-install evidence covers that gate. A destructive
  `pm clear`/device reset is not authorized and was not attempted.

## Design review and motion

The representative design matrix and its findings (nine-brand contrast unit
test, extreme accents rendered, nine swatches, light/dark, BN/EN, both digit
styles, 1.0/1.3 scale, seven sheets, motion normal/reduced) are documented in
[P08-01-design.md](P08-01-design.md). Findings are not silently accepted:
accessibility/copy/layout defects are carried to the tasks below.

## Open gates and carried findings

- **P08-02 physical device** (open): TalkBack/VoiceOver spoken output, physical
  camera/share/voice, permissions, battery/performance, large text, OS
  appearance/content-size. P08-01 observed unlabeled brand-colour swatches, a
  clipped ninth swatch and a dark-mode warning-contrast concern; these need the
  physical accessibility pass and a semantics fix.
- **P08-03 owner pilot** (open): release-blocker candidates from the design
  review — 1.3-scale clipping of Records action labels and the Profile theme
  segment, backup/privacy sheet clipping, expense date digit style, and residual
  demo claims ("New concepts (demo)", "Reset demo data", Bangla seed labels in
  the English demo state) need a product decision before the release candidate.
- **P08-04 release candidate** (open): signing/distribution, privacy/store
  declarations, release notes and recovery handoff are not started. The
  uncommitted build here is not a release candidate.
- **P08-05 runtime localization** (open): the matrix exercised in-app BN/EN and
  both digit styles on visible screens, but native notifications, permission
  surfaces and the full copy review remain. Copy defects observed: BN expense
  "টাকা: টাকা দেওয়া হয়নি", awkward EN quick-add copy, EN Garage/seed/demo Bangla
  labels.
- **Motion** was recorded separately on both platforms; it is evidence that the
  reduced-motion toggle changes the transition flows, not a measured parity or
  performance result.
