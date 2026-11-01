#!/usr/bin/env python3
"""Ratchet direct Compose Text and accessibility string literals in shared UI.

This deliberately checks a small set of sinks; it is not Kotlin semantic analysis.
"""
from collections import Counter
from hashlib import sha256
import json
from pathlib import Path
import re
import sys

from architecture_dependencies import compare, strip_noncode

PROJECT = Path(__file__).resolve().parents[1]
ROOT = PROJECT / "composeApp/src/commonMain/kotlin/com/motovault/app/ui"
BASELINE = PROJECT / "tools/ui-literal-baseline.json"
PILOT_ROOT = ROOT / "fuel"
PILOT_BASELINE = PROJECT / "tools/ui-fuel-literal-baseline.json"
SINK = re.compile(
    r"\b(?:"
    r"Text\s*\((?:(?:\s*)text\s*=)?"
    r"|contentDescription\s*="
    r"|Field\s*\((?:(?:\s*)label\s*=)?"
    r"|VaultButton\s*\((?:(?:\s*)text\s*=)?"
    r"|Primary\s*\((?:(?:\s*)text\s*=)?"
    r"|Eyebrow\s*\("
    r"|Heading\s*\("
    r"|VaultChip\s*\((?:(?:\s*)text\s*=)?"
    r"|Sheet\s*\((?:(?:\s*)title\s*=)?"
    r"|DocumentNote\s*\("
    r"|DocumentCopy\s*\("
    r"|DocumentHeading\s*\("
    r")"
)


def first_literal(text, index):
    """Return the literal token immediately following a sink, ignoring comments."""
    while index < len(text):
        if text[index].isspace():
            index += 1
        elif text.startswith("//", index):
            end = text.find("\n", index)
            index = len(text) if end < 0 else end
        elif text.startswith("/*", index):
            index += 2
            depth = 1
            while index < len(text) and depth:
                if text.startswith("/*", index):
                    depth += 1
                    index += 2
                elif text.startswith("*/", index):
                    depth -= 1
                    index += 2
                else:
                    index += 1
        else:
            break
    if not text.startswith('"', index):
        return None
    start = index
    if text.startswith('"""', index):
        end = text.find('"""', index + 3)
        return text[start:end + 3] if end >= 0 else None
    index += 1
    while index < len(text):
        if text[index] == "\\":
            index += 2
        elif text[index] == '"':
            return text[start:index + 1]
        else:
            index += 1
    return None


def inventory(root):
    if not root.is_dir():
        raise FileNotFoundError(f"UI source directory is missing: {root}")
    result = Counter()
    for path in sorted(root.rglob("*.kt")):
        text = path.read_text(encoding="utf-8")
        result.update(inventory_text(path.relative_to(root).as_posix(), text))
    return result


def inventory_text(path, text):
    result = Counter()
    code = strip_noncode(text)
    for match in SINK.finditer(code):
        token = first_literal(text, match.end())
        if token is not None:
            digest = sha256(token.encode("utf-8")).hexdigest()
            result[(path, digest)] += 1
    return result


def load_baseline(path):
    result = Counter()
    for item in json.loads(path.read_text(encoding="utf-8")):
        key = item["path"], item["sha256"]
        if key in result or not re.fullmatch(r"[0-9a-f]{64}", key[1]) or type(item["count"]) is not int or item["count"] < 1 or not item["owner"] or not item["removal_task"]:
            raise ValueError(f"Invalid baseline entry: {key}")
        result[key] = item["count"]
    return result


def main():
    try:
        pilot = sys.argv[1:] == ["--fuel"]
        if sys.argv[1:] not in ([], ["--fuel"], ["--inventory"]):
            raise ValueError("Expected --fuel, --inventory or no arguments")
        actual = inventory(PILOT_ROOT if pilot else ROOT)
        if sys.argv[1:] == ["--inventory"]:
            for (path, digest), count in sorted(actual.items()):
                print(f"{path}\t{digest}\t{count}")
            return 0
        new, resolved = compare(actual, load_baseline(PILOT_BASELINE if pilot else BASELINE))
        for label, entries in (("NEW", new), ("RESOLVED: remove baseline entry", resolved)):
            for (path, digest), count in sorted(entries.items()):
                print(f"{label}: {path}: literal sha256={digest} ({count})")
        if new or resolved:
            return 1
        print(f"Direct UI literal baseline matches ({sum(actual.values())} existing occurrences).")
        return 0
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"Cannot check UI literal baseline: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
