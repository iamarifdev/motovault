# P04A-01 dead legacy path removal

Status: complete on the uncommitted P04A working tree.

## Removed

- `composeApp/src/commonMain/kotlin/com/motovault/app/ui/Onboarding.kt` (227
  lines): legacy `Setup`, `ModelPicker` and `ModelPhoto`; no production call
  site remained. The active `OnboardingScreen` is in `ui/onboarding/`.
- `FuelDialog`, `ComponentsList`, `ServiceDialog`, `DocumentFeeDialog` and the
  duplicate `History` dialog from `ui/OwnershipForms.kt`. Only `ScheduleDialog`
  and `Attachment` remain; `Attachment` is still used by the document wallet.
- `OwnershipForms.kt` fell from 521 to 133 lines.

## Verification

- Definition/call-site searches for `Setup`, `ModelPicker`, `FuelDialog`,
  `ServiceDialog`, `ComponentsList` and `DocumentFeeDialog` return no production
  references after removal.
- `rtk run ./gradlew :composeApp:testDebugUnitTest
  :composeApp:compileKotlinIosSimulatorArm64 --offline` passes with 274 tests.
- No persisted format, active route or visual behavior changed.
