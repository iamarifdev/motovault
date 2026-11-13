# P11-03 Evidence: Selective Handover Pack UI, Preview & BRTA Checklist

- **Task**: `P11-03`
- **Owner**: Antigravity

## Prototype (`design/app-ui-v3`)
- Updated [Me.tsx](design/app-ui-v3/src/screens/Me.tsx) with a Service Passport & Handover Pack section showing default redaction privacy rules and the offline BRTA transfer checklist.
- `cd design/app-ui-v3 && npm run build`: exited 0.

## Implementation
- Added 10 EN + BN resource strings (`passport_select_bike`, `passport_privacy_title`, `passport_include_reg`, `passport_include_vin`, `passport_include_costs`, `passport_include_workshops`, `passport_include_notes`, `passport_redacted_notice`, `passport_unknown_baselines`, `passport_brta_checklist`) to [strings.xml](composeApp/src/commonMain/composeResources/values/strings.xml) and [values-bn/strings.xml](composeApp/src/commonMain/composeResources/values-bn/strings.xml).
- Updated [PassportExportUi.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/PassportExportUi.kt): extracted `PassportHandoverControls`, added active-bike selector when >1 bike exists, defaulted registration and chassis/engine chips OFF, added unknown-baseline count disclosure and BRTA transfer checklist, and surfaced redacted-field / unknown-baseline notices in `PassportVerifier`.

## Verification
- `./gradlew :composeApp:iosSimulatorArm64Test`: `BUILD SUCCESSFUL` (exit 0).
- `python3 tools/responsibility_gate.py`: `Responsibility baseline matches (2 files, 7 functions).` (exit 0).
- `python3 tools/check_agent_docs.py`: `agent docs consistent` (exit 0).
- `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: `Ran 61 tests` `OK` (exit 0).
