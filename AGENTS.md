# MotoVault repository instructions


## Non-negotiable delivery constraints

These apply to every work item, not only localization or cleanup tasks.

- **One workflow.** `docs/engineering/ai-workflow.md` is the single delivery
  workflow. `CLAUDE.md` and `GEMINI.md` are thin adapters; do not create
  per-feature agents, duplicate policy files or assistant-specific rules.
  A skill may load this workflow and a task, but may not relax its gates.
- **Localize at the boundary.** Every user-facing string — screen copy, button,
  label, error, validation message, accessibility text, notification, export and
  native prompt — comes from shared or platform resources. No inline
  Bengali/English literals and no `if (language == ...)` display branches.
- **Modern Bangla.** Bangla copy follows
  [the localization style](docs/engineering/localization-style.md): concise,
  spoken product Bangla with familiar loanwords, matching leading Bangladeshi
  apps. Review the Bangla and English together; a literal translation is a defect.
- **Stable data, localized display.** Persist IDs/codes, not display labels.
  Resolve catalogue names, units, months and status copy through resources.
  Keep language, digit style and input-digit support independent.
- **Single responsibility.** Review handwritten files over 400 lines and functions
  over 80 lines. A new or expanded file over 600 lines needs a recorded exception
  with an owner and removal trigger. Do not split by line count alone.
- **No new debt.** New hardcoded copy, untranslated language branches, production
  files over 400 lines, storage-to-UI imports or unowned large-file exceptions
  fail the work item.
- **Gates are part of done.** Run the localization, responsibility, dependency,
  package-cycle, resource-parity, fuel-policy and roadmap checks in
  `docs/engineering/verification.md`; run unit tests and Android/iOS compilation.
  Language-switch runtime evidence is required when copy or locale behavior changes.
- **One bounded slice.** Preserve unrelated changes. Update the task, evidence and
  generated roadmap views in the same change. Do not claim a build, simulator,
  device or translation review as evidence for a different gate.

## Feature delivery and progress

The tracked feature roadmap is in `docs/roadmap/README.md`. Its source of truth is
`docs/roadmap/roadmap.json`; generated views are `PROGRESS.md` and
`IMPLEMENTATION-PLAN.md` in that directory.

For P03 maintenance and all work from P04 onward, read
`docs/roadmap/ARCHITECTURE-CONTRACT.md` and the applicable phase's architecture
requirements in the tracker before planning or editing. Use the same contract for
Codex, Claude, Gemini and human contributors. The shared quality gates are in
place; the target module layout still evolves incrementally.

When implementing a roadmap feature:

1. Read `docs/engineering/ai-workflow.md`, the architecture/design contracts,
   `docs/engineering/localization-style.md` and the task with
   `rtk run python3 tools/roadmap.py show TASK-ID`.
2. Check `rtk run python3 tools/roadmap.py next`. Inspect the current branch/base and
   affected files before editing; preserve unrelated working changes.
3. Mark the task `in_progress` with an owner and a short factual note before feature edits.
4. Update progress and evidence in the same working change as the implementation.
   Keep copy in resources, persist stable IDs and verify SRP as part of the change.
   Record a concrete blocker when work is blocked. Keep incomplete validation in
   `in_review`; do not mark a task done just because its code compiles.
5. Complete the task only when its acceptance criteria and required evidence are
   satisfied. Record sanitized reports under `docs/roadmap/evidence/` with the
   actual build revision and the scope of verification.
6. Run `rtk run python3 tools/roadmap.py check` before handoff. Include the tracker
   and generated views when committing that feature, if a commit is authorized.

Use the tracker `update` and `note` commands so progress views and the activity log
are regenerated together. Do not hand-edit generated Markdown. Scope, acceptance
or dependency changes belong in the JSON and must be explained in an activity note.
The tracker does not run in the background or infer completion from Git commits.

## Design and data safeguards

- `design/app-ui-v3/` is the active visual and interaction reference. Read
  `docs/roadmap/DESIGN-CONTRACT.md` before adding a UI flow.
- Keep the existing native brand logos during brand selection, aligned with v3.
- Reuse v3 typography, tokens, four-tab navigation, quick-add, sheets, keypads,
  date chips, motion, haptics, reduced-motion and Undo patterns.
- Extend the prototype for new states before porting their UI. Preserve the
  established visual system and keep demo data separate from owner records.
- Preserve document originals and existing data. Include migrations, compatibility
  fixtures and rollback/recovery evidence for persisted-contract changes.
- Keep reusable `skyDesignSystem` free of MotoVault domain models, networking,
  authentication, persistence and financial effects.
- Distinguish source inspection, tests, build, emulator/simulator, physical device,
  cloud staging, signed release and actual publication evidence.

Route shell commands through `rtk`, using a dedicated subcommand where available
and `rtk run` otherwise. Use Podman for any required containers.
