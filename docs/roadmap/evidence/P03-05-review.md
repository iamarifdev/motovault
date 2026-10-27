# P03-05: Trustworthy-Local-Beta Architectural & Security Review

- Task and acceptance criteria covered: P03-05 (Cryptographic architecture, zero-knowledge threat model, failure recovery, transactional integrity, and trustworthy-local-beta exit gate sign-off).
- Date/time and timezone: 2026-10-27 06:28:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Security & Architecture Review.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Multiplatform (Android 14+, iOS 18+).
- Evidence kind: review.

## Cryptographic & Security Guarantees

1. **Zero Plaintext Leakage**:
   - Manifest contains only non-confidential metadata required for preflight evaluation:
     - Format version (`formatVersion = 1`).
     - Crypto parameters (PBKDF2-HMAC-SHA256, 100,000 iterations, 32-byte salt).
     - Summary counters (bike display name, odometer, record count, document count).
     - Asset digests (per-asset SHA-256 for integrity verification).
   - All vehicle VINs, registration numbers, owner personal information, mechanic repair logs, expense amounts, and raw image/PDF bytes are strictly encapsulated inside AES-256-GCM authenticated ciphertexts.
   - Device-bound hardware keys (Android Keystore / Apple Keychain) are never exported. Archives are encrypted solely with keys derived from the user's explicit passphrase.

2. **Tamper-Resistant Fail-Closed Design**:
   - Any bit-flip, truncated record, or corrupted authentication tag causes AEAD decryption to throw `BackupSecurityException` immediately during preflight.
   - If any asset's SHA-256 digest fails to match the manifest, restore aborts without modifying local database tables.
   - All database updates occur inside a single atomic Room `@Transaction`. An unhandled error or interruption rolls back the database to its pre-restore state without data loss.

3. **Archive Security & Expansion Defense**:
   - Custom `MvaZipArchive` reader rejects directory traversal attempts (`..`, absolute paths, leading slashes).
   - Bounds total archive uncompressed size to 150 MiB and maximum single entry size to 32 MiB, preventing decompression bomb vectors.

## Beta Exit Gate Checklist

- [x] Portable archive specification finalized (`docs/roadmap/P03-BACKUP-CONTRACT.md`).
- [x] PBKDF2-HMAC-SHA256 + AES-256-GCM symmetric envelope implemented natively on Android and iOS.
- [x] Pure Kotlin STORED ZIP archive engine implemented and verified.
- [x] Staged validation pipeline rejecting wrong passwords, tampered digests, and corrupted containers.
- [x] Transactional database restoration and reminder rescheduling verified.
- [x] V3 user interface implemented with natural Bangla microcopy, conflict warnings, and progress feedback.
- [x] Cross-platform recovery verified between physical Android runtime and iOS Simulator.
- [x] Physical iPhone hardware waived per explicit owner instruction.

**Conclusion**: Phase P03 (*Encrypted local backup and complete restore*) is fully complete and approved for production beta.
