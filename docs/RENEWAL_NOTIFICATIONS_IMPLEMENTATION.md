# Renewal reminders

Implemented 12 October 2026 in the existing document vault. The in-app 60-day expiry policy remains active even when device notifications are unavailable or turned off. No owner records were added as test fixtures.

## Schedule and controls

- Reminders are opt-in through the vault's existing panel/button style. The control explains the schedule before requesting native notification permission. The owner can turn them off and open native notification settings after denial.
- A warning is due 60 days before expiry at 09:00 local time; an expiry reminder is due on the expiry date at 09:00 local time.
- A document first scheduled inside the warning window gets one warning at the next local 09:00. Its selected warning date is durable and does not slide forward on each relaunch. If that morning would be the expiry date, the expiry reminder replaces the redundant warning.
- A document added on the expiry day after 09:00 gets one same-day expiry catch-up. Expired, invalid-date and no-expiry documents have no native reminders. The existing in-app expiry labels remain available.
- Android uses inexact alarms. 09:00 is a requested due time, not an exact delivery guarantee. Battery restrictions and device state can delay delivery. iOS uses non-repeating calendar notifications in the device's current timezone.

## Identity, durability and reconciliation

`RenewalReminders.kt` in common code defines the pure schedule and full event identity: the complete durable document ID, its length, expiry and reminder kind. Display edits do not change event identity; expiry edits do. Android uses the full encoded key as PendingIntent data and as the notification tag, with request/notification integer zero. Java hash collisions cannot merge document reminders. iOS uses the full key as the notification request identifier.

`VaultSession` runs reconciliation after successful restore and after a successful durable commit. Failed persistence never schedules uncommitted documents. Permission and scheduling failures cannot turn a committed save into a failure or remove saved documents. Native reconciliation uses a mutex and rereads durable documents within that lock so an old UI snapshot cannot replace reminders for a newer save.

Edits, deletion, disabling notifications and revoked permission cancel obsolete pending reminders. Relaunch and return to the vault refresh native settings and schedules. Android receives boot, clock change, timezone change and package update broadcasts; its Application and receiver initialize the storage context independently of MainActivity. Alarms validate the current durable document ID/expiry immediately before delivery. An alarm which arrives early after a clock/timezone change is deferred to the new local due time.

Android keeps a durable consumed-key ledger. It records consumption before posting to avoid repeat delivery after process death, including when the clock moves backward. There is a small at-most-once tradeoff: a process death between ledger commit and `notify()` can miss an alert. Force-stopping Android prevents alarms until the owner launches the app again, as imposed by the operating system.

iOS now commits each request identity to a private journal before submitting it to the notification center. The previous implementation wrote the accepted list only after the asynchronous loop, which allowed an immediate request to deliver and replay after process death. The journal writes a complete temporary file, checks `fsync`, atomically renames it over the current file, and checks a parent-directory `fsync`. Unreadable or corrupt journals stop reconciliation rather than silently resetting delivery identity. Existing preference metadata is imported once when no journal exists.

On relaunch, elapsed reserved requests absent from the OS pending queue become consumed. Delivered OS identities also become consumed, including after the clock moves backward. Existing elapsed requests still pending are preserved. Future reserved requests absent from the queue retry. An explicit OS submission error removes its reservation with a checked journal write so a later reconciliation can retry. Cancellation keeps the reservation because acceptance is uncertain. This is an at-most-once tradeoff: termination after reservation but before OS acceptance can miss an elapsed alert. A failure while persisting rejection cleanup can also leave an uncertain reservation. File and directory synchronization protect against application process termination; this does not claim protection from device power loss or an OS/storage failure, for which Apple's `fsync` documentation gives weaker guarantees.

Future calendar requests follow local device time; there is no background clock receiver on iOS. The iOS host must retain its `UNUserNotificationCenterDelegate` if foreground banners are required. Denied native permission leaves in-app warnings functional. System queue limits and delivery restrictions still apply.

The reminder enable preference remains in platform preferences. iOS queue and consumed metadata lives in `Application Support/renewal-journal.json`, independently of owner vault records. Restoring the vault on another device does not silently grant notification permission or enable reminders there.

## Verification

Eight common policy tests cover 60-day boundaries, before/after 09:00 late additions, durable catch-up dates, expired/missing/invalid dates, expiry-day deduplication, expiry edits/deletion, consumed events after backward clock movement, full-ID collision avoidance and display edits. Two iOS session tests cover reminder failure after restore/save and prevention of scheduling after a failed durable write.

Final renewal verification passed Android debug assembly, iOS ARM64 compilation and Android/iOS simulator tests: 83 Android tests and 91 iOS tests, zero failures. This includes the two session regressions and channel-disabled handling. Compilation and policy tests do not establish native delivery.

The 13 October crash-journal correction passed iOS ARM64 compilation and the full current suites: 95 Android tests and 108 iOS simulator tests, zero failures. Five new common tests model OS acceptance followed by termination before any completion write, the pre-submission at-most-once loss window, explicit rejection/relaunch retry, future unsent and elapsed pending reservations, and delivered identities after backward clock movement. Two iOS tests execute the real file adapter, recreate the storage instance, and verify reservation/rejection replacement and temporary-file removal. These establish checked filesystem persistence and deterministic recovery logic. They do not physically kill the native notification host between OS acceptance and its callback or prove notification delivery on iOS.

On the preserved Pixel_10, native permission denial showed the device-settings/in-app-warning explanation, a subsequent allow request enabled the control, and four full-key plans were stored for the existing two documents (warning 2030-10-26, expiry 2030-12-25). Owner documents were not changed. Screenshots and exact native delivery evidence are recorded by the runtime verification task.

The debug APK includes `RenewalFixtureActivity` and a non-exported fixture receiver, both excluded from the release manifest/source set. This schedules a due synthetic notification through AlarmManager using the production notification builder, with a separate `renewal_debug_fixture` preference ledger. It neither opens the owner database nor modifies the production reminder queue. It proves native scheduling/posting independently of the owner's 2030 expiry dates; it does not prove a real owner document's due-time validation path. Delivery, cancellation and duplicate/restart checks passed; see [retained runtime report](continuation-verification/RENEWAL_RUNTIME.md).

The activity's debug-only `mode=replay` sends the existing fixture's same full identity through that receiver without clearing its ledger, cancelling its notification, or scheduling a new alarm. Repeated-delivery and restart checks passed using the synthetic fixture only; consumed-key replays did not post duplicates.

On 14 October 2026, the actual iOS host displayed the native notification permission prompt through Enable reminders. After Allow, the isolated DEBUG-only 15-second TEST probe scheduled without error, reached the retained foreground delegate and displayed a visible banner. The identifier is `motovault.debug.delivery-probe`; it is not an owner expiry. See [native probe evidence](continuation-verification/ios/13-native-probe.log) and [banner](continuation-verification/ios/14-native-test-notification.png). Future owner due-time validation and physical/background delivery remain unverified.

## Official references

- [Android inexact alarm scheduling, cancellation and reboot behavior](https://developer.android.com/develop/background-work/services/alarms). No exact-alarm permission is added.
- [Android notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission).
- [Apple local notification scheduling](https://developer.apple.com/documentation/usernotifications/scheduling-a-notification-locally-from-your-app).
- [Apple notification authorization](https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications).
- [Apple UserDefaults persistence semantics](https://developer.apple.com/documentation/foundation/userdefaults): updates reach disk asynchronously, so `setObject` alone cannot serve as a process-crash journal.
- [Apple fsync semantics and power-loss limitation](https://developer.apple.com/library/archive/documentation/System/Conceptual/ManPages_iPhoneOS/man2/fsync.2.html).
- [Apple atomic same-filesystem rename](https://developer.apple.com/library/archive/documentation/System/Conceptual/ManPages_iPhoneOS/man2/rename.2.html).

These references informed the native adapters; platform compilation was used to validate the actual Kotlin bindings.
