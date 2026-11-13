# P16-02 Verification Evidence — Enabled-Module Matrix & Release Candidate

## Scope & Waiver
- Executed under `WD-02` ([P08-owner-decisions.md](P08-owner-decisions.md)): production release signing and store distribution stay deferred; Android debug APK (`:composeApp:assembleDebug` / `:composeApp:installDebug` on `Pixel_10`) and iOS Debug simulator build (`xcodebuild` on `iPhone 18 Pro Max`) serve as the verified simulator candidates.

## Module Enablement Matrix
| Module / Phase | Default State | Notes |
| --- | --- | --- |
| `P01`–`P08` Core Ledger, Bikes, Documents, Reminders, Export/Restore | Enabled | Local-first Room/SQLite + `.mva` encrypted archive active. |
| `P09` Vault Trust & Ownership | Enabled | Handover/theft readiness & archive verification active. |
| `P10` Cost Analytics | Enabled | Reconciled fuel/service spend insights active. |
| `P11` Quick Capture & Import | Enabled | Deep-link/share intake routing active. |
| `P12` Home Widgets & Daily Pulse | Enabled | Android & iOS `motovault://widget/action?sheet=*` deep-link flows active. |
| `P13` Seasonal Care & Breakdown Help | Enabled | 12-month Bangladesh care guidance (`OFFLINE_ONLY` weather default) and offline SOS contacts active. |
| `P14` Rider Work Ledger | **Opt-in Disabled (`workModeEnabled = false`)** | Entry UI accessible from History/Work Ledger without mutating default personal ledger; kept opt-in pending `WD-02` rider pilot. |
| `P15` Cloud Backup & Sync | **Disconnected (`CloudConnectionStatus.DISCONNECTED`)** | Contract and local fallback UI active; live remote provider disabled pending `WD-02` provider setup and security review. |

## Simulator Upgrade / Restore Smoke
- **Android (`Pixel_10`, `emulator-5554`)**: Installed over existing persisted `KTM Duke 125` vault container (`--no-clear` equivalent; data preserved), launched cleanly with `ANDROID_NO_FATAL_ERRORS`.
- **iOS (`iPhone 18 Pro Max`, `50709DF0-FEA9-4B34-9DE6-6172D868875D`)**: Installed over existing persisted `Yamaha R15 V4` vault container (`১২,৩৪০ কিমি`, `৳৫,৩৬০` spend preserved), launched home and sheets cleanly.
