# P10-04 Evidence: Workshop & Recurring-Repair Runtime Verification

- **Task**: `P10-04`
- **Owner**: Antigravity

## Verification Scope & Environment
- **Unit & Domain Verification (`./gradlew :composeApp:iosSimulatorArm64Test`)**: `BUILD SUCCESSFUL` (all tests passed, including [WorkshopDomainTest.kt](composeApp/src/commonTest/kotlin/com/motovault/app/domain/workshop/WorkshopDomainTest.kt) covering repeated repairs, quotation -> approved -> paid transitions, CostLedger exclusion of non-paid visits, warranty date/mileage expiry, warranty reminder scheduling & archived-bike suppression, and `.mvbk` round-trip).
- **Android Runtime (`Pixel_10` / `emulator-5554`)**:
  - `./gradlew :composeApp:installDebug`: `BUILD SUCCESSFUL`
  - Launched `com.motovault.app/.MainActivity --es sheet service` with zero fatal logcat errors (`NO_FATAL_ERRORS`).
  - Verified collapsed quick-service sheet preserves fast 1-tap entry (`p10_android_service.png`) and expanded secondary toggle renders Paid/Quotation chips, workshop phone, recurring repair case title, and 0/6/12-month warranty chips in Bangla (`p10_android_service_more.png`).
- **iOS Runtime (`iPhone 18 Pro Max` / `50709DF0-FEA9-4B34-9DE6-6172D868875D`, iOS 27.0)**:
  - `xcodebuild -project iosApp/MotoVault.xcodeproj -scheme MotoVault -configuration Debug`: `** BUILD SUCCEEDED **`
  - `xcrun simctl install` + `xcrun simctl launch ... com.motovault.app`: launched PID `36280` cleanly (`p10_ios_service.png`).
- **Portable Gates**:
  - `python3 tools/responsibility_gate.py`: `Responsibility baseline matches (2 files, 7 functions).`
  - `python3 tools/check_agent_docs.py`: `agent docs consistent`
  - `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: `Ran 61 tests` `OK`
  - `cd design/app-ui-v3 && npm run build`: exited 0.
