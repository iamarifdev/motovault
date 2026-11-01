# P04A-05 persisted model ownership

Status: complete on the uncommitted P04A working tree.

## Ownership move

- `VaultState` and `VaultSettings` now live in
  `com.motovault.app.domain.vault`.
- `Ride`, `RideLog`, `VaultDocument`, `DocumentPage`, `GeoCapture`,
  `ServiceLineItem`, `ServiceCatalogueItem`, the preference enums,
  `ReminderPolicy` and the time/id helpers live in
  `com.motovault.app.domain.model`.
- The UI keeps compile-compatible aliases in `ui/VaultModelAliases.kt` and
  presentation-only extensions in `ui/VaultPresentation.kt`.
- `VaultState.create/seedData/restore` are UI composition extensions because
  they depend on the product catalogue and platform load/save functions.
- **Storage has zero imports from `com.motovault.app.ui`** (verified by
  `rg 'com.motovault.app.ui' composeApp/src/commonMain/kotlin/com/motovault/app/storage`).
- The domain/model ↔ domain/usecase package cycle was removed by moving
  `VaultState` to `domain.vault` and presentation-only expiry status to UI.

## Compatibility

- `VaultState.encode/decode` and the serialized field names are unchanged.
- The Room schema remains version 6; its schedule/vehicle/ledger/document
  payloads are unchanged.
- Frozen pre-P04 JSON, current/older V1 backup payloads, all-field adapter
  mappings and archive round trips pass.
- `rtk run ./gradlew :composeApp:testDebugUnitTest
  :composeApp:compileKotlinIosSimulatorArm64 --offline` passes with 274 tests.
- `architecture_dependencies.py` and `architecture_package_cycles.py` pass;
  the package graph is now 10 packages / 14 edges with no cycle.

## Limits

- The service catalogue moved with the model to preserve persisted service
  titles and matching behavior; its Bangla/English labels are data, but their
  long-term resource ownership remains part of the localization cleanup.
- Legacy backup display names use a pure brand-level fallback when an old ride
  has no stored model name; current rides keep their stored/catalogue name.
- No physical device was used.
