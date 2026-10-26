# P01-05 phase acceptance

Base: `48fce40321600e4fbdfe124a3bb63b3032276502` plus uncommitted retention and
system-motion changes, 25 October 2026. Only the isolated synthetic
`com.motovault.baseline.p00` app is used. Physical Android/iPhone checks remain
pending by explicit owner instruction. This task is not complete.

## System motion policy

The v3 prototype already uses `MotionConfig` with `always` for the app preference
and `user` otherwise. Native `VaultTheme` previously passed only the persisted app
flag into both motion policies. It now combines that flag with the live OS
preference. Android observes `ANIMATOR_DURATION_SCALE`; iOS observes Reduce Motion
changes and refreshes on activation. Observers are removed when composition ends.
No system preference is persisted as an owner setting.

Reduced springs now snap rather than using a fast spring. An active sheet entrance
also responds to changes in the effective policy. Existing gauge/count-up effects
already respond to that policy. Tab fades now use it as well. Normal reference timings, tokens and branding
remain unchanged. No new visual flow is added to the prototype.

Both native suites and the isolated APK build pass: 148 Android/shared tests,
170 iOS simulator tests. The latter explicitly targets iOS 27 on the preferred
iPhone 18 Pro Max `50709DF0-FEA9-4B34-9DE6-6172D868875D`. The isolated Xcode host
also rebuilds and passes local ad-hoc signing validation. Compilation does not
establish native motion timing or spoken accessibility behavior.

All three Android recordings decode successfully: [video inspection](p01/android/motion-os/video-inspection.jsonl).
Their nominal rates are approximately 5.6, 5.7 and 8.9 frames per second; these
captures are insufficient for accepting fine press/spring timing. The tap timing
field measures developer-command return, not animation duration or user latency.

## Remaining acceptance

| Requirement | Current evidence / remaining work |
| --- | --- |
| Actual reminder policy, exact targets, denial, recovery, snooze, failed save | Accepted native-host results in [P01-03](P01-03-verification.md). The earlier build fingerprint is retained in [retention provenance](p01/provenance-retention.json); these runs predate the system-motion follow-up. |
| Nine accent contrasts | Existing `AppearanceTest.everyBrandHasReadableColorsInBothModes` passes for all nine palettes. This is calculated contrast evidence, not screenshots of every control state. |
| App and OS reduced motion | Android [observations](p01/android/motion-os/observations.json) and recordings confirm live OS scale 1/0/1 with the original value restored. iOS [motion-r3](p01/ios/motion-os-r3/summary.json) passes the real OS switch, normal/reduced sheet interaction and return without app restart. Original and restored Reduce Motion are both 0. [Recording](p01/ios/motion-os-r3/live-os-motion.mov) decodes at approximately 21.8 nominal fps. Fine timing/parity remains open. |
| Home/Vault/Me representative appearance/digit/text matrix | [Three cases on both hosts](P01-05-matrix.md) now include light/Bangla/normal, dark/English/large/app-reduced and System under OS dark. OS appearance is restored. Visual review corrected reduced tab fades and clipped preview metadata; r3 captures follow both fixes. This is representative coverage, not full design acceptance. |
| Sheet/press/gauge motion and native back/gestures | Compare normal and reduced recordings against v3; stills alone are insufficient. |
| TalkBack/VoiceOver | Named roles and touch flows are retained. Focus order, modal isolation and spoken announcements remain open. |
| Real Android and iPhone | [Physical Android core](P01-10-physical-android.md) passes ordinary background delivery, individual targets/retention, snooze, app restart and permission recovery. Post-boot delivery remains negative under the observed standby policy; full motion/spoken/haptic acceptance and the unavailable physical iPhone remain open. |

See [design review](P01-design.md) for the complete design acceptance list.
