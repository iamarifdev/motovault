# Continue here: MotoVault design v1.0

Read this file first in a later chat. This is workspace documentation, not an update to cross-session memory.

## Current state

Date: 2026-10-14. User requested a neutral whole-app audit, a deeper visual-system review, corrected the audience to easy/modern Bangla first and English second, then requested a consistent end-to-end reference prototype.

Delivered: interactive local prototype with 25 stable screen IDs, Bangla/English, light/dark and three accent demonstrations; shared tokens/components; sample journeys and adverse-state controls. Proposal is ready for review, not approved for native implementation. Production Kotlin/Swift files were not edited.

## Canonical files

1. `design/archive/prototype-v1/index.html`: workbench and app shell.
2. `design/archive/prototype-v1/styles.css`: palette, typography, spacing and component roles.
3. `design/archive/prototype-v1/app.js`: screen registry, bilingual copy, sample state and interaction contracts.
4. `design/archive/prototype-v1/DESIGN-SPEC.md`: rationale, tokens, screen map, acceptance and limitations.
5. `design/archive/prototype-v1/QA.md`: exact verification completed and remaining gaps.
6. `design/archive/prototype-v1/verify.cjs`: lightweight source-level smoke checks.

Origin: `DESIGN-IS-2026-10-14/01-evidence.md`, `05-visual-system-review.md`, `06-bangla-first-revision.md`. Treat the earlier 16/30 as a provisional score of the examined English-first app, not a score of this prototype.

## Open and verify

From the repository root:

```sh
rtk run python3 -m http.server 8876 --bind 127.0.0.1 --directory design/archive/prototype-v1
```

Open `http://127.0.0.1:8876/#garage` or `#welcome`. For product-only mobile review, use `http://127.0.0.1:8876/?mode=app#garage`. If the port is occupied, reuse the current prototype server or choose another free port. Sandbox loopback binding may need normal tool escalation. Direct `index.html` opening can work, but HTTP preview is the reliable reference.

```sh
rtk run node --check design/archive/prototype-v1/app.js
rtk run node design/archive/prototype-v1/verify.cjs
```

Data key: `motovault-design-v1.0`, browser-local sample data. Reset demo restores sample data/preferences and opens Welcome. Populated restores sample records; Empty records removes sample records/documents; next-save-fails affects one subsequent save; Loading and Restore error are simulations. Demo date is 2026-10-14.

## Preserve decisions

- Bangla first, complete English secondary. Avoid bilingual duplicates on every label.
- Product navigation: আমার বাইক / কাগজপত্র / রেকর্ড. 'রেকর্ড' was chosen for combined fuel/service events; validate with riders.
- Noto Sans Bengali is the prototype candidate, not a final native font approval. Internet is needed for first Google Fonts load; do not claim offline font packaging.
- Compact real-bike identity; practical actions first. No overlapping document cards or guessed health scores.
- Unknown baselines per component, integrated odometer updates and direct correction.
- Preserve original document files; draft-aware navigation; uncertain quality warnings allow user judgment.
- No false claims about cloud backup, official document validity, reminder delivery or implemented OCR.
- Workbench sidebar/annotations are review tools and do not ship inside the native app.

## Next work

First obtain feedback on this concrete visual/flow proposal. Incorporate it directly into the reference, update the change log and rerun affected checks. Do not implement Kotlin based on an unreviewed color/font choice.

Before calling the document journey complete for native delivery, implement the S23 front/back/multi-page contract and S25 original viewer with real page rendering; verify actual crop/rotation generation, S24 permission-denied recovery and HEIC support. S23/S25 currently show sample page content, not real PDF processing. Add further native-specific states under new stable IDs, preserving existing routes.

Native entrypoint to verify again: `composeApp/src/commonMain/kotlin/com/motovault/app/App.kt` → `ui/MotoVault()`. Likely mapping: Appearance.kt for tokens; Onboarding.kt for setup; OwnershipForms.kt for records; DocumentFlow.kt/DocumentWallet.kt for documents; VaultSession.kt for recovery. Re-read actual callers and state contracts before edits.

Implement approved work in small phases: bilingual tokens/copy → onboarding/garage → logging/correction → document flows → reminders/recovery → native accessibility/device QA. Keep data migration, service-baseline integrity and original preservation as explicit acceptance checks.

## Change log

- v1.0: initial Bangla-first reference. Browser QA discovered and prompted fixes for retaining document field edits, baseline-per-component honesty and latest-record correction derivation. Core browser tests and viewport checks are tracked in QA.md.
