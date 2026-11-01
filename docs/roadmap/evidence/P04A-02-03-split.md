# P04A-02/P04A-03 screen, sheet and root split

Status: split applied and behavior verified; documented large-file exceptions
remain in the responsibility baseline.

## Split files

| Original | New files |
| --- | --- |
| `ui/Screens.kt` (1,670) | `ui/garage/GarageScreen.kt`, `ui/documents/VaultScreen.kt`, `ui/history/HistoryScreen.kt`, `ui/settings/MeScreen.kt`, `ui/onboarding/OnboardingScreen.kt` |
| `ui/Sheets.kt` (1,501) | `ui/components/Sheet.kt`, `ui/quickadd/QuickAddSheet.kt`, `ui/maintenance/ServiceSheet.kt`, `ui/garage/OdometerSheet.kt`, `ui/documents/DocAddSheet.kt`, `ui/documents/DocViewSheet.kt`, `ui/history/PasteSheet.kt` |
| `ui/MotoVault.kt` (1,044) | root composition in `ui/MotoVault.kt`; `ui/navigation/ShellModels.kt`, `ui/navigation/BottomNavigation.kt`, `ui/components/CoreControls.kt`, `ui/components/BrandPresentation.kt`, `ui/format/Formatting.kt` |
| `ui/Components.kt` (967) | `ui/components/Controls.kt`, `Gauges.kt`, `DocumentArt.kt`, `Keypad.kt`, `Toast.kt` |
| `ui/privacy/VaultPrivacySheet.kt` (443) | `ui/privacy/PrivacyGate.kt`, `ui/privacy/PrivacySheet.kt` |

## Verification

`rtk run ./gradlew :composeApp:testDebugUnitTest
:composeApp:compileKotlinIosSimulatorArm64 --offline` passes with 274 tests.
Dependency direction and package-cycle checks pass. The shared UI literal
baseline was regenerated for the new paths (97 current occurrences).

## Documented exceptions

The current over-400-line baseline is recorded in
[file-responsibility-baseline.md](../engineering/file-responsibility-baseline.md).
`BackupSheet.kt` (821) remains assigned to P04A-04; the rest have an owner and a
removal trigger. `MotoVault.kt` is 667 lines and is the root-composition
exception; its sheet assembly must be extracted before the next root feature
change.
