#!/usr/bin/env python3
"""Check the configured Gradle project-dependency report for cycles."""
from pathlib import Path
import sys

REPORT = Path(__file__).resolve().parents[1] / "build/reports/architecture/module-edges.txt"


def find_cycle(edges):
    graph = {}
    for source, target in edges:
        graph.setdefault(source, set()).add(target)
        graph.setdefault(target, set())
    active = []
    visited = set()

    def visit(node):
        if node in active:
            return active[active.index(node):] + [node]
        if node in visited:
            return None
        active.append(node)
        for neighbor in sorted(graph[node]):
            cycle = visit(neighbor)
            if cycle:
                return cycle
        active.pop()
        visited.add(node)
        return None

    for node in sorted(graph):
        cycle = visit(node)
        if cycle:
            return cycle
    return None


def read_edges(path):
    edges = []
    for line in path.read_text(encoding="utf-8").splitlines():
        parts = line.split("\t")
        if len(parts) != 2 or any(not item.startswith(":") for item in parts):
            raise ValueError(f"Invalid project edge: {line}")
        edges.append(tuple(parts))
    return edges


if __name__ == "__main__":
    try:
        edges = read_edges(REPORT)
        cycle = find_cycle(edges)
        if cycle:
            print("Project dependency cycle: " + " -> ".join(cycle))
            sys.exit(1)
        print(f"No Gradle project dependency cycles ({len(edges)} edges).")
    except (OSError, ValueError) as error:
        print(f"Cannot check project dependencies: {error}", file=sys.stderr)
        sys.exit(2)
