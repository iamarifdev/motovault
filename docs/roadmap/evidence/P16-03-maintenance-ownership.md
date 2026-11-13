# P16-03 Maintenance Ownership & Observed Review

## Ownership Matrix
| Area | Assigned Owner | Scope |
| --- | --- | --- |
| Manuals, checklists, emergency/link freshness | Ariful Islam | Bangladesh emergency boundaries (999), BRTA/document checklists, seasonal monsoon/heat/dust copy accuracy. |
| OS & toolchain compatibility | Ariful Islam | Android API/emulator updates, Xcode/iOS Simulator updates, Compose Multiplatform & SQLite/Room upgrades. |
| Backup & recovery drills | Ariful Islam | Periodic `.mva` archive round-trip checks, cross-device verification, future `P15` live-provider staged rollout gate. |
| Privacy & support triage | Ariful Islam | Local-first data guarantees, deliberate GPS capture consent, opt-in `workModeEnabled` boundaries. |

## Observed Simulator & Suite Review (Anonymized Baseline)
- **Automated coverage**: 349 iOS tests (`iosSimulatorArm64Test`), 355 Android JVM unit tests (`testDebugUnitTest`), 61 Python tooling tests (`tools/tests`), and all 6 architecture/copy/responsibility/docs gates passed cleanly after all P09–P15 changes.
- **UI/UX checks**: Both EN/BN persisted bike vaults (`KTM Duke 125` on Android, `Yamaha R15 V4` on iOS) survived in-place upgrades with intact odometer, fuel history, maintenance readiness, seasonal help, and widget deep-link routing.
- **Open items from `WD-01` / `WD-02`**: Physical-device testing (`P08`), external Bangla human copy sign-off, external rider pilot (`P14`), live cloud provider provisioning (`P15`), and release signing/store publication (`P08-04` / `P16-02`) remain explicitly tracked for pre-store execution.
- **Scheduled automation policy**: No background/cron automation was created.
- **Dated next review**: `2027-02-13` (owned by Ariful Islam).
