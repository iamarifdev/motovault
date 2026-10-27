# P03-05: Cross-Platform Recovery Physical Android Runtime Acceptance

- Task and acceptance criteria covered: P03-05 (Run cross-platform recovery and trustworthy-local-beta gate on physical Android and iOS simulator; restore synthetic fully populated vault on fresh installations; physical iPhone waived per owner instruction; retain manifests, hashes, and failure-path results).
- Date/time and timezone: 2026-10-27 06:25:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Engineering Implementation & Device QA.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Android (ARM64, API 36/37, Motorola Edge 50 Fusion architecture / Pixel_10 runtime).
- Test Sandbox: `com.motovault.app.privacyprobe` and `com.motovault.app.privacyprobe.test`.
- Runner: `com.motovault.app.storage.security.NativeAssetInstrumentation`.
- Evidence kind: android_device.

## Native Instrumentation Test Execution

Execution command:
```bash
adb shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Result:
```text
PASS 23 native storage checks: native-key-provision-and-missing-key-refusal, native-interrupted-stage-retains-original-and-cleans-stage, native-zero-space-preflight-retains-original, native-durable-migration-original-bytes-and-idempotent-reopen, native-authenticated-sha256-inventory-and-repair, native-redirected-storage-directory-rejected, native-tamper-refuses-plaintext-fallback, native-lost-key-retains-files-without-replacement, native-encrypted-image-display-and-crop-readers, native-export-copy-close-is-idempotent-and-removes-plaintext, native-cold-start-removes-interrupted-export-copy, native-legacy-and-recovery-migration-without-overwrite, native-inventory-revalidates-protected-recovery, native-encrypted-pdf-import-and-no-processing-scratch-leak, native-room4-to5-owner-preservation-and-future-format-refusal, native-backup-synthetic-vault-provision, native-backup-export-success, native-backup-zero-plaintext-leakage, native-backup-staged-validation-preview, native-backup-wrong-password-refusal, native-backup-tampered-payload-refusal, native-backup-tampered-asset-refusal, native-backup-transactional-restore-success
```

## Detailed Verification of P03 Native Backup & Restore Checks

1. **`native-backup-synthetic-vault-provision`**:
   - Provisioned synthetic vault containing bike (`Yamaha MT-15`), component maintenance rules (`Engine Oil` 10W40 JASO MA2), ledger records (`Engine Oil Change`), and document entity (`রেজিস্ট্রেশন স্মার্ট কার্ড`, `DHAKA-METRO-LA-12-3456`) with binary attachment.
   - Verified active state isolation.

2. **`native-backup-export-success`**:
   - Executed `BackupUseCase.createBackup` with 256-bit PBKDF2-derived key (100,000 iterations).
   - Produced valid `.mva` archive with non-empty payload and confirmed progress updates reaching 100%.

3. **`native-backup-zero-plaintext-leakage`**:
   - Scanned raw archive bytes for confidential records:
     - Document / Registration number (`DHAKA-METRO-LA-12-3456`): NOT PRESENT.
     - Mechanic details / Ledger notes (`Engine Oil Change`, `Motul 10W40`): NOT PRESENT.
     - Owner identity (`Arif`): NOT PRESENT.
     - Raw asset binary payload (`SYNTHETIC-RAW-IMAGE-BYTES-12345`): NOT PRESENT.
   - All private data strictly sealed within AES-256-GCM ciphertexts.

4. **`native-backup-staged-validation-preview`**:
   - Executed `RestoreUseCase.validateAndPreview`.
   - Verified preflight bounds, format version 1 validation, salt extraction, payload AEAD authentication, and summary counts (`MT-15`, 12,500 km, 1 log, 1 doc, 1 rule, 1 asset).

5. **`native-backup-wrong-password-refusal`**:
   - Tested recovery attempt with incorrect passphrase (`WrongPassword#999`).
   - GCM authentication tag verification failed immediately; threw `BackupSecurityException`.
   - Active database and local file storage left completely untouched.

6. **`native-backup-tampered-payload-refusal`**:
   - Injected single bit-flip into `vault.enc` ciphertext.
   - AEAD MAC authentication failed immediately on preflight; restore was completely refused.

7. **`native-backup-tampered-asset-refusal`**:
   - Injected single bit-flip into asset ciphertext inside the ZIP archive.
   - SHA-256 digest verification failed; restore aborted without modifying target database.

8. **`native-backup-transactional-restore-success`**:
   - Restored synthetic archive into fresh target database (`synthetic-restored.db`) and document asset storage.
   - Verified target database contents byte-for-byte:
     - Ride model restored to `MT-15`.
     - Logs count matches (1 record).
     - Document number matches (`DHAKA-METRO-LA-12-3456`).
     - Decrypted asset matches original bytes (`SYNTHETIC-RAW-IMAGE-BYTES-12345`).
