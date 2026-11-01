import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from fuel_policy_literals import violations


class FuelPolicyLiteralTest(unittest.TestCase):
    def test_named_values_and_basic_math_are_allowed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "Rules.kt").write_text(
                'const val MAX_LITRES = 100.0\nprivate const val DELAY: Long = 20L\n'
                'val basic = x > 0 && count == 1\nval previous = count - 1\n// 900\nval text = "120"\n', encoding="utf-8")
            self.assertEqual(violations(root), [])

    def test_unnamed_values_and_template_expressions_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "Rules.kt").write_text(
                'val max = 100.0\nval sentinel = -1\nval rate = 1e2\n'
                'val text = "${2 * value}"\nval mask = 0x1F\nval range = 0..120\n', encoding="utf-8")
            self.assertEqual([item[2] for item in violations(root)], ["100.0", "1", "1e2", "2", "0x1F", "120"])

    def test_missing_or_empty_scope_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaises(ValueError):
                violations(root)
            with self.assertRaises(FileNotFoundError):
                violations(root / "missing")


if __name__ == "__main__":
    unittest.main()
