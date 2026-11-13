# P09 Garage Ownership & Multi-Bike Boundary Contract

Date: 13 November 2026
Owner: Ariful Islam
Scope: Phase `P09` (`P09-01` through `P09-05`)

## 1. Entity Ownership Classification

| Record / Domain Object | Scope | Stable Identifier & Scoping Key | Switching / Archive Behavior |
| --- | --- | --- | --- |
| `Ride` (model, brand, year, registration, odometer, confirmed date, market/emission) | **Bike-owned** | `Ride.id` (defaults to `"primary"` for upgraded/seeded single-bike vaults; new bikes receive `"bike_<recordId>"`) | Active bike sits at `VaultState.ride`; non-active & archived bikes live in `VaultState.garageBikes` (`GarageBikeRecord`). Duplicate model names are distinguished by `Ride.id` and `registration`. |
| `ComponentRule` (maintenance intervals & baselines) | **Bike-owned** | `ComponentRule.vehicleId == Ride.id` | Packed inside active `VaultState.rules` or `GarageBikeRecord.rules`. Archived bike schedules never trigger reminders or overdue urgency banners. |
| `RideLog` (fuel, service, document fee, expense) | **Bike-owned** | `RideLog.vehicleId == Ride.id` (defaults to `"primary"`) | Packed inside active `VaultState.logs` or `GarageBikeRecord.logs`. Cost totals, economy calculations, and history feeds only read the active bike. |
| `VaultDocument` (`registration`, `tax`, `insurance`, `fitness`, `other`) | **Bike-owned** | `VaultDocument.vehicleId == Ride.id` (legacy single-bike docs default `vehicleId = null` and migrate/resolve to active bike `"primary"`) | Swaps with the active bike on switch. Archived bike document assets remain protected in storage and backup. |
| `VaultDocument` (`licence` / Driving licence) | **Owner/Person-owned** | `VaultDocument.isOwnerScoped == true` (`kind == "licence"` and `vehicleId == null`) | Stays visible in `VaultState.documents` across every bike switch and while switching/archiving bikes. |
| Document binary assets & receipts | **Referenced by owning document** | `VaultDocument.assetIds()` aggregated across `state.allDocuments()` | Asset garbage collection (`VaultSession`, `DocumentFlow`, `PrivacySheet`, `VaultDatabase.asset_reference`, `RecoverySnapshotStore`, `LocalSync`) tracks `state.allDocumentAssetIds()` so non-active/archived bike attachments are never deleted as orphans. |
| `VaultSettings` (`language`, `digits`, `appearance`, `reduceMotion`, `textScale`, `haptics`, `serviceReminders`, `docReminders`, `reminderPolicy`, `privacy`, `lastPrices`, `lastItemPrices`, `workshops`, `places`, `ledger`, `captureReceipts`) | **App/Owner-owned** | Singleton `vault_meta` row | Shared across the owner's garage; `brandOverride` lets owner keep a theme override or follow active `ride.brandId`. |
| `CaptureDraft` & `CapturePrefill` | **Bike-scoped at creation** | `targetBikeId: String = "primary"` | Captures the active bike ID when intake begins. Delayed confirmation applies to `targetBikeId` even if the user switched bikes, or rejects cleanly if `targetBikeId` is archived. |
| `RenewalReminder` & `ReminderTarget` | **Bike-scoped or Owner-scoped** | Service & bike doc reminders carry `vehicleId`; `licence` carries `vehicleId = null` | Reminders from archived bikes are never scheduled and existing notifications for an archived bike resolve as stale rather than mutating another bike. |

## 2. Active-Bike, Switching & Archive Invariants

1. **Zero loss upgrade & v1 backup compatibility**: Existing single-bike SQLite Room (`version = 6`), JSON state, and `.mvbk` encrypted archives decode with `Ride.id = "primary"`, `Ride.archived = false`, `RideLog.vehicleId = "primary"`, `VaultDocument.vehicleId = null`, and `VaultState.garageBikes = emptyList()`.
2. **Partitioning documents without duplication**:
   - Owner-scoped (`isOwnerScoped`): `kind.equals("licence", ignoreCase = true)` (or legacy titles `"Driving licence"`, `"Driving license"`, `"ড্রাইভিং লাইসেন্স"`) with `vehicleId == null`.
   - All other documents in `VaultState.documents` are normalized with `vehicleId = currentRide.id` when packed into a `GarageBikeRecord`.
   - Duplicate `VaultDocument.id` is impossible because owner docs stay in one canonical list and bike docs belong to exactly one bike.
3. **Archive & restore lifecycle**:
   - An archived bike (`ride.archived == true`) cannot be set active via `switchToBike` until restored (`setBikeArchived(bikeId, archived = false)`).
   - At least one non-archived bike must always remain active (`activeBikes().size > 1` required to archive another bike).
   - Archiving the currently active bike atomically promotes the first remaining non-archived bike to `VaultState.ride` and preserves the archived bike's full `GarageBikeRecord` (`ride`, `rules`, `logs`, `documents`).
4. **Scoped mutation & Undo safety (`P09-04`)**:
   - Every delayed mutation (`FuelEntryRequest`, `FuelEditRequest`, `CapturePrefill`, `ReminderNavigation`) carries its `targetBikeId`.
   - Mutating an archived bike fails loudly (`IllegalArgumentException` / `IllegalStateException`) without partial state changes.
   - `VaultCoordinator` Undo remains revision-guarded (`expectedRevision`), preventing a stale Undo toast from corrupting state after a bike switch.
