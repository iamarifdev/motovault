# P07 local native verification

Date: 12 November 2026. Base: `ee36e75`, with uncommitted P07 changes. `P07-native-source.json` records the current source and built-artifact hashes after the native-camera ownership followup. `P07-native-source-before-ocr-lifetime.json` preserves the original runtime identity; `P07-native-source-before-capture-owner.json` preserves the intermediate Android OCR followup identity. P07 was closed on the user-approved scope using Motorola physical Android evidence and iOS 27 simulator evidence. This is not physical iPhone, cloud-staging, signed-release or publication acceptance.

## Passed checks

| Scope | Result and limit |
| --- | --- |
| Core suite | 320 tests passed once, 0 failed/errors/skipped. See `P07-01-verification.md`. |
| Focused unit work | First 10 selected tests, then 14 affected parser/ingress regressions after the iOS failure, then one resource-parity test after final copy. Selections overlap; counts are not additive. `P07-focused-unit-results.json`. |
| Portable checks | UI literals, Bangla copy, fuel literals/policy, responsibility, dependencies, package cycles and roadmap passed. The unchanged 54 tooling tests passed in the core batch and were reused. |
| Android | Debug and isolated privacyProbe APKs plus native instrumentation APK compiled. Existing Pixel_10, Android 37.1, arm64, temporary read-only emulator. |
| iOS | Shared Kotlin compilation and native Debug host/real CaptureShare extension built. Only existing iOS 27 iPhone 18 Pro Max, `50709DF0-FEA9-4B34-9DE6-6172D868875D`, was used. |
| Actual OCR adapters | Production ML Kit and production Vision each passed five synthetic printed-image cases: leading-zero valid odometer, regression, unsupported range, blank and multiple readings. No fabricated recognition output. |
| Native parser regression | Both native probes passed four text cases: payment versus balance, Bengali service items, compound-word rejection and malformed attached amount. Android also passed three actual Intent payload-boundary cases. |
| Android UI | Explicit candidate selection, applied reading before final Save, independent English/Bangla digits, dark/larger text, actual permission denial/manual return and reviewed voice/share disclosures. Final native UI asserts `৳৫০০.৫০` and `৳500.50`. |
| iOS visual checkpoints | Actual OCR preview/candidates, explicit selection, applied 12,345 reading with final Save still required, English/Bangla digits and dark/larger text. Final Bengali/English review captures preserve 500.50 and show honest unavailable native Bangla voice. Real native picker cancel and TCC-denial both return to usable manual entry, with Save disabled during review and enabled afterward. |
| iOS real share target | System UIActivityViewController -> actual MotoVault Share Extension -> explicit Prepare -> production App Group intake. Exact synthetic text, share completion, backup exclusion and deletion after intake passed. See `P07-ios-share-runtime.json`. |
| Prototype | TypeScript/Vite build and one bounded browser walkthrough. See `P07-prototype-walkthrough.md`. |

The simulator's file-protection attribute was empty/unavailable. The source requests complete protection, but that attribute and real locked-device behavior remain physical-device gates. The positive share report does not establish those properties.

Compose 1.7 exposes only its root in this iOS runtime's XCTest tree. Compose checkpoints assert launch/frame and capture real images, which were visually reviewed. They are not text/VoiceOver assertions. UIKit share controls and prepared status were asserted through actual native accessibility. P07 VoiceOver/TalkBack and physical focus/back behavior remain open.

The iOS 27 simulator exposes a virtual camera UI. A blank virtual preview is not a physical-camera result or an unavailable-camera result. Camera cancellation and TCC-denial are recorded separately from printed-image OCR; no simulator shutter result is labeled a real odometer photo.

## Focused iOS share cancellation

On 12 November, ran only `MotoVaultUITests/CaptureFlowTests/testNativeShareExtensionCancellation` on the already-booted iOS 27 iPhone 18 Pro Max simulator (`50709DF0-FEA9-4B34-9DE6-6172D868875D`). The result bundle `build/p07-native/native-share-cancel-corrected.xcresult` reports 1 passed, 0 failed. The test opened the native share sheet, selected MotoVault through its accessibility cell, opened the extension, dismissed it before preparing a draft, and verified the app returned to the foreground with the extension dismissed. No full UI suite was repeated. This is simulator evidence only; physical iOS share behavior remains open.

## Bugs found and bounded rechecks

- Initial iOS execution failed with `PatternSyntaxException: No such character class`. Unicode regex properties worked on the JVM but failed at Kotlin/Native initialization. Replaced those boundaries with portable character-category checks. Ran only the affected 14 unit tests and native corpus/parser checks again.
- Review amounts initially used the old whole-taka formatter. Reused the exact-poisha cost formatter for captured candidates/amounts and preserved exact fuel/service prefill. Checked final native Bengali/English 500.50 displays and resource parity.
- The first iOS share target selector treated the native activity label as a Button. Inspected its real layout and corrected only that selector; the actual extension round trip then passed. The failed run's verbose diagnostic collection stalled and was stopped; it is not a passing result.
- Cold Android fixture startup needed readiness waiting after APK installation. Final amount checks waited for the actual app tree. No failed/unfinished snapshot is used as acceptance evidence.
- Prototype walkthrough corrected the locked-text leak, Save during photo review and clipped voice panel, then checked just those changed states and exact amount rendering.
- Source review found that Android caller cancellation could release the bitmap/recognizer while the native ML Kit Task still owned its input. The followup waits for native termination, then suppresses delivery to the canceled caller. Four Google Task lifetime/terminal checks and one production ML Kit printed-display case passed. Android compilation and iOS compilation passed; earlier full suites and unchanged UI matrices were reused. See `P07-02-ocr-lifetime.md`.
- Completion source audit found native camera acquisition inside the private sheet's composition. Moved ephemeral acquisition to the root so camera pause can remove private content without losing the callback. Three new state tests, a real external-camera Android consumer-disposal/cancel check and the single affected iOS camera-cancel visual checkpoint passed. Native authentication and physical locked/unlocked camera checks remain open. See `P07-02-capture-owner.md`.

## Reproduction scope

Use `GRADLE_USER_HOME=$PWD/.gradle-home`, `KONAN_DATA_DIR=$PWD/.konan` and `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`. All shell commands were routed through rtk.

- Core: `:composeApp:testDebugUnitTest :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64`, offline/no-daemon.
- First focused selection: `:composeApp:testDebugUnitTest --tests '*CaptureIngressTest' --tests '*FuelEntryAdapterTest' --tests '*StringResourceParityTest'`.
- Native parser fix: `:composeApp:testDebugUnitTest --tests '*CaptureDraftParserTest' --tests '*CaptureIngressTest'`.
- Android corpus: `adb -s emulator-5554 shell am instrument -w -e onlyCapture true com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation`.
- iOS corpus: `xcrun simctl launch 50709DF0-FEA9-4B34-9DE6-6172D868875D com.motovault.app.privacyprobe --motovault-capture-probe --motovault-capture-fixture`. The isolated debug host writes the actual result to its Caches directory.
- Native host: Xcode project/scheme MotoVault, Debug, the allowed destination above, isolated `MOTOVAULT_BUNDLE_ID=com.motovault.app.privacyprobe`, arm64. UI-only harness builds reused the freshly built Kotlin framework with `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES`.
- UI: run the named `CaptureFlowTests` methods, then export xcresult attachments and inspect the images. Use final exact-money captures for the unified text review.
- Prototype: `rtk run npm --prefix design/app-ui-v3 run build`; browser walkthrough at local v3 demo only.

## P07 closure and P08 release gates

The owner directed that P07 be completed without a physical iPhone. The roadmap scope now accepts Android physical and iOS 27 simulator evidence for the implementation slice, while preserving physical-device and field-study gates under P08. Simulator results are not labeled physical iPhone proof.

| Task | P07 closure evidence | P08 follow-up |
| --- | --- | --- |
| P07-01 normalized drafts and parsing | Core suite, parser regressions and compatibility reports passed. | None specific to P07. |
| P07-02 photo review and fallback | Synthetic ML Kit/Vision corpus and native review UI passed. Motorola denial/manual fallback passed. Two physical `ODO 12345 km` photos produced no candidate; this limitation is retained and is not reported as OCR success. | P08-02 physical iPhone camera, cross-device glare/blur, accessibility and reliability checks. |
| P07-03 share-to-draft | Android warm share and the cold-start synthetic Save/Undo passed with Room readback and receipt removal. The iOS 27 simulator passed real extension intake and focused cancel. | P08-02 physical iPhone and full locked/duplicate/unsupported/cancel device matrix. |
| P07-04 voice availability | Native controls, processing disclosure, capability reporting, manual fallback and no-audio-retention behavior are implemented and reviewed. Neither device supplied local bn-BD recognition; no real transcript is claimed. | P08-04 provider decision/provisioning and actual bn-BD, network, noise, permission and privacy checks. The provider plan remains review-only. |
| P07-05 capture walkthrough | Android physical observations, iOS simulator walkthrough and the ready-to-run pilot protocol are recorded. No multi-owner success, effort or correction sample is claimed. | P08-03 representative Bangladesh-owner pilot. |

The source/artifact manifest and roadmap checks passed. The full unit and native suites were not repeated for closure; prior scoped results were reused. No owner records, raw audio or share payloads were retained. P07 is complete under the revised scope; P08 release gates above remain open.
