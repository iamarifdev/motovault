# Localization and responsibility gap audit

Status: audit complete, remediation tracked as follow-up work. Date: 2026-11-01.
Scope: Android and iOS production sources. Excludes test fixtures, generated
resources and synthetic probe data except where noted.

## Executive summary

The resource foundation exists and English/Bangla parity is tested, but most
production copy is still embedded in Kotlin or platform code. The app therefore
does not yet meet the “language change covers all user-facing text” requirement.

Measured gaps:

| Area | Bengali literals | Files |
| --- | ---: | ---: |
| Common Compose UI | 545 | 27 |
| Domain / persisted models | 66 | 5 |
| Android native | 25 | 5 |
| iOS native | 17 | 4 |
| Debug/fixture only | 6 | 1 |
| **Total scanned** | **659** | **42** |

> Progress update after the first settings slice: the live scanner now reports
> **46 literals** (17 common UI, 23 domain, 6 debug); the expanded literal gate is
> down to **18 occurrences**. All active screen and sheet copy, native Android/iOS
> prompts, authentication errors, preference enums, both service catalogues and
> date/unit formatting are now resource-backed. The remaining strings are matching
> aliases, parsing separators, comments, digit tables and legacy persisted
> defaults; they are not active screen copy.

There were **485 unique Bengali literals in common UI** at the initial audit. The original direct `Text`/`contentDescription` gate caught 97 occurrences. The
expanded gate now also scans `Field`, `VaultButton`, `Primary`, `Eyebrow`,
`Heading`, `VaultChip`, `Sheet`, `DocumentNote`, `DocumentCopy` and
`DocumentHeading`. The initial expanded baseline recorded 183 occurrences; the
current P08-05 baseline is 18. The lexical gate still does not see matching
aliases, indirect text or native-only strings.

The 2026-11-01 large-file baseline also still has nine production files above
400 lines; only two test-support files remain above the threshold after the
split work below.

## 1. Common UI copy — 545 literals

| File | Count | Main text |
| --- | ---: | --- |
| `ui/DocumentFlow.kt` | 0 | document capture, review, inspection copy — extracted |
| `ui/settings/MeScreen.kt` | 0 | profile/settings labels and actions — extracted in the first slice |
| `ui/ReminderSheets.kt` | 0 | reminder settings, permission and save states — extracted |
| `ui/PassportExportUi.kt` | 0 | passport review, export and verification — extracted |
| `ui/maintenance/ServiceSheet.kt` | 0 | chips, actions, validation, total labels — extracted |
| `ui/privacy/PrivacySheet.kt` | 0 | privacy controls and explanations — extracted |
| `ui/garage/GarageScreen.kt` | 0 | garage dashboard and empty states — extracted in the second slice |
| `ui/documents/DocAddSheet.kt` | 0 | document type, scan and review copy — extracted |
| `ui/DocumentWallet.kt` | 0 | wallet empty states and document actions — extracted |
| `ui/onboarding/OnboardingScreen.kt` | 0 | onboarding steps — extracted |
| `ui/garage/OdometerSheet.kt` | 0 | odometer validation and chips — extracted |
| `ui/documents/VaultScreen.kt` | 0 | vault headings and empty states — extracted |
| `ui/privacy/PrivacyGate.kt` | 12 | lock/inspection gate |
| `ui/MotoVault.kt` | 12 | Undo/failure toasts |
| `ui/quickadd/QuickAddSheet.kt` | 0 | quick-add choices — extracted |
| `ui/components/DocumentArt.kt` | 10 | document badges |
| `ui/history/PasteSheet.kt` | 0 | paste/import copy — extracted |
| `ui/documents/DocViewSheet.kt` | 9 | document detail copy |
| `ui/WearDomain.kt` | 9 | urgency and remaining-distance descriptions |
| `ui/DocumentLabels.kt` | 8 | document type labels |
| `ui/components/Gauges.kt` | 5 | gauge content descriptions |
| `ui/navigation/ShellModels.kt` | 4 | tab titles |
| remaining UI files | 20 | scattered labels and errors |

Many are plain `Text`, but a large share are `Field` labels, `VaultButton` text,
`VaultChip` labels, `Sheet` titles, `Eyebrow`/`Heading` copy, `DocumentNote`/
`DocumentCopy` prose, placeholders, accessibility descriptions, `when` branches
and template strings with formatting arguments.

## 2. Domain and persisted labels — 66 literals

| File | Count | Problem |
| --- | ---: | --- |
| `domain/model/ServiceCatalogue.kt` | 56 | Bangla/English catalogue labels and units stored as data |
| `domain/model/VaultPreferences.kt` | 5 | `Appearance`/`AppLanguage` labels carried by domain enums |
| `domain/model/VaultRecords.kt` | 3 | document-type matching strings (identifiers, not display) |
| `domain/vault/VaultState.kt` | 1 | persisted service detail `"পরিষেবার রেকর্ড"` |
| `domain/backup/BackupLog.kt` | 1 | persisted unit default `"টি"` |

The persisted `RideLog.detail`/service title and backup unit are owner-visible
text stored as data. They cannot be translated after the fact without a
migration/compatibility decision. New records should store a typed code or a
canonical detail, with presentation resolving the label.

## 3. Native platform text — 42 literals

- Android: `NativeSystemAuth.android.kt` (9), `DocumentCapture.kt` (7),
  `ServicePassport.kt` (5), `LocationCapture.android.kt` (2),
  `DocumentCamera.kt` (2).
- iOS: `DocumentCapture.kt` (6), `NativeSystemAuth.ios.kt` (6),
  `ServicePassport.kt` (3), `LocationCapture.ios.kt` (2).

These include notification content, permission explanations, camera errors,
location labels and export/share actions. They must use the platform resource
mechanisms (`res/values*/strings.xml`, `Localizable.strings`) or a
resource-aware shared presentation adapter, not Kotlin literals.

## 4. English-only user copy and errors

130 domain `require`/`check`/`error` messages exist across 19 files, including
`domain/vault/VaultState.kt` (44), `domain/backup/MvaZipArchive.kt` (19),
`ui/ServicePassport.kt` (12) and `storage/security/SecureAssetStore.kt` (12).
They are shown directly in some UI paths and are not localized.

## 5. Data that is not UI copy but needs an owner

- `ui/Catalog.kt` (35) and `ui/SmartParse.kt`/`SmartServiceDraft.kt` (40) are
  input aliases/catalogue data. They should stay data, but display labels should
  resolve through resources.
- `ui/VaultCreation.kt` (34) and `IosRuntimeFixtures.kt` are demo/fixture data and
  are intentionally excluded from UI copy.
- `ui/Bn.kt` month names and digit tables need locale-aware formatting, not
  translated literals in code.

## 6. Large-file SRP gaps

| File | Lines | Current responsibility | Split direction |
| --- | ---: | --- | --- |
| `ui/MotoVault.kt` | 371 | root composition and navigation | sheet routing extracted to `VaultSheetHost.kt`; tab content to `VaultTabContent.kt` |
| `ui/maintenance/ServiceSheet.kt` | 379 | chips, keypad, totals, validation | split into line editor, smart/date sections and workshop field |
| `ui/backup/BackupRestoreFileSection.kt` | 204 | picker and file summary | extracted |
| `ui/backup/BackupCreateContent.kt` | 291 | progress, warning and archive actions | further split when backup changes |
| `ui/fuel/FuelSheet.kt` | 361 | mode, chips, odometer, keypad | amount hero extracted; re-review on next fuel change |
| `androidInstrumentedTest/.../NativeAssetInstrumentation.kt` | 568 | test-only probe runner | stage checks already split into `Probe*Checks.kt`; keep runner |
| `commonTest/.../BackupTest.kt` | 583 | backup round-trip fixtures | split by backup stage when the test next changes |

Resolved during P04A/localization and SRP work: `MeScreen.kt` (73 + section files),
`GarageScreen.kt` (260 + sections), `Controls.kt` (three files),
`OnboardingScreen.kt` (279 + steps), `BackupCreateContent.kt` (291 + summary/
password sections), `BackupRestoreContent.kt` (375 + file section), `FuelSheet.kt` (361 + amount
hero), `ServiceSheet.kt` (379 + line editor, smart/date sections and workshop
field) and `MotoVault.kt` (371 + sheet host and tab content).

Each is behavior-preserving work; splitting and localization should happen
together only where the file is already being touched.

## 7. Fix architecture

Bangla copy follows [localization-style.md](localization-style.md): concise,
spoken product Bangla with familiar loanwords, not literal translation.

1. **Resource ownership**
   - Common copy: `commonMain/composeResources/values/strings.xml` and
     `values-bn/strings.xml`; keys grouped by feature (`garage_`, `fuel_`,
     `service_`, `document_`, `backup_`, `recovery_`, `privacy_`, `settings_`,
     `onboarding_`).
   - Use positional placeholders, plurals and locale-aware formatting; never
     concatenate translated fragments.
   - Keep stable data identifiers (catalogue IDs, document type codes) separate
     from display labels.

2. **Presentation boundary**
   - Move domain enum labels out of domain types. Domain carries codes; UI
     resolves them through resources.
   - Replace persisted display-only details with typed codes or canonical data;
     if a wire field must stay, preserve it and resolve new presentation text.
   - Map typed domain/application failures to localized presentation messages;
     do not expose raw exception text.

3. **Native text**
   - Android: `res/values/strings.xml`, `values-bn/strings.xml`; update
     notifications/camera/location/export adapters.
   - iOS: `en.lproj/Localizable.strings`, `bn.lproj/Localizable.strings`; use
     `NSLocalizedString` at the native boundary or a shared resource-aware
     adapter.

4. **Gate expansion**
   - Extend the lexical literal gate to `Field`, `VaultButton`, `Primary`,
     `Eyebrow`, `Heading`, `VaultChip`, `Sheet`, `DocumentNote`, `DocumentCopy`,
     `DocumentHeading`, placeholders and accessibility descriptions.
   - Add a baseline so extraction can proceed screen by screen; the fuel empty
     baseline stays stricter.
   - Keep manual review for indirect strings, `when` branches, native code and
     string templates.

5. **SRP split**
   - Split the nine remaining files into the sections listed above.
   - Add each new file to the responsibility baseline only if it still exceeds
     400 lines; do not create empty modules or arbitrary files.

## 8. Remediation order

| Slice | Scope | Exit |
| --- | --- | --- |
| L1 | shell/navigation, garage, settings, onboarding | all their user copy in resources; language switch verified |
| L2 | maintenance, fuel, history, quick-add | chips, labels, errors, totals and Undo copy localized |
| L3 | documents, privacy, passport/export | capture/review/inspection/wallet copy localized |
| L4 | backup, recovery, reminders | progress, validation, errors and native picker copy localized |
| L5 | domain/persisted labels and native errors | typed presentation labels; no translated text persisted as UI copy |
| L6 | remaining large-file splits | each file has one responsibility or a renewed exception |

Verification for every slice: English/Bangla resource parity, placeholder/plural
tests, language switching on Android and iOS, large text, and the expanded
literal gate. Existing direct `Text`/`contentDescription` baseline must not grow.
