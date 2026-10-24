# P00-04: prototype and native comparison

Reference: app-ui-v3 plus a review-panel-only synthetic fixture selector. The
phone's feature screens, visual tokens, typography, motion source and native
brand-logo exception were not redesigned in P00. The selector is outside the
phone UI and never touches Kotlin owner data.

[Current reference fingerprints](p00/design-source-manifest.json) preserve the
initial source comparison. The only changed file among the original 42 inputs
is `src/components/DevPanel.tsx`, which adds fixture loading. Generated local
fixture data is separate and synthetic.

## Matching input and baseline states

The same five underlying scenarios were mapped explicitly to native DTO and
prototype fields. IDs, mileage, costs, full/partial flags and dates are checked
by the fixture tool. Runtime clocks were observed, not silently overridden.

| Scenario | Prototype | Android | iOS |
| --- | --- | --- | --- |
| Empty | [JPEG](p00/prototype/empty.jpg) | [PNG](p00/android/empty.png) | [PNG](p00/ios/empty.png) |
| Overdue | [JPEG](p00/prototype/overdue.jpg) | [PNG](p00/android/overdue.png) | [PNG](p00/ios/overdue.png) |
| Partial fill | [JPEG](p00/prototype/partial-fill.jpg) | [PNG](p00/android/partial-fill.png) | [PNG](p00/ios/partial-fill.png) |
| Documents | [JPEG](p00/prototype/documents.jpg) | [PNG](p00/android/documents.png) | [PNG](p00/ios/documents.png) |
| Unknown service | [JPEG](p00/prototype/unknown-service.jpg) | [PNG](p00/android/unknown-service.png) | [PNG](p00/ios/unknown-service.png) |

Prototype viewport is 440 x 956 CSS pixels. Android and iOS retain their actual
native framebuffer dimensions and safe areas. These are labelled comparisons,
not a pixel-diff assertion across different devices. Screenshots were inspected
alongside their semantic hierarchies and fixture data.

## Motion evidence

- Native Android and iOS continuous recordings are linked from their runtime
  reports. Both cover initial gauges/count-up and normal sheets; the final
  reduced-motion path was checked against both the rendered switch state and
  committed payload; iOS final acceptance uses `motion-r7`.
- Prototype sequences use 78 actual CUA screenshots with measured timestamps,
  encoded by the installed native AVFoundation framework. They are sampled
  transition recordings, not a continuous 60-fps browser recording or proof of
  physical-device feel. [Sample timestamps](p00/prototype/motion-capture.json).
  Browser screenshots retain their original CUA JPEG bytes; their extensions
  were normalized without re-encoding after the format check.
- [Gauge/count-up](p00/prototype/gauge-and-countup.mp4),
  [normal sheet](p00/prototype/normal-sheet-open.mp4),
  [save/Undo](p00/prototype/save-undo-retry-toast.mp4),
  [Undo restoring 12340](p00/prototype/undo-restores-12340.mp4),
  [reduced open](p00/prototype/reduced-motion-sheet-open.mp4),
  [reduced close](p00/prototype/reduced-motion-sheet-close.mp4).
- [Media inspection](p00/media-inspection.jsonl) records actual duration,
  dimensions, nominal capture rate and successful decoding of a video frame.
  That validates usable media, not visual parity or animation smoothness.

## Differences retained for later implementation

| ID | Observation | Follow-up |
| --- | --- | --- |
| P00-D01 | Native Undo does not restore the saved odometer; prototype Undo does | P01-02: fix inverse-command revision handling while retaining concurrency checks |
| P00-D02 | Exposed DTO settings are not all round-tripped by Room; native switches can differ from the fixture/default prototype state | P01-01/P02/P06: durable settings/consent migration |
| P00-D03 | Empty schedules and unknown history cannot be distinguished in current persisted models | P04-03: explicit baseline certainty |
| P00-D04 | Native rule and document row titles may remain English while prototype labels are Bangla | P01-04/P05-03: source-backed labels and v3 language consistency |
| P00-D05 | A registration without an expiry can show 0 days remaining natively instead of a no-expiry state | P01-01/P01-03: expiry-policy/display consistency |
| P00-D06 | Native Vault uses large wallet previews where v3 has compact document rows | P05-03: align the visible Vault pattern while preserving original-view access |
| P00-D07 | iOS exposes some clickable chip/save labels as Other children of unnamed buttons; the visible reduced-motion row can have a zero-sized accessibility frame | P08-02 and affected UI tasks: native accessibility validation and labelled/hittable controls |

P00 deliberately records these mismatches. Its completion is not a declaration
that the existing implementation matches every v3 detail or that these defects
have been fixed. The full theme/brand/text-scale/accessibility and physical-device
matrix remains in feature and release gates.
