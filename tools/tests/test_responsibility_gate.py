import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from responsibility_gate import compare, inventory, load_baseline


def baseline(files=None, functions=None):
    return {"files": files or [], "functions": functions or []}


class ResponsibilityGateTest(unittest.TestCase):
    def test_new_large_file_and_function_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "Screen.kt").write_text("fun screen() {\n" + ("    val x = 1\n" * 405) + "}\n", encoding="utf-8")
            files, functions = inventory(root)
            self.assertGreater(files[0]["lines"], 400)
            errors = compare(files, [], baseline())
            self.assertTrue(any("NEW file over 400" in error for error in errors))

    def test_existing_file_must_not_grow(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "Screen.kt").write_text("fun screen() {\n" + ("    val x = 1\n" * 405) + "}\n", encoding="utf-8")
            files, functions = inventory(root)
            errors = compare(files, [], baseline(files=[{"path": "Screen.kt", "lines": 404, "owner": "Codex", "removal_task": "P04A-06", "justification": "test"}]))
            self.assertTrue(any("File grew from 404" in error for error in errors))

    def test_over_600_requires_recorded_exception(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "Screen.kt").write_text("fun screen() {\n" + ("    val x = 1\n" * 605) + "}\n", encoding="utf-8")
            files, functions = inventory(root)
            no_exception = compare(files, [], baseline(files=[{"path": "Screen.kt", "lines": 607, "owner": "Codex", "removal_task": "P04A-06"}]))
            self.assertTrue(any("needs a recorded exception" in error for error in no_exception))
            with_exception = compare(files, [], baseline(files=[{"path": "Screen.kt", "lines": 607, "owner": "Codex", "removal_task": "P04A-06", "justification": "test"}]))
            self.assertEqual([], with_exception)

    def test_function_ratchet_and_invalid_baseline(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            body = "".join(f"    val v{i} = {i}\n" for i in range(90))
            (root / "Rules.kt").write_text("fun rule() {\n" + body + "}\n", encoding="utf-8")
            files, functions = inventory(root)
            self.assertGreater(functions[0]["lines"], 80)
            errors = compare(files, functions, baseline(functions=[{"path": "Rules.kt", "function": "rule", "lines": 91, "owner": "Codex", "removal_task": "P04A-06"}]))
            self.assertTrue(any("Function grew" in error for error in errors))
            bad = root / "baseline.json"
            bad.write_text(json.dumps({"files": [{"path": "x", "lines": 400, "owner": "", "removal_task": "x"}], "functions": []}), encoding="utf-8")
            with self.assertRaises(ValueError):
                load_baseline(bad)

    def test_missing_or_empty_root_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(FileNotFoundError):
                inventory(Path(directory) / "missing")
            empty = Path(directory) / "empty"
            empty.mkdir()
            with self.assertRaises(ValueError):
                inventory(empty)


if __name__ == "__main__":
    unittest.main()
