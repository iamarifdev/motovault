# P01-02 scheduling and inverse-save verification

Base: `86c1598` plus this working change, 24 October 2026. Synthetic data only.

Implementation: shared `ReminderPlan.kt`, platform `RenewalReminders.kt`,
Android `MainActivity.kt`, Swift `AppDelegate.swift`, `VaultSession.kt` and the
five Undo callbacks in `MotoVault.kt`.

Service keys contain the complete schedule ID, baseline mileage/date and interval.
Distance-only schedules wait for a confirmed due mileage; calendar schedules
queue the due date. Crossing the distance limit brings a future request forward.
Per-record/global opt-out, deletion and changed baselines remove obsolete plans.
Both hosts reread committed storage rather than trusting a caller's snapshot.
The existing Android alarm receiver handles boot, clock and timezone broadcasts;
iOS retains reservations before OS submission and consumed-request recovery.
Explicit snooze gets a distinct request key and keeps consumption history.

Both notification payloads include the exact serialized target. Android uses a
unique PendingIntent data URI and handles cold launch/onNewIntent; iOS attaches
userInfo and forwards notification responses on the main queue. Missing/changed
records resolve to an explanatory toast, never a similarly named record.

| Verification | Result |
| --- | --- |
| Android/JVM planner/journal tests | PASS, retained under [scheduling/android](p01/scheduling/android/) |
| Kotlin/Native planner/journal and session tests | PASS, retained under [scheduling/ios](p01/scheduling/ios/) |
| Stable identities/collisions, deletion/opt-out, earlier distance limit, consumed events, snooze | Shared tests PASS |
| Missing/changed notification targets and malformed payload | Shared resolver tests PASS |
| Failed writes/stale revisions | Native session tests reject stale writes and never reconcile the failed candidate |
| Inverse save and asset safety | Native tests restore prior records, reject old Undo after a newer write and retain originals until the inverse is superseded |
| Android emulator UI Undo/restart | PASS: 12340 → 12350 → Undo → 12340 → process restart → 12340; [result](p01/android/undo-result.json) |
| iOS 27 simulator UI Undo/restart | PASS: same sequence, one XCTest with zero failures; [summary](p01/ios/undo-r2-summary.json) |
| iOS Swift host against current Kotlin framework | TEST BUILD SUCCEEDED, isolated bundle `com.motovault.baseline.p00` |

The initial iOS harness used a StaticText query, while Compose exposes the
odometer as a combined Button label. That run failed the harness assertion.
The second run uses the observed Button label and passes both pre/post-restart
assertions. Actual owner app data was never opened or reset.

Notification tap routing has resolver tests, native payload wiring and both host
builds. Actual Android document/service notifications also open their exact
records, as recorded in [P01-03](P01-03-verification.md). Remaining native access
and screen-reader checks stay in that task. This report does not claim physical-device
background delivery. The inverse is durably saved when tapped; the short-lived
Undo action itself does not survive process termination.
