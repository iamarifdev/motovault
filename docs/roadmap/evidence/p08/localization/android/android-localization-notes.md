# P08-05 Android runtime localization and language-switch evidence

Captured by the delegated agent `android-l10n-runner` (held shared task `task-3`)
on 2026-11-12, 23:09–23:18 Asia/Dhaka. All screenshots were captured from the
already-booted emulator with the isolated synthetic probe app; no source file was
edited and nothing was committed.

## 1. Device and OS identity

| Fact | Value |
| --- | --- |
| adb serial | `emulator-5554` |
| AVD / host identity (Lead-verified) | Pixel_10 |
| `ro.product.model` | `sdk_gphone16k_arm64` (reports the generic emulator model; the AVD name is Pixel_10) |
| `ro.product.device` | `emu64a16k` |
| `ro.build.version.release` / `ro.build.version.sdk` | Android 17 / API 37 |
| `ro.build.fingerprint` | `google/sdk_gphone16k_arm64/emu64a16k:17/CP31.260623.012/16064790:user/dev-keys` |
| `ro.product.cpu.abi` | `arm64-v8a` |
| Guest date/time at capture | `Thu Nov 12 23:21:44 +06 2026` |
| `persist.sys.locale` | unset (empty) — OS locale never set by this run |
| `settings get system font_scale` | `1.0` — OS font scale never changed |
| `cmd uimode night` | `Night mode: no` — OS night mode never changed |

## 2. Target artifacts (identity)

- Probe package: `com.motovault.app.privacyprobe` (`versionName=1.0.0-privacyprobe`, `versionCode=1`)
- Installed probe APK: `/data/app/~~vRC1bqanaNpyI6amd9YZpQ==/com.motovault.app.privacyprobe-nPPh8YY-2EKvB6QV9oYx1A==/base.apk`
  - SHA-256 `a0ee2636fadf57fef0b24bc9d88ec1582d9c121c059ee5e940ba393bb44c1581` (matches the Lead-provided hash)
- Test package: `com.motovault.app.privacyprobe.test`
  - Test APK: `/data/app/~~3iF6Nz_1jpJzjcQoFFebvQ==/com.motovault.app.privacyprobe.test-aJyEvQGSNUc0eeEEnozbdQ==/base.apk`
  - SHA-256 `7f7c82fbae147c414896f928f57a9629282ed007f418f3c9de98c076701c7713` (matches the Lead-provided hash)

Source revision: **`e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4` (HEAD) plus an
uncommitted working tree containing the P08-05/P08-01 changes.** This evidence
therefore describes the working-tree build, not a clean HEAD build, and is not
labelled as such.

`git status --short` at capture time:

```text
 M composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/ProbeFuelChecks.kt
 M composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/ProbeRenewalChecks.kt
 M composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/ProbeUiHelpers.kt
 M composeApp/src/commonMain/composeResources/values-bn/costs.xml
 M composeApp/src/commonMain/composeResources/values-bn/history.xml
 M composeApp/src/commonMain/composeResources/values-bn/strings.xml
 M composeApp/src/commonMain/composeResources/values/costs.xml
 M composeApp/src/commonMain/composeResources/values/history.xml
 M composeApp/src/commonMain/composeResources/values/strings.xml
 M composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentWallet.kt
 M composeApp/src/commonMain/kotlin/com/motovault/app/ui/GaugeDetailSheet.kt
 M composeApp/src/commonMain/kotlin/com/motovault/app/ui/garage/OdometerSheet.kt
 M composeApp/src/commonMain/kotlin/com/motovault/app/ui/history/HistoryScreen.kt
 M composeApp/src/commonMain/kotlin/com/motovault/app/ui/maintenance/ServiceMetaSections.kt
 M composeApp/src/commonMain/kotlin/com/motovault/app/ui/onboarding/OnboardingScreen.kt
 M docs/roadmap/IMPLEMENTATION-PLAN.md
 M docs/roadmap/PROGRESS.md
 M docs/roadmap/roadmap.json
 M iosApp/MotoVault.xcodeproj/project.pbxproj
 M tools/tests/test_localization_resources.py
 M tools/tests/test_ui_literal_gate.py
 M tools/ui-literal-baseline.json
 M tools/ui_literal_gate.py
?? docs/roadmap/evidence/p08/
```

## 3. Harness and exact commands

Host fixture (real production four-tab UI composables, synthetic `VaultState.seedData()`
in an isolated temp DB; the Activity creates `cacheDir/synthetic-fuel-<uuid>` and
deletes it in `onDestroy`):

```bash
# Normal launch, Bangla language + Bangla digit preference
adb -s emulator-5554 shell am force-stop com.motovault.app.privacyprobe
adb -s emulator-5554 shell am start -n com.motovault.app.privacyprobe/com.motovault.app.FuelEditFixtureActivity --es language bn --es digits bn

# Same but English / English (only used for the EN save-failure case)
adb -s emulator-5554 shell am start -n com.motovault.app.privacyprobe/com.motovault.app.FuelEditFixtureActivity --es language en --es digits en

# Write-failure variants (pre-opens the fuel sheet)
adb -s emulator-5554 shell am start -n com.motovault.app.privacyprobe/com.motovault.app.FuelEditFixtureActivity --es language bn --es digits bn --ez failWrites true
adb -s emulator-5554 shell am start -n com.motovault.app.privacyprobe/com.motovault.app.FuelEditFixtureActivity --es language en --es digits en --ez failWrites true
```

Screenshot / inspection / input commands used throughout:

```bash
adb -s emulator-5554 exec-out screencap -p > <file>.png
adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml
adb -s emulator-5554 exec-out cat /sdcard/ui.xml
adb -s emulator-5554 shell input tap X Y
adb -s emulator-5554 shell input swipe X1 Y1 X2 Y2 400
adb -s emulator-5554 shell input keyevent KEYCODE_BACK
adb -s emulator-5554 shell pidof com.motovault.app.privacyprobe
```

Probe identity and native instrumentation commands:

```bash
adb -s emulator-5554 shell pm path com.motovault.app.privacyprobe
adb -s emulator-5554 shell pm path com.motovault.app.privacyprobe.test
adb -s emulator-5554 shell sha256sum <installed-base.apk>
adb -s emulator-5554 shell getprop ro.product.model
adb -s emulator-5554 shell getprop ro.build.version.release
adb -s emulator-5554 shell getprop ro.build.version.sdk
adb -s emulator-5554 shell getprop ro.build.fingerprint
adb -s emulator-5554 shell getprop ro.product.cpu.abi
adb -s emulator-5554 shell settings get system font_scale
adb -s emulator-5554 shell cmd uimode night
adb -s emulator-5554 shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Language was switched **only** through the in-app Profile control
(`Profile → Appearance → Language → English / বাংলা`); the OS locale was never
changed. The probe was only ever `force-stop`ped and restarted between capture
sessions; it was never uninstalled and its data was never cleared.

## 4. Capture sessions and case table

| Session | Launch | Purpose |
| --- | --- | --- |
| S1 | `language bn`, `digits bn` | Bangla tab/sheet captures (11:10–11:11) |
| S2 | `language bn`, `digits bn`, then in-app switch to English | English sheet captures; PID stayed `20608` across the switch (11:11–11:13) |
| S3 | `language bn`, `digits bn`, in-app switch BN→EN→BN | Same-session tab captures in both languages; PID stayed `21845` across both switches (11:16–11:17) |
| S4 | `language bn`, `digits bn`, `failWrites=true` | Bangla fuel save-failure error (11:14) |
| S5 | `language en`, `digits en`, `failWrites=true` | English fuel save-failure error (11:15) |

All PNGs are 1080×2424. Every screenshot was opened with the image viewer and
its on-screen copy was cross-checked against the uiautomator text tree; document
counts and numerals that looked ambiguous at capture scale were re-checked against
the accessibility dump.

| File | Language | Digits | Screen / state | Session |
| --- | --- | --- | --- | --- |
| `p08-l10n-android-records-bn.png` | BN | বাংলা | Records tab, full page, before switch | S3 |
| `p08-l10n-android-garage-bn.png` | BN | বাংলা | Garage tab, due-now card + odometer + fuel average | S3 |
| `p08-l10n-android-documents-bn.png` | BN | বাংলা | Documents tab, expiry banner + document cards | S3 |
| `p08-l10n-android-profile-appearance-bn.png` | BN | বাংলা | Profile → Appearance; বাংলা selected in Language segmented control | S3 |
| `p08-l10n-android-profile-appearance-en.png` | EN | বাংলা | Same screen after tapping **English**; English selected, still same process | S3 |
| `p08-l10n-android-garage-en.png` | EN | বাংলা | Garage tab after the in-app switch | S3 |
| `p08-l10n-android-documents-en.png` | EN | বাংলা | Documents tab after the in-app switch | S3 |
| `p08-l10n-android-records-en.png` | EN | বাংলা | Records tab after the in-app switch | S3 |
| `p08-l10n-android-profile-appearance-bn-switchback.png` | BN | বাংলা | Profile → Appearance after tapping **বাংলা** (switch-back), same process | S3 |
| `p08-l10n-android-garage-bn-switchback.png` | BN | বাংলা | Garage tab after switch-back; byte-identical to the pre-switch Garage capture | S3 |
| `p08-l10n-android-quickadd-sheet-bn.png` | BN | বাংলা | Quick-add sheet: Fuel / Service / Odometer / Document | S1 |
| `p08-l10n-android-quickadd-sheet-en.png` | EN | বাংলা | Quick-add sheet after in-app switch | S2 |
| `p08-l10n-android-fuel-sheet-bn.png` | BN | বাংলা | Fuel sheet, empty amount, keypad | S1 |
| `p08-l10n-android-fuel-sheet-en.png` | EN | বাংলা | Fuel sheet after in-app switch | S2 |
| `p08-l10n-android-service-sheet-bn.png` | BN | বাংলা | Garage → সার্ভিস যোগ করুন → Service sheet | S1 |
| `p08-l10n-android-service-sheet-en.png` | EN | বাংলা | Same Service sheet after in-app switch | S2 |
| `p08-l10n-android-expense-sheet-bn.png` | BN | বাংলা | Records → খরচ যোগ করুন → Add expense sheet | S1 |
| `p08-l10n-android-expense-sheet-en.png` | EN | বাংলা | Same Add expense sheet after in-app switch | S2 |
| `p08-l10n-android-document-add-sheet-bn.png` | BN | বাংলা | Documents → + → Which document? sheet | S1 |
| `p08-l10n-android-document-add-sheet-en.png` | EN | বাংলা | Same Which document? sheet after in-app switch | S2 |
| `p08-l10n-android-privacy-sheet-bn.png` | BN | বাংলা | Profile → প্রাইভেসি সেটিংস → Privacy & Security sheet | S1 |
| `p08-l10n-android-privacy-sheet-en.png` | EN | বাংলা | Same Privacy & Security sheet after in-app switch | S2 |
| `p08-l10n-android-backup-sheet-bn.png` | BN | বাংলা | Profile → ব্যাকআপ তৈরি → Backup and restore sheet | S1 |
| `p08-l10n-android-backup-sheet-en.png` | EN | বাংলা | Same Backup and restore sheet after in-app switch | S2 |
| `p08-l10n-android-profile-notification-bn.png` | BN | বাংলা | Profile → Reminders: notification-permission card, "নোটিফিকেশন চালু করুন" | S1 |
| `p08-l10n-android-profile-notification-en.png` | EN | বাংলা | Profile → Reminders after in-app switch, "Allow phone notifications" | S2 |
| `p08-l10n-android-fuel-save-error-bn.png` | BN | বাংলা | Fuel sheet after Save on a failing write: inline Bangla error + transient snackbar | S4 |
| `p08-l10n-android-fuel-save-error-en.png` | EN | EN | Fuel sheet after Save on a failing write: inline English error + transient snackbar | S5 |

SHA-256 of each PNG is recorded per file in the capture log; the two
`garage-bn`/`garage-bn-switchback` files are byte-identical
(`929a6ff87ffd32439e5011f3317e6dd5ef287ae5d09dcbe6084923701ef46694`), showing the
UI returned to the same rendered Bangla state after the round trip.

## 5. What was observed

1. **Instant in-app language switch, no relaunch.** Tapping English in
   Profile → Appearance changed the whole UI immediately while the process PID
   stayed the same (S2: `20608` before and after; S3: `21845` before the
   BN→EN switch, after it, and after the EN→BN switch-back). No activity
   restart, no relaunch, no OS locale change.
2. **Both directions work.** BN→EN and EN→BN both re-rendered the tab bar
   (গ্যারেজ / ডকুমেন্ট / রেকর্ড / প্রোফাইল ↔ Garage / Documents / Records / Profile),
   screen headers, section titles, filters/chips, buttons, sheets, form hints,
   error copy and accessibility labels.
3. **Language and digit style are independent.** With language EN but digit
   style বাংলা, numerals stayed Bangla in the English UI (e.g. `৳১,৮১০.০০`,
   `৪৫০ km`, `৩০ days`, `Expiry: ৭ November ২০২৬`). With both set to English
   (S5 only) the numerals rendered Latin (`Save ৳5.00`, `≈ 0.04 L`,
   `৳120.00/L`). The currency symbol came from the resource in both languages.
4. **Quick add and the four tabs** are fully localized in both languages; the
   quick-add choices map 1:1 (ফুয়েল/সার্ভিস/ওডোমিটার/ডকুমেন্ট ↔ Fuel fill/
   Service/Odometer/Document) and each sheet keeps the same layout.
5. **Sheets are localized:** fuel (amount/litres/price/date controls, validation
   footer), service (schedule header, parts chips, total, save), add expense
   (category chips, amount placeholder, date hint, save), document add (all five
   document types and descriptions), privacy (app lock, scope, key-loss copy),
   backup (segmented control, contents list, password rules, warning).
6. **Notification permission card is localized.** Bangla: "ফোনের সেটিংসে
   নোটিফিকেশন চালু নেই", "নোটিফিকেশন চালু করুন", "নোটিফিকেশন সেটিংস খুলুন",
   "পারমিশন আবার যাচাই করুন". English: "Notifications are not allowed in phone
   settings", "Allow phone notifications", "Open notification settings",
   "Check permission again". This is the manual-path permission surface only; no
   OS permission prompt/dialog was triggered or manipulated.
7. **Save-failure error is localized.** The fixture's write adapter raises on
   every write, so the fuel Save path fails:
   - Bangla (S4): inline `সেভ করা যায়নি। তথ্য মুছে যায়নি, আবার চেষ্টা করুন।`
     and transient snackbar `এই ডিভাইসে সেভ করা যায়নি। আগের রেকর্ড অপরিবর্তিত আছে।`
   - English (S5): inline `Could not save. Your draft is still here. Try again.`
     and transient snackbar `Could not save on this device. Your previous records
     are unchanged.`
   - Reproduction detail: on the fuel sheet the price must first be confirmed
     with the "দাম কনফার্ম করেছি" / "I confirmed this price" checkbox; the
     primary button is disabled until then and shows the validation label
     ("টাকার পরিমাণ লিখুন" / "Enter the amount", then "প্রতি লিটারের দাম মিলিয়ে
     কনফার্ম করুন" / "Check and confirm the price per litre"). One tap on that
     disabled footer only vibrates; after confirmation the button becomes
     "৳৫.০০ যোগ করুন" / "Save ৳5.00" and the second tap triggers the failure.
8. **Settings writes obey the same failure path.** In the `failWrites` session,
   tapping English in Profile did not switch the language: the coordinator
   refused the write, the existing Bangla state and records stayed intact, and
   the write-failure snackbar was visible. The EN save-failure case therefore
   used a separate `language en`/`digits en` launch; this is the reason, and it
   is not a gap in the switch evidence (which was captured with normal writes).
9. **Layout held at the default text scale** for both languages; no truncation
   of primary actions observed (the Records category chip row intentionally
   scrolls horizontally and shows a clipped leading edge in both languages).

## 6. Defects and gaps observed (for the owner / P08-05 reviewer)

1. **Seeded demo display names are not localized.** In English the Garage
   due-now card still shows `চেইন` and the Service sheet header shows
   `Schedule: চেইন পরিষ্কার ও লুব্রিকেন্ট`; the Documents list titles are
   `ট্যাক্স টোকেন` / `ড্রাইভিং লাইসেন্স` while the document card type is
   localized ("Tax Token", "Driving License"). Cause: the synthetic seed in
   `composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultCreation.kt`
   (`VaultState.seedData`) stores Bangla display labels for component rules,
   service lines, documents, workshop and location names. This is demo/seed
   data, not owner-entered data — names a user typed are legitimately kept as
   entered — but it means these surfaces cannot demonstrate English naming in
   the seed state, and the seed data itself does not follow
   "localize at the boundary". Flagged as a demo-data localization gap, not a
   crash/data defect.
2. **English Profile reminder-settings row reads "Permission, time and later".**
   Bangla shows `রিমাইন্ডার সেটিংস` (the style-guide term); the English string
   looks like a literal translation of the avoid-list phrase "অনুমতি, সময় ও পরে
   জানান". Recommend "Reminder settings". Visible behind the privacy/backup
   English sheets and in the Profile English scroll; this is a copy review item.
3. **Mixed-script dates/numbers with digits=বাংলা and language=English**
   (e.g. `Expiry: ৭ November ২০২৬`, `৩০ days before`). This follows the design
   contract's independent digit preference rather than a defect, but reviewers
   may want to confirm that mixed month/numeral rendering is the intended look.
4. **Demo-only sections visible in the synthetic host** ("View previous vaults",
   "New concepts (demo)") are fixture/demo surfaces, not production copy, and
   were not assessed for localization.
5. **No OS notification permission prompt** was shown or changed; the card and
   manual-settings path were captured as required. Granting the permission is a
   separate owner decision and was deliberately not done.

## 7. Native instrumentation probe

File `android-native-probe.txt` in this directory is the verbatim stdout of:

```bash
adb -s emulator-5554 shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation
```

Exit code 0. The single PASS line is:

```text
PASS 37 native storage checks: synthetic-native-fuel-ui-room-reopen-and-undo, synthetic-fuel-create-failure-preserves-draft-and-room, native-maintenance-profile-actions-provenance-and-unknown-baseline, native-maintenance-onboarding-and-schedule-details-ui, native-renewal-checklist-receipt-document-expense-and-reminder, synthetic-recovery-ui-success-conflict-and-busy-dismissal, synthetic-recovery-ui-encrypted-room-reopen, synthetic-backup-create-restore-localization-ui, synthetic-backup-ui-to-injected-create-adapter, native-reminder-language-and-digit-preferences, native-compose-plural-and-fallback, native-restore-recovery-transaction-and-reopen, native-fuel-edit-reopen-stale-conflict-and-undo, native-key-provision-and-missing-key-refusal, native-interrupted-stage-retains-original-and-cleans-stage, native-zero-space-preflight-retains-original, native-durable-migration-original-bytes-and-idempotent-reopen, native-authenticated-sha256-inventory-and-repair, native-redirected-storage-directory-rejected, native-tamper-refuses-plaintext-fallback, native-lost-key-retains-files-without-replacement, native-encrypted-image-display-and-crop-readers, native-export-copy-close-is-idempotent-and-removes-plaintext, native-cold-start-removes-interrupted-export-copy, native-legacy-and-recovery-migration-without-overwrite, native-inventory-revalidates-protected-recovery, native-encrypted-pdf-import-and-no-processing-scratch-leak, native-room4-to5-owner-preservation-and-future-format-refusal, native-backup-synthetic-vault-provision, native-backup-export-success, native-backup-zero-plaintext-leakage, native-backup-staged-validation-preview, native-backup-wrong-password-refusal, native-backup-tampered-payload-refusal, native-backup-tampered-asset-refusal, native-backup-transactional-restore-success, native-backup-collision-remap-reopen
```

This includes `native-reminder-language-and-digit-preferences` (the
English/Bangla notification title, text and channel assertions) and
`native-compose-plural-and-fallback`. The test code was not modified; the
instrumentation was run once.

## 8. Constraint confirmation

- **No OS-level locale change.** No `settings put`, `cmd locale`, or locale
  intent was used; `persist.sys.locale` remained unset throughout, and the only
  language change was the in-app Profile control, which is app state.
- **No OS night-mode or font-scale change.** `cmd uimode night` → `Night mode:
  no`; `settings get system font_scale` → `1.0`; no such settings were written.
- **No emulator boot/wipe/reset.** The already-running verified emulator was
  reused; nothing was erased.
- **No app uninstall and no `pm clear`** for any package. The probe app was only
  `am force-stop`ped/`am start`ed between sessions; its isolated temp DB is
  created per launch under the app cache and removed by the Activity on destroy.
- **Production app untouched.** `com.motovault.app` was never launched,
  installed, stopped or modified by this run; only
  `com.motovault.app.privacyprobe` and its test package were used.
- **No source edits and no commits.** Only files under
  `docs/roadmap/evidence/p08/localization/android/` were written.
- The probe APK hash in section 2 matches the Lead-provided rebuilt hash, so the
  captured UI is the P08-05 build under test.

## 9. Limitations

- This is synthetic emulator evidence with fixture seed data, not owner records
  and not a physical device; it covers both languages, both digit styles and
  the affected native notification/permission surfaces, as required for the
  device set, but the report/screenshot pairs are evidence only for this build.
- Screens were driven by coordinate taps located from uiautomator bounds; where
  a tap missed (e.g. a first attempt did not open the intended screen), it was
  retried and the final screenshots were validated individually. No unreachable
  state is claimed: every capture in the case table was reached and checked.
- The transient snackbar in the save-failure captures is time-sensitive; the
  inline error in the same screenshots is the durable localization evidence.
- The `failWrites` fixture fails all writes, so in-app settings could not be
  changed inside that session (see observation 8); that is a fixture behaviour,
  not an app localization gap.

## Appendix: artefact hashes

| File | Bytes | SHA-256 |
| --- | --- | --- |
| `p08-l10n-android-backup-sheet-bn.png` | 222754 | `8f2084aa165abe860212efb939d7a19cfbb2745c2aebe9611a0e466db574eef0` |
| `p08-l10n-android-backup-sheet-en.png` | 170090 | `f957b7db063334d3d79c65e218672ac98ddb3f4967e21a0cf6ece0f5460286ce` |
| `p08-l10n-android-document-add-sheet-bn.png` | 136911 | `3d11ccd826fd4526a520f1aa1bf1c26edee3b0ddc6b5777ab887de8e77b5d9a8` |
| `p08-l10n-android-document-add-sheet-en.png` | 120944 | `3d69dfa6821fae710f941bd9cf415a242b6ef88bba24e9903a0a5b2305b59906` |
| `p08-l10n-android-documents-bn.png` | 522720 | `cedfdfe1b606667dace72e7e1f552f0ea42d1ccc96bac247dc02e4803e973c3d` |
| `p08-l10n-android-documents-en.png` | 517676 | `283f6c5b4e22c1c6ba0be459d5d677b5aaf8ddaf852d472be65b889201cde903` |
| `p08-l10n-android-expense-sheet-bn.png` | 123061 | `cced846ac2429f9953b2454c24053aa299e1bb9ad6280fb5879a692dfe471630` |
| `p08-l10n-android-expense-sheet-en.png` | 116031 | `6466143c93f3971f4a370669fa2553f77799cd465447215681c424ee62279b9d` |
| `p08-l10n-android-fuel-save-error-bn.png` | 154023 | `cf84a2714ed5bf932ed075206d907049fe83a44ef36e4aa784ee678b5d4d371e` |
| `p08-l10n-android-fuel-save-error-en.png` | 130094 | `57635fc1f0f621ea6a0a5dcb548b3a90f4d540d82025884357dca1d66ba8efcd` |
| `p08-l10n-android-fuel-sheet-bn.png` | 156784 | `37a925757b4d05d735054bea418c9f23556543a3e145f52fdd55e62bc6675ad7` |
| `p08-l10n-android-fuel-sheet-en.png` | 139136 | `6b662d9e35eaee842cd165a356dfb8be5e31f2f284834ed95be0b08480009227` |
| `p08-l10n-android-garage-bn-switchback.png` | 238624 | `929a6ff87ffd32439e5011f3317e6dd5ef287ae5d09dcbe6084923701ef46694` |
| `p08-l10n-android-garage-bn.png` | 238624 | `929a6ff87ffd32439e5011f3317e6dd5ef287ae5d09dcbe6084923701ef46694` |
| `p08-l10n-android-garage-en.png` | 208994 | `82d59fd7cb76d5d425fd2453b3867546880c8784df6a9f2c3b7304ac69bc0c8e` |
| `p08-l10n-android-privacy-sheet-bn.png` | 224534 | `cd125d087f0a818285fe4e8274812845753763fb50a8832d51179bc551bb9296` |
| `p08-l10n-android-privacy-sheet-en.png` | 166353 | `4d73e62fd118474a09f89e9c48ed65c3c76c260a7fe72c24b7347f8addb18a1f` |
| `p08-l10n-android-profile-appearance-bn-switchback.png` | 197731 | `1ccf3e841ebaee80ac7bce8b1e983d53f8ea8c32ac9a533a64fbe36f87193df9` |
| `p08-l10n-android-profile-appearance-bn.png` | 197731 | `1ccf3e841ebaee80ac7bce8b1e983d53f8ea8c32ac9a533a64fbe36f87193df9` |
| `p08-l10n-android-profile-appearance-en.png` | 187194 | `fdf85429e5c96afc5b4d514fc9c3a5731a1417b345298f63dcdcff01c9b7eafe` |
| `p08-l10n-android-profile-notification-bn.png` | 201799 | `3d98f477aac310c65c76b2e82651d4bc4d603eef6683b33b61036ecebde0e9c8` |
| `p08-l10n-android-profile-notification-en.png` | 164420 | `a020d8e693660635f1f09824247347bd2ed809d065408ec65fefafa3c264daab` |
| `p08-l10n-android-quickadd-sheet-bn.png` | 159239 | `c60e3337cb00586f1bf2d65f244a1c3fea4ba7ab448c81abb8e5def67b55a057` |
| `p08-l10n-android-quickadd-sheet-en.png` | 146046 | `74023f21e237a143527debbd1e3d9a6756db4e89bced525e04be77d133e0a925` |
| `p08-l10n-android-records-bn.png` | 222546 | `68209b5ea6fe47971acfd57445040447c3d352f82238691878d00c399e5a49d2` |
| `p08-l10n-android-records-en.png` | 202269 | `7d8ab7b2e96d08221e9e6a897d66e727006e0466e55c1dd71341113a1fabbd25` |
| `p08-l10n-android-service-sheet-bn.png` | 171168 | `5fa46e5064d41b6fccc226afed211801559061e8b9f7ac037447f8b160c075f8` |
| `p08-l10n-android-service-sheet-en.png` | 157993 | `88c5a7057bfde31108d5817b58fa997a3edeb900ac0cc5db5836524e9ce00431` |
