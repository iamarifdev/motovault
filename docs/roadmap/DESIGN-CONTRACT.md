# MotoVault v3 design contract

Active reference: [app-ui-v3](../../design/app-ui-v3/README.md), as supplied by the
owner and already applied in commit `50eda3a`. New features extend this design.
The approved exception is the existing native bike-brand logos during brand
selection. The source fingerprint is [design-reference.json](design-reference.json).
It detects reference drift; matching hashes do not establish native visual parity.

## Navigation and placement

Keep Home/গ্যারেজ, Vault/কাগজপত্র, History/ইতিহাস and Me/আমি with the central add
action accessible from every tab. The urgent next task and quick actions stay
above the bike image. Avoid adding a top-level tab for each new feature.

| Planned feature | V3 entry point | Extension |
| --- | --- | --- |
| Reminders | Home NextUp, Vault expiry, Me switches | Correct settings, permissions, snooze and targeted deep links |
| Privacy | Me and document detail | Privacy sheet, unlock boundary and selected-document inspection |
| Local/cloud backup | Me | Backup status card, archive review/restore sheets and native file actions |
| Bangladesh maintenance | Onboarding, gauge detail, service sheet | Source/manual, baseline certainty and specific care actions |
| BRTA renewal | Vault document detail | Checklist, official verification, new expiry and receipt review |
| Cost/budget insights | History summary/timeline | Period/category chips, cost details and expense sheet |
| Photo/share/voice capture | Existing add menu and entry sheets | Native capture feeds the same reviewed draft |
| Multiple bikes | Home bike header and Me | Selector sheet, add and archive using native brand logos |
| Workshop/repair/warranty | Service sheet and expanded history | Optional details, recent workshop and linked repair timeline |
| Handover/passport | History | Select, redact, preview and native share |
| Widget | Me widget setup | Real native host widgets with reviewed deep links |
| Seasonal/breakdown help | Home care card and Help sheet | Source-dated checklist, selected contacts and deliberate location share |
| Rider earnings | Opt-in Me setting and History filter | Optional quick-entry and work summary, without changing default navigation |

Before implementing a new visual flow, add its interactive states to v3 and
record a prototype walkthrough alongside the task. Existing camera/share/voice/
widget mocks are interaction references, not proof of native functionality.

## Tokens and assets

- Inspect `tokens/sky-tokens.json`, `tools/build-tokens.mjs`, generated CSS/TS and
  the current Sky Kotlin token mapping. Respect the generator; do not edit generated
  constants as independent design sources.
- Reuse existing spacing, corners, surfaces, border treatment, button/chip states,
  status colors and sheet geometry. Brand accents follow the selected bike and
  remain separate from warning/danger meaning.
- Preserve the nine native brand logos, existing photo assets and correct aspect
  ratios. Source/provenance and catalogue availability do not prove mechanical
  compatibility or a model's maintenance schedule.
- If a new token or pattern is needed, record why the existing one fails, extend
  its source and verify both prototype and native consumers. Do not replace the
  visual system with generic default components.

## Typography and language

- Follow `src/styles/base.css`: Anek Bangla for Bangla/body text and Barlow
  Condensed where the prototype uses its display face. Preserve intended real
  weights, line-height and role hierarchy in the native font registrations.
- No letter spacing on Bangla. Respect the prototype's 12 px floor by visual
  role, with native scaled equivalents and accessible scaling rather than a
  literal px-to-sp assumption.
- Verify normal and 1.3 text scales plus the device accessibility setting. Allow
  labels to wrap; do not hide actions or truncate essential numbers.
- Use natural Bangladeshi Bangla, existing terminology, lakh grouping and BDT.
  Input accepts Bengali, Latin and Arabic-Indic digits independently of the
  chosen display digit style. Normalize through the shared parser.
- Monetary precision/rounding belongs in the domain contract, not formatting.
  Dates have explicit local-time behavior; official fee/price/source freshness
  is visible when relevant.

## Input and state behavior

- Keep taka-first fuel entry, remembered confirmed prices, date chips, parts
  chips, recent workshop/pump suggestions and optional advanced detail sections.
- Numeric input uses the established in-app keypad and visible Save placement.
  New text/voice/native-auth fields can use the appropriate platform control;
  keyboard, safe area and bottom navigation must not obscure the primary action.
- Reuse content/tall sheets, draggable handle, fixed header, scrolling body and
  fixed action area. Inspect `components/Sheet.tsx` for the current behavior;
  avoid dismissing a destructive operation or silently losing a reviewed draft.
- Confirm OCR, paste, voice and estimated mileage explicitly. Reject invalid
  data close to its field. Native permission denial has a usable manual path.
- Success follows a durable save. Existing quick saves keep lightweight feedback
  and durable Undo. Encryption/export/restore can show genuine bounded progress
  because they are longer operations; never fake a progress percentage.
- Include empty, incomplete, stale, loading, denied, offline, cancelled, failed,
  saved, Undo and restored states in each relevant prototype extension.

## Motion and haptics

Reference `src/lib/motion.ts`, `components/Sheet.tsx`, `components/Toast.tsx`,
`components/Gauge.tsx`, `components/TabBar.tsx` and the current native `ui/Motion.kt`.
Page/expansion/sheet timings come from Sky; v3 adds gesture springs, gauge sweeps,
count-ups and list transitions. The captured reference uses sheet spring
420/40/0.9 (stiffness/damping/mass), press 600/32, list 500/38, marker 520/36,
and a 0.6-second gauge sweep. These are reference parameters, not proof that a
different rendering engine feels identical with the same raw numbers.

Reuse the established timing/gesture intent and judge native behavior with
recordings on a device. Reduced motion must follow both the app and OS setting,
remove nonessential sweeps/count-ups/translations and preserve visible state
changes. Haptics remain optional and are not the only confirmation or error cue.

## Design acceptance matrix

For every new visible feature, record relevant normal, empty, denied, failure,
offline and success states. Cover light/dark/system appearance, both digit
styles, normal/large text, reduced motion, native back/gesture navigation,
TalkBack/VoiceOver, keyboard/safe-area behavior and portrait scrolling. Shared
components require all nine accent contrast checks; full-app screenshot
coverage can use a documented representative state matrix plus targeted
extreme contrasts rather than pretending every combination was observed.

Keep screenshots and recordings separate: a still cannot establish motion.
Compare prototype and native with the same synthetic data and record any
intentional difference. Reports identify platform, OS/device, build revision,
reference manifest and fixture. A browser prototype review does not satisfy a
required native or physical-device check.
