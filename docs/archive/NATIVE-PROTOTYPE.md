> Historical implementation snapshot. This document describes an earlier version of MotoVault. See the [current app](../../README.md), [production structure](../UI-STRUCTURE.md) and [approved design](../../design/app-ui-v2/CONTINUE-HERE.md) for current behavior.

# MotoVault native prototype implementation

The app now supports System (default), Light, and Dark appearance while retaining each bike brand’s color family. See [theme implementation and verification](/Users/arif/workspace/bbn/motovault/docs/THEMES.md).

Reference: `/Users/arif/Documents/Codex/2026-10-10/red/outputs`.

The implementation follows the current `motovault-redesign.html`, `motovault-workflows.js`, `motovault-workflows.css`, and `bike-catalogue.js` together. The companion JavaScript replaces parts of the original static HTML.

## Prototype parity

| Prototype workflow | Native implementation |
| --- | --- |
| Choose a brand and open its model picker | Nine brand tiles show model counts and open the photo picker directly. |
| Search and select a photo card | All 78 model/trim references are bundled. Search stays within the chosen brand. Cards show a selected outline and check mark. The grid has two columns on small screens and three on larger screens. |
| Manual model entry | “My model isn’t listed” accepts a name without attaching another bike’s photo. |
| Confirm selected model | Model ID and name are saved with the ride. Setup summary, garage title, and hero photo use that selection. |
| Ownership setup | Edition/variant, year, odometer, registration, and last service mileage/date are editable. New schedules require confirmation that they are examples. |
| Service baseline | Chain 500 km / 30 days, oil 3,000 km / 180 days, and air filter 5,000 km / 180 days start from the owner-entered baseline. These are examples rather than verified factory recommendations. |
| Odometer and health | Mileage updates recalculate distance/time health. All components opens schedule details and supports additional components. |
| Log service | Component, actual mileage, date, cost, workshop, and notes are stored. The selected component baseline updates. |
| Edit schedules | Distance or time may be disabled separately. Both cannot be zero. Baseline mileage/date are editable. |
| Log fuel | Mileage, date, litres, cost, and full/partial status are stored. Partial fills accumulate between full tanks. The garage shows a weighted average over complete cycles. |
| Ownership history | All/service/fuel filters, service and fuel spend, workshop/notes, and delete confirmation are available. The current service baseline must be adjusted before its record can be deleted. |
| Documents | Add/edit/delete details with optional issue and expiry dates. Front/back JPEG, PNG, or WebP attachments are supported, up to 1 MB each. |
| Document viewing | Front/back stored copies, details, edit, and delete actions are available. Renewal warnings consistently use 60 days. |
| Inspection | A document-only view opens stored details and attachments and returns to the document list. |
| Replace the bike | The garage brand mark opens setup. Confirmation removes service/fuel data and retains documents, matching the one-bike prototype. |
| Saving | Atomic local record files are written before shared UI state changes. Save failures are shown. Earlier app-preference records remain readable. |

The common Kotlin catalogue retains each prototype model’s source URL and photograph mapping. The garage’s Photo source action opens that source.

## Source files

- `ui/BikeCatalogue.kt`: all model references and brand/search/legacy lookup.
- `ui/Onboarding.kt`: brand selection, photo picker, manual entry, ownership setup.
- `ui/MotoVault.kt`: shared shell, garage, vault, navigation, save/error handling.
- `ui/OwnershipForms.kt`: service, schedules, fuel, history, document and inspection views.
- `ui/VaultState.kt`: persistent records, validated updates, fuel cycles and migration.
- `ui/Appearance.kt`: System/Light/Dark preference, brand palettes, semantic colors, and appearance controls.
- Platform `AttachmentPicker.kt` and `VaultStorage.kt`: local file selection and atomic storage.
- `commonMain/composeResources`: model photographs, brand marks, DM Sans and Barlow Condensed.

## Verification

Build and test results are recorded in the delivery message. Compilation and installation do not establish rendered-layout or device-interaction proof. Android simulator rendering was subsequently inspected through explicitly authorized ADB navigation and screenshots; see the theme-verification evidence. iOS rendering remains unverified.

Focused tests cover the full catalogue count, unique IDs, brand-scoped search, wrong-brand selection rejection, selected-model persistence, manual photo fallback, legacy records, service baselines, partial fills, weighted economy, deletion rules, schedule validation, document date order, and the 60-day renewal boundary.

Use native Java 17 on Apple Silicon. This machine’s default Java 17 is Intel-only.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home rtk run ./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest :composeApp:compileKotlinIosArm64 :composeApp:iosSimulatorArm64Test
```

## Manual verification

1. Tap Yamaha and search for R15M. Select its photo card. Confirm that ownership setup and the garage show R15M with its own photograph.
2. Close and reopen the app. Confirm that model, edition, registration, mileage, and photograph remain correct.
3. Tap each brand. Check its model count, photo cards, search empty state, selection, and accent.
4. Use manual model entry. Confirm that the manual name survives restart and that no unrelated photo is shown.
5. Enter current mileage 4,280 km and a prior service baseline of 4,000 km. Check both distance and date-based health.
6. Log a service with mileage/date/cost/workshop/notes. Verify the component baseline and filtered history.
7. Try disabling both schedule intervals. Confirm rejection. Adjust a baseline before deleting the service that established it.
8. Record a full tank at 4,300 km, a 2 L partial fill at 4,400 km, then a 3 L full fill at 4,520 km. Confirm 44.0 km/L.
9. Add a document with no expiry, one expiring in 60 days, and one expiring today. Check labels and renewal summaries.
10. Attach front/back photos, edit the document, reopen it, flip it, and view it in inspection mode.
11. Review bike replacement, cancel it, then confirm it using disposable records. Check that documents remain while service/fuel records reset.
12. Check small screens, landscape, large text, keyboard visibility, TalkBack/VoiceOver, and restart persistence on a device.

## Remaining roadmap

The prototype excludes OCR, PDF passport export, cloud sync, encryption, biometric locking, native camera scanning, and brightness control. Those remain separate roadmap features. The checkout still has no iOS host app; shared iOS code and simulator domain tests can be built.

## Asset provenance

Model photographs and brand marks come from the supplied prototype’s bundled files. Per-model attribution is retained in `BikeCatalogue.kt`. WebP model photos are converted to PNG for shared Compose resources. SVG brand marks are converted to vector resources.

DM Sans and Barlow Condensed came from the Google Fonts repository; their SIL Open Font Licenses are bundled next to the fonts. Attachment rendering follows the [Compose resource image API](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html).
