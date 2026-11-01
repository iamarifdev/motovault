# P04A-06 responsibility ratchet and native probe split

Status: complete on the uncommitted P04A working tree.

## Ratchet

- `tools/responsibility_gate.py` scans handwritten Kotlin under `composeApp/src`
  for files above 400 lines and brace-delimited functions above 80 lines.
- `tools/responsibility-baseline.json` records 11 files and 7 functions with an
  owner, removal task and justification. Files above 600 lines require a
  recorded exception; new occurrences and growth fail.
- The check is lexical and does not measure expression-body functions or judge
  whether an extraction has one coherent responsibility. Review remains manual.
- Fixtures cover new-file rejection, growth, the 600-line exception, function
  growth, invalid baselines and missing/empty roots.
- The GitHub workflow runs the gate and its fixtures.

## Native probe split

`NativeAssetInstrumentation.kt` was split by fixture responsibility:

- `NativeAssetInstrumentation.kt` — runner and standard suite (568 lines,
  test-only baseline);
- `ProbeUiHelpers.kt` — accessibility lookup, clicks, text entry and screenshots;
- `ProbeFuelChecks.kt` — fuel edit/failure/history scenarios;
- `ProbeMaintenanceChecks.kt` — P04 profile/onboarding/detail scenarios;
- `ProbeRecoveryChecks.kt` — recovery transaction and UI scenarios;
- `ProbeBackupChecks.kt` — backup create/localization/round-trip scenarios.

## Verification

- `rtk run python3 tools/responsibility_gate.py` → baseline matches.
- `rtk run python3 -m unittest discover -s tools/tests -p test_responsibility_gate.py`
  → 5 tests pass.
- `rtk run ./gradlew :composeApp:compilePrivacyProbeAndroidTestKotlinAndroid --offline`
  passes.
