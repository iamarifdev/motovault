# P12-04 Verification Evidence — Widget Lifecycle, Privacy & Quick Actions

## Scope & Waiver
- Executed under `WD-02` ([P08-owner-decisions.md](P08-owner-decisions.md)): physical-device home-screen widget pinning, OS reboot/locked-screen battery testing, and Apple Team App Group WidgetKit extension signing are waived in favor of `android_runtime`, `ios_runtime`, and `review`.

## Automated Tests & Portable Gates
- `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:testDebugUnitTest`: `BUILD SUCCESSFUL` (exit 0), covering `WidgetContractTest` (bike selection, archived/missing fallback, privacy lock redaction, 24h staleness, and `motovault://widget/action` URI parsing) and `WorkshopDomainTest`.
- Portable gates passed on final P12 changes:
  - `python3 tools/responsibility_gate.py` -> baseline match
  - `python3 tools/architecture_dependencies.py` -> baseline match (0 new violations)
  - `python3 tools/architecture_package_cycles.py` -> 0 cycles across 18 packages / 31 edges
  - `python3 tools/bangla_copy_lint.py` -> baseline match
  - `python3 tools/ui_literal_gate.py` -> baseline match
  - `python3 tools/check_agent_docs.py` -> `agent docs consistent`
  - `python3 -m unittest discover -s tools/tests -p 'test_*.py'` -> 61 tests OK
- `cd design/app-ui-v3 && npm run build`: exited 0.

## Runtime Verification
- **Android (`Pixel_10`, `emulator-5554`)**:
  - `./gradlew :composeApp:installDebug` succeeded.
  - Cold deep-link `adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d "motovault://widget/action?sheet=fuel" com.motovault.app/.MainActivity` hydrated Room storage and opened the localized Bangla Fuel sheet (`ফুয়েল` / `কত টাকার তেল?`) with `NO_FATAL_ERRORS` in `adb logcat -d -b crash`.
- **iOS (`iPhone 18 Pro Max`, `50709DF0-FEA9-4B34-9DE6-6172D868875D`)**:
  - `xcodebuild -project iosApp/MotoVault.xcodeproj -scheme MotoVault -configuration Debug -destination 'id=50709DF0-FEA9-4B34-9DE6-6172D868875D' -derivedDataPath /tmp/motovault-ios-dd ONLY_ACTIVE_ARCH=YES build` -> `** BUILD SUCCEEDED **`.
  - `xcrun simctl openurl 50709DF0-FEA9-4B34-9DE6-6172D868875D "motovault://widget/action?sheet=odometer"` triggered the iOS SpringBoard URL-scheme prompt (`Open in "MotoVault"?`), confirming `CFBundleURLSchemes: motovault` registration.
  - `xcrun simctl launch ... --sheet odometer` rendered the localized Odometer sheet (`ওডোমিটার` / `১২,৩৪০ কিমি`) and `--tab me --scroll-bottom` rendered the Me tab cleanly.

## Localization Review Status
- All widget and settings strings are stored in EN/BN XML resources (`widget_strings.xml` + `strings.xml`); agent-drafted modern Bangla copy passed automated lint and awaits named human sign-off at release.
