# P05-03 verification: renewal sheet, verification boundary and history

Status: passed on the Android 17 emulator and the iOS 27 simulator on
2026-11-05. No physical device was used.

## Revision and tree

- HEAD `55c1e43` plus the uncommitted P05/P05-03 change set (shared UI, resources,
  domain/backup, prototype and probes).
- The reports describe the working tree, not a clean committed revision.

## Prototype

```text
cd design/app-ui-v3 && npm run build
```

Result: token generation, `tsc --noEmit` and the Vite production build passed.
[The renewal sheet](P05-prototype-renewal.png) and
[the saved history](P05-prototype-renewal-history.png) are the reference states.

## Android runtime

Environment: Android 17 (API 37) Pixel 10 emulator, isolated
`com.motovault.app.privacyprobe` build.

```text
adb shell am instrument -w \
  com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result: **PASS 37 native storage checks**, including
`native-renewal-checklist-receipt-document-expense-and-reminder`. The renewal
check drives:

- document detail to the renewal sheet (Renew action);
- checklist ticks (state has two checked step IDs and the row is checkable for
  accessibility);
- fee entry through the described field;
- save through the coordinator path;
- renewed document detail and history (previous expiry, new expiry, paid amount,
  receipt attached);
- records list with the localized renewal row and `৳১,২৫০`;
- a reminder-plan event for the new expiry;
- unchanged original PDF bytes and an existing receipt asset.

Screenshots:

- [Reminder handoff](synthetic-p05-renewal-reminder.png)
- [Checklist with source date and fee](synthetic-p05-renewal-checklist.png)
- [Renewed document with history](synthetic-p05-renewed-document.png)
- [Renewal fee in records](synthetic-p05-renewal-expense.png)
- [Dark theme](synthetic-p05-renewal-dark.png)
- [Large text (font scale 1.3)](synthetic-p05-renewal-large-text.png)
- [Privacy-enabled document detail](synthetic-p05-renewal-privacy.png)
- [Unavailable-link fallback](synthetic-p05-renewal-link-fallback.png)

### Unavailable official link

`com.android.chrome` was disabled on the synthetic emulator, the "BRTA সাইটে কপি
যাচাই করুন" control was tapped, and the app showed
"BRTA সাইট খোলা যায়নি। আবার চেষ্টা করুন বা brta.gov.bd-এ দেখুন।" The checklist and
fee fields stayed on screen and usable. Chrome was re-enabled after the check. The
navigation URL opens only from an owner tap; no file or identity value is sent by
the app.

### Accessibility and adaptation

- The checklist row exposes a checkable semantics node; the probe asserts it.
- Fee, expiry and note fields expose content descriptions equal to their labels.
- Dark mode and a 1.3 font scale keep the sheet scrollable with the save footer
  fixed; no clipped labels in the captures.
- The privacy-enabled detail shows "শুধু এই ডকুমেন্ট দেখান" and keeps the
  renewal/verify actions; no document is revealed before the owner acts.

## iOS runtime

Environment: iOS 27 iPhone 18 Pro Max simulator (iPhone19,3), ad hoc signed debug
`com.motovault.app.privacyprobe` build, run `p05-renewal-final-20261105`.

Result: **PASS 22 native iOS storage checks**, including
`native-renewal-event-fee-reminder-and-round-trip`: previous/new expiry, revision,
paid fee, fee-log link, checklist reset, reminder plan, backup document mapping and
JSON round-trip. The verbatim result is
[P05-ios-native-probe.txt](P05-ios-native-probe.txt). App binary SHA-256:
`07ff01fe28c5f83e672b4b995c46da690a5fa511f19d055bb0c1098060584d2a`.

## Compilation and gates

- `composeApp:compileDebugKotlinAndroid` passed (part of the probe build).
- `composeApp:compileKotlinIosSimulatorArm64` passed.
- `ui_literal_gate.py`: 18 existing occurrences, unchanged.
- `ui_literal_gate.py --fuel`: 0 occurrences.
- `bangla_copy_lint.py`: 15 regression phrases pass.
- shared resource parity: 5 tests pass.
- `responsibility_gate.py`: 2 files / 7 functions, unchanged; probe functions
  were split to stay under the 80-line trigger.
- dependency baseline matches; shared packages 11 / 16 edges, no cycle.
- `roadmap.py check` passed before the task update.

## Limits

- The iOS probe exercises the shared renewal state, reminder and archive mapping,
  not the production iOS sheet; iOS UI screenshots are still represented by the v3
  prototype and the Android native capture. P08-05 keeps runtime localization
  evidence open.
- The system receipt picker's successful file selection was not driven end to end;
  the fixture stages a receipt through the same `ProtectedDocumentStorage` write
  path before the sheet. P05-04 covers dismiss/failed-save cleanup and keeps the
  real picker success path as a documented limitation.
- No physical Android or iPhone was used.
