# App UI v2 verification · 2026-10-14

## Executed

- Syntax: app.js and ui-v2.js passed `node --check`.
- verify.cjs passed: Bangla/Latin numeric parsing, invalid/ambiguous input, 78 catalogue records / nine logos and asset existence, 30 service entries, itemized totals, missing-price distinction, bottle cost versus used amount, action-sensitive care mapping, short-text parsing and all 35 screen renderers/headings.
- Live browser: all 35 screens visited at 390px, with headings and no document/screen horizontal overflow.
- Live browser: all 35 screens visited at 320px using the reference's larger-text setting, with no document/screen horizontal overflow.
- Native-logo selection visually inspected; Honda opens its ten real-photo models. The full catalogue is copied from source, not a four-model replacement.
- Four font families reported loaded in the browser. Hind Siliguri selection changed the displayed selected-font label and whole-reference font setting. The user later selected Anek Bangla for the native app; its licensed variable TTF is bundled and Android/iOS compilation passes. Native visual rendering still needs device verification.
- Light/dark and English switching exercised. Full combinatorial checks for all themes/fonts/languages were not run.
- Oil + filter + labour bill: oil bottle ৳950, filter ৳250, labour ৳300 → saved total ৳1,500.
- Oil purchase/usage distinction exercised: one bottle billed at ৳950 with 0.9L used; bill remains ৳950 for that item. Saved detail retains the used-amount note.
- Higher service odometer saves in the same visit; no separate Garage detour.
- Automatic-location preference exercised in an isolated QA tab: subsequent service has a fixed sample position; workshop name edited and retained in saved detail. No real geolocation permission or position was requested.
- Fuel: previous sample rate 130 × 5 litres calculates ৳650; saved record shows type, quantity, rate, total and sample station.
- Smart Bangla text “অয়েল ৯৫০, ফিল্টার ২৫০, মজুরি ৩০০, ১২৬০০ কিমি” parses into an editable priced draft; it does not auto-save. Source checks verify grouped amounts, oil-filter precedence and ambiguous multi-amount rejection.
- Final reference uses separate storage from v1. The approved native redesign is now being applied in shared Compose code and platform location adapters.

## Fixes prompted by checks

- CFMOTO's white raster logo needed theme-aware monochrome filtering.
- Location buttons inside forms now use type=button, so changing place does not submit the service bill.
- Purchase unit and used oil quantity are separate; a bottle is not billed as only the fraction poured.
- Smart text does not invent 1L from an oil price. Oil total-expense mode supports unknown quantity.
- Inspection/adjustment does not automatically count as completed oil/chain care.
- Missing/invalid monetary values remain incomplete rather than being shown as a verified free item.
- All nine accents use the current native values, including KTM dark #FF7900.
- Font selection is a proper labelled button state; the sample card itself is not an inaccessible toggle.

## Remaining native / validation work

This is a visual/behavior implementation reference, not native runtime proof. Large-text mode is not a full 200% OS scale test. Contrast across every brand/status/state pair, native screen readers, keyboard order, physical camera/OCR, real GPS accuracy/permissions, Room migration runtime, receipts, PDF/page rendering and real notification delivery remain separate checks.

The service taxonomy is generic. Compatibility requires model/variant/manual evidence before automatic recommendations. Prices and coordinates are deliberately samples, never current market quotes or actual user location.

Native service/item editing now protects a newer schedule baseline, while document fees are separate ledger events. The browser reference remains design evidence only. Device QA must exercise those native flows and confirm that expense privacy remains intact during document inspection and export.

## Reproduce

```sh
rtk run node --check design/app-ui-v2/app.js
rtk run node --check design/app-ui-v2/ui-v2.js
rtk run node design/app-ui-v2/verify.cjs
rtk run python3 -m http.server 8877 --bind 127.0.0.1 --directory design/app-ui-v2
```

Use only sample records and nonsensitive files. Browser design controls must not be treated as implemented native features.

## Native implementation checkpoint · 2026-10-14

- Itemized service visits, a Bangla-first common-service catalogue, engine-oil metadata, unknown-vs-free prices, matching schedule links and service-history details are implemented in the shared Compose app.
- One-shot foreground GPS is implemented for Android and iOS. The preference defaults off and is persisted in Room schema v3; old v2 databases migrate with the preference off. Current-day fuel/service forms can capture location, while past-date forms cannot attach the current position. Manual area/workshop entry and GPS removal remain available.
- Android `testDebugUnitTest`, Android debug APK assembly, iOS arm64 and iOS Simulator ARM64 compilation, `Info.plist` lint, and aggregate `allTests` pass. The x64 iOS test target is skipped on this Apple Silicon configuration. Two iPhone 17 UI tests also pass after Xcode license acceptance.
- Anek Bangla rendering is confirmed in iOS 26.0 Simulator screenshots. Real-device GPS accuracy, disabled/stale/offline GPS cases, TalkBack/VoiceOver, and native document capture/import/export still need separate device QA.

## Phase 5 native checkpoint · 2026-10-14

- Service entry accepts one item per line for local suggestions, shows candidates for review, and keeps unknown text visible for manual custom-item entry. It never saves automatically.
- Itemized service and fuel history entries can be corrected under the same record ID. Fuel edits recalculate economy and reject reordered date/mileage. Service edits preserve a newer linked baseline and block edits that would move it backwards.
- Service visits accept one optional JPEG/PNG/WebP receipt up to 1 MB. Receipt data stays in the local ledger record and is omitted from service-passport export.
- Document renewal fees are independent ledger records, so editing document details cannot rewrite historic spend. They remain outside roadside inspection and the service passport.
- Passport v2 carries itemized quantities, prices, totals and oil details. Null amounts stay unknown, zero stays explicitly free, and legacy v1 JSON remains verifiable. Fuel, document fees, precise GPS, receipts and document originals remain excluded.
- Source tests cover parser review, service/fuel correction, baseline protection, document fees, export contents and v1 compatibility. Aggregate `allTests` passes with iOS x64 skipped on this Apple Silicon configuration. The latest iPhone 17 UI test suite passes two checks: Garage scroll and Bengali component labels; History navigation and filter layout. XCTest child frames remain `0×0`, so the UI tests tap native screen coordinates while asserting the resulting screen labels.
- On the isolated iOS 26.0 iPhone 17, KTM Duke 125 onboarding, model selection, dynamic KTM theme, and Garage/History were visually checked. The fuel form auto-attached the simulator's sample Dhaka location (±5 m), saved a ৳500 / 4 L entry with a GPS badge, and the entry persisted after returning Home and reopening the app. Fresh screenshots confirm the Garage service gauges now use compact Bengali-first labels, and History chips scroll horizontally without collapsing into vertical text. Native document capture/import/export, real-device GPS accuracy, system permission prompts, and VoiceOver remain separate QA gates.

## Accessibility and device-QA checkpoint · 2026-10-14

- On iPhone 18 Pro Max Simulator (iOS 27), the largest supported accessibility content-size category, dark appearance, and Increase Contrast were enabled together. The first onboarding screen remained vertically scrollable and readable. The test exposed clipped Bangla model counts in fixed-height brand cards; the cards now grow with their text. A rebuilt app screenshot confirms all nine counts fit at that size.
- The document-flow back control now has a 44 dp hit area, a button role, and a Bangla/English spoken label.
- `AppearanceTest.everyBrandHasReadableColorsInBothModes` checks text, muted text, status colors, brand accents, button labels, and control outlines against canvas/surface/raised backgrounds for all nine brands in light and dark modes. It passes with `:composeApp:testDebugUnitTest`.
- New `LocationCaptureTest` cases cover geographic bounds, invalid accuracy/timestamps/sources, stale readings, and future timestamps. They pass with the Android unit suite.
- iOS Simulator ARM64 framework compilation and Xcode's iOS Simulator app build pass. `simctl` can see the booted iPhone 18 Pro Max. Xcode only lists concrete simulator destinations when `-sdk iphonesimulator` is supplied; its XCTest run against that UUID then stayed on the onboarding screen without starting UI interaction for three minutes, so it was stopped and no XCTest result is claimed. The separate Simulator.app UI is absent from the installed Xcode bundle.
- The simulator's location was set to Dhaka and then cleared. That only configures test coordinates; without navigating to a fuel/service form, it does not verify a saved record or app permission prompt. Simulator coordinates do not measure physical GPS accuracy.
- `adb devices -l` found no attached Android device or emulator. Android/iOS native camera, file picker, PDF import, document review/export and actual screen-reader navigation were not executed end-to-end on a device in this session. Real VoiceOver/TalkBack gestures, permission denial/regrant, GPS timeout/accuracy in a workshop, physical camera/OCR, and large-text checks across the full garage/history/document surfaces still require working interactive simulators or physical devices.
