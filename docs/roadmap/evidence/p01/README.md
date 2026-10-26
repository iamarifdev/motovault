# P01 evidence index

- [Garage height correction and further open details](../P01-12-garage-layout-review.md)
- [Current Garage build and rendered-geometry provenance](garage-review/provenance.json)

Status: implementation in review. Listed physical Android core scenarios pass;
full motion/accessibility and physical iPhone acceptance remain pending. This folder contains synthetic data only, using
the isolated application ID `com.motovault.baseline.p00`.

P01-01 through P01-04 are done. P01-05 is in review with the owner's
pending-device instruction recorded; its broader device/design matrix is not
inferred from local builds.

Initial base revision: `86c1598`. P01 implementation was pushed as `48fce40`;
the delivered-alert retention follow-up and existing evidence were subsequently
committed/pushed as `792e63a` and `63198f9`. Historical collected source,
fixture and binary fingerprints are in [provenance.json](provenance.json), including
the iOS `MotoVault.debug.dylib` that contains the Kotlin implementation. A binary
hash identifies the local artifact; it does not claim release signing or device
validation. No owner database, document original or production app was reset.

- [17 v3 detail corrections and final native captures](../P01-11-v3-detail-corrections.md)
- [V3 working-tree source, artifact and test provenance](v3-details/provenance.json)
- [Policy and migration](../P01-01-verification.md)
- [Scheduling, exact targets and durable Undo](../P01-02-verification.md)
- [Native permission, snooze and failed-save checks](../P01-03-verification.md)
- [Honest gauges and confirmed mileage](../P01-04-verification.md)
- [Design coverage and remaining acceptance](../P01-design.md)
- [P01-05 system motion and remaining phase gates](../P01-05-verification.md)
- [Representative native matrix and visual corrections](../P01-05-matrix.md)
- [Native toolchain recheck](../P01-06-native-recheck.md)
- [Automated accessibility and native navigation](../P01-07-accessibility-navigation.md)
- [Numeric keypad and portrait safe bounds](../P01-08-keypad-layout.md)
- [Android duplicate window-motion correction](../P01-09-sheet-window-motion.md)
- [Completion audit and device resume conditions](../P01-COMPLETION-AUDIT.md)
- [Physical Motorola Android acceptance, including partial reboot result](../P01-10-physical-android.md)
- [Final test counts](tests/summary.json): 148 Android/shared and 170 iOS simulator
  tests, zero failures. Full XML is retained in `tests/android/` and `tests/ios/`.

`policy/` and `scheduling/` retain earlier targeted results. `android/` contains
native hierarchies, screenshots and actual notification records. `ios/` contains
XCTest summaries and named screenshot/tree attachments. `prototype/` contains
browser captures; notification access and failure states there are simulated.
`fixtures/p01/paired.json` pairs the main browser/native states. Clock-timed native
delivery fixtures are separate and must not be substituted for the paired set.

## Accepted and diagnostic runs

Accepted iOS runs include `undo-r2`, `denial-r5`, `unknown-large-r1` and
`save-failure-r2`. The initial save-failure harness selected a control below the
large-text viewport; the corrected run reached the injected failure and passed.

`settings-r1` checked only a transition into Settings and is insufficient proof.
`settings-r4` and `settings-manual-r1` are retained failures, not accepted recovery
checks. Background-delivery harness failures before permission or fixture setup
do not establish a scheduling defect or a successful delivery. Their original
`.xcresult` bundles remain under ignored `build/` for diagnosis.

`background-r5` reached actual native permission grant and OS delivery of three
notifications. Selected artifacts and the clock-timed fixture fingerprint are
retained under `ios/notifications/` and `ios/background-observation.json`. Its
whole-run summary remains **Failed** because the later notification tap did not
foreground the app. The focused `notification-taps-r4` also failed. These are
delivery observations, not accepted exact-target routing tests.

The follow-up [background-r8](ios/background-r8/summary.json) passes on the
preferred iOS 27 iPhone 18 Pro Max. Native side-swipe/Open reaches the exact
document, preserves the separate Chain alert, then reaches the exact service
schedule. Six new shared regression tests cover delivered alerts independently
of consumed scheduling events. The iOS Native suite was rerun with the explicit
`50709DF0-FEA9-4B34-9DE6-6172D868875D` destination using
`tools/baseline/isolated-ios.gradle`. Local ad-hoc test-host signing is distinct
from device/release signing.

[recovery-r4](ios/recovery-r4/summary.json) now passes revocation, the app's
notification-settings action and restoration on return on that same device.
[Snooze](ios/snooze-r1/tests.json) passes for both exact records; persisted
comparisons show unchanged expiry, baseline and ledger count. Its separate
whole-run Settings assertion failed and is retained, not relabeled as passed.

The Android [retention follow-up](android/android-retention-20261025-012210/result.json)
passes the actual-policy document-then-service sequence on the rebuilt isolated
APK. The test expands automatic notification groups before selecting individual
alerts. Earlier captures that selected the group summary, captured the launcher
before shade animation settled, or raced permission loading are diagnostic only.

Some early Android gauge captures were taken before navigation settled. Only the
accepted `paired-unknown-large-gauges` and fresh `unknown-detail-fresh` captures
are used by the reports. The capture helper now requires a fresh hierarchy file
and fails rather than reusing an old dump.

## Recheck

```sh
rtk run python3 tools/baseline/verify_p01.py check
rtk run python3 tools/roadmap.py check
```

The verifier checks retained artifacts and pending-gate declarations. It does not
run native tests, infer completion from Git, or turn the pending device/design
checks green. Keep the evidence and roadmap updated in the same working change.
