# P03A-03 fuel slice verification

Status: passed on the uncommitted working tree based on
`860c3ec04dac73bc10d6aba65798aff31887fdbb`. This report covers the final
simulator/emulator checks only; the incremental history remains in
[P03A-03-fuel-progress.md](P03A-03-fuel-progress.md).

## Implementation under test

- Fuel presentation lives in `ui/fuel/FuelSheet.kt`, `FuelDraft.kt` and
  `FuelScreenInput.kt`; the screen no longer imports the persisted UI vault
  model.
- `application/fuel/FuelWorkflow.kt` coordinates create and edit through the
  existing revision-aware writer and propagates cancellation.
- `domain/fuel/FuelEntryRules.kt` owns the calculation and timeline policy.
- `storage/FuelEntryAdapter.kt` maps the request to the unchanged serialized
  record.
- Fuel text resolves through Compose resources; the fuel UI direct-literal gate
  has an empty baseline.

Working-tree SHA-256 for the observed fuel sources:

| Source | SHA-256 |
| --- | --- |
| `ui/fuel/FuelSheet.kt` | `dffefe911fd39f6fc34ad2e13f800de6db66290b8cbcafa81d3efd4bd5589018` |
| `ui/fuel/FuelDraft.kt` | `8bf8167e612491e0dc33bade902a182c822914294d35706d7d9049e95f34cdde` |
| `application/fuel/FuelWorkflow.kt` | `41ddb2eb3d98d44142020e690669c02f98da6c347bc9abc93ba7497f8b5d2dc0` |

## Android runtime

Environment: Android 17 (API 37) Pixel 10 emulator, isolated
`com.motovault.app.privacyprobe` build with synthetic Room data and the real
coordinator writer.

Run on 2026-11-01:

```text
rtk run ./gradlew :composeApp:assemblePrivacyProbe :composeApp:assemblePrivacyProbeAndroidTest --offline
adb shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result: `PASS 34 native storage checks`. Fuel-relevant checks:

- `synthetic-native-fuel-ui-room-reopen-and-undo` — opened the seeded
  `৪.৯০ L` entry through the English UI, edited it to `৬০০`, saved, observed
  `৫.০০ L`, activated Undo, observed `৪.৯০ L`, and read the reverted record
  through a fresh Room connection.
- `synthetic-fuel-create-failure-preserves-draft-and-room` — injected a write
  failure, observed the localized error, retained the `৬০০` draft and verified
  the persisted fuel count did not change.

App/test APK SHA-256:
`ed13ad3b1667d376b5754177435e73d61f20b6c97cabbb02eb4e52f2c9d33fe4` and
`858f880682258641ce719a89ae5ccd015cf5c33e425fd6866f32e60156a593a9`.

The runner exposed the toast label as a non-clickable Compose text node; the
fixture now tries `ACTION_CLICK` at the label and its ancestors. The earlier
failures were test-automation lookup failures, not a product failure; the
corrected run is the one recorded here.

Screenshots from this run:

- [English fuel edit with Bangla digits](P03A-current-android-synthetic-fuel-edit-footer.png)
- [English labels with Bangla digits](P03A-current-android-synthetic-history-en-bn.png)
- [Bangla labels with English digits](P03A-current-android-synthetic-history-bn-en.png)

## iOS runtime

Environment: iOS 27 iPhone 18 Pro Max simulator (`iPhone19,3`), isolated ad hoc
signed `com.motovault.app.privacyprobe` debug build; simulator data was not
erased and only synthetic seed data was used.

The prior Bangla and English fuel edit/Undo/relaunch tests remain valid for the
unchanged fuel sources above:

- Bangla: [reopened screenshot](P03A-03-ios-fuel-draft-reopened.png),
  [test source](P03A-03-ios-fuel-probe.swift).
- English: [reopened screenshot](P03A-03-ios-fuel-english-reopened.png),
  [test source](P03A-03-ios-fuel-english-probe.swift).

Both tests used the isolated privacy-probe bundle, synthetic values, and a
relaunch after editing. The current 2026-11-01 iOS probe run also passed the
shared localization checks (`native-reminder-language-and-digit-preferences`,
`native-compose-plural-and-fallback`); see
[P03A-04-ios-native-probe-current.txt](P03A-04-ios-native-probe-current.txt).

## Limits

- No physical Android or iOS device was used. Physical-device accessibility,
  TalkBack/VoiceOver speech and performance remain release-gate work in P08-02.
- The Android fuel check runs in English; Bangla fuel UI evidence comes from the
  previously recorded screenshots. The iOS checks cover both languages.
- The Android runtime test covers one session; process-death draft restoration is
  not claimed.
- The UI screenshots show mixed English labels with the owner's Bangla digit
  preference. That is the stored digit setting, not a translation regression.
