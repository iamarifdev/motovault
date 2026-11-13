# P12-02 Implementation Evidence — Android Widget & iOS Widget Bridge

## Scope & Architecture
- **Contract**: [P12-WIDGET-CONTRACT.md](../P12-WIDGET-CONTRACT.md)
- **Waiver**: `WD-02` ([P08-owner-decisions.md](P08-owner-decisions.md)) — verified on Android `Pixel_10` emulator and iOS `iPhone 18 Pro Max` simulator (`android_runtime` / `ios_runtime` + `review`). Physical-device reboot/locked-screen widget host pinning and Apple Team App Group WidgetKit extension signing stay waived/open until physical device & release signing.

## Shared & Persistence Changes
- `composeApp/src/commonMain/kotlin/com/motovault/app/application/widget/WidgetContract.kt`: added `WidgetTaskStatus`, `WidgetSnapshot`, `WidgetDeepLink`, and `WidgetSnapshotProjector` (redacts `bikeId`, `bikeName`, `odometerKm`, and `taskItemKey` when `privacy.enabled == true`).
- `composeApp/src/commonTest/kotlin/com/motovault/app/application/widget/WidgetContractTest.kt`: added unit tests covering selected-bike resolution, fallback on archived/missing bike, privacy redaction, staleness (`24h`), and deep-link URI round-trip/validation.
- `VaultSettings.kt`, `VaultState.kt`, `BackupPreferences.kt`, `BackupPreferencesAdapter.kt`: persisted optional `widgetBikeId: String? = null` across state JSON and backup preferences.
- Extracted pure `@Serializable` workshop records into `domain/model/WorkshopRecords.kt` and updated `WorkshopDomainTest.kt` to remove the `domain.model <-> domain.workshop` package cycle.

## Android Host Adapter
- `composeApp/src/androidMain/kotlin/com/motovault/app/ui/MotoVaultWidgetProvider.kt`: `AppWidgetProvider` rendering localized status, bike label, odometer, staleness indicator, and PendingIntent deep links (`motovault://widget/action?sheet=fuel` / `odometer`).
- `composeApp/src/androidMain/res/layout/motovault_widget.xml`, `xml/motovault_widget_info.xml`, `values/widget_strings.xml`, `values-bn/widget_strings.xml`.
- `composeApp/src/androidMain/kotlin/com/motovault/app/ui/VaultStorage.kt`, `AndroidManifest.xml`, and `MainActivity.kt` (`acceptWidgetAction` -> `WidgetNavigation.open`).

## iOS Bridge & Deep-Link Host
- `composeApp/src/iosMain/kotlin/com/motovault/app/ui/WidgetSnapshotStorage.ios.kt` and `VaultStorage.kt` write the projected snapshot to the shared container helper consumed by `iosApp/MotoVault/SharedWidgetTimeline.swift`.
- `iosApp/MotoVault/Info.plist` registers `motovault` URL scheme; `iosApp/MotoVault/AppDelegate.swift` bridges snapshots (`NativeWidgetBridge`) and routes `openURLContexts` to `WidgetNavigation.open`.

## Verification Summary
- `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:testDebugUnitTest`: `BUILD SUCCESSFUL` (exit 0).
- `xcodebuild -project iosApp/MotoVault.xcodeproj -scheme MotoVault -configuration Debug -destination 'id=50709DF0-FEA9-4B34-9DE6-6172D868875D' -derivedDataPath /tmp/motovault-ios-dd ONLY_ACTIVE_ARCH=YES build`: `** BUILD SUCCEEDED **`.
- Portable architecture, responsibility, literal, Bangla lint, and doc gates: all green (`architecture_package_cycles.py` reports 0 cycles across 18 packages / 31 edges).
