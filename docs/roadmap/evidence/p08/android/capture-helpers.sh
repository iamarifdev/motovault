#!/usr/bin/env bash
# Helpers for the P08-01 Android design-matrix captures.
# Device is fixed to the already-booted Pixel_10 emulator (emulator-5554).
# Usage: source this file, then call the functions. Only adb shell/exec-out is used.

ADB="adb -s emulator-5554"
EVID="docs/roadmap/evidence/p08/android"
UIXML=/tmp/p08-ui.xml

# Launch the sanctioned synthetic host activity (full production four-tab UI,
# VaultState.seedData(), isolated temp DB). Args: language (bn|en) digits (bn|en)
launch_probe() {
  local lang="${1:-bn}" digits="${2:-en}"
  $ADB shell am force-stop com.motovault.app.privacyprobe >/dev/null 2>&1
  sleep 0.5
  $ADB shell am start -n com.motovault.app.privacyprobe/com.motovault.app.FuelEditFixtureActivity \
    --es language "$lang" --es digits "$digits" >/dev/null 2>&1
  sleep 2.5
}

# Save a screenshot into the evidence dir. Args: filename.png
shot() {
  $ADB exec-out screencap -p > "$EVID/$1"
  echo "shot: $EVID/$1"
}

# Dump the current UI hierarchy (uiautomator) to /tmp/p08-ui.xml and echo it.
dump_ui() {
  $ADB shell uiautomator dump /sdcard/p08-ui.xml >/dev/null 2>&1
  $ADB exec-out cat /sdcard/p08-ui.xml > "$UIXML"
  cat "$UIXML"
}

# Tap the centre of the node whose text or content-desc equals $1 (exact match),
# returns non-zero if not found. Prints the tapped point.
tap_exact() {
  dump_ui >/dev/null
  local point
  point=$(python3 - "$1" "$UIXML" <<'PY'
import re, sys
want = sys.argv[1]
data = open(sys.argv[2], encoding='utf-8', errors='replace').read()
for m in re.finditer(r'<node [^>]*>', data):
    tag = m.group(0)
    texts = [re.search(r'text="([^"]*)"', tag), re.search(r'content-desc="([^"]*)"', tag)]
    vals = [t.group(1) for t in texts if t]
    if want in vals:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
)
  if [ -z "$point" ]; then
    echo "tap_exact: NOT FOUND: $1" >&2
    return 1
  fi
  $ADB shell input tap $point
  echo "tap_exact: '$1' at ($point)"
  sleep 1.0
}

# Tap the first node whose text or content-desc *contains* $1.
tap_contains() {
  dump_ui >/dev/null
  local point
  point=$(python3 - "$1" "$UIXML" <<'PY'
import re, sys
want = sys.argv[1]
data = open(sys.argv[2], encoding='utf-8', errors='replace').read()
for m in re.finditer(r'<node [^>]*>', data):
    tag = m.group(0)
    texts = [re.search(r'text="([^"]*)"', tag), re.search(r'content-desc="([^"]*)"', tag)]
    vals = [t.group(1) for t in texts if t]
    if any(want in v for v in vals):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
PY
)
  if [ -z "$point" ]; then
    echo "tap_contains: NOT FOUND: $1" >&2
    return 1
  fi
  $ADB shell input tap $point
  echo "tap_contains: '$1' at ($point)"
  sleep 1.0
}

# Vertical swipe up (scroll content down) at x=540 by $1 px (default 900).
swipe_up() {
  local dist="${1:-900}" dur="${2:-400}"
  $ADB shell input swipe 540 $((1800)) 540 $((1800 - dist)) "$dur"
  sleep 0.8
}

# Vertical swipe down (scroll content up).
swipe_down() {
  local dist="${1:-900}" dur="${2:-400}"
  $ADB shell input swipe 540 $((500)) 540 $((500 + dist)) "$dur"
  sleep 0.8
}

# Horizontal swipe on the swatch row: negative DX scrolls the row left (reveals
# right-hand swatches), positive DX reveals left-hand swatches.
swipe_swatches() {
  local dx="${1:--700}" y="${2:-1150}" dur="${3:-350}"
  $ADB shell input swipe 540 "$y" $((540 + dx)) "$y" "$dur"
  sleep 0.8
}

# Return the current foreground activity.
foreground() {
  $ADB shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -2
}

# Press back (dismisses sheets/screens).
back() {
  $ADB shell input keyevent KEYCODE_BACK
  sleep 1.0
}

# Print all visible text/content-desc nodes with bounds, for inspection.
visible_nodes() {
  dump_ui >/dev/null
  python3 - "$UIXML" <<'PY'
import re, sys
data = open(sys.argv[1], encoding='utf-8', errors='replace').read()
for m in re.finditer(r'<node [^>]*>', data):
    tag = m.group(0)
    t = re.search(r'text="([^"]*)"', tag)
    d = re.search(r'content-desc="([^"]*)"', tag)
    b = re.search(r'bounds="(\[[^\"]*\])"', tag)
    label = (t.group(1) if t and t.group(1) else '') or ('[cd] ' + d.group(1) if d and d.group(1) else '')
    if label:
        print((b.group(1) if b else ''), label)
PY
}
