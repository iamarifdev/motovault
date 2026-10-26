# P02 privacy prototype walkthrough

27 October 2026 (Asia/Dhaka). Reference: active `design/app-ui-v3`, extended with
Privacy under Me, masked Vault cards, authentication/migration sheets and a
standalone selected-document surface. Original P00 reference hashes are retained;
the P02 extension has its own source/build/screenshot fingerprint.

Browser walkthrough used synthetic P00 records only. The prototype explicitly
states that OS authentication is simulated and browser files/data are not being
encrypted. Native implementation/evidence remains required.

## Observed states and behavior

1. Me opens the privacy review with scope and lost-key help. Dark theme, 1.3 text,
   English digits and reduced motion were active initially.
2. Authentication cancellation returned to disabled protection. Simulated
   biometric lockout retained PIN/passcode fallback. No device credential disabled
   the simulated grant and explained the manual settings path.
3. Simulated migration failure retained disabled protection and exposed retry.
   Retry required authentication and returned to verification; success enabled
   the demo preference. No fabricated progress percentage was shown.
4. Lock now removed owner panels, sheets and tab navigation from the rendered
   accessibility surface. Reload started locked; an unlock was never serialized.
5. After unlock, Vault summaries hid document numbers and images. Opening one
   document and selecting handover replaced the owner surface with a frozen copy
   of that one synthetic document. Only its image/accessible description and exit
   actions remained. Other document types, editing/sharing and tab controls were
   absent from the observed accessibility surface.
6. Leaving handover requested simulated owner authentication. Cancellation stayed
   on the same selected document. Escape cleared it and returned to the locked
   surface rather than exposing owner navigation.
7. Light theme, normal text and Bangla digits were also reviewed. At the configured
   one-minute idle limit, a passive 60-second observation confirmed the owner
   settings sheet was replaced by the locked surface without input.

The browser host's separate visible tabs did not produce an observable document-
hidden transition. Background handling is implemented but was **not** established
by that attempt. Native background/resume/snapshot behavior remains a P02-03/05
acceptance gate. Empty-state text exists, but a complete empty/system-theme and
motion recording matrix has not yet been captured.

## Findings fixed during walkthrough

- Owner panels and tabs remained exposed to accessibility behind a sheet. They
  are now inert/hidden while a sheet is open.
- Privacy cards could shrink and clip their buttons/body at large text. The
  privacy sheet now keeps content height and scrolls it beneath its fixed footer.
- The selected-document art lacked a spoken description. The chosen synthetic
  document now exposes a single description while masked cards expose no numbers.

Screenshots in `p02/prototype/` show migration failure, locked, selected-document,
light settings and observed idle-lock states. They are browser design evidence,
not native authentication, encryption or physical-device proof.
