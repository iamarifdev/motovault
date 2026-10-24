# P00-04: Android runtime baseline

Date: 24 October 2026, Asia/Dhaka. Base: `50eda3a` plus baseline tooling only.
Build: debug, isolated application ID `com.motovault.baseline.p00`, produced with
`-I tools/baseline/isolated-android.gradle`. Production main sources were unchanged.

Runtime: existing Pixel_10 AVD, `emulator-5554`, Android 17 / API 37,
`sdk_gphone16k_arm64`, 1080 x 2424. [Runtime metadata](p00/platforms.json) records
the actual fingerprint and clock. The emulator was started with `-no-snapshot-save`.
No production package was cleared or loaded with fixtures.

## Captures and observed results

| Synthetic scenario | Screenshot | Hierarchy | Observed |
| --- | --- | --- | --- |
| Empty | [PNG](p00/android/empty.png) | [XML](p00/android/empty.xml) | Bike present, no rules/logs/documents; no fuel economy |
| Overdue | [PNG](p00/android/overdue.png) | [XML](p00/android/overdue.xml) | Chain 320 km overdue; native rule title remains English |
| Partial fill | [PNG](p00/android/partial-fill.png) | [XML](p00/android/partial-fill.xml) | Four fills, one partial, recorded cost 1788 taka and economy 45.5 km/L |
| Documents | [PNG](p00/android/documents.png) | [XML](p00/android/documents.xml) | Synthetic tax/licence/registration; no real originals; native wallet preview differs from v3 compact rows |
| Unknown service | [PNG](p00/android/unknown-service.png) | [XML](p00/android/unknown-service.xml) | Current no-rule shape cannot distinguish unknown service history from empty setup |

All inputs are from [the paired fixture manifest](../fixtures/p00/manifest.json).
`tools/baseline/capture_android.py` loads only the hard-coded isolated package,
reads the current hierarchy, navigates observed labels and saves each capture.

## Motion and Undo

- [Native recording](p00/android/motion-normal-and-reduced.mp4), approximately
  26.15 seconds. [Action timestamps](p00/android/motion-events.json) identify
  gauge/count-up launch, normal sheet, save, Undo attempt, reduced-motion switch
  and reduced sheet open/close.
- [Normal sheet](p00/android/motion-odometer-sheet.png),
  [save/Undo toast](p00/android/motion-save-undo.png),
  [after Undo](p00/android/motion-after-undo.png),
  [reduced switch](p00/android/motion-reduced-enabled.png),
  [reduced sheet](p00/android/motion-reduced-sheet.png).
- Undo is an observed defect: after a confirmed +10 save, tapping Undo does not
  restore 12340. Both the UI and [durable database inspection](p00/persistence-observations.json)
  retain 12350. The successful capture script is not a passing Undo feature test.

The first motion probe also exposed a harness substring collision (`+10` matching
`+100`). The selector now prefers exact text; final screenshots/data reflect a
real +10 change. The product Undo defect remained after that correction.

## Limits

This proves the current emulator baseline, not physical notification delivery,
camera/OCR accuracy, vibration, secure storage, encryption or release readiness.
Video frame rate is the observed capture rate, not an app rendering performance
measurement. The document fixture is metadata-only; asset integrity is covered
by separate compatibility fixtures, not these wallet screenshots.
