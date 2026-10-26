# P01-03 reminder settings and native access verification

Initial base: `86c1598`; follow-up base: pushed `48fce40` plus the uncommitted
delivered-alert retention fix, 25 October 2026.
Only the isolated `com.motovault.baseline.p00` app and synthetic records were used.
See [evidence index](p01/README.md) for artifacts, revisions and remaining gates.

Implementation: `ReminderSheets.kt`, `ReminderPlan.kt`, platform
`RenewalReminders.kt`, `MainActivity.kt` and `AppDelegate.swift`. Me and Vault use
the same native permission state and committed policy. Device consent remains
separate from migrated preferences. The displayed queue reflects accepted native
requests rather than a count of documents. Permission refresh also runs on resume.

| Check | Evidence and scope |
| --- | --- |
| Android deny and revoke | Actual emulator permission prompt and return state: [denied](p01/android/permission-denied.xml), [revoked](p01/android/permission-revoked-after-return.xml) |
| Android Settings recovery | Native toggle enabled, then app shows permission restored: [native toggle](p01/android/native-after-notification-enable.xml), [return](p01/android/settings-return-recovered-final.xml) |
| Android background document delivery and tap | Actual OS notification from the vault plan, [delivery](p01/android/background-notification.json), [exact document](p01/android/notification-exact-document.xml) |
| Android background service delivery and tap | [Actual service notification](p01/android/service-runtime/background-service-notification.json), [exact Chain schedule](p01/android/service-runtime/notification-exact-service.xml) |
| Android delivered-alert retention follow-up | [Passing actual-policy sequence](p01/android/android-retention-20261025-012210/result.json): [exact document](p01/android/android-retention-20261025-012210/exact-document.xml), [Chain remains](p01/android/android-retention-20261025-012210/chain-retained-after-document-open-0.xml), then [exact service](p01/android/android-retention-20261025-012210/exact-service.xml). Native notification groups are expanded before selecting individual alerts. |
| Document snooze | [Saved snooze state](p01/android/document-snoozed.xml). Existing expiry and completion history stay unchanged. Shared tests verify consumed snoozes do not reappear after restart. |
| iOS document and service snooze | `testDocumentAndServiceSnooze` [passes](p01/ios/snooze-r1/tests.json). Both native sheets show tomorrow at 09:00 persisted. [Before](p01/ios/snooze-r1/before.json) and [after](p01/ios/snooze-r1/after.json) confirm unchanged expiry, service baseline, odometer and ledger count. The same whole run failed its separate Settings header assertion; [mixed summary](p01/ios/snooze-r1/whole-run-summary.json) is retained as failed. |
| Android failed settings save | Final APK rerun confirms [failure UI](p01/android/settings-save-failed.xml) stays beside Save. [Before](p01/android/final-before-failed-save.json) and [after](p01/android/final-after-failed-save.json) persisted values match; the synthetic trigger was removed. |
| iOS denial | Real native Don't Allow action; [successful XCTest](p01/ios/denial-r5-summary.json), including app restart. |
| iOS revoke, Settings and return | [Passing recovery-r4](p01/ios/recovery-r4/summary.json). Guarded selection of `MotoVault P01 Test` precedes toggling its real OS permission. App reports denied on return, then allowed after restoring it, without force-restarting. The app's Settings action reaches the [Notifications pane with the isolated app back button and disabled switch](p01/ios/recovery-r4/settings-url-destination.txt). |
| iOS background delivery | Actual Allow prompt and three OS-delivered notifications from the committed vault plan: Chain, expiry-today document and driving licence. [Observation and fixture fingerprint](p01/ios/background-observation.json), [native attachments](p01/ios/notifications/selected-attachments.json). The complete test run failed at the subsequent tap check; delivery evidence does not imply that routing passed. |
| iOS exact targets and delivered-alert retention, follow-up | [Passing r8 XCTest](p01/ios/background-r8/summary.json) on iOS 27, iPhone 18 Pro Max `50709DF0-FEA9-4B34-9DE6-6172D868875D`. Native side-swipe/Open opens the [exact document](p01/ios/background-r8/notification-exact-document.png), then the separate Chain alert remains and opens the [exact service schedule](p01/ios/background-r8/notification-exact-service.png). Actual calendar policy delivery, not an injected push. |
| iOS failed settings save | [Successful XCTest](p01/ios/save-failure-r2-summary.json); draft and Save remain visible after an injected SQLite failure. [Before](p01/ios/before-failed-save.json) and [after](p01/ios/after-failed-save.json) persisted values are identical. The reversible trigger was removed. |
| Native accessibility labels | Android hierarchy and iOS XCTest expose named actions, switches and gauge descriptions. Native button bounds refresh after scrolling. This is accessibility-tree and touch evidence, not a complete spoken TalkBack/VoiceOver walkthrough. |

The native failure path keeps the sheet open and announces a factual message near
Save. The button is disabled only while a save is in flight. Permission denial
keeps in-app dates usable and offers a Settings path plus manual instructions.
Explicit snooze is a distinct event; neither snooze nor notification delivery
records a service or renewal as completed.

## Open verification

The historical [failed direct-link result](p01/ios/settings-r4-summary.json) and
[failed manual route](p01/ios/settings-manual-r1-summary.json) remain diagnostic
evidence. They are superseded for simulator recovery by recovery-r4 on the
preferred iOS 27 device. The earlier settings-r1 checked only that Settings opened
and remains insufficient proof. The test host now has a unique display name and
a validated local ad-hoc signature matching its isolated bundle ID. These
packaging checks are not proof of a specific cause for the earlier failures.

Earlier notification-cell tap automation failed to foreground the app:
[historical failed result](p01/ios/notification-taps-r4-summary.json). SpringBoard
subsequently reported that it was hinting a side swipe. The native side-swipe/Open
route exercised in r8 passes both exact destinations. This does not establish
that every notification tap gesture works on a physical device.

The intervening r7 run exposed a real reconciliation bug: opening one alert
removed other delivered alerts whose scheduling events had already been consumed.
Both native hosts now reconcile pending requests separately from delivered alerts.
Consumption prevents rescheduling, while a still-current delivered alert remains
visible. Deletion, opt-out, changed expiry/baseline and snooze invalidate obsolete
alerts. Six shared regression tests cover this boundary. iOS r8 verifies the
document-then-Chain runtime sequence after the fix. Interactive iOS snooze and
revoke/return pass independently. The Android retention sequence also passes on
the rebuilt isolated APK. Its earlier group-summary tap opened the launcher
without a record payload; the accepted run selects individually clickable alerts.
Transient setup captures were retried against the same live fixture and alarms.
No short-delay injected probe substitutes for either accepted policy sequence.

Real Android and iPhone background delivery, denial, restart, settings recovery
and spoken screen-reader checks remain pending. The owner explicitly requested
keeping physical-device verification pending. Emulator/simulator success does
not close that phase exit gate. OS delivery is inexact; this change promises no
exact-alarm guarantee.
