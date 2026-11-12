# P08-04 core release-candidate pack

Status: **prepared, not released.** Signing material, distribution and the
published release are separate owner-authorized actions. This pack fills the
documentary part of P08-04 acceptance; the signed-candidate install/restore smoke
and actual distribution proof remain to be recorded.

## Release candidate identity (fill at build time)

| Field | Value |
| --- | --- |
| Candidate build | `:composeApp:assembleRelease` (unsigned), 13 Nov 2026 00:35 Asia/Dhaka |
| Android APK | `composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk` (118 MB), SHA-256 `50a000bd5cb43135143fdd2378a911cfc41aa7a68e278dc28782cfa9886b3133` |
| App id / version | `com.motovault.app`, versionName 1.0.0, versionCode 1, minSdk 24, targetSdk 34 |
| iOS archive/XCArchive SHA-256 | not built (no signing material/iPhone; WD-01) |
| Source revision + working-tree state | uncommitted P08 tree on `e6c59bf` |
| Build date/toolchain | 13 Nov 2026, Gradle 8.10.2 / JDK 17 / Kotlin+Compose (project toolchain) |
| Signing identity / distribution state | **unsigned; not authorized/signed** — release signing is an owner action |
| Release content check | release manifest has no probe `*FixtureActivity` entries; permissions identical to the declaration review ([android-release-build.txt](p08/p08-04/android-release-build.txt)) |

## Enabled features in the candidate

- Four-tab core (Garage, Documents, Records, Profile), quick-add, sheets/keypads,
  date chips, Undo, haptics, reduced motion.
- Fuel/service/renewal logging, cost insights and budgets.
- Encrypted local vault, backup/restore, recovery snapshots, migration safeguards.
- Localization BN/EN with runtime switch and independent digit style; notification
  and export copy localization.
- Capture: odometer photo (system camera + on-device recognition), explicit text
  share into the review queue, Bangla voice disclosure (offline provider where
  available).
- Reminders for services/documents (local notifications).
- Demo mode clearly labelled; synthetic seed data kept separate from owner records.

## Privacy and store declarations (draft — verify against final build)

- Data stays on device; no account, no cloud sync in this candidate.
- **Verified build-level declarations** (13 Nov 2026, probe APK `ccf20f29…`):
  see [release-declaration-verification.md](p08/p08-04/release-declaration-verification.md)
  for the `apkanalyzer` permission list, dependency scan and source checks.
- No app-configured analytics/crash-reporting SDK (scan finds none). The build
  does include **unbundled Google Play services ML Kit text recognition**
  (`play-services-mlkit-text-recognition:19.0.1`), its GMS transport plumbing and
  Google's own policy — the play-services model may be delivered/updated by GMS,
  so do not claim offline OCR on first use.
- INTERNET/ACCESS_NETWORK_STATE are declared although no app HTTP client exists;
  they come with the GMS stack and user-initiated external browser links. Vault
  data is not uploaded by app code.
- Camera/photos: owner-initiated document/odometer capture via the system camera
  and file pickers; no upload.
- Speech: platform `SpeechRecognizer`; on-device mode is explicit ("শুধু এই
  ডিভাইসে"), the phone-service option follows the device/provider, and the app
  states it does not save audio.
- Notifications: local reminder notifications only (POST_NOTIFICATIONS + boot/time
  reschedule receiver).
- Location: optional foreground auto-location only, owner-toggled; no background
  location, contacts or SMS permissions.
- Biometric: optional app lock via `BiometricPrompt`.
- Data deletion: backups and documents can be removed by the owner; uninstall
  removes the app store.
- Store listing/privacy policy text: fill from the final declarations; do not
  submit text that overstates iPhone physical verification (WD-01).
- `known-limits` to disclose honestly: iOS physical pass waived while no iPhone is
  available (WD-01); Android physical camera allowed path + native notification
  delivery verified, but scheduled/background reminder delivery, spoken TalkBack,
  real Bangla voice and unplugged battery are still unverified; **large-vault
  scroll jank measured 15.03 % (116/772 frames) on the physical phone vs the
  provisional ≤5 % budget** — root cause is the eager non-virtualized Records
  list; options and a recommendation are in
  [P08-02-large-vault-performance.md](P08-02-large-vault-performance.md)
  (separate budget vs LazyColumn virtualization). Decide before release.

## Release notes (draft)

Draft at release: core vault, backup/restore, migration safety, BN/EN
localization with runtime switch, reminder notifications, cost insights, capture
paths with review queue and Undo, accessibility improvements (wrapping layouts,
labelled brand swatches), design-matrix fixes. Keep final wording aligned with
what was actually verified.

## Support and recovery instructions (owner-facing)

1. If data seems missing: Profile → Diagnose/Recovery (existing in-app recovery)
   and choose the latest snapshot; do not clear app data.
2. Restore: Profile → backup & restore → pick `.mva` → enter the backup password →
   verify then restore; both apps keep the pre-restore state until confirmed.
3. If migration/restore is interrupted: reopen the app; the staged file is
   discarded and the original is preserved (covered by regression evidence).
4. Escalate with app version, build hash, device/OS and the exact on-screen error;
   never share documents, passwords or audio.

## Rollout / rollback criteria

- Rollout gate: signed candidate installs over the previous build on both
  platforms, restore/upgrade smoke passes, no data-loss/privacy defect open.
- Staged rollout: keep the previous build available for rollback; stop rollout on
  any data-loss/privacy/incorrect-guidance report.
- Rollback: reinstall the previous signed build (vault format must remain
  readable — migration is forward-only, so rollback requires a verified backup
  restore; document this before release).
- Distribution proof: store console release id/link, rollout percentage and
  post-publish install smoke, recorded when actually performed.


## Android release-variant build check (13 November 2026)

`:composeApp:assembleRelease` **succeeded** after fetching the declared
release-variant dependencies (offline cache was missing `skyDesignSystem`
release artifacts). Output is the **unsigned** APK recorded above; no signing
config exists in the repository, so the artifact cannot be installed as a
candidate until the owner provides signing material. `isMinifyEnabled = false`
in the release build type, so this run verifies compilation/packaging and the
release manifest, not R8 shrinking.

Checks recorded in [android-release-build.txt](p08/p08-04/android-release-build.txt):
app id/version/SDKs, the nine app permissions, and absence of probe fixture
activities from the release manifest.

## Localization parity check (13 November 2026)

Post-sign-off parity verified across all resource families:

- Compose `values/` vs `values-bn/`: all `string`, `plurals` and `string-array`
  keys match in every XML file (script comparison; no missing or extra keys).
- iOS `en.lproj/Localizable.strings` vs `bn.lproj/Localizable.strings`: 10/10 keys
  match.
- The **only** localization gap is the unlocalized `InfoPlist.strings` usage
  strings for location and photo library (open item above; proposed copy pending
  named review). `NSCameraUsageDescription`, `NSMicrophoneUsageDescription` and
  `NSSpeechRecognitionUsageDescription` are localized in both languages.
- Debug-only synthetic notification strings in `AppDelegate.swift` are inside
  `#if DEBUG` probe paths and are not release copy.

## Remaining before P08-04 can close

- Owner authorization + signing material for the release build.
- Install signed candidate on Android (available) and, per WD-01, record the
  iPhone limitation instead of blocking; repeat the restore/upgrade smoke path.
- Fill the identity/privacy tables from the real candidate and record actual
  distribution proof only when published.
