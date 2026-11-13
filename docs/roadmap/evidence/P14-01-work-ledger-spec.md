# P14-01 Evidence — Rider Work Ledger Boundaries

## Artifacts Reviewed & Produced
- Specification: [P14-WORK-LEDGER-SPEC.md](../P14-WORK-LEDGER-SPEC.md)
- Existing monetary/cost ledger contract checked: `Money.kt`, `LedgerPreferences.kt`, and `CostLedger.kt`.

## Key Decisions
1. Reuse existing `LedgerPreferences` inside `VaultState` / `VaultSettings` / `BackupPreferences` with `workModeEnabled = false` by default and persistent `workShifts`.
2. Prevent double counting: keep fuel and service expenses solely in `RideLog`, allocate work expenses proportionally by `workKm / totalDayKm`, and flag missing/inconsistent mileage explicitly (`INCOMPLETE_MILEAGE`).
3. Keep `maintenanceAllowancePoisha` separated as `allowanceReservePoisha` rather than cash spent, and represent pending payouts (`PENDING_PAYOUT`) and backdated shift corrections (`correctsShiftId`) explicitly.
