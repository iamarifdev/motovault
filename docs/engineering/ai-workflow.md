# Delivery workflow

This is the single delivery process for every assistant, automation and human
contributor. [AGENTS.md](../../AGENTS.md) is the entrypoint and owns precedence,
permissions and the non-negotiable constraints. The
[architecture contract](../roadmap/ARCHITECTURE-CONTRACT.md) owns boundaries and
data rules. [Verification](verification.md) owns the gate commands and the only
list of gates. This document owns the process and the change classes. Do not
create per-feature agents, per-concern skills or duplicate policy documents. An
integration may only load `AGENTS.md` and the task; it may not redefine or relax
anything.

## Change classes

Pick the highest class that applies. The class decides what to read, how much
to plan and which gates to run.

| Class | Applies to | Read | Plan | Gates |
| --- | --- | --- | --- | --- |
| C0 | Docs, tracker or tooling text with no code behavior | `AGENTS.md`, the files touched | None | Docs row in [verification](verification.md#gates-by-class) |
| C1 | Code change with no visible, copy, persisted or native effect | C0, the contract, the task if any | Files, intended behavior and gates, stated in the handoff | Portable and workstation gates |
| C2 | Visible UI, user-facing copy, or a new feature slice | C1, the design contract, the Bangla style | The full [change plan](templates/change-plan.md) | C1 plus runtime evidence on both platforms |
| C3 | Persisted data or backup format, native platform code, permissions or notifications | C2 | C2 plus data compatibility and recovery plan | C2 plus compatibility, recovery and native evidence |

When unsure, use the higher class. A C2/C3 plan lives in the conversation or
pull-request description, and its outcome goes into the tracker note and evidence
report. Do not commit the plan unless the owner asks.

## Capability tiers

Gates need different environments. Run what your environment supports and report
the rest as open. An open gate is never converted into a pass.

- **Portable.** A clean checkout with Python 3. This is what CI runs.
- **Workstation.** macOS with Xcode, the Android toolchain, a warm Gradle cache
  and the sibling Sky Design System checkout. Needed for Gradle and iOS builds.
- **Device.** A verified running Android emulator or device, and the verified iOS
  simulator named in [verification](verification.md). Never boot, erase or
  reconfigure these without asking; reuse a running one and keep its data.

An agent that cannot reach the workstation or device tier finishes the portable
tier, leaves the task `in_review`, and lists the open gates and the exact
commands for someone else to run.

## 1. Discover

- Inspect the branch, base, dirty tree and pre-existing changes. Preserve
  unrelated work; never stage or rewrite it.
- Read the task acceptance, dependencies and required evidence with
  `python3 tools/roadmap.py show TASK-ID`. Do not open `roadmap.json` whole.
- Locate the owners of presentation, state, application, domain, storage and
  native behavior. Check every caller of a contract you intend to change.
- For a UI change, compare the v4 prototype (`design/app-ui-v4/`) and the current native screen.
- For copy changes, read the Bangla and English resource entries together.
- If another owner holds the task, or the working tree has another contributor's
  uncommitted changes in the same files, stop and ask. Parallel work uses separate
  worktrees and separate tasks.

## 2. Plan one bounded slice

For C2 and C3 fill the [change plan](templates/change-plan.md). It must state:

- the single responsibility being changed and the exact files;
- the resource keys and copy impact, and how text reaches native surfaces;
- whether any persisted value changes and how old data remains readable;
- the SRP outcome: files and functions touched, any baseline entry and its owner;
- the tests, runtime checks and evidence that will close the slice.

Reject the plan if it depends on a new hardcoded label, a new production file
over 400 lines, a new language branch or a redesign of an unrelated screen.

## 3. Implement

- Mark a roadmap task `in_progress` with owner and factual note first.
- Keep one coherent responsibility per change. Extract behavior, not line count.
- Every user-facing string goes to `composeResources/values/strings.xml` and
  `values-bn/strings.xml`; native-owned strings go to native resources.
- No inline Bengali/English display literals, no `if (language == ...)` copy and
  no concatenated translated fragments. Use placeholders and plurals.
- Bangla follows [the copy style](localization-style.md). An agent that drafts
  Bangla marks it "needs human review" in the handoff. A literal translation is a
  defect, not a neutral choice.
- Domain and application carry codes and typed failures. Presentation resolves
  labels, units, months and messages. Never persist a localized label as the
  identity of a record.
- Keep language, digit style and accepted input digits independent.
- Preserve existing data, originals, Undo, recovery and compatibility fixtures.
- Do not add an agent or skill. If a concrete integration unavoidably needs one,
  add a single thin adapter that points to `AGENTS.md`, and record why in the
  tracker.

## 4. Verify

Run the gates for the change class from [verification](verification.md). App
behavior tests run with the platform runner: Gradle/Kotlin and Android
instrumentation on Android, `xcodebuild`/XCTest on iOS. Python runs the
repository checks only. It must not launch or drive the app, coordinate app
tests, or decide whether an app flow passed.

A missing device, simulator, permission or external tool leaves that gate open.
Do not replace runtime evidence with a build, a screenshot with a recording, or a
unit test with a translation review. Do not repeat a passing run when neither
code nor test setup changed.

### Stop conditions

Stop. Do not hand off as done. Leave the task `in_review` and ask the owner if
the change introduces any of:

- a new user-facing literal or a language branch;
- a new over-threshold production file or function, or a file over 600 lines
  without a recorded exception;
- a new storage-to-UI import or an architecture-baseline violation;
- a display label used as persisted identity;
- Bangla copy with no human review recorded where a gate needs it;
- a green unit suite with no required runtime language-switch evidence;
- unrelated files in the diff.

## 5. Evidence and handoff

- Add a sanitized report under `docs/roadmap/evidence/` for C2/C3 work, using the
  [report template](templates/verification-report.md).
- **Revision identity.** Record the base commit and the working-tree identity:

  ```bash
  git rev-parse HEAD
  git status --short
  git diff HEAD | shasum -a 256
  git ls-files --others --exclude-standard -z | xargs -0 shasum -a 256
  ```

  Never label an uncommitted build as a clean commit.
- **Sanitized** means the report contains no owner data, document images or
  contents, tokens, credentials, signing material, or user names in file paths.
  Simulator and device identifiers may be recorded.
- Attach the required evidence types with the tracker. Do not mark done from a
  compile or a partial runtime pass.
- Run `python3 tools/roadmap.py check`. After resolving a merge or rebase, run
  `python3 tools/roadmap.py render` instead of hand-merging generated views.
- State exactly what was and was not verified, which gates are open, and which
  copy still needs human review.
- Commit only after the owner authorizes it. Never push, amend or force-push
  without explicit instruction.

## Definition of done

A work item is done only when all are true:

- acceptance criteria and required evidence are satisfied;
- every new or changed user-facing string is in resources, in both languages;
- a named human has reviewed the Bangla, or the task is `in_review`;
- no new hardcoded copy, language branch or persisted display label exists;
- size rules in `AGENTS.md` hold, or a recorded exception has an owner and a
  removal trigger;
- every gate required for the change class passes, or is reported open and the
  task is `in_review`;
- the diff contains only the intended slice.
