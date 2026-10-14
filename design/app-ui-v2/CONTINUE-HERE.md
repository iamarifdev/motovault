# Continue here · app UI v2

Canonical proposed app design: `design/app-ui-v2/`. v1 remains in `design/archive/prototype-v1/` for history. This file is a workspace handoff, not a cross-session memory update.

User correction: the basic prototype did not preserve the stronger existing logo/model selection. Keep native strengths, keep the improved flow, and design UI suitable for direct native application. Add common services/replacement parts, oil categories/details, prices throughout financial contexts, optional automatic fuel/workshop position and easy/smart input. Let the user select a better Bangla font.

Read in order: UI-SPEC.md → QA.md → APPLY-MAP.md. Edit source: index.html, ui-v2.css, ui-v2.js; inherited base: styles.css, app.js. Native catalogue provenance: catalogue.js and build-catalogue.py.

Preview:

```sh
rtk run python3 -m http.server 8877 --bind 127.0.0.1 --directory design/app-ui-v2
```

- `http://127.0.0.1:8877/#bike`: nine original logos.
- `#models`: all 78 native models/photos, search/manual fallback.
- `#service`: multi-item service bill.
- `#fonts`: four Bangla fonts applied live across the reference.
- `?mode=app#garage`: product-only view without review chrome.

Data key: `motovault-app-ui-v2.0`; fixed reference date: 2026-10-14. All amounts/places are sample records. No real GPS is requested. Camera/OCR/notifications still need native implementation.

Check source:

```sh
rtk run node --check design/app-ui-v2/ui-v2.js
rtk run node design/app-ui-v2/verify.cjs
```

Current scope: 35 stable screen IDs, 30 selectable service/part/fluid entries, 78 model records and nine original logos. The user selected Anek Bangla. The native app now bundles the variable font and its SIL Open Font License; verify rendering, weights, Bangla conjuncts, and large text on Android and iOS.

Keep: native logos/catalogue/photos, existing palettes, Bangla-first/English-second, itemized money, unknown price/baseline, explicit draft/review, source/accuracy-aware optional location. Do not assign current market prices or service intervals from generic oil categories. Preserve originals and omit private expenses/coordinates from inspection/sharing by default.

Native contracts: `RideLog` now serializes itemized services, oil metadata, fuel/service GPS, receipt data, and document-fee events through the Room ledger payload. The Room database is schema v3 with the v2→v3 `autoLocation` migration. New serialized `RideLog` fields use defaults for older payloads.

Native progress: the approved garage/setup flow, catalogue and brand palettes remain native; shared UI uses Anek Bangla and Bangla-first copy. Itemized services include common Bangladesh parts/services, oil metadata, prices, optional receipt images, optional one-shot GPS and safe schedule links. History edits fuel and itemized service visits while preserving IDs and validating chronology/baselines. Document renewal fees are separate entries. The v2 service passport includes itemized service amounts while excluding fuel, document expenses, receipts and exact locations. Older v1 passport files remain verifiable. See `APPLY-MAP.md` for proof boundaries.

Next: run the built app on Android and iOS devices to verify Anek Bangla rendering/weights/conjuncts, large text, screen readers, Room v2→v3 migration, real GPS permission/denial/disabled/timeout cases, receipt selection, record edit flows and PDF page layout. Use QA.md for current runtime evidence; source/build success alone does not establish physical-device behavior. Keep the screen IDs and native brand/logo/catalogue behavior.
