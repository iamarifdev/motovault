#!/usr/bin/env python3
"""Compare direct UI literal inventory with committed HEAD without changing files."""
from collections import Counter
from pathlib import Path
import subprocess
import sys

from architecture_dependencies import compare
from ui_literal_gate import PROJECT, ROOT, inventory, inventory_text


def git(*args):
    return subprocess.run(["rtk", "run", "git", *args], cwd=PROJECT, check=True, capture_output=True, text=True).stdout


def main():
    prefix = ROOT.relative_to(PROJECT).as_posix() + "/"
    base = Counter()
    for path in git("ls-tree", "-r", "--name-only", "HEAD", "--", prefix).splitlines():
        if path.endswith(".kt") and path.startswith(prefix):
            base.update(inventory_text(path[len(prefix):], git("show", f"HEAD:{path}")))
    current = inventory(ROOT)
    added, removed = compare(current, base)
    print(f"HEAD: {sum(base.values())}; working tree: {sum(current.values())}; added: {sum(added.values())}; removed: {sum(removed.values())}")
    for label, entries in (("ADDED", added), ("REMOVED", removed)):
        for path in sorted({key[0] for key in entries}):
            print(f"{label}: {path}: {sum(count for (item, _), count in entries.items() if item == path)}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, subprocess.CalledProcessError) as error:
        print(f"Cannot compare UI literals with HEAD: {error}", file=sys.stderr)
        sys.exit(2)
