# iOS release metadata review (build-level, 13 November 2026)

Scope: `iosApp` project configuration and native metadata, reviewed from source
(no signed build, no physical iPhone per WD-01). Findings that need owner copy
sign-off or signing material are flagged, not applied.

## Version and deployment

| Field | Value |
| --- | --- |
| `MARKETING_VERSION` | 1.0.0 |
| `CURRENT_PROJECT_VERSION` | 1 |
| Bundle id | `$(MOTOVAULT_BUNDLE_ID)` (probe runs override to `…privacyprobe`) |
| CaptureShare extension id | `$(MOTOVAULT_BUNDLE_ID).capture-share` |
| App deployment target | iOS 17.0 |
| Extension deployment target | iOS 15.0 |
| Device family | iPhone + iPad (`1,2`) |

## Usage descriptions and localization — **open gap**

`MotoVault/Info.plist` declares camera, microphone, speech recognition, photo
library and when-in-use location usage strings. Localized overrides exist in
`en.lproj/InfoPlist.strings` and `bn.lproj/InfoPlist.strings` **only for camera,
microphone and speech recognition**. The result at this revision:

| Key | Base Info.plist | bn override | en override | Effect |
| --- | --- | --- | --- | --- |
| `NSCameraUsageDescription` | EN | BN | EN | OK both languages |
| `NSMicrophoneUsageDescription` | EN | BN | EN | OK both languages |
| `NSSpeechRecognitionUsageDescription` | EN | BN | EN | OK both languages |
| `NSLocationWhenInUseUsageDescription` | **BN** | missing | missing | English UI shows Bangla permission text |
| `NSPhotoLibraryUsageDescription` | **EN** | missing | missing | Bangla UI shows English permission text |

This is a native-permission localization defect discovered after the P08-05 copy
sign-off. **Fix requires new EN/BN copy and a named human review**, so it is
proposed here rather than silently edited:

- BN photo library: "ভল্টে রাখতে ডকুমেন্টের ছবি বেছে নিন।"
- EN location: "Attach current location to a fuel or service record only when you ask."
- EN photo library (keep base): "Select original document photos to import into your private vault."
- BN location (keep base): "আপনার অনুরোধে জ্বালানি বা সার্ভিস রেকর্ডে বর্তমান লোকেশন যোগ করতে।"

Action: owner (or named reviewer) approves the copy, then add the missing entries
to both `InfoPlist.strings` files, run the localization gate, and re-record P08-05
coverage. Until then this is a release-blocking localization item for App Store
submission.

## Privacy manifest

`MotoVault/PrivacyInfo.xcprivacy` exists: `NSPrivacyTracking=false`, no tracking
domains, no collected data types, and accessed-API reasons for UserDefaults
(CA92.1), File Timestamps (C617.1) and System Boot Time (35F9.1). No required-reason
API category for Disk Space/Active Keyboards is declared; confirm against the
final release binary (Apple's scanner output is authoritative).

The **CaptureShare extension has no `PrivacyInfo.xcprivacy`**. Source review of
`ShareViewController.swift` finds no required-reason API: it reads the shared
container, `fileSizeKey` (not a listed reason category), JSON and `NSFileCoordinator`
— no UserDefaults, file timestamps or boot-time calls. The in-app manifest covers
the app target's `attributesOfItem` (FileTimestamp C617.1) and declared reasons;
Apple's release scanner output remains authoritative. Recorded as an open
release-review item, not a change.

## Entitlements

- App: app group `group.$(MOTOVAULT_BUNDLE_ID).capture`, keychain access group,
  application-identifier template.
- Extension: the same app group.
- Signing/provisioning is Automatic with the owner's team; **no signing material
  is in the repository** and no signed build exists. Distribution/provisioning
  verification stays with P08-04 signing authorization.

## Store text impact

- Do not claim iPhone physical verification; WD-01 waiver must be disclosed.
- Keep the ML Kit / Play-services and INTERNET disclosures from the Android
  declaration review; the iOS build's speech service option may process audio
  when the owner selects it, as the usage string states.
