# P02 privacy and recovery contract

Decision: 27 October 2026, Asia/Dhaka. Inspected base: `cdc55e6`, Room 4.
This is the implementation contract, not a claim that these protections ship.

## Coverage and threats

Protect originals, accepted crops and previews with authenticated encryption.
Treat recovered originals and legacy embedded photos as sensitive too. Room JSON,
its WAL, legacy JSON/preferences, outbox payloads, owner names, document numbers,
expiry dates and asset references remain plaintext unless separately migrated.
The UI must say “document file encryption”, never “encrypted vault/database”.
OS sandbox/file protection is additional protection, not application encryption.

| Threat | Required boundary | Limit |
| --- | --- | --- |
| Shared unlocked phone | System credential/biometric unlock, masked summaries, background lock and bounded session | A successful unlock gives the owner session access |
| Copied document directory | AES-256-GCM with random nonce and device-protected key | Plaintext legacy/recovery files remain until a verified migration and approved retention retirement |
| Recents/background | Native opaque cover before inactive snapshot; Android secure windows including dialogs | iOS cannot promise prevention of owner screenshots; external cameras are outside scope |
| Handover | Only a frozen chosen document, no edit/export/list links; exit/back requires fresh authentication | Not OS kiosk mode; recipient can leave the app |
| Export/viewer | Fresh owner authorization, narrowly scoped temporary copy, revoke grants and remove on completion/cancel/startup | Receiving apps can retain an explicitly shared copy |
| Invalid/missing key | Fail closed, retain ciphertext and records, explain recovery | System authentication cannot recreate a lost encryption key |
| Compromised/rooted device | No protection claim | Attacker controlling a running authorized process can read its memory |

## Platform choices and lifecycle

Android uses platform `Cipher` AES/GCM/NoPadding and a 256-bit AndroidKeyStore key.
Let the provider generate a fresh 96-bit IV for each encryption. Never supply a
fixed IV. Use 128-bit authentication tags. Never export the key. Check key
existence separately from creating it: opening existing encrypted data with a
missing key must never generate a replacement. A stable versioned alias survives
ordinary app updates. Key deletion, app data reset, uninstall or credential
invalidation can make data unrecoverable. Do not assume hardware backing.

iOS uses CryptoKit AES.GCM and a random 256-bit key in Keychain, with
`kSecAttrAccessibleWhenUnlockedThisDeviceOnly` and synchronizable disabled.
This permits a separate LocalAuthentication device-owner policy with passcode
fallback and avoids tying all existing files to the current biometric enrollment.
Keychain lookup failures are not “not found”: locked/error states never create a
replacement. Retain the same service/account across upgrades. Reinstallation and
device restore are not supported recovery paths for device-bound ciphertext.
Do not claim the symmetric key is stored inside Secure Enclave.

System owner authentication uses Android credentials/biometrics and iOS
`deviceOwnerAuthentication`, with explicit cancellation, unavailable/no credential,
lockout and retry states. An app password is not introduced. Session permission
is separate from key persistence. A background transition locks immediately;
native auth transitions need a bounded pending request so stale callbacks cannot
unlock a later session. Cold launch always starts locked when enabled. No session
or selected-document permission is serialized into saved UI state.

## Versioned file and migration protocol

The envelope authenticates format version, key version and immutable asset ID as
associated data. An asset renamed to another ID must fail authentication.
Store ciphertext in a separate private directory, leaving the legacy original
untouched during staging. Immutable original bytes remain available in encrypted
form after verified publication; “preserve originals” does not require keeping
an unprotected duplicate indefinitely. Bounds include envelope overhead above the existing
32 MiB plaintext limit. Reject unknown versions, short headers, invalid IV/tag,
empty/oversized payloads and malformed IDs before publishing bytes.

1. Inventory current, Undo and recovery references. Never silently skip a missing
   referenced original. Preflight available space and readability.
2. Encrypt to a unique staged file; flush; read back, authenticate, and compare
   decrypted bytes/hash with the immutable source. No plaintext staging file.
3. Atomically publish verified ciphertext with an explicit durable per-asset
   committed marker. Logical IDs in Room remain stable; all readers understand
   both unmigrated files and protected files during an interrupted batch. Report
   full coverage only after every required asset verifies. Retry verifies existing
   results rather than reusing a nonce.
4. A crash, disk-full error, cancellation or invalid key retains original references
   and original bytes. Never fall back to plaintext after an authentication failure
   on an already committed encrypted asset.
5. Reopen, verify and retry to prove idempotence. Retire each plaintext duplicate
   only after its durable protected copy verifies byte-for-byte. Keep encrypted
   original IDs stable for Undo and pending backup references. Recovery originals
   use their own protected directory; restoration never overwrites managed files
   and retains the encrypted recovery copy. Retained legacy embedded images or
   unrecognized files must remain visible in the coverage report.

Portable P03 backup uses an independently derived recovery secret and its own
reviewed interoperable envelope. Export decrypts only after owner authorization
then encrypts directly into the portable archive. Never export device keys or
claim that copying local ciphertext restores it on another phone. P03 freezes
KDF/parameters and cross-platform vectors before shipping; P02 does not invent
a password KDF or custom cipher.

## Verification gates

Use synthetic originals only. Test changed ciphertext/header/ID, missing key,
temporarily inaccessible key, invalidation, interrupted staging/publish/reference
commit, disk full, retry and downgrade rejection. Verify byte-identical originals,
including legacy/recovery cases. Inspect cache and provider roots after native
share success/cancel/crash. No raw error/owner values in logs or evidence.

Walk through light/dark/system, large type, reduced motion, accessibility,
credential cancellation/lockout, no biometrics, deep links, back, rotation,
background/resume, timeout and handover escape. Physical Android and iPhone
credential changes and native snapshots remain mandatory P02-05 gates.

## Primary references reviewed

- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Android cryptography](https://developer.android.com/privacy-and-security/cryptography)
- [CryptoKit AES.GCM](https://developer.apple.com/documentation/cryptokit/aes/gcm)
- [Keychain accessibility](https://developer.apple.com/documentation/security/restricting-keychain-item-accessibility)
- [LocalAuthentication owner policy](https://developer.apple.com/documentation/localauthentication/lapolicy/deviceownerauthentication)
