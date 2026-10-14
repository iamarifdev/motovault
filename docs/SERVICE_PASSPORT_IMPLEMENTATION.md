# Service passport implementation

Implemented 12 October 2026 in the existing History screen. Owner records and document originals are preserved. There is no signing authority or service-certification claim.

## Workflow

History > Export service passport opens an identity review. Owner name, brand, model, edition, registration, chassis and engine are editable. Year and current mileage are shown from the saved motorcycle. Only owner-confirmed registration smart-card details can supply identity defaults. A licence, unseen OCR candidate or unconfirmed document cannot supply those defaults. Because documents can survive changing motorcycles, the review explicitly asks the owner to check they belong to the current bike.

The confirmation checkbox gates the use case as well as the button. Identity changes or a changed vault snapshot clear confirmation. Fields and confirmation are disabled while rendering; completion checks the captured vault snapshot against the latest snapshot. If records changed during suspended rendering, the unpublished stale PDF/JSON are deleted and the owner must review again. Confirm and export creates a PDF and paired JSON. Android offers View PDF, Save PDF, Save verification JSON and Share PDF and JSON. iOS uses a native PDF preview and native share sheet including Save to Files. Errors remain visible and do not alter the saved vault.

Only actual saved non-fuel ledger entries are exported. Schedule baselines are not fabricated as services. Legacy non-fuel history follows the same classification as the existing History screen. Entries sort by parsed local service date, exact mileage and record ID. Costs use BDT. Fuel, fuel efficiency, schedule rules, document numbers and original assets are excluded, except explicitly reviewed registration identity defaults.

## Canonical coverage and verification

Schema: `motovault-service-passport-v1`. The canonical byte sequence is Kotlin serialization JSON for `ServicePassport`, with defaults and nulls written, fixed declaration order, no formatting whitespace, UTF-8 and no Unicode normalization. JSON escaping is part of that byte sequence. Semantic JSON import is decoded and reserialized into this sequence; differences in external file indentation or property order alone do not change covered values.

Covered fields in order:

1. `schema`, `confirmedAt` (the owner-confirmation UTC instant).
2. `identity`: `owner`, `brand`, `model`, `edition`, `year`, `registration`, `chassis`, `engine`, `odometerKmBits`.
3. `services`, in exported chronological order. Each entry covers `id`, `date`, `title`, `detail`, `odometerKmBits`, `costBdtBits`, `workshop`, `notes`, `componentId` (including explicit null).
4. `declaration`, the fixed owner-reviewed, integrity-only statement.

Mileage and cost are the exact saved IEEE-754 binary64 values, represented by sixteen lowercase hexadecimal digits in `*Bits` fields. This avoids locale and decimal-format drift between platforms and covers even fractional changes that display rounding could hide. Rendering decodes those bits into normal visible values. Negative and non-finite values are rejected.

The JSON envelope contains `passport` and the lowercase SHA-256 digest of its canonical bytes. The envelope digest is not itself covered. The PDF layout, bytes, pagination, QR bitmap and filenames are not covered. A PDF can therefore be edited independently; verification explicitly concerns the paired data, not the PDF pixels.

The QR contains `motovault:passport:v1:<64-character digest>`. Native renderers verify the bundle before drawing a QR. History > Verify passport JSON imports the paired file, recomputes SHA-256 and compares its bundled digest. An optional independently supplied digest, or full versioned QR text, must also match. Unsupported schema/QR versions, unknown fields, invalid numbers, altered data and mismatched expected digests fail visibly. Without an independent digest, the result explicitly reports self-consistency only. Somebody changing data and recomputing a bundled digest is not authenticated by this workflow.

The SHA-256 implementation is shared common code using FIPS 180-4 operations; published empty, `abc`, and multi-block test vectors are regression-tested on Android and iOS. An independently computed Python Unicode JSON golden digest pins canonical serialization. Service fields, identity fields, timestamp changes, sorting, exact fractional bits, Unicode/escaping, confirmation and independent-digest comparison have targeted tests.

## Native layout and storage

Android uses `PdfDocument`, Unicode-aware native `StaticLayout`, per-line page splitting and white A4 pages. Long paragraphs can continue onto another page; page footers remain separate. QR uses ZXing core 3.5.3, with vector modules and a quiet zone. Exports live under private `files/passports/`; FileProvider grants only that additional directory. Sharing grants read access to both explicit URIs. Save uses Android's document-create picker. Verification import is bounded to 8 MiB using streams compatible with minimum API 24.

iOS uses a UIKit PDF context, native attributed-string measurement and drawing, composed-character boundaries for wrapping, explicit page breaks and Core Image QR generation. Exports live in application support `passports/`. JSON verification import streams in 8 KiB chunks and checks the 8 MiB limit before each append. Native preview and share adapters compile; actual iOS interaction requires the pending host application.

Private export copies are retained so outstanding view/share URIs remain valid. Exporting does not write Room or change owner identity/history. Export copies contain reviewed owner information and should be shared deliberately.

## Verification evidence

The debug-only `PassportFixtureActivity` generates 70 synthetic chronological services with Bangla, Japanese and accented Latin text, plus a single 100-line note to exercise a paragraph spanning pages. It does not load, replace or save the owner's vault. Every identity/entry is visibly marked SAMPLE. This activity and its exported launch entry are excluded from release builds.

Final source verification: Android debug assembly and iOS ARM64 compilation passed. Android 75 tests and iOS simulator 81 tests passed with zero failures/errors, including nine passport cases on each platform. This final run includes the bounded iOS importer, stale-export guard and independent canonical golden digest.

The Android renderer produced the [70-entry native fixture PDF](archive/evidence/continuation-verification/passports/service-passport-1791749644862-358121829965620573.pdf) and [paired JSON](archive/evidence/continuation-verification/passports/service-passport-1791749644862-358121829965620573.json), with sixteen A4 pages. Native extraction confirms service headings 1 through 70 remain in order, all long-note lines 1 through 100 appear in order, and sixteen distinct page footers are present. PNG inspection covered pages 1, 2, 8, 9 and 16: Bangla/Japanese/accented Latin glyphs are readable, the long note continues cleanly, footers remain separate, and the final digest/QR are legible. Captures are `unicode-01.png`, `unicode-02.png`, `long-note-08.png`, `long-note-09.png` and `final-16.png` in the same evidence directory.

Independent Python canonical JSON plus `hashlib.sha256` matched both exported envelopes. The long fixture digest is `e2f26e6aaa0a082ea205ca7b22953310c96e3485fd34e23d95e7454276ca5db8`. Apple Vision independently decoded the QR from the rendered final-page PNG to the complete matching versioned payload. This is QR raster proof, not an iOS app runtime claim.

The existing emulator vault, with zero saved service entries, also exported through owner review to [a separate native PDF](archive/evidence/continuation-verification/passports/service-passport-1791749782140--3918390498614586548.pdf) and paired JSON. Its independently recomputed digest is `e8dc24c6817ed6bfdcc120ee3b893b741beef76de8ee086d4fcec7bb2f4005fe`. The sample passport owner was edited in the export form; the saved vault was not changed.

The runtime reviewer completed the actual History workflow with editable sample identity defaults, explicit owner confirmation and native export. View PDF opened the native viewer ([capture](archive/evidence/continuation-verification/07-native-passport-viewer.png)). Save PDF and Save verification JSON both completed through the native Downloads provider. Share opened the native chooser with two files ([capture](archive/evidence/continuation-verification/08-passport-share-chooser.png)); the chooser was cancelled and no recipient received anything.

The verifier imported the JSON saved by the native picker and accepted its bundled digest ([capture](archive/evidence/continuation-verification/09-passport-integrity-valid.png)). Supplying a different independent digest rejected the file with the expected mismatch message ([capture](archive/evidence/continuation-verification/10-passport-wrong-digest-rejected.png)). These are actual Android emulator UI checks in addition to the cross-platform canonical/hash regression cases.

The runtime reviewer compared the existing database, thirteen saved document references and twenty-seven baseline asset files after these actions. They remained unchanged. A separate static review confirmed no remaining actionable findings after the bounded-import and stale-completion fixes; caller path, saved-service-only export, covered fields and integrity-only wording were inspected.

The final [iOS native report](archive/evidence/continuation-verification/ios/README.md), dated 14 October 2026, proves an owner-reviewed export from the actual saved synthetic bike (honestly zero service entries), native PDF viewing, paired share sheet and local Save to Files. A separate DEBUG-only isolated fixture rendered 70 records and a 100-line Bengali/emoji note into 16 A4 pages. Text extraction retained every record and the final note; visual checks confirmed Unicode and the final QR. The fixture never reads or writes the owner vault.

Native verifier helper execution accepted valid JSON and rejected a wrong independent digest. This does not prove the Files-picker verification UI. Physical devices remain unverified; synthetic records do not establish real service occurrence or release readiness.

## Primary API references

- [Android PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument)
- [Android StaticLayout](https://developer.android.com/reference/android/text/StaticLayout)
- [Android file sharing](https://developer.android.com/training/secure-file-sharing/share-file)
- [Apple UIKit PDF context](https://developer.apple.com/documentation/uikit/uigraphicsbeginpdfcontexttofile(_:_:_:))
- [Apple UIActivityViewController](https://developer.apple.com/documentation/uikit/uiactivityviewcontroller)
- [ZXing 3.5.3 core artifact and dependencies](https://github.com/zxing/zxing/blob/zxing-3.5.3/core/pom.xml)

These references were checked against official platform/upstream documentation during implementation. The PDF skill was applied for the planned rendered fixture review.
