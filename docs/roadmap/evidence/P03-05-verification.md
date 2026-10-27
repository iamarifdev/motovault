# P03-05: Cross-Platform Recovery and Trustworthy-Local-Beta Verification

- Task and acceptance criteria covered: P03-05 (Exercise wrong secret, missing key, offline restore, partial archive, reinstall and permission re-request; cross-platform compatibility between Android and iOS; physical iPhone waived per owner instruction).
- Date/time and timezone: 2026-10-27 06:26:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / QA & Test Verification.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Android ARM64 (API 36/37), iOS 27.0 Simulator (iPhone 18 Pro Max `50709DF0-FEA9-4B34-9DE6-6172D868875D`).
- Evidence kind: verification.

## Cross-Platform Recovery Matrix

| Scenario | Android Runtime | iOS Simulator | Result |
| :--- | :--- | :--- | :--- |
| **Synthetic Vault Export** | PASS (AES-256-GCM / PBKDF2) | PASS (CryptoKit AES.GCM / CommonCrypto) | PASS |
| **Android Export -> Android Restore** | Verified via 23 native checks | N/A | PASS |
| **Android Export -> iOS Restore** | PASS (Interchange tested) | PASS (Identical archive structure) | PASS |
| **iOS Export -> Android Restore** | PASS (Interchange tested) | PASS (Pure Kotlin ZIP + GCM cipher) | PASS |
| **Wrong Password Refusal** | PASS (`BackupSecurityException`) | PASS (`BackupSecurityException`) | PASS |
| **Tampered Payload Refusal** | PASS (AEAD Tag check abort) | PASS (AEAD Tag check abort) | PASS |
| **Tampered Asset Checksum** | PASS (SHA-256 digest abort) | PASS (SHA-256 digest abort) | PASS |
| **Corrupted ZIP Bounds** | PASS (Expansion limit enforced) | PASS (Expansion limit enforced) | PASS |
| **Duplicate Entity IDs** | PASS (Duplicate check abort) | PASS (Duplicate check abort) | PASS |
| **Offline Restore** | PASS (Zero network dependency) | PASS (Zero network dependency) | PASS |
| **Fresh Reinstall Recovery** | PASS (Clean DB target restore) | PASS (Clean DB target restore) | PASS |

## Test Suite Summary

1. **Multiplatform Unit Tests (`BackupTest.kt`)**:
   - `testZipArchiveWriteAndRead`: Pure Kotlin STORED ZIP roundtrip with CRC32.
   - `testZipRejectsPathTraversal`: Rejects `../` and directory escape attempts.
   - `testZipEnforcesSizeBounds`: Bounds total extraction to 150 MiB, entry to 32 MiB.
   - `testBackupCryptoDeriveKeyAndEncryptDecrypt`: PBKDF2 determinism and AES-GCM envelope.
   - `testCreateBackupAndValidatePreview`: Snapshot consistency and manifest calculation.
   - `testNoLeakedPlaintextInArchive`: Absence of unencrypted vehicle, note, or document data.
   - `testPasswordMismatchThrows`: Immediate refusal on wrong passphrase.
   - `testTamperedAssetFailsValidation`: Bit-flip in encrypted asset halts execution.
   - `testCorruptedZipRejected`: Malformed header rejection.
   - `testDuplicateEntityIdsRejected`: Duplicate logs/rules/documents rejected on preflight.
   - `testTransactionalRestoreSuccess`: Database replaced and reminders scheduled atomically.
   - `testConcurrentSnapshotIsolation`: Writes during backup don't corrupt exported snapshot.

2. **Android Live Runtime Instrumentation**:
   - 23 native storage & backup checks executed and passed on live runtime (`NativeAssetInstrumentation`).
   - Covered key generation, fsync staging, zero-space preflight, zero plaintext leakage, tampered asset/payload rejection, and transactional database restore.
