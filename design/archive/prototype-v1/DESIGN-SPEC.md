# Bangla-first ownership companion · v1.0

Status: proposed interactive design for review. Native app code has not been modified. This specification, styles.css and app.js form one reference package. Last updated: 2026-10-14.

## Purpose and audience

Native Bangladeshi motorcycle owners, including riders with limited English and technical confidence. The app answers: what needs attention, what did I spend/do, and where are my papers? It should save time and reduce uncertainty rather than maximize time in the app.

Bangla is the primary/default language. English is a complete secondary choice. Keep model names, official identifiers and original document content unchanged. All user-entered records are owner claims, not official verification.

## Chosen direction

A calm ownership tool with a compact motorcycle identity, a task-first garage, readable document rows and easy modern Bangla. Preserve the existing neutral/brand palette and real model photography. Drop showroom-sized daily heroes, tiny editorial labels and overlapping documents.

References used: current native source and repository screenshots from the audit. This is an evolution of MotoVault, not a competitor imitation. Historical screenshots are reference only, not proof of current behavior.

## Information architecture

Three permanent destinations:

1. **আমার বাইক / My bike:** bike identity, current reading, urgent tasks, quick fuel/service entry and maintenance timing.
2. **কাগজপত্র / Documents:** readable documents, expiry status, capture/import, originals and a focused show-documents view.
3. **রেকর্ড / Records:** fuel/service history, search/filter, correction and a service summary.

Settings are secondary from Garage. Capture/setup/inspection are focused flows with explicit Back actions, not imitation tab bars. The side screen index and English annotations belong to the design workbench, not the shipped app.

## Typography

Prototype candidate: Noto Sans Bengali, weights 400/500/600/700, with system sans fallback. Loaded from Google Fonts in this version; first font load requires internet. It was reported loaded in browser QA. Bundle a licensed font in the native app before implementation approval. Source/license reference: [Google Fonts Noto Sans Bengali](https://github.com/google/fonts/tree/main/ofl/notosansbengali).

| Role | Size | Weight | Line height |
|---|---:|---:|---:|
| Screen title | 28px | 700 | 1.4 |
| Section title | 20px | 600 | 1.5 |
| Row/component title | 17px | 600 | inherited 1.65 |
| Body, input, action | 16px | 400/600 | body 1.65, action/input 1.5 |
| Functional secondary | 14px | 400/500 | 1.6 |
| Status badge | 13px | 500 | 1.6 |
| Metric value | 26px | 600 | 1.4 |

Latin model titles use an intentional system sans pairing. Bangla is never condensed or letter-spaced as uppercase display text. Workbench labels may be smaller; they are outside the product. Native implementation translates these role decisions into sp and tested font metrics, not literal CSS pixels.

## Geometry

Spacing tokens: 4/8/12/16/24/32. Product screen gutter: 24px, narrowing to 20px and then 16px. Section gap 24; card inset 16; related detail gaps 8/12. Radii: 12 controls, 16 cards, 24 larger containers. The 28px outer preview frame is workbench chrome.

Primary/secondary action minimum height: 52px; icon action 44px. Text may wrap. Do not shrink Bangla to fit fixed English widths. Use native touch targets and content-driven height. Product preview is 440px wide on desktop and reflows to narrow screens; it is not an artificial screenshot canvas.

## Palette

| Semantic role | Light | Dark |
|---|---|---|
| Canvas | #F5F4EF | #101215 |
| Surface | #FFFFFF | #171A1E |
| Raised | #E9ECF0 | #22262C |
| Primary text | #181C22 | #F1F2F3 |
| Secondary text | #535E6B | #9DA5AF |
| Dividers | #D4D9DF | #353B44 |
| Healthy | #236D4F | #7BD3AB |
| Warning | #805817 | #EFC277 |
| Error | #AD2938 | #FF929A |

Accent variants demonstrated: Yamaha #315ABE/#88A8FF; KTM #AA4700/#FF8C42; CFMOTO #00717A/#69D2DB. Native completion must carry the remaining existing brands through the same role contract. Accent belongs to primary actions, selected states and links; semantic urgency does not change with manufacturer.

Use labels alongside status color. 'Within entered expiry' does not mean authentic. No warning styling for normal maintenance. Dark mode is explicit in the prototype; the native app must retain its existing System/Light/Dark contract. Inspection uses a deliberately light view, then restores the prior appearance.

## Component contracts

| Component | Required behavior |
|---|---|
| App header | Compact brand, language choice. Language switching must preserve committed data; prototype prevents switching mid-form rather than silently losing entries. |
| Page head | One clear task heading, short supporting sentence, Back in focused flows. |
| Bike identity | Small real photo, unmodified model name, current reading, update action. |
| Primary action | Explicit verb, 52px minimum, no disabled mystery. Validation explains missing input. |
| Field | Visible associated label, help/error IDs, logical keyboard, required/optional distinction. |
| Status | Text plus semantic color, derived from entered data. Unknown is a neutral state. |
| Maintenance row | Time/distance estimate and unknown-baseline handling per component. No mechanical health claim. |
| Document row | Type, identifier, expiry/status and open affordance without overlapping content. |
| Record row | Date, mileage, amount and direct edit. Filters remain while moving between destinations. |
| Confirmation | Concrete consequence, a safe keep-working option, only for meaningful discard/removal. |
| Toast | Short success, polite announcement; errors stay close to the form. |
| Recovery | Preserve existing data, retry and guidance. Never launch fresh setup after failed restoration. |

## Screen contracts

Each ID remains stable for tickets, screenshots and future Compose mapping. URL hashes are direct review links.

| ID | Hash | Task / acceptance |
|---|---|---|
| S01 | #welcome | Understand value in Bangla; choose English if needed; start without service knowledge. |
| S02 | #bike | Brand/model selection, search, visible selection and manual model entry. Demo images cover four models; other brands use manual fallback. |
| S03 | #setup | Model + current reading only. Unknown baseline remains unknown. |
| S04 | #garage | Urgent expiry and quick entry precede detailed maintenance. Compact identity. |
| S05 | #fuel | Date picker, litres, expense and full-tank explanation; inline errors, retry-safe saving. |
| S06 | #service | Select component and log actual reading; higher reading updates Garage in the same action. |
| S07 | #components | Explain schedule basis; list known and unknown components. |
| S08 | #schedule | Set distance/time and optional known last-service baseline. |
| S09 | #history | Persistent filter/search and direct correction. Actions before long history. |
| S10 | #documents | Readable list; add/show actions; owner-entered expiry disclaimer. |
| S11 | #capture | Choose type, import file or enter camera flow; no hidden navigation discard. |
| S12 | #review | Manual details, optional dates, owner confirmation; back/crop preserve draft. |
| S13 | #document | Original/metadata distinction; edit and explicit removal. |
| S14 | #inspection | Switch document, show large readable preview, exclude private ownership logs. |
| S15 | #passport | Review included/excluded data; download an honest sample summary. |
| S16 | #settings | Language, appearance and accent, with truthful local-storage guidance. |
| S17 | #reminders | Opt-in, timing and permission-denied explanation. No actual OS permission in browser demo. |
| S18 | #recovery | Failed restore state; retry demo and diagnostic/help path without deleting data. |
| S19 | #odometer | Update reading; require explanation for a lower correction. |
| S20 | #saved | Success-state reference; real forms use contextual success plus next destination. |
| S21 | #camera | Simulated preview and uncertain quality warning with usable shutter/import alternative. |
| S22 | #crop | Accessible edge range controls and keep-full-image option; preview geometry only. |
| S23 | #pages | Front/back and multi-page ordering, move-earlier and explicit removal. Sample page addition demonstrates the interaction. |
| S24 | #cameraDenied | Camera permission denied; import remains available and settings guidance is explicit. |
| S25 | #original | Original preview with page selection, enlargement and actual original download when a sample file was uploaded. |

## End-to-end journeys

- First use: S01 → S02 → S03 → S04; optional S08 later.
- Fuel: S04 → S05 → S09; correct via S09 → S05 → S09.
- Service: S04 → S06 → S09; current reading and affected component baseline update together.
- Documents: S10 → S11 → S21 or file import → S12 ↔ S22/S23 → S13 → S14/S25; denied camera has S24 → import/settings guidance.
- Reminder: S10 → S17; native app requests permission only after explicit opt-in.
- Summary: S09 → S15; review before any sharing.
- Restore failure: S18 → retry → S04 on actual success; persistent failure must keep recovery visible in the native app.

## Input and record rules

Accept Bangla and Latin digits, including valid grouped numbers; reject ambiguous comma decimals, exponent notation and negatives in these fields. Dates use native browser selection in the prototype; OS/browser may show an English picker. Displayed dates use the chosen locale. Demo clock is fixed at 2026-10-14 for deterministic expiry examples.

Service correction derives the affected baseline from the latest service date/reading, not whichever record was edited last. If no prior service remains, mark that component baseline unknown. Updating one component does not create a baseline for another. Meter corrections do not silently rewrite older records.

Draft data survives movement inside capture/review/crop. Leaving a dirty flow asks before discarding. Form saves retain entries on injected failure. Browser storage is sample-only; quota failure is explained.

## Scope and honesty

Implemented prototype behavior: screen navigation, local sample records, digit normalization, forms/validation, save failure/retry, corrections, filters/search, file preview, manual document details, preview rotate/crop controls, themes/language, sample summary download and recovery simulation.

Not implemented or proven: real CameraX/iOS camera, quality analysis, OCR, accepted-crop image generation, HEIC conversion, actual multi-page PDF rendering or image assembly, push notifications, PDF passport/hash/QR verification, encrypted persistence, cloud backup, authentication or sync. The prototype communicates the visual/behavior contracts, including page management and permission denial; those platform capabilities require separate native implementation and QA. Page selection displays labelled sample content, not distinct rendered PDF pages.

Uploaded sample images/PDFs may be stored as browser data URLs. Do not enter real personal documents. Prototype crop/rotation metadata does not produce a transformed file. Summary export is plain text, not a forged PDF or official credential. Reminder switch saves preference only.

## Approval and continuation

This is the concrete proposal for visual/flow review. Native changes begin only after the direction is accepted. Record approved changes here and in CONTINUE-HERE.md; keep IDs stable. Validate with Bangla-speaking riders and native large-text/screen-reader/device tests rather than treating browser QA as native proof.
