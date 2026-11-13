# P10-01 Evidence: Workshop, Repair Case, Warranty & Visit State Model

- **Task**: `P10-01`
- **Owner**: Antigravity

## Implementation
- Added [WorkshopModels.kt](composeApp/src/commonMain/kotlin/com/motovault/app/domain/workshop/WorkshopModels.kt):
  - `VisitRecordState` (`PAID`, `QUOTATION`, `APPROVED`, `CANCELLED`)
  - `IssueResolutionState` (`OPEN`, `MONITORED`, `RESOLVED`)
  - `WorkshopContact`, `RepairCase`, `PartWarranty` (with date and mileage-limit `isExpired`)
  - `migratedWorkshopContacts`, `upsertWorkshopContact`, `recordWorkshopVisit`, `transitionVisitState`
- Updated [VaultRecords.kt](composeApp/src/commonMain/kotlin/com/motovault/app/domain/model/VaultRecords.kt): added `visitState`, `workshopId`, `repairCaseId`, `warrantyIds`, `isPaidService` on `RideLog`, and `repairCases`/`warranties` on `GarageBikeRecord`.
- Updated [VaultState.kt](composeApp/src/commonMain/kotlin/com/motovault/app/domain/vault/VaultState.kt) and [VaultSettings.kt](composeApp/src/commonMain/kotlin/com/motovault/app/domain/vault/VaultSettings.kt) (`confirmedRules()` now excludes non-paid service visits).
- Updated [BikeOwnership.kt](composeApp/src/commonMain/kotlin/com/motovault/app/domain/garage/BikeOwnership.kt): keeps `repairCases` and `warranties` per-bike while preserving owner-shared `workshopContacts`.

## Verification
- Added [WorkshopDomainTest.kt](composeApp/src/commonTest/kotlin/com/motovault/app/domain/workshop/WorkshopDomainTest.kt).
- `./gradlew :composeApp:iosSimulatorArm64Test`: `BUILD SUCCESSFUL` (exit 0).
- `python3 tools/responsibility_gate.py`: `Responsibility baseline matches (2 files, 7 functions).` (exit 0).
