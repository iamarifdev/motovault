# Document recovery hardening

Local implementation evidence, 12 October 2026. This continues Phase 3 of `CONTINUATION_HANDOFF.md` and preserves the approved document screens.

## Failed operation ownership

`DocumentAssetOperation` records newly allocated filenames before the platform writes bytes. Android image import, PDF rendering/import, cropping and CameraX capture use it. iOS picker image/PDF import, cropping and AVFoundation capture also use it. Failure or coroutine cancellation removes the operation's allocations, including a raw original whose preview could not be created, partial multi-page imports, partial PDF renders and accepted crops whose preview failed. Existing page IDs are never registered by these operations. Android camera callbacks also clean up when the composition's coroutine scope was already cancelled before its result callback ran.

The import callback receives its result inside the operation boundary. Android PDF render intermediates are removed after conversion on success; rollback handles intermediates left by failure. OCR failure remains a usable document draft with `readingFailed`; it does not delete otherwise readable originals. No saved field, legacy Base64 data, document ID, original pixels or page order is rewritten by these changes.

## Startup recovery

`recoverOrphanDocumentAssets` is called by the process-scoped vault restore owner after successful database restoration and before capture becomes available. A failed restore must never call it. Each platform also guards recovery to run only once per process, preventing a new activity composition from collecting assets belonging to an active draft or a pending write.

Recovery considers only regular files in the private managed document directory, with the app's existing `epochMillis-randomLong.extension` generated filename format. It excludes all asset IDs referenced by persisted documents (raw originals, accepted crops, previews and source PDFs). Unknown/legacy names, directories, symlinks, missing/zero timestamps, future timestamps and files younger than 24 hours are kept. Eligible files move into a separate private `documents-recovery` directory on the same storage volume. Every byte is retained. There is no automatic purge, expiration or deletion of quarantined originals. A failed rename/move leaves the source in place. Existing recovery filenames are never overwritten; collisions receive a fresh recovery suffix.

Android recovery lives in `<app filesDir>/documents-recovery`; iOS recovery lives in `Application Support/documents-recovery`. Same-volume moves retain file timestamps where the platform supports this. `restoreRecoveredDocumentAsset(id)` is a platform seam that moves an owner-selected recovered file back to managed storage, validates the safe filename and regular file, and refuses to overwrite an existing managed file. It does not fabricate a saved document record or confirm OCR fields. An owner-facing recovery browser is not implemented yet.

Automatic approval review rejected launching the earlier APK because startup recovery could permanently delete unreferenced originals. Startup recovery was changed to reversible quarantine before retrying runtime verification. Failed-operation rollback still deletes only files allocated exclusively by that failed operation. Startup quarantine does not attempt to restore an interrupted unsaved draft. Draft files abandoned by process death become eligible after the grace period on a later successful launch, while remaining recoverable.

## iOS picker import byte limit

The iOS picker now reads both PDFs and images through `NSFileHandle` in chunks of at most 8 KiB. Its buffer accepts at most 32 MiB (33,554,432 bytes). Once full, the reader probes one extra byte and rejects oversized originals instead of silently truncating them. It no longer calls `NSData.dataWithContentsOfURL` before enforcing the import limit. Empty files are rejected. Successful reads preserve original bytes and still use the existing asset operation, so a later failure removes only allocations made by that operation and cannot alter saved owner records.

The reader closes its handle in `finally` on success, rejection and coroutine cancellation. Security-scoped URL access is started before opening and stopped in an outer `finally` when access was granted. Cancellation is checked before each chunk. This is an encoded file-size bound; it does not establish a decoded image memory limit.

On 13 October, `:composeApp:compileKotlinIosArm64 :composeApp:iosSimulatorArm64Test` completed successfully. XML results recorded **112 simulator tests**, zero failures/errors. `DocumentImportReadTest` executed four iOS filesystem cases: exactly-at-limit bytes spanning the 8 KiB chunk boundary, one-byte-over-limit rejection with unchanged original bytes, a real descriptor closed after rejection, and a real descriptor closed after cancellation while suspended. Small configurable bounds avoid allocating 32 MiB fixtures; production uses the fixed 32 MiB default. These tests validate native file reads and cleanup, not physical file-provider or camera behavior.

## Verification

Executed on the local checkout:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home rtk run ./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosArm64 :composeApp:testDebugUnitTest :composeApp:iosSimulatorArm64Test --console=plain
```

Earlier result: `BUILD SUCCESSFUL`. XML reports recorded 62 Android tests and 69 iOS simulator tests, zero failures or errors. The document capture suite executed 15 cases on both targets. Three new cases cover selective orphan eligibility, operation failure/cancellation preserving unowned files, and success transferring ownership without cleanup. This run predates the process-session integration and reversible-quarantine follow-up.

The quarantine follow-up adds four Android filesystem fixtures for byte-for-byte retention and restore, timestamp preservation, saved-reference/live-draft exclusion, collision retention, restore overwrite rejection, symlink/legacy-file exclusion, and dangling-link collision preservation. All four executed and passed.

Final combined command added `:composeApp:assembleDebug` to the compilation/test command above. It passed in 18 seconds and produced the debug APK. XML reports recorded **74 Android tests and 80 iOS simulator tests**, zero failures or errors, including the integrated process session and service-passport changes. The Android fixture suite recorded four tests, zero skipped/failing/error cases. Platform warnings remain (Room expect/actual beta, deprecated Compose/Gradle API and passport iOS interop opt-in warnings); none was a compiler error. Runtime launch and original-byte comparisons are verified separately by the runtime agent.

These are compiler and common test results. They do not prove physical camera readability, iOS picker/camera execution or recovery under actual process termination. The user explicitly chose synthetic fixtures for now and real-card checks later. Synthetic fixtures are not real-card evidence.

## Bangla OCR decision

The current Android adapter uses ML Kit's bundled **Latin** text recognizer. Google's [Text Recognition v2 overview](https://developers.google.com/ml-kit/vision/text-recognition/v2) documents Latin, Chinese, Devanagari, Japanese and Korean character sets. Its [supported language list](https://developers.google.com/ml-kit/vision/text-recognition/v2/languages), checked on 12 October, does not list Bengali. Devanagari support is not Bengali support. Adding its Devanagari package would not establish Bangla OCR.

The current iOS adapter explicitly requests `en-US`. Apple's [supportedRecognitionLanguages()](https://developer.apple.com/documentation/vision/vnrecognizetextrequest/supportedrecognitionlanguages()) provides the language list supported by a particular request. A generic API page cannot prove Bangla support on the eventual iOS host and OS revision. That host is still absent, so Bengali support has not been queried on an iOS runtime and is not claimed. Before selecting Vision for Bangla, query the deployed accurate-recognition request and revision and check for `bn`/`bn-BD`, then validate real document images.

Bangla digit normalization in manually supplied text remains supported by common parsing tests. It does not mean either native OCR engine reads Bangla. No new OCR package, online document upload, cloud engine or unverified model was added. A separate validated Bengali-capable engine decision remains required.
