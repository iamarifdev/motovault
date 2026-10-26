# P02-02 storage adapter acceptance

27 October 2026 (Asia/Dhaka). Base `cdc55e6` plus uncommitted P02 source delta.
Scope: storage/key adapters and migration API. This does not complete P02-03,
P02-04 or P02-05 and makes no signed-release/publication claim.

## Requirement audit

| Requirement | Implementation and inspected proof | Result |
| --- | --- | --- |
| Authenticated encryption, versioned metadata, protected local keys | `AssetEnvelope`: AES-256-GCM via AndroidKeyStore/JCE and Swift CryptoKit/Keychain; authenticated file ID and version; separate authenticated inventory purpose | Met |
| Original hash inventory | Encrypted `.committed` receipt contains original SHA-256, byte count, immutable ID, format and key versions. Inventory ciphertext cannot substitute for asset ciphertext. Failed inventory commit retains plaintext; retry verifies/rebuilds derived inventory from authenticated original bytes | Met |
| Staging, verification, recovery and original preservation | Source bytes remain until staged ciphertext, committed inventory and durable publication verify. Reads retain stable logical IDs. Repeated migration revalidates protected main/recovery inventory; recovery never overwrites a managed ID | Met |
| Interrupted migration and disk-full cases | JVM injected staging/retirement/inventory failures, native interrupted-verification cleanup, actual free-space preflight plus injected zero-space refusal. No phone was deliberately filled | Met within stated fault-injection scope |
| Altered ciphertext and missing/invalid keys | Every changed envelope byte/ID is rejected in JVM tests. Native ciphertext tampering and UUID-scoped key deletion fail closed without replacing the missing key or returning retained plaintext | Met |
| No plaintext temporary export leaks in storage lifecycle | Native helper tests create byte-identical export copies, close twice, verify deletion and invoke the actual startup cleanup for interrupted-copy leftovers. Native PDF processing also leaves no plaintext pathname/copy | Met for storage helpers; actual OS viewer/share flows remain P02-04 |
| Persisted-contract compatibility | Room 5 migration adds a supported asset-format declaration; older schemas 1-4 migrate explicitly. Real Room 4-to-5 tests preserve synthetic owner rows, references, preferences and outbox, survive reopen, and refuse unsupported asset/schema versions without replacing rows | Met for source/Room runtime scope |

The historical schema-4 application binary was not installed over a migrated
owner vault. Source inspection shows its Room builder requires supported versions
and has no destructive fallback. Runtime refusal was exercised against a future
schema using the same current Room boundary; historical-binary rollback remains
separate release evidence. Do not manually reset a real database's version.

## Observed checks

- Physical Motorola edge 50 fusion, Android 16: **15 native checks passed** in
  `com.motovault.app.privacyprobe`, a separate app/test sandbox.
- Existing iOS 27 iPhone 18 Pro Max simulator: **15 native checks passed** in
  the separate `.privacyprobe` bundle. Fresh result run ID:
  `P02-storage-schema-acceptance-1`.
- Android unit suite: **162 tests, 0 failures/errors**; 14 target storage tests.
- iOS simulator regression suite: **170 tests, 0 failures/errors**; 12 actual
  Room database/migration tests. The runner was pinned through
  `tools/baseline/isolated-ios.gradle` to the verified iPhone 18 Pro Max.
- Xcode Debug arm64 simulator build passed using Xcode-managed ad hoc signing
  and `MotoVault.entitlements`. This is not physical-device provisioning.

Current result XML, native result records and source/artifact fingerprints are
under `p02/storage/acceptance/`. Earlier adapter/integration checkpoint reports
remain historical snapshots and do not override this acceptance scope.

## Coverage and remaining phase work

Managed original imports, accepted crops, previews and recognized recovery files
have the protected file boundary. Room/outbox payload metadata and old embedded
Base64 images remain outside file encryption and are retained. Unsupported or
unrecognized files are not silently erased. No whole-database encryption claim
is made. P02-03 must expose this precise coverage and the lost-key limitation in
owner-facing help and migration controls.

Real OS credential/biometric behavior, lockout/background/timeout, selected-
document isolation, native viewer/share return/cancel and the physical iPhone
matrix remain required in their respective tasks. The native export tests prove
storage lifetime helpers; they do not claim that all external viewer applications
honor lifecycle callbacks or forget explicitly shared copies.
