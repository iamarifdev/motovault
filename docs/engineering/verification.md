# Verification and evidence

The relevant task's `required_evidence` and phase exit gate define completion. [The roadmap guide](../roadmap/README.md) and `AGENTS.md` define tracker operations. Evidence records must identify the actual source revision and working-tree changes, commands, outcomes, environment, observed scope and remaining limitations. Do not label an uncommitted build as the clean HEAD revision.

For every slice, select checks according to the changed behavior:

1. Inspect dependency direction, component responsibilities, named values, localized text, lifecycle ownership and diff scope.
2. Exercise business invariants, validation, failure, cancellation, conflicts and Undo where affected.
3. For persistence and backup changes, test existing serialized data, migrations, originals, asset references, partial failure and recovery. A Git revert is not a database rollback.
4. For visible changes, check the v3 prototype and native output, both supported languages, semantics, scaling, keyboard and reduced motion as relevant.
5. Run Android and iOS builds/tests and runtime checks to the extent demanded by the task. Native platform functionality needs appropriate emulator/simulator and physical-device evidence; record those independently.
6. Run `rtk run python3 tools/roadmap.py check` before handoff. Add sanitized reports in `docs/roadmap/evidence/` and attach the exact evidence types with the tracker.

The iOS testing restriction is iOS 27 iPhone 18 Pro or Pro Max. Verify the running simulator identity before reuse; preserve its data unless a clean-state test is explicitly needed. This restriction does not change the app's deployment target. Cloud, signed builds and publication each need their own evidence and authorization.

Use [the report template](templates/verification-report.md). Report manual inspection separately from automated evidence. The P03A-05 checks below are lexical ratchets, not semantic proof; keep incomplete validation `in_review` and do not mark a task done to unblock a dependency.

The partial dependency ratchet can be run with `rtk run python3 tools/architecture_dependencies.py`; its fixtures run with `rtk run python3 -m unittest discover -s tools/tests -p test_architecture_dependencies.py`. The baseline records existing exceptions with an owner and removal task. New imports and fully qualified references, including those in string-template expressions, fail, and a resolved entry must be removed from the baseline. This check scans shared domain and application sources only. It does not check cycles, platform source sets, literals or feature-to-feature dependencies, and is not a Kotlin compiler parser. It is not yet a substitute for the P03A-05 gate or manual review.

Run `rtk run python3 tools/architecture_package_cycles.py` to check dependencies between packages in the shared `domain` and `application` directories. Its positive and negative fixtures run with `rtk run python3 -m unittest discover -s tools/tests -p test_architecture_package_cycles.py`. The check reads package declarations, imports (including wildcard and alias imports), and fully qualified references, including expressions inside string templates. It fails if either required layer is missing or empty. It is lexical, does not resolve Kotlin symbols, and does not cover other source sets, package cycles outside those two layers, or a dependency implied without an import or fully qualified reference. It complements rather than replaces the configured Gradle module check.

To check configured Gradle project dependencies, first run `rtk run ./gradlew architectureModuleEdges --offline`, then `rtk run python3 tools/architecture_cycles.py`. Run both in sequence: the report is a generated snapshot and can otherwise be stale. The check covers configured cross-project dependencies, not package or source-set cycles inside a project. Same-project edges from Kotlin Multiplatform configurations are excluded. Its fixtures run with `rtk run python3 -m unittest discover -s tools/tests -p test_architecture_cycles.py`. The report task requires the normal project configuration, including the Sky dependency.

Run `rtk run python3 -m unittest discover -s tools/tests -p test_localization_resources.py` for shared English/Bangla key and placeholder parity, plural checks, and the current native reminder tables. Negative fixtures cover missing shared keys, placeholder position and type changes, and missing or duplicate plural variants. This is not a check for untranslated values, every native resource type, or correctness of the translations.

The GitHub Actions workflow `.github/workflows/architecture-checks.yml` runs the portable dependency ratchet, shared package-cycle check, architecture-check fixtures, resource parity checks and roadmap checks on pushes and pull requests. It does not run the configured Gradle project-cycle check: project configuration currently requires the external Sky Design System source, which a clean checkout does not contain. Run the Gradle report and cycle check above in an environment with that source. CI execution and branch protection must be verified in GitHub after the workflow is pushed; the workflow's presence alone does not prove either.

Bangla copy is reviewed against [localization-style.md](localization-style.md).
`rtk run python3 tools/ui_literal_gate.py` ratchets direct string literals supplied to shared UI `Text`, `contentDescription`, `Field`, `VaultButton`, `Primary`, `Eyebrow`, `Heading`, `VaultChip`, `Sheet`, `DocumentNote`, `DocumentCopy` and `DocumentHeading` sinks against `tools/ui-literal-baseline.json`. Each baseline entry records an owner and a removal task; new literals and resolved entries both fail. The expanded P08-05 baseline currently contains 18 path/literal entries (18 occurrences); the fuel pilot remains empty. `--inventory` prints the current inventory. Fixtures run with `rtk run python3 -m unittest discover -s tools/tests -p test_ui_literal_gate.py`. The lexical check does not detect indirect variables, custom text components, every semantics API, native sources or numeric literals, and its literal parsing is not a Kotlin parser. Do not interpret an empty inventory as full localization coverage.

For an informative comparison with the current committed revision, run `rtk run python3 tools/compare_ui_literal_head.py`. It compares path and literal hashes and counts; it does not follow renamed files or prove the changed code contains no other hardcoded text. This comparison is not a stable CI ratchet because HEAD changes after each commit.

Run `rtk run python3 tools/responsibility_gate.py` to ratchet handwritten
Kotlin files above 400 lines and brace-delimited functions above 80 lines against
`tools/responsibility-baseline.json`. Files above 600 lines need a recorded
exception; growth and new over-threshold entries fail. Fixtures run with
`rtk run python3 -m unittest discover -s tools/tests -p test_responsibility_gate.py`.
The checker is lexical; it does not measure expression-body functions or judge
whether a split has one coherent responsibility. Review [conventions](conventions.md)
manually as well.

The narrower fuel-presentation pilot is enforced with `rtk run python3 tools/ui_literal_gate.py --fuel` and runs in the GitHub workflow. It uses the explicit empty `tools/ui-fuel-literal-baseline.json`; a direct literal at the supported sinks fails rather than being silently accepted. The same lexical and sink limitations above apply, including no detection of indirect strings or magic numbers. The broad shared-UI baseline and the fuel pilot run together in CI.

Run `rtk run python3 tools/fuel_policy_literals.py` to reject unnamed nontrivial numeric literals in the shared `domain/fuel` pilot. Named `const val` declarations and basic zero/one values are permitted; the test fixtures cover unnamed values, numeric ranges, hex values, negative sentinels, string templates, missing scope and allowed constants. Run them with `rtk run python3 -m unittest discover -s tools/tests -p test_fuel_policy_literals.py`. This lexical check is scoped to that directory; it cannot determine a number's business meaning, resolve constants, enforce UI design tokens, or validate constants in other features. Review named values and exceptions manually.
