"""Regression checks for progress integrity; no app or third-party runtime required."""

import copy
import importlib.util
import json
from pathlib import Path
import shlex
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch

MODULE = Path(__file__).resolve().parents[1] / "roadmap.py"
spec = importlib.util.spec_from_file_location("roadmap", MODULE)
roadmap = importlib.util.module_from_spec(spec)
spec.loader.exec_module(roadmap)


def task(tid, deps=(), required=("implementation", "verification")):
    return {"id": tid, "title": tid, "size": 2, "depends_on": list(deps),
            "acceptance": ["Observable result"], "verification": ["Reproduce the outcome"],
            "required_evidence": list(required), "status": "planned", "owner": "Tester",
            "started_at": None, "completed_at": None, "blocker": "", "notes": [], "evidence": []}


def phase(pid, work, deps=(), group="core"):
    return {"id": pid, "title": pid, "priority": "P0", "group": group,
            "outcome": "Outcome", "rationale": "Reason", "depends_on": list(deps),
            "design": "V3 sheet", "prototype_paths": ["reference.txt"],
            "existing_paths": ["source.txt"], "proposed_paths": [], "data": "Compatible",
            "exit_criteria": ["Verified"], "risks": "Documented", "tasks": work}


class RoadmapIntegrityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for name in ["reference.txt", "source.txt", "proof.md"]:
            (self.root / name).write_text("Synthetic evidence\n")
        self.data = {"schema_version": 1, "updated_at": "2026-10-24T00:00:00+00:00",
                     "baseline": {"commit": "test", "branch": "test"}, "activity": [],
                     "milestones": [{"id": "M0", "title": "Gate", "phases": ["P00", "P01"]}],
                     "phases": [phase("P00", [task("P00-01")], group="foundation"),
                                phase("P01", [task("P01-01", required=("implementation", "android_device", "ios_device"))], ["P00-01"])]}
        mock = patch.object(roadmap, "git_revision", return_value="test-revision")
        mock.start()
        self.addCleanup(mock.stop)

    def complete_foundation(self):
        self.data = roadmap.change(self.data, "P00-01", "done", "Observed outcome", None,
                                   ["implementation:proof.md", "verification:proof.md"], self.root)

    def test_only_ready_tasks_are_returned(self):
        self.assertEqual([t["id"] for _, t in roadmap.ready_tasks(self.data)], ["P00-01"])
        self.complete_foundation()
        self.assertEqual([t["id"] for _, t in roadmap.ready_tasks(self.data)], ["P01-01"])

    def test_incomplete_dependency_prevents_start_without_mutating_input(self):
        original = copy.deepcopy(self.data)
        with self.assertRaisesRegex(ValueError, "incomplete prerequisites"):
            roadmap.change(self.data, "P01-01", "in_progress", "Starting", None, [], self.root)
        self.assertEqual(self.data, original)

    def test_missing_evidence_prevents_completion(self):
        with self.assertRaisesRegex(ValueError, "done requires"):
            roadmap.change(self.data, "P00-01", "done", "Only implementation", None,
                           ["implementation:proof.md"], self.root)

    def test_build_evidence_cannot_replace_device_evidence(self):
        self.complete_foundation()
        with self.assertRaisesRegex(ValueError, "ios_device"):
            roadmap.change(self.data, "P01-01", "done", "Built only", None,
                           ["implementation:proof.md", "verification:proof.md", "android_device:proof.md"], self.root)

    def test_missing_evidence_file_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "evidence file missing"):
            roadmap.change(self.data, "P00-01", "planned", "Missing report", None,
                           ["verification:absent.md"], self.root)

    def test_evidence_cannot_escape_repository(self):
        for reference in ["../proof.md", str(self.root / "proof.md")]:
            with self.subTest(reference=reference), self.assertRaisesRegex(ValueError, "repository-relative"):
                roadmap.change(self.data, "P00-01", None, "Invalid reference", None,
                               [f"verification:{reference}"], self.root)

    def test_evidence_symlink_cannot_escape_repository(self):
        with tempfile.TemporaryDirectory() as outside:
            target = Path(outside) / "outside.md"
            target.write_text("Outside")
            (self.root / "link.md").symlink_to(target)
            with self.assertRaisesRegex(ValueError, "escapes repository"):
                roadmap.change(self.data, "P00-01", None, "Symlink", None,
                               ["verification:link.md"], self.root)

    def test_cycle_and_duplicate_ids_are_rejected(self):
        self.data["phases"][0]["tasks"][0]["depends_on"] = ["P01-01"]
        with self.assertRaisesRegex(ValueError, "cycle"):
            roadmap.validate(self.data, self.root)
        self.data["phases"][0]["tasks"][0]["depends_on"] = []
        self.data["phases"][1]["tasks"][0]["id"] = "P00-01"
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            roadmap.validate(self.data, self.root)

    def test_blocker_requires_reason_and_all_updates_require_note(self):
        self.data["phases"][0]["tasks"][0]["status"] = "blocked"
        with self.assertRaisesRegex(ValueError, "concrete blocker"):
            roadmap.validate(self.data, self.root)
        with self.assertRaisesRegex(ValueError, "needs --note"):
            roadmap.change(self.data, "P00-01", "blocked", " ", None, [], self.root)

    def test_active_work_requires_owner(self):
        with self.assertRaisesRegex(ValueError, "need an owner"):
            roadmap.change(self.data, "P00-01", "in_progress", "Starting", "", [], self.root)

    def test_reopening_prerequisite_of_active_task_is_rejected(self):
        self.complete_foundation()
        self.data = roadmap.change(self.data, "P01-01", "in_progress", "Working", None, [], self.root)
        with self.assertRaisesRegex(ValueError, "incomplete prerequisites"):
            roadmap.change(self.data, "P00-01", "planned", "Reopen", None, [], self.root)

    def test_partial_work_and_deferral_do_not_count_as_done(self):
        self.data = roadmap.change(self.data, "P00-01", "in_progress", "Working", None, [], self.root)
        self.assertEqual(roadmap.tally([self.data["phases"][0]["tasks"][0]]), (0, 2, 0))
        self.data = roadmap.change(self.data, "P00-01", "deferred", "Postponed", None, [], self.root)
        self.assertEqual(roadmap.tally([self.data["phases"][0]["tasks"][0]]), (0, 2, 0))

    def test_foundation_does_not_inflate_product_progress(self):
        self.complete_foundation()
        self.assertIn("Product delivery: **0%**", roadmap.render_board(self.data))

    def test_note_preserves_status_completion_date_and_adds_audit_history(self):
        self.complete_foundation()
        completed_at = self.data["phases"][0]["tasks"][0]["completed_at"]
        with patch.object(roadmap, "now", return_value="2026-10-25T00:00:00+00:00"):
            updated = roadmap.change(self.data, "P00-01", None, "Reviewed report", None,
                                     ["review:proof.md"], self.root)
        t = updated["phases"][0]["tasks"][0]
        self.assertEqual(t["status"], "done")
        self.assertEqual(t["completed_at"], completed_at)
        self.assertEqual(updated["activity"][-1]["note"], "Reviewed report")
        self.assertEqual(updated["activity"][-1]["revision"], "test-revision")

    def test_generated_documents_are_deterministic_and_persisted(self):
        self.complete_foundation()
        first = roadmap.outputs(self.data)
        self.assertEqual(first, roadmap.outputs(self.data))
        roadmap.save(self.data, self.root)
        loaded = json.loads((self.root / roadmap.DATA).read_text())
        self.assertEqual(loaded, self.data)
        for path, content in first.items():
            self.assertEqual((self.root / path).read_text(), content)

    def cli(self, *args):
        (self.root / "tools").mkdir(exist_ok=True)
        shutil.copyfile(MODULE, self.root / "tools/roadmap.py")
        if not (self.root / roadmap.DATA).exists():
            roadmap.save(self.data, self.root)
        command = shlex.join(["python3", "tools/roadmap.py", *args])
        return subprocess.run(["rtk", "run", "-c", command], cwd=self.root,
                              capture_output=True, text=True)

    def test_cli_updates_and_notes_regenerate_views_with_quoted_text(self):
        started = self.cli("update", "P00-01", "in_progress", "--owner", "Codex",
                           "--note", "Started capture; iOS pending")
        self.assertEqual(started.returncode, 0, started.stderr)
        noted = self.cli("note", "P00-01", "--note", "বাংলা report ready",
                         "--evidence", "implementation:proof.md")
        self.assertEqual(noted.returncode, 0, noted.stderr)
        reviewed = self.cli("update", "P00-01", "in_review", "--note", "Reviewing report")
        self.assertEqual(reviewed.returncode, 0, reviewed.stderr)
        completed = self.cli("update", "P00-01", "done", "--note", "Verified outcome",
                             "--evidence", "verification:proof.md")
        self.assertEqual(completed.returncode, 0, completed.stderr)
        data = json.loads((self.root / roadmap.DATA).read_text())
        t = data["phases"][0]["tasks"][0]
        self.assertEqual(t["status"], "done")
        self.assertEqual(t["notes"][0]["text"], "Started capture; iOS pending")
        self.assertEqual(t["notes"][1]["text"], "বাংলা report ready")
        checked = self.cli("check")
        self.assertEqual(checked.returncode, 0, checked.stderr)
        self.assertIn("P01-01", self.cli("next").stdout)

    def test_cli_failed_completion_leaves_tracker_unchanged(self):
        roadmap.save(self.data, self.root)
        before = (self.root / roadmap.DATA).read_bytes()
        result = self.cli("update", "P00-01", "done", "--note", "No evidence")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("done requires", result.stderr)
        self.assertEqual(before, (self.root / roadmap.DATA).read_bytes())

    def test_cli_check_detects_stale_generated_views(self):
        roadmap.save(self.data, self.root)
        (self.root / roadmap.BOARD).write_text("Stale progress\n")
        failed = self.cli("check")
        self.assertNotEqual(failed.returncode, 0)
        self.assertIn("stale", failed.stderr)
        self.assertEqual(self.cli("render").returncode, 0)
        self.assertEqual(self.cli("check").returncode, 0)


if __name__ == "__main__":
    unittest.main()
