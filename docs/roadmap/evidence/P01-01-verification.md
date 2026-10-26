# P01-01 policy and persistence verification

Base revision: `86c1598`, plus this working change. Date: 24 October 2026.
Scope: Android/JVM shared tests and Kotlin/Native iOS ARM64 simulator tests.
This is not background-delivery, physical-device or release proof.

Implementation: [policy](../P01-REMINDER-POLICY.md), `VaultSettings.kt`,
`VaultDatabase.kt` schema 4 and `RenewalReminders.kt` in all three source sets.

Command (native ARM64 JDK 17):

```sh
rtk run ./gradlew -I tools/baseline/isolated-android.gradle :composeApp:testDebugUnitTest :composeApp:iosSimulatorArm64Test :composeApp:assembleDebug
```

Result: PASS. The final full suites contain 148 Android/shared tests and 170 iOS
simulator tests, zero failures. Current XML is retained under [final tests](p01/tests/).
The earlier focused policy evidence remains under [policy evidence](p01/policy/).

| Requirement | Observed evidence |
| --- | --- |
| 30-day lead, 09:00 local, global/per-record overrides | Planner tests verify exact dates, custom lead/time and Dhaka/UTC conversion |
| Old policy and clock movement | Old 60-day plans recalculate; consumed event stays suppressed after clock rollback |
| Late additions, expiry edits, missing expiry, deduplication | Planner tests cover next local morning, one expiry-day event, full-ID collision cases and equal snooze times |
| Existing settings import | Legacy JSON defaults and explicit false flags tested; schema 1 migration preserves existing rows |
| Real schema 3 → 4 migration | Native Room opens the old-shaped database with its original identity hash, recovers preferences from its atomic outbox and preserves owner rows/revision on reopen. A failed migration rolls back; repairing the synthetic outbox and retrying preserves the original state. |
| Persistence and save failure | All exposed v3 preferences round-trip; injected SQLite trigger abort rolls settings, records and outbox back together |
| No scheduling from failed writes | VaultSession test confirms only the original committed document list reaches reconciliation |
| No permission grant by migration | Source inspection: migration writes only preferences; native enabled stores and OS authorization APIs are untouched |

One initial new settings test incorrectly searched for `ride` as a substring and
matched `brandOverride`. It now checks the JSON key. The final rerun passed.
Interactive snooze, permission recovery and service delivery are tracked in
[P01-03](P01-03-verification.md), including the remaining runtime gates.
No owner data was used by these tests.
