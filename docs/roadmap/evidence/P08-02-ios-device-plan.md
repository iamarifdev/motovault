# P08-02 physical iPhone pass — one-run plan

Purpose: a ready-to-execute single pass for the iOS physical-device slice of
P08-02. It is not evidence yet: it requires an iPhone connected to this Mac and
explicit owner authorization for development signing and physical-device testing.

## Preconditions (owner)

1. iPhone connected by cable, **unlocked and trusted** on this Mac (`xcrun
   devicectl list devices` shows it as available). Keep the screen on for the run.
2. Owner authorization to install a **development-signed isolated build** on the
   phone. Use only `com.motovault.app.privacyprobe` (synthetic data); the owner
   app `com.motovault.app` must not be installed over or modified.
3. A development team/profile available to Xcode. Signing material is never added
   to the repository or evidence.
4. Confirm whether VoiceOver may be turned on for a short assisted session and
   restored afterwards.

## Build and install (one time)

- Build the isolated probe app for the connected device, e.g.:
  `xcodebuild -project iosApp/MotoVault.xcodeproj -scheme MotoVault -configuration Debug -destination 'platform=iOS,id=<UDID>' -derivedDataPath build/p08-ios-device MOTOVAULT_BUNDLE_ID=com.motovault.app.privacyprobe DEVELOPMENT_TEAM=<team> build`
- Install with `xcrun devicectl device install app --device <UDID> <MotoVault.app path>`
  and verify the installed bundle id/version + binary hash.
- Launch with `--seed` and synthetic arguments only; capture the app-data
  container marker so the isolated store is provable.

## Single pass (run once, in this order)

1. **Identity/provenance**: device model, iOS version, UDID (or sanitized device
   identifier), Xcode version, app binary SHA-256, source revision, `git status`
   confirmation that the tree is not clean.
2. **Automated suites on device**: run the existing UI tests with
   `xcodebuild test -destination 'platform=iOS,id=<UDID>'` for
   `MotoVaultUITests/CaptureFlowTests` and `P08LocalizationTests`; retain the
   `.xcresult` and export screenshots. These cover the capture/share boundary and
   the language switch on real hardware.
3. **Native camera/share/voice** (physical-only): synthetic printed odometer
   target photo → candidate selection → Save/Undo with an on-device data
   readback; share sheet → MotoVault extension → prepare → intake; Bangla voice
   capability disclosure and manual fallback. Capture screenshots and, where
   allowed, a short recording. Do not retain raw environment photos or audio
   beyond the synthetic target.
4. **Permission/denied paths**: camera permission ask/deny/allow, notification
   permission ask/deny, file picker cancel, and locked-device behavior with a
   passcode set. Record actual system dialog text and app fallback.
5. **VoiceOver (short assisted session)**: spoken labels/focus order for Garage,
   Vault, Records, Profile and the nine brand-colour swatches; confirm the
   unlabeled-swatch finding. Record observations in the report (screenshots do
   not prove speech).
6. **Performance/energy**: Instruments (or `xcrun xctrace`) launch-time, memory,
   scroll hitch and energy for a short scripted walkthrough on the iPhone; record
   measured budgets next to the Android measurements.
7. **Restore**: turn VoiceOver back off (if enabled), reset any permissions
   changed for the isolated bundle, remove the probe app if the owner asks, and
   verify the owner app/data were untouched.

## Evidence layout

`docs/roadmap/evidence/p08/p08-02/ios-device/` with sanitized screenshots,
`.xcresult`-exported attachments, a `provenance.txt` (device/build/source IDs),
and an `ios-device.md` report following the verification template. Do not include
owner documents, raw photos, audio, credentials or signing material.

## Gaps this pass cannot close

Representative-owner pilot (P08-03) and the signed release candidate (P08-04)
remain separate human/signing gates.
