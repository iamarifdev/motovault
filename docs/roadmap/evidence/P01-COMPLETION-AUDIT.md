# P01 completion audit

26 October 2026. Base `48fce40321600e4fbdfe124a3bb63b3032276502` plus the
uncommitted P01 follow-up. Phase completion is not established.

Subsequent physical-device update: the owner connected a Motorola Android and
authorized physical verification, including one reboot. [P01-10](P01-10-physical-android.md)
records the ongoing partial Android results and the observed post-boot standby
deferral. Physical iPhone evidence is still unavailable. The original inventory
and blocker below describe the earlier audit, before the Android was connected.

## Requirements and evidence

| Task | Current tracker state | Evidence and completion boundary |
| --- | --- | --- |
| P01-01: unified warning/time/override policy and safe preference migration | Done | [Policy](../P01-REMINDER-POLICY.md), [migration and persistence verification](P01-01-verification.md), retained shared/native tests. |
| P01-02: scheduling from committed state, stable targets, cancellation and consumed-event recovery | Done | [Scheduling verification](P01-02-verification.md), planner/journal/session regression tests and retained native-host sequences. |
| P01-03: actual permission state, settings recovery, exact document/service targets and snooze | Done | [Native-host verification](P01-03-verification.md), accepted policy-delivery and settings-recovery runs. Historical failed runs remain diagnostic. Physical-device phase exit is separate. |
| P01-04: interval labels, unknown history, calculation basis/source and confirmed mileage | Done | [Interval and baseline verification](P01-04-verification.md), native detail captures and shared baseline/calculation tests. |
| P01-05: real Android/iPhone reminder delivery, foreground/background, denial/recovery, restart and exact targets | In review, incomplete | [Physical Android core evidence](P01-10-physical-android.md) now passes its stated background/target/snooze/recovery scope. Uninterrupted post-boot delivery remains negative, full device/design acceptance is open and no `ios_device` evidence exists. |
| P01-05: representative appearance, digits, text and motion/design review | Partial | [Representative matrix](P01-05-matrix.md), [automated accessibility/navigation](P01-07-accessibility-navigation.md), [numeric layout](P01-08-keypad-layout.md), [Android window-motion fix](P01-09-sheet-window-motion.md). Full press/gauge comparison, spoken traversal/announcements, physical rendering and haptics remain open in [design acceptance](P01-design.md). |

Current retained suites contain 148 Android/shared and 170 iOS ARM64 tests, with
zero failures/errors/skips. iOS uses iOS 27 on the existing iPhone 18 Pro Max
simulator. Android assembly and the rebuilt isolated iOS test host passed. The
current-build Back/keypad checks and the normal/reduced/restored Android motion
sequence passed their documented scopes. All nine palette contrast calculations
pass in both appearance modes; that evidence does not establish physical control
rendering or spoken output.

`tools/baseline/verify_p01.py check` passes retained artifact/hash checks and
requires pending gates to remain declared. It does not certify physical devices
or complete design acceptance. `tools/roadmap.py check` passes schema, dependency,
evidence-reference and generated-view validation; it does not run device tests.

## External blocker

The same missing-physical-device condition has persisted across the preceding
goal turns. The current inventory was independently checked again:

- `adb devices -l`: no attached device.
- `xcrun xctrace list devices`: the Mac and iOS simulators only; no physical iPhone.

There is no active verification process to wait for. Simulator/emulator evidence
cannot supply the required physical evidence. Further green builds or repeated
fingerprint collection would leave that phase exit condition unchanged. P01-05
stays in review and the chat goal is blocked pending external device availability.
The implementation, evidence and original acceptance requirements remain intact.

## Resume conditions

When physical Android and iPhone devices are available, first inspect their
identities and OS versions, then prepare isolated device builds. A locally ad-hoc
signed simulator host is not a signed iPhone build. Preserve owner records and
document originals, and use synthetic records in the isolated application only.

Retain actual-policy notification delivery, foreground/background, denied and
revoked permissions, settings recovery, process/device restart and exact
document/service targets on both phones. Keep any short-delay diagnostic probe
separate from actual expiry-policy evidence. Complete normal/reduced sheet,
press and gauge comparisons against v3, TalkBack/VoiceOver focus order and modal
isolation, spoken success/failure announcements, representative appearance/text/
digits and safe-area checks, and physical haptics/rendering. Record build identity
and device/OS for each result. Only then assess whether P01-05 can become done.
