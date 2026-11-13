# P12 Widget Snapshot & Deep-Link Contract

## Scope & Authoritative Layer

- Application contract lives in `composeApp/src/commonMain/kotlin/com/motovault/app/application/widget/WidgetContract.kt`.
- Android (`AppWidgetProvider` / `RemoteViews`) and iOS (`WidgetKit` timeline / App Group container) adapters are read-only consumers of `WidgetSnapshot`. Neither host widget accesses Room, UI state holders, or keychain-protected files directly.
- Persisted selection (`VaultSettings.widgetBikeId: String? = null`, mirrored in `VaultState` and `BackupPreferences`) identifies the target bike by stable `id`.

## Bike Scope Resolution

1. Active candidate pool: `state.ride` (if non-null and `!archived`) plus `state.garageBikes.map { it.ride }.filterNot { it.archived }`.
2. If `state.widgetBikeId` matches an active candidate's `id`, project that bike and its `rules`.
3. Otherwise fall back to the first active non-archived bike.
4. If no active bike exists, emit `WidgetTaskStatus.NONE` with `bikeId = null`, `bikeName = ""`, `odometerKm = null`, `taskItemKey = null`.

## Minimal Immutable Projection (`WidgetSnapshot`)

```kotlin
@Serializable
data class WidgetSnapshot(
    val schemaVersion: Int = 1,
    val bikeId: String? = null,
    val bikeName: String = "",
    val odometerKm: Double? = null,
    val taskItemKey: String? = null,
    val taskStatus: WidgetTaskStatus = WidgetTaskStatus.NONE,
    val privacyRedacted: Boolean = false,
    val language: AppLanguage = AppLanguage.BN,
    val digits: Digits = Digits.BN,
    val updatedAtEpochMillis: Long = 0L,
    val staleAfterHours: Int = 24
)
```

- **Status enum (`WidgetTaskStatus`)**: `OK`, `SOON`, `OVERDUE`, `UNKNOWN`, `NONE`, `LOCKED`.
- **Privacy behavior**: When `state.privacy.enabled` is `true`, emit `taskStatus = LOCKED`, `privacyRedacted = true`, `bikeId = null`, `bikeName = ""`, `odometerKm = null`, `taskItemKey = null`. No bike identity, odometer, or service status leaks to the lock screen or home screen widget while App Lock is enabled.
- **Explicit exclusions**: Registration numbers, VIN/chassis, engine numbers, document IDs/files, phone numbers, financial totals, and free-form notes are never included in `WidgetSnapshot`.
- **Staleness rule**: `isStale(nowEpochMillis)` returns `true` when `updatedAtEpochMillis <= 0L` or `(nowEpochMillis - updatedAtEpochMillis) > staleAfterHours * 3_600_000L`. Host widgets display a localized stale badge rather than silently claiming fresh telemetry.

## Read-Only Deep-Link Contract (`WidgetDeepLink`)

- URI format: `motovault://widget/action?sheet=<fuel|odometer|garage>[&bikeId=<id>]`.
- Deep links never mutate vault records directly.
- Routing enters `WidgetNavigation` (`MutableStateFlow`), waits until `loading == false` and (if `state.privacy.enabled`) `accessMode == VaultAccessMode.OWNER` after native biometric/device unlock, switches to `bikeId` if active, sets `Tab.GARAGE`, and opens the reviewed `FuelSheet` or `OdometerSheet` (or lands on `Tab.GARAGE` for `garage`).
