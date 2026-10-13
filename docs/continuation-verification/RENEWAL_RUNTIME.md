# Android renewal runtime verification

Verified 13 October 2026 on preserved Pixel_10, emulator-5554. The root task launched the existing AVD without wiping it. No uninstall, clear-data, owner-record edit or production reminder fixture was performed.

The installed continuation APK was used without rebuilding or reinstalling. The corresponding local APK was 114265830 bytes, modified 11 October 2026 at 20:23:37 UTC; `classes4.dex` contains `RenewalFixtureActivity` and `RenewalFixtureReceiver`. New partial Room v2 source was not deployed during this check.

## Due delivery

`RenewalFixtureActivity` scheduled a synthetic alarm with `delay_ms=2000`. The separate `renewal_debug_fixture` ledger became `consumed=true`, `deliveryCount=1`, with key `renewal:debug-fixture:1791873266170:expiry`. Native notification inspection showed exactly one active MotoVault notification with that full tag, integer ID zero, channel `document_renewals`, title `Document renewal`, and text `SAMPLE TEST ONLY tax token expires today.` Screenshot `11-renewal-delivered.png` was visually inspected and shows that notification in the Android shade.

This verifies AlarmManager delivery through the debug receiver and shared production notification builder. The debug receiver does not open the owner database or production reminder preferences. It does not establish the production receiver's due-document validation, which remains supported by policy/source tests rather than a modified owner document.

## Cancel before due

A fresh fixture was scheduled with `delay_ms=30000`, then the fixture activity was invoked with `mode=cancel` before that due time. After the due time, the fixture preference map remained empty, the native notification list contained zero MotoVault records, and the alarm dump contained no `debug-renewal` entries. Screenshot `12-renewal-cancelled.png` records the shade after cancellation.

## Process restart

A fresh two-second fixture delivered once with key `renewal:debug-fixture:1791873433907:expiry`. Its ledger recorded `consumed=true` and `deliveryCount=1`. After force-stop and MainActivity relaunch, the same key and count remained unchanged. Native notification inspection showed zero active MotoVault notifications: Android removed the notification at force-stop and the app did not repost it. Screenshot `13-renewal-restart-garage.png` was visually inspected and shows the preserved KTM Duke 125 (2023) garage.

This verifies durable consumption and no repost on relaunch. The initial packaged fixture had no replay mode; the later debug-only replay verification below closes the duplicate receiver-invocation gap without resetting its ledger.

## Production queue and recovery storage

After restart, `renewal_reminders.xml` remained enabled with four full-key plans for the two existing documents: warning dates 2030-10-26 and expiry dates 2030-12-25. Fixture tests did not change these plans.

The 24-hour startup grace has now elapsed for additional prior unsaved captures. `documents-recovery` contains fifteen files, including the tracked `1234567890123-999999.png` probe. This is reversible quarantine, not deletion. The probe still matches SHA-256 `9ea7993fe968d4f082f18d33a22a1b8412e2c2be2d79ea7b1876f9210516f884`.

## Additive Room v2 migration and restart

The old report's `/tmp/motovault-runtime-before` and `/tmp/motovault-runtime-after` archives no longer exist on this host, and no retained full baseline manifest was found in the verification directory. Earlier 27-file before/after evidence remains a prior-session report, not newly reverified proof.

A fresh private baseline was captured before today's update at `/tmp/motovault-oct13-before/private-data.tar`. After root confirmed the new APK had passed compilation and tests, it was installed with `install -r`, launched, force-stopped and relaunched. Post-update snapshots are `/tmp/motovault-oct13-after/private-data.tar` and `/tmp/motovault-oct13-after/restart-private-data.tar`. These private snapshots remain outside the repository.

SQLite reported user version 1 before and version 2 after, with `integrity_check = ok` before, after and after restart. The complete SQL rows remained identical for vehicle (1), schedule (3), ledger (0), document (2), asset_reference (13), and vault_meta (1). The original JSON was byte-identical. All twenty-eight current asset files remained byte-identical at the same paths across update and restart: thirteen managed assets and fifteen quarantined assets. The managed tax-token PDF retained the previously recorded SHA-256 `b386e073c880a9c8b0413eb6af6f3fdabe375613929b602799a3846182ded1f2`.

The added `sync_outbox` table contains exactly one initial local snapshot after migration and after restart. Its columns are `id`, `installationId`, `revision`, `payload`, `tombstones`, `attemptCount`, `retryAfterEpochMillis`, and `failureCode`. The row has revision 1, empty tombstones `{}`, zero attempt count, zero retry time and null failure code. This is local preparation; it establishes neither account binding nor remote transport. Production reminder preferences and the consumed debug fixture ledger remained byte-identical across update and restart.

Screenshot `14-v2-restart-garage.png` was visually inspected and shows the preserved KTM garage after migration and restart. `OCT13_ASSET_BASELINE.json` retains today's full asset paths, sizes and SHA-256 values in the repository for future comparisons; it contains no document contents.

## Duplicate receiver replay and final preservation smoke

The first replay attempt found no attached emulator and changed no device data. Root restarted the same preserved AVD headlessly with no snapshot load and no wipe after the earlier emulator had exited. Before updating, all twenty-eight baseline asset hashes and all owner-table rows still matched, database version 2 passed integrity check, the outbox contained one row, and production reminder preferences were unchanged.

The verified replay APK (modified 13 October 2026 at 07:00:46 UTC, with the replay string and fixture symbols present in `classes4.dex`) was installed with `install -r` after a fresh private snapshot. `mode=replay` sends the existing fixture key to the non-exported fixture receiver before the activity's ordinary reset/cancel branch. It does not create a new key or reset consumption.

Two successive `mode=replay` invocations used the already consumed key `renewal:debug-fixture:1791873433907:expiry`. The ledger remained `consumed=true`, `deliveryCount=1`, and native notification inspection returned zero active MotoVault notifications. After force-stop, MainActivity relaunch and a third replay of that same key, the ledger remained byte-identical and there were again zero active MotoVault notifications. Screenshot `15-renewal-replay-suppressed.png` was visually inspected and shows the Android shade without a MotoVault reminder.

The final private snapshot `/tmp/motovault-oct13-after/final-replay-private-data.tar` again passed database integrity check at version 2. All twenty-eight asset paths, sizes and SHA-256 values matched `OCT13_ASSET_BASELINE.json`; all six owner tables and the retained legacy JSON matched the original fresh Oct13 baseline. The single outbox row was identical to the pre-replay-update snapshot, and production reminder preferences remained byte-identical. This proves duplicate suppression for the debug receiver's consumed-key guard; it remains separate from production due-document validation.

Physical-device and iOS native delivery remain outside this verification.
