import json
import tempfile
import unittest
from pathlib import Path
import sys
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from architecture_dependencies import compare
from ui_literal_gate import inventory, load_baseline
import ui_literal_gate


class UiLiteralGateTest(unittest.TestCase):
    def test_enforced_fuel_scope_accepts_resources_and_rejects_new_literals(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "fuel"
            root.mkdir()
            baseline = Path(directory) / "baseline.json"
            baseline.write_text("[]", encoding="utf-8")
            screen = root / "Fuel.kt"
            screen.write_text('Text(stringResource(Res.string.fuel))', encoding="utf-8")
            with patch.object(ui_literal_gate, "PILOT_ROOT", root), patch.object(ui_literal_gate, "PILOT_BASELINE", baseline), patch.object(sys, "argv", ["gate", "--fuel"]):
                self.assertEqual(ui_literal_gate.main(), 0)
                screen.write_text('Text("New label")', encoding="utf-8")
                self.assertEqual(ui_literal_gate.main(), 1)
            with self.assertRaises(FileNotFoundError):
                inventory(root / "missing")

    def test_direct_sinks_and_noncode(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            file = root / "Screen.kt"
            file.write_text(
                'Text("Hello")\nText(text = /* context */ "World")\n'
                'contentDescription = "Open"\nText("""Raw""")\n'
                'Text(stringResource(Res.string.hello))\n'
                '// Text("comment")\nval x = "Text(\\"text\\")"\n', encoding="utf-8")
            baseline = inventory(root)
            self.assertEqual(sum(baseline.values()), 4)
            self.assertEqual(compare(baseline, baseline), ({}, {}))
            file.write_text(file.read_text() + 'Text("New")\n', encoding="utf-8")
            added, resolved = compare(inventory(root), baseline)
            self.assertEqual(sum(added.values()), 1)
            self.assertFalse(resolved)
            file.write_text('Text(stringResource(Res.string.hello))\n', encoding="utf-8")
            self.assertEqual(sum(compare(inventory(root), baseline)[1].values()), 4)

    def test_baseline_requires_valid_hash_owner_and_removal_task(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "baseline.json"
            entry = {"path": "Screen.kt", "sha256": "a" * 64, "count": 1, "owner": "team", "removal_task": "P03A-04"}
            path.write_text(json.dumps([entry]), encoding="utf-8")
            self.assertEqual(sum(load_baseline(path).values()), 1)
            entry["owner"] = ""
            path.write_text(json.dumps([entry]), encoding="utf-8")
            with self.assertRaises(ValueError):
                load_baseline(path)


if __name__ == "__main__":
    unittest.main()
