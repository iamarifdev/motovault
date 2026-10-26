# P01 numeric keypad and safe bounds

26 October 2026. Base `48fce40321600e4fbdfe124a3bb63b3032276502` plus the
uncommitted P01 follow-up. Tests use only the isolated synthetic app
`com.motovault.baseline.p00`. Production UI code did not change in this recheck.

P01's odometer entry uses the established in-app keypad. Reminder policy uses
segmented choices, switches and buttons, with no text field or OS keyboard.
This report covers those numeric-entry bounds; it does not claim every existing
text-entry flow or every possible OS keyboard configuration has been tested.

| Host and fixture | Observed result | Evidence |
| --- | --- | --- |
| iOS 27 iPhone 18 Pro Max, normal/Bangla | Pass | [Summary](p01/ios/acceptance-keypad-native-r1/summary.json), [run](p01/ios/acceptance-keypad-native-r1/run.json) |
| iOS 27 iPhone 18 Pro Max, dark/English/1.3 text/app reduced motion | Pass | [Summary](p01/ios/acceptance-keypad-large-native-r1/summary.json), [run](p01/ios/acceptance-keypad-large-native-r1/run.json) |
| Pixel 10 ARM64 API 37.1, normal/Bangla | Pass | [Bounds](p01/android/keypad-runtime-native-r3/result.json), [before](p01/android/keypad-runtime-native-r3/before.json), [after](p01/android/keypad-runtime-native-r3/after.json) |
| Pixel 10 ARM64 API 37.1, dark/English/1.3 text/app reduced motion | Pass | [Bounds](p01/android/keypad-unknown-large-native-r3/result.json), [before](p01/android/keypad-unknown-large-native-r3/before.json), [after](p01/android/keypad-unknown-large-native-r3/after.json) |

The two iOS XCTest cases assert all ten numeric keys and Delete exist, are
hittable, have at least 44-point height, stay inside horizontal display bounds
and end above the bottom 20 points. The primary Save action is hittable, has at
least 44-point height and ends above the keypad's first row. No system keyboard
appears. After increasing the draft, Close restores the original Home odometer
label. Attachments retain screenshots and native trees for layout inspection.

Android measures clickable ancestor bounds rather than the small text glyph
bounds. Each key and Save are at least 44dp high and remain inside the display.
The retained large-text keys are 137px high at density 2.625, approximately 52dp.
The bottom row ends at y=2328 on the 2424px display; Save ends at y=1691 and the
first keypad row begins at y=1717. `dumpsys input_method` reports
`mInputShown=false`. Closing the changed draft preserves the complete sanitized
database snapshot, including preferences, confirmed mileage/date, documents,
service baselines, snoozes and ledger count.

Visual inspection of the retained large/dark iOS screenshot confirms the full
keypad, Save action and Close button are visible with bottom clearance. Native
bounds and screenshots establish portrait simulator/emulator coverage, not
physical-device rendering, haptics, landscape or spoken accessibility.

## Diagnostic attempts

Android r1 stopped before a database snapshot because its fixed one-second wait
did not establish that Home had loaded. r2 reached Home but its selector expected
the digit text node itself to be clickable. The retained r2 hierarchy instead
shows clickable key containers around non-clickable digit text. The accepted r3
waits for observed Home and walks from the exact label to its clickable ancestor.
No application code was changed to make the test pass. Partial r1/r2 folders
remain diagnostic and are not counted as passing evidence.

Reproduce with `p01_ios_acceptance.py --keypad-only` using a fresh run name and the
verified Pro Max UUID; repeat with `--case unknown-large`. Android uses
`p01_android_keypad.py --run <new-name>` and checks both fixtures. Build and sign
the current isolated iOS host before running it.
