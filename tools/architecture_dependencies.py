#!/usr/bin/env python3
"""Inventory forbidden dependencies in the shared domain and application layers.

This is an initial gate component, not the complete P03A-05 quality gate.
"""

from collections import Counter
import json
from pathlib import Path
import re
import sys

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT / "composeApp/src/commonMain/kotlin/com/motovault/app"
BASELINE = PROJECT / "tools/architecture-dependency-baseline.json"
IMPORT = re.compile(r"^\s*import\s+([\w.]+)(?:\.\*)?", re.MULTILINE)
QUALIFIED = re.compile(r"\b(?:androidx\.(?:compose|room)|android|platform|org\.koin|com\.motovault\.app\.(?:ui|storage|application))\.[A-Za-z_][\w.]*")

FORBIDDEN = (
    "androidx.compose", "androidx.room", "android.", "platform.",
    "org.koin", "com.motovault.app.ui", "com.motovault.app.storage",
)


def forbidden_dependency(name, layer):
    prefixes = FORBIDDEN + (("com.motovault.app.application",) if layer == "domain" else ())
    return any(name == prefix.rstrip(".") or name.startswith(prefix.rstrip(".") + ".") for prefix in prefixes)


def strip_noncode(text):
    """Mask comments and literal bodies, preserving code in template expressions."""
    chars = list(text)
    def mask(start, end):
        for position in range(start, min(end, len(text))):
            if chars[position] != "\n":
                chars[position] = " "

    def literal(i, quote):
        raw = text.startswith('"""', i)
        delimiter = '"""' if raw else quote
        mask(i, i + len(delimiter))
        i += len(delimiter)
        while i < len(text):
            if text.startswith(delimiter, i):
                mask(i, i + len(delimiter))
                return i + len(delimiter)
            if not raw and text[i] == "\\":
                mask(i, i + 2)
                i += 2
            elif quote == '"' and text.startswith("${", i):
                mask(i, i + 2)
                i = code(i + 2, template=True)
            else:
                mask(i, i + 1)
                i += 1
        return i

    def code(i, template=False):
        braces = 0
        while i < len(text):
            start = i
            if text.startswith("//", i):
                end = text.find("\n", i)
                i = len(text) if end < 0 else end
                mask(start, i)
            elif text.startswith("/*", i):
                i += 2
                depth = 1
                while i < len(text) and depth:
                    if text.startswith("/*", i):
                        depth += 1
                        i += 2
                    elif text.startswith("*/", i):
                        depth -= 1
                        i += 2
                    else:
                        i += 1
                mask(start, i)
            elif text[i] in ('"', "'"):
                i = literal(i, text[i])
            elif template and text[i] == "{":
                braces += 1
                i += 1
            elif template and text[i] == "}":
                if braces == 0:
                    mask(i, i + 1)
                    return i + 1
                braces -= 1
                i += 1
            else:
                i += 1
        return i

    code(0)
    return "".join(chars)


def inventory(root):
    violations = Counter()
    for layer in ("domain", "application"):
        directory = root / layer
        if not directory.is_dir():
            raise FileNotFoundError(f"Required source directory is missing: {directory}")
        sources = sorted(directory.rglob("*.kt"))
        if not sources:
            raise ValueError(f"Required source directory has no Kotlin files: {directory}")
        for source in sources:
            code = strip_noncode(source.read_text(encoding="utf-8"))
            for name in IMPORT.findall(code):
                if forbidden_dependency(name, layer):
                    violations[(source.relative_to(root).as_posix(), name)] += 1
            code = IMPORT.sub("", code)
            for name in QUALIFIED.findall(code):
                if forbidden_dependency(name, layer):
                    violations[(source.relative_to(root).as_posix(), name)] += 1
    return violations


def compare(actual, baseline):
    """Report both regressions and resolved entries, so debt cannot silently return."""
    regressions = actual - baseline
    resolved = baseline - actual
    return regressions, resolved


def load_baseline(path):
    entries = json.loads(path.read_text(encoding="utf-8"))
    baseline = Counter()
    for item in entries:
        key = (item["path"], item["dependency"])
        if key in baseline or item["count"] < 1 or not item["owner"] or not item["removal_task"]:
            raise ValueError(f"Invalid baseline entry: {key}")
        baseline[key] = item["count"]
    return baseline


if __name__ == "__main__":
    try:
        actual = inventory(SOURCE)
        regressions, resolved = compare(actual, load_baseline(BASELINE))
        for label, entries in (("NEW", regressions), ("RESOLVED: remove baseline entry", resolved)):
            for (path, dependency), count in sorted(entries.items()):
                print(f"{label}: {path}: {dependency} ({count})")
        if regressions or resolved:
            sys.exit(1)
        print(f"Dependency import baseline matches ({sum(actual.values())} existing violations).")
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"Invalid dependency baseline: {error}", file=sys.stderr)
        sys.exit(2)
