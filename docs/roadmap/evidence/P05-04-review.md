# P05-04 review: phase exit gate

Reviewed 2026-11-05 against the P05 acceptance, the phase exit gate and the
architecture/design contract.

## Findings

- **Sourced, dated and applicable checklists.** Every guide carries a checked
  date and links to the BRTA page used; registration is explicitly not a periodic
  renewal, insurance points to the insurer, and licence/fitness/tax carry their
  own steps. Checklist identity is stable step IDs in both languages.
- **Deliberate official verification.** The QR action exists only for tax token,
  licence and fitness, opens only on an owner tap and states that a file is
  uploaded only if the owner chooses one on BRTA's own site. The disabled-browser
  check showed the localized fallback without losing the checklist.
- **Consistent renewal.** Document expiry/revision, append-only event, fee ledger,
  receipt asset and reminder snooze change in one state write. Undo restores the
  previous state; a failed save leaves the document and ledger untouched; a
  dismissed or failed renewal discards the staged receipt and keeps original bytes.
- **Expense and reminder.** The fee row is resolved by stable document ID and the
  new reminder plan is produced from the new expiry; neither is a display-label
  lookup.
- **Design and localization.** Renewal is the primary action on the detail, the
  source date is visible, fees are owner-entered rather than invented, and both
  languages have short resource-backed copy. Dark and large-text captures show no
  clipping.

## Residual issues and limits

- The system receipt picker's successful selection is not automated; the probe
  stages the receipt through the protected-write API. The picker's open/cancel
  path is platform-owned and untested here.
- iOS runtime covers shared state, reminder and archive, not a screenshot-driven
  production sheet.
- No physical device; P08 owns device, accessibility-speech and release gates.
- Fees and URLs are external and can change; the UI shows the checked date and
  never claims an amount is official.
- The registration guide is informational; a reissued card is still recorded as a
  renewal event. This is intentional and documented rather than a hidden expiry.

## Verdict

P05 implementation, localization, backup compatibility and the Android/iOS
runtime gates in this phase are satisfied. The phase can be marked done with the
limits above carried into P08 evidence and the phase risk log.
