# MotoVault repository instructions

## Feature delivery and progress

The tracked feature roadmap is in `docs/roadmap/README.md`. Its source of truth is
`docs/roadmap/roadmap.json`; generated views are `PROGRESS.md` and
`IMPLEMENTATION-PLAN.md` in that directory.

When implementing a roadmap feature:

1. Read the design contract and the task with `rtk run python3 tools/roadmap.py show TASK-ID`.
2. Check `rtk run python3 tools/roadmap.py next`. Inspect the current branch/base and
   affected files before editing; preserve unrelated working changes.
3. Mark the task `in_progress` with an owner and a short factual note before feature edits.
4. Update progress and evidence in the same working change as the implementation.
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
