# MotoVault phase-by-phase implementation plan

<!-- Generated from roadmap.json by tools/roadmap.py. -->

This plan implements the crucial and additional Bangladesh ownership features while extending `design/app-ui-v3/`. It authorizes planning; it does not claim those features are implemented.

Read [the design contract](DESIGN-CONTRACT.md), [baseline findings](BASELINE.md), [progress](PROGRESS.md) and [tracking instructions](README.md) together.

## Priorities and delivery order

- **P0:** foundation, trustworthy reminders, document privacy, recoverable local backup.
- **P1:** Bangladesh maintenance, renewal help, cost insights and quick capture needed for the core release.
- **P2:** multi-bike ownership, workshops, resale, widgets and seasonal/breakdown help.
- **P3:** optional professional-rider mode and cloud backup. These do not block the core release.

Order follows dependency and risk. Independent tasks with satisfied prerequisites can run concurrently. Finish one vertical slice before widening scope. Size points 1/2/3/5/8 are relative complexity, not person-days; re-estimate after the phase's first design and technical slice.

Core beta: P00-P03. Core release: P00-P08. Expanded ownership release: P09-P13 plus P16. Optional P14/P15 each have their own enablement gate.

## Cross-cutting implementation rules

- Use commonMain/domain for pure rules, storage for Room/repositories and transactional migrations, ui for feature composables, androidMain/iosMain for OS services. Keep skyDesignSystem free of MotoVault models, auth, persistence, location and financial effects.
- Existing paths below are verified starting points. Proposed paths are design targets and can be refined with evidence before editing. Extend a focused feature file instead of expanding Screens.kt indefinitely; avoid unrelated refactors.
- Every data change needs backward-compatible decoding, stable IDs, asset ownership, transaction/rollback behavior and an old-data fixture. Future bike-scoping must be accounted for in each new model.
- Keep local operations usable without an account. Save without optional camera/mic/GPS access; confirm all OCR, voice, paste and mileage estimates before durable writes.
- Record settings, data and side effects through the durable save coordinator. A failed save must not leave a successful toast, changed reminder schedule or deleted owner asset. Undo is durable and reverses associated derived state.
- Use integer poisha for new money contracts, explicit conversion for legacy Double amounts, and one defined rounding policy. Do not infer current pump prices from prototype defaults.
- Design and physical-device evidence are release gates. Build and shared tests alone do not prove camera, notification, key storage, background delivery or battery behavior.
- Each phase produces a sanitized report in evidence/ with build revision, platform/device/OS, steps, expected/actual results and remaining gaps. Never commit owner IDs, documents, receipts, credentials or precise locations.

## Phase details

<a id="p00"></a>

## P00: Baseline, design contract and delivery foundations

**Priority:** P0 | **Scope:** foundation | **Current status:** Done

**Outcome:** A reproducible v3 reference, honest starting inventory, migration strategy and working progress system.

**Why now:** Prevents rebuilding existing features, losing owner records, or treating a new screen as a complete feature.

**Entry dependencies:** None.

**V3 placement and interaction:** Inventory all four tabs, center add, onboarding, sheets and roadside mode. Preserve native brand-logo selection as the user-approved exception.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/App.tsx`, `design/app-ui-v3/src/screens/Home.tsx`, `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/components/Sheet.tsx`, `design/app-ui-v3/src/lib/motion.ts`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultState.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt`.
- Proposed sources: None.

**Data and migration:** Record current Room schema, persisted defaults, originals/crops/previews, money conversions and the primary vehicle ID. Specify stable bike IDs for new contracts without prematurely migrating every table.

**Tasks:**

### P00-01: Inventory current features and gaps against commit 50eda3a

Status: **Done** | Size: **1** | Owner: Codex.

Additional task dependencies: None.

Acceptance criteria:

- Record existing capabilities, demo concepts, observed gaps, source anchors and verification limits.

Verification:

- Cross-check the production entrypoint, storage, reminder wiring, factory profiles and prototype source.

Required completion evidence: plan, verification.

### P00-02: Freeze the v3 design contract and feature-to-screen map

Status: **Done** | Size: **2** | Owner: Codex.

Additional task dependencies: P00-01.

Acceptance criteria:

- Document navigation, fonts, token provenance, numeric input, motion, reduced motion and brand logos.
- Map every planned feature into a v3 screen or existing sheet pattern.

Verification:

- Reference real prototype files and preserve source hashes for detecting later reference changes.

Required completion evidence: plan, design.

### P00-03: Install the task tracker and same-change update workflow

Status: **Done** | Size: **2** | Owner: Codex.

Additional task dependencies: P00-02.

Acceptance criteria:

- Use stable IDs, explicit dependencies, evidence, blockers and an activity log.
- Generate a progress board and detailed plan; reject completion with missing prerequisites or evidence.

Verification:

- Exercise the CLI in temporary fixtures, validate dependency cycles and check deterministic generated documents.

Required completion evidence: implementation, verification.

### P00-04: Capture reproducible native and prototype baseline fixtures

Status: **Done** | Size: **3** | Owner: Codex.

Additional task dependencies: P00-03.

Acceptance criteria:

- Use synthetic data for empty, overdue, partial-fill, document and unknown-service states.
- Save labelled prototype and native screenshots plus sheet, gauge, Undo and reduced-motion recordings; record build/device/OS.

Verification:

- Run on an Android emulator and an iOS simulator with the same fixture; record any mismatch instead of inheriting earlier proof.

Required completion evidence: verification, design, android_runtime, ios_runtime.

### P00-05: Specify shared data, migration and evidence contracts

Status: **Done** | Size: **3** | Owner: Codex.

Additional task dependencies: P00-03.

Acceptance criteria:

- Write decisions for stable bike IDs, person versus bike documents, integer poisha, date/time rules, asset references and migration rollback.
- Define sanitized fixture format, import limits, durable Undo and the local backup version policy.

Verification:

- Review each new phase model against the current single-ride Room snapshot and legacy JSON; retain representative compatibility fixtures.

Required completion evidence: plan, review.

**Phase exit gate:**

- Tracker validates and generated documents match its data.
- Synthetic old-vault fixtures and native screenshot/motion baselines exist for Android and iOS.
- The data evolution decision records schema ownership, money precision, bike scoping, asset ownership and rollback.

**Risks and decisions:** The v3 directory is currently untracked. Preserve its bytes; include its reviewed sources when a future commit is authorized. Historical docs are evidence, not current runtime proof.

<a id="p01"></a>

## P01: Trustworthy reminders and understandable maintenance status

**Priority:** P0 | **Scope:** core | **Current status:** Ready

**Outcome:** Every reminder switch controls real behavior; maintenance cards communicate recorded interval status accurately.

**Why now:** These are promises already visible in v3 and should be completed before widening the app.

**Entry dependencies:** P00-04, P00-05.

**V3 placement and interaction:** Home NextUp, gauge details, Vault expiry chips and Me reminder rows. Reuse Sheet, Switch, date chips, status icon/text and one-tap service entry.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Home.tsx`, `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/screens/Vault.tsx`, `design/app-ui-v3/src/components/Gauge.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/RenewalReminders.kt`, `composeApp/src/androidMain/kotlin/com/motovault/app/ui/RenewalReminders.kt`, `composeApp/src/iosMain/kotlin/com/motovault/app/ui/RenewalReminders.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultSession.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/WearDomain.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/reminders/ReminderPolicy.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/reminders/ReminderSettingsSheet.kt`.

**Data and migration:** Migrate platform renewal opt-in and new global/per-document settings into one explicit policy. Preserve consumed identities and scheduled events; do not infer notification permission from a saved switch.

**Tasks:**

### P01-01: Unify reminder policy, settings and migration

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Choose and document default warning lead times, local reminder time, snooze and per-record/global overrides.
- Reconcile the present 30-day UI versus 60-day native policy and migrate existing opt-in without enabling denied notifications.

Verification:

- Test old settings import, expiry edits, clock/timezone changes, late additions, deduplication and save failure.

Required completion evidence: implementation, verification.

### P01-02: Connect service and document scheduling after durable saves

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P01-01.

Acceptance criteria:

- Schedule from the committed snapshot; cancel obsolete events after edits, deletion and opt-out.
- Add stable service task identities and actionable deep links; preserve consumed-event recovery and background reconciliation.

Verification:

- Test stale snapshots, rollback/Undo, restart, identity collisions and notification taps for a missing or changed record.

Required completion evidence: implementation, verification.

### P01-03: Implement reminder settings, permission recovery and snooze in v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P01-02.

Acceptance criteria:

- Show actual enabled/denied state and matching schedule in Me and Vault.
- Open the exact service/document from a notification; snooze without pretending a task was completed.

Verification:

- Exercise deny, revoke, return-from-settings, save failure and screen-reader labels on both native hosts.

Required completion evidence: implementation, verification, design.

### P01-04: Make gauges, unknown history and stale mileage honest

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Call percentages interval remaining; show basis, last confirmed mileage/date and schedule source in details.
- Unknown service history cannot show reassuring measured health; estimates need explicit confirmation before saving.

Verification:

- Test whichever-limit-first, overdue text, no-data, future/invalid baseline rejection and reduced-motion rendering.

Required completion evidence: implementation, verification, design.

### P01-05: Verify reminders and status on real devices

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P01-03, P01-04.

Acceptance criteria:

- Complete Android and iPhone delivery, foreground/background, denied permission, restart and deep-link scenarios with synthetic records.
- Recheck v3 Home/Vault/Me in light/dark, large text, both digit styles and reduced motion.

Verification:

- Retain native reports and recordings; distinguish a short-delay probe from an actual expiry policy test.

Required completion evidence: verification, design, android_device, ios_device.

**Phase exit gate:**

- Service and document notifications open the correct record and honor all opt-outs.
- UI lead times match native scheduling and permission state.
- Stale mileage, unknown baselines and interval-based percentages are visible; no physical part-health claim.
- Real Android and iPhone background delivery, denial, restart and settings tests are recorded.

**Risks and decisions:** OS delivery can be delayed. Never claim an exact alarm guarantee; mileage reminders cannot know unrecorded travel.

<a id="p02"></a>

## P02: Document privacy and safe phone handover

**Priority:** P0 | **Scope:** core | **Current status:** Ready

**Outcome:** Owners control access to sensitive documents and can safely show one selected document.

**Why now:** The vault holds identity and ownership data; privacy belongs before backup and wider sharing.

**Entry dependencies:** P00-04, P00-05.

**V3 placement and interaction:** Me gains a Privacy section; Vault uses masked summaries and a protected document sheet. Extend existing roadside mode with a clear scope and exit action.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/screens/Vault.tsx`, `design/app-ui-v3/src/sheets/DocViewSheet.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentFlow.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentWallet.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/InspectionBrightness.kt`, `composeApp/src/androidMain/kotlin/com/motovault/app/ui/DocumentCapture.kt`, `composeApp/src/iosMain/kotlin/com/motovault/app/ui/DocumentCapture.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/privacy/VaultAccessPolicy.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/privacy/VaultPrivacySheet.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/security/SecureAssetStore.kt`.

**Data and migration:** Version privacy preferences. Separate device-bound keys from portable backup secrets. Encrypt sensitive local assets only through a resumable, validated migration; document any plaintext database metadata remaining.

**Tasks:**

### P02-01: Review privacy threat model, key lifecycle and recovery

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Specify what is locked/encrypted, what remains metadata, how keys survive upgrades and how lost keys are handled.
- Select reviewed platform cryptography and key storage; document backup interoperability without custom crypto.

Verification:

- Review shared-phone, stolen-file, app-switcher, export-cache and key-invalidation cases.

Required completion evidence: plan, review.

### P02-02: Implement secure key and sensitive-asset storage adapters

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P02-01.

Acceptance criteria:

- Use authenticated encryption with versioned asset metadata and platform-protected local keys.
- Migrate assets with staging, verification and recovery; preserve originals until the durable new reference is committed.

Verification:

- Test interrupted migration, altered ciphertext, missing/invalid keys, disk-full and no plaintext temporary export leaks.

Required completion evidence: implementation, verification.

### P02-03: Add lock, auto-lock, masking and selected-document inspection

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P02-02.

Acceptance criteria:

- Use system credentials/biometrics with an accessible fallback and clear recovery message.
- Lock on timeout/background; reveal only chosen documents and minimize sensitive notification/app-switcher content.

Verification:

- Test cancellation, biometric lockout, no enrolled biometric, deep links, rotation, background/resume and back navigation.

Required completion evidence: implementation, verification, design.

### P02-04: Protect export, import and temporary-file boundaries

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P02-03.

Acceptance criteria:

- Require deliberate access for sensitive exports; clean temporary decrypted files after share/cancel.
- Avoid sensitive values in logs, error reports and screenshots used as project evidence.

Verification:

- Inspect temporary directories, native shares and malformed import failures using synthetic sensitive fixtures.

Required completion evidence: implementation, verification.

### P02-05: Verify privacy and key recovery on physical Android and iPhone

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P02-03, P02-04.

Acceptance criteria:

- Exercise key storage, credential changes, lockout, background screenshots and selected-document handover.
- Record the exact local-at-rest coverage and recovery limitations in user-facing help and engineering evidence.

Verification:

- Run on both real devices; use v3 theme/large-text/reduced-motion checks for protected flows.

Required completion evidence: verification, design, android_device, ios_device, review.

**Phase exit gate:**

- Device-credential/biometric access, timeout and recovery behavior work on both platforms.
- Selected-document mode cannot expose other records through back, deep links, recents or resume.
- Local encryption claims match the implemented key/data coverage and failure recovery.

**Risks and decisions:** An app lock is not database encryption. Encryption design must preserve originals and handle lost or invalidated keys without silently wiping records.

<a id="p03"></a>

## P03: Encrypted local backup and complete restore

**Priority:** P0 | **Scope:** core | **Current status:** Waiting for prerequisites

**Outcome:** A lost phone or reinstall does not erase the ownership record when the owner has a backup.

**Why now:** Recovery gives long-term value to every record users enter. Cloud transport can follow a proven restore format.

**Entry dependencies:** P02-05.

**V3 placement and interaction:** Me > Backup uses v3 cards, Sheet steps and date labels. Show last backup, what is included and a restore preview; use real progress only for long file work.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/components/Sheet.tsx`, `design/app-ui-v3/src/components/Toast.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultSession.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/LocalSync.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentCapture.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/backup/BackupManifest.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/backup/BackupRepository.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/backup/BackupSheet.kt`.

**Data and migration:** Define a portable, versioned authenticated archive containing records, original assets, crops, settings and a manifest. Never export device keys. Regenerable previews may be omitted only if restore recreates them. Restore stages and validates before one durable replacement.

**Tasks:**

### P03-01: Specify portable archive, secret handling and compatibility policy

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Version the manifest, authenticated metadata, asset inventory, size limits and secret derivation.
- Specify replace/restore confirmation and recovery method; local lock keys remain device-bound.

Verification:

- Review format against old vaults, future bike-scoped data and the privacy key design.

Required completion evidence: plan, review.

### P03-02: Implement consistent snapshot export and native file save

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P03-01.

Acceptance criteria:

- Freeze a committed revision and include every referenced non-regenerable asset.
- Encrypt before handing the file to the native save/share destination; cancellation does not update last-successful-backup.

Verification:

- Test concurrent record changes, missing originals, cancelled save, out-of-space and no leaked plaintext.

Required completion evidence: implementation, verification.

### P03-03: Implement staged validation and transactional restore

Status: **Planned** | Size: **8** | Owner: unassigned.

Additional task dependencies: P03-02.

Acceptance criteria:

- Bound archive expansion and reject unsafe paths, unsupported versions and tampered manifests/assets.
- Preview counts and conflicts; atomically restore or roll back without deleting the previous vault; reconcile reminders after success.

Verification:

- Inject interruption at each restore stage and compare records plus asset hashes; test older supported archives and duplicate IDs.

Required completion evidence: implementation, verification.

### P03-04: Build backup, restore review and recovery help in v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P03-03.

Acceptance criteria:

- Explain file destination, backup age, included data and secret recovery in plain Bangla.
- Give separate success/error/cancel states; destructive replacement has a concrete review step.

Verification:

- Check accessibility, focus, large text, dark theme and interruption/resume without a false success toast.

Required completion evidence: implementation, verification, design.

### P03-05: Run cross-platform recovery and trustworthy-local-beta gate

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P03-04.

Acceptance criteria:

- Restore a synthetic fully populated vault on fresh physical Android and iPhone installations.
- Retain cross-platform manifests, original hashes and failure-path results; release only the verified backup behavior.

Verification:

- Exercise wrong secret, missing key, offline restore, partial archive, reinstall and permission re-request.

Required completion evidence: verification, design, android_device, ios_device, review.

**Phase exit gate:**

- Cross-platform Android-to-iOS and iOS-to-Android restore preserves records and byte-identical originals.
- Wrong secrets, damaged archives, missing assets, unsupported versions, cancellation and low disk space leave the existing vault unchanged.
- A full new-install restore drill works on physical devices; backup and notification settings do not silently grant permissions.

**Risks and decisions:** Backup-file loss and secret loss are separate failures. Choose a recoverability model explicitly; do not promise recovery without a recovery method.

<a id="p04"></a>

## P04: Bangladesh maintenance and used-bike onboarding

**Priority:** P1 | **Scope:** core | **Current status:** Waiting for prerequisites

**Outcome:** A locally relevant bike setup leads to accurate, explainable owner-confirmed maintenance tasks.

**Why now:** Daily commuters and used-bike owners need the right schedule and an honest starting baseline.

**Entry dependencies:** P01-05.

**V3 placement and interaction:** Extend the three-input onboarding with progressive detail only when needed. Keep native brand logos; put schedule provenance and edit actions inside gauge/service sheets.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Onboarding.tsx`, `design/app-ui-v3/src/screens/Home.tsx`, `design/app-ui-v3/src/sheets/ServiceSheet.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/FactoryProfiles.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Onboarding.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/BikeCatalogue.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/domain/model/ComponentRule.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/ServiceCatalogue.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/maintenance/MaintenanceAction.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/maintenance/ScheduleDetailsSheet.kt`.

**Data and migration:** Add action type, source/manual revision, applicability and baseline certainty with compatible defaults. A catalogue entry does not automatically become a manual-backed schedule.

**Tasks:**

### P04-01: Select initial local models and verify source manuals

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Choose a small initial set through pilot-owner evidence, not an unsupported popularity ranking.
- Record source URL, manual/version/page, market/model code and applicability; retain unsupported-model fallback.

Verification:

- Have every proposed interval and fluid/part recommendation traced to its applicable source.

Required completion evidence: research, review.

### P04-02: Model distinct maintenance actions and source-backed schedules

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P04-01.

Acceptance criteria:

- Separate inspection, cleaning, lubrication, adjustment and replacement.
- Service resets affect selected actions only; preserve custom rules and historical logs during migration.

Verification:

- Test running-in, whichever-limit-first, unsupported model, rule edits and mixed-action service visits.

Required completion evidence: implementation, verification.

### P04-03: Add unknown-history and used-bike starting states

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P04-02.

Acceptance criteria:

- Let owners choose known last service or unknown baseline without faking a service record.
- Suggest an initial inspection checklist; keep unknown state until the owner records a real action.

Verification:

- Check fresh and migrated users, odometer bounds, skipped setup and returning to complete a baseline.

Required completion evidence: implementation, verification, design.

### P04-04: Expose schedule source, edits and local care actions in v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P04-02, P04-03.

Acceptance criteria:

- Show source/manual or owner-defined label, next due basis and editable intervals in the details sheet.
- Keep the fast service flow and support recording several actual actions in one visit.

Verification:

- Compare against v3 service chips, Home urgency order, large text and both digit styles.

Required completion evidence: implementation, verification, design.

### P04-05: Verify schedule migration and Bangladesh setup end to end

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P04-04.

Acceptance criteria:

- Old records/rules remain readable and backups round-trip new source/action fields.
- Run supported, unsupported and unknown-history scenarios on Android and iOS.

Verification:

- Validate fixture outcomes against the cited manuals and retain native screenshots.

Required completion evidence: verification, design, android_runtime, ios_runtime, review.

**Phase exit gate:**

- Initial Bangladesh profile set has checked model/year/market manuals and source pages.
- Inspect/clean/lubricate/adjust/replace are separate actions with appropriate reset behavior.
- Unknown prior history, running-in and custom owner schedules remain useful without invented certainty.

**Risks and decisions:** Do not copy a European or Indian interval into a Bangladesh model by name alone. Local-condition adjustments require a cited manual/dealer basis or an explicit owner setting.

<a id="p05"></a>

## P05: BRTA renewal and document assistance

**Priority:** P1 | **Scope:** core | **Current status:** Waiting for prerequisites

**Outcome:** An expiry warning leads to a useful, sourced renewal checklist and a recorded renewal.

**Why now:** Document storage is already present; guided action closes the practical ownership loop.

**Entry dependencies:** P01-05, P02-05.

**V3 placement and interaction:** Vault document detail gains Renew, official verification and renewal history. Reuse DocAdd review, date chips, receipt attachment and a return-to-document flow.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Vault.tsx`, `design/app-ui-v3/src/sheets/DocAddSheet.tsx`, `design/app-ui-v3/src/sheets/DocViewSheet.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentFlow.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentWallet.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/OwnershipForms.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultState.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/documents/RenewalChecklist.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/documents/RenewalSheet.kt`.

**Data and migration:** Keep person-level licence documents separate from bike ownership scope. Append renewal events and receipts; preserve prior originals and expiry history instead of overwriting the only copy.

**Tasks:**

### P05-01: Verify official links, document applicability and checklist content

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Record official sources, checked dates and applicable document categories.
- Use a safe fallback when a service URL is unavailable; keep fee estimates separate from confirmed payments.

Verification:

- Open the official QR-verification page and selected renewal/transfer sources; review plain Bangla wording.

Required completion evidence: research, review.

### P05-02: Implement renewal events, fee links and original preservation

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P05-01.

Acceptance criteria:

- Save previous/new expiry, payment receipt, actual fee and document revision as one reviewed event.
- A failed save or Undo cannot leave a fee, file or reminder inconsistent with the document.

Verification:

- Test old document import, missing date, duplicate renewal, cancellation, asset recovery and backup round-trip.

Required completion evidence: implementation, verification.

### P05-03: Build renewal checklist and deliberate official-verification actions

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P05-02.

Acceptance criteria:

- Show checklist progress, source date and return to the exact document.
- Use the native browser/share boundary deliberately; require owner action before uploading a certificate.

Verification:

- Exercise offline/unavailable links, dark/large-text layouts, privacy lock and screen-reader navigation.

Required completion evidence: implementation, verification, design.

### P05-04: Verify a full renewal on Android and iOS

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P05-03.

Acceptance criteria:

- Run from reminder to checklist, payment attachment, renewed document, expense and new reminder.
- Use synthetic documents and retain source freshness plus native evidence.

Verification:

- Test deny/cancel/failure and ensure original bytes remain intact.

Required completion evidence: verification, design, android_runtime, ios_runtime, review.

**Phase exit gate:**

- Checklists are sourced, dated and applicable to the selected document/vehicle.
- Official verification opens deliberately; no file or identity number is silently uploaded.
- Renewal updates the record, expense and reminder consistently, with rollback on failure.

**Risks and decisions:** Official pages and fee rules change. Show last-checked dates and an offline checklist; do not invent a BRTA API, hard-code fees, imply every bike needs every document, or present stored copies as official validation.

<a id="p06"></a>

## P06: Ownership costs, budgets and fuel confidence

**Priority:** P1 | **Scope:** core | **Current status:** Ready

**Outcome:** History explains recorded ownership costs and helps plan upcoming spending.

**Why now:** Extends the existing 30-day total into practical decisions for cost-conscious owners.

**Entry dependencies:** P00-04, P00-05.

**V3 placement and interaction:** History retains its summary and expandable timeline. Add period/category chips and a cost-details sheet; keep fuel entry taka-first and use the v3 keypad.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/History.tsx`, `design/app-ui-v3/src/sheets/FuelSheet.tsx`, `design/app-ui-v3/src/sheets/ServiceSheet.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultState.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Sheets.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/domain/usecase/FuelEfficiencyUseCase.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/costs/OwnershipCostUseCase.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/costs/CostInsightsSheet.kt`.

**Data and migration:** Add parking/toll/other expenses and budget targets as bike-scoped-compatible records. Convert legacy money explicitly to poisha; document rounding, record coverage and date boundaries.

**Tasks:**

### P06-01: Define money, distance coverage and cost metrics

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Specify integer-poisha conversions, category rules, date windows and recorded-distance denominator.
- Define full-to-full fuel confidence and missing-data states before drawing a chart.

Verification:

- Review examples containing partial fills, backdated records, no mileage and renewal fees.

Required completion evidence: plan, review.

### P06-02: Implement expenses, budgets and pure cost calculations

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P06-01.

Acceptance criteria:

- Support fuel/service/renewal/parking/toll/custom expenses and owner-entered budgets.
- Preserve existing logs; prevent renewal double counting and isolate calculations from UI.

Verification:

- Test rounding, refunds/corrections policy, month boundaries, duplicate IDs and incomplete distance history.

Required completion evidence: implementation, verification.

### P06-03: Add fuel price provenance and incomplete-cycle explanations

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P06-02.

Acceptance criteria:

- Ask for confirmation of a first price; retain effective date and manually editable remembered values.
- Explain insufficient mileage data and avoid silently treating every fill as a completed cycle.

Verification:

- Test price changes, decimal/digit inputs, partial fills, missed fills and editing an old fill.

Required completion evidence: implementation, verification, design.

### P06-04: Build cost details, category entry and budgets in v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P06-02, P06-03.

Acceptance criteria:

- Extend existing History cards and timeline with accessible period/category controls.
- Use common keypad/date chips, clear coverage labels and optional maintenance allowance.

Verification:

- Reconcile visible totals to fixture records; check both digit styles, themes and large text.

Required completion evidence: implementation, verification, design.

### P06-05: Verify cost migration, edit/Undo and export consistency

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P06-04.

Acceptance criteria:

- Migrated money and every edited/deleted expense reconcile across History and backup.
- Run cost workflows on both hosts without implying estimates are complete lifetime costs.

Verification:

- Retain native evidence and independent fixture totals for complete and incomplete histories.

Required completion evidence: verification, design, android_runtime, ios_runtime.

**Phase exit gate:**

- Fuel, service, renewal and other expenses reconcile without duplication.
- Cost/km uses a defensible recorded-distance window and says insufficient data when needed.
- Fuel prices have owner-confirmed value/date/source; prototype placeholders never masquerade as current official prices.

**Risks and decisions:** Missing records and partial fills can make averages misleading. Maintenance allowance is a user budget estimate, not a financial forecast.

<a id="p07"></a>

## P07: Native quick capture: odometer, share and Bangla voice

**Priority:** P1 | **Scope:** core | **Current status:** Waiting for prerequisites

**Outcome:** Useful records can be drafted quickly from a photo, shared text or Bangla speech.

**Why now:** Reduces the effort needed to maintain a useful ownership history.

**Entry dependencies:** P01-05, P02-05.

**V3 placement and interaction:** Keep the center add menu and existing fuel/service/odometer sheets. Native capture fills the existing review draft, keeping explicit confirmation and durable Undo.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/sheets/OdometerSheet.tsx`, `design/app-ui-v3/src/sheets/PasteSheet.tsx`, `design/app-ui-v3/src/sheets/ServiceSheet.tsx`, `design/app-ui-v3/src/components/SmartInput.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Sheets.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/SmartParse.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/SmartServiceDraft.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentCapture.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/capture/QuickEntryDraft.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/capture/OdometerCapture.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/capture/VoiceEntry.kt`.

**Data and migration:** Persist only confirmed records. Draft source/confidence is bounded metadata; images/audio retention is explicit. Share targets must select the intended bike and cannot bypass a locked vault.

**Tasks:**

### P07-01: Define reviewed drafts and unify numeric/text parsing

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Use one normalized draft for manual, pasted, photo and voice inputs with explicit source/confidence.
- Accept supported digit sets; distinguish expense from odometer numbers and detect duplicate shared messages.

Verification:

- Test real-language synthetic messages, ambiguous totals, malformed amounts and invalid odometer regressions.

Required completion evidence: implementation, verification.

### P07-02: Implement odometer camera OCR with review and fallback

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P07-01.

Acceptance criteria:

- Recognize odometer candidates and show image/candidate review before applying to the sheet.
- Camera denial, blur, glare and no result preserve a usable manual flow.

Verification:

- Run a synthetic printed-display corpus and physical-camera checks; reject implausible readings.

Required completion evidence: implementation, verification, design, android_device, ios_device.

### P07-03: Implement Android and iOS share-to-draft entry

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P07-01.

Acceptance criteria:

- Accept explicitly shared text through native targets, validate payload limits and route to a locked/reviewable draft.
- Do not read the SMS inbox or clipboard automatically.

Verification:

- Test cold/warm start, locked app, duplicate share, unsupported payload and share cancellation on both devices.

Required completion evidence: implementation, verification, design, android_device, ios_device.

### P07-04: Implement Bangla voice drafting with clear processing choice

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P07-01.

Acceptance criteria:

- Show start/stop/cancel and processing/offline availability; obtain native mic permission only when used.
- Review transcript and parsed fields; do not retain raw audio by default.

Verification:

- Check bn-BD examples, noisy input, provider/network failure, permission denial and stopping after backgrounding.

Required completion evidence: implementation, verification, design, android_device, ios_device, review.

### P07-05: Validate quick-capture effort and v3 interaction parity

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P07-02, P07-03, P07-04.

Acceptance criteria:

- Measure task success, corrections and steps against existing manual entry with representative pilot owners.
- All capture flows preserve fixed Save placement, native back behavior, focus, reduced motion and confirmed Undo.

Verification:

- Record results for photo/share/voice/manual paths on both platforms; retain limitations rather than a blanket accuracy claim.

Required completion evidence: verification, design, android_device, ios_device, review.

**Phase exit gate:**

- Camera, share target and voice each have a real implementation or a clearly unavailable state on each platform.
- All extracted text, amounts and odometers are reviewed; low confidence never silently writes data.
- Native permissions and offline capability are honestly reflected in the UI.

**Risks and decisions:** Bangla speech accuracy varies by device/provider and may require network. Decide data processing and retention before selecting a provider; an unavailable provider must not block manual entry.

<a id="p08"></a>

## P08: Core ownership release gate

**Priority:** P1 | **Scope:** release | **Current status:** Waiting for prerequisites

**Outcome:** A reviewable, recoverable and verified core app can be piloted and released independently of later additions.

**Why now:** Release confidence is a separate deliverable from completing feature code.

**Entry dependencies:** P03-05, P04-05, P05-04, P06-05, P07-05.

**V3 placement and interaction:** Audit the complete four-tab experience. Remove obsolete demo claims from enabled production flows while preserving v3 layout, motion and native brand logos.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/App.tsx`, `design/app-ui-v3/src/screens/Home.tsx`, `design/app-ui-v3/src/screens/Vault.tsx`, `design/app-ui-v3/src/screens/History.tsx`, `design/app-ui-v3/src/screens/Me.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/App.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt`, `iosApp/MotoVault.xcodeproj`.
- Proposed sources: None.

**Data and migration:** Freeze a supported schema and backup version; validate upgrade from the committed baseline and retain rollback/recovery instructions.

**Tasks:**

### P08-01: Run core regression, migration and design matrix

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Verify fresh install and baseline upgrade with full synthetic records and a restorable backup.
- Cover all four tabs, sheets, both digit styles, nine brand accents, light/dark and 1.0/1.3 text scales; record motion separately.

Verification:

- Run the focused shared/platform suites plus Android/iOS builds and inspect native screenshots/recordings.

Required completion evidence: verification, design, android_runtime, ios_runtime.

### P08-02: Run physical-device accessibility, reliability and performance pass

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P08-01.

Acceptance criteria:

- Check notification/background, camera, credentials, restore, file pickers and denied permissions on physical devices.
- Measure startup, scroll, large-vault operations, memory and battery on a representative lower-resource Android plus an iPhone; define budgets from measurements.

Verification:

- Use TalkBack/VoiceOver, large text, keyboard/focus and offline/failure scenarios; report measured budgets and results.

Required completion evidence: verification, design, android_device, ios_device.

### P08-03: Pilot with representative Bangladesh owners and resolve release blockers

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P08-02.

Acceptance criteria:

- Include commuters, a first-time/used-bike owner and an enthusiast; obtain consent for anonymized observations.
- Track backup/restore success, reminder usefulness, log effort and cost comprehension; close data-loss/privacy/incorrect-guidance defects.

Verification:

- Retain anonymized findings and explicit pass/fail release decisions; do not invent retention statistics before observing them.

Required completion evidence: verification, review.

### P08-04: Prepare and verify core release candidate and recovery handoff

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P08-03.

Acceptance criteria:

- Record release build, signing/distribution state, enabled features, privacy/store declarations and known limits.
- Prepare release notes, support/recovery instructions and rollout/rollback criteria; record actual distribution proof when performed.

Verification:

- Install the signed candidate on both platforms and repeat the restore/upgrade smoke path; publication remains a separately authorized action.

Required completion evidence: verification, android_device, ios_device, review.

**Phase exit gate:**

- Core feature acceptance and migration/restore gates pass.
- Real Android and iPhone lifecycle, offline, privacy, accessibility and performance evidence exists.
- A pilot report resolves critical failures; release packaging, signing, privacy declarations and store review are separately approved/executed.

**Risks and decisions:** Do not delay the core release for P2/P3 scope. Store publication, production services and new paid dependencies require their own concrete action scope.

<a id="p09"></a>

## P09: Multiple bikes and ownership boundaries

**Priority:** P2 | **Scope:** growth | **Current status:** Waiting for prerequisites

**Outcome:** Owners can keep separate bikes and histories without replacing an old bike record.

**Why now:** Provides the ownership foundation for workshop records, resale and rider reports.

**Entry dependencies:** P08-04.

**V3 placement and interaction:** Use the existing Home bike header to open a bike-selector sheet. Me manages add/archive; keep all four tabs and brand-logo picking with the selected bike theme.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Home.tsx`, `design/app-ui-v3/src/screens/Onboarding.tsx`, `design/app-ui-v3/src/screens/Me.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultState.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Onboarding.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/garage/BikeOwnership.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/garage/BikeSwitcherSheet.kt`.

**Data and migration:** Migrate primary into a stable bike ID. Scope schedules/logs/assets/reminders/expenses by bike and licences by owner; preserve old single-bike backups and active-bike preference.

**Tasks:**

### P09-01: Finalize garage ownership model and migration fixtures

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Define person versus bike documents, active/archive status, stable IDs and record ownership.
- Map every existing primary record and shared asset to an explicit owner without duplication.

Verification:

- Review migration cases with two bikes, shared owner documents, duplicate model names and archived bikes.

Required completion evidence: plan, review.

### P09-02: Implement bike-scoped persistence and backward-compatible backups

Status: **Planned** | Size: **8** | Owner: unassigned.

Additional task dependencies: P09-01.

Acceptance criteria:

- Migrate existing data transactionally and retain all historical records and asset bytes.
- Update backup versions, import/restore and cost/reminder queries to require correct bike scope.

Verification:

- Test interrupted migration, old archive restore, no cross-bike queries and foreign-key/asset invariants.

Required completion evidence: implementation, verification.

### P09-03: Build bike switcher, add and archive in v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P09-02.

Acceptance criteria:

- Open a recognizable bike-selector sheet from the Home header; preserve selection across restarts.
- Adding does not replace history; archiving has a clear reversible state and inactive-bike visibility.

Verification:

- Check per-bike brand accents/logos, focus, large text and no misleading empty state while switching.

Required completion evidence: implementation, verification, design.

### P09-04: Scope capture, notifications, privacy and Undo to a bike

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P09-03.

Acceptance criteria:

- Deep links and share drafts identify the intended bike even if another is active.
- A delayed save, notification or Undo cannot mutate the newly selected bike.

Verification:

- Race bike switching against async capture/save/notification; check locked and archived targets.

Required completion evidence: implementation, verification.

### P09-05: Verify cross-bike isolation, migration and restore

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P09-04.

Acceptance criteria:

- Run two-bike fresh/upgrade/backup scenarios on both hosts.
- Totals, service tasks, renewals, files and app theme remain isolated with no loss of the original single-bike history.

Verification:

- Retain fixture hashes, native flows and regression reports.

Required completion evidence: verification, design, android_runtime, ios_runtime, review.

**Phase exit gate:**

- Switching bikes cannot mix costs, schedules, files, reminders or Undo.
- Archive preserves records; deleting a bike is a separately reviewed destructive action.
- Old backups restore to one bike and new backups preserve every bike.

**Risks and decisions:** This is a significant migration. Freeze the ownership model before implementation and test inactive-bike notification routing.

<a id="p10"></a>

## P10: Workshop, repair and parts warranty records

**Priority:** P2 | **Scope:** growth | **Current status:** Waiting for prerequisites

**Outcome:** Owners can explain previous repairs, compare quotations and follow up on a parts warranty.

**Why now:** Builds on itemized service visits without needing a public workshop marketplace.

**Entry dependencies:** P09-05, P04-05.

**V3 placement and interaction:** ServiceSheet adds a recent-workshop picker and optional repair/warranty details. History expands into a repair detail sheet, using existing rows, chips and attachments.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/sheets/ServiceSheet.tsx`, `design/app-ui-v3/src/screens/History.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/ServiceCatalogue.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/SmartServiceDraft.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultState.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/repairs/RepairCase.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/repairs/RepairDetailsSheet.kt`.

**Data and migration:** Add owner-managed contacts, quotations, repair cases and warranty dates linked to bike/service/part IDs. Distinguish quoted cost from actual paid expense.

**Tasks:**

### P10-01: Model workshops, repair cases, quotations and warranty links

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Link recurring issues and parts warranties to actual visits; keep contacts owner-managed.
- Separate quotation, approved work and paid expense states.

Verification:

- Test repeated repairs, expired warranty, duplicate workshop names and migrated free-text workshop fields.

Required completion evidence: implementation, verification.

### P10-02: Implement contact and repair/warranty flows in v3

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P10-01.

Acceptance criteria:

- Extend the current service sheet with optional detail sections and a recent-workshop chooser.
- Show repair timeline, receipt and warranty reminder actions in History.

Verification:

- Exercise quote-to-service, edits, cancelled work, contact call action and no attachment leak when locked.

Required completion evidence: implementation, verification, design.

### P10-03: Connect expenses, warranty reminders and portable records

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P10-02.

Acceptance criteria:

- Only actual paid work contributes to costs; service/reminder links survive edits and Undo.
- Backup/restore includes contacts, cases, warranties and referenced assets.

Verification:

- Check no duplicate expense/reminder and cross-bike isolation after restore.

Required completion evidence: implementation, verification.

### P10-04: Verify workshop and recurring-repair scenarios

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P10-03.

Acceptance criteria:

- Run a quote, completed repair, repeat issue and warranty follow-up on Android and iOS.
- Preserve v3 quick service entry when no optional repair detail is needed.

Verification:

- Retain native and calculation/backup evidence.

Required completion evidence: verification, design, android_runtime, ios_runtime.

**Phase exit gate:**

- Quotation, actual service, recurring issue and warranty coverage remain distinguishable.
- Contacts are entered/selected deliberately with no address-book upload.
- Related photos/receipts follow privacy and backup policies.

**Risks and decisions:** Owner history is not a verified public workshop rating. Avoid duplicate expenses from a quotation and a later service.

<a id="p11"></a>

## P11: Used-bike handover pack and service passport

**Priority:** P2 | **Scope:** growth | **Current status:** Waiting for prerequisites

**Outcome:** A seller can share a selected, understandable ownership history while retaining control of private data.

**Why now:** Extends the existing passport into a useful used-bike handover workflow.

**Entry dependencies:** P10-04, P05-04, P03-05.

**V3 placement and interaction:** History > Service passport opens a selection/review sheet; Me/bike settings links to an ownership-transfer checklist. Match existing export review and native share behavior.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/History.tsx`, `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/components/Sheet.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/ServicePassport.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/PassportExportUi.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/OwnershipForms.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/passport/HandoverPack.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/passport/HandoverSheet.kt`.

**Data and migration:** Version the export separately from backup. Export selected records with provenance, history gaps, generated date and optional redaction; do not transfer the seller account, local keys or private documents by default.

**Tasks:**

### P11-01: Define selective pack, provenance and privacy defaults

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Specify export schema, selected bike scope, unknown-history disclosures and per-field redaction.
- Define what the digest covers and keep official ownership transfer a separate checklist.

Verification:

- Review misleading authenticity claims and inspect the exact default field selection.

Required completion evidence: plan, review.

### P11-02: Extend passport export and verification without breaking old versions

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P11-01.

Acceptance criteria:

- Generate reviewed PDF/data with stable canonical encoding and clear provenance.
- Verify supported old passports and detect tampering; do not label a recomputed self-digest as third-party authentication.

Verification:

- Use independent golden digests, version fixtures, unknown fields, altered PDF/data and redaction-leak checks.

Required completion evidence: implementation, verification.

### P11-03: Build handover selection, preview and sourced checklist

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P11-02.

Acceptance criteria:

- Reuse v3 sheets and native sharing with explicit selected-bike and recipient-visible preview.
- Show source freshness and working official links; share/cancel never changes ownership records.

Verification:

- Check long histories, page layout, large text, offline checklist and export cleanup on both platforms.

Required completion evidence: implementation, verification, design.

### P11-04: Verify buyer-readable output and privacy boundaries

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P11-03.

Acceptance criteria:

- Render/inspect every PDF page and verify the paired data independently.
- Record both native share/cancel paths and no leakage of excluded fields or other bikes.

Verification:

- Use a synthetic seller/buyer scenario; confirm original records remain unchanged.

Required completion evidence: verification, design, android_runtime, ios_runtime, review.

**Phase exit gate:**

- Owner previews exactly what is shared and can exclude contacts, IDs, locations, receipts and documents.
- Clearly distinguish owner-entered, imported and supported workshop evidence.
- A digest proves consistency of covered data, not mechanic authenticity, legal ownership or physical bike condition.

**Risks and decisions:** Official transfer requirements change. A handover pack does not execute legal ownership transfer or erase the seller vault.

<a id="p12"></a>

## P12: Real home-screen widgets and fast actions

**Priority:** P2 | **Scope:** growth | **Current status:** Waiting for prerequisites

**Outcome:** Owners can see the next task and open fuel/odometer entry without navigating through the app.

**Why now:** Makes the prototype widget concept useful on the actual phone.

**Entry dependencies:** P09-05, P01-05, P02-05.

**V3 placement and interaction:** Promote Me widget preview to native setup/help. Widget uses the v3 visual roles with minimal summary and direct links to existing quick-add sheets.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/components/TabBar.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Screens.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Motion.kt`, `iosApp/MotoVault.xcodeproj`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/widgets/WidgetSnapshot.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/widgets/WidgetSetupSheet.kt`.

**Data and migration:** Publish a minimal, non-sensitive durable snapshot for the chosen bike. Never expose document IDs or decrypted vault data; stale snapshot time is explicit.

**Tasks:**

### P12-01: Define minimal widget snapshot and deep-link contract

Status: **Planned** | Size: **2** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Specify bike scope, last-update time, permitted summary fields and privacy behavior.
- Use read-only snapshots and route mutations to reviewed app sheets.

Verification:

- Test archived/missing bike, locked state and stale snapshot selection.

Required completion evidence: plan, review.

### P12-02: Implement Android widget and iOS widget extension

Status: **Planned** | Size: **8** | Owner: unassigned.

Additional task dependencies: P12-01.

Acceptance criteria:

- Implement actual host widgets with reviewed entitlements and minimal shared storage.
- Update after durable relevant changes and handle OS refresh limits.

Verification:

- Install/add/remove widgets and test process death, reboot, locked device and stale data on physical devices.

Required completion evidence: implementation, verification, android_device, ios_device.

### P12-03: Connect widget setup and quick actions to v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P12-02.

Acceptance criteria:

- Replace preview-only wording with truthful setup, availability and selected-bike controls.
- Fuel and odometer actions open the corresponding sheet after any required unlock.

Verification:

- Inspect widget typography, contrast, theme variants and native deep-link focus.

Required completion evidence: implementation, verification, design.

### P12-04: Verify widget lifecycle, privacy and resource use

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P12-03.

Acceptance criteria:

- Document measured refresh behavior and snapshot freshness under real OS constraints.
- Confirm no private fields leak and no aggressive background polling is introduced.

Verification:

- Retain physical Android/iPhone screenshots and lifecycle/battery observations.

Required completion evidence: verification, design, android_device, ios_device.

**Phase exit gate:**

- Real Android and iOS widgets render the selected bike and navigate to correct locked/unlocked entry points.
- Widget refresh follows OS constraints and indicates stale data.
- Disabling/removing a widget leaves owner data unchanged.

**Risks and decisions:** An in-app preview is not a native widget. iOS needs extension/app-group/entitlement review and minimal data sharing; no precise refresh-time promise.

<a id="p13"></a>

## P13: Seasonal care and user-controlled breakdown help

**Priority:** P2 | **Scope:** growth | **Current status:** Waiting for prerequisites

**Outcome:** Owners can find relevant care checks and their chosen help contacts when needed.

**Why now:** Adds practical wet-weather and breakdown support without requiring constant tracking.

**Entry dependencies:** P09-05, P04-05, P02-05.

**V3 placement and interaction:** Home shows at most one dismissible care card when relevant. A Help sheet holds saved contacts, manual location and deliberate share; keep the urgent maintenance card first.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Home.tsx`, `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/components/Sheet.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/LocationCapture.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/ServiceCatalogue.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Screens.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/care/SeasonalChecklist.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/help/BreakdownHelpSheet.kt`.

**Data and migration:** Store selected region, content source/version and owner-chosen contacts. Use optional foreground location only; freshness/expiry are part of any cached weather item.

**Tasks:**

### P13-01: Review seasonal content, sources and help boundaries

Status: **Planned** | Size: **2** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Source wet/dust/heat care instructions from applicable manuals and weather links from BMD.
- Define offline-only behavior first; select contacts manually and avoid unsupported emergency promises.

Verification:

- Review source freshness, applicability and stale/unavailable weather wording.

Required completion evidence: research, review.

### P13-02: Implement offline care checklist and bounded optional weather access

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P13-01.

Acceptance criteria:

- Show region, last update and expired-cache state; no precise background location is needed.
- Follow source terms and use an external official link if no supported feed is available.

Verification:

- Test offline, stale cache, unavailable source and dismissing/re-enabling a card.

Required completion evidence: implementation, verification.

### P13-03: Build breakdown contacts and deliberate location sharing in v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P13-02.

Acceptance criteria:

- Keep contacts available offline; open native call/share actions deliberately.
- Show location accuracy/time and editable text; denial or no GPS still allows help.

Verification:

- Test no network, no location, timeout, locked app and share cancellation on both physical devices.

Required completion evidence: implementation, verification, design, android_device, ios_device.

### P13-04: Verify care relevance, offline help and privacy

Status: **Planned** | Size: **2** | Owner: unassigned.

Additional task dependencies: P13-03.

Acceptance criteria:

- Check region/bike scope and that care cards do not displace the most urgent task.
- Record source review and physical-device contact/location behavior with synthetic contacts.

Verification:

- Verify no background tracking or automatic communication and inspect accessibility states.

Required completion evidence: verification, design, android_device, ios_device, review.

**Phase exit gate:**

- Offline care checklists are available with source and applicability.
- Weather alerts have official attribution and freshness; a public API is not assumed.
- Calling/sharing requires owner action and no automatic SOS or continuous location collection.

**Risks and decisions:** A weather warning does not establish road safety. Start with sourced links/manual region; add a live feed only after terms, availability and data handling are verified.

<a id="p14"></a>

## P14: Optional professional-rider earnings mode

**Priority:** P3 | **Scope:** optional | **Current status:** Waiting for prerequisites

**Outcome:** A rider can understand recorded work earnings after operating costs.

**Why now:** Useful to delivery/ride-sharing riders while keeping ordinary ownership logging simple.

**Entry dependencies:** P09-05, P06-05.

**V3 placement and interaction:** Me enables Work mode; History gains an optional work filter/summary and a quick-entry sheet. Default Home and four-tab navigation remain intact.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/History.tsx`, `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/sheets/QuickAdd.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultState.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Sheets.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/domain/usecase/FuelEfficiencyUseCase.kt`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/domain/work/WorkLedger.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/work/WorkSummarySheet.kt`.

**Data and migration:** Add work shifts, earnings, deductions, work-distance attribution and owner-defined maintenance allowance. Keep actual expenses separate from estimates and prevent double subtraction.

**Tasks:**

### P14-01: Validate rider workflow and define accounting boundaries

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Observe representative riders and define shift, work kilometres, earnings, deductions and cost-allocation rules.
- Separate maintenance allowance from actual expense and show incomplete records clearly.

Verification:

- Review worked examples with mixed personal/work use, delayed payout and refunds.

Required completion evidence: research, plan, review.

### P14-02: Implement opt-in work ledger and reconciled summaries

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P14-01.

Acceptance criteria:

- Store money in poisha, scope to bike/shift and support corrections.
- Do not double-count fuel/service expenses or treat a budget allowance as cash paid.

Verification:

- Test mixed use, payout deductions, no mileage, backdated corrections and backup/restore.

Required completion evidence: implementation, verification.

### P14-03: Build optional work entry and reports in v3

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P14-02.

Acceptance criteria:

- Reuse keypad, date chips, quick-add and History summaries.
- Disabling Work mode preserves records and removes routine entry clutter.

Verification:

- Run no-work, mixed-work and incomplete-shift flows with both digit styles and large text.

Required completion evidence: implementation, verification, design.

### P14-04: Pilot rider mode and decide enablement

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P14-03.

Acceptance criteria:

- Reconcile sample summaries independently and collect anonymized rider feedback.
- Record whether the optional module is ready to enable; core release remains independent.

Verification:

- Run Android/iOS scenarios plus an agreed representative-rider pilot.

Required completion evidence: verification, design, android_runtime, ios_runtime, review.

**Phase exit gate:**

- Mode is opt-in and hidden from everyday flows when disabled.
- Net figures reconcile with selected recorded income, deductions and allocated costs.
- No earnings guarantee, automated platform account access or background tracking is implied.

**Risks and decisions:** Business/personal expense attribution needs explicit rules and usable corrections. Validate the module with actual professional riders before enabling broadly.

<a id="p15"></a>

## P15: Optional encrypted cloud backup and device recovery

**Priority:** P3 | **Scope:** optional | **Current status:** Waiting for prerequisites

**Outcome:** An owner can recover a portable encrypted snapshot from an explicitly connected account.

**Why now:** Build cloud transport only after local encrypted restore and bike-scoped ownership are proven.

**Entry dependencies:** P03-05, P09-05, P02-05.

**V3 placement and interaction:** Me > Backup adds explicit connect/disconnect, last successful backup, pending/error state and restore selection using the same local restore review.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/screens/Me.tsx`, `design/app-ui-v3/src/components/Sheet.tsx`, `design/app-ui-v3/src/components/Toast.tsx`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/storage/LocalSync.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt`, `docs/LOCAL_SYNC_PREPARATION.md`, `docs/backend/local-sync-preparation.sql`.
- Proposed sources: `composeApp/src/commonMain/kotlin/com/motovault/app/storage/backup/CloudBackupRepository.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/ui/backup/CloudBackupSheet.kt`.

**Data and migration:** Reuse the portable encrypted manifest with immutable objects, account-to-vault binding, authenticated publication and remote revision preconditions. Device restore creates a reviewed local state; do not silently multi-writer merge ciphertext.

**Tasks:**

### P15-01: Select provider/account model and review cloud recovery design

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Record provider choice, project, auth, billing/quota, key recovery and metadata disclosure.
- Define explicit account binding, logout/switch behavior, retention, remote deletion and single-writer/conflict behavior.

Verification:

- Review against existing local-sync contract and complete the chosen provider security/terms review before deployment.

Required completion evidence: plan, review.

### P15-02: Implement staged backend isolation and publication contract

Status: **Planned** | Size: **8** | Owner: unassigned.

Additional task dependencies: P15-01.

Acceptance criteria:

- Use private immutable encrypted objects, owner-scoped authorization and atomic revision-checked publication.
- Keep privileged service credentials out of app builds; make receipt/idempotency checks enforceable server-side.

Verification:

- Run account A/B denial tests, cross-owner object paths, duplicate operation IDs and concurrent revision conflicts in the selected staging project.

Required completion evidence: implementation, verification, cloud, review.

### P15-03: Implement encrypted uploader, retries and account lifecycle

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P15-02.

Acceptance criteria:

- Freeze a durable revision, encrypt and upload all referenced assets before publishing its manifest.
- Stop jobs on logout/switch and retain newer queued revisions when acknowledging an older upload.

Verification:

- Test network loss, token expiry, cancellation, process death, stale revisions, quota and account switch.

Required completion evidence: implementation, verification, cloud.

### P15-04: Build truthful connection, backup status and restore selection

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P15-03.

Acceptance criteria:

- Show disconnected/pending/running/failed/last-success states based on confirmed transport results.
- Use existing restore preview and explicit account/vault/secret review; no connected badge from configuration alone.

Verification:

- Exercise offline/error/auth/conflict and accessible progress without blocking local record entry.

Required completion evidence: implementation, verification, design.

### P15-05: Run cross-device recovery, isolation and failure drills

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: P15-04.

Acceptance criteria:

- Restore Android-to-iPhone and iPhone-to-Android with a new device and correct recovery method.
- Prove wrong-account denial, missing-asset rejection, altered ciphertext rejection and no overwrite on conflict.

Verification:

- Retain sanitized staging reports, manifests/hashes and physical-device recovery evidence.

Required completion evidence: verification, cloud, android_device, ios_device, review.

### P15-06: Review production readiness and explicitly enable cloud backup

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P15-05.

Acceptance criteria:

- Record actual production configuration, provider cost controls, retention/recovery support and reviewed release settings.
- Enable only after concrete production-action authorization and recorded smoke proof; otherwise keep cloud disconnected.

Verification:

- Verify signed app account binding, actual backup receipt and restore path in the approved environment.

Required completion evidence: verification, cloud, review.

**Phase exit gate:**

- Only ciphertext and minimal approved metadata reach the chosen provider.
- Two-account isolation, conflict handling, cancelled uploads, retry and cross-device recovery pass in staging.
- Account switching cannot assign existing local records to a new owner; recovery secrets and remote retention/deletion are explicit.

**Risks and decisions:** Supabase is an existing preparation seam, not a deployed backend. Provider, project, auth, cost/quota, recovery and production actions need concrete decisions. Concurrent multi-device editing is separate future scope.

<a id="p16"></a>

## P16: Expanded ownership release and ongoing maintenance

**Priority:** P2 | **Scope:** release | **Current status:** Waiting for prerequisites

**Outcome:** The additional ownership features ship with regression evidence and a maintained support/source-refresh process.

**Why now:** Protects the reliable core while adding more models, records and OS integrations.

**Entry dependencies:** P08-04, P10-04, P11-04, P12-04, P13-04.

**V3 placement and interaction:** Run the complete v3 matrix for the core and enabled additions, including multi-bike accents and native widget surfaces. Optional modules remain disabled until their own gates pass.

**Implementation map:**

- Prototype references: `design/app-ui-v3/src/App.tsx`, `design/app-ui-v3/src/styles/base.css`, `design/app-ui-v3/src/styles/app.css`, `design/app-ui-v3/src/lib/motion.ts`.
- Existing native sources: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt`, `composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt`, `iosApp/MotoVault.xcodeproj`.
- Proposed sources: None.

**Data and migration:** Document the complete supported upgrade/backup matrix, retention policy and model/manual content versions; old records and passport formats remain readable as promised.

**Tasks:**

### P16-01: Run expanded migration, backup and v3 regression matrix

Status: **Planned** | Size: **5** | Owner: unassigned.

Additional task dependencies: None.

Acceptance criteria:

- Upgrade from baseline and core release with multiple bikes, repair records, handover exports and all asset types.
- Recheck design, accessibility, widgets, reminders and privacy on both physical platforms.

Verification:

- Run shared/platform suites and retain independent cost totals, archive hashes and native recordings.

Required completion evidence: verification, design, android_device, ios_device.

### P16-02: Verify enabled-module gates and expanded release candidate

Status: **Planned** | Size: **3** | Owner: unassigned.

Additional task dependencies: P16-01.

Acceptance criteria:

- Keep P14/P15 disabled unless their completion gates pass; record exactly what is enabled.
- Prepare signed candidate, upgrade/recovery notes, support handoff and rollout decision.

Verification:

- Install candidate on both platforms, run fresh/upgrade/restore smoke and record distribution state separately.

Required completion evidence: verification, android_device, ios_device, review.

### P16-03: Assign maintenance ownership and review observed product outcomes

Status: **Planned** | Size: **2** | Owner: unassigned.

Additional task dependencies: P16-02.

Acceptance criteria:

- Assign manual/checklist/link freshness, OS compatibility, backup drills and privacy support ownership.
- Review observed log completion, restore success, reminder action and comprehension; create next work from evidence.

Verification:

- Retain an anonymized release/pilot review with a dated next review and explicit action owners; do not create a scheduled automation without a separate request.

Required completion evidence: plan, review.

**Phase exit gate:**

- Core and additional ownership workflows pass a full old-install upgrade and physical-device regression.
- Every enabled optional module has passed P14-04/P15-06 as applicable.
- Support instructions, source freshness ownership and measured release/pilot outcomes are assigned.

**Risks and decisions:** Do not mark an optional service enabled merely to make the roadmap percentage increase. Treat a deferred feature as visible unshipped scope.

## Release evidence checklist

- Shared rule tests, migration/asset tests, Android build and iOS build/test results are separately recorded.
- Prototype review, native screenshots and native motion recordings are separately recorded against the same design contract.
- Physical Android and physical iPhone checks cover the OS-dependent functionality enabled in the release; unavailable hardware keeps its gate open.
- Optional external services use an explicitly selected account/project and staging validation before production writes. A missing public API never silently becomes scraping or an invented verification result.
- Core release is independent of growth features. Features whose gates are open remain disabled and honestly labelled in the production UI.
