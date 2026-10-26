# P02-01 threat model and recovery review

27 October 2026 (Asia/Dhaka). Source inspection at `cdc55e6` plus the working
contract in `docs/roadmap/P02-PRIVACY-CONTRACT.md`. No native runtime claim.

Inspected Android/iOS `ui/DocumentCapture.kt`, common `DocumentCapture.kt`,
`DocumentFlow.kt`, `storage/VaultDatabase.kt`, `ui/VaultSettings.kt`, native hosts,
and Android `res/xml/document_paths.xml`.

Findings: originals and previews are ordinary app-private files; Android opens
the original documents directory through FileProvider; iOS native image/PDF
readers use local paths; common display reads bytes; orphan recovery preserves
original files in a separate directory. Room 4 document/outbox JSON contains
sensitive fields and legacy photos. No existing app lock or local AEAD boundary
was found in these inspected paths. Keeping only common display encrypted would
miss native OCR/crop/PDF/viewer paths.

Reviewed shared-phone, copied-file, recents, export-cache and lost-key cases in
the contract table. Chosen platform AES-GCM and Keystore/Keychain primitives,
explicit metadata exclusions, stable versioned key identities, fail-closed lost
key behavior, staged migration and separate P03 portable secrets address P02-01.
No custom cryptography or unsupported whole-vault encryption claim is authorized.

Implementation review gates: all native readers and recovery paths must route
through the same storage boundary; authenticated failures must not fall back to
legacy bytes; missing-key creation must differ from temporarily inaccessible keys;
plaintext retention must be reported. User-visible protection starts only after
these implementation and migration checks pass.

Device inventory during this run: `adb devices -l` returned no attached Android
devices. `xcrun devicectl list devices` listed only shutdown simulated iPhones.
This is not physical-device verification; P02-05 remains required.
