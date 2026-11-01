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

These exceptions describe existing code, not permission to add more coupling. They are owned by the P03A implementer, with the named task serving as the removal or verification target.

| Existing condition | Target task | Reason and limit |
| --- | --- | --- |
| `ui/VaultState.kt` owns persisted models and business operations; storage imports UI types | P03A-02 | Introduce explicit boundaries and retain serialized compatibility; do not move schema blindly. |
| Backup restore uses a legacy archive/model staging adapter | P03A-04 | The writer now coordinates restore; complete archive/asset recovery, collision handling and native bridge verification before claiming the backup migration complete. |
| Backup/restore use cases import UI and concrete database types | P03A-04 | Separate workflow and adapters while preserving old archives and recoverable replacement. |
| Legacy root/screens/sheets combine several feature responsibilities and contain inline text | P03A-03 and subsequent feature phases | Migrate the fuel slice first; do not require unrelated screens to be rewritten as part of it. |
| In-app language handling and resource selection need native compatibility verification | P03A-06 | Preserve current preference, default, digits and runtime switching; no claim that locale handling is already solved. |
| Architecture, literal and localization checks are lexical ratchets, not Kotlin semantic analysis | P03A-05 | Keep reviewing boundary imports, indirect text and numeric policy manually; the executable checks cover only their documented scopes. |
| Sky sources are configured from a sibling checkout | P03A-02 review and P03A-05 delivery | Record the input provenance and reproducibility limitation; do not claim a self-contained build. |

P03A-01 establishes the workflow only. P03A-02, -03, -04, -05 and -06 require their own implementation, evidence and status updates. Their requirements cannot be satisfied by this document.
