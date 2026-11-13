# P09-03: Bike Switcher, Add Bike & Archive UI (Prototype v3 + Compose)

- Task: `P09-03` ("Build bike switcher, add and archive in v3")
- Status: Complete (`implementation`, `design`, `verification`)

## 1. Design Prototype (`design/app-ui-v3`)

- Updated [types.ts](../../../design/app-ui-v3/src/types.ts) with optional `Ride.id`, `Ride.archived`, and `GarageBike[]` (`garageBikes`) in `AppData`.
- Updated [store.ts](../../../design/app-ui-v3/src/store.ts):
  - `completeOnboarding`: appends new bikes to the garage without overwriting prior bike logs, rules, or bike-scoped documents while keeping owner-scoped `licence` documents shared.
  - Added `switchBike`, `toggleArchiveBike` (enforcing >= 1 active bike), and `addingBike` UI state.
- Created [BikeSwitcherSheet.tsx](../../../design/app-ui-v3/src/sheets/BikeSwitcherSheet.tsx) and wired switcher triggers in `Home.tsx`, `Me.tsx`, and `App.tsx`.
- Verified `cd design/app-ui-v3 && npm run build` (`tsc --noEmit && vite build`) exited 0.

## 2. Localized Resources (EN + BN)

- Added 14 string keys (`garage_switch_bike`, `garage_switcher_title`, `garage_active_bikes`, `garage_archived_bikes`, `garage_active_badge`, `garage_archived_badge`, `garage_add_bike`, `garage_archive_bike`, `garage_restore_bike`, `garage_use_bike`, `garage_archive_last_error`, `garage_bike_switched`, `garage_bike_archived`, `garage_bike_restored`) to both `values/strings.xml` and `values-bn/strings.xml`.
- Verified by `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: 61 tests OK.

## 3. Compose Multi-Bike Switcher & Onboarding Preservation

- Created [BikeSwitcherSheet.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/garage/BikeSwitcherSheet.kt) (`<= 240` lines; functions `<= 80` lines) showing active and archived bikes with brand accents, localized year/odometer formatting, active badge, archive guard on the last active bike, restore button, and `"Add another bike"` CTA.
- Added `VaultState.completeBikeOnboarding` in [BikeOwnership.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/domain/garage/BikeOwnership.kt) so onboarding preserves prior garage bikes rather than replacing history.
- Wired `"Switch bike"` chip in [GarageSections.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/garage/GarageSections.kt) / [GarageScreen.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/garage/GarageScreen.kt), profile switcher in [MeScreen.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/settings/MeScreen.kt), and `"bikeSwitcher"` in [VaultSheetHost.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultSheetHost.kt) + [MotoVault.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/MotoVault.kt).

## 4. Verification

- `python3 tools/responsibility_gate.py`: passed (`Responsibility baseline matches (2 files, 7 functions)`).
- `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: 61 tests OK.
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug`: `BUILD SUCCESSFUL`.
