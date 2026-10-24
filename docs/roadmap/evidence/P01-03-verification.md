# P01-03 reminder settings and native access verification

Base: `86c1598` plus the uncommitted P01 working change, 24 October 2026.
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
| Document snooze | [Saved snooze state](p01/android/document-snoozed.xml). Existing expiry and completion history stay unchanged. Shared tests verify consumed snoozes do not reappear after restart. |
| Android failed settings save | Final APK rerun confirms [failure UI](p01/android/settings-save-failed.xml) stays beside Save. [Before](p01/android/final-before-failed-save.json) and [after](p01/android/final-after-failed-save.json) persisted values match; the synthetic trigger was removed. |
| iOS denial | Real native Don't Allow action; [successful XCTest](p01/ios/denial-r5-summary.json), including app restart. |
| iOS background delivery | Actual Allow prompt and three OS-delivered notifications from the committed vault plan: Chain, expiry-today document and driving licence. [Observation and fixture fingerprint](p01/ios/background-observation.json), [native attachments](p01/ios/notifications/selected-attachments.json). The complete test run failed at the subsequent tap check; delivery evidence does not imply that routing passed. |
| iOS failed settings save | [Successful XCTest](p01/ios/save-failure-r2-summary.json); draft and Save remain visible after an injected SQLite failure. [Before](p01/ios/before-failed-save.json) and [after](p01/ios/after-failed-save.json) persisted values are identical. The reversible trigger was removed. |
| Native accessibility labels | Android hierarchy and iOS XCTest expose named actions, switches and gauge descriptions. Native button bounds refresh after scrolling. This is accessibility-tree and touch evidence, not a complete spoken TalkBack/VoiceOver walkthrough. |

The native failure path keeps the sheet open and announces a factual message near
Save. The button is disabled only while a save is in flight. Permission denial
keeps in-app dates usable and offers a Settings path plus manual instructions.
Explicit snooze is a distinct event; neither snooze nor notification delivery
records a service or renewal as completed.

## Open verification

iOS system-settings recovery is not yet accepted. The stronger native test
reached Settings home instead of MotoVault's notification page; the manual search
also failed to find the app in this simulator. The implementation uses Apple's
notification-settings URL on iOS 15.4+ and the app-settings URL on older supported
systems. [Failed direct-link result](p01/ios/settings-r4-summary.json) and
[failed manual route](p01/ios/settings-manual-r1-summary.json) remain diagnostic
evidence. The earlier settings-r1 result checked only that Settings opened and
must not be treated as permission-recovery proof.

iOS notification-cell taps did not foreground the app in the exercised simulator
automation, including a focused check against the observed native `ListCell`
buttons. [Final tap result](p01/ios/notification-taps-r4-summary.json) is failed.
Exact iOS document/service destinations, interactive snooze and revoke/return
therefore remain unverified. Shared resolver tests and host wiring are not used
as substitutes. Also check whether foreground reconciliation retains unrelated
delivered alerts while consuming a tapped alert; queue consumption and unread
notification visibility need separate runtime coverage.

Real Android and iPhone background delivery, denial, restart, settings recovery
and spoken screen-reader checks remain pending. The owner explicitly requested
keeping physical-device verification pending. Emulator/simulator success does
not close that phase exit gate. OS delivery is inexact; this change promises no
exact-alarm guarantee.
