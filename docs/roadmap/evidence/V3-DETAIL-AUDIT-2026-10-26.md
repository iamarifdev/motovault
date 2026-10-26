# v3 detail audit, 2026-10-26

## Scope and result

Read-only comparison of the current `design/app-ui-v3/src` reference with the
active native route (`MotoVault.kt` -> `Screens.kt` / `Sheets.kt` /
`Components.kt`). HEAD: `48fce40321600e4fbdfe124a3bb63b3032276502`, with
existing uncommitted P01 changes. No application code changed in this audit.

Result: native design parity is incomplete. The reported Fuel mismatch is
confirmed in active source. The native UI uses Feather SVGs where v3 uses Lucide;
similar names do not establish matching artwork. The legacy `Glyph("fuel")`
Canvas drawing is not the icon used by the active Home quick action, so changing
that legacy helper would not fix the reported Home mismatch.

This report establishes source differences, not screenshot-measured pixel
differences. CSS px values below are reference design values; native dp/sp values
are source values, not physical-pixel measurements. No installed app identity,
new runtime captures, or yesterday's build were verified during this audit.

## Confirmed differences, in review order

| # | Area | v3 reference | Active native source | Finding |
|---|---|---|---|---|
| 1 | Home Fuel | `screens/Home.tsx:86`, Lucide `Fuel` | `Screens.kt:222`, `fi_droplet.svg` | Fuel-pump replaced by droplet. |
| 2 | Quick Add Fuel | `sheets/QuickAdd.tsx:9`, `Fuel` | `Sheets.kt:216`, `fi_droplet.svg` | Same mismatch in a separate entry point. |
| 3 | History Fuel | `screens/History.tsx:31`, `Fuel` | `Screens.kt:760`, `fi_droplet.svg` | Same mismatch on saved records. |
| 4 | Odometer icons | `screens/Home.tsx:88`, `sheets/QuickAdd.tsx:11`, `sheets/FuelSheet.tsx:122`, `sheets/ServiceSheet.tsx:180`: `Gauge` | `Screens.kt:224`, `Sheets.kt:218,542,1083`: `fi_activity.svg` | Activity waveform substitutes for gauge across four contexts. |
| 5 | Vault tab | `components/TabBar.tsx:10`: `FolderOpen` | `MotoVault.kt:83`: `fi_folder.svg` | Open-folder reference uses folder asset in native. Exact asset rendering still needs capture. |
| 6 | Home quick-action icon size | `screens/Home.tsx:92`: 26 | `Screens.kt:249`: 24dp | Native icons smaller inside matching 52-unit containers. |
| 7 | Tab icons | `components/TabBar.tsx:28`: 28, selected stroke 2.4 vs 2 | `MotoVault.kt:662`: 24dp; selection changes tint only | Size and active-stroke treatment differ. |
| 8 | Tab marker | `styles/app.css:27`: 28 x 4, `layoutId` movement in `TabBar.tsx:30` | `MotoVault.kt:678`: 24 x 3dp, per-item static colour | Smaller marker; shared sliding marker is absent here. |
| 9 | Centre plus | `styles/app.css:29`: 60, top -18, outer 5 ring and shadow; `TabBar.tsx:41`: icon 30 | `MotoVault.kt:581`: outer 56dp, top -14dp, inset 4dp; icon 28dp at :608 | Size, rise, ring construction and shadow treatment differ. |
| 10 | Quick Add icons/labels | `QuickAdd.tsx:24`: icon 28; `app.css:179`: heading 1.125em (18 at base16) | `Sheets.kt:256`: icon26dp; :261 heading16sp | Both reduced in native. |
| 11 | Quick Add paste shortcut | `QuickAdd.tsx:31`: Sparkles20; `app.css:181`: dashed border | `Sheets.kt:285`: `fi_zap.svg`22dp; :273 solid border | Wrong symbol, different size and border treatment. |
| 12 | Vault empty state | `screens/Vault.tsx:34`: ScanLine36 and ScanLine button | `Screens.kt:563`: FileText36; :570 camera button | Empty-state and action symbols both differ. |
| 13 | Profile placeholder | `screens/Me.tsx:43`: Gauge32 | `Screens.kt:894`: activity32dp | Placeholder artwork differs even though size matches. |
| 14 | Onboarding identity | `screens/Onboarding.tsx:46`: Bike26; model fallback Bike34 at :82 | `Screens.kt:1198`: activity24dp | Header artwork and size differ; model fallback needs separate render review. Preserve existing native brand logos per contract. |
| 15 | Sheet heading | `app.css:106-107`: min-height56, padding 0/8/4/20, title1.5em (24 at base16), border1 | `Sheets.kt:157-174`: padding20/12/8, title22sp, divider0.5dp, no same explicit header min-height | Shared difference affects every sheet using this host. |
| 16 | Dark toast | `app.css:377,380`: light background/dark text, dark-blue Undo in dark theme | `Components.kt:894,906,929`: hardcoded dark background/light text/light-blue Undo | Native toast does not implement the reference dark-theme inversion. |
| 17 | Toast layout/Undo | `app.css:376,379`: padding12/8/14/16; Undo min-height40, radius12 | `Components.kt:900,915,921`: padding16/14, Undo radius10 and padding10/6 without equivalent explicit minimum | Spacing and Undo control geometry differ. |

## Matching foundations checked in source

- Home quick-action grid: four columns, gap10; icon container52, radius16,
  brand tint and brand-line border1 agree (`Screens.kt:217-249`, `app.css:134-137`).
- Quick Add: two-column layout, gap12, card minimum140, padding16, radius16 agree
  (`Sheets.kt:222-242`, `app.css:176-178`). Internal spacing still differs.
- Shared button: normal minimum52, horizontal padding20, radius12; small
  minimum40, padding14, radius10; press scale0.97 agree
  (`Components.kt:98-105`, `app.css:32,34`, `components/ui.tsx:25`).
- Chip minimum42, padding14, border1.5, radius999 and press scale0.95 agree
  (`Components.kt:235-256`, `app.css:46`, `components/ui.tsx:46`).
- Switch track52x32, padding3, thumb26, radius16 agree
  (`Components.kt:370-383`, `app.css:62-64`). Native thumb shadow needs visual review.
- Sheet top radius20 agrees with `tokens.generated.css:43`.
- Anek Bangla body and Barlow Condensed display families are present in native
  `Appearance.kt:192-209` and reference `styles/base.css:3-9,32`.
  Matching family names do not verify weight shaping or rendered line breaks.

## Screen/state checklist and remaining limits

| Surface | Source review result | Still needed |
|---|---|---|
| Home | Fuel, odometer, quick-icon sizing differences above | Matching data screenshots; gauges, chart, status badges and text baselines |
| Vault | Empty-state symbols differ | Every document kind/front/back, expiry/no-expiry, original-image fit |
| History | Fuel symbol differs | Expanded rows, filters, empty state, delete/Undo and scrolling |
| Me | Profile fallback differs | Widget preview, all settings/control states, large text and every accent |
| Onboarding | Header symbol differs | Every step, selected/unselected cards, search, model fallbacks; retain native logos |
| Quick Add | Fuel/Gauge/Sparkles, sizing and dashed-border differences | Matched screenshots and press/entry animation |
| Fuel sheet | Gauge substitute plus shared-host differences | Price editing, full/partial tank, keypad, date/place and error states |
| Service sheet | Gauge substitute plus shared-host differences | Copy/Sparkles/Wrench geometry, quantities, reset note and errors |
| Odometer sheet | Shared-host differences apply | Camera/result/manual variants and confirmed-value typography |
| Document add/view | Scan symbol substitutions and shared-host differences | Capture steps, reminders, inspection/brightness and original preservation |
| Paste sheet | Shared-host differences apply | Smart-input symbols, parsing, empty/error/result states |
| Gauge detail / rule | Shared-host differences where used | Unknown/known/basis/source states and comparison of all gauge artwork |
| Reminders | Existing P01 extension evidence remains relevant | Full light/dark and permission/snooze visual comparison |
| Shared navigation/toast | Multiple confirmed differences above | Safe areas, dark theme, reduced motion and all tab transitions |

Source review is sufficient to reject a claim of complete v3 application. It is
not sufficient to sign off the unrendered cells above. No fixes, build, tests or
completion status changes were made. Existing P01 design, physical-device and
spoken-accessibility gates remain open.

## Suggested correction order

1. Introduce exact v3 icon artwork in the MotoVault consumer, then map Fuel,
   Gauge, Sparkles, FolderOpen, ScanLine and Bike at every active call site.
   Do not globally replace Droplet: the fuel-reminder nudge legitimately uses
   Droplets in v3. Do not edit the reusable Sky assets globally for this app.
2. Correct shared navigation, sheet header, toast and Quick Add geometry.
3. Capture matching synthetic states side by side on the existing iOS27
   iPhone18 Pro Max simulator and Android; inspect the outstanding checklist.
4. Correct confirmed remaining differences, then run appropriate native checks.
   Record visual and runtime evidence separately; retain physical iPhone,
   haptics and screen-reader gates until those can actually be verified.
