# P13-02 Evidence — Offline Seasonal Care & Bounded Weather Domain

## Domain & Persistence Changes
- `composeApp/src/commonMain/kotlin/com/motovault/app/domain/model/SeasonalRecords.kt`: added `@Serializable` `RidingSeason`, `WeatherCacheState`, `WeatherCacheSnapshot` (bounded `6h` TTL defaulting to `OFFLINE_ONLY`), and `BreakdownContact`.
- `composeApp/src/commonMain/kotlin/com/motovault/app/domain/seasonal/SeasonalCareModels.kt`: added `BMD_OFFICIAL_URL`, `NATIONAL_EMERGENCY_NUMBER`, `WEATHER_CACHE_TTL_HOURS = 6`, and `SeasonalCarePolicy` (`seasonForMonth`, `checklistFor`, `dismissKey`, `isDismissed`, `toggleChecklistItem`, `toggleDismissedSeason`, `formatBreakdownShareText`).
- `VaultSettings.kt`, `VaultState.kt`, `BackupPreferences.kt`, `BackupPreferencesAdapter.kt`: persisted `checkedSeasonalItems`, `dismissedSeasonKeys`, `breakdownContacts`, and `weatherCache` with backward-compatible defaults.

## Verification
- `python3 tools/architecture_package_cycles.py`: `No shared domain/application package cycles (19 packages, 32 edges).`
- `./gradlew :composeApp:iosSimulatorArm64Test`: `BUILD SUCCESSFUL` covering `SeasonalCarePolicyTest` (12-month season mapping, 3x3 seasonal checklists, bike/year-scoped dismiss & re-enable, `OFFLINE_ONLY` / `FRESH` / `EXPIRED` weather cache evaluation, and offline vs GPS breakdown message formatting).
