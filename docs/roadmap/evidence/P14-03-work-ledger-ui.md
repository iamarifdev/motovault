# P14-03 Verification Evidence — Work Ledger UI & v3 Prototype Parity

## Implementation
- **Shared Kotlin UI**: Created [WorkLedgerSection.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/history/WorkLedgerSection.kt) and integrated it into [HistoryScreen.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/history/HistoryScreen.kt) and [VaultSheetHost.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultSheetHost.kt) (`"work-ledger"` modal sheet route).
- **Prototype Parity (`design/app-ui-v3`)**: Added `'work-ledger'` sheet routing in `design/app-ui-v3/src/store.ts` and work ledger opt-in summary entry in `design/app-ui-v3/src/screens/History.tsx`.
- **Localization**: Added EN + BN `work_ledger_*` string resources in `values/strings.xml` and `values-bn/strings.xml` with no inline display literals.

## Verification
- Opt-in toggle (`workModeEnabled = false` default) hides routine shift entry clutter while preserving stored `workShifts` records in `LedgerPreferences`.
- `design/app-ui-v3` build (`npm run build`): `vite v6.0.7`, `1972 modules transformed`, exit 0.
- Portable gates (`responsibility_gate.py`, `architecture_dependencies.py`, `architecture_package_cycles.py`, `bangla_copy_lint.py`, `ui_literal_gate.py`, `check_agent_docs.py`, `tools/tests` 61 tests): exit 0.
