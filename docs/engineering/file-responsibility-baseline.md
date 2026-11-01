# Handwritten file responsibility baseline

Status: P04A-06 responsibility baseline, 2026-11-01. This records the remaining
over-400-line production files after the screen/sheet/root split. P04A-06 turns
this into an executable ratchet; the entries below are exceptions, not permission
to add new debt.

| File | Lines | Owner / reason | Removal or re-review trigger |
| --- | ---: | --- | --- |
| `ui/backup/BackupCreateContent.kt` | 404 | Create-backup stage | P04A-04 exception: review on the next backup change. |
| `ui/backup/BackupRestoreContent.kt` | 465 | Restore/review stage | P04A-04 exception: extract review sub-stage on the next restore change. |
| `ui/MotoVault.kt` | 667 | Root composition, navigation and sheet assembly | P04A-03 exception: extract sheet assembly before the next root feature change. |
| `ui/maintenance/ServiceSheet.kt` | 586 | Fast multi-line service flow | P04A-02 exception: extract the line editor when this flow next changes. |
| `ui/garage/GarageScreen.kt` | 485 | Garage dashboard composition | P04A-02 exception: extract dashboard sections when the garage changes for P09-03. |
| `ui/components/Controls.kt` | 445 | Shared control family | P04A-04 exception: split button/chip/segmented families on the next control change. |
| `ui/settings/MeScreen.kt` | 440 | Settings/profile composition | P04A-02 exception: extract settings sections on the next settings change. |
| `ui/onboarding/OnboardingScreen.kt` | 422 | Three-step onboarding | P04A-02 exception: extract step bodies on the next onboarding change. |
| `ui/fuel/FuelSheet.kt` | 420 | Fuel sheet composition | P04A-04 exception: extract amount/keypad sections on the next fuel change. |
| `androidInstrumentedTest/.../NativeAssetInstrumentation.kt` | 568 | Test-only probe entry after fixture split | P04A-06 exception: keep the entry runner here; stage checks live in `Probe*Checks.kt`. |

Files above 600 lines require the recorded exception above. No production file
above 400 lines may grow without updating this table and the tracker.
