# Production app UI

The app uses `com.motovault.app.ui` as its production UI package. `App.kt` calls `ui.MotoVault()` directly. The former staging package was removed on 2026-10-14.

| Source set | Directory | Purpose |
| --- | --- | --- |
| commonMain | `composeApp/src/commonMain/kotlin/com/motovault/app/ui/` | Shared app screens, appearance, catalogue, records and platform contracts |
| androidMain | `composeApp/src/androidMain/kotlin/com/motovault/app/ui/` | Android implementations for capture, location, appearance, storage, reminders and passport export |
| iosMain | `composeApp/src/iosMain/kotlin/com/motovault/app/ui/` | iOS implementations of the shared platform contracts |
| commonTest | `composeApp/src/commonTest/kotlin/com/motovault/app/ui/` | Shared behavior and contrast tests; these are not app screens |
| androidUnitTest | `composeApp/src/androidUnitTest/kotlin/com/motovault/app/ui/` | Android document-recovery tests |
| iosTest | `composeApp/src/iosTest/kotlin/com/motovault/app/ui/` | iOS document-import and renewal-storage tests |

Persistence coordination remains under `com.motovault.app.storage`. Android and iOS host entrypoints remain under `com.motovault.app`. All platform implementations now have directory paths that match their package declarations.

The approved design reference remains in [app UI v2](../design/app-ui-v2/CONTINUE-HERE.md), with its [native implementation map](../design/app-ui-v2/APPLY-MAP.md). Existing source links and the catalogue generator were updated to the production paths.

This migration preserved current file contents except package, import and source-path references. It did not change stored JSON fields, the Room schema, app identifiers, assets or screen behavior.

Verification: Android Kotlin compilation and 113 Android/shared tests passed with zero failures or errors. The iOS simulator framework linked successfully, and shared/iOS test sources compiled successfully. No old staging-package references or directories remain in the source sets. This verification does not claim a new device UI audit.
