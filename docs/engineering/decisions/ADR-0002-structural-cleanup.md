# ADR-0002: Track structural cleanup as P04A before remaining feature phases

Status: accepted 2026-11-01.

## Context

P03A completed the initial domain/application boundary, the fuel pilot, backup
adaptation and executable gates. A post-P04 audit found that the architecture
is not yet fully represented in the source layout:

- `Screens.kt` is 1,670 lines with five screens; `Sheets.kt` is 1,501 lines with
  seven flows; `MotoVault.kt` is 1,044 lines and its root composable is roughly
  580 lines; `Components.kt` is 967 lines; `BackupSheet.kt` is 821 lines.
- `OwnershipForms.kt` and `Onboarding.kt` still contain large legacy paths with
  no production call sites.
- 30 storage import sites point at `com.motovault.app.ui`, and persisted models
  still live in the UI package.
- The dependency gate only scans shared `domain` and `application`, so a green
  result does not mean the target boundaries are complete.

Leaving this debt as an informal recommendation would let P05-P16 add more code
on top of it, and would make the cleanup depend on individual reviewer memory.

## Decision

1. Add a tracked foundation phase **P04A: Shared UI decomposition and ownership
   cleanup** after P04.
2. Split it into seven bounded tasks in dependency order:
   - P04A-01 remove proved-unused legacy UI paths;
   - P04A-02 split screens and sheets by feature;
   - P04A-03 extract root composition and shared primitives;
   - P04A-04 split component families and backup stages;
   - P04A-05 move persisted models out of the UI package and remove storage-to-UI
     imports;
   - P04A-06 add the file/function responsibility ratchet and split the native
     probe;
   - P04A-07 verify Android/iOS behavior and publish the updated ownership/gate
     baseline.
3. Make P04A-07 a prerequisite for P05-01, P06-01, P07-01 and P08-01, and include
   P04A in both release milestones. Later phases therefore inherit the gate
   through normal task dependencies.
4. Keep P04A in the `foundation` group so it does not inflate product-delivery
   points, while still appearing in the phase board and release milestones.

## Consequences

- P05, P06, P07 and P08 show `Waiting for prerequisites` until P04A-07 is done.
- The cleanup is behavior-preserving: no screen redesign and no Room/JSON/archive
  wire change. P04A-05 requires frozen compatibility fixtures if persisted model
  ownership moves.
- Splitting is driven by a coherent responsibility, not a line-count target.
  Existing over-400 files need an owner and removal task; new files above 600
  need a recorded exception. P04A-06 makes this executable.
- The tracker and architecture documents must be updated in the same change as
  each task, as required by `AGENTS.md`.

## Alternatives considered

- **Fold the cleanup into P16 or the release gate only.** Rejected: it would
  happen after the remaining feature phases and allow the same debt to grow.
- **Treat the green P03A gates as sufficient.** Rejected: the checks do not cover
  storage-to-UI imports, file responsibility or dead legacy paths.
- **Perform an untracked one-off refactor.** Rejected: it would not be visible to
  later phases or future contributors and would bypass the evidence workflow.
