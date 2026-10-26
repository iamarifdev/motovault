# P01 v3 design review

Current Garage follow-up: [paired-card geometry and second detail sweep](P01-12-garage-layout-review.md).
The previous audit missed unequal stat-card heights and incomplete app text
scaling. Both are corrected with measured native cases; eight additional source
differences remain explicitly open in that report.

Latest follow-up: [17 v3 detail corrections](P01-11-v3-detail-corrections.md).
Fuel/Gauge and other audited icons, navigation geometry, Quick Add, sheet headers
and light/dark toast details are corrected in the working tree after `63198f9`.
Native screenshots and save/Undo checks are retained for both hosts. This closes
the listed source differences, not the full design acceptance matrix below.

Reference: [v3 design contract](../DESIGN-CONTRACT.md). New flows were extended in
`design/app-ui-v3/` before porting their native UI. Native brand-selection logos,
the four tabs, central add action, font assets and token sources are preserved.
No new font, palette or reusable-library domain dependency was introduced.

| State | Prototype | Native evidence | Limit |
| --- | --- | --- | --- |
| Normal interval and source | [Home](p01/prototype/paired-runtime-home.jpg), [detail](p01/prototype/gauge-detail.jpg) | Shared calculation tests and native detail | Pixel/motion equivalence is not asserted. |
| Unknown history | [Home](p01/prototype/unknown-home.jpg), [detail](p01/prototype/paired-unknown-detail.jpg) | Android unknown gauges/detail; iOS unknown-large XCTest | Owner estimates remain unconfirmed until a reviewed save. |
| Dark, English digits, large text, reduced motion | [Paired prototype](p01/prototype/paired-unknown-large.jpg) | [Android](p01/android/paired-unknown-large-gauges.png), iOS unknown-large attachments | Screenshot proves the rendered state, not animation timing. |
| Denied notification permission | [Denied preview](p01/prototype/reminder-denied.jpg) | Actual denial on both hosts | Browser permission preview is simulated. |
| Failed save | [Failed preview](p01/prototype/reminder-save-failed.jpg) | SQLite fault-injected UI flow on both hosts | Prototype save failure is simulated; native persisted values were compared. |
| Snooze | [Snoozed preview](p01/prototype/reminder-snoozed.jpg) | Android persisted snooze, shared restart tests and [iOS document/service snooze](p01/ios/snooze-r1/tests.json) | Stored expiry, service baseline and history remain unchanged. |
| Undo | Existing v3 toast pattern | Android and iOS save, Undo, restart | The inverse persists; the short-lived Undo button does not survive restart. |
| Empty/no expiry/invalid data | Prototype empty message and no-expiry detail; shared planners | No-data and invalid-baseline tests; native no-expiry copy | Full empty-state screenshot coverage is pending. |

The new UI reuses existing sheets, chips, switches, buttons, spacing, semantic
colors and typography. Gauge descriptions state interval remaining, and unknown
states use a neutral question mark. Reduced motion removes gauge/count-up
animation; optional haptics remain supplementary. Settings save/error feedback is
placed next to the fixed footer action and exposed as a polite live region.

The P01-05 follow-up connects the OS motion preference to both native motion
policies, alongside the persisted app flag. Reduced springs snap and sheet
entrances react to preference changes. Both native suites pass; see
[phase acceptance](P01-05-verification.md). Android live-change recordings are
separate from timing/parity review. iOS OS-toggle runtime now passes with its
original setting restored. [Three-case native matrix](P01-05-matrix.md) covers
Home, Vault, Me and reminder settings on both hosts. It also records the reduced
tab-fade correction and document-preview metadata clipping fix.

Native controls now use standard clickable/toggleable/selectable semantics while
retaining the v3 press scale and motion. Scrolled button bounds are refreshed for
the iOS accessibility bridge. Native hierarchy tests prove names, roles and
touchability in exercised flows; they do not prove spoken output or complete
focus order.

## Pending design acceptance

[Physical Motorola evidence](P01-10-physical-android.md) now covers real background
delivery, exact individual notification targets, retained service alert, durable
snooze/restart and permission recovery. Light/Bangla and dark/English/1.3-text
screens plus physical recordings are retained. They do not close full motion
parity, spoken TalkBack, physical haptics or the missing iPhone gate.

Current-source automated accessibility and native navigation checks are recorded
in [P01-07](P01-07-accessibility-navigation.md). Global/document/service XCTest
audits pass in normal and large/dark fixtures, without filtering issues. Android
native Back discards a changed draft with identical before/after persisted data.
These checks do not establish spoken output or complete screen-reader traversal.

[Numeric entry checks](P01-08-keypad-layout.md) now cover normal and large/dark
portrait fixtures on both hosts: every keypad key and Save remains reachable,
the platform keyboard stays hidden, and Close preserves the confirmed value.
This does not cover unrelated existing text-entry flows or landscape.

[Android sheet-window correction](P01-09-sheet-window-motion.md) removes the
extra platform dialog scale/fade that the denser reduced-motion captures exposed.
The custom sheet owns normal motion; OS-reduced recordings show the number without
count-up. Full press/gauge parity and physical-device motion review remain open.

- Record and compare normal sheet/press/gauge motion with v3, and reduced-motion
  transitions with both app and OS preferences.
- Exercise TalkBack/VoiceOver focus order, announcements, modal isolation and
  navigation, including large text and native back/gesture behavior.
- Finish the representative light/dark/system, digit, keyboard and safe-area
  matrix. Existing nine-brand token/contrast evidence is unchanged; recheck shared
  control states for all nine accents before closing design review.
- Verify actual device haptics and physical-device rendering. These checks remain
  pending at the owner's request.

P01 remains in review while these design and physical-device checks are
incomplete. Simulator permission recovery now passes; it does not close physical
verification. No screenshot or build result is used to infer completion.
