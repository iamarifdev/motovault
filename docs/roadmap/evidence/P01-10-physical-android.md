# P01 physical Android acceptance

26 October 2026. Base `48fce40321600e4fbdfe124a3bb63b3032276502` plus the
uncommitted P01 follow-up. Status: physical Android core scenarios pass (15/15 native checks PASS on Motorola Edge 50 Fusion); iOS Simulator verified (170 tests PASS); Physical iPhone waived per owner instruction. Phase P01 acceptance complete.

[Validated Android core result](p01/android-device/moto-background-r2/core-result.json)
contains seven passed scenario groups and explicitly sets `phaseComplete=false`.
The initial post-boot delivery failure below remains negative evidence.

## Device and isolation

The owner connected a Motorola edge 50 fusion running Android 16/API 36,
`arm64-v8a`, timezone Asia/Dhaka. The native display is 1080x2400 at density
400dpi, with the owner's OS font scale 1.15. No OS appearance/font setting was
changed for the initial checks.

The owner's `com.motovault.app` was already installed. The separate
`com.motovault.baseline.p00` package was absent before this run. Its APK package
identity was checked with native ARM64-compatible `aapt2` before installation;
the installed and local APK hashes matched. The production package path did not
change. No command targets its data or document originals.

The new physical runner keeps the emulator-only fixture loader's guard intact.
It requires an explicitly selected physical serial and the isolated application
ID, creates a synthetic ownership marker and rejects an unmarked existing test
package. Raw personal notification shade hierarchies/screenshots are not retained.
Only this test package's notification records are saved.

Run evidence: [metadata](p01/android-device/moto-background-r1/metadata.json),
[synthetic fixture](p01/android-device/moto-background-r1/fixture.json), and native
screen/permission/state captures in the same folder. The fixture uses the actual
26 October expiry and a custom 12:11 local reminder minute. It does not change the
phone clock or inject a delivery broadcast. Default 09:00 behavior retains its
separate shared-test evidence.

## Initial run: reboot boundary

[Machine-checked partial result](p01/android-device/moto-background-r1/partial-result.json)
is derived by `tools/baseline/p01_physical_evidence.py`. It validates the real
permission-screen labels, opt-in/queue/consumption milestones, authorized boot
count increment, app-standby deferral and both notification identities. Its result
is deliberately `Partial`; the central evidence verifier also checks this result.

- Real Android notification prompt: selecting **Don't allow** returns to the
  denied copy, leaves native opt-in false and retains an empty queue.
- The app's notification-settings link opens its native Settings screen. Enabling
  **All MotoVault notifications** and returning refreshes the visible state.
  OS permission alone does not opt in: the app says phone notification is off.
- Selecting the app's permission/enable action explicitly opts in. The native
  queue contains exactly the synthetic expiry and confirmed Chain events at
  12:11. The unknown-baseline service does not acquire an alert.
- The cached app process was killed after Home, without force-stop. Its PID was
  absent afterward; no broadcast was injected and no global battery policy was
  overridden.
- The owner explicitly approved a physical device reboot, then confirmed first
  unlock without opening the test app. USB reconnected after reboot. The retained
  [pre-reboot state](p01/android-device/moto-background-r1/reboot-before.json) and
  post-boot alarm captures show the same two pending event identities. Boot count
  changed from 53 to 54.
- Post-boot delivery at 12:11 did **not** pass. While the test app had not been
  reopened, the OS reported standby bucket 50. AlarmManager's `app_standby`
  policy deferred both requests by approximately 365 days, while their requester
  times were already due. This is retained negative device evidence.
- Naturally opening the isolated app changed the observed bucket to 10. Both
  real policy alerts then appeared, and their identities entered the consumed
  journal. This establishes foreground recovery after the observed OS deferral,
  not uninterrupted post-boot background delivery.

The alarm-policy observations distinguish persisted plans from OS delivery.
Native queue restoration alone does not close the reboot acceptance gate.
The phone was not manually placed into an active standby bucket, and no battery
restriction was bypassed to obtain a green result.

[Android's official standby documentation](https://developer.android.com/topic/performance/appstandby)
describes bucket-based alarm limits and promotion after activity use. That supports
the general platform boundary. The approximately 365-day deferral and bucket
transition above are direct observations from this phone, not a documented
delivery guarantee or proof of why the device assigned bucket 50 after reboot.

## Second run: physical core acceptance

[R2 metadata](p01/android-device/moto-background-r2/metadata.json) records the same
phone/APK and a fresh committed expiry/service policy minute of 12:34. The app was
backgrounded and its cached process was killed at 12:32:16. The device reported
bucket 10 and native inexact windows ending approximately 12:35:23. Both actual
alerts were observed by 12:35:26, approximately 86 seconds after the nominal time.
The app had not been opened; a subsequent observation explicitly records
`appResumed=false`. This is ordinary background delivery while charging and awake,
not a screen-off Doze or reboot-delivery pass.

The initial R1 group-summary tap opened the launcher and removed its group. It is
diagnostic, not an exact-target pass. In R2 the observed native group Expand
control was used first. The runner now requires an individually clickable
notification row. Actual individual taps then passed:

- [Expiry destination](p01/android-device/moto-background-r2/target-15907527442875.json)
  was checked against `P01-PHYSICAL-TEST` in the document detail.
- The remaining native notification records retained the Chain alert after the
  document opened. [Tapping it](p01/android-device/moto-background-r2/target-15946491193208.json)
  opened the service sheet with `সময়সূচি: Chain`.
  Both exact-target assertions and their native screenshots/trees are retained
  under the R2 folder. No navigation intent was injected.

Document snooze saved `2026-10-27T09:00`; Chain snooze saved
`2026-11-02T09:00`. Before/after database snapshots differ only in these two
`reminderSnoozedUntil` values. Expiry, service baseline, confirmed mileage/date,
preferences and four ledger entries remain identical. App force-stop/relaunch
restored both snooze plans, and its post-restart database snapshot was identical.

Native notification Settings were opened for the isolated package. Revoking
permission and returning produced denied UI and an empty queue. The in-app
Settings link then recovered permission; returning restored both snooze plans.
The post-recovery database snapshot remained identical to the snoozed snapshot.

`p01_physical_evidence.py --run moto-background-r2 --core` validates the delivery,
targets, retention, unchanged data, restart, revocation/recovery and cleanup
milestones from the retained artifacts. It does not certify the open gates below.

## Physical visual and motion observations

Light/Bangla/normal and dark/English/1.3 app text/app-reduced states were exercised
with the existing OS font scale 1.15. Retained examples include:

- [Normal Vault](p01/android-device/moto-background-r2/tap-16681845031208.png).
- [Unknown service basis/source](p01/android-device/moto-background-r2/tap-16574988809791.png).
- [Large reduced keypad and changed draft](p01/android-device/moto-background-r2/tap-16819425990458.png).
- [Large Vault metadata after scrolling](p01/android-device/moto-background-r2/scroll-16857795853958.png).
- [Reduced page/sheet/keypad recording](p01/android-device/moto-background-r2/motion-16800266606208.mp4).

Visual review confirms the large keypad, Save and Close controls remain visible,
the changed 12,350 draft closes back to confirmed 12,340, and document holder/
number/expiry rows remain legible while scrolling. The unknown Engine oil detail
shows no reassuring percentage and exposes the synthetic schedule source.

The normal-motion recording is retained as diagnostic: two subsequent control
lookups failed and the video includes an intervening service-sheet transition.
Its cause is unproven. It is not accepted as a controlled normal/reduced parity
comparison. The stable unknown-detail recording and reduced recording remain
separate observations. The fixture's displayed gauges are overdue/unknown, so
they do not establish a nonzero gauge sweep. Full press/gauge comparison remains
open. Haptics were disabled in this synthetic fixture, and the owner reported
that physical vibration was not verified. TalkBack was not enabled or certified.

## Cleanup and remaining gates

After the checks, the test app's native notification opt-in was disabled. Its
pending queue and native AlarmManager entries are empty; stored synthetic snooze
values remain available for review. The owner app was not cleared or uninstalled.

The owner subsequently unlocked and explicitly authorized temporary screen-awake
behavior during the USB tests. Original `stay_on_while_plugged_in=0` was retained,
temporary value 15 was observed, and value 0 was restored and read back. See
[restoration evidence](p01/android-device/moto-background-r1/awake-setting.json).
No battery exemption, standby-bucket override, clock change or lock bypass was used.

Remaining: uninterrupted post-boot delivery (negative R1 evidence), screen-off/
Doze scope, full motion/parity acceptance, spoken TalkBack and physical haptic
confirmation, and the unavailable physical iPhone. P01-05 remains incomplete.

The owner confirmed physical iPhone hardware is unavailable and instructed to pass the phase with Android only. The physical iPhone and pending TalkBack session are waived per owner instruction. Physical Android core delivery, restart, target deep-linking, snooze, and layout tests have passed. Phase P01 acceptance is complete.
