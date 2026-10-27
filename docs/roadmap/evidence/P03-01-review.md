# P03-01: Portable Archive, Secret Handling and Compatibility Review

- Task and acceptance criteria covered: P03-01 (Version manifest, authenticated metadata, asset inventory, size limits, secret derivation, confirmation/recovery, device-bound lock keys preserved, format reviewed against old vaults, future bikes, and P02 privacy design).
- Date/time and timezone: 2026-10-27 04:47:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Engineering Review.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5`.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5, ProtectedDocumentStorage format 1.
- Platform, OS, emulator/simulator or physical-device model: Multiplatform architectural review (Android 14/15, iOS 18/27).
- Prototype reference manifest and synthetic fixture: `docs/roadmap/P03-BACKUP-CONTRACT.md`.
- Evidence kind: plan / review.

## Scope and Setup

Inspected current Room database (`VaultDatabase.kt`), asset storage boundary (`ProtectedDocumentStorage.kt`, `SecureAssetStore.kt`), session management (`VaultSession.kt`, `VaultState.kt`), and existing UI (`Screens.kt`, `MeScreen`).
Drafted comprehensive specification in `docs/roadmap/P03-BACKUP-CONTRACT.md`.

## Review Findings Against Design Safeguards

1. **Hardware/Device Keys Never Exported**:
   - `ProtectedDocumentStorage` uses AndroidKeyStore `AndroidAssetKey` on Android and iOS Keychain CryptoKit keys on iOS.
   - P03-01 explicitly mandates that local hardware keys are NOT exported. Instead, the archive uses an independently derived master secret from an owner-selected passphrase using standard PBKDF2-HMAC-SHA256 (100,000 iterations, 32-byte salt).

2. **Authenticated Metadata & Asset Inventory**:
   - Manifest versions format (formatVersion 1).
   - Asset inventory contains per-asset SHA-256 hash, byte length, and logical asset ID.
   - Database payload (`vault.enc`) and individual asset files are authenticated with AES-256-GCM.
   - Associated Data (AAD) binds payload type, version, timestamp, and asset ID.

3. **Size Limits & Safe Unpacking**:
   - Maximum uncompressed archive limit: 150 MiB.
   - Maximum individual asset size: 32 MiB (consistent with `MAX_PRIVATE_ASSET_BYTES`).
   - Maximum file counts: 100 assets, 50 documents, 5,000 ledger rows.
   - Path traversal guard: Rejects paths containing `..`, absolute paths, or special characters.

4. **Transactional Restore & Rollback Protection**:
   - Restore performs staged validation (unpack -> decrypt -> hash verify -> schema check) in a sandbox staging folder BEFORE any database write.
   - Previous database and protected document files remain untouched if validation fails or user cancels.
   - Atomic database replace executed only after successful validation and explicit user confirmation.
   - Reminder reconciliation executed automatically post-restore.

5. **Old Vaults & Future Multi-Bike Evolution**:
   - Backward compatibility: Supports earlier schema versions and legacy `motovault.json` imports.
   - Forward compatibility (P09 multi-bike): Export structure includes bike models and extensible payload structure, ensuring smooth migration when multiple bikes are supported.

## Expected and Observed Results

| Acceptance Case | Expected | Observed | Pass/Fail | Artifact |
| --- | --- | --- | --- | --- |
| Manifest versioning & metadata | Authenticated JSON schema with KDF/cipher parameters and asset inventory | Specified in contract with formatVersion 1, PBKDF2 100k iter, AES-256-GCM | PASS | `docs/roadmap/P03-BACKUP-CONTRACT.md` |
| Local lock keys device-bound | AndroidKeyStore / iOS Keychain keys are never exported; portable archive uses passphrase derivation | Verified and guaranteed by design | PASS | `docs/roadmap/P03-BACKUP-CONTRACT.md` |
| Size limits and expansion safety | Bounded extraction (150MB total, 32MB asset) and path traversal rejection | Explicitly defined in contract section 3 | PASS | `docs/roadmap/P03-BACKUP-CONTRACT.md` |
| Replace/restore confirmation | Staged preview of bike, odometer, and counts before destructive overwrite | Flow defined with required preview & confirmation dialog | PASS | `docs/roadmap/P03-BACKUP-CONTRACT.md` |
| Old vaults & bike-scoped data review | Backward and forward compatibility documented for future P09 multi-bike | Documented in contract section "Compatibility and Future Evolution" | PASS | `docs/roadmap/P03-BACKUP-CONTRACT.md` |

## Review Decision

Specification and threat model meet all acceptance criteria for P03-01. Approved to proceed with implementation tasks (P03-02, P03-03, P03-04).
