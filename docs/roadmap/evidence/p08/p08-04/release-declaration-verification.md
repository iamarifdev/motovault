# P08-04 release declaration verification (build-level)

Date: 13 November 2026 (Asia/Dhaka). Basis: the isolated probe APK
`ccf20f29e00265212da9b6c7dc3f2388d9dd6e3d7e521a80b0909378c3415955`
(built from `e6c59bf` + uncommitted P08 tree; `versionName 1.0.0-privacyprobe`,
`versionCode 1`, minSdk 24, targetSdk 34). The release `applicationId` variant is
`com.motovault.app`; the probe suffix only changes the id/dir. This is build-level
verification, not a signed release.

## Commands and artifacts

- `apkanalyzer manifest permissions|target-sdk|min-sdk|version-*` on the APK →
  [permissions-and-version.txt](permissions-and-version.txt)
- `./gradlew :composeApp:dependencies --configuration releaseRuntimeClasspath`
  filtered for ML Kit/Firebase/transport/GMS →
  [release-dependency-privacy-scan.txt](release-dependency-privacy-scan.txt)
- source inspection for network/location/speech/biometric usage.

## Permissions in the release manifest (and why)

| Permission | Reason in this app |
| --- | --- |
| `CAMERA` | Owner-initiated document/odometer capture via system camera (`TakePicture`). |
| `RECORD_AUDIO` | Bangla/English voice entry through the platform `SpeechRecognizer`; on-device mode is explicit, and the app states it does not save audio. |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Optional "auto location" for fuel/service logs, foreground `LocationManager` only when the owner enables it. No background location. |
| `USE_BIOMETRIC` | Optional app lock via `BiometricPrompt`. |
| `POST_NOTIFICATIONS` | Local reminder notifications (Android 13+). |
| `RECEIVE_BOOT_COMPLETED` | Reschedule local reminder alarms after reboot/time/timezone change (also `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED` receiver filters). |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Declared by the app/GMS stack; **no app HTTP client exists** (source scan). Used by Google Play services ML Kit model delivery/telemetry and user-initiated external browser links; vault data is not uploaded by app code. |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX auto-generated signature permission for non-exported dynamic receivers. |

## Dependency facts (privacy relevant)

- Text recognition uses **unbundled** Google Play services ML Kit:
  `com.google.android.gms:play-services-mlkit-text-recognition:19.0.1` +
  `play-services-mlkit-text-recognition-common:19.1.0`. The OCR model is provided
  by Google Play services (may be downloaded/updated by GMS on first use), so
  "works fully offline on first use" is not claimed.
- Transitive Google components: `play-services-base/basement/tasks`,
  `com.google.android.datatransport:transport-*`,
  `firebase-encoders`/`firebase-components` (ML Kit logging plumbing).
- **No analytics/crash-reporting SDK is configured by the app** — dependency scan
  finds no firebase-analytics, crashlytics, sentry, amplitude, appsflyer,
  mixpanel, datadog, newrelic or bugsnag. Store declarations should still note
  that Google Play services operates under Google's own policy.
- No cloud sync/account code; vault persistence is local (Room + protected files).

## Store/privacy text correction

The earlier draft line "no analytics/telemetry SDK in this build" remains true
for app-configured SDKs, but the declarations must disclose the Google Play
services ML Kit text recognizer and the INTERNET/network-state permissions, and
must not claim fully offline first-use OCR. Update the store listing/privacy
policy from this table before submission.
