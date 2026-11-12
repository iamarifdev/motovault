# P07 v3 prototype walkthrough

Reviewed the local v3 UI in the in-app browser on 12 November 2026. Demo data only. This is design/prototype evidence; no real camera, native share transport, microphone, provider, owner record or pilot was exercised here.

- Odometer: explicit demo label; clear/multiple readings; Use disabled until selection; blur/glare and permission states retain manual/retry actions. Save stays disabled while photo review is open, matching the native flow.
- Share: warm/cold/locked/duplicate/unsupported previews. The locked state hides the message input, sample actions, parsed amount and continuation. The warning is visible without exposing the shared text.
- Typed synthetic `Fuel payment Tk 500.50. Balance Tk 2,140.50.`: the preview shows `৳৫০০.৫০`. Capture money uses one formatter with two decimal places and independent display digits.
- Service smart input: provider-processing disclosure and explicit Start are visible in the scrollable sheet. The panel no longer shrinks and clips its voice controls. No browser microphone request was made and speech was not started.
- Prototype speech has no fabricated sentence fallback. Its unavailable branch and native provider failures remain distinct from successful transcription.

The walkthrough found and corrected three design mismatches: Save enabled during photo review, raw text visible in the locked demo, and the expanded voice panel being clipped. Checked the affected states after the changes. TypeScript/Vite production build passed after the final prototype changes.

Changed reference files: `design/app-ui-v3/src/sheets/OdometerSheet.tsx`, `PasteSheet.tsx`, `ServiceSheet.tsx`, `components/SmartInput.tsx`, `data/capture-copy.json` and `lib/captureMoney.ts`.
