# Garage layout recheck, 2026-10-26

## Finding and correction

The owner correctly identified unequal Odometer and Fuel economy card heights.
The prior 17-item audit missed this relationship. Its screenshots showed the
problem, but its checks did not assert equal rendered bounds. The previous report
must not be read as a complete Garage or full-app parity sign-off.

In the earlier normal Android hierarchy, Odometer occupied `[53,1764][527,2133]`
(369px high), while economy occupied `[553,1764][1027,2070]` (306px high).
The earlier large-labelled run had heights422/362px. These are retained in
`p01/android/v3-details-{runtime,unknown-large}-final-r1/home.xml`.

V3 uses a two-column CSS grid with stretched children; native used two independent
content-height Columns. Native now measures the pair together and fills both
cards to the tallest content. There is no arbitrary fixed card height.

The live browser reference was measured in its review phone frame:

| Reference case | Card width | Odometer height | Economy height | Value font |
|---|---:|---:|---:|---:|
| p01-runtime | 188px | 156px | 156px | 30px |
| p01-unknown-large | 188px | 184px | 184px | 39px |

These are browser design measurements, not a demand that a different native
viewport produce the same physical-pixel height. Equal height, wrapping, baseline,
spacing and scaling relationships are the acceptance criteria.

## Additional corrections from the Garage sweep

| Detail | Correction |
|---|---|
| Stat values | 26sp -> reference30sp, line37.5; value/unit baseline alignment and6-unit gap, with wrapping |
| Stat captions | 12sp ->13sp, line19.5 |
| Hint rows | Minimum26 units, line18.6; remove forced single-line clipping; retain both odometer hints |
| Confirmed date | Use the selected digit style and short date formatter instead of raw ISO text |
| App text-size setting | Apply the multiplier through local font density to explicit sp values as well as theme roles; stop pre-scaling theme roles so it is applied once; retain the OS font-scale multiplier |
| Page spacing | Garage top8/bottom28 and horizontal20 as `.screen-pad` |
| Bike heading/avatar | Heading36/semibold/line37.8; avatar photo118% rather than a46-unit image |
| Next-task text | Title22/semibold/line29.7, description16/line24.8; remove extra internal text gap |
| Quick actions/nudge | Quick-action bottom padding10; nudge end padding6 and paragraph line19.6 |
| Section spacing | Group section heading and content so parent gap20 is not added to the heading's bottom10; gauge strip padding top2/bottom8 |
| Gauge artwork | Preserve84 viewport with reference80-unit SVG geometry: radius32 and stroke6; previous native radius filled almost the entire viewport |
| Gauge type/caption | Number24, percent12, name line18.9, status line15.6; restore missing interval caption and its computed13.333/line20.667 style; bottom padding12 |
| Status icons | Exact CircleCheck/Timer/TriangleAlert/HelpCircle artwork;14/stroke2.2 inside gauges; correct unknown hero TriangleAlert |
| Renewals | Allow the text column to shrink/wrap before the chevron; title16; correct Timer/TriangleAlert |
| Bike photo | Height176, max80%/340 width, Gauge fallback |
| History summary | The economy card opens History; its three summary columns now have equal weight and gap8, matching the grid and preventing enlarged labels from competing for unbounded width |

The font-scale finding matters beyond this pair of cards. Earlier fixtures marked
`textScale=1.3` did not prove every explicitly sized label was enlarged. The new
Android check measures a label at64px normally and84px at1.3, in the same host.
Existing historical evidence is retained, not retroactively relabelled.

## Rendered checks

The dedicated Android runner loads only isolated synthetic emulator fixtures,
brings both complete cards into view, and asserts identical top/bottom edges,
equal widths and contained text bounds. It includes normal Bangla, large English
with unknown economy, and six-digit long odometer values in large Bangla/English.
It also opens History and checks that summary heading bounds do not overlap.

Final Android measurements are in
[garage-layout-r4/result.json](p01/android/garage-layout-r4/result.json).
The previous r2 geometry run measured equal heights434/434px,481/481px,
643/643px and481/481px respectively. Differences between cases are allowed:
wrapped accessible text determines the row height.

iOS XCTest measures both full card frames and checks equality within0.5 points,
then verifies that Odometer opens its sheet and economy opens History. Normal
and actual1.3/dark cases use the existing iOS27 iPhone18 Pro Max simulator.
Final measured frames have equal heights159 points normally and212 points at1.3.
Reports and final source/artifact hashes are linked in
[Garage provenance](p01/garage-review/provenance.json).

Reviewed final screenshots:

- [Android normal stats](p01/android/garage-layout-r4/normal-bn/stats.png)
- [Android large English/unknown](p01/android/garage-layout-r4/large-en-unknown/stats.png)
- [Android long Bangla value](p01/android/garage-layout-r4/large-bn-long/stats.png)
- [iOS normal stats](p01/ios/acceptance-garage-normal-r3/attachments/0764F24F-0E81-49D5-A4A8-29C55CF28DCC.png)
- [iOS large stats](p01/ios/acceptance-garage-large-r3/attachments/9AB7C1BB-66E6-49A8-B25B-DBE555458A68.png)

Large-text keypad regression checks and representative Home/Vault/Me/reminder
captures are retained on both hosts after the font-scale correction. These are
targeted checks, not all-state accessibility or full-app design acceptance.
They precede the last History-column/caption-only refinements; the final Garage
and History captures use the complete source. Android148 shared/unit tests also
passed during this change; iOS compilation and the stated XCTest cases passed.

Diagnostic runs are preserved. Android r1 used lowercase display-style names
where the native fixture expects enum values `BN`/`EN`; the loader correctly
preserved the unreadable fixture and reported a recovery state. The harness now
writes the proper enum casing. iOS geometry r1 passed the card-frame assertions
but looked for `সেভ করুন` before editing; the actual initial action is
`ঠিক আছে, সেভ করুন`. The test now checks the actual sheet boundary and both
navigation destinations.

## More source differences found, still open

This second sweep also confirms that the app still has details outside the
corrected set. They are not silently treated as matched:

| Surface | Reference | Remaining native difference |
|---|---|---|
| Vault empty state | `.empty`: dashed border, padding28 vertical/20 horizontal | Solid border and28 on every side |
| Vault renewal hero | `.next`: gap12, title22/semibold | Gap6 and title20/bold |
| Vault/History/Me page spacing | `.screen-pad`: top8/bottom28, section gap20 | Existing top/bottom12 and gaps18/16/18 |
| Vault long wallet titles | Constrained title beside expiry state | Actual1.3 capture shows a long synthetic title competing with the expiry chip; the title row still needs a wrap/width constraint review |
| Me accents | `.swatches`: wrapping flex rows | Horizontal scrolling Row, so all nine are not shown together |
| Me profile photo | Image width110% of72-unit photo well |48-unit image |
| Garage gauge strip edges | Negative horizontal margin20 with inner padding20 | Strip still remains inside the page's horizontal inset |
| Bike cut-out shadow | CSS photo drop-shadow | Native photo has no equivalent image shadow |

Sources: `design/app-ui-v3/src/styles/app.css`, `screens/Vault.tsx`,
`screens/Me.tsx`, and active native `Screens.kt`. Font-family presence does not
close real variable-weight shaping parity. The remaining sheets, document states,
press/gauge motion and screen-reader behavior still require their own bounded
review. This report does not certify them.

Evidence was captured from working changes after base `63198f9`; subsequent
commits are recorded in Git history. No production owner app,
physical phone records or reusable Sky source was changed. P01-05 remains in
review; physical iPhone, spoken accessibility and haptics stay pending.
