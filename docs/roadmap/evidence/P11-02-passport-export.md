# P11-02 Evidence: Passport Export & Verification With Selective Redaction

- **Task**: `P11-02`
- **Owner**: Antigravity

## Implementation
- Extended [ServicePassport.kt](composeApp/src/commonMain/kotlin/com/motovault/app/ui/ServicePassport.kt):
  - Added `PassportRedactionOptions` (`includeRegistration`, `includeChassisAndEngine`, `includeCosts`, `includeWorkshops`, `includeNotes`) and `redactedFieldList()`.
  - Added `@EncodeDefault(EncodeDefault.Mode.NEVER) val redactedFields: List<String> = emptyList()` and `@EncodeDefault(EncodeDefault.Mode.NEVER) val unknownBaselineCount: Int? = null` to `ServicePassport` while preserving `motovault-service-passport-v2` and legacy `motovault-service-passport-v1` canonical digests.
  - Scoped `passportDefaults(state, bikeId)` and `ExportServicePassportUseCase.execute(..., options, bikeId)` to a single bike, filtered service records via `isPaidService`, stripped redacted fields prior to canonical encoding and SHA-256 hashing, and emitted `redactedFields` / `unknownBaselineCount` notices in `passportParagraphs`.

## Verification
- Extended [ServicePassportTest.kt](composeApp/src/commonTest/kotlin/com/motovault/app/ui/ServicePassportTest.kt) with `selectiveRedactionStripsSecretsFromJsonAndPdfAndCoversRedactedList` and `quotationsExpensesAndOtherBikesAreExcludedFromSelectedBikeExport` alongside all golden SHA-256 and v1/v2 tests.
- `./gradlew :composeApp:iosSimulatorArm64Test`: `BUILD SUCCESSFUL` (exit 0).
- `python3 tools/responsibility_gate.py`: `Responsibility baseline matches (2 files, 7 functions).` (exit 0).
