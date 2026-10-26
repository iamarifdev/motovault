# P01 Android sheet window motion

26 October 2026. Base `48fce40321600e4fbdfe124a3bb63b3032276502` plus the
uncommitted P01 follow-up. All runtime evidence uses synthetic data in
`com.motovault.baseline.p00` on the native ARM64 Pixel 10 API 37.1 emulator.

## Observed defect and correction

Higher-density motion recordings exposed an additional platform window
transition. With app reduced motion off and OS animator scale 0, the custom
sheet translation and count-up stopped, but Android's default dialog animation
still faded/scaled the complete sheet. The separate window animation did not
follow the app's effective reduced-motion policy.

`SheetWindowPolicy` now disables Android window animations for this custom sheet
dialog. The existing Compose sheet transition remains the single owner of normal
entrance/exit motion. Reduced motion skips that custom transition and count-up.
The iOS implementation is empty; it adds no iOS window behavior. No motion tokens,
normal spring parameters, document viewer, persistence or reusable design-system
models were changed.

The new sheet hook lives inside the Dialog and configures its own window rather
than changing the activity or global OS animation scales. The OS setting is
restored after the live-change recording sequence.

## Retained recordings

Capture size is 540x1212, using native `screenrecord`. The earlier full-resolution
recordings had too few frames to diagnose fine window motion reliably. Reducing
recording resolution improved temporal coverage while preserving the unchanged
1080x2424 native layout.

| Run | Normal | OS reduced | OS restored |
| --- | --- | --- | --- |
| Before correction | [96 frames](p01/android/motion-os-native-size-r1/normal.mp4) | [41 frames](p01/android/motion-os-native-size-r1/os-reduced-live.mp4) | [95 frames](p01/android/motion-os-native-size-r1/os-restored-live.mp4) |
| After correction | [87 frames](p01/android/motion-os-native-size-r2/normal.mp4) | [28 frames](p01/android/motion-os-native-size-r2/os-reduced-live.mp4) | [84 frames](p01/android/motion-os-native-size-r2/os-restored-live.mp4) |

Visual review of the sampled [before](p01/android/motion-os-native-size-r2/reduced-entry-before.png)
and [after](p01/android/motion-os-native-size-r2/reduced-entry-after.png) entrance
strips confirms the translucent platform scale/fade is removed. Normal recordings
retain the custom entrance, count-up and exit; reduced recordings show the final
number immediately and dismiss without the custom slide. Both strips sample
0.68 to 1.18 seconds at 30 samples/second; repeated samples are not new source
frames or proof of a fixed recording FPS. Initial native layout/inset settling
is not claimed to be pixel-identical to the browser reference.

Frame timestamps show dense active sequences separated by static-screen gaps.
For example the corrected normal entrance/count-up cluster contains 57 frames
from 0.713244 to 1.731711 seconds. Its exit cluster contains 23 frames from
5.101467 to 5.480044 seconds. These are capture clusters, not measured animation
durations. Developer-command return times are not animation latency.

[Observations](p01/android/motion-os-native-size-r2/observations.json) retain the
live 1/0/1 OS scale sequence with app reduced motion false. The original setting
was unset (`null`) and was restored to unset. The earlier source/artifact
collection is retained as [pre-correction provenance](p01/provenance-before-sheet-policy.json).
The current installed APK and local built APK had the same SHA-256:
`6f504c6843bd78e085433d4f923d813d49aa619cad49b3b6fb48efe2d2c8445f`.

## Verification boundary

Android assembly and 148 shared/JVM tests pass after the correction. All 170 iOS
ARM64 tests also pass on the explicitly selected iOS 27 iPhone 18 Pro Max.
Current XML is retained under `p01/tests/`. Current-build native Back and keypad
regressions are retained separately from the pre-correction layout evidence.

- Android Back: [normal](p01/android/navigation-runtime-sheet-policy-r1/result.json)
  and [large/dark](p01/android/navigation-unknown-large-sheet-policy-r1/result.json),
  with identical persisted before/after snapshots in both directories.
- Android keypad: [normal](p01/android/keypad-runtime-sheet-policy-r1/result.json)
  and [large/dark](p01/android/keypad-unknown-large-sheet-policy-r1/result.json),
  with hidden OS keyboard, measured key/Save bounds and identical persisted snapshots.
- Rebuilt iOS host: [normal](p01/ios/acceptance-keypad-sheet-policy-r1/summary.json)
  and [large/dark](p01/ios/acceptance-keypad-large-sheet-policy-r1/summary.json)
  keypad/cancellation tests both pass. Their `run.json` files retain the new host
  executable and Kotlin implementation hashes.

This closes the observed duplicate Android dialog-window transition defect.
It does not establish full prototype/native press, gauge or spring parity,
spoken screen-reader output, physical haptics or physical notification delivery.
Those acceptance gates remain open, including the owner's deferred device checks.
