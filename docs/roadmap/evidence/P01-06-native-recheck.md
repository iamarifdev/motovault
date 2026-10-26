# P01 native toolchain recheck

26 October 2026. Base `48fce40321600e4fbdfe124a3bb63b3032276502` plus the
existing uncommitted P01 follow-up. This report does not close P01-05.

The owner confirmed physical Android and iPhone checks must remain pending
because no devices are currently available. `adb devices -l` returned no device;
`xcrun xctrace list devices` listed the Mac and simulators only.

## Current-source checks

- Native macOS aarch64 and Homebrew ARM64 OpenJDK 17.0.20.1 were verified.
  Gradle uses `/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home`.
- The repository wrapper is Gradle 8.10.2. No Gradle/AGP upgrade was performed.
- Android debug APK assembly passed. A subsequent run forced both test tasks
  to execute while allowing unchanged compilation prerequisites to remain cached.
- Android/shared: 148 tests, 21 suites, zero failures/errors/skips.
- iOS ARM64: 170 tests, 24 suites, zero failures/errors/skips. The test task used
  `tools/baseline/isolated-ios.gradle` with
  `-Pp01.ios.device=50709DF0-FEA9-4B34-9DE6-6172D868875D`, the existing iOS 27.0
  iPhone 18 Pro Max. The simulator was initially shut down and was restored to
  that state afterward. No simulator data was reset.
- `git diff --check` passed.

The initial blanket `--rerun-tasks` attempt exceeded the 60-second tool timeout.
The accepted run instead disabled up-to-date checks only for the two test tasks;
both task names appeared without `UP-TO-DATE`, and Gradle exited successfully.
The test XML under `p01/tests/` was refreshed from that run.

## Evidence boundary

The initial evidence check correctly detected a stale source fingerprint for
`MotoVault.kt`. The manifest is refreshed after this recheck. Collection records
current hashes, not proof that historical screenshots were recaptured. Historical
runtime results keep their original reports and dates. The current run verifies
tests and Android assembly only; it does not refresh native UI/motion recordings,
screen-reader spoken output, physical haptics, or physical notification delivery.

Remaining design and accessibility acceptance is tracked in `P01-design.md` and
`P01-05-verification.md`. P01 remains in review until those checks and the
owner-deferred physical gates are satisfied.
