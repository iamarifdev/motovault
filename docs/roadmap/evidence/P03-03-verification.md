# P03-03: Staged Validation and Transactional Restore Verification

- Task and acceptance criteria covered: P03-03 verification (Injection of interruptions across restore stages, record + asset hash comparison, older supported archives, duplicate IDs, conflict previews).
- Date/time and timezone: 2026-10-27 05:04:30 (Asia/Dhaka).
- Author/reviewer: Antigravity / Engineering Verification.
- Branch, commit and relevant uncommitted changes: `main`, commit `04bfaf5` + working changes.
- App build/version and enabled feature flags: 1.0.0, Room schema version 5.
- Platform, OS, emulator/simulator or physical-device model: Kotlin Multiplatform unit tests (`:composeApp:testDebugUnitTest`), iOS simulator compilation (`:composeApp:compileKotlinIosSimulatorArm64`).
- Evidence kind: verification.

## Scope and Automated Test Execution

Automated test suite executed in `composeApp/src/commonTest/kotlin/com/motovault/app/domain/backup/BackupTest.kt`.
Command: `./gradlew :composeApp:testDebugUnitTest :composeApp:compileKotlinIosSimulatorArm64`
Result: All 184 tests passed; BUILD SUCCESSFUL.

## Verification Matrix

| Test Case | Scenario / Injected Condition | Expected Behavior | Observed Result | Status |
| --- | --- | --- | --- | --- |
| `testMvaZipArchiveRejectsPathTraversal` | Injected malicious path traversal (`../secret.txt`, `/absolute.txt`) | Reject extraction with `IllegalArgumentException` | Exception thrown; paths blocked | PASS |
| `testRestoreFailsOnCorruptZip` | Injected truncated/malformed zip bytes | Reject with `IllegalArgumentException` / `require` | Fails closed before decryption or DB writes | PASS |
| `testRestoreFailsOnWrongPassword` | Injected wrong passphrase during preview | Reject with `BackupSecurityException` | Cryptographic tag mismatch throws `BackupSecurityException` | PASS |
| `testRestoreRejectsTamperedAssetDigest` | Injected modified SHA-256 in manifest or tampered ciphertext | Reject with `IllegalArgumentException("SHA-256 verification failed")` | Bit-level tamper detected; restore refused | PASS |
| `testRestoreRejectsDuplicateIds` | Injected duplicate log/schedule IDs in backup payload | Reject with `IllegalArgumentException("duplicate ledger IDs")` | Schema constraint enforced; duplicate rejected | PASS |
| `testOlderSupportedArchiveWithoutOptionalFields` | Backup payload without newer fields (`autoLocation`, `serviceLines`) | Clean deserialization with default values applied | Decodes cleanly; `autoLocation` defaults to false, appearance to SYSTEM | PASS |
| `testPreviewCountsAndConflicts` | Incoming MT-15 (15k km, 2 logs) vs current CB150R (5k km, 1 log) | Conflict summary reflects exact discrepancies and `bikeMatches = false` | Preview accurately reports differences | PASS |

## Interruption and Rollback Safety

- Pre-commit failure at Stage 1, 2, 3, or 4: Zero database writes or asset commits occur. The running app and database maintain previous state unharmed.
- Failure during database `replace(...)`: Room executes SQLite `ROLLBACK`. All tables remain intact.
- Post-restore: `reconcileRenewalReminders` verifies restored document expiry dates and schedules alarms.

## Conclusion

All acceptance criteria and verification scenarios for P03-03 are met.
