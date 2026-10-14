# Apply the approved UI to the native app

This is the implementation boundary map for the user-approved native redesign. Anek Bangla is the selected font and is now bundled in the common Compose resources with its OFL license. The implementation proceeds in reviewable phases while preserving current native behavior and the approved visual direction.

Current entrypoint verified: App.kt → ui.MotoVault(). Current persistence inspected: Room VaultDatabase version 3 with JSON payload rows, not just the older atomic-JSON memory description. Re-read actual DAO write/read and migration before extending data.

| UI reference | Native target | Preserve / required change |
|---|---|---|
| Logo/model picker S02/S26 | Onboarding.kt, BikeCatalogue.kt | Preserve logos, all 78 catalogue records, photos/sources, manual fallback. Apply Bangla copy/type/spacing; no reduced demo list. |
| Font/color/component roles | Appearance.kt, MotoVault.kt shared components | Bundle selected licensed font; semantic typography, localized copy, existing theme/brand semantics. Reuse one icon system. |
| Simple setup S03 | Onboarding.kt, VaultState.create() | Model/current mileage first; optional purchase price/year. Unknown service baseline requires a domain representation, not pretending today/zero is known. |
| Garage S04 | MotoVault.kt Garage | Compact identity, quick actions, owner-entered purchase cost, explicitly scoped expense totals and honest schedule state. |
| Multi-item service S06/S27-S30 | OwnershipForms.kt, VaultState.kt, RideLog | One service event with multiple typed line items, amounts, extra labour, receipt and location. Existing componentId is insufficient. |
| Oil S28 | Proposed typed oil metadata in service line item | Store base type, grade, standards, product, purchase unit, pack volume, bought quantity and used amount separately. Never use type as universal compatibility/interval advice. |
| Fuel S05 | OwnershipForms.kt FuelDialog, VaultState.logFuel() | Fuel type, unit rate, explicit calculated-field UX, previous user suggestion and optional place. Preserve the current economy rules/partial fills. |
| Record correction S31 | OwnershipForms.kt History, VaultState mutation functions | Edit a visit atomically; update every eligible affected schedule using the latest relevant event. Preserve cost once; no duplicated item totals as visits. |
| GPS S16/S32 | LocationCapture.kt + Android/iOS actuals, VaultState.autoLocation, RideLog.location | Foreground opt-in defaults off. When enabled, opening fuel/service entry requests one fresh fix; a per-record action remains available. Save works after denial, timeout or disabled GPS; manual station/workshop text is supported. Never start background tracking or treat a position as verified identity. |
| Document expense | DocumentFlow.kt, DocumentWallet.kt, VaultDocument / renewal ledger contract | Optional owner expense. Inspection excludes private costs. Renewal payment events must not be double-counted from a mutable document field. |
| Smart entry S33 | New local parsing use case / UI draft state | Bounded grammar → reviewable candidates. Preserve unmatched text, no silent replacement or auto-save. Native OCR/speech are separate capabilities. |
| Service export | PassportExportUi.kt, ServicePassport.kt | Include selected itemized records/costs; preserve honest owner-entered limits. Exclude precise GPS and document IDs by default. |
| Restore/compatibility | VaultSession.kt, VaultDatabase.kt, LocalSync.kt | Backward-compatible defaults plus actual read/write persistence. Preserve asset references and pending sync; reject destructive recovery shortcuts. |

## Proposed domain contracts

These are design requirements, not declarations of existing API names:

- Service visit: immutable ID, date, odometer, ordered line items, separate labour, miscellaneous expense, discount, total, optional receipt, optional location and note.
- Line item: stable catalogue/custom identifier, performed action, quantity/unit, price in poisha, line total, product metadata and optional schedule association.
- Oil metadata: category, SAE grade, JASO/API labels as entered, product name, bottle/loose purchase basis, purchased quantity, bottle volume and optionally amount actually added.
- Location: optional latitude/longitude, accuracy metres, capture timestamp, source, user-confirmed place name/area. Nullable independently of a successful record save.
- Preferences: language, appearance, auto-location opt-in. Final font is a product design decision, not necessarily a shipped user setting.
- Schedule baseline: known/unknown per component, with the action that established it. Inspection/cleaning/adjustment/replacement are not interchangeable.

Room currently stores each ledger record as a serialized RideLog payload, while VaultDao reconstructs settings from `vault_meta`. The auto-location preference is included in `vault_meta` with an off-by-default column and the v2→v3 migration. Future settings still need explicit Room read/write and migration coverage; adding fields only to VaultState is not enough.

## Delivery phases

1. **Implemented, native rendering pending:** Anek Bangla is bundled with its OFL license; common typography and semantic brand palette apply at the app entry. Verify Bengali conjuncts, weights, large text and contrast on physical Android/iOS devices.
2. **Implemented, build verified:** setup and garage retain the native brand logo/model/photo catalogue, all 78 model records, nine logos and manual model fallback. Verify initial setup and switching bikes with persisted data on devices.
3. **Implemented:** typed multi-item service, prices, oil metadata, receipts, service editing and v2 passport export. v2 preserves item amounts, unknown-vs-free prices and oil metadata; v1 passports remain verifiable. Receipts, precise GPS, fuel, document fees and document originals stay out of the shared passport.
4. **Implemented, native runtime pending:** persisted opt-in for one-shot GPS, Android/iOS foreground permission adapters, fuel/service location fields and manual area fallback. Common tests cover JSON round-trip and validation. Verify actual permission, denial, disabled-GPS and timeout behavior on devices before treating this as runtime-proven.
5. **Implemented, source tests pass:** local text-to-draft suggestions with unmatched text preserved, optional service receipt images, stable-ID edits for fuel and itemized service, separate document-renewal fee events, and itemized v2 export. Current reminder baselines reject unsafe edits; earlier visits cannot move a newer baseline.
6. **Build verified, native runtime pending:** common tests, Android debug compilation and iOS arm64 compilation pass. Still exercise real permissions/GPS, font rendering, TalkBack/VoiceOver, text scaling and Room migration on devices; browser render and compilation are not runtime proof.

For each phase, preview exact file scope and data-contract impact before edits. Keep the design reference and screen IDs synchronized with the accepted implementation.
