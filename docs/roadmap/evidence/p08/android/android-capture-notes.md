# P08-01 Android design-matrix evidence

Captured 12 November 2026 on the already-booted Pixel_10 emulator `emulator-5554`
(Android 17 / API 37, arm64) using the isolated synthetic privacy-probe app only.
This is a representative screenshot/recording matrix for the P08-01 design review,
not a claim of pixel or motion parity with the v3 prototype. All states were driven
with `adb` shell commands (UI Automation dumps, `input tap/swipe`, `screencap`,
`screenrecord`); no Android source, test, Gradle file or tracker file was changed.

**Claim:** shared task `task-1`, claimed by `android-runner` at revision 2
(`team_task_update action:"claim"`).

## Environment and build identity

| Item | Value |
| --- | --- |
| Device | `emulator-5554`, Pixel_10 AVD (`ro.product.name` = `sdk_gphone16k_arm64`, model `sdk_gphone16k_arm64`) |
| OS | Android 17, `ro.build.version.sdk` = 37, `ro.build.version.release` = 17 |
| Build fingerprint | `google/sdk_gphone16k_arm64/emu64a16k:17/CP31.260623.012/16064790:user/dev-keys` |
| Display | 1080x2424 px @ 420 dpi (portrait) |
| Synthetic host | `com.motovault.app.privacyprobe` (`com.motovault.app.FuelEditFixtureActivity`), `VaultState.seedData()`, isolated temp DB |
| Probe APK path | `/data/app/~~LUw40sQkSYzInEy0b1n6Mg==/com.motovault.app.privacyprobe-4TM0youBOQka2xFTcwiFcQ==/base.apk` |
| Probe APK SHA-256 | `73a3ce93f164fc890a50523abca20c43e6719f2140e5716e88704a4444812479` |
| Probe test APK SHA-256 | `7f7c82fbae147c414896f928f57a9629282ed007f418f3c9de98c076701c7713` |
| Source revision | `e6c59bf06b03fc54f32ab6ad86b2117a17ac53d4` (working tree is not clean; see below) |
| Capture date | 2026-11-12, ~22:10–22:25 Asia/Dhaka |

`git status --short` at capture time (the test-file/tracker modifications pre-date
this capture session; the only additions made here are under
`docs/roadmap/evidence/p08/`):

```
 M composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/ProbeFuelChecks.kt
 M composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/ProbeRenewalChecks.kt
 M composeApp/src/androidInstrumentedTest/kotlin/com/motovault/app/storage/security/ProbeUiHelpers.kt
 M docs/roadmap/IMPLEMENTATION-PLAN.md
 M docs/roadmap/PROGRESS.md
 M docs/roadmap/roadmap.json
 M iosApp/MotoVault.xcodeproj/project.pbxproj
 M tools/tests/test_localization_resources.py
?? docs/roadmap/evidence/P08-01-core-regression.md
?? docs/roadmap/evidence/p08/
?? iosApp/MotoVaultUITests/P08DesignMatrixTests.swift
?? iosApp/scripts/
```

This is an uncommitted-build capture: the APKs already installed on the emulator
were built before this session; they are not labelled as the clean HEAD revision.

## Method

The sanctioned harness was launched before each state family:

```bash
adb -s emulator-5554 shell am force-stop com.motovault.app.privacyprobe
adb -s emulator-5554 shell am start -n com.motovault.app.privacyprobe/com.motovault.app.FuelEditFixtureActivity \
  --es language <bn|en> --es digits <bn|en>
```

Observed harness behaviour (important for reading the table):

- Every force-stop/relaunch starts on the **Records** tab with **System default**
  app theme, the **bike-brand** accent and **Normal** text size; language and
  digit style come from the intent extras. Settings set in-app do **not** survive
  a relaunch of this synthetic host.
- Therefore tab/theme/accent/text-scale/reduced-motion states were all set with the
  **in-app controls** (Profile tab) inside one launch, as required, and the app was
  relaunched only to change language/digit style (or to reset accent/theme).
- The in-app language and number-style controls were also exercised (see
  `p08-android-profile-inapp-switch-en-en-light.png`); the switch is instant, and the
  resulting Garage render was byte-identical to the launch-argument EN/EN Garage
  capture (`2047b361…`, see manifest).

Capture primitives (full helper with the raw commands:
[`capture-helpers.sh`](capture-helpers.sh)):

```bash
adb -s emulator-5554 exec-out screencap -p > <file>.png
adb -s emulator-5554 shell uiautomator dump /sdcard/ui.xml
adb -s emulator-5554 exec-out cat /sdcard/ui.xml     # locate text/content-desc + bounds
adb -s emulator-5554 shell input tap <x> <y>          # centre of the node's bounds
adb -s emulator-5554 shell input swipe <x1> <y1> <x2> <y2> <ms>
adb -s emulator-5554 shell input keyevent KEYCODE_BACK
```

Representative exact sequences (labels are the visible BN/EN strings; the helper
prints the tap points it resolves from the accessibility tree):

```bash
# Four tabs, one launch: Records is initial; tap nav labels.
input tap 540 2210            # quick-add FAB (content-desc "নতুন রেকর্ড যোগ করুন")
tap গ্যারেজ / ডকুমেন্ট / রেকর্ড / প্রোফাইল        (EN: Garage / Documents / Records / Profile)

# Appearance section: scroll swatch row horizontally at y=1090 to reach swatch 9.
input swipe 900 1090 200 1090 400     # row start -> end (swatches 2..9)
input swipe 200 1090 950 1090 400     # row end -> start (swatches 1..8)
input tap 124 1085                     # first swatch, KTM orange
input tap 904 1085                     # last swatch, Royal Enfield brown
# Dark theme: Profile -> tap "ডার্ক" / "Dark" (in-app segmented control only).

# Text scale: Profile -> "সবচেয়ে বড়" / "Extra large"; reset with "ডিফল্ট" / "Normal".

# Sheets:
#   quick add  -> FAB 540 2210
#   fuel       -> quick add card "ফুয়েল" (Fuel fill)
#   service    -> Garage "সার্ভিস যোগ করুন" (Log service)
#   expense    -> Records "খরচ যোগ করুন" (Add expense)
#   document   -> Documents "+" (content-desc "কাগজ যোগ করুন"), then "কোন ডকুমেন্ট?"
#   backup     -> Profile -> scroll to "ব্যাকআপ ও রিস্টোর" -> "ব্যাকআপ তৈরি"
#   privacy    -> Profile -> "প্রাইভেসি সেটিংস"

# Reduced motion: Profile -> scroll to "পছন্দ ও সতর্কতা" -> toggle "মোশন কমান".
```

Motion recordings (separate from the stills, driven in the same light/BN/BN
default-accent, Normal-text state; add/close of the quick-add sheet plus tab
switches):

```bash
adb -s emulator-5554 shell screenrecord --time-limit 14 --bit-rate 4000000 /sdcard/p08-motion-normal.mp4 &
adb -s emulator-5554 shell screenrecord --time-limit 14 --bit-rate 4000000 /sdcard/p08-motion-reduced.mp4 &
# (tab switches + quick-add open/close were driven while recording)
adb -s emulator-5554 pull /sdcard/p08-motion-normal.mp4  recordings/p08-android-motion-normal.mp4
adb -s emulator-5554 pull /sdcard/p08-motion-reduced.mp4 recordings/p08-android-motion-reduced.mp4
```

`ffprobe`: normal 12.98 s / 2,104,076 B; reduced 12.36 s / 479,917 B. The
reduced-motion recording is much smaller for the same interaction set, which is
consistent with fewer animated frames; this is a file-size observation, not a
measured timing comparison.

## Case table

File names follow `p08-android-<surface>-<language>-<digits>-<theme>[...].png`.
"Accent" is the in-app bike brand colour; default = "Your bike's brand color"
(Yamaha blue). All stills are 1080x2424.

| # | File | Tab / surface | Lang | Digits | Theme | Text | Accent | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `p08-android-garage-bn-bn-light.png` | Garage | BN | BN | Light (system) | 1.0 | default | Base four-tab state, seeded synthetic data |
| 2 | `p08-android-documents-bn-bn-light.png` | Documents | BN | BN | Light (system) | 1.0 | default | Tax token / licence cards |
| 3 | `p08-android-records-bn-bn-light.png` | Records | BN | BN | Light (system) | 1.0 | default | Recap + timeline |
| 4 | `p08-android-profile-bn-bn-light.png` | Profile | BN | BN | Light (system) | 1.0 | default | Bike card + appearance (same view as #18) |
| 5 | `p08-android-garage-bn-bn-dark.png` | Garage | BN | BN | Dark | 1.0 | default | Four-tab dark set |
| 6 | `p08-android-documents-bn-bn-dark.png` | Documents | BN | BN | Dark | 1.0 | default | |
| 7 | `p08-android-records-bn-bn-dark.png` | Records | BN | BN | Dark | 1.0 | default | |
| 8 | `p08-android-profile-bn-bn-dark.png` | Profile | BN | BN | Dark | 1.0 | default | Appearance incl. dark-palette swatches |
| 9 | `p08-android-garage-en-en-light.png` | Garage | EN | EN | Light | 1.0 | default | Four-tab EN light set |
| 10 | `p08-android-documents-en-en-light.png` | Documents | EN | EN | Light | 1.0 | default | Document titles still BN (see D2) |
| 11 | `p08-android-records-en-en-light.png` | Records | EN | EN | Light | 1.0 | default | |
| 12 | `p08-android-profile-en-en-light.png` | Profile | EN | EN | Light | 1.0 | default | English appearance section |
| 13 | `p08-android-garage-en-en-dark.png` | Garage | EN | EN | Dark | 1.0 | default | Four-tab EN dark set |
| 14 | `p08-android-documents-en-en-dark.png` | Documents | EN | EN | Dark | 1.0 | default | |
| 15 | `p08-android-records-en-en-dark.png` | Records | EN | EN | Dark | 1.0 | default | |
| 16 | `p08-android-profile-en-en-dark.png` | Profile | EN | EN | Dark | 1.0 | default | |
| 17 | `p08-android-garage-bn-en-light.png` | Garage | BN | EN | Light | 1.0 | default | Cross digit style: Bangla UI, Latin digits |
| 18 | `p08-android-garage-en-bn-light.png` | Garage | EN | BN | Light | 1.0 | default | Cross digit style: English UI, Bangla digits |
| 19 | `p08-android-garage-large-text-bn-bn-light.png` | Garage | BN | BN | Light | 1.3 | default | Text scale 1.3 |
| 20 | `p08-android-records-large-text-bn-bn-light.png` | Records | BN | BN | Light | 1.3 | default | Wrapping/overflow at 1.3 (D4) |
| 21 | `p08-android-profile-large-text-bn-bn-light.png` | Profile | BN | BN | Light | 1.3 | default | Theme label truncation at 1.3 (D5) |
| 22 | `p08-android-profile-appearance-1of2-bn-bn-light.png` | Profile appearance | BN | BN | Light | 1.0 | default | Swatches 1–8 visible |
| 23 | `p08-android-profile-appearance-2of2-bn-bn-light.png` | Profile appearance | BN | BN | Light | 1.0 | default | Swatches 2–9 visible (row scrolled to end) |
| 24 | `p08-android-profile-swatch-row-end-bn-bn-light.png` | Profile appearance | BN | BN | Light | 1.0 | KTM selected, row at end | Position evidence for the last swatch |
| 25 | `p08-android-profile-accent-ktm-selected-bn-bn-light.png` | Profile appearance | BN | BN | Light | 1.0 | KTM orange (1st) | Caption changes to "KTM কালার" |
| 26 | `p08-android-profile-accent-royal-enfield-selected-bn-bn-light.png` | Profile appearance | BN | BN | Light | 1.0 | Royal Enfield brown (9th) | Caption changes to "Royal Enfield কালার" |
| 27 | `p08-android-garage-accent-ktm-orange-bn-bn-light.png` | Garage | BN | BN | Light | 1.0 | KTM orange | Extreme accent, light |
| 28 | `p08-android-garage-accent-ktm-orange-bn-bn-dark.png` | Garage | BN | BN | Dark | 1.0 | KTM orange | Extreme accent, dark |
| 29 | `p08-android-garage-accent-royal-enfield-bn-bn-light.png` | Garage | BN | BN | Light | 1.0 | Royal Enfield | Extreme accent, light |
| 30 | `p08-android-garage-accent-royal-enfield-bn-bn-dark.png` | Garage | BN | BN | Dark | 1.0 | Royal Enfield | Extreme accent, dark |
| 31 | `p08-android-sheet-quickadd-bn-bn-light.png` | Quick add sheet | BN | BN | Light | 1.0 | default | Fuel/Service/Odometer/Document grid + paste row |
| 32 | `p08-android-sheet-quickadd-en-en-light.png` | Quick add sheet | EN | EN | Light | 1.0 | default | English copy; mistranslation D11 |
| 33 | `p08-android-sheet-fuel-bn-bn-light.png` | Fuel sheet | BN | BN | Light | 1.0 | default | Taka-first keypad, price confirm, tank/date chips |
| 34 | `p08-android-sheet-service-bn-bn-light.png` | Service sheet | BN | BN | Light | 1.0 | default | Parts chips, total, fixed save action |
| 35 | `p08-android-sheet-expense-bn-bn-light.png` | Expense sheet | BN | BN | Light | 1.0 | default | Parking/toll/other, date, note, keypad |
| 36 | `p08-android-sheet-document-scan-bn-bn-light.png` | Document add/scan sheet | BN | BN | Light | 1.0 | default | "কোন ডকুমেন্ট?" document-type picker |
| 37 | `p08-android-sheet-backup-bn-bn-light.png` | Backup & restore sheet | BN | BN | Light | 1.0 | default | Review + password fields; no backup was created |
| 38 | `p08-android-sheet-privacy-bn-bn-light.png` | Privacy & security sheet | BN | BN | Light | 1.0 | default | App-lock state, protected-data copy |
| 39 | `p08-android-profile-preferences-bn-bn-light.png` | Profile preferences | BN | BN | Light | 1.0 | default | পছন্দ ও সতর্কতা incl. Reduce motion row |
| 40 | `p08-android-profile-reduced-motion-on-bn-bn-dark.png` | Profile preferences | BN | BN | Dark | 1.0 | KTM orange | Reduce motion ON (evidence of toggle state) |
| 41 | `p08-android-profile-reduced-motion-off-bn-bn-light.png` | Profile preferences | BN | BN | Light | 1.0 | default | Reduce motion restored OFF after recording |
| 42 | `p08-android-profile-inapp-switch-en-en-light.png` | Profile | EN (in-app switch) | EN | Light | 1.0 | default | Instant in-app language/digit switch, no relaunch |

Recordings (`recordings/`):

| File | State | Content |
| --- | --- | --- |
| `recordings/p08-android-motion-normal.mp4` | BN/BN, light, Normal text, default accent, Reduce motion OFF | Tab switches + quick-add open/close, 12.98 s |
| `recordings/p08-android-motion-reduced.mp4` | BN/BN, light, Normal text, default accent, **Reduce motion ON** | Same interaction set, 12.36 s |

## Observed results

- All four tabs render in light and dark, in BN and EN, with both digit styles and
  with the cross combinations (BN text + Latin digits; EN text + Bangla digits).
  `৳`, dates and counters follow the selected in-app digit style; the expense sheet
  date input is the exception (D7).
- The app theme, nine-swatch accent row, language, number style and text size all
  update immediately from the Profile in-app controls; the EN/EN Garage after an
  in-app switch is byte-identical to the launch-argument EN/EN Garage frame.
- The first and last swatch (KTM orange, Royal Enfield brown) apply across the
  Garage surfaces (buttons, chips, icons, chart line, nav selection) in both themes.
  The dark theme uses a lighter swatch palette than light theme, as expected.
- Nine swatches do not fit on one 1080 px-wide frame; `1of2` shows 1–8 and `2of2`
  shows 2–9 after scrolling the row, which together cover all nine.
- Sheets open as bottom sheets with a draggable handle, fixed header, scrolling
  body and fixed action area where present; the fuel/expense/service sheets show
  the in-app keypad above the bottom bar.
- Text scale 1.3: most copy wraps without losing actions; the specific clipping
  below was observed.
- The backup sheet was opened only up to its password step; no backup was created,
  no restore was attempted, and no file/picker operation was triggered.
- Emulator state was not reconfigured: no boot/wipe/erase, no `pm clear`, no
  `adb uninstall`, no OS night-mode/font-scale/locale changes. Read-only values
  afterwards: `settings get secure ui_night_mode` = 1,
  `settings get system font_scale` = 1.0 (unchanged by this session).
- The production app was never targeted by a state-changing command. Its device
  record is unchanged: `lastUpdateTime=2026-10-27 06:06:30`,
  APK SHA-256 `e4afa377b3a882c1f128fa5f7e4dcd026a815f1a7cbc000674d8e73d349ab512`;
  only read-only `pm path` / `dumpsys package` / APK hash reads were issued against it.

## Defects observed

Severity is not assigned; these are visual/copy observations from the stills.

1. **D1 – Untranslated part name in English Garage.** With language EN the
   "Due now" urgent card still shows the Bangla part name "চেইন" ("Chain") in all
   EN Garage captures (`garage-en-en-light/dark`, `garage-en-bn-light`); the
   Garage frame after the in-app EN switch was byte-identical to the EN/EN frame,
   so it has the same defect. Other card copy is English.
2. **D2 – Untranslated document titles in English Documents.** "Next renewal" and
   the two document cards show Bangla headings "ট্যাক্স টোকেন" and
   "ড্রাইভিং লাইসেন্স" while the document artwork inside the same card says
   "Tax Token" / "Driving License" in English (`documents-en-en-light/dark`).
3. **D3 – Records filter row clipped at the right edge.** The category chip row
   always shows a partially cut chip (e.g. "পার্কিং"/"Pa…") at 1.0 scale, and
   "রিনিউয়াল"/"Renewal" is cut at 1.3 scale (`records-bn-bn-*`,
   `records-en-en-*`, `records-large-text`). The row is a scrollable chip row, so
   the chip may be reachable by scrolling; there is no ellipsis/fade cue.
4. **D4 – Records action buttons clipped at 1.3 text scale.** "খরচের বিস্তারিত"
   is cut mid-label at the right edge; the third button extends past the viewport
   (`records-large-text-bn-bn-light`).
5. **D5 – Profile theme label truncated at 1.3 text scale.** The selected theme
   segment shows "সিস্টেম" instead of "সিস্টেম ডিফল্ট" (the rest is clipped);
   the bike card also wraps "Yamaha R15 / V4" and pushes "কিমি" to its own line
   (`profile-large-text-bn-bn-light`).
6. **D6 – Accent swatches are unlabelled.** The nine swatch buttons expose no text
   or content-desc nodes in the UI Automation tree (the row between "রঙ"/"Color"
   and its caption is empty in the dump); colour identity is only discoverable by
   tapping. A screen-reader user cannot enumerate the nine brand colours. The row
   also requires horizontal scrolling to reach the 9th swatch on this width.
7. **D7 – Expense date field uses Latin digits.** With number style "১২৩ বাংলা" the
   expense sheet date field renders `2026-11-12` in Latin digits while the rest of
   the screen uses Bangla digits (`sheet-expense-bn-bn-light`).
8. **D8 – Backup sheet taller than the screen at first position.** The bottom
   warning ("জরুরি সতর্কতা…") is clipped at the fold and no primary action is
   visible until the body is scrolled (`sheet-backup-bn-bn-light`). No fixed action
   bar was observed in this sheet.
9. **D9 – Privacy sheet bottom card clipped by the fixed action.** The
   "সিকিউরিটি কী হারালে" card is cut behind the fixed "সম্পন্ন" button; the body is
   scrollable, but the card is clipped at rest (`sheet-privacy-bn-bn-light`).
10. **D10 – Service sheet parts row clipped.** The "কী কী কাজ হয়েছে" chip row cuts
    a fourth chip at the right edge; the row is horizontally scrollable
    (`sheet-service-bn-bn-light`).
11. **D11 – English microcopy issues in quick add.** "Arrange by typing or pasting"
    is a mistranslation of "লিখে বা পেস্ট করে যোগ করুন" (should be "Add by typing or
    pasting"), and "A mechanic, bKash or Nagad message" is a sentence fragment
    (`sheet-quickadd-en-en-light`). Bangla/English copy review remains a named-human
    sign-off item.
12. **D12 – Long document cards run under the bottom bar.** At rest the second
    document card continues under the floating bottom bar; the page is scrollable,
    so content is reachable, but the fold cuts the card artwork
    (`documents-*`). This is a scroll-state observation, not a lost control.

No contrast failure was measured; both themes looked legible in the inspected
frames (contrast was judged visually, not computed). Status-bar clock/network
artifacts are part of `screencap` and are not app content.

## Gaps and limitations (honest)

- The nine swatches cannot all be visible in one 1080 px frame; two frames cover
  them (1–8 and 2–9). This is documented instead of claiming a single all-nine view.
- Only the in-app controls were used for theme/language/digits/text size/accent/
  reduced motion. OS night mode, font scale, locale and accessibility settings were
  not changed, so the OS-dark and OS-font-scale variants are **not** covered here.
  There is no TalkBack pass; the unlabelled swatch finding comes from the
  accessibility tree, not from spoken output.
- Dark-theme sheets were not captured (sheets are all BN/EN light); only the EN
  quick-add sheet is in English. The backup restore branch, in-app document
  picker/camera/scan and Undo flows were not exercised.
- The motion recordings show the flows at normal and in-app reduced motion, but do
  not measure frame timings or establish pixel/motion parity with v3. Screen
  recordings are `.mp4` evidence only.
- The probe APKs were built before this session; the captures document the working
  tree at `e6c59bf…` plus pre-existing uncommitted changes, not a clean-HEAD build.
- The synthetic host resets its settings on each relaunch; this is a harness
  property, not app behaviour, and is recorded so the case table is reproducible.

## Artifact manifest

SHA-256 of every artifact is appended below (`shasum -a 256`). 42 PNG stills and
2 MP4 recordings.
```
3375f1bf39f4ffe2d4b173598682bda256ebb95e805f7b8d64710bee3c96e05e  p08-android-documents-bn-bn-dark.png
cedfdfe1b606667dace72e7e1f552f0ea42d1ccc96bac247dc02e4803e973c3d  p08-android-documents-bn-bn-light.png
c53714bffec848ae0f707d6d4aac643aa0cf265a5b75197ab2874338feec07a1  p08-android-documents-en-en-dark.png
dc91d4150e07ca5258b0977c8973bbbd8c431081883cae0ad20cdb0ba26bcaeb  p08-android-documents-en-en-light.png
0096dd8be5a0195820dc573d78d296d569af17c35f422d2b3f113a4c908500ae  p08-android-garage-accent-ktm-orange-bn-bn-dark.png
05859c6f9f099919ec2e1b6a5bd850ce1c8d80f0bd8d22005377ae58eaac9037  p08-android-garage-accent-ktm-orange-bn-bn-light.png
bdc3407faa3a6ca7844c57351b135d6cc27f9d4c1608190913b1d2f4067ffe6c  p08-android-garage-accent-royal-enfield-bn-bn-dark.png
4741bdc26f7659a7c655550b5c27b701be670502b6962c7072bcfeabe7f56b2c  p08-android-garage-accent-royal-enfield-bn-bn-light.png
8ca084cc1ef0ee5fd6e24c8875a12e3d375f4b59bee977eca34dcfa5ad78ad1d  p08-android-garage-bn-bn-dark.png
929a6ff87ffd32439e5011f3317e6dd5ef287ae5d09dcbe6084923701ef46694  p08-android-garage-bn-bn-light.png
fd697e8363a9e2cb3d04cd2ed485f0bb8f8522867609cc1e8906204db16b85f5  p08-android-garage-bn-en-light.png
82d59fd7cb76d5d425fd2453b3867546880c8784df6a9f2c3b7304ac69bc0c8e  p08-android-garage-en-bn-light.png
81f7c3253c5eeb502e4561ed8ca545869d7e5a43588f611cccc82dbd18c2b481  p08-android-garage-en-en-dark.png
2047b3618d457b5276722e1811381aca5d0c7954576cea53f79034491cc1c1f2  p08-android-garage-en-en-light.png
8541c055f7647faf7ebe205de2ccd66ea58a86ab3e3495537ec14c0437d1f14e  p08-android-garage-large-text-bn-bn-light.png
ed1e6e977f59435e46a1cfc3ca26e01f72b17ffc0e54900ac29b04368ff0cd92  p08-android-profile-accent-ktm-selected-bn-bn-light.png
64b65718d51cf45ad0e80d4099bee025d21a3b95563c35f8c0cbd318599b9ec9  p08-android-profile-accent-royal-enfield-selected-bn-bn-light.png
1ccf3e841ebaee80ac7bce8b1e983d53f8ea8c32ac9a533a64fbe36f87193df9  p08-android-profile-appearance-1of2-bn-bn-light.png
3253b61dfb6922105db1793a680a98a0791697ba799328b4e943f569d3ea0560  p08-android-profile-appearance-2of2-bn-bn-light.png
928a06fc06c527c8391711a704907ac07ced18baad15f53ad805ab107975af8e  p08-android-profile-bn-bn-dark.png
1ccf3e841ebaee80ac7bce8b1e983d53f8ea8c32ac9a533a64fbe36f87193df9  p08-android-profile-bn-bn-light.png
e78587589850aa3facbe7dd053d999df714fde3ccd637e99d5cf8d33c5831a82  p08-android-profile-en-en-dark.png
4aca81719f7454c9bf9866695a10ff38c4594f6dfc3a84f8e21e042973244352  p08-android-profile-en-en-light.png
a21c6235fc069efc3667a17cc0baeef738f6ac0272af7c923e4c3954682e4fbd  p08-android-profile-inapp-switch-en-en-light.png
02460817bef94a50bc872330d95e88e26c1ce80ddbb2c2f6b0b5629ba4eb9018  p08-android-profile-large-text-bn-bn-light.png
bf1b8e69898e9554ef81c1fdd7bf36ab106a9de34f8f633ce0ea2636ac295f6a  p08-android-profile-preferences-bn-bn-light.png
9e56f04c7a152d13d1035cf3050818fa84d00247b8ed60721b3f08eee5a5f95a  p08-android-profile-reduced-motion-off-bn-bn-light.png
c5a11e9bd321caa9efc6ad6b2cb6216af9dfe8e54f1336f0f53c37657ec210d6  p08-android-profile-reduced-motion-on-bn-bn-dark.png
c19340236c7a781858014e5ed4290bfe921f7008d6ff04704972e3a2fa44b73d  p08-android-profile-swatch-row-end-bn-bn-light.png
23bb04f772bf22697c8e18b81bd57066a32f11a0840c5a300a0877047e5b71d4  p08-android-records-bn-bn-dark.png
68209b5ea6fe47971acfd57445040447c3d352f82238691878d00c399e5a49d2  p08-android-records-bn-bn-light.png
adfd09cae91cc4a1aa1f18c1650ff5512c67cc2a872136d45e7eaa8124357cab  p08-android-records-en-en-dark.png
c1a7c590e10b633655a67928cf18200f880df4d7e2bfd4831a6b36f81b374827  p08-android-records-en-en-light.png
d2621a51aa9088e8276c92be7d8a7d94b470b6545052c2ba276f90a36a239f3e  p08-android-records-large-text-bn-bn-light.png
789d27641cc12fa020cfbfd32817420ccee564e13d87d69ba6edf41924761939  p08-android-sheet-backup-bn-bn-light.png
f2799909175081f6c7bb367f5f51dac5e5b3b1c13f62e496bafce08c32bc390b  p08-android-sheet-document-scan-bn-bn-light.png
53dd1413f628c12af3fe537adbb50de08f6b3da2d431212426e5038171f1835b  p08-android-sheet-expense-bn-bn-light.png
fc6eeaa09f6d88cf11343950ebabde5c0800392d5b068cb2b0bf1e3c0436aa89  p08-android-sheet-fuel-bn-bn-light.png
6a5321828c9fafe3661659c9e50ada55f702e9cb1f47f0f1a9a99f4ced89c7bc  p08-android-sheet-privacy-bn-bn-light.png
cd3033a18147ddb308117b09905bedc617cfdf5a5afb172032b77894f2263ad3  p08-android-sheet-quickadd-bn-bn-light.png
813f26faecbc10ffa2be7632df9cfb65e07a48c5738d9f253dfec39361b46c1a  p08-android-sheet-quickadd-en-en-light.png
5e0d6b88103db420e2bd1885ab8c88a299f71823f5ca8f15392307610ceb0a96  p08-android-sheet-service-bn-bn-light.png
39e2a37f41fd1ef95e87f6b8b84da408ad1943e0901ddcb384a371efa78657ae  p08-android-motion-normal.mp4
a954f866103bba57eb3641dad14118d5e69be53a0a1d8fab9183c589d2f795cd  p08-android-motion-reduced.mp4
```

Captured files:
      42
       2
