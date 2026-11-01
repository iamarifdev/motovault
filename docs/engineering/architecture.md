# MotoVault architecture

The authoritative contributor rules are in [the architecture contract](../roadmap/ARCHITECTURE-CONTRACT.md). This document explains their target and transition; [the roadmap](../roadmap/roadmap.json) owns work status and phase-specific acceptance. An intended module is not evidence that it exists.

## Dependency direction

```text
shared Compose screens -> feature state owners -> application workflows -> domain contracts
                  root composition -> data/native implementations -> domain/application contracts
```

The app root wires collaborators. Domain owns rules and value types. Application owns user operations and cross-feature coordination. Data owns Room, migrations, serialization and repositories. Native adapters own platform APIs. Presentation owns localized rendering, transient UI state and navigation requests. Dependencies point inward, with constructor injection across boundaries.

The target feature ownership is garage/onboarding, maintenance, ledger/fuel, documents, backup and settings. Features do not depend on another feature's UI or access Room/native services directly. UI components belong to Sky when product independent, core UI when shared by MotoVault, and otherwise their feature.

Keep a single authoritative vault writer during migration. Save success, revision checks, durable Undo, asset ownership and reminder reconciliation must retain their current semantics. Adapters cannot independently replace durable snapshots.

## Recorded transition exceptions

These exceptions describe existing code, not permission to add more coupling. They are owned by the P04A implementer, with the named task serving as the removal or verification target. P04A-07 closes the phase only when the remaining entries have evidence or an explicit, tracked exception.

| Existing condition | Target task | Reason and limit |
| --- | --- | --- |
| Backup restore uses a legacy archive/model staging adapter | P03A-04 | The writer now coordinates restore; complete archive/asset recovery, collision handling and native bridge verification before claiming the backup migration complete. |
| Remaining large feature files exceed the 400-line review trigger | P04A-02/P04A-04 and the responsibility baseline | Screens, sheets, root primitives and component families are split; the remaining exceptions have owners and re-review triggers in the responsibility baseline. |
| In-app language handling and resource selection need native compatibility verification | P03A-06 | Preserve current preference, default, digits and runtime switching; no claim that locale handling is already solved. |
| Architecture, literal and localization checks are lexical ratchets, not Kotlin semantic analysis | P03A-05 and P04A-06 | Keep reviewing boundary imports, indirect text and numeric policy manually; P04A-06 adds the file/function responsibility ratchet and extends the boundary check to storage-to-UI edges. |
| Sky sources are configured from a sibling checkout | P03A-02 review and P03A-05 delivery | Record the input provenance and reproducibility limitation; do not claim a self-contained build. |

P03A-01 establishes the workflow only. P03A-02 through P03A-06 require their own implementation, evidence and status updates. P04A-01 through P04A-07 completed the residual UI and data-boundary cleanup; persisted models now live under `domain/vault` and `domain/model`, and storage has no UI imports.

## File and function responsibility policy

Review handwritten production files above 400 lines and functions above 80 lines. A new or expanded file above 600 lines needs a recorded exception with an owner, a coherent responsibility and a removal trigger. Generated data, fixtures and design tokens are reviewed separately. Do not split a file only to satisfy a number; extract a coherent owner and verify behavior. P04A-06 makes this a ratcheted check with an expiring baseline rather than an informal rule.
