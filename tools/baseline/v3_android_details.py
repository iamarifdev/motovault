#!/usr/bin/env python3
"""Capture the v3 detail corrections using only the isolated emulator app."""
import argparse
import json
import shlex
import subprocess
import time

import capture_android as ui
import p00
import p01_android_reminders as reminders
import p01_storage


def snapshot(name):
    tree = ui.tree(name)
    ui.screenshot(name)
    return tree


def ready(label):
    for attempt in range(10):
        tree = ui.tree('ready-' + str(attempt))
        if label in reminders.labels(tree):
            return tree
        time.sleep(0.5)
    raise RuntimeError('Missing visible control: ' + label)


def tap(tree, label):
    ui.tap_label(tree, label)
    time.sleep(0.4)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--edges-only', action='store_true')
    parser.add_argument('--navigation-only', action='store_true')
    args = parser.parse_args()
    assert args.run.replace('-', '').isalnum()
    p00.FIXTURES = p00.ROOT / 'docs/roadmap/fixtures/p01'
    assert not (args.edges_only and args.navigation_only)
    if args.navigation_only:
        navigation(args.run)
        return
    if args.edges_only:
        edge_states(args.run)
        return
    for case in ['runtime', 'unknown-large']:
        out = p00.ROOT / 'docs/roadmap/evidence/p01/android' / ('v3-details-' + case + '-' + args.run)
        out.mkdir(exist_ok=False)
        ui.OUT = out
        p00.android(ui.SERIAL, case)  # Keeps the existing emulator-only guard.
        ready('ওডোমিটার')
        before = p01_storage.operate('android', 'read')
        ui.adb('shell', 'am', 'start', '-n', f'{p00.APP}/com.motovault.app.MainActivity')
        ready('ওডোমিটার')
        tree = snapshot('home')
        tap(tree, 'নতুন রেকর্ড যোগ করুন')
        tree = snapshot('quick-add')
        assert 'কী যোগ করবেন?' in reminders.labels(tree)
        tap(tree, 'তেল ভরা')
        tree = snapshot('fuel-sheet')
        tap(tree, 'বন্ধ করুন')
        tree = ready('ওডোমিটার')
        tap(tree, 'ইতিহাস')
        tree = snapshot('history')
        tap(tree, 'গ্যারেজ')
        tree = ready('ওডোমিটার')
        tap(tree, 'ওডোমিটার')
        tree = snapshot('odometer-sheet')
        tap(tree, '+১০' if case == 'runtime' else '+10')
        tree = ui.tree('odometer-draft')
        ui.tap_label(tree, 'সেভ করুন')
        time.sleep(0.3)
        ui.screenshot('saved-toast')
        tree = ui.tree('saved-toast')
        assert 'আনডু' in reminders.labels(tree)
        ui.tap_label(tree, 'আনডু')
        ready('ওডোমিটার')
        after = p01_storage.operate('android', 'read')
        assert before == after, 'Save followed by Undo must restore persisted data'
        p00.write_json(out / 'result.json', dict(result='Passed', case=case,
            applicationId=p00.APP, synthetic=True, nativeUndoPreservesData=True,
            scope='Native screenshots for icon/geometry/theme review and real save/Undo. Not full-app parity or physical-device proof.'))
        print(case + ': v3 detail screenshots and durable Undo passed', flush=True)


def edge_states(run):
    base = json.loads((p00.FIXTURES / 'native/runtime.json').read_text())
    base['documents'] = []
    base['ride']['modelId'] = 'v3-synthetic-unlisted'
    base['ride']['modelName'] = 'V3 synthetic unlisted model'
    p00.FIXTURES = p00.ROOT / 'build' / ('v3-edge-fixtures-' + run)
    p00.write_json(p00.FIXTURES / 'native/empty-vault.json', base)
    out = p00.ROOT / 'docs/roadmap/evidence/p01/android' / ('v3-edges-' + run)
    out.mkdir(exist_ok=False)
    ui.OUT = out
    p00.android(ui.SERIAL, 'empty-vault')
    tree = ready('ওডোমিটার')
    tap(tree, 'কাগজপত্র')
    tree = snapshot('empty-vault')
    assert 'কাগজ স্ক্যান করুন' in reminders.labels(tree)
    tap(tree, 'আমি')
    snapshot('profile-fallback')
    base['ride'] = None
    p00.write_json(p00.FIXTURES / 'native/onboarding.json', base)
    p00.android(ui.SERIAL, 'onboarding')
    ready('কোন বাইক চালান?')
    snapshot('onboarding')
    p00.write_json(out / 'result.json', dict(result='Passed', applicationId=p00.APP,
        synthetic=True, fixtures=str(p00.FIXTURES.relative_to(p00.ROOT)),
        scope='Empty Vault ScanLine, unlisted-model Gauge fallback, onboarding Bike and preserved native brand logos. Visual captures, not complete onboarding acceptance.'))
    print('Edge-state icon captures passed', flush=True)


def navigation(run):
    out = p00.ROOT / 'docs/roadmap/evidence/p01/android' / ('v3-navigation-' + run)
    out.mkdir(exist_ok=False)
    ui.OUT = out
    p00.android(ui.SERIAL, 'runtime')
    ready('ওডোমিটার')
    remote = '/sdcard/motovault-v3-tabs-' + str(time.monotonic_ns()) + '.mp4'
    command = ['adb', '-s', ui.SERIAL, 'shell', 'screenrecord', '--time-limit', '20', remote]
    recorder = subprocess.Popen(['rtk', 'run', '-c', shlex.join(command)], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        for label, name in [('কাগজপত্র', 'vault'), ('ইতিহাস', 'history'), ('আমি', 'me'), ('গ্যারেজ', 'home')]:
            tree = ui.tree('before-' + name)
            tap(tree, label)
            time.sleep(0.8)
            snapshot(name)
        stdout, stderr = recorder.communicate(timeout=25)
        assert recorder.returncode == 0, (stdout + stderr).decode()
        ui.adb('pull', remote, str(out / 'tab-marker.mp4'))
    finally:
        if recorder.poll() is None:
            recorder.terminate()
            recorder.wait(timeout=5)
        ui.adb('shell', 'rm', '-f', remote, check=False)
    p00.write_json(out / 'result.json', dict(result='Passed', applicationId=p00.APP,
        synthetic=True, appReducedMotion=False, tabs=['vault', 'history', 'me', 'home'],
        scope='Retained normal tab navigation recording and settled marker captures. Fine spring equality with the browser is not claimed.'))
    print('Tab navigation recording captured', flush=True)


if __name__ == '__main__':
    main()
