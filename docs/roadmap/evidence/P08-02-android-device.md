# P08-02 Android physical-device pass (Motorola Edge 50 Fusion)

Date: 12 November 2026 (Asia/Dhaka). Scope: the Android slice of the P08-02
physical-device accessibility, reliability and performance pass, done as one
consolidated run on the owner's connected phone. iOS physical hardware remains
outstanding, so P08-02 stays in review.

## Device and build identity

| Fact | Value |
| --- | --- |
| Device | motorola edge 50 fusion (motorola) |
| OS | Android 16 / API 36, arm64-v8a |
| Display | 1080 × 2400 px @ 400 dpi |
| Build fingerprint | `motorola/cuscoi_g/cuscoi:16/W1UUIS36H.110-42-4/83f47c-9d83a:user/release-keys` |
| Probe APK | `e60d3bf49011cea4b48aef711b849e35eb47710b954d04f4c207860968bd5564` |
| Test APK | `7f7c82fbae147c414896f928f57a9629282ed007f418f3c9de98c076701c7713` |
| Source | `e6c59bf` + uncommitted P08 working tree (not a clean HEAD build) |
| Isolated app | `com.motovault.app.privacyprobe` (synthetic data only; production app not launched or modified) |

Owner's original settings were recorded before testing and verified unchanged
afterwards: font scale **1.15**, night mode off, screen timeout 60 s; no OS
locale, font-scale, night-mode or timeout change was made. `stay_on_while_
plugged_in` was temporarily set to 3 for the run and restored to 0. Evidence
commands, settings and per-file hashes: [README](p08-02/android-device/README.md).

## What ran

1. `adb shell am instrument -w com.motovault.app.privacyprobe.test/com.motovault.app.storage.security.NativeAssetInstrumentation`
   — **PASS 37 native storage checks** on the physical device (fuel,
   maintenance, renewal, recovery, backup/restore, Room 4→5 preservation,
   EN/BN reminder localisation, plural/fallback, document assets). Console output:
   [native-probe-device.txt](p08-02/android-device/native-probe-device.txt).
2. One UI/performance pass on the isolated synthetic host
   (`FuelEditFixtureActivity`, `VaultState.seedData()`): launch, in-app language
   switch, 1.3 text scale, notification-permission prompt, camera-permission
   prompt and denial fallback, frame/memory/startup measurements.
3. Device state restored afterwards (permissions and stay-awake as recorded).

## Results

### Latest-change UI smoke (language, layout, copy)

- The fixture opens on Records; the **new wrapping layout is visible on the
  physical device at the owner's effective scale**: period/category chips wrap
  across lines and the Records action row wraps instead of clipping labels
  (`01-launch-records-bn.png`).
- In-app Bangla → English switch with no restart: Profile, Records and Garage
  render English copy (`03-profile-en.png`, `04-records-en.png`,
  `05-garage-en.png`); switching back to Bangla works (`06-profile-bn-again.xml`).
- In-app text size Extra large: the theme segment now wraps "সিস্টেম ডিফল্ট"
  fully inside its option instead of truncating, and language/records rows remain
  complete (`07-profile-1.3-bn.png`, `08-records-1.3-bn.png`). Note: the owner's
  **OS font scale is 1.15**, so the in-app 1.3 produces an effective ≈1.5×
  scale — a stronger stress than the design matrix's 1.3, and the layout still
  holds without clipped chips or segmented labels.
- No clipped/overlapping copy was seen in the captured states.

### Native permission surfaces

- **Notifications** (`10-notification-permission-dialog.png`): the system prompt
  "Allow MotoVault to send you notifications?" appeared from the in-app
  notification card. The dialog is system-owned chrome (English because the
  device locale is English) while the app card behind it is localized. The allow
  tap was **not confirmed** (the dump still showed the dialog immediately after),
  so no grant is claimed; POST_NOTIFICATIONS was left at its original `false`.
  Notification delivery/background behaviour therefore remains unverified here.
- **Camera** (`c03-camera-permission-dialog.png`): the prompt "Allow MotoVault to
  take pictures and record video?" appeared from the odometer photo action.
  Choosing "Don't allow" returned to the odometer sheet with the value unchanged
  (১২,৩৪০) and the manual keypad/save path usable
  (`c04-camera-denied-fallback.png`, `c04-camera-denied-fallback.xml`). Camera
  permission was restored to its original granted state afterwards. No photo was
  taken and no camera preview image is retained.

### Accessibility (physical tree, not spoken output)

`uiautomator` tree of the Profile screen (`02-profile-bn.xml`) exposes **nine
equal-size clickable nodes in one row with empty `text` and empty
`content-desc`** — the nine brand-colour swatches are unlabeled on the physical
device, confirming the P08-01 finding. The Profile screen has 19 clickable nodes
whose own node has no label; some are grouped containers whose child text may
still be announced, so this raw count is not a TalkBack result. **Spoken
TalkBack output, focus order and announcements were not measured** (would need a
TalkBack session on the device) — the P08-01/remaining-gates accessibility item
stays open.

### Performance measurements (single short pass)

| Metric | Measured |
| --- | --- |
| Cold start (`am start -W`, synthetic fixture activity) | 943–1018 ms (`TotalTime`) across launches |
| Total PSS | ~82 MB at launch; ~133 MB after the permission flow; ~137 MB after list scrolling |
| Frames during 5 swipes | 431 rendered, **6 janky (1.39%)**; 50th 12 ms, 90th 14 ms, 95th 15 ms |
| Battery | 89% → 88% while USB-charging over ~6 min (not a battery-drain measurement) |

Proposed budgets derived from these measurements on this representative
mid-range device (to be confirmed with repeat/longer runs before being frozen):
cold start ≤ 2.5 s, total PSS ≤ 250 MB, janky frames ≤ 5 %, 90th percentile
≤ 16 ms. Battery and sustained-load budgets need a longer, unplugged run.


## Physical artifact review addendum (12 November 2026)

Reviewing the retained screenshots/dumps after the run:

- **English UI at the owner's OS scale (1.15)**: Profile renders the English
  labels correctly ("System default" wraps inside the theme segment, no
  truncation); no clipped or mixed-language copy was visible in the captured
  Profile/Records/Garage states.
- **Nine brand-colour swatches**: the physical tree shows nine self-closing
  clickable nodes (row at y≈990–1110, equal widths) with empty `text` and empty
  `content-desc` and no child text — they have no accessible name on the device.
  The ninth swatch is also partly clipped by the card edge at this scale, the
  same finding as the simulator matrix.
- **Raw empty-label counts per screen** (physical dumps): Records 17/18 clickable
  nodes, Profile 19/20, Garage 12/14, Records at 1.3 15/16 have no label on the
  clickable node itself. Many are grouped containers whose child text may still
  be announced by TalkBack, so this is an audit signal, **not** a spoken-output
  result; the swatch row is the clear unlabeled-control defect.
- **Notification-permission capture**: the retained "after tap" image still shows
  the system dialog (Allow / Don't allow), confirming the allow tap was not
  registered; no notification grant or delivery is claimed.
- **Camera denial fallback**: the odometer sheet returns with the value intact
  and the manual keypad/save path visible; no camera photo was taken.


## Additional Android physical checks (13 November 2026, one consolidated pass)

Run on the same Motorola Edge 50 Fusion (Android 16/API 36) with the
`e60d3bf…` probe build. Only app-window captures were retained; an initial share
attempt that captured the phone home screen was deleted, so no owner data or
wallpaper appears in evidence.

- **Share boundary (cold and warm)**: `ACTION_SEND` `text/plain` into the isolated
  probe opened the review sheet with the shared text (`ODO12345km`, `ODO23456km`),
  parsed fuel amount (১২,৩৪৫.০), and the Bangla voice disclosure. Cancelling the
  sheet returned to the launcher (`share-cold-review.png`/`.xml`,
  `share-warm-review.png`/`.xml`).
- **Share rejection**: a `text/plain` share carrying `EXTRA_STREAM` stayed on the
  capture activity and was dismissed without enqueueing a draft (recorded top
  activity only); an unsupported `image/jpeg` share showed the truthful app
  rejection copy "Could not prepare this share…" (`share-mime-rejection.png`/`.xml`).
- **Backup-restore file picker**: the restore sheet's "ব্যাকআপ ফাইল (.mva)
  নির্বাচন করুন" launched the system resolver ("Complete action using: Files —
  Just once"); cancelling returned to the restore sheet. The picker window itself
  was not screenshotted, to avoid exposing owner files
  (`picker-03-restore-sheet.xml`, `p2-03-restore-sheet.xml`,
  `picker-files-02-after-cancel.png`/`.xml`).
- **Notification permission plumbing**: after granting `POST_NOTIFICATIONS` the
  Profile reminder card changed to "নোটিফিকেশন চালু আছে"
  (`notif-03-reminder-card-granted.png`/`.xml`); the permission was then revoked
  back to its original `false`. Scheduling/delivery and background behaviour are
  still unverified.
- **Sustained scroll (10 swipes on Records)**: 840 frames rendered, 8 janky
  (0.95 %), 50th 12 ms, 90th 12 ms, 95th 14 ms; TOTAL PSS ~137 MB after the
  scrolls. The phone was USB-charging at 87 %, so no discharge-based battery
  budget was measured.
- **Demo-mode limitation observed**: the demo document scan shows "ডেমো: আসল
  অ্যাপে এখানে লাইভ ক্যামেরা দেখাবে" and has no photo/file action, so the
  document-import picker could not be exercised through that path; the backup
  restore picker above covers the system picker boundary instead.

Still unverified on Android: notification scheduling/delivery and background
work, spoken TalkBack output and focus order, real Bangla voice input, the
allowed camera-preview/capture path on the current build, large-vault
operations, and an unplugged battery/sustained-energy budget. Physical iOS is
waived under WD-01 ([P08-owner-decisions.md](P08-owner-decisions.md)).


## Remaining Android probes attempted (13 November 2026) — negative results

Two further one-pass attempts were made and are recorded as **not verified**, not
as passes:

- **Reminder scheduling**: with `POST_NOTIFICATIONS` granted and the app opened
  through `MainActivity` then Profile (which triggers reminder reconciliation),
  `dumpsys alarm` contained **zero** alarms for the probe package (checked twice,
  after waiting for the UI). Notification scheduling/delivery and background
  behaviour therefore remain unverified; no notification-delivery evidence is
  claimed. Temporary dumps were deleted because `dumpsys notification` includes
  other apps' notifications (owner data); nothing from them was retained.
- **Camera allowed path**: tapping the odometer sheet's camera action opened an
  app surface, but `dumpsys media.camera` showed **no probe camera client**
  within four seconds (twice), so the real camera-preview/capture path on this
  build is unconfirmed. No camera preview screenshot was kept (would show the
  environment). P07's older-build physical camera evidence cannot be reused as
  current-build proof.
- Device restored afterwards: unlocked, font scale 1.15, night mode off,
  stay-awake 0, POST_NOTIFICATIONS false (original), camera granted (original).

These join the still-open Android gaps: notification scheduling/delivery and
background work, spoken TalkBack/VoiceOver, real Bangla voice input, large-vault
operations, and unplugged battery/energy measurement.


## Large-vault synthetic fixture (13 November 2026)

Added the test-only `LargeVaultFixtureActivity` in the isolated privacy probe: it
renders a synthetic vault of **1,200 fuel logs + 200 documents** (no owner data)
through the same `MotoVault` composition, so large-vault startup/scroll/memory can
be measured without touching owner records. Build:
`ccf20f29e00265212da9b6c7dc3f2388d9dd6e3d7e521a80b0909378c3415955` (probe APK).

Emulator pre-work (Pixel_10, Android 17/API 37 — **not** a physical-device result):
`am start -W` TotalTime **2,119 ms**; 10 swipes rendered 284 frames with
**81 janky (28.52 %)** and p90 18 ms; TOTAL PSS ~126 MB at launch and ~117 MB
after scrolling. This is a software-rendered emulator baseline; the physical
Motorola measurement is still required for the P08-02 budget (the phone was
auto-locked before this run, so it is recorded as pending, not failed).

## Camera allowed path — implementation note

The odometer photo action uses `ActivityResultContracts.TakePicture()`, i.e. the
**system camera app**, not an in-app CameraX client. The earlier check for our
package in `dumpsys media.camera` was therefore the wrong probe. What remains to
verify on the physical phone is that the system camera activity opens and Back
cancels without saving; the phone locked before that run could be captured. The
permission prompt and denial fallback were already verified on this device.


## Camera launch wiring verified on emulator (13 November 2026)

Because the physical phone auto-locked before the check, the system-camera launch
contract was verified on the Pixel_10 emulator (installed probe APK `ccf20f29…`):
tapping the odometer sheet's camera action moved the foreground activity to
`com.android.camera2/com.android.camera.CaptureActivity` (the platform camera app
launched through `ActivityResultContracts.TakePicture()`), and Back returned to
the fixture with the odometer sheet intact
(`ce-04-after-camera-cancel.png`/`.xml`, `android-emulator/cam-emu.log`). This
verifies the launcher wiring, not real-camera hardware; the physical allowed-path
check (open system camera, cancel, no photo stored) remains pending an unlock.

## Final physical checks on the unlocked phone (13 November 2026)

Probe APK `f53c7a9f23472d29a01d1e85d5454ed077c86bc75356654ace3f628098f59c65`
(swatch fix + large-vault/notification probe activities), device restored after
the run (notifications false, stay-awake 0, font 1.15, night off; camera grant
unchanged).

- **System-camera allowed path (real hardware): PASS.** Tapping the odometer photo
  action moved the foreground to `com.motorola.camera5/com.motorola.camera.Camera`;
  Back returned to the fixture with the odometer sheet intact, and
  `cache/odometer-capture` was **empty** afterwards (no photo saved). No camera
  preview was screenshotted.
- **Physical large-vault measurement** (1,200 fuel logs + 200 docs): cold start
  **1,318 ms** (`am start -W`); 10 swipes rendered **772 frames with 116 janky
  (15.03 %)**, p50 9 ms, p90 12 ms, p95 14 ms; PSS ~146 MB before and ~156 MB
  after the scroll. Compared with the emulator pre-work (2,119 ms; 28.5 % janky),
  startup improves on hardware but **large-vault scroll jank (15 %) exceeds the
  provisional ≤5 % jank budget** — recorded as a performance finding for the
  release decision (separate large-vault budget or list/UI optimization; the
  default 8-log vault measured 0.95 % janky earlier).
- **Notification delivery (native, physical)**: using the production renewal
  notification builder and channel, the probe posted a notification that the
  notification service recorded with `channel=document_renewals`, localized
  channel name "কাগজ ও সার্ভিসের রিমাইন্ডার", and the reminder title/body
  (`p2-notification-delivered-filtered.txt`, filtered to probe-only lines).
  Force-stop cleared it (after force-stop the notification was no longer listed),
  so this proves **delivery while the app runs**, not background retention after
  force-stop. Scheduling was not observed: a filtered `dumpsys alarm` query after
  granting notifications and opening Profile returned **0** probe alarms
  (`p2-reminder-alarms-filtered.txt`).
- Remaining Android gaps: background/scheduled reminder delivery, spoken
  TalkBack/voice, real Bangla voice input, unplugged battery budget, and a
  large-vault performance decision. iPhone physical waived (WD-01).

## Restored state and limitations

- Restored: camera permission granted (original), POST_NOTIFICATIONS false
  (original), `stay_on_while_plugged_in` 0, font scale 1.15, night mode off,
  timeout 60 s, brightness unchanged beyond normal auto-brightness drift.
- Not verified / outstanding for P08-02: **physical iPhone** (no iOS hardware
  available), spoken TalkBack/VoiceOver output, notification delivery and
  background behaviour, a full locked/duplicate/unsupported/cancel share matrix,
  physical share/voice flows, long-run battery/sustained-load budgets, and
  repeat performance runs. Those remain in the task, which stays `in_review`.
- One consolidated pass on one device; findings are observations from that pass,
  not a representative-owner pilot (P08-03) or release candidate (P08-04).
