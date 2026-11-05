# P05-03 renewal checklist, verification boundary and history implementation

Status: implemented on 2026-11-05; runtime evidence is in
[P05-03-verification.md](P05-03-verification.md).

## Files

Shared UI:

- `ui/documents/RenewalSheet.kt` — tall sheet; progress, dated source line,
  checklist rows, official links, fee field and calculator link, late-fine note,
  receipt picker/attached receipt, new expiry with year chips, notes, verification
  action, save footer and back action.
- `ui/documents/RenewalLabels.kt` — stable step/link IDs to localized resources.
- `ui/documents/RenewalHistorySection.kt` — append-only history with new expiry,
  paid amount, previous expiry, renewal date, receipt marker and note.
- `ui/documents/DocViewSheet.kt` — Renew, official verification and history above
  the privacy/roadside/reminder controls; the new-expiry copy and error state are
  localized.
- `ui/PaymentReceiptPicker.kt` plus Android/iOS actuals — owner-triggered picker
  that copies a JPEG/PNG/WebP into protected document storage and returns the
  managed asset ID; failures keep no partial asset.
- `ui/VaultSheetHost.kt` — renewal sheet routing, checklist commit, save toast and
  Undo, and return to the exact document.
- `ui/history/HistoryScreen.kt` — document-fee rows resolve the document name by
  stable ID and the localized "renewal" label instead of a persisted string.

Resources:

- 91 new keys in `composeResources/values/strings.xml` and
  `values-bn/strings.xml`: sheet copy, fee/receipt/expiry/notes, history,
  verification note, and 24 checklist steps with details. Every placeholder is
  shared by both languages.
- Bangla follows the P05-01 review table; English is equally short.

Prototype:

- `design/app-ui-v3` gained `RenewalSheet.tsx`, `data/renewal.ts`, renewal store
  actions/types, document-detail actions and history, and renewal styles.
  `npm run build` passed.

## Behaviour

- Checklist taps persist only stable step IDs on the document; renewal clears
  them in the same save.
- The fee is optional, owner-entered and stored as poisha on the renewal event.
  No fee rule is embedded; the late-fine sentence is a warning and the calculator
  is a link.
- The receipt is copied into protected storage before the save. A failed save
  keeps it staged for retry; dismissing the sheet discards an uncommitted receipt.
  A committed receipt is kept and appears in history as attached.
- Official links open only after a tap through `LocalUriHandler`; a launch failure
  shows the localized "BRTA site could not open" message. Nothing is prefilled or
  uploaded.
- The save routes through the existing coordinator and shows the standard Undo
  toast; Undo restores the previous document, renewal history, checklist and fee.
- The sheet returns to the same document ID after saving (no list jump).

## Tests and coverage added

- `ProbeRenewalChecks.kt` (Android instrumentation) drives the synthetic document,
  checklist, fee, save, history, records list, dark theme, privacy-enabled detail,
  checkable semantics and reminder plan.
- `IosStorageProbe.verifyRenewalProbe()` verifies the shared renewal state,
  reminder plan, archive mapping and JSON round-trip in the iOS simulator.
- Probe helpers were extracted to keep every probe function under the 80-line
  review trigger without changing the responsibility baseline.
