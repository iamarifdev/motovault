#!/usr/bin/env python3
"""Check that the agent entrypoint docs stay consistent.

- CLAUDE.md and GEMINI.md must be identical thin pointers to AGENTS.md.
- Relative markdown links in the entrypoint and engineering docs must resolve.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADAPTERS = ("CLAUDE.md", "GEMINI.md")
MAX_ADAPTER_LINES = 12
LINK = re.compile(r"\[[^\]]*\]\(([^)\s]+)\)")
FENCE = re.compile(r"^```")


def doc_files(root: Path) -> list[Path]:
    files = [root / "AGENTS.md", *(root / name for name in ADAPTERS)]
    files += sorted((root / "docs/engineering").glob("*.md"))
    files += sorted((root / "docs/engineering/templates").glob("*.md"))
    files.append(root / "docs/roadmap/ARCHITECTURE-CONTRACT.md")
    return [path for path in files if path.exists()]


def adapter_problems(root: Path) -> list[str]:
    problems: list[str] = []
    texts = {}
    for name in ADAPTERS:
        path = root / name
        if not path.exists():
            problems.append(f"{name}: missing")
            continue
        text = path.read_text(encoding="utf-8")
        texts[name] = text
        if "AGENTS.md" not in text:
            problems.append(f"{name}: must point to AGENTS.md")
        if len(text.splitlines()) > MAX_ADAPTER_LINES:
            problems.append(f"{name}: adapter must stay under {MAX_ADAPTER_LINES} lines")
    if len(set(texts.values())) > 1:
        problems.append("CLAUDE.md and GEMINI.md must be identical")
    return problems


def link_problems(root: Path) -> list[str]:
    problems: list[str] = []
    for path in doc_files(root):
        in_fence = False
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if FENCE.match(line.strip()):
                in_fence = not in_fence
            if in_fence:
                continue
            for target in LINK.findall(line):
                if re.match(r"^[a-z][a-z0-9+.-]*:", target) or target.startswith("#"):
                    continue
                file_part = target.split("#", 1)[0]
                if file_part and not (path.parent / file_part).exists():
                    problems.append(f"{path.relative_to(root)}:{number}: broken link {target}")
    return problems


def problems_for(root: Path = ROOT) -> list[str]:
    return adapter_problems(root) + link_problems(root)


def main() -> int:
    problems = problems_for()
    for problem in problems:
        print(problem, file=sys.stderr)
    if problems:
        return 1
    print("agent docs consistent")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
