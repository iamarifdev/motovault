# Android continuation runtime evidence

Verified 12 October 2026 on the existing software-rendered Pixel_10 emulator, Android emulator-5554. No device wipe, uninstall, clear-data, commit or push was performed. Physical device quality is not covered.

## Migration and restart

Before update, the existing JSON contained one KTM Duke 125 (2023) owner-defined ride, three owner-defined schedules, no ledger entries and two documents: registration front/back and a two-page tax-token PDF. The registration holder was already `SAMPLE HOLDER_EDITED`; this was preserved along with candidates, confirmation status, IDs and original asset metadata.

The debug APK was installed with `install -r`, launched, subsequently updated again and force-stopped/relaunched. SQLite snapshot inspection returned `PRAGMA integrity_check = ok`, user version 1, one vehicle, three schedules, zero ledger rows, two document rows and thirteen asset references. Every original JSON field matched the corresponding decoded database payload. Migration metadata recorded `legacyImported = 1` and `appearance = SYSTEM`.

The first attempted shell backup failed because the RTK shell wrapper did not preserve the quoted `sh -c` payload. The successful private archive was captured immediately after migration, not before update. The original JSON remained available. All 27 private document files and JSON bytes remained identical between this post-migration archive and subsequent update/process-restart snapshots. This establishes post-migration restart preservation, not a fresh before/after migration byte comparison for every original.

The current original tax PDF SHA-256 is `b386e073c880a9c8b0413eb6af6f3fdabe375613929b602799a3846182ded1f2`, matching the prior source hash recorded in CONTINUATION_HANDOFF.md. No ledger data existed on this emulator to exercise live fuel/service restart preservation.

Private archives are kept in `/tmp/motovault-runtime-before/private-data.tar` and `/tmp/motovault-runtime-after/`; they are not repository artifacts. The misleading `before` directory name refers to before the restart checks, not before migration.

## Inspection and themes

Actual window inspection showed the document dialog with `sbrt=1.0` during inspection. Pressing Home removed that override while preserving the dialog; bringing the app back restored `sbrt=1.0`; Exit inspection dismissed the dialog and removed the override. UI inspection contained document labels and original image only, with no service costs or workshop notes.

Screenshots were captured and visually inspected. The saved green sample document remains visibly green; Dark changes the app surface and brand controls while inspection remains light. Dark inspection retains dark system bars with readable light icons. System was restored at the end of theme verification.

- `01-inspection-light.png`: original registration in light document-only inspection.
- `02-restored-garage.png`: existing owner-defined ride restored after update/restart.
- `03-dark-garage.png`: actual Dark garage, KTM brand mark and model photograph.
- `04-dark-vault.png`: actual Dark vault with preserved document records.
- `05-light-inspection-from-dark.png`: light inspection within Dark app.

Opening replacement setup showed the existing selected KTM model and brand choices. Android Back exited the activity; force-stop/relaunch discarded the unsaved draft and restored the saved garage. Replacement submission was deliberately not performed against the preserved owner record.

## Reversible startup recovery and recreation

The initial deletion-based startup APK was installed, but launch was rejected by automatic approval review. No rejection bypass was attempted. Source was changed to same-volume private quarantine with no deletion or automatic purge. Inspection confirmed failed moves retain sources, name collisions do not overwrite, saved asset references are excluded and symlinks are excluded. Normal approval review subsequently allowed the materially safer build to launch.

All 27 original baseline files remained byte-identical after revised startup. All 13 saved asset references remained in their original managed directory. The original unsaved capture files were younger than the 24-hour grace and remained there. A separately tracked synthetic 54-byte probe was created, given modification time 1 January 2020 and renamed to the eligible filename `1234567890123-999999.png`. Startup moved it to `files/documents-recovery/` with exactly the original timestamp and SHA-256 `9ea7993fe968d4f082f18d33a22a1b8412e2c2be2d79ea7b1876f9210516f884`. This tests quarantine byte retention; the fixture is arbitrary test bytes, not an image decoded by the UI. It remains in private recovery storage.

The original system font scale was 1.0. Changing it to 1.3 recreated the activity and restored the same saved garage; `06-font-scale-130-garage.png` records the visible result. Font scale was restored to 1.0. Subsequent database and hash checks again passed without modifying owner records. The existing emulator was rebooted without snapshots or wipe after its earlier tool process ended.

## Passport runtime evidence

The debug-only fixture activity generated a real 16-page Android PDF and paired JSON containing 70 synthetic service entries, Bangla, Japanese and Café text, and one 100-line note. It never loads or saves the owner vault. Artifacts and page renders are in `passports/`. Native renderer pagination and readable Unicode were visually checked on pages 1, 2, 8, 9 and 16. The passport implementation agent independently checked all ordered service headings and note lines, canonical SHA-256 and decoded the final-page QR.

The actual History flow exported the existing SAMPLE identity with zero saved services after explicit review and checkbox confirmation. Native PDF viewing opened successfully (`07-native-passport-viewer.png`). Native save completed for PDF and JSON in emulator Downloads. Share opened the native chooser showing two files (`08-passport-share-chooser.png`) and was cancelled without selecting a recipient. The app verifier imported the native-saved JSON and accepted its bundled digest (`09-passport-integrity-valid.png`), then rejected an all-zero independently expected digest (`10-passport-wrong-digest-rejected.png`). All owner records and original asset bytes remained unchanged after these actions. See SERVICE_PASSPORT_IMPLEMENTATION.md for full export proof and integrity scope.

No physical camera/card, iOS runtime, notification delivery or cloud claim is established by this report.
