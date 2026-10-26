#!/usr/bin/env python3
"""Record live OS motion changes in the isolated synthetic emulator app."""
import argparse
import shlex
import subprocess
import time

import capture_android as ui
import p00


def capture(name, size=None):
    for attempt in range(10):
        tree = ui.tree(name + '-home')
        if any('ওডোমিটার' in (n.get('text', '') + n.get('content-desc', '')) for n in tree.iter('node')):
            break
        time.sleep(0.5)
    else:
        raise RuntimeError('The isolated Home screen did not become ready')
    assert not ui.adb('shell', 'pidof', 'screenrecord', check=False).strip()
    remote = '/sdcard/motovault-p01-motion.mp4'
    command = ['adb', '-s', ui.SERIAL, 'shell', 'screenrecord', '--bit-rate', '4000000', '--time-limit', '20', remote]
    if size:
        command[-1:-1] = ['--size', size]
    recorder = subprocess.Popen(['rtk', 'run', '-c', shlex.join(command)], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    time.sleep(0.7)
    pid = ui.adb('shell', 'pidof', 'screenrecord').strip()
    assert pid.isdigit()
    started = time.monotonic()
    try:
        ui.tap_label(tree, 'ওডোমিটার')
        action = time.monotonic() - started
        time.sleep(2)
        sheet = ui.tree(name + '-sheet')
        assert any('সেভ করুন' in n.get('text', '') for n in sheet.iter('node'))
        ui.screenshot(name + '-sheet')
        ui.tap_label(sheet, 'বন্ধ করুন')
        time.sleep(1)
    finally:
        ui.adb('shell', 'kill', '-2', pid, check=False)
        recorder.communicate(timeout=15)
        ui.adb('pull', remote, str(ui.OUT / (name + '.mp4')))
        ui.adb('shell', 'rm', '-f', remote)
    return dict(name=name, tapReturnedSeconds=round(action, 3),
                osAnimatorScale=ui.adb('shell', 'settings', 'get', 'global', 'animator_duration_scale').strip())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--size', choices=['540x1212', '1080x2424'], default='540x1212')
    args = parser.parse_args()
    assert args.run.replace('-', '').isalnum()
    ui.OUT = p00.ROOT / 'docs/roadmap/evidence/p01/android' / ('motion-os-' + args.run)
    ui.OUT.mkdir(exist_ok=False)
    original = ui.adb('shell', 'settings', 'get', 'global', 'animator_duration_scale').strip()
    p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
    p00.android(ui.SERIAL, 'runtime')
    ui.adb('shell', 'cmd', 'statusbar', 'collapse')
    time.sleep(2)
    observations = []
    try:
        for name, scale in [('normal', '1'), ('os-reduced-live', '0'), ('os-restored-live', '1')]:
            ui.adb('shell', 'settings', 'put', 'global', 'animator_duration_scale', scale)
            time.sleep(1)
            observations.append(capture(name, args.size))
            print(name + ' recorded', flush=True)
    finally:
        if original == 'null':
            ui.adb('shell', 'settings', 'delete', 'global', 'animator_duration_scale')
        else:
            ui.adb('shell', 'settings', 'put', 'global', 'animator_duration_scale', original)
        restored = ui.adb('shell', 'settings', 'get', 'global', 'animator_duration_scale').strip()
        p00.write_json(ui.OUT / 'observations.json', dict(app=p00.APP, synthetic=True,
            originalScale=original, restoredScale=restored, observations=observations,
            recordingSize=args.size,
            scope='Emulator recordings after live OS changes with app reduceMotion=false; timing/parity review remains separate.'))
        assert restored == original


if __name__ == '__main__':
    main()
