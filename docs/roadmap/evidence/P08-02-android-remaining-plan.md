# P08-02 remaining Android physical checks — one-run plan

Run on the owner's connected Motorola Edge 50 Fusion when the phone is next
unlocked. Keep the screen on; do not change OS locale/font/night-mode; restore
anything changed. Current probe APK:
`ccf20f29e00265212da9b6c7dc3f2388d9dd6e3d7e521a80b0909378c3415955`.

## 1. System-camera allowed path (real hardware)

1. `adb shell pm grant com.motovault.app.privacyprobe android.permission.CAMERA`
   if not granted.
2. Launch `FuelEditFixtureActivity`, Garage → ওডোমিটার → "ডিস্প্লের ছবি তুলুন".
3. Confirm the foreground activity is the **Motorola/system camera** app (log
   `dumpsys activity` top activity; do not screenshot the camera preview).
4. Press Back to cancel; capture the returned odometer sheet (`screencap` +
   `uiautomator`) and confirm no photo was saved (no new file in the probe cache).
5. Record the camera package/activity name as evidence; no environment photos.

## 2. Physical large-vault measurement

1. Launch `LargeVaultFixtureActivity --es language bn --es digits bn` (synthetic
   1,200 fuel logs + 200 documents; no owner data).
2. `am start -W` TotalTime (cold), then `dumpsys gfxinfo … reset` + 10 swipes,
   then `dumpsys gfxinfo` (frame/jank percentiles) and `dumpsys meminfo` (PSS).
3. Switch Records/Profile tabs once, record any jank/ANR and PSS again.
4. Compare against the emulator pre-work (2,119 ms start, 81/284 janky, PSS
   ~126 MB) and record the physical budget decision.

## 3. Reminder scheduling (real app path)

1. `POST_NOTIFICATIONS` granted; launch `MainActivity` (seeded synthetic vault).
2. Profile → reminders: confirm the card says notifications are enabled.
3. Enable document/service reminders if they are off, reopen Profile, then check
   `dumpsys alarm` filtered for `com.motovault.app.privacyprobe` — record whether
   at least one renewal alarm is scheduled (do not screenshot other alarms).
4. If an alarm is scheduled for the future, delivery itself is not claimed until
   it fires; only record scheduling.
5. Restore: revoke POST_NOTIFICATIONS if it was originally false.

## 4. Optional: unplugged battery/energy (owner must approve unplugging)

Only if the owner unplugs USB (or authorizes Wi-Fi adb): run a 10-minute scripted
walkthrough (scroll Records, open sheets, one capture-denied path), then read
`dumpsys batterystats` for the app's estimated power use and battery percentage
delta. Do not claim a battery budget without this; charging-state measurements do
not count.

## 5. Restore and evidence

- Force-stop the probe; restore permissions/stay-awake/font/night/timeout exactly
  as found; verify with a before/after settings dump.
- Evidence: screenshots of app windows only (no home screen, no camera preview,
  no notification shade), dumps filtered to the probe package, provenance file
  with device/APK hashes and commands.
- Update the P08-02 report with results and any waivers; spoken TalkBack/voice
  remain separate assisted checks, and iPhone physical stays waived (WD-01).
