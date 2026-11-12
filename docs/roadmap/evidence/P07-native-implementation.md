# P07 camera, share and voice implementation

Scope: local implementation and review. Physical Android/iPhone and representative-owner results remain required in P07, as confirmed by the owner. This report does not close a device or pilot gate.

## Boundaries and data

- `domain/capture` owns candidates, ambiguity, bounds, service IDs and recent accepted-message fingerprints. JVM and Kotlin/Native use character categories for word boundaries; no Unicode regex property is required.
- `application/capture` injects production SHA-256 and holds one explicit incoming share. Acquirers cannot write the vault. A share corrected in review retains the original incoming message identity.
- The unlocked owner receives a waiting share only after loading, ride setup and any current sheet finish. Inspection mode cannot consume it. Type/paste/voice text can be corrected before choosing an existing fuel/service/odometer/expense sheet.
- Successful fuel preparation adds the receipt in `FuelEntryAdapter`; other reviewed sheets add it in the root commit boundary. Both use the same revision-aware writer and durable Undo. Failure/cancel consumes no receipt. The recent duplicate window is bounded at 50 fingerprints.
- Capture prefill preserves poisha and odometer values. Existing payloads default receipt IDs to empty; settings/archive mappings carry them. No Room schema or competing persistence path was added.

## Odometer photo

Android uses permission-on-action plus the native camera result contract. An app-private cache file is downsampled/oriented by the existing image decoder and deleted after decoding. Production bundled ML Kit retains the in-memory bitmap and recognizer until its native Task is terminal, including when the caller cancels. Cancellation suppresses delivery after that cleanup boundary; it does not claim to stop native OCR. Pre-decode failure, camera cancellation, disposal and next-process cleanup also remove pending scratch files. See `P07-02-ocr-lifetime.md` for the focused followup. iOS uses permission-on-action and a camera-only image picker, an in-memory scaled preview and production Vision text recognition. It does not add the photo to the library or document vault.

The shared screen shows the actual preview and valid readings. No reading is selected automatically. The owner explicitly chooses a reading, applies it to the odometer sheet, then uses its existing fixed Save. Save is disabled while the photo review is open. Denial, unavailable camera, failed/no recognition, regression and unsupported readings retain manual entry.

Ephemeral camera acquisition is remembered at the root, outside the private-content visibility gate. Native-camera pause can remove the private sheet while the root retains its callback and pending result. Review waits for owner content to return; closing/changing the sheet discards the result. The privacy policy and final Save path are unchanged. See `P07-02-capture-owner.md` for the bounded source finding and runtime scopes.

## Explicit share

Android exports a text-only ACTION_SEND target. Wrong MIME, attached streams, multiple clip items, invalid/oversized text and a second waiting share are rejected. Nothing reads SMS, a clipboard or other app data in the background.

The iOS `CaptureShare` extension explicitly prepares one plain-text message. Main app and extension use an App Group derived from `MOTOVAULT_BUNDLE_ID`, coordinated atomic writes, complete file protection and backup exclusion. Review requires opening/unlocking MotoVault. The app does not use responder-chain tricks to open another app. Unconsumed messages are not overwritten. Messages older than ten minutes are rejected/deleted on intake or replaced on the next explicit share; no background deletion deadline is claimed. The isolated iOS simulator also exercised the real system share target, extension preparation, backup exclusion and exact production intake/deletion. Its file-protection attribute was unavailable, so complete protection is still a physical-device gate. Real-device App Group provisioning and cold/warm behavior remain open.

## Bangla voice

Both platforms disclose local-only versus the phone speech service before Start. Local-only never silently falls back to the network. Recognition requests bn-BD. Unsupported provider/locale, denied access, failure and no speech have localized manual fallbacks. Stop prepares a transcript; Cancel/disposal/backgrounding ends the session and invalidates stale callbacks. Native capture has a bounded session timeout. The app stores no raw audio; the phone provider may process audio when that mode is explicitly selected.

Android uses on-device SpeechRecognizer only where supported, or the explicitly selected system recognition service. iOS injects a real Swift Speech/AVAudioEngine bridge and checks supported locales and on-device capability. A native provider reporting bn-BD unavailable is a limitation, not successful Bangla recognition. Physical provider/noise/offline/privacy validation remains open. This iOS runtime reports no Bangla locales. The owner requested a separate opt-in provider plan; `../P07-VOICE-PROVIDER-PLAN.md` proposes a controlled Google Cloud STT V2 pilot. That cloud path is not implemented or enabled.

## Design and localization

V3 odometer review includes clear, blur/glare, multiple-reading, permission and manual states. Paste review includes cold/warm/locked/duplicate/unsupported states. Prototype voice no longer fills fabricated speech when unavailable; it discloses provider processing and supports stop/cancel/background cleanup.

New shared copy is paired in English/Bangla resources. Camera, microphone and speech prompts use localized native InfoPlist strings; share labels/errors use native resources. Language and digit style stay independent. Production files/functions pass the shared responsibility gate without a new exception.

Isolated debug fixtures use synthetic data and actual OCR previews. Their in-memory Save is only UI evidence. It is not native persistence, physical camera, speech accuracy, VoiceOver or owner-pilot proof.
