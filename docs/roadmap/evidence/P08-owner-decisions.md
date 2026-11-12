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
