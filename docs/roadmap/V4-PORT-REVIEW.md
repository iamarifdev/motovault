# v4 port review (2026-11-19)

Scope: the uncommitted implementation on `feat/v4-literal-port` (188 files changed), compared
with the v4 prototype code in `design/app-ui-v4/src/` and the checklists in
[V4-PORT-PLAN.md](V4-PORT-PLAN.md). Method: six read-only code reviews (one per plan phase),
spot-checks of the top findings in code, and a walk through the app on the iPhone 18 Pro Max
simulator. Nothing was changed.

Paths are relative to `composeApp/src/commonMain/kotlin/com/motovault/app/`.

## Summary

The structure is mostly right: screens, sheets, routes, copy keys and most sizes follow v4.
What feels "off" comes from four kinds of problem:

1. **A few real bugs** that break core flows (carousel, voice, sheet drag, an onboarding crash).
2. **Data risks** in document and service save paths.
3. **Many small visual drifts**: font sizes 1sp smaller than v4 (15 vs 16, 13 vs 14), wrong
   paddings and gaps, a Material shadow where v4 uses a custom one, plain text fields where v4
   uses a date picker.
4. **Missing motion**: sheet exit on save/swap, wallet entry, model list stagger, and the
   carousel spring/threshold.

## P0: bugs that break flows

| # | Where | Problem | Fix |
| --- | --- | --- | --- |
| 1 | `ui/garage/ShowroomCarousel.kt:119-128` | **Carousel swipe never switches the bike (the colour bug you saw).** The settle listener is a `LaunchedEffect(pagerState)` that captures the first `onSwitchBike`. That lambda uses the first `state` and, through `MotoVault.commit`, the first `revision`. After any later commit the save is rejected silently. The pager stays on the swiped page while the active bike (and colour) stays the old one. Seen on the simulator: Yamaha active, KTM image shown, swipes do nothing. | Wrap `onSwitchBike` in `rememberUpdatedState`. Listen to `settledPage` (drop the first value). Dots and neighbour taps should animate the pager and let the settle listener switch (v4 switches after the slide settles). Re-sync the pager when `settledPage != activeIndex`. Set `flingBehavior` to v4's 22% threshold and spring 420/40/.9. |
| 2 | `ui/fuel/FuelEntryState.kt:64-66` | Fuel voice state (`status`, `banner`, `disclosureOpen`) uses `mutableStateOf` without `remember`. Every change resets it, so the mic never shows "listening", notes vanish, the disclosure card cannot stay open. | Wrap in `remember { }` (as `ServiceHeaderPanels.kt:112` does). |
| 3 | androidMain `ui/VoiceCapture.kt:28,32,52` | `localAvailable` is true when any on-device recogniser exists, even without Bangla. `bn-BD` then fails as "unavailable" and never offers the cloud provider with disclosure. Bangla voice will not work on most phones. The disclosure also shows on every tap (D8 says one time). | On `LOCAL_UNAVAILABLE` offer the disclosure path when a provider exists. Remember consent once. |
| 4 | `ui/maintenance/ServiceHeaderPanels.kt:69` | Closing the service SmartInput panel does not stop voice; transcript keeps filling hidden text. | Cancel voice and clear text when the panel closes. |
| 5 | `ui/MotoVault.kt:232` → `ui/VaultCreation.kt:103` | **Onboarding crash.** Keypad allows 9 digits and voice has no limit, but `create` requires odometer ≤ 9,999,999 and nothing catches the throw. | Apply v4's `< 1,000,000` on voice, cap the keypad / disable the CTA, catch the failure. |
| 6 | `ui/components/SheetDrag.kt:77-82`, `Sheet.kt:173,233` | Sheet drag measures the finger on a surface that moves with it, so the sheet follows at about 0.4× and needs about 2.4× more travel to close. Threshold is checked on the sheet offset, not the finger. | Read the pointer on a parent that does not move; compare 110dp / 650dp/s to finger travel. |
| 7 | `ui/components/Toast.kt:152-168` | Same problem for toast swipe: about 213dp needed instead of 80. | Same fix. |

## P1: data risks

| # | Where | Problem | Fix |
| --- | --- | --- | --- |
| 8 | `ui/documents/DocAddSheet.kt:231-234` → `domain/vault/DocumentUpsert.kt:66,119` | OCR fills number/holder while the optional section stays closed. The rider saves values they never saw, and upsert marks them `detailsConfirmed`, so the service passport exports them as confirmed. | Open the optional section when OCR fills it; never set `detailsConfirmed` from OCR. |
| 9 | `DocAddSheet.kt:73-78,276-280`, `ui/sheets/DocumentSheetRoutes.kt:30-44`, `RenewalSheet.kt:95-100` | If the sheet is dragged closed during the save, `onDispose` deletes the new pages/receipt, then the save stores a document pointing at deleted files. | Mark assets committed before `onCommit`, or block dismiss while saving. |
| 10 | `ui/maintenance/ServiceDraftState.kt:154-157` | Editing a cancelled visit saves it as paid and resets reminders; approved becomes quotation. | Keep the original state when it is not paid/quote, or block editing. |
| 11 | `ServiceDraftState.kt:128-141`, `domain/workshop/ServiceVisitUpdate.kt:161` | Warranty months on a quote are lost on reopen and on quote → paid. | Store `warrantyMonths` on the visit. |
| 12 | `RenewalSheet.kt:85,179-185` | For a document expired over a year ago, the default new expiry passes the UI check but the domain rejects it (must be after today); the user gets a generic error. | Default to max(previous, today) + 365 and check `> today` in the UI. |
| 13 | `ui/sheets/DocumentSheetRoutes.kt:32-44` | Failed document save does nothing visible. | Show `saveError`. |
| 14 | `DocAddSheet.kt:41`, `DocumentUpsert.kt:25,121` | Editing a legacy document with an unknown kind rewrites it as registration. | Keep the stored kind or block editing. |
| 15 | `domain/ledger/WorkLedger.kt:56-58,113-114` | Work-ledger daily cost also counts expenses and document fees (v4: fuel and paid service only). Allocated/actual totals are too high. | Filter to fuel and service. |
| 16 | `ui/QuickService.kt:23-25` | A paid service older than a confirmed baseline is rejected, so riders cannot add old services. v4 saves it and just skips the reminder reset. | Save the log, skip the baseline reset. |

## P2: wrong names and copy (visible on every screen that uses them)

- **English rule names** ("Chain lubrication" on Home): `ui/VaultCreation.kt:114-131` creates
  3 English rules with no `catalogueId` when onboarding sets `factoryProfileId = null`.
  "chain lubrication" matches no label, so it shows in English. v4 seeds 5 Bangla rules (oil 5000,
  chain 500, air 17000, spark 17000, brakes 5000). Fix: use the v4 rule set with catalogue ids.
  This predates the port.
- **Registration name**: `ui/DocumentLabels.kt:14` maps `"registration"` to "রেজিস্ট্রেশন
  সার্টিফিকেট"; v4 says "রেজিস্ট্রেশন স্মার্ট কার্ড". Shows in wallet, next card, doc view,
  toasts, renewal.
- Copy not taken from v4: "অ্যাপিয়ারেন্স" (v4 "চেহারা"), "ডিসকাউন্ট" (v4 "ছাড়"), privacy
  sheet title and texts, settings labels ("থিম", "ভাষা (Language)", "নিজে থেকে লক হবে"),
  widget texts, onboarding voice notes, `capture_voice_unavailable`, scanning "বাতিল" (v4
  "বাতিল করুন"), renewal checklist/offline copy.
- History tags: "হিসাব / কোটেশন" plus approved/cancelled tags, warranty shown as a date (v4:
  "কোটেশন", "ওয়ারেন্টি N মাস"). Extra "নোট" row not in v4.

## P3: visual drift by area

**Shared (Phase 1)**
- Sheets close instantly on save or when one sheet opens another (Quick Add → Fuel); the new
  scrim starts from 0, so the screen flashes. v4 always slides the old sheet out (200ms).
- Android sheets never call `setDimAmount(0f)`, so the system dim stacks on our 0.6 scrim.
- Sheets without a footer add keyboard height twice (gap above the keyboard in pickers).
  Tab bar and toast use `safeDrawing`, so they rise with the keyboard.
- In-sheet toast sits on the Save button (v4: tab bar + 10).
- Date fields everywhere (fuel, service, expense, doc expiry, renewal, last-done editor) are plain
  text boxes needing `yyyy-mm-dd`; v4 uses a date picker with a max/min date.
- Tabs dispose their screen on switch, so in-screen state is lost (v4 keeps all 4 mounted).
- Android haptics use fixed constants; v4 patterns not implemented.
- PrivacyGate adds a bordered help card and a frame around the document; titles not bold.
- Toast undo 15 (v4 16), pill 13 (v4 14), brand fallback ktm (v4 yamaha).

**Garage (Phase 3)**
- Brand colour changes before the slide moves on dot tap (v4: after it settles).
- Stage not offset −6, dots spacing larger, bike shadow is a separate oval instead of a
  drop-shadow on the image, neighbour fade follows the finger instead of a 300ms transition.
- Owner-estimated baselines are shown as "unknown" in gauge detail and jobs (v4: only
  `unknown` counts as unknown).
- Jobs row title uses catalogue short label instead of v4's split on " ও "/" পরিষ্কার".
- Gauge detail and breakdown text 15/14 where v4 is 16; padding 14 all round where v4 is 14/16.

**Onboarding (Phase 2)**
- Keypad and privacy line do not sit at the bottom of the screen (whole step is one scroll column).
- Model row padding 12 (v4 8 14 8 8, gap 14); variant showcase 116 including padding (v4 136).
- Custom bike card uses Material text fields instead of v4's `.mini` boxes.
- No stagger on the model list, step transition flips direction on Back (v4 always x +28).
- Search hiding the selected family disables the CTA.

**Entry sheets (Phase 4)**
- Fuel and service header icons are bare (v4: tinted circle buttons).
- "গ্রেড?" pill twice too tall; bill name/price 15/14 (v4 16/16), unknown price in brand colour.
- Extra visible "অন্যান্য" misc row in the bill (v4 keeps misc in the total only).
- A first-time rider must confirm the default fuel price (v4 treats the seed price as known).
- Fuel edit can save ৳0; no double-tap guard on fuel save; custom row uid can collide.

**Documents (Phase 5)**
- Wallet shadow is a Material shadow (v4: upward shadow on the card behind), so the stack looks
  flat. Extra 56dp minimum exposure not in v4. No card entry/press motion.
- "সব ডকুমেন্ট" header hidden when empty. `+` header button has no tint circle.
- Saved full-page photos (tax, insurance, fitness) cropped into the card shape.
- Photo hints loop forever (v4 stops on the last, green hint).

**History and Profile (Phase 6)**
- Open history row shadow is clipped by the swipe container, so it never shows.
- Detail area still has the 70dp left indent (v4 overrides it to 14).
- Screen gap 8 everywhere (v4 20 between blocks); "মোট খরচ" 18sp (v4 24 bold).
- Privacy sheet hero is a bordered card (v4 plain column, 26sp title); no "checking" body;
  warnings use red/warn tint (v4 neutral box); turn-off failure says "could not turn on".
- Recovery list has no pinned footer and no cancelled state.
- Work-ledger sheet wraps the switch in an extra card; cloud card sizes 12/14 (v4 13/16).

## Checklist status

| Phase | Status |
| --- | --- |
| 1 Foundation | Partial: sheet drag, exit animation, toast swipe, tab state, haptics |
| 2 Onboarding | Partial: layout drift, crash, copy; data mapping and catalogue done |
| 3 Garage | Partial: carousel broken; rest done with small drifts |
| 4 Entry sheets | Partial: voice broken, service edit state/warranty, date input |
| 5 Documents | Partial: data risks 8, 9, 12-14; registration name; BRTA card done |
| 6 History/Profile | Partial: logic done; spacing, privacy sheet, copy |

Done well: catalogue (94 variants match v4), BRTA card geometry, wash shape and colours, keypad,
chip line, Quick Add, long-press pill, health logic, jobs order, document upsert keeping pages
and renewals, delete with Undo safe from file cleanup, history filters, D12/D13/D14.

## Size rule (functions over 80 lines)

`ServiceSheetHost` 155, `ToastCard` 114, `OdometerSheet` 103, `EntryAmountCard` 99,
`NamePickerContent` 98, `OnboardingOdometerStep` 94, `MotoVault` 84, `LockedGateBody` 84,
`DocPhotoStep` 84, `SheetDialogContent` 83, `ServiceMoreSection` 82, `GaugeDetailSheet` 81,
`RenewalGuideSection` 81, `ServiceSaveMapper.save` 81. No file over 400 lines.

## Also

- `Ride.cc` has round-trip tests but no frozen new-format fixture with `"cc": 155`.
- Dead code: `replaceServiceVisit`, `HistoryServiceMetaSection` (no caller, so history quote
  approve/cancel buttons are gone), 10 unused document strings, helpers in `ui/DocumentCapture.kt`.
- The plan said to delete 4 "old" drawables, but `BikeCatalogueFamilies.kt` uses them; keep them.

## Native mobile patterns instead of literal web controls (for owner review)

v4 is a web prototype, so some of its controls are browser built-ins (`<input type="date">`,
plain text inputs, `<details>`, `navigator.vibrate`). Copying those literally gives a weaker
mobile experience. Proposal: keep v4's look, chips and copy, but use the pattern people already
know from popular apps (Google Pay, bKash, Pathao, Uber) where v4 only shows a browser control.

| # | Where v4 uses a web control | Proposed mobile pattern | Notes |
| --- | --- | --- | --- |
| N1 | `<input type="date">` in fuel, service, expense, last-done editor, doc expiry, renewal | **Bottom-sheet calendar.** Keep the quick chips (আজ / গতকাল, 1/2/5/10 বছর পর). "অন্য দিন" or tapping the date opens our own `Sheet` with a month calendar (Material 3 `DatePicker` styled with our tokens), Bangla months and digits, past/future limits enforced (greyed days), a year jump for far expiry dates, and a pinned "ঠিক আছে" button. The chip then shows the formatted date. | Same component on Android and iOS, so it matches the rest of the app. Replaces every free-text `yyyy-mm-dd` box. One shared `DatePickerSheet` in `ui/components/entry/`. |
| N2 | Plain `<input>` text fields | Correct keyboard per field: phone pad for workshop phone, numbers for cc, plain text with sentence case for names and notes; IME "Next"/"Done" moves focus; search fields use the search action; auto-focus where v4 auto-focuses (pump and workshop pickers, last-done km). | Small change in each field. |
| N3 | `navigator.vibrate` timing patterns | Platform haptics: iOS `UIImpactFeedbackGenerator` / `UINotificationFeedbackGenerator` (success, warning, error); Android predefined `VibrationEffect` (tick, click, heavy click, double click) instead of raw millisecond waveforms. | Feels native on each phone; v4 timings are a web approximation. |
| N4 | Sheet close by drag only | Also Android predictive Back animation on sheets; keep drag down on both platforms. | Back already closes sheets; this adds the system animation. |
| N5 | Browser-style scroll | iOS: tapping the status bar scrolls tab screens to the top (in addition to the v4 jump pill). | Expected on iOS. |
| N6 | `tel:999` link | Open the phone dialer with 999 filled in (no auto-call). | Same as v4 behaviour on a phone; listed for clarity. |
| N7 | Swipe reveal row | Keep v4's swipe reveal, and also offer delete in a long-press menu for riders who do not find the swipe. | Optional. |

Recommended now: N1, N2, N3. N4-N7 are smaller follow-ups.

## Progress on fixes (2026-11-19)

| # | Status |
| --- | --- |
| P0-1 Carousel | Fixed: the swipe handler always uses the latest data, switches after the slide settles, resyncs if a save fails, and snaps with v4's 22% threshold and spring. Dots and neighbour taps now move the slide first, then switch. |
| P0-2 Fuel voice state | Fixed (`remember`). |
| P0-3 Android Bangla voice | Fixed in one shared place (`ui/capture/LocalFirstVoice.kt`): local recogniser first; if it has no Bangla, the privacy notice is shown once per app session, then the cloud recogniser is used. Applied to fuel, service, paste, odometer and onboarding. Consent is kept for the session only; saving it across launches needs a settings field (follow-up). |
| P0-4 Service voice panel | Fixed: closing the panel stops voice and clears the text. |
| P0-5 Onboarding crash | Fixed: voice accepts only 0 < km < 1,000,000 (v4), the start button is disabled above the vault limit, and creation can no longer throw. |
| P0-6 Sheet drag | Fixed: drag follows the finger at v4's 0.7 factor; close is decided by finger travel (110dp) or finger speed (650dp/s). |
| P0-7 Toast swipe | Fixed: swipe measured outside the moving layer; closes at 80dp of finger travel. |

## Suggested fix order

1. P0 1-7 (one change set; carousel first).
2. P1 8-16 with unit tests.
3. P2 names and copy (rule seed, registration label, copy sweep against v4 TSX).
4. Shared P3 items (sheet exit animation, date picker, keyboard insets, tab state).
5. Per-area P3 drifts, then the size-rule splits.
