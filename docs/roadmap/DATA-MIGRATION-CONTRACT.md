# P00 shared data, migration and recovery contract

Decision date: 24 October 2026 (Asia/Dhaka). Baseline: `50eda3a`, Room schema 3.
Status: implementation contract for P01 onward. This document does not implement
the new models, encryption, backup, migrations or cloud transport.

## 1. Authoritative current storage

`storage/VaultDatabase.kt` stores JSON payload rows in `vehicle`, `schedule`,
`ledger` and `document`, plus `asset_reference`, `vault_meta` and `sync_outbox`.
The single vehicle row is `primary`. Room schema 1 -> 2 adds the outbox;
2 -> 3 adds `autoLocation`. Exported schemas are under
`composeApp/schemas/com.motovault.app.storage.VaultDatabase/`.

Android: Room `motovault.db`, legacy `files/motovault.json` or the old `motovault`
preferences string. iOS: Application Support `motovault.db`, legacy
`motovault.json` or the old defaults value. The loader migrates legacy data only
when no Room snapshot exists. Existing database state takes precedence.

Current contracts include `Ride`, `RideLog`, `VaultDocument`, `DocumentPage`,
`ComponentRule` and `ServiceLineItem`. `VaultState.decode` accepts old missing
fields/unknown JSON fields and recomputes fuel economy from logs. Decoding a
legacy JSON payload is not proof of a Room migration or asset recovery.

### Observed persistence gap

Room reconstructs only `appearance` and `autoLocation` among the exposed global
UI settings. `digits`, `haptics`, `reduceMotion`, `textScale`, `brandOverride`,
`serviceReminders`, `docReminders`, remembered prices, workshop/pump suggestions
are not currently round-tripped by the inspected Room mapper. A flag can change
within a live session and still revert after relaunch. JSON DTO compatibility
does not fix this. P01/P02/P06 must include these fields in a durable settings
contract and migration. A default `true` is never evidence of notification consent.

## 2. Identity and ownership

| Entity | Identity and scope | Migration rule |
| --- | --- | --- |
| Local vault | Stable opaque `vaultId`; account-independent | Generate once, commit with migration and reuse on retries |
| Owner | Local `ownerId`; optional later account binding | Never derive from NID, licence, phone number or device name |
| Bike | Stable opaque `bikeId` | Map `primary` to exactly one generated ID once; persist the mapping transactionally |
| Log/action/rule | Existing stable record ID plus bike scope | Preserve existing IDs, dates, order and references; never regenerate on read |
| Owner document | Driving licence and owner identity material | Associate with the local owner; unknown ownership needs explicit review |
| Bike document | Registration, tax and other applicable bike documents | Associate with migrated bike; document applicability is sourced, not inferred from category alone |
| Asset | Opaque immutable asset ID plus explicit owner references | Preserve original bytes and accepted/preview distinction; shared assets require explicit references |
| Workshop/contact | Owner-managed stable ID, referenced by service/repair | Import existing free text without guessing that similar names mean the same business |
| Renewal/repair/warranty/work shift | Stable event ID with explicit linked owner/bike/record IDs | Append history; later correction is traceable, not replacement of the only prior record |

Do not use catalogue model ID as bike ID: two owned bikes can be the same model.
Keep market/year/model-code applicability separate from presentation names and
brand logos. P09 performs the complete bike-scoping migration; P01-P07 models
must already have a documented future mapping to avoid another ambiguous primary.

Preserve owner-written names. Localize known catalogue/document-type labels in
the UI; do not rewrite free-form workshop, bike or service names as a migration.
The paired fixture adapters retain the current native English default rule/type
names and prototype Bangla display labels for the same semantic IDs. This is a
documented mapping, not byte-identical payload text across different models.

## 3. Money and quantities

- New monetary contracts use signed 64-bit integer poisha plus `currency=BDT`.
  Current expense entry remains nonnegative; credits/refunds require an explicit
  typed adjustment design rather than accepting negative expenses accidentally.
- Parse human decimal input after digit normalization, without converting through
  binary floating point. Persist exact unit prices, quantities and totals using
  declared precision. Quantity/litres are not money and retain their own units.
- Convert legacy taka from its serialized decimal token where available. If only
  an already-decoded finite Double exists, use its shortest round-trip decimal
  representation. Reject NaN/infinity and overflow. Preserve the original value
  in the migration review evidence, not in telemetry.
- At the money boundary, round once to the nearest poisha, decimal half-up.
  Examples: 120.00 taka -> 12000 poisha; 0.10 -> 10; 19.995 -> 2000;
  0.005 -> 1. Item quantities multiply exact unit values, then the defined total
  calculation is checked against the confirmed total. Record any reconciliation
  adjustment; do not silently change a paid service amount.
- Existing service poisha fields are authoritative when internally consistent;
  the legacy `cost` field must agree after conversion or be surfaced for review.
  A quotation, allowance and actual expense are different states. A linked
  renewal payment contributes to costs once.
- Fuel prices are confirmed values with an effective date/source. The fixture
  and prototype prices are synthetic; no current market-price claim is made.

## 4. Dates, time and certainty

- Service/fuel/expense/expiry dates are valid Gregorian ISO `YYYY-MM-DD` dates.
  Never interpret a stored date-only value as midnight UTC and shift its date.
- Capture/save/export instants use ISO 8601 UTC with an explicit offset or `Z`.
  Evidence displays Bangladesh local time; keep the original machine timestamp.
- Reminders store requested local time and timezone policy. Default behavior may
  follow the device, but the policy must be explicit and tested for date/timezone
  changes. UI text, app settings and native scheduling use the same policy.
- Store mileage value, unit, confirmation instant and source separately from
  estimated mileage. Never advance the durable odometer based on an estimate alone.
- Add baseline certainty (`known`, `unknown`, `owner_estimate`) and source/manual
  applicability when P04 implements it. Current empty schedules cannot encode this
  distinction; a fresh baseline is not an observed service visit.
- An interval percentage measures recorded interval consumption, not the physical
  condition of brakes, tyres, oil or another component.

## 5. Assets and sensitive data

Original imports are immutable; accepted crops and previews have different IDs.
Hash every non-regenerable original with SHA-256 in migration/backup manifests.
Hashes prove byte consistency, not ownership, authenticity or legal validity.
Keep a complete reference graph from documents/receipts to their assets.

Preserve current original import limits (32 MiB each; two pages for card documents,
eight for other current document types) unless a separately measured/reviewed
change updates them. Validate MIME/content, dimensions, page count and bounded
decode work. A suffix alone does not establish content type. Legacy Base64
photos/receipts must be bounded, decoded and included during a reviewed conversion,
or retained as legacy data; never silently omit them from a backup.

Asset IDs cannot be absolute paths, contain traversal, or name another vault's
files. Stage new files, validate/read them, commit references, then retire old
unreferenced files only after the recovery and Undo retention policy permits it.
Cleanup must never delete a file still referenced by current state, a live Undo
snapshot, a pending restore or a frozen backup/upload revision.

P02 defines the cryptographic/key lifecycle. App lock, local asset encryption,
database encryption and portable backup encryption are separate claims. Do not
claim all local data is encrypted while plaintext payload/outbox metadata remains.
Device keys are never exported as a portable recovery method.

## 6. Durable commands, Undo and side effects

Use a serialized command with an expected local revision. Validate ownership and
references against the latest durable state, write all related records/settings
in one transaction, increment the revision, then publish success. Failed writes
retain the previous visible/durable state and preserve files needed for recovery.
An outbox entry refers to that committed revision, not an optimistic UI snapshot.

Undo is an inverse durable command against the appropriate revision. It restores
the complete affected state: records, money, rules/baselines, odometer and asset
references. Its UI availability window is explicit. Surviving app restart is a
separate feature requiring a persisted inverse journal; do not imply it from an
in-memory toast. Once Undo succeeds, its result must survive restart.

The P00 Android capture observed a non-restoring odometer Undo. The inspected
callback closes over the revision from before the save, while `VaultSession`
rejects commits against an old revision. The baseline retains this mismatch;
P01's reliability work must give inverse commands the appropriate current
revision without bypassing concurrency protection.

Reschedule/cancel native reminders and refresh widgets only after durable commit
or durable Undo. Scheduling failure cannot undo a successful record save; expose
its separate retry/status. Delay destructive file cleanup until no live inverse
or recovery revision references the assets. Current document-delete/cleanup
paths need targeted verification before extending Undo to asset-bearing records.

## 7. Migration and rollback protocol

1. Inventory exact schema/DTO version, rows, IDs, asset references and hashes using
   synthetic compatibility cases before release. Refuse unsupported future data.
2. Preserve an untouched recovery snapshot and original files. Preflight free
   space, source readability and version applicability. Never use demo seeding as
   a corruption fallback.
3. Build new fields/mappings in a staged transaction with explicit compatible
   defaults. Reuse IDs/migration mappings across retries. A missing record or asset
   cannot be discarded to make validation pass.
4. Validate row counts, ordering, referential integrity, integer totals, source
   preservation and asset hashes before committing the new schema/revision.
5. On error/cancel/interruption, retain or restore the previous coherent snapshot.
   Recover staged files using a durable migration journal; do not reset counters
   or reuse sync operation IDs.
6. Reopen/restart and verify the committed result before cleanup. Reconcile OS
   effects from the new revision after commit. Run the migration a second time to
   prove idempotent no-op behavior.
7. Publish a supported-version matrix. An older binary must refuse an unsupported
   newer database/archive without erasing it. App downgrade is not a substitute
   for a reviewed data rollback/export path.

The next Room version is assigned at implementation after inspecting the latest
schema; do not independently claim the same version in parallel branches. Retain
explicit migrations from every supported installed schema. A schema export or
DTO decode test alone is insufficient evidence of an actual Room upgrade.

## 8. Portable backup version policy

The first portable encrypted format is a separate `motovault-backup/v1` contract;
Room schema numbers and existing passport v1/v2 are independent. P03 freezes the
reviewed cryptographic envelope before shipping. This contract requires:

- Authenticated format/version, vault identity, frozen local revision, creation
  instant, data manifest, original asset hashes/lengths, declared optional features
  and key-version/recovery metadata. Owner secrets/keys never appear in plaintext.
- Include every supported record, owner/bike mapping, preference and original.
  A preview may be omitted only if restore regenerates it. OS permissions are
  re-requested; a restored preference cannot grant permission or revive old jobs.
- Unknown future major version fails closed. A supported older archive is upgraded
  in staging through explicit version adapters; never guess or silently drop fields.
- Initial safety ceilings to validate in P03: 16 MiB decoded JSON manifest/data,
  10,000 archive entries, 1 GiB total decoded payload, 32 MiB per original, and a
  100:1 expansion ratio. These are proposed limits, not current implementation or
  measured device capability. Stream and preflight available storage; a lower
  device budget fails with a useful message before replacing anything.
- Reject duplicate entry names/IDs, unsafe paths, symlinks, mismatched lengths/
  hashes, missing originals, decompression bombs and unsupported encryption.
  Wrong recovery secret or altered ciphertext leaves the old vault untouched.
- Restore verifies all referenced assets before one transactional state switch.
  Review counts and replacement/conflicts first; cancellation is not success.
  Export/restore temporary plaintext follows P02 cleanup and lock rules.
- Cross-platform Android-to-iOS and iOS-to-Android recovery is a physical-device
  release gate. Every later data feature includes an archive-version decision
  and old/new restore fixtures in the same change.

## 9. Phase model coverage and required decisions

| Phase | New model/state | Compatibility and ownership decision |
| --- | --- | --- |
| P01 | Reminder policy, native IDs/consumption, snooze | Migrate platform opt-in plus global/per-document settings; unknown consent stays disabled; preserve consumed identities |
| P02 | Lock/key versions, encrypted assets, timeout | Keep local versus portable keys separate; resumable asset migration and explicit lost-key behavior |
| P03 | Portable manifest, restore journal and backup receipt | Freeze a coherent revision; include originals, legacy images and settings; atomic restore |
| P04 | Action type, manual source and baseline certainty | Preserve old custom rules; do not infer factory applicability or known history |
| P05 | Document revisions, renewal events and linked payments | Owner licence versus bike paperwork; append prior expiry/originals; count one expense |
| P06 | Money/expense categories, budgets, coverage | Poisha conversion and reconciliation; preserve partial-fill semantics and incomplete periods |
| P07 | Reviewed capture draft/source/confidence | Confirm before write; bounded external payload; no default raw-audio retention |
| P08 | Release/schema/archive support matrix | Upgrade from baseline plus full restore; separate signed install and publication proof |
| P09 | Garage, owner and bike membership | Migrate primary once; isolate all queries, deep links, side effects and inverse commands |
| P10 | Workshops, repair cases, quotations, warranties | Stable owner contacts; bike/visit links; quotations are not paid expenses |
| P11 | Selective handover/passport | Separate export schema, redaction and provenance; digests do not authenticate a mechanic |
| P12 | Minimal widget snapshot | Non-sensitive chosen-bike projection; no document numbers/keys; OS-limited refresh |
| P13 | Region/source version, care state and chosen contacts | Offline first; no continuous GPS or automatic contact transmission |
| P14 | Work shifts, income, deductions and cost allocation | Opt-in bike-scoped ledger; no duplicate expense subtraction or earnings guarantee |
| P15 | Account binding, immutable encrypted objects/remote revision | No account reassignment on logout/switch; conflict pauses without overwriting; cloud backup is not multi-writer sync |
| P16 | Expanded supported-version/enablement matrix | Optional modules enable only after their own acceptance gates |

## 10. Fixture and evidence contract

[P00 paired fixtures](fixtures/p00/manifest.json) carry `fixtureVersion`, scenario
ID, reference date, `synthetic=true`, native DTO input and prototype input with
the same underlying records/amounts/IDs. Source and generated exports are hash-
checked. Empty and unknown-history scenarios currently have the same serialized
schedule shape; the fixture's semantic label records the distinction the app
cannot yet express. Runtime clocks are recorded rather than silently altered.

Compatibility cases under `fixtures/compatibility/` retain legacy/current JSON,
source schema exports, expected ownership/money mappings and original synthetic
bytes/hashes. Native tests exercise current DTO decoding and round trips. Host
fixture checks exercise deterministic source/mapping integrity. Neither replaces
the future real Room migration, encrypted restore or physical-device gates.

Evidence includes build/base plus uncommitted test-tool delta, platform/OS/device,
scenario and fixture hash, steps, expected/observed results, screenshots, separate
motion files, result/log references and limits. Keep native, prototype, shared
tests, physical-device, cloud and release proof separately labelled. Use the
[evidence template](evidence/TEMPLATE.md). Never commit real owner identifiers,
originals, receipts, location, credentials or secret recovery material.

## Review and next implementation gate

P00-05 review must cross-check the current source/exports and all phase rows above,
retain representative fixtures, and list mismatches as follow-up gates. The
review report is [P00-05 review](evidence/P00-05-review.md). P00 completion permits
P01/P02/P06 work according to the tracker; it does not implement any future model.
