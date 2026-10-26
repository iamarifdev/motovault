#!/usr/bin/env python3
"""Fingerprint the P02 source delta and supplied build artifacts without owner data."""
import argparse
import hashlib
import json
from datetime import datetime, timezone
from pathlib import Path


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--platform", choices=("android", "ios", "prototype"), required=True)
    parser.add_argument("--base", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--artifact", action="append", type=Path, default=[])
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    sources = set((root / "composeApp/src/commonMain/kotlin/com/motovault/app/storage/security").glob("*.kt"))
    sources.add(root / "composeApp/src/commonMain/kotlin/com/motovault/app/ui/DocumentCapture.kt")
    sources.add(root / "composeApp/src/commonMain/kotlin/com/motovault/app/storage/VaultDatabase.kt")
    sources.update(root / f"composeApp/src/commonMain/kotlin/com/motovault/app/ui/{name}.kt" for name in ("VaultState", "VaultSettings"))
    sources.update((root / "composeApp/schemas/com.motovault.app.storage.VaultDatabase").glob("*.json"))
    sources.add(root / "composeApp/build.gradle.kts")
    targets = ["androidMain", "androidInstrumentedTest", "androidUnitTest"] if args.platform == "android" else ["iosMain"]
    for target in targets:
        sources.update((root / f"composeApp/src/{target}/kotlin/com/motovault/app/storage/security").glob("*.kt"))
    sources.update(root / f"composeApp/src/{targets[0]}/kotlin/com/motovault/app/ui/{name}.kt" for name in ("DocumentCapture", "DocumentCamera", "VaultStorage"))
    if args.platform == "ios":
        sources.update((root / "iosApp/MotoVault/AppDelegate.swift", root / "iosApp/MotoVault/MotoVault.entitlements", root / "iosApp/MotoVault.xcodeproj/project.pbxproj"))
        sources.add(root / "composeApp/src/iosTest/kotlin/com/motovault/app/VaultDatabaseTest.kt")
    else:
        sources.update((root / "composeApp/src/androidMain/kotlin/com/motovault/app/MainActivity.kt", root / "composeApp/src/androidMain/res/xml/document_paths.xml"))
    if args.platform == "prototype":
        prototype = root / "design/app-ui-v3"
        sources = {path for path in (prototype / "src").rglob("*") if path.is_file() and path.suffix in (".ts", ".tsx", ".css")}
        sources.update((prototype / "package.json", prototype / "tokens/sky-tokens.json"))
    artifacts = []
    for path in args.artifact:
        artifacts.extend(sorted(child for child in path.rglob("*") if child.is_file()) if path.is_dir() else [path])
    data = {
        "baseRevision": args.base,
        "uncommittedDelta": True,
        "platform": args.platform,
        "capturedAtUtc": datetime.now(timezone.utc).isoformat(),
        "sources": {str(path.relative_to(root)): digest(path) for path in sorted(sources)},
        "artifacts": [{"name": path.name, "bytes": path.stat().st_size, "sha256": digest(path)} for path in artifacts],
    }
    args.output.write_text(json.dumps(data, indent=2) + "\n")
    print(f"Recorded {len(sources)} source hashes and {len(artifacts)} artifacts in {args.output}")


if __name__ == "__main__":
    main()
