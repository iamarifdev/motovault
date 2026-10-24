# MotoVault app UI v3: reviewable web prototype

A clickable prototype of the improved MotoVault, built with Sky design tokens. It exists so the changes can be confirmed before anything is applied to the Kotlin app. It does not touch `composeApp/`. All data is demo data stored in the browser (`localStorage`).

```sh
cd design/app-ui-v3
npm install
npm run dev        # http://127.0.0.1:5273
npm run build      # type-check + production build
```

On a desktop it shows a phone frame with a review panel (theme, digits, text size, accent, reduced motion, shortcuts to every flow, a haptic log and the list of changes). On a narrow window or a real phone it fills the screen.

## What to try

| Flow | Where | Look for |
| --- | --- | --- |
| Fuel in two taps | "+" > তেল ভরা | Taka first, litres follow, price remembered, estimated odometer, full tank on, date chips, Undo after save |
| Service without dropdowns | "+" > সার্ভিস | Tap parts, last price filled in, "আগের সার্ভিসের মতো", oil grade chips, live text and voice parsing |
| Odometer | "+" > ওডোমিটার | One tap on "ঠিক আছে", +10/+50 chips, photo read (mock) |
| Paste to add | "+" > লিখে বা পেস্ট করে সাজান | Use the two sample chips |
| Documents | Vault tab or "+" > কাগজ | Scan (mock), review, save, then the reminder prompt |
| History | History tab | Tap a row to expand, drag a row left to delete, then Undo |
| Onboarding | Review panel > Onboarding | Three inputs only |
| Theme, digits, text size, accent | "আমি" tab | Bengali or Latin digits, 1.3× text, all nine brand accents |

Mocks, clearly labelled in the UI: camera scan, odometer photo reading, speech (uses the browser's bn-BD recogniser when it exists), share-sheet and widget.

## How I would build the app from scratch with Sky

1. **One token source.** Sky's `tokens/tokens.json` is the only place colours, radii, spacing, nav geometry and motion timings live. A script (`tools/build-tokens.mjs`) generates CSS variables for this prototype; Sky already generates the Kotlin constants for the app. MotoVault adds only an overlay: the bike-brand accent (nine pairs, all contrast-checked) and status colours.
2. **Layers, each depending only on the one above.**
   - Sky foundations: tokens, type roles, motion policy, icons (Feather).
   - MotoVault theme: brand accent, status colours, Bangla type scale (12 px floor, no tracking, variable-font weights), numerals and date formatting, haptics, reduced motion.
   - Primitives: Button, Chip, Segmented, Switch, Sheet (drag to dismiss), Keypad, Toast with Undo, Gauge, CountUp.
   - Patterns: NextUp card, QuickActions, AmountEntry, ServiceLines, Wallet, ExpiryChip.
   - Features: garage, fuel, service, odometer, documents, history, settings.
3. **Domain rules as pure, tested functions** that a UI can only call: `wearOf`, `describeWear` (says how far overdue), `economyOf`, `estimateOdometer`, `parseServiceText`, `parseAmount`. In Kotlin these go in `commonMain/domain` with unit tests, as the wear and fuel use cases already do.
4. **Input rules.** Every numeric field goes through one parser that accepts Bengali, Arabic-Indic and Latin digits. The display digit style is a setting, separate from input. Numbers use lakh grouping. Numbers are entered on an in-app keypad (no OS keyboard covering Save). Dates are chips first, picker second. Estimates are always shown as estimates and saved only on an explicit tap.
5. **Local-first data.** One repository, optimistic writes, a snapshot for Undo, images stored as files (not Base64 inside the state), reminders scheduled from the same state.
6. **Motion.** Sky timings for page and expansion; springs for sheets and presses; gauge sweep and count-up; list layout animation; one `reduceMotion` switch that also follows the system setting.
7. **Verification built in.** Screenshot tests per theme × brand × text scale (1×, 1.3×), UI tests that address controls by test tag instead of screen coordinates, and a contrast test that reads the tokens.

### Prototype to Compose map

| Prototype | Compose Multiplatform equivalent |
| --- | --- |
| `tokens.generated.css` | `SkyTokens` plus a small `VaultTokens` overlay |
| `Sheet` | Sky sheet host with a `draggable` handle and the same springs |
| `Keypad` + `useEntry` | `VaultKeypad` composable with a `NumberEntryState` holder |
| `Gauge`, `CountUp` | `Canvas` ring with `Animatable`; `animateFloatAsState` |
| `Toast` with Undo | Snackbar host fed by a snapshot-based `UndoStack` |
| `store.ts` | Room repository + `StateFlow`, with `snapshot()` and `restore()` |
| `lib/smartParse.ts` | `QuickEntryParser` in `commonMain` |

## Limits

- Web motion and a desktop browser are not a phone. Judge feel on a real device before committing to timings.
- Camera, odometer photo reading, speech, share target and widget are mock-ups. The real versions need platform code and permissions.
- Demo fuel prices are placeholders. Check the current official prices and how often they change before shipping fuel-by-taka defaults.
- Bike photos are the same transparent reference photos the Kotlin app ships. Only six models have photos here.
