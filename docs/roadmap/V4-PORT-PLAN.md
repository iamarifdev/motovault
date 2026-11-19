# MotoVault v4 literal port plan

Status: draft for owner review (2026-11-16). Nothing in this plan has been implemented.

Source of truth: the code in `design/app-ui-v4/src/` (TSX and CSS) at commits `5118695`,
`da622bf` and `3754157`. Compose target: `composeApp/src/commonMain/kotlin/com/motovault/app/`.
In this file `ui/` means `composeApp/.../app/ui/` and `v4/` means `design/app-ui-v4/src/`.

Baseline for the port: git `HEAD` (`3754157`), which is the v3 port. The uncommitted Gemini
attempt in the working tree is treated as a parts bin, not as a starting point (see Phase 0).

---

## 1. Why the Gemini attempt drifted

Six read-only audits compared v4, HEAD and Gemini's working tree screen by screen. The main
patterns:

- **It worked from prose, not code.** `CHANGELOG-V4.md` and the DevPanel text disagree with the
  v4 code in several places. Examples: the changelog says the fuel price is no longer required,
  but v4 `FuelSheet.tsx` still blocks save without a price. Gemini's design contract says the
  work-ledger card is "always top" and rows are "swipe-left-to-delete"; in v4 the card is second
  (after the cost card) and swipe only reveals a delete button that must be tapped.
- **It invented features v4 does not have.** History search and CSV copy, a 6-month cost chart,
  KPI strip, ring-gauge strip on Home, a status banner, a quick-add bike form in the switcher,
  fixed pump-brand chips, service combo presets, a due banner, a 2-column model grid, a version
  bottom sheet, a language/theme toggle in onboarding, an "অন্য খরচ" tile in Quick Add.
- **It dropped things v4 keeps.** The Home quick-action row, the stacked cost bar, sticky month
  headers, the kv detail table, jump-to-top, the 3 grouped settings lists, the 7-section
  settings sheet, the pinned sheet footers, the inline last-done editor.
- **It changed values.** Sheet radius 22 (v4: 20), title 22sp (v4: 24), linear wash at 8.5%
  (v4: radial at 18%), wash shown during onboarding (v4: never), compact keypad everywhere
  (v4: only Fuel and Expense).
- **It left data bugs.** Document save can delete the old document of the same kind (losing
  pages and renewal history), sample number/holder values get saved as real, service edit fails
  for any visit with engine oil or a custom line and turns quotations into paid visits.
- **Its roadmap evidence claims verification that does not match the code** (P17-01..07 evidence
  files, P17 tasks set to `in_review`).

---

## 2. Rules for a literal port

Every phase follows these rules. They exist to stop the same drift.

1. **Spec = v4 code only.** Read the TSX component and its CSS selectors. Do not use
   `CHANGELOG-V4.md`, `DevPanel.tsx` text, Gemini's `DESIGN-CONTRACT.md` table or the old P17
   tasks as a spec.
2. **Units.** 1 CSS px = 1 dp. Font sizes: 1em = 16sp × text scale (1 / 1.15 / 1.3).
   `color-mix(in srgb, A p%, B)` = linear mix in sRGB, implemented once in `Appearance.kt`.
3. **Nothing extra.** If an element is not in v4, it goes. The only exceptions are the
   owner decisions in section 10.
4. **Nothing missing.** Each phase below has a checklist. A phase is done only when every line is
   checked against the running prototype.
5. **Copy.** Bangla is copied exactly from the TSX into `values-bn/strings.xml`. English is
   written new and reviewed. Every string goes through resources (AGENTS.md localization rule).
6. **Prototype-only things are never ported:** DevPanel, demo states, sample message chips,
   "প্রোটোটাইপ v4" footer, reset-demo on Me, the demo map row, "নমুনা · ডেমো" (see D3).
7. **Same data, side by side.** Load the same fixture (`public/p01-fixtures.json`) in the
   prototype and the app. Compare screenshots at the same logical width (440 wide) in light and
   dark, Bangla, for every state in the checklists.
8. **Size rule.** No file over 400 lines, no function over 80. Each phase lists its split.
9. **One phase, one reviewable change set,** with the gates from
   `docs/engineering/verification.md` for its change class.

---

## 3. Phase 0: Baseline and housekeeping

Needs owner approval (D1) because it touches changes this session did not make.

| Step | Action |
| --- | --- |
| 0.1 | Commit the whole Gemini working tree to a parking branch `wip/gemini-v4-attempt`. Nothing is lost. |
| 0.2 | Create `feat/v4-literal-port` from `HEAD` (`3754157`). |
| 0.3 | Bring over only the salvage list below. Whole files (icons, catalogue, images, `SpokenNumberParser`, doc pointers) come over in Phase 0. Partial pieces (Sheet, Keypad, Appearance, Toast, navigation, routes, DocumentArt, HistoryLogCard, Fuel validation, Service edit loading) are taken from `wip/gemini-v4-attempt` in the phase that rebuilds that file, so Phase 0 stays a clean HEAD baseline. |
| 0.4 | Keep Gemini's small pointer edits (v3 → v4) in `AGENTS.md`, `ai-workflow.md`, `verification.md`, `ARCHITECTURE-CONTRACT.md`, `roadmap/README.md`, `tools/roadmap.py`, but fix the AGENTS.md wording "radial shortcuts" to "long-press shortcut pill". |
| 0.5 | Rewrite the v4 table in `DESIGN-CONTRACT.md` from this plan. Regenerate `design-reference-v4.json`. |
| 0.6 | Re-create the P17 tracker tasks from the phases in this plan (JSON edit with an activity note). Do not bring Gemini's P17 evidence files. |
| 0.7 | Capture reference screenshots of every prototype state in the checklists (kept locally, not committed, unless the owner wants them in evidence). |

### Gemini salvage list

Take these. Everything else from the parking branch stays there.

| Item | Take | Fix while taking |
| --- | --- | --- |
| `tools/build-v3-icons.mjs` + regenerated `ui/V3IconPaths.kt` | 19 new lucide icons, ellipse support | Add `pencil` and `delete` (keypad backspace) if missing |
| `ui/BikeCatalogueFamilies.kt` | 9 brands, 56 families, 94 variants (script-checked against v4) | Fix 2 cut KTM labels (lines 193-194) |
| `ui/BikeCatalogue.kt` | 16 new variant entries | Fix 9 display names to v4; make `legacyModel()` look up by id, not name |
| New drawables (`model_*`, `brta_*_bg.jpg`) | All | Rename `*_v4.png` to the original names, delete the orphaned `.jpg`. Then copy all 61 differing images from `v4/../public/bikes/` (v4 re-cropped them) |
| `domain/capture/SpokenNumberParser.kt` + test | All | Normalise the Bangla letter য় (two code forms) so words like পঁয়ত্রিশ parse |
| `ui/components/Sheet.kt` | `actions` slot only | Gap 2dp, not 4 |
| `ui/components/Keypad.kt` | `compact` mode (46/6), press scale .92 | Used only by Fuel and Expense |
| `ui/Appearance.kt` | Dark ok/warn/err values, `wash`/`sheetWash` fields, dark segmented pill colour | Fix wash percentages (section 4.1) |
| `ui/capture/VoiceCapture.kt` | `anyAvailable` | none |
| `MotoVault.kt` | `previewBrandId` state | Precedence must be override, then preview, then ride |
| `ui/components/Toast.kt` | Swipe-to-dismiss | Threshold 80dp, elastic .6, spring back |
| `ui/navigation/BottomNavigation.kt` | `combinedClickable` long-press wiring, `onQuickAction` | Layout rebuilt (4.7) |
| `ui/VaultSheetHost.kt` | Routes `serviceEdit`, `docEdit`, `settings`; doc delete with Undo | Pass ids, not object snapshots; add `section` to `settings` |
| `ui/components/DocumentArt.kt` | `Res.drawable.brta_*` wiring, ExpiryChip palette refactor | Card body rebuilt (Phase 5) |
| `ui/history/HistoryLogCard.kt` | The file split and `serviceEdit` entry | Content rebuilt (Phase 6) |
| `ui/fuel/PumpPickerSheet.kt` | `resolveFuelIssue` / `fuelIssueText` / `resolveFuelDate` | Move to `fuel/FuelEntryValidation.kt` |
| `ui/maintenance/ServiceSheet.kt` | Loading existing labour/misc/discount/date/workshop for edit, `dueCatalogKeys` | Domain fix first (Phase 4) |

---

## 4. Phase 1: Foundation (tokens, wash, sheet, toast, controls, navigation)

Everything later depends on this. Do it first and alone.

### 4.1 Tokens (`ui/Appearance.kt`, new `ui/Wash.kt`)
- Dark status colours: ok `#86EFAC`, warn `#FCD34D`, err `#FCA5A5`. Light unchanged
  (`#236D4F` / `#805817` / `#AD2938`).
- `brand-tint` 11% in both modes. `brand-line` 45%.
- Status badges as a typed enum (ok / soon / overdue), not strings. Dark overrides:
  ok bg `mix(#22c55e 22%, #111814)` fg `#86efac`; soon `mix(#f59e0b 24%, #1a150b)` fg `#fde68a`;
  overdue `mix(#ef4444 26%, #240d0f)` fg `#fecaca`.
- `wash` = brand 18% over canvas (dark 30%). `sheetWash` = brand 16% over surface (dark 24%).
- Add a `shadow` token. Scrim .6.
- Optional: line heights title 32.4, head 28, label 21, caption 19.5.

### 4.2 Brand wash
- Screen: radial ellipse 140% × 48% at top centre: `wash` → `mix(wash 45%, canvas)` at 55% →
  `canvas`. Compose `radialGradient` is a circle, so draw it in `drawBehind` with a scale
  transform to get the ellipse.
- Show only when a ride exists **and** onboarding / add-bike is not open. All 4 tabs. 350ms fade.
- Sheets: radial 150% × 300dp at top centre, `sheetWash` → mix 45% at 50% → `surface`.
  Footer stays plain surface.
- In wash mode: showroom card has no background or border; the stats strip becomes a bordered
  surface card (radius 16); sticky month headers use `wash` at 88%.

### 4.3 Sheet (`ui/components/Sheet.kt`, new `SheetDrag.kt`)
- Top radius 20. Max height = screen − safe top − 60. Shadow `0 -10 40 rgba(0,0,0,.25)`.
- Scrim .6 with 250ms fade. Open: spring 420/40/.9. Close: 200ms.
- Handle 40×4, margin 10/4, `line-strong` at 60%.
- Header min height 56, padding 0 8 4 20, 1dp bottom line. Title 24/32.4, weight 600.
- `actions` slot left of the close button, gap 2. Tint icon button (44, brand on brand-tint).
- Body padding 16 20 20, gap 14. Pinned footer padding 10 16 (10 + safe bottom), 1dp top line,
  surface, gap 10.
- Drag to close: from handle/header always; from the body when scrolled to top and the finger
  moves more than 10dp down. Close when offset > 110dp or speed > 650dp/s. Elastic .7.
  Tick haptic. Fix the current px/dp mix-up (sheet moves faster than the finger).
- `compact` mode (Fuel and Expense only): body gap 10, padding 12; footer gap 8, top 8;
  keypad gap 6, key 46.

### 4.4 Toast (`ui/components/Toast.kt`, new `ToastHost.kt`)
- Width fits content, centred, bottom = tab bar + 10, 12dp side margin.
- Light bg `#181C1A`, text `#F1F2F3`, undo `#8FB6FF`. Dark bg `#E9ECEA`, text `#181818`,
  undo `#1E47A8`. Radius 16, padding 12 8 14 16, text 15/600.
- Undo: Undo2 icon 16 + "আনডু", min height 40, weight 700, warning haptic.
- 3dp progress bar, **linear**, 5s.
- Enter y 24, scale .96, sheet spring. Exit y 12, 180ms. Swipe > 80dp to dismiss.
- Draw above sheets. Key by `ToastData.id`. Clear when a sheet opens. Hide while locked.

### 4.5 Shared entry controls (new folder `ui/components/entry/`)
Built once, used by Fuel, Service, Expense, Odometer, Onboarding.
- `ChipLine`: one horizontal scroll row, gap 8, bleeds 20dp past the screen padding, fade 16dp
  left / 24dp right, chips 38 high with padding 0 13, optional 1×22 divider.
- `SegmentedPill` (`.mode-seg`): padding 2, radius 999, surface, 1dp border; buttons min 30 high,
  padding 0 11, 13sp/600; selected = brand fill, on-brand text.
- `EntryAmountCard`: padding 10/14, radius 16, border 2 `line`, surface-2; active = brand border
  and brand-tint. Number 40sp/700 (lh 1.15), unit 24sp/600, blinking caret 3×44 brand.
- `ConfirmPill`, `PickerField` (dashed, min 46, radius 16, 1.5 dashed `line-strong`),
  `OdoRow` (optional +10/+50/+100 slot), `DateChips` (আজ / গতকাল / অন্য দিন that turns into an
  inline date chip), `NamePickerContent` (swaps the sheet content; used for pump and workshop).

### 4.6 Keypad (`ui/components/Keypad.kt`)
- Keys 52 high, radius 12, text 24/600. Backspace = lucide `delete` 24 in text-2.
- Fire on press-down, not release. Pressed scale .92.
- Max 9 characters, 2 decimals. First press on a newly selected field replaces it.

### 4.7 Navigation (`ui/navigation/*`, `VaultTabContent.kt`)
- Tab labels: গ্যারেজ, ডকুমেন্ট, রেকর্ড, প্রোফাইল.
- Tab switch: fade + translateY 10→0, 300ms, curve (0,0,.2,1). Keep one scroll state per tab
  (v4 keeps all 4 panels mounted). Draw surface behind the bottom inset.
- + button: unchanged geometry. New a11y label "নতুন রেকর্ড যোগ করুন (চেপে ধরে রাখলে শর্টকাট)".
- **Long-press shortcut pill** (new `ui/navigation/QuickShortcutPop.kt`):
  - Hold 360ms → success haptic → open. The click after the hold is ignored.
  - Overlay at the screen root (not a column item). Scrim rgba(0,0,0,.24) over everything,
    tab bar included. Tap scrim, Back, or any tab closes it.
  - Pill: radius 999, padding 8, gap 8, surface, 1dp `line-strong`, shadow. Centred,
    bottom = tab bar + 18.
  - Items: min height 40, padding 0 14, gap 6, icon 18, 14sp/600, brand text on brand-tint:
    **তেল** → fuel, **সার্ভিস** → service, **মিটার রিডিং** → odometer. Tick haptic.
  - Enter: opacity, y 16→0, scale .9→1, press spring. Exit: y 12, scale .92, 140ms.

### 4.8 Quick Add (`ui/quickadd/QuickAddSheet.kt`, start from HEAD)
- Title "কী যোগ করবেন?". 2 columns, gap 12.
- Card: min height 140, padding 16, radius 16, 1dp line, surface-2. Icon box 52 (radius 16,
  brand fill, icon 28). Label 18/600, sub 13 text-2.
- Cards: তেল ভরা / "টাকা, ছবি বা মুখে বলে যোগ"; সার্ভিস / "কী কাজ করালেন ও খরচ";
  মিটার রিডিং / "মিটারের ছবি বা মুখে বলুন"; ডকুমেন্ট / "ছবি তুলে মেয়াদ রাখুন" (→ docAdd).
- Paste row: Mic 20 brand, 1.5 dashed brand-line, padding 14, gap 12.
  "মেসেজ পেস্ট বা মুখে বলে যোগ করুন" / "মেকানিকের হিসাব, বিকাশ বা নগদের মেসেজ" → paste.
- Stagger: y 14, list spring, delay .08 + i×.04. Press .96.

### 4.9 Motion, haptics, PrivacyGate, routing, copy
- Springs: sheet 420/40/.9 (damping ratio about 1.03, today .85), press 600/32, list 500/38,
  marker 520/36. Separate haptic types: tick 8ms, success [12,40,18], warning [20,60,20],
  error [30,50,30,50,30] (today success/warning/error all use LongPress).
- PrivacyGate: left-aligned, top 36, lock icon 42, h1 28, v4 copy (no "demo" words).
- Split `VaultSheetHost.kt` (one 349-line function) by area **before** later phases:
  `GarageSheetRoutes`, `EntrySheetRoutes`, `DocumentSheetRoutes`, `SettingsSheetRoutes`
  (settings, privacy, backup, cloud). Add `settings(section)`.
- Move the wash, toast host and shortcut overlay out of `MotoVault.kt` (390 lines today).
- Rename the ~39 strings that still say "কাগজ" to "ডকুমেন্ট" (v4 wording everywhere).
- Optional (D18): split `strings.xml` into per-area files to avoid merge conflicts.

### 4.10 Remove in Phase 1
`Gauge()` in `components/Gauges.kt` (no callers), `Top` / `Heading` / `Glyph` in
`CoreControls.kt` (no callers), `AppearanceButton` if nothing else uses it, ~70 unused imports
and the empty legacy banner in `BottomNavigation.kt`, `quickadd_smart_*` / `quickadd_expense*`
strings (if any were brought over).

### Phase 1 checklist
- [ ] Wash visible on all 4 tabs, absent in onboarding and add-bike, radial shape matches.
- [ ] Sheet radius, title, scrim, handle, footer, drag from body, compact mode.
- [ ] Toast width, colours, linear bar, swipe, above sheets, Undo.
- [ ] Keypad press-down, icon, sizes.
- [ ] Tab labels, tab switch motion, scroll kept per tab.
- [ ] Long-press pill with scrim and Back; 3 items open the right sheets.
- [ ] Quick Add cards, copy, paste row, stagger.
- [ ] Dark mode status colours and badges.

---

## 5. Phase 2: Onboarding and catalogue

### Spec (v4 `screens/Onboarding.tsx`)
- **Shell:** full screen on plain `canvas` (no wash), padding 20, gap 14. Header min 52, gap 14:
  back button (44) when step > 0; on step 0 an X "বাতিল করুন" only when adding a bike, else a
  plain brand bike glyph 26. Middle: 3 dots (h 4, radius 3, gap 6, on = brand). Right "১ / ৩"
  (13sp text-2). Step change: x 28→0 + fade 300ms; exit x −28, 120ms. h1 28/600, lead 16 text-2.
- **Step 0 brand:** "কোন বাইক চালান?" / "ব্র্যান্ড বেছে নিন। অ্যাপের রঙ আপনার বাইকের সঙ্গে মিলবে।"
  3-column grid, gap 10, order Yamaha, Honda, Bajaj, TVS, Suzuki, Hero, KTM, CFMOTO,
  Royal Enfield. Tile padding 14/6, radius 16, border 1.5; logo 52×28; name 14/600 (may wrap);
  "{n}টি মডেল" (n = families). Selected = brand-tint + brand border. Stagger 30ms (spring
  500/38), press .95. Tap = live brand preview for the whole app, reset model, go to step 1.
  Footer caption "আপনার সব তথ্য এই ফোনেই থাকবে…".
- **Step 1 model:** "{Brand} এর কোন মডেল?" / "মডেলটি খুঁজে ট্যাপ করুন।" Search (h 52, r 12,
  1.5 `line-strong`, surface-2, brand border on focus) matches family, variant name or label.
  Vertical list of family rows (r 16, border 1.5): photo well 104×68 r 12, name 17/600,
  "{cc} cc" + "{n}টি ভার্সন" pill (only if > 1 variant). Selected = 30dp brand check +
  brand-tint header. Tap selects family + first variant. If > 1 variant, expands inline (200ms):
  showcase stage 116 high, "ভার্সন বেছে নিন" (11.5/700), chips with `variant.label` only.
  No CC filters, spec pills, year chips. Empty search text. Link "আমার বাইক তালিকায় নেই" opens an
  inline card (name, optional cc default 150, **no year**). CTA "{name} বেছে নিন".
- **Step 2 odometer:** "মিটারে এখন কত কিমি আছে?" Centred number 52/700, unit 20/600. Two small
  buttons in 2 columns: Camera "মিটারের ছবি তুলুন", Mic "মুখে বলুন" ("শুনছে…"). Voice note
  copy (heard / not understood / not available / failed). Camera card: `#0d1210`, r 16,
  dashed reticle, sweeping scan line 1.2s, result "এই কিমি নিন" / "নিজে লিখুন"; keypad hidden
  and CTA disabled while open. "শেষবার সার্ভিস কবে করিয়েছেন?" chips "এই মাসে" | "২–৩ মাস আগে" |
  "মনে নেই" (default মনে নেই). CTA "শুরু করুন" + link "এখন মনে নেই · পরে দেব" (finish with 0 km).
  Full keypad (52), no decimal.
- **Saved:** variant id, name, year = clamp(this year − 2, variant years), cc, odometer.
  "এই মাসে" → last service odo − 300, today − 14 days (estimate). "২–৩ মাস আগে" → odo − 1200,
  today − 60. "মনে নেই" or skip → unknown.

### Work
- Rewrite `ui/onboarding/*` from HEAD. Split: `OnboardingScreen.kt` (scaffold, header,
  transition), `OnboardingDraft.kt` (pure: year, timing → baseline, result; unit tests),
  `OnboardingBrandStep.kt`, `OnboardingModelStep.kt`, `ModelFamilyRow.kt`, `CustomBikeCard.kt`,
  `OnboardingOdometerStep.kt`, `OdometerCameraCard.kt` (dark card around `OdometerPhotoReviewBody`).
- Catalogue: make `BikeCatalogueFamilies` the single source; derive the flat list from it.
  **No ID migration needed:** all 78 stored HEAD ids equal v4 variant ids.
- Custom bike: keep `modelId = null` + `modelName` (do not port v4's `${brand}-custom` id;
  `VaultCreation.kt` rejects unknown ids). Add optional `Ride.cc: Int? = null` (backup schema +
  compatibility fixture), used by the showroom line.
- Voice: see D8.

### Remove
Gemini: header language/theme toggles, `SelectedBikeBanner`, `OdometerPresetStrip`,
`BaselineQuickStrip`, `ExpandableServiceAndManualDetails`, `BikeFamilyCard`,
`VariantPickerSheet`, `CustomBikeSheet`. HEAD: step-2 baseline fields, first-inspection
checklist, manual block, extra Cancel button, `initialStep` / `initialModelId` /
`initialModelCode` params (only the probe uses them; update the probe). Strings:
`onboarding_bike_details`, `_market`, `_emission`, `_change_bike`, `_baseline_*`,
`_manual_*`, `_first_inspection_*`, `_model_year`, `_model_code`, `ride_market_*`, and Gemini's
`_variant_*`, `_custom_*`, `_preset_new`, `_lang_pill_*`, `_theme_toggle_desc`.
Keep `onboarding_last_service_km/date` (used by `OwnershipForms.kt`). Factory manual: D7.

### Phase 2 checklist
- [ ] 3 steps match screenshots (brand grid, family list + inline variants, custom card, odometer).
- [ ] Live brand preview on step 0 and back again on cancel.
- [ ] Camera and voice states, keypad hide, skip link.
- [ ] Saved data matches the mapping above (unit tests in `OnboardingDraft`).

---

## 6. Phase 3: Garage (Home tab and its sheets)

### Spec (v4 `screens/Home.tsx`, `components/BikeShowroom.tsx`)
Screen padding 8/20/28, gap **20**. Zones fade in (y 12→0, delay i×.05). Order: showroom,
quick actions, jobs, seasonal.

- **Z1 Showroom**
  - Top row: name button (18/700 + ChevronDown 16) → bike switcher; under it
    "brand · year · {cc} cc" (12sp).
  - Health pill (right): padding 5/10, 11.5/600, icon 14. `overdue` if any wear overdue or any
    document expired; `soon` if any non-ok wear (unknown counts) or a renewal in its warning
    window; else `ok`. Label "{n}টি কাজ বাকি" or "সব ঠিক আছে".
  - Stage 210 high, swipe carousel: slides 84% wide when > 1 bike, neighbours at opacity .4,
    scale .82; image max 190 high, drop shadow. Swipe threshold 22% (offset + velocity × .2),
    spring 420/40/.9, then tick haptic + switch bike. Tap a neighbour to go to it. Stable order:
    primary first, then by id. No photo: Gauge icon 40 + brand name.
  - Dots only with > 1 bike: 24×24 target, 7dp dot, active 18×7 brand.
  - Odometer button → odometer sheet: CountUp 36/800 + "কিমি"; "আজ আনুমানিক {estimate} · আপডেট ›".
  - Stats strip (2 columns): left Fuel "{x.x} কিমি/লি" or "--", note "{n} দিন তেলের হিসাব নেই"
    (≥ 10 days) / "শেষ তেল: {date}" / "খরচ ও মাইলেজ" → History tab. Right Wrench: next job
    (first due wear, else first wear) short name + status in err/warn → gauge detail; none →
    "পরের কাজ" / "এখন কিছু বাকি নেই" → service.
- **Z2 Quick actions:** 4 columns, gap 10. Tile surface-2, r 16, padding 12/4/10; icon well
  52×52 r 16 brand-tint + brand-line border, icon 24. Labels তেল ভরা · সার্ভিস · মিটার রিডিং ·
  ডকুমেন্ট. Warning dot 7dp (warn, 2dp surface ring) on Fuel when ≥ 10 days since last fuel,
  on Document when a renewal is due.
- **Z3 Jobs "কাজ ও যন্ত্রাংশ":** one bordered list, r 20. Order: overdue wears, then renewals
  (by days left), then soon/unknown wears. Wear row: icon 30 r 9, name 14/600, meta
  "শেষ: {km} · {date}" (unknown: "শেষ সার্ভিসের তথ্য দিন"), badge, 5dp bar (fill clamp .08–1,
  striped when unknown, grows over `dur.sweep`, delay min(.28, i×.04)) → gauge detail. Doc row:
  FileText, name, "{n} আগে মেয়াদ শেষ" / "মেয়াদ শেষ হতে {n}", meta "ডকুমেন্ট", no bar →
  **doc view**. Header chips "বাকি {n}" (only if any due) and "সব {n}"; default due. "All" shows
  5 then "আরও দেখুন ({n}টি)" / "কম দেখুন". Nothing due: calm card "সব ঠিক আছে। কোনো সার্ভিস বা
  ডকুমেন্ট বাকি নেই।"
- **Z4 Seasonal pill:** card padding 12/14, r 16; icon well 36 (CloudRain 18);
  "বর্ষায় বাইকের যত্ন ও জরুরি সাহায্য" + hint; opens to one static line + button
  "ব্রেকডাউন ও জরুরি সহায়তা" → breakdown. No checklist, no weather, no dismiss (D5).
- **Gauge detail:** 3 rows in a card stack. Row 2 "শেষবার করা হয়েছে" opens an **inline editor**
  (km + date side by side, 4 error messages, Save/Cancel) → confirmed baseline, Undo toast.
  Row 3 interval (read only). Buttons "আজ করিয়েছি · রিসেট করুন" (baseline = now, **no history
  log**, Undo) and "সার্ভিস ও খরচ লিখুন" → service for that item. No provenance, no %-left,
  no physical note.
- **Bike switcher:** rows (border 1.5, r 16), avatar 44 tinted with the bike's own brand
  colour, "brand · year · km"; current first with "✓ এখন চালাচ্ছেন"; archive icon (only if > 1
  active) with Undo; dashed "আরেকটি বাইক যোগ করুন" → onboarding; archived section at .75.
- **Breakdown:** "জরুরি সহায়তা"; 999 card + full button "৯৯৯ জরুরি সহায়তা কল"; 3 static tips;
  button "রাস্তায় ডকুমেন্ট দেখানোর মোড" → first doc view (no docs: "ডকুমেন্ট খুলুন" → Vault tab).
- **Odometer sheet:** title "মিটার রিডিং"; Camera + Mic in header `actions`; voice note;
  number 52/700 with "আজ আনুমানিক" / "আপনার লেখা কিমি"; "শেষ আপডেট: …"; chips
  +10/+50/+100/+500 and "আনুমানিক"; keypad hidden and save disabled while camera on; Undo toast.

### Work
- New pure `garage/GarageHomeModel.kt` (unit tests): health state, due count (unknown counts),
  merged job order, fuel note, next job, 10-day flag.
- Domain: stable bike order for the carousel (today `activeBikes()` puts current first, so bikes
  would jump); cc from `Ride.cc ?: variantOf(modelId)?.cc`; reset/edit last-done through
  `saveSchedule(rule.copy(..., baselineCertainty = CONFIRMED))` with Undo (Gemini wrote a 0-cost
  service log instead; do not); "last updated" from `odometerConfirmedDate`.
- Files: `GarageScreen.kt` (layout only), `BikeShowroom.kt`, `ShowroomCarousel.kt`
  (`HorizontalPager`, page 0.84), `ShowroomStatsStrip.kt`, `GarageQuickActions.kt`,
  `GarageJobsSection.kt`, `SeasonalCareSection.kt`, `GaugeDetailSheet.kt` +
  `GaugeLastDoneEditor.kt`, `BikeSwitcherSheet.kt`, `BreakdownHelpSheet.kt`, `OdometerSheet.kt`.

### Remove
`garage/GarageSections.kt` (whole file, incl. `Quad`), `Spark()` and `WearCalculator.economySeries`
/ `statusLabel`, the `schedule` and `serviceReminder` routes from gauge detail (D6), breakdown
contacts/location UI (D4), seasonal checklist/weather/dismiss UI (D5), Gemini's banner, gauge
strip, quick-add bike form, interval stepper, gauge history list, wear summary panel, breakdown
checklist. Strings: `garage_banner_*`, `garage_jobs_*` (Gemini), `garage_bike_count_pill`,
`garage_tab_*`, `garage_quick_bike_*`, `gauge_interval_*`, `gauge_recent_logs_*`,
`gauge_physical_note`, `breakdown_step*`, `odo_delta_badge`, `garage_status_*`,
`garage_unknown_percent`, `garage_log_item`, `garage_sample_photo`, `garage_title`,
`garage_bike_settings`, `garage_archive_last_error`, `schedule_source*`, `seasonal_*` items.
Unreferenced old drawables: `model_honda_dream_110`, `model_honda_shine_100`,
`model_ktm_duke_125_2023`, `model_ktm_rc_125_2022` (after the rename in Phase 0).

### Phase 3 checklist
- [ ] Single bike and 3 bikes (swipe, peek, dots, stable order, haptic on switch).
- [ ] Health pill 3 states; stats strip both columns incl. empty states.
- [ ] Quick actions with both warning dots.
- [ ] Jobs list: order, chips, bars, unknown stripes, see more, calm card, doc row → doc view.
- [ ] Gauge detail edit + reset with Undo, no history log written.
- [ ] Switcher, breakdown, odometer sheets.

---

## 7. Phase 4: Entry sheets (Fuel, Service, Expense, Paste)

Split into 4a Fuel + Expense, 4b Service, 4c Capture (pump camera, bill camera, paste).

### 4a Fuel (v4 `sheets/FuelSheet.tsx`)
- Title "তেল ভরা" (edit: "ফুয়েল রেকর্ড ঠিক করুন"). Header actions: Camera "পাম্প মিটারের ছবি
  তুলুন", Mic "মুখে বলুন" / "শোনা বন্ধ করুন". Hidden while camera is open.
- Amount card (`EntryAmountCard`): caption "কত টাকার তেল?" / "কত লিটার?" + `SegmentedPill`
  টাকা / লিটার. Sub-line "≈ X লিটার", price pill "৳130/লি ✎", confirm pill "নতুন দাম · ঠিক?" →
  "✓ দাম ঠিক" (only when the price is new). Message line: voice note or "লিটার অনেক বেশি মনে
  হচ্ছে, মিলিয়ে নিন" (> 40 L).
- Presets (Taka mode only): ৳200 ৳300 ৳500 ৳1000, absolute values.
- Pump `PickerField` "পাম্প বেছে নিন (ঐচ্ছিক)" → picker swaps the sheet content: "কোন পাম্পে?",
  search (max 60), recent (5, newest, "শেষ: {date}"), "\"x\" ব্যবহার করুন", "পাম্প মুছে দিন".
  Tap picks at once. Map row: D10.
- `OdoRow`: label is the error text, or "মিটার রিডিং" / "মিটার রিডিং · আনুমানিক"; value 18 bold;
  +10/+50/+100 only while active, else "✓ ঠিক আছে" pill.
- `ChipLine`: ফুল ট্যাংক | অকটেন পেট্রোল | আজ গতকাল অন্য দিন | মাঝের ফিল বাদ পড়েছে.
- Footer: optional save-failed line; button = reason text or "৳X সেভ করুন"; compact keypad.
- Defaults: octane, Taka mode, **full = false** (D9), missed fill off, last price, estimated odo
  (not confirmed), today.
- Validation order: amount, price > 0, new price confirmed, **odometer confirmed**, litres ≤ 100,
  odometer range/order, valid date, not future, not before last fill.
- Domain: none blocking (price still required, `missedFill` / `priceConfirmed` still saved).
  Add `recentPumps` (name + last date) from fuel logs in `FuelScreenAdapter.kt`.
- Files: `FuelSheet.kt`, `FuelEntryState.kt`, `FuelEntryValidation.kt`, `FuelChipStrip.kt`,
  `PumpPicker.kt`.
- Remove: `FuelConfidenceControls.kt`, `FuelDateControls.kt`, `FuelAmountHero.kt` (replaced by
  `EntryAmountCard`), the price checkbox, the cycle paragraph, Taka/Litre chips, the `আনুমানিক`
  tag and reset chip, free-text pump field, Gemini's presets/accordion/chain chips and strings
  `fuel_voice_prompt`, `fuel_paste_prompt`, `fuel_pump_*`, `fuel_details_toggle`,
  `fuel_full_preset`.

### 4a Expense (v4 `sheets/ExpenseSheet.tsx`)
- Title "অন্যান্য খরচ যোগ" / "খরচ ঠিক করুন". Chips পার্কিং / টোল / অন্যান্য (default অন্যান্য).
  Amount card "কত টাকা খরচ হয়েছে?". `DateChips`. Note (44 high, max 80,
  "নোট (ঐচ্ছিক) · যেমন: ব্রিজ টোল"). Button reason or "৳X সেভ করুন". Keypad whole taka only,
  hidden while typing the note. No delete in the sheet (delete is in the cost card).
- Domain: `ExpenseRules.validate` must reject 0.
- Remove: `CostDateControls`, Gemini's trash header icon.

### 4b Service (v4 `sheets/ServiceSheet.tsx`, `servicePickers.tsx`)
- Title "সার্ভিস" / "সার্ভিস রেকর্ড ঠিক করুন". Header actions: Camera "রশিদের ছবি তুলুন",
  Mic "মুখে বলুন বা লিখুন" (toggles SmartInput), Copy "আগের সার্ভিসের মতো" (only if a previous
  service exists). Default height.
- SmartInput: 3-row text area (max 600), real Bangla voice with 3 animated bars, "যোগ করুন (N)",
  parsed list, "চেনা যায়নি: …". Replace the fake demo voice.
- `OdoRow` first: "X কিমি · আজ", "ঠিক আছে" pill; date chips only while editing the odometer.
- Empty bill: "কী কী কাজ হয়েছে?" + up to 6 suggestion chips (due first with warn dot, then
  ones with a last price, then the rest) + dashed "অন্য কাজ বা পার্টস খুঁজুন".
- Part picker (tall): search "খুঁজুন বা নতুন কাজের নাম লিখুন", custom add row, groups
  "নিয়মিত সার্ভিস" / "মেরামত ও অন্যান্য", 22dp tick box, "সময় হয়েছে", "শেষ ৳X", sticky footer
  "Nটি কাজ যোগ করুন".
- Bill card: lines (padding 8/6/8/14, active = brand-tint), "× N", engine oil "গ্রেড?" pill,
  price button min 76×40 ("দাম?"), X remove (36), quantity stepper only on the active line,
  extra lines "সার্ভিস চার্জ" and "ছাড়" (−৳X), footer "+ আরও কাজ" | "+ চার্জ / ছাড়".
- Reminder note "রিমাইন্ডার নতুন করে গোনা হবে: …" (ok colour, paid only).
- Workshop `PickerField` → workshop picker (recent with History icon, use typed name, clear).
- "আরও তথ্য" toggle with summary text; inside: quote chip + note, phone (max 20), repeat
  problem (max 80), warranty chips (always shown).
- Footer: "মোট · Nটির দাম জানা নেই", total 21sp, Wrench button "সার্ভিস সেভ করুন" /
  "কোটেশন সেভ করুন". Keypad starts hidden, 26dp chevron to hide.
- **Domain fixes (required):** new `updateServiceVisit` that keeps `visitState`, `workshopId`,
  `repairCaseId`, `warrantyIds`; accepts custom lines (`catalogueId = null` + `customName`);
  uses the lenient oil check from `saveQuickService`. Today `replaceServiceVisit`
  (`VaultState.kt:174`) rebuilds through `logServiceVisit`, which requires `OilDetail` on every
  engine-oil line (`:132`) and a catalogue id (`:127`). Show stored `misc` in the total.
  Toggling the quote chip off in edit = quote → paid (replaces the history quote buttons).
  Never map custom lines to `general_service` (Gemini bug). Make `ServiceRowState` immutable.
- Files: `ServiceSheet.kt` (thin), `ServiceDraftState.kt`, `ServiceSaveMapper.kt` (pure, tests),
  `ServiceBill.kt`, `ServiceSuggestions.kt`, `ServicePartPicker.kt`, `ServiceWorkshopPicker.kt`,
  `ServiceMoreSection.kt`, `ServiceHeaderPanels.kt`.
- Remove: `ServiceWorkshopField.kt`, `ServiceDateOdometerSection`, `ServiceSmartInput` fake
  voice + `service_demo_*`, `ServiceRepairWarrantyControls`, `ServiceExtraChargesRow`,
  `SmartServiceDraft.kt` (test-only), Gemini's `ServiceDueBanner`, `ServicePresetRow`,
  `ServiceCatalogueGrid` and strings `service_smart_banner_*`, `service_preset*`,
  `service_details_toggle`, `service_workshop_picker_*`, `service_viscosity_grade`.
  Keep `HistoryServiceMetaSection`.

### 4c Capture
- Generalise `rememberOdometerCamera` into a text camera. Add pure parsers in `domain/capture`:
  pump meter (amount, price, litres, fuel type) and spoken fuel (amount, fuel type). Bill scan
  feeds `applyParsed`; the photo is **not** saved (v4).
- Pump camera card: `#0d1210`, padding 12, r 16; "পাম্প মিটার স্ক্যান করা হচ্ছে…"; result
  buttons "এই হিসাব নিন" / "আবার ছবি তুলুন" / "বাতিল". Applying still needs price confirm.
- Paste sheet: "লিখে, বলে বা পেস্ট করে সাজান", intro, small Mic button, 4-row text area,
  `.found` cards for fuel ("তেল ভরা খুলুন") and service ("সার্ভিস খুলুন") only (D16).
  Replaces `CaptureReviewSheet` layout and the local/provider chooser (D8).
- Reminder sheet: copy only (কাগজ → ডকুমেন্ট).

### Phase 4 checklist
- [ ] Fuel: Taka and Litre modes, presets, new-price pill, pump picker, odo confirm rule, chip
  line, save text, compact spacing.
- [ ] Expense: categories, date chips, keypad hides on note, reject 0.
- [ ] Service: header panels, suggestions, part picker, bill editing, more section, quote save,
  edit of a visit with engine oil, a custom line and a quote (all must save).
- [ ] Pump and bill camera, paste sheet.

---

## 8. Phase 5: Documents (Vault tab)

### Spec (v4 `screens/Vault.tsx`, `components/DocCard.tsx`, doc sheets)
- **Vault screen:** h1 "ডকুমেন্ট" + tint + button (44). Next card "পরের নবায়ন" (padding 16,
  r 16, border 1.5, status mix ok 8%/35%, soon 9%/40%, overdue 9%/40%), "soon" = days ≤
  `warningDays ?? lead ?? 30`. Empty state with **dashed** border, padding 28/20.
- **Wallet:** "সব ডকুমেন্ট"; one stack of all docs sorted by days left (overdue first, no-expiry
  last). Each card after the first overlaps by −142dp; z order = index; padding 10/12/12, r 20,
  shadow `0 -6 16 rgba(0,0,0,.1)`. Top row name + ExpiryChip. Privacy on: mask "নম্বর ও ছবি
  লুকানো আছে। খুলতে ট্যাপ করুন।" Footer switch "মেয়াদের আগে রিমাইন্ডার দিন" / "{N} দিন আগে এবং
  মেয়াদের দিন।"
- **ExpiryChip:** padding 3/10, 13sp/600, icon 14; copy "{n} দিন আগে শেষ" / "{n} দিন বাকি" /
  "মেয়াদ আছে" / "মেয়াদ নেই"; dark overrides.
- **BRTA card (licence, registration only):** aspect 1.586, r 14, background image stretched
  (`ContentScale.FillBounds`, not Crop), **no dark overlay**, text `#141821`. Overlays in % of
  card:

  | | photo box | fields panel | label |
  | --- | --- | --- | --- |
  | licence | L4.8 T25.5 W28 H59 `#4c6178` | L35.5 T25 R22.5 B14, scrim rgba(253,249,243,.58) | L4.8 B4.2 |
  | registration | L5.5 T27.5 W24.5 H48 `#3f6659` | L34 T24.5 R11.5 B10, scrim rgba(251,253,250,.64) | L5.5 B5 |

  Field label 11/600 (`#8b1d27` licence, `#1c5a44` registration), value 13/700 one line.
  Order: licence = নাম, লাইসেন্স নং, মেয়াদ; registration = রেজিস্ট্রেশন নং, মালিকের নাম, মেয়াদ.
  Missing value placeholder "নাম দেওয়া নেই" / "নম্বর দেওয়া নেই". Tax / insurance / fitness keep
  the gradient card. See D2 and D3.
- **Add sheet:** type → photo → review. Type rows (padding 14, r 16) with sub "সামনে ও পেছনের ছবি"
  / "পুরো পৃষ্ঠার ছবি", link "ছবি ছাড়া শুধু তারিখ লিখব". Photo: viewfinder 250 (card) / 340
  (page), r 18, corner marks 34 `#7ee2a7`, rotating hints (1.1s), shutter 76/56, link
  "ছবি ছাড়া এগিয়ে যান". Review: found banner, update warning if same kind exists, expiry default
  **today + 365**, chips **1/2/5/10 বছর পর**, reminder switch **on** by default, optional
  number/holder behind a text link (closed), one button = reason or "ডকুমেন্টে সেভ করুন". Only
  rule: expiry set. No confirm checkbox. Toast with Undo.
- **Doc view:** large card + chip + expiry; "শুধু এই ডকুমেন্ট দেখান"; reminder switch; primary
  "নবায়ন"; "তথ্য ঠিক করুন" (Pencil); BRTA verify button if QR; "নবায়নের ইতিহাস"; danger link
  "ডকুমেন্ট মুছুন" with Undo. Raise screen brightness while open (`InspectionBrightness`).
- **Renewal:** footer button = error or "নবায়ন সেভ করুন"; summary "আগের মেয়াদ: …"; new expiry
  default previous + 365, chips **1/2/5**; fee (optional); receipt (optional); note (max 500);
  checklist toggle **closed by default** with progress, steps, links, fine, QR, offline note
  inside; link "ডকুমেন্টে ফিরে যান". After save or edit, return to doc view.

### Work
- Domain: `saveDocument` must accept blank number/holder (`VaultState.kt:248`). New
  `upsertDocumentByKind` (match `stableKind()` + owner/bike scope; keep id, pages, renewals,
  checklist, details; keep old number/holder when new ones are blank). Save the canonical title
  (not the translated label; today "Registration Smart Card" fails the case-sensitive card check).
  Passport check: use `stableKind()` or set `detailsConfirmed = true` for typed values.
  Delete the ui copy of `VaultDocument.assetIds()` (it misses renewal receipts). Pass doc ids to
  sheets, not snapshots. Make sure the orphan sweep cannot delete originals while Undo is
  possible. No schema change.
- Real camera in the photo step (`DocumentCamera` / `DocumentImportActions`; front + back for
  cards; no crop UI).
- Files: `VaultScreen.kt`, `DocumentWalletStack.kt` (custom Layout), `NextRenewalCard.kt`,
  `BrtaCardArt.kt` (position table), `GenericDocArt.kt`, `ExpiryChip.kt`, `DocAddSheet.kt` +
  `DocTypeStep.kt` / `DocPhotoStep.kt` / `DocExpiryStep.kt`, `DocViewSheet.kt`,
  `RenewalSheet.kt` + `RenewalGuideSection.kt`, `DocumentSheetRoutes.kt`.

### Remove
`DocumentWallet.kt` (dead), in `DocumentFlow.kt` the dead `DocumentCaptureFlow`,
`DocumentConfirmation`, `DocumentEditFlow`, `OriginalDocumentDetail`, `CropEditor`,
`DocumentScreen` (keep `DocumentImage` / `LegacyDocumentImage`, rename file to
`DocumentImages.kt`), ui duplicates in `DocumentCapture.kt` (`documentTypes`,
`isCardDocument`, `documentPageLimit`, `validateDocumentAssetId`, `assetIds`),
`ReminderAccessPanel`, inspection-mode button, police switch, back footer, reminders button.
Strings: `docflow_*`, `wallet_*`, `inspection_*`, `doc_add_confirm`, `doc_add_error_*`
(number/holder/confirm), `doc_add_reminder_*`, `doc_add_sample_*`, `doc_add_manual`,
`doc_add_demo_note`, `vault_reminder_action`, `vault_inspection_mode`, and Gemini's
`doc_brta_*`, `vault_smart_cards_section`, `vault_papers_section`, `vault_missing_add_cta`,
`doc_add_validity_*`, `doc_add_optional_toggle`.

### Phase 5 checklist
- [ ] Wallet with 1, 3 and 5 docs; overlap and order; privacy mask.
- [ ] BRTA licence and registration cards match the prototype at 100% and "large".
- [ ] Add flow with and without photo; editing; adding a second licence updates, not duplicates,
  and keeps pages and renewal history.
- [ ] Doc view order, brightness, delete + Undo.
- [ ] Renewal: chips, collapsed checklist, back to doc view.

---

## 9. Phase 6: History and Me (profile, settings, privacy, backup)

### Spec: History (v4 `screens/History.tsx`, `components/CostInsights.tsx`)
1. Title "রেকর্ড" (1.5em/600, min 48).
2. **Cost card:** border 1 `line`, r 16, padding 16, gap 10. "মোট খরচ" + small button
   "অন্যান্য খরচ যোগ" (min 40, r 10). Period chips ৩০ দিন / এই মাস / সব. Category chips সব / তেল /
   সার্ভিস. Total 28sp/700. Stacked bar (total > 0): 10 high, pill, surface-2, gap 2, each part
   ≥ 4%; fuel = brand, service = ok, other = warn; .35s width animation. Legend 12sp, 8dp dots:
   "তেল ৳X", "সার্ভিস ৳X", "অন্যান্য ৳X". Expense rows: brand link "{cat} (note) · ৳X" → edit;
   red "মুছুন" with Undo ("খরচ মুছে গেছে"). Chips also filter the timeline.
3. **Work-ledger card** (second, always shown): whole card is a button; "রাইডার ওয়ার্ক লেজার
   (ঐচ্ছিক)", caption (on: "Nটি শিফট · নিট ৳X"; off: "ডিফল্টভাবে বন্ধ…"), link
   "ওয়ার্ক লেজার দেখুন".
4. Empty: dashed, padding 28/20, "এখনও কিছু নেই। নিচের + চাপুন।"
5. **Sticky month headers:** 14/600 text-2, padding 6 0, canvas 92% (wash 88%) + blur.
6. **Row:** r 16, padding 12/14; icon box 44 r 14 (fuel brand-tint, service ok 14%); title
   "তেল ভরা · X লিটার" or "{line0} ও আরও Nটি"; sub "date · km"; amount 700; chevron rotates.
   Open: `line-strong` border + shadow. Detail: **kv table** (surface-2, r 12, padding 10/12) with
   fuel rows দাম / ট্যাংক / পাম্প and service rows per line, সার্ভিস চার্জ, ছাড়, ওয়ার্কশপ; tags
   কোটেশন / সমস্যা / ওয়ারেন্টি; partial-tank note. Two equal outlined buttons "ঠিক করুন" (fuel or
   service edit) and red "মুছুন".
7. **Swipe:** drag left max 88dp reveals a 96dp red delete button (opacity follows the drag);
   past 60dp only a warning haptic; tapping the button deletes; toast "রেকর্ড মুছে গেছে" + Undo.
8. **Jump to top** (> 6 rows): pill "উপরে ফিরে যান" (ArrowUp 15) at the end, smooth scroll.

Expenses are **not** timeline rows any more; they live in the cost card.
Work-ledger sheet: switch, summary, 4 fields, footer only when on. No allowance or payout lines.

### Spec: Me (v4 `screens/Me.tsx`, `sheets/SettingsSheet.tsx`)
- Title "প্রোফাইল". Profile card (whole card is a button → bike switcher): photo 72×56 r 14,
  "brand name", "year · km[ · Nটি বাইক]" (count only if > 1), "বদলান" + chevron.
- Three groups (title 13/600 text-2; list border, r 16, dividers). Row min 56, icon tile 34 r 10
  brand-tint (icon 19), label 15/600, value 13 text-2 (max 52% width, ok/warn tone = 600).

  | Group | Row → opens |
  | --- | --- |
  | নিরাপত্তা ও ডেটা | অ্যাপ লক ("চালু" ok / "বন্ধ · Nটি ডকুমেন্ট" warn) → settings(lock); ফোনে ব্যাকআপ ও পুনরুদ্ধার → D12; ক্লাউড ব্যাকআপ ("সংযুক্ত নয়") → cloud backup; বিক্রয় ও হস্তান্তর প্যাক → settings(sale) |
  | রিমাইন্ডার | সার্ভিস ও ডকুমেন্ট ("চালু · N দিন আগে" / "বন্ধ") → settings(reminders) |
  | অ্যাপ | চেহারা ("{theme} · {brand}") → appearance; ভাষা ও সংখ্যা ("বাংলা · ১২৩") → language; আরও সেটিংস ("ভাইব্রেশন, অ্যানিমেশন, লোকেশন") → more; হোম স্ক্রিন উইজেট → widget |

- **SettingsSheet(section):** title per section, no footer, content in one card with dividers.
  appearance (theme segmented, 34dp colour swatches, text size); language (language and digits as
  **two separate** controls); more (3 switches); reminders (2 switches, lead 30/14/7, time
  9am/6pm, "ফোনের নোটিফিকেশন যাচাই করুন"); lock (switch opens privacy sheet, auto-lock 1/5/15,
  "লক যাচাই ও বিস্তারিত"); sale (static text, D15); widget (220dp preview tile, r 22).
- **Privacy sheet:** pinned footer by state (off: "ফোনের লক দিয়ে চালু করুন" + "হয়েছে"; on:
  "এখন লক করুন" + "হয়েছে"; checking: "থামান"; failed: "আবার চেষ্টা করুন"). Turn-off stays a
  ghost button in the body.
- **Recovery sheet:** footer per state (review: "ফিরে যান" + "বাতিল"; failed: "আবার দেখুন";
  others: "তালিকায় ফিরুন"). No Done button.
- **Cloud backup:** one card with a divider; footer "লোকাল .mva ব্যাকআপ খুলুন".

### Work
- Domain: keep `monthlyBudgetPoisha` / `maintenanceAllowancePoisha` stored for backup
  compatibility, no UI (D13). `CostCategory` keeps 6 values; parking + toll + custom sum into
  "অন্যান্য". Document fees: D14. Quote → paid now happens in service edit (Phase 4b).
  `lastBackupEpochMillis` shown inside the backup sheet.
- Files: `HistoryScreen.kt` (+ `LazyListScope.historyMonthGroup()`), `HistoryHeader.kt`,
  `JumpToTopPill.kt`, `HistoryLogCard.kt` + `HistoryLogKv.kt` + `HistoryLogActions.kt`,
  `components/SwipeRevealRow.kt`, `CostInsightsCard.kt`, `ExpenseSheet.kt`, `WorkLedgerCard.kt`,
  `MeScreen.kt`, `settings/SettingsRows.kt` (`SettingsGroup`, `SettingsRow`),
  `SettingsSheet.kt` (router) + one file per section group, `PrivacySheet.kt` split
  (`PrivacyFooter`, `PrivacyStatusCard`, `PrivacyScopeCard`, state holder), `RecoverySheet.kt`,
  `CloudBackupSheet.kt`.

### Remove
`history/BudgetSheet.kt` (BudgetSheet, CostDetailsSheet) and routes `budget` / `cost-details`,
`CostDetailsSelection`; in `CostOverview.kt` `MonthlyBudgetSummary`, `CostCoverage`,
`FuelCycleSummary`, `localizedCostDate` (file replaced by `CostInsightsCard.kt`); expense and
document-fee branches in the timeline; in `SettingsActionSections.kt` the reminders / privacy /
backup / demo / about sections (content moves into sheet sections); `MeScreen` params
`onResetDemo`, `onStartOnboarding` (D17); widget bike chips. Strings: `cost_budget*`,
`cost_allowance`, `cost_plan_only`, `cost_details`, `cost_incomplete`, `cost_coverage`,
`cost_per_km`, `cost_ratio_scope`, `cost_no_distance`, `cost_cycle_*`, `cost_title`; fix
`cost_legend_*` words to তেল / সার্ভিস / অন্যান্য. Gemini's `cost_kpi_*`, `cost_chart_title`,
`history_search_*`, `history_export_*`, `history_month_selected_clear`, `settings_sheet_title`,
`settings_open_sheet`, `settings_rider_stats_line`. Keep `OwnershipForms.kt` (still used).

### Phase 6 checklist
- [ ] Cost card: all 9 chip combinations, bar and legend, expense edit/delete + Undo.
- [ ] Work-ledger card position and both states.
- [ ] Sticky headers, kv table (fuel full/partial, service with charge/discount/workshop/tags),
  edit for both types, swipe reveal + tap delete, jump to top.
- [ ] Me: profile card, 3 groups with live values, each row opens the right place.
- [ ] Settings sections, privacy and recovery footers, cloud card.

---

## 10. Decisions for the owner

Each has a recommended default. Literal v4 is the default unless a rule in AGENTS.md or an
existing privacy/data contract says otherwise.

| # | Question | Recommended |
| --- | --- | --- |
| D1 | Park Gemini's working tree on `wip/gemini-v4-attempt` and restart from HEAD with the salvage list? | **Yes** |
| D2 | v4 DocCard is a "stand-in for the saved photo". Show the saved page photo when one exists, BRTA template only as fallback? | **Yes** |
| D3 | Keep the "নমুনা · ডেমো" label on the BRTA template in the real app? | **No**, drop it |
| D4 | Breakdown contacts and location share (shipped) are not in v4. | Remove the UI, keep stored fields for old backups |
| D5 | Seasonal checklist, weather and dismiss (shipped) are not in v4 (static pill). | Remove the UI, keep stored fields |
| D6 | Interval editing (`schedule` sheet) has no entry in v4. | Remove the route; interval shown read-only |
| D7 | Factory-manual schedule opt-in loses its only screen (onboarding). | Drop the UI, keep `MaintenanceProfiles` and `factoryProfileId`; update the privacy probe |
| D8 | v4 has no voice disclosure; P07 policy needs local-first or a disclosure. | Local-first; one-time disclosure before any cloud provider (exception to literal v4) |
| D9 | v4 default `full = false`; mileage then needs the rider to tap "ফুল ট্যাংক". | Follow v4 |
| D10 | Pump picker "ম্যাপ থেকে বেছে নিন" needs a Places SDK and network. | Leave it out until approved |
| D11 | Pump-meter and bill cameras need new OCR parsers (Phase 4c). | Build them; hide the header icon until each works |
| D12 | v4 sends "ফোনে ব্যাকআপ ও পুনরুদ্ধার" to the recovery sheet, but Compose has real create/restore. | Open `BackupSheet` (create + restore) with a link to recovery |
| D13 | Budget and allowance UI is gone. Allowance still changes work-ledger numbers. | Keep stored, stop using allowance in the calculation so numbers match the screen |
| D14 | Document fees: v4 has no cost entry for them. | Count in "অন্যান্য", hide from the timeline |
| D15 | Sale/handover section is static text in v4; `PassportExportButton` has no caller. | Static text now; export as a separate task |
| D16 | Paste sheet targets: v4 has fuel and service only (Compose also has odometer, expense). | Follow v4 |
| D17 | Reset demo / restart onboarding on Me are prototype-only. | Remove from Me; debug build only if needed |
| D18 | Split `strings.xml` into per-area files to reduce merge conflicts. | Yes, in Phase 1 |

---

## 11. Data safety summary

- **New persisted field:** `Ride.cc: Int? = null` only. Needs a backup schema update and a
  compatibility fixture (old backups load with `cc = null`).
- **No ID migration:** stored bike model ids, document kinds and cost categories stay the same.
- **Fields kept with no UI** (so old backups still load): `monthlyBudgetPoisha`,
  `maintenanceAllowancePoisha`, `breakdownContacts`, `checkedSeasonalItems`,
  `dismissedSeasonKeys`, `weatherCache`, `factoryProfileId`.
- **Behaviour changes in domain:** `saveDocument` accepts blank number/holder;
  `upsertDocumentByKind` keeps pages and renewals; `updateServiceVisit` keeps visit details;
  `ExpenseRules` rejects 0; gauge reset writes no history log.
- Each of these gets unit tests plus a compatibility fixture where stored data is involved
  (C3 change class for Phases 2, 4b and 5).

---

## 12. Order, parallel work and gates

- Phase 0 → Phase 1 must run in order, alone.
- After Phase 1 (with `VaultSheetHost` split and per-area strings), Phases 2, 3, 4, 5, 6 touch
  separate folders and can run in separate worktrees. Shared hotspots: `MotoVault.kt`,
  `Appearance.kt`, `V3IconPaths.kt`. Put any change to these back into a Phase 1 follow-up.
- Suggested order if one person does it: 1 → 3 (Home is the first screen) → 4 → 5 → 6 → 2.
- **Phase 7 (final):** delete everything in the remove lists that is still present, run
  `compare_ui_literal_head.py`, regenerate `ui-literal-baseline.json`, run the full portable +
  workstation sets, Android and iOS runtime checks of every phase checklist, a language switch
  (bn/en, both digit styles), dark mode, text scale 1.3, reduced motion, and a named Bangla
  reviewer sign-off. Evidence per `docs/engineering/verification.md`.

Per-phase gates: portable set and workstation set for every phase; C2 runtime checks for every
visible phase; C3 (fixtures, recovery) for Phases 2, 4b and 5.

---

## 13. Risks

- **Screen width:** the prototype phone is 440 wide. Common Android phones are 360-412dp.
  Check the 4-column quick actions, the 3-column brand grid and the fuel chip line at 360dp.
- **Radial ellipse wash and wallet overlap** need custom drawing/layout in Compose; budget time.
- **Pump/bill OCR** quality is unknown; D11 lets the sheets ship without them.
- **Removing shipped features** (D4-D7, D13) changes what existing users see. Old data stays.
- **Copy:** v4 is Bangla only. Every English string is new work and needs review.
