# P17-01 Baseline Evidence — Salvage & Clean v4 Port Baseline

- Task and acceptance criteria: `P17-01` (park `wip/gemini-v4-attempt`, restart from `3754157`, salvage icons/catalogue/drawables/`SpokenNumberParser`/`VoiceCaptureActions.anyAvailable`, rewrite `DESIGN-CONTRACT.md` and `design-reference-v4.json`).
- Source commit and working-diff identity:
  - Branch: `feat/v4-literal-port` from base `37541576a2fc6870590159abe9a82ba1a2bf4f0a`
  - Parked branch: `wip/gemini-v4-attempt` (`bdee299`)
  - Working-tree diff SHA-256 at baseline check: `f931499f6e640cd5fa7a285a7dbc39b3c171de15e1817e9d8fe4e00ed04ebf91` (uncommitted)
- Toolchain, platform, OS and device: macOS workstation (`openjdk@17`, Gradle 8.10.2, Python 3, Node v24.22.1).
- Commands, exit status and artifacts:
  - `node tools/build-v3-icons.mjs` + `--check`: exit 0, `58 v3 Lucide icons verified (0.468.0)` including `fi_delete.svg` and `fi_lock-plain.svg`.
  - Catalogue parity check (`catalog.ts` vs `BikeCatalogue.kt` & `BikeCatalogueFamilies.kt`): 94/94 variant IDs, names, and labels match (0 mismatches).
  - Asset parity check: 89/89 `model_*` bike PNGs and `brta_licence_bg.jpg` / `brta_registration_bg.jpg` match `design/app-ui-v4/public/` SHA-256 byte-for-byte.
  - Portable gates (`check_agent_docs.py`, `ui_literal_gate.py`, `ui_literal_gate.py --fuel`, `bangla_copy_lint.py`, `responsibility_gate.py`, `architecture_dependencies.py`, `architecture_package_cycles.py`, `fuel_policy_literals.py`, `unittest discover` 61 tests, `roadmap.py check`): all exit 0.
  - `./gradlew :composeApp:testDebugUnitTest :composeApp:compileKotlinIosSimulatorArm64 --offline`: `BUILD SUCCESSFUL` (exit 0).
- Observed behavior and assertions: Clean baseline compiles for Android and iOS simulator (`arm64`) and passes all unit tests and portable checks.
- Data/format compatibility and recovery checks: No schema changes in `P17-01`.
- Accessibility, language and design scope: Baseline catalogue, icon paths, and `DESIGN-CONTRACT.md` / `design-reference-v4.json` synchronized with `design/app-ui-v4/src/`.
- Unverified checks, blockers and follow-up: None for `P17-01`. Proceeding to `P17-02` (Phase 1 Foundation).
