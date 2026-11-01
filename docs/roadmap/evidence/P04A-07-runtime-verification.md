# P04A-07 cleanup regression verification

Status: passed on the isolated Android emulator and iOS simulator. No physical
device was used.

## Build and tests

```text
rtk run ./gradlew :composeApp:testDebugUnitTest :composeApp:compileKotlinIosSimulatorArm64 --offline
```

Result: **274 unit tests, 0 failures**, shared iOS compilation passed.

## Android runtime

Android 17 (API 37) Pixel 10 emulator, isolated
`com.motovault.app.privacyprobe` build:

```text
adb shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result: **PASS 36 native storage checks**. The suite still covers fuel edit/Undo,
failed-create draft retention, maintenance profiles, onboarding/schedule details,
recovery, backup localization/create/restore, encrypted assets, migration and
collision recovery after the file/model split.

## iOS runtime

iOS 27 iPhone 18 Pro Max simulator (`iPhone19,3`), ad hoc signed debug
`com.motovault.app.privacyprobe` build:

```text
PASS 21 native iOS storage checks
```

The verbatim result is [P04A-ios-native-probe.txt](P04A-ios-native-probe.txt).
It includes the shared maintenance profile/unknown-baseline check and the
backup/recovery fixtures.

## Architecture and responsibility

- `composeApp/src/commonMain/.../storage` has **zero** imports from
  `com.motovault.app.ui`.
- `architecture_dependencies.py` matches its baseline; the shared package graph
  is **10 packages / 14 edges with no cycle**.
- `tools/responsibility_gate.py` matches 11 file and 7 function baseline entries;
  its five fixtures pass.
- The shared UI literal gate matches the regenerated 97-occurrence baseline.
- `roadmap.py check` passes with 19 phases / 91 tasks.

## Screenshots

- [Onboarding unknown baseline](P04A-synthetic-p04-onboarding-unknown.png)
- [Onboarding known baseline](P04A-synthetic-p04-onboarding-known.png)
- [Schedule details with manual source](P04A-synthetic-p04-schedule-details.png)

The screenshots show the post-split UI; behavior and appearance were preserved.

## Limits

- Physical-device accessibility, TalkBack/VoiceOver speech and performance remain
  P08-02 work.
- The responsibility gate is lexical; expression-body functions are not measured
  and the over-400 file exceptions still require manual review. Current exceptions
  are listed in [file-responsibility-baseline.md](../engineering/file-responsibility-baseline.md).
- The service catalogue is now under `domain.model`; its Bangla/English labels
  are data but their resource ownership remains a localization follow-up.
