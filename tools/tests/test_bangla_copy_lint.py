import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from bangla_copy_lint import violations


class BanglaCopyLintTest(unittest.TestCase):
    def test_known_regressions_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "strings.xml"
            path.write_text(
                '<resources><string name="a">শিগগির দরকার</string>'
                '<string name="b">কাগজ স্ক্যান করুন করুন</string></resources>',
                encoding="utf-8",
            )
            found = violations(path)
            self.assertEqual(["a", "a", "b"], [item[0] for item in found])

    def test_product_copy_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "strings.xml"
            path.write_text(
                '<resources><string name="a">শীঘ্রই প্রয়োজন</string>'
                '<string name="b">কাগজ স্ক্যান করুন</string>'
                '<string name="c">টাচ ভাইব্রেশন</string></resources>',
                encoding="utf-8",
            )
            self.assertEqual([], violations(path))

    def test_missing_file_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(FileNotFoundError):
                violations(Path(directory) / "missing.xml")

    def test_directory_scan_checks_all_xml_files(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "strings.xml").write_text('<resources><string name="ok">ডকুমেন্ট স্ক্যান করুন</string></resources>', encoding="utf-8")
            (root / "strings-entry.xml").write_text('<resources><string name="bad">ট্যাংক পূর্ণ</string></resources>', encoding="utf-8")
            found = violations(root)
            self.assertEqual([("bad", "ট্যাংক পূর্ণ", "use ফুল ট্যাংক")], found)

    def test_empty_directory_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                violations(Path(directory))


if __name__ == "__main__":
    unittest.main()
