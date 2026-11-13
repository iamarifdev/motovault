# P14 Opt-In Rider Work Ledger Specification

## 1. Opt-In Boundary & Clutter-Free Default
- `LedgerPreferences.workModeEnabled` defaults to `false`.
- Riders who do not opt into commercial/delivery tracking never see shift-entry controls or work-accounting banners cluttering routine logs.
- Toggling `workModeEnabled = false` preserves all previously saved `WorkShiftRecord` entries (`workShifts`) untouched so no accounting history is lost when pausing work mode.

## 2. Integer Poisha & Shift Contract
- Every monetary figure (`grossEarningsPoisha`, `deductionsPoisha`, `refundsPoisha`, and allocated/actual expense summaries) uses integer **poisha** (`Long`, `0..MAX_COST_POISHA`) via shared ledger rules (`Money.kt`).
- `WorkShiftRecord` tracks `id`, `bikeId`, `date` (ISO date), `platformCode` (e.g., `delivery`, `ride`, `courier`), `grossEarningsPoisha`, `deductionsPoisha`, `refundsPoisha`, optional `workKm` / `totalDayKm`, `payoutStatus` (`SETTLED` / `PENDING`), optional `correctsShiftId`, and `notes`.

## 3. Expense Allocation & Double-Counting Guard
- Fuel, service, and renewal expenses live strictly in `RideLog` and are summarized by `CostLedger.summarize(...)` (excluding cancelled/quoted visits). `WorkShiftRecord` never stores duplicate fuel/service expenditures.
- For each date where shifts record positive `totalDayKm` and `workKm <= totalDayKm`, work allocation uses the day's distance ratio `workKm / totalDayKm` applied to actual eligible `RideLog` expenses on that date.
- When `workKm == null`, `totalDayKm == null`, `totalDayKm <= 0`, or `workKm > totalDayKm`, `WorkLedger` flags `WorkLedgerIssue.INCOMPLETE_MILEAGE` rather than guessing hidden ratios.

## 4. Maintenance Allowance Separation & Worked Example
- `LedgerPreferences.maintenanceAllowancePoisha` represents a non-cash budget reserve (`allowanceReservePoisha`), never added to cash expenses paid (`actualTotalExpensePoisha` or `allocatedExpensePoisha`).
- **Worked example**:
  - Shift 1: Gross 1,500.00 Tk (`150000` poisha), platform deduction 150.00 Tk (`15000` poisha), refund 50.00 Tk (`5000` poisha), `workKm = 80.0`, `totalDayKm = 100.0`, `SETTLED`.
  - Actual fuel log same day: 500.00 Tk (`50000` poisha).
  - Net shift earnings = `150000 - 15000 - 5000 = 130000` poisha (1,300.00 Tk).
  - Allocated work expense = `80% * 50000 = 40000` poisha (400.00 Tk); actual total expense remains `50000` poisha.
  - Net after allocated expense = `130000 - 40000 = 90000` poisha (900.00 Tk), while `allowanceReservePoisha` remains displayed independently.
- Delayed payouts (`WorkPayoutStatus.PENDING`) accumulate into `pendingPayoutPoisha` and surface `WorkLedgerIssue.PENDING_PAYOUT`. Backdated corrections reference an existing shift ID (`correctsShiftId`); missing targets flag `WorkLedgerIssue.UNLINKED_CORRECTION`.
