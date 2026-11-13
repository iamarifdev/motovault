# P09-04: Bike-Scoped Capture, Reminders, Privacy & Undo

- Task: `P09-04` ("Scope capture, notifications, privacy and Undo to a bike")
- Status: Complete (`implementation`, `verification`)

## 1. Bike-Scoped Capture & Fuel Mutations

- Added `targetBikeId: String = "primary"` to [CaptureDraft.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/domain/capture/CaptureDraft.kt) and `targetBikeId: String? = null` to `CapturePrefill` in [CapturePrefill.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/capture/CapturePrefill.kt).
- Updated [CaptureReviewSheet.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/capture/CaptureReviewSheet.kt) to stamp `state.ride?.id` onto `CapturePrefill`, and updated `VaultState.acceptPrefill` + `applyPrefillScopedMutation` to route through `mutateBike(targetBikeId)` so switched or archived targets never leak across bikes.
- Added `targetBikeId: String? = null` to `FuelEntryRequest` and `FuelEditRequest` in [FuelEntryRequest.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/application/fuel/FuelEntryRequest.kt), stamped `targetBikeId` in [VaultSheetHost.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultSheetHost.kt), and wrapped both `FuelEntryAdapter.build` and `FuelEntryAdapter.edit` in `mutateBike(request.targetBikeId)` inside [FuelEntryAdapter.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/storage/FuelEntryAdapter.kt).
- Updated `"service"`, `"odometer"`, and `"expense"` sheet flows in [VaultSheetHost.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/VaultSheetHost.kt) to display and merge back into `targetBikeId` cleanly.

## 2. Reminder & Navigation Isolation

- Added `val vehicleId: String? = null` to `RenewalReminder` ([RenewalReminders.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/RenewalReminders.kt)) and `ReminderTarget` ([ReminderPlan.kt](../../../composeApp/src/commonMain/kotlin/com/motovault/app/ui/ReminderPlan.kt)).
- `reminderPlan` and `reminderTarget` now resolve document reminders from `state.activeReminderDocuments()` (including owner-scoped driving licences while excluding archived bikes) and service reminders across non-archived bikes (`vehicleId = bike.id`), returning `null` if the target bike has since been archived.
- Updated `MotoVault.kt` (`LaunchedEffect(reminderRequest, loading)`) to switch to `target.vehicleId` when opening a notification targeting another active bike.

## 3. Verification

- Added [ScopedMutationsTest.kt](../../../composeApp/src/commonTest/kotlin/com/motovault/app/domain/garage/ScopedMutationsTest.kt) (3 tests covering cross-bike fuel create/edit after switch, archived-target rejection for fuel and `CapturePrefill`, and reminder isolation across archive while preserving owner licence reminders).
- `./gradlew :composeApp:iosSimulatorArm64Test`: 57 test files, 328 tests, 0 failed, 0 skipped (exit code 0).
- `python3 tools/responsibility_gate.py`: passed (`Responsibility baseline matches (2 files, 7 functions)`).
- `python3 tools/check_agent_docs.py`: passed (`agent docs consistent`).
