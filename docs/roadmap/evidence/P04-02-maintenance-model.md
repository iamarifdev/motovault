# P04-02 maintenance model verification

Status: implemented and tested on the uncommitted P04 working tree based on
`f33ff31` with the later P04 changes. No owner data or physical device was used.

## Model

- `domain/maintenance/MaintenanceAction.kt` defines `INSPECT`, `CLEAN`,
  `LUBRICATE`, `ADJUST` and `REPLACE`, plus `SchedulePhase`, `ScheduleSource`
  and `ScheduleProvenance` (label, source URL, revision, page, applicability).
- `domain/maintenance/MaintenanceProfile.kt` owns the profile shape and
  `MaintenanceScheduleSpec`. A spec must have a distance or time interval and
  one-time schedules can only be running-in schedules.
- `domain/maintenance/MaintenanceProfiles.kt` owns the source-backed R15 V4,
  MT-15 V2 and KTM profiles. Manual specs carry the manual number/page and the
  market applicability note; owner fallbacks are marked `OWNER`.
- `ComponentRule` persists `action`, `provenance`, `phase`, `oneTime` and
  `catalogueId` with defaults, so pre-P04 schedule JSON still decodes.
- `LogMaintenanceUseCase` resets only the selected schedules, removes a one-time
  running-in schedule after it is recorded, and starts the matching recurring
  schedule from the same service (for example the running-in oil starts the
  5,000 km oil schedule). Unselected custom rules and baselines are untouched.

## Verification

Run on the Android 17 Pixel 10 emulator and the iOS 27 iPhone 18 Pro Max
simulator; the native probe reports:

```text
native-maintenance-profile-actions-provenance-and-unknown-baseline
```

Android unit tests cover:

- exact model/year/market/code matching and unsupported-model fallback;
- manual provenance URL/revision/page on every manual spec;
- running-in applicability only below the first service;
- distinct lubrication and adjustment schedules for the chain;
- selected-action reset only and preservation of unselected custom rules;
- one-time running-in removal and recurring-start behavior;
- mixed-action service visits resetting only linked schedules;
- pre-P04 schedule JSON defaults and encoded round trip;
- backup payload round trip of action, provenance, phase, one-time and
  catalogue fields.

`rtk run ./gradlew :composeApp:testDebugUnitTest --offline` passes with 274
tests. `rtk run ./gradlew :composeApp:compileKotlinIosSimulatorArm64 --offline`
passes.

## Limits

- The Bangladesh applicability gap is explicit: the manuals are India-market
  prints for the same model code. The owner must confirm the local year,
  variant and paperwork. P08-03 must confirm the provisional model set.
- The manual gives distance intervals without a time interval for engine oil and
  most wear items; those specs use `intervalDays = 0` rather than inventing one.
- Local-condition shortening is an owner edit, not an automatic multiplier.
