# Synthetic compatibility cases

- `legacy-minimal.json`: old fields without new preferences, new fuel fields or
  an explicit log ID. First native decode assigns compatible defaults; the first
  persisted identity must then remain stable. Fresh decodes of the raw legacy
  input are not expected to generate the same random default ID.
- `current-all-fields.json`: current DTO settings, full/partial fuel history,
  itemized service total, a legacy Base64 receipt and explicit original/crop/
  preview references. These fields are intentionally richer than what the current
  Room settings mapper preserves.
- `assets/`: three generated one-pixel PNGs, each with its own bytes/hash. They
  are synthetic document-content stand-ins, not user documents.
- `room-schema-1.json` through `room-schema-3.json`: copies of the actual exported
  Room schemas for host SQL/shape comparison and future real migration tests.
- `expected-mappings.json`: source-reviewed future identity/ownership decisions,
  exact decimal money examples and service reconciliation expectation.
- `manifest.json`: source input hashes. `tools/baseline/compatibility.py` verifies
  them and checks that the generated Kotlin fixture strings match those inputs.

The native tests verify current DTO compatibility. The host SQLite check executes
the existing migration SQL and checks columns/payload preservation. These are
different from opening an old database through the native Room migration driver;
future schema upgrades must add that verification in the implementing phase.
