# Document scanning implementation

Implemented on main, October 11, 2026. Application code and tests were committed as `0d687bc`; this report and verification screenshots remain uncommitted. For the remaining product roadmap, see [CONTINUATION_HANDOFF.md](/Users/arif/workspace/bbn/motovault/docs/CONTINUATION_HANDOFF.md).

## Behaviour

- Scan first, with manual entry and photo/PDF import fallbacks. Android uses a custom CameraX preview inside the branded MotoVault flow. iOS has an AVFoundation adapter. Neither relies on the Google scanner screen or its first-use download.
- Five document kinds, front/back cards and up to eight pages for paper documents. Missing backs remain visible as incomplete attachments.
- Raw originals, accepted crops and display previews are separate managed files. Raw imports are copied without modification. Crop handles and 90-degree rotation write accepted copies without changing raw files. Printed colours and field positions stay in the scan; the app does not fabricate official credentials.
- On-device English OCR supplies conservative editable suggestions. Bangla fields require manual entry. Ambiguous day/month dates are left for the owner to enter.
- Explicit confirmation is required before saving a capture. Required fields and issue/expiry date ordering are checked. Summary edits preserve original files and existing document IDs.
- The HTML reference now drives the native document selection, front/back guidance, review, confirmation, wallet stack, original viewer and inspection screens. Wallet cards overlap with fixed emerald, blue and bronze type markers. Tapping opens the original. Document detail includes side/page selection, zoom, raw/accepted-file opening and a separate detail grid.
- Live camera light, overexposure and detail measurements provide capture hints. They are heuristics, not proof that every edge is present or every character is readable. Owners align the frame and check the crop. Focus can be adjusted by tapping the Android preview.
- Uncertain required values have field-level review hints. Optional OCR extras remain candidates until their fields are opened for review. Summary edits preserve the capture timestamp and extraction metadata as well as files.
- System, light and dark themes retain motorcycle brand accents on app controls. Document images are not tinted.
- Older details and Base64 photos remain readable. New originals use managed files instead of embedding large images in the state JSON. Android backups are disabled and FileProvider exposes only the document directory with temporary read grants.
- Discarded review drafts are removed. Replacement/deletion removes old files only after the new state saves successfully. A capture failure before review can leave an unreferenced private file; there is no startup garbage collection.

## Verified

- Android debug APK assembled. iOS ARM64 source compiled.
- 53 common tests passed on each of the Android and iOS simulator targets, including raw/crop references, crop bounds, page replacement and append limits, capture gating, unseen OCR extras, date ambiguity, managed path validation and legacy compatibility.
- Android emulator: the custom camera captured both sides and progressed to review. Crop corner dragging and rotation changed accepted copies. Raw front and back imports remained byte-for-byte identical to their sources.
- Synthetic image imported, English values suggested, confirmation enforced, saved and reopened after process restart. Original PNG matched its source byte for byte.
- Summary holder edited through the prototype confirmation screen. Raw/crop references, expiry, capture timestamp and OCR candidate metadata were unchanged.
- Synthetic two-page insurance PDF imported, OCR reviewed, saved, page 2 selected, original opened in a PDF viewer. Stored PDF matched its source byte for byte.
- The final prototype flow also imported a two-page tax-token PDF, confirmed and saved it, reopened it after reinstall, selected page 2 and opened its original in the native PDF viewer. Original SHA-256: `b386e073c880a9c8b0413eb6af6f3fdabe375613929b602799a3846182ded1f2`, identical to the source.
- Wallet, review, details, editor and full-screen viewer visually inspected. Both system light and dark modes exercised with unchanged image colours.
- Final light document dialog has dark status-bar icons. Android dialog bounds and inset fitting are applied after layout so Compose cannot restore the earlier white-on-white status icons. Final screenshots: `50-light.png`, `51-dark.png` and `52-inspection-system-dark.png`.

Current evidence is in [prototype-verification](archive/evidence/prototype-verification); [scanning-verification](archive/evidence/scanning-verification) records the preceding implementation. Sample documents are marked TEST ONLY. These checks establish the flow, not real-card OCR accuracy or physical-camera quality. iOS has no host app in this repository, so camera and picker adapters have compilation proof only. Android permission, photo-picker and external viewer windows use their operating-system UI; the browser's simulated camera and sample credential artwork are replaced by real camera pixels and stored originals.

APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk.

## Reference coverage

| Prototype screen | Native implementation |
| --- | --- |
| Vault stack | Overlapping original-copy cards, type colours and next renewal |
| Choose document | Registration, licence and tax token, additional types, photo/PDF import and manual entry |
| Capture front/back | Live native camera, alignment guide, light/detail hints and optional back |
| Review capture | Ordered thumbnails, crop handles, rotation, retake and additional paper pages |
| Confirm details | Editable OCR candidates, amber review hints and mandatory owner confirmation |
| Original document | Side/page tabs, stored copy, raw/accepted/PDF actions and two-column summary |
| Full-size view | Dark overlay, pinch zoom and pan |
| Roadside inspection | Light document view with original colours |

This implements the prototype's phone flow with native camera, picker and viewer windows. It does not reproduce the desktop presentation wrapper or substitute the prototype's sample credentials for the owner's actual scans. Physical-camera readability and iOS host integration remain unverified.
