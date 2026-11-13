# P14-02 Evidence — Opt-In Work Ledger & Reconciled Summaries

## Domain & Persistence Changes
- `composeApp/src/commonMain/kotlin/com/motovault/app/domain/ledger/WorkLedger.kt`: implemented `WorkPayoutStatus`, `WorkLedgerIssue`, `WorkShiftRecord` (integer poisha), `WorkReconciledSummary`, and `WorkLedger.reconcile(...)` allocating recorded fuel/service expenses strictly from `RideLog` by `workKm / totalDayKm` ratio without double counting or converting `maintenanceAllowancePoisha` into cash paid.
- `composeApp/src/commonMain/kotlin/com/motovault/app/domain/ledger/LedgerPreferences.kt`: added `workModeEnabled: Boolean = false` and `workShifts: List<WorkShiftRecord> = emptyList()` with poisha and ISO date validation, automatically preserved across Room and `.mva` backup archives.

## Verification
- `python3 tools/architecture_package_cycles.py`: 0 package cycles across 19 packages / 32 edges.
- `./gradlew :composeApp:iosSimulatorArm64Test`: `BUILD SUCCESSFUL`, passing `WorkLedgerTest` (mixed-use expense allocation, quote exclusion, allowance separation, pending payouts, missing mileage, unlinked corrections, and `LedgerPreferences` serialization round-trip).
