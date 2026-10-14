# Approved app UI

The production app uses the approved [app UI v2 reference](../design/app-ui-v2/CONTINUE-HERE.md). It retains the native brand logos and full model/photo catalogue, with Bangla-first copy, Anek Bangla typography and dynamic brand palettes.

The app entrypoint is [App.kt](../composeApp/src/commonMain/kotlin/com/motovault/app/App.kt). It calls `com.motovault.app.ui.MotoVault()` directly. See [UI-STRUCTURE.md](UI-STRUCTURE.md) for shared screens, platform implementations and test locations.

Fuel/service forms, typed service/oil entries, costs, optional GPS, document capture/review, expiry/reminders, history corrections and service export are mapped in [APPLY-MAP.md](../design/app-ui-v2/APPLY-MAP.md). Local records use Room schema v3. The native iOS host is `iosApp/MotoVault.xcodeproj`.

Fuel economy is calculated by the domain `FuelEfficiencyUseCase` using complete full-to-full cycles, including intermediate partial fills. Document expiry classification uses the domain `DocumentExpiryUseCase` and the shared 60-day warning boundary. Existing stored `RideLog` and `VaultDocument` payload fields remain unchanged.

See the [QA record](../design/app-ui-v2/QA.md) and [native verification report](CONTINUATION_PROGRESS.md) for proof boundaries. The [earlier native prototype document](archive/NATIVE-PROTOTYPE.md) is retained as a historical snapshot.
