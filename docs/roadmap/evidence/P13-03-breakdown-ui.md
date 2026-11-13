# P13-03 Evidence — Seasonal Care & Breakdown Help in v3

## Prototype & Shared UI Implementation
- Updated v3 prototype (`design/app-ui-v3/src/store.ts`, `design/app-ui-v3/src/screens/Home.tsx`) with a Seasonal Care card below the Garage stats section opening the `'breakdown'` sheet; `cd design/app-ui-v3 && npm run build` exited 0 (`dist/` built).
- Added localized EN/BN resource strings in `composeApp/src/commonMain/composeResources/values/strings.xml` and `values-bn/strings.xml`.
- Created `composeApp/src/commonMain/kotlin/com/motovault/app/ui/garage/SeasonalCareSection.kt` rendered below `GarageStatsSection` in `GarageScreen.kt` (preserves Next Action hero & Quick Actions priority, displays Bangladesh BMD region, cache status, official BMD link chip, 3-item offline checklist, dismiss/re-enable per season+bike+year, and breakdown help CTA).
- Created `composeApp/src/commonMain/kotlin/com/motovault/app/ui/garage/BreakdownHelpSheet.kt` wired into `VaultSheetHost.kt` under `"breakdown"`, surfacing:
  - Boundary disclosure and explicit `tel:999` emergency dial chip.
  - Offline saved contacts merging `state.breakdownContacts` and `state.workshopContacts`, inline add-contact form, and deliberate `tel:` call actions.
  - Optional one-shot GPS capture via existing `LocationCaptureButton`, accuracy/time badge, editable issue note, and copy-to-clipboard message share that continues working offline without GPS.

## Automated & Gate Results
- Portable gates (`responsibility_gate.py`, `architecture_dependencies.py`, `architecture_package_cycles.py`, `bangla_copy_lint.py`, `ui_literal_gate.py`, `check_agent_docs.py`, `tools/tests` 61 tests): all green.
- `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:testDebugUnitTest`: `BUILD SUCCESSFUL`.
- Agent-drafted modern Bangla strings passed `bangla_copy_lint.py` and await named human sign-off at release.
