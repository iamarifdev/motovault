# P03-04: Build Backup, Restore Review and Recovery Help in v3 Implementation

- Task and acceptance criteria covered: P03-04 (Explain file destination, backup age, included data and secret recovery in plain Bangla; separate success/error/cancel states; destructive replacement review step; accessibility, focus, large text, dark theme, and interruption/resume without false success toast).
- Date/time and timezone: 2026-10-27 05:48:00 (Asia/Dhaka).
- Author/reviewer: Antigravity / Engineering Implementation.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Kotlin Multiplatform (Android 14/15, iOS 27.0 Simulator, iPhone 18 Pro Max `50709DF0-FEA9-4B34-9DE6-6172D868875D`).
- Evidence kind: implementation.

## Implementation Details

1. **V3 Sheet Integration (`BackupSheet.kt`)**:
   - Location: `composeApp/src/commonMain/kotlin/com/motovault/app/ui/backup/BackupSheet.kt`
   - Hosts dual-tab interface (`BackupSheetTab.CREATE` and `BackupSheetTab.RESTORE`) styled using `VaultSegmented`.
   - Hosted inside standard `Sheet` with `SheetDetent.TALL`, seamless dismissal grab handle, title bar, and close button.
   - Pinned natural Bangla microcopy without technical jargon or translation artifacts.

2. **Create Backup Flow ("ব্যাকআপ তৈরি")**:
   - Security Info Card: Details portable `.mva` archive format, local encryption, and storage options (Google Drive, pendrive, SD card).
   - Live Snapshot Inventory: Displays current bike model, active odometer, service/fuel ledger record count, and document count directly from active `VaultState`.
   - Passphrase Validation: Enforces minimum 8-character passphrase with confirmation match. Includes visibility toggle ("পাসওয়ার্ড দেখুন" / "পাসওয়ার্ড লুকান").
   - Critical Secret Recovery Warning: Prompts user that forgotten passwords cannot be recovered because encryption is completely zero-knowledge client-side.
   - Native Save Action: Launches native OS file exporter (`BackupNativeActions.saveBackupFile`) on Android SAF / iOS UIActivityViewController.
   - Live Progress & State Machine: Tracks progress (`exportProgress` 0% to 100%) and updates `lastBackupEpochMillis` upon confirmed completion.

3. **Restore & Conflict Review Flow ("রিস্টোর")**:
   - Native File Picker: Invokes `BackupNativeActions.pickBackupFile` to select `.mva` archive from device storage or cloud providers.
   - Preflight Password Prompt: Decrypts and authenticates snapshot via `RestoreUseCase.validateAndPreview`.
   - Concrete Conflict Summary (`BackupConflictCard`): Compares current active garage (bike model, odometer, ledger count, documents) side-by-side with incoming archive.
   - Destructive Overwrite Warning Banner: Clear red-accented warning notice that restore replaces the local database.
   - Explicit Confirmation Gate: Checkbox / switch ("আমি নিশ্চিত যে বর্তমান ডাটা মুছে ফেলে ব্যাকআপ ডাটা প্রতিস্থাপন করতে চাই") required before unlocking the restore button.
   - Transactional Rollback Safety: Aborts without touching the existing vault if validation fails or decryption throws `BackupSecurityException`.

4. **MeScreen Integration (`Screens.kt`)**:
   - Added persistent "ব্যাকআপ ও রিস্টোর" card in `MeScreen`.
   - Shows last backup timestamp formatted with localized Bengali digits, or "এখনও কোনো ব্যাকআপ নেওয়া হয়নি" when null.
   - One-tap quick actions for "ব্যাকআপ তৈরি" and "রিস্টোর করুন".
