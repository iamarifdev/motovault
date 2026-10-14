# Prototype verification · 2026-10-14

## Completed

- JavaScript syntax check: `rtk run node --check design/archive/prototype-v1/app.js`.
- Source smoke checks: `rtk run node design/archive/prototype-v1/verify.cjs`. Bangla/Latin numbers, grouped entry, invalid numeric formats, latest-service baseline derivation, unknown baseline after removal, and all 25 renderer headings passed.
- Live Codex in-app browser at loopback HTTP preview: Bangla font reported loaded and the Garage photo reported loaded.
- All 22 screens visited at 390px in Bangla: headings present, no document or screen horizontal overflow detected.
- All 22 screens visited at 320px with the prototype's larger-text setting: no document or screen horizontal overflow detected.
- All 22 screens visited at 320px in English: headings present and no screen horizontal overflow detected.
- Final expanded 25-screen reference visited at 320px with larger text in Bangla: every heading present, no document/screen horizontal overflow. Page-management, camera-denied and original-viewer screens were included. The initial 22-screen English/390px sweep preceded these three additions; those complete combinations need a later pass.
- Visual screenshot inspection of desktop Garage and mobile Garage; compact identity, readable Bangla and task-first hierarchy are present. The mobile workbench adds review chrome above the app; use `?mode=app#garage` for a product-only view.
- Empty form validation displays local errors.
- Bangla-digit fuel entry saves and renders localized date, amount, distance and litres in history.
- Injected next-save failure retains entries; retry saves successfully.
- Service entry above the stored odometer saves directly without leaving the form.
- Document sample → review → save → detail succeeded.
- Leaving dirty document review for another destination shows a discard dialog; keep-working retains the form.
- Document holder edit survives Back to import and return to review. This was rechecked using the live input value after a defect was fixed.
- Camera sample → review → crop → keyboard edge adjustment → review succeeded.
- Multi-page sample addition and return to review succeeded. Page-order/removal controls were not individually exercised.
- English, light/dark and empty-document states were exercised.
- Recovery retry simulation returns to Garage; sample data is not deleted.
- Browser viewport override reset after testing; default final state is Bangla, light, populated Garage.
- Final browser console error query returned no errors.

## Limits

The larger-text switch raises body/input/action text; it is not equivalent to a complete 200% OS font-scale test. Horizontal overflow checks do not prove all content is readable or all touch targets pass. No native TalkBack/VoiceOver, physical camera, Bengali OCR, accepted-crop file generation, multi-page PDF, notification delivery or cloud behavior was tested.

File import/preview, original download and text-summary download exist in the prototype, but an actual uploaded-file/download round trip was not executed in this QA pass. Use only nonsensitive sample files for subsequent checks. Date selection uses the browser's native picker and can follow OS locale rather than app language.

Selected palette contrast comes from the earlier source audit; the complete prototype combination matrix has not been measured. Check all brand/status/focus/disabled pairs before native delivery. No rider usability or retention study has occurred.

## Fixes made during verification

- Explicit hidden-state CSS prevents layout utilities from revealing hidden fields/navigation.
- Per-component unknown baselines avoid inventing chain service history after oil service.
- Correction/deletion derives the affected baseline from remaining latest service records.
- Numeric normalization rejects ambiguous comma decimals and exponent notation.
- Document draft fields are captured before navigation, preserving edits during review/back/crop.
- Manual model names do not display an unrelated motorcycle photo.
- Garage expiry wording derives from the earliest relevant document, rather than always saying seven days.

## Next verification

Check sample image/PDF import and download, direct browser Back with dirty forms, record-edit/delete UI cases, valid/invalid optional document dates, all accent combinations, app-only view at device sizes, and actual 200% font scaling. Native implementation needs its own data integrity and platform verification. See DESIGN-SPEC.md for missing multi-page document states and capability boundaries.
