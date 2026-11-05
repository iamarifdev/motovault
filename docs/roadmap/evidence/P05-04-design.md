# P05-04 renewal flow design and evidence map

## Flow

```text
document reminder / expiry
        |
        v
Vault document detail  ──history/verify──> BRTA source (owner tap only)
        |
      Renew
        v
Renewal sheet
  progress + dated source
  checklist (stable IDs, persisted)
  official links (owner tap)
  paid amount + calculator link
  receipt (protected copy)
  new expiry + year chips
  notes
        |
     Save (one coordinator commit)
        v
renewed document + renewal history + fee row + new reminder plan
```

Checklist taps are separate small commits so progress survives leaving the sheet.
The expiry, event, fee, receipt reference, revision and snooze reset are one save.
The footer keeps Save reachable while the content scrolls.

## Visual reference

- [Renewal sheet](P05-prototype-renewal.png) and
  [history](P05-prototype-renewal-history.png) in the v3 prototype.
- Native: [reminder handoff](synthetic-p05-renewal-reminder.png),
  [checklist](synthetic-p05-renewal-checklist.png),
  [renewed document](synthetic-p05-renewed-document.png),
  [fee row](synthetic-p05-renewal-expense.png),
  [dark](synthetic-p05-renewal-dark.png),
  [large text](synthetic-p05-renewal-large-text.png),
  [privacy detail](synthetic-p05-renewal-privacy.png),
  [link fallback](synthetic-p05-renewal-link-fallback.png).

The sheet reuses the Sky sheet geometry, shared buttons, chips, typography and
surface/border tokens already used by the document detail. The fee row uses the
same history-row family as service and fuel rows.

## Interaction rules

- Renew is the primary action on the document detail, above protection and
  reminder controls.
- The source date is always visible with the source name.
- Official links are optional; a failed launch shows localized fallback copy and
  never blocks the checklist or the save.
- The fee is the amount actually paid, never a calculated total; the official
  calculator is a link.
- The receipt is optional; a picked file is copied to protected storage and a
  cancelled or failed renewal does not keep an unreferenced file.
- History is append-only and shows the previous expiry, new expiry, payment,
  receipt marker and note.
- Save returns to the same document, where the new expiry and history are visible;
  the standard Undo toast is available.

## Adaptation

- The tall sheet scrolls; the footer is fixed.
- Dark surfaces and 1.3x text were captured without clipping.
- Checklist rows expose a checkable semantics node; fee/expiry/note fields expose
  their labels as content descriptions.
- The privacy-enabled detail still leads with renewal actions but does not reveal
  the document before the owner acts.
