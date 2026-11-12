# P08-01 design-matrix layout resolutions

Date: 12 November 2026 (Asia/Dhaka). Scope: the owner authorized fixing the
clear 1.3-scale/sheet-clipping findings from the P08-01 design matrix before the
physical-device pass and pilot. This report records what changed, what was
verified on the simulator/emulator, and what was deliberately left for the
release decision.

## Findings addressed

### D1/D4 — Records filter and action rows clipped at 1.0/1.3

The native Records (History) tab used three horizontal-scroll rows for period,
category and action chips. At 1.3 text scale a chip and the third action label
were cut mid-label at the viewport edge, and there was no scroll cue.

**Fix:** the three rows now use `FlowRow` (wrapping), matching the v3 prototype's
`.row-wrap` rule (`design/app-ui-v3/src/styles/app.css`: `display:flex;
flex-wrap:wrap; gap:8px`) as used by `components/CostInsights.tsx`. `CostOverview`
no longer uses `horizontalScroll` for these rows.

**Verified:** Android emulator screenshots at 1.0 and 1.3 show every period and
category chip fully wrapped and readable; the action row wraps instead of
truncating. iOS simulator captures at 1.3 (records, BN and EN) show the same.

### D5 — Profile theme segment truncated at 1.3

`VaultSegmented` used a fixed 40 dp option height and a single-line-sized text
box, so "সিস্টেম ডিফল্ট" was cut to "সিস্টেম" at 1.3.

**Fix:** each segment now has `heightIn(min = 40.dp)`, internal 6×8 dp padding,
and an explicit 18 sp line height so a label can wrap to two lines and the
control grows with the text scale. This applies to every segmented control that
reuses the component (theme, language, digits, text size, reminder intervals,
backup tabs).

**Verified:** Android 1.3 Profile shows "সিস্টেম ডিফল্ট" on two lines inside the
selected segment; iOS 1.3 Profile shows the same. The other segmented rows
(language/digits/text size) remain single-line and complete.

## Findings deliberately left for the release decision

- **Demo claims and demo data** ("New concepts (demo)", "Reset demo data", Bangla
  `seedData` labels in the English demo state): product decision, not a layout
  fix. Carried in [P08-remaining-gates.md](P08-remaining-gates.md#p08-03-representative-owner-pilot-humans).
- **Expense date field renders Latin digits under the Bengali number style**:
  whether the native date control is exempt is a presentation decision (recorded
  in P08-01 and the review). Unchanged.
- **Backup sheet content/action below the fold and privacy-sheet card at the fold**
  (P08-01 D3/D7): both sheets have a scrolling body with the privacy sheet using
  the shared fixed footer; the content below the fold is reachable by scrolling
  and no control is lost. Moving the backup primary action into a pinned footer
  would be a structural sheet refactor, not a clipping fix; left for the
  release-candidate design review. Recorded honestly rather than marked fixed.

## Verification

- `./gradlew :composeApp:testDebugUnitTest :composeApp:compileKotlinIosSimulatorArm64 --offline`:
  BUILD SUCCESSFUL; **327 tests, 0 failures/errors/skipped**.
- Portable gates: literal (9 justified data-only entries), Bangla copy lint,
  responsibility (only the 2 baseline test-support files), dependency direction,
  package cycles and fuel numeric policy all pass; tooling unittest suite passes.
- Android: rebuilt isolated probe APK `e60d3bf49011cea4b48aef711b849e35eb47710b954d04f4c207860968bd5564`,
  installed on `emulator-5554`; captures at 1.0 and 1.3 in
  [p08/layout/android/](p08/layout/android/).
- iOS: `P08DesignMatrixTests/testTextScaleMatrix` passed on the pinned iOS 27
  simulator after rebuild; captures in [p08/layout/ios/](p08/layout/ios/).
- No persistence, copy-key, dependency or navigation change was made; the fixes
  are layout-only.

## Limits

- Screenshot review at 1.0/1.3 on the two simulator/emulator targets; physical
  device layout/accessibility remains P08-02.
- The wrapping-row change affects all cost rows; no automated UI test asserts
  wrapping, so the retained screenshots are the evidence.
