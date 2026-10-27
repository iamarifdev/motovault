# P03-02: Consistent Snapshot Export and Native File Save Implementation

- Task and acceptance criteria covered: P03-02 (Freeze committed revision, include every referenced non-regenerable asset, encrypt before handing to native save/share, cancellation does not update last-successful-backup).
- Date/time and timezone: 2026-10-27 05:01:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Engineering Implementation.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Kotlin Multiplatform (Android 14/15, iOS 18/27).
- Evidence kind: implementation.

## Implementation Details

1. **Portable Archive & Metadata (`BackupModels.kt`, `MvaZipArchive.kt`)**:
   - Implemented standard STORED uncompressed ZIP reader and writer in pure Kotlin (`MvaZipArchive`), eliminating third-party zip library dependencies while maintaining standard ZIP compatibility across JVM, Android, and iOS.
   - Enforced security bounds: max 500 entries, max 32 MiB per entry (`MAX_ENTRY_BYTES`), max 150 MiB total uncompressed archive size (`DEFAULT_MAX_TOTAL_BYTES`).
   - Strictly validates entry names against directory traversal: rejects leading slashes (`/`, `\`) and path traversal sequences (`..`).

2. **Cryptographic Engine (`BackupCrypto.kt`, Android & iOS implementations)**:
   - Secret Key Derivation: Standard PBKDF2-HMAC-SHA256 with 100,000 iterations and a 32-byte cryptographically secure random salt producing a 256-bit symmetric key.
   - Symmetrical Encryption: AES-256-GCM with a fresh 12-byte random nonce and 128-bit authentication tag.
   - Android implementation: `javax.crypto.Cipher` (AES/GCM/NoPadding) and `javax.crypto.SecretKeyFactory` (PBKDF2WithHmacSHA256).
   - iOS implementation: Swift `CryptoKit` (`AES.GCM`) and Apple `CommonCrypto` (`CCKeyDerivationPBKDF`) bridged through `IosBackupCryptoBridge`.

3. **Snapshot Export Engine (`BackupUseCase.kt`)**:
   - `createBackup()` freezes an immutable `VaultExportPayload` snapshot of all entities (`Ride`, `ComponentRule`s, `RideLog`s, `VaultDocument`s, preferences, appearance, autoLocation).
   - Resolves all referenced non-regenerable document assets (`VaultDocument.assetIds()`) through `assetReader`. If any asset is missing or unreadable, throws `IllegalStateException` and aborts export without generating an incomplete archive.
   - Computes SHA-256 digests for each asset.
   - Encrypts each asset using AES-256-GCM with individual nonces and Authenticated Associated Data (`MOTOVAULT-ASSET-v1:<id>`).
   - Encrypts the entire database payload using AES-256-GCM with AAD (`MOTOVAULT-VAULT-v1:<timestamp>`).
   - Bundles `manifest.json`, `vault.enc`, and `assets/<assetId>.bin` into `.mva`.

4. **Native Save and Share Integration (`BackupNativeActions.kt`)**:
   - Android (`BackupNativeActions.android.kt`):
     - Uses `ActivityResultContracts.CreateDocument("application/octet-stream")` to write bytes directly to user-selected storage (Downloads, external SD, Google Drive).
     - Cancellation handling: when user cancels the picker (null Uri), `onSaved()` is NOT called, preserving the contract that cancellation never updates `lastSuccessfulBackupEpochMillis`.
     - Uses `Intent.ACTION_SEND` via Android `FileProvider` (`document-exports/`) to share to external apps.
   - iOS (`BackupNativeActions.ios.kt`):
     - Uses `UIActivityViewController` to support Save to Files, AirDrop, and system share sheet.
     - Updates `lastSuccessfulBackupEpochMillis` only upon completed action.
