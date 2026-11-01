# P04-04 schedule source, edits and multi-action service

Status: implemented and exercised on the Android emulator; the shared state
checks run on the iOS simulator.

## Native behaviour

- The gauge detail sheet now shows the action (inspect/clean/lubricate/adjust/
  replace), the source (`manual`/`owner`/`dealer`), the manual label with
  revision and page, the applicability note and an open-manual action.
- The next-due block shows the remaining distance/time and which limit applies.
  Unknown baselines say so instead of showing a fabricated percentage.
- `ব্যবধান বদলান` opens the schedule editor. It shows the same provenance, allows
  distance/time edits (zero means no limit), keeps the baseline state, and marks
  an owner-adjusted manual interval as owner-adjusted on save.
- The service flow now offers `lubricate` in addition to inspect/clean/adjust/
  replace, links schedules by catalogue item *and* action, and supports several
  action lines in one visit. Each linked line resets only its own action.
- The v3 prototype's gauge detail now shows the action and source revision/page;
  the service and onboarding prototypes were extended with the same states.
  `rtk run npm run typecheck` passes.

## Evidence

- [Native schedule details with manual revision/page and editable interval](P04-android-schedule-details.png)
- Unit tests cover mixed-action resets, action-specific chain schedules,
  owner-adjusted intervals, unknown baseline display and service validation.
- The Android probe name is
  `native-maintenance-onboarding-and-schedule-details-ui`.

## Limits

- The Android screenshot uses synthetic data and the isolated probe package.
- The service action picker is still a dropdown rather than the v3 chip grid; the
  fast multi-line visit and reset behaviour are preserved.
- TalkBack/VoiceOver speech and physical-device layout remain release-gate work.
