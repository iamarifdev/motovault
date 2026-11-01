import tempfile
import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from collections import Counter
from architecture_dependencies import compare, inventory, load_baseline


class DependencyInventoryTest(unittest.TestCase):
    def test_ratchet_rejects_new_and_resolved_violations(self):
        legacy = Counter({("domain/A.kt", "com.motovault.app.ui.Foo"): 1})
        added = Counter({("domain/B.kt", "com.motovault.app.storage.Bar"): 1})
        self.assertEqual(compare(legacy, legacy), (Counter(), Counter()))
        self.assertEqual(compare(legacy + added, legacy), (added, Counter()))
        self.assertEqual(compare(Counter(), legacy), (Counter(), legacy))

    def test_baseline_requires_owner_and_removal_task(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "baseline.json"
            path.write_text('[{"path":"a", "dependency":"b", "count":1, "owner":"", "removal_task":"P03A-04"}]')
            with self.assertRaises(ValueError):
                load_baseline(path)

    def test_allowed_contract_and_forbidden_adapter_imports(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "application").mkdir()
            (root / "domain").mkdir()
            (root / "application" / "Feature.kt").write_text(
                "import com.motovault.app.domain.model.Bike\n"
                "import com.motovault.app.storage.VaultSession\n"
                "import androidx.compose.runtime.Composable\n", encoding="utf-8")
            (root / "domain" / "Rules.kt").write_text(
                "import com.motovault.app.domain.model.Bike\n"
                "import com.motovault.app.application.Feature\n", encoding="utf-8")
            self.assertEqual(set(inventory(root)), {
                ("application/Feature.kt", "com.motovault.app.storage.VaultSession"),
                ("application/Feature.kt", "androidx.compose.runtime.Composable"),
                ("domain/Rules.kt", "com.motovault.app.application.Feature"),
            })

    def test_prefix_does_not_match_unrelated_names(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "domain").mkdir()
            (root / "application").mkdir()
            (root / "application" / "Contract.kt").write_text("", encoding="utf-8")
            (root / "domain" / "Rules.kt").write_text(
                "import androidx.composematerial.Fake\n", encoding="utf-8")
            self.assertFalse(inventory(root))

    def test_fully_qualified_reference_and_noncode_are_distinct(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "domain").mkdir()
            (root / "application").mkdir()
            (root / "application" / "Contract.kt").write_text("", encoding="utf-8")
            (root / "domain" / "Rules.kt").write_text(
                '/* import com.motovault.app.ui.Comment */\n'
                '// com.motovault.app.storage.Comment\n'
                'val s = "com.motovault.app.ui.Text"\n'
                'val r = """com.motovault.app.ui.Raw"""\n'
                'val value = com.motovault.app.ui.VaultState()\n', encoding="utf-8")
            self.assertEqual(inventory(root), Counter({
                ("domain/Rules.kt", "com.motovault.app.ui.VaultState"): 1,
            }))

    def test_template_expressions_are_code_in_regular_and_raw_strings(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "domain").mkdir()
            (root / "application").mkdir()
            (root / "application" / "Contract.kt").write_text("", encoding="utf-8")
            (root / "domain" / "Rules.kt").write_text(
                'val a = "text com.motovault.app.ui.Fake ${com.motovault.app.ui.Real()}"\n'
                'val b = """${ run { /* com.motovault.app.ui.Comment */ com.motovault.app.storage.File() } }"""\n'
                'val c = "${ "nested ${com.motovault.app.ui.Inner()}" }"\n'
                'val d = "\\${com.motovault.app.ui.Escaped}"\n', encoding="utf-8")
            self.assertEqual(inventory(root), Counter({
                ("domain/Rules.kt", "com.motovault.app.ui.Real"): 1,
                ("domain/Rules.kt", "com.motovault.app.storage.File"): 1,
                ("domain/Rules.kt", "com.motovault.app.ui.Inner"): 1,
            }))

    def test_missing_or_empty_required_layer_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "domain").mkdir()
            (root / "domain" / "Rules.kt").write_text("", encoding="utf-8")
            with self.assertRaises(FileNotFoundError):
                inventory(root)
            (root / "application").mkdir()
            with self.assertRaises(ValueError):
                inventory(root)


if __name__ == "__main__":
    unittest.main()
