# P10-02 Evidence: Workshop Contacts, Repair Cases & Warranty UI

- **Task**: `P10-02`
- **Owner**: Antigravity

## Prototype (`design/app-ui-v3`)
- Extended [types.ts](design/app-ui-v3/src/types.ts), [store.ts](design/app-ui-v3/src/store.ts), [ServiceSheet.tsx](design/app-ui-v3/src/sheets/ServiceSheet.tsx), and [History.tsx](design/app-ui-v3/src/screens/History.tsx) with visit state (`paid` / `quote`), workshop phone, recurring repair case title, and warranty duration (`0`, `6`, `12` months) controls inside the secondary sheet toggle and history badges.
- `cd design/app-ui-v3 && npm run build`: exited 0.

## Implementation
- Added EN and BN string resources (12 keys: `service_visit_paid`, `service_visit_quote`, `service_visit_approved`, `service_visit_cancelled`, `service_workshop_phone`, `service_repair_case_placeholder`, `service_warranty_none`, `service_warranty_6m`, `service_warranty_12m`, `service_warranty_badge`, `service_quote_approve_paid`, `service_quote_cancel`) to [strings.xml](composeApp/src/commonMain/composeResources/values/strings.xml) and [values-bn/strings.xml](composeApp/src/commonMain/composeResources/values-bn/strings.xml).
- Created [ServiceRepairSection.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/maintenance/ServiceRepairSection.kt) (`ServiceRepairWarrantyControls`, `HistoryServiceMetaSection`) and wired into [ServiceSheet.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/maintenance/ServiceSheet.kt), [HistoryScreen.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/history/HistoryScreen.kt), and [VaultTabContent.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultTabContent.kt).

## Verification
- `./gradlew :composeApp:compileKotlinIosSimulatorArm64`: `BUILD SUCCESSFUL` (exit 0).
- `python3 tools/responsibility_gate.py`: `Responsibility baseline matches (2 files, 7 functions).` (exit 0).
- `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: `Ran 61 tests` `OK` (exit 0).
