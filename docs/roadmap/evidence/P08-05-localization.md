# P08-05 localization and SRP establishment

Status: implementation and static verification complete ahead of the release-stage
prerequisite; Android/iOS runtime language-switch evidence remains pending.

## Completed

- All active screen/sheet copy, Android/iOS native prompts, catalogue labels and
  authentication messages resolve through resources or typed presentation.
- Domain catalogues use stable IDs; labels/units are resolved by ID. No new
  localized display labels are persisted as record identity.
- Bangla copy was reviewed against
  [localization-style.md](../../engineering/localization-style.md), with
  bKash/Pathao-style terminology and a regression lint for known robotic
  translations.
- Date, unit, month, digit and currency formatting is locale-aware.
- No production file is above 400 lines. The only remaining over-threshold files
  are test support (`NativeAssetInstrumentation.kt`, `BackupTest.kt`).

## Verification run

```text
274 Kotlin tests, 0 failures
Android/iOS shared compilation passed
bangla_copy_lint.py: 15 regression phrases pass
ui_literal_gate.py: 18 data/alias entries
responsibility_gate.py: 2 test-support files
architecture_dependencies.py / architecture_package_cycles.py: pass
roadmap.py check: 19 phases / 92 tasks
```

## Still required before P08-05 is done

- Android emulator language switch across affected screens, native notifications
  and permission/error surfaces.
- iOS simulator language switch for the same scope.
- Screenshots/recording evidence and final review against the design and copy
  contracts.
- P08-05 depends on P08-01, which is not complete; this work is recorded as
  in progress rather than done.
