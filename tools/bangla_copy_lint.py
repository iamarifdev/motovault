#!/usr/bin/env python3
"""Reject known literal/robotic Bangla regressions in the shared copy table."""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COPY = ROOT / "composeApp/src/commonMain/composeResources/values-bn/strings.xml"

# Keep this list small and evidence-based. It is a regression guard, not a
# complete style checker; review the rest against localization-style.md.
BANNED = {
    "শিগগির": "use শীঘ্রই",
    "দরকার": "use প্রয়োজন",
    "পেরিয়ে গেছে": "use ছাড়িয়ে গেছে",
    "ফুয়েলর": "use ফুয়েলের",
    "করুন করুন": "remove the duplicate verb",
    "হ্যাপটিক ফিডব্যাক": "use টাচ ভাইব্রেশন",
    "চেহারা": "use অ্যাপিয়ারেন্স",
    "অনুমতি, সময় ও পরে জানান": "use রিমাইন্ডার সেটিংস",
    "মনে করিয়ে দেব": "use রিমাইন্ডার দিতে চান",
    "আমি পাতায়": "use অ্যাপে",
    "শ্রম": "use মজুরি or সার্ভিস চার্জ",
    "ট্যাংক পূর্ণ": "use ফুল ট্যাংক",
    "দস্তাবেজ": "use ডকুমেন্ট",
    "পড়া হচ্ছে": "use স্ক্যান করা হচ্ছে",
    "মেয়াদ উত্তীর্ণ": "use মেয়াদ শেষ",
}

STRING = re.compile(r'<string name="([^"]+)">(.*?)</string>', re.S)


def violations(path: Path = COPY) -> list[tuple[str, str, str]]:
    if not path.is_file():
        raise FileNotFoundError(path)
    text = path.read_text(encoding="utf-8")
    found: list[tuple[str, str, str]] = []
    for match in STRING.finditer(text):
        key, value = match.group(1), match.group(2)
        for phrase, guidance in BANNED.items():
            if phrase in value:
                found.append((key, phrase, guidance))
    return found


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=COPY)
    args = parser.parse_args()
    try:
        found = violations(args.root)
    except (FileNotFoundError, ValueError) as error:
        print(f"Bangla copy check failed: {error}", file=sys.stderr)
        return 1
    if found:
        for key, phrase, guidance in found:
            print(f"ROBOTIC: {key}: {phrase!r} — {guidance}", file=sys.stderr)
        return 1
    print(f"Bangla copy style baseline matches ({len(BANNED)} regression phrases).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
