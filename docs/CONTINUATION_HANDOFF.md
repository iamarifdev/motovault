# MotoVault continuation handoff

Prepared 12 October 2026. Workspace: `/Users/arif/workspace/bbn/motovault`.

Current completion checkpoint: [CONTINUATION_PROGRESS.md](CONTINUATION_PROGRESS.md), updated 14 October 2026. This handoff preserves the original baseline; use the current checkpoint and linked native evidence for delivered status and remaining gates.

## 1. Objective and current position

Continue the existing native application until the remaining original product scope is implemented and verified. Preserve the approved redesign, real document originals, existing owner records and System/Light/Dark themes. This is an implementation continuation, not a new redesign or a new application.

MotoVault is currently a working offline Android MVP for one motorcycle: setup, model selection, maintenance, fuel, history and a document vault. The latest document implementation is committed on `main` as `0d687bc feat(documents): implement scanning and original document flow`. Its preceding UI/theme commit is `ae2e585 feat(ui): redesign MotoVault with model selection and system themes`.

The broader digital-passport scope remains incomplete: factory-aware setup, Room persistence, service-passport export, cloud backup/sync, background reminders, maximum-brightness inspection and runnable iOS delivery. Code compilation and synthetic simulator checks do not establish physical-camera OCR quality or release readiness.

This document records the inspected baseline, proposed implementation sequence and completion criteria. Recheck Git and source state at the beginning of the next conversation. Do not assume the simulator is still running or the physical phone is connected.

## 2. Design references and precedence

Read the following before changing visible UI. All paths below were verified to exist when this handoff was prepared.

| Reference | Exact path and role |
| --- | --- |
| Overall approved app prototype | [motovault-redesign.html](/Users/arif/Documents/Codex/2026-10-10/red/outputs/motovault-redesign.html) |
| Prototype behaviour | [motovault-workflows.js](/Users/arif/Documents/Codex/2026-10-10/red/outputs/motovault-workflows.js) |
| Prototype workflow styles | [motovault-workflows.css](/Users/arif/Documents/Codex/2026-10-10/red/outputs/motovault-workflows.css) |
| Prototype motorcycle catalogue | [bike-catalogue.js](/Users/arif/Documents/Codex/2026-10-10/red/outputs/bike-catalogue.js) |
| Approved document-flow design | [document-design/index.html](/Users/arif/workspace/bbn/motovault/docs/document-design/index.html) |
| Document requirements and design explanation | [DOCUMENT_SCANNING_DESIGN.md](/Users/arif/workspace/bbn/motovault/docs/DOCUMENT_SCANNING_DESIGN.md) |
| Native document implementation and evidence | [DOCUMENT_SCANNING_IMPLEMENTATION.md](/Users/arif/workspace/bbn/motovault/docs/DOCUMENT_SCANNING_IMPLEMENTATION.md) |
| Overall redesign mapping | [REDESIGN.md](/Users/arif/workspace/bbn/motovault/docs/REDESIGN.md) |
| Theme rules and contrast verification | [THEMES.md](/Users/arif/workspace/bbn/motovault/docs/THEMES.md) |
| Original product roadmap | [CHATGPT_HANDOFF.md](/Users/arif/workspace/bbn/motovault/docs/CHATGPT_HANDOFF.md), especially section 7 |
| Original architecture and domain intent | [ARCHITECTURE_AND_HANDOFF.md](/Users/arif/workspace/bbn/motovault/docs/ARCHITECTURE_AND_HANDOFF.md) |

Precedence: current user instructions, then the newer approved prototypes/theme decisions, then the original roadmap for unfinished capabilities. The original documents and README describe earlier implementations and contain stale claims. In particular, their dark-only design, fabricated BRTA replica cards, 3D flip interaction and assumption that an `iosApp` already exists are not the current baseline.

The overall HTML must be read with its companion JavaScript and CSS; the workflow script modifies the initial static markup. The document HTML is the reference for the phone flow, not a requirement to reproduce its desktop presentation wrapper. Use an HTTP preview if the browser cannot open local files.

Visual rules to preserve:

- System is the default; explicit Light and Dark override it and persist locally.
- Brand marks, real model photographs and brand colour families remain visible in both themes. There are nine brands and 78 model/trim references.
- Registration, licence and tax documents retain emerald, blue and bronze type markers. Brand colours apply to app controls, never to scanned pixels.
- Show actual originals, including the real printed layout. Do not generate government-looking credentials from OCR values or embed prototype sample credentials as owner records.
- Keep front/back or page tabs, crop/rotate review, field-level uncertainty hints, explicit owner confirmation, original viewing, zoom and a light document-only inspection view.

## 3. What is implemented and what is missing

| Capability | Baseline | Remaining work |
| --- | --- | --- |
| Motorcycle selection | Nine brands, 78 photo references, brand-scoped search, manual model name, year and free-text edition | Connect structured edition/emission choices and appropriate verified presets |
| Maintenance | Distance/time wear, component gauges, editable intervals/baselines, service costs/workshops/notes and history | Factory-specific schedules; restore a deliberate quick chain-care action if matching the original requirement |
| Fuel | Full/partial fills, cost and history; partial fills accumulate until a complete full-tank cycle | Keep current validated behaviour during persistence migration; do not display a preset efficiency as measured data |
| Documents | Five types; two sides for cards; up to eight pages for paper; camera, image/PDF import, crop/rotate/retake, OCR review and save | Real-card validation, more robust extraction, interruption/cleanup hardening and iOS runtime proof |
| Original files | Private managed raw, accepted and preview files; source PDF retained | Encryption/key lifecycle decisions, robust asset migration and recovery; no cloud storage yet |
| OCR | Bundled Android Latin ML Kit, iOS Vision English recognition, editable conservative candidates | Bangla OCR is not implemented. Validate supported language/SDK choices before promising it |
| Inspection | Light document-only view excludes maintenance/fuel costs | Scoped maximum brightness and restoration on exit/background |
| Renewal | In-app expiry status and 60-day warning window | Native notification scheduling, permissions, update/cancellation and platform verification |
| Persistence | Atomic local JSON plus managed document files; legacy preference/Base64 compatibility | Planned Room KMP database, versioned migration and repository integration |
| Service passport | Maintenance ledger data is stored | Export PDF, vehicle identity, QR/hash verification and save/share actions |
| Cloud sync | Not implemented | Authentication, private storage, RLS, encryption, offline queue, conflict handling and recovery |
| iOS | Shared/native adapter source compiles; simulator target runs common tests | Create a host app, configure permissions/signing, run all platform flows |
| Release | Android debug APK available | Physical-device QA, release configuration, backup/recovery and platform delivery checks |

There is one active bike record. Replacing the bike currently requires confirmation, clears service/fuel records and keeps documents. Multiple simultaneous garages, cars and EV workflows are future expansion, not part of the implemented motorcycle UX. The old polymorphic domain models alone do not prove those features exist.

## 4. Active code map and architectural traps

All paths in this section are relative to `/Users/arif/workspace/bbn/motovault` for command use.

| File or directory | Responsibility |
| --- | --- |
| `composeApp/src/commonMain/kotlin/com/motovault/app/App.kt` | Actual entry point: calls `ui.redesign.MotoVault()` |
| `.../ui/redesign/MotoVault.kt` | Navigation, garage, local state, commit/save ordering and document routing |
| `.../ui/redesign/Onboarding.kt` | Brand/model selection, owner setup and baseline confirmation |
| `.../ui/redesign/BikeCatalogue.kt` | Bundled 78-model reference catalogue, photographs and source URLs |
| `.../ui/redesign/Appearance.kt` | System/Light/Dark palettes and brand contrast |
| `.../ui/redesign/VaultState.kt` | Active serializable Ride, RideLog, VaultDocument and VaultState; validation and fuel cycles |
| `.../ui/redesign/OwnershipForms.kt` | Service/fuel/schedule/history forms; some older document helpers remain |
| `.../ui/redesign/DocumentCapture.kt` | Shared capture models, crop bounds, page limits, candidate parsing and platform contracts |
| `.../ui/redesign/DocumentFlow.kt` | Capture/import, review, confirmation, edit, originals, zoom and inspection UI |
| `.../ui/redesign/DocumentWallet.kt` | Overlapping wallet cards, renewals and inspection list |
| `composeApp/src/androidMain/kotlin/com/motovault/app/{VaultStorage,DocumentCamera,DocumentCapture,PlatformAppearance}.kt` | Atomic files, CameraX, import/PDF/OCR/crop/viewer and Android window handling |
| `composeApp/src/iosMain/kotlin/com/motovault/app/{VaultStorage,DocumentCamera,DocumentCapture,PlatformAppearance}.kt` | Atomic files, AVFoundation, picker/Vision/PDF/crop/viewer; status-bar integration belongs to the missing host |
| `composeApp/src/androidMain/AndroidManifest.xml` and `res/xml/document_paths.xml` | Camera permission, backups disabled and private FileProvider paths |
| `composeApp/src/commonMain/kotlin/com/motovault/app/domain/` | Existing component wear, expiry, service logic, vehicle models and KTM preset |
| `composeApp/src/commonTest/kotlin/com/motovault/app/` | Domain tests plus AppearanceTest, VaultStateTest and DocumentCaptureTest |

Important traps:

1. `VaultState.create()` currently installs three owner-defined examples: chain 500 km/30 days, oil 3,000 km/180 days and air filter 5,000 km/180 days. These are not verified manufacturer schedules.
2. `domain/preset/KtmDuke125Preset.kt` exists but has no active common-main caller. It defines seven components with different intervals and an initial efficiency of 38.5. Do not simply connect it and label everything factory-verified. Check model/year/market service manuals and reconcile the conflicting values. European edition mapping also needs review.
3. The active `Ride` uses free-text edition; the older `Vehicle` model has structured edition/emission/powertrain data. Decide one persisted contract and migrate it rather than maintaining two disconnected truths.
4. Tests of old domain presets do not prove the onboarding-to-garage path uses them. Add feature-owned integration checks around the active setup/state path.
5. Some old UI classes/helpers remain in source. Trace callers from `App()` before editing a screen.
6. A Supabase version entry in the version catalogue is not a working SDK integration. There is no Room database, sync client, export use case or notification scheduler.

## 5. Persistence and document invariants

- Android state is `filesDir/motovault.json`, written using `AtomicFile`. iOS writes `Application Support/motovault.json` atomically. Both can read older preference storage.
- New document files are stored in private `documents/` directories. Persist managed IDs, not device-specific absolute paths or temporary picker URIs.
- A page contains `originalId`, `acceptedId`, `previewId`, MIME type, dimensions and side label. A saved document can also reference `originalPdf`, PDF page count, confirmed details, extraction candidates and capture timestamp.
- Raw imports must remain byte-for-byte unchanged. Accepted crops and previews are separate files. Rotation, retake and summary edits must not silently overwrite raw files.
- Preserve document IDs, page order, confirmed values and source metadata when editing or migrating. Optional OCR candidates must not become confirmed fields unless shown for owner review.
- Saving currently writes durable state before replacing visible state or deleting superseded assets. Preserve this ordering when moving to Room or adding sync.
- Read legacy Base64 front/back photos. Do not lose them during database migration; convert with a recoverable migration strategy if needed.
- Asset IDs are restricted to safe filenames; card limit is two pages, paper limit eight; Android imports are bounded to 32 MB. Keep path traversal and oversized-file protection.
- Discard/replacement cleanup exists, but a failed capture/import before review may leave private unreferenced files. There is no startup garbage collector. Any cleanup must exclude saved assets and active drafts.
- Current files are app-private, but application-level document encryption is not implemented. Android backup is disabled; uninstall/device loss can therefore lose the local vault. Do not claim backup exists.

## 6. Proposed implementation sequence and acceptance criteria

These phases are the recommended continuation order. Complete implementation and relevant verification for each phase; do not replace code work with a new planning document. Routine local work can continue within the user's scope. Real backend credentials, account choices and provisioning need user input when unavailable; make independent local progress while waiting.

### Phase 1: Reconcile product data and factory-aware setup

Trace the active setup path, choose a unified vehicle identity contract, and add model/year/market-specific edition and emission options. Preserve manual setup for unsupported bikes. Verify maintenance/specification values against authoritative manuals rather than old prose or catalogue photographs.

Acceptance: incompatible edition/emission combinations cannot be saved; a supported preset reaches the actual garage; owner customisations remain editable; existing records restore safely; historical fuel economy remains calculated from logs; unknown models retain honest owner-defined schedules. Add tests covering onboarding/state selection, market/year boundaries and migration defaults.

### Phase 2: Room KMP persistence and migration

Introduce the planned local database and repositories for vehicle identity, component schedules, logs, documents and asset references. Check current compatible stable dependency versions before adopting the old roadmap's alpha versions. Define schema versions and transactions. Keep offline operation and explicit save failures.

Acceptance: migrate existing JSON/preferences and legacy attachments without loss; migration is repeatable; crashes or failed writes preserve the previous valid vault; complete records survive process kill/relaunch; updating/deleting documents cannot delete another record's assets; replacing the bike preserves documents as before. Test real database operations and migration fixtures, not only JSON serialization.

### Phase 3: Document and roadside readiness

Validate real registration/licence/tax layouts with user-provided or suitably redacted fixtures. Exercise physical capture, glare/darkness hints, cancellation, denied permission, rotation/backgrounding, crop, missing back, retake, eight-page limit and corrupt/oversized PDFs. Improve field extraction without silently guessing uncertain dates or values. Investigate supported Bangla recognition; retain manual review if unavailable.

Add scoped inspection brightness controls. Restore the previous brightness on dismissal, navigation, backgrounding and failure. Maintain document-only privacy and original image colours. Add safe orphan recovery only after distinguishing saved assets from live drafts.

Acceptance: originals remain unchanged; unseen candidates remain unconfirmed; failed/cancelled operations preserve saved records; real-device readability is demonstrated; brightness restores reliably; no costs/workshop notes appear in inspection. Camera quality indicators remain clearly heuristic unless actual document-boundary detection is implemented and verified.

### Phase 4: Service-passport PDF export

Implement `ExportServicePassportUseCase` or an equivalent shared orchestration with platform rendering/export adapters. Include owner-approved vehicle identity, plate/chassis/engine where available, chronological service entries, mileage, dates, workshop and costs. Define exactly which fields the verification hash covers, canonical serialization and a verification mechanism before rendering a QR.

Acceptance: exported PDF contains actual saved records, handles long ledgers/page breaks, preserves chronological order and Unicode text, and opens through native save/share/view actions. Changing a covered entry changes the hash. A hash alone proves integrity relative to a known digest, not that a service occurred or was independently certified. Do not label owner-entered history independently verified without an authority/signature workflow.

### Phase 5: Renewal notifications

Use the existing 60-day in-app warning policy as the baseline; specify additional reminder times explicitly. Implement native Android/iOS permission and scheduling adapters, durable document IDs, cancellation/rescheduling and clock/time-zone behaviour.

Acceptance: new/edit/delete operations update the correct reminders; denied permission leaves in-app warnings functional; expired/no-expiry documents behave deliberately; relaunch and supported reboot cases work; duplicate alerts are avoided. Verify delivered notifications on devices, not just scheduler calls.

### Phase 6: Supabase backup and sync

Prepare local sync states/queue, authentication UI consistent with the design, per-user private storage, RLS, encryption/key recovery, upload/download and conflict/tombstone handling. Obtain the intended Supabase project/configuration and account decisions before real provisioning. Keep credentials out of source and never ship service-role keys in the client.

Acceptance: offline changes survive restart and sync later; account A cannot read account B's records/files; missing network/retry does not duplicate entries; delete and conflict rules are deterministic; another device can restore both records and original files; logout/account switching does not mix vaults; encrypted backup recovery is proven. Mocked tests are useful but do not prove live RLS/storage isolation.

### Phase 7: iOS host and release completion

Create a host application for the static `ComposeApp` framework. Configure the camera usage description, lifecycle/window appearance, document picker/viewer presentation, brightness and notification adapters. Simulator compilation currently cannot prove these flows. Run on iOS simulator and a device when available.

Complete Android physical-device QA on the user's Moto Edge 50 Fusion when connected. Verify permissions, restart, rotation/backgrounding, long history, accessibility/font scaling, all themes, document pixels, export, reminders and backup recovery. Review release signing/versioning and current platform requirements before distribution.

Acceptance: both platforms have runnable host apps; the same owner workflow works without data loss; builds/tests pass; device evidence covers platform-specific features; documentation reflects actual delivered behaviour. Do not count compilation, test discovery or a mock camera as runtime proof.

## 7. Build and test baseline

Inspected build configuration: Kotlin 2.0.21, Compose Multiplatform 1.7.0, AGP 8.7.1, Android compile SDK 35 / target SDK 34 / minimum SDK 24; Java/JVM 17. CameraX 1.4.2 and bundled ML Kit text recognition 16.0.1 are added directly in `composeApp/build.gradle.kts`. Verify compatibility before dependency upgrades; historical source caches are not the authoritative resolved build configuration.

On this Mac, use native ARM64 JDK 17. Every shell command must go through `rtk`, using its dedicated command when available and `rtk run` otherwise.

```sh
cd /Users/arif/workspace/bbn/motovault
rtk git status --short
rtk git log -5 --oneline
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home rtk run ./gradlew :composeApp:assembleDebug :composeApp:compileKotlinIosArm64 :composeApp:testDebugUnitTest :composeApp:iosSimulatorArm64Test --console=plain
```

APK: `/Users/arif/workspace/bbn/motovault/composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

Latest implementation verification on 11 October: Android debug assembly and iOS ARM64 compilation passed; 53 tests passed on each Android and iOS simulator test target with zero failures. Tests include wear/fuel/service/expiry logic, old KTM presets, active state behaviour, palette contrast, capture bounds, page replacement/append limits, candidate confirmation and legacy compatibility. Re-run relevant checks after changes; this handoff does not report a new test run on 12 October.

## 8. Runtime evidence and limits

- Android simulator: custom front/back camera flow; synthetic front/back image import; crop handles and rotation; confirmation required before save; saved details edited and reopened after restart.
- Two-page tax-token PDF imported, reviewed, confirmed, saved, reopened after reinstall, switched to page 2 and opened in the native PDF viewer.
- Raw photo files and original PDFs compared byte-for-byte with sources. Tax-token original SHA-256: `b386e073c880a9c8b0413eb6af6f3fdabe375613929b602799a3846182ded1f2`.
- System light/dark switching, brand accents, light inspection, camera clipping and final light-mode system-bar contrast checked.
- Evidence: `/Users/arif/workspace/bbn/motovault/docs/prototype-verification/`, `/Users/arif/workspace/bbn/motovault/docs/theme-verification/`, `/Users/arif/workspace/bbn/motovault/docs/scanning-verification/` and `/Users/arif/workspace/bbn/motovault/docs/contrast-audit/`.
- Useful final images: `prototype-verification/50-light.png`, `51-dark.png`, `52-inspection-system-dark.png` and `45-native-pdf.png`. Earlier captures include intermediate failures; a filename alone does not prove a screen passed.
- Test credentials are visibly marked SAMPLE/TEST ONLY and are emulator data, not bundled production owner records.
- Physical Moto Edge 50 Fusion disconnected during earlier work; simulator inspection was explicitly authorised. Recheck `rtk run adb devices` before selecting a target.
- Software-rendered Pixel_10 emulator was more stable than the earlier graphics session. If needed, launch the existing AVD without snapshots using `rtk run /Users/arif/Library/Android/sdk/emulator/emulator -avd Pixel_10 -no-snapshot-load -gpu software`. Preserve its data; do not wipe it to simplify QA.
- ADB screen interaction/capture was explicitly authorised in this conversation. Follow the next conversation's tool/permission rules. Use current UI bounds; delayed imports and scrolling made blind coordinate taps unreliable.
- The final Android dialog fix in `PlatformAppearance.kt` applies bounds/inset/icon settings after layout. Do not remove its lifecycle handling based solely on appearance flags in dumpsys. Check actual light and dark screenshots after changes.
- Physical-card OCR accuracy, iOS camera/picker runtime, cloud isolation, export integrity and native notification delivery remain unverified.

## 9. Repository and collaboration rules

Only report mode was requested before this handoff. No application code was changed while preparing it. The new handoff is intentionally uncommitted; do not commit or push without an explicit request.

At preparation time, application changes were committed, while these local references remained untracked: `docs/CONTRAST_FINDINGS.md`, `docs/DOCUMENT_SCANNING_DESIGN.md`, `docs/DOCUMENT_SCANNING_IMPLEMENTATION.md`, `docs/REDESIGN.md`, `docs/THEMES.md`, and the design/evidence directories listed above. An ordinary clone of `main` will not contain them. Continue in this same workspace or explicitly package/commit the design material before moving machines. The overall prototype also lives outside the repository under `/Users/arif/Documents/Codex/2026-10-10/red/outputs/`.

User preferences: inspect current branch/base; preserve unrelated changes; use `rtk`; use Podman if containers are needed. Read applicable AGENTS.md instructions and relevant memory at session start. For commit messages sample `git log`; for other writing under the user's name sample the same medium, use plain English, keep it short and avoid em dashes. Do not repeatedly request approval for ordinary work already authorised. Ask only for genuinely missing decisions/access, and keep moving on independent work.

## 10. Paste this into the next conversation

> Continue the remaining MotoVault implementation in `/Users/arif/workspace/bbn/motovault`. First read `/Users/arif/workspace/bbn/motovault/docs/CONTINUATION_HANDOFF.md`, inspect the current branch and working tree, and open the design references it lists. The overall prototype is `/Users/arif/Documents/Codex/2026-10-10/red/outputs/motovault-redesign.html` with its companion workflow JS/CSS and bike catalogue. The document prototype is `/Users/arif/workspace/bbn/motovault/docs/document-design/index.html`. Preserve the approved designs, System/Light/Dark themes, actual document originals and all existing records. Implement the remaining phases with migration and runtime evidence, starting with factory-aware setup and persistence. Do not treat unused legacy code, passing compilation or synthetic tests as delivered functionality. Continue through the remaining local implementation; ask only when a necessary decision, credential or external access is missing. Do not commit, push or deploy unless I request it.
