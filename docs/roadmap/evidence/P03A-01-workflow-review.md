# P03A-01 workflow review

Source baseline: `860c3ec04dac73bc10d6aba65798aff31887fdbb` on `main`. The reviewed work is uncommitted and includes the existing P03A roadmap edits plus the files below; the commit alone does not identify these changes.

## Plan and scope

The shared contract is `docs/roadmap/ARCHITECTURE-CONTRACT.md`. The contributor entry points `AGENTS.md`, `CLAUDE.md` and `GEMINI.md` all explicitly reference it. The contract links the practical architecture, conventions, verification and AI workflow guides under `docs/engineering/`. It remains authoritative if those guides differ. The decision record and templates are in `docs/engineering/decisions/` and `docs/engineering/templates/`.

The guides document feature ownership, SRP, inward dependencies, constants, localization, dirty-tree and data protection, tracker workflow, platform evidence and transitional exceptions. The phase-specific mapping is in `roadmap.json` and visible through the generated implementation plan and `roadmap.py show`.

## Verification performed

- Checked that all three assistant entry files contain the same architecture-contract path. Claude and Gemini entry files have identical SHA-256 `e4d48f4d181f9cf82bc3657fc3460db444be7ea6d99a1147a35f107285a03d96`; AGENTS SHA-256 is `57a5c495b5d30305dbd13b35978b017c1d2e0c1812a772198f81049522b9d284`.
- Contract SHA-256: `fd9f00e506f2f599dd48f2e31bf21a592b66e23428dd67585434bf4be5e39302`. AI workflow SHA-256: `8f4f3fbcb281ddf5aff86736da223d54644e728239ba5d43ec4aa56f718782ef`.
- `rtk run python3 tools/roadmap.py show P03A-01` exposed the phase requirements, prerequisites, acceptance and evidence gates. The tracker update command set the task to `in_progress` and regenerated both views.
- `rtk run python3 tools/roadmap.py check`: passed, 18 phases and 84 tasks, generated views current at the time of the check.
- `rtk run python3 -m unittest discover -s tools/tests`: 21 tests passed.
- `rtk git diff --check`: passed for tracked changes. New untracked files were reviewed as part of the file inspection.

## Limits

This review checks repository instructions and tracker behavior; it does not prove that external Claude or Gemini clients automatically load their entry files. It does not implement the target modules, localization foundation, runtime flows or automated architecture checks. Those remain separate P03A tasks. No app build or native runtime was required for this documentation task.
