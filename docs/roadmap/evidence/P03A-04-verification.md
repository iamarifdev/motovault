# P03A-04 backup and restore verification

Status: passed on the uncommitted working tree based on
`860c3ec04dac73bc10d6aba65798aff31887fdbb`. The incremental history remains in
[P03A-04-backup-progress.md](P03A-04-backup-progress.md).

## Boundaries under test

- `domain/backup` owns the archive format, crypto contracts, validation and
  snapshot models; it has no UI or concrete storage imports.
- `application/VaultBackupOperations.kt` and
  `application/VaultBackupRestorer.kt` define the workflows.
- `storage` adapters connect the legacy persisted representation, Room, the
  encrypted asset store, crypto services and `RecoverySnapshotStore`.
- `ui/backup` renders localized create/restore/recovery states and reports typed
  failures. The shared-UI literal gate reports no direct literal in this scope.

## Android runtime

Environment: Android 17 (API 37) Pixel 10 emulator, isolated
`com.motovault.app.privacyprobe` build, synthetic Room database, synthetic
encrypted asset stores and device-bound test keys.

Standard suite, 2026-11-01:

```text
adb shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result: `PASS 34 native storage checks`, including:

- backup export, zero-plaintext leakage, staged validation and preview;
- wrong-password, tampered-payload and tampered-asset refusal;
- transactional restore, original preservation and same-ID/different-content
  collision remapping with Room reopen;
- `synthetic-backup-create-restore-localization-ui` (English and Bangla create
  and restore entries);
- `synthetic-backup-ui-to-injected-create-adapter`;
- recovery UI success, stale-conflict and busy-dismissal checks, plus encrypted
  recovery snapshot reopen.

Full UI-driven round trip, run on the same emulator:

```text
adb shell am instrument -w -e onlyRoundTrip true com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result: `PASS synthetic backup UI restore and Room reopen`. The fixture created an
archive through the screen with a synthetic document, saved it through the system
picker, changed the current synthetic vault in a second isolated store, reopened
the file through the picker, entered the password, reviewed and confirmed the
restore. A fresh Room connection verified the restored bike and remapped document
ID; the previous vault was retained in the authenticated recovery snapshot, and
both distinct original byte sequences remained readable.

App/test APK SHA-256:
`ed13ad3b1667d376b5754177435e73d61f20b6c97cabbb02eb4e52f2c9d33fe4` and
`858f880682258641ce719a89ae5ccd015cf5c33e425fd6866f32e60156a593a9`.

Current screenshots:

- [Backup created, English labels with the owner's digit preference](P03A-current-android-synthetic-backup-created.png)
- [English backup screen](P03A-current-android-synthetic-backup-en.png)
- [Bangla backup screen](P03A-current-android-synthetic-backup-bn.png)

## iOS runtime

Environment: iOS 27 iPhone 18 Pro Max simulator (`iPhone19,3`), ad hoc signed
debug `com.motovault.app.privacyprobe` build. Build command:

```text
xcodebuild -project iosApp/MotoVault.xcodeproj -scheme MotoVault -configuration Debug \
  -destination 'platform=iOS Simulator,id=50709DF0-FEA9-4B34-9DE6-6172D868875D' \
  -derivedDataPath build/ios-p03a build \
  PRODUCT_BUNDLE_IDENTIFIER=com.motovault.app.privacyprobe \
  CODE_SIGN_IDENTITY=- CODE_SIGNING_ALLOWED=YES CODE_SIGNING_REQUIRED=YES
```

The app was launched with `--motovault-storage-probe` and wrote:

```text
PASS 20 native iOS storage checks
```

The result file is retained verbatim at
[P03A-04-ios-native-probe-current.txt](P03A-04-ios-native-probe-current.txt).
Backup/restore-relevant checks:

- `native-backup-adapter-create-preview-wrong-password` — encrypted archive
  creation, authenticated preview and wrong-passphrase refusal;
- `native-backup-coordinated-recovery-collision-room-reopen` — coordinated
  restore with an asset-ID collision, preserved previous original, remapped
  incoming asset, authenticated previous-state recovery snapshot and Room reopen;
- `native-interrupted-stage-retains-original-and-cleans-stage`;
- `native-zero-space-preflight-retains-original`;
- `native-tamper-refuses-plaintext-fallback` and
  `native-encrypted-recovery-snapshot-and-reopen`;
- `native-room4-to5-owner-preservation-and-future-format-refusal`.

The built app binary SHA-256 is
`e82c62d390ed7610ba69328535398e923730b14b4a41fbb343d8872dfbaaa105`; the result
file SHA-256 is
`e736d1df601a5771b9a93f26f49cd809c82394c7aa33b1cb75d3bc1a5a2b949b`. The prior
Bangla iOS backup-create screen is retained at
[P03A-04-ios-backup-create-bn.png](P03A-04-ios-backup-create-bn.png).

## Localization coverage

Backup progress, passwords, validation, restore review and recovery states
resolve through shared English/Bangla resources with placeholders and plurals.
The Android suite drove the English and Bangla create/restore screens; the
resource parity tests pass for the shared catalogs. Native picker chrome remains
system-owned and is not localized by MotoVault.

## Limits

- No physical Android or iOS device was used. Physical-iPhone checks remain in
  P08-02.
- The iOS restore evidence exercises the real adapters, coordinator, crypto
  bridge, Room and recovery snapshot service, but not the production iOS sheet or
  Files picker. The Android `onlyRoundTrip` fixture drives the actual screen and
  system pickers.
- Process-kill recovery in the middle of the durable transaction is not claimed;
  the interrupted-stage and zero-space fixtures cover staged-file failure.
- Historical archives from a released build are not required before first release
  and were not tested. Current-format archives were.
- Unique synthetic export files remain in the emulator's selected folder.
