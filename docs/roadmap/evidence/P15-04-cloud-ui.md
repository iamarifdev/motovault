# P15-04 Verification Evidence — Cloud Backup Status Sheet & v3 Prototype

## Implementation
- **Shared Kotlin UI**: Created [CloudBackupSheet.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/backup/CloudBackupSheet.kt), wired `"cloud-backup"` route in [VaultSheetHost.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultSheetHost.kt), and added the entry row in [SettingsActionSections.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/settings/SettingsActionSections.kt).
- **Prototype Parity (`design/app-ui-v3`)**: Added `'cloud-backup'` sheet state and Cloud Backup status row in `design/app-ui-v3/src/store.ts` and `design/app-ui-v3/src/screens/Me.tsx`.
- **Localization & Honest Status**: Added EN + BN `cloud_backup_*` strings displaying `CloudConnectionStatus.DISCONNECTED` (`সংযুক্ত নয় — লোকাল ভল্ট সম্পূর্ণ অফলাইনে কাজ করে`), key-separation notice (passphrase never leaves device; only encrypted `.mva` archives leave device), and direct handoff to local `.mva` backup.

## Verification
- `design/app-ui-v3` build (`npm run build`): exit 0.
- Portable gates (`bangla_copy_lint.py`, `ui_literal_gate.py`, `responsibility_gate.py`, `architecture_dependencies.py`): exit 0.
