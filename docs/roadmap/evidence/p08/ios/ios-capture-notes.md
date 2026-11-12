# P08-01 iOS design-matrix evidence — capture notes

Status: final. Date: 12 November 2026. Owner: `ios-runner` (shared task
`task-2`, claimed at revision 2). Scope: the iOS slice of the P08-01 design
matrix, captured on the verified already-booted iOS 27 simulator with the
isolated synthetic probe app. This is simulator evidence, not physical-iPhone,
cloud-staging, signed-release or publication evidence.

## What was produced

- **36 xcresult screenshots** from the final green XCTest run
  (`build/p08-ios-runs/final-20261112-225133`), copied to
  `docs/roadmap/evidence/p08/ios/xcresult/`.
- **2 h264 screen recordings**, kept separate from stills:
  `recordings/p08-ios-motion-normal.mov` (25.8 s) and
  `recordings/p08-ios-motion-reduced.mov` (32.8 s).
- **12 direct simulator-display captures** via `xcrun simctl io screenshot`:
  11 in `simctl/` plus the Me-bottom provenance image
  `p08-ios-profile-scroll-bottom-simctl.png` in this directory.
- Provenance files in `provenance/` (Lead-recorded identity plus the native
  storage-probe result reused from the same build).

Every still was opened and checked visually against its named state. The final
matrix run passed 8/8 tests; a green runner is not itself a visual assertion.

## Final run identity

| Fact | Value |
| --- | --- |
| Run id | `final-20261112-225133` |
| Run directory | `build/p08-ios-runs/final-20261112-225133/` (`png/`, `attachments/`, `xcodebuild.log`, `summary.json`, `run-info.txt`) |
| Result bundle | `$TMPDIR/MotoVault-Design-P08-01-iOS-final-20261112-225133.xcresult` (ephemeral; exported PNGs above are the durable record) |
| Result | Passed — 8 tests, 0 failures, 0 unexpected, 0 skipped; test time 193.5 s |
| Log | `xcodebuild.log` ends with `** TEST SUCCEEDED **` |
| Destination | `platform=iOS Simulator,id=50709DF0-FEA9-4B34-9DE6-6172D868875D` |
| Bundle id (build setting) | `MOTOVAULT_BUNDLE_ID=com.motovault.app.privacyprobe` |

Exact final command:

```bash
cd /Users/arif/workspace/bbn/motovault
export GRADLE_USER_HOME=$PWD/.gradle-home KONAN_DATA_DIR=$PWD/.konan \
  JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
xcodebuild test -project iosApp/MotoVault.xcodeproj -scheme MotoVault \
  -destination 'platform=iOS Simulator,id=50709DF0-FEA9-4B34-9DE6-6172D868875D' \
  -derivedDataPath /Users/arif/workspace/bbn/motovault/build/p08-derived \
  -only-testing:MotoVaultUITests/P08DesignMatrixTests \
  -test-timeouts-enabled YES \
  -default-test-execution-time-allowance 120 \
  -maximum-test-execution-time-allowance 300 \
  -parallel-testing-enabled NO \
  -collect-test-diagnostics never \
  -resultBundlePath "$TMPDIR/MotoVault-Design-P08-01-iOS-final-20261112-225133.xcresult" \
  MOTOVAULT_BUNDLE_ID=com.motovault.app.privacyprobe
```

Export:

```bash
xcrun xcresulttool export attachments \
  --path "$TMPDIR/MotoVault-Design-P08-01-iOS-final-20261112-225133.xcresult" \
  --output-path <run-dir>/attachments
# exported PNGs renamed to their `p08-ios-*` case names (see table)
```

## Environment identity

| Fact | Value |
| --- | --- |
| Simulator | `iPhone 18 Pro Max`, UDID `50709DF0-FEA9-4B34-9DE6-6172D868875D` (already booted; never booted, erased, reset or reconfigured by this work) |
| Device model identifier | `iPhone19,3` |
| Runtime | iOS 27.0 (27.0 - 24A434), `com.apple.CoreSimulator.SimRuntime.iOS-27-0` |
| Window geometry | 440 x 956 pt; screenshots 1320 x 2868 px (3x) |
| Xcode | Xcode 27.0, Build version 27A266a |
| Host | macOS 27.2.1 (26B131) |
| Test host bundle | `com.motovault.app.privacyprobe` (isolated synthetic probe) |

The production app `com.motovault.app` was never launched, installed over or
modified.

## Source and build identity

- Source revision: `e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4` with an
  uncommitted working tree. **This is not a clean-HEAD build.**
- Working-tree state at capture time (`git status --short`): modified
  `composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/ProbeFuelChecks.kt`,
  `.../ProbeRenewalChecks.kt`, `.../ProbeUiHelpers.kt`,
  `docs/roadmap/IMPLEMENTATION-PLAN.md`, `docs/roadmap/PROGRESS.md`,
  `docs/roadmap/roadmap.json`, `iosApp/MotoVault.xcodeproj/project.pbxproj`,
  `tools/tests/test_localization_resources.py`; untracked
  `docs/roadmap/evidence/P08-01-core-regression.md`,
  `docs/roadmap/evidence/P08-01-design.md`, `docs/roadmap/evidence/p08/`,
  `iosApp/MotoVaultUITests/P08DesignMatrixTests.swift`, `iosApp/scripts/`.
- Built products: `build/p08-derived/Build/Products/Debug-iphonesimulator/`.
  The installed app is the `xcodebuild test` build from that directory.
  `ComposeApp` is **statically linked into the app binary**; the app bundle has
  no `Frameworks/ComposeApp.framework` (the Kotlin framework artifact exists
  only in `composeApp/build/xcode-frameworks/Debug/iphonesimulator27.0/` as an
  intermediate and is not shipped separately in the installed bundle).
- Installed bundle container (after the final run):
  `/Users/arif/Library/Developer/CoreSimulator/Devices/50709DF0-FEA9-4B34-9DE6-6172D868875D/data/Containers/Bundle/Application/E82EC660-89FB-4018-9501-BBF3E54458DB/MotoVault.app`
- Installed data container:
  `/Users/arif/Library/Developer/CoreSimulator/Devices/50709DF0-FEA9-4B34-9DE6-6172D868875D/data/Containers/Data/Application/EA226EA8-A3A5-411A-B93F-669942D9F20A`
- Bundle identity: `com.motovault.app.privacyprobe`, `CFBundleVersion 1`,
  `CFBundleShortVersionString 1.0.0`.
- Binary sha256:
  - `MotoVault` (thin app binary): `5acfc7adafa9cb54c2c88ce2439d65a5bb88f36b5a24d1639970d4e39acab9ec`
  - `MotoVault.debug.dylib` (debug payload carrying linked ComposeApp code):
    `cb3931712665c643a04b2059f06f06193e3b1ca551ac3d93691134b2c4daa656`
  - `PlugIns/CaptureShare.appex/CaptureShare`:
    `ef4d4d22305d3293ca763084071f60367ce90cf74f182c125431f5a446d08c0e`
  - The built-product copies of `MotoVault` and `CaptureShare` have the same
    hashes as the installed bundle.

## Direct simulator-display captures (`simctl/`)

These are `xcrun simctl io <udid> screenshot` PNGs (not xcresult attachments).
Each launch used `--seed` on the isolated probe (synthetic `VaultState.seedData()`
only). Unless stated otherwise the appearance is the seeded default: in-app
theme **System default rendered light** (OS appearance was never changed),
Bangla UI, Bengali digits, text scale 1.0, Yamaha accent.

| File | Surface | State |
| --- | --- | --- |
| `simctl/p08-ios-tab-garage-light-bn.png` | Garage | BN, BN digits, light, 1.0, Yamaha |
| `simctl/p08-ios-tab-vault-light-bn.png` | Documents | same |
| `simctl/p08-ios-tab-history-light-bn.png` | Records | same |
| `simctl/p08-ios-tab-profile-light-bn.png` | Profile + Appearance card | same |
| `simctl/p08-ios-sheet-quickadd.png` | Quick add | same |
| `simctl/p08-ios-sheet-fuel.png` | Fuel entry | same |
| `simctl/p08-ios-sheet-service.png` | Service entry | same |
| `simctl/p08-ios-sheet-document-scan.png` | Document add/scan (docAdd) | same |
| `simctl/p08-ios-sheet-backup.png` | Backup & restore | same |
| `simctl/p08-ios-sheet-privacy.png` | Privacy & security | same |
| `simctl/p08-ios-sheet-expense.png` | Expense (history) | same |
| `p08-ios-profile-scroll-bottom-simctl.png` | Me scrolled via `--scroll-bottom` (provenance) | BN, BN digits, light, 1.0, Yamaha |

## xcresult screenshot matrix (36)

All files are in `docs/roadmap/evidence/p08/ios/xcresult/`. "Light"/"Dark" mean
the in-app theme control was set explicitly; "System default" means the seeded
default was left in place (rendered light because the host appearance was light
and was not changed). Accent is the in-app brand-colour override (Yamaha unless
stated).

### Tabs, theme, language, digits, text scale

| File | Tab | Language | Digits | Theme | Text scale | Accent | Verified |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `p08-ios-tab-garage-light-bn.png` | Garage | BN | BN | Light (in-app) | 1.0 | Yamaha | yes |
| `p08-ios-tab-vault-light-bn.png` | Documents | BN | BN | Light | 1.0 | Yamaha | yes |
| `p08-ios-tab-history-light-bn.png` | Records | BN | BN | Light | 1.0 | Yamaha | yes |
| `p08-ios-tab-profile-light-bn.png` | Profile | BN | BN | Light | 1.0 | Yamaha | yes |
| `p08-ios-tab-garage-dark-bn.png` | Garage | BN | BN | Dark | 1.0 | Yamaha | yes |
| `p08-ios-tab-vault-dark-bn.png` | Documents | BN | BN | Dark | 1.0 | Yamaha | yes |
| `p08-ios-tab-history-dark-bn.png` | Records | BN | BN | Dark | 1.0 | Yamaha | yes |
| `p08-ios-tab-profile-dark-bn.png` | Profile | BN | BN | Dark | 1.0 | Yamaha | yes |
| `p08-ios-tab-profile-light-en.png` | Profile | EN | BN | System default | 1.0 | Yamaha | yes — English copy, language segment EN |
| `p08-ios-tab-profile-light-en-digits.png` | Profile | EN | EN | System default | 1.0 | Yamaha | yes — "123 English" selected, Latin digits |
| `p08-ios-tab-garage-light-en-digits.png` | Garage | EN | EN | System default | 1.0 | Yamaha | yes (see defect 3: next-up title still Bangla) |
| `p08-ios-tab-profile-light-bn-en-digits.png` | Profile | BN | EN | System default | 1.0 | Yamaha | yes — BN copy, Latin digits |
| `p08-ios-tab-garage-light-bn-en-digits.png` | Garage | BN | EN | System default | 1.0 | Yamaha | yes |
| `p08-ios-tab-profile-light-bn-scale13.png` | Profile | BN | BN | System default | 1.3 | Yamaha | yes — "সবচেয়ে বড়" selected |
| `p08-ios-tab-garage-light-bn-scale13.png` | Garage | BN | BN | System default | 1.3 | Yamaha | yes — no lost actions; labels wrap |
| `p08-ios-tab-profile-light-en-scale13.png` | Profile | EN | BN | System default | 1.3 | Yamaha | yes — "Extra large" selected |
| `p08-ios-tab-garage-light-en-scale13.png` | Garage | EN | BN | System default | 1.3 | Yamaha | yes |

### Appearance section, nine swatches, extreme accents

| File | Surface | State | Verified |
| --- | --- | --- | --- |
| `p08-ios-profile-swatches-start-light.png` | Profile Appearance | Light, swatch row at rest: first eight swatches fully visible, ninth clipped by card edge | yes |
| `p08-ios-profile-swatches-end-light.png` | Profile Appearance | Light, row scrolled to end: ninth (Royal Enfield) fully visible, first clipped; together the two captures show all nine | yes |
| `p08-ios-profile-appearance-ktm-light.png` | Profile Appearance | Light, KTM swatch selected, subtitle "KTM কালার", KTM accent on nav | yes |
| `p08-ios-profile-appearance-enfield-light.png` | Profile Appearance | Light, Royal Enfield swatch selected, subtitle "Royal Enfield কালার", Enfield accent on nav | yes |
| `p08-ios-garage-accent-ktm-light.png` | Garage | Light, KTM accent (#AA4700) on CTA/quick actions/chart/nav | yes |
| `p08-ios-garage-accent-ktm-dark.png` | Garage | Dark, KTM accent (#FF7900) | yes |
| `p08-ios-garage-accent-enfield-light.png` | Garage | Light, Royal Enfield accent (#805E28 brown) | yes |
| `p08-ios-garage-accent-enfield-dark.png` | Garage | Dark, Royal Enfield accent (#DBB77C tan) | yes |

### Sheets and preferences

| File | Surface | State | Verified |
| --- | --- | --- | --- |
| `p08-ios-sheet-quickadd.png` | Quick add sheet | Four entry cards + paste/share row | yes |
| `p08-ios-sheet-fuel.png` | Fuel sheet | Amount hero, per-litre chip, mode/date chips, checkboxes, disabled action with reason, in-app keypad | yes |
| `p08-ios-sheet-service.png` | Service sheet | Suggestions, item/date chips, odometer, workshop, total, disabled action | yes |
| `p08-ios-sheet-document-scan.png` | Document add sheet | Five document types with scan/manual entry affordances | yes |
| `p08-ios-sheet-backup.png` | Backup sheet | Encrypted-backup explainer, contents, password fields, warning | yes |
| `p08-ios-sheet-privacy.png` | Privacy sheet | Selected-document review, app lock action, encryption explainer, warning | yes |
| `p08-ios-sheet-expense.png` | Expense sheet | Categories, amount chip, date, note, validation, keypad (copy defect 1 visible) | yes |
| `p08-ios-profile-bottom-bn.png` | Profile scrolled to bottom | Privacy/backup/demo cards visible (`--scroll-bottom` provenance; demo features present) | yes |
| `p08-ios-profile-preferences-bn.png` | Profile Preferences | হ্যাপটিক ভাইব্রেশন ON, মোশন কমান OFF, অটো লোকেশন OFF + reminder block | yes |
| `p08-ios-profile-preferences-reduce-motion-bn.png` | Profile Preferences | মোশন কমান switch visibly blue/ON after in-app tap | yes |
| `p08-ios-motion-reduced-enabled-check.png` | Profile Preferences (motion run) | Second evidence that the in-app Reduce motion switch is ON | yes |

## Motion recordings

Recorded with `xcrun simctl io <udid> recordVideo --codec=h264 --force <out.mov>`
around single-method runs; stopped with SIGINT. Stills and motion are kept
separate: a still cannot establish motion.

| File | Duration | Size | sha256 | Content |
| --- | --- | --- | --- | --- |
| `recordings/p08-ios-motion-normal.mov` | 25.8 s | 19,883,051 B | `7ce6a663366ff6e55166e69c596e9dd7388072f05d666ce22e5ba8a55564ec9d` | `testMotionSequenceNormal`: seeded Garage, tab switches across all four tabs, quick-add sheet opens and closes, normal spring/timing |
| `recordings/p08-ios-motion-reduced.mov` | 32.8 s | 15,658,118 B | `aafed5e57041e2d01ac73818e71dc1b2b20b11c5bf2fbc200dec2ceab149aa35` | `testMotionSequenceReduced`: in-app Reduce motion enabled (ON state proven by the two stills above), then the same tab/sheet sequence with reduced motion |

The clips show that the sequence executes with and without the in-app Reduce
motion setting. They are **not** a measured timing/parity comparison with the v3
prototype or with physical devices; no frame-by-frame or measured-parity claim
is made.

## Observed defects and observations

1. **Duplicate word in the empty expense amount chip (copy defect, Bangla).**
   The expense sheet renders `টাকা: টাকা দেওয়া হয়নি`. Cause:
   `cost_field_value = "%1$s: %2$s"` with label `cost_amount = "টাকা"` and
   placeholder `cost_empty_amount = "টাকা দেওয়া হয়নি"` in
   `composeApp/src/commonMain/kotlin/com/motovault/app/ui/history/ExpenseSheet.kt:56`
   and `composeResources/values-bn/costs.xml`. The same format with the
   English resources would read "Amount: No amount" (mildly redundant; the
   English expense sheet itself was not captured in this matrix). Suggested fix: drop the value label
   or use a non-duplicating placeholder. Evidence:
   `xcresult/p08-ios-sheet-expense.png`.
2. **Nine brand-colour swatches are unlabeled buttons, and the ninth is clipped
   at rest (accessibility + layout defect).**
   The exported XCTest tree for the Profile capture contains 41 buttons, exactly
   9 of which have no label — the nine swatches (`SemanticsNode id=39..47`).
   Their frames are 34 x 34 pt at x = 34, 78, … 386; the ninth is reported as
   `{386, 382.7}, {20, 34}`, i.e. only 20 of 34 pt are on screen until the row is
   scrolled horizontally. There is no visible scroll affordance; the only cue is
   the clipped circle. Suggested fix: a per-brand `contentDescription` and a
   visible scroll/fade affordance. Evidence:
   `xcresult/p08-ios-tab-profile-light-bn.png`,
   `xcresult/p08-ios-profile-swatches-start-light.png`,
   `build/p08-ios-runs/final-20261112-225133/attachments/` tree diagnostics.
3. **Demo/seed Bangla label leaks into the English UI.** In the English Garage
   capture the surrounding copy is English ("Due now", "320 km overdue", "Log
   service", "Fuel average") but the next-up title remains `চেইন`.
   `VaultCreation.seedData` stores `ComponentRule` names as Bengali display text
   (`r-chain = "চেইন পরিষ্কার ও লুব্রিকেন্ট"`) and Garage renders the rule name
   directly. This is the stable-data/localized-display rule applied to demo
   data; flag for P08-05 copy/release review. Evidence:
   `xcresult/p08-ios-tab-garage-light-en-digits.png`,
   `xcresult/p08-ios-tab-garage-light-en-scale13.png`.
4. **Dark-mode warning-badge contrast is visibly lower than light mode**
   (dark-red expiry badge text on the dark maroon surface). This is a visual
   observation only; no contrast ratio was measured and no WCAG failure is
   claimed here. Evidence: `xcresult/p08-ios-tab-vault-dark-bn.png`.
5. **Count-up/spring values can be captured mid-animation.** Garage stills show
   varying intermediate odometer/efficiency values (e.g. `১২,৮২৫`, `১১,৮২২`,
   `১০,৬৬০` against the seeded `১২,৩৪০`) because the XCTest screenshot is taken
   immediately after navigation while the count-up spring is still settling.
   The named matrix state is correct; this is a capture artifact, not a data
   change, and is one reason the motion clips matter. Pixel-exact numeric stills
   would need a post-navigation settle delay.
6. **Demo affordances remain in the Profile.** The Me bottom capture shows
   `নতুন ফিচার (ডেমো)` with share/paste and widget demo rows
   (`xcresult/p08-ios-profile-bottom-bn.png`); the About section below that
   capture (seen in the accessibility tree) contains `ডেমো ডেটা রিসেট করুন`.
   Not a defect for the probe/demo build, but relevant to what a release build
   exposes.
7. **No truncation/overlap/clipping of actions was found at text scale 1.3** in
   the captured Garage and Profile states; all four bottom-nav labels and the
   primary actions remained visible. The odometer hero wraps its unit below the
   number at 1.3, which is expected wrapping rather than clipping.
8. **Scan entry is inside the document-add sheet**, not surfaced on the
   Documents tab itself; the tab only offers the round `+`. Observation for the
   entry-point review, not a rendering defect.
9. **Fuel/service sheets show honest disabled primary actions** in the empty
   seeded state (reason text `টাকার পরিমাণ লিখুন` / `কী কী কাজ হয়েছে বেছে নিন`);
   no clipping of the pinned footer/keypad was found.

## Accessibility notes

The debug build enables Compose accessibility sync
(`AccessibilitySyncOptions.Always`), and the exported XCTest trees show semantic
labels for most controls with real frames only for nodes inside the visible
scroll viewport (off-screen nodes are present with zero-size frames). Visible
controls in the captured states are labelled (theme options, language/digits/
text-size segments, category/date chips, tab bar `গ্যারেজ/ডকুমেন্ট/রেকর্ড/প্রোফাইল`,
quick add `নতুন রেকর্ড যোগ করুন`, settings rows including
`মোশন কমান`/`অটো লোকেশন`). The exception is the nine swatches (defect 2).

This is tree inspection, **not** a VoiceOver spoken-announcement pass; no
spoken/focus-order evidence is claimed.

## Honest gaps

- Simulator only. No physical iPhone, real camera/locked-device, real
  notification-permission prompt, cloud staging, signed release or publication
  evidence is claimed.
- The matrix is representative, not exhaustive: the four tabs are covered in
  light and dark; sheets are captured in the light/system-default appearance
  only; dark-mode sheets, system-dark-from-OS, and per-sheet 1.3-scale states
  were not captured. Shared components are not exhaustively contrast-checked.
- Motion: two clips only. No measured timing, parity or reduced-motion
  frame-by-frame comparison; the in-app Reduce motion ON state is proven by the
  stills, not by pixel analysis of the clip.
- VoiceOver spoken pass, focus order and native back-navigation are not covered.
- The build is an uncommitted working-tree build; it must not be reported as the
  clean `e6c59bf` revision.
- The probe app's `--seed` path resets its isolated vault to synthetic
  `VaultState.seedData()`. No uninstall, erase or data clearing was performed;
  the production bundle was never touched.

## Reproduction note (test harness)

`iosApp/MotoVaultUITests/P08DesignMatrixTests.swift` drives the matrix with
normalized coordinate taps (Compose exposes only limited semantics; swatches are
unlabeled) and label-queried taps for the settings rows once they are inside the
scroll viewport. The reduce-motion step waits for a stable row frame after a
low-velocity scroll pan before tapping, then asserts the `Selected` trait; this
was required because a fast pan left a fling running and the next tap was spent
stopping it. The test file is registered in `project.pbxproj` with build/file ids
`C90000000000000000000002` / `C90000000000000000000001` (four additive entries).

The `ios-runner` session could build but not launch UI tests itself: its host
file sandbox denies PTY allocation (`os.open("/dev/ptmx")` → EPERM, `openpty()`
→ "out of pty devices"), so Xcode's UI-test launcher failed with
`IDEPseudoTerminalDomain Code 7`. The final run was executed by the Team Lead
from a session with the required access; the failed attempts changed no
simulator state and produced no evidence. The default `~/Library` DerivedData
path is likewise unwritable from the delegated session, hence
`-derivedDataPath build/p08-derived` in the reproduction command.
