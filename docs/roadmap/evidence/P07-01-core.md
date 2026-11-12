# P07-01 reviewed draft and parsing implementation

Base: `ee36e75`, `main`, initially clean tree. Exact changed-source hashes are in
[P07-01-source.json](P07-01-source.json). The phase plan is
[P07-CAPTURE-PLAN.md](../P07-CAPTURE-PLAN.md).

`domain/input/NormalizedNumbers` now owns supported digits, valid international
and Indian grouping, canonical numeric text and finite parsing. `ui/Bn.kt`
delegates to it, so existing manual fields and new capture use the same rules.
Malformed grouping, exponents and partial numeric matches are not silently
accepted. Canonical money continues through the P06 integer-poisha contract.

`domain/capture` defines one transient draft with manual/paste/photo/share/voice
source, explicit confidence, bounded raw text, money and odometer candidates,
stable-ID service suggestions and typed issues. Paid/total context distinguishes
money from mileage, phone/reference numbers, balance and fees. Conflicting totals
or unlabeled photo readings require an explicit choice. Regression and out-of-
range odometers are rejected. Every acquisition source is covered by fixtures;
no native camera or speech success is claimed by these domain tests.

Both old service parser entry points delegate to one `CaptureServiceParser`.
Existing call-site payloads remain compatible. No parallel alias/price parser,
display-language branch or storage-to-UI dependency was added.

`CaptureDraftFactory` receives a fingerprint contract. The storage adapter uses
the production native SHA-256 implementation over canonical input. Explicit
duplicate shares are detected from accepted fingerprints. Only hashes are added
as optional vault/settings/backup preferences; raw messages are not persisted.
`acceptCapture` returns a new aggregate for the existing writer. Cancel/failure
does not mutate the original; writer Undo can restore the previous aggregate.
The receipt list is bounded and old JSON defaults to empty, without a Room schema
change. A real encrypted archive preserves accepted fingerprints.

Native acquisition, reviewed UI integration and actual host delivery belong to
P07-02/03/04. Physical-device and representative-owner pilot checks remain in P07
at the owner's request. This report is not evidence of those later gates.
