import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from architecture_cycles import find_cycle
from architecture_package_cycles import package_edges


class PackageCycleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "domain").mkdir()
        (self.root / "application").mkdir()
        self.write("application/App.kt", "package com.motovault.app.application\n")

    def write(self, path, source):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(source, encoding="utf-8")

    def test_import_alias_wildcard_and_qualified_references_form_cycle(self):
        self.write("domain/a/A.kt", "package com.motovault.app.domain.a\nimport com.motovault.app.domain.b.*\n")
        self.write("domain/b/B.kt", "package com.motovault.app.domain.b\nimport com.motovault.app.domain.c.C as Name\n")
        self.write("domain/c/C.kt", "package com.motovault.app.domain.c\nval value = com.motovault.app.domain.a.A()\n")
        edges, _ = package_edges(self.root)
        self.assertEqual(find_cycle(edges), ["com.motovault.app.domain.a", "com.motovault.app.domain.b", "com.motovault.app.domain.c", "com.motovault.app.domain.a"])

    def test_noncode_and_same_package_do_not_create_cycle(self):
        self.write("domain/a/A.kt", 'package com.motovault.app.domain.a\n// import com.motovault.app.domain.b.B\nval s = "com.motovault.app.domain.b.B"\n')
        self.write("domain/b/B.kt", "package com.motovault.app.domain.b\nimport com.motovault.app.domain.b.Other\nimport com.motovault.app.domain.a.A\n")
        edges, _ = package_edges(self.root)
        self.assertEqual(edges, {("com.motovault.app.domain.b", "com.motovault.app.domain.a")})
        self.assertIsNone(find_cycle(edges))

    def test_template_expression_is_code(self):
        self.write("domain/a/A.kt", 'package com.motovault.app.domain.a\nval s = "${com.motovault.app.domain.b.B()}"\n')
        self.write("domain/b/B.kt", "package com.motovault.app.domain.b\n")
        edges, _ = package_edges(self.root)
        self.assertEqual(edges, {("com.motovault.app.domain.a", "com.motovault.app.domain.b")})

    def test_missing_empty_and_invalid_scope_fail_closed(self):
        with self.assertRaises(ValueError):
            package_edges(self.root)
        self.write("domain/a/A.kt", "package com.motovault.app.ui\n")
        with self.assertRaises(ValueError):
            package_edges(self.root)
        self.write("domain/a/A.kt", "package com.motovault.app.domain.a\n")
        (self.root / "application" / "App.kt").unlink()
        with self.assertRaises(ValueError):
            package_edges(self.root)
        (self.root / "application").rmdir()
        with self.assertRaises(FileNotFoundError):
            package_edges(self.root)


if __name__ == "__main__":
    unittest.main()
