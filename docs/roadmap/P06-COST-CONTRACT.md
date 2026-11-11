# P06 recorded costs and fuel confidence

Owner: Codex. Applies to the single vault writer and current local backup format.

## Money and records

- BDT amounts use integer poisha (`Long`). One taka is 100 poisha. Legacy
  nonnegative finite `Double` amounts convert once at the calculation boundary,
  half up to the nearest poisha. New input accepts at most two decimal places.
- Keep legacy `cost` readable. An optional `costPoisha` is authoritative for new
  expenses/fuel; existing `serviceTotalPoisha` is authoritative for service.
- Categories are stable codes: fuel, service, renewal, parking, toll, custom.
  Renewal metadata is supporting history, never an additional expense. Only its
  linked `document_fee` log contributes. Duplicate record/renewal IDs make a
  summary unavailable instead of producing a misleading total.
- No negative expenses/refunds in this phase. Correct the original amount or
  delete it, with durable Undo. A custom name is owner text, not a category ID.
  Fuel/service/renewal retain their existing entry flows; the generic expense
  sheet creates parking/toll/custom records and edits those records only.
- A monthly budget and optional maintenance allowance are owner-entered plans,
  never transactions. No default budget or official price is implied.

## Windows, distance and confidence

- Dates are local calendar dates. Last 30 days includes today and the preceding
  29 days. This month begins on day 1. All recorded includes no future records.
  Backdated edits immediately move a record into its proper period.
- Category selection filters both the visible timeline and recorded total.
  The budget always compares all categories in the current calendar month.
- Recorded distance uses fuel/service readings only, never copied mileage on
  fees or general expenses. At least two distinct increasing readings are
  required. Show the actual first/last dates and kilometres. Cost per km uses
  expenses strictly after the first reading's date through the last reading's
  date, so starting-day costs with uncertain order are excluded. Missing or
  contradictory mileage suppresses the ratio. This is partial recorded coverage,
  never lifetime ownership cost or an extrapolated monthly distance.
- Economy is distance divided by litres consumed between full tanks. Exclude
  the opening fill's litres; include every intervening partial fill and closing
  full fill. A partial tail stays open. A missed fill breaks that cycle and the
  next full fill becomes a new baseline. Invalid/missing litres or non-increasing
  mileage also break a cycle. Completed cycles may coexist with an open tail;
  name both states in the UI and recalculate after every edit/delete.
- Remember a price only after explicit owner confirmation, with its effective
  date. Legacy defaults are unconfirmed suggestions. Editing an old fill must
  not replace a newer remembered price. Decimal and supported input digits use
  the existing shared number parser. There is no online or official price feed.

## Reviewed examples

| Fixture | Expected |
| --- | --- |
| Legacy ৳1.005, ৳2.004 | 101 and 200 poisha, total 301 |
| Full at 1000, partial 2 L at 1060, full 3 L at 1150 | 150 / 5 = 30 km/L; opening litres excluded |
| Same records, missed-fill flag at 1060 | No completed cycle; 1150 is the new baseline |
| ৳1250 renewal event and its fee log | ৳1250 once |
| Parking on Nov 1 corrected to Oct 31 | Leaves November budget spending |
| Fees/custom records with copied 5000 km only | Money total available, distance/ratio unavailable |
| Duplicate IDs or repeated renewal ID | Summary unavailable; records preserved |
| Delete expense then Undo | Original poisha, date and ID restored in History and backup |

## Change plan and validation

Domain `domain/ledger` owns conversion, category projection, date windows,
coverage, budget and expense rules. `domain/usecase/FuelEfficiencyUseCase.kt`
owns full-to-full confidence. The vault coordinator remains the sole writer.
`RideLog`, `VaultState`, `VaultSettings` and backup counterparts gain optional
fields with defaults; the existing Room JSON columns need no schema change.
Backup adapters map every new field. Old snapshots retain their bytes/values;
new fields survive current archive round-trip and writer Undo.

Extend v3 History and expense/budget/fuel states first, then port through shared
UI components in `ui/history` and `ui/fuel`. Add English/Bangla cost resources;
keep category codes out of display. Existing theme, digits, text scaling, sheets,
keypad and date controls remain the presentation boundary. New production files
stay below 400 lines; no baseline exception or storage-to-UI dependency is added.

One final verification batch covers domain/compatibility regressions, portable
gates, Android build/tests and iOS compilation. Native probes on the available
Android emulator and preserved iOS 27 iPhone 18 Pro Max cover persisted records,
edit/delete/Undo, backup mapping, both languages/digit styles and visible cost
states. Record screenshots separately from probe assertions. No physical-device
or release claim follows from these checks. Re-run only a failed or affected
check after a corrective change.
