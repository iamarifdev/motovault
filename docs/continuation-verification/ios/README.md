# iOS host verification

Target: existing iPhone 17 Pro simulator `93CA20AD-7FA0-4564-AC1D-E1CF507C02A2`, iOS 26.0. It was booted without erasing or resetting its data.

The arm64 Xcode Debug simulator host builds, installs and launches. Native framework/resource integration and Swift/Kotlin Boolean bridge compiled successfully. Onboarding selected the real KTM Duke 125 reference photo, entered explicitly synthetic edition data, confirmed the baseline and created the garage. The saved bike survived process termination/relaunch and subsequent app updates. Room database reports schema version 2.

Screenshots `01` through `06` prove saved setup, default System selection, live System dark, explicit Light overriding a dark device, explicit Dark overriding a light device and Dark persistence after restart. The app was restored to System and the simulator to light. Brand marks, model photo and readable status bars remain visible.

Native service-passport export used the actual saved synthetic bike plus owner-reviewed name `SYNTHETIC OWNER`. It contained zero service entries at that checkpoint, honestly labelled. `07-native-passport-viewer.png` proves PDF viewing; `08-passport-share-sheet.png` shows the paired PDF/JSON native share sheet. Save to Files completed locally; the saved PDF appeared in Files Recents. No file was sent to an external recipient.

Native Files import used the existing two-page `SAMPLE-tax-token.pdf` copied read-only from the Android simulator. English Vision recognition supplied editable number, holder, issue and expiry candidates. Confirmation was required before saving. `09` proves the saved tax wallet; `10` proves the native original two-page viewer. The source and managed iOS original both have 111162 bytes and SHA-256 `b386e073c880a9c8b0413eb6af6f3fdabe375613929b602799a3846182ded1f2`. Summary editing changed the synthetic holder to `IOSSAMPLE HOLDER`, then required confirmation again; the change survived an app update/relaunch while the printed original retained `SAMPLE HOLDER`. `11` shows document-only light inspection.

The macOS Simulator accessibility tree did not expose Compose screen semantics. Desktop CUA clicks/input worked, but its wheel/drag scrolling did not. A confined native XCUITest swipe moved the actual app correctly; its foreground assertion passed. The screenshot attachment is in `native-scroll/`. Absence of macOS AX entries does not establish app accessibility failure; VoiceOver/device QA remains pending.

Camera permission was denied deliberately and the user returned safely to the import flow. No simulator camera, physical camera or real-card OCR accuracy is claimed. Initial disk-full and plist-sanity launch failures were corrected before the successful proof above.

Final 14 October checkpoint: the arm64 Debug host including AppIcon, privacy resource and isolated probes builds successfully. Native confined swipe tests pass. The isolated passport fixture renders 70 records and a 100-line Bengali/emoji note into a 16-page Quartz PDF; text extraction retains all records and the final note, and visual page checks confirm Unicode and the final QR. Paired PDF/JSON and PNGs are saved as `12-*`.

The native fixture verifies valid JSON and rejects a wrong independent digest (`13-native-probe.log`). This proves the actual iOS helper path, not the Files-picker verification UI. The actual app notification prompt was accepted for the local test, and the isolated 15-second TEST notification scheduled without error, reached the foreground delegate and displayed a banner (`14`). This does not establish delivery of a future owner expiry or physical background delivery.

Actual document inspection logs show one active lease requesting full brightness, background restoration, foreground reacquisition, two leases during nested zoom, one after closing zoom, and zero with restoration after Exit inspection (`13`, `15`). Simulator `UIScreen` observation stayed at 0.5 despite requested 1.0, so physical luminance remains unverified. Real device signing, camera quality, physical brightness/readability and cloud restore remain explicit gates.
