# MotoVault UI/UX, motion and input evaluation

Date: 23 October 2026. Scope: the current `main` source (commit `a1183a4` plus untracked docs), the merged Android manifest and debug APK in `composeApp/build`, and the Sky design system tokens. The source review was then checked by running the app on an **iPhone 18 Pro Max simulator (iOS 27, 440 × 956 pt)**, on a fresh simulator so no owner data was touched. Section 11 lists what the simulator confirmed, corrected and could not test. Nothing was run on Android.

Companion documents: the earlier Rams audit in `DESIGN-IS-2026-10-14/` and the adoption notes in `docs/SKY-UI-VERIFICATION.md`. This review does not repeat them. Where an old finding is still open, it says so.

## 1. Verdict

MotoVault has a good base: Bangla-first copy, a brand accent per bike, real bike photos, an honest tone and a careful scan flow for documents. It does not yet feel like a modern, effortless rider app. Three reasons:

1. **Logging is slow.** Every record is a long form with typed values. A rider on a bike at a fuel pump needs two taps and one number.
2. **Nothing moves with meaning.** Two animations exist in the whole app (tab fade, sheet slide). There is no haptic feedback anywhere.
3. **Bangla typography is mostly unverified and partly wrong.** Tiny sizes, letter spacing on Bangla, and a variable font used without its weight settings.

| Area | Score /5 | One-line reason |
| --- | ---: | --- |
| Visual identity and colour | 4 | Strong brand accents, all 18 accent pairs pass contrast. Red brands collide with error red. |
| Typography | 2 | 45 text styles at 9 to 11 sp, spaced Bangla, likely fake bold. |
| Layout and information priority | 2.5 | Urgent care sits below the bike photo. Home screen has no quick action row. |
| Motion and fluidness | 1.5 | Fade and slide only. No haptics, no reduced-motion link, blocking save dialog. |
| Input effort | 1.5 | Typed dates, English-only number parsing, 13 inputs in setup, no automation. |
| Bangladesh fit | 2 | Catalogue and wording fit. Fuel, price and digit handling do not. |
| Trend alignment | 2.5 | Wallet stack and sheets are current. Home widgets, quick-add and smart capture are missing. |

## 2. Fix first (defects, not taste)

| # | Finding | Evidence | Effect |
| --- | --- | --- | --- |
| P0-1 | The Android manifest names `.ui.redesign.RenewalApplication` and `.ui.redesign.RenewalAlarmReceiver`. The classes live in `com.motovault.app.ui`. | `AndroidManifest.xml:13,20`. Merged manifest lines 28 and 46 use the `redesign` name. The APK dex contains only `com/motovault/app/ui/RenewalApplication`. | On Android the app very likely crashes at launch with `ClassNotFoundException`. Not run on a device, so confirm first. iOS is not affected. |
| P0-2 | Decimal fields (odometer, litres, price, quantity) are parsed with `toDoubleOrNull()`. On the JVM that rejects Bengali digits (checked: `Double.parseDouble("১২০০")` throws, `Long.parseLong` accepts). Save then stays disabled with no message. | `Field(...)` callers in `OwnershipForms.kt` and `Onboarding.kt`. Only `SmartServiceDraftParser` converts digits. | A rider whose Android keyboard types Bengali digits cannot save a fuel fill. Not reproduced on iOS: the iOS decimal pad showed Latin digits, and I could not type Bengali digits there. The service form even tells riders to "write ০ for free" (Bengali zero). |
| P0-3 | Fuel dialog defaults "full tank" to unchecked. | `OwnershipForms.kt:36` (`existing?.fullTank ?: false`). | Most fill-ups are full. The default makes them partial, so economy is never computed unless the rider finds the checkbox. |
| P1-1 | English text still reaches the Bangla UI: document badges ("Valid", "Expired", "N days", "No expiry"), validation errors from `require(...)`, document-fee history titles ("Tax token renewal"), schedule names such as "Chain" in the service form. | `VaultState.kt:67-75`, `VaultState.kt:79-140`, `VaultState.kt:~290`, `OwnershipForms.kt:212`. `documentDisplayName` only maps five document titles. | The language audit says this was fixed. It was fixed for labels, not for these data-driven strings. |
| P1-2 | `↗` and `‹` are text characters. iOS draws `↗` as a blue emoji square. | Screenshot `03-garage.png` ("সব যন্ত্রাংশ", "ছবির সূত্র"). | Visible glitch on the home screen. |
| P1-3 | Every save shows a full-screen modal dialog ("রেকর্ড সেভ হচ্ছে…"). | `MotoVault.kt:279`. | Flashes for fast saves and blocks the screen. |
| P1-5 | Overdue chain shows "সময় পেরিয়েছে" with "0 km / ৩০ দিন বাকি" underneath. | Simulator: Garage priority card (section 11). | The card contradicts itself. It should show how far overdue the part is. |
| P1-6 | iOS decimal pad has no Done or Next key, and the keyboard hides Save and the next fields. | Simulator: fuel form and setup (section 11). | Riders must find their own way to close the keyboard on every numeric form. |
| P1-4 | The reduced-motion setting is never connected. `VaultTheme` calls `SkyTheme(...)` without `motion = SkyMotionPolicy(...)`, so the default (`false`) is always used. | `Appearance.kt:109`, `SkyTheme.kt:51`. | Users who turn off animation on their phone still get them. |

## 3. Typography

Fonts: Anek Bangla (body) and Barlow Condensed (bike name, Latin only).

1. **Variable font used as four static weights.** `anek_bangla.ttf` has an `fvar` table (variable). `MotoVault.kt:191-196` registers it four times with different weights and no `FontVariation` settings. The screenshots show heavy headings with crowded glyphs (`05-vault.png`), which is what synthetic bold looks like. Fix: pass `variationSettings = FontVariation.Settings(FontVariation.weight(w))` for each entry, or ship static instances. Check this against the Compose Multiplatform 1.7 API on iOS before relying on it.
2. **Small sizes.** Count of literal sizes in `commonMain`: 9 sp ×2, 10 sp ×15, 11 sp ×28, 12 sp ×55, 13 sp ×23. That is 45 uses below 12 sp. Bangla conjuncts and vowel marks need more room than Latin. Set a floor of 12 sp for any text, 14 sp for body text, 16 sp for form values.
3. **Letter spacing and upper case on Bangla.** `Eyebrow` uses `uppercase()` and `letterSpacing = 1.6.sp` at 9 sp bold (`MotoVault.kt:396`). Upper case does nothing in Bangla, and spacing breaks joined letters (see "গুরুত্বপূর্ণ কাগজ এক জায়গায়" in `05-vault.png`). Remove both for Bangla text. Keep them only for Latin labels.
4. **No type scale.** Sizes are typed in each call (142 literal sizes, 16 distinct values). Sky roles exist but are used for fields and nav only. Define five roles: Display, Title, Body, Label, Caption. Map every screen to them.
5. **Numbers.** Odometer and money use Latin digits and 3-digit groups, step counters use Bengali digits, dates use `YYYY-MM-DD`, gauges show Latin `100`. Pick one rule: show Bengali digits when the app language is Bangla, accept both on input, format money with Bangladeshi grouping.
6. **Heading gradient.** `SkyGradientHeading` runs brand accent to status green. On KTM that is orange to olive (`08-service-scrolled.png`). It looks muddy and ties brand colour to the "healthy" status colour. Use a solid ink colour for headings and keep the accent for actions.
7. Small items: `FontFamily(Font(...))` for Barlow is rebuilt on every recomposition (`MotoVault.kt:293`). Only 8 of 15 Material text styles take the Bangla font.

## 4. Colour and theme

What I measured (WCAG contrast, all computed from `Appearance.kt` and `SkyTokens.kt`):

| Check | Result |
| --- | --- |
| Light accent with white button text, 9 brands | 5.76 to 6.95. All pass 4.5:1. |
| Dark accent with dark button text, 9 brands | 7.14 to 10.58. All pass. |
| Dark accent on dark canvas and surface | 5.80 to 10.48. All pass. |
| Status colours (healthy, warning, error) light and dark | 6.23 to 11.2. All pass. |
| Field border, light and dark | 4.13 and 3.92. Pass the 3:1 rule for controls. |
| Sky `fieldBorder` and card `line` in light mode | 1.32. Cards and dividers are very faint. Fine for decoration, not for controls. |

The earlier audit checked only a few pairs. All 18 brand pairs now pass.

Problems:

1. **Red brands collide with error red.** Honda `B42A38`, TVS `A52B41` and Hero `B1362C` sit next to error `AD2938`. Honda and error are 7 RGB steps apart. A red primary button looks like a danger button, and a real error looks like a normal action. The delete confirmation uses the normal accent button (`MotoVault.kt:269`). Fix: destructive actions use an error-container style with an icon; for red brands, add an icon or shape to every status, never colour alone.
2. **Bike photo in dark mode.** `ModelPhoto` forces `EFEFED` behind every photo and multiplies the image with it. On `101411` that is a 16:1 bright slab (`04-garage-dark.png`), the loudest thing on the screen. Correction after checking the assets: the photos are already transparent PNGs, so the slab is a code choice, not an image limit. The fix is small: drop the `EFEFED` background and multiply filter, and put the photo on a brand-tinted gradient well in both themes (done in the v3 prototype, see section 12).
3. **Hard-coded colours bypass the theme.** `EFEFED`, `535B65`, the wallet stripe colours (`33795B`, `4567AC`, `A37B38`), camera overlay greens, `Color.White` in inspection mode. These do not follow brand or dark mode. Move them to palette roles.
4. **Dynamic colour.** The app does not use Android 12+ Material You colours. That is a correct choice for a brand-per-bike app. Keep it, and say so in the design notes, so nobody "fixes" it.
5. **Gauge colour carries the status.** Ring colour changes at 25% and 0% with no icon or shape. About 1 in 12 men has red-green colour weakness, and the healthy and warning pairs sit close in lightness. Add a symbol (tick, clock, alert) inside or beside the ring.
6. Dark theme is not black-first. `101411` and `202723` are green-tinted greys, which suits the Sky base but fights red and blue bike brands. Consider tinting the dark surfaces with the brand hue at 4 to 6%.

## 5. Motion, animation and fluidness

What exists:

| Motion | Where | Quality |
| --- | --- | --- |
| Tab crossfade, 300 ms | `AnimatedContent` in `MotoVault.kt:217` | Fine, but scroll position and History filter reset when you switch tabs. |
| Bottom sheet slide, 250 ms in, 200 ms out | `Sheet` | Smooth. The drag handle is drawn but cannot be dragged. No swipe to close. No Android predictive back. |

What does not exist: gauge sweep or count-up, list insert and remove animation, inline error and draft expansion (they push the layout), success feedback, button press feedback beyond the platform ripple, haptics, camera shutter feedback, a skeleton or splash while loading (it shows one spinner at top-left, `MotoVault.kt:209`).

Recommended motion system (small, five tokens):

| Token | Time | Use |
| --- | ---: | --- |
| instant | 100 ms | press, toggle |
| quick | 180 ms | chip, inline error, field focus |
| standard | 280 ms | tab, card expand |
| emphasis | 420 ms | sheet, success |
| sweep | 600 ms | gauge fill, odometer count-up, staggered 60 ms |

Specific changes:

1. Gauge rings animate from 0 to value on first show, and animate again when the value changes (`animateFloatAsState`).
2. Odometer and totals count up when they change.
3. Save: no modal. Close the sheet at once, show a snackbar with Undo, and write in the background. Show a spinner only if the write takes over 300 ms.
4. Lists use `Modifier.animateItem()`. Inline errors and the smart-text result use `AnimatedVisibility`.
5. Sheets: add drag to dismiss and predictive back. Make the handle real.
6. Haptics on save success, invalid input, stepper taps and shutter. There are none today.
7. Overdue ring: one slow pulse when the screen opens, never looping.
8. Connect reduced motion: read the system setting on both platforms and pass `SkyMotionPolicy`.
9. Keep scroll state per tab with `rememberSaveable` outside `AnimatedContent`.

Fluidness risk (not profiled): receipts and document photos are stored as Base64 text inside the same state object as the records (`RideLog.receiptBase64`, `VaultDocument.frontPhoto`), and every save writes the whole state. A few receipts of up to 1.4 MB each will make every save slower. Low-end Android phones, common in Bangladesh, will show this first. Store images as files and keep only IDs in the state.

## 6. Layout and current trends

Home (Garage) at 440×956 (`03-garage.png`): header, eyebrow, 38 sp bike name, photo, photo caption, photo source link, odometer panel. The gauges are cut off by the navigation bar, and the action buttons are further down. The earlier audit asked to move urgent care and quick logging above the photo. That is still not done.

Recommended Garage order:

1. Slim header: bike name, small logo, settings icon (replaces the 10 sp "থিম" text).
2. "Next up" card: the most urgent item, remaining km or days, and one button ("এখন লিখুন").
3. Quick action row: তেল, সার্ভিস, কিমি, কাগজ. Four large round buttons.
4. Odometer and economy numbers.
5. Gauges, sorted by urgency (today `state.rules.take(3)` shows the first three, not the worst three).
6. Photo as a smaller card. Move the photo source link to an About page.

Other layout points:

- A centre "+" in the bottom navigation that opens a quick-add sheet is now the common pattern in maintenance and fuel apps. It fixes the "where do I add?" problem on every tab.
- Icons come from three sources: Canvas `Glyph`, Sky Feather SVGs and text characters. Use Feather SVGs only.
- History cards carry up to 8 lines at 10 to 11 sp, indented with spaces. Edit and delete are 10 sp text buttons with no padding. Use one summary line, tap to expand, swipe for actions, and give every target at least 48 dp.
- The document wallet stack (overlapping cards, `-36.dp`) is on trend. Keep it. Show expiry as a large chip, not 9 sp text.
- Trends worth adopting, from my own knowledge of current apps (not live-researched for this review): home-screen widgets and iOS Live Activities, Material 3 Expressive springs and shape change, floating glass-style bottom bars on iOS 26, large numerals, one hero "next up" card. Material 3 Expressive needs a newer Compose Multiplatform than the project's 1.7.

## 7. How input is taken today

Counted from source (inputs the rider must type or pick, before Save becomes enabled):

| Task | Taps and typing today | What blocks speed |
| --- | --- | --- |
| Update odometer | Tap panel, type number, Save (3 actions) | Fine. Add quick chips (+50, +100) and an estimate. |
| Log fuel | Tap button, retype odometer (field holds the old value), litres, taka, tick full tank, Save. About 5 fields, one scroll. | Asks litres, but riders know taka. Date is typed text. Full tank defaults wrong. |
| Log service | Tap button, pick item, pick action, pick reminder, quantity, unit price, "add to list", odometer, date, labour, other, discount, Save. 12+ controls. Engine oil adds 9 more (SAE, API, JASO, brand, pack size, bought, used). | Three dropdowns before any value. Oil fields are always open. `YYYY-MM-DD` typed. |
| First setup | Step 3 holds 13 inputs: variant, year, market (4 radios), emission (up to 6 radios), model code, manual toggle, odometer, registration, last service km, date, confirm tick. | Market and emission (BS4, Euro 5) are irrelevant for most Bangladeshi riders. Confirm tick adds friction. |
| Add document | Choose type, camera, crop, "use these images", OCR, check each field, tick "verified", Save. | Careful and honest. Only English text is read (see below). |

Other gaps:

- **No date picker anywhere.** Six forms ask for `YYYY-MM-DD` as text, in a Bangla app.
- **No keyboard flow.** Zero uses of `ImeAction`, `FocusRequester` or autofocus. The rider must tap every field. Integer fields (year, km, days) use the decimal keyboard.
- **Disabled Save has no reason.** Fuel and service forms show no message for a bad field. Errors that do show are English.
- **Reminders only cover documents.** Service due dates (chain, oil) are only visible when the app is opened. No service notification exists.
- **ML Kit reads Latin text only** (`TextRecognizerOptions.DEFAULT_OPTIONS`). Bangla text on documents is typed by hand, and the app says so.
- No home-screen widget, no app shortcut, no share target, no voice input, no photo-to-odometer, no repeat-last-service.

## 8. Ideas for an effortless flow in Bangladesh

Ordered by value for effort. Items 1 to 6 need no new permission and reuse code already in the app.

| # | Idea | Why it fits Bangladesh | Reuses |
| --- | --- | --- | --- |
| 1 | **Fuel by taka.** Fields: taka and fuel type (অকটেন or পেট্রোল). Litres = taka ÷ price per litre. Price is remembered and editable. Default full tank to on. | People say "৫০০ টাকার তেল", not litres. Pump prices are set centrally and change by period, so keep the price editable instead of hard-coding it. | `logFuel` |
| 2 | **Accept Bengali digits everywhere** and show a gentle hint. One shared parser. | Many Bangla keyboards type Bengali digits. | `asciiDigits` in the parser |
| 3 | **Quick-add sheet** from a centre "+" and from the home action row. Opens with today's date and the estimated odometer already filled. | Two taps and one number for fuel. | New |
| 4 | **Date chips**: আজ, গতকাল, অন্য দিন (opens a picker). | Removes typed dates. | New |
| 5 | **Odometer estimate.** Predict from average km per day in the history. Show "≈ ১২,৪৫০ কিমি, ঠিক আছে?" with Confirm or Edit. | Most riders do not remember the reading. | History data |
| 6 | **Service quick chips and "same as last time".** Top 6 items as big chips, each remembers its last price. One tap adds a line. Oil details only open on request, with common grades (10W-30, 10W-40, 20W-40, 20W-50) as chips. | Local mechanics, cash, no receipts. Typical visit is oil plus filter plus chain. | Catalogue, parser |
| 7 | **Photo the odometer.** Camera reads the dashboard digits and the rider confirms. | Dashboards use Latin digits. Not reliable on 7-segment LCD, so confirm is mandatory with typed fallback. | CameraX, ML Kit |
| 8 | **Voice in Bangla.** "পাঁচশো টাকার অকটেন" fills a fuel draft. Needs Bangla number-word support in the parser. | Hands busy, low typing comfort. | Platform speech recognition, `SmartServiceDraftParser` |
| 9 | **Share into MotoVault.** Accept shared text from WhatsApp, Messenger or a bKash/Nagad confirmation and run it through the draft parser. Never read SMS in the background. | WhatsApp and mobile payment messages are where the numbers already are. SMS permissions are also restricted on Google Play. | Parser |
| 10 | **Service reminders by notification**, at 9 am like documents. Also a nudge after about 10 days without a fuel entry: "তেল ভরেছেন?" with a one-tap action. | Current alerts exist only for documents. | `RenewalReminders` |
| 11 | **Home widget** with next-due item and two buttons (তেল, কিমি). iOS: a Lock Screen widget. | Zero-open logging. | New |
| 12 | **Setup in three inputs**: brand, model, odometer. Everything else later. Default market to Bangladesh, drop emission from onboarding, allow "last service unknown". | First value in under a minute. | `Setup` |
| 13 | **BRTA renewals.** Prompt right after saving a document with an expiry date: "মেয়াদের ৩০ দিন আগে জানাব?" instead of an opt-in buried in the Vault. | Tax token, fitness and insurance dates are what riders forget. | Reminders |

Principles to keep: nothing is saved without the rider confirming it, no background location, no SMS reading, everything works offline. These match the app's current promises.

## 9. Suggested order of work

| Step | Work | Size |
| --- | --- | --- |
| 1 | P0-1 manifest names, P0-2 Bengali digits, P0-3 full tank default, P1-1 English strings, P1-2 arrow icons | 1 to 2 days |
| 2 | Typography: fix variable font weights, 12 sp floor, remove Bangla spacing and upper case, five text roles | 2 to 3 days |
| 3 | Date chips, keyboard flow (`ImeAction`, focus order, integer keypads), inline Bangla errors, fuel by taka | 3 to 4 days |
| 4 | Home re-order: next-up card, quick action row, centre "+" quick-add, smaller photo, sorted gauges | 3 to 4 days |
| 5 | Motion tokens, gauge sweep, count-up, optimistic save with Undo, haptics, reduced motion, drag-to-dismiss | 3 to 4 days |
| 6 | Colour: destructive style, status icons, dark photo treatment, move hard-coded colours | 2 days |
| 7 | Automation: odometer estimate, service chips, service reminders, share target | 1 to 2 weeks |
| 8 | Odometer photo, Bangla voice, widgets | 2 to 3 weeks each, after testing 1 to 7 with real riders |

Sizes are rough and assume one developer who knows the code.

## 10. Limits of this review

- Run on an iOS simulator only (see section 11). Android was not run.
- The Android launch crash (P0-1) is from static evidence: manifest versus compiled classes. Confirm on a device or emulator.
- No user testing. The effort counts are from the code, not from timed sessions. Test steps 3 to 5 with 5 to 8 riders, including older riders and low-end phones.
- Trend comments come from general knowledge, not from a fresh survey of apps.
- Fuel price behaviour in Bangladesh is described in general terms. Check the current official price and update rule before building idea 1.
- No performance profiling. The Base64 image storage concern is a risk, not a measurement.

## 11. Simulator verification (iPhone 18 Pro Max, iOS 27, fresh install, Yamaha R15 V4)

Walked: brand pick, model picker, setup step 3, Garage (light and dark), fuel form, service form, History, Vault, theme sheet. The app was typed into with the on-screen number pad. Screenshots lag the tap by about a second on this setup, so I waited before capturing.

### Confirmed on screen

| Finding | What the screen showed |
| --- | --- |
| Heading gradient | "কোন বাইক চালান?" and the sheet titles run brown to green (KTM) or blue to teal (Yamaha). Muddy, and it ties brand colour to status green. |
| Letter-spaced Bangla eyebrows | "আপনার গ্যারেজ শুরু হোক", "গুরুত্বপূর্ণ কাগজ এক জায়গায়" show widely spaced, broken-looking letters at about 9 sp. |
| Faux bold | "রাস্তায় বের হওয়ার আগে প্রস্তুত থাকুন" is heavy with glyphs almost touching. Looks synthetic. Not measured against a true bold. |
| `↗` emoji glitch | "সব যন্ত্রাংশ" and "ছবির সূত্র" show a blue emoji square on the current build. Also on the Vault link. |
| Garage first screen has no action | Header, bike name, photo, caption, odometer panel. Gauges are cut off by the navigation bar. Action buttons need a scroll. |
| Chain-only primary action | The big primary button reads "চেইনের যত্ন লিখুন". Fuel is the secondary outline button. |
| Dark mode photo slab | The bike photo stays a light grey slab on the dark canvas. It is the brightest element on the screen. |
| English leaks | "Chain" in the service form, "Add GPS location" on the GPS button, and the validation error "Use mileage greater than your latest fill-up." |
| Fuel defaults | Odometer pre-filled with the old reading, litres (not taka), full-tank box unchecked, date as typed `2026-10-23`. |
| Disabled Save has no reason | Empty litres leaves a grey Save button with no message. |
| Mixed digits | "2টি মডেল" and "17টি মডেল" (Latin digit, Bangla word), "4.5 L", "1,200 km", but "ধাপ ৩ / ৩" and "৩০ দিন" in Bengali digits. |
| Setup is long | Step 3 needs about three screens: market (4 radios), emission (up to 6 radios), model code, odometer, registration, last service km and date, confirm tick. "বাংলাদেশ" is the last market option, after India and Europe. Year defaults to 2023. |
| Snackbar covers the buttons | "রেকর্ড সংরক্ষণ হয়েছে" sits over the Garage action buttons for a few seconds, has no Undo, and is a bright white slab in dark mode. |
| History is crowded | Totals read "৳0.0" and "৳500.0". Service passport and verify buttons come before the first record. Edit and delete are tiny text. |
| Theme is hidden | The only way to reach dark mode (and the GPS setting) is a tiny "থিম" text button. Theme change is instant with no cross-fade. |

### New findings from the simulator

1. **Overdue message contradicts itself.** The Garage priority card says "চেইন · সময় পেরিয়েছে" (overdue) and then "0 km / ৩০ দিন বাকি" (0 km and 30 days left). The chain interval is 500 km and the bike is at 1,200 km, so it is 700 km overdue. The card should say how far overdue, not "0 km left". Colours also disagree: red status text, amber card, grey ring.
2. **iOS decimal pad has no Done or Next key.** Every numeric field uses it. With the keyboard open, the Save button and the next fields sit behind it, and the rider must find a way to dismiss it. This is the missing `ImeAction` problem, shown on screen.
3. **The date field opens the full letter keyboard**, so a rider types digits and dashes on a QWERTY layout.
4. **Small working area.** In the fuel sheet, the fixed header and intro text take about a fifth of the screen. With the keyboard up, only about three fields are visible.
5. **Validation error appears at the bottom of the form**, in small amber English text, away from the odometer field that caused it (that field was scrolled out of view).
6. **Model search box has no placeholder or search icon.** It is an empty rounded rectangle.
7. **Status bar band is a different shade** from the canvas in dark mode, giving a visible two-tone seam at the top.

### Corrected

- The English renewal-reminder panel in the 21 October screenshot is now Bangla. That item is closed. English data-driven strings (document badges, validation errors, fee titles) are still open.
- P0-2 is narrower than first written: confirmed for JVM parsing rules (Android), not reproduced on iOS.

### Not verified

- **Android launch crash (P0-1).** iOS only was run. Still static evidence.
- **Bengali digit typing on any device.** Pasting Bengali text into the number field did not land, so I could not test it directly.
- **Motion quality.** Stills cannot show smoothness. The motion findings are from code: one tab fade, one sheet slide, no haptics, no gauge animation. A screen recording on a real device is needed.
- **Document scan, OCR, camera, GPS and notifications.** The simulator has no camera and no document to scan. Those flows were read in source only.
- **Document wallet with real documents**, and the expiry badges ("Valid", "Expired"). Source only.
- **Light-mode fuel and service forms at large text sizes.** Not tested.

## 12. Prototype for review

A clickable web prototype of the proposed changes is in [design/app-ui-v3](../design/app-ui-v3/README.md). It uses Sky's own `tokens.json`, an in-app keypad with Bengali digits, taka-first fuel entry, tap-to-add service, estimated odometer, Undo toasts, drag-to-dismiss sheets, animated gauges and a reduced-motion switch. It was checked in a browser at 440 × 956 in light and dark, at 1.3× text, and through onboarding, fuel, service, odometer, document and History flows. Nothing in `composeApp/` has been changed. The README also explains how the app would be built from scratch on Sky and how each prototype piece maps to Compose.
