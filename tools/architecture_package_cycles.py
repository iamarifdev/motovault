#!/usr/bin/env python3
"""Check package dependencies within the migrated shared domain/application scope.

Imports and fully qualified references are inspected lexically, not semantically.
The check does not establish package dependencies outside this scope.
"""
from pathlib import Path
import re
import sys

from architecture_cycles import find_cycle
from architecture_dependencies import SOURCE, strip_noncode

PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*$", re.MULTILINE)
IMPORT = re.compile(r"^\s*import\s+([\w.]+)(?:\.\*)?(?:\s+as\s+\w+)?\s*$", re.MULTILINE)
REFERENCE = re.compile(r"\bcom\.motovault\.app\.(?:domain|application)(?:\.[A-Za-z_]\w*)+")


def package_edges(root: Path):
    files = []
    for layer in ("domain", "application"):
        directory = root / layer
        if not directory.is_dir():
            raise FileNotFoundError(f"Required source directory is missing: {directory}")
        sources = sorted(directory.rglob("*.kt"))
        if not sources:
            raise ValueError(f"Required source directory has no Kotlin files: {directory}")
        for path in sources:
            code = strip_noncode(path.read_text(encoding="utf-8"))
            declarations = PACKAGE.findall(code)
            if len(declarations) != 1:
                raise ValueError(f"Expected one package declaration: {path}")
            package = declarations[0]
            expected = f"com.motovault.app.{layer}"
            if package != expected and not package.startswith(expected + "."):
                raise ValueError(f"Unexpected package {package}: {path}")
            files.append((package, code))

    packages = {package for package, _ in files}
    ordered = sorted(packages, key=lambda package: (-len(package), package))
    edges = set()
    for source, code in files:
        imports = IMPORT.findall(code)
        body = PACKAGE.sub("", IMPORT.sub("", code))
        for reference in imports + REFERENCE.findall(body):
            target = next((p for p in ordered if reference == p or reference.startswith(p + ".")), None)
            if target and target != source:
                edges.add((source, target))
    return edges, packages


def main():
    try:
        edges, packages = package_edges(SOURCE)
        cycle = find_cycle(edges)
        if cycle:
            print("Shared package dependency cycle: " + " -> ".join(cycle))
            return 1
        print(f"No shared domain/application package cycles ({len(packages)} packages, {len(edges)} edges).")
        return 0
    except (OSError, ValueError) as error:
        print(f"Cannot check shared package dependencies: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
