# P11 Selective Handover Pack, Provenance & Privacy Specification

- **Task**: `P11-01`
- **Status**: Reviewed

## 1. Export Schema & Backward Compatibility
- Canonical export envelope remains `PassportBundle(passport: ServicePassport, sha256: String)` with `passportJson = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = false }`.
- `ServicePassport.schema` remains `motovault-service-passport-v2`; legacy `motovault-service-passport-v1` bundles remain verifiable alongside `v2`.
- Optional P11 metadata fields on `ServicePassport`:
  - `redactedFields: List<String> = emptyList()` with `@EncodeDefault(EncodeDefault.Mode.NEVER)`
  - `unknownBaselineCount: Int? = null` with `@EncodeDefault(EncodeDefault.Mode.NEVER)`
- Because `@EncodeDefault(EncodeDefault.Mode.NEVER)` omits default empty/null metadata from JSON, unredacted exports with `redactedFields = emptyList()` and `unknownBaselineCount = null` remain byte-for-byte identical to existing `v2` golden SHA-256 vectors.

## 2. Selected Bike Scope & Paid Service Boundaries
- Exports target exactly one selected bike (`bikeId: String?`, defaulting to the active bike). Other bikes in `garageBikes`, their logs, fuel fill-ups, unrelated owner expenses (`kind == "expense"`), `document_fee` logs, non-paid workshop visits (`visitState != "paid"`), and document originals/PDFs are strictly excluded.
- When generating `passportDefaults(state, bikeId)`, only owner-confirmed `"Registration smart card"` metadata matching that scope is prefilled.

## 3. Selective Per-Field Redaction & Privacy Defaults
- UI review (`PassportExportUi.kt`) defaults sensitive identifiers **OFF** (`includeRegistration = false`, `includeChassisAndEngine = false`), while keeping maintenance facts, service costs, workshops, and notes selectable (`true` by default):
  - `registration`: blanks `PassportIdentity.registration` before canonical serialization & hashing
  - `chassis_engine`: blanks `PassportIdentity.chassis` and `PassportIdentity.engine`
  - `costs`: sets `costBdtBits = passportNumber(0.0)` and clears `unitPricePoisha`, `lineTotalPoisha`, `labourPoisha`, `miscellaneousPoisha`, `discountPoisha`, `totalPoisha`
  - `workshops`: blanks `PassportService.workshop`
  - `notes`: blanks `PassportService.notes`
- Redacted values are stripped **prior** to canonical encoding, so excluded secrets never appear in `ServicePassport` JSON, PDF paragraphs, or the SHA-256 preimage.
- The resulting `redactedFields` list records the redacted categories in deterministic order inside the signed/hashed payload.

## 4. Unknown-History Disclosure & Non-Certification
- When `unknownBaselineCount` is included (> 0 when redaction options are active), recipients see how many maintenance schedules on that bike lack a confirmed baseline so gaps are explicit rather than silent.
- Every passport carries `PASSPORT_DECLARATION`: owner-supplied records, verified only by canonical SHA-256 self-integrity against a supplied digest, never third-party workshop/regulator certification.

## 5. Official Ownership Transfer Checklist (BRTA)
- Official BRTA ownership transfer remains an offline legal checklist outside the app export (buyer & seller verification, original smart card / bluebook, tax token, fitness if applicable, and stamp/affidavit at BRTA counter). Exporting or cancelling share never mutates local bike ownership state.
