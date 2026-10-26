#!/usr/bin/env python3
"""Verify numeric sheet bounds and cancellation using only the synthetic emulator app."""
import argparse
import re
import time

import capture_android as ui
import p00
import p01_android_reminders as reminders
import p01_storage


def button(tree, label):
    parents = {child: parent for parent in tree.iter() for child in parent}
    node = next(n for n in tree.iter('node') if label in [n.get('text'), n.get('content-desc')])
    while node.get('clickable') != 'true':
        node = parents[node]
    return node


def bounds(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds'))))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    args = parser.parse_args()
    assert args.run.replace('-', '').isalnum()
    p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
    density = int(ui.adb('shell', 'wm', 'density').split()[-1]) / 160
    width, height = map(int, ui.adb('shell', 'wm', 'size').split()[-1].split('x'))
    for case in ['runtime', 'unknown-large']:
        out = p00.ROOT / 'docs/roadmap/evidence/p01/android' / ('keypad-' + case + '-' + args.run)
        out.mkdir(exist_ok=False)
        ui.OUT = out
        p00.android(ui.SERIAL, case)
        for attempt in range(10):
            loaded = ui.tree('loaded-home-' + str(attempt))
            if 'ওডোমিটার' in reminders.labels(loaded):
                break
            time.sleep(0.5)
        else:
            raise RuntimeError('Synthetic Home did not finish loading before snapshot')
        before = p01_storage.operate('android', 'read')
        p00.write_json(out / 'before.json', before)
        ui.adb('shell', 'am', 'start', '-n', f'{p00.APP}/com.motovault.app.MainActivity')
        tree = ui.tree('home')
        ui.tap_label(tree, 'ওডোমিটার')
        tree = ui.tree('numeric-sheet')
        bn = any(n.get('text') == '১' for n in tree.iter('node'))
        digits = list('১২৩৪৫৬৭৮৯০' if bn else '1234567890') + ['মুছুন']
        measured = {}
        for key in digits:
            rect = bounds(button(tree, key))
            left, top, right, bottom = rect
            assert bottom - top >= 44 * density, (key, rect)
            assert 0 <= left < right <= width and 0 <= top < bottom <= height, (key, rect)
            measured[key] = rect
        ui.tap_label(tree, '+১০' if bn else '+10')
        tree = ui.tree('numeric-draft')
        save = bounds(button(tree, 'সেভ করুন'))
        assert save[3] < measured[digits[0]][1], 'Save overlaps the keypad'
        assert save[3] - save[1] >= 44 * density
        ui.screenshot('numeric-keypad-safe-area')
        ime = ui.adb('shell', 'dumpsys', 'input_method')
        shown = re.search(r'mInputShown=(true|false)', ime)
        assert shown is not None and shown[1] == 'false', 'Expected observable hidden platform keyboard'
        ui.tap_label(tree, 'বন্ধ করুন')
        tree = ui.tree('numeric-cancel-home')
        assert 'ওডোমিটার' in reminders.labels(tree) and 'সেভ করুন' not in reminders.labels(tree)
        ui.screenshot('numeric-cancel-home')
        after = p01_storage.operate('android', 'read')
        p00.write_json(out / 'after.json', after)
        assert before == after, 'Closing the keypad changed durable data'
        p00.write_json(out / 'result.json', dict(result='Passed', applicationId=p00.APP,
            synthetic=True, case=case, screenPixels=[width, height], density=density,
            keyBounds=measured, saveBounds=save, platformKeyboardShown=False,
            scope='Numeric keypad hit bounds and Save placement in portrait. Closing a changed draft preserves persisted data. No spoken screen-reader or physical-device claim.'))
        print(case + ': keypad bounds, hidden IME and cancel preservation passed', flush=True)


if __name__ == '__main__':
    main()
