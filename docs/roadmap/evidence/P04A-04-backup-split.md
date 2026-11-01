# P04A-04 component and backup stage split

Status: complete on the uncommitted P04A working tree.

## Split

- `Components.kt` (967) became `ui/components/Controls.kt`, `Gauges.kt`,
  `DocumentArt.kt`, `Keypad.kt` and `Toast.kt`.
- `BackupSheet.kt` (821) became:
  - `ui/backup/BackupSheet.kt` (225) — tab shell and busy/dismiss state;
  - `ui/backup/BackupCreateContent.kt` (404) — create/password/progress/save;
  - `ui/backup/BackupRestoreContent.kt` (465) — picker, validation, review and
    restore; state is held in `BackupCreateState`/`BackupRestoreState` so the
    stage composables retain the same behavior.
- `backupErrorText` and `backupProgressText` are shared internal helpers.

## Verification

`rtk run ./gradlew :composeApp:testDebugUnitTest
:composeApp:compileKotlinIosSimulatorArm64 --offline` passes with 274 tests.
The Android backup/restore unit suites remain green. The shared UI literal gate
matches the regenerated baseline.

The two stage files are over 400 lines and are recorded as exceptions in
[file-responsibility-baseline.md](../engineering/file-responsibility-baseline.md);
they are under 600 and have a re-review trigger.
