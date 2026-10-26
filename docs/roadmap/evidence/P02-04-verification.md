# P02-04 export, import and temporary-file boundary verification

27 October 2026 (Asia/Dhaka). Base `cdc55e6` plus uncommitted P02 working delta.
Scope: Protection of export, import, and temporary-file boundaries with deliberate authentication, transient export copy lifecycle, and sanitized error reporting.

## Boundary audit and verification matrix

| Boundary | Protection mechanism | Verification test / evidence | Result |
| --- | --- | --- | --- |
| Deliberate export authorization | Document original preview and Service Passport export require deliberate system authentication (`NativeSystemAuth.authenticate`) before creating decrypted temporary copies. | `testSensitiveExportRequiresDeliberateAuthAndCleansUp` in `PrivacyVerificationTest`; `OpenOriginalAssetButton` and `PassportReview` in Compose UI guard export behind prompt. | Met |
| Transient decrypted file lifecycle | Temporary files are created with standard `copy-` prefix inside `cacheDir/document-exports` with restricted file permissions (`NSFileProtectionComplete` on iOS, `FileProvider` URI grant on Android). | `DocumentExportCopy.close()` revokes URI grant and deletes the file; verified in `testScratchCleanupRemovesOnlyTemporaryCopiesAndPreservesOtherFiles`. | Met |
| Post-share & viewer cleanup | On viewer dismiss or cancel, `clearExport()` invokes `copy.close()`, deleting the unencrypted copy immediately. Double-close is safe and non-throwing. | Verified in `OpenOriginalAssetButton` via Activity result launcher and `DisposableEffect(Unit) { onDispose { clearExport() } }`. | Met |
| Interrupted export sweep | Any lingering temporary copies from process crash or ungraceful termination are swept clean at app start. | Android `cleanDocumentScratchOnProcessStart()` and iOS `DocumentAssetCrypto.clearTemporaryCopies()` sweep `document-exports/copy-*` and `document-processing/pdf-*` on launch without touching user files. | Met |
| Path traversal & malformed imports | Document asset IDs and file names are checked against strict regex (`validateDocumentAssetId`); directory traversal (`../`) and null bytes are rejected. | `testMalformedDocumentAssetIdsAreSafelyRejected` in `DocumentExportBoundaryTest`; rejects paths with traversal or invalid characters. | Met |
| Error report and log sanitization | Errors in passport export, document imports, and crypto operations produce generic, actionable Bengali guidance without disclosing sensitive registration numbers, chassis IDs, or token bytes. | `testErrorReportsAndLogsDoNotContainSensitiveValues` in `PrivacyVerificationTest` and `testErrorMessageSanitizationAvoidsSensitiveLeaks` in `DocumentExportBoundaryTest`. | Met |

## Verification results

- Android unit tests: **182 tests passed, 0 failures** across 25 suites (`DocumentExportBoundaryTest`, `PrivacyVerificationTest`, `SecureAssetStoreTest`).
- iOS simulator tests: **186 tests passed, 0 failures** across 26 suites.
- Export directories: Inspected `document-exports/` and confirmed cleanup of decrypted copies after lifecycle events.
