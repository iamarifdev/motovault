# Shared AI delivery workflow

This is the **single** workflow for Codex, Claude, Gemini, other assistants and
human contributors. `AGENTS.md` is the repository entrypoint;
`CLAUDE.md` and `GEMINI.md` are thin adapters. Do not create per-feature agents,
per-concern skills or duplicate policy documents. If an integration needs a
skill, it may only load this workflow and the task — never redefine or relax it.

Read, in this order:

1. `AGENTS.md`
2. [the architecture contract](../roadmap/ARCHITECTURE-CONTRACT.md)
3. [the design contract](../roadmap/DESIGN-CONTRACT.md) for any UI change
4. [the Bangla copy style](localization-style.md) for any user-facing text
5. `docs/roadmap/roadmap.json` / `roadmap.py show TASK-ID` for the exact task
6. the current implementation, callers, tests and prior evidence

## 1. Discover

- Inspect branch/base, dirty tree and pre-existing changes. Preserve unrelated
  work; never stage or rewrite it.
- Read the task acceptance, dependencies and required evidence.
- Locate the owners of presentation, state, application, domain, storage and
  native behavior. Check every caller of a contract you intend to change.
- For a UI change, compare the v3 prototype and the current native screen.
- For copy changes, read the Bangla and English resource entries together.

## 2. Plan one bounded slice

Fill the [change plan](templates/change-plan.md). It must state:

- the single responsibility being changed and the exact files;
- the resource keys / copy impact and how text reaches native surfaces;
- whether any persisted value changes and how old data remains readable;
- the SRP outcome: files/functions touched, any baseline entry and its owner;
- the tests, Android/iOS runtime checks and evidence that will close the slice.

Reject the plan if it depends on a new hardcoded label, a new production file
over 400 lines, a new language branch or a redesign of an unrelated screen.

## 3. Implement

- Mark a roadmap task `in_progress` with owner and factual note first.
- Keep one coherent responsibility per change. Extract behavior, not line count.
- Every user-facing string goes to `composeResources/values/strings.xml` and
  `values-bn/strings.xml`; native-owned strings go to native resources.
- No inline Bengali/English display literals, no `if (language == ...)` copy and
  no concatenated translated fragments. Use placeholders/plurals.
- Bangla follows [the copy style](localization-style.md): concise, spoken,
  bKash/Pathao-style product Bangla with familiar loanwords. A literal
  translation is a defect, not a neutral choice.
- Domain/application carry codes and typed failures. Presentation resolves labels,
  units, months and messages. Never persist a localized label as the identity of
  a record.
- Keep language, digit style and accepted input digits independent.
- Preserve existing data, originals, Undo, recovery and compatibility fixtures.
- Route commands through `rtk`; use Podman for containers.
- Do not add a new agent or skill unless a concrete integration requires one.
  If one is unavoidable, add one thin adapter that points here.

## 4. Verify

Run the task-specific checks, then the portable gates:

```bash
rtk run ./gradlew :composeApp:testDebugUnitTest \
  :composeApp:compileKotlinIosSimulatorArm64 --offline

rtk run python3 tools/ui_literal_gate.py
rtk run python3 tools/bangla_copy_lint.py
rtk run python3 tools/ui_literal_gate.py --fuel
rtk run python3 tools/responsibility_gate.py
rtk run python3 tools/architecture_dependencies.py
rtk run python3 tools/architecture_package_cycles.py
rtk run python3 tools/fuel_policy_literals.py
rtk run python3 -m unittest discover -s tools/tests -p 'test_*.py'
rtk run python3 tools/roadmap.py check
```

Also use, when relevant:

- `rtk run ./gradlew :composeApp:assembleDebug --offline` for Android host changes.
- the pinned iOS simulator build and runtime checks for native changes.
- an actual Android emulator and iOS simulator language switch for any copy or
  locale change; verify both languages, digit styles and the affected native
  notification/permission surfaces.
- screenshots or recordings for visible changes, kept separate from test output.

A missing device, simulator, permission or external tool leaves that gate open.
Do not replace runtime evidence with a build, a screenshot with a recording, or a
unit test with a translation review.

### Stop conditions

Stop and report, rather than merge, if the change introduces any of:

- a new user-facing literal or a language branch;
- a production file over 400 lines without a reviewed owner/exception;
- a new storage-to-UI import or an architecture-baseline violation;
- the same display label used as persisted identity;
- a Bangla string that reads like a literal translation;
- a green unit suite with no required runtime language-switch evidence;
- unrelated files in the diff.

## 5. Evidence and handoff

- Add a sanitized report under `docs/roadmap/evidence/` with revision, dirty-tree
  identity, commands, platform, outcome and remaining gates.
- Attach the required evidence types with the tracker; do not mark done from a
  compile or a partial runtime pass.
- Run `roadmap.py check` and include the tracker/generated views in the change.
- State exactly what was and was not verified. Leave incomplete work `in_review`.
- Commit only after the user authorizes it; never push, amend or force-push without
  explicit instruction.

## Definition of done

A work item is done only when all are true:

- acceptance criteria and required evidence are satisfied;
- every new/changed user-facing string is in resources, in both languages;
- Bangla has been reviewed against the copy style;
- no new hardcoded copy, language branch or persisted display label exists;
- production files remain under 400 lines and functions under 80, or a recorded
  exception has an owner and removal trigger;
- all portable gates above pass, including resource parity, literal, SRP,
  dependency, package-cycle, fuel and roadmap checks;
- Android/iOS compilation and the task's runtime gates pass;
- the diff contains only the intended slice.
