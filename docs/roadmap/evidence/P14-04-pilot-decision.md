# P14-04 Verification & Enablement Decision — Rider Work Ledger

## Scope & Waiver
- Executed under `WD-02` ([P08-owner-decisions.md](P08-owner-decisions.md)): automated shared tests and Android/iOS simulator runtime checks replace live external rider pilot sessions.

## Verification & Reconciliation
- `WorkLedgerTest` in `:composeApp:iosSimulatorArm64Test` and `:composeApp:testDebugUnitTest` verified independent reconciliation (`WorkLedger.reconcile`) across no-work, mixed-work, and incomplete-shift inputs using typed `Money` values (`WorkReconciledSummary`).
- **Android (`Pixel_10`, `emulator-5554`)**: Rendered `রাইডার ওয়ার্ক লেজার` modal sheet (`p14_android_work_ledger.png`) with toggle off and reconciled zeroed net summary (`বণ্টনকৃত খরচের পর নিট: ৳0`); logcat crash buffer reported `ANDROID_NO_FATAL_ERRORS`.
- **iOS (`iPhone 18 Pro Max`, `50709DF0-FEA9-4B34-9DE6-6172D868875D`)**: `xcodebuild ... build` succeeded (`** BUILD SUCCEEDED **`) and `--sheet work-ledger` launch rendered `ওয়ার্ক মোড চালু করুন` and `সমন্বিত হিসাব (বর্তমান বাইক)` (`p14_ios_work_ledger.png`).

## Enablement Decision
- Keep `workModeEnabled = false` (opt-in disabled by default). Core personal bike ledger and release remain completely independent of rider work-mode adoption; external rider feedback stays deferred under `WD-02`.
