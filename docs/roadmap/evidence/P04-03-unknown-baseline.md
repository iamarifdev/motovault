# P04-03 unknown-history and used-bike baseline

Status: implemented and exercised on the Android emulator, with shared state
checks on the iOS simulator. Screenshots are from the isolated Android
privacy-probe package and synthetic data.

## Behaviour

- Onboarding step 2 now offers `শেষ সার্ভিস মনে নেই` and `শেষ সার্ভিস মনে আছে`.
  The unknown choice creates rules with `BaselineCertainty.UNKNOWN` and no
  service log; it never writes a fake service on the owner's behalf.
- The known choice asks for the real last-service odometer and date. The value is
  stored as `OWNER_ESTIMATE` until a linked service confirms it.
- The unknown choice shows a short first-inspection checklist: tyres and
  pressure, brakes and fluid level, chain slack and lubrication, lights/horn and
  switches, oil/coolant/fuel leaks.
- When the selected catalogue model has a profile family, the screen exposes a
  progressive manual section: model year, market, model code, the matched manual
  reference and a toggle. A code mismatch keeps the owner-defined fallback.
- Skipping setup is allowed. Returning through the gauge detail sheet or the
  schedule editor lets the owner enter a real baseline later; the unknown state
  remains until that happens.
- New onboarding and schedule text resolves through the shared English/Bangla
  resources.

## Evidence

Android 17 Pixel 10 emulator, isolated `com.motovault.app.privacyprobe`:

- [Unknown-baseline onboarding with checklist](P04-android-onboarding-unknown.png)
- [Known-baseline onboarding with real last-service fields](P04-android-onboarding-known.png)

The probe's `native-maintenance-profile-actions-provenance-and-unknown-baseline`
check also verifies an unknown state has no logs and all rules remain `UNKNOWN`.
The iOS simulator probe includes the same check.

The v3 prototype now shows the unknown checklist and real last-service fields;
`rtk run npm run typecheck` passes.

## Verification limits

- No screenshot of the complete manual-match expansion was retained; the source
  and details screen below covers the manual provenance display.
- The Android fixture opens step 2 directly to keep the evidence deterministic;
  the normal brand/model path is unchanged and covered by the existing tests.
- Odometer bounds are enforced by `VaultState.create` and the probe; a real
  owner's paperwork still needs confirmation at the P08 pilot.
