# P08-01 design matrix evidence

Status: design matrix executed and reviewed on the iOS 27 simulator and the
Android emulator with isolated synthetic data. This is release-gate evidence for
[P08-01](../../roadmap/roadmap.json) acceptance: four tabs, sheets, both digit
styles, nine brand accents, light/dark and 1.0/1.3 text scales, with motion
recorded separately.

This is **not** physical-device evidence, not a complete Cartesian product of
every setting, and not a translation sign-off. Design findings and their
disposition are recorded below.

## Revision and environment identity

- Source: `e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4` plus the uncommitted working
  tree (the P08 design-capture files, the probe test-only fixes and the iOS
  sign-extension harness are uncommitted). This is **not** a clean-HEAD build.
- iOS: already-booted iOS 27.0 (24A434) iPhone 18 Pro Max simulator
  `50709DF0-FEA9-4B34-9DE6-6172D868875D`; isolated synthetic probe app
  `com.motovault.app.privacyprobe`; Xcode 27.0 (27A266a). Build identity and
  hashes: [p08/ios/provenance/p08-ios-provenance.txt](p08/ios/provenance/p08-ios-provenance.txt).
- Android: existing Pixel_10 `emulator-5554`, Android 17/API 37, arm64; isolated
  synthetic probe app `com.motovault.app.privacyprobe`; probe APK
  `73a3ce93f164fc890a50523abca20c43e6719f2140e5716e88704a4444812479`, test APK
  `7f7c82fbae147c414896f928f57a9629282ed007f418f3c9de98c076701c7713`. Full
  identity, commands and artifact hashes:
  [p08/android/android-capture-notes.md](p08/android/android-capture-notes.md).
- Synthetic data only: iOS `VaultState.seedData()` through `--seed`; Android the
  production UI hosted by the isolated `FuelEditFixtureActivity` with
  `VaultState.seedData()` in a temporary database. No owner record, document
  image, credential or precise location was used. No device was booted, erased,
  reset, reinstalled or had system appearance, font scale or locale changed.
- iOS storage/migration regression on the same build:
  [p08/ios/provenance/p08-ios-storage-probe-full2-run.txt](p08/ios/provenance/p08-ios-storage-probe-full2-run.txt)
  (PASS 22 native storage checks).

## Method

- iOS matrix: `MotoVaultUITests/P08DesignMatrixTests` on the pinned simulator,
  launched per case with `--seed` plus tab/sheet arguments and driven only
  through in-app Profile controls. Command and per-case mapping:
  [ios-capture-notes.md](p08/ios/ios-capture-notes.md). Final run: 8/8 tests
  passed; screenshots are the kept xcresult attachments (36 PNGs). A smaller
  direct-display capture set under `p08/ios/simctl/` records the seven sheets and
  four tabs independently of XCTest.
- Android matrix: `adb` UI-automation dumps plus `input tap/swipe`, `screencap`
  and `screenrecord` against the isolated synthetic host. Exact commands and
  per-case table: [android-capture-notes.md](p08/android/android-capture-notes.md).
- Recording is separate from stills. A still cannot establish motion, and these
  recordings are not a measured timing or pixel-parity comparison.

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

## Coverage matrix

Links point at representative files; the case tables in the two capture notes
list every artifact.

| Requirement | iOS (simulator) | Android (emulator) | Result |
| --- | --- | --- | --- |
| Four tabs, light | [profile](p08/ios/xcresult/p08-ios-tab-profile-light-bn.png), [garage](p08/ios/xcresult/p08-ios-tab-garage-light-bn.png), [documents](p08/ios/xcresult/p08-ios-tab-vault-light-bn.png), [records](p08/ios/xcresult/p08-ios-tab-history-light-bn.png) | [garage](p08/android/p08-android-garage-bn-bn-light.png), [documents](p08/android/p08-android-documents-bn-bn-light.png), [records](p08/android/p08-android-records-bn-bn-light.png), [profile](p08/android/p08-android-profile-bn-bn-light.png) | Pass |
| Four tabs, dark | [profile](p08/ios/xcresult/p08-ios-tab-profile-dark-bn.png), [garage](p08/ios/xcresult/p08-ios-tab-garage-dark-bn.png), [documents](p08/ios/xcresult/p08-ios-tab-vault-dark-bn.png), [records](p08/ios/xcresult/p08-ios-tab-history-dark-bn.png) | [garage](p08/android/p08-android-garage-bn-bn-dark.png) + the matching dark tab files | Pass |
| Bangla and English copy | [BN](p08/ios/xcresult/p08-ios-tab-profile-light-bn.png) / [EN](p08/ios/xcresult/p08-ios-tab-profile-light-en.png) profile, EN garage | [BN garage](p08/android/p08-android-garage-bn-bn-light.png) / [EN garage](p08/android/p08-android-garage-en-en-light.png), EN quick add | Pass; copy findings D2/D5 |
| Both digit styles, independent of language | BN text + Latin digits, EN text + Bangla digits on Profile and Garage | [BN text + Latin digits](p08/android/p08-android-garage-bn-en-light.png), [EN text + Bangla digits](p08/android/p08-android-garage-en-bn-light.png) | Pass |
| Text scale 1.0 and 1.3 | [BN 1.3 profile](p08/ios/xcresult/p08-ios-tab-profile-light-bn-scale13.png), [BN 1.3 garage](p08/ios/xcresult/p08-ios-tab-garage-light-bn-scale13.png), EN 1.3 | [BN 1.3 profile](p08/android/p08-android-profile-large-text-bn-bn-light.png), [BN 1.3 records](p08/android/p08-android-records-large-text-bn-bn-light.png) | Pass with findings D1/D4 |
| Nine brand accents | Appearance row start/end (1–8, 9), KTM-orange and Royal-Enfield-brown Garage in light and dark | Two swatch frames covering 1–8 and 2–9, KTM and Royal Enfield applied in light and dark | Pass; contrast ratios are covered by the unit test below, accessibility by D6 |
| Sheets | quick add, fuel, service, document add, backup, privacy, expense | quick add (BN/EN), fuel, service, expense, document add, backup, privacy | Pass; copy/layout findings D2/D3/D7/D8 |
| Secondary production screens | — | retained prior probe captures: backup create review/confirmation, renewal checklist (dark/expense/privacy/document), recovery success/conflict, fuel-edit footer, maintenance schedule | Supplementary; see [prior-probe-cache README](p08/android/prior-probe-cache/README.md) |
| Motion (normal) | [p08-ios-motion-normal.mov](p08/ios/recordings/p08-ios-motion-normal.mov) | [p08-android-motion-normal.mp4](p08/android/recordings/p08-android-motion-normal.mp4) | Recorded; see limits |
| Reduced motion | [p08-ios-motion-reduced.mov](p08/ios/recordings/p08-ios-motion-reduced.mov) + ON-state still | [p08-android-motion-reduced.mp4](p08/android/recordings/p08-android-motion-reduced.mp4) + ON-state still | Recorded; not a timing measurement |

The nine brand accents are additionally verified by
`AppearanceTest.everyBrandHasReadableColorsInBothModes`
(`composeApp/src/commonTest/.../AppearanceTest.kt`), which asserts accent/label
contrast thresholds for all nine brands in light and dark. That is a colour-math
check on the palette, not proof that every component renders correctly; the two
extreme accents and the swatch-row captures are the rendered evidence.

## Findings and disposition

These are observations from the reviewed screenshots. No product code was changed
by this evidence slice.

| ID | Finding | Platform | Disposition |
| --- | --- | --- | --- |
| D1 | Records filter chip row clips a chip at the right edge (visible at 1.0 and 1.3); the row scrolls but has no fade/ellipsis cue. At 1.3 the third Records action ("খরচের বিস্তারিত") is also cut mid-label; that row is horizontally scrollable, so no action is lost. | Android | Cosmetic/affordance follow-up; not a lost control. Keep for design review before the release candidate. |
| D2 | Locale copy: English expense sheet empty amount renders "Amount: No amount"; Bangla renders the duplicated word "টাকা: টাকা দেওয়া হয়নি" (`cost_field_value` + `cost_empty_amount`). English quick-add text "Arrange by typing or pasting" / "A mechanic, bKash or Nagad message" is awkward. | Android + iOS | Copy defect for P08-05 (English/Bangla pair review by a named human). |
| D3 | Backup sheet is taller than the viewport at first position; the warning is clipped at the fold and no primary action is visible until the body scrolls. | Android | Layout observation for the release-candidate design review. |
| D4 | Text scale 1.3: Profile theme segment truncates "সিস্টেম ডিফল্ট" to "সিস্টেম"; the bike card wraps. Essential numbers and actions remain visible. | Android | Layout observation; needs a decision (wrap vs segment width) before release. |
| D5 | Synthetic seed/demo data stores Bangla display labels (`চেইন`, `ড্রাইভিং লাইসেন্স`, `ট্যাক্স টোকেন`) and the English UI renders them unchanged. This is demo/owner-entered data, not inline UI copy, but it leaks Bangla into an English demo state. The Profile also still exposes a "New concepts (demo)" block and "Reset demo data". | Both | Release-gate clean-up item (P08 design: remove obsolete demo claims from enabled flows); track with the P08 release tasks, not a silent fix here. |
| D6 | The nine brand-colour swatches expose no accessible name; the ninth swatch is only partly visible at rest and requires an unmarked horizontal scroll. The accessibility tree shows unlabeled colour buttons. | Both | P08-02 physical-device accessibility / TalkBack-VoiceOver pass; a semantics fix is needed before release. |
| D7 | Privacy sheet's bottom card is clipped behind the fixed action; the service-sheet parts chip row clips a fourth chip (row scrolls). | Android | Layout observation for the release-candidate design review. |
| D8 | Expense date field renders `2026-11-12` in Latin digits while the selected number style is Bengali. | Android | Presentation inconsistency; confirm whether the native date control is exempt before release. |
| D9 | Dark-mode expiry warning badge reads red-on-maroon with visibly lower contrast than light mode; no ratio was measured. | iOS | Flag for the release-candidate design review; needs a measured contrast check. |

No clipping of primary actions was observed at 1.3 on iOS. Findings D5–D9 need a
release decision; none of them is silently counted as accepted.

**Resolution update (12 November 2026, owner-authorized):** D1/D4 (Records rows
clipped at 1.0/1.3) and D5 (Profile theme segment truncated at 1.3) were fixed by
wrapping the Records rows with the v3 `.row-wrap` pattern and letting segmented
options grow/wrap; Android 1.0/1.3 and iOS 1.3 captures verify the fix. D6 (nine
unlabeled brand-colour swatches and the clipped ninth) was fixed on 13 November
2026: the row now wraps as a radiogroup matching the v3 `.swatches` pattern and
each swatch has a reviewed accessible name and selected/radio semantics,
verified on the Android emulator and iOS simulator
([evidence](P08-02-swatch-accessibility.md)); spoken physical TalkBack/VoiceOver
verification stays in P08-02. Demo
claims/data, the expense date digit style, and the backup/privacy sheet
below-the-fold behaviour remain for the release decision (the sheets are
scrolling bodies with a fixed footer; no control is lost). See
[P08-01-layout-resolutions.md](P08-01-layout-resolutions.md).

## Motion

Recordings (kept separate from stills):

- iOS normal: `p08/ios/recordings/p08-ios-motion-normal.mov` — tab switches and
  quick-add open/close.
- iOS reduced: `p08/ios/recordings/p08-ios-motion-reduced.mov` — same sequence
  after enabling the in-app Reduce motion switch; the ON state is also retained as
  a still so the recording is not mislabelled.
- Android normal/reduced: `p08/android/recordings/p08-android-motion-normal.mp4`
  (12.98 s) and `p08-android-motion-reduced.mp4` (12.36 s), both with the
  quick-add open/close and tab switches; the reduced clip is smaller for the same
  interaction set, consistent with fewer animated frames (file size is not a
  timing measurement).
- iOS clip lengths: normal 25.8 s, reduced 32.8 s (the reduced clip also contains
  the scroll to the toggle, the ON-state check and a longer launch window).

Review scope: the recordings show the expected transitions and that the
reduced-motion switch changes the flow; they do not establish platform timing
parity with the v3 prototype and were not measured frame-by-frame.

## Limits

- iOS and Android simulator/emulator only. Physical-device accessibility,
  reliability, performance, camera/share/voice and battery remain P08-02.
- Representative matrix, not every combination: sheets were captured mostly in
  light/Bangla; OS-level dark, OS dynamic type, OS reduced motion, landscape,
  split screen, keyboard/safe-area edge cases and notifications permission
  surfaces are not all covered here.
- No TalkBack/VoiceOver pass; D6 is an accessibility-tree observation, not spoken
  output. No Bangla copy sign-off is claimed.
- The iOS build is uncommitted and was rebuilt during this session; the capture
  artefacts identify that build, not a published release.
