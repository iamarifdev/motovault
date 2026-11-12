# P07 reviewed native capture plan

Owner: Codex. Base: `ee36e75`, branch `main`, initially clean working tree.
Scope: P07-01 through P07-05, preserving every physical-device and pilot gate.

## Contract and boundaries

Manual, pasted, explicitly shared, photographed and spoken input become the same
transient capture draft. The draft carries a source code, explicit confidence,
raw input for review, money/odometer candidates, service suggestions and typed
issues. Acquisition never commits owner records. The existing vault writer owns
Save and durable Undo after review in the existing entry sheets.

One shared numeric parser accepts Latin, Bengali and Arabic-Indic digits and
valid international/Indian grouping. It rejects malformed grouping, exponent,
non-finite and partially matched amounts. Currency labels, paid/total context,
balance/fee exclusions and odometer units distinguish amounts from mileage.
Multiple contradictory totals require a choice. Odometers below the confirmed
reading or above the existing supported range are rejected. Service aliases are
input data, not display labels, and resolve to stable catalogue IDs.

Explicit shared text is bounded and fingerprinted through an injected SHA-256
adapter. Only accepted fingerprints are persisted, never an automatic SMS inbox
or clipboard read. Cancelled/failed drafts do not consume the fingerprint.
Recent accepted fingerprints participate in the same commit/Undo and optional
backup preferences as other settings. Existing payloads default to an empty
list; no Room schema change or competing writer is introduced.

Camera OCR will retain an in-memory preview/candidates for confirmation and
remove temporary files on cancel/completion. Blur/glare/no-result/denial leaves
manual entry usable. Voice starts only after the owner chooses processing and
uses the native permission flow. Local-only requests never silently fall back
to a network provider. Stop/cancel/backgrounding ends capture; raw audio is not
retained by the app. Provider and locale availability are reported honestly.

Android share intents and an iOS Share extension will accept only explicitly
shared supported text. Cold/warm delivery and locked-vault review use a narrow
native inbox boundary. No share payload may bypass review or vault access.

## Files and responsibilities

- `domain/input/NormalizedNumbers.kt`: canonical numeric input.
- `domain/capture/`: typed drafts, candidates, parsing, service aliases, limits
  and accepted fingerprint policy.
- `application/capture/`: draft factory, injected fingerprint contract and
  transient inbox/processing state.
- `storage/CaptureFingerprintAdapter.kt`: production digest adapter.
- Existing `ui/Bn.kt`, `ui/SmartParse.kt`, `ui/SmartServiceDraft.kt`: compatible
  wrappers using the shared rules, with no second parser.
- Existing vault/settings/backup preferences and adapters: optional accepted
  fingerprints, current-payload compatibility and Undo.
- Shared `ui/capture/` and existing odometer/paste/fuel/service entry points:
  source/confidence review and unchanged fixed Save/keypad/date patterns.
- Android/iOS narrow camera, speech and shared-text adapters, host entry points,
  manifests/entitlements and native resources; no native adapter writes the vault.
- `ui/capture/OdometerCaptureState.kt`: root-owned ephemeral camera acquisition.
  A private sheet may disappear on native-camera pause; pending results stay in
  RAM until owner review returns. The lock policy and final Save stay authoritative.
- Extend the v3 odometer/paste/SmartInput states before native presentation.
- `commonTest` capture/input/application fixtures and isolated native probes:
  parser ambiguity, malformed input, duplicate delivery, failure/cancellation,
  compatibility and actual OCR/provider integration.

New user copy uses shared English/Bangla string resources; OS prompts and
extension labels use native resources. Review the two languages together using
the product Bangla style. Language, input digits and display digits remain
independent. New production files remain below 400 lines and functions below 80;
no responsibility exception or storage-to-UI import is planned.

## Validation and external gates

Validate P07-01 once to close its dependency gate. Subsequent verification will
target new capture paths and reuse unchanged passing coverage, with one final
native/build batch where possible. Re-run an affected check only after a code
change or failure. Run portable gates and roadmap validation at handoff.

On 12 November, ADB reported no connected device and Xcode listed no physical
iPhone. The owner explicitly chose to keep physical camera/share/voice checks
and representative-owner pilot measurements in P07 and will arrange them.
Keep these requirements and their evidence kinds intact. Simulator, synthetic
OCR and compilation evidence cannot replace them. Tasks with incomplete gates
remain in review; no pilot participant or accuracy/success rate is invented.
