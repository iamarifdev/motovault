# P01 automated accessibility and navigation

26 October 2026. Base `48fce40321600e4fbdfe124a3bb63b3032276502` plus the
uncommitted P01 follow-up. All runs use synthetic records and the isolated
`com.motovault.baseline.p00` app. No owner app records were reset.

## iOS 27, iPhone 18 Pro Max

The current isolated host was rebuilt using native ARM64 tooling and locally
ad-hoc signed with the isolated bundle identifier. The destination was
`50709DF0-FEA9-4B34-9DE6-6172D868875D`. Runtime metadata and binary fingerprints
are retained in each run's `run.json`.

| Fixture | Result | Retained evidence |
| --- | --- | --- |
| Light, Bangla digits, normal text/motion | 4 tests passed | [Summary](p01/ios/acceptance-native-r1/summary.json), [run](p01/ios/acceptance-native-r1/run.json) |
| Dark, English display digits, 1.3 text, app reduced motion | 4 tests passed | [Summary](p01/ios/acceptance-large-native-r1/summary.json), [run](p01/ios/acceptance-large-native-r1/run.json) |
| Changed draft, drag dismissal, reopen: normal | 1 test passed | [Summary](p01/ios/acceptance-navigation-native-r2/summary.json) |
| Changed draft, drag dismissal, reopen: large/dark | 1 test passed | [Summary](p01/ios/acceptance-navigation-large-native-r2/summary.json) |
| No document or service reminders | 1 audit passed | [Summary](p01/ios/acceptance-empty-native-r1/summary.json) |

Each run contains all-category XCTest accessibility audits for the global
reminder sheet, document reminder and service reminder, plus drag dismissal.
The global-sheet test verifies background Me navigation is absent from the
modal accessibility hierarchy. No audit issue is filtered or ignored.

The earlier [r2 failure](p01/ios/accessibility-audit-r2/summary.json) remains
diagnostic evidence. Its small-hit-area and missing-description failures did not
recur on the current host. The accepted artifacts include screenshots and native
trees for each audited screen. Visual inspection of the normal and large/dark
global sheet found legible text, reachable selections and a separate fixed Save
footer; document snooze actions and its Back footer remain visible after scroll.

The initial drag tests establish dismissal only. A stronger follow-up changes
the lead-time draft, dismisses by dragging and reopens the sheet to compare the
committed summary. Both follow-up fixtures pass. The empty reminder audit passes
separately, using the retained P00 `unknown-service` fixture with no documents or
service schedules. Results are recorded separately rather than inferred from
the initial tests. In total these runs contain 11 passing XCTest cases.

## Android ARM64 emulator

The SDK's `emulator/emulator` and `qemu/darwin-aarch64/qemu-system-aarch64` are
native ARM64. The existing Pixel 10 AVD uses `arm64-v8a`, API 37.1, Google APIs
Play Store with 16 KB pages. The shell's legacy `sdk/tools/emulator` fails with
`Bad CPU type in executable`; it was not used for these runs. No shell settings
or SDK installations were changed.

The current isolated APK was installed. New [normal](p01/android/matrix-runtime-native-r4/capture.json)
and [large/dark](p01/android/matrix-unknown-large-native-r4/capture.json) captures
retain Home, Vault, Me and reminder settings on that native emulator.

Native Back tests changed the lead-time draft to 14 days, checked the displayed
draft, pressed the OS Back key and verified return to Me. The complete sanitized
database snapshots before and after were identical, including preferences,
document expiry, service baselines, snoozes and log count. Background tab labels
were absent while the reminder modal was open.

- [Normal result](p01/android/navigation-runtime-native-r1/result.json),
  [before](p01/android/navigation-runtime-native-r1/before.json),
  [after](p01/android/navigation-runtime-native-r1/after.json).
- [Large/dark result](p01/android/navigation-unknown-large-native-r1/result.json),
  [before](p01/android/navigation-unknown-large-native-r1/before.json),
  [after](p01/android/navigation-unknown-large-native-r1/after.json).

## Remaining acceptance

Reproduce with `tools/baseline/p01_ios_acceptance.py --device <verified-Pro-Max-UUID>
--run <new-name>`; choose `--case unknown-large` or `--case empty` for the other
fixtures, and `--navigation-only` for the strengthened drag check. Rebuild and
sign the isolated host with the current `P01DeliveryTests.swift` first. Android
uses `tools/baseline/p01_android_navigation.py --run <new-name>`. These runners
preserve prior run directories and refuse to operate on production identities.

Automated audits and hierarchy checks do not establish spoken VoiceOver/TalkBack
output, complete focus traversal, focus restoration or live-region announcements.
Fine motion comparison, keyboard/safe-area extremes and physical haptics remain
open. Physical Android/iPhone notification delivery and rendering remain pending
at the owner's explicit request. P01-05 is not complete.
