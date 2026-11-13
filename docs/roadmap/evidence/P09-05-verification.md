# P09-05: Multi-Bike Cross-Bike Isolation, Migration & Restore Verification

- Task: `P09-05` ("Verify cross-bike isolation, migration and restore")
- Status: Complete (`verification`, `design`, `android_runtime`, `ios_runtime`, `review`)

## 1. Portable & Workstation Gates

- `python3 tools/roadmap.py check`: passed (`Roadmap valid: 19 phases, 93 tasks; generated documents current.`).
- `python3 tools/check_agent_docs.py`: passed (`agent docs consistent`).
- `python3 tools/responsibility_gate.py`: passed (`Responsibility baseline matches (2 files, 7 functions)`).
- `python3 -m unittest discover -s tools/tests -p 'test_*.py'`: passed (61 tests, OK).
- `cd design/app-ui-v3 && npm run build`: passed (`tsc --noEmit && vite build`).

## 2. Unit, Room v6 Migration & Backup/Restore Tests

- `./gradlew :skyDesignSystem:compileKotlinIosSimulatorArm64 :composeApp:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug :composeApp:iosSimulatorArm64Test`: `BUILD SUCCESSFUL`.
- `composeApp/build/test-results/iosSimulatorArm64Test/*.xml`: 57 test files, 328 tests, 0 failed, 0 skipped, including:
  - `BikeOwnershipTest` (duplicate models, switch isolation, owner-scoped licence sharing, archive/restore guard)
  - `VaultDatabaseTest.multiBikeStateAndGarageAssetReferencesPersistAcrossReopenAndSwitch` (Room v6 persistence across reopen/switch and inactive bike asset preservation)
  - `BackupOperationsAdapterTest.multiBikeBackupIncludesNonActiveGarageAssetsAndArchivedBikes` (full `.mvbk` multi-bike backup/restore round-trip)
  - `ScopedMutationsTest` (cross-bike fuel create/edit after switch, archived target rejection for fuel & `CapturePrefill`, reminder exclusion on archived bike while keeping owner-scoped licence reminders).

## 3. Android Emulator Runtime (`Pixel_10`, `emulator-5554`)

- Installed `:composeApp:installDebug` onto `Pixel_10` without clearing existing owner data.
- Verified `com.motovault.app/.MainActivity` preserved the existing single-bike KTM Duke 125 record (`p09_android_home.png`) and displayed `"বাইক বদলান"` in the Home header alongside `BikeSwitcherSheet` (`"আপনার গ্যারেজ"`, `"সক্রিয় বাইক"`, `"সক্রিয়"`, `"আর্কাইভ করুন"`, `"আরেকটি বাইক যোগ করুন"` — `p09_android_bike_switcher.png`).
- `adb logcat -d -s AndroidRuntime:E FATAL:E System.err:W` showed zero fatal crashes.

## 4. iOS 27 Simulator Runtime (`iPhone 18 Pro Max`, `50709DF0-FEA9-4B34-9DE6-6172D868875D`)

- Built `iosApp/MotoVault.xcodeproj` (`scheme MotoVault`, `Debug`, `CODE_SIGNING_ALLOWED=NO`): `** BUILD SUCCEEDED **`.
- Installed and launched `com.motovault.app` (`simctl install` + `simctl launch`, PID `30610`) without clearing existing simulator records; verified preserved Yamaha R15 V4 record (`12,340 km`, existing rules & chart) and `"বাইক বদলান"` chip on Home (`p09_ios_home.png`).
