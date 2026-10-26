# P01-04 interval and baseline honesty verification

Base: `86c1598` plus the uncommitted P01 working change, 24 October 2026.

Implementation: `WearDomain.kt`, `GaugeDetailSheet.kt`, `QuickService.kt`,
`ComponentRule.kt`, `VaultState.kt`, native screens/sheets and the corresponding
v3 prototype extension. The percent describes recorded service interval
remaining. It does not measure the physical condition of a component.

| Requirement | Verification |
| --- | --- |
| Whichever limit first | `IntervalHonestyTest` verifies distance-first and calendar-first overdue cases, including the amount overdue. |
| Unknown or estimated history | Unknown, owner-estimated, invalid and future baselines produce UNKNOWN, with a neutral question mark and no reassuring percentage. |
| Old records | Only a matching linked service record can establish legacy certainty; matching labels alone cannot. Owner-written labels remain intact. |
| Baseline reset | A reviewed service resolves an unambiguous stable schedule ID and establishes a confirmed baseline. Older mileage/date cannot replace a newer confirmed service. |
| Stale mileage | The confirmed mileage/date are shown separately from an estimate. Missing confirmation dates do not create fictional freshness. Tests verify estimates do not mutate the confirmed value. |
| Detail basis and source | [Android native detail](p01/android/unknown-detail-fresh.xml), [iOS native XCTest](p01/ios/unknown-large-r1-summary.json), and prototype detail captures show source, calculation basis and last confirmed mileage/date. |
| Large text and reduced motion | Paired unknown-large fixture uses dark appearance, English digits, 1.3 text scale and app reduced motion. [Android gauges](p01/android/paired-unknown-large-gauges.png) and the iOS unknown-large attachments show neutral gauges and usable scrolling. Source inspection confirms unknown/reduced-motion sweeps snap directly to their target. Stills alone are not motion-parity proof. |

Final shared suites: [148 Android tests and 170 iOS simulator tests](p01/tests/summary.json),
zero failures. Retained XML includes interval, service-baseline, reminder,
migration/recovery and session tests. Prototype TypeScript and production build
also pass; this is browser build evidence, not native visual parity.

The native Save paths close only after a durable success. Service entry keeps the
selected schedule ID, updates only matched service/replacement baselines, and
clears an obsolete snooze. Onboarding no longer invents a 40%-used baseline.
No-expiry documents say that they have no expiry; malformed dates ask for review.

Remaining design checks, including normal-motion recording comparison and spoken
screen-reader review, are listed in the [design matrix](P01-design.md). Physical
haptics and device rendering have not been verified.
