# P01 v3 detail corrections, 2026-10-26

## Revision and scope

Owner requested committing/pushing the previous work, then correcting all 17
findings in [the detail audit](V3-DETAIL-AUDIT-2026-10-26.md).

Previous work is committed as `792e63a` (native fixes/tests/tools) and `63198f9`
(evidence/roadmap/audit), and pushed to `origin/main`. These new UI corrections
were captured as a working change based on `63198f9`; subsequent commits are
recorded in Git history. Exact source/artifact hashes
and retained test summaries are in [v3 provenance](p01/v3-details/provenance.json).
The earlier P01 provenance remains historical and was not relabelled as proof of
this build.

The 17 listed source differences have been corrected. This is a bounded detail
fix, not acceptance of complete app-ui-v3 visual, motion or accessibility parity.
Production owner records and the connected physical phone were not modified.
Runtime checks use only `com.motovault.baseline.p00` on an emulator/simulator.

## Finding-by-finding implementation

| Audit ID | Correction | Evidence |
|---|---|---|
| 1 | Home Fuel uses the exact Lucide Fuel path | Android/iOS Home captures |
| 2 | Quick Add Fuel uses the same Fuel artwork | Android/iOS Quick Add captures |
| 3 | History fuel rows use Fuel | Android/iOS History captures |
| 4 | Home/Quick Add/Fuel sheet/Service sheet use Gauge instead of activity | Source mapping; Home/Quick Add/Fuel captures. Service call site inspected, not separately captured |
| 5 | Vault navigation uses FolderOpen | Both hosts' Home/History navigation captures |
| 6 | Home quick-action icons use 26-unit design size | Source and both Home captures |
| 7 | Tab icons use 28-unit size, stroke2 inactive/2.4 selected | Both hosts' navigation captures; vectors accept explicit stroke width |
| 8 | One shared 28x4 marker animates between slots with the effective reduced-motion preference | Android normal recording and extracted frames; large/reduced settled captures |
| 9 | Centre plus is 60 inside a 5-unit surface ring, top -18 for the coloured circle; icon30/stroke2.4; shadow and live press state | Both hosts' Home captures; source. Native shadow rendering is an approximation of CSS blur, not pixel-identical blur proof |
| 10 | Quick Add icons28, heading18, gap4 and equal-height cards in each row | Both hosts' Quick Add captures; Android wrapping review |
| 11 | Paste shortcut uses Sparkles20, dashed1.5 border and surface background | Both hosts' Quick Add captures |
| 12 | Empty Vault and scan action both use ScanLine | Android empty-Vault edge capture |
| 13 | Missing-model profile placeholder uses Gauge32 | Android unlisted-model edge capture |
| 14 | First onboarding header uses Bike26 | Android onboarding edge capture; native brand logos preserved |
| 15 | Shared sheet title24/line32.4, minimum header56, padding20/8/4, divider1 | Both hosts' Quick Add/Fuel/Odometer captures; normal/large keypad checks |
| 16 | Dark toast uses #e9ecea background/#181818 text/#1e47a8 Undo; light retains its dark toast | Both hosts' actual save-toast captures |
| 17 | Toast padding16/12/8/14, gap12, text15/line21; Undo minimum40/radius12/padding12/gap6/icon16; progress uses40% text colour | Both hosts' actual save/Undo captures and restart checks |

## Artwork and safeguards

`tools/build-v3-icons.mjs` extracts the exact 31 required icon definitions from
the locally installed `lucide-react 0.468.0`, the dependency used by the v3
prototype. It generates app-owned `V3IconPaths.kt`; `--check` verifies reproducible
paths and the retained ISC license. Native vectors preserve the 24x24 viewport,
round caps/joins, no fill and reference stroke widths. Selected tabs/plus use2.4.

The reusable Sky design system and external asset directory were not edited.
Unmapped existing controls retain their existing Sky asset through `V3Icon`.
Droplets remain appropriate for the fuel-log nudge; Fuel is used specifically
for fuel entry/history. Native brand logos and domain/storage contracts remain
unchanged.

## Verification

- Native Apple Silicon host and Homebrew JDK17.0.20.1; repository Gradle8.10.2
  wrapper retained. No toolchain upgrade was performed.
- Android assembly and iOS simulator ARM64 compilation passed. Android/shared
  tests:148 across21 suites; iOS ARM64 tests:170 across24 suites, zero failures
  or errors. The final equal-height-card/shadow refinement was rebuilt on both
  hosts and covered by the final UI captures below.
- Final Android detail runner passed normal Bangla/light and English/dark/1.3
  reduced states. It captures Home, Quick Add, Fuel, History, Odometer and the
  actual save toast; stored snapshots before Save and after Undo are identical.
- Final iOS detail test passed both equivalent cases on **iOS27 iPhone18 Pro
  Max**. Each checks navigation and real Save/Undo, then terminates/relaunches
  the app and verifies the confirmed odometer is restored.
- Android normal/large keypad bounds, visible Save, hidden system IME and
  cancellation-preserves-data checks passed against the final sheet geometry.
- iOS normal/large keypad bounds, visible Save, hidden platform keyboard and
  cancellation checks are recorded separately in the final acceptance runs.
- The reference browser's p01-runtime fixture was inspected: Home icons resolve
  to Fuel/Wrench/Gauge/FileText, all size26/stroke2. Native Home, Quick Add,
  placeholder/onboarding and light/dark toast captures were visually inspected.
- Normal Android navigation recording retains all four tabs; extracted 10fps
  frames show marker travel from Garage to Vault and a settled marker. This does
  not establish exact spring feel against another rendering engine.

### Final retained runs

- [Android normal details](p01/android/v3-details-runtime-final-r1/result.json)
- [Android large/dark details](p01/android/v3-details-unknown-large-final-r1/result.json)
- [Android empty/placeholder/onboarding](p01/android/v3-edges-final-r1/result.json)
- [Android normal tab recording](p01/android/v3-navigation-final-r1/tab-marker.mp4)
- [Android normal keypad](p01/android/keypad-runtime-v3details-final-r1/result.json)
- [Android large keypad](p01/android/keypad-unknown-large-v3details-final-r1/result.json)
- [iOS normal details](p01/ios/acceptance-v3details-normal-final-r1/summary.json)
- [iOS large/dark details](p01/ios/acceptance-v3details-large-final-r1/summary.json)
- [iOS normal keypad](p01/ios/acceptance-v3keypad-normal-final-r1/summary.json)
- [iOS large keypad](p01/ios/acceptance-v3keypad-large-final-r1/summary.json)

Representative captures:

- [Android corrected Home](p01/android/v3-details-runtime-final-r1/home.png)
- [Android corrected Quick Add](p01/android/v3-details-runtime-final-r1/quick-add.png)
- [Android dark toast](p01/android/v3-details-unknown-large-final-r1/saved-toast.png)
- [iOS corrected Quick Add](p01/ios/acceptance-v3details-normal-final-r1/attachments/A22B1850-9629-4BF6-8E23-1B932FEB018C.png)
- [iOS dark toast](p01/ios/acceptance-v3details-large-final-r1/attachments/259DDC71-0D64-4A44-8F47-019E32817EE2.png)

Initial diagnostic runs are retained rather than presented as passes. Android r1
captured the launcher because the database-read helper stops the test app; the
runner now explicitly reopens it. iOS normal-r1 used an exact Fuel label, but
Compose exposes the Quick Add title plus subtitle as one button label; the
scoped button-prefix selector fixes that harness error. Final runs above pass.

## Remaining phase gates

P01-05 remains in review. Physical iPhone verification, owner-deferred spoken
TalkBack/VoiceOver and haptics, full screen/state visual coverage and fine motion
comparison are not satisfied by these corrections. The earlier physical Android
post-boot delivery limitation also remains recorded in P01-10. No release or
publication claim is made.
