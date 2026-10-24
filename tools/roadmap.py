#!/usr/bin/env python3
"""Maintain MotoVault's evidence-based feature roadmap using only the standard library."""

from __future__ import annotations

import argparse
import copy
import datetime as dt
import json
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
DATA = Path("docs/roadmap/roadmap.json")
BOARD = Path("docs/roadmap/PROGRESS.md")
PLAN = Path("docs/roadmap/IMPLEMENTATION-PLAN.md")
STATUSES = {"planned", "in_progress", "in_review", "blocked", "done", "deferred"}
EVIDENCE = {"plan", "implementation", "verification", "design", "android_runtime",
            "ios_runtime", "android_device", "ios_device", "cloud", "research", "review"}
LABELS = {"planned": "Planned", "in_progress": "In progress", "in_review": "In review",
          "blocked": "Blocked", "done": "Done", "deferred": "Deferred"}


def now() -> str:
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat()


def local_time(value: str) -> str:
    dhaka = dt.timezone(dt.timedelta(hours=6))
    return dt.datetime.fromisoformat(value).astimezone(dhaka).strftime("%Y-%m-%d %H:%M BDT (UTC+06)")


def git_revision(root: Path) -> str:
    try:
        return subprocess.run(["rtk", "git", "rev-parse", "--short", "HEAD"], cwd=root,
                              text=True, capture_output=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return "unavailable"


def tasks(data: dict) -> dict:
    return {t["id"]: (p, t) for p in data["phases"] for t in p["tasks"]}


def dependencies(phase: dict, task: dict) -> list[str]:
    return list(dict.fromkeys(phase["depends_on"] + task["depends_on"]))


def safe_path(root: Path, reference: str) -> Path:
    # Evidence is a repository artifact. Remote results need a sanitized local report.
    path = Path(reference.split("#", 1)[0])
    if path.is_absolute() or ".." in path.parts or not path.parts:
        raise ValueError(f"Evidence must be a repository-relative path: {reference}")
    resolved = (root / path).resolve()
    if not resolved.is_relative_to(root.resolve()):
        raise ValueError(f"Evidence escapes repository: {reference}")
    return resolved


def validate(data: dict, root: Path = ROOT) -> None:
    if data.get("schema_version") != 1:
        raise ValueError("Unsupported roadmap schema_version")
    phase_ids = [p["id"] for p in data["phases"]]
    ids = [t["id"] for p in data["phases"] for t in p["tasks"]]
    if len(set(ids)) != len(ids) or len(set(phase_ids)) != len(phase_ids):
        raise ValueError("Duplicate phase or task ID")
    index = tasks(data)
    for phase in data["phases"]:
        if not phase["tasks"] or not phase["exit_criteria"]:
            raise ValueError(f"{phase['id']}: phase needs tasks and an exit gate")
        if phase["priority"] not in {"P0", "P1", "P2", "P3"}:
            raise ValueError(f"{phase['id']}: invalid priority")
        for path in phase["existing_paths"] + phase["prototype_paths"]:
            if not safe_path(root, path).exists():
                raise ValueError(f"{phase['id']}: missing existing source {path}")
        for task in phase["tasks"]:
            tid = task["id"]
            if task["status"] not in STATUSES or task["size"] not in {1, 2, 3, 5, 8}:
                raise ValueError(f"{tid}: invalid status or size")
            if not task["acceptance"] or not task["verification"] or not task["required_evidence"]:
                raise ValueError(f"{tid}: acceptance, verification and evidence gates are required")
            if not set(task["required_evidence"]) <= EVIDENCE:
                raise ValueError(f"{tid}: unknown required evidence kind")
            prereqs = dependencies(phase, task)
            for dep in prereqs:
                if dep == tid or dep not in index:
                    raise ValueError(f"{tid}: invalid dependency {dep}")
            if task["status"] in {"in_progress", "in_review", "done"}:
                if not task.get("owner", "") or not task["owner"].strip():
                    raise ValueError(f"{tid}: active/completed tasks need an owner")
                pending = [dep for dep in prereqs if index[dep][1]["status"] != "done"]
                if pending:
                    raise ValueError(f"{tid}: incomplete prerequisites: {', '.join(pending)}")
            if task["status"] == "blocked" and not task.get("blocker", "").strip():
                raise ValueError(f"{tid}: blocked tasks need a concrete blocker")
            for proof in task["evidence"]:
                if proof["kind"] not in EVIDENCE or not proof["summary"].strip() or not proof["at"] or not proof["revision"]:
                    raise ValueError(f"{tid}: incomplete evidence metadata")
                if not safe_path(root, proof["reference"]).is_file():
                    raise ValueError(f"{tid}: evidence file missing: {proof['reference']}")
            if task["status"] == "done":
                kinds = {proof["kind"] for proof in task["evidence"]}
                missing = set(task["required_evidence"]) - kinds
                if missing or not task.get("completed_at"):
                    raise ValueError(f"{tid}: done requires completion date and evidence: {sorted(missing)}")
            if task["status"] == "in_review" and "implementation" in task["required_evidence"]:
                if not any(e["kind"] == "implementation" for e in task["evidence"]):
                    raise ValueError(f"{tid}: review requires implementation evidence")
    visiting, visited = set(), set()

    def visit(tid: str) -> None:
        if tid in visiting:
            raise ValueError(f"Dependency cycle involving {tid}")
        if tid in visited:
            return
        visiting.add(tid)
        phase, task = index[tid]
        for dep in dependencies(phase, task):
            visit(dep)
        visiting.remove(tid)
        visited.add(tid)

    for tid in index:
        visit(tid)
    for milestone in data["milestones"]:
        if not milestone["phases"] or not set(milestone["phases"]) <= set(phase_ids):
            raise ValueError(f"Invalid milestone: {milestone['id']}")


def tally(items: list[dict]) -> tuple[int, int, int]:
    done = sum(t["size"] for t in items if t["status"] == "done")
    total = sum(t["size"] for t in items)
    return done, total, round(100 * done / total) if total else 0


def ready_tasks(data: dict) -> list[tuple[dict, dict]]:
    index = tasks(data)
    return [(p, t) for p in data["phases"] for t in p["tasks"]
            if t["status"] == "planned" and all(index[d][1]["status"] == "done"
                                                for d in dependencies(p, t))]


def phase_status(phase: dict, data: dict) -> str:
    states = {t["status"] for t in phase["tasks"]}
    if states == {"done"}:
        return "Done"
    if "blocked" in states:
        return "Blocked"
    if states & {"in_progress", "in_review"} or "done" in states:
        return "In progress"
    if states == {"deferred"}:
        return "Deferred"
    if any(p["id"] == phase["id"] for p, _ in ready_tasks(data)):
        return "Ready"
    return "Waiting for prerequisites"


def cell(value: object) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def render_board(data: dict) -> str:
    phases = data["phases"]
    product = [t for p in phases if p["group"] != "foundation" for t in p["tasks"]]
    done, total, percent = tally(product)
    lines = ["# MotoVault feature progress", "", "<!-- Generated by tools/roadmap.py. Edit roadmap.json through the tracker. -->", "",
             f"Last tracker update: {local_time(data['updated_at'])}. Baseline: `{data['baseline']['commit']}` on `{data['baseline']['branch']}`.", "",
             f"Product delivery: **{percent}%** ({done}/{total} relative scope points). Foundation planning is reported separately.", "",
             "Only `done` tasks count. Partial work, a completed build and elapsed time do not increase delivery progress. Points measure scope, not hours, value or release readiness. Deferred scope stays visible in the denominator.", "",
             "[Implementation plan](IMPLEMENTATION-PLAN.md) | [Design contract](DESIGN-CONTRACT.md) | [Tracking guide](README.md)", "",
             "## Release milestones", "", "| Milestone | Scope | Done tasks | Gate |", "| --- | --- | --- | --- |"]
    for milestone in data["milestones"]:
        included = [t for p in phases if p["id"] in milestone["phases"] for t in p["tasks"]]
        n = sum(t["status"] == "done" for t in included)
        lines.append(f"| {milestone['id']}: {cell(milestone['title'])} | {', '.join(milestone['phases'])} | {n}/{len(included)} | {'Passed' if n == len(included) else 'Open'} |")
    lines += ["", "## Phases", "", "| Phase | Priority | Scope | Status | Done tasks | Scope points |", "| --- | --- | --- | --- | --- | --- |"]
    for p in phases:
        d, total, pct = tally(p["tasks"])
        n = sum(t["status"] == "done" for t in p["tasks"])
        lines.append(f"| [{p['id']}: {cell(p['title'])}](IMPLEMENTATION-PLAN.md#{p['id'].lower()}) | {p['priority']} | {p['group']} | {phase_status(p, data)} | {n}/{len(p['tasks'])} | {d}/{total} ({pct}%) |")
    lines += ["", "## Current work and blockers", ""]
    active = [(p, t) for p in phases for t in p["tasks"] if t["status"] in {"in_progress", "in_review", "blocked"}]
    for _, t in active:
        lines.append(f"- **{t['id']}** {t['title']}: {LABELS[t['status']]}. Owner: {t['owner'] or 'unassigned'}. {t.get('blocker', '')}")
    if not active:
        lines.append("No feature task is currently in progress or blocked.")
    lines += ["", "## Next available tasks", ""]
    for p, t in ready_tasks(data)[:10]:
        lines.append(f"- **{t['id']}** ({p['priority']}, {t['size']} points): {t['title']}")
    if not ready_tasks(data):
        lines.append("No planned task has all prerequisites complete. Review active work, blockers and deferred scope.")
    lines += ["", "## Task board", "", "| ID | Task | Status | Owner | Dependencies | Evidence |", "| --- | --- | --- | --- | --- | --- |"]
    for p in phases:
        for t in p["tasks"]:
            proof = ", ".join(f"[{e['kind']}](../../{e['reference']})" for e in t["evidence"]) or "None"
            lines.append(f"| {t['id']} | {cell(t['title'])} | {LABELS[t['status']]} | {cell(t['owner'] or 'Unassigned')} | {', '.join(dependencies(p, t)) or 'None'} | {proof} |")
    lines += ["", "## Recent updates", ""]
    for event in reversed(data["activity"][-20:]):
        lines.append(f"- {local_time(event['at'])}: **{event['task']}**, {event['from']} -> {event['to']}. {event['note']}")
    return "\n".join(lines) + "\n"


def render_plan(data: dict) -> str:
    lines = ["# MotoVault phase-by-phase implementation plan", "", "<!-- Generated from roadmap.json by tools/roadmap.py. -->", "",
             "This plan implements the crucial and additional Bangladesh ownership features while extending `design/app-ui-v3/`. It authorizes planning; it does not claim those features are implemented.", "",
             "Read [the design contract](DESIGN-CONTRACT.md), [baseline findings](BASELINE.md), [progress](PROGRESS.md) and [tracking instructions](README.md) together.", "",
             "## Priorities and delivery order", "",
             "- **P0:** foundation, trustworthy reminders, document privacy, recoverable local backup.",
             "- **P1:** Bangladesh maintenance, renewal help, cost insights and quick capture needed for the core release.",
             "- **P2:** multi-bike ownership, workshops, resale, widgets and seasonal/breakdown help.",
             "- **P3:** optional professional-rider mode and cloud backup. These do not block the core release.", "",
             "Order follows dependency and risk. Independent tasks with satisfied prerequisites can run concurrently. Finish one vertical slice before widening scope. Size points 1/2/3/5/8 are relative complexity, not person-days; re-estimate after the phase's first design and technical slice.", "",
             "Core beta: P00-P03. Core release: P00-P08. Expanded ownership release: P09-P13 plus P16. Optional P14/P15 each have their own enablement gate.", "",
             "## Cross-cutting implementation rules", "",
             "- Use commonMain/domain for pure rules, storage for Room/repositories and transactional migrations, ui for feature composables, androidMain/iosMain for OS services. Keep skyDesignSystem free of MotoVault models, auth, persistence, location and financial effects.",
             "- Existing paths below are verified starting points. Proposed paths are design targets and can be refined with evidence before editing. Extend a focused feature file instead of expanding Screens.kt indefinitely; avoid unrelated refactors.",
             "- Every data change needs backward-compatible decoding, stable IDs, asset ownership, transaction/rollback behavior and an old-data fixture. Future bike-scoping must be accounted for in each new model.",
             "- Keep local operations usable without an account. Save without optional camera/mic/GPS access; confirm all OCR, voice, paste and mileage estimates before durable writes.",
             "- Record settings, data and side effects through the durable save coordinator. A failed save must not leave a successful toast, changed reminder schedule or deleted owner asset. Undo is durable and reverses associated derived state.",
             "- Use integer poisha for new money contracts, explicit conversion for legacy Double amounts, and one defined rounding policy. Do not infer current pump prices from prototype defaults.",
             "- Design and physical-device evidence are release gates. Build and shared tests alone do not prove camera, notification, key storage, background delivery or battery behavior.",
             "- Each phase produces a sanitized report in evidence/ with build revision, platform/device/OS, steps, expected/actual results and remaining gaps. Never commit owner IDs, documents, receipts, credentials or precise locations.", "",
             "## Phase details", ""]
    for p in data["phases"]:
        lines += [f"<a id=\"{p['id'].lower()}\"></a>", "", f"## {p['id']}: {p['title']}", "",
                  f"**Priority:** {p['priority']} | **Scope:** {p['group']} | **Current status:** {phase_status(p, data)}", "",
                  f"**Outcome:** {p['outcome']}", "", f"**Why now:** {p['rationale']}", "",
                  f"**Entry dependencies:** {', '.join(p['depends_on']) or 'None'}.", "",
                  f"**V3 placement and interaction:** {p['design']}", "", "**Implementation map:**", ""]
        for label, key in [("Prototype references", "prototype_paths"), ("Existing native sources", "existing_paths"), ("Proposed sources", "proposed_paths")]:
            refs = ", ".join(f"`{path}`" for path in p[key]) or "None"
            lines.append(f"- {label}: {refs}.")
        lines += ["", f"**Data and migration:** {p['data']}", "", "**Tasks:**", ""]
        for t in p["tasks"]:
            lines += [f"### {t['id']}: {t['title']}", "", f"Status: **{LABELS[t['status']]}** | Size: **{t['size']}** | Owner: {t['owner'] or 'unassigned'}.", "",
                      f"Additional task dependencies: {', '.join(t['depends_on']) or 'None'}.", "", "Acceptance criteria:", ""]
            lines += [f"- {criterion}" for criterion in t["acceptance"]]
            lines += ["", "Verification:", ""] + [f"- {v}" for v in t["verification"]]
            lines += ["", f"Required completion evidence: {', '.join(t['required_evidence'])}.", ""]
        lines += ["**Phase exit gate:**", ""] + [f"- {v}" for v in p["exit_criteria"]]
        lines += ["", f"**Risks and decisions:** {p['risks']}", ""]
    lines += ["## Release evidence checklist", "",
              "- Shared rule tests, migration/asset tests, Android build and iOS build/test results are separately recorded.",
              "- Prototype review, native screenshots and native motion recordings are separately recorded against the same design contract.",
              "- Physical Android and physical iPhone checks cover the OS-dependent functionality enabled in the release; unavailable hardware keeps its gate open.",
              "- Optional external services use an explicitly selected account/project and staging validation before production writes. A missing public API never silently becomes scraping or an invented verification result.",
              "- Core release is independent of growth features. Features whose gates are open remain disabled and honestly labelled in the production UI.", ""]
    return "\n".join(lines)


def outputs(data: dict) -> dict[Path, str]:
    return {BOARD: render_board(data), PLAN: render_plan(data)}


def atomic_write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, delete=False) as handle:
        temp = Path(handle.name)
        handle.write(content)
    temp.replace(path)


def save(data: dict, root: Path = ROOT) -> None:
    validate(data, root)
    atomic_write(root / DATA, json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    for path, content in outputs(data).items():
        atomic_write(root / path, content)


def change(data: dict, tid: str, status: str | None, note: str, owner: str | None,
           evidence: list[str], root: Path = ROOT) -> dict:
    next_data = copy.deepcopy(data)
    index = tasks(next_data)
    if tid not in index:
        raise ValueError(f"Unknown task: {tid}")
    _, task = index[tid]
    old = task["status"]
    status = status or old
    if status not in STATUSES:
        raise ValueError(f"Unknown status: {status}")
    if not note.strip():
        raise ValueError("Every update needs --note describing the real change")
    stamp = now()
    revision = git_revision(root)
    for value in evidence:
        kind, separator, reference = value.partition(":")
        if not separator or kind not in EVIDENCE:
            raise ValueError("Use --evidence KIND:repository/path; see the tracking guide")
        task["evidence"].append({"kind": kind, "reference": reference, "summary": note,
                                 "at": stamp, "revision": revision})
    task["status"] = status
    if owner is not None:
        task["owner"] = owner
    if status == "in_progress" and not task.get("started_at"):
        task["started_at"] = stamp
    task["completed_at"] = (task.get("completed_at") or stamp) if status == "done" else None
    task["blocker"] = note if status == "blocked" else ""
    task["notes"].append({"at": stamp, "text": note})
    next_data["updated_at"] = stamp
    next_data["activity"].append({"at": stamp, "task": tid, "from": old, "to": status,
                                   "note": note, "revision": revision})
    validate(next_data, root)
    return next_data


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("status", help="Print compact phase progress")
    commands.add_parser("next", help="List planned tasks with complete prerequisites")
    commands.add_parser("render", help="Regenerate progress and detailed plan")
    commands.add_parser("check", help="Validate references, dependencies and generated documents")
    show = commands.add_parser("show", help="Show a task's scope, acceptance and evidence")
    show.add_argument("task")
    update = commands.add_parser("update", help="Update a task and regenerate progress")
    update.add_argument("task")
    update.add_argument("status", choices=sorted(STATUSES))
    update.add_argument("--note", required=True)
    update.add_argument("--owner")
    update.add_argument("--evidence", action="append", default=[])
    note = commands.add_parser("note", help="Record evidence/progress without changing status")
    note.add_argument("task")
    note.add_argument("--note", required=True)
    note.add_argument("--owner")
    note.add_argument("--evidence", action="append", default=[])
    args = parser.parse_args()
    try:
        data = json.loads((ROOT / DATA).read_text(encoding="utf-8"))
        validate(data)
        if args.command in {"update", "note"}:
            data = change(data, args.task, getattr(args, "status", None), args.note,
                          args.owner, args.evidence)
            save(data)
            print(f"Updated {args.task}; regenerated {BOARD} and {PLAN}.")
        elif args.command == "render":
            for path, content in outputs(data).items():
                atomic_write(ROOT / path, content)
            print("Rendered progress and implementation plan.")
        elif args.command == "check":
            stale = [str(path) for path, content in outputs(data).items()
                     if not (ROOT / path).exists() or (ROOT / path).read_text(encoding="utf-8") != content]
            if stale:
                raise ValueError(f"Generated documents are stale: {', '.join(stale)}. Run render.")
            print(f"Roadmap valid: {len(data['phases'])} phases, {len(tasks(data))} tasks; generated documents current.")
        elif args.command == "show":
            if args.task not in tasks(data):
                raise ValueError(f"Unknown task: {args.task}")
            phase, task = tasks(data)[args.task]
            print(json.dumps({"phase": phase["id"], "prerequisites": dependencies(phase, task), **task}, ensure_ascii=False, indent=2))
        elif args.command == "next":
            for phase, task in ready_tasks(data):
                print(f"{task['id']} [{phase['priority']}, {task['size']} points] {task['title']}")
        else:
            for phase in data["phases"]:
                done, total, percent = tally(phase["tasks"])
                print(f"{phase['id']} [{phase['priority']}] {phase_status(phase, data)}: {done}/{total} points ({percent}%) - {phase['title']}")
        return 0
    except (OSError, KeyError, TypeError, ValueError) as error:
        print(f"Roadmap error: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
