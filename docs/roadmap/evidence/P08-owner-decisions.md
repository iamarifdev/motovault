# P08 owner decisions and waivers

Recorded from direct owner instructions in the working conversation. A waiver
narrows the phase scope; it does not convert an unrun check into a pass.

## WD-01 — Physical iOS verification deferred (12/13 November 2026)

- **Owner:** Ariful Islam (repository owner), direct instruction: "I don't have an
  iPhone at this moment. Skip the verification if it is not possible with the
  simulator. I only have Android device which is connected. Remember that
  globally."
- **Scope:** P08-02's `ios_device` evidence (physical iPhone pass: VoiceOver
  spoken output, iPhone camera/share/voice, iPhone notification/background,
  battery) is **waived/deferred** while no iPhone is available. Simulator
  evidence remains simulator evidence and is not relabelled as physical-device
  proof. Android physical-device checks continue on the connected Motorola.
- **Removal trigger:** when an iPhone becomes available, or before any signed
  release/App Store submission that claims iPhone physical support — whichever
  comes first. The ready one-run plan is
  [P08-02-ios-device-plan.md](P08-02-ios-device-plan.md).
- **Also standing:** do not ask the owner for an iPhone again while this waiver is
  in force; keep working with Android hardware/simulators and record any
  iPhone-only gap as a waiver reference rather than a blocker.
- **Effect on tracker:** P08-02 may close on Android physical evidence plus this
  recorded waiver once its Android-testable checks are complete, provided the
  evidence report names the waiver and keeps the simulator/physical distinction.

## WD-02 — Simulator-only roadmap execution waiver (13 November 2026)

- **Owner:** Ariful Islam (repository owner), direct instruction in the working
  conversation: "I would like to complete remaining phases only using
  iphone/android simulators as much as it can be done. Use argon only when
  planning/or heavy load task, else use hydrogen. Follow workflow and best
  industry practice to resolve issues."
- **Scope:** For `P08` remaining open gates and phases `P09` through `P16`:
  - Physical-device gates (`android_device`, `ios_device`), human-pilot gates
    (`P08-03`, `P14-04` live human sample), live cloud-provider execution
    (`P15` real server/account provisioning), and release-signing/store
    publication gates (`P08-04`, `P16-02`) are **waived/deferred** from blocking
    simulator-doable implementation and verification.
  - All portable checks (`roadmap.py check`, `check_agent_docs.py`,
    `responsibility_gate.py`, `tools/tests`), workstation builds and unit/DB
    tests (`iosSimulatorArm64Test`, Android `assembleDebug`), and runtime
    checks on the Android emulator (`Pixel_10`) and iOS simulator
    (`iPhone 18 Pro Max`, iOS 27.0) remain **mandatory**.
  - Simulator runtime evidence is recorded honestly as `android_runtime` and
    `ios_runtime`, and is never relabelled as `android_device` or `ios_device`.
  - Unblock `P09` in `docs/roadmap/roadmap.json` by depending on
    `["P08-01", "P08-05"]` instead of `["P08-04"]`, leaving `P08-02`, `P08-03`,
    and `P08-04` honestly incomplete for their non-simulator gates.
- **Removal trigger:** when physical devices, human pilot participants, live
  staging cloud infrastructure, or release-signing credentials become available,
  and before external store release or production cloud cutover.
