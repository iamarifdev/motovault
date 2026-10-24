# P01 v3 design review

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
| Snooze | [Snoozed preview](p01/prototype/reminder-snoozed.jpg) | Android persisted snooze and shared restart tests | iOS interactive snooze still needs runtime coverage. |
| Undo | Existing v3 toast pattern | Android and iOS save, Undo, restart | The inverse persists; the short-lived Undo button does not survive restart. |
| Empty/no expiry/invalid data | Prototype empty message and no-expiry detail; shared planners | No-data and invalid-baseline tests; native no-expiry copy | Full empty-state screenshot coverage is pending. |

The new UI reuses existing sheets, chips, switches, buttons, spacing, semantic
colors and typography. Gauge descriptions state interval remaining, and unknown
states use a neutral question mark. Reduced motion removes gauge/count-up
animation; optional haptics remain supplementary. Settings save/error feedback is
placed next to the fixed footer action and exposed as a polite live region.

Native controls now use standard clickable/toggleable/selectable semantics while
retaining the v3 press scale and motion. Scrolled button bounds are refreshed for
the iOS accessibility bridge. Native hierarchy tests prove names, roles and
touchability in exercised flows; they do not prove spoken output or complete
focus order.

## Pending design acceptance

- Record and compare normal sheet/press/gauge motion with v3, and reduced-motion
  transitions with both app and OS preferences.
- Exercise TalkBack/VoiceOver focus order, announcements, modal isolation and
  navigation, including large text and native back/gesture behavior.
- Finish the representative light/dark/system, digit, keyboard and safe-area
  matrix. Existing nine-brand token/contrast evidence is unchanged; recheck shared
  control states for all nine accents before closing design review.
- Verify actual device haptics and physical-device rendering. These checks remain
  pending at the owner's request.

P01 remains in review while these checks and the native permission-recovery
gates are incomplete. No screenshot or build result is used to infer completion.
