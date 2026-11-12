# P07 native camera ownership followup

Date: 12 November 2026 (Asia/Dhaka). Base revision: `ee36e75dd5d03e70c8e14ba8930b9df6f134b14b`, uncommitted P07 working tree. Current source and native artifacts are identified by `P07-native-source.json`. `P07-native-source-before-capture-owner.json` preserves the previous Android OCR followup's identity.

## Finding and bounded change

Source inspection found that `MotoVault` backgrounds/locks on native pause, hides private content and disposes its sheet. The camera launcher and result state were remembered inside that sheet's photo session. An external camera can therefore outlive its callback owner. This was a production-source finding; no physical owner-vault failure is claimed.

`OdometerCaptureState` now owns one ephemeral acquisition at the root, outside the private-content visibility gate. A result can wait in RAM while the vault remains locked. Only the existing owner-visible odometer sheet can show candidates or apply one; the existing final Save and revision-aware writer remain required. Closing or changing the sheet discards the photo and invalidates delivery. A pending discarded request cannot be reassigned to a new request before its terminal callback.

The native camera adapters, permission-on-action behavior and privacy policy are unchanged. No authentication bypass, persistent photo, automatic Save, new copy or new visual state was added. This restores the established v3 camera-return/manual-review interaction. The isolated shared capture fixture uses the same root ownership pattern.

## Observed verification

| Scope | Result and limits |
| --- | --- |
| New focused state tests | `OdometerCaptureStateTest`: 3 tests, 0 failures/errors/skips. Covers retained result until explicit close, discarded pending-result isolation, native cancel/manual return and retry after denial. |
| Android actual external camera | Existing Pixel 10 emulator in a temporary read-only instance, isolated privacy-probe package, camera permission pre-granted. Real external camera pause disposed the review consumer. Back/cancel remounted it, the same root owner received cancellation, review closed and the private scratch directory was empty. |
| Android harness boundary | The isolated `CaptureOwnerFixture` deliberately removes its consumer on real lifecycle pause. It is a composition/callback lifetime check, not a native-authentication, physical camera or complete locked-vault end-to-end check. |
| iOS affected runtime | Fresh shared framework and isolated native host on the existing iOS 27 iPhone 18 Pro Max. Only `CaptureFlowTests/testCaptureNativeCameraCancellation` ran, successfully. Both screenshots were exported and visually reviewed: native virtual camera opens, then Cancel returns to the manual sheet with its fixed Save and keypad. |
| iOS harness boundary | The Compose XCTest tree still exposes only its root. Frame/test execution plus manually reviewed screenshots establish this checkpoint, not VoiceOver semantics, shutter/OCR accuracy, owner authentication or physical-device behavior. |
| Compilation and portable gates | Android debug/probe/test APKs, iOS compilation/link and native host build passed. Localization, Bangla copy, fuel UI literals, responsibility, dependency, package-cycle, fuel-policy and roadmap checks passed after the code change. |

Android output: `PASS actual external camera pause, consumer disposal/remount, retained root callback, native cancellation and empty scratch cache`.

The earlier full 320-test suite, 54 tooling tests, parser/writer checks, OCR corpus and unchanged language matrices were reused. Only the three new state tests and the affected native-camera paths ran for this followup. No audio or cloud service was used.

The first iOS build command split its multiword destination argument and exited before building or running a test. Using the exact allowed destination ID corrected it. The failed command is retained separately. Attachment export initially needed access to Xcode's temporary report cache; exporting with that access succeeded without rerunning the test.

## Reproduction and evidence

Use the same local Gradle/Kotlin caches and JDK 17 documented in `P07-native-verification.md`. Commands were routed through rtk. The focused build selects `:composeApp:testDebugUnitTest --tests com.motovault.app.ui.OdometerCaptureStateTest`, assembles debug/privacy-probe/test APKs and compiles/links `iosSimulatorArm64`.

The Android probe is selected by `onlyCaptureOwner=true` on `NativeAssetInstrumentation`. It launches only the isolated fixture and uses the actual external-camera contract. The iOS build uses project/scheme MotoVault, Debug, `MOTOVAULT_BUNDLE_ID=com.motovault.app.privacyprobe`, destination `id=50709DF0-FEA9-4B34-9DE6-6172D868875D` and the existing derived-data directory. Test selection is `-only-testing:MotoVaultUITests/CaptureFlowTests/testCaptureNativeCameraCancellation` with `test-without-building`.

Actual screenshots:

- `P07/screenshots/p07-ios-owner-camera-open.png`
- `P07/screenshots/p07-ios-owner-camera-cancelled-manual.png`

Machine-local logs and the xcresult are under `build/p07-capture-owner/`. `P07-02-capture-owner-results.json` records their hashes and exact scope. No screenshot is substituted for a required physical-device, authentication or accessibility result.

Physical Android/iPhone camera, glare/blur, locked/unlocked acquisition, native back/focus, accessibility and the representative-owner pilot remain open in P07. Online Bangla voice remains a separate review-only plan by owner decision. This followup does not complete P07.
