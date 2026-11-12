# P07 Android OCR lifetime followup

Date: 12 November 2026 (Asia/Dhaka). Base revision: `ee36e75dd5d03e70c8e14ba8930b9df6f134b14b`, uncommitted P07 working tree. This followup's source and built APK hashes are preserved in `P07-native-source-before-capture-owner.json`; the earlier native verification identity is preserved in `P07-native-source-before-ocr-lifetime.json`. The current combined tree is recorded separately in `P07-native-source.json`.

## Source finding and change

The former cancellable await could leave its `finally` blocks and close the recognizer/recycle its bitmap before the Google Task reported completion. This was a source-lifetime finding, not a reproduced camera crash. The Google Tasks API exposes success, failure and cancellation terminal listeners. ML Kit's own Android example releases its image input after the recognition task completes. [Google Task](https://developers.google.com/android/reference/com/google/android/gms/tasks/Task), [ML Kit input lifecycle example](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

`awaitOdometerTask` now waits for a native terminal signal in a non-cancellable block. It then checks the caller's cancellation before returning or throwing the native outcome. This keeps the bitmap/recognizer alive through native processing and prevents a canceled caller receiving a stale success or failure. An SDK-canceled task becomes the existing recoverable failure path. The camera scratch file is deleted immediately after decoding; only the bitmap remains while recognition runs. There is no new copy, persisted data contract, network call or UI state.

The capture-only instrumentation routing was extracted into `CaptureProbeRunner.kt` so this followup does not expand the already large native-storage test runner.

## Focused verification

On the existing Pixel 10 Android emulator, using a temporary read-only instance and the isolated `com.motovault.app.privacyprobe` package:

1. Caller cancellation while a real Google Task is pending keeps the resource alive. A later success releases it without delivery.
2. The same pending cancellation followed by SDK failure releases it without delivering the stale error.
3. An active caller receives an SDK failure as a recoverable exception.
4. An SDK-canceled Task terminates as a recoverable exception rather than hanging.
5. One synthetic printed `ODO 012345 km` image passes through the actual production ML Kit recognizer and shared parser, producing 12,345.

Observed output: `PASS 4 Google Task lifetime/terminal checks and 1 production ML Kit printed-display case`.

The controlled lifetime checks use the actual Google Tasks library and the production await helper. They do not simulate a successful OCR engine. The fifth check separately exercises production ML Kit. No physical camera, actual odometer photo or speech accuracy was tested in this followup.

Commands, routed through rtk, with the existing local Gradle/Kotlin caches and JDK 17:

```text
rtk run ./gradlew :composeApp:assembleDebug :composeApp:assemblePrivacyProbe :composeApp:assemblePrivacyProbeAndroidTest :composeApp:compileKotlinIosSimulatorArm64 --offline --no-daemon --console=plain -Pkotlin.incremental=false
rtk run adb -s emulator-5554 install -r composeApp/build/outputs/apk/privacyProbe/composeApp-privacyProbe.apk
rtk run adb -s emulator-5554 install -r composeApp/build/outputs/apk/androidTest/privacyProbe/composeApp-privacyProbe-androidTest.apk
rtk run adb -s emulator-5554 shell am instrument -w -e onlyOcrLifetime true com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Build result: `BUILD SUCCESSFUL in 1m 3s`, 141 actionable tasks (22 executed, 119 up-to-date). iOS compilation was up-to-date; no iOS source changed in this followup. Portable localization, Bangla copy, fuel literal, responsibility, dependency, package-cycle, fuel-policy and roadmap gates passed after the code change. The first responsibility attempt rejected growth of the existing test runner; extracting capture routing resolved it before the successful build and runtime run.

The earlier full 320-test run, focused parser/writer/resource tests, 54 tooling tests and native language/UI checkpoints remain scoped in `P07-native-verification.md`. They were not repeated for this Android-only lifecycle change. Machine-local raw logs are retained under `build/p07-ocr-lifetime/`; their hashes are recorded in `P07-02-ocr-lifetime-results.json`.

Physical Android/iPhone camera, glare/blur, permission/background behavior and accessibility acceptance remain required in P07. The owner will arrange those devices and the representative-owner pilot. This followup returns P07-02 to review; it does not close those gates.
