# P15-05 Verification Evidence — Staged Recovery Drills & Simulator Runtime

## Scope & Waiver
- Executed under `WD-02` ([P08-owner-decisions.md](P08-owner-decisions.md)): live cloud provider deployment and external account provisioning are waived; staged `InMemoryStagedCloudTransport` recovery drills and Android/iOS simulator UI runtime checks provide verification while live cloud remains disabled in production (`DISCONNECTED`).

## Staged Recovery & Resilience Drills
- Verified in `CloudBackupTest` across `:composeApp:iosSimulatorArm64Test` (349 tests, 0 failures) and `:composeApp:testDebugUnitTest` (355 tests, 0 failures):
  - Full upload, manifest commit, and pruning recovery (`resumableAssetUploadManifestCommitAndRetentionPruning`).
  - Pre-commit payload corruption rejection (`corruptedAssetBytePayloadIsRejectedBeforeCommit`).
  - Concurrent writer conflict protection (`singleWriterConflictRejectsStaleBaseRevision`).
  - Safe account disconnect/switch preserving local vault state (`disconnectOrSwitchClearsRemoteSessionWhilePreservingLocalVault`).

## Simulator Runtime Checks
- **Android (`Pixel_10`, `emulator-5554`)**: Rendered `ক্লাউড ব্যাকআপ ও সিঙ্ক` sheet with offline/disconnected status and local `.mva` handoff CTA (`p15_android_cloud_backup.png`); `ANDROID_NO_FATAL_ERRORS` verified.
- **iOS (`iPhone 18 Pro Max`, `50709DF0-FEA9-4B34-9DE6-6172D868875D`)**: `--sheet cloud-backup` launch rendered `সংযোগের অবস্থা`, `সংযুক্ত নয় — লোকাল ভল্ট সম্পূর্ণ অফলাইনে কাজ করে`, and `লোকাল .mva ব্যাকআপ খুলুন` (`p15_ios_cloud_backup.png`).
