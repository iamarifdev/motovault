# MotoVault

MotoVault is a Bangla-first motorcycle maintenance and document app for Bangladesh, built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS.

## Current app

- Anek Bangla typography, System/Light/Dark appearance and bike-brand accent colors.
- Nine brands, 78 reference model/trim records with photographs, search and manual model entry.
- Owner-entered mileage and service baselines, editable reminders and component wear calculations.
- Itemized service visits with common parts/services, mineral/semi-synthetic/full-synthetic oil details, prices, labour, discounts and optional receipts.
- Fuel history, partial fills and weighted economy across completed full-to-full cycles.
- Optional foreground GPS for fuel stations/workshops, with manual location entry and saving without a GPS fix.
- Document capture/import, review of extracted details, original copies, expiry status and opt-in local renewal notifications.
- Document-only roadside viewing with a scoped brightness override.
- Separate document-renewal expense records and owner-reviewed PDF/JSON service passports.
- Local Room persistence with schema migrations and durable save handling. Sync preparation exists, but a cloud service is not connected.

Catalogue records and generic example schedules do not establish compatibility or factory service recommendations. The optional manual-backed factory profile is scoped by model/year/market; see [factory setup](docs/CONTINUATION_PROGRESS.md).

## Source structure

Shared production screens and platform contracts live in `composeApp/src/commonMain/kotlin/com/motovault/app/ui/`. Android and iOS implementations mirror that package in their source sets. Shared behavior lives under `domain/`, and persistence coordination lives under `storage/`.

The native iOS host is `iosApp/MotoVault.xcodeproj`, with shared scheme `MotoVault`. See [production UI structure](docs/UI-STRUCTURE.md).

## Build and run

Use JDK 17, the Android SDK configured by Gradle, and Xcode for iOS.

```sh
rtk run ./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
rtk run ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

On Apple Silicon, use an ARM64 JDK. If needed:

```sh
rtk run env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :composeApp:assembleDebug
```

Open `iosApp/MotoVault.xcodeproj` in Xcode, select the `MotoVault` scheme and an iPhone simulator, then Run.

## Design and verification

- [Approved app UI v2](design/app-ui-v2/CONTINUE-HERE.md) and [native implementation map](design/app-ui-v2/APPLY-MAP.md).
- [Current QA record](design/app-ui-v2/QA.md), [native delivery evidence](docs/CONTINUATION_PROGRESS.md) and [iOS host details](docs/IOS_HOST_IMPLEMENTATION.md).
- [Historical prototype and QA archive](docs/archive/README.md).

Build, test, simulator and physical-device results have different scopes. The linked reports record the observed results and remaining device checks.
