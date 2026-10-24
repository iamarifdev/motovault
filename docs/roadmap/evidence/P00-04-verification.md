# P00-04: baseline capture verification

Date: 24 October 2026, Asia/Dhaka. Owner/reviewer: Codex. Source baseline:
`main` / `50eda3a` plus P00 fixtures, review controls, test harness and reports.
Production main-source behavior is unchanged. No commit or publication was made.

## Required baseline coverage

| Requirement | Authoritative evidence | Result |
| --- | --- | --- |
| Sanitized empty/overdue/partial-fill/document/unknown-history inputs | [Paired manifest](../fixtures/p00/manifest.json), source/export/native fixture equality check | Passed |
| Same underlying data on all three surfaces | Five labelled captures per surface plus native/prototype field-parity and DTO tests | Passed |
| Android emulator screens and semantics | [Android report](P00-04-android.md), PNG/XML files | Passed as baseline observations |
| iOS simulator screens and semantics | [iOS report](P00-04-ios.md), five passing scenario summaries and exported attachments | Passed as baseline observations |
| Sheet, gauge, Undo and reduced-motion records | Continuous native movies, timestamped prototype clips and actual flag audit | Passed as baseline observations; native Undo remains a recorded defect |
| Build/device/OS/source provenance | [Runtime metadata](p00/platforms.json), retained build/test logs and [reference hashes](p00/design-source-manifest.json) | Passed |
| Record discrepancies instead of inheriting old proof | [Comparison and discrepancy register](P00-04-design.md), actual failed Undo and settings observations | Passed |

This acceptance table is for producing a reliable baseline. It does not accept
known product defects. The final iOS motion run is `motion-r7`: the visible blue
on-pill and committed `reduceMotion=true` were checked separately from XCTest's
pass status. Android's committed flag is also true. Future reliability/design
tasks must still fix the recorded product issues.

## Review entry points

- [Side-by-side local capture index](p00/index.html).
- [Prototype/native comparison and motion links](P00-04-design.md).
- [Shared data and migration contract](../DATA-MIGRATION-CONTRACT.md).
- [P00-05 source/fixture review](P00-05-review.md).

## Checks performed

- Isolated Android debug build and iOS simulator build succeeded.
- Prototype type-check and production build succeeded after adding the local
  fixture selector outside the phone UI.
- Seven Android/JVM and seven iOS simulator compatibility tests passed.
- Eighteen roadmap integrity/CLI regression tests passed.
- Paired inputs, generated native fixtures, money examples, asset/source hashes
  and existing migration SQL checks passed. SQL checks are host SQLite scope.
- Media inspection decodes a frame and records actual dimensions/duration/rate.
  Native capture rate and prototype sampling rate are not app rendering benchmarks.

The final `tools/baseline/verify_p00.py` audit checks screenshot/tree presence,
the exact test counts, final capture summaries, actual reduced-motion flags,
usable media and source/artifact hashes. Its evidence checks are more specific
than the roadmap metadata validator. The retained baseline intentionally asserts
the observed native Undo failure; it never counts that as feature acceptance.

## Important findings for subsequent phases

1. Native odometer Undo did not restore 12340 after saving 12350; the unchanged
   durable value is recorded on Android and iOS. Source callback/revision tracing
   points to a stale inverse-command revision.
2. Current Room reconstruction does not round-trip several v3 preferences. A
   serialized setting in an outbox is not the same as restoring it into the app.
3. Native expiry/no-expiry labels, English titles, Vault card structure and some
   iOS accessibility/hit geometry differ from v3 and need the linked follow-ups.
4. Unknown service history needs an explicit data state; current no-rule input
   cannot express that certainty distinction.

No real owner records, credentials or precise locations were used as fixtures.
Original app records were not cleared/reseeded by the tooling. The current phase
does not implement encryption, portable recovery, physical-device verification
or cloud/release behavior.
