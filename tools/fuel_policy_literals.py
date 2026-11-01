#!/usr/bin/env python3
"""Reject nontrivial unnamed numeric literals in the shared fuel domain pilot.

This lexical gate covers only domain/fuel. Zero and one are allowed as basic
mathematical values; literals on a const val declaration are named policies.
"""
from pathlib import Path
import re
import sys

from architecture_dependencies import SOURCE, strip_noncode

ROOT = SOURCE / "domain/fuel"
NUMBER = re.compile(r"(?<!\w)(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|(?:\d[\d_]*)(?:\.\d[\d_]*)?(?:[eE][+-]?\d[\d_]*)?)(?:[uU][lL]?|[lLfF])?(?!\w|\.(?!\.))")
NAMED = re.compile(r"\bconst\s+val\s+[A-Za-z_]\w*(?:\s*:\s*[\w.<>?]+)?\s*=\s*[+-]?\s*$")


def is_trivial(token):
    token = token.replace("_", "")
    suffix = r"(?:[uU][lL]?|[lL])$" if token.lower().startswith(("0x", "0b")) else r"(?:[uU][lL]?|[lLfF])$"
    token = re.sub(suffix, "", token)
    try:
        return float(token) in (0.0, 1.0) if not token.lower().startswith(("0x", "0b")) else int(token, 0) in (0, 1)
    except ValueError:
        return False


def violations(root: Path):
    if not root.is_dir():
        raise FileNotFoundError(f"Fuel policy directory is missing: {root}")
    sources = sorted(root.rglob("*.kt"))
    if not sources:
        raise ValueError(f"Fuel policy directory has no Kotlin files: {root}")
    found = []
    for path in sources:
        code = strip_noncode(path.read_text(encoding="utf-8"))
        for match in NUMBER.finditer(code):
            start = code.rfind("\n", 0, match.start()) + 1
            prefix = code[start:match.start()]
            if NAMED.search(prefix):
                continue
            # Negative one can be a business sentinel and must be named.
            unary_minus = re.search(r"(?:^|[=(,:{}\[])\s*-\s*$", prefix)
            if is_trivial(match.group()) and not unary_minus:
                continue
            line = code.count("\n", 0, match.start()) + 1
            found.append((path.relative_to(root).as_posix(), line, match.group()))
    return found


def main():
    try:
        found = violations(ROOT)
        for path, line, token in found:
            print(f"{path}:{line}: name this fuel policy value ({token})")
        if found:
            return 1
        print("Fuel domain numeric policy check passed.")
        return 0
    except (OSError, ValueError) as error:
        print(f"Cannot check fuel policy literals: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
