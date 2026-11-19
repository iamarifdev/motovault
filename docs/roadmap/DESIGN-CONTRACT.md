# MotoVault v4 design contract

Active reference: [app-ui-v4](../../design/app-ui-v4/README.md) (commits `5118695`,
`da622bf`, `3754157`), built on [app-ui-v3](../../design/app-ui-v3/README.md). The spec is
the TSX and CSS code in `design/app-ui-v4/src/`. `CHANGELOG-V4.md` and the prototype's
DevPanel text are summaries and disagree with the code in places; when they differ, the
code wins. The port plan, per-screen checklists and open owner decisions are in
[V4-PORT-PLAN.md](V4-PORT-PLAN.md). The approved exception is the existing native
bike-brand logos during brand selection. The source fingerprint is
[design-reference-v4.json](design-reference-v4.json) (v3 history: [design-reference.json](design-reference.json)). Matching hashes do not establish native visual parity.

## Navigation and placement

Keep four tabs, গ্যারেজ, ডকুমেন্ট, রেকর্ড and প্রোফাইল, with the central add action on
every tab. Tapping `+` opens Quick Add; holding it for 360ms opens a shortcut pill
(তেল, সার্ভিস, মিটার রিডিং) over a scrim. Avoid adding a top-level tab for each new feature.

| Area | v4 entry point (code) | Shape |
| --- | --- | --- |
| Brand wash | `App.tsx`, `base.css`, `app.css` | Radial wash on all four tabs and sheets when a bike exists; never during onboarding or add-bike |
| Garage | `Home.tsx`, `BikeShowroom.tsx` | Showroom (name, health pill, swipe carousel, odometer, stats strip), 4 quick actions, merged jobs list (wear and renewals), seasonal pill |
| Onboarding | `Onboarding.tsx`, `catalog.ts` | Brand grid with live preview, model family rows with inline versions, custom bike card, odometer with camera and voice, 3 service-timing chips, skip |
| Entry sheets | `FuelSheet.tsx`, `ServiceSheet.tsx`, `ExpenseSheet.tsx`, `OdometerSheet.tsx` | Header camera/voice actions, compact amount card, chip line, pickers that swap sheet content, odometer confirm before save |
| Documents | `Vault.tsx`, `DocCard.tsx`, doc sheets | Overlapping wallet stack, BRTA card template for licence and registration, add flow type, photo, expiry |
| Records | `History.tsx`, `CostInsights.tsx` | Cost card with chips and stacked bar, work-ledger card second, sticky months, kv details, swipe reveals delete, jump to top |
| Profile | `Me.tsx`, `SettingsSheet.tsx` | Profile card plus three grouped lists opening focused settings sections, privacy, backup and cloud sheets with pinned footers |

Before implementing a new visual flow, add its interactive states to `design/app-ui-v4`
and record a prototype walkthrough alongside the task. Existing camera/share/voice/
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
- Use natural Bangladeshi Bangla (ব্যবহারিক চলিত বাংলা), existing terminology, lakh grouping and BDT.
  Copy must follow the standard of leading Bangladeshi mobile apps (bKash, Pathao, Nagad) using respectful
  "আপনি" tone-of-voice. Universal tech and biker loanwords written in Bengali script are standard and required
  over literal dictionary translations (e.g., "প্রোফাইল" never "আমি"; "রেকর্ড" never "ইতিহাস"; "ফুল ট্যাংক"
  never "ট্যাংক পূর্ণ"; "সার্ভিস চার্জ / মজুরি" never "শ্রম"; "টাচ ভাইব্রেশন" never "হালকা কাঁপুনি"; "স্ক্যান"
  never "পড়া হচ্ছে"; "ফিটনেস সার্টিফিকেট" never "ফিটনেস সনদ"; "ব্যাকগ্রাউন্ড" never "অ্যাপ পেছনে গেলে").
- All UI microcopy across Compose Multiplatform must be declared in string resource tables
  (`composeResources/values/strings.xml` for English base and `composeResources/values-bn/strings.xml` for Bengali).
  Raw hardcoded user-facing string literals in Composable functions are prohibited.
- Support runtime language selection (AppLanguage.BN and AppLanguage.EN) with instant UI updates without
  requiring an app restart.
- Input accepts Bengali, Latin and Arabic-Indic digits independently of the
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
Page/expansion/sheet timings come from Sky; v3 and v4 add gesture springs, wear-bar
sweeps, the showroom carousel spring, the shortcut pill, count-ups and list transitions. The captured reference uses sheet spring
420/40/0.9 (stiffness/damping/mass), press 600/32, list 500/38, marker 520/36,
and a 0.6-second sweep. These are reference parameters, not proof that a
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
