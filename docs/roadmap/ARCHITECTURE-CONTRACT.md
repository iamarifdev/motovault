# MotoVault architecture and AI delivery contract

This contract applies to maintenance of completed P03 backup/restore work and to
all subsequent roadmap phases. It is the shared instruction source for people,
Codex, Claude, Gemini and other assistants. The detailed rationale and staged
target are in [the architecture proposal](../ARCHITECTURE-PROPOSAL.md). Task
scope and dependencies remain in [the tracker](roadmap.json); use the tracker
tools and the phase-specific architecture application in its generated plan.
Practical contributor guidance is in [engineering architecture](../engineering/architecture.md),
[conventions](../engineering/conventions.md), [verification](../engineering/verification.md)
and the [shared AI workflow](../engineering/ai-workflow.md). This contract governs
if those documents appear to conflict.

P00–P03 completion records describe their original scope and evidence. They are
not proof that the target architecture is already in place. P03A tracks the
foundation and the separate P03 backup/restore migration. P03A is planned, not an
assertion that modules, localization infrastructure or checks already exist.

## Target boundaries

- Organize code by feature and responsibility. Presentation renders state and
  sends actions; a lifecycle-scoped state owner coordinates each feature;
  application workflows orchestrate operations; domain rules own invariants;
  data and native adapters implement explicit contracts.
- Dependencies point toward domain/application contracts. Domain and application
  do not depend on Compose UI, Room, native SDKs, Koin or concrete adapters.
  Feature presentation does not directly use Room or platform services.
- Use constructor injection and an explicit composition root. Feature modules
  communicate through domain/application contracts and app navigation, not by
  importing another feature's UI. Introduce Gradle modules incrementally as the
  foundation tasks become ready; do not invent paths or APIs before they exist.
- Keep the current vault coordinator as the sole durable writer during
  migration. Preserve transactional commits, revision checks, durable Undo,
  original assets, reminder reconciliation and existing recovery behavior.
  Evolve its interface without replacing it with competing feature snapshots.
  Backup restore must reserve this writer before staging; P03A-04 retains the
  remaining archive, collision and platform-recovery verification gates.
- The owner confirmed the app has not been released or deployed to users.
  Historical released-archive compatibility and migration from released owner
  data are not P03A-04 requirements. Test the current backup/restore contract
  and preserve real data if it exists; synthetic fixtures do not imply a release.
- A single reason to change, clear ownership and testable behavior determine
  extraction. Long files trigger review; splitting by size alone is insufficient.
- Reuse existing v3 components and tokens. Keep product-independent Sky free of
  MotoVault models, persistence, network, authentication and financial effects.

## Constants and language support

- Name business limits and units next to their rule, use generated design tokens
  for visual values, and use validated configuration for environment-specific
  settings. Keep protocol/storage identifiers stable and typed or named in their
  owning adapter. No secrets in source or resources. No global miscellaneous
  constants file or unclear names such as `VALUE_30`.
- User-facing text, validation messages, accessibility descriptions, notification
  content and export labels use localized resources, not inline literals or
  language branches. Use Compose Multiplatform resources and translated
  placeholders/plurals for shared UI, with platform-native localization for
  OS-owned integration strings. Keep domain failures typed and resolve their
  messages at the presentation/platform boundary.
- Preserve the existing language preference, Bangla digits, input semantics and
  default behavior until explicitly specified and verified. Format localized
  dates, distances, money and numbers separately from canonical stored values.
  Validate resource selection and language switching on both platforms against
  the pinned toolchain before relying on a particular locale API.
- Resource files, fixtures and generated data are the intended sources of text
  and values; review meaningful literals rather than blindly banning syntax.
  Apply these requirements to new and changed code, and migrate legacy code by
  feature. Record any temporary exceptions, owner and removal task.

## Required workflow

1. Read `AGENTS.md`, the design contract, this contract, the relevant tracker
   task and phase mapping. Inspect the current branch, dirty tree, callers,
   platforms, persistence/backup formats and existing verification evidence.
2. State the responsibility boundary, concrete files, behavior/data contracts,
   dependency changes, design impact and verification plan. Extend the v3
   prototype first for new UI states. Research-only tasks do not require code.
3. Respect tracker dependencies. Mark a feature task `in_progress` with owner
   and factual note before feature edits. Preserve unrelated work and do not
   claim proposed modules or checks are present before inspecting them.
4. Implement one bounded slice. Enforce the boundary, SRP, reuse, constants and
   localization requirements in changed code. Test critical business behavior,
   error/cancellation paths, state lifetime and durable data changes as relevant.
5. Verify Android/iOS behavior at the affected scope, including compatibility,
   existing translations and native integration when applicable. Use actual
   available quality commands; until automated gates exist, record explicit
   manual checks and limitations. A missing tool or device leaves its gate open.
6. Record sanitized evidence with actual commit and dirty diff identity,
   commands, platform, outcome and remaining gates. Keep incomplete work in
   review. Run `rtk run python3 tools/roadmap.py check` before handoff.

Do not present a build, simulator, physical device, cloud staging, signed
release or publication as proof of another. Preserve migration fixtures and
recovery plans for persisted-contract changes. Do not turn existing planning
tasks into implementation claims. Optional cloud and publication operations
retain their separate user-authorization and release gates.

## Acceptance for P03 onward

Each phase applies the task-specific mapping recorded in the tracker. Its
implementation acceptance must cover the relevant dependency direction, state
ownership, feature UI reuse, named values and localization; where persisted or
native behavior changes, it must also cover compatibility, recovery and
platform-specific evidence. Release phases verify P03A prerequisites as well as
their existing product and device gates. Completed historical P03 evidence is
not rewritten; P03A-04 records the separate architecture migration.
