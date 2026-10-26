# P02-02 integrated storage evidence

27 October 2026 (Asia/Dhaka). Build base: `cdc55e6` plus the uncommitted P02
source delta. Status: **in progress**, not a completed privacy phase.

## Implemented behavior

New imported originals, rendered PDF pages, accepted crops and previews use
`ProtectedDocumentStorage`. Common display, Android bitmap/EXIF/OCR, iOS
image/OCR/crop and original-viewer paths read through the same protected boundary.
Android camera capture migrates its initial camera output before returning the
captured page. iOS PDF decoding uses an in-memory data provider. Android's
seekable PDF scratch file is unlinked immediately after opening its descriptor
and cleaned on failure.

An explicit `protectDocumentAssets` operation migrates managed legacy files,
referenced originals and recognized recovery files. Logical asset IDs stay stable.
Encrypted publication and committed markers are synced before retiring a
byte-identical plaintext duplicate. A retry authenticates the existing ciphertext;
it does not generate replacement ciphertext or keys on authentication failure.
Original bytes remain preserved in encrypted form. A missing committed ciphertext
fails closed even if a plaintext duplicate exists. Recovery restoration creates a
verified protected managed copy without overwriting an existing ID and retains
the protected recovery original.

Original viewers create bounded private cache copies. Android FileProvider now
exposes only the export cache for documents, rather than the original document
directory. Return/cancel/disposal handlers remove copies and revoke Android grants;
process startup clears operation-owned leftovers. Native viewer lifecycle acceptance
is still pending; implementation is not proof that every OS return path was observed.

Room/outbox payload metadata and legacy embedded Base64 photos remain outside this
file-encryption boundary. An app lock is not database encryption. The owner-facing
migration/coverage and recovery help still need implementation in P02-03.

## Observed verification

| Evidence | Observed result | Scope |
| --- | --- | --- |
| `p02/storage/TEST-SecureAssetStoreTest.xml` | 11 tests, zero failures/errors | JVM/JCE; injected disk-full, interruption and retirement failure; tamper/ID binding, lost/wrong keys, no fallback, original bytes, retry and interoperability |
| `p02/storage/android-native-result.txt` | 8 native checks passed | Physical Motorola edge 50 fusion, Android 16; isolated `.privacyprobe` app |
| `p02/storage/ios-native-result.txt` | 8 native checks passed | iOS 27 iPhone 18 Pro Max simulator; isolated `.privacyprobe` bundle |
| Kotlin iOS Arm64 compilation | Passed | Compile evidence only |
| Xcode Debug arm64 simulator build | Passed | Native Swift/CryptoKit bridge and simulator Keychain entitlements |

Native checks use actual platform keys and private file staging/fsync, interrupted
verification cleanup, encrypted original migration/read/retry, tamper refusal,
synthetic-key deletion, image/crop readers, legacy/recovery migration with collision
refusal, and PDF import. Crop checks compare metadata with decoded saved-image
dimensions. No owner database was seeded, cleared or migrated by these probes.
The existing primary Android app and simulator app were not replaced.

`p02/storage/android-source-manifest.json` and `ios-source-manifest.json` retain
source and tested-artifact SHA-256 fingerprints. They describe these working-tree
builds, not a signed release. `tools/security/evidence_manifest.py` regenerates a
fingerprint only after a corresponding build/test run; a changed hash requires
fresh relevant verification before reusing an earlier result.

## Defects caught during native verification

1. Android app-private paths can have a trusted OS alias. Using the raw root with
   strict canonical-path comparisons rejected valid files. The factory now
   canonicalizes the trusted app root and still rejects redirected asset paths.
2. iOS crop metadata captured outer `width`/`height` locals rather than the cropped
   image receiver. Explicit receiver fields now match the saved image bytes.
3. Unsigned simulator builds lack Keychain entitlements. The Xcode target now has
   a bundle-relative entitlement source; native tests use Xcode-managed ad hoc
   simulator signing. This does not establish physical provisioning or publication.

## Remaining acceptance work

- Persist/verify an original SHA-256 migration inventory and expose precise
  coverage, including retained legacy embedded images/unrecognized files.
- Integrate the owner-triggered migration and retry/recovery UI after the v3
  privacy prototype states are extended.
- Verify temporary export cleanup through actual native viewer/share success,
  cancellation, process death and denied/unavailable cases. Current probes verify
  PDF processing cleanup; full native viewer behavior remains open.
- Complete P02-03 authentication, masking, lock/background/timeout and selected-
  document isolation, then P02-04 deliberate export/import boundaries.
- Complete P02-05 on both physical devices. Only the Android phone is currently
  available; the iPhone has not appeared in native device discovery.
