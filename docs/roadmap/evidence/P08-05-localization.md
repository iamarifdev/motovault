# P08-05 localization implementation and verification

Status: implementation, static gates and the Android/iOS runtime language-switch
evidence are complete. The task stays **in review** because the Bangla/English
copy changes still need a **named human reviewer** (see
[P08-05-review.md](P08-05-review.md) for the exact list). Nothing was committed.

This slice closes the localization debt left by P04A-08 and the direct-literal
baseline that P04A-08 scheduled for P08-05.

## Changes in this slice

- **Expanded literal baseline closed.** Nine former copy-debt compositions were
  migrated to shared format resources: `wallet_next_line`,
  `schedule_source_line`, `schedule_applicability_value`,
  `schedule_interval_summary`, `schedule_odometer_confirmed`,
  `odometer_bump_chip`, `history_discount_amount`, `service_odometer_value`, and
  the existing `onboarding_step` format. The remaining nine occurrences are
  data-only glyphs (back chevron, add/remove, percent, unknown `?`, decimal
  point, check marks). `tools/ui-literal-baseline.json` now records an explicit
  `kind` and, for `kind: data`, a written `justification` per entry;
  `tools/ui_literal_gate.py` validates that requirement, and the fixture test
  covers both `copy` (needs removal task) and `data` (needs justification).
- **Native UI error copy localized.** Android document capture/picker/viewer and
  iOS capture/picker/camera/passport error strings that reach the screen now
  resolve through shared resources (`capture_*`, `pick_*`, `passport_*`) or a
  localized display boundary. Raw exception text is no longer shown for the
  passport export/verify paths and the document-import path.
- **Copy defects fixed** (from the P08-01 design matrix): Bangla expense empty
  amount no longer repeats the word (`টাকা: দেওয়া হয়নি`); English quick-add
  reads "Add by typing or pasting" / "From a mechanic, bKash or Nagad message".
- **No persistence change.** No new translated display text is persisted;
  catalogue/domain identity still uses stable IDs and typed presentation. The
  compatibility fixtures and `P00CompatibilityTest` pass.

## Static verification (final run, this working tree)

| Gate | Result |
| --- | --- |
| `tools/check_agent_docs.py` | pass |
| `tools/ui_literal_gate.py` | pass — 9 occurrences, all `kind: data` with justification |
| `tools/ui_literal_gate.py --fuel` | pass (empty baseline) |
| `tools/bangla_copy_lint.py` | pass (15 regression phrases) |
| `tools/responsibility_gate.py` | pass — only the 2 test-support baseline files |
| `tools/architecture_dependencies.py` | pass (0 violations) |
| `tools/architecture_package_cycles.py` | pass (no cycles) |
| `tools/fuel_policy_literals.py` | pass |
| `python3 -m unittest discover -s tools/tests -p 'test_*.py'` | pass |
| `./gradlew :composeApp:testDebugUnitTest --offline` | BUILD SUCCESSFUL; 327 tests, 0 failures/errors/skipped |
| `./gradlew :composeApp:compileKotlinIosSimulatorArm64 --offline` | pass |
| Android `assemblePrivacyProbe` + `assemblePrivacyProbeAndroidTest` | pass (final probe APK SHA-256 `b903f93338d1120826a15ee6264846a2934e36397d62b1fd18fdfd6c06e51756`) |
| `tools/roadmap.py check` | pass |

Environment/build identity: source `e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4`
plus the uncommitted working tree (not a clean HEAD). Device identity, exact
commands, case tables and artifact hashes are in the platform notes below.

## Runtime language-switch evidence

- **Android emulator** (Pixel_10, Android 17/API 37, isolated synthetic
  privacy-probe app): same-session BN→EN and EN→BN switches without relaunch,
  four tabs, seven sheets, notification-permission card and a localized save
  failure in both languages; native probe PASS 37 storage checks including
  `native-reminder-language-and-digit-preferences` (EN/BN notification title,
  text, channel) and `native-compose-plural-and-fallback`.
  [Android notes](p08/localization/android/android-localization-notes.md),
  [native probe output](p08/localization/android/android-native-probe.txt),
  [final-build spot check](p08/localization/android/final-build-check/README.md).
- **iOS 27 simulator** (iPhone 18 Pro Max, same isolated app): `P08LocalizationTests`
  3/3 passed with 23 reviewed screenshots — in-app BN→EN→BN switching without
  restart, four tabs, seven sheets and the notification-permission card;
  assertion that the previous language's tab labels disappear from the
  accessibility tree. Native storage probe PASS 22 checks including the
  EN/BN reminder localisation and plural/fallback checks.
  [iOS notes](p08/localization/ios/ios-localization-notes.md),
  [iOS provenance](p08/localization/ios/provenance/ios-provenance.txt).

Motion is not claimed for this task; the switch is a static UI change verified by
stills and accessibility assertions.

## Demo, fixture and identifier data (not UI copy)

`VaultCreation.seedData()` still contains Bangla display strings for its demo
records (component names such as `চেইন`, document titles), `ui/Catalog.kt` holds
input aliases, and storage uses stable IDs/codes. These are **data**, are not
resolved as localized UI copy, and remain owner-editable values. The English UI
therefore shows Bangla demo labels in seeded demo states; this is documented in
[P08-05-review.md](P08-05-review.md) rather than counted as localized copy.


## Recorded working-tree identity

Commands run in the repository root while finalising this report (per-artifact
SHA-256 values are in the platform notes):

```bash
git rev-parse HEAD
# e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4
git diff HEAD | shasum -a 256
# 74061b3a50029e266a13b89a4b67cb69cc4c0b85f56e5d22365b2191aeaec86f
git ls-files --others --exclude-standard -z | xargs -0 shasum -a 256 | shasum -a 256
# dfae854d01a2ec2e62639c5ad765e34c333e5576c5a87ed74a1fa54c37006b94
```

Final platform builds observed in this slice: iOS probe app binary
`c29ee1921470962c0bf2c143f62b8e26df7788c515697025f6898a7c5151424d` (iOS 27
iPhone 18 Pro Max simulator) and Android probe APK
`b903f93338d1120826a15ee6264846a2934e36397d62b1fd18fdfd6c06e51756` (Pixel_10
emulator; the full capture matrix used the immediately preceding
`a0ee2636…` build whose visible screens differ only by the later native
error-path copy, verified by the final-build spot check). Nothing was committed.

## Limits and open items

- **Named human copy review is outstanding.** The changed/new strings are listed
  in the review document; this agent review is not a native-speaker sign-off.
- The lexical literal gate does not parse Kotlin, does not see indirect strings,
  `when` branches, numeric literals or every native string; the platform notes
  list what was and was not inspected.
- System-owned chrome (permission alerts, document picker, share sheet) follows
  the OS locale, not the in-app language; that is expected and was not switched.
- Physical-device runtime remains P08-02; this task is simulator/emulator
  evidence only.
