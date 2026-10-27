# P03 Backup and recovery contract

Decision: 27 October 2026, Asia/Dhaka.
Inspected base: `04bfaf5`, Room 5, ProtectedDocumentStorage (format 1, AES-256-GCM).
This is the implementation contract for Phase P03: Encrypted local backup and complete restore.

## Scope, Threat Model and Principles

MotoVault is an offline-first motorcycle ownership and vault app. The local backup archive
allows riders to create a portable, complete, and self-contained backup of their motorcycle
data and protected documents, and restore it on the same or another device (Android or iOS)
without relying on any external cloud server or third-party intermediary.

### Core Safeguards and Non-Negotiable Rules

1. **Hardware/Device Keys Never Exported**:
   Device-bound encryption keys residing in AndroidKeyStore or iOS Keychain (used by P02's
   `ProtectedDocumentStorage`) are non-exportable and bound to the physical hardware/device.
   They must NEVER be included in any backup archive.

2. **Owner Passphrase / Recovery Secret**:
   The backup archive is encrypted end-to-end using an authenticated key derived from an
   owner-provided passphrase or recovery key using PBKDF2-HMAC-SHA256 (minimum 100,000 iterations,
   256-bit cryptographically secure random salt, 256-bit key output).

3. **Complete Asset and Record Bundle**:
   The backup contains:
   - Authenticated `manifest.json` (format version, timestamp, app build revision, installation ID,
     KDF parameters, cipher parameters, and asset inventory with SHA-256 digests).
   - Encrypted snapshot of all database records: `Ride` (vehicle), `ComponentRule`s (schedules),
     `RideLog`s (service/fuel ledger), `VaultDocument`s (metadata), preferences and settings.
   - All referenced non-regenerable document originals (front, back, PDF pages).
   Assets are stored in plaintext inside the archive payload ONLY AFTER the entire archive payload
   is authenticated and encrypted, or stored as authenticated ciphertext entries within an encrypted
   container. To maximize cross-platform interoperability, streaming efficiency, and memory safety,
   the archive format is a standard Zip container with an authenticated envelope or an AEAD container
   holding the encrypted payload bundle.

4. **Fail-Closed, Non-Destructive Restore**:
   Restoring an archive is strictly staged and transactional:
   - Stage 1: Unpack into an isolated sandbox staging directory.
   - Stage 2: Validate manifest schema, supported format versions, and bounds (file count, total size,
     individual asset size). Reject path traversal (`..`, absolute paths, symlinks).
   - Stage 3: Decrypt with provided passphrase. Any MAC/GCM tag failure aborts immediately with zero
     changes to the active database.
   - Stage 4: Verify SHA-256 digests of every extracted asset against the manifest.
   - Stage 5: Preview summary to the owner (bike name, odometer, document count, log count, date).
   - Stage 6: Upon explicit destructive confirmation, perform atomic database replace and copy
     assets into `documents-protected`. If any step fails, roll back completely: the previous vault
     remains 100% untouched.

5. **Reconciliation After Restore**:
   Following successful restore, renewal reminders (`reconcileRenewalReminders`) are immediately
   scheduled according to restored document expiry dates, and the local sync protocol outbox
   is reset/reinitialized with a fresh revision.

---

## Threat Model and Boundaries

| Threat | Required Mitigation | Operational Boundary |
| --- | --- | --- |
| Backup file intercepted or stolen | PBKDF2-HMAC-SHA256 (100k iter) + AES-256-GCM. Unauthenticated reads are cryptographically impossible without the passphrase. | Passphrase strength is user-selected; enforce minimum 8 characters and provide strength feedback. |
| Corrupted or tampered archive | AEAD tag on encrypted database payload + SHA-256 checksums on all asset files. Any bit-flip fails validation. | Tampered archives fail closed during preflight before database touches. |
| Zip bomb / archive decompression attack | Strict pre-extraction inspection: max 100 MB total uncompressed, max 32 MiB per asset, max 500 files. | Extraction aborts if uncompressed stream exceeds limits. |
| Path traversal attack (`../../`) | Sanitize entry names. Only alphanumeric, hyphen, underscore, and dot allowed in file names. Disallow parent directories (`..`) or leading slashes. | Files extracted only to isolated staging directory. |
| Wrong passphrase entered | PBKDF2 derivation followed by AES-256-GCM open. Fails with `AuthenticationFailedException` without crashing or corrupting data. | Rate-limit UI retries and give clear error feedback in natural Bangla. |
| Interrupted export (crash / out of disk) | Export writes to a temporary staging file (`backup.tmp`). It is renamed/finalized only after complete flush and verification. | Incomplete backup file is never registered or handed to system share. Last successful backup timestamp is NOT updated on cancellation/error. |
| Interrupted restore (crash / battery pull) | Active database and protected document store are modified in a transaction or staged atomic swap. Previous database remains intact if restore aborts before commit. | App reopens previous vault unharmed. |
| Cross-platform schema mismatch | Manifest specifies format version (1) and Room database schema version (5). Reader supports migration from earlier schemas. | Rejects newer unsupported formats gracefully with "update app" guidance. |

---

## Archive Format Specification (MVA-ZIP v1)

The portable backup file uses the `.mva` (MotoVault Archive) extension (MIME type: `application/octet-stream` or `application/zip`).
Internally, the archive uses standard Zip structure containing:

```text
motovault-backup.mva (ZIP container)
├── manifest.json            [Plaintext JSON header describing envelope and parameters]
├── vault.enc                [AES-256-GCM encrypted payload: contains full database snapshot JSON]
└── assets/                  [Encrypted asset files or directory of authenticated asset binaries]
    ├── doc_101_front.bin    [AES-256-GCM payload for asset doc_101_front]
    ├── doc_101_back.bin     [AES-256-GCM payload for asset doc_101_back]
    └── ...
```

### 1. `manifest.json` Schema

```json
{
  "formatVersion": 1,
  "appVersion": "1.0.0",
  "buildRevision": "04bfaf5",
  "createdAtEpochMillis": 1729999000000,
  "installationId": "c3d9a1e0-7f22-4a88-8b91-112233445566",
  "vaultRevision": 14,
  "kdf": {
    "algorithm": "PBKDF2-HMAC-SHA256",
    "iterations": 100000,
    "saltHex": "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"
  },
  "cipher": {
    "algorithm": "AES-256-GCM",
    "tagLengthBits": 128
  },
  "summary": {
    "bikeDisplayName": "Yamaha MT-15 V2",
    "odometer": 14250.0,
    "logCount": 24,
    "documentCount": 3,
    "totalAssetBytes": 4210980
  },
  "assets": [
    {
      "assetId": "doc_101_front",
      "path": "assets/doc_101_front.bin",
      "byteLength": 2105490,
      "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "nonceHex": "0102030405060708090a0b0c"
    }
  ]
}
```

### 2. Encryption Details

- **Key Derivation**:
  - `masterKey = PBKDF2(passphrase.encodeToByteArray(), salt, iterations = 100_000, keyLength = 32 bytes, PRF = HMAC-SHA256)`
- **Vault Database Payload (`vault.enc`)**:
  - Plaintext: Serialized `VaultExportPayload` JSON containing:
    - `ride: Ride?`
    - `schedules: List<ComponentRule>`
    - `ledger: List<RideLog>`
    - `documents: List<VaultDocument>`
    - `preferences: VaultSettings`
    - `appearance: String`
    - `autoLocation: Boolean`
  - Nonce: 12 cryptographically random bytes.
  - Authenticated Associated Data (AAD): `"MOTOVAULT-VAULT-v1".encodeToByteArray() + manifest.createdAtEpochMillis`
  - Combined output: `nonce (12 bytes) + ciphertext + tag (16 bytes)`.
- **Assets (`assets/<assetId>.bin`)**:
  - Plaintext: Original raw image / PDF bytes.
  - Nonce: 12 cryptographically random bytes.
  - AAD: `"MOTOVAULT-ASSET-v1:".encodeToByteArray() + assetId.encodeToByteArray()`
  - Combined output: `nonce (12 bytes) + ciphertext + tag (16 bytes)`.

### 3. Size and Resource Limits

| Resource | Maximum Limit | Rationale |
| --- | --- | --- |
| Max Archive Uncompressed Size | 150 MiB | Prevents memory exhaustion on mobile devices. |
| Max Individual Asset Size | 32 MiB | Matches `MAX_PRIVATE_ASSET_BYTES` in `SecureAssetStore.kt`. |
| Max Asset Count | 100 assets | MotoVault handles motorcycle documents; 100 assets is generous while bounded. |
| Max Document Count | 50 documents | Bounded room entity count. |
| Max Ledger Logs | 5,000 logs | Bounded room entity count. |
| Max File Name Length | 128 characters | Prevents OS filesystem buffer overflows. |

---

## Compatibility and Future Evolution

1. **Schema Migration from Legacy / Earlier Versions**:
   - The restore engine maps deserialized `VaultExportPayload` into the current Room database schema.
   - Default values are applied if fields were added in later app releases (e.g. `serviceLines`, `serviceLabourPoisha`, `autoLocation`).
2. **Future Multi-Bike Support (Phase P09)**:
   - In P09, multiple bikes will be introduced. `VaultExportPayload` contains `bikes: List<Ride>` or a primary `ride`. If a future multi-bike archive is imported into a single-bike version, the primary bike is restored with a notification. Conversely, single-bike archives restore cleanly into multi-bike systems as the primary/default bike.
3. **Format Version Gate**:
   - `manifest.json` contains `formatVersion`. If `formatVersion > 1`, MotoVault aborts restore and guides the user to update the app.
   - If `formatVersion < 1` or corrupt, the archive is rejected as invalid.

---

## User Interaction Contract (V3 Compliance)

1. **Export Flow**:
   - Entry point: `MeScreen` -> "ব্যাকআপ ও রিস্টোর" (Backup & Restore) card -> tap "লোকাল ব্যাকআপ তৈরি করুন" (Create local backup).
   - Dialog/Sheet: Prompts for a backup password (minimum 8 characters) with confirmation field and clear warning:
     "এই পাসওয়ার্ডটি মনে রাখুন। পাসওয়ার্ড ভুলে গেলে ব্যাকআপ রিস্টোর করা সম্ভব হবে না।" (Remember this password. If forgotten, backup cannot be restored.)
   - Progress: Shows genuine deterministic progress (reading assets, encrypting, packing).
   - Completion: Triggers native share sheet / file save dialog (`Intent.ACTION_CREATE_DOCUMENT` on Android, `UIActivityViewController` / `UIDocumentPickerViewController` on iOS).
   - Last Backup Date: Persisted in `VaultSettings.lastBackupEpochMillis` ONLY upon verified completion of file save/share.
2. **Restore Flow**:
   - Entry point: `MeScreen` -> "ব্যাকআপ ফাইল থেকে রিস্টোর করুন" (Restore from backup file).
   - File Picker: Native document picker to select `.mva` or `.zip` file.
   - Password prompt: User enters passphrase.
   - Validation & Preview: Displays summary sheet:
     - Bike: Name & Model
     - Last Odometer: e.g., 14,250 km
     - Records: X service logs, Y documents
     - Backup Date: Formatted date
   - Destructive Warning:
     "বর্তমান ডেটা প্রতিস্থাপিত হবে। আপনি কি নিশ্চিত?" (Current data will be replaced. Are you sure?)
   - Atomic Restore: Commits to Room, updates `ProtectedDocumentStorage`, refreshes `activeVaultSession`, reconciles reminders.
   - Success Toast: "ডাটা সফলভাবে রিস্টোর করা হয়েছে" (Data restored successfully).
