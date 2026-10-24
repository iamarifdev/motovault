# Baseline for the feature roadmap

Inspected on 24 October 2026, Bangladesh local date. Repository branch: `main`.
HEAD: `50eda3a` (`feat(ui): redesign MotoVault with prototype visual system, motion, and localized UX`).
The initial planning inventory was source-only. P00 subsequently built and ran
isolated synthetic test identities; [the current capture report](evidence/P00-04-verification.md)
records that runtime evidence and its limits. Production main-source behavior
remains unchanged.

## Existing implementation to preserve and extend

| Area | Current evidence | Planning implication |
| --- | --- | --- |
| Production entrypoint | `composeApp/src/commonMain/kotlin/com/motovault/app/App.kt` calls `ui.MotoVault()` | Work in the production package, not the removed staging package |
| V3 design | `design/app-ui-v3/src/`, tokens, README and current commit | Preserve four tabs, quick-add, typography, sheets, keypad, motion and native brand-logo exception |
| Durable local state | `storage/VaultDatabase.kt`, `VaultSession.kt`, `ui/VaultState.kt` | Migrate existing Room/legacy data and coordinate writes/Undo/side effects |
| Documents | `ui/DocumentFlow.kt`, `DocumentCapture.kt`, native adapters | Extend capture/import/review/originals; do not rebuild a vault as if none exists |
| Service and fuel | Itemized visits, receipts, full/partial fills, economy, GPS labels | Extend costs and capture while preserving existing semantics |
| Document renewals | Shared/native `ui/RenewalReminders.kt` | Preserve identity/consumption/restart safeguards while fixing settings wiring |
| Factory profiles | `ui/FactoryProfiles.kt` | One tightly scoped KTM 125 Duke 2023 Europe manual profile; other schedules are owner-defined examples |
| Service passport | `ui/ServicePassport.kt`, `PassportExportUi.kt` | Extend versioned data/PDF export and precise digest claims |
| Local sync | `storage/LocalSync.kt`, `docs/LOCAL_SYNC_PREPARATION.md` | Outbox/backend preparation exists; encrypted transport/working cloud restore do not |
| Platform support | Android source set and `iosApp/MotoVault.xcodeproj` | Android and iOS are both in scope; native/physical evidence remains separate |

## Concrete gaps observed in source

1. `serviceReminders` is persisted and bound to a switch, but the inspected native
   reconciliation accepts documents and does not schedule service tasks.
2. Me/Vault text promises a 30-day warning while existing document policy/channel
   documentation uses 60 days. New `docReminders` state and existing platform
   opt-in need one durable, tested settings contract.
3. V3 `lib/wear.ts` computes the remaining fraction of a time/mileage interval.
   A percentage is not a physical measurement of brakes, oil or other parts.
4. `docs/LOCAL_SYNC_PREPARATION.md` explicitly records no implemented encryption,
   recoverable backup, account binding or sender. Prepared SQL is not deployed
   isolation proof.
5. `VaultState` currently has one `Ride`; Room uses a `primary` vehicle. Multiple
   bikes require a real migration, scoped records and old-backup compatibility.
6. Native/widget preview concepts and prototype OCR/share/voice interactions do
   not establish full working native features. Scope each capability by platform
   and test it instead of promoting demo labels to shipped claims.
7. README/UI-STRUCTURE still pointed to v2. This change updates the active design
   links to v3 while keeping historical v2 evidence labelled as historical.

No physical-device, native notification-delivery, camera, crypto, cloud, signing
or release claim is made by P00. Previous reports remain historical evidence;
P00-04 now provides the new reproducible emulator/simulator/prototype baseline.

## P00 runtime findings

The [comparison register](evidence/P00-04-design.md) records seven concrete
differences/gaps. Most urgently, both isolated native apps retained odometer
12350 after an Undo intended to restore 12340, and Room does not round-trip
several v3 settings. Unknown history, no-expiry labels, native default-language
labels, Vault layout and iOS hit/accessibility geometry have explicit follow-up
tasks. These are recorded baseline defects, not P00 fixes.

## Source and workspace safeguards

Before this change, there were no tracked working changes. Existing untracked
entries included `.claude/`, `.serena/`, `DESIGN-IS-2026-10-14/`, `design/app-ui-v3/`,
and three UI audit/verification documents. Preserve them; this task does not
delete, stage, commit or push those files. The design reference fingerprint
captures source paths/bytes without copying node_modules or build output.

## Audience assumptions to validate

Initial focus: daily commuters, first-time/used-bike owners and enthusiasts.
Professional delivery/ride-sharing use is optional. These are hypotheses, not
measured market shares or confirmed retention needs. Validate through the core
pilot and professional-rider phase. Priorities favor data recovery, trustworthy
reminders, paperwork, ownership cost and low-effort logging.

## External sources and freshness

- [BRTA electronic-certificate verification](https://bsp.brta.gov.bd/bsp/qr/qrScanner?lan=en),
  checked 24 October 2026: offers QR scanning or certificate PDF/image upload for
  e-tax token, e-driving licence and e-fitness verification. This does not establish
  a public API or permission to upload user documents automatically.
- [Bangladesh Meteorological Department warnings](https://www.bmd.gov.bd/web/en/warning),
  checked 24 October 2026: lists heavy-rainfall, Kalbaishakhi and heat-wave warning
  categories. A production data feed, usage terms and refresh behavior remain to
  be verified in P13.
- The previously identified `https://bsp.brta.gov.bd/ownershipTransfer` returned
  HTTP 404 during this planning pass. P05/P11 must find and verify a current
  official transfer source before publishing checklist links. Do not ship the
  broken link as working guidance.

Fees, fuel prices, legal requirements, maintenance intervals and official links
need source/applicability/freshness checks in their implementation tasks. The
prototype's fuel prices are demo placeholders.
