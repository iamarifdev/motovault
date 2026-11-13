# P11-01 Evidence: Selective Handover Pack & Privacy Spec Review

- **Task**: `P11-01`
- **Owner**: Antigravity

## Plan & Review
- Authored [P11-HANDOVER-PACK-SPEC.md](docs/roadmap/P11-HANDOVER-PACK-SPEC.md) covering:
  - Canonical `motovault-service-passport-v2` JSON & backward compatibility with `v1` and existing `v2` golden SHA-256 digests (`@EncodeDefault(EncodeDefault.Mode.NEVER)` on optional `redactedFields` and `unknownBaselineCount`).
  - Single-bike scoping and exclusion of fuel, owner personal expenses, `document_fee`, non-paid visits (`quote` / `approved` / `cancelled`), and original documents/PDFs.
  - Default UI privacy posture: registration plate and chassis/engine numbers redacted (`false`) by default; redactions applied before canonical encoding and hashing so secrets never leak into exported artifacts or digest preimages.
  - Explicit unknown-baseline count disclosure and non-certification statement (`PASSPORT_DECLARATION`).
  - Official BRTA ownership transfer kept as a separate offline checklist that never mutates device ownership records on share or cancel.
