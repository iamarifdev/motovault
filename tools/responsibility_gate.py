#!/usr/bin/env python3
"""Ratchet handwritten Kotlin file and function responsibility.

This is a lexical review trigger, not a Kotlin parser. It catches new files
above the review thresholds and functions whose brace-delimited body exceeds
the threshold. Expression-body functions are not measured.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
DEFAULT_ROOT = PROJECT / "composeApp/src"
DEFAULT_BASELINE = PROJECT / "tools/responsibility-baseline.json"
FILE_REVIEW = 400
FILE_EXCEPTION = 600
FUNCTION_REVIEW = 80
DECL = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:(?:internal|private|public|protected|override|suspend|inline|operator|tailrec|infix)\s+)*"
    r"fun\s+(?:<[^>]+>\s*)?([A-Za-z_]\w*)"
)


def mask_noncode(text: str) -> str:
    chars = list(text)
    i = 0
    n = len(text)
    in_string = in_line = in_block = escaped = False
    while i < n:
        c = text[i]
        if in_line:
            if c == "\n":
                in_line = False
            else:
                chars[i] = " "
        elif in_block:
            if text.startswith("*/", i):
                chars[i] = chars[i + 1] = " "
                in_block = False
                i += 1
            else:
                chars[i] = " "
        elif in_string:
            if escaped:
                escaped = False
            elif c == "\\":
                escaped = True
            elif c == '"':
                in_string = False
            chars[i] = " "
        elif text.startswith("//", i):
            chars[i] = chars[i + 1] = " "
            in_line = True
            i += 1
        elif text.startswith("/*", i):
            chars[i] = chars[i + 1] = " "
            in_block = True
            i += 1
        elif c == '"':
            chars[i] = " "
            in_string = True
        i += 1
    return "".join(chars)


def function_lengths(text: str) -> list[tuple[str, int, int]]:
    masked = mask_noncode(text)
    lines = text.splitlines()
    offsets = [0]
    for line in lines:
        offsets.append(offsets[-1] + len(line) + 1)
    found: list[tuple[str, int, int]] = []
    for index, line in enumerate(lines):
        match = DECL.match(line)
        if not match:
            continue
        start = offsets[index]
        brace = masked.find("{", start, offsets[index + 1] if index + 1 < len(offsets) else len(masked))
        if brace < 0:
            continue
        depth = 0
        end = None
        for pos in range(brace, len(masked)):
            if masked[pos] == "{":
                depth += 1
            elif masked[pos] == "}":
                depth -= 1
                if depth == 0:
                    end = pos
                    break
        if end is not None:
            end_line = masked.count("\n", 0, end) + 1
            found.append((match.group(1), index + 1, end_line - index + 1))
    return found


def source_files(root: Path) -> list[Path]:
    if not root.is_dir():
        raise FileNotFoundError(f"source directory is missing: {root}")
    files = sorted(path for path in root.rglob("*.kt") if path.is_file())
    if not files:
        raise ValueError(f"source directory is empty: {root}")
    return files


def inventory(root: Path) -> tuple[list[dict], list[dict]]:
    files = []
    functions = []
    for path in source_files(root):
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(root).as_posix()
        count = len(text.splitlines())
        if count > FILE_REVIEW:
            files.append({"path": rel, "lines": count})
        for name, line, length in function_lengths(text):
            if length > FUNCTION_REVIEW:
                functions.append({"path": rel, "function": name, "line": line, "lines": length})
    return files, functions


def load_baseline(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    for entry in data.get("files", []):
        if (not entry.get("owner") or not entry.get("removal_task") or
                type(entry.get("lines")) is not int or entry["lines"] <= FILE_REVIEW):
            raise ValueError(f"Invalid file baseline entry: {entry}")
    for entry in data.get("functions", []):
        if (not entry.get("owner") or not entry.get("removal_task") or
                type(entry.get("lines")) is not int or entry["lines"] <= FUNCTION_REVIEW):
            raise ValueError(f"Invalid function baseline entry: {entry}")
    return data


def compare(actual_files, actual_functions, baseline) -> list[str]:
    errors: list[str] = []
    base_files = {entry["path"]: entry["lines"] for entry in baseline.get("files", [])}
    for entry in actual_files:
        path, count = entry["path"], entry["lines"]
        if path not in base_files:
            errors.append(f"NEW file over {FILE_REVIEW} lines: {path} ({count})")
        elif count > base_files[path]:
            errors.append(f"File grew from {base_files[path]} to {count} lines: {path}")
        elif count > FILE_EXCEPTION and not any(
                item["path"] == path and item.get("justification") for item in baseline.get("files", [])):
            errors.append(f"File over {FILE_EXCEPTION} lines needs a recorded exception: {path}")
    base_functions = {(entry["path"], entry["function"]): entry["lines"] for entry in baseline.get("functions", [])}
    for entry in actual_functions:
        key = (entry["path"], entry["function"])
        if key not in base_functions:
            errors.append(f"NEW function over {FUNCTION_REVIEW} lines: {entry['path']} {entry['function']} ({entry['lines']})")
        elif entry["lines"] > base_functions[key]:
            errors.append(f"Function grew from {base_functions[key]} to {entry['lines']} lines: {entry['path']} {entry['function']}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=DEFAULT_ROOT)
    parser.add_argument("--baseline", type=Path, default=DEFAULT_BASELINE)
    parser.add_argument("--inventory", action="store_true", help="print current files/functions as JSON")
    args = parser.parse_args()
    try:
        files, functions = inventory(args.root)
        if args.inventory:
            print(json.dumps({"files": files, "functions": functions}, indent=2))
            return 0
        errors = compare(files, functions, load_baseline(args.baseline))
        for error in errors:
            print(f"RESPONSIBILITY: {error}")
        if errors:
            return 1
        print(f"Responsibility baseline matches ({len(files)} files, {len(functions)} functions).")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Cannot check responsibility baseline: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
