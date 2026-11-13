# P16-01 Verification Evidence — Expanded Regression Matrix

## Scope & Waiver
- Executed under `WD-02` ([P08-owner-decisions.md](P08-owner-decisions.md)): physical device checks mapped to Android (`Pixel_10`) and iOS (`iPhone 18 Pro Max`) simulator runtime verification; Bangla copy uses automated lint with human release sign-off deferred.

## Automated Gate & Test Matrix
- `python3 tools/responsibility_gate.py`: passed (`Responsibility baseline matches (2 files, 7 functions)`).
- `python3 tools/architecture_dependencies.py`: passed (`0 existing violations`).
- `python3 tools/architecture_package_cycles.py`: passed (`0 cycles`, 20 packages, 33 edges).
- `python3 tools/bangla_copy_lint.py`: passed (`15 regression phrases` baseline matched).
- `python3 tools/ui_literal_gate.py`: passed (`9 existing occurrences` baseline matched).
- `python3 tools/check_agent_docs.py`: passed (`agent docs consistent`).
- `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: passed (`Ran 61 tests`, `OK`).
- `design/app-ui-v3` (`npm run build`): passed (`121 custom properties`, `1972 modules transformed`, `vite build` exit 0).
- `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:testDebugUnitTest :composeApp:assembleDebug`: `BUILD SUCCESSFUL`:
  - `iosSimulatorArm64Test`: 62 suites, 349 tests, 0 failures, 0 errors, 0 skipped.
  - `testDebugUnitTest`: 64 suites, 355 tests, 0 failures, 0 errors, 0 skipped.

## Simulator Runtime Checks
- **Android (`Pixel_10`, `emulator-5554`)**: `./gradlew :composeApp:installDebug` + live launch verified settled Bangla Home (`আমার গ্যারেজ`, `Duke 125 • 2023`, `KTM • ২০২৩`, `মৌসুমি যত্ন ও জরুরি সহায়তা`; `p16_android_home.png`), `p14_android_work_ledger.png`, `p15_android_cloud_backup.png`; crash logcat clean (`ANDROID_NO_FATAL_ERRORS`).
- **iOS (`iPhone 18 Pro Max`, `50709DF0-FEA9-4B34-9DE6-6172D868875D`)**: `xcodebuild ... build` succeeded (`** BUILD SUCCEEDED **`); `--tab garage`, `--sheet work-ledger`, and `--sheet cloud-backup` launches rendered R15 V4 garage home (`১২,৩৪০ কিমি`), work ledger, and cloud backup status sheets (`p16_ios_home.png`, `p14_ios_work_ledger.png`, `p15_ios_cloud_backup.png`).
