# P01 representative native matrix

25 October 2026. Base `48fce40321600e4fbdfe124a3bb63b3032276502` plus the
uncommitted P01 follow-up. All data belongs to `com.motovault.baseline.p00`.
The retained r3 captures contain Home, Vault, Me and reminder settings, with
fixture hashes, actual OS appearance and native hierarchies. They establish
representative coverage and usable controls, not complete pixel/motion parity.

| Case | Android emulator (API 37) | iOS 27 iPhone 18 Pro Max |
| --- | --- | --- |
| Light, Bangla digits, normal text/motion | [Metadata](p01/android/matrix-runtime-r3/capture.json), [Vault](p01/android/matrix-runtime-r3/vault.png) | [Passing XCTest](p01/ios/matrix-runtime-r3/summary.json), [Vault](p01/ios/matrix-runtime-r3/matrix-vault.png) |
| Dark, English digits, 1.3 text scale, app reduced motion, unknown service history | [Metadata](p01/android/matrix-unknown-large-r3/capture.json), [Vault](p01/android/matrix-unknown-large-r3/vault.png) | [Passing XCTest](p01/ios/matrix-unknown-large-r3/summary.json), [Vault](p01/ios/matrix-unknown-large-r3/matrix-vault.png) |
| System appearance under OS dark, Bangla digits, normal text | [Metadata](p01/android/matrix-system-appearance-r3/capture.json) | [Passing XCTest](p01/ios/matrix-system-appearance-r3/summary.json) |

Both OS appearance overrides were restored: [Android](p01/android/system-appearance-restoration-r3.json),
[iOS](p01/ios/system-appearance-restoration-r3.json). The iOS destination is
`50709DF0-FEA9-4B34-9DE6-6172D868875D`; no iOS 26 destination was used.
The extra [system fixture](../fixtures/p01/system-appearance.json) retains the
paired data while selecting the existing System appearance in each implementation.

Visual review found two issues and drove corrections:

- The tab transition bypassed the effective reduced-motion policy. It now uses
  that policy, matching v3's zero-duration app-reduced CSS transitions. Earlier
  iOS Me screenshots captured a crossfade and remain diagnostic. Settled r3
  captures expose the intended single page.
- Native document artwork used a fixed 140dp height and stacked metadata. Its
  holder line was clipped. It now follows the reference's 1.586 aspect ratio and
  three label/value rows. Normal and large-text captures on both hosts show the
  holder and expiry. This changes the decorative preview only; owner data and
  document originals are unchanged. The reference is `DocCard.tsx` and the
  `.docart`/`.docart-body` rules in v3 `styles/app.css`.

Earlier r1/r2 artifacts are historical observations. The accepted r3 screenshots
follow the layout correction and include a settled initial iOS Home capture.
Native builds pass after that correction. The shared/native unit suites retained
in `p01/tests/` cover the policy/business work; screenshots and actual interactions
provide the layout evidence.

Remaining acceptance: fine motion comparison with v3, native back/gesture and
screen-reader focus/modal/announcement checks, and the owner-deferred physical
Android/iPhone matrix. These captures do not establish spoken output, haptics,
device delivery or every possible combination of settings.
