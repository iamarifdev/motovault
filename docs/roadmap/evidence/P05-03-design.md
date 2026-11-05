# P05-03 renewal checklist and deliberate official verification design

Status: prototype extended and native sheet implemented on 2026-11-05. Runtime
evidence is recorded in [P05-03-verification.md](P05-03-verification.md).

## Placement in the document detail

The v3 document detail now leads with the renewal task instead of burying it under
privacy controls:

1. document art and expiry chip;
2. **Renew**;
3. **Verify the copy on BRTA** (only for tax token, licence and fitness) with the
   note that a file is uploaded only if the owner chooses one on BRTA's site;
4. renewal history;
5. protection, roadside/police mode and reminder controls;
6. reminder settings and back.

The renewal sheet is a tall Sky sheet with a fixed save footer. It reuses the
existing sheet, button, chip, section-title, switch and control tokens from
`design/app-ui-v3`; no new visual language was introduced.

## States covered in the prototype first

`design/app-ui-v3` gained:

- `src/data/renewal.ts` — dated official sources, per-kind step IDs, BRTA links,
  fee-calculator and QR applicability, late-fine note;
- `src/sheets/RenewalSheet.tsx` — checklist progress, source line, offline note,
  fee entry and calculator link, receipt file, new expiry and year chips, notes,
  verification action and save;
- `DocViewSheet.tsx` — Renew, verify and append-only history;
- store actions `renewDoc`/`toggleDocStep`, document renewal types and styles.

Prototype screenshots:

- [Document detail with Renew and verify](P05-prototype-doc-view.png)
- [Renewal checklist with source date, fee and save](P05-prototype-renewal.png)
- [Saved renewal in history](P05-prototype-renewal-history.png)

The prototype build (`npm run build`: token generation, `tsc --noEmit`, Vite
production build) passed. The demo data and the native app stay separate.

## Native composition

- `ui/documents/RenewalSheet.kt` — tall sheet, progress bar, checklist rows,
  source/date line, official links, fee field and calculator link, receipt picker
  or attached receipt, new-expiry field and year chips, notes, verification, save.
- `ui/documents/RenewalLabels.kt` — stable step/link ID to resource mapping; no
  localized label is persisted.
- `ui/documents/RenewalHistorySection.kt` — append-only history cards with new
  expiry, paid amount, previous expiry, renewal date, receipt marker and note.
- `ui/PaymentReceiptPicker.kt` (+ Android/iOS actuals) — copies an owner-chosen
  JPEG/PNG/WebP into protected document storage and returns a managed asset ID;
  failures are reported and no partial asset is kept.
- `ui/documents/DocViewSheet.kt` — Renew, verify and history, ordered above the
  privacy and reminder controls.

The fee section never calculates a fee. It shows the documented late-renewal
warning and links to BRTA's calculator; the value the owner enters is the amount
actually paid and is stored on the renewal event. The official source's checked
date (5 November 2026) is always visible.

## Accessibility and adaptation

- Checklist rows use `Modifier.toggleable(role = Role.Checkbox)` with a visible
  `Checkbox`; the step title and detail stay readable in one node.
- Fee, expiry and note fields expose a `contentDescription` matching their label,
  so screen-reader and instrumentation users can find and set them.
- Buttons are verb-first and the offline note explains that the link is optional.
- Dark mode and large text were exercised on the synthetic Android fixture; the
  tall sheet scrolls, the fixed footer keeps Save reachable and the history cards
  wrap instead of truncating.

## Native screenshots

- [Renewal checklist, Bangla](synthetic-p05-renewal-checklist.png)
- [Renewed document and history](synthetic-p05-renewed-document.png)
- [Renewal fee in the records list](synthetic-p05-renewal-expense.png)
- [Dark theme](synthetic-p05-renewal-dark.png)
- [Large text](synthetic-p05-renewal-large-text.png)
- [Privacy-enabled document detail](synthetic-p05-renewal-privacy.png)
- [Unavailable-link fallback](synthetic-p05-renewal-link-fallback.png)
