# P03A-05 quality-gate verification

Status: passed on the uncommitted working tree based on
`860c3ec04dac73bc10d6aba65798aff31887fdbb`. Incremental history remains in
[P03A-05-quality-gates-progress.md](P03A-05-quality-gates-progress.md).

## Executable checks and scopes

| Check | Command | Enforced scope |
| --- | --- | --- |
| Dependency direction ratchet | `rtk run python3 tools/architecture_dependencies.py` | shared `domain` and `application` imports/qualified references, including string-template expressions |
| Shared package cycles | `rtk run python3 tools/architecture_package_cycles.py` | packages under shared `domain` and `application` |
| Configured Gradle module cycles | `rtk run ./gradlew architectureModuleEdges --offline` then `rtk run python3 tools/architecture_cycles.py` | configured project dependencies |
| Shared UI direct literals | `rtk run python3 tools/ui_literal_gate.py` | direct `Text`/`contentDescription` literals in shared `ui` against the reviewed baseline |
| Fuel UI direct literals | `rtk run python3 tools/ui_literal_gate.py --fuel` | `ui/fuel`, empty baseline |
| Fuel domain numeric policy | `rtk run python3 tools/fuel_policy_literals.py` | `domain/fuel` unnamed numeric literals |
| Translation keys, placeholders and plurals | `rtk run python3 -m unittest discover -s tools/tests -p test_localization_resources.py` | shared English/Bangla catalogs and current native reminder tables |
| Portable fixtures | `rtk run python3 -m unittest discover -s tools/tests -p 'test_*.py'` | all tool fixtures |

The shared UI literal baseline was reviewed and added in the working tree as
`tools/ui-literal-baseline.json`: 178 path/literal entries covering 180
occurrences. Every entry records an owner and the feature-migration task expected
to remove it:

- `OwnershipForms.kt`, `Screens.kt`, `Onboarding.kt`, `Sheets.kt`,
  `ReminderSheets.kt`, `GaugeDetailSheet.kt`, `Components.kt` → `P04-04`
- `DocumentFlow.kt`, `DocumentWallet.kt` → `P05-03`
- `privacy/VaultPrivacySheet.kt` → `P09-04`
- `PassportExportUi.kt` → `P11-02`

New literals and stale baseline entries both fail. The fuel pilot keeps the
stricter empty baseline. The baseline is intentionally legacy debt, not a claim
that those screens are localized.

## Results on 2026-11-01

```text
rtk run python3 tools/architecture_dependencies.py
  Dependency import baseline matches (0 existing violations).

rtk run python3 tools/architecture_package_cycles.py
  No shared domain/application package cycles (8 packages, 7 edges).

rtk run python3 tools/ui_literal_gate.py
  Direct UI literal baseline matches (180 existing occurrences).

rtk run python3 tools/ui_literal_gate.py --fuel
  Direct UI literal baseline matches (0 existing occurrences).

rtk run python3 tools/fuel_policy_literals.py
  (passed; no unnamed nontrivial values in domain/fuel)

rtk run python3 -m unittest discover -s tools/tests -p 'test_*.py'
  Ran 46 tests ... OK
```

The localization fixture suite and the roadmap tests are inside the 46 passing
tests. The configured Gradle module report is generated locally (one configured
edge, `:composeApp` → `:skyDesignSystem`) and its cycle check passed. It is not in
the portable CI job because a clean checkout lacks the external Sky source.

## CI

`.github/workflows/architecture-checks.yml` runs the portable checks on pushes and
pull requests: dependency ratchet, shared package cycles, architecture fixtures,
UI literal fixtures, shared and fuel literal gates, fuel constants, localization
fixtures and roadmap validation. The workflow has not been executed on GitHub and
branch protection is not verified from this workspace.

## Limits

- The checks are lexical ratchets, not a Kotlin compiler/parser or complete
  semantic analysis. They do not detect indirect strings, custom text components,
  every semantics API, native text, design-token misuse, or business meaning of a
  named constant.
- The broader shared UI baseline prevents new literal debt but does not prove the
  existing screens are localized; the removal tasks remain open.
- Package-cycle coverage is limited to the shared `domain`/`application` layers;
  other source sets and dynamic dependencies need the Gradle report and review.
- The GitHub workflow has not run remotely, and branch protection is not verified.
