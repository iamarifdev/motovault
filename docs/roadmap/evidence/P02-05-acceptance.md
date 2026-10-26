# P02-05 privacy and key recovery device acceptance

27 October 2026 (Asia/Dhaka). Base `cdc55e6` plus uncommitted P02 delta.
Status: **Physical Android verified (15/15 native checks PASS); iOS Simulator verified (15 native checks PASS, 186 tests PASS); Physical iPhone waived per owner instruction**.

## Physical Android verification (Motorola Edge 50 Fusion, Android 16)

- Device: Motorola Edge 50 Fusion (`motorola_edge_50_fusion`, Android 16, API 36, arm64-v8a, Serial: `ZA222M4SWQ`).
- Test Sandbox: `com.motovault.app.privacyprobe` and `com.motovault.app.privacyprobe.test`.
- Runner: `com.motovault.app.storage.security.NativeAssetInstrumentation`.
- Execution Result: **PASS 15 native checks** (code `-1`, `RESULT_OK`).
  1. `native-key-provision-and-missing-key-refusal`: AndroidKeyStore AES-256-GCM key generation and refusal when key missing.
  2. `native-interrupted-stage-retains-original-and-cleans-stage`: File staging and fsync; abort before publication retains unencrypted source and cleans stage.
  3. `native-zero-space-preflight-retains-original`: Rejection on insufficient space preflight without corrupting files.
  4. `native-durable-migration-original-bytes-and-idempotent-reopen`: Encrypted publication, SHA-256 validation, plaintext retirement, idempotent retry.
  5. `native-authenticated-sha256-inventory-and-repair`: Encrypted inventory receipt validation and auto-repair on re-migration.
  6. `native-redirected-storage-directory-rejected`: Symlink and path traversal attacks rejected.
  7. `native-tamper-refuses-plaintext-fallback`: Single-bit flipped ciphertext fails closed without fallback to original.
  8. `native-lost-key-retains-files-without-replacement`: Entry deletion in AndroidKeyStore fails closed without overwriting ciphertexts.
  9. `native-encrypted-image-display-and-crop-readers`: Native JPEG/PNG decode and crop preview from AEAD envelope.
  10. `native-export-copy-close-is-idempotent-and-removes-plaintext`: Transient export copy deletion and URI permission revocation.
  11. `native-cold-start-removes-interrupted-export-copy`: App launch sweep of leftover `copy-*` and `pdf-*` files.
  12. `native-legacy-and-recovery-migration-without-overwrite`: Migration of existing legacy/recovery files without ID collision overwrite.
  13. `native-inventory-revalidates-protected-recovery`: Cryptographic inventory check across recovery files.
  14. `native-encrypted-pdf-import-and-no-processing-scratch-leak`: Multi-page PDF decryption, rendering, and scratch deletion.
  15. `native-room4-to5-owner-preservation-and-future-format-refusal`: Room migration preserving existing owner rows and refusing unsupported schema versions.

Full logs recorded in `p02/android-device/motorola-native-result.txt`.

## iOS verification (iPhone 18 Pro Max, iOS 27 Simulator)

- Device: iPhone 18 Pro Max simulator (`50709DF0-FEA9-4B34-9DE6-6172D868875D`, iOS 27).
- Runner: `runIosStorageProbe` via `--motovault-storage-probe`.
- Result: **PASS 15 native checks** (run `P02-storage-schema-acceptance-1`), synthetic Keychain deletion status `-25300`.
- Automated test suites: **186 tests passed, 0 failures** across 26 test suites (`iosSimulatorArm64Test`).

## Design and accessibility review

- UI walk-through recorded in `docs/roadmap/evidence/P02-03-prototype.md`:
  - `MeScreen`: Added Privacy section with biometric/PIN enrollment guide, lost key recovery notice, and auto-lock timeout preferences.
  - `VaultScreen`: Document cards masked when locked ("নম্বর ও ছবি লুকানো আছে। খুলতে ট্যাপ করুন।").
  - `PrivacyGate`: Dedicated lock screen and roadside inspection surface isolating single document from all owner records.
  - App Switcher / Recent Apps: Protected via `FLAG_SECURE` (`MainActivity.setSecureDisplay(true)`) to prevent sensitive document leaks in app switcher thumbnails.

## Hardware gate status

- **Physical Android**: Satisfied (`android_device` evidence recorded above with 15/15 native checks PASS on Motorola Edge 50 Fusion).
- **iOS Runtime**: Satisfied on iOS Simulator (15 native checks PASS, 186/186 automated tests PASS).
- **Physical iPhone**: Waived per owner instruction ("I don't have physical iPhone yet, not sure when it will be available. You need to pass the phase with Android only").
- **Phase P02 Exit Gate**: Complete. Key lifecycle, AES-256 storage adapters, lock/masking/handover UI, and export/import boundaries are verified.
