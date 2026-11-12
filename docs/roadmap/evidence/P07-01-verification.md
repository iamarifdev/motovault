# P07-01 verification

Revision: `ee36e75` plus the source files hashed in
[P07-01-source.json](P07-01-source.json). Date: 12 November 2026 BDT.

One combined Gradle run passed:

```text
:composeApp:testDebugUnitTest
:composeApp:assembleDebug
:composeApp:compileKotlinIosSimulatorArm64
--offline --no-daemon --console=plain
```

Used existing `.gradle-home` / `.konan` caches and JDK 17. Result: **320 tests,
zero failures/errors/skips**, Android debug build and shared iOS compilation.
The suite includes the previous 305 tests plus 15 focused regressions for the new
input/capture rules. [Counts](P07-01-unit-results.json). Unchanged passing tests
will be reused; later native work will run new/affected checks rather than repeat
this complete suite without a reason.

New checks cover supported digit sets, lakh/international grouping, malformed
amounts, finite numbers, paid amount versus fee/balance/phone/mileage, synthetic
Bangla and Arabic-Indic messages, ambiguous totals, invalid odometer regressions,
photo ODO versus TRIP, unlabeled multiple readings, source/confidence, payload
limits, stable catalogue IDs, exact service poisha and unmatched text. Production
SHA-256 is checked against the known `abc` vector; normalized repeated shares
are detected. Old JSON, bounded accepted IDs and the real encrypted backup
preferences round-trip are also covered.

Passed portable checks: expanded UI literals, fuel literals, Bangla-copy lint,
responsibility, dependency direction, package cycles, fuel numeric policy, all
54 tooling tests (including resource parity), and roadmap validation. There is
no new resource/copy surface in this core slice, no baseline exception and no
production file above 400 lines. Domain/application imports remain independent
of UI and native services; native digest behavior is injected.

This is Android-hosted common-unit, source/gate, Android build and iOS compilation
evidence. It does not prove physical camera, real microphone/Bangla provider,
native share delivery, VoiceOver, pilot effort or publication. P07-02 onward keep
those separate gates intact.

## Native integration follow-up

The first real iOS capture execution exposed an unsupported Unicode regex property during `CaptureTextNumbers` initialization. The shared rules now use portable character-category boundaries. Four native text regressions and five actual-OCR printed images passed on both platforms after the fix; 14 affected JVM tests passed. The full 320-test suite was not repeated. Current source/artifact identity and staged verification are in `P07-native-source.json` and `P07-native-verification.md`. The original core manifest records the earlier snapshot, not the later whole phase.
