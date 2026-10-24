# MotoVault tracked feature roadmap

Start with [progress and next tasks](PROGRESS.md), then open the
[phase-by-phase implementation plan](IMPLEMENTATION-PLAN.md). The plan covers all
seven crucial features and the additional ownership, workshop, resale, widget,
seasonal, rider and cloud-backup features discussed on 24 October 2026.

The scope is Android and iOS. The design reference is
[app-ui-v3](../../design/app-ui-v3/README.md), with the existing native brand logos
preserved during brand selection. [Design rules and screen placement](DESIGN-CONTRACT.md)
apply to every phase. [Baseline findings](BASELINE.md) distinguish existing code,
demo concepts and future work.

P00 deliverables: [baseline captures and verification](evidence/P00-04-verification.md),
[local comparison index](evidence/p00/index.html), [data/migration contract](DATA-MIGRATION-CONTRACT.md)
and [contract review](evidence/P00-05-review.md). See the progress board for the
actual task status; captured product defects remain open in later phases.

## Delivery sequence

| Stage | Phases | Exit |
| --- | --- | --- |
| Foundation | P00 | Reproducible fixtures, design contract, migration strategy and tracker |
| Trustworthy local beta | P01-P03 | Real reminders, protected documents, encrypted backup and complete restore |
| Core ownership release | P04-P08 | Bangladesh maintenance, renewal assistance, cost insights, native capture and release evidence |
| Additional ownership | P09-P13, P16 | Multiple bikes, workshops/warranties, handover pack, real widgets and seasonal/breakdown help |
| Optional rider mode | P14 | Recorded work earnings and cost attribution, separately piloted |
| Optional cloud backup | P15 | Reviewed provider/account binding, isolated encrypted transport and real recovery |

Phase numbering is the preferred reading/delivery order, not a requirement to
idle independent work. Task dependencies define what can start. The core release
does not wait for later ownership or optional modules. Release phases produce
verified candidates and rollout decisions; publication is a separate action.

No calendar promises are assigned before implementation baselines and initial
slices are measured. Relative sizes 1/2/3/5/8 help split work and compare scope;
they are not hours or a claim about engineering speed. Assign people and dates
when the work is scheduled, after accounting for physical-device access and
external decisions.

## What updates automatically

`roadmap.json` contains task scope, acceptance, dependencies, status, owner,
evidence and update history. `tools/roadmap.py update` and `note` write the data
and regenerate both Markdown views immediately. `check` detects invalid
dependencies, missing evidence files and stale generated views. The root
[AGENTS.md](../../AGENTS.md) instructs future implementation work to update the
tracker in the same change.

There is no background watcher, recurring job or automatic claim of completion.
The implementer records progress after actual work. A Markdown report can be
opened at any time, and Git preserves its history once committed.

## Everyday commands

Run from the repository root (the script also resolves its repository paths when
called by absolute path):

```sh
rtk run python3 tools/roadmap.py status
rtk run python3 tools/roadmap.py next
rtk run python3 tools/roadmap.py show P00-04
rtk run -c 'python3 tools/roadmap.py update P00-04 in_progress --owner Codex --note "Capturing synthetic Android and iOS baselines"'
```

Record partial progress or attach evidence without changing status:

```sh
rtk run -c 'python3 tools/roadmap.py note P00-04 --note "Android baseline recorded; iOS baseline pending" --evidence android_runtime:docs/roadmap/evidence/P00-04-android.md'
```

After all acceptance checks pass, attach each required evidence type. Paths below
illustrate the naming; the files must exist and contain the actual observations:

```sh
rtk run -c 'python3 tools/roadmap.py update P00-04 done --note "Both native baselines captured and compared with v3" --evidence verification:docs/roadmap/evidence/P00-04-checks.md --evidence design:docs/roadmap/evidence/P00-04-design.md --evidence android_runtime:docs/roadmap/evidence/P00-04-android.md --evidence ios_runtime:docs/roadmap/evidence/P00-04-ios.md'
rtk run python3 tools/roadmap.py check
```

Block, review, defer or reopen work explicitly:

```sh
rtk run -c 'python3 tools/roadmap.py update P00-04 blocked --note "iOS simulator cannot boot; Android baseline retained; repair simulator before completing this task"'
rtk run -c 'python3 tools/roadmap.py update P00-04 in_review --note "Implementation and captures ready; final comparison pending"'
rtk run -c 'python3 tools/roadmap.py update P00-04 planned --note "Reopened because the design baseline changed"'
```

An `in_review` task requiring implementation evidence must already have that
evidence attached. Starting, reviewing or completing a task requires its
prerequisites to be done. If reopening a prerequisite would invalidate active or
completed dependent tasks, reopen those dependent tasks first. A blocked task
needs a concrete reason and next action. Future external decisions are recorded
as risks; unstarted tasks are not all labelled blocked.

Commands with multi-word notes use `rtk run -c` so their quoting is preserved by
the shell. The note is ordinary text; quote it as shown rather than interpolating
untrusted content into a command.

After an intentional scope/acceptance/dependency edit to `roadmap.json`, use
`note` on the affected task to explain the change and regenerate the views, or
run `render` after recording the reason in its notes/activity. Existing evidence
does not automatically establish acceptance for expanded scope; reopen affected
tasks and reverify them.

## Status and evidence rules

| Status | Meaning |
| --- | --- |
| planned | Scope recorded; no active implementation |
| in_progress | Owner is actively working; prerequisites complete |
| in_review | Work exists but acceptance/review is incomplete |
| blocked | A concrete condition prevents progress; reason and next action recorded |
| done | All acceptance checks and required evidence satisfied |
| deferred | Explicitly postponed scope; remains visible and does not count as shipped |

Evidence kinds: `plan`, `implementation`, `verification`, `design`,
`android_runtime`, `ios_runtime`, `android_device`, `ios_device`, `cloud`,
`research`, `review`. Evidence references are repository-relative files. Store
remote results as sanitized local reports with relevant source links. Do not
attach personal documents, secrets, precise locations or unredacted owner data.

The validator checks that evidence metadata and files exist. It cannot judge
whether a report's claims are true or whether an image looks correct; the
implementer/reviewer remains responsible for reading acceptance criteria and
the evidence. A test result never substitutes for a required device result.

Completion is weighted by relative scope points for `done` tasks only. Planning
foundation is shown separately from product delivery. Deferred and optional
scope remain in the all-product denominator; milestone/task counts show the
core release independently. Percentages are not release readiness or time spent.

## Verification and handoff

The planning/tracker check uses no new third-party package:

```sh
rtk run python3 -m unittest discover -s tools/tests -p test_roadmap.py
rtk run python3 tools/roadmap.py check
```

For actual feature changes, choose the checks justified by the task. The current
repository commands include:

```sh
rtk run env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
rtk run env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :composeApp:compileKotlinIosArm64 :composeApp:iosSimulatorArm64Test
rtk run ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

These commands are guidance from the current build, not results from this
planning change. Prototype changes should run its existing type-check/build
and browser checks through `rtk`, without installing dependencies unless needed
and authorized. Check physical notifications, camera, authentication, secure
storage, native share/widget lifecycle and recovery on real devices where the
task requires them.

Copy [the evidence report template](evidence/TEMPLATE.md) for each task. A handoff
states the task IDs completed, paths changed, tests/runtime evidence, remaining
gates, blocker/next task, and tracker check result. Commit/push/deploy actions
follow the user's specific instructions; this plan does not perform them.
