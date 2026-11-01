# P04-05 schedule migration and Bangladesh setup verification

Status: passed on the isolated Android emulator and iOS simulator. No physical
device was used.

## Migration and backup

- `ComponentRule`'s new fields have serialization defaults, so existing Room
  schedule payloads and legacy JSON decode without a schema change.
- `FactorySetupTest.preP04ScheduleJsonDecodesWithSafeDefaults` and
  `BackupPayloadCompatibilityTest.preP04RuleWirePayloadUsesSafeDefaults` pin the
  old wire shape.
- `BackupPayloadCompatibilityTest.p04ActionAndProvenanceFieldsRoundTripThroughBackupPayload`
  verifies the backup payload preserves action, provenance, phase, one-time and
  catalogue fields.
- Existing `BackupTest`/restore suites still pass; the Room schema stays at
  version 6 because the table shape did not change.

## Android runtime

Device: Android 17 (API 37) Pixel 10 emulator, isolated
`com.motovault.app.privacyprobe` build.

```text
adb shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result: **PASS 36 native storage checks**. The new P04 checks are:

- `native-maintenance-profile-actions-provenance-and-unknown-baseline`;
- `native-maintenance-onboarding-and-schedule-details-ui`.

The suite covers supported R15 V4, unsupported R15 V3 and unknown-baseline
state; the onboarding/schedule screenshots are retained in
[P04-android-onboarding-unknown.png](P04-android-onboarding-unknown.png),
[P04-android-onboarding-known.png](P04-android-onboarding-known.png) and
[P04-android-schedule-details.png](P04-android-schedule-details.png).

## iOS runtime

Device: iOS 27 iPhone 18 Pro Max simulator (`iPhone19,3`), ad hoc signed debug
`com.motovault.app.privacyprobe` build.

The native probe result is retained verbatim at
[P04-ios-native-probe.txt](P04-ios-native-probe.txt):

```text
PASS 21 native iOS storage checks
```

It includes `native-maintenance-action-provenance-and-unknown-baseline`, which
creates the supported, unsupported and unknown states and checks the running-in
reset. The shared iOS compilation and 274-test Kotlin suite pass.

## Review

- The model set is source-checked but provisional; the Bangladesh distributor
  catalogue confirms availability while the owner's manuals are India-market
  prints for the same model codes. P08-03 must confirm the pilot-owner set.
- Manual intervals are base intervals. Local-condition shortening is an explicit
  owner edit; no undocumented multiplier is applied.
- The iOS evidence exercises the shared state model and native probe, not the
  production onboarding/gauge UI. Android supplies the native UI screenshots.
