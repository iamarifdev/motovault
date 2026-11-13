# P13-04 Verification Evidence — Seasonal Care, Offline Help & Privacy

## Scope & Waiver
- Executed under `WD-02` ([P08-owner-decisions.md](P08-owner-decisions.md)): offline contact persistence, seasonal relevance, bounded BMD weather state (`OFFLINE_ONLY` default, `6h` TTL), deliberate GPS boundary, and Android/iOS simulator UI verification (`android_runtime`, `ios_runtime`, `review`).

## Care Relevance & Priority Check
- `SeasonalCareSection` is placed below `GarageHeroCard`, Quick Actions row, Nudge banner, and `GarageStatsSection` in `GarageScreen.kt`, guaranteeing overdue/upcoming bike maintenance tasks are never displaced.
- Verified in `SeasonalCarePolicyTest`: 12-month Bangladesh season mapping (`MONSOON`, `WINTER_DUST`, `HEAT`), per-bike/year season dismissal and re-enable, `OFFLINE_ONLY` default weather cache with `6h` expiration (`WEATHER_CACHE_TTL_HOURS`), and offline breakdown message generation with or without optional GPS coordinates.
- Privacy & offline safety: no background location polling, no automatic network requests, explicit one-shot `LocationCaptureButton` trigger inside `BreakdownHelpSheet`, and full offline contact + message availability even when GPS is denied or absent.

## Runtime & Suite Results
- `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:testDebugUnitTest`: `BUILD SUCCESSFUL` (exit 0).
- Portable gates (`responsibility_gate.py`, `architecture_dependencies.py`, `architecture_package_cycles.py`, `bangla_copy_lint.py`, `ui_literal_gate.py`, `check_agent_docs.py`, `tools/tests` 61 tests): all green.
- **Android (`Pixel_10`, `emulator-5554`)**: `./gradlew :composeApp:installDebug` OK; cold `--es sheet breakdown` launch opened localized Bangla `ব্রেকডাউন সহায়তা` sheet (`p13_android_breakdown.png`) showing the 999 boundary warning, offline contacts form, optional GPS status, and share CTA with `ANDROID_NO_FATAL_ERRORS`.
- **iOS (`iPhone 18 Pro Max`, `50709DF0-FEA9-4B34-9DE6-6172D868875D`)**: `xcodebuild ... build` `** BUILD SUCCEEDED **`; `--sheet breakdown` launch rendered `ব্রেকডাউন সহায়তা` sheet cleanly in Yamaha palette (`p13_ios_breakdown.png`).
