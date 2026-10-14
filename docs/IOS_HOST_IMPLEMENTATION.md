# MotoVault iOS host

The checked-in UIKit host is `iosApp/MotoVault.xcodeproj`, shared scheme `MotoVault`. It calls the active Kotlin `App()` through `MainViewController`, rather than a duplicate Swift UI. Minimum deployment target is iOS 15.0. The existing static `ComposeApp` framework is linked directly; the Gradle Xcode integration phase selects the device/simulator target and copies Compose resources.

The host retains its `UNUserNotificationCenterDelegate` as the application delegate and requests banner/list/sound presentation for foreground notifications. Permission and durable request reconciliation remain in the existing Kotlin reminder adapter. No remote push entitlement or background fetch is needed for local calendar notifications.

UIKit status-bar style follows the active Compose palette. Document dialogs temporarily publish their own appearance and restore the app palette on disposal. The host deliberately leaves the Compose controller's `overrideUserInterfaceStyle` unspecified: changing that trait to match the resolved palette would prevent System mode from observing device theme changes. Inspection brightness remains a separate scoped lease in `InspectionBrightness.kt`, including restoration while inactive.

Camera access uses `NSCameraUsageDescription`. Import uses the system document picker with copy/import semantics; it does not require full Photo Library access. A photo usage description is present for future direct photo selection, but the host does not ask for that permission at startup. Original bytes continue to live in private Application Support storage. There are no bundled owner credentials or synthetic vault records.

Build locally with native ARM64 Java 17 and `rtk` on PATH:

```sh
rtk run xcodebuild -project iosApp/MotoVault.xcodeproj -scheme MotoVault -configuration Debug -sdk iphonesimulator -destination id=<simulator-UUID> -derivedDataPath /private/tmp/motovault-ios-derived CODE_SIGNING_ALLOWED=NO build
```

The build phase disables Xcode user script sandboxing for Kotlin direct integration. It uses the machine's existing `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`; it does not download a runtime or create signing credentials. Open the project in Xcode to select a development team for a real device. No team, provisioning profile, signing identity or production cloud key is checked in.

The AppIcon asset uses the exact existing MotoVault mark geometry and light canvas/orange palette. `iosApp/tools/render-app-icon.swift` renders an opaque 1024×1024 PNG; the asset catalogue generates device variants. App Documents is visible in Files for owner-selected imports/exports. Private vault originals and Room database remain in Application Support.

As of 13 October 2026, [Apple requires Xcode 26 or later and iOS/iPadOS 26 SDK or later for App Store uploads](https://developer.apple.com/news/upcoming-requirements/?id=04282026a). This host was built with the installed Xcode/iPhoneSimulator26.0 SDK; a simulator build does not validate a signed device archive. Before distribution, set the intended bundle identifier, development team, release signing/provisioning, version/build number, launch assets, privacy declarations and store metadata. Validate device camera/OCR, foreground/background reminder delivery, brightness restoration, save/share permissions, large-font/accessibility and backup recovery. Current Compose 1.7.0 predates JetBrains' stable iOS release; a distribution dependency upgrade requires its own regression work. Physical-card and cloud isolation proof remain separate gates.

Runtime evidence is recorded in `docs/archive/evidence/continuation-verification/ios/`. A failed compile or simulator build never establishes native runtime behavior.

Final local checkpoint, 14 October 2026: the arm64 Debug host including AppIcon, privacy manifest and isolated probes builds successfully. Native tests and actual app interactions verify saved synthetic setup/restart, themes, Files/Vision import with original-byte preservation, native original viewing, reviewed summary persistence, actual saved-record passport view/share/Save to Files, and a visible local TEST notification after the actual permission prompt. The isolated passport fixture retains 70 records and a 100-line Bengali/emoji note; native verifier helpers accept valid JSON and reject a wrong independent digest. Files-picker verification UI remains unproved.

Inspection lease logs cover background restoration, foreground reacquisition, nested zoom and exit. Simulator observed `UIScreen` brightness stays 0.5 despite requested changes; physical luminance/readability remains a device gate. No simulator erase, uninstall or owner-vault reset was used. These checks do not establish future owner-expiry delivery, physical/background notifications, real-card OCR, live cloud restore or signed distribution. The final [native report](archive/evidence/continuation-verification/ios/README.md) records the exact evidence and limits.

Primary integration references:

Local privacy preparation (14 October 2026): `iosApp/MotoVault/PrivacyInfo.xcprivacy` declares no tracking domains, no tracking and no off-device data collection. Private documents, OCR fields, reminder preferences and Room records remain on the device; a future network sync implementation must revisit these declarations before enabling collection. User-selected export is a local sharing action, not an app telemetry sender.

The required-reason entries are `CA92.1` for app-only `NSUserDefaults` reminder preferences and legacy vault reads, `C617.1` for private document metadata used by orphan recovery, and `35F9.1` for elapsed-event timing in Compose's native dependency code. Apple limits `0A2A.1` to third-party SDK wrappers, so it is not used as an app-level substitute for the private-file reason. The Kotlin framework is static (`isStatic = true`); its code is linked into the host executable, so this direct integration packages the manifest as a host app resource rather than adding a framework-resource plugin. Independently distributed SDKs still need their own valid manifests.

Source manifest validation: `rtk run plutil -lint iosApp/MotoVault/PrivacyInfo.xcprivacy` passed. A bounded `nm -u` review of the current simulator static framework found file metadata symbols in Skia, POSIX and SQLite, `mach_absolute_time` in Skia's DNG code, and `statfs`/`fstatfs` in SQLite. Archive imports can include code removed from the final executable; they are not proof of live runtime calls. Final linked executable and packaged manifest verification must be recorded separately. A release SDK/vendor audit, signed device archive privacy report and App Store validation remain distribution gates. Disk-space reasons must be based on the actual SQLite call behavior, not added from a symbol name alone.

- [Apple required-reason API policy and bundle placement](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api)
- [Apple category APIs and approved reasons](https://developer.apple.com/documentation/bundleresources/app-privacy-configuration/nsprivacyaccessedapitypes/nsprivacyaccessedapitype)
- [Official Kotlin privacy-manifest guidance and Compose native API use](https://kotlinlang.org/docs/multiplatform/multiplatform-privacy-manifest.html)
- [SQLite native file handling source](https://sqlite.org/src/artifact/410185df49) uses `statfs`/`fstatfs` for filesystem classification and locking in its Apple path. That reference is not a match to the bundled binary's exact source version; verify the shipped SQLite build before a release. It does not establish the low-space behavior required for `E174.1`, so this local manifest does not invent a disk-space reason.

- [Kotlin direct Xcode integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html)
- [Compose UIKit/SwiftUI entry integration](https://kotlinlang.org/docs/multiplatform/compose-swiftui-integration.html)
- [Apple notification delegate lifetime](https://developer.apple.com/documentation/usernotifications/unusernotificationcenter/delegate)
- [Apple foreground presentation callback](https://developer.apple.com/documentation/usernotifications/unusernotificationcenterdelegate/usernotificationcenter(_:willpresent:withcompletionhandler:))
