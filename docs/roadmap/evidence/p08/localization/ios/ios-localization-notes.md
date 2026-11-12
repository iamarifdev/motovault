# P08-05 iOS runtime language-switch evidence

Date: 12 November 2026 (Asia/Dhaka). Scope: the iOS slice of the P08-05 runtime
language-switch verification. Simulator evidence only; not physical-iPhone,
signed-release or publication evidence.

## Environment and build identity

| Fact | Value |
| --- | --- |
| Simulator | `iPhone 18 Pro Max`, UDID `50709DF0-FEA9-4B34-9DE6-6172D868875D`, iOS 27.0 (24A434), already booted and reused |
| Xcode | 27.0 (27A266a) |
| App | isolated synthetic `com.motovault.app.privacyprobe`, bundle version 1 / 1.0.0 |
| App container | `…/Bundle/Application/407519E1-DA4B-4E1C-9B8C-3C080216FFDF/MotoVault.app` |
| `MotoVault` binary SHA-256 | `c29ee1921470962c0bf2c143f62b8e26df7788c515697025f6898a7c5151424d` |
| `CaptureShare.appex` SHA-256 | `ef4d4d22305d3293ca763084071f60367ce90cf74f182c125431f5a446d08c0e` |
| Source | `e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4` + uncommitted P08-05/P08-01 working tree (**not** a clean HEAD build) |
| Test class | `MotoVaultUITests/P08LocalizationTests` (new P08-05 test file, registered in the Xcode project) |

Exact command (result bundle path made unique per run):

```bash
xcodebuild test -project iosApp/MotoVault.xcodeproj -scheme MotoVault \
  -destination 'platform=iOS Simulator,id=50709DF0-FEA9-4B34-9DE6-6172D868875D' \
  -derivedDataPath "$PWD/build/p08-derived" \
  -only-testing:MotoVaultUITests/P08LocalizationTests \
  -test-timeouts-enabled YES -default-test-execution-time-allowance 300 \
  -maximum-test-execution-time-allowance 600 -parallel-testing-enabled NO \
  -collect-test-diagnostics never \
  -resultBundlePath "$TMPDIR/MotoVault-L10n-P08-05-iOS-<unique>.xcresult" \
  MOTOVAULT_BUNDLE_ID=com.motovault.app.privacyprobe
```

Final run: `build/p08-l10n-runs/l10n-ios3-20261112-231753` —
**3/3 tests passed**, 48 attachments exported (23 PNGs + diagnostic trees),
`** TEST SUCCEEDED **`. The PNGs are retained at
[../../../../docs/roadmap/evidence/p08/localization/ios/](../../../../docs/roadmap/evidence/p08/localization/ios/)
and build/probe provenance is in
[provenance/ios-provenance.txt](provenance/ios-provenance.txt).

## Method

Each launch seeds `VaultState.seedData()` in the isolated probe (Bangla default,
Bengali digits). Language is changed only through **Profile -> Appearance ->
English / বাংলা**; no relaunch is used for the switch itself. Sheet cases use one
launch per case so a sheet cannot leak into the next state; each case switches
language first, opens the real sheet through its in-app entry point, and captures
one screenshot plus the accessibility tree. Every PNG was opened and matched to
its named state.

Assertions beyond the images: after switching to English the
Bangla tab labels are absent from the accessibility tree
(`app.buttons["গ্যারেজ"].exists == false`), and after switching back the English
labels are absent. The run also re-executed the native storage probe on the same
build: **PASS 22 native iOS storage checks**, including
`native-reminder-language-and-digit-preferences` (EN/BN reminder title, message
and digits) and `native-compose-plural-and-fallback`; result retained in
[provenance/ios-storage-probe.txt](provenance/ios-storage-probe.txt)
(`syntheticKeychainStatus: -25300`; synthetic isolated key service, not a real
keychain pass).

## Captured cases

| File | State |
| --- | --- |
| `p08-ios-l10n-profile-bn.png` | Profile top, Bangla baseline |
| `p08-ios-l10n-profile-en.png` | Profile top immediately after in-app English switch (no relaunch) |
| `p08-ios-l10n-garage-en.png`, `documents-en`, `records-en`, `profile-en` | Four tabs in English after the same-session switch |
| `p08-ios-l10n-profile-bn-after-switchback.png`, `garage-bn-after-switchback.png` | Switch back to Bangla in the same session |
| `p08-ios-l10n-quickadd-bn/en` | Quick-add sheet |
| `p08-ios-l10n-fuel-bn/en` | Fuel sheet |
| `p08-ios-l10n-service-bn/en` | Service sheet |
| `p08-ios-l10n-document-add-bn/en` | Document add/type sheet |
| `p08-ios-l10n-expense-bn/en` | Expense sheet (empty amount state) |
| `p08-ios-l10n-privacy-bn/en` | Privacy settings sheet |
| `p08-ios-l10n-backup-bn/en` | Backup & restore sheet |
| `p08-ios-l10n-notification-permission-bn/en` | In-app notification-permission card (`নোটিফিকেশন চালু করুন` / `Allow phone notifications`) |

## Observations and defects

- Switching is instant in both directions with no restart; tab labels, sheet
  titles, bodies, action labels, keypad hints and error copy all changed.
- The fixed copy is visible: the Bangla expense empty amount now reads
  `টাকা: দেওয়া হয়নি` (no duplicated word) and English quick-add reads
  "Add by typing or pasting" / "From a mechanic, bKash or Nagad message".
- Seeded demo data still shows Bangla display names in the English UI
  (`চেইন` in the Garage due card; Bangla document titles in Documents). These are
  `VaultCreation.seedData()` demo labels documented as data, not localized copy;
  they are excluded from the language-switch claim (see the P08-05 review).
- The expense date field renders `2026-11-12` in Latin digits while the selected
  number style is Bengali. This is retained as an observation for the release
  review; it was already recorded in the P08-01 design matrix.
- System-owned chrome (permission alert text, document picker, share sheet) is
  OS-language chrome and was not switched by the in-app language change; the
  app-owned permission explanation is localized (captured above).
- No clipped or overlapping copy was observed in the captured 1.0-scale states.

## Limits

- iOS simulator only; physical-iPhone behaviour remains P08-02.
- Representative screens/sheets, not every screen at every digit/text scale.
- The OS locale was not changed (that would reconfigure the simulator and is not
  authorized), so the system permission dialog language is not exercised here.
- The Bangla/English copy still needs a **named human review**; this evidence
  records the agent's visual/copy review, not a native-speaker sign-off.
