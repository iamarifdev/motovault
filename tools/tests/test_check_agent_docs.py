import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from check_agent_docs import adapter_problems, link_problems, problems_for

POINTER = "Read and follow AGENTS.md.\n"


def make_repo(directory: str, claude: str = POINTER, gemini: str = POINTER) -> Path:
    root = Path(directory)
    (root / "docs/engineering").mkdir(parents=True)
    (root / "AGENTS.md").write_text("# Agents\n", encoding="utf-8")
    (root / "CLAUDE.md").write_text(claude, encoding="utf-8")
    (root / "GEMINI.md").write_text(gemini, encoding="utf-8")
    return root


class CheckAgentDocsTest(unittest.TestCase):
    def test_repository_is_consistent(self):
        self.assertEqual([], problems_for())

    def test_clean_fixture_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual([], problems_for(make_repo(directory)))

    def test_diverging_adapters_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            root = make_repo(directory, gemini=POINTER + "Extra rule.\n")
            self.assertIn(
                "CLAUDE.md and GEMINI.md must be identical", adapter_problems(root)
            )

    def test_adapter_without_pointer_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            root = make_repo(directory, "Do things.\n", "Do things.\n")
            self.assertEqual(2, len(adapter_problems(root)))

    def test_broken_link_fails_but_code_fence_is_ignored(self):
        with tempfile.TemporaryDirectory() as directory:
            root = make_repo(directory)
            (root / "docs/engineering/a.md").write_text(
                "[ok](../../AGENTS.md)\n[bad](missing.md)\n```\n[x](nope.md)\n```\n",
                encoding="utf-8",
            )
            found = link_problems(root)
            self.assertEqual(1, len(found))
            self.assertIn("missing.md", found[0])


if __name__ == "__main__":
    unittest.main()
