# P05-04 Android runtime: full renewal

Environment: Android 17 (API 37) Pixel 10 emulator (`sdk_gphone16k_arm64`,
arm64-v8a), isolated `com.motovault.app.privacyprobe` build. No physical device
was used.

## Build

```text
:composeApp:assemblePrivacyProbe
:composeApp:assemblePrivacyProbeAndroidTest
```

APK SHA-256:

- app: `e63144b8a011f223b4ca046238fdf1f40619152c40755697cc2b389c2e6ff729`
- test: `8a5d500c50e32f50318044fb6044bc6133c22b3db71e4e8f05bce0109908eedb`

## Run

```text
adb shell am instrument -w \
  com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result: **PASS 37 native storage checks**, including
`native-renewal-checklist-receipt-document-expense-and-reminder`.

The renewal check starts from a synthetic tax token whose plan produces a renewal
reminder event (asserted before the UI flow). It renders the production reminder
heading/message in a reminder handoff screen, opens the exact document, renews,
and then verifies:

- checklist ticks persist by stable step ID and the row exposes checkable
  semantics;
- a synthetic payment receipt is attached through the protected asset path;
- the saved document has the new expiry, revision 1 and one renewal event with
  previous expiry, new expiry, fee 125000 poisha, receipt ID and linked fee log;
- the fee log is a `document_fee` row with ৳১,২৫০, the document ID and the
  renewal ID;
- the records screen shows the localized "ট্যাক্স টোকেন নবায়ন" row and ৳১,২৫০;
- a new reminder-plan event exists for the new expiry;
- the original PDF bytes are unchanged before and after the renewal;
- the checklist is cleared and the reminder snooze is cleared.

Additional checks in the same probe:

- **cancel**: the sheet is dismissed after entering a fee with a staged receipt;
  the document stays unchanged, the staged receipt is discarded and the original
  bytes are intact;
- **failed save**: the fixture returns a failed save; the localized failure copy
  is shown, the document has no renewal or fee log, and the staged receipt is
  discarded on dismissal;
- **dark mode**, **privacy-enabled detail** and **large text (1.3 font scale)**
  are captured without clipped labels;
- **unavailable link**: with Chrome disabled, the verify action shows the
  localized BRTA fallback instead of crashing; the checklist stays usable.

## Screenshots

- [Reminder handoff](synthetic-p05-renewal-reminder.png)
- [Checklist with source date and fee](synthetic-p05-renewal-checklist.png)
- [Renewed document and history](synthetic-p05-renewed-document.png)
- [Renewal fee in the records list](synthetic-p05-renewal-expense.png)
- [Dark theme](synthetic-p05-renewal-dark.png)
- [Large text](synthetic-p05-renewal-large-text.png)
- [Privacy-enabled detail](synthetic-p05-renewal-privacy.png)
- [Unavailable-link fallback](synthetic-p05-renewal-link-fallback.png)

## Limits

- The system receipt picker's successful file selection is not driven; the
  fixture writes the receipt through the same protected storage API and the sheet
  attaches it. The real picker is exercised only for opening/cancel behaviour by
  the platform implementation, not by this probe.
- The flow starts from a synthetic reminder handoff screen using the production
  reminder heading/message, not from a delivered notification tap. Notification
  delivery and tap handling were verified by the P01 runtime evidence and are not
  re-run here.
