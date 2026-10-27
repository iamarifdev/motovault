# P03-03: Staged Validation and Transactional Restore Implementation

- Task and acceptance criteria covered: P03-03 (Bound archive expansion, reject unsafe paths, unsupported versions, tampered manifests/assets, preview counts and conflicts, atomic restore/rollback without deleting previous vault, reconcile reminders after success).
- Date/time and timezone: 2026-10-27 05:04:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Engineering Implementation.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Kotlin Multiplatform (Android 14/15, iOS 18/27).
- Evidence kind: implementation.

## Implementation Details

1. **Staged Preflight Validation (`RestoreUseCase.validateAndPreview`)**:
   - Stage 1 (Archive Bounds & Paths): `MvaZipArchive.read()` enforces max 150 MiB total uncompressed size, max 32 MiB per entry, max 500 entries, and rejects path traversal (`..`, `/`, `\`).
   - Stage 2 (Manifest & Schema Check): Validates `manifest.json`. Checks `manifest.formatVersion <= 1`. Rejects unsupported future versions with clear update prompt.
   - Stage 3 (Passphrase & Key Derivation): Derives symmetric 256-bit key from passphrase and salt via PBKDF2-HMAC-SHA256 (`BackupCrypto.deriveKey`).
   - Stage 4 (Authenticated Payload Decryption): Decrypts `vault.enc` using AES-256-GCM with authenticated associated data (`MOTOVAULT-VAULT-v1:<createdAt>`). Throws `BackupSecurityException` if passphrase is incorrect or ciphertext/tag has been altered.
   - Stage 5 (Entity Deduplication & Integrity): Validates uniqueness of entity IDs in rules, ledger, and documents:
     - `require(payload.rules.map { it.id }.distinct().size == payload.rules.size)`
     - `require(payload.logs.map { it.id }.distinct().size == payload.logs.size)`
     - `require(payload.documents.map { it.id }.distinct().size == payload.documents.size)`
   - Stage 6 (Asset Decryption & SHA-256 Checksums): For every asset in the manifest, decrypts using AES-256-GCM (`MOTOVAULT-ASSET-v1:<assetId>`), checks exact byte length, and computes SHA-256 digest to verify byte-for-byte authenticity against the manifest.
   - Stage 7 (Referential Integrity): Verifies that all assets referenced across document pages are present and verified in the archive.

2. **Conflict Analysis (`BackupConflictSummary`)**:
   - Compares active `VaultState` against incoming backup payload:
     - Bike brand & display name match detection.
     - Active odometer vs incoming backup odometer.
     - Active log count vs incoming backup log count.
     - Active document count vs incoming backup document count.
   - Provides summary to `BackupPreview` for user review before destructive overwrite.

3. **Transactional Atomic Restore (`RestoreUseCase.executeRestore`)**:
   - Assets are written and protected in `ProtectedDocumentStorage` first.
   - Room database `VaultDatabase.vault().replace(restoredState, ...)` executes inside an atomic Room `@Transaction`. If any constraint fails, SQLite rolls back completely: previous vehicle, schedules, ledger, documents, and preferences remain 100% intact.
   - Initializes local sync protocol revision (`prepareLocalSync()`).
   - Reconciles renewal reminders (`reconcileRenewalReminders(restoredState.documents)`) immediately post-commit.
