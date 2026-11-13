# P08-02 brand-colour swatch accessibility and wrap fix

Date: 13 November 2026 (Asia/Dhaka). Scope: fix the carried P08-01 finding D6 —
the nine brand-colour swatches were unlabeled buttons and the ninth was clipped at
rest — and verify it on the emulator and simulator. Spoken TalkBack/VoiceOver on
physical hardware is still outstanding in P08-02.

## Reference and change

The v3 prototype renders the accent colours as a **wrapping radiogroup**
(`design/app-ui-v3/src/styles/app.css`: `.swatches { display:flex; flex-wrap:wrap;
gap:10px }`, `role="radiogroup"` in `screens/Me.tsx`). The native row used a
horizontal scroll and each swatch was a bare clickable `Box`.

Changes in `ui/settings/SettingsAppearanceSections.kt`:

- the row is now a `FlowRow` (wraps, 10 dp gaps) inside a `selectableGroup()`,
  matching the prototype and removing the clipped ninth swatch;
- each swatch is a `selectable(selected, role = Role.RadioButton)` control with a
  `contentDescription` built from the existing reviewed resource
  `settings_brand_color_named` and the brand name resolved by catalogue ID
  (for example `KTM কালার` / `KTM color`), so selection state and the radiogroup
  role are announced instead of an unnamed button.

No new user-facing copy was introduced, no persistence/format changed, and the
selection behaviour (`brandOverride`) is unchanged.

## Verification

### Android emulator

- Rebuilt isolated probe APK
  `5c0b352f7fbca73be7580915f337cbd618ff3e2153a4523d38af3ed1d7a450e8`,
  installed on the Pixel_10 emulator (Android 17/API 37).
- Profile screen: the swatches now wrap into two rows (7 + 2 visible, no clipped
  circle) — [screenshot](p08/p08-02/android-emulator/profile-swatches-wrapped.png).
- `uiautomator` tree contains nine named swatches: `KTM কালার`, `CFMOTO কালার`,
  `Yamaha কালার`, `Honda কালার`, `Suzuki কালার`, `Bajaj কালার`, `TVS কালার`,
  `Hero কালার`, `Royal Enfield কালার`
  ([tree](p08/p08-02/android-emulator/profile-swatches-wrapped.xml)).

### iOS 27 simulator

- New test `MotoVaultUITests/P08LocalizationTests/testBrandColourSwatchAccessibility`
  launches the seeded probe, opens Profile and asserts at least nine labelled
  swatches; **test passed** on the pinned iPhone 18 Pro Max simulator
  (`build/p08-swatch-runs/swatch-ios-20261113-000351`).
- Accessibility tree shows all nine labels plus the caption
  ([tree](p08/p08-02/ios-simulator/accessibility-tree.txt)); the screenshot shows
  the wrapped two-row layout ([screenshot](p08/p08-02/ios-simulator/p08-ios-a11y-brand-swatches.png)).

### Gates

- `./gradlew :composeApp:testDebugUnitTest :composeApp:compileKotlinIosSimulatorArm64 --offline`:
  BUILD SUCCESSFUL; **327 tests, 0 failures/errors/skipped**.
- Portable literal, Bangla-copy, responsibility, dependency, package-cycle and
  fuel-policy gates pass; tooling unittest suite passes; `roadmap.py check` passes.


## Post-change regression runs (13 November 2026)

- **iOS design matrix**: full `MotoVaultUITests/P08DesignMatrixTests` suite on the
  pinned iOS 27 simulator re-ran after the swatch wrap/semantics change and the
  wrapping layout fixes: **8/8 tests passed** (`testExtremeAccentMatrix`,
  `testLanguageAndDigitMatrix`, `testMotionSequenceNormal`,
  `testMotionSequenceReduced`, `testProfilePreferencesAndReducedMotion`,
  `testSheetMatrix`, `testTabMatrixLightAndDark`, `testTextScaleMatrix`;
  run `build/p08-design-runs/design-regress-20261113-002937`, TEST SUCCEEDED).
- **Android native probe**: `NativeAssetInstrumentation` re-ran on the Android
  emulator against probe APK `ccf20f29…` (swatch fix + large-vault fixture
  added): **PASS 37 checks**.
- Physical-device re-verification of the swatch wrap/semantics passed accessibility-tree inspection on the Motorola Edge 50 Fusion (see below); spoken TalkBack announcements remain an assisted pass and physical iPhone stays waived under `WD-01`.
## Physical-device verification addendum (Motorola Edge 50 Fusion, 13 November 2026)

- Rebuilt and installed `composeApp-privacyProbe.apk` (`8bd3b17cb42704f4254b448a63a05b40ca9d340966b70d31d86f8d4bc6395b90`) on `motorola edge 50 fusion` (`Android 16 / API 36`, `font_scale=1.15`).
- `uiautomator` dump ([profile-swatches-wrapped-device.xml](p08/p08-02/android-device/profile-swatches-wrapped-device.xml)) and screenshot ([profile-swatches-wrapped-device.png](p08/p08-02/android-device/profile-swatches-wrapped-device.png)) verify all nine localized swatches wrap cleanly into an 8 + 1 two-row layout with zero edge clipping and `checked=true` on the active selection (`Yamaha কালার`):
  `KTM কালার`, `CFMOTO কালার`, `Yamaha কালার`, `Honda কালার`, `Suzuki কালার`, `Bajaj কালার`, `TVS কালার`, `Hero কালার`, `Royal Enfield কালার`.

## What this does and does not prove

- Proves: the controls have accessible names and selected/radio semantics in the
  Unity accessibility trees on both platforms, and the row wraps with all nine
  swatches visible at rest.
- Does not prove: spoken TalkBack/VoiceOver output, focus order or announcement
  quality on a physical device; the Android physical pass was taken before this
  change. That remains in P08-02 along with the physical iPhone pass.
- Build/device provenance: [p08-02 swatch verification README](p08/p08-02/README-swatch-verification.md).
