# MotoVault document scanning and authentic layouts

Design proposal, 11 October 2026. Interactive review: [document-flow preview](/Users/arif/workspace/bbn/motovault/docs/document-design/index.html).

## Product direction

Restore the original plan’s document-camera workflow and wallet-style presentation. A saved document should feel like the owner’s physical credential, rather than a generic form inside a card.

The **captured original is the document**. It retains the real printed typography, photo, signature, chip, barcode/QR, seals, colors, proportions, and field positions. The app does not rebuild those elements from recognized text. A separate summary provides readable numbers, dates, renewal status, and search. Correcting a summary field never changes the image.

Bike-brand accents stay on app controls and the wallet frame. System is the default, with Light and Dark overrides. Printed document colors are preserved in every app theme.

## Relationship to the original plan

[CHATGPT_HANDOFF.md](/Users/arif/workspace/bbn/motovault/docs/CHATGPT_HANDOFF.md) describes a wallet stack, distinct registration/licence/tax-token treatments, and front/back interaction. [ARCHITECTURE_AND_HANDOFF.md](/Users/arif/workspace/bbn/motovault/docs/ARCHITECTURE_AND_HANDOFF.md) calls for physical capture and text recognition. The current native app has typed document fields and attachment import, but no document-camera or recognition pipeline.

The old emerald/navy/bronze palette is suitable for the **app’s document-type accents**, not evidence of the credential’s actual printed colors. The public government reference below shows older registration and licence cards with pale green backgrounds and portraits on the left. Layout versions can vary; presenting the owner’s captured original makes the app faithful to whichever version they hold.

## Proposed screens

| Screen | Design and behavior |
| --- | --- |
| Vault | Overlapping wallet cards with a preview of the original, document type, and expiry status. Registration gets an emerald type marker, licence a blue marker, tax token a bronze marker. The selected bike still controls the app accent. |
| Add document | Select registration, driving licence, tax token, insurance, or fitness. Scan a physical original, import existing images/PDF, or enter details manually. Card and paper formats are treated differently. |
| Capture | Guided framing, complete document edges, focus/glare feedback, and a visible front/back step. Physical cards use their landscape proportions. Paper certificates keep their complete page. |
| Review capture | Check all edges, rotate, adjust crop, or retake. Keep original color and detail. Front/back thumbnails clearly show missing sides. Retain the raw image as well as the accepted crop. |
| Confirm details | Recognized candidates are editable. Unreadable, missing, or ambiguous fields get a review state. Required fields and date ordering are validated. A person confirms the values before saving. |
| Saved original | Front/back controls, full-size viewing, pinch-to-zoom/pan, and a separate readable summary. Replacing a scan and editing extracted details are separate actions. |
| Inspection | Original images and document details only. No service history, fuel costs, or unrelated personal information. The light inspection surface uses contrasting controls. |

The interactive preview includes registration, licence, and tax-token examples, clear/glare capture states, front/back capture, review confirmation, original-versus-summary separation, and enlarged viewing. Its sample layouts demonstrate hierarchy and proportions; they are not certified current BRTA templates.

## Layout requirements

### Registration and driving licence

- Preserve the source image’s aspect ratio. Never stretch a card to fill a tall container.
- Keep its portrait/photo, authority heading, bilingual labels, numbers, and signature where they actually appear.
- Back-side chip, address, barcode, and vehicle/class information remain part of the original image. Do not regenerate codes or move them to a generic template.
- Fit the complete card initially; provide deliberate zoom for the small printed fields.
- Show important readable details below the scan, using document-specific fields rather than one generic holder/number pair.

### Tax token, insurance, fitness, and e-documents

- Preserve the original page ratio and number of pages. Do not force paper documents into a credit-card frame.
- Keep the full header, tables, signatures, payment details, and original QR code visible.
- An imported PDF remains a document attachment; generated summaries do not replace its original pages.
- Show renewal information outside the original page. Validity labels describe expiry, not an authenticity assessment.

## Capture and recognition behavior

1. Explain the chosen document’s capture requirements before launching the camera.
2. Capture/import the original, then let the owner accept a crop and orientation.
3. Read available text locally. Map candidates using labels, position, and document type; the old handoff’s regex examples are only tentative patterns.
4. Highlight fields that need human confirmation, especially names, document numbers, registration/chassis/engine numbers, and ambiguous dates.
5. Keep the scan usable even if text recognition fails. Offer manual entry and save the original.
6. Save only after required values and dates are checked. A later scan replacement must not silently overwrite confirmed details.

Google’s current text-recognition list does not include Bengali. Latin fields can be attempted with ML Kit, but Bangla names/text require manual fallback or a separately validated Bengali-capable recognizer. Script/digit normalization belongs in the extracted summary, while the original remains unchanged. A language-identification API supporting Bengali does not establish Bengali OCR support.

## Native implementation boundary

- **Shared Compose:** document-type choice, capture guidance, scan review, confidence/review states, typed confirmation, wallet cards, original viewer, and inspection.
- **Android:** evaluate ML Kit’s on-device document scanner for detection/crop/rotation and text recognition for supported scripts. Its camera flow has its own platform UI and limited customization. Use a CameraX adapter if the exact branded camera overlay in this proposal is required; do not assume the platform scanner can be fully reskinned.
- **iOS:** VisionKit supplies page-by-page camera capture. Apple Vision supplies recognition, with supported recognition languages checked for the deployed OS. The checkout currently has no iOS host app.
- **Local storage:** store raw originals, accepted crops, and thumbnails as app-owned files. Keep file identifiers, type, side/page order, confirmed fields, extraction candidates, and timestamps in the document record. Avoid storing full-resolution originals in the current 1 MB Base64 preference-style attachment format. Plan migration of existing attachments without losing records.
- **Verification links:** opening an issuer’s check page is an explicit action. Reading a barcode or saving a scan is not itself proof of issuer verification. BRTA offers its own QR-certificate checking entry point.

The camera image in this preview is a design simulation, not an implemented scanner. No camera permission, recognition model, or native dependency was added in this design pass.

## States to cover before shipping

- Camera unavailable/permission denied, scanner resources not ready, capture cancelled.
- Glare, blur, clipped corners, wrong orientation, wrong document type, missing back, and multi-page paper.
- Unreadable text, unsupported script, ambiguous dates, differing front/back candidates, and duplicate document numbers.
- Save failure, missing attachment file, replacing an original, and retaining the original after field corrections.
- Light/Dark/System, text contrast, large text, reduced motion, landscape, and usable full-size document viewing.

## Real references

- [Road Transport and Highways Division’s official 2015 booklet, pages 52–53](https://eservices.rthd.gov.bd/assets/docs/Development%20Forum%20Booklet_2015.pdf#page=52): historical front/back registration and driving-licence examples. This is a layout reference, not confirmation of every current card version.
- [BRTA certificate QR scanner](https://bsp.brta.gov.bd/qr/qrScanner?lan=en): official verification entry for BSP-generated e-tax, e-driving-licence, and e-fitness certificates.
- [BRTA fitness and tax-token services](https://www.bsp.brta.gov.bd/bsp/fitnessTaxToken?lan=en): certificate/document context.
- [Google ML Kit document scanner](https://developers.google.com/ml-kit/vision/doc-scanner): on-device capture, automatic detection, crop, rotation, and editing capabilities.
- [ML Kit text-recognition languages](https://developers.google.com/ml-kit/vision/text-recognition/v2/languages): actual script/language coverage.
- [Apple VisionKit](https://developer.apple.com/documentation/visionkit): native document-camera capture.

## Preview verification

- JavaScript syntax check passed.
- Browser checks covered scan entry, glare blocking capture, front/back progression, capture review, confirmation before save, original/summary separation, full-size viewing, and inspection mode.
- A corrected sample document number appeared in the summary while the original sample card kept its original number.
- Light/Dark/System controls were exercised. Inspection uses readable light-surface actions even with a dark app theme.
- A 390 px viewport check showed no horizontal page overflow.
- No browser console errors were observed in the checked flow.

Preview images: [wallet](/Users/arif/workspace/bbn/motovault/docs/document-design/wallet-preview.jpg), [front capture](/Users/arif/workspace/bbn/motovault/docs/document-design/scan-front-preview.jpg), [detail review](/Users/arif/workspace/bbn/motovault/docs/document-design/review-preview.jpg), [inspection](/Users/arif/workspace/bbn/motovault/docs/document-design/inspection-preview.jpg), and [phone viewport](/Users/arif/workspace/bbn/motovault/docs/document-design/mobile-preview.jpg).

The design direction is ready to review before changing the native document flow. Camera/OCR implementation and its device verification remain outstanding.
