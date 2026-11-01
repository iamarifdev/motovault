# UI language and document-dialog audit

Scope: user screenshots from 21 October 2026, followed by a source audit of the shared screens and Android/iOS document controls. The app currently has a Bengali interface, without a language selector. Brand names, document contents, technical formats (PDF, JSON, SHA-256, YYYY-MM-DD) and stored identifiers remain unchanged.

## Findings and fixes, in order

| Step | Finding | Fix |
| --- | --- | --- |
| 1 | Document screens used the iOS dialog's automatic platform insets plus screen safe-area padding. This left a grey strip outside the white screen. | Disable automatic dialog insets and keep the screen's single safe-area padding. The background now covers the window. Applies to choose, capture, review, manual entry, original detail and inspection. |
| 2 | Document dialogs had an old footer with three text tabs. Two were disabled; it differed from the actual app navigation. | Remove the duplicate footer. These are focused document flows with an explicit back action. |
| 3 | Vault reminder panel was English inside the Bengali page. | Translate heading, explanation, enable/disable action, settings link and status on both platforms. |
| 4 | Document choose, scan, crop, confirmation, detail and inspection copy was English. | Translate visible headings, instructions, actions and field labels. Add separate Bengali display names for document types and page sides; retain original stored keys and OCR matching. |
| 5 | Photo/PDF import actions and Android camera permission guidance were English. | Translate the app-owned controls and guidance. Native system pickers follow device language. |
| 6 | Onboarding, appearance and fuel/service forms repeated English after Bengali labels. | Remove duplicate English labels, retain optional-field meaning and technical units, and translate schedule actions and status. |
| 7 | Odometer, replace-bike and delete confirmations were English. | Translate titles, descriptions, actions and the minimum-distance hint. |
| 8 | Passport review/verification and native file actions were English or mixed. | Translate app controls and explanatory text. Exported passport contents and verification format are unchanged. |
| 9 | Screenshot review found a missed manual-entry type label, tight checkbox text and negative letter spacing in Bengali document headings. | Apply display-name mapping inside the document controls, add 8 dp checkbox spacing and use normal letter spacing with a 38 sp heading line height. |
| 10 | Existing bike data contained the older bilingual default edition label. | Render both known default labels as Bengali without changing the stored record or custom editions. |

## Verification

- Final Android/shared unit tests passed: 114 tests, zero failures. Debug APK assembly and whitespace checks passed.
- Two focused native UI tests passed in `/private/tmp/motovault-bengali-final.xcresult`: Bengali document/manual-entry screenshots plus popup and navigation geometry. The subsequent full walkthrough run stalled in the XCTest runner and was cancelled; it is not counted as a passing full-app verification.
- Final document typography/dropdown changes were rechecked: `/private/tmp/motovault-bengali-complete.xcresult`, two tests passed, zero failures. Screenshots were visually reviewed. The final existing-edition label correction is a separate display-only build.
- Final iOS build passed in `/private/tmp/motovault-bengali-display-build.log` and was installed and launched on simulator `50709DF0-FEA9-4B34-9DE6-6172D868875D` without clearing data. Document selection and manual-form screenshots have white bottom corners, confirming the external grey strip is removed.
- First iPhone simulator capture confirms the document screen covers the bottom edge, the obsolete footer is absent and the Vault reminder panel is Bengali.
- Document dialogs on Compose 1.7 are not fully exposed in the XCTest accessibility tree on this runtime. Document manual-entry navigation therefore uses a coordinate reviewed against its screenshot; captures are visually inspected separately from test assertions.
- This audit does not certify physical camera/OCR, native imports, export sharing, notification delivery, keyboard save/edit flows or a full accessibility audit. Existing user records are not cleared or saved by the layout walkthrough.

## Reviewed screenshots

- [Document selection, bottom grey strip removed](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/18-bengali-document.png)
- [Bengali renewal panel](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/19-bengali-reminders.png)
- [Manual details with Bengali document type](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/20-bengali-manual.png)
- [Scrolled manual-entry footer](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/21-bengali-manual-footer.png)
- [Compact Bengali odometer popup](/Users/arif/.codex/visualizations/2026/10/21/01a15f10-3d86-7a02-854e-56369ab869e9/sky-ui-verification/22-bengali-odometer.png)
