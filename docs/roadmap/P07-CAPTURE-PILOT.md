# P07 capture-pilot protocol for P08-03

Status: Android physical validation is in progress on the owner's connected Motorola edge 50 Fusion; see `evidence/P07-android-motorola.md` for its exact partial scope. Physical iPhone access and representative owners remain pending. These gates remain in P07. No participant comparison, speech accuracy or effort result is recorded by this protocol.

## Physical checks

Use a private synthetic vault. Record device, OS, app source manifest, recognizer/provider and locale availability. Keep owner records and real payment messages out of shared evidence.

| Path | Required checks | Evidence to collect |
| --- | --- | --- |
| Odometer photo | Printed clear display; blur/glare; multiple numbers; below saved reading; unsupported range; camera denial; cancel and retake. Choose a candidate, check the sheet, Save and Undo. | Sanitized result table and screen evidence on physical Android and iPhone. |
| Explicit share | Plain text from a real share sheet; app cold/warm; app locked; already open draft; same message after Save; Undo then retry; oversized/unsupported payload; cancel. | Actual native target, protected review, saved record/receipt and Undo outcomes. Verify iOS App Group provisioning and expiration. |
| Bangla voice | Inspect bn-BD availability first. Test on-device and explicitly selected phone service separately; noisy/quiet input; permission denial; service/network failure; Stop; Cancel; background during speech. | Actual engine/locale, processing choice, transcript corrections, manual fallback, absence of app audio files. An unavailable native locale does not prove Bangla recognition. |
| Parity | Language switch with draft retained, independent input/display digits, native back/focus, fixed Save, reduced motion, larger text, TalkBack/VoiceOver and confirmed Undo. | Physical Android/iPhone results. Simulator screenshots cannot close this row. |

On-device mode must never fall back to a network recognizer. If the native provider does not support bn-BD on a target device, record that limitation; do not mark a successful voice path. Any additional speech provider is a separate implementation/privacy decision.

## Representative owner comparison

Each owner first enters the same synthetic information manually, then tries photo, explicit share and voice where available. Vary the order between owners to reduce learning effects. Record failures and unavailable paths as outcomes, including the manual recovery effort.

| Owner alias | Device/locale/digits | Path | Success after review | Steps to saved record | Elapsed seconds | Corrected fields | Save/Undo result | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |

The table is intentionally empty. Record observed values only. Do not retain raw audio; sanitize transcripts and photos before adding evidence. P07-05 compares success, corrections and effort with each owner's manual baseline, reviews platform limitations and obtains product/design review before completion.
