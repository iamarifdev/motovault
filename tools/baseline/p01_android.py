#!/usr/bin/env python3
"""Synthetic P01 checks. The imported loader hard-restricts writes to the isolated emulator app."""
import json
import time
import p00
import capture_android as capture

OUT = p00.ROOT / 'docs/roadmap/evidence/p01/android'


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    capture.OUT = OUT
    p00.android(capture.SERIAL, 'overdue')
    time.sleep(1)
    capture.tap_label(capture.tree('undo-home'), 'ওডোমিটার')
    capture.tap_label(capture.tree('undo-sheet'), '+১০')
    capture.tap_label(capture.tree('undo-increment'), 'সেভ করুন')
    capture.screenshot('saved-12350')
    capture.tap_label(capture.tree('undo-saved'), 'আনডু')
    tree = capture.tree('undo-restored')
    labels = ' '.join(n.get('text', '') + n.get('content-desc', '') for n in tree.iter('node'))
    assert '১২,৩৪০' in labels and '১২,৩৫০' not in labels, 'Undo did not restore the synthetic odometer'
    capture.screenshot('undo-restored')
    capture.adb('shell', 'am', 'force-stop', p00.APP)
    capture.adb('shell', 'am', 'start', '-n', f'{p00.APP}/com.motovault.app.MainActivity')
    time.sleep(1)
    labels = ' '.join(n.get('text', '') + n.get('content-desc', '') for n in capture.tree('undo-after-restart').iter('node'))
    assert '১২,৩৪০' in labels, 'Restored odometer was not durable'
    p00.write_json(OUT/'undo-result.json', dict(expected=12340, restored=True, survivedRestart=True,
        app=p00.APP, scope='Synthetic Android emulator UI save/Undo/relaunch'))
    print('Android P01 Undo restored 12340 and survived process restart.')


if __name__ == '__main__':
    main()
