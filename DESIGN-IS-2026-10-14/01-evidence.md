# Whole-app expert review

Audience revision: easy, modern Bangla is primary and English secondary. See 06-bangla-first-revision.md, which supersedes English-first assumptions. The examined English interface is an additional audience-fit problem; local motorcycle selection alone is insufficient.

Overall judgment: MotoVault has a distinctive, coherent visual identity and credible practical value, but completing and correcting everyday tasks takes too much effort. Prioritize the journeys and recovery states before further visual polish. Engagement should come from useful reminders, quick logging and clear ownership progress.

This is a heuristic review, not a usability study or release certification. Current source was inspected across the active native app. Existing screenshots were visually inspected; they are historical. Live simulator inspection reached the iOS home screen only, so no current app interaction, physical-camera quality, screen-reader behavior, task completion time or retention was measured. No product code was changed.

## Source key

Source paths below resolve under `composeApp/src/commonMain/kotlin/com/motovault/app/` unless a platform prefix is specified. `ui/` is abbreviated UI. Screenshots resolve from the repository root. Active entrypoint: App.kt:6-7.

## E1. First use and onboarding

Strengths: nine relevant motorcycle brands, real model photographs, searchable model selection, no-results guidance and manual fallback (UI/MotoVault.kt:53-62; UI/Onboarding.kt:165-197). Brand personalization gives emotional ownership without needing a social feed.

Friction: ownership setup contains seven text fields, two radio groups and a mandatory schedule/baseline confirmation, plus conditional factory controls (UI/Onboarding.kt:110-145). There is no clear unknown-last-service route. Defaults use zero mileage and today's baseline date (:45-49). A first-time user may not know these facts, yet must make decisions before gaining value.

Recommendation: let users establish bike identity and current mileage first. Guide maintenance setup later; represent unknown service history honestly. Keep the photo-led picker.

## E2. Garage, hierarchy and repeated tasks

The three labelled tabs Garage, Vault and History are a strong information architecture (UI/MotoVault.kt:65,150-153). Shared buttons and forms provide consistency (:317-345).

The identity header and 190dp bike photo precede practical metrics, maintenance and log actions (:206-243,265). In `docs/archive/evidence/continuation-verification/03-dark-garage.png`, the photo occupies substantial screen space and maintenance is partly below the fold. This is attractive for ownership identity, but weak for a quick repeated visit.

The highest-wear component always receives warning-panel styling, including a Healthy result (:231-239). Percent gauges labelled Component health are schedule estimates, not measured mechanical condition (:220-237,277-289). Explain 'Service schedule remaining' and style urgency according to actual thresholds.

Recommendation: place the most urgent task and Log fuel/Log service where users can reach them immediately. Keep the bike identity in a smaller supporting role. Avoid alarm styling for normal conditions.

## E3. Logging and correction

Fuel calculation is explained well (UI/OwnershipForms.kt:43); service explicitly says it updates the baseline (:92). Save errors protect prior records (:52,105,136). These are useful trust details.

Service mileage above the stored odometer is rejected (UI/VaultState.kt:88) even though the form asks for actual mileage (UI/OwnershipForms.kt:92-104). A rider logging 5,200km against a garage reading of 5,000km must leave, update Garage and repeat. Offer a combined odometer update.

History exposes Delete but no edit (UI/OwnershipForms.kt:155-167). Deleting the latest baseline-establishing service is refused unless the schedule baseline is adjusted first (UI/VaultState.kt:106-111). Correcting a typo should be a guided operation, not require understanding internal dependencies.

Four ownership dates are exact-format text fields (UI/Onboarding.kt:138; UI/OwnershipForms.kt:45,98,132). Save gates parse values but provide no field-specific invalid-state feedback (:50,103,134; UI/MotoVault.kt:328-329). Use date pickers, inline explanations and field-level errors.

## E4. History and long-term engagement

Empty history offers a next task (UI/OwnershipForms.kt:154). Fuel, service and document renewal provide legitimate reasons to return. There is no need to add streaks or artificial urgency.

Entries use eager `logs.forEach` and logging actions follow every entry (:153-167), within the parent scroll container (UI/MotoVault.kt:138). Long ownership histories become harder to navigate; no date-range, component filter or search is exposed here. Tab selection resets the History filter to All (:150-153).

Recommendation: persistent logging actions, retained filters, scalable history and meaningful comparisons such as expense or fuel trends. Test usefulness with riders before adding more dashboard metrics. Engagement and retention remain unmeasured.

## E5. Documents and inspection

Strong features: owner confirmation of OCR suggestions, manual correction and optional extra details (UI/DocumentFlow.kt:253-268), original access (:302), and a clear inspection limitation/originals reminder (:318).

Stacked cards overlap by 36dp and later cards have 80dp previews (UI/DocumentWallet.kt:27,38). `docs/archive/evidence/continuation-verification/04-dark-vault.png` confirms the attractive wallet metaphor but also concealed content. Show a compact readable list when users need to compare documents, expiry dates or duplicates.

Document dates require YYYY-MM-DD typing (UI/DocumentFlow.kt:259-260). Save needs confirmation, number and holder without a clear explanation of why it is disabled (:270). Add required labels and local validation.

The document flow displays Garage/Vault/History, but Garage and History are disabled; Vault dismisses the flow (:59). Leaving discards draft assets (:115-116). This risks navigation expectations and lost work. Use an explicit Back/Close action and warn when a meaningful draft would be discarded.

## E6. Camera and import risks

Capture is hard-gated on glare, darkness or blur (UI/DocumentCapture.kt:53). Android's glare heuristic is a bright-pixel threshold, not recognition of reflected light (`composeApp/src/androidMain/kotlin/com/motovault/app/ui/DocumentCamera.kt:63-72`). UI states 'Glare detected' and disables capture (UI/DocumentFlow.kt:175-176). White paper may trigger this. This is a source-derived risk, not a reproduced defect. Offer an actionable soft warning and override when the camera is ready.

iOS allows an image picker but accepts only JPG/JPEG/PNG/WebP extensions (`composeApp/src/iosMain/kotlin/com/motovault/app/ui/DocumentCapture.kt:169,181-183`). HEIC rejection is a device-test priority. Camera-denied copy offers import but no direct camera-settings recovery (Android DocumentCamera.kt:109; iOS DocumentCamera.kt:74).

## E7. Trust, privacy and recovery

Local-storage labels are honest (UI/DocumentWallet.kt:22; UI/DocumentFlow.kt:55). Reminders are opt-in and permission failure is handled with settings recovery and safe-save copy (Android RenewalReminders.kt:125-136; iOS equivalent:110-123). Export explains owner-entered records and certification limitations (UI/PassportExportUi.kt:24,43-54).

The export interface exposes JSON, SHA-256 and digest terminology (:23,71,88-95). Put plain-language verification first and move technical details behind disclosure.

Restoration failure publishes an empty state and blocks every commit (storage/VaultSession.kt:38-47). The UI shows an error but routes the user into Setup (UI/MotoVault.kt:128-136). There is no actionable retry/recovery in that branch. Provide a dedicated recovery state before allowing setup.

No general backup/recovery affordance was found in the examined redesign UI. 'On this device' should lead to an understandable explanation of device loss, uninstall and recovery. A service-passport export should not be presented as full backup.

## E8. Accessibility and visual evidence

Historical continuation screenshots show coherent light/dark surfaces, brand accents, rounded cards, real photos and restrained navigation. `docs/archive/evidence/theme-verification/12-light-setup.png` shows clear selection and a visible Continue action. Earlier `docs/archive/evidence/contrast-audit/` images contain stale rendering issues; they are not accepted as current defects.

The custom document + action is a 36dp Box with no contentDescription/role (UI/DocumentWallet.kt:23); a full primary scan action also exists (:54). Crop handles are drawn at 7dp, with no semantic alternative in the crop Canvas (UI/DocumentFlow.kt:219-232). The handles' drawn size is not their full drag hit area; do not equate those measurements.

DocumentField places a visual label separately from BasicTextField without an explicit semantic association or tailored keyboard options (:237-241). Screen-reader naming is a risk requiring VoiceOver/TalkBack testing. Small 8-12sp supporting text is widespread; large-font screenshots cannot establish all flows remain usable.

Loading, success, disabled, error and empty states exist (UI/MotoVault.kt:108-114,128-130,194-195; UI/OwnershipForms.kt:50-54,154). Field-specific errors, recovery and nonvisual crop operation are incomplete. Keyboard/focus and screen-reader behavior were not tested. Contrast calculations and further token evidence are recorded in the scorecard if supplied; no full WCAG conformance claim is made.

## E9. Weight, attention and evidence gaps

No idle animation loop was found in examined redesign UI. Camera analysis is limited to active capture (Android DocumentCamera.kt:58). Brand imagery increases native asset weight, but installed size, decoded memory, energy and startup latency were not measured. Web JS bytes, request count, ARIA landmarks and skip links are not applicable to this native app.

Exact whole-app interactive-element count, maximal composed tree depth, unused imports, every rendered string, exhaustive contrast and device performance are not established. Counts from E1-E4 are scoped inventory, not runtime totals.

## Recommended acceptance checks

1. New owner with unknown service history reaches a useful garage without inventing a baseline.
2. Returning rider logs fuel and service quickly, including mileage above the previous stored reading.
3. Rider corrects the latest maintenance record without losing schedule integrity.
4. Document draft survives accidental navigation or warns clearly before discard.
5. White paper under normal light can be captured; HEIC imports work on a physical iPhone or explain supported conversion before selection.
6. Restore failure offers an actionable recovery path and never looks like a normal empty garage.
7. VoiceOver/TalkBack users can name fields, add a document and complete or bypass cropping; large text and smallest supported phones retain all actions.
8. Long history keeps logging and retrieval efficient. All urgency copy distinguishes schedule estimates from measured condition.
