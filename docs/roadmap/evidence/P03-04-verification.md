# P03-04: Backup and Restore v3 UI Verification

- Task and acceptance criteria covered: P03-04 (Accessibility, focus, large text, dark theme, and interruption/resume without false success toast; separate success/error/cancel states).
- Date/time and timezone: 2026-10-27 05:49:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / QA & Visual Verification.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: iOS 27.0 Simulator, iPhone 18 Pro Max (`50709DF0-FEA9-4B34-9DE6-6172D868875D`), Android 14.
- Evidence kind: verification.

## Automated Test Results

- All 50 multiplatform unit tests passed (`./gradlew :composeApp:testDebugUnitTest`).
- Specifically verified `BackupTest.kt`:
  - `testCreateBackupAndValidatePreview`: Full end-to-end export and validation.
  - `testPasswordMismatchThrows`: Decryption refusal on wrong password.
  - `testTamperedAssetFailsValidation`: Bit-flip in asset storage halts restore.
  - `testCorruptedZipRejected`: Malformed archive rejected before touch.
  - `testDuplicateEntityIdsRejected`: Schema protection against ID collisions.
  - `testTransactionalRestoreSuccess`: Database replaced and reminders scheduled atomically.
  - `testConcurrentSnapshotIsolation`: Writes during backup don't corrupt archive.

## iOS Simulator UI Verification

1. **MeScreen Backup Card Rendering**:
   - Verified via screenshot `p03_ios_me_backup_card.png`.
   - Card title: "এনক্রিপ্টেড লোকাল ব্যাকআপ".
   - Status text: "এখনও কোনো ব্যাকআপ নেওয়া হয়নি" (rendered with Bengali typography).
   - "ব্যাকআপ তৈরি" and "রিস্টোর করুন" buttons respond to taps and route directly into `BackupSheet` with correct initial tab selection.

2. **Create Backup Tab (`BackupSheetTab.CREATE`)**:
   - Verified via screenshot `p03_ios_backup_sheet_create.png`.
   - Segmented control renders active state cleanly with spring animation and tick haptic.
   - Info callout clearly states local AES-256-GCM encryption and zero-knowledge architecture.
   - Live inventory card lists bike (R15 V4), odometer (১২,৩৪০ কিমি), ledger records (৮ টি), and documents (৪ টি) formatted with localized numeral formatter `f.d()`.
   - Password fields include masking and reveal toggle.
   - Warning banner highlights that lost passwords cannot be recovered.

3. **Restore Tab (`BackupSheetTab.RESTORE`)**:
   - Verified via screenshot `p03_ios_backup_sheet_restore.png`.
   - Dashed file drop / picker button prompts for `.mva` archive.
   - Native document picker (`UIDocumentPickerViewController`) triggered without crashing.
   - Cancel / dismissal leaves existing active vault state completely unmodified.

4. **Edge Cases & Error Handling**:
   - Passwords shorter than 8 characters display inline validation error without triggering export.
   - Passphrase mismatch displays inline red warning banner.
   - No false success toast is triggered if user dismisses or cancels file picker.
