import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from architecture_cycles import find_cycle, read_edges


class ModuleCycleTest(unittest.TestCase):
    def test_acyclic_diamond(self):
        self.assertIsNone(find_cycle([(':a', ':b'), (':a', ':c'), (':b', ':d'), (':c', ':d')]))

    def test_cycle_and_self_cycle(self):
        self.assertEqual(find_cycle([(':a', ':b'), (':b', ':a')]), [':a', ':b', ':a'])
        self.assertEqual(find_cycle([(':a', ':a')]), [':a', ':a'])

    def test_malformed_report_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / 'edges.txt'
            report.write_text('invalid\n')
            with self.assertRaises(ValueError):
                read_edges(report)


if __name__ == '__main__':
    unittest.main()
