# P07 Motorola physical-device verification

Date: 12 November 2026 (Asia/Dhaka). Status: in progress, partial evidence only. The owner connected a Motorola edge 50 Fusion on Android 16/API 36 and agreed to point its camera at synthetic targets and provide a short Bangla phrase.

## Identity and isolation

The existing verified privacy-probe APK and test APK were installed into `com.motovault.app.privacyprobe`, which was not previously installed on this phone. The installed probe APK's SHA-256 matches the local verified artifact. `P07-motorola-device.json` records sanitized hardware/OS/APK identity; the serial is omitted. The updated privacy-probe APK hash matches the local build. `P07-native-source.json` records the current Android source and artifact hashes.

No primary MotoVault data was cleared. Real onboarding created a synthetic Yamaha MT-15 V2 with a 1,000 km baseline inside the isolated probe. Readback of its actual Room database confirms odometer 1,000 and zero ledger entries. This is physical-phone storage evidence for that synthetic setup, not an owner-record result.

## Observed so far

| Check | Outcome |
| --- | --- |
| Permission on photo action | Real native camera permission prompt appeared only after the photo action. |
| Camera denial | Denied once. Localized fallback appeared; the actual clickable Save ancestor was disabled during photo review. |
| Manual recovery | Manual entry returned with the 1,000 km value, fixed Save/keypad visible and camera retry available. |
| Native camera launch | Retried and selected `Only this time`. Actual Motorola camera controls appeared. |
| Target photo / OCR candidate | Failed on two synthetic `ODO 12345 km` targets, including a larger full-screen high-contrast target. Each real camera capture returned an image preview, but the review exposed no odometer candidate or Use action. The baseline stayed at 1,000 km. No raw preview screenshot was kept. |
| Manual fallback Save / Undo | Passed. Entered 12,345 km manually after the no-candidate result. Room readback showed 12,345 after Save; immediate Undo restored 1,000 km and zero ledger entries. This does not close candidate-based photo Save/Undo. |
| Native text share, warm app | Passed one basic Android `ACTION_SEND` flow. The owner selected MotoVault; the app review showed shared `ODO 12345 km` and candidate 12,345. Choosing Odometer opened the existing editor with 12,345 km and Save still required. Room readback showed 12,345 after Save and 1,000 after immediate Undo; the temporary capture receipt was also removed. No external message was sent. |
| Native text share, cold start before forwarding fix | Failed. After force-stop, the synthetic `ACTION_SEND` opened Garage without a review draft. Room remained at 1,000 km and zero ledger entries. |
| Native text share cold start after forwarding fix | Passed one automated run on the isolated probe. Readback first confirmed the synthetic 1,000 km baseline and zero ledger rows. After force-stop, an explicit synthetic `ACTION_SEND` reached `CaptureShareActivity` with Android `LaunchState COLD`, then forwarded to `MainActivity`. The review hierarchy showed `ODO 12345 km` and candidate 12,345; selecting Odometer opened the editor at the 1,000 km baseline. Room readback showed 12,345 after Save, then 1,000 after immediate Undo; ledger count stayed zero and the temporary receipt was removed. This run targeted the MotoVault component directly, so it did not test the Android chooser. |
| Unsupported MIME share | Pending. Synthetic `image/jpeg` reached the isolated `CaptureShareActivity`, but the guarded UI reader stopped on a package outside its allowlist. The unavailable-message UI was not verified. Room stayed at 1,000 km and zero ledger entries. |
| Share cancellation | Partial. The cold system ResolverActivity opened for synthetic text; Back was sent without selecting a target. Room stayed at 1,000 km and zero ledger entries, but the guarded UI reader could not verify return to Garage. |
| Local Bangla voice | Started with `শুধু এই ডিভাইসে` selected. The app reported that offline Bangla is unavailable on this device and offered manual entry or the separate phone speech service. No phrase was recorded and the phone speech service was not selected. |
| Audio / final storage | No audio files were found in the probe's private files/cache. Final Room readback remained at 1,000 km with zero ledger entries. |

Only the synthetic probe and expected camera/permission UI are used. No owner record, credential or device lock was changed. For the automated cold-share run, plugged-in stay-awake changed from 0 to 3 and screen timeout from 60,000 to 600,000 ms; both were restored and verified at 0 and 60,000 ms. The original OS font scale is 1.15.

Actual screenshots are under `P07/screenshots/motorola-odometer-before-camera.png`, `motorola-camera-denied.png` and `motorola-camera-denied-manual.png`. Hashes and case status are in `P07-motorola-results.json`; raw synthetic database copies remain under ignored `build/p07-motorola/`.

## Still required

Photo candidate selection/application and photo-based Save/Undo, clear/multiple/regression/range and blur/glare cases, Android return-to-Garage after cancel and unsupported-MIME UI, locked/duplicate cases, iOS physical share checks, a supported Bangla voice path and accessibility/parity checks remain open. The separate phone speech service was not selected because its processing mode requires an explicit choice. No raw audio was retained.

Physical iPhone evidence and representative-owner comparison remain required in P07. The separate online voice-provider plan remains in review by owner decision. The updated Android privacy-probe APK build passed once. The previous full unit/build suites were not repeated; no raw share payload or audio was retained.
