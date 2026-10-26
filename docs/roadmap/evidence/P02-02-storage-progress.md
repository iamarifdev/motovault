# P02-02 storage implementation progress

27 October 2026 (Asia/Dhaka). Inspected/build base `cdc55e6` plus uncommitted
P02 storage sources and the Swift host bridge. Status: **in progress**.

This is the initial adapter checkpoint. Later integration results are recorded in
`P02-02-integrated-storage.md`; the initial limitation list below is historical.

## Implemented boundary

- Common `storage/security/AssetEnvelope` uses a five-byte format/key-version
  header and binds the immutable asset ID and header as AES-GCM associated data.
  Nonce/ciphertext/tag layout is interoperable across JCE and CryptoKit.
- `SecureAssetStore` validates sizes/IDs, verifies staged ciphertext against the
  original bytes before publication, retries already published migrations without
  replacing ciphertext, and fails closed on tampering/key errors. New writes
  cannot shadow an existing original or encrypted ID.
- Android AES-256-GCM uses provider-generated random IVs and AndroidKeyStore.
  iOS host uses CryptoKit with a non-synchronizing, device-bound unlocked Keychain
  item. Existing-key lookup never creates a replacement. Fresh-store provisioning
  refuses nonempty protected storage when the old key is missing.
- Native file adapters bound reads, stage ciphertext only, verify it, publish
  atomically, sync publication, and create committed markers. A committed marker
  with missing ciphertext must fail rather than fall back to legacy plaintext.
  Failed staging removes only the operation's own staging file.
- Originals are retained. No automatic migration, plaintext retirement or
  application-wide encryption claim is enabled by this work.

## Observed verification

Commands ran through `rtk` with native JDK 17 for Gradle:

| Check | Observed result | Limit |
| --- | --- | --- |
| `:composeApp:testDebugUnitTest --tests com.motovault.app.storage.security.SecureAssetStoreTest` | 9 tests, 0 failures/errors | JVM/JCE, not AndroidKeyStore hardware |
| `:composeApp:compileKotlinIosArm64` | Passed during initial storage adapter build | Kotlin compile, not physical execution |
| `:composeApp:linkDebugFrameworkIosSimulatorArm64` | Passed | Framework link, not UI/runtime |
| Xcode Debug simulator host build, signing disabled | `BUILD SUCCEEDED` | Swift bridge compiles, not Keychain/runtime proof |
| `swift -module-cache-path /tmp/motovault-p02-swift-cache tools/security/asset-vector.swift` | CryptoKit vector passed | Host macOS CryptoKit; shared synthetic vector also opened in JVM test |

The XML is retained at `p02/storage/TEST-SecureAssetStoreTest.xml`. The fixture
key/nonce in `tools/security/asset-vector.swift` and the matching unit test are
public synthetic test material, never used by production encryption.

Tests cover every altered envelope byte, renamed asset IDs, truncated headers,
missing/wrong keys, no plaintext fallback on authentication errors, interrupted
publication and disk-full fault injection, no-overwrite behavior, random nonce
output, idempotent migration and original retention. Disk-full/interruption tests
use an injected file backend. Actual native filesystem and key-invalidation
behavior remain unverified.

## Remaining P02-02 work

1. Route every native image/PDF/OCR/camera/viewer and common display read/write
   through the boundary. Current app document operations still use their existing
   plaintext paths; installing the Swift provider does not activate encryption.
2. Integrate a durable inventory/migration operation with Room owner references,
   recovery files, legacy embedded photos, failure/retry UI and explicit coverage.
   Original retirement must preserve immutable bytes and respect recovery/Undo.
3. Exercise actual platform staging, disk-full/interruption, missing/inaccessible
   keys and native temporary-file cleanup, including restart/recovery scenarios.

P02-03 lock/handover, P02-04 export protection and P02-05 physical-device evidence
remain planned. No protection should be represented as complete from these tests.
