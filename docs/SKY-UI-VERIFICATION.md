# Sky adoption UI verification, 21 October 2026

Device: iPhone 18 Pro Max, iOS 27.0, 440 × 956 logical points. UI testing used a separate simulator (`3A228794-CFBD-45B1-A1B3-DA1A6E7DBBD9`) to keep synthetic records out of the owner's simulator.

## Corrections

1. `AnimatedContent` places children in a box. The initial adoption passed column-oriented Garage, Vault and History content directly into it, causing sections to overlap. Each destination now has its own full-size scrolling column and section spacing. The bottom navigation stays outside the animated content.
2. Long sheets scrolled their close button away. The header now stays fixed while the body scrolls, including when the keyboard is open.
3. The three gauge labels had different heights. They now have explicit line height, centred text and a common minimum label height.
4. Sky primary buttons were shorter than adjacent app actions. The shared primary helper now uses the same 52-point minimum.
5. Shared fields now expose their product label directly and use the action colour for a visible focus outline while retaining Sky geometry and typography.

## Follow-up: sheet and navigation spacing

The spacing follow-up replaces the fixed 650-point sheet limit and the nested overlay host with a local dialog adapter. A short popup sizes to its content. A long form uses the current viewport minus the top safe area and a 60-point toolbar gap. The sheet surface includes its bottom safe area, its handle has a single 10-point top/4-point bottom gap, and the fixed header is separated from the body by one divider. Body padding is 20 points horizontally and 16 points vertically, with 12-point gaps between controls.

Navigation destinations now use equal-width slots with an 80-point content height at standard text size. Icons remain 30 points; labels use explicit 12/16-point font/line height and a 24-point slot for Bangla glyph metrics. This slot grows with system font scale. The selected marker remains 28 × 4 points. The shell supplies the system safe area once, on the navigation surface colour. The first simulator measurement caught the Bangla font exceeding a proposed 16-point label slot; the final slot preserves its glyphs without clipping.

`testSheetAndNavigationGeometry` checks equal navigation widths and heights, a compact odometer popup, a taller fuel form, and a close action that remains reachable after scrolling. Earlier screenshots below describe the preceding repair; the follow-up captures 50–53 supersede their sheet/navigation geometry.

The iOS sheet dialog disables its automatic platform insets; Android uses an edge-to-edge dialog. This is specific to sheets and does not change document dialogs. In the final iPhone popup screenshot, both bottom corners measure RGB `(255, 255, 255)`, confirming that the previous external grey band has been removed. See [navigation spacing](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/14-navigation-spacing.png) and [compact popup](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/15-compact-popup.png).

## Reviewed screenshots

Screenshots are actual simulator captures, not mockups. The full runs also retain accessibility trees and intermediate screenshots in their `.xcresult` bundles.

| UI or step | Check | Screenshot |
| --- | --- | --- |
| Brand onboarding | Brand grid, selected state, continue action | [Onboarding](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/01-onboarding.png) |
| Model picker | Model photos, selection and confirmation | [Model picker](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/02-model-picker.png) |
| Bike details | Top and bottom, confirmation and garage creation | Pass 1 captures 03–04 |
| Garage | Photo above metrics, ordered sections, scrolling and fixed navigation | [Garage](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/03-garage.png) |
| Dark appearance | Theme switching, legible text and icons | [Dark Garage](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/04-garage-dark.png) |
| Odometer | Sheet opens and closes, input and save action visible | Pass 3 capture 13 |
| Vault | Header and content ordering, empty state, actions | [Vault](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/05-vault.png) |
| Inspection | Empty state, exit and return to Vault | Pass 3 capture 15 |
| Document entry | Document choice, manual entry, confirmation and return | [Manual confirmation](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/11-document-manual.png) |
| History | Heading, totals, filters, empty state and actions | [History](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/06-history.png) |
| Fuel form | Top/bottom, disabled empty save, close remains reachable | [Scrolled fuel form](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/07-fuel-scrolled.png) |
| Dark fuel form | Contrast and close action | [Dark fuel form](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/10-fuel-dark.png) |
| Service form | Top/middle/bottom, controls, disabled empty save, dismissal | [Scrolled service form](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/08-service-scrolled.png) |
| Components and schedule | List, add action, input layout and dismissal | [Schedule](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/09-schedule.png) |
| Service passport | Review top/bottom, confirmation state and verification form | [Scrolled passport review](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/12-passport-scrolled.png) |

## Automated evidence

- Final sheet/navigation geometry test passed (1 test, 0 failures): `/private/tmp/motovault-spacing-verified.xcresult`. It ran on the original iPhone 18 Pro Max simulator, opened and cancelled forms without saving records, and checked 80 pt equal-width navigation, compact popup bounds and the fixed header during scrolling. Final captures: [long sheet](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/16-long-sheet.png), [scrolled footer](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/17-scrolled-sheet.png).
- Fresh onboarding walkthrough passed: `/private/tmp/motovault-sky-ui-pass1.xcresult`.
- Tab, appearance and scrolling-form walkthrough passed after fixes: `/private/tmp/motovault-sky-ui-pass3.xcresult`. It asserts photo/metric separation, Vault heading order and a reachable close button after scrolling.
- Document, manual-entry, passport and dark-form walkthrough passed in `/private/tmp/motovault-sky-ui-records3.xcresult`. The separate record-input test in that bundle required another test-driver correction.
- Android/shared tests and debug APK assembly passed after the UI corrections.

The first failing walkthroughs are retained as diagnostic evidence. Tests are in `iosApp/MotoVaultUITests/OwnerFlowTests.swift`. The fresh onboarding test requires an empty app container; the remaining walkthroughs require that garage setup has completed. Run on a disposable verification simulator.

The corrected app was reinstalled and launched on the original simulator (`50709DF0-FEA9-4B34-9DE6-6172D868875D`) without clearing its data. [Original simulator Garage capture](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/13-original-simulator.png) shows its existing 6 km odometer, ordered sections and fixed navigation.

## Limits

These checks establish the listed iPhone simulator layouts and interactions, not exhaustive product or release verification. Camera capture, OCR, original-document import/crop, populated document inspection, native sharing/export, reminders, GPS permissions, Android runtime layouts, large accessibility text and physical-device behaviour were not reverified in this UI repair.

Compose 1.7 does not expose the full-screen document dialog controls or editable-control focus consistently to XCTest on this iOS runtime. The document walkthrough uses screenshot-derived coordinates for its visible back/manual-entry controls. This workaround is test evidence, not a completed VoiceOver accessibility audit.

The extended numeric save/edit probe did not pass: direct typing could not obtain XCTest keyboard focus, and keyboard-key taps did not complete entry reliably. Later test-service stalls prevented a conclusive rerun. An optional keyboard-dismiss experiment and the unsuccessful exploratory test were removed from the final code. Diagnostic runs remain under `/private/tmp/motovault-sky-ui-records*.xcresult` and their exported screenshot folders. Saving and editing records with the native keyboard remain explicitly unverified in this repair; they are not included in the passing visual walkthrough claims.
