# P10-03 Evidence: Cost Ledger Exclusion, Warranty Reminders & Backup Persistence

- **Task**: `P10-03`
- **Owner**: Antigravity

## Implementation
- Updated [CostLedger.kt](composeApp/src/commonMain/kotlin/com/motovault/app/domain/ledger/CostLedger.kt): `CostLedger.summarize` excludes non-paid service visits (`visitState.isNotBlank() && visitState != "paid"`) from totals, category sums, duplicate checks, and distance coverage.
- Updated [ReminderPlan.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/ReminderPlan.kt): `reminderPlan` emits warranty expiration reminders (`targetType = "warranty"`) for non-expired active warranties when service reminders are enabled on non-archived bikes; `reminderTarget` and `deliveredReminderIsCurrent` resolve `"warranty"` targets and suppress archived bikes.
- Updated [BackupPreferences.kt](composeApp/src/commonMain/kotlin/com/motovault/app/domain/backup/BackupPreferences.kt) and [BackupPreferencesAdapter.kt](composeApp/src/commonMain/kotlin/com/motovault/app/storage/BackupPreferencesAdapter.kt): persisted `workshopContacts`, `repairCases`, and `warranties` with empty-list defaults for backwards compatibility.

## Verification
- Extended [WorkshopDomainTest.kt](composeApp/src/commonTest/kotlin/com/motovault/app/domain/workshop/WorkshopDomainTest.kt) with `costLedgerExcludesNonPaidVisitsFromTotalsAndCoverage`, `warrantyRemindersScheduleAndSuppressWhenBikeArchived`, and `backupPreferencesRoundTripPreservesWorkshopContactsRepairCasesAndWarranties`.
- `./gradlew :composeApp:iosSimulatorArm64Test`: `BUILD SUCCESSFUL` (exit 0).
- `python3 tools/responsibility_gate.py`: `Responsibility baseline matches (2 files, 7 functions).` (exit 0).
